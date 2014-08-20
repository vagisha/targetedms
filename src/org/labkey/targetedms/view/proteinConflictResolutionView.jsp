<%
/*
 * Copyright (c) 2012-2014 LabKey Corporation
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
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
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
        toggleProteinDetails(this, tokens[0], tokens[1]);
    });

    $("#selectAllNew").click(function(){
        $('input.newProtein').attr('checked', true);
        $('input.oldProtein').attr('checked', false);
        $('td.newProtein').removeClass('representative').addClass('representative');
        $('td.oldProtein').removeClass('representative');
    });
    $("#selectAllOld").click(function(){
        $('input.oldProtein').attr('checked', true);
        $('input.newProtein').attr('checked', false);
        $('td.oldProtein').removeClass('representative').addClass('representative');
        $('td.newProtein').removeClass('representative');
    });
});

function loadProteinDetails(element, newProteinId, oldProteinId, response, request) {

    if(response.status == 200)
    {
        //alert(response.responseText);
        var jsonResponse = Ext.util.JSON.decode(response.responseText);
        var result = jsonResponse.conflictPeptides;
        var newPeptidesTable = "<table width='100%'><thead><tr><td>Peptide</td><td>Rank</td></tr></thead><tbody>";
        var oldPeptidesTable = "<table width='100%'><thead><tr><td>Peptide</td><td>Rank</td></tr></thead><tbody>";
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
        $("#"+newProteinId+"_details").text(""); // Remove "loading..."
        $("#"+oldProteinId+"_details").text(""); // Remove "loading..."
        $("#"+newProteinId+"_details").append(newPeptidesTable);
        $("#"+oldProteinId+"_details").append(oldPeptidesTable);

        $(element).addClass('content_loaded');
    }
}

function toggleProteinDetails(element, newProteinId, oldProteinId)
{
    if(!$(element).hasClass('content_loaded'))
    {
        var url = <%=q(conflictPeptidesUrl.toString())%> // +'newProteinId='+newProteinId+"&oldProteinId="+oldProteinId;
            var url;
            // alert(url);

            Ext.Ajax.request({
                url: url,
                params: {newProteinId: newProteinId, oldProteinId: oldProteinId},
                method: 'GET',
                success: function(response, request){
                    loadProteinDetails(element, newProteinId, oldProteinId, response, request);
                },
                failure: function(response, request) {
                    $("#"+newProteinId+"_details").text("ERROR: "+response.responseText);
                    $("#"+oldProteinId+"_details").text("ERROR");
                }
            });
    }

    if($(element).hasClass('open'))
    {
        $(element).removeClass('open').addClass('closed');
        $(element).children('img').attr('src', "<%=getWebappURL("_images/plus.gif")%>");
        $("#"+newProteinId+"_details").hide();
        $("#"+oldProteinId+"_details").hide();
    }
    else
    {
        $(element).removeClass('closed').addClass('open');
        $(element).children('img').attr('src', "<%=getWebappURL("_images/minus.gif")%>");
        $("#"+newProteinId+"_details").show();
        $("#"+oldProteinId+"_details").show();
    }
}

function toggleCheckboxSelection(element)
{
    var cls = element.attr('class').split(' ')[0]; // get the first class name
    // alert(cls);
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


<%int colspan=3;%>
<form <%=formAction(TargetedMSController.ResolveConflictAction.class, Method.Post)%>><labkey:csrf/>
<input type="hidden" name="conflictLevel" value="protein"/>
<table class="labkey-data-region labkey-show-borders">
    <thead>
       <tr>
            <th colspan="<%=colspan%>"><div class="labkey-button-bar" style="width:98%">Conflicting Proteins in Document</div></th>
            <th colspan="<%=colspan%>"><div class="labkey-button-bar" style="width:98%">Current Library Proteins</div></th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td class="labkey-column-header"></td>
            <td class="labkey-column-header">Protein</td>
            <td class="labkey-column-header">Document</td>
            <!--<td class="labkey-column-header">ProteinId</td>-->

            <td class="labkey-column-header"></td>
            <td class="labkey-column-header">Protein</td>
            <td class="labkey-column-header">Document</td>
            <!--<td class="labkey-column-header">ProteinId</td>-->
        </tr>
    <% int index = 0; %>
    <%for (ConflictProtein protein: bean.getConflictProteinList()) {%>
         <tr class="labkey-alternate-row">

             <!-- New representative protein -->
             <td class="representative newProtein <%=protein.getNewProteinId()%>">
                 <input type="checkbox" class="<%=protein.getNewProteinId()%> newProtein"
                                        name="selectedInputValues"
                                        value="<%=protein.getNewProteinId()%>_<%=protein.getOldProteinId()%>"
                                        checked/></td>
             <!--<td class="representative newProtein <%=protein.getNewProteinId()%>"><%=protein.getNewProteinId()%></td>-->
             <td class="representative newProtein label <%=protein.getNewProteinId()%>">
                 <span class="label" id="<%=protein.getNewProteinId()%>_<%=protein.getOldProteinId()%>">
                     <img src="<%=getWebappURL("_images/plus.gif")%>"/>
                     <%=protein.getNewProteinLabel()%>
                 </span>
             </td>
             <td class="representative newProtein <%=protein.getNewProteinId()%>">
                 <a href="<%=h(runProteinDetailsUrl)%>id=<%=protein.getNewProteinId()%>"><%=protein.getNewRunFile()%></a>
             </td>

             <!-- Old representative protein -->
             <td class="oldProtein <%=protein.getNewProteinId()%>">
                 <input type="checkbox" class="<%=protein.getNewProteinId()%> oldProtein"
                                        name="selectedInputValues"
                                        value="<%=protein.getOldProteinId()%>_<%=protein.getNewProteinId()%>" /></td>
             <!--<td class="oldProtein <%=protein.getNewProteinId()%>"><%=protein.getOldProteinId()%></td>-->
             <td class="oldProtein label <%=protein.getNewProteinId()%>">
                 <span class="label" id="<%=protein.getNewProteinId()%>_<%=protein.getOldProteinId()%>">
                     <img src="<%=getWebappURL("_images/plus.gif")%>"/>
                     <%=protein.getOldProteinLabel()%>
                 </span>
             </td>
             <td class="oldProtein <%=protein.getNewProteinId()%>">
                 <a href="<%=h(runProteinDetailsUrl)%>id=<%=protein.getOldProteinId()%>"><%=protein.getOldRunFile()%></a>
             </td>
         </tr>
        <!-- This is a hidden table row where peptide and transition details will be displayed -->
        <tr>
            <td colspan="<%=colspan%>"><div id="<%=protein.getNewProteinId()%>_details" style="display:none;">Loading...</div></td>
            <td colspan="<%=colspan%>"><div id="<%=protein.getOldProteinId()%>_details" style="display:none;">Loading...</div></td>
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


