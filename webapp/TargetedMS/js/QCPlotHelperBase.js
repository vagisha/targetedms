Ext4.define("LABKEY.targetedms.QCPlotHelperBase", {

    showLJPlot: function()
    {
        return this.isPlotTypeSelected('Levey-Jennings');
    },

    showMovingRangePlot: function()
    {
        return this.isPlotTypeSelected('Moving Range');
    },

    showMeanCUSUMPlot: function()
    {
        return this.isPlotTypeSelected('CUSUMm');
    },

    showVariableCUSUMPlot: function()
    {
        return this.isPlotTypeSelected('CUSUMv');
    },

    isPlotTypeSelected: function(plotType)
    {
        return this.plotTypes.indexOf(plotType) > -1;
    },

    getGuideSetData : function(useRaw) {
        var config = this.getReportConfig();
        var metricProps = this.getMetricPropsById(this.metric);

        var guideSetSql = "SELECT s.*, g.Comment FROM (";
        if (useRaw) {
            guideSetSql += this.metricGuideSetRawSql(metricProps.series1SchemaName, metricProps.series1QueryName, metricProps.series2SchemaName, metricProps.series2QueryName);
        }
        else {
            guideSetSql += this.metricGuideSetSql(metricProps.series1SchemaName, metricProps.series1QueryName, metricProps.series2SchemaName, metricProps.series2QueryName);
        }
            guideSetSql +=  ") s"
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

        var sqlConfig = {
            schemaName: 'targetedms',
            sql: guideSetSql,
            scope: this,
            success: useRaw? this.processRawGuideSetData : this.processLJGuideSetData,
            failure: this.failureHandler
        };
        if (!useRaw)
            sqlConfig.sort = 'TrainingStart,SeriesLabel';
        LABKEY.Query.executeSql(sqlConfig);
    },

    getLJGuideSetData : function() {
        this.getGuideSetData(false);
    },

    getRawGuideSetData : function() {
        this.getGuideSetData(true);
    },

    processLJGuideSetData : function(data)
    {
        this.guideSetDataMap = {};
        Ext4.each(data.rows, function(row) {
            var guideSetId = row['GuideSetId'];
            if (!this.guideSetDataMap[guideSetId])
            {
                this.guideSetDataMap[guideSetId] = this.getGuideSetDataObj(row);
                this.hasGuideSetData = true;
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

        if (this.showMovingRangePlot())
            this.getRawGuideSetData();
        else
            this.getPlotData();
    },

    getGuideSetDataObj : function(row)
    {
        var guideSet = {
            ReferenceEnd: row['ReferenceEnd'],
            TrainingEnd: row['TrainingEnd'],
            TrainingStart: row['TrainingStart'],
            Comment: row['Comment'],
            Series: {}
        };
        return guideSet;
    },

    processRawGuideSetData : function(data)
    {
        var guideSetAvgMRs = this.getGuideSetAvgMRs(data);
        if (!this.guideSetDataMap)
            this.guideSetDataMap = {};
        Ext4.each(data.rows, function(row) {
            var guideSetId = row['GuideSetId'];
            if (!this.guideSetDataMap[guideSetId])
            {
                this.guideSetDataMap[guideSetId] = this.getGuideSetDataObj(row);
                this.hasGuideSetData = true;
            }

            var seriesLabel = row['SeriesLabel'];
            if (!this.guideSetDataMap[guideSetId].Series[seriesLabel])
            {
                this.guideSetDataMap[guideSetId].Series[seriesLabel] = {
                    MeanMR: guideSetAvgMRs[guideSetId].Series[seriesLabel]
                };
            }
            else {
                this.guideSetDataMap[guideSetId].Series[seriesLabel].MeanMR = guideSetAvgMRs[guideSetId].Series[seriesLabel];
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
                    + "\nORDER BY X.SeriesLabel, SeriesType, X.AcquiredTime"; //it's important that record is sorted by AcquiredTime asc as ordering is critical in calculating mR and CUSUM

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
        this.processedPlotData = this.preprocessPlotData(this.showLJPlot(), this.showMovingRangePlot(), this.showMeanCUSUMPlot(), this.showVariableCUSUMPlot());

        // process the data to shape it for the JS LeveyJenningsPlot API call
        this.fragmentPlotData = {};
        Ext4.iterate(this.processedPlotData, function(seriesLabel, seriesVal)
        {
            var fragment = seriesLabel;
            Ext4.iterate(seriesVal.Series, function (seriesTypeKey, seriesTypeObj)
            {
                var seriesType = seriesTypeKey;

                Ext4.each(seriesTypeObj.Rows, function(row)
                {
                    var data = this.processPlotDataRow(row, fragment, seriesType, metricProps);
                    this.fragmentPlotData[fragment].data.push(data);

                    this.setSeriesMinMax(this.fragmentPlotData[fragment], data);

                }, this);
            }, this);
        }, this);

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

    getCombinedPlotLegendData: function(metricProps, groupColors, yAxisCount, plotType, isCUSUMMean)
    {
        var newLegendData = Ext4.Array.clone(this.legendData),
                proteomicsLegend = [{ //Temp holder for proteomics legend labels
                    text: 'Peptides',
                    separator: true
                }],
                ionLegend = [{ //Temp holder for small molecule legend labels
                    text: 'Ions',
                    separator: true
                }],
                precursorInfo;

        //Add series1 separator to Legend sections
        if (this.isMultiSeries()) {
            proteomicsLegend.push({
                text: metricProps.series1Label,
                separator: true
            });

            ionLegend.push({
                text: metricProps.series1Label,
                separator: true
            });
        }

        var legendSeries = this.getCombinedPlotLegendSeries(plotType, isCUSUMMean);
        // traverse the precursor list for: calculating the longest legend string and combine the plot data
        for (var i = 0; i < this.precursors.length; i++)
        {
            precursorInfo = this.fragmentPlotData[this.precursors[i]];

            var appropriateLegend = precursorInfo.dataType == 'Peptide' ?  proteomicsLegend : ionLegend;

            appropriateLegend.push({
                name: precursorInfo.fragment + (this.isMultiSeries() ? '|' + legendSeries[0] : ''),
                text: precursorInfo.fragment,
                color: groupColors[i % groupColors.length]
            });
        }

        // add the fragment name for each group to the legend again for the series2 axis metric series
        if (this.isMultiSeries()) {
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
                    name: precursorInfo.fragment + '|' + legendSeries[1],
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
        return newLegendData;
    },

    addEachCombinedPrecusorPlot: function(combinePlotData, groupColors, yAxisCount, metricProps, showLogInvalid, lengthOfLongestLegend, plotType, isCUSUMMean)
    {
        var plotLegendData = this.getCombinedPlotLegendData(metricProps, groupColors, yAxisCount, plotType, isCUSUMMean);
        var plotTypeIdLabel = plotType == LABKEY.vis.TrendingLinePlotType.LeveyJennings ? '' : plotType  + (isCUSUMMean === undefined ? '' : isCUSUMMean ? 'm' : 'v');
        var id = 'combinedPlot' + plotTypeIdLabel;
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

        Ext4.apply(ljProperties, this.getPlotTypeProperties(combinePlotData, plotType, isCUSUMMean));

        var basePlotConfig = this.getBasePlotConfig(id, combinePlotData.data, plotLegendData);
        var plotConfig = Ext4.apply(basePlotConfig, {
            margins : {
                top: 65 + this.getMaxStackedAnnotations() * 12,
                right: 11 * lengthOfLongestLegend + (this.isMultiSeries() ? 50 : 0),
                left: 75,
                bottom: 75
            },
            labels : {
                main: {
                    value: LABKEY.targetedms.QCPlotHelperWrapper.getQCPlotTypeLabel(plotType, isCUSUMMean) + ' Plot'
                },
                subtitle: {
                    value: "All Series",
                    color: '#555555',
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

        plotConfig.qcPlotType = plotType;
        var plot = LABKEY.vis.TrendingLinePlot(plotConfig);
        plot.render();

        this.addAnnotationsToPlot(plot, combinePlotData);

        this.addGuideSetTrainingRangeToPlot(plot, combinePlotData);

        this.createExportToPDFButton(id, "Export combined " + plotType  + (isCUSUMMean === undefined ? '' : isCUSUMMean ? 'm' : 'v') + " plot for  All Series", "QC Combined Plot");

    },

    addEachIndividualPrecusorPlot: function(i, precursorInfo, metricProps, plotType, isCUSUMMean, scope)
    {
        var plotTypeIdLabel = plotType == LABKEY.vis.TrendingLinePlotType.LeveyJennings ? '' : plotType  + (isCUSUMMean === undefined ? '' : isCUSUMMean ? 'm' : 'v');
        var id = this.plotDivId + "-precursorPlot" + plotTypeIdLabel + i;
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
            position: this.groupedX ? 'jitter' : undefined,
            disableRangeDisplay: this.isMultiSeries() || !this.hasGuideSetData
        };

        Ext4.apply(ljProperties, this.getPlotTypeProperties(precursorInfo, plotType, isCUSUMMean));

        var basePlotConfig = this.getBasePlotConfig(id, precursorInfo.data, this.legendData);
        var plotConfig = Ext4.apply(basePlotConfig, {
            margins : {
                top: 65 + this.getMaxStackedAnnotations() * 12,
                left: 75,
                bottom: 75
            },
            labels : {
                main: {
                    value: LABKEY.targetedms.QCPlotHelperWrapper.getQCPlotTypeLabel(plotType, isCUSUMMean) + ' Plot'
                },
                subtitle: {
                    value: this.precursors[i],
                    color: '#555555',
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
                    scope.plotBrushStartEvent(plot);
                },
                brush: function(event, data, extent, plot, layerSelections) {
                    scope.plotBrushEvent(extent, plot, layerSelections);
                },
                brushend: function(event, data, extent, plot, layerSelections) {
                    scope.plotBrushEndEvent(data[0], extent, plot);
                },
                brushclear: function(event, data, plot, layerSelections) {
                    scope.plotBrushClearEvent(data[0], plot);
                }
            }
        });

        // create plot using the JS Vis API
        plotConfig.qcPlotType = plotType;
        var plot = LABKEY.vis.TrendingLinePlot(plotConfig);
        plot.render();

        this.addAnnotationsToPlot(plot, precursorInfo);

        this.addGuideSetTrainingRangeToPlot(plot, precursorInfo);

        this.createExportPlotToPDFButton(id, "Export " + plotType  + (isCUSUMMean === undefined ? '' : isCUSUMMean ? 'm' : 'v') + " plot for " + precursorInfo.fragment, "QC Plot-"+precursorInfo.fragment, 0); //TODO plotTypeIndex not used yet

    }
});