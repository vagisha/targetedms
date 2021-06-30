/*
 * Copyright (c) 2016-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define('LABKEY.targetedms.QCSummary', {
    extend: 'Ext.panel.Panel',

    border: false,

    numSampleFileStats: null,

    initComponent: function (config)
    {
        this.qcPlotPanel = Ext4.create('LABKEY.targetedms.BaseQCPlotPanel');

        this.callParent();

        this.qcPlotPanel.queryInitialQcMetrics(this.initPanel, this);
        this.numSampleFileStats = config ? config.sampleLimit : 3;
    },

    initPanel : function() {
        this.qcPlotPanel.queryQCInstruments(this.getQCSummary, this);
    },

    getQCSummary: function () {
        LABKEY.Ajax.request({
            url: LABKEY.ActionURL.buildURL('targetedms', 'getQCSummary.api'),
            params: {
                includeSubfolders: true
            },
            scope: this,
            success: LABKEY.Utils.getCallbackWrapper(function (response) {
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
                if (this.qcPlotPanel.qcIntrumentsArr) {
                    if (this.qcPlotPanel.qcIntrumentsArr.length > 1) {
                        var msg = 'We recommend that each instrument use its own QC folder.'
                        container.instrument = 'multiple instruments - ' + this.qcPlotPanel.qcIntrumentsArr.join(', ') + '. ' + msg;

                    }
                    else if (this.qcPlotPanel.qcIntrumentsArr.length === 1) {
                        container.instrument = this.qcPlotPanel.qcIntrumentsArr[0];
                    }
                }
                this.add(this.getContainerSummaryView(container, hasChildren, width));

                // Add the set of child containers in an hbox layout
                if (hasChildren) {
                    for (var i = 1; i < containers.length; i++) {
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
            failure: LABKEY.Utils.getCallbackWrapper(function (response) {
                this.add(Ext4.create('Ext.Component', {
                    autoEl: 'span',
                    cls: 'labkey-error',
                    html: 'Error: ' + response.exception
                }));
            }, this, true)
        });
    },

    getContainerSummaryView: function (container, hasChildren, width) {
        container.viewCmpId = Ext4.id();
        container.autoQcCalloutId = Ext4.id();

        var config = {
            id: container.viewCmpId,
            data: container,
            tpl: this.getSummaryDisplayTpl(),
            listeners: {
                scope: this,
                render: function () {
                    this.queryContainerSampleFileStats(container);

                    // add hover event listeners for showing AutoQC message
                    this.showAutoQCMessage(container.autoQcCalloutId, container.autoQCPing, hasChildren);
                }
            }
        };

        if (Ext4.isDefined(hasChildren)) {
            config.cls = hasChildren ? 'summary-view' : '';
            config.width = hasChildren ? width : undefined;
            config.minHeight = 21;
        }
        else {
            config.cls = 'summary-view subfolder-view';
            config.width = width;
            config.minHeight = 136;
        }

        config.cls += ' summary-tile'; // For tests

        return Ext4.create('Ext.view.View', config);
    },

    getSummaryDisplayTpl: function () {
        return new Ext4.XTemplate(
            '<tpl if="showName !== undefined">',
                '<tpl if="showName === true &amp;&amp; (isParent !== true || docCount &gt; 0)">',
                    '<div class="folder-name">',
                        '<a href="{path:this.getContainerLink}">{name:htmlEncode}</a>',
                    '</div>',
                '</tpl>',
                '<tpl if="docCount == 0 && isParent !== true">',
                    '<div class="qc-summary-text">No sample files imported</div>',
                    '<div class="auto-qc-ping" id="{autoQcCalloutId}">AutoQC <span class="{autoQCPing:this.getAutoQCPingClass}"></span></div>',
                '<tpl elseif="docCount == 0 && parentOnly">',
                    '<div class="qc-summary-text">No data found.</div>',
                '<tpl elseif="docCount &gt; 0">',
                    '<div class="qc-summary-text">',
                        '<a href="{path:this.getSampleFileLink}">{fileCount} sample file{fileCount:this.pluralize}</a> ' +
                            'tracking {precursorCount} precursor{precursorCount:this.pluralize} with {metricCount} metric{metricCount:this.pluralize} for {instrument}',
                    '</div>',
                    '<div class="item-text sample-file-details sample-file-details-loading" id="qc-summary-samplefiles-{id}">Loading...</div>',
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
                getFullHistoryLink: function (path)
                {
                    return LABKEY.ActionURL.buildURL('targetedms', 'qCSummaryHistory', path);
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

    showAutoQCMessage : function(divId, autoQC, hasChildren) {
        var divEl = Ext4.get(divId),
            content = '';

        if (!divEl)
            return;

        if (autoQC == null) {
            content = 'Has never been pinged';
        }
        else {
            var modifiedFormatted = Ext4.util.Format.date(Ext4.Date.parse(autoQC.modified, LABKEY.Utils.getDateTimeFormatWithMS()), LABKEY.extDefaultDateTimeFormat || 'Y-m-d H:i:s');
            content = autoQC.isRecent ? 'Was pinged recently on ' + modifiedFormatted : 'Was pinged on ' + modifiedFormatted;
        }

        content = '<div>' + content + '</div><br/><div>To point AutoQC at this folder, go to configuration\'s Panorama setting tab. ' +
                'Check the Publish to Panorama checkbox. Use <strong>' + LABKEY.Utils.encodeHtml(LABKEY.ActionURL.getBaseURL()) +
                '</strong> as the URL and <strong>' + LABKEY.Utils.encodeHtml(LABKEY.ActionURL.getContainer()) + '</strong> as the folder.</div>'

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
                width: 300,
                showCloseButton: false,
                content: content
            });
        }, this);

        // close the hover details on mouseout of the autoQC element
        divEl.on('mouseout', function() {
            hopscotch.getCalloutManager().removeAllCallouts();
        }, this);
    },

    queryContainerSampleFileStats: function (container) {
        if (container.fileCount > 0) {
            LABKEY.Ajax.request({
                url: LABKEY.ActionURL.buildURL('targetedms', 'GetQCMetricOutliers.api', container.path),
                params: {sampleLimit: this.sampleLimit},
                success: function(response) {
                    var parsed = JSON.parse(response.responseText);
                    if(parsed.sampleFiles) {
                        this.renderContainerSampleFileStats({
                            container: container,
                            limitedSampleFiles: true,
                            sampleFiles: parsed.sampleFiles
                        })
                    } else {
                        this.removeSampleFilesDetailsDiv(container);
                    }
                },
                failure: LABKEY.Utils.getCallbackWrapper(function(response) {
                    var sampleFilesDiv = Ext4.get('qc-summary-samplefiles-' + container.id);

                    if (response.message) {
                        sampleFilesDiv.update("<span>" + Ext4.util.Format.htmlEncode(response.message) + "</span>");
                    }
                    else {
                        sampleFilesDiv.update("<span class='labkey-error'>Error: " + Ext4.util.Format.htmlEncode(response.exception) + "</span>");
                    }
                    sampleFilesDiv.removeCls('sample-file-details-loading');
                }, null, true),
                scope: this
            });
        }
        else if (container.docCount > 0) {
           this.removeSampleFilesDetailsDiv(container);
        }
    },

    removeSampleFilesDetailsDiv: function (container) {
        var sampleFilesDiv = Ext4.get('qc-summary-samplefiles-' + container.id);
        sampleFilesDiv.update('');
        sampleFilesDiv.removeCls('sample-file-details-loading');
    },

    renderContainerSampleFileStats: function (params) {
        var container = params.container;
            var html = '<table class="table-condensed labkey-data-region-legacy labkey-show-borders"><thead><tr><td class="labkey-column-header">Sample Name</td><td class="labkey-column-header">Acquired</td><td class="labkey-column-header">Total outliers</td></tr></thead>';
            var sampleFiles = params.sampleFiles;
            Ext4.iterate(sampleFiles, function (sampleFile) {
                // create a new div id for each sampleFile to use for the hover details callout
                sampleFile.calloutId = Ext4.id();

                var totalOutliers = sampleFile.LeveyJennings + sampleFile.mR + sampleFile.CUSUMm + sampleFile.CUSUMv;

                var iconCls = !sampleFile.IgnoreForAllMetric ? (totalOutliers === 0 ? 'fa-file-o qc-correct' : 'fa-file qc-error') : 'fa-file-o qc-none';
                html += '<tr id="' + sampleFile.calloutId + '"><td><div class="sample-file-item">'
                        + '<span class="fa ' + iconCls + '"></span> ' + Ext4.util.Format.htmlEncode(sampleFile.SampleFile) + '</div></td><td><div class="sample-file-item-acquired">' + Ext4.util.Format.date(Ext4.Date.parse(sampleFile.AcquiredTime, LABKEY.Utils.getDateTimeFormatWithMS()), LABKEY.extDefaultDateTimeFormat || 'Y-m-d H:i:s') + '</div></td>';

                html += '<td style="text-align: right"><div class="sample-file-item-outliers">';
                if (sampleFile.IgnoreForAllMetric) {
                    html += 'not included in QC';
                }
                else {
                    html += totalOutliers;
                }
                html += '</div></td></tr>';
            });
            html += '</table>';
            if (container.fileCount > sampleFiles.length) {
                html += '<div class="qc-summary-text">Showing only the most recently imported samples. <a href="' + LABKEY.ActionURL.buildURL('targetedms', 'qcSummaryHistory.view', container.path) + '">View all</a></div>';
            }
            var sampleFilesDiv = Ext4.get('qc-summary-samplefiles-' + container.id);
            sampleFilesDiv.update(html);
            sampleFilesDiv.removeCls('sample-file-details-loading');

            // since the height of the panel will change from adding up to three lines of text, need to reset the size of the view
            this.doLayout();

            // add a hover listener for each of the sample file divs
            Ext4.iterate(sampleFiles, function (sampleFile) {
                this.showSampleFileStatsDetails(sampleFile.calloutId, sampleFile);
            }, this);
    },

    showSampleFileStatsDetails : function(divId, sampleFile) {
        var task = new Ext4.util.DelayedTask(),
            divEl = Ext4.get(divId),
            content = '';

        var sampleHREF = LABKEY.ActionURL.buildURL('targetedms', 'showSampleFile', LABKEY.ActionURL.getContainer(), {id: sampleFile.SampleId});

        // generate the HTML content for the sample file display details
        content += '<span class="sample-file-field-label">Name:</span> <a href="' + sampleHREF + '">' + Ext4.util.Format.htmlEncode(sampleFile.SampleFile) + '</a>'
                + '<br/><span class="sample-file-field-label">Acquired Date/Time:</span> ' + Ext4.util.Format.date(Ext4.Date.parse(sampleFile.AcquiredTime, LABKEY.Utils.getDateTimeFormatWithMS()), LABKEY.extDefaultDateTimeFormat || 'Y-m-d H:i:s')
        if (sampleFile.IgnoreForAllMetric) {
            content += '<div>Not included in QC</div>';
        }
        else if (!sampleFile.LeveyJennings && !sampleFile.mR && !sampleFile.CUSUMm && !sampleFile.CUSUMv) {
            content += '<div>No outliers</div>';
        }
        else {
            content += '<table class="labkey-data-region-legacy labkey-show-borders">';
            content += '<thead><tr><td class="labkey-column-header"></td><td class="labkey-column-header" colspan="7" align="center">Outliers</td></tr>' +
                            '<tr>' +
                                '<td class="labkey-column-header"/>' +
                                '<td class="labkey-column-header" colspan="2" /><td class="labkey-column-header" colspan="4" align="center">CUSUM</td>' +
                                '<td class="labkey-column-header"/>' +
                            '</tr>' +
                            '<tr>' +
                                '<td class="labkey-column-header outlier-column-header">Metric</td>' +
                                '<td class="labkey-column-header outlier-column-header">Levey-Jennings</td>' +
                                '<td class="labkey-column-header outlier-column-header">Moving Range</td>' +
                                '<td class="labkey-column-header outlier-column-header" title="Mean CUSUM-">Mean-</td>' +
                                '<td class="labkey-column-header outlier-column-header" title="Mean CUSUM+">Mean+</td>' +
                                '<td class="labkey-column-header outlier-column-header" title="Variability CUSUM-">Variability-</td>' +
                                '<td class="labkey-column-header outlier-column-header" title="Variability CUSUM+">Variability+</td>' +
                                '<td class="labkey-column-header outlier-column-header"><b>Total</b></td>' +
                            '</tr>' +
                        '</thead><tbody>';

            var rowCount = 0;
            Ext4.each(sampleFile.Metrics, function (item)
            {
                var href = LABKEY.ActionURL.buildURL('project', 'begin', item.ContainerPath, {metric: item.MetricId});
                content += '<tr class="' + (rowCount % 2 === 0 ? 'labkey-alternate-row' : 'labkey-row') + '">';
                content += '<td class="outlier-metric-label"><a href="' + href + '">' + Ext4.util.Format.htmlEncode(item.MetricLabel) + '</a></td>';
                if (item.IgnoreInQC) {
                    content += '<td align="center" colspan="6"><em>not included in QC</em></td>';
                }
                else {
                    content += '<td align="right">' + this.getSampleDetailOutlierDisplayValue(item, 'LeveyJennings') + '</td>';
                    content += '<td align="right">' + this.getSampleDetailOutlierDisplayValue(item, 'mR') + '</td>';
                    content += '<td align="right">' + this.getSampleDetailOutlierDisplayValue(item, 'CUSUMmN') + '</td>';
                    content += '<td align="right">' + this.getSampleDetailOutlierDisplayValue(item, 'CUSUMmP') + '</td>';
                    content += '<td align="right">' + this.getSampleDetailOutlierDisplayValue(item, 'CUSUMvN') + '</td>';
                    content += '<td align="right">' + this.getSampleDetailOutlierDisplayValue(item, 'CUSUMvP') + '</td>';
                    content += '<td align="right">' + this.getSampleDetailOutlierDisplayValue(item, 'Total') + '</td>';
                }
                content += '</tr>';
                rowCount++;
            }, this);

            content += '<tr class="' + (rowCount % 2 === 0 ? 'labkey-alternate-row' : 'labkey-row') + '">';
            content += '<td class="outlier-metric-label"><b>Total</b></td>';
            content += '<td align="right">' + this.getSampleDetailOutlierDisplayValue(sampleFile, 'LeveyJennings') + '</td>';
            content += '<td align="right">' + this.getSampleDetailOutlierDisplayValue(sampleFile, 'mR') + '</td>';
            content += '<td align="right">' + this.getSampleDetailOutlierDisplayValue(sampleFile, 'CUSUMmN') + '</td>';
            content += '<td align="right">' + this.getSampleDetailOutlierDisplayValue(sampleFile, 'CUSUMmP') + '</td>';
            content += '<td align="right">' + this.getSampleDetailOutlierDisplayValue(sampleFile, 'CUSUMvN') + '</td>';
            content += '<td align="right">' + this.getSampleDetailOutlierDisplayValue(sampleFile, 'CUSUMvP') + '</td>';
            content += '<td align="right">' + this.getSampleDetailOutlierDisplayValue(sampleFile, 'Total') + '</td>';
            content += '</tr>';

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
                    width: sampleFile.Metrics.length > 0 ? 720 : 300,
                    title: 'Sample Details',
                    content: content,
                    onShow: this.attachHopscotchMouseClose
                });
            }, this, [divEl]);
        }, this);

        // cancel the hover details show event if the user was just passing over the div without stopping for X amount of time
        divEl.on('mouseout', function() {
            task.cancel();
        }, this);
    },

    attachHopscotchMouseClose: function() {
        var closeTask = new Ext4.util.DelayedTask();
        var h = Ext4.select('.hopscotch-bubble-container');

        // on mouseout call the delayed task to close the callout
        h.on('mouseout', function() {
            closeTask.delay(1000, function() {
                hopscotch.getCalloutManager().removeAllCallouts();
            });
        });

        // if the mouseover happens again for this element before the delay, cancel it to keep callout open
        h.on('mouseover', function() {
            closeTask.cancel();
        });
    },

    getSampleDetailOutlierDisplayValue : function(item, variable) {
        var value = item[variable];
        if ('Total' === variable) {
            value = item['LeveyJennings'] + item['mR'] + item['CUSUMmN'] + item['CUSUMmP'] + item['CUSUMvN'] + item['CUSUMvP'];
        }
        return value ? ('<b>' + value + '</b>') : 0
    },
    
    sortObjectOfObjects: function (data, attr) {
        var arr = [];
        for (var prop in data) {
            if (data.hasOwnProperty(prop)) {
                var obj = {};
                obj[prop] = data[prop];
                obj.tempSortName = data[prop][attr];
                arr.push(obj);
            }
        }

        arr.sort(function(a, b) {
            var at = a.tempSortName,
                    bt = b.tempSortName;
            return at > bt ? 1 : ( at < bt ? -1 : 0 );
        });

        var result = [];
        for (var i=0, l=arr.length; i<l; i++) {
            var obj = arr[i];
            delete obj.tempSortName;
            for (var prop in obj) {
                if (obj.hasOwnProperty(prop)) {
                    var id = prop;
                }
            }
            var item = obj[id];
            result.push(item);
        }
        return result;
    }
});
