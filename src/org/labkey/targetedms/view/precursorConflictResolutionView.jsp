<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
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
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.targetedms.conflict.ConflictPrecursor" %>
<%@ page import="org.labkey.targetedms.query.PrecursorManager" %>
<%@ page import="org.labkey.targetedms.view.ModifiedPeptideHtmlMaker" %>
<%@ page import="org.labkey.targetedms.view.PrecursorHtmlMaker" %>
<%@ page import="org.labkey.targetedms.TargetedMSSchema" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("Ext4");
        dependencies.add("TargetedMS/jquery/jquery-1.8.3.min.js");
        dependencies.add("TargetedMS/DataTables/jquery.dataTables.min.js");
        dependencies.add("TargetedMS/DataTables/jquery.dataTables.min.css");
    }
%>
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
    table.dataTable.myTable thead th,
    table.dataTable.myTable thead td,
    table.dataTable.myTable tbody td,
    table.dataTable.myTable tfoot th
    {padding: 2px 0 2px 0}
    table.precursor_details td {background:lightgoldenrodyellow;padding:0 0 0 0;}
    div.protein_details {padding:3px;}
</style>

<script type="text/javascript">

    var table;

$(document).ready(function () {
    $('input[name="selectedVals"]').click(function() {
         toggleCheckboxSelection($(this));
    });

    $("span.label").click(function() {
        var id = $(this).attr('id');
        //alert("You clicked "+id);
        var tokens = id.split('_');
        togglePrecursorDetails(this, tokens[0], tokens[1]);
    });

    $("#selectAllNew").click(function(){

        var oldPrecursorCells = table.cells(".oldPrecursor").nodes();
        var newPrecursorCells = table.cells(".newPrecursor").nodes();
        $(newPrecursorCells).removeClass('representative').addClass('representative');
        $(newPrecursorCells).find(':checkbox').attr('checked', 'checked');
        $(oldPrecursorCells).removeClass('representative');
        $(oldPrecursorCells).find(':checkbox').removeAttr('checked');
    });
    $("#selectAllOld").click(function(){

        var oldPrecursorCells = table.cells(".oldPrecursor").nodes();
        var newPrecursorCells = table.cells(".newPrecursor").nodes();
        $(oldPrecursorCells).removeClass('representative').addClass('representative');
        $(oldPrecursorCells).find(':checkbox').attr('checked', 'checked');
        $(newPrecursorCells).removeClass('representative');
        $(newPrecursorCells).find(':checkbox').removeAttr('checked');
    });

    table = $("#dataTable").DataTable(
            {
                "bSort":false,
                "searching":false,
                "autoWidth": false,
                "lengthMenu": [[10, 20, 50, -1], [10, 20, 50, "All"]],
                "pageLength": 20
            }
    );

    table.rows().every(function()
    {
        this.child($('<tr>'+
                '<td colspan="4"><div class="newPrecursor precursor_details">Loading...</div></td>'+
                '<td colspan="4"><div class="oldPrecursor precursor_details">Loading...</div></td>'+
                '</tr>'));
    });

    table.on('click', 'td.details-control', function() {
        var srcTd = $(this);
        var span = $(this).children("span");
        var cls = span.attr('class');
        //console.log("You clicked "+cls);
        var tokens = cls.split('_');
        var newPrecursorId = tokens[0];
        var oldPrecursorId = tokens[1];

        var tr = $(this).closest('tr');
        var row = table.row(tr);
        if(row.child.isShown())
        {
            row.child.hide();
            tr.removeClass('shown');
            $("." + cls).children('img').attr('src', "<%=getWebappURL("_images/plus.gif")%>");
        }
        else {
            row.child.show();
            tr.addClass('shown');
            $("." + cls).children('img').attr('src', "<%=getWebappURL("_images/minus.gif")%>");
        }

        if(!srcTd.hasClass('content_loaded'))
        {
            var url = <%=q(conflictTransitionsUrl)%>

            Ext4.Ajax.request({
                url: url,
                params: {newPrecursorId: newPrecursorId, oldPrecursorId: oldPrecursorId},
                method: 'GET',
                success: function(response, request){
                    loadPrecursorDetails(row.child(), newPrecursorId, oldPrecursorId, response, request);
                    srcTd.addClass('content_loaded');
                },
                failure: function(response, request) {
                    console.log("ERROR: " + response.responseText);
                    row.child().find("div.newPrecursor").text("ERROR: "+response.responseText);
                    row.child().find("div.oldPrecursor").text("ERROR");
                }
            });
        }
    });
});

function loadPrecursorDetails(tr, newPrecursorId, oldPrecursorId, response, request) {

    if(response.status == 200)
    {
        // alert(response.responseText);
        var jsonResponse = Ext4.JSON.decode(response.responseText);
        var result = jsonResponse.conflictTransitions;
        var newTransitionsTable = "<table width='100%' class='precursor_details'><thead><tr><td>Transition</td><td>Rank</td></tr></thead><tbody>";
        var oldTransitionsTable = "<table width='100%' class='precursor_details'><thead><tr><td>Transition</td><td>Rank</td></tr></thead><tbody>";
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
        tr.find("div.newPrecursor").text(""); // Remove "loading..."
        tr.find("div.oldPrecursor").text(""); // Remove "loading..."
        tr.find("div.newPrecursor").append(newTransitionsTable);
        tr.find("div.oldPrecursor").append(oldTransitionsTable);
    }
}

