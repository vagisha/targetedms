<%
/*
 * Copyright (c) 2014-2019 LabKey Corporation
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
<%@ page import="org.labkey.api.view.ActionURL"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.targetedms.parser.Peptide" %>
<%@ page import="org.labkey.targetedms.parser.Replicate" %>
<%@ page import="java.util.List" %>
<%@ page import="org.labkey.targetedms.parser.ReplicateAnnotation" %>
<%@ page import="org.labkey.targetedms.parser.Molecule" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("Ext4");
    }
%>
<%
    JspView<TargetedMSController.SummaryChartBean> me = (JspView<TargetedMSController.SummaryChartBean>) HttpView.currentView();
    TargetedMSController.SummaryChartBean bean = me.getModelBean();
    int peptideGroupId = bean.getPeptideGroupId(); // Used when displaying peak areas for all peptides of a protein

    List<Replicate> replicateList = bean.getReplicateList();
    List<String> replicateAnnotationNameList = bean.getReplicateAnnotationNameList();
    List<ReplicateAnnotation> replicateAnnotationValueList = bean.getReplicateAnnotationValueList();

    ActionURL peakAreaUrl = new ActionURL(TargetedMSController.ShowPeakAreasAction.class, getContainer());
    ActionURL retentionTimesUrl = new ActionURL(TargetedMSController.ShowRetentionTimesChartAction.class, getContainer());

    // for proteomics summary charts
    List<Peptide> peptideList = bean.getPeptideList();
    int peptideId = bean.getPeptideId(); // Used when displaying peak areas for a single peptide in multiple replicates
                                         // or grouped by replicate annotation.
    int precursorId = bean.getPrecursorId(); // Used when displaying peak areas for a single precursor

    // for small molecule summary charts
    List<Molecule> moleculeList = bean.getMoleculeList();
    int moleculeId = bean.getMoleculeId();
    int moleculePrecursorId = bean.getMoleculePrecursorId();

    if ((peptideList != null && !peptideList.isEmpty()) || peptideId != 0 || precursorId != 0)
    {
        peakAreaUrl.addParameter("asProteomics", true);
        retentionTimesUrl.addParameter("asProteomics", true);
    }

    if (peptideGroupId != 0)
    {
        peakAreaUrl.addParameter("peptideGroupId", peptideGroupId);
        retentionTimesUrl.addParameter("peptideGroupId", peptideGroupId);
    }
    else if (peptideId != 0)
    {
        peakAreaUrl.addParameter("peptideId", peptideId);
        retentionTimesUrl.addParameter("peptideId", peptideId);
    }
    else if (moleculeId != 0)
    {
        peakAreaUrl.addParameter("moleculeId", moleculeId);
        retentionTimesUrl.addParameter("moleculeId", moleculeId);
    }

    if (precursorId != 0)
    {
        peakAreaUrl.addParameter("precursorId", precursorId);
        retentionTimesUrl.addParameter("precursorId", precursorId);
    }
    else if (moleculePrecursorId != 0)
    {
        peakAreaUrl.addParameter("moleculePrecursorId", moleculePrecursorId);
        retentionTimesUrl.addParameter("moleculePrecursorId", moleculePrecursorId);
    }
    peakAreaUrl.addParameter("chartWidth", bean.getInitialWidth());
    peakAreaUrl.addParameter("chartHeight", bean.getInitialHeight());
    retentionTimesUrl.addParameter("chartWidth", bean.getInitialWidth());
    retentionTimesUrl.addParameter("chartHeight", bean.getInitialHeight());
%>
<style>
    .summary_form_box {
        padding-bottom: 25px;
    }

    .summary_title_box {
        border: #ccc 1px solid;
        float: left;
        margin-right: 1em;
        margin-bottom: 1em;
    }

    .summary_title_box .title {
        position: relative;
        top : -0.6em;
        margin-left: 20px;
        display: inline;
        background-color: white;
        font-size: 18px;
        padding: 0 5px;
    }
    .valuelabel
    {
        color: #000000;
        font-size: 10px;
        padding-left:105px;
    }
</style>
<script type="text/javascript">
    Ext4.onReady(function() {

    // data stores
    var replicateStore = Ext4.create('Ext.data.Store', {
        fields: ['name', 'replicateId'],
        data:   [
            {"name":"All", "replicateId":"0"}
            <%for(int i = 0; i < replicateList.size(); i++) {%>
            ,{"name":<%=q(replicateList.get(i).getName())%>, "replicateId":"<%=replicateList.get(i).getId()%>" }
            <%}%>
        ]
    });

    var replicateAnnotNameStore = Ext4.create('Ext.data.Store', {
        fields: ['name'],
        data:   [
            {"name":"None"}
            <%for(int i = 0; i < replicateAnnotationNameList.size(); i++){%>
            ,{"name":<%=q(replicateAnnotationNameList.get(i))%>}
            <%}%>
        ]
    });

    var replicateAnnotNameValueStore = Ext4.create('Ext.data.Store', {
        fields: ['name-value'],
        data:   [
            {"name-value":"None"}
            <%for(int i = 0; i < replicateAnnotationValueList.size(); i++){%>
            ,{"name-value":<%=q(replicateAnnotationValueList.get(i).getDisplayName())%>}

            <%}%>
        ]
    });

    var valueStore = Ext4.create('Ext.data.Store', {
        fields: ['value'],
        data:   [
            {"value":"All"},
            {"value":"Retention Time"},
            {"value":"FWHM"},
            {"value":"FWB"}
        ]
    });

    var peptideStore = Ext4.create('Ext.data.Store', {
        fields: ['sequence', 'peptideId'],
        data: [
            {"sequence":"All", "peptideId":"0"}
            <%for(int i = 0; i < peptideList.size(); i++) {%>
            ,{"sequence":<%=q(peptideList.get(i).getPeptideModifiedSequence())%>, "peptideId": "<%=peptideList.get(i).getId()%>"}
            <%}%>
        ]
    });

    var moleculeStore = Ext4.create('Ext.data.Store', {
        fields: ['customIonName', 'moleculeId'],
        data: [
            {"customIonName":"All", "moleculeId":"0"}
            <%for(int i = 0; i < moleculeList.size(); i++) {%>
            ,{"customIonName":<%=q(moleculeList.get(i).getCustomIonName())%>, "moleculeId": "<%=moleculeList.get(i).getId()%>"}
            <%}%>
        ]
    });

    // combo boxes
    var replicateComboBox;
    if(replicateStore.count() > 0)
    {
        replicateComboBox = Ext4.create('Ext.form.ComboBox', {
            fieldLabel: 'Replicate',
            store: replicateStore ,
            queryMode: 'local',
            displayField: 'name',
            valueField: 'replicateId',
            forceSelection: 'true',
            allowBlank: 'false',
            value: replicateStore.getAt(0),
            width: 400,
            listeners:{
                select: function(combo, record, index){
                    var selected = combo.getValue();
                    if(selected != 0)
                    {
                        replicateAnnotNameComboBox.disable();
                        replicateAnnotNameComboBox.setValue(replicateAnnotNameStore.getAt(0));
                        peptideComboBox.disable();
                        peptideComboBox.setValue(peptideStore.getAt(0));
                        moleculeComboBox.disable();
                        moleculeComboBox.setValue(moleculeStore.getAt(0));
                        replicateAnnotNameValueComboBox.disable();
                        replicateAnnotNameValueComboBox.setValue(replicateAnnotNameValueStore.getAt(0));
                    }
                    else
                    {
                        replicateAnnotNameComboBox.enable();
                        peptideComboBox.enable();
                        moleculeComboBox.enable();
                        replicateAnnotNameValueComboBox.enable();
                    }
                    updateCvCheckbox();
                }
            }
        });
    }

    var replicateAnnotNameComboBox;
    if(replicateAnnotNameStore.count() > 0)
    {
        replicateAnnotNameComboBox = Ext4.create('Ext.form.ComboBox',{
            fieldLabel: 'Group By',
            store: replicateAnnotNameStore ,
            queryMode: 'local',
            displayField: 'name',
            valueField: 'name',
            forceSelection: 'true',
            allowBlank: 'false',
            value: replicateAnnotNameStore.getAt(0),
            width: 400,
            listeners:{
                select: function(combo, record, index) {
                    updateCvCheckbox();
                }
            }
        });
    }

    var valueComboBox;
    valueComboBox = Ext4.create('Ext.form.ComboBox',{
        fieldLabel: 'Value',
        store: valueStore ,
        queryMode: 'local',
        displayField: 'value',
        valueField: 'value',
        forceSelection: 'true',
        allowBlank: 'false',
        value: valueStore.getAt(0),
        width: 400,
        listeners:{
            select: function(combo, record, index) {
                updateCvCheckbox();
            }
        }
    });

    var valueLabel;
    valueLabel = Ext4.create('Ext.form.Label',
            {
                fieldLabel: 'valueLabel',
                forId: 'valueLabel',
                text: 'Value only effects Retention Times chart.',
                baseCls: "valuelabel"
            });

    var replicateAnnotNameValueComboBox;
    if(replicateAnnotNameValueStore.count() > 0)
    {
        replicateAnnotNameValueComboBox = Ext4.create('Ext.form.ComboBox',{
            fieldLabel: 'Filter',
            lazyRender: true,
            store: replicateAnnotNameValueStore ,
            queryMode: 'local',
            displayField: 'name-value',
            valueField: 'name-value',
            forceSelection: 'true',
            allowBlank: 'false',
            value: replicateAnnotNameValueStore.getAt(0),
            width: 400,
            listeners:{
                select: function(combo, record, index){
                    var selected = combo.getValue();
                    if(selected != 0)
                    {
                        updateCvCheckbox();
                    }
                }
            }
        });
    }

    var peptideComboBox;
    if(peptideStore.count() > 0)
    {
        peptideComboBox = Ext4.create('Ext.form.ComboBox', {
            fieldLabel: 'Peptide',
            store: peptideStore,
            queryMods: 'local',
            displayField: 'sequence',
            valueField: 'peptideId',
            forceSelection: 'true',
            allowBlank: 'false',
            value: peptideStore.getAt(0),
            width: 400,
            listeners:{
                select: function(combo, record, index){
                    var selected = combo.getValue();
                    if(selected != 0)
                    {
                        replicateComboBox.setValue(replicateStore.getAt(0));
                        replicateComboBox.disable();
                    }
                    else
                    {
                        replicateComboBox.enable();
                    }
                    updateCvCheckbox();
                }
            }
        });
    }

    var moleculeComboBox;
    if(moleculeStore.count() > 0)
    {
        moleculeComboBox = Ext4.create('Ext.form.ComboBox', {
            fieldLabel: 'Small Molecule',
            store: moleculeStore,
            queryMods: 'local',
            displayField: 'customIonName',
            valueField: 'moleculeId',
            forceSelection: 'true',
            allowBlank: 'false',
            value: moleculeStore.getAt(0),
            width: 400,
            listeners:{
                select: function(combo, record, index){
                    var selected = combo.getValue();
                    if(selected != 0)
                    {
                        replicateComboBox.setValue(replicateStore.getAt(0));
                        replicateComboBox.disable();
                    }
                    else
                    {
                        replicateComboBox.enable();
                    }
                    updateCvCheckbox();
                }
            }
        });
    }

    // check boxes
    var cvValuesCheckbox = Ext4.create('Ext.form.Checkbox', {
        id: 'cvCheckbox',
        fieldLabel: 'CV Values'
    });

    var logValuesCheckbox = Ext4.create('Ext.form.Checkbox', {
        id: 'logCheckbox',
        fieldLabel: 'Log Scale'
    });

    // peak areas graph
    var peakAreasImg = Ext4.create('Ext.Img', {
        src: '<%=peakAreaUrl%>',
        renderTo: Ext4.get('peakAreasGraphImg')
    });

    var retentionTimesImg = Ext4.create('Ext.Img', {
        src: '<%=retentionTimesUrl%>',
        renderTo: Ext4.get('retentionTimesGraphImg')
    });

    // buttons
    var updateBtn = Ext4.create('Ext.button.Button', {
        text:'Update',
        handler: function(){

            var params = {
                asProteomics: <%=(peptideList != null && !peptideList.isEmpty()) || peptideId != 0 || precursorId != 0%>,
                peptideGroupId: <%=peptideGroupId%>,
                replicateId: replicateComboBox.getValue(),
                groupByReplicateAnnotName: replicateAnnotNameComboBox.getValue(),
                filterByReplicateAnnotName: replicateAnnotNameValueComboBox.getValue(),
                peptideId: peptideStore.count() > 1 ? peptideComboBox.getValue() : <%=peptideId%>,
                moleculeId: moleculeStore.count() > 1 ? moleculeComboBox.getValue() : <%=moleculeId%>,
                cvValues: cvValuesCheckbox.getValue(),
                logValues: logValuesCheckbox.getValue(),
                chartWidth: chartWidthTb.getValue(),
                chartHeight: chartHeightTb.getValue()
            };

            var pearAreaUrl =  LABKEY.ActionURL.buildURL(
                    'targetedms',  // controller
                    'showPeakAreas', // action
                    LABKEY.ActionURL.getContainer(),
                    params
            );

            var retentionTimeParams = Ext4.apply(Ext4.clone(params), {
                value: valueComboBox.getValue()
            });

            var retentionTimesUrl =  LABKEY.ActionURL.buildURL(
                    'targetedms',  // controller
                    'showRetentionTimesChart', // action
                    LABKEY.ActionURL.getContainer(),
                    retentionTimeParams
            );

            // change the src of the image
            peakAreasImg.setSrc(pearAreaUrl);
            retentionTimesImg.setSrc(retentionTimesUrl);
        }
    });

    // chart width and height
    var chartWidthTb = Ext4.create('Ext.form.TextField',{
                fieldLabel: "Width",
                name: "chartWidth",
                id: "chartWidth",
                allowBlank: false,
                maskRe: /[0-9]/,
                value: "<%= bean.getInitialWidth() %>"}
    );
    var chartHeightTb = Ext4.create('Ext.form.TextField',{
                fieldLabel: "Height",
                name: "chartHeight",
                id: "chartHeight",
                allowBlank: false,
                maskRe: /[0-9]/,
                value: "<%= bean.getInitialHeight() %>"}
    );

    var items = [];
    if(replicateStore.count() > 2) items.push(replicateComboBox);
    if(replicateAnnotNameStore.count() > 1) items.push(replicateAnnotNameComboBox);
    if(replicateAnnotNameValueStore.count() > 1) items.push(replicateAnnotNameValueComboBox);
    if(peptideStore.count() > 2) items.push(peptideComboBox);
    if(moleculeStore.count() > 2) items.push(moleculeComboBox);
    if(replicateStore.count() > 2 || replicateAnnotNameStore.count() > 1)
    {
        items.push(cvValuesCheckbox);
    }
    items.push(logValuesCheckbox);
    items.push(chartWidthTb);
    items.push(chartHeightTb);
    items.push(valueComboBox);
    items.push(valueLabel);

    if(items.length > 0)
    {
        Ext4.create('Ext.form.Panel', {
            renderTo: Ext4.get('peakAreasFormDiv'),
            border: false, frame: false,
            buttonAlign: 'left',
            items: items,
            buttons: [updateBtn],
            defaults: {
                labelWidth: 150,
                labelStyle: 'background-color: #E0E6EA; padding: 2px 4px; margin: 0px;'
            }
        });
    }
    updateCvCheckbox();


    function updateCvCheckbox()
    {
        if(!cvValuesCheckbox.isVisible())
            return;

        var allReplicates = replicateComboBox.getValue() == replicateStore.getAt(0).get('replicateId'); // replicate = 'All'
        var noAnnotations = replicateAnnotNameComboBox.getValue() == replicateAnnotNameStore.getAt(0).get('name'); // annotation = 'None'

        var replicateEnabled = false;
        if(replicateComboBox.isVisible())
            replicateEnabled = replicateComboBox.disabled == false;
        var annotEnabled = false;
        if(replicateAnnotNameComboBox.isVisible())
            annotEnabled = replicateAnnotNameComboBox.disabled == false;

        //alert("allReplicates: "+allReplicates+", noAnnotations: "+noAnnotations+", replicateEnabled: "+replicateEnabled+", annotEnabled: "+annotEnabled);
        if((replicateEnabled && allReplicates) ||
                (annotEnabled && !noAnnotations)
                )
        {
            cvValuesCheckbox.enable();
        }
        else
        {
            cvValuesCheckbox.setValue(false);
            cvValuesCheckbox.disable();
        }
    }
});

</script>
<div>
    <div id="peakAreasFormDiv" class="summary_form_box" style="display: <%= bean.isShowControls() ? "block" : "none"%>;"></div>
    <div class="summary_title_box">
        <h3 class="title">Peak Areas</h3>
        <div id="peakAreasGraphImg"></div>
    </div>
    <div class="summary_title_box">
        <h3 class="title">Retention Times</h3>
        <div id="retentionTimesGraphImg"></div>
    </div>
</div>

