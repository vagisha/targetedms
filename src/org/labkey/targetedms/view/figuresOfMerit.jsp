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
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
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
    JspView<TargetedMSController.FomForm> me = (JspView<TargetedMSController.FomForm>) HttpView.currentView();
    TargetedMSController.FomForm bean = me.getModelBean();
%>
<div class="container-fluid targetedms-fom">
    <div id="targetedms-fom-export" class="export-icon" data-toggle="tooltip" title="Export to Excel">
        <i class="fa fa-file-excel-o" onclick="exportExcel()"></i>
    </div>
    <span id="fom-loading">Loading...<i class="fa fa-spinner fa-pulse"></i></span>
    <h3 id="fom-title1"></h3>
    <h4 id="fom-title2"></h4>
    <h4 id="fom-title3"></h4><div id="fom-title3-value" class="collapse"></div>
    <br>
    <div class="container-fluid targetedms-fom">
        <div class="row">
            <labkey:panel title="Summary">
                <table id="fom-table-summary" class="table table-responsive fom-table summary">
                    <thead id="summary-header"><tr><td>Limit of Quantitation</td><td class="shaded left">Limit of Detection</td></tr></thead>
                    <tbody id="summary-body">
                    <tr><td id="loq-stat"></td><td class="shaded left" id="lod-value"></td></tr>
                        <tr><td id="uloq-stat"></td><td class="shaded left" id="lod-calc"></td></tr>
                        <tr><td id="bias-limit"></td><td class="shaded left"></td></tr>
                        <tr><td id="cv-limit"></td><td class="shaded left"></td></tr>
                    </tbody>
                </table>
            </labkey:panel>
        </div>
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
        this.sampleListCollapsed = true;
        this.tableCount = 0;

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

        this.toggleSampleList = function () {
            if (this.sampleListCollapsed) {
                expandSampleList();
                this.sampleListCollapsed = false;
            }
            else {
                collapseSampleList();
                this.sampleListCollapsed = true;

            }
        };

        var collapseSampleList = function () {
            $('#fom-title3-icon').attr("class", "fa fa-caret-down");
            $('#fom-title3-value').collapse("hide");
        };

        var expandSampleList = function () {
            $('#fom-title3-icon').attr("class", "fa fa-caret-up");
            $('#fom-title3-value').collapse("show");
        };

        var getSampleFilesHtml = function (samples) {
            var html = "<ul>";
            html += samples.reduce(function(list, file) { return list + "<li>" + file + "</li>"; }, "");
            html += "</ul>";

            return html;
        };

        var getSampleListIcon = function () {
            return '<i id="fom-title3-icon" onClick="toggleSampleList();" class="fa fa-caret-down" ></i>';
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

        var displayHeader = function(blanks, sampleType) {
            $('#fom-title1').html(this.title);
            $('#fom-title2').html("Skyline File: " + this.fileName);

            var samples = this.sampleFiles.split(", ");
            $('#fom-title3').html(samples.length + " Sample" + (samples.length!=1 ? "s " : " ") + getSampleListIcon());
            $('#fom-title3-value').html(getSampleFilesHtml(samples));
            collapseSampleList();

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
                if (!blanks) {
                    hdrs = standardHeader(hdrs, sampleType);
                }
            }

            $('#' + sampleType + '-header').html(hdrs.hdr);
            this[sampleType + 'xlsExport'].push(hdrs.expHdrs);
        };

        var displayBody = function(blanks, sampleType) {
            if (this[sampleType + 'hdrLabels'] != null && this[sampleType + 'hdrLabels'].length > 0) {
                var html = getRawDataHtml(blanks, sampleType);
                html += getSummaryHtml(blanks, sampleType);

                $('#' + sampleType + '-body').html(html);
            }
        };

        var getSummaryHtml = function(blanks, sampleType) {

            var stats = ["n", "Mean", "StdDev", "CV", "Bias"];
            var units = "";
            var exportRow, summaryValue, html = "";

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

        var getRawDataHtml = function(blanks, sampleType) {
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

            // No data
            if (data.rows.length < 1)
                return;

            // Process data to collapse rows
            data.rows.forEach(function (row) {
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

            // No data
            if (data.rows.length < 1)
                return;

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

        var handleData = function (blanks, sampleType) {

            displayHeader(blanks, sampleType);
            displayBody(blanks, sampleType);

            if (sampleType === 'standard')
                createLoqStats(sampleType);

            if (sampleType === 'blank')
                createLodStats(sampleType);

            if (this[sampleType + 'callback'] != null)
                this[sampleType + 'callback']();

            if (--this.tableCount === 0)
                LABKEY.Utils.signalWebDriverTest('targetedms-fom-loaded');

            $('#fom-loading').hide();
        };

        var createFomTable = function(sampleType, callback) {
            this.tableCount++;
            this[sampleType + 'rawData'] = {};
            this[sampleType + 'summaryData'] = {};
            this[sampleType + 'hdrLabels'] = [];

            this[sampleType + 'callback'] = callback;
            var multi = new LABKEY.MultiRequest();
            var filter = [LABKEY.Filter.create('RunId', this.runId),
                LABKEY.Filter.create('MoleculeId', this.moleculeId),
                LABKEY.Filter.create('SampleType', sampleType)
            ];

            multi.add(LABKEY.Query.selectRows, {
                schemaName: 'targetedms',
                queryName: 'FiguresOfMerit',
                filterArray: filter,
                scope: this,
                success: function(data) {
                    parseRawData(data, sampleType);
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
                    parseSummaryData(data, sampleType);
                },
                failure: function (response) {
                    LABKEY.Utils.alert('Error', response.exception);
                }
            });

            multi.send(function() {
                handleData(sampleType === "blank", sampleType);
            }, this);
        };

        var createLoqStats = function(sampleType) {
            var loq = 'NA', uloq = 'NA', bias, cv, label;
            var hdrs = this[sampleType + 'hdrLabels'].reverse();

            for (var i=0; i<hdrs.length; i++) {
                label = hdrs[i];

                // All excluded data points
                if (!this[sampleType + 'summaryData'][label]) {
                    continue;
                }
                bias = Math.abs(Number(this[sampleType + 'summaryData'][label]["Bias"]));
                cv = Math.abs(Number(this[sampleType + 'summaryData'][label]["CV"]));
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
            $('#bias-limit').html('Bias Limit: ' + this.biasLimit + '%');
            $('#cv-limit').html('CV Limit: ' + (this.cvLimit ? (this.cvLimit + '%') : 'N/A'));
            $('#loq-stat').html('Lower: ' + loq + ' ' + LABKEY.Utils.encodeHtml(units));
            $('#uloq-stat').html('Upper: ' + uloq + ' ' + LABKEY.Utils.encodeHtml(units));

            this[sampleType + 'xlsExport'].push([]);
            this[sampleType + 'xlsExport'].push(['Limit of Quantitation']);
            this[sampleType + 'xlsExport'].push(['Lower: ' + loq + ' ' + units]);
            this[sampleType + 'xlsExport'].push(['Upper: ' + uloq + ' ' + units]);
            this[sampleType + 'xlsExport'].push(['Bias Limit: ' + this.biasLimit + '%']);
            this[sampleType + 'xlsExport'].push(['CV Limit: ' + (this.cvLimit ? (this.cvLimit + '%') : 'N/A')]);
        };

        var createLodStats = function (sampleType) {
            var lodValue = "NA";

            if (this.lodCalculation != "none") {
                var mean = "NA", stddev = 0;
                if (this[sampleType + 'summaryData'][null] && this[sampleType + 'summaryData'][null]["Mean"]) {
                    mean = Number(this[sampleType + 'summaryData'][null]["Mean"]);
                }

                if (this[sampleType + 'summaryData'][null] && this[sampleType + 'summaryData'][null]["StdDev"]) {
                    stddev = Number(this[sampleType + 'summaryData'][null]["StdDev"]);
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

            var calculation = "None";
            if (this.lodCalculation === "blank_plus_2_sd") {
                calculation = "Blank plus 2 * SD";
            }
            else if (this.lodCalculation === "blank_plus_3_sd") {
                calculation = "Blank plus 3 * SD";
            }

            $('#lod-value').html('Lower: ' + lodValue);
            $('#lod-calc').html('Calculation: ' + calculation);

            this[sampleType + 'xlsExport'].push([]);
            this[sampleType + 'xlsExport'].push(["Limit of Detection"]);
            this[sampleType + 'xlsExport'].push(['Lower: ' + lodValue]);
            this[sampleType + 'xlsExport'].push(['Calculation: ' + calculation]);
        };

        createFomTable('standard', null);
        createFomTable('blank', null);
        createFomTable('qc', null);

    }(jQuery);


</script>


