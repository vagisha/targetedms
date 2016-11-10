Ext4.define("LABKEY.targetedms.MovingRangePlotHelper", {
    extend: 'LABKEY.targetedms.QCPlotHelperBase',
    setMovingRangeSeriesMinMax: function(dataObject, row) {
        // track the min and max data so we can get the range for including the QC annotations
        var val = row['MR'];
        if (LABKEY.vis.isValid(val))
        {
            if (dataObject.minMR == null || val < dataObject.minMR) {
                dataObject.minMR = val;
            }
            if (dataObject.maxMR == null || val > dataObject.maxMR) {
                dataObject.maxMR = val;
            }

            if (this.yAxisScale == 'log' && val <= 0)
            {
                dataObject.showLogInvalid = true;
            }

            var mean = row['meanMR'];
            if (LABKEY.vis.isValid(mean))
            {
                if (dataObject.maxMRMean == null || mean > dataObject.maxMRMean) {
                    dataObject.maxMRMean = mean;
                }
            }
        }
        else if (this.isMultiSeries())
        {
            // check if either of the y-axis metric values are invalid for a log scale
            var val1 = row['valueMR_series1'],
                    val2 = row['valueMR_series2'];
            if (dataObject.showLogInvalid == undefined && this.yAxisScale == 'log')
            {
                if ((LABKEY.vis.isValid(val1) && val1 <= 0) || (LABKEY.vis.isValid(val2) && val2 <= 0))
                {
                    dataObject.showLogInvalid = true;
                }
            }
        }
    },
    getMovingRangePlotTypeProperties: function(precursorInfo)
    {
        var plotProperties = {};
        // some properties are specific to whether or not we are showing multiple y-axis series
        if (this.isMultiSeries())
        {
            plotProperties['valueMR'] = 'MR_series1';
            plotProperties['valueRightMR'] = 'MR_series2';
        }
        else
        {
            plotProperties['valueMR'] = 'MR';
            plotProperties['meanMR'] = 'meanMR';
            var lower = Math.min(LABKEY.vis.Stat.MOVING_RANGE_LOWER_LIMIT, precursorInfo.minMR);
            var upper = Math.max(precursorInfo.maxMRMean * LABKEY.vis.Stat.MOVING_RANGE_UPPER_LIMIT_WEIGHT, precursorInfo.maxMR);
            plotProperties['yAxisDomain'] = [lower, upper];
        }
        return plotProperties;
    },
    getMRInitFragmentPlotData: function()
    {
        return {
            minMR: null,
            maxMR: null,
            maxMRMean: null
        }
    },
    processMRPlotDataRow: function(row, fragment, seriesType, metricProps)
    {
        var data = {};
        // if a guideSetId is defined for this row, include the guide set stats values in the data object
        if (Ext4.isDefined(row['GuideSetId']))
        {
            var gs = this.guideSetDataMap[row['GuideSetId']];
            if (Ext4.isDefined(gs) && gs.Series[fragment])
            {
                data['meanMR'] = gs.Series[fragment]['MeanMR'];
            }
        }

        if (this.isMultiSeries())
        {
            data['MR_' + seriesType] = row['MR'];
            data['MR_' + seriesType + 'Title'] = metricProps[seriesType + 'Label'];
        }
        else
        {
            data['MR'] = row['MR'];
        }
        return data;
    },

    processMRCombinedMinMax: function(combinePlotData, precursorInfo)
    {
        if (combinePlotData.minMR == null || combinePlotData.minMR > precursorInfo.minMR)
        {
            combinePlotData.minMR = precursorInfo.minMR;
        }
        if (combinePlotData.maxMR == null || combinePlotData.maxMR < precursorInfo.maxMR)
        {
            combinePlotData.maxMR = precursorInfo.maxMR;
        }
    },

    getMRCombinedPlotLegendSeries: function()
    {
        return ['MR_series1', 'MR_series2'];
    }
});