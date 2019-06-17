/*
 * Copyright (c) 2015-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define('LABKEY.targetedms.BaseQCPlotPanel', {

    extend: 'Ext.panel.Panel',

    // properties used for the various data queries based on chart metric type
    metricPropArr: [],

    getMetricPropsById: function(id) {
        for (var i = 0; i < this.metricPropArr.length; i++) {
            if (this.metricPropArr[i].id == id) {
                return this.metricPropArr[i];
            }
        }
        return undefined;
    },

    getMetricPropsByLabel: function(label) {
        for (var i = 0; i < this.metricPropArr.length; i++) {
            if (this.metricPropArr[i].name == label || this.metricPropArr[i].series1Label == label || this.metricPropArr[i].series2Label == label) {
                return this.metricPropArr[i];
            }
        }
        return undefined;
    },

    isMultiSeries : function(metricId)
    {
        var metric = Ext4.isNumber(this.metric) ? this.metric : metricId;
        if (Ext4.isNumber(metric))
        {
            var metricProps = this.getMetricPropsById(metric);
            if(metricProps) {
                return Ext4.isDefined(metricProps.series2SchemaName) && Ext4.isDefined(metricProps.series2QueryName);
            }
        }
        return false;
    },

    getExclusionWhereSql : function(metricId)
    {
        return ' WHERE SampleFileId.ReplicateId NOT IN '
            + '(SELECT ReplicateId FROM QCMetricExclusion WHERE MetricId IS NULL OR MetricId = ' + metricId + ')';
    },

    includeAllValuesSql : function(schema, table, series, exclusion) {
        return '\nUNION'
                + '\nSELECT NULL AS GuideSetId, MIN(SampleFileId.AcquiredTime) AS TrainingStart, MAX(SampleFileId.AcquiredTime) AS TrainingEnd,'
                + '\nNULL AS ReferenceEnd, SeriesLabel, \'' + series + '\' AS SeriesType, COUNT(SampleFileId) AS NumRecords, AVG(MetricValue) AS Mean, STDDEV(MetricValue) AS StandardDev'
                + '\nFROM '+ schema + '.' + table
                + exclusion
                + '\nGROUP BY SeriesLabel';
    },

    metricGuideSetSql : function(id, schema1Name, query1Name, schema2Name, query2Name, includeAllValues)
    {
            var includeSeries2 = Ext4.isDefined(schema2Name) && Ext4.isDefined(query2Name),
            selectCols = 'SampleFileId, SampleFileId.AcquiredTime, SeriesLabel, MetricValue',
            series1SQL = 'SELECT \'series1\' AS SeriesType, ' + selectCols + ' FROM '+ schema1Name + '.' + query1Name,
            series2SQL = !includeSeries2 ? '' : ' UNION SELECT \'series2\' AS SeriesType, ' + selectCols + ' FROM '+ schema2Name + '.' + query2Name,
            exclusionWhereSQL = this.getExclusionWhereSql(id);

        return 'SELECT gs.RowId AS GuideSetId, gs.TrainingStart, gs.TrainingEnd, gs.ReferenceEnd, p.SeriesLabel, p.SeriesType, '
            + '\nCOUNT(p.SampleFileId) AS NumRecords, '
            + '\nAVG(p.MetricValue) AS Mean, '
            + '\nSTDDEV(p.MetricValue) AS StandardDev '
            + '\nFROM guideset gs'
            + '\nLEFT JOIN (' + series1SQL + series2SQL + exclusionWhereSQL + ') as p'
            + '\n  ON p.AcquiredTime >= gs.TrainingStart AND p.AcquiredTime <= gs.TrainingEnd'
            + '\nGROUP BY gs.RowId, gs.TrainingStart, gs.TrainingEnd, gs.ReferenceEnd, p.SeriesLabel, p.SeriesType'
            + (!includeAllValues ? '' : (this.includeAllValuesSql(schema1Name, query1Name, 'series1', exclusionWhereSQL)
            + (includeSeries2 ? this.includeAllValuesSql(schema2Name, query2Name, 'series2', exclusionWhereSQL) : '')));
    },

    metricGuideSetRawSql : function(id, schema1Name, query1Name, schema2Name, query2Name, includeAllValues, series)
    {
            var includeSeries2 = Ext4.isDefined(schema2Name) && Ext4.isDefined(query2Name),
            selectCols = 'SampleFileId, SampleFileId.AcquiredTime, SeriesLabel, MetricValue',
            series1SQL = 'SELECT \'' + (series ? series : 'series1') + '\' AS SeriesType, ' + selectCols + ' FROM '+ schema1Name + '.' + query1Name,
            series2SQL = !includeSeries2 ? '' : ' UNION SELECT \'series2\' AS SeriesType, ' + selectCols + ' FROM '+ schema2Name + '.' + query2Name,
            exclusionWhereSQL = this.getExclusionWhereSql(id);

        return 'SELECT gs.RowId AS GuideSetId, gs.TrainingStart, gs.TrainingEnd, gs.ReferenceEnd, p.SeriesLabel, p.AcquiredTime, p.MetricValue, p.SeriesType'
                + '\nFROM guideset gs'
                + (includeAllValues ? '\nFULL' : '\nLEFT') + ' JOIN (' + series1SQL + series2SQL + exclusionWhereSQL + ') as p'
                + '\n  ON p.AcquiredTime >= gs.TrainingStart AND p.AcquiredTime <= gs.TrainingEnd'
                + '\n ORDER BY GuideSetId, p.SeriesLabel, p.AcquiredTime'; //it's important that record is sorted by AcquiredTime asc as ordering is critical in calculating mR
    },

    getEachSeriesTypePlotDataSql: function (type, metricProps, whereClause, MetricType)
    {
        var schema = metricProps[type + 'SchemaName'],
            query = metricProps[type + 'QueryName'],
            sql = "SELECT '" + type + "' AS SeriesType, X.SampleFile, ";

        if (MetricType) {
            sql +=  "'" + MetricType + "'"  + " AS MetricType, ";
        }

        sql += "\nX.PrecursorId, X.PrecursorChromInfoId, X.SeriesLabel, X.DataType, X.mz, X.AcquiredTime,"
                + "\nX.FilePath, X.MetricValue, x.ReplicateId, gs.RowId AS GuideSetId,"
                + "\nCASE WHEN (exclusion.ReplicateId IS NOT NULL) THEN TRUE ELSE FALSE END AS IgnoreInQC,"
                + "\nCASE WHEN (X.AcquiredTime >= gs.TrainingStart AND X.AcquiredTime <= gs.TrainingEnd) THEN TRUE ELSE FALSE END AS InGuideSetTrainingRange"
                + "\nFROM (SELECT *, SampleFileId.AcquiredTime AS AcquiredTime, SampleFileId.FilePath AS FilePath,"
                + "\n      SampleFileId.SampleName AS SampleFile, SampleFileId.ReplicateId AS ReplicateId"
                + "\n      FROM " + schema + '.' + query + whereClause + ") X "
                + "\nLEFT JOIN (SELECT DISTINCT ReplicateId FROM QCMetricExclusion WHERE MetricId IS NULL OR MetricId = " + metricProps.id + ") exclusion"
                + "\nON X.ReplicateId = exclusion.ReplicateId"
                + "\nLEFT JOIN guideset gs"
                + "\nON ((X.AcquiredTime >= gs.TrainingStart AND X.AcquiredTime < gs.ReferenceEnd) OR (X.AcquiredTime >= gs.TrainingStart AND gs.ReferenceEnd IS NULL))"
                + "\nORDER BY X.SeriesLabel, SeriesType, X.AcquiredTime"; //it's important that record is sorted by AcquiredTime asc as ordering is critical in calculating mR and CUSUM
        return '(' + sql + ')';
    },

    getSeriesTypePlotDataSql: function(seriesTypes, metricProps, whereClause, MetricType)
    {
        var sql = "", sep = "";
        Ext4.each(seriesTypes, function (type)
        {
            sql += sep;
            sql += this.getEachSeriesTypePlotDataSql(type, metricProps, whereClause, MetricType);
            sep = "\nUNION\n";
        }, this);

        return sql;
    },

    getGuideSetAvgMRs : function(data, isLogScale)
    {
        var movingRangeMap = {}, guideSetDataMap = {};
        Ext4.each(data, function(row) {
            var guideSetId = row['GuideSetId'];
            var seriesLabel = row['SeriesLabel'];
            var seriesType = row['SeriesType'];

            if (!guideSetDataMap[guideSetId]) {
                guideSetDataMap[guideSetId] = {};
            }

            if (!guideSetDataMap[guideSetId].Series) {
                guideSetDataMap[guideSetId].Series = {};
            }

            if (!guideSetDataMap[guideSetId].Series[seriesLabel]) {
                guideSetDataMap[guideSetId].Series[seriesLabel] = {};
            }

            if (!guideSetDataMap[guideSetId].Series[seriesLabel][seriesType])
            {
                guideSetDataMap[guideSetId].Series[seriesLabel][seriesType] = {
                    MetricValues: []
                };
            }
            guideSetDataMap[guideSetId].Series[seriesLabel][seriesType].MetricValues.push(row.MetricValue); //data.rows should already be sorted by AcquiredTime
        }, this);

        Ext4.iterate(guideSetDataMap, function(guideSetId, setVal){
            movingRangeMap[guideSetId] = {};
            var series = setVal.Series;
            Ext4.iterate(series, function(seriesLabel, seriesVal){
                Ext4.iterate(seriesVal, function(typeName, seriesTypeVal){
                    var metricVals = seriesTypeVal.MetricValues;
                    if (metricVals == null || metricVals.length === 0)
                        return;
                    var metricMovingRanges = LABKEY.vis.Stat.getMovingRanges(metricVals, isLogScale);

                    if (!movingRangeMap[guideSetId].Series) {
                        movingRangeMap[guideSetId].Series = {};
                    }

                    if (!movingRangeMap[guideSetId].Series[seriesLabel]) {
                        movingRangeMap[guideSetId].Series[seriesLabel] = {};
                    }

                    movingRangeMap[guideSetId].Series[seriesLabel][typeName] = {
                        avgMR: LABKEY.vis.Stat.getMean(metricMovingRanges),
                        stddevMR: LABKEY.vis.Stat.getStdDev(metricMovingRanges)
                    };
                });

            });
        });
        return movingRangeMap;
    },

    getAllProcessedMetricDataSets: function(rawData)
    {
        var metricDataSet =  {};
        Ext4.each(rawData, function (row){
            if (!metricDataSet[row['MetricType']])
                metricDataSet[row['MetricType']] = {};
            if (!metricDataSet[row['MetricType']][row.GuideSetId])
                metricDataSet[row['MetricType']][row.GuideSetId] = [];
            metricDataSet[row['MetricType']][row.GuideSetId].push(row);
        });

        var processedMetricDataSet = {};
        Ext4.iterate(metricDataSet, function(metric, guides){
            processedMetricDataSet[metric] = {};
            Ext4.iterate(guides, function(guideId, guideset){
                processedMetricDataSet[metric][guideId] = this.preprocessPlotData(guideset, true, true, true);
            }, this);
        }, this);
        return processedMetricDataSet;
    },

    getAllProcessedGuideSets: function(rawData)
    {
        var metricGuideSet =  {};
        Ext4.each(rawData, function (row){
            if (!metricGuideSet[row['MetricType']])
                metricGuideSet[row['MetricType']] = [];
            metricGuideSet[row['MetricType']].push(row);
        });
        var processedMetricGuides = {};
        Ext4.iterate(metricGuideSet, function(key, val){
            processedMetricGuides[key] = this.getGuideSetAvgMRs(val);
        }, this);
        return processedMetricGuides;
    },

    preprocessPlotData: function(plotDataRows, hasMR, hasCUSUMm, hasCUSUMv, isLogScale) {
        var plotDataMap = {};

        if (plotDataRows) {
            for (var i = 0; i < plotDataRows.length; i++) {
                var row = plotDataRows[i];
                if (!plotDataMap[row['SeriesLabel']]) {
                    plotDataMap[row['SeriesLabel']] = {
                        Series: {}
                    };
                }
                if (!plotDataMap[row['SeriesLabel']].Series[row['SeriesType']]) {
                    plotDataMap[row['SeriesLabel']].Series[row['SeriesType']] = {
                        Rows: [],
                        MetricValues: []
                    };
                }
                row.MetricValue = Math.round(row.MetricValue * 10000) / 10000; // round to four decimals

                plotDataMap[row['SeriesLabel']].Series[row['SeriesType']].MetricValues.push(row.MetricValue);
                plotDataMap[row['SeriesLabel']].Series[row['SeriesType']].Rows.push(row);
            }

            if (hasMR || hasCUSUMm || hasCUSUMv) {
                Ext4.iterate(plotDataMap, function (seriesLabel, seriesVal) {
                    Ext4.iterate(seriesVal.Series, function (seriesType, seriesTypeObj) {
                        var mRs, positiveCUSUMm, negativeCUSUMm, positiveCUSUMv, negativeCUSUMv,
                                metricVals = seriesTypeObj.MetricValues;
                        if (hasMR)
                            mRs = LABKEY.vis.Stat.getMovingRanges(metricVals, isLogScale);
                        if (hasCUSUMm) {
                            positiveCUSUMm = LABKEY.vis.Stat.getCUSUM(metricVals, false, false, isLogScale);
                            negativeCUSUMm = LABKEY.vis.Stat.getCUSUM(metricVals, true, false, isLogScale);
                        }
                        if (hasCUSUMv) {
                            positiveCUSUMv = LABKEY.vis.Stat.getCUSUM(metricVals, false, true, isLogScale);
                            negativeCUSUMv = LABKEY.vis.Stat.getCUSUM(metricVals, true, true, isLogScale);
                        }
                        for (var i = 0; i < seriesTypeObj.Rows.length; i++) {
                            var row = seriesTypeObj.Rows[i];
                            if (hasMR)
                                row['MR'] = mRs[i];
                            if (hasCUSUMm) {
                                row['CUSUMmP'] = positiveCUSUMm[i];
                                row['CUSUMmN'] = negativeCUSUMm[i];
                            }
                            if (hasCUSUMv) {
                                row['CUSUMvP'] = positiveCUSUMv[i];
                                row['CUSUMvN'] = negativeCUSUMv[i];
                            }
                        }
                    }, this)
                }, this);
            }
        }
        return plotDataMap;
    },

    getPlotWebPartHeader: function(wp, title)
    {
        var html = '<br/>' +
                '<table class="labkey-wp ' + wp + '">' +
                ' <tr class="labkey-wp-header">' +
                '     <th class="labkey-wp-title-left">' +
                '        <span class="labkey-wp-title-text ' +  wp + '-title">'+ Ext4.util.Format.htmlEncode(title) + '</span>' +
                '     </th>' +
                ' </tr>';
        return html;
    },

    addPlotWebPartToPlotDiv: function (id, title, div, wp)
    {
        var html = this.getPlotWebPartHeader(wp, title);
            html += '<tr>' +
                    '     <td class="labkey-wp-body">' +
                    '        <div id="' + id + '" class="chart-render-div"></div>' +
                    '     </td>' +
                    ' </tr>' +
                    '</table>';
        Ext4.get(div).insertHtml('beforeEnd', html);
    },

    addPlotsToPlotDiv: function(ids, title, div, wp)
    {
        if (this.largePlot)
            this.addLargePlotsToPlotDiv(ids, title, div, wp);
        else
            this.addSmallPlotsToPlotDiv(ids, title, div, wp);
    },

    addLargePlotsToPlotDiv: function (ids, title, div, wp)
    {
        var html = this.getPlotWebPartHeader(wp, title);

        Ext4.each(ids, function(plotId){
            html += '<tr>' +
                    '     <td><div id="' + plotId + '" class="chart-render-div"></div></td>' +
                    ' </tr>';
        });
        html += '</table>';
        Ext4.get(div).insertHtml('beforeEnd', html);
    },

    addSmallPlotsToPlotDiv: function (ids, title, div, wp)
    {
        var html = this.getPlotWebPartHeader(wp, title);

        if (ids.length > 0)
        {
            html += ' <tr>' +
                    '     <td><div class="plot-small-layout chart-render-div" id="' + ids[0] + '"></div>';
            if (ids.length > 1)
                html += '<div class="plot-small-layout chart-render-div" id="' + ids[1] + '"></div>';
            html += ' </td></tr>';
        }

        if (ids.length > 2)
        {
            html += ' <tr>' +
                    '     <td><div class="plot-small-layout chart-render-div" id="' + ids[2] + '"></div>';
            if (ids.length > 3)
                html += '<div class="plot-small-layout chart-render-div" id="' + ids[3] + '"></div>';
            html += ' </td></tr>';
        }
        html += '</table>';
        Ext4.get(div).insertHtml('beforeEnd', html);
    },

    setPlotWidth: function (div)
    {
        if (this.plotWidth == null)
        {
            // set the width of the plot webparts based on the first labkey-wp-body element
            this.plotWidth = 900;
            var spacer = 33;
            var wp = document.querySelector('.panel.panel-portal');
            if (wp && (wp.clientWidth - spacer) > this.plotWidth) {
                this.plotWidth = wp.clientWidth - spacer;
            }

            Ext4.get(div).setWidth(this.plotWidth);
        }
    },

    attachPlotExportIcons : function(id, plotTitle, plotIndex, plotWidth, extraMargin)
    {
        this.createExportIcon(id, 'fa-file-pdf-o', 'Export to PDF', 0, plotIndex, plotWidth, function(){
            this.exportChartToImage(id, extraMargin, LABKEY.vis.SVGConverter.FORMAT_PDF, plotTitle);
        });

        this.createExportIcon(id, 'fa-file-image-o', 'Export to PNG', 1, plotIndex, plotWidth, function(){
            this.exportChartToImage(id, extraMargin, LABKEY.vis.SVGConverter.FORMAT_PNG, plotTitle);
        });
    },

    createExportIcon : function(divId, iconCls, tooltip, indexFromLeft, plotIndex, plotWidth, callbackFn)
    {
        var leftPositionPx = (( (!this.largePlot && this.plotTypes && this.plotTypes.length > 1) ? plotIndex % 2 : 0) * plotWidth) + (indexFromLeft * 30) + 60,
            exportIconDivId = divId + iconCls,
            html = '<div id="' + exportIconDivId + '" class="export-icon" style="left: ' + leftPositionPx + 'px;">'
                    + '<i class="fa ' + iconCls + '"></i></div>';

        Ext4.get(divId).insertHtml('afterBegin', html);

        Ext4.create('Ext.tip.ToolTip', {
            target: exportIconDivId,
            html: tooltip
        });

        Ext4.get(exportIconDivId).on('click', callbackFn, this);
    },

    exportChartToImage : function(svgDivId, extraMargin, type, fileName)
    {
        var svgStr = this.getExportSVGStr(svgDivId, extraMargin),
            exportType = type || LABKEY.vis.SVGConverter.FORMAT_PDF;
        LABKEY.vis.SVGConverter.convert(svgStr, exportType, fileName);
    },

    getExportSVGStr: function(svgDivId, extraWidth)
    {
        var svgEls = Ext4.get(svgDivId).select('svg');
        var targetSvg = svgEls.elements[0];
        var oldWidth = targetSvg.getBoundingClientRect().width;
        // temporarily increase svg size to allow exporting of legends that's outside svg
        if (extraWidth)
            targetSvg.setAttribute('width', oldWidth + extraWidth);
        var svgStr = LABKEY.vis.SVGConverter.svgToStr(targetSvg);
        if (extraWidth)
            targetSvg.setAttribute('width', oldWidth);
        svgStr = svgStr.replace(/visibility="hidden"/g, 'visibility="visible"');
        return svgStr;
    },

    failureHandler: function(response) {
        var plotDiv = Ext4.get(this.plotDivId);
        if (plotDiv) {
            if (response.message) {
                plotDiv.update("<span>" + response.message + "</span>");
            }
            else {
                plotDiv.update("<span class='labkey-error'>Error: " + response.exception + "</span>");
            }

            plotDiv.unmask();
        }
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
    },

    getSingleMetricGuideSetRawSql: function(metricId, metricType, schemaName, queryName, series)
    {
        var guideSetSql = "SELECT s.*, g.Comment, '" +  metricType + "' AS MetricType FROM (";
        guideSetSql += this.metricGuideSetRawSql(metricId, schemaName, queryName, undefined, undefined, false, series);
        guideSetSql +=  ") s"
                + " LEFT JOIN GuideSet g ON g.RowId = s.GuideSetId";
        return guideSetSql;
    },

    queryContainerSampleFileRawData: function(params, cb, scope)
    {
        // build query to query for all raw data
        var sql = "", sep = "", where = '';
        Ext4.each(this.metricPropArr, function (metricType)
        {
            var id = metricType.id,
                label = metricType.series1Label,
                metricProps = this.getMetricPropsById(id);

            var newSql = this.getEachSeriesTypePlotDataSql('series1', metricProps, where, label);

            sql += sep + '(' + newSql + ')';
            sep = "\nUNION\n";

            if (Ext4.isDefined(metricType.series2SchemaName) && Ext4.isDefined(metricType.series2QueryName))
            {
                label = metricType.series2Label;
                newSql = this.getEachSeriesTypePlotDataSql('series2', metricProps, where, label);
                sql += sep + '(' + newSql + ')';
            }
        }, this);

        sql = "SELECT * FROM (" + sql + ") a";  //wrap unioned results in sql to support sorting

        var sqlObj = {
            schemaName: 'targetedms',
            sql: sql,
            scope: this,
            sort: 'SeriesType, SeriesLabel, AcquiredTime', //it's important that record is sorted by AcquiredTime asc as ordering is critical in calculating mR and CUSUM
            success: function (data)
            {
               if(data) { //TODO : hack to avoid timing issue until this code is removed
                   params.rawMetricDataSet = data.rows;
                   cb.call(scope, params);
               }
            }
        };
        if (params.container)
            sqlObj.containerPath = params.container.path;

        LABKEY.Query.executeSql(sqlObj);
    },

    queryContainerSampleFileRawGuideSetStats: function(params, cb, scope)
    {
        // build query to query for all metric guide set
        var sql = "", sep = "";
        Ext4.each(this.metricPropArr, function (metricType)
        {
            var id = metricType.id,
                label = metricType.series1Label,
                metricProps = this.getMetricPropsById(id);

            sql += sep + '(' + this.getSingleMetricGuideSetRawSql(id, label, metricProps.series1SchemaName, metricProps.series1QueryName, 'series1') + ')';
            sep = "\nUNION\n";

            if (Ext4.isDefined(metricType.series2SchemaName) && Ext4.isDefined(metricType.series2QueryName))
            {
                label = metricType.series2Label;
                sql += sep + '(' + this.getSingleMetricGuideSetRawSql(id, label, metricProps.series2SchemaName, metricProps.series2QueryName, 'series2') + ')';
            }
        }, this);

        sql = "SELECT * FROM (" + sql + ") a"; //wrap unioned results in sql to support sorting

        var sqlObj = {
            schemaName: 'targetedms',
            sql: sql,
            scope: this,
            sort: 'GuideSetId, SeriesLabel, AcquiredTime', //it's important that record is sorted by AcquiredTime asc as ordering is critical in calculating mR and CUSUM
            success: function (data)
            {
                params.rawGuideSet = data.rows;
                this.queryContainerSampleFileRawData(params, cb, scope);
                cb.call(scope, params);
            }
        };
        if (params && params.container)
            sqlObj.containerPath = params.container.path;
        LABKEY.Query.executeSql(sqlObj);
    },

    processEachOutlier: function(groupByGuideSet, countObj, guideSetId, sampleFiles, targetSampleFile)
    {
        if (groupByGuideSet)
        {
            if (!countObj[guideSetId])
                countObj[guideSetId] = 0;
            countObj[guideSetId]++;
        }
        else
        {
            if (sampleFiles.indexOf(targetSampleFile) > -1)
            {
                if (!countObj[targetSampleFile])
                    countObj[targetSampleFile] = 0;
                countObj[targetSampleFile]++;
            }
        }
    },

    getQCPlotMetricOutliers: function(processedMetricGuides, processedMetricDataSet, CUSUMm, CUSUMv, mR, groupByGuideSet, sampleFiles)
    {
        if (!processedMetricGuides || Object.keys(processedMetricGuides).length == 0)
            return null;
        if (!groupByGuideSet && !sampleFiles)
            return null;
        var plotOutliers = {};

        Ext4.iterate(processedMetricDataSet, function(metric, metricVal){
            var countCUSUMmP = {}, countCUSUMmN = {}, countCUSUMvP = {}, countCUSUMvN = {}, countMR = {};
            plotOutliers[metric] = {TotalCount: 0, outliers: {}};
            Ext4.iterate(metricVal, function(guideSetId, peptides)
            {
                if (plotOutliers[metric].TotalCount < Object.keys(peptides).length)
                    plotOutliers[metric].TotalCount = Object.keys(peptides).length;
                Ext4.iterate(peptides, function (peptide, peptideVal) {
                    if (!peptideVal || !peptideVal.Series)
                        return;
                    Ext4.iterate(peptideVal.Series, function (series, seriesVal) {
                        if (!seriesVal)
                            return;

                        var dataRows = seriesVal.Rows;

                        if (CUSUMm) {
                            Ext4.each(dataRows, function (data) {
                                var sampleFile = data.SampleFile;
                                if (data.CUSUMmN > LABKEY.vis.Stat.CUSUM_CONTROL_LIMIT) {
                                    this.processEachOutlier(groupByGuideSet, countCUSUMmN, guideSetId, sampleFiles, sampleFile);
                                }
                                else if (data.CUSUMmP > LABKEY.vis.Stat.CUSUM_CONTROL_LIMIT) {
                                    this.processEachOutlier(groupByGuideSet, countCUSUMmP, guideSetId, sampleFiles, sampleFile);
                                }
                            }, this);

                        }
                        if (CUSUMv) {
                            Ext4.each(dataRows, function (data) {
                                var sampleFile = data.SampleFile;
                                if (data.CUSUMvN > LABKEY.vis.Stat.CUSUM_CONTROL_LIMIT) {
                                    this.processEachOutlier(groupByGuideSet, countCUSUMvN, guideSetId, sampleFiles, sampleFile);
                                }
                                else if (data.CUSUMvP > LABKEY.vis.Stat.CUSUM_CONTROL_LIMIT) {
                                    this.processEachOutlier(groupByGuideSet, countCUSUMvP, guideSetId, sampleFiles, sampleFile);
                                }
                            }, this);

                        }
                        if (mR) {
                            Ext4.each(dataRows, function (data) {
                                if (!processedMetricGuides[metric] || !processedMetricGuides[metric][guideSetId] || !processedMetricGuides[metric][guideSetId].Series
                                        || !processedMetricGuides[metric][guideSetId].Series[peptide] || !processedMetricGuides[metric][guideSetId].Series[peptide][series])
                                    return;

                                var controlRange = processedMetricGuides[metric][guideSetId].Series[peptide][series].avgMR;
                                if (data.MR > LABKEY.vis.Stat.MOVING_RANGE_UPPER_LIMIT_WEIGHT * controlRange) {
                                    var sampleFile = data.SampleFile;
                                    this.processEachOutlier(groupByGuideSet, countMR, guideSetId, sampleFiles, sampleFile);
                                }
                            }, this);
                        }
                    }, this);
                }, this);
            }, this);
            plotOutliers[metric].outliers.CUSUMmP = countCUSUMmP;
            plotOutliers[metric].outliers.CUSUMvP = countCUSUMvP;
            plotOutliers[metric].outliers.CUSUMmN = countCUSUMmN;
            plotOutliers[metric].outliers.CUSUMvN = countCUSUMvN;
            plotOutliers[metric].outliers.mR = countMR;
        }, this);

        return plotOutliers;
    },

    getMetricOutliersByFileOrGuideSetGroup: function(metricOutlier) {
        var transformedOutliers = {};
        Ext4.iterate(metricOutlier, function(metric, vals){
            var totalCount = vals.TotalCount;
            Ext4.iterate(vals.outliers, function(type, groups){
                Ext4.iterate(groups, function(group, count){
                    if (!transformedOutliers[group])
                        transformedOutliers[group] = {};
                    if (!transformedOutliers[group][metric])
                        transformedOutliers[group][metric] = {TotalCount: totalCount};
                    transformedOutliers[group][metric][type] = count;
                }, this);
            }, this);
        }, this);
        return transformedOutliers;
    },

    formatValue: function(value) {
        return Math.round(value * 10000) / 10000;
    }
});

