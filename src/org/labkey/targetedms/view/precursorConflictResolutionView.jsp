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
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.api.settings.AppProps" %>
<%@ page import="org.labkey.targetedms.conflict.ConflictPrecursor" %>
<%@ page import="org.labkey.targetedms.view.ModifiedPeptideHtmlMaker" %>
<%@ page import="org.labkey.targetedms.query.PrecursorManager" %>
<%@ page import="org.labkey.targetedms.view.PrecursorHtmlMaker" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    ViewContext context = HttpView.currentContext();
    JspView<TargetedMSController.PrecursorConflictBean> me = (JspView<TargetedMSController.PrecursorConflictBean>) HttpView.currentView();
    TargetedMSController.PrecursorConflictBean bean = me.getModelBean();
    String conflictTransitionsUrl = new ActionURL(TargetedMSController.PrecursorConflictTransitionsAjaxAction.class, context.getContainer()).getLocalURIString();

    String plusImgUrl = request.getContextPath()+"/_images/plus.gif";
    String minusImgUrl = request.getContextPath()+"/_images/minus.gif";

    ModifiedPeptideHtmlMaker modifiedPeptideHtmlMaker = new ModifiedPeptideHtmlMaker();
%>

<style type="text/css">
    td.representative {background-color:#8FBC8F;}
    span.label {text-decoration: underline; cursor: pointer;}
</style>

<script type="text/javascript" src="<%= AppProps.getInstance().getContextPath()%>/TargetedMS/jquery/jquery-1.8.3.min.js"></script>

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
            var url;
            // alert(url);

            Ext.Ajax.request({
                url: url,
                params: {newPrecursorId: newPrecursorId, oldPrecursorId: oldPrecursorId},
                method: 'GET',
                success: function(response, request){
                    loadPrecursorDetails(element, newPrecursorId, oldPrecursorId, response, request);
                },
                failure: null
            });
    }

    if($(element).hasClass('open'))
    {
        $(element).removeClass('open').addClass('closed');
        $(element).children('img').attr('src', "<%=plusImgUrl%>");
        $("#"+newPrecursorId+"_details").hide();
        $("#"+oldPrecursorId+"_details").hide();
    }
    else
    {
        $(element).removeClass('closed').addClass('open');
        $(element).children('img').attr('src', "<%=minusImgUrl%>");
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

<%int colspan=3;%>
<form <%=formAction(TargetedMSController.ResolveConflictAction.class, Method.Post)%>>
<input type="hidden" name="conflictLevel" value="peptide"/>
<table class="labkey-data-region labkey-show-borders">
    <thead>
       <tr>
            <th colspan="<%=colspan%>"><div class="labkey-button-bar" style="width:98%">Conflicting Representative Results</div></th>
            <th colspan="<%=colspan%>"><div class="labkey-button-bar" style="width:98%">Current Representative Results</div></th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td class="labkey-column-header"></td>
            <td class="labkey-column-header">Precursor</td>
            <td class="labkey-column-header">Run</td>
            <!--<td class="labkey-column-header">PrecursorId</td>-->

            <td class="labkey-column-header"></td>
            <td class="labkey-column-header">Precursor</td>
            <td class="labkey-column-header">Run</td>
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
                                        checked="checked"/></td>
             <!--<td class="representative newPrecursor <%=precursor.getNewPrecursorId()%>"><%=precursor.getNewPrecursorId()%></td>-->
             <td class="representative newPrecursor label <%=precursor.getNewPrecursorId()%>">
                 <span class="label" id="<%=precursor.getNewPrecursorId()%>_<%=precursor.getOldPrecursorId()%>">
                     <img src="<%=plusImgUrl%>"/>
                     <%=PrecursorHtmlMaker.getModSeqChargeHtml(modifiedPeptideHtmlMaker, PrecursorManager.get(precursor.getNewPrecursorId()))%>
                 </span>
             </td>
             <td class="representative newPrecursor <%=precursor.getNewPrecursorId()%>"><%=precursor.getNewRunFile()%></td>

             <!-- Old representative precursor -->
             <td class="oldPrecursor <%=precursor.getNewPrecursorId()%>">
                 <input type="checkbox" class="<%=precursor.getNewPrecursorId()%> oldPrecursor"
                                        name="selectedInputValues"
                                        value="<%=precursor.getOldPrecursorId()%>_<%=precursor.getNewPrecursorId()%>" /></td>
             <!--<td class="oldPrecursor <%=precursor.getNewPrecursorId()%>"><%=precursor.getOldPrecursorId()%></td>-->
             <td class="oldPrecursor label <%=precursor.getNewPrecursorId()%>">
                 <span class="label" id="<%=precursor.getNewPrecursorId()%>_<%=precursor.getOldPrecursorId()%>">
                     <img src="<%=plusImgUrl%>"/>
                     <%=PrecursorHtmlMaker.getModSeqChargeHtml(modifiedPeptideHtmlMaker, PrecursorManager.get(precursor.getOldPrecursorId()))%>
                 </span>
             </td>
             <td class="oldPrecursor <%=precursor.getNewPrecursorId()%>"><%=precursor.getOldRunFile()%></td>
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
                <%=generateSubmitButton("Apply Changes")%>
                &nbsp;
                <%=generateButton("Cancel", TargetedMSController.ShowListAction.class)%>
            </td>
        </tr>
    </tbody>
</table>