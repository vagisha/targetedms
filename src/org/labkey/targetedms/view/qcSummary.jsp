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
<%@ page import="org.labkey.targetedms.SkylineDocImporter" %>
<%@ page import="org.labkey.api.util.UniqueID" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    int uid = UniqueID.getRequestScopedUID(HttpView.currentRequest());
    String qcSummaryId = "qcSummary-" + uid;
%>

<div id=<%=q(qcSummaryId)%>></div>

<script type="text/javascript">
    function init()
    {
        var pluralize = function(val)
        {
            return val == 1 ? '' : 's';
        };

        LABKEY.Ajax.request({
            url: LABKEY.ActionURL.buildURL('targetedms', 'getQCSummary', null, {includeSubfolders: true}),
            scope: this,
            success: LABKEY.Utils.getCallbackWrapper(function(response)
            {
                var html = '', fileUrl;

                Ext4.each(response['containers'], function(container)
                {
                    fileUrl = LABKEY.ActionURL.buildURL('query', 'executeQuery', container['path'],
                                {schemaName: 'targetedms', 'query.queryName': 'SampleFile'});

                    // it we have subfolders, first write out the container name
                    html += (container['subfolder'] || response['containers'].length > 1 ?
                            (container['subfolder'] ? '<br/>' : '') + '<span style="font-weight: bold;">'
                            + container['name'] + '</span><br/>' : '');

                    // skyline document count and count/link for sample files
                    html += container['docCount'] + ' Skyline document' + pluralize(container['docCount'])
                            + ' uploaded containing ' + '<a href="' + fileUrl + '">' + container['fileCount']
                            + ' sample file' + pluralize(container['fileCount']) + '</a><br/>';

                    // precursor count
                    html += container['precursorCount'] + ' precursor' + pluralize(container['precursorCount']) + ' tracked<br/>';

                    // date of last import
                    html += (container['lastImportDate'] != null ? 'Date of last import: '
                            + Ext4.util.Format.date(container['lastImportDate'], LABKEY.extDefaultDateFormat)
                            + '<br/>' : '');
                });

                Ext4.get(<%=q(qcSummaryId)%>).update(html);
            }, this, false),
            failure: LABKEY.Utils.getCallbackWrapper(function(response)
            {
                Ext4.get(<%=q(qcSummaryId)%>).update("<span class='labkey-error'>Error: " + response.exception + '</span>');
            }, this, true)
        });
    }
    Ext4.onReady(init);
</script>