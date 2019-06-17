/*
 * Copyright (c) 2016-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
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

    prepareAndRenderQCPlot : function() {
        if (this.showLJPlot())
            return this.getLJGuideSetData();
        return this.getRawGuideSetData(this.showMovingRangePlot());
    },

    addIndividualPrecursorPlots : function()
    {
        var addedPlot = false,
                metricProps = this.getMetricPropsById(this.metric),
                me = this; // for plot brushing

        this.longestLegendText = 0;

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
                this.addPlotsToPlotDiv(ids, this.precursors[i] + ", " + precursorInfo.mz, this.plotDivId, 'qc-plot-wp');
                var plotIndex = 0;
                // add a new panel for each plot so we can add the title to the frame
                if (this.showLJPlot())
                {
                    this.addEachIndividualPrecusorPlot(plotIndex, ids[plotIndex++], i, precursorInfo, metricProps, LABKEY.vis.TrendingLinePlotType.LeveyJennings, undefined, me);
                }
                if (this.showMovingRangePlot())
                {
                    this.addEachIndividualPrecusorPlot(plotIndex, ids[plotIndex++], i, precursorInfo, metricProps, LABKEY.vis.TrendingLinePlotType.MovingRange, undefined, me);
                }
                if (this.showMeanCUSUMPlot())
                {
                    this.addEachIndividualPrecusorPlot(plotIndex, ids[plotIndex++], i, precursorInfo, metricProps, LABKEY.vis.TrendingLinePlotType.CUSUM, true, me);
                }
                if (this.showVariableCUSUMPlot())
                {
                    this.addEachIndividualPrecusorPlot(plotIndex, ids[plotIndex], i, precursorInfo, metricProps, LABKEY.vis.TrendingLinePlotType.CUSUM, false, me);
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
                lengthOfLongestLegend = 8,  // At least length of label 'Peptides'
                lengthOfLongestAnnot = 1,
                showLogInvalid,
                precursorInfo,
                prefix, ellipCount, prefLength, ellipMatch = new RegExp(this.legendHelper.ellipsis, 'g');

        //Add series1 separator to Legend sections
        if (this.isMultiSeries()) {
            if (metricProps.series1Label.length > lengthOfLongestLegend)
                lengthOfLongestLegend = metricProps.series1Label.length;
        }

        // traverse the precursor list for: calculating the longest legend string and combine the plot data
        for (var i = 0; i < this.precursors.length; i++)
        {
            precursorInfo = this.fragmentPlotData[this.precursors[i]];
            prefix = this.legendHelper.getLegendItemText(precursorInfo);
            ellipCount = prefix.match(ellipMatch) ? prefix.match(ellipMatch).length : 0;
            prefLength = prefix.length + ellipCount;  // ellipsis count for two chars

            if (prefLength > lengthOfLongestLegend) {
                lengthOfLongestLegend = prefLength;
            }

            // for combined plot, concat all data together into a single array and track min/max for all
            combinePlotData.data = combinePlotData.data.concat(precursorInfo.data);
            this.processCombinedPlotMinMax(combinePlotData, precursorInfo);

            showLogInvalid = showLogInvalid || precursorInfo.showLogInvalid;
        }

        // Annotations
        Ext4.each(this.legendData, function(entry) {
            if (entry.text.length > lengthOfLongestLegend) {
                lengthOfLongestAnnot = entry.text.length;
            }
        }, this);

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
        var legendMargin = 14 * lengthOfLongestLegend + (this.isMultiSeries() ? 50 : 0) + 20;
        var annotMargin = 11 * lengthOfLongestAnnot;

        if( annotMargin > legendMargin) {
            legendMargin = annotMargin;  // Give some extra space if annotations defined
        }

        // Annotations can still push legend too far so cap this
        if (legendMargin > 300)
            legendMargin = 300;

        if (this.showLJPlot())
        {
            this.addEachCombinedPrecusorPlot(plotIndex, ids[plotIndex++], combinePlotData, groupColors, yAxisCount, metricProps, showLogInvalid, legendMargin, LABKEY.vis.TrendingLinePlotType.LeveyJennings);
        }
        if (this.showMovingRangePlot())
        {
            this.addEachCombinedPrecusorPlot(plotIndex, ids[plotIndex++], combinePlotData, groupColors, yAxisCount, metricProps, showLogInvalid, legendMargin, LABKEY.vis.TrendingLinePlotType.MovingRange);
        }
        if (this.showMeanCUSUMPlot())
        {
            this.addEachCombinedPrecusorPlot(plotIndex, ids[plotIndex++], combinePlotData, groupColors, yAxisCount, metricProps, showLogInvalid, legendMargin, LABKEY.vis.TrendingLinePlotType.CUSUM, true);
        }
        if (this.showVariableCUSUMPlot())
        {
            this.addEachCombinedPrecusorPlot(plotIndex, ids[plotIndex], combinePlotData, groupColors, yAxisCount, metricProps, showLogInvalid, legendMargin, LABKEY.vis.TrendingLinePlotType.CUSUM, false);
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

    getInitFragmentPlotData: function(fragment, dataType, mz)
    {
        var fragmentData = {
            fragment: fragment,
            dataType: dataType,
            data: [],
            mz: mz
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
        var mz = Ext4.util.Format.number(row['mz'], '0.0000');
        if (!this.fragmentPlotData[fragment])
        {
            this.fragmentPlotData[fragment] = this.getInitFragmentPlotData(fragment, dataType, mz);
        }

        var data = {
            type: 'data',
            fragment: fragment,
            mz: mz,
            ReplicateId: row['ReplicateId'], // keep in data for click handler
            PrecursorId: row['PrecursorId'], // keep in data for click handler
            PrecursorChromInfoId: row['PrecursorChromInfoId'], // keep in data for click handler
            FilePath: row['FilePath'], // keep in data for hover text display
            IgnoreInQC: row['IgnoreInQC'], // keep in data for hover text display
            fullDate: row['AcquiredTime'] ? this.formatDate(Ext4.Date.parse(row['AcquiredTime'], LABKEY.Utils.getDateTimeFormatWithMS()), true) : null,
            date: row['AcquiredTime'] ? this.formatDate(Ext4.Date.parse(row['AcquiredTime'], LABKEY.Utils.getDateTimeFormatWithMS())) : null,
            groupedXTick: row['AcquiredTime'] ? this.formatDate(Ext4.Date.parse(row['AcquiredTime'], LABKEY.Utils.getDateTimeFormatWithMS())) : null,
            dataType: dataType, //needed for plot point click handler
            SeriesType: row['SeriesType']
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
    },

    getAdditionalPlotLegend: function(plotType)
    {
        if (plotType === LABKEY.vis.TrendingLinePlotType.CUSUM)
            return this.getCUSUMGroupLegend();
        if (plotType === LABKEY.vis.TrendingLinePlotType.MovingRange)
            return this.getMRLegend();
        if (plotType === LABKEY.vis.TrendingLinePlotType.LeveyJennings)
            return this.getLJLegend();
        if (this.showMeanCUSUMPlot() || this.showVariableCUSUMPlot())
            return this.getEmptyLegend();
        return [];
    },

    getPlotTypeHelpTooltip: function(plotTypeName)
    {
        if (plotTypeName == 'Levey-Jennings')
            return LABKEY.targetedms.LeveyJenningsPlotHelper.tooltips['Levey-Jennings'];
        else if (plotTypeName == 'Moving Range')
            return LABKEY.targetedms.MovingRangePlotHelper.tooltips['Moving Range'];
        else if (plotTypeName == 'CUSUMm')
            return LABKEY.targetedms.CUSUMPlotHelper.tooltips['CUSUMm'];
        else if (plotTypeName == 'CUSUMv')
            return LABKEY.targetedms.CUSUMPlotHelper.tooltips['CUSUMv'];
        return '';
    }
});