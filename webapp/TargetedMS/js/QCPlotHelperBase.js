/*
 * Copyright (c) 2016-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define("LABKEY.targetedms.QCPlotHelperBase", {

    statics: {
        qcPlotTypesWithYOptions : ['Levey-Jennings', 'Moving Range'],
        qcPlotTypesWithoutYOptions : ['CUSUMm', 'CUSUMv']
    },

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

    getGuideSetData : function(useRaw, includeAllValues) {
        var config = this.getReportConfig();
        var metricProps = this.getMetricPropsById(this.metric);

        var guideSetSql = "SELECT s.*, g.Comment FROM (";
        if (useRaw) {
            guideSetSql += this.metricGuideSetRawSql(metricProps.id, metricProps.series1SchemaName, metricProps.series1QueryName, metricProps.series2SchemaName, metricProps.series2QueryName, includeAllValues);
        }
        else {
            guideSetSql += this.metricGuideSetSql(metricProps.id, metricProps.series1SchemaName, metricProps.series1QueryName, metricProps.series2SchemaName, metricProps.series2QueryName, true);
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
            var sub = separator + "(";
            if (includeAllValues) {
                sub += "s.TrainingStart IS NULL OR ";
            }
            guideSetSql += sub + "s.TrainingStart < TIMESTAMPADD('SQL_TSI_DAY', 1, CAST('" + config.EndDate + "' AS TIMESTAMP)))";

            if (includeAllValues) {
                guideSetSql += " AND s.AcquiredTime <= TIMESTAMPADD('SQL_TSI_DAY', 1, CAST('" + config.EndDate + "' AS TIMESTAMP)) AND s.AcquiredTime >= TIMESTAMPADD('SQL_TSI_DAY', 1, CAST('" + config.StartDate + "' AS TIMESTAMP))";
            }
        }

        var sqlConfig = {
            schemaName: 'targetedms',
            sql: guideSetSql,
            sort: 'GuideSetId, SeriesLabel, AcquiredTime', //it's important that record is sorted by AcquiredTime asc as ordering is critical in calculating mR and CUSUM
            scope: this,
            success: useRaw? this.processRawGuideSetData : this.processLJGuideSetData,
            failure: this.failureHandler
        };
        if (!useRaw)
            sqlConfig.sort = 'TrainingStart,SeriesLabel';
        LABKEY.Query.executeSql(sqlConfig);
    },

    getRawGuideSetData : function(includeAllValues) {
        this.getGuideSetData(true, includeAllValues);
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
        if(data) {
            var guideSetAvgMRs = this.getGuideSetAvgMRs(data.rows, this.yAxisScale == 'log');
            if (!this.guideSetDataMap)
                this.guideSetDataMap = {};
            Ext4.each(data.rows, function (row) {
                var guideSetId = row['GuideSetId'];
            if (!this.guideSetDataMap[guideSetId])
            {
                    this.guideSetDataMap[guideSetId] = this.getGuideSetDataObj(row);
                    this.hasGuideSetData = true;
                }

                var seriesLabel = row['SeriesLabel'];
                var seriesType = row['SeriesType'];
                if (guideSetId == null) {

                    if (!this.defaultGuideSet) {
                        this.defaultGuideSet = {};
                    }

                    if (!this.defaultGuideSet[seriesLabel]) {
                        this.defaultGuideSet[seriesLabel] = {};
                    }

                    if (!this.defaultGuideSet[seriesLabel][seriesType]) {
                        this.defaultGuideSet[seriesLabel][seriesType] = {};
                    }

                    this.defaultGuideSet[seriesLabel][seriesType].MR =
                            {
                                Mean: guideSetAvgMRs[guideSetId].Series[seriesLabel][seriesType].avgMR,
                                StdDev: guideSetAvgMRs[guideSetId].Series[seriesLabel][seriesType].stddevMR
                            };

                }
                else {
                    if (!this.guideSetDataMap[guideSetId].Series[seriesLabel]) {
                        this.guideSetDataMap[guideSetId].Series[seriesLabel] = {};
                    }

                    if (!this.guideSetDataMap[guideSetId].Series[seriesLabel][seriesType]) {
                        this.guideSetDataMap[guideSetId].Series[seriesLabel][seriesType] = {
                            MeanMR: guideSetAvgMRs[guideSetId].Series[seriesLabel][seriesType].avgMR,
                            StdDevMR: guideSetAvgMRs[guideSetId].Series[seriesLabel][seriesType].stddevMR
                        };
                    }
                    else {
                        this.guideSetDataMap[guideSetId].Series[seriesLabel][seriesType].MeanMR = guideSetAvgMRs[guideSetId].Series[seriesLabel][seriesType].avgMR;
                        this.guideSetDataMap[guideSetId].Series[seriesLabel][seriesType].StdDevMR = guideSetAvgMRs[guideSetId].Series[seriesLabel][seriesType].stddevMR;
                    }
                }
            }, this);

            this.getPlotData();
        }
    },

    getPlotData: function ()
    {
        var config = this.getReportConfig(),
                metricProps = this.getMetricPropsById(this.metric);

        // Filter on start/end dates, casting as DATE to ignore the time part
        var whereClause = " WHERE ", sep = "";
        if (config.StartDate)
        {
            whereClause += "CAST(SampleFileId.AcquiredTime AS DATE) >= '" + config.StartDate + "'";
            sep = " AND ";
        }
        if (config.EndDate)
        {
            whereClause += sep + "CAST(SampleFileId.AcquiredTime AS DATE) <= '" + config.EndDate + "'";
        }

        if(Object.keys(this.selectedAnnotations).length > 0)
        {
            var filterClause = "SampleFileId.ReplicateId in (";
            var intersect = "";
            var selectSql = "(SELECT ReplicateId FROM targetedms.ReplicateAnnotation WHERE ";
            Ext4.Object.each(this.selectedAnnotations, function(name, values)
            {
                filterClause += (intersect + selectSql + " Name='" + name + "' AND ( ");
                var or = "";
                for (var i = 0; i < values.length; i++)
                {
                    // Escape single quotes for SQL query
                    var val = values[i].replace(/\'/g,"''");
                    filterClause += (or + "Value='" + val + "'");
                    or = " OR ";
                }
                filterClause += " ) ) ";
                intersect = " INTERSECT ";
            });
            filterClause += ") ";
            whereClause += sep + filterClause;
        }

        this.plotDataRows = [];
        var seriesTypes = this.isMultiSeries() ? ['series1', 'series2'] : ['series1'];
        var sql = this.getSeriesTypePlotDataSql(seriesTypes, metricProps, whereClause);

        sql = "SELECT * FROM (" + sql + ") a"; //wrap unioned results in sql to support sorting

        if (!this.showExcluded) {
            sql += ' WHERE IgnoreInQC = false';
        }

        LABKEY.Query.executeSql({
            schemaName: 'targetedms',
            sql: sql,
            scope: this,
            sort: 'SeriesType, SeriesLabel, AcquiredTime', //it's important that record is sorted by AcquiredTime asc as ordering is critical in calculating mR and CUSUM
            success: function (data) {
                if(data) {
                    this.plotDataRows = this.plotDataRows.concat(data.rows);

                    this.processPlotData(this.plotDataRows);
                }
            },
            failure: this.failureHandler
        });

    },

    processPlotData: function(plotDataRows) {
        var metricProps = this.getMetricPropsById(this.metric);
        var allPlotDateValues = [];
        
        this.processedPlotData = this.preprocessPlotData(plotDataRows, this.showMovingRangePlot(), this.showMeanCUSUMPlot(), this.showVariableCUSUMPlot(), this.yAxisScale == 'log');

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
                    allPlotDateValues.push(data.fullDate);
                }, this);
            }, this);
        }, this);

        // Issue 31678: get the full set of dates values from the precursor data and from the annotations
        for (var j = 0; j < this.annotationData.length; j++) {
            allPlotDateValues.push(this.formatDate(Ext4.Date.parse(this.annotationData[j].Date, LABKEY.Utils.getDateTimeFormatWithMS()), true));
        }
        allPlotDateValues = Ext4.Array.unique(allPlotDateValues).sort();

        this.legendHelper = Ext4.create("LABKEY.targetedms.QCPlotLegendHelper");
        this.legendHelper.setupLegendPrefixes(this.fragmentPlotData, 3);

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

                // Issue 31678: add any missing dates from the other plots or from the annotations
                var dateProp = this.groupedX ? "date" : "fullDate";
                var precursorDates = Ext4.Array.pluck(precursorInfo.data, dateProp);
                var datesToAdd = [];
                for (var j = 0; j < allPlotDateValues.length; j++) {
                    var dateVal = this.formatDate(allPlotDateValues[j], !this.groupedX);
                    var dataIsMissingDate = precursorDates.indexOf(dateVal) == -1 && Ext4.Array.pluck(datesToAdd, dateProp).indexOf(dateVal) == -1;
                    if (dataIsMissingDate) {
                        datesToAdd.push({
                            type: 'missing',
                            fullDate: this.formatDate(allPlotDateValues[j], true),
                            date: this.formatDate(allPlotDateValues[j]),
                            groupedXTick: dateVal
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
            clipRect: true, // set this to true to prevent lines from running outside of the plot region
            data : Ext4.Array.clone(data),
            width : this.getPlotWidth(),
            height : this.singlePlot ? 500 : 300,
            gridLineColor : 'white',
            legendData : Ext4.Array.clone(legenddata),
            legendNoWrap: true
        };
    },

    getPlotWidth: function()
    {
        var width = this.plotWidth - 30;
        return !this.largePlot && this.plotTypes.length > 1 ? width / 2 : width;
    },

    // TODO: Move this to tests
    testVals: {
        a: {fragment:'', dataType: 'Peptide', result: ''},
        b: {fragment:'A', dataType: 'Peptide', result: 'A'},
        c: {fragment:'A', dataType: 'Peptide', result: 'A'}, // duplicate
        d: {fragment:'AB', dataType: 'Peptide', result: 'AB'},
        e: {fragment:'ABC', dataType: 'Peptide', result: 'ABC'},
        f: {fragment:'ABCD', dataType: 'Peptide', result: 'ABCD'},
        g: {fragment:'ABCDE', dataType: 'Peptide', result: 'ABCDE'},
        h: {fragment:'ABCDEF', dataType: 'Peptide', result: 'ABCDEF'},
        i: {fragment:'ABCDEFG', dataType: 'Peptide', result: 'ABCDEFG'},
        j: {fragment:'ABCDEFGH', dataType: 'Peptide', result: 'ABC…FGH'},
        k: {fragment:'ABCDEFGHI', dataType: 'Peptide', result: 'ABC…GHI'},
        l: {fragment:'ABCE', dataType: 'Peptide', result: 'ABCE'},
        m: {fragment:'ABDEFGHI', dataType: 'Peptide', result: 'ABD…'},
        n: {fragment:'ABEFGHI', dataType: 'Peptide', result: 'ABEFGHI'},
        o: {fragment:'ABEFGHIJ', dataType: 'Peptide', result: 'ABE…HIJ'},
        p: {fragment:'ABEFHI', dataType: 'Peptide', result: 'ABEFHI'},
        q: {fragment:'ABFFFGHI', dataType: 'Peptide', result: 'ABF(5)'},
        r: {fragment:'ABFFFFGHI', dataType: 'Peptide', result: 'ABF(6)'},
        s: {fragment:'ABFFFFAFGHI', dataType: 'Peptide', result: 'ABF…FA…'},
        t: {fragment:'ABFFFAFFGHI', dataType: 'Peptide', result: 'ABF…A…'},
        u: {fragment:'ABGAABAABAGHI', dataType: 'Peptide', result: 'ABG…B…B…'},
        v: {fragment:'ABGAAbAABAGHI', dataType: 'Peptide', result: 'ABG…b…B…'},
        w: {fragment:'ABGAABAAbAGHI', dataType: 'Peptide', result: 'ABG…B…b…'},
        x: {fragment:'ABGAAB[80]AAB[99]AGHI', dataType: 'Peptide', result: 'ABG…b…b…'},
        y: {fragment:'C32:0', dataType: 'ion', result: 'C32:0'},
        z: {fragment:'C32:1', dataType: 'ion', result: 'C32:1'},
        aa: {fragment:'C32:2', dataType: 'ion', result: 'C32:2'},
        bb: {fragment:'C32:2', dataType: 'ion', result: 'C32:2'},
        cc: {fragment:'C30:0', dataType: 'ion', result: 'C30:0'},
        dd: {fragment:'C[30]:0', dataType: 'ion', result: 'C[30]:0'},
        ee: {fragment:'C[400]:0', dataType: 'ion', result: 'C[4…'},
        ff: {fragment:'C12:0 fish breath', dataType: 'ion', result: 'C12…'},
        gg: {fragment:'C15:0 fish breath', dataType: 'ion', result: 'C15(14)'},
        hh: {fragment:'C15:0 doggy breath', dataType: 'ion', result: 'C15(15)'},
        ii: {fragment:'C16:0 fishy breath', dataType: 'ion', result: 'C16…f…'},
        jj: {fragment:'C16:0 doggy breath', dataType: 'ion', result: 'C16…d…'},
        kk: {fragment:'C14', dataType: 'ion', result: 'C14'},
        ll: {fragment:'C14:1', dataType: 'ion', result: 'C14:1'},
        mm: {fragment:'C14:1-OH', dataType: 'ion', result: 'C14:1…'},
        nn: {fragment:'C14:2', dataType: 'ion', result: 'C14:2'},
        oo: {fragment:'C14:2-OH', dataType: 'ion', result: 'C14:2…'},
    },

    testLegends: function() {
        var legendHelper = Ext4.create("LABKEY.targetedms.QCPlotLegendHelper");
        legendHelper.setupLegendPrefixes(this.testVals, 3);

        for (var key in this.testVals) {
            if (this.testVals.hasOwnProperty(key)) {
                var val = legendHelper.getUniquePrefix(this.testVals[key].fragment, (this.testVals[key].dataType == 'Peptide'));
                if(val !== this.testVals[key].result)
                    console.log("Incorrect result for " + this.testVals[key].fragment + ". Expected: " + this.testVals[key].result + ", Actual: " + val);
            }
        }
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
                text: this.legendHelper.getLegendItemText(precursorInfo),
                hoverText: precursorInfo.fragment,
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
                    text: this.legendHelper.getLegendItemText(precursorInfo),
                    hoverText: precursorInfo.fragment,
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

        var extraPlotLegendData = this.getAdditionalPlotLegend(plotType);
        newLegendData = newLegendData.concat(extraPlotLegendData);

        return newLegendData;
    },

    getYScaleLabel: function(plotType, conversion, label) {
        var yScaleLabel;

        if (plotType !== LABKEY.vis.TrendingLinePlotType.MovingRange && plotType !== LABKEY.vis.TrendingLinePlotType.LeveyJennings) {
            yScaleLabel = 'Sum of Deviations'
        }
        else if (conversion && label !== "Transition Area") {
            var options = this.getYAxisOptions();
            for (var i = 0; i < options.data.length; i++) {
                if (options.data[i][0] === conversion)
                    yScaleLabel = options.data[i][1];
            }
        }

        // TODO: Add these to targetedms.QCMetricConfiguration
        if (!yScaleLabel) {
            var metricName = this.getMetricPropsById(this.metric).name;

            if (metricName === "Full Width at Base (FWB)") {
                yScaleLabel = "Minutes";
            }
            else if (metricName === "Full Width at Half Maximum (FWHM)") {
                yScaleLabel = "Minutes";
            }
            else if (metricName === "Light/Heavy Ratio") {
                yScaleLabel = "Ratio";
            }
            else if (metricName === "Mass Accuracy") {
                yScaleLabel = "PPM";
            }
            else if (metricName === "Peak Area") {
                yScaleLabel = "Area";
            }
            else if (metricName === "Retention Time") {
                yScaleLabel = "Minutes";
            }
            else if (metricName === "Transition/Precursor Area Ratio") {
                yScaleLabel = "Ratio";
            }
            else if (metricName === "Transition Area") {
                yScaleLabel = "Area";
            }
            else {
                yScaleLabel = label;
            }
        }
        return yScaleLabel;
    },

    getSubtitle: function(precursor) {
        return precursor + ' - ' + this.getMetricPropsById(this.metric).name;;
    },

    addEachCombinedPrecusorPlot: function(plotIndex, id, combinePlotData, groupColors, yAxisCount, metricProps, showLogInvalid, legendMargin, plotType, isCUSUMMean)
    {
        var plotLegendData = this.getCombinedPlotLegendData(metricProps, groupColors, yAxisCount, plotType, isCUSUMMean);

        if (plotType !== LABKEY.vis.TrendingLinePlotType.CUSUM) {
            this.showInvalidLogMsg(id, showLogInvalid);
        }

        var disableRange = true;
        if (plotType === LABKEY.vis.TrendingLinePlotType.CUSUM && !this.getMetricPropsById(this.metric).series2QueryName) {
            disableRange = false;
        }
        else if (this.yAxisScale === 'standardDeviation' && plotType === LABKEY.vis.TrendingLinePlotType.LeveyJennings) {
            disableRange = false;
        }

        var trendLineProps = {
            disableRangeDisplay: disableRange,
            xTick: this.groupedX ? 'groupedXTick' : 'fullDate',
            xTickLabel: 'date',
            shape: 'guideSetId',
            combined: true,
            yAxisScale: (showLogInvalid ? 'linear' : (this.yAxisScale !== 'log' ? 'linear' : 'log')),
            valueConversion: (this.yAxisScale === 'percentDeviation' || this.yAxisScale === 'standardDeviation' ? this.yAxisScale : undefined),
            defaultGuideSets: this.defaultGuideSet,
            groupBy: 'fragment',
            color: 'fragment',
            defaultGuideSetLabel: 'fragment',
            pointOpacityFn: function(row) { return row.IgnoreInQC ? 0.4 : 1; },
            showTrendLine: true,
            showDataPoints: true,
            mouseOverFn: this.plotPointHover,
            mouseOverFnScope: this,
            position: this.groupedX ? 'jitter' : undefined
        };

        Ext4.apply(trendLineProps, this.getPlotTypeProperties(combinePlotData, plotType, isCUSUMMean));

        var mainTitle = LABKEY.targetedms.QCPlotHelperWrapper.getQCPlotTypeLabel(plotType, isCUSUMMean);

        var basePlotConfig = this.getBasePlotConfig(id, combinePlotData.data, plotLegendData);
        var plotConfig = Ext4.apply(basePlotConfig, {
            margins : {
                top: 65 + this.getMaxStackedAnnotations() * 12,
                right: (this.showInPlotLegends() ? legendMargin : 30 ) + (this.isMultiSeries() ? 50 : 0),
                left: 75,
                bottom: 75
            },
            labels : {
                main: {
                    value: mainTitle
                },
                subtitle: {
                    value: this.getSubtitle("All Series", plotType, trendLineProps.valueConversion),
                    color: '#555555'
                },
                yLeft: {
                    value: this.getYScaleLabel(plotType, trendLineProps.valueConversion, metricProps.series1Label)
                },
                yRight: {
                    value: this.isMultiSeries() ? metricProps.series2Label : undefined,
                    visibility: this.isMultiSeries() ? undefined : 'hidden'
                }
            },
            properties: trendLineProps
        });

        plotConfig.qcPlotType = plotType;
        this.lastPlotConfig = plotConfig; // remember the plot config for generating legend popup
        var plot = LABKEY.vis.TrendingLinePlot(plotConfig);
        plot.render();

        this.addAnnotationsToPlot(plot, combinePlotData);

        this.addGuideSetTrainingRangeToPlot(plot, combinePlotData);

        this.attachPlotExportIcons(id, mainTitle + '- All Series', plotIndex, this.getPlotWidth(), this.showInPlotLegends() ? 0 : legendMargin);
    },

    addEachIndividualPrecusorPlot: function(plotIndex, id, precursorIndex, precursorInfo, metricProps, plotType, isCUSUMMean, scope)
    {
        if (this.yAxisScale == 'log' && plotType != LABKEY.vis.TrendingLinePlotType.LeveyJennings && plotType != LABKEY.vis.TrendingLinePlotType.CUSUM)
        {
            Ext4.get(id).update("<span style='font-style: italic;'>Values that are 0 have been replaced with 0.0000001 for log scale plot.</span>");
        }
        else if (precursorInfo.showLogInvalid && plotType !== LABKEY.vis.TrendingLinePlotType.CUSUM)
        {
            this.showInvalidLogMsg(id, true);
        }
        else if (precursorInfo.showLogWarning && plotType !== LABKEY.vis.TrendingLinePlotType.CUSUM)
        {
            Ext4.get(id).update("<span style='font-style: italic;'>For log scale, standard deviations below "
                    + "the mean with negative values have been omitted.</span>");
        }

        var trendLineProps = {
            xTick: this.groupedX ? 'groupedXTick' : 'fullDate',
            xTickLabel: 'date',
            yAxisScale: (precursorInfo.showLogInvalid ? 'linear' : (this.yAxisScale !== 'log' ? 'linear' : 'log')),
            valueConversion: (this.yAxisScale === 'percentDeviation' || this.yAxisScale === 'standardDeviation' ? this.yAxisScale : undefined),
            shape: 'guideSetId',
            combined: false,
            pointOpacityFn: function(row) { return row.IgnoreInQC ? 0.4 : 1; },
            pointIdAttr: function(row) { return row['fullDate']; },
            showTrendLine: true,
            showDataPoints: true,
            defaultGuideSetLabel: 'fragment',
            defaultGuideSets: this.defaultGuideSet,
            mouseOverFn: this.plotPointHover,
            mouseOverFnScope: this,
            position: this.groupedX ? 'jitter' : undefined,
            disableRangeDisplay: this.isMultiSeries()
        };

        Ext4.apply(trendLineProps, this.getPlotTypeProperties(precursorInfo, plotType, isCUSUMMean));

        var plotLegendData = this.getAdditionalPlotLegend(plotType);
        if (Ext4.isArray(this.legendData))
        {
            plotLegendData = plotLegendData.concat(this.legendData);
        }

        if (plotLegendData && plotLegendData.length > 0)
        {
            Ext4.each(plotLegendData, function(legend){
                if (legend.text && legend.text.length > 0)
                {
                    if ( !this.longestLegendText || (this.longestLegendText && legend.text.length > this.longestLegendText))
                        this.longestLegendText = legend.text.length;
                }
            }, this);
        }

        var mainTitle = LABKEY.targetedms.QCPlotHelperWrapper.getQCPlotTypeLabel(plotType, isCUSUMMean);

        var basePlotConfig = this.getBasePlotConfig(id, precursorInfo.data, plotLegendData);
        var plotConfig = Ext4.apply(basePlotConfig, {
            margins : {
                top: 65 + this.getMaxStackedAnnotations() * 12,
                left: 75,
                bottom: 75,
                right: (this.showInPlotLegends() ? 0 : 30) // if in plot, set to 0 to auto calculate margin; otherwise, set to small value to cut off legend
            },
            labels : {
                main: {
                    value: mainTitle
                },
                subtitle: {
                    value: this.getSubtitle(this.precursors[precursorIndex], plotType, trendLineProps.valueConversion),
                    color: '#555555'
                },
                yLeft: {
                    value: this.getYScaleLabel(plotType, trendLineProps.valueConversion, metricProps.series1Label),
                    color: this.isMultiSeries() ? this.getColorRange()[0] : undefined
                },
                yRight: {
                    value: this.isMultiSeries() ? metricProps.series2Label : undefined,
                    visibility: this.isMultiSeries() ? undefined : 'hidden',
                    color: this.isMultiSeries() ? this.getColorRange()[1] : undefined
                }
            },
            properties: trendLineProps,
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
                    scope.plotBrushEndEvent(data[data.length - 1], extent, plot);
                },
                brushclear: function(event, data, plot, layerSelections) {
                    scope.plotBrushClearEvent(data[data.length - 1], plot);
                }
            }
        });

        // create plot using the JS Vis API
        plotConfig.qcPlotType = plotType;
        this.lastPlotConfig = plotConfig; // remember the plot config for generating legend popup
        var plot = LABKEY.vis.TrendingLinePlot(plotConfig);
        plot.render();

        this.addAnnotationsToPlot(plot, precursorInfo);

        this.addGuideSetTrainingRangeToPlot(plot, precursorInfo);

        var extraMargin = this.showInPlotLegends() ? 0 : 10 * this.longestLegendText;
        this.attachPlotExportIcons(id, mainTitle + '-' + this.precursors[precursorIndex] + '-' + this.getMetricPropsById(this.metric).series1Label, plotIndex, this.getPlotWidth(), extraMargin);
    },

    // empty legend to reserve plot space for plot alignment
    getEmptyLegend: function()
    {
        var empty = [];
        empty.push({
            text: '',
            shape: function(){
                return 'M0,0L0,0Z';
            }
        });
        return empty;
    },

    getPeptidePlotCount: function()
    {
        if (this.peptidePlotCount)
            return this.peptidePlotCount;
        var count = 0;
        for (var i = 0; i < this.precursors.length; i++)
        {
            var precursorInfo = this.fragmentPlotData[this.precursors[i]];

            // We don't necessarily have info for all possible precursors, depending on the filters and plot type
            if (precursorInfo)
            {
                count++;
            }
        }
        this.peptidePlotCount = count;
        return this.peptidePlotCount;
    },

    showInPlotLegends: function () {
        return true;
    }
});