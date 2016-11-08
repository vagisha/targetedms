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

                var plotTypeIndex = 0;
                if (this.showLJPlot)
                {
                    this.addEachIndividualPrecusorPlot(id, i, precursorInfo, metricProps, LABKEY.vis.TrendingLinePlotType.LeveyJennings, me);
                    this.createExportPlotToPDFButton(id, "Export Levey-Jennings plot for " + precursorInfo.fragment, "QC Plot-"+precursorInfo.fragment, plotTypeIndex++);
                }
                if (this.showMovingRangePlot())
                {
                    this.addEachIndividualPrecusorPlot(id, i, precursorInfo, metricProps, LABKEY.vis.TrendingLinePlotType.MovingRange, me);
                    this.createExportPlotToPDFButton(id, "Export Moving Range plot for " + precursorInfo.fragment, "QC Plot-"+precursorInfo.fragment, plotTypeIndex++);
                }
                if (this.showMeanCUSUMPlot())
                {
                    this.addEachIndividualPrecusorPlot(id, i, precursorInfo, metricProps, LABKEY.vis.TrendingLinePlotType.CUSUM, me);
                    this.createExportPlotToPDFButton(id, "Export Mean CUSUM plot for " + precursorInfo.fragment, "QC Plot-"+precursorInfo.fragment, plotTypeIndex++);
                }
                if (this.showVariableCUSUMPlot()) //TODO separate out mean and variable
                {
                    this.addEachIndividualPrecusorPlot(id, i, precursorInfo, metricProps, LABKEY.vis.TrendingLinePlotType.CUSUM, me);
                    this.createExportPlotToPDFButton(id, "Export Variable CUSUM plot for " + precursorInfo.fragment, "QC Plot-"+precursorInfo.fragment, plotTypeIndex++);
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
    }
});