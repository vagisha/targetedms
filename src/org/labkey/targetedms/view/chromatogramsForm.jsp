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
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.targetedms.parser.ReplicateAnnotation" %>
<%@ page import="org.labkey.targetedms.query.ReplicateManager" %>
<%@ page import="java.util.List" %>
<%@ page import="org.labkey.targetedms.parser.Replicate" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<TargetedMSController.PeptideChromatogramsViewBean> me = (JspView<TargetedMSController.PeptideChromatogramsViewBean>) HttpView.currentView();
    TargetedMSController.PeptideChromatogramsViewBean bean = me.getModelBean();
    bean.setReplicateAnnotationNameList(ReplicateManager.getReplicateAnnotationNamesForRun(bean.getRun().getId()));
    bean.setReplicatesFilter(ReplicateManager.getReplicatesForRun(bean.getRun().getId()));
    bean.setReplicateAnnotationValueList(ReplicateManager.getUniqueSortedAnnotationNameValue(bean.getRun().getId()));
    List<ReplicateAnnotation> replicateAnnotationList = bean.getReplicateAnnotationValueList();
    List<Replicate> replicatesList = bean.getReplicatesFilter();

//    pageContext.setAttribute("autocompleteReplicates", replicateAnnotationList);
    String filter = getViewContext().getRequest().getParameter("annotationsFilter");
    String replicatesFilter = getViewContext().getRequest().getParameter("replicatesFilter");
%>
<style>
    #allFilters
    {
        max-height: 300px;
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
</style>
<script type="text/javascript" src="/labkey/MS2/lorikeet_0.3/js/jquery-1.4.2.min.js"></script>
<script type="text/javascript">
    var hiddenFields;
    Ext4.onReady(function(){
        var replicateStore = Ext4.create('Ext.data.Store', {
            fields: ['replicateName','replicateId'],
            data:   [
                <%for(Replicate rep: replicatesList){%>
                {"replicateName":"<%=h(rep.getName())%>","replicateId":"<%=h(rep.getId())%>"},
                <%}%>
            ]
        });

        var filterStore = Ext4.create('Ext.data.Store', {
            fields: ['name-value'],
            data:   [
                <%for(ReplicateAnnotation rep: replicateAnnotationList){%>
                {"name-value":"<%=h(rep.getDisplayName())%>"},
                <%}%>
            ]
        });

        // syncMargin is the negative margin-left of the syncX and syncY fields.  If the filters are not being displayed
        // the sync fields slide under the width/heigh fields.
        var syncMargin;
        <%if(replicateAnnotationList.size() <= 1 && replicatesList.size() <= 1){%>
          syncMargin = 0;
        <%}else{%>
          syncMargin = 130;
        <%}%>

        var filter = "<%=h(filter)%>";
        var filters = filter.split(',');
        var replicateFilter = "<%=h(replicatesFilter)%>";
        var replicatesFilter = replicateFilter.split(',');

        var form = Ext4.create('Ext.form.Panel', {
            renderTo: 'formContainer',
            standardSubmit: true,
            name: 'chromForm',
            border: false, frame: false,
            defaults: {
                labelWidth: 150,
                labelHeight: 23,
                labelStyle: 'background-color: #E0E6EA; padding: 6px; margin-top:0px;'
            }, layout: {
                type: 'table',
                columns: 3
            }, items: [
                {
                    xtype:'hidden',
                    name: 'id',
                    value: <%=bean.getForm().getId()%>
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
                    colspan:2,
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
                    colspan:2,
                    style:"margin-left:-"+syncMargin+"px;"
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
                    width: 450,
                    height: 23,
                    colspan:2
                },
                {
                    xtype: 'displayfield',
                    name: "hideReplicates",
                    value:'<a style="color:#069; cursor:pointer;" id="clear-rep">Clear</a>',
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
                    width: 450,
                    height: 23,
                    colspan:2
                },
                {
                    xtype: 'displayfield',
                    value:'<a style="color:#069; cursor:pointer;" id="clear-annot">Clear</a>',
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
                    form.submit({
                    url: <%=q(bean.getResultsUri())%>,
                        method: 'GET'
                    });
                }
            }]
        })
        // This has to happen after form is rendered.
        form.getForm().findField("annotationsFilter").setValue(filters);
        form.getForm().findField("replicatesFilter").setValue(replicatesFilter);

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

        // Clears Annotations and Replciates filter box selected values.
        $('#clear-annot').click(clearAnnotationFilter);
        function clearAnnotationFilter() {
            form.getForm().findField('annotationsFilter').clearValue();
        }
        $('#clear-rep').click(clearReplicatesFilter);
        function clearReplicatesFilter() {
            form.getForm().findField('replicatesFilter').clearValue();
        }
    });
</script>

<div id="headContainer">
    <div id="formContainer"></div>
    <table id="allFilters"></table>
</div>