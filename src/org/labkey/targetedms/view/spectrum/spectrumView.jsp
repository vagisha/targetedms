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
<%@ page import="org.labkey.api.util.PageFlowUtil"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView"%>
<%@ page import="org.labkey.targetedms.view.spectrum.LibrarySpectrumMatch" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.targetedms.parser.PeptideSettings" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("Ext4");
        dependencies.add("MS2/lorikeet");
    }
%>

<%
    JspView<LibrarySpectrumMatch> me = (JspView<LibrarySpectrumMatch>) HttpView.currentView();
    LibrarySpectrumMatch bean = me.getModelBean();
%>

<%if (bean.getSpectrum() != null) {%>

<div style="margin-bottom:10px;">
    <%
        String lorikeetOptionsId = bean.getLorikeetId() + "_options";
    %>
</div>

<!-- PLACE HOLDER DIV FOR THE SPECTRUM -->
<div id="<%=lorikeetOptionsId%>"></div>
<div id="<%=bean.getLorikeetId()%>"></div>
<script type="text/javascript">

Ext4.onReady(function () {

    /* render the spectrum with the given options */
    $("#<%=bean.getLorikeetId()%>").specview({sequence: <%= PageFlowUtil.jsString(bean.getPeptide()) %>,
        staticMods: <%= bean.getStructuralModifications()%>,
        variableMods: <%= bean.getVariableModifications()%>,
        ntermMod: <%=bean.getNtermModMass()%>,
        ctermMod: <%=bean.getCtermModMass()%>,
        maxNeutralLossCount: <%= bean.getMaxNeutralLosses()%>,
        width: 600,
        charge: <%= bean.getCharge()%>,
        peaks: <%= bean.getPeaks()%>,// peaks in the scan: [m/z, intensity] pairs.
        extraPeakSeries: [],
        peakDetect: false
    });

    var libSelectData = [];
    var libCount = <%=bean.getLibraries().size()%>;
    if(libCount > 0)
    {
        libSelectData =   [<%for(PeptideSettings.SpectrumLibrary lib: bean.getLibraries()) {%>
            {
                "libName": <%=q(lib.getName())%>
            },
            <% } %>
        ];
    }
    var libSource = Ext4.create('Ext.data.Store', {
        fields: ['libName'],
        data: libSelectData
    });

    var specSelectData = [];
    var spectrumCount = <%=bean.getSpectrum().getRedundantSpectrumList().size()%>;
    if(spectrumCount > 1)
    {
        specSelectData = <%=bean.getRedundantSpectraList()%>;
    }
    var spectrumSource = Ext4.create('Ext.data.Store', {
        fields: ['redundantSpectrumId', 'fileName', 'retentionTime', 'isReference', 'display'],
        data:  specSelectData
    });

    Ext4.create('LABKEY.targetedms.LorikeetOptions', {
        renderTo: <%=q(lorikeetOptionsId)%>,
        lorikeetId: <%=q(bean.getLorikeetId())%>,
        sourceFileName: <%=q(bean.getSpectrum().getSourceFileName())%>,
        retentionTime: <%=q(bean.getSpectrum().getRetentionTimeF2())%>,
        isReference: <%=bean.getSpectrum().getRedundantSpectrumList().size() > 1%>,
        precursorId: <%=bean.getPrecursorId()%>,
        <%if(bean.getLibraries().size() > 1) {%>
        libraryStore: libSource,
        <%}%>
        libraryName: <%=q(bean.getLibrary().getName())%>,
        spectrumStore: spectrumSource
    });

});

Ext4.define('LABKEY.targetedms.LorikeetOptions', {

    extend: 'Ext.panel.Panel',
    layout: {
        type: 'table',
        columns: 1
    },
    border: false,
    details_panel_id: 'details_panel_',

    initComponent : function() {
        Ext4.tip.QuickTipManager.init();

        this.details_panel_id = this.details_panel_id + this.lorikeetId;

        this.items = this.getItems();
        this.callParent();
    },
    getItems: function(){
        var items = [];
        var wrapper = this;
        if(!this.sourceFileName)
        {
            return items;
        }

        items.push({
                xtype: 'box',
                itemId: this.details_panel_id,
                html: this.getDetailsHtml(this.sourceFileName, this.retentionTime, this.isReference)
                });

        if(this.libraryStore)
        {
            items.push({
                xtype: 'combobox',
                itemId: 'libSelector',
                hidden: false,
                fieldLabel: 'Library',
                labelStyle: 'font-weight: bold',
                editable: false,
                store: this.libraryStore ,
                queryMode: 'local',
                displayField: 'libName',
                valueField: 'libName',
                forceSelection: 'true',
                allowBlank: 'false',
                value: this.libraryStore.getAt(0),
                width: 500,
                listeners:{
                    select: function(combo, records, index){

                        var record = records[0];
                        // console.log(record.get('libName') + " " + record.get('libId'));

                        Ext4.get(wrapper.lorikeetId).mask("Loading...");

                        Ext4.Ajax.request({
                            url: LABKEY.ActionURL.buildURL('targetedms', 'librarySpectrumData'),
                            method: 'GET',
                            params: {
                                libraryName: record.get('libName'),
                                precursorId: wrapper.precursorId
                            },
                            callback: function(options, success, response) {
                                wrapper.refreshSpectrum(success, response, true, true);
                            }
                        });
                    }
                }
            });
        }
        else if(this.libraryName)
        {
            items.push({
                xtype: 'box',
                html: '<span style="font-weight:bold">Library: </span><span>' + this.libraryName + '</span>'
            });
        }

        if(this.spectrumStore)
        {
            items.push({
                xtype: 'combobox',
                itemId: 'specSelector',
                hidden: this.spectrumStore.getCount() <= 1,
                fieldLabel: 'Spectrum',
                labelStyle: 'font-weight: bold',
                editable: false,
                emptyText: "",
                store: this.spectrumStore ,
                queryMode: 'local',
                displayField: 'display',
                valueField: 'redundantSpectrumId',
                forceSelection: 'true',
                allowBlank: 'false',
                value: this.spectrumStore.getAt(0),
                width: 500,
                listeners:{
                    select: function(combo, records, index){

                        var record = records[0];
                        //console.log(record.get('fileName') + " " + record.get('isReference') + " "
                        //        + record.get('retentionTime') + " "  + record.get('redundantSpectrumId'));

                        var selectedLib = wrapper.libraryName;
                        var libSelector = wrapper.getComponent('libSelector');
                        if(libSelector)
                        {
                            selectedLib = libSelector.getValue();
                        }
                        Ext4.get(wrapper.lorikeetId).mask("Loading...");

                        Ext4.Ajax.request({
                            url: LABKEY.ActionURL.buildURL('targetedms', 'librarySpectrumData'),
                            method: 'GET',
                            params: {
                                libraryName: selectedLib,
                                redundantRefSpectrumId: record.get('redundantSpectrumId'),
                                precursorId: wrapper.precursorId
                            },
                            callback: function(options, success, response) {
                                wrapper.refreshSpectrum(success, response, record.get('isReference'), false);
                            }
                        });
                    }
                }
            });
        }

        return items;
    },
    refreshSpectrum: function (success, response, isReference, refreshSpecSelector)
    {
        Ext4.get(this.lorikeetId).unmask();
        //console.log(response.responseText);

        var jsonData = Ext4.JSON.decode(response.responseText);

         if(!success || response.status != 200)
         {
             if(jsonData.exception)
             {
                 this.onError(jsonData.exception);
                 return;
             }
             else
             {
                 this.onError("An unknown error occurred");
             }
             return;
         }

         if(jsonData.error)
         {
             this.onError(jsonData.error);
             return;
         }

        /* render the spectrum with the given options */
        var spectrum = jsonData.spectrum;
        this.renderSpectrum(spectrum, isReference, refreshSpecSelector);
    },
    renderSpectrum: function (spectrum, isReference, refreshSpecSelector)
    {
        // Clear the existing spectrum
        $('#' + this.lorikeetId).empty();

        $('#' + this.lorikeetId).specview({sequence: spectrum.sequence,
            staticMods: Ext4.JSON.decode(spectrum.staticMods),
            variableMods: Ext4.JSON.decode(spectrum.variableMods),
            maxNeutralLossCount: spectrum.maxNeutralLossCount,
            width: 600,
            charge: spectrum.charge,
            peaks: Ext4.JSON.decode(spectrum.peaks), // peaks in the scan: [m/z, intensity] pairs.
            extraPeakSeries: [],
            peakDetect: false
        });

        if(refreshSpecSelector) {
            var spectrumSelector = this.getComponent('specSelector');
            var spectrumSource = Ext4.create('Ext.data.Store', {
                fields: ['redundantSpectrumId', 'fileName', 'retentionTime', 'isReference', 'display'],
                data: Ext4.JSON.decode(spectrum.redundantSpectra)
            });
            if (spectrumSource.getCount() > 1) {
                spectrumSelector.bindStore(spectrumSource);
                spectrumSelector.select(spectrumSource.first());
                spectrumSelector.show();
            }
            else
            {
                spectrumSelector.hide();// Hide the spectrum selector
                isReference = false; // There is only one spectrum in the redundant lib, and it is the reference spectrum
            }
        }
        this.getComponent(this.details_panel_id).update(this.getDetailsHtml(spectrum.fileName, spectrum.retentionTime, isReference));
    },
    getDetailsHtml: function(fileName, retentionTime, isReference)
    {
        var fileNameHtml = '<span style="font-weight:bold;">File: </span><span>' + Ext4.String.htmlEncode(fileName) + '</span>';
        var rtHtml = '&nbsp;<span> (RT: ' + retentionTime + ')</span>';
        var refHtml = '&nbsp;<span style="color:mediumblue; font-size:small;">' + (isReference ? "Reference spectrum" : "") + '</span>';
        return fileNameHtml + rtHtml + refHtml;
    },
    onError: function (message)
    {
        this.getComponent(this.details_panel_id).getComponent(this.refspec_span_id).update("");
        $('#'+this.lorikeetId).html('<div style="margin:20px;color:red;">' + message + '</div>');
    }
});

</script>
<% } else { %> Spectrum information unavailable.<% }%>
