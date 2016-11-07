Ext4.define("LABKEY.targetedms.MovingRangePlotHelper", {
    extend: 'LABKEY.targetedms.QCPlotHelperBase',
    setMovingRangeSeriesMinMax: function(dataObject, row) {
        // track the min and max data so we can get the range for including the QC annotations
        var val = row['valueMR'];
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
                if (dataObject.max == null || (mean * LABKEY.vis.Stat.MOVING_RANGE_UPPER_LIMIT_WEIGHT) > dataObject.max) {
                    dataObject.max = mean * LABKEY.vis.Stat.MOVING_RANGE_UPPER_LIMIT_WEIGHT;
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
    }

});