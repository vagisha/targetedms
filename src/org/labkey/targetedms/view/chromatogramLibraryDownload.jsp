<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.ms2.MS2Urls" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
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

<div class="labkey-download"><style type="text/css">

    div.banner {
            margin-top: 15px;
            background: #ffffff; /* Old browsers */
            /* IE9 SVG, needs conditional override of 'filter' to 'none' */
            background: url(data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiA/Pgo8c3ZnIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgdmlld0JveD0iMCAwIDEgMSIgcHJlc2VydmVBc3BlY3RSYXRpbz0ibm9uZSI+CiAgPGxpbmVhckdyYWRpZW50IGlkPSJncmFkLXVjZ2ctZ2VuZXJhdGVkIiBncmFkaWVudFVuaXRzPSJ1c2VyU3BhY2VPblVzZSIgeDE9IjAlIiB5MT0iMCUiIHgyPSIwJSIgeTI9IjEwMCUiPgogICAgPHN0b3Agb2Zmc2V0PSIwJSIgc3RvcC1jb2xvcj0iI2ZmZmZmZiIgc3RvcC1vcGFjaXR5PSIxIi8+CiAgICA8c3RvcCBvZmZzZXQ9IjcwJSIgc3RvcC1jb2xvcj0iI2UwZTZlYiIgc3RvcC1vcGFjaXR5PSIxIi8+CiAgICA8c3RvcCBvZmZzZXQ9IjcwJSIgc3RvcC1jb2xvcj0iI2ZmZmZmZiIgc3RvcC1vcGFjaXR5PSIxIi8+CiAgICA8c3RvcCBvZmZzZXQ9IjEwMCUiIHN0b3AtY29sb3I9IiNmZmZmZmYiIHN0b3Atb3BhY2l0eT0iMSIvPgogIDwvbGluZWFyR3JhZGllbnQ+CiAgPHJlY3QgeD0iMCIgeT0iMCIgd2lkdGg9IjEiIGhlaWdodD0iMSIgZmlsbD0idXJsKCNncmFkLXVjZ2ctZ2VuZXJhdGVkKSIgLz4KPC9zdmc+);
            background: -moz-linear-gradient(top,  #ffffff 0%, #e0e6eb 70%, #ffffff 70%, #ffffff 100%); /* FF3.6+ */
            background: -webkit-gradient(linear, left top, left bottom, color-stop(0%,#ffffff), color-stop(70%,#e0e6eb), color-stop(70%,#ffffff), color-stop(100%,#ffffff)); /* Chrome,Safari4+ */
            background: -webkit-linear-gradient(top,  #ffffff 0%,#e0e6eb 70%,#ffffff 70%,#ffffff 100%); /* Chrome10+,Safari5.1+ */
            background: -o-linear-gradient(top,  #ffffff 0%,#e0e6eb 70%,#ffffff 70%,#ffffff 100%); /* Opera 11.10+ */
            background: -ms-linear-gradient(top,  #ffffff 0%,#e0e6eb 70%,#ffffff 70%,#ffffff 100%); /* IE10+ */
            background: linear-gradient(to bottom,  #ffffff 0%,#e0e6eb 70%,#ffffff 70%,#ffffff 100%); /* W3C */
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
<div class="banner">
<table width="100%">
<tr>
    <!-- context path -->
<td width="60%"><img src="<%= h(getViewContext().getContextPath())%>/TargetedMS/images/ChromatogramLibraryScreenshot.png"></td><td valign="top" align="center" width="40%">
<br>
<br>
<h3>Download Chromatogram Library:</h3>
<a href="" class="banner-button">Download</a>
</tr>
</table>
</div>
<table width="100%">
<tr>
<td width="80%">
    <h3>Library Statistics:</h3>
    <p class="banner">
        The library contains X proteins, Y peptides, and Z chromatograms and was last updated on XX/YY/ZZ.<br>
</td><td valign="top" width="20%" nowrap>
</td>
</tr>
</table>
