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
<%@ page import="org.labkey.api.targetedms.TargetedMSService" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.targetedms.TargetedMSManager" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    TargetedMSService.FolderType folderType = TargetedMSManager.getFolderType(getContainer());
    boolean isNull = folderType == null;
    boolean isUndefined = folderType == TargetedMSService.FolderType.Undefined;
    boolean isExperiment = folderType == TargetedMSService.FolderType.Experiment;
    boolean isSet = ( isExperiment ||
            folderType == TargetedMSService.FolderType.Library ||
            folderType == TargetedMSService.FolderType.LibraryProtein ||
            folderType == TargetedMSService.FolderType.QC);

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

<div id="folder-type-set" <%= unsafe(isUndefined ? "" : "style=\"display:none\"") %> >
    <labkey:form action="<%= new ActionURL(TargetedMSController.FolderSetupAction.class, getContainer()) %>" method="post">
        <table cellspacing="7" width="100%">
            <tr class="spaceUnder">
                <td>
                    <input type="radio" name="folderType" id="experimentalData" value="<%= h(TargetedMSService.FolderType.Experiment.toString()) %>"<%=checked(isUndefined || isExperiment)%>> <label for="experimentalData"><strong>Experimental data</strong></label>
                    - A collection of published Skyline documents for various experimental designs
                </td>
            </tr>
            <tr class="spaceUnder">
                <td>
                    <input type="radio" name="folderType" id="multiAttributeMethod" value="<%= h(TargetedMSService.FolderType.ExperimentMAM.toString()) %>"> <label for="multiAttributeMethod"><strong>Multi-attribute method (MAM)</strong></label> - An experimental data folder variant with additional reporting for multi-attribute method analyses
                </td>
            </tr>
            <tr>
                <td>
                    <input type="radio" name="folderType" id="chromatogramLibrary" value="<%= h(TargetedMSService.FolderType.Library.toString()) %>"> <label for="chromatogramLibrary"><strong>Chromatogram library</strong></label> - Curated precursor and product ion expression data for use in designing and validating future experiments
                </td>
            </tr>
            <tr>
                <td style="padding: 14px 0 14px 25px;">
                    <input type="checkbox" name="precursorNormalized" id="precursorNormalized" value="true"><label for="precursorNormalized">Rank peptides within proteins by peak area</label>
                </td>
            </tr>
            <tr>
                <td>
                    <input type="radio" name="folderType" id="QC" value="<%= h(TargetedMSService.FolderType.QC.toString()) %>"> <label for="QC"><strong>Quality control</strong></label> - System suitability monitoring of reagents and instruments
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

<div id="folder-type-unset" <%= unsafe(isSet ? "" : "style=\"display:none\"") %> >
    This Panorama folder has already been configured with the following folder type: '<%= h(folderType == null ? "none" : folderType.toString())%>'.<br>
</div>

