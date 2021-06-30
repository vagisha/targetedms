<%
/*
 * Copyright (c) 2017-2019 LabKey Corporation
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
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.targetedms.view.FiguresOfMeritView" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.targetedms.TargetedMSRun" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("targetedms/css/FiguresOfMerit.css");
        dependencies.add("internal/jQuery");
    }
%>
<%
    JspView<FiguresOfMeritView.MoleculeInfo> me = (JspView<FiguresOfMeritView.MoleculeInfo>) HttpView.currentView();
    FiguresOfMeritView.MoleculeInfo bean = me.getModelBean();
    TargetedMSRun run = bean.getRun();

    var lodCalculation = switch (bean.getLODCalculation()) {
            case "blank_plus_2_sd" -> "Blank plus 2 * SD";
            case "blank_plus_3_sd" -> "Blank plus 3 * SD";
            default -> "None";
        };
%>

<table class="lk-fields-table">
    <% if (!bean.getMinimize()) { %>
        <tr>
            <td class="labkey-form-label">File</td>
            <td>
                <a href="<%= h(TargetedMSController.getShowRunURL(run.getContainer(), run.getRunId()))%>"><%= h(run.getDescription()) %></a> &nbsp;&nbsp;
                <% if (run.getFileName() != null) { TargetedMSController.createDownloadMenu(run).render(out); } %>
            </td>
        </tr>
        <tr>
            <td class="labkey-form-label"><%= h(bean.getPeptideName() != null ? "Peptide" : "Molecule") %></td>
            <td><% if (bean.getPeptideName() != null) { %>
                    <a href="<%= h(new ActionURL(TargetedMSController.ShowPeptideAction.class, getContainer()).addParameter("id", bean.getGeneralMoleculeId())) %>"><%= h(bean.getPeptideName()) %></a>
                <% } else { %>
                    <a href="<%= h(new ActionURL(TargetedMSController.ShowMoleculeAction.class, getContainer()).addParameter("id", bean.getGeneralMoleculeId())) %>"><%= h(bean.getMoleculeName()) %></a>
                <% } %>
            </td>
        </tr>
        <tr>
            <td class="labkey-form-label">Samples</td>
            <td>
                <div id="fom-sampleList"></div>
            </td>
        </tr>
    <% } %>
    <tr>
        <td class="labkey-form-label">Lower Limit of Detection</td>
        <td id="llod-value">...</td>
    </tr>
    <tr>
        <td class="labkey-form-label">Lower Limit of Quantitation</td>
        <td id="lloq-stat">...</td>
    </tr>
    <tr>
        <td class="labkey-form-label">Upper Limit of Quantitation</td>
        <td id="uloq-stat">...</td>
    </tr>
    <tr>
        <td class="labkey-form-label">LOQ Bias Limit</td>
        <td id="bias-limit"><%= h(bean.getMaxLOQBias()) %>%</td>
    </tr>
    <tr>
        <td class="labkey-form-label">LOQ CV Limit</td>
        <td id="cv-limit"><%= h(bean.getMaxLOQCV() == null ? "N/A" : (bean.getMaxLOQCV() + "%")) %></td>
    </tr>
    <tr>
        <td class="labkey-form-label">LOD Calculation</td>
        <td id="lod-calc"><%= h(lodCalculation) %></td>
    </tr>
    <tr>
        <td class="labkey-form-label"></td>
        <td>
            <% if (!bean.getMinimize()) { %>
                <div class="export-icon" data-toggle="tooltip" title="Export to Excel">
                    <a id="targetedms-fom-export" href="javascript:exportExcel()"><i class="fa fa-file-excel-o"></i> Export to Excel</a>
                </div>
            <% } else { %>
                <%= link("Show Details", new ActionURL(TargetedMSController.ShowFiguresOfMeritAction.class, getContainer()).addParameter("GeneralMoleculeId", bean.getGeneralMoleculeId()))%>
            <% } %>
            <span id="fom-loading">Loading...<i class="fa fa-spinner fa-pulse"></i></span>
        </td>
    </tr>
</table>

<div class="container-fluid targetedms-fom" <%= unsafe(bean.getMinimize() ? "style=\"display: none;\" " : "") %>>
    <br>

    <div class="container-fluid targetedms-fom">
        <div class="row">
            <labkey:panel title="Concentrations in Standards">
                <table id="fom-table-standard" class="table table-responsive fom-table">
                    <thead id="standard-header"/>
                    <tbody id="standard-body"/>
                </table>
            </labkey:panel>
            <labkey:panel title="Concentrations in Quality Control">
                <table id="fom-table-qc" class="table table-responsive fom-table">
                    <thead id="qc-header"/>
                    <tbody id="qc-body"/>
                </table>
            </labkey:panel>
            <labkey:panel title="Blanks">
                <table id="fom-table-blank" class="table table-responsive fom-table blanks">
                    <thead id="blank-header"/>
                    <tbody id="blank-body"/>
                </table>
            </labkey:panel>
        </div>
    </div>
</div>

<script type="text/javascript">

    +function($) {
        this.$ = $;

        this.moleculeId = <%=bean.getGeneralMoleculeId()%>;
        this.moleculeName = <%=q(bean.getMoleculeName())%>;
        this.peptideName = <%=q(bean.getPeptideName())%>;
        this.fileName = <%=q(bean.getFileName())%>;
        this.sampleFiles = <%=q(bean.getSampleFiles())%>;
        this.runId = <%=bean.getRunId()%>;
        this.biasLimit = <%=bean.getMaxLOQBias()%>;  // percent
        this.cvLimit = <%=text(bean.getMaxLOQCV() == null ? "null" : Double.toString(bean.getMaxLOQCV()))%>;  // percent, can be null, in which case CV is ignored as a criteria
        this.lodCalculation = <%=q(bean.getLODCalculation())%>;  // 'none', etc
        this.xlsExport = [];
        this.title = "Molecule ID: " + this.moleculeId;

        if (this.peptideName !== null) {
            this.title = "Peptide: " + this.peptideName;
        }
        else if (this.moleculeName !== null) {
            this.title = "Molecule: " + this.moleculeName;
        }

        this.exportExcel = function() {
            var data = [];
            data = data.concat(this.xlsExport);
            data = data.concat(this['standardxlsExport']);
            data = data.concat(this['blankxlsExport']);
            data = data.concat(this['qcxlsExport']);

            LABKEY.Utils.convertToExcel({
                fileName : this.fileName.split(".")[0] + '_' + this.title + '.xlsx',
                sheets:
                        [
                            {
                                name: 'Sheet1',
                                data: data
                            }
                        ]
            });
        };

        this.expandSampleList = function () {
            $('.hiddenSample').css('display', '');
            $('#sampleExpando').css('display', 'none')
        };

        var getSampleFilesHtml = function (samples) {
            var html = '';
            var displayLimit = 3;
            html += samples.reduce(function(list, file, index) {
                var extra = index >= displayLimit ? ' class="hiddenSample" style="display: none"' : '';
                if (index === displayLimit) {
                    list += '<div onClick="expandSampleList();" id="sampleExpando">and ' + (samples.length - displayLimit) + ' more <i class="fa fa-caret-down" ></i></div>';
                }
                return list + '<div' + extra + '>' + LABKEY.Utils.encodeHtml(file) + '</div>';
                }, "");

            return html;
        };

        var standardHeader = function (hdrs, sampleType) {
            hdrs.expHdrs.push("");
            hdrs.hdr += '<tr><td>Expected Concentration</td>';
            var units = '';

            if (this.Units)
                units = ' (' + LABKEY.Utils.encodeHtml(this.Units) + ')';

            var css, shade = true;
            this[sampleType + 'hdrLabels'].forEach(function (label) {
                if (shade) {
                    css = 'fom-number shaded';
                    shade = false;
                }
                else {
                    css = 'fom-number';
                    shade = true;
                }

                hdrs.hdr += '<td class=\'' + css + ' left\'>' + LABKEY.Utils.encodeHtml(label) + LABKEY.Utils.encodeHtml(units) + '</td>' + '<td class=\'' + css + ' right\'>Bias (%)</td>';
                hdrs.expHdrs.push(label + units);
                hdrs.expHdrs.push("Bias (%)");
            }, this);
            hdrs.hdr += '</tr>';

            return hdrs;
        };

        var displayHeader = function(sampleType) {
            var samples = this.sampleFiles.split(", ");
            $('#fom-sampleList').html(getSampleFilesHtml(samples));

            // Add title to export
            if (this.xlsExport.length < 1) {
                this.xlsExport.push([this.title]);
                this.xlsExport.push(["Skyline File: " + this.fileName]);
                this.xlsExport.push(["Sample(s): " + this.sampleFiles]);
            }

            if (!this[sampleType + 'xlsExport'])
                this[sampleType + 'xlsExport'] = [];

            var title2 = "Concentrations";
            if(sampleType === 'qc') {
                title2 = "Concentrations in Quality Control";
            }
            else if(sampleType === 'standard') {
                title2 = "Concentrations in Standards"
            }
            else if(sampleType === 'blank') {
                title2 = "Concentrations in Blanks"
            }

            this[sampleType + 'xlsExport'].push([]);
            this[sampleType + 'xlsExport'].push([title2]);

            // Create table headers
            var hdrs = {
                expHdrs: [],
                hdr: ""
            };
            if (this[sampleType + 'hdrLabels'] === null || this[sampleType + 'hdrLabels'].length < 1) {
                $('#' + sampleType + '-header').html("No data of this type");
                hdrs.expHdrs.push("No data of this type");
                hdrs.hdr += "No data of this type";
            }
            else {
                if (sampleType !== 'blank') {
                    hdrs = standardHeader(hdrs, sampleType);
                }
            }

            $('#' + sampleType + '-header').html(hdrs.hdr);
            this[sampleType + 'xlsExport'].push(hdrs.expHdrs);
        };

        var displayBody = function(sampleType) {
            if (this[sampleType + 'hdrLabels'] != null && this[sampleType + 'hdrLabels'].length > 0) {
                var html = getRawDataHtml(sampleType);
                html += getSummaryHtml(sampleType);

                $('#' + sampleType + '-body').html(html);
            }
        };

        var getSummaryHtml = function(sampleType) {

            var stats = ["n", "Mean", "StdDev", "CV", "Bias"];
            var units = "";
            var exportRow, summaryValue, html = "";
            var blanks = sampleType === 'blank';

            stats.forEach(function(stat) {
                if (stat === "CV" || stat === "Bias") {
                    units = "(%)";
                }
                else {
                    units = "";
                }

                if (blanks && stat === "Bias") {
                    return;
                }

                html += '<tr><td class="fom-label">' + stat + units + '</td>';
                exportRow = [stat];
                var data, css, shade = true;
                this[sampleType + 'hdrLabels'].forEach(function(col) {
                    if (shade) {
                        css = 'fom-number shaded';
                        shade = false;
                    }
                    else {
                        css = 'fom-number';
                        shade = true;
                    }

                    if (stat === "n") {
                        data = this[sampleType + 'rawData'][col];
                        var n = 0;
                        data.forEach(function(datum) {
                            if (datum.exclude !== true) {
                                n++;
                            }
                        }, this);
                        html += '<td class=\'' + css + ' left\'>' + n + '</td>';
                        exportRow.push(n);
                    }
                    else {
                        // If all excluded then no summary data
                        if (this[sampleType + 'summaryData'][col] && this[sampleType + 'summaryData'][col][stat]) {
                            summaryValue = this[sampleType + 'summaryData'][col][stat];
                        }
                        else {
                            summaryValue = "";
                        }

                        if (summaryValue === "") {
                            summaryValue = "NA";
                        }
                        html += '<td class=\'' + css + ' left\'>' + summaryValue + '</td>';

                        if (summaryValue === "NA") {
                            exportRow.push(summaryValue);
                        }
                        else {
                            exportRow.push(Number(summaryValue));
                        }
                    }

                    if (!blanks) {
                        html += '<td class=\'' + css + ' right\'></td>';
                        exportRow.push("");
                    }

                }, this);
                html += '</tr>';
                this[sampleType + 'xlsExport'].push(exportRow);
            }, this);

            return html;
        };

        var getRawDataHtml = function(sampleType) {
            var blanks = sampleType === 'blank';
            var index = 0;
            var found;
            var html = "", exportRow;
            var first = true;
            do {
                found = false;

                if (first) {
                    html += "<tr><td class='fom-label'>Calculated Concentrations</td>";
                    first = false;
                }
                else {
                    html += "<tr><td></td>";
                }

                exportRow = [""];

                var data, css, shade = true;
                this[sampleType + 'hdrLabels'].forEach(function (label) {
                    if (shade) {
                        css = 'fom-number shaded';
                        shade = false;
                    }
                    else {
                        css = 'fom-number';
                        shade = true;
                    }
                    data = this[sampleType + 'rawData'][label][index];
                    if (data) {
                        found = true;
                        html += '<td class=\'' + css + ' left\'>' + (data.exclude === true?'<s>':'') + data.value
                                + (data.exclude === true?'</s>':'') + '</td>';
                        if (!blanks)
                                html += '<td class=\'' + css + ' right\'>' + (data.exclude === true?'<s>':'') + data.bias
                                        + (data.exclude === true?'</s>':'') + '</td>';

                        var exportVal;
                        var exportBias;
                        if (data.exclude === true) {
                            exportVal = Number(data.value) + ' excluded';
                            exportBias = Number(data.bias) + ' excluded';
                        }
                        else {
                            exportVal = Number(data.value);
                            exportBias = Number(data.bias);
                        }
                        exportRow.push(exportVal);
                        if (!blanks)
                            exportRow.push(exportBias);
                    }
                    else {
                        html += '<td class=\'' + css + ' left\'>'
                        if (!blanks)
                            html += '</td><td class=\'' + css + ' right\'></td>';

                        exportRow.push("");
                        if (!blanks)
                            exportRow.push("");
                    }
                }, this);

                html += "</tr>";
                index++;
                this[sampleType + 'xlsExport'].push(exportRow);

            } while (found);

            return html;
        };

        // Format to have at least one decimal place, unless the value has more
        var formatConcentration = function(conc) {
            return conc.toFixed(Math.max(1, (conc.toString().split('.')[1] || []).length));
        };

        var parseRawData = function(data, sampleType) {

            this[sampleType + 'rawData'] = {};
            this[sampleType + 'hdrLabels'] = [];

            // Process data to collapse rows
            data.rows.forEach(function (row) {
                if (row.SampleType !== sampleType)
                    return;

                var analyteConc = row["AnalyteConcentration"];
                if (analyteConc != null) {
                    analyteConc = formatConcentration(analyteConc);
                }

                if (analyteConc != null || row.SampleType === "blank") {
                    if (!this[sampleType + 'rawData'][analyteConc]) {
                        this[sampleType + 'rawData'][analyteConc] = [];
                        this[sampleType + 'hdrLabels'].push(analyteConc);
                    }

                    this[sampleType + 'rawData'][analyteConc].push({
                        'value': row['ReplicateConcentration'].toFixed(2),
                        'bias': row['Bias'] ? row['Bias'].toFixed(2) : "NA",
                        'exclude': row['ExcludeFromCalibration'] === true
                    });
                }
            }, this);

            // Sort columns
            this[sampleType + 'hdrLabels'].sort(function(a, b){return a-b;});
        };

        var parseSummaryData = function (data, sampleType) {

            this[sampleType + 'summaryData'] = {};

            // Process data to collapse rows
            data.rows.forEach(function (row) {
                var analyteConc = row["AnalyteConcentration"];
                if (analyteConc != null) {
                    analyteConc = formatConcentration(analyteConc);
                }

                if (analyteConc != null || row.SampleType === "blank") {
                    if (!this[sampleType + 'summaryData'][analyteConc]) {
                        this[sampleType + 'summaryData'][analyteConc] = {};
                    }

                    this[sampleType + 'summaryData'][analyteConc]['Mean'] = (row['Mean'] ? row['Mean'].toFixed(2) : "");
                    this[sampleType + 'summaryData'][analyteConc]['Bias'] = (row['Bias'] ? row['Bias'].toFixed(2) : "");
                    this[sampleType + 'summaryData'][analyteConc]['CV'] = (row['CV'] ? row['CV'].toFixed(2) : "");
                    this[sampleType + 'summaryData'][analyteConc]['StdDev'] = (row['StdDev'] ? row['StdDev'].toFixed(2) : "");
                }
            }, this);
        };

        var handleData = function () {

            displayHeader('blank');
            displayHeader('qc');
            displayHeader('standard');

            displayBody('blank');
            displayBody('qc');
            displayBody('standard');

            createLoqStats();
            createLodStats();

            LABKEY.Utils.signalWebDriverTest('targetedms-fom-loaded');

            $('#fom-loading').hide();
        };

        var createFomTables = function () {
            var multi = new LABKEY.MultiRequest();
            var filter = [LABKEY.Filter.create('RunId', this.runId),
                LABKEY.Filter.create('MoleculeId', this.moleculeId)
            ];

            multi.add(LABKEY.Query.selectRows, {
                schemaName: 'targetedms',
                queryName: 'FiguresOfMerit',
                filterArray: filter,
                scope: this,
                success: function(data) {
                    parseRawData(data, 'blank');
                    parseRawData(data, 'qc');
                    parseRawData(data, 'standard');
                },
                failure: function (response) {
                    LABKEY.Utils.alert('Error', response.exception);
                }
            });

            multi.add(LABKEY.Query.selectRows, {
                schemaName: 'targetedms',
                queryName: 'FiguresOfMeritSummary',
                filterArray: filter,
                scope: this,
                success: function(data) {
                    parseSummaryData(data, 'standard');
                    parseSummaryData(data, 'qc');
                    parseSummaryData(data, 'blank');
                },
                failure: function (response) {
                    LABKEY.Utils.alert('Error', response.exception);
                }
            });

            multi.send(function() {
                handleData();
            }, this);
        };

        var createLoqStats = function() {
            var loq = 'NA', uloq = 'NA', bias, cv, label;
            var hdrs = this['standardhdrLabels'].reverse();

            for (var i=0; i<hdrs.length; i++) {
                label = hdrs[i];

                // All excluded data points
                if (!this['standardsummaryData'][label]) {
                    continue;
                }
                bias = Math.abs(Number(this['standardsummaryData'][label]["Bias"]));
                cv = Math.abs(Number(this['standardsummaryData'][label]["CV"]));
                if (bias <= this.biasLimit && (this.cvLimit === null || cv <= this.cvLimit)) {
                    if (uloq === 'NA') {
                        uloq = label;
                    }
                    loq = label;
                }
                else if (uloq !== 'NA') {
                    break;
                }
            }

            var units = this.Units ? this.Units : '';
            $('#lloq-stat').html(loq + ' ' + LABKEY.Utils.encodeHtml(units));
            $('#uloq-stat').html(uloq + ' ' + LABKEY.Utils.encodeHtml(units));

            this['standardxlsExport'].push([]);
            this['standardxlsExport'].push(['Limit of Quantitation']);
            this['standardxlsExport'].push(['Lower: ' + loq + ' ' + units]);
            this['standardxlsExport'].push(['Upper: ' + uloq + ' ' + units]);
            this['standardxlsExport'].push(['Bias Limit: ' + this.biasLimit + '%']);
            this['standardxlsExport'].push(['CV Limit: ' + (this.cvLimit ? (this.cvLimit + '%') : 'N/A')]);
        };

        var createLodStats = function () {
            var lodValue = "NA";

            if (this.lodCalculation != "none") {
                var mean = "NA", stddev = 0;
                if (this['blanksummaryData'][null] && this['blanksummaryData'][null]["Mean"]) {
                    mean = Number(this['blanksummaryData'][null]["Mean"]);
                }

                if (this['blanksummaryData'][null] && this['blanksummaryData'][null]["StdDev"]) {
                    stddev = Number(this['blanksummaryData'][null]["StdDev"]);
                }

                if (mean !== "NA") {
                    if (this.lodCalculation === "blank_plus_2_sd") {
                        lodValue = (mean + 2 * stddev).toFixed(2);
                    }
                    else if (this.lodCalculation === "blank_plus_3_sd") {
                        lodValue = (mean + 3 * stddev).toFixed(2);
                    }
                }
            }

            $('#llod-value').html(lodValue);

            this['blankxlsExport'].push([]);
            this['blankxlsExport'].push(["Limit of Detection"]);
            this['blankxlsExport'].push(['Lower: ' + lodValue]);
            this['blankxlsExport'].push(['Calculation: ' + <%= q(lodCalculation)%>]);
        };

        createFomTables();

    }(jQuery);


</script>


