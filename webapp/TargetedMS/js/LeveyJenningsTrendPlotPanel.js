/*
 * Copyright (c) 2011-2015 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

/**
 * Class to create a panel for displaying the R plot for the trending of retention times, peak areas, and other
 * values for the selected graph parameters.
 *
 * To add PDF export support use LABKEY.vis.SVGConverter.convert.
 */
Ext4.define('LABKEY.targetedms.LeveyJenningsTrendPlotPanel', {

    extend: 'Ext.form.Panel',

    header: false,
    border: false,
    labelAlign: 'left',
    items: [],
    defaults: {
        xtype: 'panel',
        border: false
    },

    // properties specific to this TargetedMS Levey-Jennings plot implementation
    yAxisScale: 'linear',
    chartType: 'retentionTime',
    groupedX: false,
    singlePlot: false,
    plotWidth: null,
    enableBrushing: false,

    // properties used for the various data queries based on chart metric type
    chartTypePropArr: [{
        name: 'retentionTime',
        title: 'Retention Time',
        baseTableName: 'PrecursorChromInfo',
        baseLkFieldKey: '',
        colName: 'BestRetentionTime',
        statsTableName: 'GuideSetRetentionTimeStats'
    },{
        name: 'peakArea',
        title: 'Peak Area',
        baseTableName: 'PrecursorChromInfo',
        baseLkFieldKey: '',
        colName: 'TotalArea',
        statsTableName: 'GuideSetPeakAreaStats'
    },{
        name: 'fwhm',
        title: 'Full Width at Half Maximum (FWHM)',
        baseTableName: 'PrecursorChromInfo',
        baseLkFieldKey: '',
        colName: 'MaxFWHM',
        statsTableName: 'GuideSetFWHMStats'
    },{
        name: 'fwb',
        title: 'Full Width at Base (FWB)',
        baseTableName: 'PrecursorChromInfo',
        baseLkFieldKey: '',
        colName: '(MaxEndTime - MinStartTime)',
        statsTableName: 'GuideSetFWBStats'
    },{
        name: 'ratio',
        title: 'Light/Heavy Ratio',
        baseTableName: 'PrecursorAreaRatio',
        baseLkFieldKey: 'PrecursorChromInfoId.',
        colName: 'AreaRatio',
        statsTableName: 'GuideSetLHRatioStats'
    },{
        name: 'transitionPrecursorRatio',
        title: 'Transition/Precursor Area Ratio',
        baseTableName: 'PrecursorChromInfo',
        baseLkFieldKey: '',
        colName: 'transitionPrecursorRatio',
        statsTableName: 'GuideSetTPRatioStats'
    }],

    initComponent : function() {
        Ext4.tip.QuickTipManager.init();

        this.trendDiv = 'tiledPlotPanel';
        if (!this.startDate)
            this.startDate = null;
        if (!this.endDate)
            this.endDate = null;

        // initialize the y-axis scale combo for the top toolbar
        this.scaleCombo = Ext4.create('Ext.form.field.ComboBox', {
            id: 'scale-combo-box',
            width: 155,
            labelWidth: 80,
            fieldLabel: 'Y-Axis Scale',
            triggerAction: 'all',
            mode: 'local',
            store: Ext4.create('Ext.data.ArrayStore', {
                fields: ['value', 'display'],
                data: [['linear', 'Linear'], ['log', 'Log']]
            }),
            valueField: 'value',
            displayField: 'display',
            value: 'linear',
            forceSelection: true,
            editable: false,
            listeners: {
                scope: this,
                change: function(cmp, newVal, oldVal) {
                    this.yAxisScale = newVal;
                    this.renderPlots();
                }
            }
        });

        // initialize the date range selection fields for the top toolbar
        this.startDateField = Ext4.create('Ext.form.field.Date', {
            id: 'start-date-field',
            width: 180,
            labelWidth: 65,
            fieldLabel: 'Start Date',
            value: this.startDate,
            allowBlank: false,
            format:  'Y-m-d',
            listeners: {
                scope: this,
                validitychange: function (df, isValid) {
                    this.applyFilterButton.setDisabled(!isValid);
                }
            }
        });

        this.endDateField = Ext4.create('Ext.form.field.Date', {
            id: 'end-date-field',
            width: 175,
            labelWidth: 60,
            fieldLabel: 'End Date',
            value: this.endDate,
            allowBlank: false,
            format:  'Y-m-d',
            listeners: {
                scope: this,
                validitychange: function (df, isValid) {
                    this.applyFilterButton.setDisabled(!isValid);
                }
            }
        });

        this.chartTypeField = Ext4.create('Ext.form.field.ComboBox', {
            id: 'chart-type-field',
            width: 340,
            labelWidth: 70,
            fieldLabel: 'Chart Type',
            triggerAction: 'all',
            mode: 'local',
            store: Ext4.create('Ext.data.Store', {
                fields: ['name', 'title'],
                data: this.chartTypePropArr
            }),
            valueField: 'name',
            displayField: 'title',
            value: this.chartType,
            forceSelection: true,
            editable: false,
            listeners: {
                scope: this,
                change: function(cmp, newVal, oldVal) {
                    this.chartType = newVal;
                    this.displayTrendPlot();
                }
            }
        });

        // initialize the refresh graph button
        this.applyFilterButton = Ext4.create('Ext.button.Button', {
            disabled: true,
            text: 'Apply',
            handler: this.applyGraphFilterBtnClick,
            scope: this
        });

        // initialize the checkbox to toggle separate vs groups x-values
        this.groupedXCheckbox = Ext4.create('Ext.form.field.Checkbox', {
            id: 'grouped-x-field',
            boxLabel: 'Group X-Axis Values by Date',
            listeners: {
                scope: this,
                change: function(cb, newValue, oldValue) {
                    this.groupedX = newValue;

                    // we don't allow creation of guide sets in grouped x-axis mode
                    this.createGuideSetToggleButton.setDisabled(this.groupedX || this.singlePlot);
                    this.setBrushingEnabled(false);

                    //TODO this should be this.renderPlots() but there is a bug with the yStepIndex being reset on grouping of x-axis
                    this.getAnnotationData();
                }
            }
        });

        // initialize the checkbox to show peptides in a single plot
        this.peptidesInSinglePlotCheckbox = Ext4.create('Ext.form.field.Checkbox', {
            id: 'peptides-single-plot',
            boxLabel: 'Show All Peptides in Single Plot',
            listeners: {
                scope: this,
                change: function(cb, newValue, oldValue) {
                    this.singlePlot = newValue;

                    // we don't currently allow creation of guide sets in single plot mode
                    this.createGuideSetToggleButton.setDisabled(this.groupedX || this.singlePlot);
                    this.setBrushingEnabled(false);

                    this.renderPlots();
                }
            }
        });

        // initialize the create guide set button
        this.createGuideSetToggleButton = Ext4.create('Ext.button.Button', {
            text: 'Create Guide Set',
            tooltip: 'Enable/disable guide set creation mode',
            enableToggle: true,
            handler: function(btn) {
                this.setBrushingEnabled(btn.pressed);
            },
            scope: this
        });

        var tbspacer = {xtype: 'tbspacer'};

        var toolbar1 = Ext4.create('Ext.toolbar.Toolbar', {
            ui: 'footer',
            layout: { pack: 'center' },
            padding: 10,
            items: [
                this.chartTypeField, tbspacer,
                {xtype: 'tbseparator'}, tbspacer,
                this.startDateField, tbspacer,
                this.endDateField, tbspacer,
                this.applyFilterButton
            ]
        });

        var toolbar2Items = [
            this.scaleCombo, tbspacer,
            {xtype: 'tbseparator'}, tbspacer,
            this.groupedXCheckbox, tbspacer,
            {xtype: 'tbseparator'}, tbspacer,
            this.peptidesInSinglePlotCheckbox, tbspacer
        ];

        // only add the create guide set button if the user has the proper permissions to insert/update guide sets
        if (this.canUserEdit()) {
            toolbar2Items.push({xtype: 'tbseparator'}, tbspacer, this.createGuideSetToggleButton);
        }

        var toolbar2 = Ext4.create('Ext.toolbar.Toolbar', {
            ui: 'footer',
            layout: { pack: 'center' },
            padding: '0 10px 10px 10px',
            items: toolbar2Items
        });

        var toolbar3 = Ext4.create('Ext.toolbar.Toolbar', {
            ui: 'footer',
            layout: { pack: 'center' },
            cls: 'guideset-toolbar-msg',
            hidden: true,
            items: [{
                xtype: 'box',
                itemId: 'GuideSetMessageToolBar',
                html: 'Please click and drag in the plot to select the guide set training date range.'
            }]
        });

        this.items = [{ tbar: toolbar1 }, { tbar: toolbar2 }, { tbar: toolbar3 }];

        this.callParent();

        this.displayTrendPlot();
    },

    setBrushingEnabled : function(enabled) {
        this.enableBrushing = enabled;
        this.clearPlotBrush();
        this.setPlotBrushingDisplayStyle();
        this.toggleGuideSetMsgDisplay();
        this.createGuideSetToggleButton.toggle(enabled);
    },

    setLoadingMsg : function() {
        Ext4.get(this.trendDiv).update("");
        Ext4.get(this.trendDiv).mask("Loading...");
    },

    displayTrendPlot: function() {
        this.setLoadingMsg();
        this.getDistinctPrecursors();
    },

    getChartTypePropsByName: function(name) {
        for (var i = 0; i < this.chartTypePropArr.length; i++) {
            if (this.chartTypePropArr[i].name == name) {
                return this.chartTypePropArr[i];
            }
        }
        return {};
    },

    getDistinctPrecursors: function() {

        var chartTypeProps = this.getChartTypePropsByName(this.chartType);
        var baseTableName = chartTypeProps.baseTableName;
        var baseLkFieldKey = chartTypeProps.baseLkFieldKey;

        var sql = "SELECT DISTINCT " + baseLkFieldKey + "PrecursorId.ModifiedSequence AS Sequence FROM " + baseTableName;
        var separator = ' WHERE ';
        // CAST as DATE to ignore time portion of value
        if (this.startDate)
        {
            var startDate = this.startDate instanceof Date ? Ext4.util.Format.date(this.startDate, 'Y-m-d') : this.startDate;
            sql += separator + "CAST(" + baseLkFieldKey + "PeptideChromInfoId.SampleFileId.AcquiredTime AS DATE) >= '" + startDate + "'";
            separator = " AND ";
        }
        if (this.endDate)
        {
            var endDate = this.endDate instanceof Date ? Ext4.util.Format.date(this.endDate, 'Y-m-d') : this.endDate;
            sql += separator + "CAST(" + baseLkFieldKey + "PeptideChromInfoId.SampleFileId.AcquiredTime AS DATE) <= '" + endDate + "'";
        }

        // Cap the peptide count at 50
        sql += " ORDER BY Sequence LIMIT 50";

        LABKEY.Query.executeSql({
            schemaName: 'targetedms',
            sql: sql,
            scope: this,
            success: function(data) {

                if (data.rows.length == 0) {
                    this.failureHandler({message: "There were no records found. The date filter applied may be too restrictive."});
                    return;
                }

                // stash the set of precursor sequences for use with the plot rendering
                this.precursors = [];
                for (var i = 0; i < data.rows.length; i++) {
                    this.precursors.push(data.rows[i].Sequence);
                }

                this.getAnnotationData();
            },
            failure: this.failureHandler
        });
    },

    getAnnotationData: function() {
        var config = this.getReportConfig();

        var annotationSql = "SELECT qca.Date, qca.Description, qca.Created, qca.CreatedBy.DisplayName, qcat.Name, qcat.Color FROM qcannotation qca JOIN qcannotationtype qcat ON qcat.Id = qca.QCAnnotationTypeId";

        // Filter on start/end dates
        var separator = " WHERE ";
        if (config.StartDate) {
            annotationSql += separator + "Date >= '" + config.StartDate + "'";
            separator = " AND ";
        }
        if (config.EndDate) {
            annotationSql += separator + "Date <= '" + config.EndDate + "'";
        }

        LABKEY.Query.executeSql({
            schemaName: 'targetedms',
            sql: annotationSql,
            sort: 'Date',
            containerFilter: LABKEY.Query.containerFilter.currentPlusProjectAndShared,
            scope: this,
            success: this.processAnnotationData,
            failure: this.failureHandler
        });
    },

    processAnnotationData: function(data) {
        this.annotationData = data.rows;
        this.annotationShape = LABKEY.vis.Scale.Shape()[4]; // 0: circle, 1: triangle, 2: square, 3: diamond, 4: X

        var dateCount = {};
        this.legendData = [];
        for (var i = 0; i < this.annotationData.length; i++)
        {
            var annotation = this.annotationData[i];
            var annotationDate = this.formatDate(new Date(annotation['Date']), !this.groupedX);

            // track if we need to stack annotations that fall on the same date
            if (!dateCount[annotationDate]) {
                dateCount[annotationDate] = 0;
            }
            annotation.yStepIndex = dateCount[annotationDate];
            dateCount[annotationDate]++;

            // get unique annotation names and colors for the legend
            if (Ext4.Array.pluck(this.legendData, "text").indexOf(annotation['Name']) == -1)
            {
                this.legendData.push({
                    text: annotation['Name'],
                    color: '#' + annotation['Color'],
                    shape: this.annotationShape
                });
            }
        }

        this.getGuideSetData();
    },

    getGuideSetData : function() {
        var config = this.getReportConfig();
        var chartTypeProps = this.getChartTypePropsByName(this.chartType);

        // Filter on start/end dates from the QC plot form
        var guideSetSql = "SELECT s.*, g.Comment FROM " + chartTypeProps.statsTableName + " s"
                + " LEFT JOIN GuideSet g ON g.RowId = s.GuideSetId";
        var separator = " WHERE ";
        if (config.StartDate) {
            guideSetSql += separator + "s.TrainingEnd >= '" + config.StartDate + "'";
            separator = " AND ";
        }
        if (config.EndDate) {
            guideSetSql += separator + "s.TrainingStart <= TIMESTAMPADD('SQL_TSI_DAY', 1, CAST('" + config.EndDate + "' AS TIMESTAMP))";
        }

        LABKEY.Query.executeSql({
            schemaName: 'targetedms',
            sql: guideSetSql,
            sort: 'TrainingStart,Sequence',
            scope: this,
            success: this.processGuideSetData,
            failure: this.failureHandler
        });
    },

    processGuideSetData : function(data) {
        this.guideSetTrainingData = data.rows;
        var guideSetTrainingDataUniqueIds = Ext4.Array.toValueMap(this.guideSetTrainingData , 'GuideSetId');
        this.guideSetTrainingDataUniqueObjects = [];

        for(var i in guideSetTrainingDataUniqueIds) {
            if(guideSetTrainingDataUniqueIds.hasOwnProperty(i)) {
                this.guideSetTrainingDataUniqueObjects.push(Ext4.clone(guideSetTrainingDataUniqueIds[i]));
            }
        }
      this.getPlotData();
    },

    getPlotData: function() {
        var config = this.getReportConfig();

        var chartTypeProps = this.getChartTypePropsByName(this.chartType);
        var baseTableName = chartTypeProps.baseTableName;
        var baseLkFieldKey = chartTypeProps.baseLkFieldKey;
        var typeColName = chartTypeProps.colName;
        var statsTableName = chartTypeProps.statsTableName;

        // Filter on start/end dates, casting as DATE to ignore the time part
        var whereClause = " WHERE ";
        var separator = "";
        if (config.StartDate) {
            whereClause += separator + "CAST(" + baseLkFieldKey + "PeptideChromInfoId.SampleFileId.AcquiredTime AS DATE) >= '" + config.StartDate + "'";
            separator = " AND ";
        }
        if (config.EndDate) {
            whereClause += separator + "CAST(" + baseLkFieldKey + "PeptideChromInfoId.SampleFileId.AcquiredTime AS DATE) <= '" + config.EndDate + "'";
        }

        var guideSetStatsJoinClause = "ON X.Sequence = stats.Sequence AND ((X.AcquiredTime >= stats.TrainingStart "
                + "AND X.AcquiredTime < stats.ReferenceEnd) OR (X.AcquiredTime >= stats.TrainingStart AND stats.ReferenceEnd IS NULL))";

        // Build query to get the values and mean/stdDev ranges for each data point
        var sql = "SELECT X.PrecursorId, X.PrecursorChromInfoId, X.Sequence, X.AcquiredTime, X.FilePath, X.Value, "
            + " CASE WHEN (X.AcquiredTime >= stats.TrainingStart AND X.AcquiredTime <= stats.TrainingEnd) THEN TRUE ELSE FALSE END AS InGuideSetTrainingRange, "
            + " stats.GuideSetId, stats.Mean, stats.StandardDev "
            + " FROM (SELECT " + baseLkFieldKey + "PrecursorId.Id AS PrecursorId, "
            + "       " + baseLkFieldKey + "Id AS PrecursorChromInfoId, "
            + "       " + baseLkFieldKey + "PrecursorId.ModifiedSequence AS Sequence, "
            + "       " + baseLkFieldKey + "PeptideChromInfoId.SampleFileId.AcquiredTime AS AcquiredTime, "
            + "       " + baseLkFieldKey + "PeptideChromInfoId.SampleFileId.FilePath AS FilePath, "
            + "       "  + typeColName + " AS Value FROM " + baseTableName + whereClause + ") X "
            + " LEFT JOIN " + statsTableName + " stats " + guideSetStatsJoinClause;

        LABKEY.Query.executeSql({
            schemaName: 'targetedms',
            sql: sql,
            sort: 'Sequence, AcquiredTime',
            scope: this,
            success: this.processPlotData,
            failure: this.failureHandler
        });
    },

    processPlotData: function(data) {
        // process the data to shape it for the JS LeveyJenningsPlot API call
        this.sequencePlotData = {};
        for (var i = 0; i < data.rows.length; i++)
        {
            var row = data.rows[i];
            var sequence = row['Sequence'];

            if (!this.sequencePlotData[sequence]) {
                this.sequencePlotData[sequence] = {sequence: sequence, data: [], min: null, max: null};
            }

            this.sequencePlotData[sequence].data.push({
                type: 'data',
                sequence: sequence,
                PrecursorId: row['PrecursorId'], // keep in data for click handler
                PrecursorChromInfoId: row['PrecursorChromInfoId'], // keep in data for click handler
                FilePath: row['FilePath'], // keep in data for hover text display
                fullDate: row['AcquiredTime'] ? this.formatDate(new Date(row['AcquiredTime']), true) : null,
                date: row['AcquiredTime'] ? this.formatDate(new Date(row['AcquiredTime'])) : null,
                value: row['Value'],
                mean: row['Mean'],
                stdDev: row['StandardDev'],
                guideSetId: row['GuideSetId'],
                inGuideSetTrainingRange: row['InGuideSetTrainingRange']
            });

            this.setSequenceMinMax(this.sequencePlotData[sequence], row);
        }

        // merge in the annotation data to make room on the y axis
        for (var i = 0; i < this.precursors.length; i++)
        {
            var precursorInfo = this.sequencePlotData[this.precursors[i]];

            // We don't necessarily have info for all possible precursors, depending on the filters and plot type
            if (precursorInfo)
            {
                // if the min and max are the same, or very close, increase the range
                if (precursorInfo.max - precursorInfo.min < 0.0001)
                {
                    precursorInfo.max += 1;
                    precursorInfo.min -= 1;
                }

                // add any missing dates from the QC annotation data to the plot data
                var precursorDates = Ext4.Array.pluck(precursorInfo.data, (this.groupedX ? "date" : "fullDate"));
                var datesToAdd = [];
                for (var j = 0; j < this.annotationData.length; j++)
                {
                    var annFullDate = this.formatDate(new Date(this.annotationData[j].Date), true);
                    var annDate = this.formatDate(new Date(this.annotationData[j].Date));

                    if (this.groupedX) {
                        if (precursorDates.indexOf(annDate) == -1 && Ext4.Array.pluck(datesToAdd, "date").indexOf(annDate) == -1) {
                            datesToAdd.push({ type: 'annotation', fullDate: annDate, date: annDate }); // we don't need full date if grouping x-values
                        }
                    }
                    else {
                        if (precursorDates.indexOf(annFullDate) == -1 && Ext4.Array.pluck(datesToAdd, "fullDate").indexOf(annFullDate) == -1) {
                            datesToAdd.push({ type: 'annotation', fullDate: annFullDate, date: annDate });
                        }
                    }
                }
                if (datesToAdd.length > 0)
                {
                    var index = 0;
                    for (var k = 0; k < datesToAdd.length; k++)
                    {
                        var added = false;
                        for (var l = index; l < precursorInfo.data.length; l++)
                        {
                            if ((this.groupedX && precursorInfo.data[l].date > datesToAdd[k].date)
                                || (!this.groupedX && precursorInfo.data[l].fullDate > datesToAdd[k].fullDate))
                            {
                                precursorInfo.data.splice(l, 0, datesToAdd[k]);
                                added = true;
                                index = l;
                                break;
                            }
                        }
                        // tack on any remaining dates to the end
                        if (!added)
                        {
                            precursorInfo.data.push(datesToAdd[k]);
                        }
                    }
                }
            }
        }

        this.renderPlots();
    },

    renderPlots: function() {
        this.setLoadingMsg();
        this.setPlotWidth();

        var addedPlot = false;
        if (this.singlePlot) {
            addedPlot = this.addCombinedPeptideSinglePlot();
        }
        else {
            addedPlot = this.addIndividualPrecursorPlots();
        }

        if (!addedPlot) {
            Ext4.get(this.trendDiv).insertHtml('beforeEnd', '<div>No data to plot</div>');
        }

        Ext4.get(this.trendDiv).unmask();
    },

    setPlotWidth : function() {
        if (this.plotWidth == null)
        {
            // set the width of the plot webparts based on the first labkey-wp-body element (i.e. QC Summary webpart in this case)
            this.plotWidth = 900;
            var wp = document.querySelector('.labkey-wp-body');
            if (wp && (wp.clientWidth - 20) > this.plotWidth) {
                this.plotWidth = wp.clientWidth - 20;
            }

            Ext4.get(this.trendDiv).setWidth(this.plotWidth);
        }
    },

    addPlotWebPartToTrendDiv : function(id, title) {
        Ext4.get(this.trendDiv).insertHtml('beforeEnd', '<br/>' +
            '<table class="labkey-wp qc-plot-wp">' +
            ' <tr class="labkey-wp-header">' +
            '     <th class="labkey-wp-title-left"><span class="labkey-wp-title-text qc-plot-wp-title">' + Ext4.util.Format.htmlEncode(title) +
            ' <div style="display:inline; float:right;" id="' + id + '-exportToPDFbutton"></div></span></th>' +
            ' </tr><tr>' +
            '     <td class="labkey-wp-body"><div id="' + id + '"></div></</td>' +
            ' </tr>' +
            '</table>'
        );
    },

    getMaxStackedAnnotations : function() {
        if (this.annotationData.length > 0) {
            return Math.max.apply(Math, (Ext4.Array.pluck(this.annotationData, "yStepIndex"))) + 1;
        }
        return 0;
    },

    getBasePlotConfig : function(id, data, legenddata) {
        return {
            rendererType : 'd3',
            renderTo : id,
            data : Ext4.Array.clone(data),
            width : this.plotWidth - 30,
            height : this.singlePlot ? 500 : 300,
            gridLineColor : 'white',
            legendData : Ext4.Array.clone(legenddata)
        };
    },

    createExportToPDFButton : function(id, sequence) {
        new Ext4.Button({
            renderTo: id+"-exportToPDFbutton",
            svgDivId: id,
            sequence: sequence ? sequence : "",
            icon: LABKEY.contextPath + "/_icons/pdf.gif",
            tooltip: "Export PDF of this plot",
            handler: function(btn) {
                var svgEls = Ext4.get(btn.svgDivId).select('svg');
                var title = btn.sequence ? 'QC Plot for peptide ' + btn.sequence : 'QC Combined Plot for All Peptides' ;
                var svgStr = LABKEY.vis.SVGConverter.svgToStr(svgEls.elements[0]);
                svgStr = svgStr.replace(/visibility="hidden"/g, 'visibility="visible"');
                LABKEY.vis.SVGConverter.convert(svgStr, LABKEY.vis.SVGConverter.FORMAT_PDF, title);
            },
            scope: this
        });
    },

    addIndividualPrecursorPlots : function() {
        var addedPlot = false;

        for (var i = 0; i < this.precursors.length; i++)
        {
            var precursorInfo = this.sequencePlotData[this.precursors[i]];

            // We don't necessarily have info for all possible precursors, depending on the filters and plot type
            if (precursorInfo)
            {
                addedPlot = true;

                // add a new panel for each plot so we can add the title to the frame
                var id = "precursorPlot" + i;
                this.addPlotWebPartToTrendDiv(id, this.precursors[i]);

                if (precursorInfo.showLogWarning) {
                    Ext4.get(id).update("<span style='font-style: italic;'>For log scale, standard deviations below the mean with negative values have been omitted.</span>");
                }

                var me = this; // for plot brushing

                // create plot using the JS Vis API
                var basePlotConfig = this.getBasePlotConfig(id, precursorInfo.data, this.legendData);
                var plotConfig = Ext4.apply(basePlotConfig, {
                    margins : {
                        top: 45 + this.getMaxStackedAnnotations() * 12,
                        bottom: 75
                    },
                    labels : {
                        main: {value: this.precursors[i], visibility: 'hidden'},
                        y: {value: this.getChartTypePropsByName(this.chartType).title, visibility:'hidden'}
                    },
                    properties: {
                        value: 'value',
                        mean: 'mean',
                        stdDev: 'stdDev',
                        xTick: this.groupedX ? 'date' : 'fullDate',
                        xTickLabel: 'date',
                        yAxisDomain: [precursorInfo.min, precursorInfo.max],
                        yAxisScale: this.yAxisScale,
                        shape: 'guideSetId',
                        showTrendLine: true,
                        hoverTextFn: this.plotHoverTextDisplay,
                        pointClickFn: this.plotPointClick,
                        position: this.groupedX ? 'jitter' : undefined
                    },
                    brushing: !this.allowGuideSetBrushing() ? undefined : {
                        dimension: 'x',
                        fillOpacity: 0.4,
                        fillColor: 'rgba(20, 204, 201, 1)',
                        strokeColor: 'rgba(20, 204, 201, 1)',
                        brushstart: function(event, data, extent, plot, layerSelections) {
                            me.plotBrushStartEvent(plot);
                        },
                        brush: function(event, data, extent, plot, layerSelections) {
                            me.plotBrushEvent(extent, plot, layerSelections);
                        },
                        brushend: function(event, data, extent, plot, layerSelections) {
                            me.plotBrushEndEvent(data[0], extent, plot);
                        },
                        brushclear: function(event, data, plot, layerSelections) {
                            me.plotBrushClearEvent(data[0], plot);
                        }
                    }
                });

                var plot = LABKEY.vis.LeveyJenningsPlot(plotConfig);
                plot.render();

                this.addAnnotationsToPlot(plot, precursorInfo);

                // only show the guide set training ranges when not grouping x-axis by date
                if (!this.groupedX) {
                    this.addGuideSetTrainingRangeToPlot(plot, precursorInfo, this.guideSetTrainingData);
                }

                this.createExportToPDFButton(id, precursorInfo.sequence);
            }
        }

        this.setPlotBrushingDisplayStyle();

        return addedPlot;
    },

    addCombinedPeptideSinglePlot : function() {
        var groupColors = LABKEY.vis.Scale.ColorDiscrete().concat(LABKEY.vis.Scale.DarkColorDiscrete());

        var newLegendData = Ext4.Array.clone(this.legendData);

        var combinePlotData = {min: null, max: null, data: []};

        var lengthOfLongestPeptide = 1;

        for (var i = 0; i < this.precursors.length; i++)
        {
            var precursorInfo = this.sequencePlotData[this.precursors[i]];

            if (precursorInfo.sequence.length > lengthOfLongestPeptide) {
                lengthOfLongestPeptide = precursorInfo.sequence.length;
            }

            // for combined plot, concat all data together into a single array and track min/max for all
            combinePlotData.data = combinePlotData.data.concat(precursorInfo.data);
            if (combinePlotData.min == null || combinePlotData.min > precursorInfo.min) {
                combinePlotData.min = precursorInfo.min;
            }
            if (combinePlotData.max == null || combinePlotData.max < precursorInfo.max) {
                combinePlotData.max = precursorInfo.max;
            }

            //add the sequence name for each group to the legend
            if(this.singlePlot)
            {
                newLegendData.push({
                    text: precursorInfo.sequence,
                    color: groupColors[i % groupColors.length]
                });
            }
        }

        var id = 'combinedPlot';
        this.addPlotWebPartToTrendDiv(id, 'All Peptides');

        var basePlotConfig = this.getBasePlotConfig(id, combinePlotData.data, newLegendData);
        var plotConfig = Ext4.apply(basePlotConfig, {
            margins : {
                top: 45 + this.getMaxStackedAnnotations() * 12,
                right: 10*lengthOfLongestPeptide,
                bottom: 75
            },
            labels : {
                main: {value: "All Peptides", visibility: 'hidden'},
                y: {value: this.getChartTypePropsByName(this.chartType).title, visibility:'hidden'}
            },
            properties: {
                value: 'value',
                xTick: this.groupedX ? 'date' : 'fullDate',
                xTickLabel: 'date',
                yAxisDomain: [combinePlotData.min, combinePlotData.max],
                yAxisScale: this.yAxisScale,
                shape: 'guideSetId',
                groupBy: 'sequence',
                color: 'sequence',
                showTrendLine: true,
                disableRangeDisplay: true,
                hoverTextFn: this.plotHoverTextDisplay,
                pointClickFn: this.plotPointClick,
                position: this.groupedX ? 'jitter' : undefined
            }
        });

        var plot = LABKEY.vis.LeveyJenningsPlot(plotConfig);
        plot.render();

        this.addAnnotationsToPlot(plot, combinePlotData);

        if (!this.groupedX) {
            this.addGuideSetTrainingRangeToPlot(plot, combinePlotData, this.guideSetTrainingDataUniqueObjects);
        }

        this.createExportToPDFButton(id);

        return true;
    },

    plotHoverTextDisplay : function(row){
        return 'Acquired: ' + row['fullDate'] + ", "
            + '\nValue: ' + row.value + ", "
            + '\nFile Path: ' + row['FilePath'];
    },

    plotPointClick : function(event, row) {
        window.location = LABKEY.ActionURL.buildURL('targetedms', "precursorAllChromatogramsChart", LABKEY.ActionURL.getContainer(), { id: row.PrecursorId, chromInfoId: row.PrecursorChromInfoId }) + '#ChromInfo' + row.PrecursorChromInfoId;
    },

    plotBrushStartEvent : function(plot) {
        this.clearPlotBrush(plot);
    },

    plotBrushEvent : function(extent, plot, layers) {
        Ext4.each(layers, function(layer){
            var points = layer.selectAll('.point path');
            if (points[0].length > 0)
            {
                var colorAcc = function(d) {
                    var x = plot.scales.x.scale(d.seqValue);
                    d.isInSelection = (x > extent[0][0] && x < extent[1][0]);
                    return d.isInSelection ? 'rgba(20, 204, 201, 1)' : '#000000';
                };

                points.attr('fill', colorAcc).attr('stroke', colorAcc);
            }
        });
    },

    plotBrushEndEvent : function(data, extent, plot) {
        var selectedPoints = Ext4.Array.filter(data, function(point){ return point.isInSelection; });
        this.plotBrushSelection = {plot: plot, points: selectedPoints};

        // add the guide set create and cancel buttons over the brushed region
        if (selectedPoints.length > 0)
        {
            var me = this;
            var xMid = extent[0][0] + (extent[1][0] - extent[0][0]) / 2;

            var createBtn = this.createGuideSetSvgButton(plot, 'Create', xMid - 57, 54);
            createBtn.on('click', function() {
                me.createGuideSetBtnClick();
            });

            var cancelBtn = this.createGuideSetSvgButton(plot, 'Cancel', xMid + 3, 53);
            cancelBtn.on('click', function () {
                me.clearPlotBrush(plot);
                plot.clearBrush();
                me.setBrushingEnabled(false);
            });

            this.bringSvgElementToFront(plot, "g.guideset-svg-button");
        }
    },

    plotBrushClearEvent : function(data, plot) {
        this.plotBrushSelection = undefined;
    },

    canUserEdit : function() {
        return LABKEY.user.canInsert && LABKEY.user.canUpdate;
    },

    allowGuideSetBrushing : function() {
        return this.canUserEdit() && !this.groupedX;
    },

    createGuideSetSvgButton : function(plot, text, xLeftPos, width) {
        var yRange = plot.scales.yLeft.range;
        var yTopPos = yRange[1] + (yRange[0] - yRange[1]) / 2 - 10;

        var svgBtn = this.getSvgElForPlot(plot).append('g')
                .attr('class', 'guideset-svg-button');

        svgBtn.append('rect')
                .attr('x', xLeftPos).attr('y', yTopPos).attr('rx', 5).attr('ry', 5)
                .attr('width', width).attr('height', 20)
                .style({'fill': '#ffffff', 'stroke': '#b4b4b4'});

        svgBtn.append('text').text(text)
                .attr('x', xLeftPos + 5).attr('y', yTopPos + 14)
                .style({'fill': '#126495', 'font-size': '10px', 'font-weight': 'bold', 'text-transform': 'uppercase'});

        return svgBtn;
    },

    setPlotBrushingDisplayStyle : function() {
        // hide the brushing related components for all plots if not in "create guide set" mode
        var displayStyle = this.enableBrushing ? 'inline' : 'none';
        d3.selectAll('.brush').style({'display': displayStyle});
        d3.selectAll('.x-axis-handle').style({'display': displayStyle});
    },

    clearPlotBrush : function(plot) {
        // clear any create/cancel buttons and brush areas from other plots
        if (this.plotBrushSelection) {
            this.getSvgElForPlot(this.plotBrushSelection.plot).selectAll(".guideset-svg-button").remove();

            if (this.plotBrushSelection.plot != plot) {
                this.plotBrushSelection.plot.clearBrush();
            }
        }
    },

    getSvgElForPlot : function(plot) {
        return d3.select('#' + plot.renderTo + ' svg');
    },

    toggleGuideSetMsgDisplay : function() {
        var toolbarMsg = this.down('#GuideSetMessageToolBar');
        toolbarMsg.up('toolbar').setVisible(this.enableBrushing);
    },

    addGuideSetTrainingRangeToPlot : function(plot, precursorInfo, guideSetTrainingDataOrig) {
        var me = this;
        var guideSetTrainingData = [];

        // find the x-axis starting and ending index based on the guide set information attached to each data point
        for (var i = 0; i < guideSetTrainingDataOrig.length; i++)
        {
            // only compare guide set info for matching precursor sequence
            if (!this.singlePlot && precursorInfo.sequence != guideSetTrainingDataOrig[i].Sequence) {
                continue;
            }

            var gs = Ext4.clone(guideSetTrainingDataOrig[i]);

            for (var j = 0; j < precursorInfo.data.length; j++)
            {
                // only use data points that match the GuideSet RowId and are in the training set range
                if (precursorInfo.data[j].guideSetId == gs.GuideSetId && precursorInfo.data[j].inGuideSetTrainingRange) {
                    if (gs.StartIndex == undefined) {
                        gs.StartIndex = precursorInfo.data[j].seqValue;
                    }
                    gs.EndIndex = precursorInfo.data[j].seqValue;
                }
            }

            if (gs.StartIndex != undefined) {
                guideSetTrainingData.push(gs);
            }
        }

        if (guideSetTrainingData.length > 0)
        {
            // add a "shaded" rect to indicate which points in the plot are part of the guide set training range
            var binWidth = (plot.grid.rightEdge - plot.grid.leftEdge) / (plot.scales.x.scale.domain().length);
            var yRange = plot.scales.yLeft.range;

            var xAcc = function (d) {
                return plot.scales.x.scale(d.StartIndex) - (binWidth/2);
            };

            var widthAcc = function (d) {
                return plot.scales.x.scale(d.EndIndex) - plot.scales.x.scale(d.StartIndex) + binWidth;
            };

            var guideSetTrainingRange = this.getSvgElForPlot(plot).selectAll("rect.training").data(guideSetTrainingData)
                .enter().append("rect").attr("class", "training")
                .attr('x', xAcc).attr('y', yRange[1])
                .attr('width', widthAcc).attr('height', yRange[0] - yRange[1])
                .attr('stroke', '#000000').attr('stroke-opacity', 0.1)
                .attr('fill', '#000000').attr('fill-opacity', 0.1);

            guideSetTrainingRange.append("title").text(function (d) {
                return "Guide Set ID: " + d['GuideSetId'] + ","
                        + "\nStart: " + me.formatDate(new Date(d['TrainingStart']), true)
                        + ",\nEnd: " + me.formatDate(new Date(d['TrainingEnd']), true)
                        + (!me.singlePlot ? ",\n# Runs: " + d['NumRecords'] : "")
                        + (!me.singlePlot ? ",\nMean: " + me.formatNumeric(d['Mean']) : "")
                        + (!me.singlePlot ? ",\nStd Dev: " + me.formatNumeric(d['StandardDev']) : "")
                        + (d['Comment'] ? (",\nComment: " + d['Comment']) : "");
            });
        }

        this.bringSvgElementToFront(plot, "g.error-bar");
        this.bringSvgElementToFront(plot, "path");
        this.bringSvgElementToFront(plot, "a.point");
        this.bringSvgElementToFront(plot, "rect.extent");
    },

    bringSvgElementToFront: function(plot, selector) {
        this.getSvgElForPlot(plot).selectAll(selector)
            .each(function() {
               this.parentNode.parentNode.appendChild(this.parentNode);
            });
    },

    addAnnotationsToPlot: function(plot, precursorInfo) {
        var me = this;

        var xAxisLabels = Ext4.Array.pluck(precursorInfo.data, (this.groupedX ? "date" : "fullDate"));

        if (this.groupedX || this.singlePlot) {
            xAxisLabels = Ext4.Array.sort(Ext4.Array.unique(xAxisLabels));
        }
        // use direct D3 code to inject the annotation icons to the rendered SVG
        var xAcc = function(d) {
            var annotationDate = me.formatDate(new Date(d['Date']), !me.groupedX);
            return plot.scales.x.scale(xAxisLabels.indexOf(annotationDate));
        };
        var yAcc = function(d) {
            return plot.scales.yLeft.scale(precursorInfo.max) - (d['yStepIndex'] * 12) - 12;
        };
        var transformAcc = function(d){
            return 'translate(' + xAcc(d) + ',' + yAcc(d) + ')';
        };
        var colorAcc = function(d) {
            return '#' + d['Color'];
        };
        var annotations = this.getSvgElForPlot(plot).selectAll("path.annotation").data(this.annotationData)
            .enter().append("path").attr("class", "annotation")
            .attr("d", this.annotationShape(5)).attr('transform', transformAcc)
            .style("fill", colorAcc).style("stroke", colorAcc);

        // add hover text for the annotation details
        annotations.append("title")
            .text(function(d) {
                return "Created By: " + d['DisplayName'] + ", "
                    + "\nDate: " + me.formatDate(new Date(d['Date']), true) + ", "
                    + "\nDescription: " + d['Description'];
            });

        // add some mouseover effects for fun
        var mouseOn = function(pt, strokeWidth) {
            d3.select(pt).transition().duration(800).attr("stroke-width", strokeWidth).ease("elastic");
        };
        var mouseOff = function(pt) {
            d3.select(pt).transition().duration(800).attr("stroke-width", 1).ease("elastic");
        };
        annotations.on("mouseover", function(){ return mouseOn(this, 3); });
        annotations.on("mouseout", function(){ return mouseOff(this); });
    },

    formatDate: function(d, includeTime) {
        if (d instanceof Date) {
            if (includeTime) {
                return Ext4.util.Format.date(d, 'Y-m-d H:i:s');
            }
            else {
                return Ext4.util.Format.date(d, 'Y-m-d');
            }
        }
        else {
            return d;
        }
    },

    formatNumeric: function(val) {
        if (LABKEY.vis.isValid(val)) {
            if (val > 100000 || val < -100000) {
                return val.toExponential(3);
            }
            return parseFloat(val.toFixed(3));
        }
        return "N/A";
    },

    getReportConfig: function() {
        var config = { chartType: this.chartType };

        if (this.startDate) {
            config['StartDate'] = this.formatDate(this.startDate);
        }
        if (this.endDate) {
            config['EndDate'] = this.formatDate(this.endDate);
        }

        return config;
    },

    setSequenceMinMax: function(dataObject, row) {
        // track the min and max data so we can get the range for including the QC annotations
        if (LABKEY.vis.isValid(row['Value']))
        {
            if (dataObject.min == null || row['Value'] < dataObject.min) {
                dataObject.min = row['Value'];
            }
            if (dataObject.max == null || row['Value'] > dataObject.max) {
                dataObject.max = row['Value'];
            }

            var mean = row['Mean'];
            var sd = LABKEY.vis.isValid(row['StandardDev']) ? row['StandardDev'] : 0;
            if (LABKEY.vis.isValid(mean))
            {
                var minSd = (mean - (3 * sd));
                if (this.yAxisScale == 'log' && minSd <= 0)
                {
                    // Avoid setting our scale to be negative based on the three standard deviations to avoid messing up log plots
                    dataObject.showLogWarning = true;
                    for (var i = 2; i >= 0; i--)
                    {
                        minSd = (mean - (i * sd));
                        if (minSd > 0) {
                            break;
                        }
                    }
                }
                if (dataObject.min == null || minSd < dataObject.min) {
                    dataObject.min = minSd;
                }

                if (dataObject.max == null || (mean + (3 * sd)) > dataObject.max) {
                    dataObject.max = (mean + (3 * sd));
                }
            }
        }
    },

    failureHandler: function(response) {
        if (response.message) {
            Ext4.get(this.trendDiv).update("<span>" + response.message +"</span>");
        }
        else {
            Ext4.get(this.trendDiv).update("<span class='labkey-error'>Error: " + response.exception + "</span>");
        }

        Ext4.get(this.trendDiv).unmask();
    },

    applyGraphFilterBtnClick: function() {
        // make sure that at least one filter field is not null
        if (this.startDateField.getRawValue() == '' && this.endDateField.getRawValue() == '')
        {
            Ext4.Msg.show({
                title:'ERROR',
                msg: 'Please enter a value for filtering.',
                buttons: Ext4.Msg.OK,
                icon: Ext4.MessageBox.ERROR
            });
        }
        // verify that the start date is not after the end date
        else if (this.startDateField.getValue() > this.endDateField.getValue() && this.endDateField.getValue() != '')
        {
            Ext4.Msg.show({
                title:'ERROR',
                msg: 'Please enter an end date that does not occur before the start date.',
                buttons: Ext4.Msg.OK,
                icon: Ext4.MessageBox.ERROR
            });
        }
        else
        {
            // get date values without the time zone info
            this.startDate = this.startDateField.getRawValue();
            this.endDate = this.endDateField.getRawValue();

            this.displayTrendPlot();
        }
    },

    createGuideSetBtnClick: function() {
        var minGuideSetPointCount = 5; // to warn user if less than this many points are selected for the new guide set

        if (this.plotBrushSelection && this.plotBrushSelection.points.length > 0)
        {
            var startDate = this.plotBrushSelection.points[0]['fullDate'];
            var endDate = this.plotBrushSelection.points[this.plotBrushSelection.points.length - 1]['fullDate'];

            if (this.plotBrushSelection.points.length < minGuideSetPointCount) {
                Ext4.Msg.show({
                    title:'Create Guide Set Warning',
                    icon: Ext4.MessageBox.WARNING,
                    msg: 'Fewer than ' + minGuideSetPointCount + ' data points were selected for the new guide set, which may not be statistically significant. Would you like to proceed anyway?',
                    buttons: Ext4.Msg.YESNO,
                    scope: this,
                    fn: function(btnId, text, opt){
                        if(btnId == 'yes'){
                            this.insertNewGuideSet(startDate, endDate);
                        }
                    }
                });
            }
            else {
                this.insertNewGuideSet(startDate, endDate);
            }
        }
    },

    insertNewGuideSet : function(startDate, endDate) {
        LABKEY.Query.insertRows({
            schemaName: 'targetedms',
            queryName: 'GuideSet',
            rows: [{TrainingStart: startDate, TrainingEnd: endDate}],
            success: function(data) {
                this.plotBrushSelection = undefined;
                this.setBrushingEnabled(false);
                this.displayTrendPlot();
            },
            failure: function(response) {
                Ext4.Msg.show({
                    title:'Error Creating Guide Set',
                    icon: Ext4.MessageBox.ERROR,
                    msg: response.exception,
                    buttons: Ext4.Msg.OK
                });
            },
            scope: this
        })
    }
});
