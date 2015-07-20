/**
 *
 * Copyright (c) 2015 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 *
 * Created by binalpatel on 7/9/15.
 */

Ext4.define('LABKEY.targetedms.ParetoPlotPanel', {

    extend: 'Ext.panel.Panel',

    initComponent : function() {

        this.callParent();

        this.plotPanelDiv = 'tiledPlotPanel';

        var guideSetDataRows = this.guideSetData.rows;
        this.guideSetIdTrainingDatesMap = {};

        //gather training start, end, and reference dates per guideSetId for Pareto Plot title
        for(var i = 0; i < guideSetDataRows.length; i++)
        {
            var key = guideSetDataRows[i].RowId;

            if(this.guideSetIdTrainingDatesMap[key] == undefined)
            {
                this.guideSetIdTrainingDatesMap[key] = {data : []};
            }

            this.guideSetIdTrainingDatesMap[key].data.push({trainingStart :  guideSetDataRows[i].TrainingStart , trainingEnd :  guideSetDataRows[i].TrainingEnd, referenceEnd :  guideSetDataRows[i].ReferenceEnd});
        }

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
        var guideSetCount = 1;

        for(var i = 0; i < nonConformers.length; i++)
        {
            var key = nonConformers[i]['GuideSetId'];
            var count = nonConformers[i]['NonConformers'];
            var trainingDatesData = this.guideSetIdTrainingDatesMap[key].data;

            if(guideSetMap[key] == undefined)
            {
                guideSetMap[key] = {data : []};
            }
            guideSetMap[key].data.push({guidesetId: key, metric : nonConformers[i]['Metric'],
                metricLong: nonConformers[i]['MetricLongLabel'], count : count, percent: 0,
                trainingStart: trainingDatesData[0].trainingStart, trainingEnd: trainingDatesData[0].trainingEnd,
                referenceEnd: trainingDatesData[0].referenceEnd});
        }

        for(var key in guideSetMap)
        {
            var dataSet = guideSetMap[key].data;
            var totalCount = 0;

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

            var title = "'Training Start: " + sortedDataset[0].trainingStart;
            title += (sortedDataset[0].referenceEnd) ? (" - Reference End: " + sortedDataset[0].referenceEnd) : (" - Training End: " + sortedDataset[0].trainingEnd);
            title += "'";
            var id = "paretoPlot-GuideSetId-" + key;

            this.addPlotWebPartToPlotDiv(id, "Guide Set " + guideSetCount);
            this.createExportToPDFButton(id, title);
            this.plotPareto(id, sortedDataset, title);
            guideSetCount++;
        }
    },

    plotPareto: function(id, data, title)
    {
        var barChart = new LABKEY.vis.Plot({
            renderTo: id,
            rendererType: 'd3',
            width: 1000,
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
                    aes: { x: 'metric', yRight: 'percent' }
                })
            ],
            aes: { x: 'metric', y: 'count' },
            scales : {
                x : { scaleType: 'discrete' },
                yLeft : { domain: [0, null] },
                yRight : { domain: [0, 100] }
            }
        });
        Ext4.get(this.plotPanelDiv).setWidth(1500);
        barChart.render();
    },

    createExportToPDFButton : function(id, datesTitle) {
        new Ext4.Button({
            renderTo: id+"-exportToPDFbutton",
            svgDivId: id,
            icon: LABKEY.contextPath + "/_icons/pdf.gif",
            tooltip: "Export PDF of this plot",
            handler: function(btn) {
                var svgEls = Ext4.get(btn.svgDivId).select('svg');
                var title = "Pareto Plot for " + datesTitle;
                var svgStr = LABKEY.vis.SVGConverter.svgToStr(svgEls.elements[0]);
                svgStr = svgStr.replace(/visibility="hidden"/g, 'visibility="visible"');
                LABKEY.vis.SVGConverter.convert(svgStr, LABKEY.vis.SVGConverter.FORMAT_PDF, title);
            },
            scope: this
        });
    },

    addPlotWebPartToPlotDiv : function(id, title) {
        Ext4.get(this.plotPanelDiv).insertHtml('beforeEnd', '<br/>' +
                '<table class="labkey-wp pareto-plot-wp">' +
                ' <tr class="labkey-wp-header">' +
                '     <th class="labkey-wp-title-left">' +
                '        <span class="labkey-wp-title-text pareto-plot-wp-title">' + Ext4.util.Format.htmlEncode(title) +
                '           <div style="float:right" id="' + id + '-exportToPDFbutton"></div>' +
                '        </span>' +
                '     </th>' +
                ' </tr><tr>' +
                '     <td class="labkey-wp-body"><div id="' + id + '"></div></</td>' +
                ' </tr>' +
                '</table>'
        );
    },

    plotBarClickEvent : function(event, row) {
        window.location = LABKEY.ActionURL.buildURL('project', 'begin', null, {startDate: row.trainingStart, endDate: row.trainingEnd , metric: row.metricLong});
    }
});