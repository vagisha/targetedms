<%
/*
 * Copyright (c) 2012 LabKey Corporation
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
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.ViewContext"%>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.template.ClientDependency" %>
<%@ page import="java.io.File" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    ViewContext context = HttpView.currentContext();
    org.labkey.targetedms.TargetedMSController.SkylinePipelinePathForm form = (org.labkey.targetedms.TargetedMSController.SkylinePipelinePathForm)HttpView.currentModel();
    org.labkey.api.view.ActionURL targetURL = new org.labkey.api.view.ActionURL(TargetedMSController.SkylineDocUploadAction.class, context.getContainer());
%>
<table>
    <tr>
        <th>Representative</th>
        <th>File Name</th>
    </tr>
    <form action="<%= h(targetURL) %>" method="POST">
        <input type="hidden" name="path" value="<%= h(form.getPath() )%>" />
        <% for (java.io.File file : form.getValidatedFiles(context.getContainer()))
        { %>
            <tr>
                <td><input name="representative" type="checkbox" value="<%=h(file.getName()) %>"/></td>
                <td><input type="hidden" name="file" value="<%= h(file.getName()) %>" /><%= h(file.getName())%></td>
            </tr><%
        } %>
        <tr>
            <td colspan="2">
                <%= generateSubmitButton("Import") %> <%= generateButton("Cancel", form.getReturnActionURL(context.getContainer().getStartURL(context.getUser())))%>
            </td>
        </tr>
    </form>
</table>