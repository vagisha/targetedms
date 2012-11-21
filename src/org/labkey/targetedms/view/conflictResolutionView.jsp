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
<%@ page import="org.labkey.api.settings.AppProps" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.targetedms.conflict.ConflictProtein" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    ViewContext context = HttpView.currentContext();
    JspView<TargetedMSController.ProteinConflictBean> me = (JspView<TargetedMSController.ProteinConflictBean>) HttpView.currentView();
    TargetedMSController.ProteinConflictBean bean = me.getModelBean();
    ActionURL conflictPeptidesUrl = new ActionURL(TargetedMSController.ProteinConflictPeptidesAjaxAction.class, context.getContainer());

    String plusImgUrl = request.getContextPath()+"/_images/plus.gif";
    String minusImgUrl = request.getContextPath()+"/_images/minus.gif";
%>

<style type="text/css">
    td.representative {background-color:#8FBC8F;}
    span.label {text-decoration: underline; cursor: pointer;}
</style>

<script type="text/javascript" src="//ajax.googleapis.com/ajax/libs/jquery/1.8.2/jquery.min.js"></script>

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
                failure: null
            });
    }

    if($(element).hasClass('open'))
    {
        $(element).removeClass('open').addClass('closed');
        $(element).children('img').attr('src', "<%=plusImgUrl%>");
        $("#"+newProteinId+"_details").hide();
        $("#"+oldProteinId+"_details").hide();
    }
    else
    {
        $(element).removeClass('closed').addClass('open');
        $(element).children('img').attr('src', "<%=minusImgUrl%>");
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

<%int colspan=3;%>
<form <%=formAction(TargetedMSController.ResolveConflictAction.class, Method.Post)%>>
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
            <td class="labkey-column-header">Protein</td>
            <td class="labkey-column-header">Run</td>
            <!--<td class="labkey-column-header">ProteinId</td>-->

            <td class="labkey-column-header"></td>
            <td class="labkey-column-header">Protein</td>
            <td class="labkey-column-header">Run</td>
            <!--<td class="labkey-column-header">ProteinId</td>-->
        </tr>
    <% int index = 0; %>
    <%for (ConflictProtein protein: bean.getConflictNodeList()) {%>
         <tr class="labkey-alternate-row">

             <!-- New representative protein -->
             <td class="representative newProtein <%=protein.getNewProteinId()%>">
                 <input type="checkbox" class="<%=protein.getNewProteinId()%> newProtein"
                                        name="selectedInputValues"
                                        value="<%=protein.getNewProteinId()%>_<%=protein.getOldProteinId()%>"
                                        checked="checked"/></td>
             <!--<td class="representative newProtein <%=protein.getNewProteinId()%>"><%=protein.getNewProteinId()%></td>-->
             <td class="representative newProtein label <%=protein.getNewProteinId()%>">
                 <span class="label" id="<%=protein.getNewProteinId()%>_<%=protein.getOldProteinId()%>">
                     <img src="<%=plusImgUrl%>"/>
                     <%=protein.getNewProteinLabel()%>
                 </span>
             </td>
             <td class="representative newProtein <%=protein.getNewProteinId()%>"><%=protein.getNewRunFile()%></td>

             <!-- Old representative protein -->
             <td class="oldProtein <%=protein.getNewProteinId()%>">
                 <input type="checkbox" class="<%=protein.getNewProteinId()%> oldProtein"
                                        name="selectedInputValues"
                                        value="<%=protein.getOldProteinId()%>_<%=protein.getNewProteinId()%>" /></td>
             <!--<td class="oldProtein <%=protein.getNewProteinId()%>"><%=protein.getOldProteinId()%></td>-->
             <td class="oldProtein label <%=protein.getNewProteinId()%>">
                 <span class="label" id="<%=protein.getNewProteinId()%>_<%=protein.getOldProteinId()%>">
                     <img src="<%=plusImgUrl%>"/>
                     <%=protein.getOldProteinLabel()%>
                 </span>
             </td>
             <td class="oldProtein <%=protein.getNewProteinId()%>"><%=protein.getOldRunFile()%></td>
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
                <%=generateSubmitButton("Apply Changes")%>
                &nbsp;
                <%=generateButton("Cancel", TargetedMSController.ShowListAction.class)%>
            </td>
        </tr>
    </tbody>
</table>


