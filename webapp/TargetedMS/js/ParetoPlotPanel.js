/**
 *
 * Copyright (c) 2015 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 *
 * Created by binalpatel on 7/9/15.
 */

Ext4.define('LABKEY.targetedms.ParetoPlotPanel', {

    extend: 'LABKEY.targetedms.BaseQCPlotPanel',

    initComponent : function() {

        this.callParent();

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
        Ext4.each(this.chartTypePropArr, function(chartTypeProps)
        {
            if (chartTypeProps.showInParetoPlot)
            {
                applicableChartTypes.push(chartTypeProps);
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
            + "\n'" + chartTypeProps.shortName + "' AS Metric,"
            + "\n'" + chartTypeProps.title + "' AS MetricLongLabel,"
            + "\n'" + (chartTypeProps.altParetoPlotClickName || chartTypeProps.name) + "' AS MetricName,"
            + "\nSUM(CASE WHEN X.Value > (stats.Mean + (3 * stats.StandardDev)) OR"
            + "\n   X.Value < (stats.Mean - (3 * stats.StandardDev)) THEN 1 ELSE 0 END) AS NonConformers"
            + "\nFROM ("
            + "\n   SELECT " + chartTypeProps.baseLkFieldKey + "PrecursorId.ModifiedSequence AS Sequence,"
            + "\n   " + chartTypeProps.baseLkFieldKey + "SampleFileId.AcquiredTime AS AcquiredTime,"
            + "\n   " + chartTypeProps.colName + " AS Value"
            + "\n   FROM " + chartTypeProps.baseTableName
            + "\n) X"
            + "\nLEFT JOIN GuideSetStats_" + chartTypeProps.name + " stats"
            + "\n  ON X.Sequence = stats.Sequence"
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
        var shortNameLongNameMap = {};

        for (var i = 0; i < nonConformers.length; i++)
        {
            var key = nonConformers[i]['GuideSetId'];
            var count = nonConformers[i]['NonConformers'];

            if(guideSetMap[key] == undefined) {
                guideSetMap[key] = {data : []};
            }

            guideSetMap[key].data.push({
                guidesetId: key,
                metric : nonConformers[i]['Metric'],
                metricLong: nonConformers[i]['MetricLongLabel'],
                metricName: nonConformers[i]['MetricName'],
                count : count,
                percent: 0,
                trainingStart: this.guideSetIdTrainingDatesMap[key].trainingStart,
                trainingEnd: this.guideSetIdTrainingDatesMap[key].trainingEnd,
                referenceEnd: this.guideSetIdTrainingDatesMap[key].referenceEnd
            });

            //store short name long name in a map for hover text
            shortNameLongNameMap[nonConformers[i]['Metric']] = nonConformers[i]['MetricLongLabel'];
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
            this.plotPareto(id, sortedDataset, title, shortNameLongNameMap, maxNumNonConformers);
            guideSetCount++;
        }
    },

    plotPareto: function(id, data, title, hoverTextMap, yAxisMax)
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
                    aes: { x: 'metric', yRight: 'percent' }
                }),
                new LABKEY.vis.Layer({
                    geom: new LABKEY.vis.Geom.Point({color: 'steelblue'}),
                    aes: { x: 'metric', yRight: 'percent', hoverText: function(val){return val.percent.toPrecision(4) + "%"}}
                })
            ],
            aes: { x: 'metric', y: 'count' },
            scales : {
                x : { scaleType: 'discrete', tickHoverText: function(val) { return hoverTextMap[val]}},
                yLeft : { domain: [0, (yAxisMax==0 ? 1 : yAxisMax)]},
                yRight : { domain: [0, 100] },

            }
        });
        barChart.render();
    },


    plotBarClickEvent : function(event, row) {
        var params = {startDate: row.trainingStart, metric: row.metricName};
        if (row.referenceEnd)
        {
            params.endDate = row.referenceEnd;
        }
        window.location = LABKEY.ActionURL.buildURL('project', 'begin', null, params);
    }

});