Ext4.define("LABKEY.targetedms.CUSUMPlotHelper", {
    extend: 'LABKEY.targetedms.QCPlotHelperBase',
    setCUSUMSeriesMinMax: function(dataObject, row, isCUSUMmean) {
        dataObject.showLogInvalid = true; //CUSUM- is always negative

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
    }

});