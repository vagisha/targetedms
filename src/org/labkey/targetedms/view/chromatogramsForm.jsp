<%
/*
 * Copyright (c) 2012-2013 LabKey Corporation
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
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<TargetedMSController.PeptideChromatogramsViewBean> me = (JspView<TargetedMSController.PeptideChromatogramsViewBean>) HttpView.currentView();
    TargetedMSController.PeptideChromatogramsViewBean bean = me.getModelBean();
%>

<form action="<%=bean.getResultsUri()%>" method="get">
<input type="hidden" name="id" value="<%=bean.getForm().getId()%>"/>
<table>
    <tr>
        <td class="labkey-form-label">Chart Width</td>
        <td><input type="text" name="chartWidth" value="<%= bean.getForm().getChartWidth() %>"/></td>
        <td class="labkey-form-label">Chart Height</td>
        <td><input type="text" name="chartHeight" value="<%= bean.getForm().getChartHeight() %>"/></td>
    </tr>
    <tr>
        <td class="labkey-form-label">Synchronize Y-axis</td>
        <td nowrap><input type="checkbox" name="syncY"<%=checked(bean.getForm().isSyncY())%>/></td>
        <td class="labkey-form-label">Synchronize X-axis</td>
        <td nowrap><input type="checkbox" name="syncX"<%=checked(bean.getForm().isSyncX())%>/></td>
    </tr>
    <tr>
        <td colspan = "4"><labkey:button text="Update" /></td>
    </tr>
</table>
</form>