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

    initPlot : function() {

        Ext4.get(this.plotDivId).mask("Loading...");

        LABKEY.Ajax.request({
            url: LABKEY.ActionURL.buildURL('targetedms', 'GetQCMetricOutliers.api'),
            params: {sampleLimit: this.sampleLimit},
            success: this.processResponse,
            failure: LABKEY.Utils.getCallbackWrapper(this.failureHandler),
            scope: this
        });
    },

    processResponse: function(response) {
        Ext4.get(this.plotDivId).unmask();

        var parsed = JSON.parse(response.responseText);

        if (Object.keys(parsed.sampleFiles).length === 0) {
            Ext4.get(this.plotDivId).update('<div class="tiledPlotPanel">No sample files loaded yet. Import some via Skyline, AutoQC, or the Data Pipeline tab here in Panorama.</div>');
            return;
        }

        this.guideSetIdTrainingDatesMap = {};

        var guideSetDataRows = parsed.guideSets;
        for(var i = 0; i < guideSetDataRows.length; i++)
        {
            this.guideSetIdTrainingDatesMap[guideSetDataRows[i].RowId] = {
                trainingStart: guideSetDataRows[i].TrainingStart,
                trainingEnd: guideSetDataRows[i].TrainingEnd,
                referenceEnd: guideSetDataRows[i].ReferenceEnd,
                stats: {
                    CUSUMm: {count: 0, data: []},
                    CUSUMmN: {count: 0, data: []},
                    CUSUMmP: {count: 0, data: []},
                    CUSUMv: {count: 0, data: []},
                    CUSUMvN: {count: 0, data: []},
                    CUSUMvP: {count: 0, data: []},
                    movingRange: {count: 0, data: []},
                    leveyJennings: {count: 0, data: []}
                }
            };
        }

        var outliers = parsed.outliers;
        for (var j = 0; j < outliers.length; j++)
        {
            var outlier = outliers[j];

            var guideSet = this.guideSetIdTrainingDatesMap[outlier.GuideSetId];

            this.addOutlierToCounts(guideSet.stats.CUSUMm, 'CUSUMm', outlier, 'CUSUMm', true, guideSet);
            this.addOutlierToCounts(guideSet.stats.CUSUMv, 'CUSUMv', outlier, 'CUSUMv', true, guideSet);
            this.addOutlierToCounts(guideSet.stats.leveyJennings, 'LeveyJennings', outlier, 'Levey-Jennings', false, guideSet);
            this.addOutlierToCounts(guideSet.stats.movingRange, 'mR', outlier, 'Moving Range', false, guideSet);
        }

        var guideSetMap = this.guideSetIdTrainingDatesMap;

        var guideSetCount = 1;

        Ext4.iterate(guideSetMap, function(key, guideSet){
            Ext4.iterate(guideSet.stats, function(statsName, data){
                var maxOutliers = 0;
                var dataSet = data.data;
                var totalCount = 0;

                //find total count per guidesetID
                for (var i = 0; i < dataSet.length; i++) {
                    totalCount += dataSet[i]['count'];

                    if(maxOutliers < dataSet[i]['count'])
                    {
                        maxOutliers = dataSet[i]['count'];
                    }
                }

                //sort by count in descending order
                var sortedDataset = dataSet.sort(function(a, b) {
                    var order = parseFloat(b.count) - parseFloat(a.count);
                    if (order !== 0)
                        return order;
                    return a.metricLabel.localeCompare(b.metricLabel);
                });

                //calculate cumulative percentage on sorted data
                for(var j = 0; j < sortedDataset.length; j++) {
                    sortedDataset[j]['percent'] = (j == 0 ? 0 : sortedDataset[j-1]['percent']) + ((sortedDataset[j]['count'] / totalCount) * 100);
                }
                data.maxOutliers = maxOutliers;
            }, this)
        }, this);


        for (var key in guideSetMap)
        {
            var guideSetData = guideSetMap[key];
            var id = "paretoPlot-GuideSet-"+guideSetCount;

            var title = "Training Start: " + guideSetData.trainingStart
                    + (guideSetData.referenceEnd ? " - Reference End: " + guideSetData.referenceEnd : " - Training End: " + guideSetData.trainingEnd);

            var plotIdSuffix = '', plotType = "Levey-Jennings", plotWp = 'pareto-plot-wp', plotData = guideSetData.stats.leveyJennings.data, plotMaxY = guideSetData.stats.leveyJennings.maxOutliers;
            var webpartTitleBase = "Guide Set " + guideSetCount + ' ';
            this.addEachParetoPlot(id, webpartTitleBase, plotType, plotWp, title, "ParetoPlot-Guide Set "+guideSetCount + plotIdSuffix, plotData, plotMaxY);

            plotIdSuffix = '_mR'; plotType = "Moving Range"; plotData = guideSetData.stats.movingRange.data; plotMaxY = guideSetData.stats.movingRange.maxOutliers;
            this.addEachParetoPlot(id + plotIdSuffix, webpartTitleBase, plotType, plotWp, title, "ParetoPlot-Guide Set "+guideSetCount + plotIdSuffix, plotData, plotMaxY);

            plotIdSuffix = '_CUSUMm'; plotType = "Mean CUSUM"; plotData = guideSetData.stats.CUSUMm.data; plotMaxY = guideSetData.stats.CUSUMm.maxOutliers;
            this.addEachParetoPlot(id + plotIdSuffix, webpartTitleBase, plotType, plotWp, title, "ParetoPlot-Guide Set "+guideSetCount + plotIdSuffix, plotData, plotMaxY);

            plotIdSuffix = '_CUSUMv'; plotType = "Variability CUSUM"; plotData = guideSetData.stats.CUSUMv.data; plotMaxY = guideSetData.stats.CUSUMv.maxOutliers;
            this.addEachParetoPlot(id + plotIdSuffix, webpartTitleBase, plotType, plotWp, title, "ParetoPlot-Guide Set "+guideSetCount + plotIdSuffix, plotData, plotMaxY);

            guideSetCount++;
        }
    },

    addOutlierToCounts: function(stat, outlierCountProperty, outlier, plotType, isCusum, guideSet) {
        var dataElement;
        for (var i = 0; i < stat.data.length; i++) {
            if (outlier.MetricId === stat.data[i].metricId && outlier.MetricLabel === stat.data[i].metricLabel)
            {
                dataElement = stat.data[i];
                break;
            }
        }

        if (!dataElement) {
            dataElement = {
                count: 0,
                metricId: outlier.MetricId,
                metricLabel: outlier.MetricLabel,
                metricName: outlier.MetricName,
                guideSetId: outlier.GuideSetId,
                trainingStart: guideSet.trainingStart,
                referenceEnd: guideSet.referenceEnd,
                plotType: plotType,
                percent: 0
            };
            if (isCusum) {
                dataElement.CUSUMNegative = 0;
                dataElement.CUSUMPositive = 0;
            }
            stat.data.push(dataElement);
        }

        dataElement.count += outlier[outlierCountProperty];
        if (isCusum) {
            dataElement.CUSUMNegative += outlier[outlierCountProperty + 'N'];
            dataElement.CUSUMPositive += outlier[outlierCountProperty + 'P'];
        }
    },

    addEachParetoPlot: function (id, wpTitle, plotType, wp, plotTitle, fileName, plotData, yAxisMax)
    {
        this.addPlotWebPartToPlotDiv(id, wpTitle, this.plotDivId, wp);
        this.setPlotWidth(this.plotDivId);
        this.plotPareto(id, plotData, plotTitle, yAxisMax, plotType);
        this.attachPlotExportIcons(id, id, 0, this.plotWidth - 30, 0);
    },

    plotPareto: function(id, data, title, yAxisMax, plotType)
    {
        var tickValues;
        if (yAxisMax < 10) {
            tickValues = [];
            for (var i = 0; i <= yAxisMax; i++) {
                tickValues.push(i);
            }
        }
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
                yLeft: {value: '# Outliers'},
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
                    domain: [0, (yAxisMax==0 ? 1 : yAxisMax)],
                    tickValues: tickValues
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