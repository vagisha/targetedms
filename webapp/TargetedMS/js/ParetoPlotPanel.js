/**
 *
 * Copyright (c) 2015-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 *
 * Created by binalpatel on 7/9/15.
 */

Ext4.define('LABKEY.targetedms.ParetoPlotPanel', {

    extend: 'LABKEY.targetedms.BaseQCPlotPanel',

    initComponent : function()
    {

        this.callParent();

        this.queryInitialQcMetrics(this.initPlot, this);
    },

    initPlot : function(){

        var guideSetDataRows = this.guideSetData.rows;
        this.guideSetIdTrainingDatesMap = {};

        //gather training start, end, and reference dates per guideSetId for Pareto Plot title
        for(var i = 0; i < guideSetDataRows.length; i++)
        {
            var key = guideSetDataRows[i].RowId;

            if(this.guideSetIdTrainingDatesMap[key] == undefined) {
                this.guideSetIdTrainingDatesMap[key] = {trainingStart :  guideSetDataRows[i].TrainingStart,
                    trainingEnd :  guideSetDataRows[i].TrainingEnd,
                    referenceEnd :  guideSetDataRows[i].ReferenceEnd};
            }
        }

        var applicableChartTypes = [];
        Ext4.each(this.metricPropArr, function(chartTypeProps)
        {
            if (Ext4.isDefined(chartTypeProps['series1Label']))
            {
                applicableChartTypes.push({
                    id: chartTypeProps['id'],
                    name: chartTypeProps['series1Label'],
                    schemaName: chartTypeProps['series1SchemaName'],
                    queryName: chartTypeProps['series1QueryName']
                });
            }

            if (Ext4.isDefined(chartTypeProps['series2Label']))
            {
                applicableChartTypes.push({
                    id: chartTypeProps['id'],
                    name: chartTypeProps['series2Label'],
                    schemaName: chartTypeProps['series2SchemaName'],
                    queryName: chartTypeProps['series2QueryName']
                });
            }
        });

        var queryCounter = applicableChartTypes.length;
        var dataRows = [];
        Ext4.each(applicableChartTypes, function(chartTypeProps)
        {
            LABKEY.Query.executeSql({
                schemaName: 'targetedms',
                sql: this.getGuideSetNonConformersSql(chartTypeProps),
                sort: 'GuideSetId',
                scope: this,
                success: function(data) {
                    dataRows = dataRows.concat(data.rows);
                    queryCounter--;

                    if (queryCounter == 0) {
                        this.queryContainerSampleFileRawGuideSetStats({dataRowsLJ: dataRows}, this.nonConformersForParetoPlot, this);
                    }
                },
                failure: this.failureHandler
            });
        }, this);
    },

    getGuideSetNonConformersSql : function(chartTypeProps)
    {
        return "SELECT stats.GuideSetId,"
            + "\n'" + chartTypeProps.id + "' AS MetricId,"
            + "\n'" + chartTypeProps.name + "' AS MetricLabel,"
            + "\nSUM(CASE WHEN exclusion.ReplicateId IS NULL AND (X.MetricValue > (stats.Mean + (3 * (CASE WHEN stats.StandardDev IS NULL THEN 0 ELSE stats.StandardDev END))) OR"
            + "\n   X.MetricValue < (stats.Mean - (3 * (CASE WHEN stats.StandardDev IS NULL THEN 0 ELSE stats.StandardDev END)))) THEN 1 ELSE 0 END) AS NonConformers"
            + "\nFROM (SELECT *, SampleFileId.AcquiredTime AS AcquiredTime,"
            + "\n  SampleFileId.FilePath AS FilePath, SampleFileId.ReplicateId AS ReplicateId"
            + "\n  FROM " + chartTypeProps.schemaName + "." + chartTypeProps.queryName + ") X"
            + "\nLEFT JOIN (SELECT DISTINCT ReplicateId FROM QCMetricExclusion WHERE MetricId IS NULL OR MetricId = " + chartTypeProps.id + ") exclusion"
            + "\nON X.ReplicateId = exclusion.ReplicateId"
            + "\nLEFT JOIN (" + this.metricGuideSetSql(chartTypeProps.id, chartTypeProps.schemaName, chartTypeProps.queryName) + ") stats"
            + "\n  ON X.SeriesLabel = stats.SeriesLabel"
            + "\n  AND ((X.AcquiredTime >= stats.TrainingStart AND X.AcquiredTime < stats.ReferenceEnd)"
            + "\n  OR (X.AcquiredTime >= stats.TrainingStart AND stats.ReferenceEnd IS NULL))"
            + "\nWHERE stats.GuideSetId IS NOT NULL"
            + "\nGROUP BY stats.GuideSetId";
    },

    getGuideSetDataObj: function(key, metricLabel, metricId, count, CUSUMpositive, CUSUMnegative, plotType)
    {
        return {
            guidesetId: key,
            metricLabel : metricLabel,
            metricId: metricId,
            count : count,
            CUSUMNegative: CUSUMnegative,
            CUSUMPositive: CUSUMpositive,
            plotType: plotType,
            percent: 0,
            trainingStart: this.guideSetIdTrainingDatesMap[key].trainingStart,
            trainingEnd: this.guideSetIdTrainingDatesMap[key].trainingEnd,
            referenceEnd: this.guideSetIdTrainingDatesMap[key].referenceEnd
        }
    },

    populateQCGuideSetMapData: function(guideSetMap, transformedOutliers, CUSUMm, CUSUMv, mR)
    {
        Ext4.iterate(guideSetMap, function(key, guideSet){
            guideSet.stats.CUSUMm = {data : []};
            guideSet.stats.CUSUMv = {data : []};
            guideSet.stats.rM = {data : []};

            var metricOutliers = transformedOutliers[key] || {},
                metricNames = Ext4.Array.clean(Ext4.Array.pluck(this.metricPropArr, 'series1Label').concat(Ext4.Array.pluck(this.metricPropArr, 'series2Label'))),
                ljMetricNames = Ext4.Array.pluck(guideSet.stats.LJ.data, 'metricLabel');

            Ext4.each(metricNames, function(metric) {
                var metricProps = this.getMetricPropsByLabel(metric),
                    metricId = metricProps['id'],
                    outliers = metricOutliers[metric] || {};

                if (CUSUMm) {
                    var positive = outliers.CUSUMmP ? outliers.CUSUMmP : 0, negative = outliers.CUSUMmN ? outliers.CUSUMmN : 0;
                    guideSet.stats.CUSUMm.data.push(this.getGuideSetDataObj(key, metric, metricId, positive + negative, positive, negative, 'CUSUMm'));
                }
                if (CUSUMv) {
                    var positive = outliers.CUSUMvP ? outliers.CUSUMvP : 0, negative = outliers.CUSUMvN ? outliers.CUSUMvN : 0;
                    guideSet.stats.CUSUMv.data.push(this.getGuideSetDataObj(key, metric, metricId, positive + negative, positive, negative, 'CUSUMv'));
                }
                if (mR) {
                    var count = outliers.mR ? outliers.mR : 0;
                    guideSet.stats.rM.data.push(this.getGuideSetDataObj(key, metric, metricId, count, null, null, 'Moving Range'));
                }

                // also add any missing metrics to the LJ data array so we have a full metric set for each plot type
                if (ljMetricNames.indexOf(metric) == -1) {
                    guideSet.stats.LJ.data.push(this.getGuideSetDataObj(key, metric, metricId, 0, null, null, 'Levey-Jennings'));
                }
            }, this);
        }, this);

        Ext4.iterate(guideSetMap, function(key, guideSet){
            Ext4.iterate(guideSet.stats, function(statsName, data){
                var maxNumNonConformers = 0;
                var dataSet = data.data;
                var totalCount = 0;

                //find total count per guidesetID
                for (var i = 0; i < dataSet.length; i++) {
                    totalCount += dataSet[i]['count'];

                    if(maxNumNonConformers < dataSet[i]['count'])
                    {
                        maxNumNonConformers = dataSet[i]['count'];
                    }
                }

                //sort by count in descending order
                var sortedDataset = dataSet.sort(function(a, b) {
                    var order = parseFloat(b.count) - parseFloat(a.count);
                    if (order != 0)
                        return order;
                    return a.metricLabel.localeCompare(b.metricLabel);
                });

                //calculate cumulative percentage on sorted data
                for(var j = 0; j < sortedDataset.length; j++) {
                    sortedDataset[j]['percent'] = (j == 0 ? 0 : sortedDataset[j-1]['percent']) + ((sortedDataset[j]['count'] / totalCount) * 100);
                }
                data.maxNumNonConformers = maxNumNonConformers;
            }, this)
        }, this);
    },

    nonConformersForParetoPlot : function(params)
    {
        if (!params.rawGuideSet || !params.rawMetricDataSet)
            return;
        var processedMetricGuides =  this.getAllProcessedGuideSets(params.rawGuideSet);
        var processedMetricDataSet = this.getAllProcessedMetricDataSets(params.rawMetricDataSet.filter(function(row) {
            return !row.IgnoreInQC;
        }));

        var metricOutlier = this.getQCPlotMetricOutliers(processedMetricGuides, processedMetricDataSet, true, true, true, true);
        var transformedOutliers = this.getMetricOutliersByFileOrGuideSetGroup(metricOutlier);

        var nonConformers = params.dataRowsLJ;

        var guideSetMap = {};
        var guideSetCount = 1;

        for (var i = 0; i < nonConformers.length; i++)
        {
            var key = nonConformers[i]['GuideSetId'];
            var count = nonConformers[i]['NonConformers'];

            if(guideSetMap[key] == undefined) {
                guideSetMap[key] = {
                    trainingStart: this.guideSetIdTrainingDatesMap[key].trainingStart,
                    trainingEnd: this.guideSetIdTrainingDatesMap[key].trainingEnd,
                    referenceEnd: this.guideSetIdTrainingDatesMap[key].referenceEnd,
                    stats: {
                        LJ: {
                            data: []
                        }
                    }
                };
            }
            guideSetMap[key].stats.LJ.data.push(this.getGuideSetDataObj(key, nonConformers[i]['MetricLabel'], nonConformers[i]['MetricId'], count, null, null, 'Levey-Jennings'));
        }

        this.populateQCGuideSetMapData(guideSetMap, transformedOutliers, true, true, true);

        for (var key in guideSetMap)
        {
            var guideSetData = guideSetMap[key];
            var id = "paretoPlot-GuideSet-"+guideSetCount;

            var title = "Training Start: " + guideSetData.trainingStart
                + (guideSetData.referenceEnd ? " - Reference End: " + guideSetData.referenceEnd : " - Training End: " + guideSetData.trainingEnd);

            var plotIdSuffix = '', plotType = "Levey-Jennings", plotWp = 'pareto-plot-wp', plotData = guideSetData.stats.LJ.data, plotMaxY = guideSetData.stats.LJ.maxNumNonConformers;
            var webpartTitleBase = "Guide Set " + guideSetCount + ' ';
            this.addEachParetoPlot(id, webpartTitleBase, plotType, plotWp, title, "ParetoPlot-Guide Set "+guideSetCount + plotIdSuffix, plotData, plotMaxY);

            plotIdSuffix = '_mR'; plotType = "Moving Range"; plotData = guideSetData.stats.rM.data; plotMaxY = guideSetData.stats.rM.maxNumNonConformers;
            this.addEachParetoPlot(id + plotIdSuffix, webpartTitleBase, plotType, plotWp, title, "ParetoPlot-Guide Set "+guideSetCount + plotIdSuffix, plotData, plotMaxY);

            plotIdSuffix = '_CUSUMm'; plotType = "Mean CUSUM"; plotData = guideSetData.stats.CUSUMm.data; plotMaxY = guideSetData.stats.CUSUMm.maxNumNonConformers;
            this.addEachParetoPlot(id + plotIdSuffix, webpartTitleBase, plotType, plotWp, title, "ParetoPlot-Guide Set "+guideSetCount + plotIdSuffix, plotData, plotMaxY);

            plotIdSuffix = '_CUSUMv'; plotType = "Variability CUSUM"; plotData = guideSetData.stats.CUSUMv.data; plotMaxY = guideSetData.stats.CUSUMv.maxNumNonConformers;
            this.addEachParetoPlot(id + plotIdSuffix, webpartTitleBase, plotType, plotWp, title, "ParetoPlot-Guide Set "+guideSetCount + plotIdSuffix, plotData, plotMaxY);

            guideSetCount++;
        }
    },

    addEachParetoPlot: function (id, wpTitle, plotType, wp, plotTitle, fileName, plotData, yAxisMax)
    {
        this.addPlotWebPartToPlotDiv(id, wpTitle, this.plotPanelDiv, wp);
        this.setPlotWidth(this.plotPanelDiv);
        this.plotPareto(id, plotData, plotTitle, yAxisMax, plotType);
        this.attachPlotExportIcons(id, id, 0, this.plotWidth - 30, 0);
    },

    plotPareto: function(id, data, title, yAxisMax, plotType)
    {
        var hoverFn = plotType.indexOf('CUSUM') > -1 ? this.plotBarHoverEvent : undefined;
        var barChart = new LABKEY.vis.Plot({
            renderTo: id,
            rendererType: 'd3',
            width: this.plotWidth - 30,
            height: 500,
            data: Ext4.Array.clone(data),
            labels: {
                main: {value: "Pareto Plot - " + plotType},
                subtitle: {value: title, color: '#555555'},
                yLeft: {value: '# Nonconformers'},
                yRight: {value: 'Cumulative Percentage'}
            },
            layers : [
                new LABKEY.vis.Layer({
                    geom: new LABKEY.vis.Geom.BarPlot({clickFn: this.plotBarClickEvent, hoverFn: hoverFn})
                }),
                new LABKEY.vis.Layer({
                    geom: new LABKEY.vis.Geom.Path({color: 'steelblue'}),
                    aes: { x: 'metricLabel', yRight: 'percent' }
                }),
                new LABKEY.vis.Layer({
                    geom: new LABKEY.vis.Geom.Point({color: 'steelblue'}),
                    aes: { x: 'metricLabel', yRight: 'percent', hoverText: function(val){return val.percent.toPrecision(4) + "%"}}
                })
            ],
            aes: {
                x: 'metricLabel',
                y: 'count'
            },
            scales : {
                x : {
                    scaleType: 'discrete',
                    tickHoverText: function(val) {
                        return val;
                    }
                },
                yLeft : {
                    domain: [0, (yAxisMax==0 ? 1 : yAxisMax)]
                },
                yRight : {
                    domain: [0, 100]
                }
            },
            margins: {
                bottom: 75
            }
        });
        barChart.render();
    },

    plotBarClickEvent : function(event, row) {
        var params = {startDate: row.trainingStart, metric: row.metricId, plotTypes: row.plotType};
        if (row.referenceEnd)
        {
            params.endDate = row.referenceEnd;
        }
        window.location = LABKEY.ActionURL.buildURL('project', 'begin', null, params);
    },

    plotBarHoverEvent : function(row) {
        var CUSUMN = row.CUSUMNegative ? row.CUSUMNegative : 0, CUSUMP = row.CUSUMPositive ? row.CUSUMPositive : 0;
        return 'CUSUM-:' + ' ' + CUSUMN + '\nCUSUM+:' + ' ' + CUSUMP + '\nTotal: ' + row.count;

    }
});