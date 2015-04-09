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
    plotWidth: null,

    initComponent : function() {
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
                    this.displayTrendPlot();
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
            store: Ext4.create('Ext.data.ArrayStore', {
                fields: ['value', 'display'],
                data: [
                    ['retentionTime', 'Retention Time']
                    , ['peakArea', 'Peak Area']
                    , ['fwhm', 'Full Width at Half Maximum (FWHM)']
                    , ['fwb', 'Full Width at Base (FWB)']
                    , ['ratio', 'Light/Heavy Ratio']
                ]
            }),
            valueField: 'value',
            displayField: 'display',
            value: 'retentionTime',
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

        // initialize the refesh graph button
        this.applyFilterButton = Ext4.create('Ext.button.Button', {
            disabled: true,
            text: 'Apply',
            handler: this.applyGraphFilter,
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
                    this.displayTrendPlot();
                }
            }
        });

        var tbspacer = {xtype: 'tbspacer'};

        var toolbar1 = Ext4.create('Ext.toolbar.Toolbar', {
            //height: 30,
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

        var toolbar2 = Ext4.create('Ext.toolbar.Toolbar', {
            //height: 30,
            ui: 'footer',
            layout: { pack: 'center' },
            padding: '0 10px 10px 10px',
            items: [
                this.scaleCombo, tbspacer,
                {xtype: 'tbseparator'}, tbspacer,
                this.groupedXCheckbox
            ]
        });

        this.items = [{ tbar: toolbar1 }, { tbar: toolbar2 }];

        this.callParent();

        this.displayTrendPlot();
    },

    displayTrendPlot: function() {
        Ext4.get(this.trendDiv).update("");
        Ext4.get(this.trendDiv).mask("Loading...");

        this.getDistinctPrecursors();
    },

    getDistinctPrecursors: function() {

        var sql = 'SELECT DISTINCT PrecursorId.ModifiedSequence AS Sequence FROM PrecursorChromInfo';
        var separator = ' WHERE ';
        // CAST as DATE to ignore time portion of value
        if (this.startDate)
        {
            sql += separator + "CAST(PeptideChromInfoId.SampleFileId.AcquiredTime AS DATE) >= '" + (this.startDate instanceof Date ? Ext4.util.Format.date(this.startDate, 'Y-m-d') : this.startDate) + "'";
            separator = " AND ";
        }
        if (this.endDate)
        {
            sql += separator + "CAST(PeptideChromInfoId.SampleFileId.AcquiredTime AS DATE) <= '" + (this.endDate instanceof Date ? Ext4.util.Format.date(this.endDate, 'Y-m-d') : this.endDate) + "'";
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

        this.getPlotData();
    },

    getPlotData: function() {
        var config = this.getReportConfig();

        // Filter on start/end dates, casting as DATE to ignore the time part
        var whereClause = " WHERE ";
        var separator = "";
        if (config.StartDate) {
            whereClause += separator + "CAST(PeptideChromInfoId.SampleFileId.AcquiredTime AS DATE) >= '" + config.StartDate + "'";
            separator = " AND ";
        }
        if (config.EndDate) {
            whereClause += separator + "CAST(PeptideChromInfoId.SampleFileId.AcquiredTime AS DATE) <= '" + config.EndDate + "'";
        }

        // Build query to get the values and mean/stdDev ranges for each data point. Default to retention time
        var sql = "";
        if (this.chartType == "peakArea") {
            sql = "SELECT * FROM (SELECT PrecursorId AS PrecursorId, Id AS PrecursorChromInfoId, PrecursorId.ModifiedSequence AS Sequence, PeptideChromInfoId.SampleFileId.AcquiredTime AS AcquiredTime, PeptideChromInfoId.SampleFileId.FilePath AS FilePath, TotalArea AS Value FROM precursorchrominfo" + whereClause + ") X"
                + " JOIN (SELECT PrecursorId.ModifiedSequence AS Sequence2, AVG(TotalArea) AS Mean, STDDEV(TotalArea) AS StandardDev FROM precursorchrominfo" + whereClause
                + " GROUP BY PrecursorId.ModifiedSequence) AS stats ON X.Sequence = stats.Sequence2";
        }
        else if (this.chartType == "fwhm") {
            sql = "SELECT * FROM (SELECT PrecursorId AS PrecursorId, Id AS PrecursorChromInfoId, PrecursorId.ModifiedSequence AS Sequence, PeptideChromInfoId.SampleFileId.AcquiredTime AS AcquiredTime, PeptideChromInfoId.SampleFileId.FilePath AS FilePath, MaxFWHM AS Value FROM precursorchrominfo" + whereClause + ") X"
                + " JOIN (SELECT PrecursorId.ModifiedSequence AS Sequence2, AVG(MaxFWHM) AS Mean, STDDEV(MaxFWHM) AS StandardDev FROM precursorchrominfo" + whereClause
                + " GROUP BY PrecursorId.ModifiedSequence) AS stats ON X.Sequence = stats.Sequence2";
        }
        else if (this.chartType == "fwb") {
            sql = "SELECT * FROM (SELECT PrecursorId AS PrecursorId, Id AS PrecursorChromInfoId, PrecursorId.ModifiedSequence AS Sequence, PeptideChromInfoId.SampleFileId.AcquiredTime AS AcquiredTime, PeptideChromInfoId.SampleFileId.FilePath AS FilePath, (MaxEndTime - MinStartTime) AS Value FROM precursorchrominfo" + whereClause + ") X"
                + " JOIN (SELECT PrecursorId.ModifiedSequence AS Sequence2, AVG((MaxEndTime - MinStartTime)) AS Mean, STDDEV((MaxEndTime - MinStartTime)) AS StandardDev FROM precursorchrominfo" + whereClause
                + " GROUP BY PrecursorId.ModifiedSequence) AS stats ON X.Sequence = stats.Sequence2";
        }
        else if (this.chartType == "ratio") {
            // Need to tweak the WHERE clause because we're selecting from the precursorarearatio table instead
            whereClause = " WHERE ";
            separator = "";
            if (config.StartDate) {
                whereClause += separator + "CAST(PrecursorChromInfoId.PeptideChromInfoId.SampleFileId.AcquiredTime AS DATE) >= '" + config.StartDate + "'";
                separator = " AND ";
            }
            if (config.EndDate) {
                whereClause += separator + "CAST(PrecursorChromInfoId.PeptideChromInfoId.SampleFileId.AcquiredTime AS DATE) <= '" + config.EndDate + "'";
            }

            sql = "SELECT * FROM (SELECT PrecursorChromInfoId.PrecursorId AS PrecursorId, PrecursorChromInfoId AS PrecursorChromInfoId, PrecursorChromInfoId.PrecursorId.ModifiedSequence AS Sequence, PrecursorChromInfoId.PeptideChromInfoId.SampleFileId.AcquiredTime AS AcquiredTime, PrecursorChromInfoId.PeptideChromInfoId.SampleFileId.FilePath AS FilePath, AreaRatio AS Value FROM precursorarearatio " + whereClause + ") X"
                + " JOIN (SELECT PrecursorChromInfoId.PrecursorId.ModifiedSequence AS Sequence2, AVG(AreaRatio) AS Mean, STDDEV(AreaRatio) AS StandardDev FROM precursorarearatio" + whereClause
                + " GROUP BY PrecursorChromInfoId.PrecursorId.ModifiedSequence) AS stats ON X.Sequence = stats.Sequence2";
        }
        else {
            sql = "SELECT * FROM (SELECT PrecursorId AS PrecursorId, Id AS PrecursorChromInfoId, PrecursorId.ModifiedSequence AS Sequence, PeptideChromInfoId.SampleFileId.AcquiredTime AS AcquiredTime, PeptideChromInfoId.SampleFileId.FilePath AS FilePath, BestRetentionTime AS Value FROM precursorchrominfo" + whereClause + ") X "
                + " JOIN (SELECT PrecursorId.ModifiedSequence AS Sequence2, AVG(BestRetentionTime) AS Mean, STDDEV(BestRetentionTime) AS StandardDev FROM precursorchrominfo" + whereClause
                + " GROUP BY PrecursorId.ModifiedSequence) AS stats ON X.Sequence = stats.Sequence2";
        }

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
                this.sequencePlotData[sequence] = {data: [], min: null, max: null};
            }

            this.sequencePlotData[sequence].data.push({
                AcquiredTime: row['AcquiredTime'], // keep in data for hover text display
                PrecursorId: row['PrecursorId'], // keep in data for click handler
                PrecursorChromInfoId: row['PrecursorChromInfoId'], // keep in data for click handler
                FilePath: row['FilePath'], // keep in data for hover text display
                fullDate: row['AcquiredTime'] ? this.formatDate(new Date(row['AcquiredTime']), true) : null,
                date: row['AcquiredTime'] ? this.formatDate(new Date(row['AcquiredTime'])) : null,
                value: row['Value'],
                mean: row['Mean'],
                stdDev: row['StandardDev']
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
                            datesToAdd.push({ fullDate: annDate, date: annDate }); // we don't need full date if grouping x-values
                        }
                    }
                    else {
                        if (precursorDates.indexOf(annFullDate) == -1 && Ext4.Array.pluck(datesToAdd, "fullDate").indexOf(annFullDate) == -1) {
                            datesToAdd.push({ fullDate: annFullDate, date: annDate });
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
        var maxStackedAnnotations = 0;
        if (this.annotationData.length > 0) {
            maxStackedAnnotations = Math.max.apply(Math, (Ext4.Array.pluck(this.annotationData, "yStepIndex"))) + 1;
        }

        var addedPlot = false;

        for (var i = 0; i < this.precursors.length; i++)
        {
            var precursorInfo = this.sequencePlotData[this.precursors[i]];

            // We don't necessarily have info for all possible precursors, depending on the filters and plot type
            if (precursorInfo)
            {
                addedPlot = true;

                if (this.plotWidth == null) {
                    // set the width of the plot webparts based on the first labkey-wp-body element (i.e. QC Summary webpart in this case)
                    this.plotWidth = 900;
                    var wp = document.querySelector('.labkey-wp-body');
                    if (wp && (wp.clientWidth - 20) > this.plotWidth) {
                        this.plotWidth = wp.clientWidth - 20;
                    }

                    Ext4.get(this.trendDiv).setWidth(this.plotWidth);
                }

                // add a new panel for each plot so we can add the title to the frame
                var id = "precursorPlot" + i;
                Ext4.get(this.trendDiv).insertHtml('beforeEnd', '<br/>' +
                        '<table class="labkey-wp qc-plot-wp">' +
                        ' <tr class="labkey-wp-header">' +
                        '     <th class="labkey-wp-title-left"><span class="labkey-wp-title-text qc-plot-wp-title">' + Ext4.util.Format.htmlEncode(this.precursors[i]) + '</span></th>' +
                        ' </tr><tr>' +
                        '     <td class="labkey-wp-body"><div id="' + id + '"></div></</td>' +
                        ' </tr>' +
                        '</table>'
                );

                if (precursorInfo.showLogWarning) {
                    Ext4.get(id).update("<span style='font-style: italic;'>For log scale, standard deviations below the mean with negative values have been omitted.</span>");
                }

                // create plot using the JS Vis API
                var plot = LABKEY.vis.LeveyJenningsPlot({
                    renderTo: id,
                    rendererType: 'd3',
                    width: this.plotWidth - 30,
                    height: 300,
                    data: precursorInfo.data,
                    properties: {
                        value: 'value',
                        mean: 'mean',
                        stdDev: 'stdDev',
                        topMargin: 10 + maxStackedAnnotations * 12,
                        xTick: this.groupedX ? 'date' : undefined,
                        xTickLabel: 'date',
                        yAxisDomain: [precursorInfo.min, precursorInfo.max],
                        yAxisScale: this.yAxisScale,
                        showTrendLine: true,
                        hoverTextFn: function(row){
                            return 'Acquired: ' + row['AcquiredTime'] + ", "
                                    + '\nValue: ' + row.value + ", "
                                    + '\nFile Path: ' + row['FilePath'];
                        },
                        pointClickFn: function(event, row) {
                            window.location = LABKEY.ActionURL.buildURL('targetedms', "precursorAllChromatogramsChart", LABKEY.ActionURL.getContainer(), { id: row.PrecursorId, chromInfoId: row.PrecursorChromInfoId }) + '#ChromInfo' + row.PrecursorChromInfoId;
                        }
                    },
                    gridLineColor: 'white',
                    legendData: this.legendData.length > 0 ? this.legendData : undefined
                });
                plot.render();

                this.addAnnotationsToPlot(plot, precursorInfo);
            }
        }

        if (!addedPlot)
        {
            Ext4.get(this.trendDiv).insertHtml('beforeEnd', '<div>No data to plot</div>');
        }

        Ext4.get(this.trendDiv).unmask();
    },

    addAnnotationsToPlot: function(plot, precursorInfo) {
        var me = this;

        var xAxisLabels = Ext4.Array.pluck(precursorInfo.data, (this.groupedX ? "date" : "fullDate"));
        if (this.groupedX) {
            xAxisLabels = Ext4.Array.unique(xAxisLabels);
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
        var annotations = d3.select('#' + plot.renderTo + ' svg').selectAll("path.annotation").data(this.annotationData)
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
                return Ext4.util.Format.date(d, 'Y-m-d H:i');
            }
            else {
                return Ext4.util.Format.date(d, 'Y-m-d');
            }
        }
        else {
            return d;
        }
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

    applyGraphFilter: function() {
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
    }
});
