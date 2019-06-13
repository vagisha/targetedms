<%
/*
 * Copyright (c) 2013-2019 LabKey Corporation
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
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.targetedms.TargetedMSManager" %>
<%@ page import="org.labkey.targetedms.TargetedMSModule" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    TargetedMSModule.FolderType folderType = TargetedMSManager.getFolderType(getContainer());
    boolean isNull = folderType == null;
    boolean isUndefined = folderType == TargetedMSModule.FolderType.Undefined;
    boolean isExperiment = folderType == TargetedMSModule.FolderType.Experiment;
    boolean isSet = ( isExperiment ||
            folderType == TargetedMSModule.FolderType.Library ||
            folderType == TargetedMSModule.FolderType.LibraryProtein ||
            folderType == TargetedMSModule.FolderType.QC);

    if (isNull)
    {
%>
  The Panorama folder type cannot be configured for this page.
<%
    }
%>

<style type="text/css">
tr.spaceUnder > td
{
  padding-bottom: 1em;
}
</style>

<div id="folder-type-set" <%= text(isUndefined ? "" : "style=\"display:none\"") %> >
    <labkey:form action="<%= new ActionURL(TargetedMSController.FolderSetupAction.class, getContainer()) %>" method="post">
        <table cellspacing="7" width="100%">
            <tr class="spaceUnder">
                <td>
                    <input type="radio" name="folderType" id="experimentalData" value="<%= h(TargetedMSModule.FolderType.Experiment.toString()) %>"<%=checked(isUndefined || isExperiment)%>> <b>Experimental data</b>
                    - A collection of published Skyline documents for various experimental designs
                </td>
            </tr>
            <tr>
                <td>
                    <input type="radio" name="folderType" id="chromatogramLibrary" value="<%= h(TargetedMSModule.FolderType.Library.toString()) %>"> <b>Chromatogram library</b> - Curated precursor and product ion expression data for use in designing and validating future experiments
                </td>
            </tr>
            <tr>
                <td style="padding: 14px 0 14px 25px;">
                    <input type="checkbox" name="precursorNormalized" value="true">Rank peptides within proteins by peak area
                </td>
            </tr>
            <tr>
                <td>
                    <input type="radio" name="folderType" id="QC" value="<%= h(TargetedMSModule.FolderType.QC.toString()) %>"> <b>QC</b> - Quality control metrics of reagents and instruments
                </td>
            </tr>
            <tr>
                <td align="right">
                    <labkey:button text="Finish" />
                </td>
            </tr>
        </table>
    </labkey:form>
</div>

<div id="folder-type-unset" <%= text(isSet ? "" : "style=\"display:none\"") %> >
    This Panorama folder has already been configured with the following folder type: '<%= h(folderType == null ? "none" : folderType.toString())%>'.<br>
</div>

