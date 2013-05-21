<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.targetedms.TargetedMSManager" %>
<%@ page import="org.labkey.targetedms.TargetedMSModule" %>
<%@ page import="org.labkey.targetedms.TargetedMSFolderType" %>
<%--
~ Copyright (c) 2012-2013 LabKey Corporation
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
<%
    String folderType = TargetedMSManager.getFolderType(getViewContext().getContainer());
    boolean isNull = folderType == null;
    boolean isUndefined = TargetedMSModule.FolderType.Undefined.toString().equalsIgnoreCase(folderType);
    boolean isExperiment = TargetedMSModule.FolderType.Experiment.toString().equalsIgnoreCase(folderType);
    boolean isSet = ( isExperiment ||
            TargetedMSModule.FolderType.Library.toString().equalsIgnoreCase(folderType) ||
            TargetedMSModule.FolderType.LibraryProtein.toString().equalsIgnoreCase(folderType) );

    if (isNull)
    {
%>
  The Targeted MS folder type cannot be configured for this page.
<%
    }
%>

<div id="folder-type-set" <%= text(isUndefined ? "" : "style=\"display:none\"") %> >
    <form action="<%= h(new ActionURL(TargetedMSController.FolderSetupAction.class, getViewContext().getContainer())) %>" method="post">
        <table cellspacing="7" width="100%">
            <tr>
                <td>
                    <input type="radio" name="folderType" id="experimentalData" value="<%= h(TargetedMSModule.FolderType.Experiment.toString()) %>" <%= text(isUndefined || isExperiment ? "checked" : "" )%>> <b>Experimental data</b> - a collection of published Skyline documents for various experimental designs</br>
                </td>
            </tr>
            <tr>
                <td>
                <input type="radio" name="folderType" id="chromatogramLibrary" value="<%= h(TargetedMSModule.FolderType.Library.toString()) %>"> <b>Chromatogram library</b> - curated precursor and product ion expression data for use in designing and validating future experiments</br>
                </td>
            </tr>
            <tr>
                <td>
                &nbsp &nbsp &nbsp &nbsp &nbsp &nbsp &nbsp  <input type="checkbox" name="precursorNormalized" value="true">Rank peptides within proteins by peak area<br>
                </td>
            </tr>
            <tr>
                <td align="right">
                    <labkey:button text="Finish" />
                </td>
            </tr>
        </table>

    </form>
</div>

<div id="folder-type-unset" <%= text(isSet ? "" : "style=\"display:none\"") %> >
    This Targeted MS folder has already been configured with the following folder type: '<%= h(folderType)%>'.<br>
</div>

