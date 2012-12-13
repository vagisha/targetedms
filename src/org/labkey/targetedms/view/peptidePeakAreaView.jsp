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

<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView"%>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="java.util.List" %>
<%@ page import="org.labkey.targetedms.parser.Replicate" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.targetedms.parser.Peptide" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<TargetedMSController.PeakAreaGraphBean> me = (JspView<TargetedMSController.PeakAreaGraphBean>) HttpView.currentView();
    TargetedMSController.PeakAreaGraphBean bean = me.getModelBean();
    int peptideGroupId = bean.getPeptideGroupId(); // Used when displaying peak areas for all peptides of a protein
    int peptideId = bean.getPeptideId(); // Used when displaying peak areas for a single peptide in multiple replicates
                                         // or grouped by replicate annotation.
    List<Replicate> replicateList = bean.getReplicateList();
    List<String> replicateAnnotationNameList = bean.getReplicateAnnotationNameList();
    List<Peptide> peptideList = bean.getPeptideList();

    ActionURL peakAreaUrl = new ActionURL(TargetedMSController.ShowPeptidePeakAreasAction.class, getViewContext().getContainer());
    if(peptideGroupId != 0)
        peakAreaUrl.addParameter("peptideGroupId", peptideGroupId);
    else if(peptideId != 0)
        peakAreaUrl.addParameter("peptideId", peptideId);
%>
<script type="text/javascript">
    Ext.onReady(function() {

        // data stores
        var replicateStore = Ext4.create('Ext.data.Store', {
           fields: ['name', 'replicateId'],
           data:   [
               {"name":"All", "replicateId":"0"},
               <%for(int i = 0; i < replicateList.size(); i++) {%>
               {"name":"<%=replicateList.get(i).getName()%>", "replicateId":"<%=replicateList.get(i).getId()%>" },
               <%}%>
           ]
        });

        var replicateAnnotNameStore = Ext4.create('Ext.data.Store', {
            fields: ['name'],
            data:   [
                {"name":"None"},
                <%for(int i = 0; i < replicateAnnotationNameList.size(); i++){%>
                    {"name":"<%=replicateAnnotationNameList.get(i)%>"},
                <%}%>
            ]
        });

        var peptideStore = Ext4.create('Ext.data.Store', {
            fields: ['sequence', 'peptideId'],
            data: [
                {"sequence":"All", "peptideId":"0"},
                <%for(int i = 0; i < peptideList.size(); i++) {%>
                {"sequence":"<%=peptideList.get(i).getSequence()%>", "peptideId":"<%=peptideList.get(i).getId()%>"},
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
                                                    }
                                                    else
                                                    {
                                                        replicateAnnotNameComboBox.enable();
                                                        peptideComboBox.enable();
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

        // check boxes
        var cvValuesCheckbox = Ext4.create('Ext.form.Checkbox', {
                                            id: 'cvCheckbox',
                                            fieldLabel: 'CV Values'
        });

        // peak areas graph
        var peakAreasImg = Ext4.create('Ext.Img', {
                                src: '<%=peakAreaUrl%>',
                                renderTo: Ext.get('peakAreasGraphImg')
                            });

        // buttons
        var updateBtn = new Ext.Button({text:'Update',
            handler: function(){

                var url =  LABKEY.ActionURL.buildURL(
                                'targetedms',  // controller
                                'showPeptidePeakAreas', // action
                                 LABKEY.ActionURL.getContainer(),
                                 {
                                    peptideGroupId: <%=peptideGroupId%>,
                                    replicateId: replicateComboBox.getValue(),
                                    groupByReplicateAnnotName: replicateAnnotNameComboBox.getValue(),
                                    peptideId: peptideStore.count() > 1 ? peptideComboBox.getValue() : <%=peptideId%>,
                                    cvValues: cvValuesCheckbox.getValue(),
                                    chartWidth: chartWidthTb.getValue(),
                                    chartHeight: chartHeightTb.getValue()

                                 }
                            );
                    //alert(url);
                    // change the src of the image
                    peakAreasImg.setSrc(url);
                }
        });

        // chart width and height
        var chartWidthTb = Ext4.create('Ext.form.TextField',{
                    fieldLabel: "Width",
                    name: "chartWidth",
                    id: "chartWidth",
                    allowBlank: false,
                    maskRe: /[0-9]/,
                    value: "600"}
        );
        var chartHeightTb = Ext4.create('Ext.form.TextField',{
                    fieldLabel: "Height",
                    name: "chartHeight",
                    id: "chartHeight",
                    allowBlank: false,
                    maskRe: /[0-9]/,
                    value: "400"}
        );

        var items = [];
        if(replicateStore.count() > 2) items.push(replicateComboBox);
        if(replicateAnnotNameStore.count() > 1) items.push(replicateAnnotNameComboBox);
        if(peptideStore.count() > 2) items.push(peptideComboBox);
        if(replicateStore.count() > 2 || replicateAnnotNameStore.count() > 1)
        {
            items.push(cvValuesCheckbox);
        }
        items.push(chartWidthTb);
        items.push(chartHeightTb);

        if(items.length > 0)
        {
            Ext4.create('Ext.form.Panel', {
                renderTo: Ext.get('peakAreasFormDiv'),
                // resizable: true,
                border: false,
                buttonAlign: 'center',
                items: items,
                buttons: [updateBtn]
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

<table>
    <tbody>
        <tr>
            <td>
                <div id="peakAreasGraphImg"></div>
            </td>
            <td valign="top">
                <table>
                    <tbody>
                        <tr>
                            <td>
                                <div id="peakAreasFormDiv"></div>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </td>
        </tr>
    </tbody>
</table>

