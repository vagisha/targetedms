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
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.targetedms.parser.ReplicateAnnotation" %>
<%@ page import="org.labkey.targetedms.query.ReplicateManager" %>
<%@ page import="java.util.List" %>
<%@ page import="org.labkey.targetedms.parser.Replicate" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("Ext4");
        dependencies.add("MS2/lorikeet_0.3/js/jquery-1.4.2.min.js");
    }
%>
<%
    JspView<TargetedMSController.GeneralMoleculeChromatogramsViewBean> me = (JspView<TargetedMSController.GeneralMoleculeChromatogramsViewBean>) HttpView.currentView();
    TargetedMSController.GeneralMoleculeChromatogramsViewBean bean = me.getModelBean();
    bean.setReplicateAnnotationNameList(ReplicateManager.getReplicateAnnotationNamesForRun(bean.getRun().getId()));
    bean.setReplicatesFilter(ReplicateManager.getReplicatesForRun(bean.getRun().getId()));
    bean.setReplicateAnnotationValueList(ReplicateManager.getUniqueSortedAnnotationNameValue(bean.getRun().getId()));
    List<ReplicateAnnotation> replicateAnnotationList = bean.getReplicateAnnotationValueList();
    List<Replicate> replicatesList = bean.getReplicatesFilter();

    String selectedAnnotations = getViewContext().getRequest().getParameter("annotationsFilter");
    String selectedReplicates = getViewContext().getRequest().getParameter("replicatesFilter");
%>
<style type="text/css">
    #allFilters
    {
        overflow:hidden;
    }
    .filter
    {
        background-color:#fff;
        padding:0px 1px 0px 1px;
        margin:0;
        border:1px solid #ccc;
        border-radius:3px;
        float:left;
    }
    .filter td{
        font-size:11px !important;
    }
    #allFilters
    {
        float:left;
    }
    #headContainer
    {
        width:100%;
        overflow:hidden;
    }
    .chrom_title_box {
        border: #ccc 1px solid;
        margin-top:20px;
        padding: 5px;
    }

    .chrom_title_box .title {
        position: relative;
        top : -0.9em;
        margin-left: 20px;
        display: inline;
        background-color: white;
        font-size: 16px;
        padding: 0 5px;
    }
    .item
    {
        font-size:10px;
    }
