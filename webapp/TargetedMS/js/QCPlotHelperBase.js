Ext4.define("LABKEY.targetedms.QCPlotHelperBase", {
    getGuideSetData : function() {
        var config = this.getReportConfig();
        var metricProps = this.getMetricPropsById(this.metric);

        var guideSetSql = "SELECT s.*, g.Comment FROM ("
                + this.metricGuideSetSql(metricProps.series1SchemaName, metricProps.series1QueryName, metricProps.series2SchemaName, metricProps.series2QueryName) + ") s"
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
                metricProps = this.getMetricPropsById(this.metric);

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
            var schema = metricProps[type + 'SchemaName'],
                    query = metricProps[type + 'QueryName'];

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
        var metricProps = this.getMetricPropsById(this.metric);

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
                data['value_' + seriesType + 'Title'] = metricProps[seriesType + 'Label'];
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
            chartType: this.metric,
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
                metricProps = this.getMetricPropsById(this.metric),
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
                            value: metricProps.series1Label,
                            visibility: this.isMultiSeries() ? undefined : 'hidden',
                            color: this.isMultiSeries() ? this.getColorRange()[0] : undefined
                        },
                        yRight: {
                            value: this.isMultiSeries() ? metricProps.series2Label : undefined,
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
        if (Ext4.isNumber(this.metric))
        {
            var metricProps = this.getMetricPropsById(this.metric);
            return Ext4.isDefined(metricProps.series2SchemaName) && Ext4.isDefined(metricProps.series2QueryName);
        }
        return false;
    },

    getColorRange: function()
    {
        return LABKEY.vis.Scale.ColorDiscrete().concat(LABKEY.vis.Scale.DarkColorDiscrete());
    },

    addCombinedPeptideSinglePlot : function() {
        var metricProps = this.getMetricPropsById(this.metric),
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
            if (metricProps.series1Label.length > lengthOfLongestLegend)
                lengthOfLongestLegend = metricProps.series1Label.length;

            proteomicsLegend.push({
                text: metricProps.series1Label,
                separator: true
            });

            ionLegend.push({
                text: metricProps.series1Label,
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
            if (metricProps.series2Label.length > lengthOfLongestLegend)
                lengthOfLongestLegend = metricProps.series2Label.length;


            proteomicsLegend.push({
                text: metricProps.series2Label,
                separator: true
            });

            ionLegend.push({
                text: metricProps.series2Label,
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
                    value: metricProps.series1Label,
                    visibility: this.isMultiSeries() ? undefined : 'hidden'
                },
                yRight: {
                    value: this.isMultiSeries() ? metricProps.series2Label : undefined,
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
    }
});