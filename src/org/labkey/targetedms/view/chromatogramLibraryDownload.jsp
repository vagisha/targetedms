<%
/*
 * Copyright (c) 2013-2016 LabKey Corporation
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
<%@ page import="org.apache.commons.io.FileUtils" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.targetedms.TargetedMSManager" %>
<%@ page import="org.labkey.targetedms.TargetedMSModule" %>
<%@ page import="org.labkey.targetedms.chromlib.ChromatogramLibraryUtils" %>
<%@ page import="org.labkey.targetedms.query.ConflictResultsManager" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.security.User" %>
<%@ page import="java.nio.file.Path" %>
<%@ page import="java.nio.file.Files" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    Container c = getContainer();
    User user = getUser();
    TargetedMSModule.FolderType folderType = TargetedMSManager.getFolderType(c);

    long peptideGroupCount = TargetedMSController.getNumRepresentativeProteins(user, c);
    long peptideCount = TargetedMSController.getNumRepresentativePeptides(c);
    long transitionCount = TargetedMSController.getNumRankedTransitions(c);
    DecimalFormat format = new DecimalFormat("#,###");
    int currentRevision = ChromatogramLibraryUtils.getCurrentRevision(c, user);
    Path archiveFile = ChromatogramLibraryUtils.getChromLibFile(c, currentRevision);

    long conflictCount = ConflictResultsManager.getConflictCount(user, c);
    String conflictViewUrl = (folderType == TargetedMSModule.FolderType.LibraryProtein) ?
                                           new ActionURL(TargetedMSController.ShowProteinConflictUiAction.class, c).getLocalURIString() :
                                           new ActionURL(TargetedMSController.ShowPrecursorConflictUiAction.class, c).getLocalURIString();
%>

<div class="labkey-download"><style type="text/css">

    div.banner {
            margin-top: 15px;
background: #ffffff; /* Old browsers */
background: #ffffff; /* Old browsers */
            /* IE9 SVG, needs conditional override of 'filter' to 'none' */
            background: -moz-linear-gradient(top,  #ffffff 0%, #e0e6eb 100%); /* FF3.6+ */
            background: -webkit-gradient(linear, left top, left bottom, color-stop(0%,#ffffff), color-stop(100%,#e0e6eb)); /* Chrome,Safari4+ */
            background: -webkit-linear-gradient(top,  #ffffff 0%,#e0e6eb 100%); /* Chrome10+,Safari5.1+ */
            background: -o-linear-gradient(top,  #ffffff 0%,#e0e6eb 100%); /* Opera 11.10+ */
            background: -ms-linear-gradient(top,  #ffffff 0%,#e0e6eb 100%); /* IE10+ */
            background: linear-gradient(to bottom,  #ffffff 0%,#e0e6eb 100%); /* W3C */
            filter: progid:DXImageTransform.Microsoft.gradient( startColorstr='#ffffff', endColorstr='#e0e6eb',GradientType=0 ); /* IE6-8 */
    }


a.banner-button {
        display: block;
        width: 200px;
        height: 30px;
        color: #fff;
        border-radius: 20px;
        font-size: 115%;
        font-weight: bold;
        border: 1px solid #215da0;
        padding-top: 10px;
        text-shadow: -1px -1px #2e6db3;
        box-shadow: 0 2px #ccc;

        background: #73a0e2; /* Old browsers */
        /* IE9 SVG, needs conditional override of       'filter' to 'none' */
        background: url(data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiA/Pgo8c3ZnIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgdmlld0JveD0iMCAwIDEgMSIgcHJlc2VydmVBc3BlY3RSYXRpbz0ibm9uZSI+CiAgPGxpbmVhckdyYWRpZW50IGlkPSJncmFkLXVjZ2ctZ2VuZXJhdGVkIiBncmFkaWVudFVuaXRzPSJ1c2VyU3BhY2VPblVzZSIgeDE9IjAlIiB5MT0iMCUiIHgyPSIwJSIgeTI9IjEwMCUiPgogICAgPHN0b3Agb2Zmc2V0PSIwJSIgc3RvcC1jb2xvcj0iIzczYTBlMiIgc3RvcC1vcGFjaXR5PSIxIi8+CiAgICA8c3RvcCBvZmZzZXQ9IjEwMCUiIHN0b3AtY29sb3I9IiMyMTVkYTAiIHN0b3Atb3BhY2l0eT0iMSIvPgogIDwvbGluZWFyR3JhZGllbnQ+CiAgPHJlY3QgeD0iMCIgeT0iMCIgd2lkdGg9IjEiIGhlaWdodD0iMSIgZmlsbD0idXJsKCNncmFkLXVjZ2ctZ2VuZXJhdGVkKSIgLz4KPC9zdmc+);
        background: -moz-linear-gradient(top,  #73a0e2 0%, #215da0 100%); /* FF3.6+ */
        background: -webkit-gradient(linear, left top, left bottom, color-stop(0%,#73a0e2), color-stop(100%,#215da0)); /* Chrome,Safari4+ */
        background: -webkit-linear-gradient(top,  #73a0e2 0%,#215da0 100%); /* Chrome10+,Safari5.1+ */
        background: -o-linear-gradient(top,  #73a0e2 0%,#215da0 100%); /* Opera 11.10+ */
        background: -ms-linear-gradient(top,  #73a0e2 0%,#215da0 100%); /* IE10+ */
        background: linear-gradient(to bottom,  #73a0e2 0%,#215da0 100%); /* W3C */
        filter: progid:DXImageTransform.Microsoft.gradient( startColorstr='#73a0e2', endColorstr='#215da0',GradientType=0 ); /* IE6-8 */
}

div.labkey-download h3 {
    font-size: 125%;
}


</style>
<!--[if gte IE 9]>
  <style type="text/css">
    div.banner {
       filter: none;
    }
  </style>
<![endif]-->

<%
        if (peptideCount > 0 || peptideGroupCount > 0)
        {
%>
<div class="banner">
<table>
<tr>
    <! -- graph # of proteins and peptides in the library -->
<td><img src="<%= h(new ActionURL(TargetedMSController.GraphLibraryStatisticsAction.class, c)) %>"></td>
<td valign="top" align="center">
<br>
<h3><%= h(c.getName())%> Library</h3>
<a href="<%= h(new ActionURL(TargetedMSController.DownloadChromLibraryAction.class, c)) %>" class="banner-button">Download</a> <br/>
        <%= h(ChromatogramLibraryUtils.getDownloadFileName(c, currentRevision)) %>
    <%= h(Files.exists(archiveFile) ? "(" + FileUtils.byteCountToDisplaySize(Files.size(archiveFile)) + ")" : "") %>
    <br/>
    Revision <%= h(currentRevision)%><br/>
    <br/>
<%= PageFlowUtil.textLink("Archived Revisions", new ActionURL(TargetedMSController.ArchivedRevisionsAction.class, c))%>
</tr>
</table>
</div>
<table width="100%">
<tr>
<td width="80%">
    <h3>Library Statistics:</h3>
    <p class="banner">
<%
            if (folderType == TargetedMSModule.FolderType.Library)
            {
%>
        The library contains <%= h(format.format(peptideCount))%> peptides with <%= h(format.format(transitionCount))%> ranked transitions.

<%
                if(conflictCount > 0) {
%>
                    <div style="color:red; font-weight:bold; margin-top:10px">
                        There are <%=conflictCount%> conflicting peptides in this folder.
                        <a style="color:red; text-decoration:underline;" href="<%= h(conflictViewUrl) %>">Resolve conflicts</a>
                    </div>
<%
                }
%>
<%
            }
            else if (folderType == TargetedMSModule.FolderType.LibraryProtein)
            {
%>
        The library contains <%= h(format.format(peptideGroupCount))%> proteins with <%= h(format.format(peptideCount)) %> ranked peptides.<br>
        The <%=h(format.format(peptideCount))%> ranked peptides contain <%= h(format.format(transitionCount))%> ranked transitions.

<%
                if(conflictCount > 0) {
%>
                    <div style="color:red; font-weight:bold; margin-top:10px">
                        There are <%=conflictCount%> conflicting proteins in this folder.
                        <a style="color:red; text-decoration:underline;" href="<%= h(conflictViewUrl) %>">Resolve conflicts</a>
                    </div>
<%
                }
%>
<%
            }
%>
</td><td valign="top" width="20%" nowrap>
</td>
</tr>
</table>
<%
        }
        else
        {
%>
  This library does not currently contain any data. Import a file in the Data Pipeline to proceed.
<%
        }
%>
