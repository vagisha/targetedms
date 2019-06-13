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
<%@ page import="org.apache.commons.io.FileUtils" %>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.security.permissions.UpdatePermission" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.targetedms.SkylineFileUtils" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.targetedms.TargetedMSRun" %>
<%@ page import="java.nio.file.Path" %>
<%@ page import="java.nio.file.Files" %>
<%@ page import="org.labkey.targetedms.TargetedMSManager" %>
<%@ page import="org.labkey.targetedms.TargetedMSSchema" %>
<%@ page import="org.labkey.api.util.StringUtilsLabKey" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<%
    JspView<TargetedMSController.RunDetailsBean> me = (JspView<TargetedMSController.RunDetailsBean>) HttpView.currentView();
    TargetedMSController.RunDetailsBean bean = me.getModelBean();
    TargetedMSRun run = bean.getRun();
    Path skyDocFile = SkylineFileUtils.getSkylineFile(run.getExperimentRunLSID(), getContainer());

    ActionURL downloadAction = new ActionURL(TargetedMSController.DownloadDocumentAction.class, getContainer());
    downloadAction.addParameter("id", run.getId());
    Container c = getContainer();

    ActionURL versionsAction = new ActionURL(TargetedMSController.ShowVersionsAction.class, getContainer());
    versionsAction.addParameter("id", run.getId());

    ActionURL precursorListAction = new ActionURL(TargetedMSController.ShowPrecursorListAction.class, getContainer());
    precursorListAction.addParameter("id", run.getId());

    ActionURL transitionListAction = new ActionURL(TargetedMSController.ShowTransitionListAction.class, getContainer());
    transitionListAction.addParameter("id", run.getId());

    ActionURL calibrationCurveListAction = new ActionURL(TargetedMSController.ShowCalibrationCurvesAction.class, getContainer());
    calibrationCurveListAction.addParameter("id", run.getId());

    ActionURL replicateListAction = new ActionURL(TargetedMSController.ShowReplicatesAction.class, getContainer());
    replicateListAction.addParameter("id", run.getId());

    ActionURL renameAction = null;
    if(c.hasPermission(getUser(), UpdatePermission.class))
        renameAction = TargetedMSController.getRenameRunURL(c, run, getActionURL());

    String peptideGroupLabel = TargetedMSManager.containerHasSmallMolecules(getContainer()) ? TargetedMSSchema.COL_LIST.toLowerCase() : TargetedMSSchema.COL_PROTEIN.toLowerCase();
%>


<div id="targetedmsDocumentSummary">
    <div><strong>Name:</strong> <%= h(run.getDescription()) %>
        <%= h(run.getFileName() == null || run.getFileName().equals(run.getDescription()) ? "" : (", from file " + run.getFileName())) %>
        <%= h(skyDocFile == null ? "" : "(" + FileUtils.byteCountToDisplaySize(Files.size(skyDocFile)) + ")") %>
        <% if (renameAction != null) { %><%= iconLink("edit-views-link fa fa-pencil", "Rename File", renameAction) %><% } %>
        <% if (run.getFileName() != null) { TargetedMSController.createDownloadMenu(run).render(out); } %>&nbsp;
        <a href="<%= h(versionsAction) %>"><%= h(StringUtilsLabKey.pluralize(bean.getVersionCount(), "version"))%></a>
    </div>
    &nbsp;
    <div>
        <a href="<%= h(precursorListAction.getLocalURIString()) %>"><%= h(StringUtilsLabKey.pluralize(run.getPeptideGroupCount(), peptideGroupLabel))%></a>,
        <% if (run.getPeptideCount() > 0) { %><a href="<%= h(precursorListAction.getLocalURIString()) %>"><%= h(StringUtilsLabKey.pluralize(run.getPeptideCount(), "peptide"))%></a>,<% } %>
        <% if (run.getSmallMoleculeCount() > 0) { %><a href="<%= h(precursorListAction.getLocalURIString() + "#Small Molecule Precursor List") %>"><%= h(StringUtilsLabKey.pluralize(run.getSmallMoleculeCount(), "small molecule"))%></a>,<% } %>
        <a href="<%= h(precursorListAction.getLocalURIString()) %>"><%= h(StringUtilsLabKey.pluralize(run.getPrecursorCount(), "precursor"))%></a>,
        <a href="<%= h(transitionListAction.getLocalURIString()) %>"><%= h(StringUtilsLabKey.pluralize(run.getTransitionCount(), "transition"))%></a>
        &nbsp;-&nbsp;
        <a href="<%= h(replicateListAction.getLocalURIString()) %>"><%= h(StringUtilsLabKey.pluralize(run.getReplicateCount(), "replicate"))%></a><%
        if (run.getCalibrationCurveCount() > 0) { %>, <a href="<%= h(calibrationCurveListAction.getLocalURIString()) %>"><%= h(StringUtilsLabKey.pluralize(run.getCalibrationCurveCount(), "calibration curve"))%></a><% } %>
        <% if (run.getSoftwareVersion() != null) { %>&nbsp;-&nbsp; <%= h(run.getSoftwareVersion()) %> <% } %>
    </div>
</div>