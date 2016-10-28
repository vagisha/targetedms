<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
/*
 * Copyright (c) 2012-2016 LabKey Corporation
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
<%@ page import="org.labkey.api.settings.AppProps" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.targetedms.conflict.ConflictPrecursor" %>
<%@ page import="org.labkey.targetedms.query.PrecursorManager" %>
<%@ page import="org.labkey.targetedms.view.ModifiedPeptideHtmlMaker" %>
<%@ page import="org.labkey.targetedms.view.PrecursorHtmlMaker" %>
<%@ page import="org.labkey.targetedms.TargetedMSSchema" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<TargetedMSController.PrecursorConflictBean> me = (JspView<TargetedMSController.PrecursorConflictBean>) HttpView.currentView();
    TargetedMSController.PrecursorConflictBean bean = me.getModelBean();
    String conflictTransitionsUrl = new ActionURL(TargetedMSController.PrecursorConflictTransitionsAjaxAction.class, getContainer()).getLocalURIString();

    ModifiedPeptideHtmlMaker modifiedPeptideHtmlMaker = new ModifiedPeptideHtmlMaker();

    String runPrecursorDetailsUrl = new ActionURL(TargetedMSController.PrecursorAllChromatogramsChartAction.class, getContainer()).getLocalURIString();
    String precursorConflictUiUrl = new ActionURL(TargetedMSController.ShowPrecursorConflictUiAction.class, getContainer()).getLocalURIString();
%>

<style type="text/css">
    td.representative {background-color:#8FBC8F;}
    span.label {text-decoration: underline; cursor: pointer;}
</style>

<script type="text/javascript" src="<%=getContextPath()%>/TargetedMS/jquery/jquery-1.8.3.min.js"></script>

<script type="text/javascript">

$(document).ready(function () {
    $('input[name="selectedInputValues"]').click(function() {
         toggleCheckboxSelection($(this));
    });

    $("span.label").click(function() {
        var id = $(this).attr('id');
        //alert("You clicked "+id);
        var tokens = id.split('_');
        togglePrecursorDetails(this, tokens[0], tokens[1]);
    });

    $("#selectAllNew").click(function(){
        $('input.newPrecursor').attr('checked', true);
        $('input.oldPrecursor').attr('checked', false);
        $('td.newPrecursor').removeClass('representative').addClass('representative');
        $('td.oldPrecursor').removeClass('representative');
    });
    $("#selectAllOld").click(function(){
        $('input.oldPrecursor').attr('checked', true);
        $('input.newPrecursor').attr('checked', false);
        $('td.oldPrecursor').removeClass('representative').addClass('representative');
        $('td.newPrecursor').removeClass('representative');
    });
});

function loadPrecursorDetails(element, newPrecursorId, oldPrecursorId, response, request) {

    if(response.status == 200)
    {
        // alert(response.responseText);
        var jsonResponse = Ext.util.JSON.decode(response.responseText);
        var result = jsonResponse.conflictTransitions;
        var newTransitionsTable = "<table width='100%'><thead><tr><td>Transition</td><td>Rank</td></tr></thead><tbody>";
        var oldTransitionsTable = "<table width='100%'><thead><tr><td>Transition</td><td>Rank</td></tr></thead><tbody>";
        for(var i = 0; i < result.length; i++)
        {
            var conflictTransition = result[i];
            newTransitionsTable += "<tr>";
            newTransitionsTable += "<td>"+conflictTransition.newTransition+"</td><td>"+conflictTransition.newTransitionRank+"</td>";
            newTransitionsTable += "</tr>";
            oldTransitionsTable += "<tr>";
            oldTransitionsTable += "<td>"+conflictTransition.oldTransition+"</td><td>"+conflictTransition.oldTransitionRank+"</td>";
            oldTransitionsTable += "</tr>";
        }
        newTransitionsTable += "</tbody></table>";
        oldTransitionsTable += "</tbody></table>";
        $("#"+newPrecursorId+"_details").text(""); // Remove "loading..."
        $("#"+oldPrecursorId+"_details").text(""); // Remove "loading..."
        $("#"+newPrecursorId+"_details").append(newTransitionsTable);
        $("#"+oldPrecursorId+"_details").append(oldTransitionsTable);

        $(element).addClass('content_loaded');
    }
}

function togglePrecursorDetails(element, newPrecursorId, oldPrecursorId)
{
    if(!$(element).hasClass('content_loaded'))
    {
        var url = <%=q(conflictTransitionsUrl)%>

            Ext.Ajax.request({
                url: url,
                params: {newPrecursorId: newPrecursorId, oldPrecursorId: oldPrecursorId},
                method: 'GET',
                success: function(response, request){
                    loadPrecursorDetails(element, newPrecursorId, oldPrecursorId, response, request);
                },
                failure: function(response, request) {
                    $("#"+newPrecursorId+"_details").text("ERROR: "+response.responseText);
                    $("#"+oldPrecursorId+"_details").text("ERROR");
                }
            });
    }

    if($(element).hasClass('open'))
    {
        $(element).removeClass('open').addClass('closed');
        $(element).children('img').attr('src', "<%=getWebappURL("_images/plus.gif")%>");
        $("#"+newPrecursorId+"_details").hide();
        $("#"+oldPrecursorId+"_details").hide();
    }
    else
    {
        $(element).removeClass('closed').addClass('open');
        $(element).children('img').attr('src', "<%=getWebappURL("_images/minus.gif")%>");
        $("#"+newPrecursorId+"_details").show();
        $("#"+oldPrecursorId+"_details").show();
    }
}

function toggleCheckboxSelection(element)
{
    var cls = element.attr('class').split(' ')[0]; // get the first class name
    // alert(cls);
    $("td."+cls).toggleClass("representative");
    if(element.is(":checked"))
    {
        $("."+cls).attr('checked', false); // Both old and new precursor checkboxes have the same class. First deselect all.
        element.attr('checked', 'checked'); // Select the one that triggered this function call.
    }
    else
    {
        $("."+cls).attr('checked', 'checked'); // First select all.
        element.attr('checked', false);        // Deselect the one that triggered the function call.
    }
}

</script>

<%if(bean.getAllConflictRunFiles() != null && bean.getAllConflictRunFiles().size() > 1) {%>
    <div style="margin-bottom:10px;">
        The following runs have conflicting peptides:
        <ul>
            <%for(String conflictRun: bean.getAllConflictRunFiles().keySet()) {%>
                 <li>
                     <a href="<%=precursorConflictUiUrl%>conflictedRunId=<%=bean.getAllConflictRunFiles().get(conflictRun)%>"><%=h(conflictRun)%></a>
                 </li>
            <%}%>
        </ul>
        Conflicts can be resolved for one run at a time.
    </div>
<%}%>

<div style="margin-bottom:10px;">Check the peptides that you would like to include in the library.</div>

<%if(bean.getConflictRunFileName() != null) {%>
    <div style="color:red; margin-bottom:10px;">
        Resolve conflicts for <%=h(bean.getConflictRunFileName())%>.
    </div>
<%}%>

<%int colspan=3;%>
<form <%=formAction(TargetedMSController.ResolveConflictAction.class, Method.Post)%>><labkey:csrf/>
<input type="hidden" name="conflictLevel" value="peptide"/>
<table class="labkey-data-region labkey-show-borders">
    <thead>
       <tr>
            <th colspan="<%=colspan%>"><div class="labkey-button-bar" style="width:98%">Conflicting Peptides in Document</div></th>
            <th colspan="<%=colspan%>"><div class="labkey-button-bar" style="width:98%">Current Library Peptides</div></th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td class="labkey-column-header"></td>
            <td class="labkey-column-header">Precursor</td>
            <td class="labkey-column-header">Document</td>
            <!--<td class="labkey-column-header">PrecursorId</td>-->

            <td class="labkey-column-header"></td>
            <td class="labkey-column-header">Precursor</td>
            <td class="labkey-column-header">Document</td>
            <!--<td class="labkey-column-header">PrecursorId</td>-->
        </tr>
    <% int index = 0; %>
    <%for (ConflictPrecursor precursor: bean.getConflictPrecursorList()) {%>
         <tr class="labkey-alternate-row">

             <!-- New representative precursor -->
             <td class="representative newPrecursor <%=precursor.getNewPrecursorId()%>">
                 <input type="checkbox" class="<%=precursor.getNewPrecursorId()%> newPrecursor"
                                        name="selectedInputValues"
                                        value="<%=precursor.getNewPrecursorId()%>_<%=precursor.getOldPrecursorId()%>"
                                        checked/></td>
             <!--<td class="representative newPrecursor <%=precursor.getNewPrecursorId()%>"><%=precursor.getNewPrecursorId()%></td>-->
             <td class="representative newPrecursor label <%=precursor.getNewPrecursorId()%>">
                 <span class="label" id="<%=precursor.getNewPrecursorId()%>_<%=precursor.getOldPrecursorId()%>">
                     <img src="<%=getWebappURL("_images/plus.gif")%>"/>
                     <%=PrecursorHtmlMaker.getModSeqChargeHtml(modifiedPeptideHtmlMaker, PrecursorManager.getPrecursor(getContainer(), precursor.getNewPrecursorId(),
                             getUser()), precursor.getNewPrecursorRunId(), new TargetedMSSchema(getUser(), getContainer()))%>
                 </span>
             </td>
             <td class="representative newPrecursor <%=precursor.getNewPrecursorId()%>">
                 <a href="<%=runPrecursorDetailsUrl%>id=<%=precursor.getNewPrecursorId()%>"><%=precursor.getNewRunFile()%></a>
             </td>

             <!-- Old representative precursor -->
             <td class="oldPrecursor <%=precursor.getNewPrecursorId()%>">
                 <input type="checkbox" class="<%=precursor.getNewPrecursorId()%> oldPrecursor"
                                        name="selectedInputValues"
                                        value="<%=precursor.getOldPrecursorId()%>_<%=precursor.getNewPrecursorId()%>" /></td>
             <!--<td class="oldPrecursor <%=precursor.getNewPrecursorId()%>"><%=precursor.getOldPrecursorId()%></td>-->
             <td class="oldPrecursor label <%=precursor.getNewPrecursorId()%>">
                 <span class="label" id="<%=precursor.getNewPrecursorId()%>_<%=precursor.getOldPrecursorId()%>">
                     <img src="<%=getWebappURL("_images/plus.gif")%>"/>
                     <%=PrecursorHtmlMaker.getModSeqChargeHtml(modifiedPeptideHtmlMaker, PrecursorManager.getPrecursor(getContainer(), precursor.getOldPrecursorId(),
                             getUser()), precursor.getOldPrecursorRunId(), new TargetedMSSchema(getUser(), getContainer()))%>
                 </span>
             </td>
             <td class="oldPrecursor <%=precursor.getNewPrecursorId()%>">
                <a href="<%=runPrecursorDetailsUrl%>id=<%=precursor.getOldPrecursorId()%>"><%=precursor.getOldRunFile()%></a>
             </td>
         </tr>
        <!-- This is a hidden table row where transition details will be displayed -->
        <tr>
            <td colspan="<%=colspan%>"><div id="<%=precursor.getNewPrecursorId()%>_details" style="display:none;">Loading...</div></td>
            <td colspan="<%=colspan%>"><div id="<%=precursor.getOldPrecursorId()%>_details" style="display:none;">Loading...</div></td>
        </tr>
    <%}%>
        <tr>
            <td colspan="<%=colspan%>"><span style="text-decoration:underline;cursor:pointer;" id="selectAllNew">Select All</span></td>
            <td colspan="<%=colspan%>"><span style="text-decoration:underline;cursor:pointer;" id="selectAllOld">Select All</span></td>
        </tr>
        <tr>
            <td colspan="8" style="padding:10px;" align="center">
                <%= button("Apply Changes").submit(true) %>
                &nbsp;
                <%= button("Cancel").href(getContainer().getStartURL(getUser())) %>
            </td>
        </tr>
    </tbody>
</table>