/*
 * Copyright (c) 2015-2016 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define('LABKEY.targetedms.BaseQCPlotPanel', {

    extend: 'Ext.panel.Panel',

    // properties used for the various data queries based on chart metric type
    metricPropArr: [],

    metricGuideSetSql : function(schema1Name, query1Name, schema2Name, query2Name)
    {
        var includeCalc = !Ext4.isDefined(schema2Name) && !Ext4.isDefined(query2Name),
            selectCols = 'SampleFileId, SampleFileId.AcquiredTime, SeriesLabel' + (includeCalc ? ', MetricValue' : ''),
            series1SQL = 'SELECT ' + selectCols + ' FROM '+ schema1Name + '.' + query1Name,
            series2SQL = includeCalc ? '' : ' UNION SELECT ' + selectCols + ' FROM '+ schema2Name + '.' + query2Name;

        return 'SELECT gs.RowId AS GuideSetId, gs.TrainingStart, gs.TrainingEnd, gs.ReferenceEnd, p.SeriesLabel, '
            + '\nCOUNT(p.SampleFileId) AS NumRecords, '
            + '\n' + (includeCalc ? 'AVG(p.MetricValue)' : 'NULL') + ' AS Mean, '
            + '\n' + (includeCalc ? 'STDDEV(p.MetricValue)' : 'NULL') + ' AS StandardDev '
            + '\nFROM guideset gs'
            + '\nLEFT JOIN (' + series1SQL + series2SQL + ') as p'
            + '\n  ON p.AcquiredTime >= gs.TrainingStart AND p.AcquiredTime <= gs.TrainingEnd'
            + '\nGROUP BY gs.RowId, gs.TrainingStart, gs.TrainingEnd, gs.ReferenceEnd, p.SeriesLabel';
    },

    metricGuideSetRawSql : function(schema1Name, query1Name, schema2Name, query2Name)
    {
        var includeCalc = !Ext4.isDefined(schema2Name) && !Ext4.isDefined(query2Name),
                selectCols = 'SampleFileId, SampleFileId.AcquiredTime, SeriesLabel' + (includeCalc ? ', MetricValue' : ''),
                series1SQL = 'SELECT ' + selectCols + ' FROM '+ schema1Name + '.' + query1Name,
                series2SQL = includeCalc ? '' : ' UNION SELECT ' + selectCols + ' FROM '+ schema2Name + '.' + query2Name;

        return 'SELECT gs.RowId AS GuideSetId, gs.TrainingStart, gs.TrainingEnd, gs.ReferenceEnd, p.SeriesLabel, p.AcquiredTime ' + (includeCalc ? ', p.MetricValue' : '')
                + '\nFROM guideset gs'
                + '\nLEFT JOIN (' + series1SQL + series2SQL + ') as p'
                + '\n  ON p.AcquiredTime >= gs.TrainingStart AND p.AcquiredTime <= gs.TrainingEnd'
                + '\n ORDER BY GuideSetId, p.SeriesLabel, p.AcquiredTime'; //it's important that record is sorted by AcquiredTime asc as ordering is critical in calculating mR
    },

    getGuideSetAvgMRs : function(data)
    {
        var movingRangeMap = {}, guideSetDataMap = {};
        Ext4.each(data.rows, function(row) {
            var guideSetId = row['GuideSetId'];
            if (!guideSetDataMap[guideSetId])
            {
                guideSetDataMap[guideSetId] = {
                    Series: {}
                };
            }

            var seriesLabel = row['SeriesLabel'];
            if (!guideSetDataMap[guideSetId].Series[seriesLabel])
            {
                guideSetDataMap[guideSetId].Series[seriesLabel] = {
                    MetricValues: []
                };
            }
            guideSetDataMap[guideSetId].Series[seriesLabel].MetricValues.push(row.MetricValue); //data.rows should already be sorted by AcquiredTime
        }, this);

        Ext4.iterate(guideSetDataMap, function(guideSetId, setVal){
            movingRangeMap[guideSetId] = {Series: {}};
            var series = setVal.Series;
            Ext4.iterate(series, function(seriesLabel, seriesVal){
                var metricVals = seriesVal.MetricValues;
                if (metricVals == null || metricVals.length == 0)
                    return;
                var metricMovingRanges = LABKEY.vis.Stat.getMovingRanges(metricVals);
                movingRangeMap[guideSetId].Series[seriesLabel] = LABKEY.vis.Stat.getMean(metricMovingRanges);
            });
        });
        return movingRangeMap;
    },

    reprocessPlotData: function() {
        var plotDataMap = {};
        for (var i = 0; i < this.plotDataRows.length; i++)
        {
            var row = this.plotDataRows[i];
            if (!plotDataMap[row['SeriesLabel']])
            {
                plotDataMap[row['SeriesLabel']] = {
                    Series: {}
                };
            }
            if (!plotDataMap[row['SeriesLabel']].Series[row['SeriesType']])
            {
                plotDataMap[row['SeriesLabel']].Series[row['SeriesType']] = {
                    Rows: [],
                    MetricValues: []
                };
            }
            plotDataMap[row['SeriesLabel']].Series[row['SeriesType']].MetricValues.push(row.MetricValue);
            plotDataMap[row['SeriesLabel']].Series[row['SeriesType']].Rows.push(row);
        }
        this.useRawData = true; //TODO
        if (this.useRawData) //TODO
        {
            Ext4.iterate(plotDataMap, function(seriesLabel, seriesVal){
                Ext4.iterate(seriesVal.Series, function(seriesType, seriesTypeObj){
                    //TODO adjust per actual plot types selected
                    var mRs = LABKEY.vis.Stat.getMovingRanges(seriesTypeObj.MetricValues);
                    var positiveCUSUMm = LABKEY.vis.Stat.getCUSUM(seriesTypeObj.MetricValues);
                    var negativeCUSUMm = LABKEY.vis.Stat.getCUSUM(seriesTypeObj.MetricValues, true);
                    var positiveCUSUMv = LABKEY.vis.Stat.getCUSUM(seriesTypeObj.MetricValues, false, true);
                    var negativeCUSUMv = LABKEY.vis.Stat.getCUSUM(seriesTypeObj.MetricValues, true, true);
                    for (var i = 0; i < seriesTypeObj.Rows.length; i++)
                    {
                        var row = seriesTypeObj.Rows[i];
                        row['MR'] = mRs[i];
                        row['CUSUMmP'] = positiveCUSUMm[i];
                        row['CUSUMmN'] = negativeCUSUMm[i];
                        row['CUSUMvP'] = positiveCUSUMv[i];
                        row['CUSUMvN'] = negativeCUSUMv[i];
                    }
                })
            });
        }
        return plotDataMap;
    },

    addPlotWebPartToPlotDiv: function (id, title, div, wp)
    {
        Ext4.get(div).insertHtml('beforeEnd', '<br/>' +
                '<table class="labkey-wp ' + wp + '">' +
                ' <tr class="labkey-wp-header">' +
                '     <th class="labkey-wp-title-left">' +
                '        <span class="labkey-wp-title-text ' +  wp + '-title">'+ Ext4.util.Format.htmlEncode(title) +
                '           <div class="plot-export-btn" id="' + id + '-exportToPDFbutton"></div>' +
                '        </span>' +
                '     </th>' +
                ' </tr><tr>' +
                '     <td class="labkey-wp-body"><div id="' + id + '"></div></</td>' +
                ' </tr>' +
                '</table>'
        );
    },

    setPlotWidth: function (div)
    {
        if (this.plotWidth == null)
        {
            // set the width of the plot webparts based on the first labkey-wp-body element
            this.plotWidth = 900;
            var wp = document.querySelector('.labkey-wp-body');
            if (wp && (wp.clientWidth - 20) > this.plotWidth)
            {
                this.plotWidth = wp.clientWidth - 20;
            }

            Ext4.get(div).setWidth(this.plotWidth);
        }
    },

    createExportToPDFButton: function (id, title, filename)
    {
        new Ext4.Button({
            renderTo: id + "-exportToPDFbutton",
            svgDivId: id,
            icon: LABKEY.contextPath + "/_icons/pdf.gif",
            tooltip: "Export PDF of this plot",
            handler: function (btn)
            {
                LABKEY.vis.SVGConverter.convert(this.getExportSVGStr(btn), LABKEY.vis.SVGConverter.FORMAT_PDF, filename);
            },
            scope: this
        });
    },

    createExportPlotToPDFButton: function (id, title, filename, index)
    {
        new Ext4.Button({
            renderTo: id + "-exportToPDFbutton",
            svgDivId: id,
            icon: LABKEY.contextPath + "/_icons/pdf.gif",
            tooltip: {
                text: title,
                mouseOffset: [-250,0]
            },
            margin: '0, 10, 0 10',
            plotIndex: index,
            handler: function (btn)
            {
                LABKEY.vis.SVGConverter.convert(this.getExportSVGStr(btn), LABKEY.vis.SVGConverter.FORMAT_PDF, filename);
            },
            scope: this
        });
    },

    getExportSVGStr: function(btn)
    {
        var svgEls = Ext4.get(btn.svgDivId).select('svg');
        var index = 0;
        if (btn.plotIndex)
            index = btn.plotIndex;
        var svgStr = LABKEY.vis.SVGConverter.svgToStr(svgEls.elements[index]);
        svgStr = svgStr.replace(/visibility="hidden"/g, 'visibility="visible"');
        return svgStr;
    },

    failureHandler: function(response) {
        if (response.message) {
            Ext4.get(this.plotDivId).update("<span>" + response.message +"</span>");
        }
        else {
            Ext4.get(this.plotDivId).update("<span class='labkey-error'>Error: " + response.exception + "</span>");
        }

        Ext4.get(this.plotDivId).unmask();
    },

    queryInitialQcMetrics : function(successCallback,callbackScope) {
        LABKEY.Ajax.request({
            url: LABKEY.ActionURL.buildURL('targetedms', 'GetQCMetricConfigurations.api'),
            method: 'GET',
            success: function(response) {
                this.metricPropArr = Ext4.JSON.decode(response.responseText).configurations;
                successCallback.call(callbackScope);
            },
            failure: LABKEY.Utils.getCallbackWrapper(function(response) {
                this.failureHandler(response);
            }, null, true),
            scope: this
        });
    }
});





