Ext4.define("LABKEY.targetedms.LeveyJenningsPlotHelper", {
    extend: 'LABKEY.targetedms.QCPlotHelperBase',
    setLJSeriesMinMax: function(dataObject, row) {
        // track the min and max data so we can get the range for including the QC annotations
        var val = row['value'];
        if (LABKEY.vis.isValid(val))
        {
            if (dataObject.min == null || val < dataObject.min) {
                dataObject.min = val;
            }
            if (dataObject.max == null || val > dataObject.max) {
                dataObject.max = val;
            }

            if (this.yAxisScale == 'log' && val <= 0)
            {
                dataObject.showLogInvalid = true;
            }

            var mean = row['mean'];
            var sd = LABKEY.vis.isValid(row['stdDev']) ? row['stdDev'] : 0;
            if (LABKEY.vis.isValid(mean))
            {
                var minSd = (mean - (3 * sd));
                if (dataObject.showLogInvalid == undefined && this.yAxisScale == 'log' && minSd <= 0)
                {
                    // Avoid setting our scale to be negative based on the three standard deviations to avoid messing up log plots
                    dataObject.showLogWarning = true;
                    for (var i = 2; i >= 0; i--)
                    {
                        minSd = (mean - (i * sd));
                        if (minSd > 0) {
                            break;
                        }
                    }
                }
                if (dataObject.min == null || minSd < dataObject.min) {
                    dataObject.min = minSd;
                }

                if (dataObject.max == null || (mean + (3 * sd)) > dataObject.max) {
                    dataObject.max = (mean + (3 * sd));
                }
            }
        }
        else if (this.isMultiSeries())
        {
            // check if either of the y-axis metric values are invalid for a log scale
            var val1 = row['value_series1'],
                    val2 = row['value_series2'];
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