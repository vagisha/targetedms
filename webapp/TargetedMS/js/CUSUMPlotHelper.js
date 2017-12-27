/*
 * Copyright (c) 2016-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define("LABKEY.targetedms.CUSUMPlotHelper", {
    extend: 'LABKEY.targetedms.QCPlotHelperBase',
    statics: {
        tooltips: {
            'CUSUMm' : 'A CUSUM plot is a time-weighted control plot that displays the cumulative sums of the deviations of each sample value from the target value.' +
            ' CUSUMm (mean CUSUM) plots two types of CUSUM statistics: one for positive mean shifts and the other for negative mean shifts.',
            'CUSUMv' : 'A CUSUM plot is a time-weighted control plot that displays the cumulative sums of the deviations of each sample value from the target value. ' +
            'CUSUMv (variability or scale CUSUM) plots two types of CUSUM statistics: one for positive variability shifts and the other for negative variability shifts. ' +
            'Variability is a transformed standardized normal quantity which is sensitive to variability changes.'
        }
    },
    setCUSUMSeriesMinMax: function(dataObject, row, isCUSUMmean) {
        // track the min and max data so we can get the range for including the QC annotations
        var negative = 'CUSUMmN', positive = 'CUSUMmP';
        if (!isCUSUMmean)
        {
            negative = 'CUSUMvN'; positive = 'CUSUMvP';
        }
        var maxNegative = 'max' + negative, maxPositive = 'max' + positive, minNegative = 'min' + negative, minPositive = 'min' + positive;
        var valNegative = row[negative], valPositive = row[positive];
        if (LABKEY.vis.isValid(valNegative) && LABKEY.vis.isValid(valPositive))
        {
            if (this.yAxisScale == 'log' && (valNegative <= 0 || valPositive <= 0))
            {
                dataObject.showLogEpsilonWarning = true;
            }

            if (dataObject[minNegative] == null || valNegative < dataObject[minNegative]) {
                dataObject[minNegative] = valNegative;
            }
            if (dataObject[maxNegative] == null || valNegative > dataObject[maxNegative]) {
                dataObject[maxNegative] = valNegative;
            }

            if (dataObject[minPositive] == null || valPositive < dataObject[minPositive]) {
                dataObject[minPositive] = valPositive;
            }
            if (dataObject[maxPositive] == null || valPositive > dataObject[maxPositive]) {
                dataObject[maxPositive] = valPositive;
            }
        }
    },

    getCUSUMPlotTypeProperties: function(precursorInfo, isMean)
    {
        var plotProperties = {};
        // some properties are specific to whether or not we are showing multiple y-axis series
        if (this.isMultiSeries())
        {
            if (isMean)
            {
                plotProperties['positiveValue'] = 'CUSUMmP_series1';
                plotProperties['positiveValueRight'] = 'CUSUMmP_series2';
                plotProperties['negativeValue'] = 'CUSUMmN_series1';
                plotProperties['negativeValueRight'] = 'CUSUMmN_series2';

            }
            else
            {
                plotProperties['positiveValue'] = 'CUSUMvP_series1';
                plotProperties['positiveValueRight'] = 'CUSUMvP_series2';
                plotProperties['negativeValue'] = 'CUSUMvN_series1';
                plotProperties['negativeValueRight'] = 'CUSUMvN_series2';

            }

        }
        else
        {
            var lower, upper;
            if (isMean)
            {
                plotProperties['positiveValue'] = 'CUSUMmP';
                plotProperties['negativeValue'] = 'CUSUMmN';
                lower = Math.min(LABKEY.vis.Stat.CUSUM_CONTROL_LIMIT_LOWER, precursorInfo.minCUSUMmP, precursorInfo.minCUSUMmN);
                upper = Math.max(LABKEY.vis.Stat.CUSUM_CONTROL_LIMIT, precursorInfo.maxCUSUMmP, precursorInfo.maxCUSUMmN);
            }
            else
            {
                plotProperties['positiveValue'] = 'CUSUMvP';
                plotProperties['negativeValue'] = 'CUSUMvN';
                lower = Math.min(LABKEY.vis.Stat.CUSUM_CONTROL_LIMIT_LOWER, precursorInfo.minCUSUMvP, precursorInfo.minCUSUMvN);
                upper = Math.max(LABKEY.vis.Stat.CUSUM_CONTROL_LIMIT, precursorInfo.maxCUSUMvP, precursorInfo.maxCUSUMvN);
            }

            plotProperties['yAxisDomain'] = [lower, upper];

        }
        return plotProperties;
    },

    getCUSUMInitFragmentPlotData: function(isMeanCUSUM)
    {
        if (isMeanCUSUM)
        {
            return {
                minCUSUMmP: null,
                maxCUSUMmP: null,
                minCUSUMmN: null,
                maxCUSUMmN: null
            }
        }
        else {
            return {
                minCUSUMvP: null,
                maxCUSUMvP: null,
                minCUSUMvN: null,
                maxCUSUMvN: null
            }
        }
    },

    processCUSUMPlotDataRow: function(row, fragment, seriesType, metricProps, isMeanCUSUM)
    {
        var data = {};

        if (isMeanCUSUM)
        {
            if (this.isMultiSeries())
            {
                data['CUSUMmN_' + seriesType] = row['CUSUMmN'];
                data['CUSUMmN_' + seriesType + 'Title'] = metricProps[seriesType + 'Label'];
                data['CUSUMmP_' + seriesType] = row['CUSUMmP'];
                data['CUSUMmP_' + seriesType + 'Title'] = metricProps[seriesType + 'Label'];
            }
            else
            {
                data['CUSUMmN'] = row['CUSUMmN'];
                data['CUSUMmP'] = row['CUSUMmP'];
            }
        }
        else
        {
            if (this.isMultiSeries())
            {
                data['CUSUMvP_' + seriesType] = row['CUSUMvP'];
                data['CUSUMvP_' + seriesType + 'Title'] = metricProps[seriesType + 'Label'];
                data['CUSUMvN_' + seriesType] = row['CUSUMvN'];
                data['CUSUMvN_' + seriesType + 'Title'] = metricProps[seriesType + 'Label'];
            }
            else
            {
                data['CUSUMvP'] = row['CUSUMvP'];
                data['CUSUMvN'] = row['CUSUMvN'];
            }
        }
        return data;
    },

    processCUSUMCombinedMinMax: function(combinePlotData, precursorInfo, isMeanCUSUM)
    {
        var negative = 'CUSUMmN', positive = 'CUSUMmP';
        if (!isMeanCUSUM)
        {
            negative = 'CUSUMvN'; positive = 'CUSUMvP';
        }
        var maxNegative = 'max' + negative, maxPositive = 'max' + positive, minNegative = 'min' + negative, minPositive = 'min' + positive;
        var valNegativeMin = precursorInfo[minNegative], valPositiveMin = precursorInfo[minPositive];
        var varNegativeMax = precursorInfo[maxNegative], valPositiveMax = precursorInfo[maxPositive];
        if (combinePlotData[minNegative] == null || valNegativeMin < combinePlotData[minNegative])
        {
            combinePlotData[minNegative] = valNegativeMin;
        }
        if (combinePlotData[maxNegative] == null || varNegativeMax > combinePlotData[maxNegative])
        {
            combinePlotData[maxNegative] = varNegativeMax;
        }

        if (combinePlotData[minPositive] == null || valPositiveMin < combinePlotData[minPositive])
        {
            combinePlotData[minPositive] = valPositiveMin;
        }
        if (combinePlotData[maxPositive] == null || valPositiveMax > combinePlotData[maxPositive])
        {
            combinePlotData[maxPositive] = valPositiveMax;
        }

    },

    getCUSUMCombinedPlotLegendSeries: function(isMeanCUSUM)
    {
        //positive or negative will use the same color, special casing done in plot.js
        //normalizedGroup = group.replace('CUSUMmN', 'CUSUMm').replace('CUSUMmP', 'CUSUMm');
        //normalizedGroup = group.replace('CUSUMvN', 'CUSUMv').replace('CUSUMvP', 'CUSUMv');
        if (isMeanCUSUM)
            return ['CUSUMm_series1', 'CUSUMm_series2'];
        return ['CUSUMv_series1', 'CUSUMv_series2'];
    },

    getCUSUMGroupLegend: function()
    {
        var cusumLegend = [];
        cusumLegend.push({
            text: 'CUSUM Group',
            separator: true
        });
        cusumLegend.push({
            text: 'CUSUM-',
            color: '#000000',
            shape: LABKEY.vis.TrendingLineShape.negativeCUSUM
        });
        cusumLegend.push({
            text: 'CUSUM+',
            color: '#000000',
            shape: LABKEY.vis.TrendingLineShape.positiveCUSUM
        });
        return cusumLegend;
    }

});