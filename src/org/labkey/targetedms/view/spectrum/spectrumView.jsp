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
<%@ page import="org.labkey.api.util.PageFlowUtil"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView"%>
<%@ page import="org.labkey.targetedms.view.spectrum.LibrarySpectrumMatch" %>
<%@ page import="org.labkey.targetedms.parser.blib.BlibSpectrum" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<%!
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

<%

if (bean.getSpectrum() != null)
{

%>
<div style="margin:10px;">
    <%
        BlibSpectrum spectrum = bean.getSpectrum();
        String sourceFileSpanId = "source_file_" + bean.getLorikeetId();
        String rtSpanId = "rt_" + bean.getLorikeetId();
        String refSpecLabelId = "ref_spec_label_" + bean.getLorikeetId();
        String specSelectorId = "spectrum_selector_" + bean.getLorikeetId();
    %>
    <span style="font-weight:bold">Library: </span><%=h(bean.getLibrary().getName())%>
    <% if(spectrum.getSourceFile() != null) { %>
        </br>
        <span style="font-weight:bold">File: </span><span id="<%=sourceFileSpanId%>"><%=h(spectrum.getSourceFileName())%></span>
        </br>
        <span style="font-weight:bold">Retention time: </span><span id="<%=rtSpanId%>"><%=spectrum.getRetentionTimeF2()%></span>
        <% if(spectrum.getRedundantSpectrumList().size() > 1) { %>
            <!-- When we load initially we are showing the reference spectrum) -->
            <span id="<%=refSpecLabelId%>" style="color:mediumblue; font-size:small;" >(Reference spectrum)</span>
        <% } %>
        </br></br>

        <div id="<%=specSelectorId%>"></div>
    <% } %>

</div>

<!-- PLACE HOLDER DIV FOR THE SPECTRUM -->
<div id="<%=bean.getLorikeetId()%>"></div>
<script type="text/javascript">

Ext4.onReady(function () {

    /* render the spectrum with the given options */
    $("#<%=bean.getLorikeetId()%>").specview({sequence: <%= PageFlowUtil.jsString(bean.getPeptide()) %>,
        staticMods: <%= bean.getStructuralModifications()%>,
        variableMods: <%= bean.getVariableModifications()%>,
        maxNeutralLossCount: <%= bean.getMaxNeutralLosses()%>,
        width: 600,
        charge: <%= bean.getCharge()%>,
        peaks: <%= bean.getPeaks()%>,// peaks in the scan: [m/z, intensity] pairs.
        extraPeakSeries: [],
        peakDetect: false
    });

    var spectrumCount = <%=bean.getSpectrum().getRedundantSpectrumList().size()%>;

    if(spectrumCount > 1)
    {
        var spectrumSource = Ext4.create('Ext.data.Store', {
            fields: ['redundantSpectrumId', 'fileName', 'retentionTime', 'isReference', 'display'],
            data:   [
                <%for(BlibSpectrum.RedundantSpectrum rSpec: bean.getSpectrum().getRedundantSpectrumList()) {%>
                {
                    "redundantSpectrumId":<%=rSpec.getRedundantRefSpectrumId()%>,
                    "fileName": "<%=q(rSpec.getSourceFileName())%>",
                    "retentionTime":<%=rSpec.getRetentionTimeF2()%>,
                    "isReference":<%=rSpec.isBestSpectrum()%>,
                    "display":<%=q(rSpec.getSourceFileName() + " (" + rSpec.getRetentionTimeF2() + ")")%>
                },
                <% } %>
            ]
        });

        Ext4.create('Ext.form.field.ComboBox', {
            renderTo:  Ext4.get('<%=specSelectorId%>'),
            fieldLabel: 'Spectrum:',
            labelStyle: 'font-weight: bold',
            editable: false,
            emptyText: "",
            store: spectrumSource ,
            queryMode: 'local',
            displayField: 'display',
            valueField: 'redundantSpectrumId',
            forceSelection: 'true',
            allowBlank: 'false',
            value: spectrumSource.getAt(0),
            width: 500,
            listeners:{
                select: function(combo, records, index){

                    var record = records[0];
                    console.log(record.get('fileName') + " " + record.get('isReference') + " "
                              + record.get('retentionTime') + " "  + record.get('redundantSpectrumId'));

                    Ext4.Ajax.request({
                        url: LABKEY.ActionURL.buildURL('targetedms', 'librarySpectrumData'),
                        method: 'GET',
                        params: {
                            libraryName: <%=q(bean.getLibrary().getName())%>,
                            redundantRefSpectrumId: record.get('redundantSpectrumId'),
                            precursorId: <%=bean.getPrecursorId()%>
                        },
                        callback: function(options, success, response) {

                            // console.log(response.responseText);

                            var jsonData = Ext4.JSON.decode(response.responseText);

                            if(!success || response.status != 200)
                            {
                                if(jsonData.exception)
                                {
                                    onError(jsonData.exception);
                                    return;
                                }
                                else
                                {
                                    onError("An unknown error occurred");
                                }
                                return;
                            }

                            if(jsonData.error)
                            {
                                onError(jsonData.error);
                                return;
                            }

                            /* render the spectrum with the given options */
                            var spectrum = jsonData.spectrum;

                            // Clear the existing spectrum
                            $("#<%=bean.getLorikeetId()%>").empty();

                            $("#<%=bean.getLorikeetId()%>").specview({sequence: spectrum.sequence,
                                staticMods: Ext4.JSON.decode(spectrum.staticMods),
                                variableMods: Ext4.JSON.decode(spectrum.variableMods),
                                maxNeutralLossCount: spectrum.maxNeutralLossCount,
                                width: 600,
                                charge: spectrum.charge,
                                peaks: Ext4.JSON.decode(spectrum.peaks), // peaks in the scan: [m/z, intensity] pairs.
                                extraPeakSeries: [],
                                peakDetect: false
                            });

                            Ext4.fly("<%=sourceFileSpanId%>").update(spectrum.fileName);
                            Ext4.fly("<%=rtSpanId%>").update(spectrum.retentionTime);

                            if(record.get('isReference'))
                            {
                                Ext4.fly("<%=refSpecLabelId%>").update("(Reference spectrum)");
                            }
                            else
                            {
                                Ext4.fly("<%=refSpecLabelId%>").update("");
                            }
                        }
                    });
                }
            }
        });
    }
});

    function onError(message)
    {
        Ext4.fly("<%=refSpecLabelId%>").update("");
        Ext4.fly("<%=refSpecLabelId%>").update("");
        $("#<%=bean.getLorikeetId()%>").html('<div style="margin:20px;color:red;">' + message + '</div>');
    }

</script>
<% } else { %> Spectrum information unavailable.<% }%>
