<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.targetedms.TargetedMSManager" %>
<%@ page import="org.labkey.targetedms.TargetedMSModule" %>
<%@ page import="org.labkey.targetedms.chromlib.ChromatogramLibraryUtils" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="java.io.File" %>
<%@ page import="org.labkey.api.util.FileUtil" %>
<%@ page import="org.apache.commons.io.FileUtils" %>
<%@ page import="java.util.Date" %>
<%--
~ Copyright (c) 2013 LabKey Corporation
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
    TargetedMSModule.FolderType folderType = TargetedMSManager.getFolderType(getViewContext().getContainer());

    boolean isLibrary = ( folderType == TargetedMSModule.FolderType.Library ||
                          folderType == TargetedMSModule.FolderType.LibraryProtein );

    int currentRevision = ChromatogramLibraryUtils.getCurrentRevision(getViewContext().getContainer());

%>

<h3>Download Archived Revisions</h3>
<table>
<%
    for (int i=1; i <= currentRevision; i++)
    {
        ActionURL u = new ActionURL(TargetedMSController.DownloadChromLibraryAction.class, getViewContext().getContainer());
        u.addParameter("revision", i);
        File archiveFile = ChromatogramLibraryUtils.getChromLibFile(getViewContext().getContainer(), i);
        if (archiveFile.isFile()) {
%>
    <tr>
        <td><%= PageFlowUtil.textLink("Revision " + i, u) %></td>
        <td><%= h(FileUtils.byteCountToDisplaySize(archiveFile.length())) %>, created <%= h(formatDateTime(new Date(archiveFile.lastModified())))%></td>
    </tr>
<%
        }
        else
        {
%><tr>
    <td>Revision <%= i %></td>
    <td>unavailable<br/></td>
</tr><%
        }
    }
%>