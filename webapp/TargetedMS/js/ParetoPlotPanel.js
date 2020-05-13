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

        var guideSets = parsed.guideSets;

        Ext4.each(guideSets, function(guideSet) {
            guideSet.stats = {
                CUSUMm: {count: 0, data: []},
                CUSUMv: {count: 0, data: []},
                mR: {count: 0, data: []},
                LeveyJennings: {count: 0, data: []}
            };

            Ext4.iterate(guideSet.MetricCounts, function(metricName, data) {
                this.addOutlierToCounts(guideSet, data, metricName,'CUSUMm', 'CUSUMm', true);
                this.addOutlierToCounts(guideSet, data, metricName, 'CUSUMv', 'CUSUMv', true);
                this.addOutlierToCounts(guideSet, data, metricName, 'mR', 'Moving Range', true);
                this.addOutlierToCounts(guideSet, data, metricName, 'LeveyJennings', 'Levey-Jennings', true);
            }, this);

            Ext4.iterate(guideSet.stats, function(outlierType, data) {
                var dataSet = data.data;

                var totalCount = 0;
                var maxOutliers = 0;

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
                    var order = b.count - a.count;
                    if (order !== 0)
                        return order;
                    return a.metricLabel.localeCompare(b.metricLabel);
                });

                //calculate cumulative percentage on sorted data
                for(var j = 0; j < sortedDataset.length; j++) {
                    sortedDataset[j].percent = (j == 0 ? 0 : sortedDataset[j-1].percent) + ((sortedDataset[j].count / totalCount) * 100);
                }
                data.maxOutliers = maxOutliers;
            }, this)
        }, this);


        var guideSetCount = 1;
        Ext4.each(guideSets, function(guideSetData) {
            var id = "paretoPlot-GuideSet-"+guideSetCount;

            var title = "Training Start: " + guideSetData.TrainingStart
                    + (guideSetData.ReferenceEnd ? " - Reference End: " + guideSetData.ReferenceEnd : " - Training End: " + guideSetData.TrainingEnd);

            var plotIdSuffix = '', plotType = "Levey-Jennings", plotWp = 'pareto-plot-wp', plotData = guideSetData.stats.LeveyJennings.data, plotMaxY = guideSetData.stats.LeveyJennings.maxOutliers;
            var webpartTitleBase = "Guide Set " + guideSetCount + ' ';
            this.addEachParetoPlot(id, webpartTitleBase, plotType, plotWp, title, "ParetoPlot-Guide Set "+guideSetCount + plotIdSuffix, plotData, plotMaxY);

            plotIdSuffix = '_mR'; plotType = "Moving Range"; plotData = guideSetData.stats.mR.data; plotMaxY = guideSetData.stats.mR.maxOutliers;
            this.addEachParetoPlot(id + plotIdSuffix, webpartTitleBase, plotType, plotWp, title, "ParetoPlot-Guide Set "+guideSetCount + plotIdSuffix, plotData, plotMaxY);

            plotIdSuffix = '_CUSUMm'; plotType = "Mean CUSUM"; plotData = guideSetData.stats.CUSUMm.data; plotMaxY = guideSetData.stats.CUSUMm.maxOutliers;
            this.addEachParetoPlot(id + plotIdSuffix, webpartTitleBase, plotType, plotWp, title, "ParetoPlot-Guide Set "+guideSetCount + plotIdSuffix, plotData, plotMaxY);

            plotIdSuffix = '_CUSUMv'; plotType = "Variability CUSUM"; plotData = guideSetData.stats.CUSUMv.data; plotMaxY = guideSetData.stats.CUSUMv.maxOutliers;
            this.addEachParetoPlot(id + plotIdSuffix, webpartTitleBase, plotType, plotWp, title, "ParetoPlot-Guide Set "+guideSetCount + plotIdSuffix, plotData, plotMaxY);

            guideSetCount++;
        }, this);
    },

    addOutlierToCounts: function(guideSet, data, metricName, propertyName, plotTypeParamValue, isCusum) {
        var count = data[propertyName];
        guideSet.stats[propertyName].count += count;
        var newData = {
            metricLabel: metricName,
            count: count,
            metricId: data.MetricId,
            TrainingStart: guideSet.TrainingStart,
            ReferenceEnd: guideSet.ReferenceEnd,
            plotType: plotTypeParamValue
        };
        if (isCusum) {
            newData.CUSUMNegative = data[propertyName + 'N'];
            newData.CUSUMPositive = data[propertyName + 'P'];
        }

        guideSet.stats[propertyName].data.push(newData);

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
        var params = {startDate: row.TrainingStart, metric: row.metricId, plotTypes: row.plotType};
        if (row.ReferenceEnd)
        {
            params.endDate = row.ReferenceEnd;
        }
        window.location = LABKEY.ActionURL.buildURL('project', 'begin', null, params);
    },

    plotBarHoverEvent : function(row) {
        var CUSUMN = row.CUSUMNegative ? row.CUSUMNegative : 0, CUSUMP = row.CUSUMPositive ? row.CUSUMPositive : 0;
        return 'CUSUM-:' + ' ' + CUSUMN + '\nCUSUM+:' + ' ' + CUSUMP + '\nTotal: ' + row.count;

    }
});