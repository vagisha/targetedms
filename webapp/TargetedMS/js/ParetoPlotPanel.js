/**
 *
 * Copyright (c) 2015-2016 LabKey Corporation
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

                    if (queryCounter == 0)
                    {
                        this.nonConformersForParetoPlot(dataRows);
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
            + "\nSUM(CASE WHEN X.MetricValue > (stats.Mean + (3 * stats.StandardDev)) OR"
            + "\n   X.MetricValue < (stats.Mean - (3 * stats.StandardDev)) THEN 1 ELSE 0 END) AS NonConformers"
            + "\nFROM (SELECT *, SampleFileId.AcquiredTime AS AcquiredTime, SampleFileId.FilePath AS FilePath"
            + "\n  FROM " + chartTypeProps.schemaName + "." + chartTypeProps.queryName + ") X"
            + "\nLEFT JOIN (" + this.metricGuideSetSql(chartTypeProps.schemaName, chartTypeProps.queryName) + ") stats"
            + "\n  ON X.SeriesLabel = stats.SeriesLabel"
            + "\n  AND ((X.AcquiredTime >= stats.TrainingStart AND X.AcquiredTime < stats.ReferenceEnd)"
            + "\n  OR (X.AcquiredTime >= stats.TrainingStart AND stats.ReferenceEnd IS NULL))"
            + "\nWHERE stats.GuideSetId IS NOT NULL"
            + "\nGROUP BY stats.GuideSetId";
    },

    nonConformersForParetoPlot : function(dataRows)
    {
        var nonConformers = dataRows;
        var guideSetMap = {};
        var guideSetCount = 1;

        for (var i = 0; i < nonConformers.length; i++)
        {
            var key = nonConformers[i]['GuideSetId'];
            var count = nonConformers[i]['NonConformers'];

            if(guideSetMap[key] == undefined) {
                guideSetMap[key] = {data : []};
            }

            guideSetMap[key].data.push({
                guidesetId: key,
                metricLabel : nonConformers[i]['MetricLabel'],
                metricId: nonConformers[i]['MetricId'],
                count : count,
                percent: 0,
                trainingStart: this.guideSetIdTrainingDatesMap[key].trainingStart,
                trainingEnd: this.guideSetIdTrainingDatesMap[key].trainingEnd,
                referenceEnd: this.guideSetIdTrainingDatesMap[key].referenceEnd
            });
        }

        for (var key in guideSetMap)
        {
            var maxNumNonConformers = 0;
            var dataSet = guideSetMap[key].data;
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
                return parseFloat(b.count) - parseFloat(a.count);
            });

            //calculate cumulative percentage on sorted data
            for(var j = 0; j < sortedDataset.length; j++) {
                sortedDataset[j]['percent'] = (j == 0 ? 0 : sortedDataset[j-1]['percent']) + ((sortedDataset[j]['count'] / totalCount) * 100);
            }

            var title = "'Training Start: " + sortedDataset[0].trainingStart;
            title += (sortedDataset[0].referenceEnd) ? (" - Reference End: " + sortedDataset[0].referenceEnd) : (" - Training End: " + sortedDataset[0].trainingEnd);
            title += "'";
            var id = "paretoPlot-GuideSet-"+guideSetCount;

            this.addPlotWebPartToPlotDiv(id, "Guide Set " + guideSetCount, this.plotPanelDiv, 'pareto-plot-wp');
            this.setPlotWidth(this.plotPanelDiv);
            this.createExportToPDFButton(id, title, "ParetoPlot-Guide Set "+guideSetCount);
            this.plotPareto(id, sortedDataset, title, maxNumNonConformers);
            guideSetCount++;
        }
    },

    plotPareto: function(id, data, title, yAxisMax)
    {
        var barChart = new LABKEY.vis.Plot({
            renderTo: id,
            rendererType: 'd3',
            width: this.plotWidth - 30,
            height: 500,
            data: Ext4.Array.clone(data),
            labels: {
                main: {value: "Pareto Plot for " + title},
                yLeft: {value: '# Nonconformers'},
                yRight: {value: 'Cumulative Percentage'}
            },
            layers : [
                new LABKEY.vis.Layer({
                    geom: new LABKEY.vis.Geom.BarPlot({clickFn: this.plotBarClickEvent})
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
        var params = {startDate: row.trainingStart, metric: row.metricId};
        if (row.referenceEnd)
        {
            params.endDate = row.referenceEnd;
        }
        window.location = LABKEY.ActionURL.buildURL('project', 'begin', null, params);
    }

});