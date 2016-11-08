Ext4.define("LABKEY.targetedms.QCPlotHelperWrapper", {
    mixins: {
        leveyJennings: 'LABKEY.targetedms.LeveyJenningsPlotHelper',
        cusum: 'LABKEY.targetedms.CUSUMPlotHelper',
        movingRange: 'LABKEY.targetedms.MovingRangePlotHelper'
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

                // add a new panel for each plot so we can add the title to the frame
                if (this.showLJPlot)
                {
                    this.addEachIndividualPrecusorPlot(i, precursorInfo, metricProps, LABKEY.vis.TrendingLinePlotType.LeveyJennings, undefined, me);

                }
                if (this.showMovingRangePlot())
                {
                    this.addEachIndividualPrecusorPlot(i, precursorInfo, metricProps, LABKEY.vis.TrendingLinePlotType.MovingRange, undefined, me);
                }
                if (this.showMeanCUSUMPlot())
                {
                    this.addEachIndividualPrecusorPlot(i, precursorInfo, metricProps, LABKEY.vis.TrendingLinePlotType.CUSUM, true, me);
                }
                if (this.showVariableCUSUMPlot())
                {
                    this.addEachIndividualPrecusorPlot(i, precursorInfo, metricProps, LABKEY.vis.TrendingLinePlotType.CUSUM, false, me);
                }

            }
        }

        this.setPlotBrushingDisplayStyle();

        return addedPlot;
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
        var plotProperties = {};
        // some properties are specific to whether or not we are showing multiple y-axis series
        if (this.isMultiSeries())
        {
            plotProperties['disableRangeDisplay'] = true;
        }
        else
        {
            plotProperties['disableRangeDisplay'] = !this.hasGuideSetData;
        }

        if (plotType == LABKEY.vis.TrendingLinePlotType.MovingRange)
            return Ext4.apply(plotProperties, this.getMovingRangePlotTypeProperties(precursorInfo));
        else if (plotType == LABKEY.vis.TrendingLinePlotType.CUSUM)
            return Ext4.apply(plotProperties, this.getCUSUMPlotTypeProperties(precursorInfo, isMean));
        else
            return Ext4.apply(plotProperties, this.getLJPlotTypeProperties(precursorInfo));
    },

    getInitFragmentPlotData: function(fragment, dataType)
    {
        var fragmentData = {
            fragment: fragment,
            dataType: dataType,
            data: []
        };

        if (this.showLJPlot())
        {
            Ext4.apply(fragmentData, this.getLJInitFragmentPlotData());
        }
        if (this.showMovingRangePlot())
        {
            Ext4.apply(fragmentData, this.getMRInitFragmentPlotData());
        }
        if (this.showMeanCUSUMPlot())
        {
            Ext4.apply(fragmentData, this.getCUSUMInitFragmentPlotData(true));
        }
        if (this.showVariableCUSUMPlot())
        {
            Ext4.apply(fragmentData, this.getCUSUMInitFragmentPlotData(false));
        }
        return fragmentData;
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
    }
});