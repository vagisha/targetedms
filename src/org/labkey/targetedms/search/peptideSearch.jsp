<%
/*
 * Copyright (c) 2012-2017 LabKey Corporation
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
<%@ page import="org.labkey.api.protein.ProteinService" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.targetedms.query.JournalManager" %>
<%@ page import="org.labkey.api.ms1.MS1Urls" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<%
    boolean isJournalProject = JournalManager.isJournalProject(getContainer());
%>

<labkey:form action="<%=h(PageFlowUtil.urlProvider(MS1Urls.class).getPepSearchUrl(getContainer()))%>" method="get">
    <table class="lk-fields-table">
        <tr>
            <td class="labkey-form-label">Peptide sequence *<%=helpPopup("Peptide Sequence", "Enter the peptide sequence to find, or multiple sequences separated by commas. Use * to match any sequence of characters.")%></td>
            <td><input id="pepSeq" type="text" name="<%=ProteinService.PeptideSearchForm.ParamNames.pepSeq.name()%>" value="" size="40"/></td>
        </tr>
        <tr>
            <td class="labkey-form-label">Exact matches only<%=helpPopup("Exact matches only", "If checked, the search will match the peptides exactly; if unchecked, it will match any peptide that starts with the specified sequence and ignore modifications.")%></td>
            <td><input id="cbxExact" type="checkbox" name="<%=ProteinService.PeptideSearchForm.ParamNames.exact.name()%>" style="vertical-align:middle"<%=checked(true)%> />
        </tr>
        <% if (isJournalProject) {%>
            <input type="hidden" name="<%=ProteinService.PeptideSearchForm.ParamNames.subfolders.name()%>" value="true"/>
        <% }else{ %>
        <tr>
            <td class="labkey-form-label">Search in subfolders<%=helpPopup("Search in subfolders", "Check to search this folder and all of its descendants.")%></td>
            <td><input id="cbxSubfolders" type="checkbox" name="<%=ProteinService.PeptideSearchForm.ParamNames.subfolders.name()%>" style="vertical-align:middle"<%=checked(false)%> /></td>
        </tr>
        <% } %>
        <tr>
            <td colspan="2" style="padding-top: 10px;">
                <labkey:button text="Search" />
            </td>
        </tr>
    </table>
</labkey:form>

