/*
 * Copyright (c) 2011-2014 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext.namespace('LABKEY');

/**
 * Class to create a panel for displaying the R plot for the trending of retention times, peak areas, and other
 * values for the selected graph parameters.
 *
 * To add PDF export support use LABKEY.vis.SVGConverter.convert.
 */
LABKEY.LeveyJenningsTrendPlotPanel = Ext.extend(Ext.FormPanel, {
    constructor : function(config){
        // apply some Ext panel specific properties to the config
        Ext.apply(config, {
            items: [],
            header: false,
            labelAlign: 'left',
            width: 900,
            border: false,
            yAxisScale: 'linear',
            chartType: 'retentionTime'
        });

        LABKEY.LeveyJenningsTrendPlotPanel.superclass.constructor.call(this, config);
    },

    initComponent : function() {
        this.trendDiv = 'tiledPlotPanel';
        if (!this.startDate)
            this.startDate = null;
        if (!this.endDate)
            this.endDate = null;

        // initialize the y-axis scale combo for the top toolbar
        this.scaleLabel = new Ext.form.Label({text: 'Y-Axis Scale:'});
        this.scaleCombo = new Ext.form.ComboBox({
            id: 'scale-combo-box',
            width: 75,
            triggerAction: 'all',
            mode: 'local',
            store: new Ext.data.ArrayStore({
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
                'select': function(cmp, newVal, oldVal) {
                    this.yAxisScale = cmp.getValue();
                    this.displayTrendPlot();
                }
            }
        });

        // initialize the date range selection fields for the top toolbar
        this.startDateLabel = new Ext.form.Label({text: 'Start Date:'});
        this.startDateField = new Ext.form.DateField({
            id: 'start-date-field',
            value: this.startDate,
            format:  'Y-m-d',
            listeners: {
                scope: this,
                'valid': function (df) {
                    if (df.getValue() != '')
                        this.applyFilterButton.enable();
                },
                'invalid': function (df, msg) {
                    this.applyFilterButton.disable();
                }
            }
        });
        this.endDateLabel = new Ext.form.Label({text: 'End Date:'});
        this.endDateField = new Ext.form.DateField({
            id: 'end-date-field',
            value: this.endDate,
            format:  'Y-m-d',
            listeners: {
                scope: this,
                'valid': function (df) {
                    if (df.getValue() != '')
                        this.applyFilterButton.enable();
                },
                'invalid': function (df, msg) {
                    this.applyFilterButton.disable();
                }
            }
        });

        this.chartTypeLabel = new Ext.form.Label({text: 'Chart Type:'});
        this.chartTypeField = new Ext.form.ComboBox({
            id: 'chart-type-field',
            triggerAction: 'all',
            mode: 'local',
            store: new Ext.data.ArrayStore({
                fields: ['value', 'display'],
                data: [
                    ['retentionTime', 'Retention Time']
                    , ['peakArea', 'Peak Area']
                    , ['fwhm', 'Full Width at Half Maximum (FWHM)']
                    , ['fwb', 'Full Width at Base (FWB)']
                ]
            }),
            valueField: 'value',
            displayField: 'display',
            value: 'retentionTime',
            width: 270,
            forceSelection: true,
            editable: false,
            listeners: {
                scope: this,
                'select': function(cmp, newVal, oldVal) {
                    this.chartType = cmp.getValue();
                    this.displayTrendPlot();
                }
            }
        });

        // initialize the refesh graph button
        this.applyFilterButton = new Ext.Button({
            disabled: true,
            text: 'Apply',
            handler: this.applyGraphFilter,
            scope: this
        });

        var tbspacer = {xtype: 'tbspacer', width: 5};

        var items = [
            this.scaleLabel, tbspacer,
            this.scaleCombo, tbspacer,
            {xtype: 'tbseparator'}, tbspacer,
            this.startDateLabel, tbspacer,
            this.startDateField, tbspacer,
            this.endDateLabel, tbspacer,
            this.endDateField, tbspacer,
            this.applyFilterButton, tbspacer,
            {xtype: 'tbseparator'}, tbspacer,
            this.chartTypeLabel, tbspacer,
            this.chartTypeField
        ];

        this.tbar = new Ext.Toolbar({
            height: 30,
            buttonAlign: 'center',
            items: items
        });

        LABKEY.LeveyJenningsTrendPlotPanel.superclass.initComponent.call(this);

        this.displayTrendPlot();
    },

    displayTrendPlot: function() {
        Ext.get(this.trendDiv).update("");
        Ext.get(this.trendDiv).mask("Loading...");

        this.getDistinctPrecursors();
    },

    getDistinctPrecursors: function() {

        var sql = 'SELECT DISTINCT PrecursorId.ModifiedSequence AS Sequence FROM PrecursorChromInfo';
        var separator = ' WHERE ';
        // CAST as DATE to ignore time portion of value
        if (this.startDate)
        {
            sql += separator + "CAST(PeptideChromInfoId.SampleFileId.AcquiredTime AS DATE) >= '" + (this.startDate instanceof Date ? Ext.util.Format.date(this.startDate, 'Y-m-d') : this.startDate) + "'";
            separator = " AND ";
        }
        if (this.endDate)
        {
            sql += separator + "CAST(PeptideChromInfoId.SampleFileId.AcquiredTime AS DATE) <= '" + (this.endDate instanceof Date ? Ext.util.Format.date(this.endDate, 'Y-m-d') : this.endDate) + "'";
        }

        // Cap the peptide count at 50
        sql += " ORDER BY Sequence LIMIT 50";

        LABKEY.Query.executeSql({
            schemaName: 'targetedms',
            sql: sql,
            scope: this,
            success: function(data) {

                if (data.rows.length == 0) {
                    this.failureHandler({message: "there were no records found."});
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

        annotationSql += " ORDER BY qca.Date, qcat.Name";

        LABKEY.Query.executeSql({
            schemaName: 'targetedms',
            sql: annotationSql,
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
            // track if we need to stack annotations that fall on the same date
            var annotation = this.annotationData[i];
            if (!dateCount[annotation['Date']]) {
                dateCount[annotation['Date']] = 0;
            }
            annotation.yStepIndex = dateCount[annotation['Date']];
            dateCount[annotation['Date']]++;

            // get unique annotation names and colors for the legend
            if (Ext.pluck(this.legendData, "text").indexOf(annotation['Name']) == -1)
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
            sql = "SELECT * FROM (SELECT PrecursorId.ModifiedSequence AS Sequence, PeptideChromInfoId.SampleFileId.AcquiredTime AS AcquiredTime, PeptideChromInfoId.SampleFileId.FilePath AS FilePath, TotalArea AS Value FROM precursorchrominfo" + whereClause + ") X"
                + " JOIN (SELECT PrecursorId.ModifiedSequence AS Sequence2, AVG(TotalArea) AS Mean, STDDEV(TotalArea) AS StandardDev FROM precursorchrominfo" + whereClause
                + " GROUP BY PrecursorId.ModifiedSequence) AS stats ON X.Sequence = stats.Sequence2";
        }
        else if (this.chartType == "fwhm") {
            sql = "SELECT * FROM (SELECT PrecursorId.ModifiedSequence AS Sequence, PeptideChromInfoId.SampleFileId.AcquiredTime AS AcquiredTime, PeptideChromInfoId.SampleFileId.FilePath AS FilePath, MaxFWHM AS Value FROM precursorchrominfo" + whereClause + ") X"
                + " JOIN (SELECT PrecursorId.ModifiedSequence AS Sequence2, AVG(MaxFWHM) AS Mean, STDDEV(MaxFWHM) AS StandardDev FROM precursorchrominfo" + whereClause
                + " GROUP BY PrecursorId.ModifiedSequence) AS stats ON X.Sequence = stats.Sequence2";
        }
        else if (this.chartType == "fwb") {
            sql = "SELECT * FROM (SELECT PrecursorId.ModifiedSequence AS Sequence, PeptideChromInfoId.SampleFileId.AcquiredTime AS AcquiredTime, PeptideChromInfoId.SampleFileId.FilePath AS FilePath, (MaxEndTime - MinStartTime) AS Value FROM precursorchrominfo" + whereClause + ") X"
                + " JOIN (SELECT PrecursorId.ModifiedSequence AS Sequence2, AVG((MaxEndTime - MinStartTime)) AS Mean, STDDEV((MaxEndTime - MinStartTime)) AS StandardDev FROM precursorchrominfo" + whereClause
                + " GROUP BY PrecursorId.ModifiedSequence) AS stats ON X.Sequence = stats.Sequence2";
        }
        else {
            sql = "SELECT * FROM (SELECT PrecursorId.ModifiedSequence AS Sequence, PeptideChromInfoId.SampleFileId.AcquiredTime AS AcquiredTime, PeptideChromInfoId.SampleFileId.FilePath AS FilePath, BestRetentionTime AS Value FROM precursorchrominfo" + whereClause + ") X "
                + " JOIN (SELECT PrecursorId.ModifiedSequence AS Sequence2, AVG(BestRetentionTime) AS Mean, STDDEV(BestRetentionTime) AS StandardDev FROM precursorchrominfo" + whereClause
                + " GROUP BY PrecursorId.ModifiedSequence) AS stats ON X.Sequence = stats.Sequence2";
        }
        sql += " ORDER BY Sequence, AcquiredTime";

        LABKEY.Query.executeSql({
            schemaName: 'targetedms',
            sql: sql,
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
                label: row['AcquiredTime'] ? this.formatDate(new Date(row['AcquiredTime'])) : null,
                value: row['Value'],
                mean: row['Mean'],
                stdDev: row['StandardDev']
            });

            this.setSequenceMinMax(this.sequencePlotData[sequence], row);
        }

        // merge in the annotation data to make room on the x and y axis accordingly
        var maxStackedAnnotations = Math.max.apply(Math, (Ext.pluck(this.annotationData, "yStepIndex"))) + 1.35;
        for (var i = 0; i < this.precursors.length; i++)
        {
            var precursorInfo = this.sequencePlotData[this.precursors[i]];

            // adjust plot min to make room for rendering QC annotations
            if (this.annotationData.length > 0)
            {
                precursorInfo.ystep = (precursorInfo.max - precursorInfo.min) * 0.06;
                precursorInfo.plotMin = precursorInfo.min - (precursorInfo.ystep * maxStackedAnnotations);
            }

            // if the min and max are the same, or very close, increase the range
            if (precursorInfo.max - precursorInfo.plotMin < 0.0001)
            {
                precursorInfo.max += 1;
                precursorInfo.plotMin -= 1;
            }

            // add any missing dates from the QC annotation data to the plot data
            var precursorDates = Ext.pluck(precursorInfo.data, "label");
            var datesToAdd = [];
            for (var j = 0; j < this.annotationData.length; j++)
            {
                var annotationDate = this.formatDate(new Date(this.annotationData[j].Date));
                if (precursorDates.indexOf(annotationDate) == -1 && datesToAdd.indexOf(annotationDate) == -1)
                {
                    datesToAdd.push(annotationDate);
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
                        if (precursorInfo.data[l].label > datesToAdd[k])
                        {
                            precursorInfo.data.splice(l, 0, {label: datesToAdd[k]});
                            added = true;
                            index = l;
                            break;
                        }
                    }
                    // tack on any remaining dates to the end
                    if (!added) {
                        precursorInfo.data.push({label: datesToAdd[k]});
                    }
                }
            }
        }

        this.renderPlots();
    },

    renderPlots: function() {
        for (var i = 0; i < this.precursors.length; i++)
        {
            var precursorInfo = this.sequencePlotData[this.precursors[i]];

            // add a new panel for each plot so we can add the title to the frame
            var id = "precursorPlot" + i;
            Ext.get(this.trendDiv).insertHtml('beforeEnd', '<br/>' +
                '<table class="labkey-wp">' +
                ' <tr class="labkey-wp-header">' +
                '     <th class="labkey-wp-title-left"><span class="labkey-wp-title-text">' + Ext.util.Format.htmlEncode(this.precursors[i]) + '</span></th>' +
                ' </tr><tr>' +
                '     <td class="labkey-wp-body"><div id="' + id + '"></div></</td>' +
                ' </tr>' +
                '</table>'
            );

            // create plot using the JS Vis API
            var plot = LABKEY.vis.LeveyJenningsPlot({
                renderTo: id,
                rendererType: 'd3',
                width: 870,
                height: 300,
                data: precursorInfo.data,
                properties: {
                    value: 'value',
                    mean: 'mean',
                    stdDev: 'stdDev',
                    xTickLabel: 'label',
                    yAxisDomain: [precursorInfo.plotMin, precursorInfo.max],
                    yAxisScale: this.yAxisScale,
                    hoverTextFn: function(row){
                        return 'Acquired: ' + row['AcquiredTime'] + '\nValue: ' + row.value;
                    }
                },
                gridLineColor: 'white',
                legendData: this.legendData.length > 0 ? this.legendData : undefined
            });
            plot.render();

            this.addAnnotationsToPlot(plot, precursorInfo);
        }

        Ext.get(this.trendDiv).unmask();
    },

    addAnnotationsToPlot: function(plot, precursorInfo) {
        var me = this;

        // use direct D3 code to inject the annotation icons to the rendered SVG
        var xAxisLabels = Ext.pluck(precursorInfo.data, "label");
        var xAcc = function(d) {
            var annotationDate = me.formatDate(new Date(d['Date']));
            return plot.scales.x.scale(xAxisLabels.indexOf(annotationDate));
        };
        var yAcc = function(d) {
            return plot.scales.yLeft.scale(precursorInfo.plotMin + (precursorInfo.ystep * (d['yStepIndex']+0.5)));
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
                return "Created By: " + d['DisplayName']
                    + "\nCreated: " + me.formatDate(new Date(d['Created']))
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

        // add the same mouseover effects for the data points
        var points = d3.select('#' + plot.renderTo + ' svg').selectAll("a.point path");
        points.on("mouseover", function(){ return mouseOn(this, 5); });
        points.on("mouseout", function(){ return mouseOff(this); });
    },

    formatDate: function(d) {
        if (d instanceof Date) {
            return Ext.util.Format.date(d, 'Y-m-d');
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
            if (LABKEY.vis.isValid(mean)) {
                if (dataObject.min == null || (mean-(3*sd)) < dataObject.min) {
                    dataObject.min = (mean-(3*sd));
                }
                if (dataObject.max == null || (mean+(3*sd)) > dataObject.max) {
                    dataObject.max = (mean+(3*sd));
                }
            }
        }
    },

    failureHandler: function(response) {
        Ext.get(this.trendDiv).update("Error: " + (response.exception || response.message));
        Ext.get(this.trendDiv).unmask();
    },

    applyGraphFilter: function() {
        // make sure that at least one filter field is not null
        if (this.startDateField.getRawValue() == '' && this.endDateField.getRawValue() == '')
        {
            Ext.Msg.show({
                title:'ERROR',
                msg: 'Please enter a value for filtering.',
                buttons: Ext.Msg.OK,
                icon: Ext.MessageBox.ERROR
            });
        }
        // verify that the start date is not after the end date
        else if (this.startDateField.getValue() > this.endDateField.getValue() && this.endDateField.getValue() != '')
        {
            Ext.Msg.show({
                title:'ERROR',
                msg: 'Please enter an end date that does not occur before the start date.',
                buttons: Ext.Msg.OK,
                icon: Ext.MessageBox.ERROR
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
