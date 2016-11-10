Ext4.define("LABKEY.targetedms.QCPlotHelperWrapper", {
    mixins: {
        leveyJennings: 'LABKEY.targetedms.LeveyJenningsPlotHelper',
        cusum: 'LABKEY.targetedms.CUSUMPlotHelper',
        movingRange: 'LABKEY.targetedms.MovingRangePlotHelper'
    },

    statics: {
        getQCPlotTypeLabel: function(visPlotType, isCUSUMMean)
        {
            if (visPlotType == LABKEY.vis.TrendingLinePlotType.MovingRange)
                return 'Moving Range';
            else if (visPlotType == LABKEY.vis.TrendingLinePlotType.CUSUM)
            {
                if (isCUSUMMean)
                    return 'CUSUMm';
                else
                    return 'CUSUMv';
            }
            else
                return 'Levey-Jennings';
        }
    },

    prepareAndRenderQCPlot : function(plotType) {
        if (this.showLJPlot())
            return this.getLJGuideSetData(plotType);
        return this.getRawGuideSetData();
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

                var id = this.plotDivId + "-precursorPlot" + i;
                var ids = [id];
                for (var j = 1; j < this.plotTypes.length; j++)
                {
                    ids.push(id + '_plotType_' + j);
                }
                this.addPlotsToPlotDiv(ids, this.precursors[i], this.plotDivId, 'qc-plot-wp');
                var plotIndex = 0;
                // add a new panel for each plot so we can add the title to the frame
                if (this.showLJPlot())
                {
                    this.addEachIndividualPrecusorPlot(ids[plotIndex++], i, precursorInfo, metricProps, LABKEY.vis.TrendingLinePlotType.LeveyJennings, undefined, me);
                }
                if (this.showMovingRangePlot())
                {
                    this.addEachIndividualPrecusorPlot(ids[plotIndex++], i, precursorInfo, metricProps, LABKEY.vis.TrendingLinePlotType.MovingRange, undefined, me);
                }
                if (this.showMeanCUSUMPlot())
                {
                    this.addEachIndividualPrecusorPlot(ids[plotIndex++], i, precursorInfo, metricProps, LABKEY.vis.TrendingLinePlotType.CUSUM, true, me);
                }
                if (this.showVariableCUSUMPlot())
                {
                    this.addEachIndividualPrecusorPlot(ids[plotIndex], i, precursorInfo, metricProps, LABKEY.vis.TrendingLinePlotType.CUSUM, false, me);
                }

            }
        }

        this.setPlotBrushingDisplayStyle();

        return addedPlot;
    },

    addCombinedPeptideSinglePlot : function() {
        var metricProps = this.getMetricPropsById(this.metric),
                yAxisCount = this.isMultiSeries() ? 2 : 1, //Will only have a right if there is already a left y-axis
                groupColors = this.getColorRange(),
                combinePlotData = this.getCombinedPlotInitData(),
                lengthOfLongestLegend = 1,
                showLogInvalid,
                precursorInfo;

        //Add series1 separator to Legend sections
        if (this.isMultiSeries()) {
            if (metricProps.series1Label.length > lengthOfLongestLegend)
                lengthOfLongestLegend = metricProps.series1Label.length;
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
            this.processCombinedPlotMinMax(combinePlotData, precursorInfo);

            showLogInvalid = showLogInvalid || precursorInfo.showLogInvalid;
        }

        if (this.isMultiSeries()) {
            if (metricProps.series2Label.length > lengthOfLongestLegend)
                lengthOfLongestLegend = metricProps.series2Label.length;
        }
        var id = 'combinedPlot';
        var ids = [id];
        for (var i = 1; i < this.plotTypes.length; i++)
        {
            ids.push(id + 'plotType' + i.toString());
        }
        this.addPlotsToPlotDiv(ids, 'All Series', this.plotDivId, 'qc-plot-wp');
        var plotIndex = 0;
        if (this.showLJPlot())
        {
            this.addEachCombinedPrecusorPlot(ids[plotIndex++], combinePlotData, groupColors, yAxisCount, metricProps, showLogInvalid, lengthOfLongestLegend, LABKEY.vis.TrendingLinePlotType.LeveyJennings);
        }
        if (this.showMovingRangePlot())
        {
            this.addEachCombinedPrecusorPlot(ids[plotIndex++], combinePlotData, groupColors, yAxisCount, metricProps, showLogInvalid, lengthOfLongestLegend, LABKEY.vis.TrendingLinePlotType.MovingRange);
        }
        if (this.showMeanCUSUMPlot())
        {
            this.addEachCombinedPrecusorPlot(ids[plotIndex++], combinePlotData, groupColors, yAxisCount, metricProps, showLogInvalid, lengthOfLongestLegend, LABKEY.vis.TrendingLinePlotType.CUSUM, true);
        }
        if (this.showVariableCUSUMPlot())
        {
            this.addEachCombinedPrecusorPlot(ids[plotIndex], combinePlotData, groupColors, yAxisCount, metricProps, showLogInvalid, lengthOfLongestLegend, LABKEY.vis.TrendingLinePlotType.CUSUM, false);
        }

        return true;
    },

    setSeriesMinMax: function(dataObject, row) {
        // track the min and max data so we can get the range for including the QC annotations
        if (this.showLJPlot)
            this.setLJSeriesMinMax(dataObject, row);
        if (this.showMovingRangePlot())
            this.setMovingRangeSeriesMinMax(dataObject, row);
        if (this.showMeanCUSUMPlot())
            this.setCUSUMSeriesMinMax(dataObject, row, true);
        if (this.showVariableCUSUMPlot())
            this.setCUSUMSeriesMinMax(dataObject, row, false);
    },

    getPlotTypeProperties: function(precursorInfo, plotType, isMean)
    {
        if (plotType == LABKEY.vis.TrendingLinePlotType.MovingRange)
            return this.getMovingRangePlotTypeProperties(precursorInfo);
        else if (plotType == LABKEY.vis.TrendingLinePlotType.CUSUM)
            return this.getCUSUMPlotTypeProperties(precursorInfo, isMean);
        else
            return this.getLJPlotTypeProperties(precursorInfo);
    },

    getInitFragmentPlotData: function(fragment, dataType)
    {
        var fragmentData = {
            fragment: fragment,
            dataType: dataType,
            data: []
        };

        Ext4.apply(fragmentData, this.getInitPlotMinMaxData());

        return fragmentData;
    },

    getInitPlotMinMaxData: function()
    {
        var plotData = {};
        if (this.showLJPlot())
        {
            Ext4.apply(plotData, this.getLJInitFragmentPlotData());
        }
        if (this.showMovingRangePlot())
        {
            Ext4.apply(plotData, this.getMRInitFragmentPlotData());
        }
        if (this.showMeanCUSUMPlot())
        {
            Ext4.apply(plotData, this.getCUSUMInitFragmentPlotData(true));
        }
        if (this.showVariableCUSUMPlot())
        {
            Ext4.apply(plotData, this.getCUSUMInitFragmentPlotData(false));
        }
        return plotData;
    },

    processPlotDataRow: function(row, fragment, seriesType, metricProps)
    {
        var dataType = row['DataType'];
        if (!this.fragmentPlotData[fragment])
        {
            this.fragmentPlotData[fragment] = this.getInitFragmentPlotData(fragment, dataType);
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
                data['guideSetId'] = row['GuideSetId'];
                data['inGuideSetTrainingRange'] = row['InGuideSetTrainingRange'];
                data['groupedXTick'] = data['groupedXTick'] + '|'
                        + (gs['TrainingStart'] ? gs['TrainingStart'] : '0') + '|'
                        + (row['InGuideSetTrainingRange'] ? 'include' : 'notinclude');
            }
        }

        if (this.showLJPlot())
        {
            Ext4.apply(data, this.processLJPlotDataRow(row, fragment, seriesType, metricProps));
        }
        if (this.showMovingRangePlot())
        {
            Ext4.apply(data, this.processMRPlotDataRow(row, fragment, seriesType, metricProps));
        }
        if (this.showMeanCUSUMPlot())
        {
            Ext4.apply(data, this.processCUSUMPlotDataRow(row, fragment, seriesType, metricProps, true));
        }
        if (this.showVariableCUSUMPlot())
        {
            Ext4.apply(data, this.processCUSUMPlotDataRow(row, fragment, seriesType, metricProps, false));
        }

        return data;
    },

    getCombinedPlotInitData: function()
    {
        var combinePlotData = {data: []};
        Ext4.apply(combinePlotData, this.getInitPlotMinMaxData());
        return combinePlotData
    },

    processCombinedPlotMinMax: function(combinePlotData, precursorInfo)
    {
        if (this.showLJPlot())
        {
            this.processLJCombinedMinMax(combinePlotData, precursorInfo);
        }
        if (this.showMovingRangePlot())
        {
            this.processMRCombinedMinMax(combinePlotData, precursorInfo);
        }
        if (this.showMeanCUSUMPlot())
        {
            this.processCUSUMCombinedMinMax(combinePlotData, precursorInfo, true);
        }
        if (this.showVariableCUSUMPlot())
        {
            this.processCUSUMCombinedMinMax(combinePlotData, precursorInfo, false);
        }
    },

    getCombinedPlotLegendSeries: function(plotType, isCUSUMMean)
    {
        if (plotType == LABKEY.vis.TrendingLinePlotType.MovingRange)
            return this.getMRCombinedPlotLegendSeries();
        else if (plotType == LABKEY.vis.TrendingLinePlotType.CUSUM)
            return this.getCUSUMCombinedPlotLegendSeries(isCUSUMMean);
        else
            return this.getLJCombinedPlotLegendSeries();
    }

});