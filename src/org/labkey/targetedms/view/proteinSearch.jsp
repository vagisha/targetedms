<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.ms2.MS2Urls" %>
<%--
~ Copyright (c) 2012 LabKey Corporation
~
~ Licensed under the Apache License, Version 2.0 (the "License");
~ You may obtain a copy of the License at
~
~ http://www.apache.org/licenses/LICENSE-2.0
~
~ Unless required by applicable law or agreed to in writing, software
~ distributed under the License is distributed on an "AS IS" BASIS,
~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
~ See the License for the specific language governing permissions and
~ limitations under the License.
--%>
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<form action="<%= h(PageFlowUtil.urlProvider(MS2Urls.class).getProteinSearchUrl(getViewContext().getContainer())) %>" method="get">

    <table>
        <tr>
            <td class="labkey-form-label"><label for="identifierInput">Protein Name</label> *<%= helpPopup("Protein name", "Required to search for proteins. You may use the name as specified by the FASTA file, or an annotation, such as a gene name, that has been loaded from an annotations file. You may comma separate multiple names.") %></td>
            <td nowrap><input size="20" type="text" id="identifierInput" name="identifier" value=""/></td>
        </tr>
        <tr>
            <td class="labkey-form-label"><label for="includeSubfoldersInput">Search in subfolders</label><%= helpPopup("Search in subfolders", "If checked, the search will also look in all of this folder's children.") %></td>
            <td nowrap><input type="checkbox" id="includeSubfoldersInput" name="includeSubfolders" checked="true" /></td>
        </tr>
        <tr>
            <td colspan="2"><labkey:button text="Search" /></td>
        </tr>
    </table>

</form>