</style>
<script type="text/javascript">
    var hiddenFields;
    var replicateStore;
    var filterStore;
    var selectedReplicatesFilterList;
    var selectedAnnotationsFilterList;
    var form;
    Ext4.onReady(function(){
        replicateStore = Ext4.create('Ext.data.Store', {
            fields: ['replicateName','replicateId'],
            data:   [
                <%int i = 0; for(Replicate rep: replicatesList){
                    if(i++ > 0){%>, <%}%>
                    {"replicateName":"<%=h(rep.getName())%>","replicateId":"<%=h(rep.getId())%>"}
                <%}%>
            ]
        });

        filterStore = Ext4.create('Ext.data.Store', {
            fields: ['name-value'],
            data:   [
                <%int j = 0; for(ReplicateAnnotation rep: replicateAnnotationList){
                    if(j++ > 0){%>,<%}%>
                    {"name-value":"<%=h(rep.getDisplayName())%>"}
            <%}%>
            ]
        });

        // syncMargin is the negative margin-left of the syncX and syncY fields.  If the filters are not being displayed
        // the sync fields slide under the width/heigh fields.
        var syncMargin;
        <%if(replicateAnnotationList.size() > 1 || replicatesList.size()  > 1){%>
          syncMargin = 130;
        <%}else{%>
          syncMargin = 0;
        <%}%>
        var annotationsFilter = "<%=h(selectedAnnotations)%>";
        selectedAnnotationsFilterList = annotationsFilter.split(',');
        var replicateFilter = "<%=h(selectedReplicates)%>";
        selectedReplicatesFilterList = replicateFilter.split(',');
        form = Ext4.create('Ext.form.Panel', {
            renderTo: 'formContainer',
            standardSubmit: true,
            name: 'chromForm',
            border: false, frame: false,
            width:550,
            defaults: {
                labelWidth: 150,
                labelHeight: 23,
                labelStyle: 'background-color: #E0E6EA; padding: 2px 4px; margin:0px;'
            }, layout: {
                type: 'table',
                columns: 3
            }, items: [
                { xtype: 'hidden', name: 'X-LABKEY-CSRF', value: LABKEY.CSRF },
                {
                    xtype:'hidden',
                    name: 'id',
                    value: <%=bean.getForm().getId()%>
                },
                {
                    xtype:'hidden',
                    name: 'update',
                    value: <%=bean.getForm().isUpdate()%>
                },
                {
                    xtype: 'textfield',
                    fieldLabel: 'Chart Width',
                    name: 'chartWidth',
                    width: 300,
                    value: <%= bean.getForm().getChartWidth() %>,
                    style: "margin-right:30px;"
                },
                {
                    xtype: 'checkbox',
                    name: 'syncX',
                    fieldLabel: 'Synchronize X-axis',
                    inputValue: true,
                    checked: <%=bean.getForm().isSyncX()%>,
                    colspan:3,
                    style:"margin-left:-"+syncMargin+"px;"
                },
                {
                    xtype: 'textfield',
                    fieldLabel: 'Chart Height',
                    width: 300,
                    name: 'chartHeight',
                    value: <%= bean.getForm().getChartHeight() %>
                },
                {
                    xtype: 'checkbox',
                    name: 'syncY',
                    fieldLabel: 'Synchronize Y-axis',
                    inputValue: true,
                    checked: <%=bean.getForm().isSyncY()%>,
                    colspan:3,
                    style:"margin-left:-"+syncMargin+"px;"
                },
                {
                    xtype: 'checkbox',
                    name: 'splitGraph',
                    fieldLabel: 'Split Graphs',
                    inputValue: true,
                    checked: <%=bean.getForm().isSplitGraph()%>,
                    colspan:4,
                    hidden:<%=!bean.canBeSplitView()%>
                },
                {
                    xtype: 'checkbox',
                    name: 'showOptimizationPeaks',
                    fieldLabel: 'Show Optimization Peaks',
                    inputValue: true,
                    checked: <%=bean.getForm().isShowOptimizationPeaks()%>,
                    colspan:4,
                    hidden:<%=!bean.isShowOptPeaksOption()%>
                },
                {
                    xtype: 'combobox',
                    name: 'replicatesFilter',
                    multiSelect: true,
                    store: replicateStore,
                    fieldLabel: 'Replicates <br />(multi-select)',
                    inputValue: true,
                    queryMode: 'local',
                    displayField: 'replicateName',
                    valueField: 'replicateId',
                    editable: false,
                    width: 450,
                    colspan:2,
                    listeners: {
                        change: function (field, newValue, oldValue) {
                            modifyFilter(newValue,oldValue,"replicateFilters");
                        }
                    }
                },
                {
                    xtype: 'displayfield',
                    name: "hideReplicates",
                    value:'<a style="color:#069; cursor:pointer;" id="clear-rep" onclick="clearReplicates()">Clear</a>',
                    style:'margin-left:15px;'
                },
                {
                    xtype: 'combobox',
                    name: 'annotationsFilter',
                    multiSelect: true,
                    store: filterStore,
                    fieldLabel: 'Annotations <br />(multi-select)',
                    inputValue: true,
                    queryMode: 'local',
                    displayField: 'name-value',
                    valueField: 'name-value',
                    editable: false,
                    width: 450,
                    colspan:2,
                    listeners: {
                        change: function (field, newValue, oldValue) {
                            modifyFilter(newValue,oldValue,"annotationFilters");
                        }
                    }
                },
                {
                    xtype: 'displayfield',
                    value:'<a style="color:#069; cursor:pointer;" id="clear-annot" onclick="clearAnnotations()">Clear</a>',
                    style:'margin-left:15px;',
                    name: "hideAnnotations"
                }
            ],
            buttonAlign: 'left',
            buttons: [{
                text: 'Update',
                handler: function(btn) {
                    form.getForm().findField("annotationsFilter").setValue(form.getForm().findField("annotationsFilter").getValue().join(","));
                    form.getForm().findField("replicatesFilter").setValue(form.getForm().findField("replicatesFilter").getValue().join(","));
                    form.getForm().findField("update").setValue(true);
                    form.submit({
                    url: <%=q(bean.getResultsUri())%>,
                        method: 'GET'
                    });
                }
            }]
        });

        // This has to happen after form is rendered.
        form.getForm().findField("annotationsFilter").setValue(selectedAnnotationsFilterList);
        for(var i = 0; i < selectedAnnotationsFilterList.length; i++)
        {
            if(selectedAnnotationsFilterList[i])
            {
                appendFilterItem($('#annotationFilters'), selectedAnnotationsFilterList[i], "annotationFilters",selectedAnnotationsFilterList[i]);
            }
        }

        form.getForm().findField("replicatesFilter").setValue(selectedReplicatesFilterList);
        for(var i = 0; i < selectedReplicatesFilterList.length; i++)
        {
            var val;
            var id;
            for(var a = 0; a < replicateStore.getCount(); a++)
            {
                if(replicateStore.data.items[a].data.replicateId == selectedReplicatesFilterList[i])
                {
                    val = replicateStore.data.items[a].data.replicateName;
                    id = replicateStore.data.items[a].data.replicateId;
                }
            }
            if(val)
            {
                appendFilterItem($('#replicateFilters'), val, "replicateFilters", id);
            }
        }
        manageFilterListVisibility();

        // Hides multi-select combo boxes if there is only one or zero values in its value store.
        <%if(replicatesList == null || replicatesList.size() <= 1){%>
            form.getForm().findField("replicatesFilter").hide();
            form.getForm().findField("hideReplicates").hide();
            hiddenFields++;
        <%}%>
        <%if( replicateAnnotationList == null || replicateAnnotationList.size() <= 1){%>
            form.getForm().findField("annotationsFilter").hide();
            form.getForm().findField("hideAnnotations").hide();
            hiddenFields++;
        <%}%>

        // appendFilterItem adds a filter table row & column to the appropriate parent filter box.
        function appendFilterItem(element, displayValue, parentTable, id)
        {
            if(displayValue != null)
            {
                element.append("<tr id=\'"+id+"\'><td class="+"item"+"><img src='<%=getViewContext().getContextPath()%>/_images/delete.png' style='width:10px; height:10px; margin-right:3px;' onclick=\"deleteFilter(this, \'"+parentTable+"\')\">"+displayValue+"</td></tr>");
            }
        }

        // modifyFilter is triggered when user modifies selection through the combobox and will add/remove items from the list based on a newValue and an oldValue.
        function modifyFilter(newValues, oldValues, parentId)
        {
            if(newValues !=null && oldValues !=null)
            {
                if(newValues.length >= oldValues.length)
                {
                    for (var i = 0; i < newValues.length; i++)
                    {
                        if(oldValues.indexOf(newValues[i]) == -1)
                        {
                            var val;
                            var id;
                            if(parentId == "replicateFilters")
                            {
                                for(var a = 0; a < replicateStore.getCount(); a++)
                                {
                                    if(replicateStore.data.items[a].data.replicateId == newValues[i])
                                    {
                                        val = replicateStore.data.items[a].data.replicateName;
                                        id =    replicateStore.data.items[a].data.replicateId;
                                    }
                                }
                            }
                            else
                            {
                                val = newValues[i];
                                id = newValues[i];
                            }
                            appendFilterItem($('#'+parentId+''), val, parentId, id);
                        }
                    }
                }
                else if(newValues.length < oldValues.length)
                {
                    for (var j = 0; j < oldValues.length; j++) {
                        if(newValues.indexOf(oldValues[j]) == -1)
                        {
                            var textValue = oldValues[j];
                            if(parentId == "replicateFilters")
                            {
                                for(var a = 0; a < replicateStore.getCount(); a++)
                                {
                                    if(replicateStore.data.items[a].data.replicateId == oldValues[j])
                                    {
                                        textValue = replicateStore.data.items[a].data.replicateName;
                                    }
                                }
                            }
                            var el = findByText(document.getElementById("allFilters"), textValue);
                            var $el = $(el);
                            if(null != el)
                            {
                            $el.closest('tr').remove();
                            }
                        }
                    }
                }
            }
            manageFilterListVisibility();
        }
    });

    // Finds element that contains text value.  Searches all children of node.
    function findByText(node, text) {
        if(node.nodeValue == text) {
            return node.parentNode;
        }

        for (var i = 0; i < node.childNodes.length; i++) {
            var returnValue = findByText(node.childNodes[i], text);
            if (returnValue != null) {
                return returnValue;
            }
        }

        return null;
    }

    // Clears Annotations filter box selected values.
    function clearAnnotations() {
        form.getForm().findField('annotationsFilter').clearValue();
        $('#annotationFilters').empty();
        manageFilterListVisibility();
    }

    // Clears Replicates filter box selected values.
    function clearReplicates() {
        form.getForm().findField('replicatesFilter').clearValue();
        $('#replicateFilters').empty();
        manageFilterListVisibility();
    }

    // Manages the visibility of the filter lists in the UI.
    function manageFilterListVisibility()
    {
        if($('#replicateFilters').text().replace(/\s+/g, '') == '')
        {
              $('#reptitle').hide();
        }
        else
        {
            $('#reptitle').show();
        }

        if($('#annotationFilters').text().replace(/\s+/g, '') == '')
        {
            $('#annottitle').hide();
        }
        else
        {
            $('#annottitle').show();
        }
    }

    // Triggered by onclick() function in each list element in the [-](delete) button.
    function deleteFilter(el, parentTable)
    {
        var values;
        var formId;
        var id = el.parentElement.parentElement.id;
        if(parentTable == "annotationFilters")
        {
            values = form.getForm().findField("annotationsFilter").getValue();
            formId =    "annotationsFilter";
        }
        else
        {
            values = form.getForm().findField("replicatesFilter").getValue();
            formId = "replicatesFilter";
        }
        if(null != el)
        {
            var index = values.indexOf(id);
            values.splice(index, 1);
            el.parentElement.parentElement.parentNode.removeChild( el.parentElement.parentElement );
            form.getForm().findField(formId).setValue(values);
        }
        manageFilterListVisibility();
    }

    $(document).ready(function() {
        if ( $('#formContainer').length) // formContainer must exist inorder for showChart to work.
        {
            showChart();
        }
    });

    // Handels showing and hiding the form.
    function showChart()
    {
        if($('#showGraphImg').attr('src') === LABKEY.contextPath + "/_images/plus.gif")
        {
            $('#showGraphImg').attr('src', LABKEY.contextPath + "/_images/minus.gif");
            $('#formContainer').show("slow");
            $('#allFilters').show("slow");
        }
        else
        {
            $('#showGraphImg').attr('src', LABKEY.contextPath + "/_images/plus.gif");
            $('#formContainer').hide("slow");
            $('#allFilters').hide("slow");
        }
    }
</script>
<div id="headContainer">
    <div  onclick="showChart()" style="margin-bottom: 10px;"><img id="showGraphImg" src="<%=getViewContext().getContextPath()%>/_images/minus.gif"> <strong>Display Chart Settings</strong></div>
    <div id="formContainer" style="float:left; width:550px; padding-bottom: 25px;"></div>
    <div id="allFilters" style="float:left;">

        <div style="float:left;" class="chrom_title_box" id="reptitle" >
            <h3 class="title">Replicate Filters</h3>
        <table id="replicateFilters"></table>
        </div>

        <div style="float:left; margin-left:20px;" class="chrom_title_box" id="annottitle">
            <h4 class="title">Annotation Filters</h4>
        <table id="annotationFilters"></table>
        </div>
    </div>
</div>