/*
 * Copyright (c) 2016-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define('LABKEY.targetedms.QCSummary', {
    extend: 'Ext.panel.Panel',

    border: false,

    numSampleFileStats: 3,

    initComponent: function ()
    {
        this.qcPlotPanel = Ext4.create('LABKEY.targetedms.BaseQCPlotPanel');

        this.callParent();

        this.qcPlotPanel.queryInitialQcMetrics(this.initPanel, this);

    },

    initPanel : function(){

        LABKEY.Ajax.request({
            url: LABKEY.ActionURL.buildURL('targetedms', 'getQCSummary.api'),
            params: {
                includeSubfolders: true
            },
            scope: this,
            success: LABKEY.Utils.getCallbackWrapper(function (response)
            {
                var containers = response['containers'],
                    container,
                    childPanelItems = [],
                    hasChildren = containers.length > 1;

                // determine the summaryView width
                var portalWebpart = document.querySelector('.panel.panel-portal'),
                    minWidth = 750,
                    width = portalWebpart ? Math.max(portalWebpart.clientWidth - 50, minWidth) : minWidth;
                if (hasChildren && containers.length > 1 && (width/2) > minWidth) {
                    width = (width / 2) - 5;
                }

                // Add the current (root) container to the QC Summary display
                container = containers[0];
                container.showName = hasChildren;
                container.isParent = true;
                container.parentOnly = containers.length == 1;
                this.add(this.getContainerSummaryView(container, hasChildren, width));

                // Add the set of child containers in an hbox layout
                if (hasChildren)
                {
                    for (var i = 1; i < containers.length; i++)
                    {
                        container = containers[i];
                        container.showName = true;
                        container.parentOnly = false;
                        container.isParent = false;
                        childPanelItems.push(this.getContainerSummaryView(container, undefined, width));
                    }

                    this.add(Ext4.create('Ext.panel.Panel', {
                        border: false,
                        items: childPanelItems
                    }));
                }

            }, this, false),
            failure: LABKEY.Utils.getCallbackWrapper(function (response)
            {
                this.add(Ext4.create('Ext.Component', {
                    autoEl: 'span',
                    cls: 'labkey-error',
                    html: 'Error: ' + response.exception
                }));
            }, this, true)
        });
    },

    getContainerSummaryView: function (container, hasChildren, width)
    {
        container.viewCmpId = Ext4.id();
        container.autoQcCalloutId = Ext4.id();

        var config = {
            id: container.viewCmpId,
            data: container,
            tpl: this.getSummaryDisplayTpl(),
            listeners: {
                scope: this,
                render: function ()
                {
                    this.queryContainerSampleFileStats(container);

                    // add hover event listeners for showing AutoQC message
                    this.showAutoQCMessage(container.autoQcCalloutId, container.autoQCPing, hasChildren);
                }
            }
        };

        if (Ext4.isDefined(hasChildren))
        {
            config.cls = hasChildren ? 'summary-view' : '';
            config.width = hasChildren ? width : undefined;
            config.minHeight = 21;
        }
        else
        {
            config.cls = 'summary-view subfolder-view';
            config.width = width;
            config.minHeight = 136;
        }

        config.cls += ' summary-tile'; // For tests

        return Ext4.create('Ext.view.View', config);
    },

    getSummaryDisplayTpl: function ()
    {
        return new Ext4.XTemplate(
            '<tpl if="showName !== undefined">',
                '<tpl if="showName === true &amp;&amp; (isParent !== true || docCount &gt; 0)">',
                    '<div class="folder-name">',
                        '<a href="{path:this.getContainerLink}">{name:htmlEncode}</a>',
                    '</div>',
                '</tpl>',
                '<tpl if="docCount == 0 && isParent !== true">',
                    '<div class="item-text">No sample files imported</div>',
                    '<div class="auto-qc-ping" id="{autoQcCalloutId}">AutoQC <span class="{autoQCPing:this.getAutoQCPingClass}"></span></div>',
                '<tpl elseif="docCount == 0 && parentOnly">',
                    '<div class="item-text">No data found.</div>',
                '<tpl elseif="docCount &gt; 0">',
                    '<div class="item-text">',
                        '<a href="{path:this.getSampleFileLink}">{fileCount} sample file{fileCount:this.pluralize}</a>',
                    '</div>',
                    '<div class="item-text">{precursorCount} precursor{precursorCount:this.pluralize}</div>',
                    '<div class="item-text sample-file-details sample-file-details-loading" id="qc-summary-samplefiles-{id}">...</div>',
                    '<div class="auto-qc-ping" id="{autoQcCalloutId}">AutoQC <span class="{autoQCPing:this.getAutoQCPingClass}"></span></div>',
                '</tpl>',
            '</tpl>',
            {
                pluralize: function (val)
                {
                    return val == 1 ? '' : 's';
                },
                getContainerLink: function (path)
                {
                    return LABKEY.ActionURL.buildURL('project', 'begin', path);
                },
                getSampleFileLink: function (path)
                {
                    return LABKEY.ActionURL.buildURL('query', 'executeQuery', path,
                            {schemaName: 'targetedms', 'query.queryName': 'SampleFile'});
                },
                getAutoQCPingClass: function (val)
                {
                    if (val == null)
                        return 'qc-none fa fa-circle-o';
                    return val.isRecent ? 'qc-correct fa fa-check-circle' : 'qc-error fa fa-circle';
                }
            }
        );
    },

    showAutoQCMessage : function(divId, autoQC, hasChildren)
    {
        var divEl = Ext4.get(divId),
            content = '', width = undefined;

        if (!divEl)
            return;

        if (autoQC == null)
        {
            content = 'Has never been pinged';
            width = 155;
        }
        else
        {
            content = autoQC.isRecent ? 'Was pinged recently on ' + autoQC.modified : 'Was pinged on ' + autoQC.modified;
            width = autoQC.isRecent ? 160 : 140;
        }

        // add mouse listeners to the div element for when to show the AutoQC message
        divEl.on('mouseover', function() {
            var calloutMgr = hopscotch.getCalloutManager();
            calloutMgr.removeAllCallouts();
            calloutMgr.createCallout({
                id: Ext4.id(),
                target: divEl.dom,
                placement: 'left',
                yOffset: -22,
                arrowOffset: 7,
                width: width,
                showCloseButton: false,
                content: content
            });
        }, this);

        // cancel the hover details show event if the user was just passing over the div without stopping for X amount of time
        divEl.on('mouseout', function() {
            hopscotch.getCalloutManager().removeAllCallouts();
        }, this);
    },

    queryContainerSampleFileStats: function (container)
    {
        if (container.fileCount > 0)
        {
            // generate a UNION SQL query for the relevant metrics to get the summary info for the last N sample files
            var sql = "", sep = "";
            Ext4.each(this.qcPlotPanel.metricPropArr, function (metricType)
            {
                var id = metricType.id,
                    name = metricType.name,
                    label = metricType.series1Label,
                    schema = metricType.series1SchemaName,
                    query = metricType.series1QueryName;

                sql += sep + '(' + this.getLatestSampleFileStatsSql(id, name, label, schema, query) + ')';
                sep = "\nUNION\n";

                if (Ext4.isDefined(metricType.series2SchemaName) && Ext4.isDefined(metricType.series2QueryName))
                {
                    label = metricType.series2Label;
                    schema = metricType.series2SchemaName;
                    query = metricType.series2QueryName;

                    sql += sep + '(' + this.getLatestSampleFileStatsSql(id, name, label, schema, query) + ')';
                    sep = "\nUNION\n";
                }
            }, this);

            LABKEY.Query.executeSql({
                containerPath: container.path,
                schemaName: 'targetedms',
                sql: sql,
                sort: '-AcquiredTime,MetricLabel', // remove this if the perf is bad and we can sort the data in the success callback
                scope: this,
                success: function (data) {
                    this.qcPlotPanel.queryContainerSampleFileRawGuideSetStats({container: container, dataRowsLJ: data.rows, limitedSampleFiles: true}, this.renderContainerSampleFileStats, this);
                }
            });
        }
        else if (container.docCount > 0)
        {
            var sampleFilesDiv = Ext4.get('qc-summary-samplefiles-' + container.id);
            sampleFilesDiv.update('');
            sampleFilesDiv.removeCls('sample-file-details-loading');
        }
    },

    getOtherQCSampleFileStats: function(params, sampleFiles)
    {
        var validGuideSetIds = [];
        Ext4.iterate(sampleFiles, function(key, val){
           validGuideSetIds.push(val.GuideSetId);
        });
        var processedMetricGuides =  this.qcPlotPanel.getAllProcessedGuideSets(params.rawGuideSet);
        var processedMetricDataSet = this.qcPlotPanel.getAllProcessedMetricDataSets(params.rawMetricDataSet.filter(function(row) {
            return validGuideSetIds.indexOf(row.GuideSetId) > -1 && !row.IgnoreInQC;
        }));
        var metricOutlier = this.qcPlotPanel.getQCPlotMetricOutliers(processedMetricGuides, processedMetricDataSet, true, true, true, false, Object.keys(sampleFiles));
        var transformedOutliers = this.qcPlotPanel.getMetricOutliersByFileOrGuideSetGroup(metricOutlier);

        Ext4.iterate(transformedOutliers, function(filename, metrics){
            var info = sampleFiles[filename];
            Ext4.iterate(metrics, function(metric, outliers){
                var matchedItem = null;
                Ext4.each(info.Items, function(item){
                   if (item.MetricLabel == metric)
                   {
                       matchedItem = item;
                       return false;
                   }
                });
                if (!matchedItem) {
                    var metricId = this.qcPlotPanel.getMetricPropsByLabel(metric);
                    if (metricId)
                        metricId = metricId.id;
                    matchedItem = {
                        MetricLabel: metric,
                        MetricName: metric,
                        NonConformers: 0,
                        ContainerPath: params.container.path,
                        MetricId: metricId
                    };
                    info.Items.push(matchedItem);
                }
                Ext4.apply(matchedItem, {CUSUMmN: 0, CUSUMmP: 0, CUSUMvP: 0, CUSUMvN: 0, mR: 0});
                Ext4.apply(matchedItem, outliers);
                matchedItem.CUSUMm = matchedItem.CUSUMmN + matchedItem.CUSUMmP;
                matchedItem.CUSUMv = matchedItem.CUSUMvN + matchedItem.CUSUMvP;
            }, this);

        }, this);

        Ext4.iterate(sampleFiles, function(name, sample){
            var CUSUMmP = 0, CUSUMmN = 0, CUSUMvP = 0, CUSUMvN = 0, mR = 0;
            Ext4.each(sample.Items, function(item){
                if (item.CUSUMmN)
                    CUSUMmN += item.CUSUMmN;
                if (item.CUSUMmP)
                    CUSUMmP += item.CUSUMmP;
                if (item.CUSUMvP)
                    CUSUMvP += item.CUSUMvP;
                if (item.CUSUMvN)
                    CUSUMvN += item.CUSUMvN;
                if (item.mR)
                    mR += item.mR;
            }, this);
            sample.CUSUMm = CUSUMmP + CUSUMmN;
            sample.CUSUMv = CUSUMvP + CUSUMvN;
            sample.CUSUMmP = CUSUMmP;
            sample.CUSUMmN = CUSUMmN;
            sample.CUSUMvN = CUSUMvN;
            sample.CUSUMvP = CUSUMvP;
            sample.mR = mR;
            sample.hasOutliers = !(sample.NonConformers == 0 && sample.CUSUMm == 0 && sample.CUSUMv == 0 && sample.mR == 0);
        }, this);
    },

    renderContainerSampleFileStats: function (params)
    {
        var container = params.container;
        if (!params.rawGuideSet || !params.rawMetricDataSet)
            return;

        var sampleFiles = {}, info = null, index = 1;
        Ext4.each(params.dataRowsLJ, function(row)
        {
            if (info == null || row.SampleFile != info.SampleFile)
            {
                info = {
                    Index: index++,
                    SampleFile: row.SampleFile,
                    AcquiredTime: row.AcquiredTime,
                    Metrics: 0,
                    NonConformers: 0,
                    TotalCount: 0,
                    Items: [],
                    GuideSetId: row.GuideSetId,
                    IgnoreForAllMetric: row.IgnoreInQC
                };
                sampleFiles[row.SampleFile] = info;
            }

            info.IgnoreForAllMetric = info.IgnoreForAllMetric && row.IgnoreInQC;
            if (!row.IgnoreInQC) {
                info.Metrics++;
                info.NonConformers += row.NonConformers;
                info.TotalCount += row.TotalCount;
            }

            Ext4.apply(row,{CUSUMm: 0, CUSUMv: 0, CUSUMmN: 0, CUSUMmP: 0, CUSUMvP: 0, CUSUMvN: 0, mR: 0});
            row.ContainerPath = container.path;
            info.Items.push(row);
        }, this);

        this.getOtherQCSampleFileStats(params, sampleFiles);

        var html = '';
        Ext4.iterate(sampleFiles, function(name, sampleFile)
        {
            // create a new div id for each sampleFile to use for the hover details callout
            sampleFile.calloutId = Ext4.id();

            var iconCls = !sampleFile.IgnoreForAllMetric ? (!sampleFile.hasOutliers ? 'fa-file-o qc-correct' : 'fa-file qc-error') : 'fa-file-o qc-none';
            html += '<div class="sample-file-item" id="' + sampleFile.calloutId + '">'
                    + '<span class="fa ' + iconCls + '"></span> ' + sampleFile.AcquiredTime + ' - ';

            if (sampleFile.IgnoreForAllMetric) {
                html += 'not included in QC</div>';
            }
            else if (!sampleFile.NonConformers && !sampleFile.mR && !sampleFile.CUSUMm && !sampleFile.CUSUMv) {
                html += 'no outliers</div>';
            }
            else {
                var sep = '';
                if (sampleFile.NonConformers > 0) {
                    html += (sampleFile.NonConformers + '/' + sampleFile.TotalCount) + ' (Levey-Jennings)';
                    sep = ', ';
                }
                if (sampleFile.mR > 0) {
                    html += sep + (sampleFile.mR + '/' + sampleFile.TotalCount) + ' (Moving Range)';
                    sep = ', ';
                }
                if (sampleFile.CUSUMm > 0) {
                    html += sep + (sampleFile.CUSUMm + '/' + sampleFile.TotalCount) + ' (CUSUMm)';
                    sep = ', ';
                }
                if (sampleFile.CUSUMv > 0) {
                    html += sep +  (sampleFile.CUSUMv + '/' + sampleFile.TotalCount) + ' (CUSUMv)';
                }

                html += ' outliers</div>';
            }

        });
        var sampleFilesDiv = Ext4.get('qc-summary-samplefiles-' + container.id);
        sampleFilesDiv.update(html);
        sampleFilesDiv.removeCls('sample-file-details-loading');

        // since the height of the panel will change from adding up to three lines of text, need to reset the size of the view
        this.doLayout();

        // add a hover listener for each of the sample file divs
        Ext4.iterate(sampleFiles, function(name, sampleFile)
        {
            this.showSampleFileStatsDetails(sampleFile.calloutId, sampleFile);
        }, this);
    },

    showSampleFileStatsDetails : function(divId, sampleFile)
    {
        var task = new Ext4.util.DelayedTask(),
            divEl = Ext4.get(divId),
            content = '';

        // generate the HTML content for the sample file display details
        content += '<span class="sample-file-field-label">Name:</span> ' + sampleFile.SampleFile
                + '<br/><span class="sample-file-field-label">Acquired Date/Time:</span> ' + sampleFile.AcquiredTime
                + '<br/><span class="sample-file-field-label">Number of tracked metrics:</span> ' + sampleFile.Metrics
                + '<br/><span class="sample-file-field-label">Number of data points:</span> ' + (sampleFile.Metrics > 0 ? sampleFile.TotalCount/sampleFile.Metrics : 'n/a')
                + '<br/><span class="sample-file-field-label">Out of guide set range:</span> ';
        if (sampleFile.IgnoreForAllMetric) {
            content += 'not included in QC<br/>';
        }
        else if (!sampleFile.NonConformers && !sampleFile.mR && !sampleFile.CUSUMm && !sampleFile.CUSUMv) {
            content += 'no outliers<br/>';
        }
        else {
            content += '<table class=" labkey-data-region-legacy labkey-show-borders">';
            content += '<thead><tr><td class="labkey-column-header"></td><td class="labkey-column-header" colspan="6" align="center">Outliers</td></tr>' +
                            '<tr>' +
                                '<td class="labkey-column-header"></td><td class="labkey-column-header"></td>' +
                                '<td class="labkey-column-header"></td><td class="labkey-column-header" colspan="4" align="center">CUSUM</td>' +
                            '</tr>' +
                            '<tr>' +
                                '<td class="labkey-column-header outlier-column-header">Metric</td>' +
                                '<td class="labkey-column-header outlier-column-header">Levey-Jennings</td>' +
                                '<td class="labkey-column-header outlier-column-header">Moving Range</td>' +
                                '<td class="labkey-column-header outlier-column-header" title="Mean CUSUM-">Mean-</td>' +
                                '<td class="labkey-column-header outlier-column-header" title="Mean CUSUM+">Mean+</td>' +
                                '<td class="labkey-column-header outlier-column-header" title="Variability CUSUM-">Variability-</td>' +
                                '<td class="labkey-column-header outlier-column-header" title="Variability CUSUM+">Variability+</td>' +
                            '</tr>' +
                        '</thead><tbody>';

            // sort by metric label, alphabetically
            sampleFile.Items.sort(function(a, b) {
                if (a.MetricLabel < b.MetricLabel)
                    return -1;
                if (a.MetricLabel > b.MetricLabel)
                    return 1;
                return 0;
            });

            var rowCount = 0;
            Ext4.each(sampleFile.Items, function (item)
            {
                var href = LABKEY.ActionURL.buildURL('project', 'begin', item.ContainerPath, {metric: item.MetricId});
                content += '<tr class="' + (rowCount % 2 == 0 ? 'labkey-alternate-row' : 'labkey-row') + '">';
                content += '<td class="outlier-metric-label"><a href="' + href + '">' + item.MetricLabel + '</a></td>';
                if (item.IgnoreInQC) {
                    content += '<td align="center" colspan="6"><em>not included in QC</em></td>';
                }
                else {
                    content += '<td align="right">' + this.getSampleDetailOutlierDisplayValue(item, 'NonConformers') + '</td>';
                    content += '<td align="right">' + this.getSampleDetailOutlierDisplayValue(item, 'mR') + '</td>';
                    content += '<td align="right">' + this.getSampleDetailOutlierDisplayValue(item, 'CUSUMmN') + '</td>';
                    content += '<td align="right">' + this.getSampleDetailOutlierDisplayValue(item, 'CUSUMmP') + '</td>';
                    content += '<td align="right">' + this.getSampleDetailOutlierDisplayValue(item, 'CUSUMvN') + '</td>';
                    content += '<td align="right">' + this.getSampleDetailOutlierDisplayValue(item, 'CUSUMvP') + '</td>';
                }
                content += '</tr>';

                rowCount++;
            }, this);
            content += '</tbody>';
            content += '</table>';
        }

        // add mouse listeners to the div element for when to show the hover details for this sample file
        divEl.on('mouseover', function() {
            task.delay(1000, function(el){
                var calloutMgr = hopscotch.getCalloutManager();
                calloutMgr.removeAllCallouts();
                calloutMgr.createCallout({
                    id: Ext4.id(),
                    target: el.dom,
                    placement: 'bottom',
                    width: sampleFile.Items.length > 0 ? 655 : 300,
                    title: 'Recent Sample File Details',
                    content: content
                });
            }, this, [divEl]);
        }, this);

        // cancel the hover details show event if the user was just passing over the div without stopping for X amount of time
        divEl.on('mouseout', function() {
            task.cancel();
        }, this);
    },

    getSampleDetailOutlierDisplayValue : function(item, variable) {
        return item[variable] ? item[variable] : 0
    },

    getLatestSampleFileStatsSql : function(id, name, label, schema, query)
    {
        return "SELECT stats.GuideSetId,"
            + "\n'" + id + "' AS MetricId,"
            + "\n'" + name + "' AS MetricName,"
            + "\n'" + label + "' AS MetricLabel,"
            + "\nX.SampleFile,"
            + "\nX.AcquiredTime,"
            + "\nCASE WHEN (exclusion.ReplicateId IS NOT NULL) THEN TRUE ELSE FALSE END AS IgnoreInQC,"
            + "\nSUM(CASE WHEN exclusion.ReplicateId IS NULL AND (X.MetricValue > (stats.Mean + (3 * (CASE WHEN stats.StandardDev IS NULL THEN 0 ELSE stats.StandardDev END)))"
            + "\n   OR X.MetricValue < (stats.Mean - (3 * (CASE WHEN stats.StandardDev IS NULL THEN 0 ELSE stats.StandardDev END)))) THEN 1 ELSE 0 END) AS NonConformers,"
            + "\nCOUNT(*) AS TotalCount"
            + "\nFROM (SELECT *, SampleFileId.AcquiredTime AS AcquiredTime, SampleFileId.SampleName AS SampleFile, SampleFileId.ReplicateId AS ReplicateId"
            + "\n      FROM " + schema + "." + query
            + "\n      WHERE SampleFileId.Id IN (SELECT Id FROM SampleFile WHERE AcquiredTime IS NOT NULL ORDER BY AcquiredTime DESC LIMIT 3)"
            + "\n) X"
            + "\nLEFT JOIN (SELECT DISTINCT ReplicateId FROM QCMetricExclusion WHERE MetricId IS NULL OR MetricId = " + id + ") exclusion"
            + "\nON X.ReplicateId = exclusion.ReplicateId"
            + "\nLEFT JOIN (" + this.qcPlotPanel.metricGuideSetSql(id, schema, query) + ") stats"
            + "\nON X.SeriesLabel = stats.SeriesLabel"
            + "\nAND ((X.AcquiredTime >= stats.TrainingStart AND X.AcquiredTime < stats.ReferenceEnd)"
            + "\n   OR (X.AcquiredTime >= stats.TrainingStart AND stats.ReferenceEnd IS NULL))"
            + "\nGROUP BY stats.GuideSetId, X.SampleFile, X.AcquiredTime, exclusion.ReplicateId"
            + "\nORDER BY X.AcquiredTime DESC";
    }
});