function toggleCheckboxSelection(element)
{
    var cls = element.attr('class').split(' ')[0]; // get the first class name
    //console.log(cls);

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

    function submitMyForm()
    {
        var selectedIds = [];
        table.rows().every(function(){
            var selected = $(this.nodes()).find("input:checked").val();
            // console.log(selected);
            selectedIds.push(selected);
        });
        $("#conflictTableForm #selectedInputValues").val(selectedIds);
        $("#conflictTableForm").submit();
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

<%int colspan=4;%>
<form <%=formAction(TargetedMSController.ResolveConflictAction.class, Method.Post)%> id="conflictTableForm"><labkey:csrf/>
<input type="hidden" name="conflictLevel" value="peptide"/>
<input type="hidden" name="selectedInputValues" id="selectedInputValues"/>
<table class="labkey-data-region-legacy labkey-show-borders myTable" id="dataTable">
    <thead>
       <tr>
            <th colspan="<%=colspan%>"><div class="labkey-button-bar" style="width:98%">Conflicting Peptides in Document</div></th>
            <th colspan="<%=colspan%>"><div class="labkey-button-bar" style="width:98%">Current Library Peptides</div></th>
        </tr>
       <tr>
           <th class="labkey-column-header"></th>
           <th class="labkey-column-header"></th>
           <th class="labkey-column-header">Precursor</th>
           <th class="labkey-column-header">Document</th>
           <!--<td class="labkey-column-header">ProteinId</td>-->

           <th class="labkey-column-header"></th>
           <th class="labkey-column-header"></th>
           <th class="labkey-column-header">Precursor</th>
           <th class="labkey-column-header">Document</th>
           <!--<td class="labkey-column-header">ProteinId</td>-->
       </tr>
    </thead>
    <tbody>

    <%for (ConflictPrecursor precursor: bean.getConflictPrecursorList()) {%>
         <tr class="labkey-alternate-row">

             <!-- New representative precursor -->
             <td class="representative newPrecursor <%=precursor.getNewPrecursorId()%>">
                 <input type="checkbox" class="<%=precursor.getNewPrecursorId()%> newPrecursor"
                                        name="selectedVals"
                                        value="<%=precursor.getNewPrecursorId()%>_<%=precursor.getOldPrecursorId()%>"
                                        checked/></td>
             <!--<td class="representative newPrecursor <%=precursor.getNewPrecursorId()%>"><%=precursor.getNewPrecursorId()%></td>-->
             <td class="representative details-control newPrecursor <%=precursor.getNewPrecursorId()%>">
                <span class="<%=precursor.getNewPrecursorId()%>_<%=precursor.getOldPrecursorId()%>">
                     <img src="<%=getWebappURL("_images/plus.gif")%>"/>
                 </span>
             </td>
             <td class="representative newPrecursor <%=precursor.getNewPrecursorId()%>">
                 <span class="label">
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
                                        name="selectedVals"
                                        value="<%=precursor.getOldPrecursorId()%>_<%=precursor.getNewPrecursorId()%>" /></td>
             <!--<td class="oldPrecursor <%=precursor.getNewPrecursorId()%>"><%=precursor.getOldPrecursorId()%></td>-->
             <td class="details-control oldPrecursor <%=precursor.getNewPrecursorId()%>">
                <span class="<%=precursor.getNewPrecursorId()%>_<%=precursor.getOldPrecursorId()%>">
                     <img src="<%=getWebappURL("_images/plus.gif")%>"/>
                 </span>
             </td>
             <td class="oldPrecursor <%=precursor.getNewPrecursorId()%>">
                 <span class="label">
                     <%=PrecursorHtmlMaker.getModSeqChargeHtml(modifiedPeptideHtmlMaker, PrecursorManager.getPrecursor(getContainer(), precursor.getOldPrecursorId(),
                             getUser()), precursor.getOldPrecursorRunId(), new TargetedMSSchema(getUser(), getContainer()))%>
                 </span>
             </td>
             <td class="oldPrecursor <%=precursor.getNewPrecursorId()%>">
                <a href="<%=runPrecursorDetailsUrl%>id=<%=precursor.getOldPrecursorId()%>"><%=precursor.getOldRunFile()%></a>
             </td>
         </tr>
    <%}%>
    </tbody>
    <thead>
        <tr>
            <th colspan="<%=colspan%>"><span style="text-decoration:underline;cursor:pointer;" id="selectAllNew">Select All</span></th>
            <th colspan="<%=colspan%>"><span style="text-decoration:underline;cursor:pointer;" id="selectAllOld">Select All</span></th>
        </tr>
        <tr>
            <th colspan="8" style="padding:10px;" align="center">
                <div class="labkey-button-bar">
                    <%= button("Apply Changes").onClick("submitMyForm(); return false;") %>
                    &nbsp;
                    <%= button("Cancel").href(getContainer().getStartURL(getUser())) %>
                </div>
            </th>
        </tr>
    </thead>
</table>