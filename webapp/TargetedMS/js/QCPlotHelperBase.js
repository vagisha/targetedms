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

    processRawGuideSetData: function (plotDataRows) {
        if (!this.guideSetDataMap)
            this.guideSetDataMap = {};

        Ext4.each(plotDataRows, function (plotDataRow) {
            Ext4.each(plotDataRow.GuideSetStats, function (guideSetStat) {
                var guideSetId = guideSetStat['GuideSetId'];
                var seriesType = guideSetStat['SeriesType'] === 2 ? 'series2' : 'series1';
                var seriesLabel = plotDataRow['SeriesLabel'];

                if (guideSetId === 0) {
                    if (!this.defaultGuideSet) {
                        this.defaultGuideSet = {};
                    }

                    if (!this.defaultGuideSet[seriesLabel]) {
                        this.defaultGuideSet[seriesLabel] = {};
                    }

                    if (!this.defaultGuideSet[seriesLabel][seriesType]) {
                        this.defaultGuideSet[seriesLabel][seriesType] = {};
                    }

                    this.defaultGuideSet[seriesLabel][seriesType].MR = {
                        Mean: guideSetStat['MeanMR'],
                        StdDev: guideSetStat['StdDevMR']
                    };
                }
                else {
                    if (!this.guideSetDataMap[guideSetId]) {
                        this.guideSetDataMap[guideSetId] = this.getGuideSetDataObj(guideSetStat);
                    }
                    if (!this.guideSetDataMap[guideSetId].Series[seriesLabel]) {
                        this.guideSetDataMap[guideSetId].Series[seriesLabel] = {};
                    }

                    if (!this.guideSetDataMap[guideSetId].Series[seriesLabel][seriesType]) {
                        this.guideSetDataMap[guideSetId].Series[seriesLabel][seriesType] = {
                            MeanMR: guideSetStat['MeanMR'],
                            StdDevMR: guideSetStat['StdDevMR']
                        };
                    }
                    else {
                        this.guideSetDataMap[guideSetId].Series[seriesLabel][seriesType].MeanMR = guideSetStat['MeanMR'];
                        this.guideSetDataMap[guideSetId].Series[seriesLabel][seriesType].StdDevMR = guideSetStat['StdDevMR'];
                    }
                }
            }, this);

        }, this);

    },

    getPlotsData: function() {
        var plotsConfig = {};
        plotsConfig.metricId = this.metric;
        plotsConfig.includeLJ = this.showLJPlot();
        plotsConfig.includeMR = this.showMovingRangePlot();
        plotsConfig.includeMeanCusum = this.showMeanCUSUMPlot();
        plotsConfig.includeVariableCusum = this.showVariableCUSUMPlot();
        plotsConfig.showExcluded = this.showExcluded;
        // show reference guide set for custom date range
        plotsConfig.showReferenceGS = this.showReferenceGS && this.dateRangeOffset !== 0;

        var config = this.getReportConfig()

        if (this.selectedAnnotations) {
            plotsConfig.selectedAnnotations = [];
            Ext4.Object.each(this.selectedAnnotations, function (name, values) {
                plotsConfig.selectedAnnotations.push({
                    name: name,
                    values: values
                })
            }, this);
        }

        if (config) {
            plotsConfig.startDate = config.StartDate;
            plotsConfig.endDate = config.EndDate;
        }

        LABKEY.Ajax.request({
            url: LABKEY.ActionURL.buildURL('targetedms', 'GetQCPlotsData.api'),
            success: this.processPlotData,
            failure: LABKEY.Utils.getCallbackWrapper(this.failureHandler),
            scope: this,
            jsonData: plotsConfig
        });
    },

    processPlotData: function(response) {
        var parsed = JSON.parse(response.responseText);
        var plotDataRows = parsed.plotDataRows;
        var metricProps = parsed.metricProps;
        var sampleFiles = parsed.sampleFiles;
        this.filterQCPoints = parsed.filterQCPoints;

        var allPlotDateValues = [];

        this.setPrecursorsForPage(plotDataRows);

        // process the data to shape it for the JS LeveyJenningsPlot API call
        this.fragmentPlotData = {};

        if (this.showLJPlot()) {
            this.processLJGuideSetData(plotDataRows);
        }
        else if (this.showMovingRangePlot() || this.showMeanCUSUMPlot() || this.showVariableCUSUMPlot()) {
            this.processRawGuideSetData(plotDataRows);
        }

        var tempData; // temp variable to store data for setting the date
        for (var i = this.pagingStartIndex; i < this.pagingEndIndex; i++) {
            var plotDataRow = plotDataRows[i];
            tempData = plotDataRow;
            var fragment = plotDataRow.SeriesLabel;
            Ext4.iterate(plotDataRow.data, function (plotData) {
                Ext4.iterate(sampleFiles, function (sampleFile) {
                    if (plotData['SampleFileId'] === sampleFile['SampleId']) {
                        plotData['FilePath'] = sampleFile['FilePath'];
                        plotData['ReplicateId'] = sampleFile['ReplicateId'];
                        plotData['AcquiredTime'] = sampleFile['AcquiredTime'];
                    }
                }, this);

                var data = this.processPlotDataRow(plotData, plotDataRow, fragment, metricProps);
                this.fragmentPlotData[fragment].data.push(data);
                this.fragmentPlotData[fragment].precursorScoped = metricProps.precursorScoped;
                this.setSeriesMinMax(this.fragmentPlotData[fragment], data);
                allPlotDateValues.push(data.fullDate);

            }, this);

            if (this.filterQCPoints && !this.groupedX) {
                for (var j = 0; j < plotDataRow.data.length; j++) {
                    var plotData = plotDataRow.data[j];

                    Ext4.Object.each(this.guideSetDataMap, function(guideSetId, guideSetData) {
                        // for truncating out of range guideset data  find first index of plotDate ending at guideset.trainingEnd
                        if (plotData.GuideSetId == guideSetId && plotData.InGuideSetTrainingRange && guideSetData.TrainingEnd <= this.startDate) {
                            this.filterPointsFirstIndex = j + 1;
                        }
                    }, this);

                    // for truncating out of range guideset data find last index of plotData starting from this.startDate
                    if (plotData.AcquiredTime >= this.startDate) {
                        if (!this.filterPointsLastIndex) {
                            this.filterPointsLastIndex = j;
                        }
                    }
                }
            }
        }

        if (this.showExpRunRange && this.filterPointsLastIndex && this.filterPointsFirstIndex) {
            // no need to filter if less than 6 data points are present between reference end of guideset and startdate
            if (this.filterPointsLastIndex - this.filterPointsFirstIndex < 6) {
                this.filterQCPoints = false;
                // set the startDate field = acquired time of the 1st point of 5 points before the experiment run range
                this.getStartDateField().setValue(this.formatDate(tempData.data[this.filterPointsFirstIndex].AcquiredTime));
            }
            else { // skip 5 points
                this.filterPointsLastIndex = this.filterPointsLastIndex - 6;
                // set the startDate field = acquired time of the 1st point of 5 points before the experiment run range
                // adding 1 as the point is right after filter last index
                this.getStartDateField().setValue(this.formatDate(tempData.data[this.filterPointsLastIndex + 1].AcquiredTime));
            }
        }

        // Issue 31678: get the full set of dates values from the precursor data and from the annotations
        for (var j = 0; j < this.annotationData.length; j++) {
            allPlotDateValues.push(this.formatDate(Ext4.Date.parse(this.annotationData[j].Date, LABKEY.Utils.getDateTimeFormatWithMS()), true));
        }
        allPlotDateValues = Ext4.Array.unique(allPlotDateValues).sort();

        this.legendHelper = LABKEY.targetedms.QCPlotLegendHelper;
        this.legendHelper.setupLegendPrefixes(this.fragmentPlotData, 3);

        // merge in the annotation data to make room on the y axis
        for (var i = 0; i < this.precursors.length; i++) {
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

    renderPlots: function() {
        if (this.filterQCPoints && !this.groupedX) {
            this.truncateOutOfRangeQCPoints();
        }
        // do not persist plot options in qc folder if changed after coming through experimental folder link
        if (!this.showExpRunRange) {
            this.persistSelectedFormOptions();
        }

        if (this.precursors.length === 0) {
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

    truncateOutOfRangeQCPoints: function() {
        Ext4.Object.each(this.fragmentPlotData, function(label, fragmentData) {
            // traverse plotData backwards from firstIndex to lastIndex and
            // remove them from the array
            if (this.filterQCPoints && this.filterPointsFirstIndex && this.filterPointsLastIndex) {
                for (var i = this.filterPointsLastIndex; i >= this.filterPointsFirstIndex; i--) {
                    this.fragmentPlotData[label].data.splice(i, 1);
                }

                // ReferenceRangeSeries is used to separate series
                for (var i = 0; i < this.filterPointsFirstIndex; i++) {
                    fragmentData.data[i]['ReferenceRangeSeries'] = "GuideSet";
                }

                for (var i = this.filterPointsFirstIndex; i < fragmentData.data.length; i++) {
                    fragmentData.data[i]['ReferenceRangeSeries'] = "InRange";
                }
            }
        }, this);
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

    getPlotWidth: function() {
        var width = this.plotWidth - 30;
        return !this.largePlot && this.plotTypes.length > 1 ? width / 2 : width;
    },

    calculatePlotIndicesBetweenDates: function (precursorInfo) {
        var startDate = new Date(this.expRunDetails.startDate);
        var endDate = new Date(this.expRunDetails.endDate);
        var startIndex;
        var endIndex;

        if (precursorInfo) {
            // fragmentPlotData has plot data separated by series labels
            var data = precursorInfo.data;

            for (var index = 0; index < data.length; index++) {
                var pointDate = new Date(data[index].fullDate)
                if (pointDate >= startDate && pointDate < endDate) {
                    if (startIndex === undefined) {
                        startIndex = data[index].seqValue;
                    }
                }

                if (pointDate >= endDate) {
                    if (!endIndex) {
                        endIndex = data[index].seqValue;
                    }
                }
                // this happens for custom date range shorter than exp date range
                else if (index === data.length - 1 && endIndex === undefined && startIndex !== undefined) {
                    endIndex = data[data.length - 1].seqValue;
                }

                var foundIndices = startIndex !== undefined && endIndex !== undefined;

                if (foundIndices) {
                    this.expRunDetails['startIndex'] = startIndex;
                    this.expRunDetails['endIndex'] = endIndex;
                    break;
                }
            }

        }
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
        var legendHelper = LABKEY.targetedms.QCPlotLegendHelper;
        legendHelper.setupLegendPrefixes(this.testVals, 3);

        for (var key in this.testVals) {
            if (this.testVals.hasOwnProperty(key)) {
                var val = legendHelper.getUniquePrefix(this.testVals[key].fragment, (this.testVals[key].dataType == 'Peptide'));
                if(val !== this.testVals[key].result)
                    console.log("Incorrect result for " + this.testVals[key].fragment + ". Expected: " + this.testVals[key].result + ", Actual: " + val);
            }
        }
    },

    getCombinedPlotLegendData: function(metricProps, groupColors, yAxisCount, plotType, isCUSUMMean) {
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
            // We may not have a match if it's been filtered out - see issue 38720
            if (precursorInfo) {
                var appropriateLegend = precursorInfo.dataType == 'Peptide' ? proteomicsLegend : ionLegend;

                appropriateLegend.push({
                    name: precursorInfo.fragment + (this.isMultiSeries() ? '|' + legendSeries[0] : ''),
                    text: this.legendHelper.getLegendItemText(precursorInfo),
                    hoverText: precursorInfo.fragment,
                    color: groupColors[i % groupColors.length]
                });
            }
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

        if (!yScaleLabel) {
            yScaleLabel = label;
        }
        return yScaleLabel;
    },

    getSubtitle: function(precursor) {
        if (precursor === this.getMetricPropsById(this.metric).name)
            return precursor;
        else
            return precursor + ' - ' + this.getMetricPropsById(this.metric).name;
    },

    addEachCombinedPrecusorPlot: function(plotIndex, id, combinePlotData, groupColors, yAxisCount, metricProps, showLogInvalid, legendMargin, plotType, isCUSUMMean) {
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
        var scope = this;

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
                    value: this.getYScaleLabel(plotType, trendLineProps.valueConversion, metricProps.yAxisLabel1)
                },
                yRight: {
                    value: this.isMultiSeries() ? metricProps.yAxisLabel2 : undefined,
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

    addEachIndividualPrecusorPlot: function(plotIndex, id, precursorIndex, precursorInfo, metricProps, plotType, isCUSUMMean, scope) {
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

        // lines are not separated when indices are not present
        if (this.filterQCPoints && this.filterPointsLastIndex && this.filterPointsFirstIndex) {
            trendLineProps.lineColor = '#000000';
            trendLineProps.groupBy = "ReferenceRangeSeries";
        }

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

        var leftMargin = 75;
        var leftMarginOffset = this.getYAxisLeftMarginOffset(precursorInfo) + leftMargin;

        var basePlotConfig = this.getBasePlotConfig(id, precursorInfo.data, plotLegendData);
        var plotConfig = Ext4.apply(basePlotConfig, {
            margins : {
                top: 65 + this.getMaxStackedAnnotations() * 12,
                left: leftMarginOffset,
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
                    value: this.getYScaleLabel(plotType, trendLineProps.valueConversion, metricProps.yAxisLabel1),
                    color: this.isMultiSeries() ? this.getColorRange()[0] : undefined,
                    position: leftMarginOffset > 0 ? leftMarginOffset - 15 : undefined
                },
                yRight: {
                    value: this.isMultiSeries() ? metricProps.yAxisLabel2 : undefined,
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

    getYAxisLeftMarginOffset: function(precursorInfo) {
        if (precursorInfo.min === undefined || precursorInfo.max === undefined) {
            return 0;
        }

        var maxLength = Math.max(precursorInfo.min.toString().length, precursorInfo.max.toString().length);

        // maxLength of yAxis value
        // if less than 10 then the current left margin works fine
        // else add 2 pixels per digit/character
        if (maxLength < 10) {
            return 0;
        } else {
            return (maxLength - 10) * 2;
        }
    },

    // empty legend to reserve plot space for plot alignment
    getEmptyLegend: function() {
        var empty = [];
        empty.push({
            text: '',
            shape: function(){
                return 'M0,0L0,0Z';
            }
        });
        return empty;
    },

    showInPlotLegends: function () {
        return true;
    }
});