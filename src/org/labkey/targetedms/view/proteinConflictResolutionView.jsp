<%
/*
 * Copyright (c) 2012-2017 LabKey Corporation
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
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.targetedms.conflict.ConflictProtein" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
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
    Container c = getContainer();
    JspView<TargetedMSController.ProteinConflictBean> me = (JspView<TargetedMSController.ProteinConflictBean>) HttpView.currentView();
    TargetedMSController.ProteinConflictBean bean = me.getModelBean();
    String conflictPeptidesUrl = new ActionURL(TargetedMSController.ProteinConflictPeptidesAjaxAction.class, c).getLocalURIString();

    ActionURL runProteinDetailsUrl = new ActionURL(TargetedMSController.ShowProteinAction.class, c);
    ActionURL proteinConflictUiUrl = new ActionURL(TargetedMSController.ShowProteinConflictUiAction.class, c);
%>

<style type="text/css">
    td.representative {background-color:#8FBC8F;}
    span.label {text-decoration: underline; cursor: pointer;}
    table.dataTable.myTable thead th,
    table.dataTable.myTable thead td,
    table.dataTable.myTable tbody td,
    table.dataTable.myTable tfoot th
    {padding: 2px 0 2px 0}
    table.protein_details td {background:lightgoldenrodyellow;padding:0 0 0 0;}
    div.protein_details {padding:3px;}
</style>

<script type="text/javascript">

    var table;

$(document).ready(function () {
    $('input[name="selectedVals"]').click(function() {
         toggleCheckboxSelection($(this));
    });

    $("#selectAllNew").click(function(){

        var oldProteinCells = table.cells(".oldProtein").nodes();
        var newProteinCells = table.cells(".newProtein").nodes();
        $(newProteinCells).removeClass('representative').addClass('representative');
        $(newProteinCells).find(':checkbox').attr('checked', 'checked');
        $(oldProteinCells).removeClass('representative');
        $(oldProteinCells).find(':checkbox').removeAttr('checked');
    });
    $("#selectAllOld").click(function(){

        var oldProteinCells = table.cells(".oldProtein").nodes();
        var newProteinCells = table.cells(".newProtein").nodes();
        $(oldProteinCells).removeClass('representative').addClass('representative');
        $(oldProteinCells).find(':checkbox').attr('checked', 'checked');
        $(newProteinCells).removeClass('representative');
        $(newProteinCells).find(':checkbox').removeAttr('checked');
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
                '<td colspan="4"><div class="newProtein protein_details">Loading...</div></td>'+
                '<td colspan="4"><div class="oldProtein protein_details">Loading...</div></td>'+
                '</tr>'));
    });

    table.on('click', 'td.details-control', function() {
        var srcTd = $(this);
        var span = $(this).children("span");
        var cls = span.attr('class');
        //console.log("You clicked "+cls);
        var tokens = cls.split('_');
        var newProteinId = tokens[0];
        var oldProteinId = tokens[1];
        // toggleProteinDetails(this, tokens[0], tokens[1], table);

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
            var url = <%=q(conflictPeptidesUrl.toString())%> // +'newProteinId='+newProteinId+"&oldProteinId="+oldProteinId;
            var url;
            // alert(url);

            Ext4.Ajax.request({
                url: url,
                params: {newProteinId: newProteinId, oldProteinId: oldProteinId},
                method: 'GET',
                success: function(response, request){
                    loadProteinDetails(row.child(), newProteinId, oldProteinId, response, request);
                    srcTd.addClass('content_loaded');
                },
                failure: function(response, request) {
                    console.log("ERROR: " + response.responseText);
                    row.child().find("div.newProtein").text("ERROR: "+response.responseText);
                    row.child().find("div.oldProtein").text("ERROR");
                }
            });
        }
    });
});

function loadProteinDetails(tr, newProteinId, oldProteinId, response, request) {

    if(response.status == 200)
    {
        //console.log(response.responseText);
        var jsonResponse = Ext4.JSON.decode(response.responseText);
        var result = jsonResponse.conflictPeptides;
        var newPeptidesTable = "<table width='100%' class='protein_details'><thead><tr><td>Peptide</td><td>Rank</td></tr></thead><tbody>";
        var oldPeptidesTable = "<table width='100%' class='protein_details'><thead><tr><td>Peptide</td><td>Rank</td></tr></thead><tbody>";
        for(var i = 0; i < result.length; i++)
        {
            var conflictPeptide = result[i];
            newPeptidesTable += "<tr>";
            newPeptidesTable += "<td>"+conflictPeptide.newPeptide+"</td><td>"+conflictPeptide.newPeptideRank+"</td>";
            newPeptidesTable += "</tr>";
            oldPeptidesTable += "<tr>";
            oldPeptidesTable += "<td>"+conflictPeptide.oldPeptide+"</td><td>"+conflictPeptide.oldPeptideRank+"</td>";
            oldPeptidesTable += "</tr>";
        }
        newPeptidesTable += "</tbody></table>";
        oldPeptidesTable += "</tbody></table>";
        tr.find("div.newProtein").text(""); // Remove "loading..."
        tr.find("div.oldProtein").text(""); // Remove "loading..."
        tr.find("div.newProtein").append(newPeptidesTable);
        tr.find("div.oldProtein").append(oldPeptidesTable);
    }
}

function toggleCheckboxSelection(element)
{
    var cls = element.attr('class').split(' ')[0]; // get the first class name
    //console.log(cls);

    $("td."+cls).toggleClass("representative");

    if(element.is(":checked"))
    {
        $("."+cls).attr('checked', false); // Both old and new protein checkboxes have the same class. First deselect all.
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
            //console.log(selected);
            selectedIds.push(selected);
        });
        $("#conflictTableForm #selectedInputValues").val(selectedIds);
        $("#conflictTableForm").submit();
    }

</script>

<%if(bean.getAllConflictRunFiles() != null && bean.getAllConflictRunFiles().size() > 1) {%>
    <div style="margin-bottom:10px;">
        The following runs have conflicting proteins:
        <ul>
            <%for(String conflictRun: bean.getAllConflictRunFiles().keySet()) {%>
                 <li>
                     <a href="<%=h(proteinConflictUiUrl)%>conflictedRunId=<%=bean.getAllConflictRunFiles().get(conflictRun)%>"><%=conflictRun%></a>
                 </li>
            <%}%>
        </ul>
        Conflicts can be resolved for one run at a time.
    </div>
<%}%>

<div style="margin-bottom:10px;">Check the proteins that you would like to include in the library.</div>

<%if(bean.getConflictRunFileName() != null) {%>
    <div style="color:red; margin-bottom:10px;">
        Resolve conflicts for <%=bean.getConflictRunFileName()%>.
    </div>
<%}%>


<%int colspan=4;%>
<form <%=formAction(TargetedMSController.ResolveConflictAction.class, Method.Post)%> id="conflictTableForm"><labkey:csrf/>
<input type="hidden" name="conflictLevel" value="protein"/>
<input type="hidden" name="selectedInputValues" id="selectedInputValues"/>
<table class="labkey-data-region-legacy labkey-show-borders myTable" id="dataTable">
    <thead>
       <tr>
            <th colspan="<%=colspan%>"><div class="labkey-button-bar" style="width:98%">Conflicting Proteins in Document</div></th>
            <th colspan="<%=colspan%>"><div class="labkey-button-bar" style="width:98%">Current Library Proteins</div></th>
        </tr>
       <tr>
           <th class="labkey-column-header"></th>
           <th class="labkey-column-header"></th>
           <th class="labkey-column-header">Protein</th>
           <th class="labkey-column-header">Document</th>
           <!--<td class="labkey-column-header">ProteinId</td>-->

           <th class="labkey-column-header"></th>
           <th class="labkey-column-header"></th>
           <th class="labkey-column-header">Protein</th>
           <th class="labkey-column-header">Document</th>
           <!--<td class="labkey-column-header">ProteinId</td>-->
       </tr>
    </thead>
    <tbody>

    <%for (ConflictProtein protein: bean.getConflictProteinList()) {%>
         <tr class="labkey-alternate-row">

             <!-- New representative protein -->
             <td class="representative newProtein <%=protein.getNewProteinId()%>">
                 <input type="checkbox" class="<%=protein.getNewProteinId()%> newProtein"
                                        name="selectedVals"
                                        value="<%=protein.getNewProteinId()%>_<%=protein.getOldProteinId()%>"
                                        checked/></td>
             <!--<td class="representative newProtein <%=protein.getNewProteinId()%>"><%=protein.getNewProteinId()%></td>-->
             <td class="representative details-control newProtein <%=protein.getNewProteinId()%>">
                <span class="<%=protein.getNewProteinId()%>_<%=protein.getOldProteinId()%>">
                     <img src="<%=getWebappURL("_images/plus.gif")%>"/>
                 </span>
             </td>
             <td class="representative newProtein <%=protein.getNewProteinId()%>">
                 <span class="label">
                     <%=protein.getNewProteinLabel()%>
                 </span>
             </td>
             <td class="representative newProtein <%=protein.getNewProteinId()%>">
                 <a href="<%=h(runProteinDetailsUrl)%>id=<%=protein.getNewProteinId()%>"><%=protein.getNewRunFile()%></a>
             </td>

             <!-- Old representative protein -->
             <td class="oldProtein <%=protein.getNewProteinId()%>">
                 <input type="checkbox" class="<%=protein.getNewProteinId()%> oldProtein"
                                        name="selectedVals"
                                        value="<%=protein.getOldProteinId()%>_<%=protein.getNewProteinId()%>" /></td>
             <!--<td class="oldProtein <%=protein.getNewProteinId()%>"><%=protein.getOldProteinId()%></td>-->
             <td class="details-control oldProtein <%=protein.getNewProteinId()%>">
                <span class="<%=protein.getNewProteinId()%>_<%=protein.getOldProteinId()%>">
                     <img src="<%=getWebappURL("_images/plus.gif")%>"/>
                 </span>
             </td>
             <td class="oldProtein label <%=protein.getNewProteinId()%>">
                 <span class="label">
                     <%=protein.getOldProteinLabel()%>
                 </span>
             </td>
             <td class="oldProtein <%=protein.getNewProteinId()%>">
                 <a href="<%=h(runProteinDetailsUrl)%>id=<%=protein.getOldProteinId()%>"><%=protein.getOldRunFile()%></a>
             </td>
         </tr>
    <%}%>
    </tbody>
    <tfoot>
        <tr>
            <th colspan="<%=colspan%>"><span style="text-decoration:underline;cursor:pointer;" id="selectAllNew">Select All</span></th>
            <th colspan="<%=colspan%>"><span style="text-decoration:underline;cursor:pointer;" id="selectAllOld">Select All</span></th>
        </tr>
        <tr>
            <th colspan="8" align="center">
                <div class="labkey-button-bar">
                    <%= button("Apply Changes").onClick("submitMyForm(); return false;") %>
                    &nbsp;
                    <%= button("Cancel").href(getContainer().getStartURL(getUser())) %>
                </div>
            </th>
        </tr>
    </tfoot>
</table>
</form>


