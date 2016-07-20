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

    extend: 'LABKEY.targetedms.BaseQCPlotPanel',
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
    chartType: null,
    dateRangeOffset: 0,
    minAcquiredTime: null,
    maxAcquiredTime: null,
    startDate: null,
    endDate: null,
    groupedX: false,
    singlePlot: false,
    plotWidth: null,
    enableBrushing: false,
    havePlotOptionsChanged: false,

    // Max number of plots/series to show
    maxCount: 50,

    initComponent : function() {
        Ext4.tip.QuickTipManager.init();

        this.callParent();

        // min and max acquired date must be provided
        if (this.minAcquiredTime == null || this.maxAcquiredTime == null)
            Ext4.get(this.plotDivId).update("<span class='labkey-error'>Unable to render report. Missing min and max AcquiredTime from data query.</span>");
        else
            this.queryInitialQcMetrics(this.queryInitialPlotOptions, this);
    },

    queryInitialPlotOptions : function()
    {
        // If there are URL parameters (i.e. from Pareto Plot click), set those as initial values as well.
        LABKEY.Ajax.request({
            url: LABKEY.ActionURL.buildURL('targetedms', 'leveyJenningsPlotOptions.api'),
            scope: this,
            success: LABKEY.Utils.getCallbackWrapper(function(response)
            {
                // convert the boolean and integer values from strings
                var initValues = {};
                Ext4.iterate(response.properties, function(key, value)
                {
                    if (value === "true" || value === "false")
                    {
                        value = value === "true";
                    }
                    else if (value != undefined && value.length > 0 && !isNaN(Number(value)))
                    {
                        value = +value;
                    }
                    initValues[key] = value;
                });

                // apply any URL parameters to the initial values
                Ext4.apply(initValues, this.getInitialValuesFromUrlParams());

                this.initPlotForm(initValues);
            }, this, false)
        });
    },

    calculateStartDateByOffset : function()
    {
        if (this.dateRangeOffset > 0)
        {
            var todayMinusOffset = new Date();
            todayMinusOffset.setDate(todayMinusOffset.getDate() - this.dateRangeOffset);
            return todayMinusOffset;
        }

        return this.minAcquiredTime;
    },

    calculateEndDateByOffset : function()
    {
        if (this.dateRangeOffset > 0)
            return new Date();

        return this.maxAcquiredTime;
    },

    initPlotForm : function(initValues)
    {
        // apply the initial values to the panel object so they are used in form field initialization
        Ext4.apply(this, initValues);

        // if we have a dateRangeOffset, we need to calculate the start and end date
        if (this.dateRangeOffset > -1)
        {
            this.startDate = this.formatDate(this.calculateStartDateByOffset());
            this.endDate = this.formatDate(this.calculateEndDateByOffset());
        }

        // initialize the form panel toolbars and display the plot
        this.add(this.initPlotFormToolbars());

        this.displayTrendPlot();
    },

    initPlotFormToolbars : function()
    {
        return [
            { tbar: this.getMainPlotOptionsToolbar() },
            { tbar: this.getCustomDateRangeToolbar() },
            { tbar: this.getOtherPlotOptionsToolbar() },
            { tbar: this.getGuideSetMessageToolbar() }
        ];
    },

    getMainPlotOptionsToolbar : function()
    {
        if (!this.mainPlotOptionsToolbar)
        {
            this.mainPlotOptionsToolbar = Ext4.create('Ext.toolbar.Toolbar', {
                ui: 'footer',
                cls: 'levey-jennings-toolbar',
                padding: 10,
                layout: { pack: 'center' },
                items: [
                    this.getChartTypeCombo(),
                    {xtype: 'tbspacer'}, {xtype: 'tbseparator'}, {xtype: 'tbspacer'},
                    this.getDateRangeCombo()
                ]
            });
        }

        return this.mainPlotOptionsToolbar;
    },

    getOtherPlotOptionsToolbar : function()
    {
        if (!this.otherPlotOptionsToolbar)
        {
            var  toolbarItems = [
                this.getScaleCombo(), {xtype: 'tbspacer'},
                {xtype: 'tbseparator'}, {xtype: 'tbspacer'},
                this.getGroupedXCheckbox(), {xtype: 'tbspacer'},
                {xtype: 'tbseparator'}, {xtype: 'tbspacer'},
                this.getSinglePlotCheckbox(), {xtype: 'tbspacer'}
            ];

            // only add the create guide set button if the user has the proper permissions to insert/update guide sets
            if (this.canUserEdit())
            {
                toolbarItems.push({xtype: 'tbspacer'}, {xtype: 'tbseparator'}, {xtype: 'tbspacer'});
                toolbarItems.push(this.getGuideSetCreateButton());
            }

            this.otherPlotOptionsToolbar = Ext4.create('Ext.toolbar.Toolbar', {
                ui: 'footer',
                cls: 'levey-jennings-toolbar',
                layout: { pack: 'center' },
                padding: '0 10px 10px 10px',
                items: toolbarItems
            });
        }

        return this.otherPlotOptionsToolbar;
    },

    getCustomDateRangeToolbar : function()
    {
        if (!this.customDateRangeToolbar)
        {
            this.customDateRangeToolbar = Ext4.create('Ext.toolbar.Toolbar', {
                ui: 'footer',
                cls: 'levey-jennings-toolbar',
                padding: '0 10px 10px 10px',
                hidden: this.dateRangeOffset > -1,
                layout: { pack: 'center' },
                items: [
                    this.getStartDateField(), {xtype: 'tbspacer'},
                    this.getEndDateField(), {xtype: 'tbspacer'},
                    this.getApplyDateRangeButton()
                ]
            });
        }

        return this.customDateRangeToolbar;
    },

    getGuideSetMessageToolbar : function()
    {
        if (!this.guideSetMessageToolbar)
        {
            this.guideSetMessageToolbar = Ext4.create('Ext.toolbar.Toolbar', {
                ui: 'footer',
                cls: 'guideset-toolbar-msg',
                hidden: true,
                layout: { pack: 'center' },
                items: [{
                    xtype: 'box',
                    itemId: 'GuideSetMessageToolBar',
                    html: 'Please click and drag in the plot to select the guide set training date range.'
                }]
            });
        }

        return this.guideSetMessageToolbar;
    },

    getInitialValuesFromUrlParams : function()
    {
        var urlParams = LABKEY.ActionURL.getParameters(),
            paramValues = {},
            alertMessage = '', sep = '',
            paramValue,
            chartType;

        paramValue = urlParams['metric'];
        if (paramValue != undefined)
        {
            chartType = this.validateChartTypeId(paramValue);
            if(chartType == null)
            {
                alertMessage += "Invalid Chart Type, reverting to default chart type.";
                sep = ' ';
            }
            else
            {
                paramValues['chartType'] = chartType;
            }
        }

        if (urlParams['startDate'] != undefined)
        {
            paramValue = new Date(urlParams['startDate']);
            if(paramValue == "Invalid Date")
            {
                alertMessage += sep + "Invalid Start Date, reverting to default start date.";
                sep = ' ';
            }
            else
            {
                paramValues['dateRangeOffset'] = -1; // force to custom date range selection
                paramValues['startDate'] = this.formatDate(paramValue);
            }
        }

        if (urlParams['endDate'] != undefined)
        {
            paramValue = new Date(urlParams['endDate']);
            if(paramValue == "Invalid Date")
            {
                alertMessage += sep + "Invalid End Date, reverting to default end date.";
            }
            else
            {
                paramValues['dateRangeOffset'] = -1; // force to custom date range selection
                paramValues['endDate'] = this.formatDate(paramValue);
            }
        }

        if (alertMessage.length > 0)
        {
            LABKEY.Utils.alert('Invalid URL Parameter(s)', alertMessage);
        }
        else if (Object.keys(paramValues).length > 0)
        {
            this.havePlotOptionsChanged = true;
            return paramValues;
        }

        return null;
    },

    validateChartTypeId : function(id)
    {
        for (var i = 0; i < this.chartTypePropArr.length; i++)
        {
            if (this.chartTypePropArr[i].id == id)
            {
                return this.chartTypePropArr[i].id;
            }
        }
        return null;
    },

    getScaleCombo : function()
    {
        if (!this.scaleCombo)
        {
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
                value: this.yAxisScale,
                forceSelection: true,
                editable: false,
                listeners: {
                    scope: this,
                    change: function(cmp, newVal, oldVal)
                    {
                        this.yAxisScale = newVal;
                        this.havePlotOptionsChanged = true;

                        // call processPlotData instead of renderPlots so that we recalculate min y-axis scale for log
                        this.setLoadingMsg();
                        this.processPlotData();
                    }
                }
            });
        }

        return this.scaleCombo;
    },

    getDateRangeCombo : function()
    {
        if (!this.dateRangeCombo)
        {
            this.dateRangeCombo = Ext4.create('Ext.form.field.ComboBox', {
                id: 'daterange-combo-box',
                width: 190,
                labelWidth: 75,
                fieldLabel: 'Date Range',
                triggerAction: 'all',
                mode: 'local',
                store: Ext4.create('Ext.data.ArrayStore', {
                    fields: ['value', 'display'],
                    data: [
                        [0, 'All dates'],
                        [7, 'Last 7 days'],
                        [15, 'Last 15 days'],
                        [30, 'Last 30 days'],
                        [90, 'Last 90 days'],
                        [180, 'Last 180 days'],
                        [365, 'Last 365 days'],
                        [-1, 'Custom range']
                    ]
                }),
                valueField: 'value',
                displayField: 'display',
                value: this.dateRangeOffset,
                forceSelection: true,
                editable: false,
                listeners: {
                    scope: this,
                    change: function(cmp, newVal, oldVal)
                    {
                        this.dateRangeOffset = newVal;
                        this.havePlotOptionsChanged = true;

                        var showCustomRangeItems = this.dateRangeOffset == -1;
                        this.getCustomDateRangeToolbar().setVisible(showCustomRangeItems);

                        if (!showCustomRangeItems)
                        {
                            // either use the min and max values based on the data
                            // or calculate range based on today's date and the offset
                            this.startDate = this.formatDate(this.calculateStartDateByOffset());
                            this.endDate = this.formatDate(this.calculateEndDateByOffset());

                            this.setBrushingEnabled(false);
                            this.displayTrendPlot();
                        }
                    }
                }
            });
        }

        return this.dateRangeCombo;
    },

    getStartDateField : function()
    {
        if (!this.startDateField)
        {
            this.startDateField = Ext4.create('Ext.form.field.Date', {
                id: 'start-date-field',
                width: 180,
                labelWidth: 65,
                fieldLabel: 'Start Date',
                value: this.startDate,
                allowBlank: false,
                format: 'Y-m-d',
                listeners: {
                    scope: this,
                    validitychange: function (df, isValid)
                    {
                        this.getApplyDateRangeButton().setDisabled(!isValid);
                    }
                }
            });
        }

        return this.startDateField;
    },

    getEndDateField : function()
    {
        if (!this.endDateField)
        {
            this.endDateField = Ext4.create('Ext.form.field.Date', {
                id: 'end-date-field',
                width: 175,
                labelWidth: 60,
                fieldLabel: 'End Date',
                value: this.endDate,
                allowBlank: false,
                format: 'Y-m-d',
                listeners: {
                    scope: this,
                    validitychange: function (df, isValid)
                    {
                        this.getApplyDateRangeButton().setDisabled(!isValid);
                    }
                }
            });
        }

        return this.endDateField;
    },

    getApplyDateRangeButton : function()
    {
        if (!this.applyFilterButton)
        {
            this.applyFilterButton = Ext4.create('Ext.button.Button', {
                text: 'Apply',
                handler: this.applyGraphFilterBtnClick,
                scope: this
            });
        }

        return this.applyFilterButton;
    },

    assignDefaultChartTypeIfNull: function ()
    {
        if (this.chartType == null || isNaN(Number(this.chartType)))
        {
            this.chartType = this.chartTypePropArr[0].id;
        }
        else if (!this.getChartTypePropsById(this.chartType))
        {
            console.log("The chart type with id " + this.chartType + " is no longer available.");
            this.chartType = this.chartTypePropArr[0].id;
            console.log("Using default chart type with id " + this.chartType + ".");
        }
    },

    getChartTypeCombo : function()
    {
        if (!this.chartTypeField)
        {
            this.assignDefaultChartTypeIfNull();
            
            this.chartTypeField = Ext4.create('Ext.form.field.ComboBox', {
                id: 'chart-type-field',
                width: 340,
                labelWidth: 70,
                fieldLabel: 'Chart Type',
                triggerAction: 'all',
                mode: 'local',
                store: Ext4.create('Ext.data.Store', {
                    fields: ['id', 'name'],
                    sorters: [{property: 'name'}],
                    data: this.chartTypePropArr
                }),
                valueField: 'id',
                displayField: 'name',
                value: this.chartType,
                forceSelection: true,
                editable: false,
                listeners: {
                    scope: this,
                    change: function(cmp, newVal, oldVal)
                    {
                        this.chartType = newVal;
                        this.havePlotOptionsChanged = true;

                        this.setBrushingEnabled(false);
                        this.displayTrendPlot();
                    }
                }
            });
        }

        return this.chartTypeField;
    },

    getGroupedXCheckbox : function()
    {
        if (!this.groupedXCheckbox)
        {
            this.groupedXCheckbox = Ext4.create('Ext.form.field.Checkbox', {
                id: 'grouped-x-field',
                boxLabel: 'Group X-Axis Values by Date',
                checked: this.groupedX,
                listeners: {
                    scope: this,
                    change: function(cb, newValue, oldValue)
                    {
                        this.groupedX = newValue;
                        this.havePlotOptionsChanged = true;

                        this.setBrushingEnabled(false);
                        this.setLoadingMsg();
                        this.getAnnotationData();
                    }
                }
            });
        }

        return this.groupedXCheckbox;
    },

    getSinglePlotCheckbox : function()
    {
        if (!this.peptidesInSinglePlotCheckbox)
        {
            this.peptidesInSinglePlotCheckbox = Ext4.create('Ext.form.field.Checkbox', {
                id: 'peptides-single-plot',
                boxLabel: 'Show All Series in a Single Plot',
                checked: this.singlePlot,
                listeners: {
                    scope: this,
                    change: function(cb, newValue, oldValue)
                    {
                        this.singlePlot = newValue;
                        this.havePlotOptionsChanged = true;

                        this.setBrushingEnabled(false);
                        this.setLoadingMsg();
                        this.getAnnotationData();
                    }
                }
            });
        }

        return this.peptidesInSinglePlotCheckbox;
    },

    getGuideSetCreateButton : function()
    {
        if (!this.createGuideSetToggleButton)
        {
            this.createGuideSetToggleButton = Ext4.create('Ext.button.Button', {
                text: 'Create Guide Set',
                tooltip: 'Enable/disable guide set creation mode',
                disabled: this.groupedX || this.singlePlot || this.isMultiSeries(),
                enableToggle: true,
                handler: function(btn) {
                    this.setBrushingEnabled(btn.pressed);
                },
                scope: this
            });
        }

        return this.createGuideSetToggleButton;
    },

    setBrushingEnabled : function(enabled) {
        // we don't currently allow creation of guide sets in single plot mode, grouped x-axis mode, or multi series mode
        this.getGuideSetCreateButton().setDisabled(this.groupedX || this.singlePlot || this.isMultiSeries());

        this.enableBrushing = enabled;
        this.clearPlotBrush();
        this.setPlotBrushingDisplayStyle();
        this.toggleGuideSetMsgDisplay();
        this.getGuideSetCreateButton().toggle(enabled);
    },

    setLoadingMsg : function() {
        Ext4.get(this.plotDivId).update("");
        Ext4.get(this.plotDivId).mask("Loading...");
    },

    displayTrendPlot: function() {
        this.setLoadingMsg();
        this.getDistinctPrecursors();
    },

    getChartTypePropsById: function(id) {
        for (var i = 0; i < this.chartTypePropArr.length; i++) {
            if (this.chartTypePropArr[i].id == id) {
                return this.chartTypePropArr[i];
            }
        }
        return {};
    },

    getDistinctPrecursors: function() {

        this.assignDefaultChartTypeIfNull();

        var chartTypeProps = this.getChartTypePropsById(this.chartType);

        var series1Sql = "SELECT SeriesLabel FROM " + chartTypeProps.series1SchemaName + "." + chartTypeProps.series1QueryName,
            series2Sql = this.isMultiSeries() ? " UNION SELECT SeriesLabel FROM " + chartTypeProps.series2SchemaName + "." + chartTypeProps.series2QueryName : '',
            separator = ' WHERE ';

        // CAST as DATE to ignore time portion of value
        if (this.startDate)
        {
            series1Sql += separator + "CAST(SampleFileId.AcquiredTime AS DATE) >= '" + this.startDate + "'";
            if (series2Sql.length > 0)
                series2Sql += separator + "CAST(SampleFileId.AcquiredTime AS DATE) >= '" + this.startDate + "'";

            separator = " AND ";
        }
        if (this.endDate)
        {
            series1Sql += separator + "CAST(SampleFileId.AcquiredTime AS DATE) <= '" + this.endDate + "'";
            if (series2Sql.length > 0)
                series2Sql += separator + "CAST(SampleFileId.AcquiredTime AS DATE) <= '" + this.endDate + "'";
        }

        var sql = "SELECT DISTINCT SeriesLabel FROM (\n" + series1Sql + series2Sql + "\n) X ORDER BY SeriesLabel";

        LABKEY.Query.executeSql({
            schemaName: 'targetedms',
            sql: sql,
            scope: this,
            success: function(data) {

                // stash the set of precursor series labels for use with the plot rendering
                this.precursors = [];
                if (data.rows.length > this.maxCount) {
                    Ext4.get(this.countLimitedDivId).update("Limiting display to the first " + this.maxCount + " precursors out of " + data.rows.length + " total");
                    Ext4.get(this.countLimitedDivId).setStyle("display", "block");
                }
                else {
                    Ext4.get(this.countLimitedDivId).update("");
                    Ext4.get(this.countLimitedDivId).setStyle("display", "none");
                }

                for (var i = 0; i < Math.min(data.rows.length, this.maxCount); i++) {
                    this.precursors.push(data.rows[i].SeriesLabel);
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

        // if we are showing the All Peptides plot, add a legend header for annotations
        if (this.annotationData.length > 0 && this.singlePlot)
        {
            this.legendData.push({
                text: 'Annotations',
                separator: true
            });
        }

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
        var chartTypeProps = this.getChartTypePropsById(this.chartType);

        var guideSetSql = "SELECT s.*, g.Comment FROM ("
                + this.metricGuideSetSql(chartTypeProps.series1SchemaName, chartTypeProps.series1QueryName, chartTypeProps.series2SchemaName, chartTypeProps.series2QueryName) + ") s"
                + " LEFT JOIN GuideSet g ON g.RowId = s.GuideSetId";

        // Filter on start/end dates from the QC plot form
        var separator = " WHERE ";
        if (config.StartDate) {
            guideSetSql += separator + "(s.ReferenceEnd >= '" + config.StartDate + "' OR s.ReferenceEnd IS NULL)";
            separator = " AND ";
        }
        if (config.EndDate) {
            guideSetSql += separator + "s.TrainingStart < TIMESTAMPADD('SQL_TSI_DAY', 1, CAST('" + config.EndDate + "' AS TIMESTAMP))";
        }

        LABKEY.Query.executeSql({
            schemaName: 'targetedms',
            sql: guideSetSql,
            sort: 'TrainingStart,SeriesLabel',
            scope: this,
            success: this.processGuideSetData,
            failure: this.failureHandler
        });
    },

    processGuideSetData : function(data)
    {
        this.guideSetDataMap = {};
        Ext4.each(data.rows, function(row) {
            var guideSetId = row['GuideSetId'];
            if (!this.guideSetDataMap[guideSetId])
            {
                this.guideSetDataMap[guideSetId] = {
                    ReferenceEnd: row['ReferenceEnd'],
                    TrainingEnd: row['TrainingEnd'],
                    TrainingStart: row['TrainingStart'],
                    Comment: row['Comment'],
                    Series: {}
                };
            }

            var seriesLabel = row['SeriesLabel'];
            if (!this.guideSetDataMap[guideSetId].Series[seriesLabel])
            {
                this.guideSetDataMap[guideSetId].Series[seriesLabel] = {
                    NumRecords: row['NumRecords'],
                    Mean: row['Mean'],
                    StandardDev: row['StandardDev']
                };
            }
        }, this);

        this.getPlotData();
    },

    getPlotData: function()
    {
        var config = this.getReportConfig(),
            chartTypeProps = this.getChartTypePropsById(this.chartType);

        // Filter on start/end dates, casting as DATE to ignore the time part
        var whereClause = " WHERE ", sep = "";
        if (config.StartDate) {
            whereClause += "CAST(SampleFileId.AcquiredTime AS DATE) >= '" + config.StartDate + "'";
            sep = " AND ";
        }
        if (config.EndDate) {
            whereClause += sep + "CAST(SampleFileId.AcquiredTime AS DATE) <= '" + config.EndDate + "'";
        }

        this.plotDataRows = [];
        var seriesTypes = this.isMultiSeries() ? ['series1', 'series2'] : ['series1'],
            seriesCount = 0;
        Ext4.each(seriesTypes, function(type)
        {
            var schema = chartTypeProps[type + 'SchemaName'],
                query = chartTypeProps[type + 'QueryName'];

            // Build query to get the metric values and related guide set info for this series
            var sql = "SELECT '" + type + "' AS SeriesType,"
                    + "\nX.PrecursorId, X.PrecursorChromInfoId, X.SeriesLabel, X.DataType, X.AcquiredTime,"
                    + "\nX.FilePath, X.MetricValue, gs.RowId AS GuideSetId,"
                    + "\nCASE WHEN (X.AcquiredTime >= gs.TrainingStart AND X.AcquiredTime <= gs.TrainingEnd) THEN TRUE ELSE FALSE END AS InGuideSetTrainingRange"
                    + "\nFROM (SELECT *, SampleFileId.AcquiredTime AS AcquiredTime, SampleFileId.FilePath AS FilePath"
                    + "\n      FROM " + schema + '.' + query + whereClause + ") X "
                    + "\nLEFT JOIN guideset gs"
                    + "\nON ((X.AcquiredTime >= gs.TrainingStart AND X.AcquiredTime < gs.ReferenceEnd) OR (X.AcquiredTime >= gs.TrainingStart AND gs.ReferenceEnd IS NULL))"
                    + "\nORDER BY X.SeriesLabel, X.AcquiredTime";

            LABKEY.Query.executeSql({
                schemaName: 'targetedms',
                sql: sql,
                scope: this,
                success: function(data) {
                    this.plotDataRows = this.plotDataRows.concat(data.rows);

                    seriesCount++;
                    if (seriesCount == seriesTypes.length)
                    {
                        this.processPlotData();
                    }
                },
                failure: this.failureHandler
            });

        }, this);
    },

    processPlotData: function() {
        var chartTypeProps = this.getChartTypePropsById(this.chartType);

        // process the data to shape it for the JS LeveyJenningsPlot API call
        this.fragmentPlotData = {};
        for (var i = 0; i < this.plotDataRows.length; i++)
        {
            var row = this.plotDataRows[i],
                seriesType = row['SeriesType'],
                fragment = row['SeriesLabel'],
                dataType = row['DataType'];

            if (!this.fragmentPlotData[fragment])
            {
                this.fragmentPlotData[fragment] = {
                    fragment: fragment,
                    dataType: dataType,
                    data: [],
                    min: null,
                    max: null
                };
            }

            var data = {
                type: 'data',
                fragment: fragment,
                PrecursorId: row['PrecursorId'], // keep in data for click handler
                PrecursorChromInfoId: row['PrecursorChromInfoId'], // keep in data for click handler
                FilePath: row['FilePath'], // keep in data for hover text display
                fullDate: row['AcquiredTime'] ? this.formatDate(new Date(row['AcquiredTime']), true) : null,
                date: row['AcquiredTime'] ? this.formatDate(new Date(row['AcquiredTime'])) : null,
                groupedXTick: row['AcquiredTime'] ? this.formatDate(new Date(row['AcquiredTime'])) : null,
                dataType: dataType //needed for plot point click handler
            };

            // if a guideSetId is defined for this row, include the guide set stats values in the data object
            if (Ext4.isDefined(row['GuideSetId']))
            {
                var gs = this.guideSetDataMap[row['GuideSetId']];
                if (Ext4.isDefined(gs) && gs.Series[fragment])
                {
                    data['mean'] = gs.Series[fragment]['Mean'];
                    data['stdDev'] = gs.Series[fragment]['StandardDev'];
                    data['guideSetId'] = row['GuideSetId'];
                    data['inGuideSetTrainingRange'] = row['InGuideSetTrainingRange'];
                    data['groupedXTick'] = data['groupedXTick'] + '|'
                            + (gs['TrainingStart'] ? gs['TrainingStart'] : '0') + '|'
                            + (row['InGuideSetTrainingRange'] ? 'include' : 'notinclude');
                }
            }

            if (this.isMultiSeries())
            {
                data['value_' + seriesType] = row['MetricValue'];
                data['value_' + seriesType + 'Title'] = chartTypeProps[seriesType + 'Label'];
            }
            else
            {
                data['value'] = row['MetricValue'];
            }

            this.fragmentPlotData[fragment].data.push(data);

            this.setSeriesMinMax(this.fragmentPlotData[fragment], data);
        }

        // merge in the annotation data to make room on the y axis
        for (var i = 0; i < this.precursors.length; i++)
        {
            var precursorInfo = this.fragmentPlotData[this.precursors[i]];

            // We don't necessarily have info for all possible precursors, depending on the filters and plot type
            if (precursorInfo)
            {
                // if the min and max are the same, or very close, increase the range
                if (precursorInfo.max == null && precursorInfo.min == null) {
                    precursorInfo.max = 1;
                    precursorInfo.min = 0;
                }
                else if (precursorInfo.max - precursorInfo.min < 0.0001) {
                    var factor = precursorInfo.max < 0.1 ? 0.1 : 1;
                    precursorInfo.max += factor;
                    precursorInfo.min -= factor;
                }

                // add any missing dates from the QC annotation data to the plot data
                var precursorDates = Ext4.Array.pluck(precursorInfo.data, (this.groupedX ? "date" : "fullDate"));
                var datesToAdd = [];
                for (var j = 0; j < this.annotationData.length; j++)
                {
                    var annFullDate = this.formatDate(new Date(this.annotationData[j].Date), true);
                    var annDate = this.formatDate(new Date(this.annotationData[j].Date));

                    var toAddAnnDate = precursorDates.indexOf(annDate) == -1 && Ext4.Array.pluck(datesToAdd, "date").indexOf(annDate) == -1;
                    var toAddFullAnnDate = precursorDates.indexOf(annFullDate) == -1 && Ext4.Array.pluck(datesToAdd, "fullDate").indexOf(annFullDate) == -1;

                    if ((this.groupedX && toAddAnnDate) || (!this.groupedX && toAddFullAnnDate))
                    {
                        datesToAdd.push({
                            type: 'annotation',
                            fullDate: annFullDate,
                            date: annDate,
                            groupedXTick: annDate
                        });
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

    renderPlots: function()
    {
        this.persistSelectedFormOptions();

        if (this.precursors.length == 0) {
            this.failureHandler({message: "There were no records found. The date filter applied may be too restrictive."});
            return;
        }

        this.setLoadingMsg();
        this.setPlotWidth(this.plotDivId);

        var addedPlot = false;
        if (this.singlePlot) {
            addedPlot = this.addCombinedPeptideSinglePlot();
        }
        else {
            addedPlot = this.addIndividualPrecursorPlots();
        }

        if (!addedPlot) {
            Ext4.get(this.plotDivId).insertHtml('beforeEnd', '<div>No data to plot</div>');
        }

        Ext4.get(this.plotDivId).unmask();
    },

    persistSelectedFormOptions : function()
    {
        if (this.havePlotOptionsChanged)
        {
            this.havePlotOptionsChanged = false;
            LABKEY.Ajax.request({
                url: LABKEY.ActionURL.buildURL('targetedms', 'leveyJenningsPlotOptions.api'),
                params: this.getSelectedPlotFormOptions()
            });
        }
    },

    getSelectedPlotFormOptions : function()
    {
        var props = {
            chartType: this.chartType,
            yAxisScale: this.yAxisScale,
            groupedX: this.groupedX,
            singlePlot: this.singlePlot,
            dateRangeOffset: this.dateRangeOffset
        };

        // set start and end date to null unless we are
        props.startDate = this.dateRangeOffset == -1 ? this.formatDate(this.startDate) : null;
        props.endDate = this.dateRangeOffset == -1 ? this.formatDate(this.endDate) : null;

        return props;
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

    addIndividualPrecursorPlots : function()
    {
        var addedPlot = false,
            chartTypeProps = this.getChartTypePropsById(this.chartType),
            me = this; // for plot brushing

        for (var i = 0; i < this.precursors.length; i++)
        {
            var precursorInfo = this.fragmentPlotData[this.precursors[i]];

            // We don't necessarily have info for all possible precursors, depending on the filters and plot type
            if (precursorInfo)
            {
                addedPlot = true;

                // add a new panel for each plot so we can add the title to the frame
                var id = this.plotDivId + "-precursorPlot" + i;
                this.addPlotWebPartToPlotDiv(id, this.precursors[i], this.plotDivId, 'qc-plot-wp');

                if (precursorInfo.showLogInvalid)
                {
                    this.showInvalidLogMsg(id, true);
                }
                else if (precursorInfo.showLogWarning)
                {
                    Ext4.get(id).update("<span style='font-style: italic;'>For log scale, standard deviations below "
                            + "the mean with negative values have been omitted.</span>");
                }

                var ljProperties = {
                    xTick: this.groupedX ? 'groupedXTick' : 'fullDate',
                    xTickLabel: 'date',
                    yAxisScale: (precursorInfo.showLogInvalid ? 'linear' : this.yAxisScale),
                    shape: 'guideSetId',
                    showTrendLine: true,
                    hoverTextFn: this.plotHoverTextDisplay,
                    pointClickFn: this.plotPointClick,
                    position: this.groupedX ? 'jitter' : undefined
                };
                // some properties are specific to whether or not we are showing multiple y-axis series
                if (this.isMultiSeries())
                {
                    ljProperties['disableRangeDisplay'] = true;
                    ljProperties['value'] = 'value_series1';
                    ljProperties['valueRight'] = 'value_series2';
                }
                else
                {
                    ljProperties['disableRangeDisplay'] = false;
                    ljProperties['value'] = 'value';
                    ljProperties['mean'] = 'mean';
                    ljProperties['stdDev'] = 'stdDev';
                    ljProperties['yAxisDomain'] = [precursorInfo.min, precursorInfo.max];
                }

                var basePlotConfig = this.getBasePlotConfig(id, precursorInfo.data, this.legendData);
                var plotConfig = Ext4.apply(basePlotConfig, {
                    margins : {
                        top: 45 + this.getMaxStackedAnnotations() * 12,
                        left: 75,
                        bottom: 75
                    },
                    labels : {
                        main: {
                            value: this.precursors[i],
                            visibility: 'hidden'
                        },
                        yLeft: {
                            value: chartTypeProps.series1Label,
                            visibility: this.isMultiSeries() ? undefined : 'hidden',
                            color: this.isMultiSeries() ? this.getColorRange()[0] : undefined
                        },
                        yRight: {
                            value: this.isMultiSeries() ? chartTypeProps.series2Label : undefined,
                            visibility: this.isMultiSeries() ? undefined : 'hidden',
                            color: this.isMultiSeries() ? this.getColorRange()[1] : undefined
                        }
                    },
                    properties: ljProperties,
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

                // create plot using the JS Vis API
                var plot = LABKEY.vis.LeveyJenningsPlot(plotConfig);
                plot.render();

                this.addAnnotationsToPlot(plot, precursorInfo);

                this.addGuideSetTrainingRangeToPlot(plot, precursorInfo);

                this.createExportToPDFButton(id, "QC Plot for fragment " + precursorInfo.fragment, "QC Plot-"+precursorInfo.fragment);
            }
        }

        this.setPlotBrushingDisplayStyle();

        return addedPlot;
    },

    isMultiSeries : function()
    {
        if (Ext4.isNumber(this.chartType))
        {
            var chartTypeProps = this.getChartTypePropsById(this.chartType);
            return Ext4.isDefined(chartTypeProps.series2SchemaName) && Ext4.isDefined(chartTypeProps.series2QueryName);
        }
        return false;
    },

    getColorRange: function()
    {
        return LABKEY.vis.Scale.ColorDiscrete().concat(LABKEY.vis.Scale.DarkColorDiscrete());
    },

    addCombinedPeptideSinglePlot : function() {
        var chartTypeProps = this.getChartTypePropsById(this.chartType),
            yAxisCount = this.isMultiSeries() ? 2 : 1, //Will only have a right if there is already a left y-axis
            groupColors = this.getColorRange(),
            newLegendData = Ext4.Array.clone(this.legendData),
            combinePlotData = {min: null, max: null, data: []},
            lengthOfLongestLegend = 1,
            proteomicsLegend = [{ //Temp holder for proteomics legend labels
                text: 'Peptides',
                separator: true
            }],
            ionLegend = [{ //Temp holder for small molecule legend labels
                text: 'Ions',
                separator: true
            }],
            showLogInvalid,
            precursorInfo;

        //Add series1 separator to Legend sections
        if (this.isMultiSeries()) {
            if (chartTypeProps.series1Label.length > lengthOfLongestLegend)
                lengthOfLongestLegend = chartTypeProps.series1Label.length;

            proteomicsLegend.push({
                text: chartTypeProps.series1Label,
                separator: true
            });

            ionLegend.push({
                text: chartTypeProps.series1Label,
                separator: true
            });
        }

        // traverse the precursor list for: calculating the longest legend string and combine the plot data
        for (var i = 0; i < this.precursors.length; i++)
        {
            precursorInfo = this.fragmentPlotData[this.precursors[i]];

            if (precursorInfo.fragment.length > lengthOfLongestLegend) {
                lengthOfLongestLegend = precursorInfo.fragment.length;
            }

            // for combined plot, concat all data together into a single array and track min/max for all
            combinePlotData.data = combinePlotData.data.concat(precursorInfo.data);
            if (combinePlotData.min == null || combinePlotData.min > precursorInfo.min) {
                combinePlotData.min = precursorInfo.min;
            }
            if (combinePlotData.max == null || combinePlotData.max < precursorInfo.max) {
                combinePlotData.max = precursorInfo.max;
            }

            showLogInvalid = showLogInvalid || precursorInfo.showLogInvalid;

            var appropriateLegend = precursorInfo.dataType == 'Peptide' ?  proteomicsLegend : ionLegend;

            appropriateLegend.push({
                name: precursorInfo.fragment + (this.isMultiSeries() ? '|value_series1' : ''),
                text: precursorInfo.fragment,
                color: groupColors[i % groupColors.length]
            });
        }

        // add the fragment name for each group to the legend again for the series2 axis metric series
        if (this.isMultiSeries()) {
            if (chartTypeProps.series2Label.length > lengthOfLongestLegend)
                lengthOfLongestLegend = chartTypeProps.series2Label.length;


            proteomicsLegend.push({
                text: chartTypeProps.series2Label,
                separator: true
            });

            ionLegend.push({
                text: chartTypeProps.series2Label,
                separator: true
            });

            for (var i = 0; i < this.precursors.length; i++)
            {
                var appropriateLegend = precursorInfo.dataType == 'Peptide' ?  proteomicsLegend : ionLegend;

                precursorInfo = this.fragmentPlotData[this.precursors[i]];
                appropriateLegend.push({
                    name: precursorInfo.fragment + '|value_series2',
                    text: precursorInfo.fragment,
                    color: groupColors[(this.precursors.length + i) % groupColors.length]
                });
            }
        }

        //Add legends if there is at least one non-separator label
        if (proteomicsLegend.length > yAxisCount + 1) {
            newLegendData = newLegendData.concat(proteomicsLegend);
        }

        if (ionLegend.length > yAxisCount + 1) {
            newLegendData = newLegendData.concat(ionLegend);
        }

        var id = 'combinedPlot';
        this.addPlotWebPartToPlotDiv(id, 'All Series', this.plotDivId, 'qc-plot-wp');
        this.showInvalidLogMsg(id, showLogInvalid);

        var ljProperties = {
            disableRangeDisplay: true,
            xTick: this.groupedX ? 'groupedXTick' : 'fullDate',
            xTickLabel: 'date',
            yAxisScale: (showLogInvalid ? 'linear' : this.yAxisScale),
            shape: 'guideSetId',
            groupBy: 'fragment',
            color: 'fragment',
            showTrendLine: true,
            hoverTextFn: this.plotHoverTextDisplay,
            pointClickFn: this.plotPointClick,
            position: this.groupedX ? 'jitter' : undefined
        };
        // some properties are specific to whether or not we are showing multiple y-axis series
        if (this.isMultiSeries())
        {
            ljProperties['value'] = 'value_series1';
            ljProperties['valueRight'] = 'value_series2';
        }
        else
        {
            ljProperties['value'] = 'value';
            ljProperties['mean'] = 'mean';
            ljProperties['stdDev'] = 'stdDev';
            ljProperties['yAxisDomain'] = [combinePlotData.min, combinePlotData.max];
        }

        var basePlotConfig = this.getBasePlotConfig(id, combinePlotData.data, newLegendData);
        var plotConfig = Ext4.apply(basePlotConfig, {
            margins : {
                top: 45 + this.getMaxStackedAnnotations() * 12,
                right: 11 * lengthOfLongestLegend + (this.isMultiSeries() ? 50 : 0),
                left: 75,
                bottom: 75
            },
            labels : {
                main: {
                    value: "All Series",
                    visibility: 'hidden'
                },
                yLeft: {
                    value: chartTypeProps.series1Label,
                    visibility: this.isMultiSeries() ? undefined : 'hidden'
                },
                yRight: {
                    value: this.isMultiSeries() ? chartTypeProps.series2Label : undefined,
                    visibility: this.isMultiSeries() ? undefined : 'hidden'
                }
            },
            properties: ljProperties
        });

        var plot = LABKEY.vis.LeveyJenningsPlot(plotConfig);
        plot.render();

        this.addAnnotationsToPlot(plot, combinePlotData);

        this.addGuideSetTrainingRangeToPlot(plot, combinePlotData);

        this.createExportToPDFButton(id, "QC Combined Plot for All Series", "QC Combined Plot");

        return true;
    },

    getExportSVGStr: function(btn)
    {
        var svgStr = this.callParent([btn]);

        // issue 25066: pdf export has artifact of the brush resize handlers
        svgStr = svgStr.replace('class="e-resize-handle-rect"', 'class="e-resize-handle-rect" visibility="hidden"');
        svgStr = svgStr.replace('class="w-resize-handle-rect"', 'class="w-resize-handle-rect" visibility="hidden"');

        return svgStr;
    },

    showInvalidLogMsg : function(id, toShow)
    {
        if (toShow)
        {
            Ext4.get(id).update("<span style='font-style: italic;'>Log scale invalid for values &le; 0. "
                    + "Reverting to linear y-axis scale.</span>");
        }
    },

    plotHoverTextDisplay : function(row, valueName){
        return (row[valueName + 'Title'] != undefined ? 'Metric: ' + row[valueName + 'Title'] + '\n' : '')
            + row.dataType + ': ' + row['fragment']
            + '\nAcquired: ' + row['fullDate'] + ", "
            + '\nValue: ' + (valueName ? row[valueName] : row.value) + ", "
            + '\nFile Path: ' + row['FilePath'];
    },

    plotPointClick : function(event, row) {
        //Chose action target based on precursor type
        var action = row.dataType == 'Peptide' ? "precursorAllChromatogramsChart" : "moleculePrecursorAllChromatogramsChart";

        var url = LABKEY.ActionURL.buildURL('targetedms', action, LABKEY.ActionURL.getContainer(), {
                    id: row.PrecursorId,
                    chromInfoId: row.PrecursorChromInfoId
                });

        window.location = url + '#ChromInfo' + row.PrecursorChromInfoId;
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

            var createBtn = this.createGuideSetSvgButton(plot, 'Create', xMid - 57, 50);
            createBtn.on('click', function() {
                me.createGuideSetBtnClick();
            });

            var cancelBtn = this.createGuideSetSvgButton(plot, 'Cancel', xMid + 3, 49);
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

    addGuideSetTrainingRangeToPlot : function(plot, precursorInfo) {
        var me = this;
        var guideSetTrainingData = [];

        // find the x-axis starting and ending index based on the guide set information attached to each data point
        Ext4.Object.each(this.guideSetDataMap, function(guideSetId, guideSetData)
        {
            // only compare guide set info for matching precursor fragment
            if (!this.singlePlot && guideSetData.Series[precursorInfo.fragment] == undefined) {
                return true; // continue
            }

            var gs = {GuideSetId: guideSetId};
            for (var j = 0; j < precursorInfo.data.length; j++)
            {
                // only use data points that match the GuideSet RowId and are in the training set range
                if (precursorInfo.data[j].guideSetId == gs.GuideSetId && precursorInfo.data[j].inGuideSetTrainingRange)
                {
                    if (gs.StartIndex == undefined)
                    {
                        gs.StartIndex = precursorInfo.data[j].seqValue;
                    }
                    gs.EndIndex = precursorInfo.data[j].seqValue;
                }
            }

            if (gs.StartIndex != undefined)
            {
                guideSetTrainingData.push(gs);
            }
        }, this);

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
                var guideSetInfo = me.guideSetDataMap[d.GuideSetId],
                    seriesGuideSetInfo = guideSetInfo.Series[precursorInfo.fragment],
                    numRecs = seriesGuideSetInfo ? seriesGuideSetInfo.NumRecords : 0,
                    showGuideSetStats = !me.singlePlot && numRecs > 0,
                    mean, stdDev, percentCV;

                if (showGuideSetStats)
                {
                    mean = me.formatNumeric(seriesGuideSetInfo.Mean);
                    stdDev = me.formatNumeric(seriesGuideSetInfo.StandardDev);
                    percentCV = me.formatNumeric((stdDev / mean) * 100);
                }

                return "Guide Set ID: " + d.GuideSetId + ","
                    + "\nStart: " + me.formatDate(new Date(guideSetInfo.TrainingStart), true)
                    + ",\nEnd: " + me.formatDate(new Date(guideSetInfo.TrainingEnd), true)
                    + (showGuideSetStats ? ",\n# Runs: " + numRecs : "")
                    + (showGuideSetStats ? ",\nMean: " + mean : "")
                    + (showGuideSetStats ? ",\nStd Dev: " + stdDev : "")
                    + (showGuideSetStats ? ",\n%CV: " + percentCV : "")
                    + (guideSetInfo.Comment ? (",\nComment: " + guideSetInfo.Comment) : "");
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

        var xAxisLabels = Ext4.Array.pluck(precursorInfo.data, "fullDate");
        if (this.groupedX)
        {
            xAxisLabels = [];

            // determine the annotation index based on the "date" but unique values are based on "groupedXTick"
            var prevGroupedXTick = null;
            Ext4.each(precursorInfo.data, function(row)
            {
                if (row['groupedXTick'] != prevGroupedXTick)
                {
                    xAxisLabels.push(row['date']);
                }
                prevGroupedXTick = row['groupedXTick'];
            });
        }

        // use direct D3 code to inject the annotation icons to the rendered SVG
        var xAcc = function(d) {
            var annotationDate = me.formatDate(new Date(d['Date']), !me.groupedX);
            return plot.scales.x.scale(xAxisLabels.indexOf(annotationDate));
        };
        var yAcc = function(d) {
            return plot.scales.yLeft.range[1] - (d['yStepIndex'] * 12) - 12;
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

    setSeriesMinMax: function(dataObject, row) {
        // track the min and max data so we can get the range for including the QC annotations
        var val = row['value'];
        if (LABKEY.vis.isValid(val))
        {
            if (dataObject.min == null || val < dataObject.min) {
                dataObject.min = val;
            }
            if (dataObject.max == null || val > dataObject.max) {
                dataObject.max = val;
            }

            if (this.yAxisScale == 'log' && val <= 0)
            {
                dataObject.showLogInvalid = true;
            }

            var mean = row['mean'];
            var sd = LABKEY.vis.isValid(row['stdDev']) ? row['stdDev'] : 0;
            if (LABKEY.vis.isValid(mean))
            {
                var minSd = (mean - (3 * sd));
                if (dataObject.showLogInvalid == undefined && this.yAxisScale == 'log' && minSd <= 0)
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
        else if (this.isMultiSeries())
        {
            // check if either of the y-axis metric values are invalid for a log scale
            var val1 = row['value_series1'],
                val2 = row['value_series2'];
            if (dataObject.showLogInvalid == undefined && this.yAxisScale == 'log')
            {
                if ((LABKEY.vis.isValid(val1) && val1 <= 0) || (LABKEY.vis.isValid(val2) && val2 <= 0))
                {
                    dataObject.showLogInvalid = true;
                }
            }
        }
    },

    applyGraphFilterBtnClick: function() {
        var startDateRawValue = this.getStartDateField().getRawValue(),
            startDateValue = this.getStartDateField().getValue(),
            endDateRawValue = this.getEndDateField().getRawValue(),
            endDateValue = this.getEndDateField().getValue();

        // make sure that at least one filter field is not null
        if (startDateRawValue == '' && endDateRawValue == '')
        {
            Ext4.Msg.show({
                title:'ERROR',
                msg: 'Please enter a value for filtering.',
                buttons: Ext4.Msg.OK,
                icon: Ext4.MessageBox.ERROR
            });
        }
        // verify that the start date is not after the end date
        else if (startDateValue > endDateValue && endDateValue != '')
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
            this.startDate = startDateRawValue;
            this.endDate = endDateRawValue;
            this.havePlotOptionsChanged = true;

            this.setBrushingEnabled(false);
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

                // issue 26019: since guide sets won't be created that often and we now remember plot option selections,
                // force page reload for new guide set creation this allows the sample file information to be updated
                // easily in the QC Summary webpart (which is commonly displayed on the same page as this plot).
                window.location.reload();
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
