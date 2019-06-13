<%
/*
 * Copyright (c) 2012-2019 LabKey Corporation
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
<%@ page import="org.labkey.api.view.ActionURL"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    TargetedMSController.SkylinePipelinePathForm form = (TargetedMSController.SkylinePipelinePathForm)HttpView.currentModel();
    ActionURL targetURL = new org.labkey.api.view.ActionURL(TargetedMSController.SkylineDocUploadAction.class, getContainer());
%>
<script type="text/javascript">
    Ext.onReady(function() {

        Ext.select('.repr_cb').on('click', function(event, target) {

            if(target.checked)
            {
                if(target.name == 'proteinRepresentative')
                {
                    document.getElementById('peptide_'+target.value).checked= false;
                }
                else
                {
                    document.getElementById('protein_'+target.value).checked= false;
                }
            }
        });
    });
</script>

<table cellspacing="0" cellpadding="5" class="labkey-show-borders">
    <tr>
        <th>Representative</th>
        <th>File Name</th>
    </tr>
    <labkey:form action="<%= h(targetURL) %>" method="POST">
        <input type="hidden" name="path" value="<%= h(form.getPath() )%>" />
        <% for (java.io.File file : form.getValidatedFiles(getContainer()))
        { %>
            <tr style="border:1px solid;">
                <td>
                    <input name="proteinRepresentative" class="repr_cb" id="protein_<%=h(file.getName()) %>" type="checkbox" value="<%=h(file.getName()) %>"/>Protein
                    <input name="peptideRepresentative" class="repr_cb" id="peptide_<%=h(file.getName()) %>" type="checkbox" value="<%=h(file.getName()) %>"/>Peptide
                </td>
                <td><input type="hidden" name="file" value="<%= h(file.getName()) %>" /><%= h(file.getName())%></td>
            </tr><%
        } %>
        <tr>
            <td colspan="2" align="center">
                <%= button("Import").submit(true) %> <%= button("Cancel").href(form.getReturnActionURL(getContainer().getStartURL(getUser()))) %>
            </td>
        </tr>
    </labkey:form>
</table>