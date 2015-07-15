/**
 *
 * Copyright (c) 2011-2015 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 *
 * Created by binalpatel on 7/9/15.
 */

Ext4.define('LABKEY.targetedms.ParetoPlotPanel', {

    extend: 'Ext.panel.Panel',

    initComponent : function() {

        this.callParent();

                var nonConformerSql = "SELECT * FROM GuideSetNonConformers";
                LABKEY.Query.executeSql({
                    schemaName: 'targetedms',
                    sql: nonConformerSql,
                    scope: this,
                    success: this.nonConformersForParetoPlot,
                    failure: this.failureHandler
                });
    },

    nonConformersForParetoPlot : function(data)
    {
        var nonConformers = data.rows;
        var guideSetMap = {};
        this.maxCount = 0;

        for(var i = 0; i < nonConformers.length; i++)
        {
            var guideSetId = nonConformers[i]['GuideSetId'];
            var chartType = nonConformers[i]['metric'];
            var key = guideSetId;
            var count = nonConformers[i]['NonConformers'];

            if(guideSetMap[key] == undefined)
            {
                guideSetMap[key] = {data : []};
            }
            guideSetMap[key].data.push({metric: nonConformers[i]['Metric'], count : count, percent: 0});

            if(this.maxCount < count)
            {
                this.maxCount = count;
            }
        }

        for(var key in guideSetMap)
        {
            var dataSet = guideSetMap[key].data;

            var totalCount = 0;
            var cumulativePercent = 0;

            //find total count per guidesetID
            for(var i = 0; i < dataSet.length; i++)
            {
                totalCount += dataSet[i]['count'];
            }

            //sort by count in descending order
            var sortedDataset = dataSet.sort(function(a, b) {
                return parseFloat(b.count) - parseFloat(a.count);
            });


            //calculate cumulative percentage on sorted data
            for(var j = 0; j < sortedDataset.length; j++)
            {
                sortedDataset[j]['percent'] = (j == 0 ? 0 : sortedDataset[j-1]['percent']) + (((sortedDataset[j]['count'])/totalCount) * 100);
            }

            this.plotPareto(sortedDataset);

        }
    },

    plotPareto: function(data)
    {
        var barChart = new LABKEY.vis.Plot({
            renderTo: 'paretoPlotDiv',
            rendererType: 'd3',
            width: 900,
            height: 500,
            data: Ext4.Array.clone(data),
            labels: {
                main: {value: 'Example Pareto Plot'},
                yLeft: {value: '# Nonconformers'},
                yRight: {value: 'Cumulative Percentage'}
            },
            layers : [
                new LABKEY.vis.Layer({
                    geom: new LABKEY.vis.Geom.BarPlot({})
                }),
                new LABKEY.vis.Layer({
                    geom: new LABKEY.vis.Geom.Path({color: 'steelblue'}),
                    aes: { x: 'metric', yRight: 'percent' }
                }),
                new LABKEY.vis.Layer({
                    geom: new LABKEY.vis.Geom.Point({color: 'steelblue'}),
                    aes: { x: 'metric', yRight: 'percent' }
                })
            ],
            aes: { x: 'metric', y: 'count' },
            scales : {
                x : { scaleType: 'discrete' },
                yLeft : { domain: [0, this.maxCount] },
                yRight : { domain: [0, 100] }
            }
        });
        barChart.render();
    }
});