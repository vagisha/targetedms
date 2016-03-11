<%
/*
 * Copyright (c) 2014-2015 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
%>
<%@ page import="org.labkey.api.util.UniqueID" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.template.ClientDependency" %>
<%@ page import="java.util.LinkedHashSet" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    public LinkedHashSet<ClientDependency> getClientDependencies()
    {
        LinkedHashSet<ClientDependency> resources = new LinkedHashSet<>();
        resources.add(ClientDependency.fromPath("Ext4"));
        return resources;
    }
%>
<%!
    int uid = UniqueID.getRequestScopedUID(HttpView.currentRequest());
    String qcSummaryId = "qcSummary-" + uid;
%>

<div id=<%=q(qcSummaryId)%>></div>

<style>
    .summary-view {
        padding: 5px;
        margin: 5px;
        position: relative;
    }

    .subfolder-view {
        background-color: #EEEEEE;
        border: solid #C0C0C0 1px;
        border-radius: 5px;
        display: inline-block;
        vertical-align: top;
    }

    .subfolder-view .item-text {
        color: #303030;
    }

    .folder-name {
        font-weight: bold;
    }

    .folder-name a {
        color: #000000;
    }

    .auto-qc-ping-none, .auto-qc-ping-not-recent, .auto-qc-ping-recent {
        width: 15px;
        height: 15px;
        border-radius: 3px;
        font-size: 16px;
        position: absolute;
        top: 5px;
        right: 5px;
        cursor: pointer
    }
    .auto-qc-ping-none {
        color: grey;
    }
    .auto-qc-ping-not-recent {
        color: red;
    }
    .auto-qc-ping-recent {
        color: green;
    }
</style>

<script type="text/javascript">
    Ext4.onReady(function()
    {
        Ext4.create('LABKEY.targetedms.QCSummary', {
            renderTo: <%=q(qcSummaryId)%>
        });
    });

    Ext4.define('LABKEY.targetedms.QCSummary', {
        extend: 'Ext.panel.Panel',
        border: false,
        initComponent : function()
        {
            this.callParent();

            LABKEY.Ajax.request({
                url: LABKEY.ActionURL.buildURL('targetedms', 'getQCSummary.api'),
                params: {includeSubfolders: true},
                scope: this,
                success: LABKEY.Utils.getCallbackWrapper(function(response)
                {
                    var containers = response['containers'],
                        container,
                        childPanelItems = [],
                        hasChildren = containers.length > 1;

                    var tpl = new Ext4.XTemplate(
                        '<tpl if="showName !== undefined">',
                            '<tpl if="showName === true">',
                                '<div class="folder-name">',
                                    '<a href="{path:this.getContainerLink}">{name:htmlEncode}</a>',
                                '</div>',
                            '</tpl>',
                            '<tpl if="docCount == 0">',
                                '<div class="item-text">No Skyline documents</div>',
                            '<tpl elseif="docCount &gt; 0">',
                                '<div class="item-text">{docCount} Skyline document{docCount:this.pluralize}</div>',
                                '<div class="item-text">',
                                    '<a href="{path:this.getSampleFileLink}">{fileCount} sample file{fileCount:this.pluralize}</a>',
                                '</div>',
                                '<div class="item-text">{precursorCount} precursor{precursorCount:this.pluralize}</div>',
                                '<div class="item-text">Last update {lastImportDate:this.formatDate}</div>',
                            '</tpl>',
                            '<div class="{autoQCPing:this.getAutoQCPingClass}" title="{autoQCPing:this.getAutoQCPingText}"></div>',
                        '</tpl>',
                        {
                            pluralize: function (val)
                            {
                                return val == 1 ? '' : 's';
                            },
                            formatDate: function (val)
                            {
                                return Ext4.util.Format.date(val, LABKEY.extDefaultDateFormat);
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
                            getAutoQCPingClass: function(val)
                            {
                                if (val == null)
                                    return 'auto-qc-ping-none fa fa-circle-o';
                                return val.isRecent ? 'auto-qc-ping-recent fa fa-check-circle' : 'auto-qc-ping-not-recent fa fa-circle';
                            },
                            getAutoQCPingText: function(val)
                            {
                                if (val == null)
                                    return 'Has never been pinged';
                                return val.isRecent ? 'Was pinged recently on ' + val.modified : 'Was pinged on ' + val.modified
                            }
                        }
                    );

                    // Add the current (root) container to the QC Summary display
                    container = containers[0];
                    container.showName = hasChildren;
                    this.add(Ext4.create('Ext.view.View', {
                        cls: hasChildren ? 'summary-view' : undefined,
                        width: hasChildren ? 250 : undefined,
                        minHeight: 21,
                        data: container,
                        tpl: tpl
                    }));

                    // Add the set of child containers in an hbox layout
                    if (hasChildren)
                    {
                        for (var i = 1; i < containers.length; i++)
                        {
                            container = containers[i];
                            container.showName = hasChildren;

                            childPanelItems.push(Ext4.create('Ext.view.View', {
                                cls: 'summary-view subfolder-view',
                                width: 250,
                                minHeight: 97,
                                data: container,
                                tpl: tpl
                            }));
                        }

                        this.add(Ext4.create('Ext.panel.Panel', {
                            border: false,
                            items: childPanelItems
                        }));
                    }

                }, this, false),
                failure: LABKEY.Utils.getCallbackWrapper(function(response)
                {
                    this.add(Ext4.create('Ext.Component', {
                        autoEl: 'span',
                        cls: 'labkey-error',
                        html: 'Error: ' + response.exception
                    }));
                }, this, true)
            });
        }
    });
</script>