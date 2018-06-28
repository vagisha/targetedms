

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
    <h3 id="fom-title1"></h3>
    <h4 id="fom-title2"></h4>
    <h4 id="fom-title3"></h4><div id="fom-title3-value" class="collapse"></div>
    <br>
    <div class="container-fluid targetedms-fom">
        <div class="row">
            <labkey:panel title="Summary">
                <table id="fom-table-summary" class="table table-responsive fom-table summary">
                    <thead id="summary-header"><tr><td>Limit of Quantitation</td><td class="shaded">Limit of Detection</td></tr></thead>
                    <tbody id="summary-body">
                    <tr><td id="loq-stat"></td><td class="shaded" id="lod-value"></td></tr>
                        <tr><td id="uloq-stat"></td><td class="shaded" id="lod-calc"></td></tr>
                        <tr><td id="bias-limit"></td><td class="shaded"></td></tr>
                        <tr><td id="cv-limit"></td><td class="shaded"></td></tr>
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

        if (this.peptideName !== null) {
            this.title = "Peptide: " + this.peptideName;
        }
        else if (this.moleculeName !== null) {
            this.title = "Molecule: " + this.moleculeName;
        }

        this.exportExcel = function() {
            LABKEY.Utils.convertToExcel({
                fileName : this.fileName.split(".")[0] + '_' + this.title + '.xlsx',
                sheets:
                        [
                            {
                                name: 'Sheet1',
                                data: this.xlsExport
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

        var blanksHeader = function(hdrs) {
            return hdrs;
        };

        var standardHeader = function (hdrs) {
            hdrs.expHdrs.push("");
            hdrs.hdr += '<tr><td>Expected Concentration</td>';
            var units = '';

            if (this.Units)
                units = ' (' + LABKEY.Utils.encodeHtml(this.Units) + ')';

            var css, shade = true;
            this.hdrLabels.forEach(function (label) {
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

        var displayHeader = function(blanks) {
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

            var title2 = "Concentrations";
            if(this.sampleType === 'qc') {
                title2 = "Concentrations in Quality Control";
            }
            else if(this.sampleType === 'standard') {
                title2 = "Concentrations in Standards"
            }
            else if(this.sampleType === 'blank') {
                title2 = "Concentrations in Blanks"
            }

            this.xlsExport.push([]);
            this.xlsExport.push([title2]);

            // Create table headers
            var hdrs = {
                expHdrs: [],
                hdr: ""
            };
            if (this.hdrLabels === null || this.hdrLabels.length < 1) {
                $('#' + this.sampleType + '-header').html("No data of this type");
                hdrs.expHdrs.push("No data of this type");
                hdrs.hdr += "No data of this type";
            }
            else {
                if (!blanks) {
                    hdrs = standardHeader(hdrs);
                }
                else {
                    hdrs = blanksHeader(hdrs);
                }
            }

            $('#' + this.sampleType + '-header').html(hdrs.hdr);
            this.xlsExport.push(hdrs.expHdrs);
        };

        var displayBody = function(blanks) {
            if (this.hdrLabels != null && this.hdrLabels.length > 0) {
                var html = getRawDataHtml(blanks);
                html += getSummaryHtml(blanks);

                $('#' + this.sampleType + '-body').html(html);
            }
        };

        var getSummaryHtml = function(blanks) {

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
                this.hdrLabels.forEach(function(col) {
                    if (shade) {
                        css = 'fom-number shaded';
                        shade = false;
                    }
                    else {
                        css = 'fom-number';
                        shade = true;
                    }

                    if (stat === "n") {
                        data = this.rawData[col];
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
                        if (this.summaryData[col] && this.summaryData[col][stat]) {
                            summaryValue = this.summaryData[col][stat];
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
                this.xlsExport.push(exportRow);
            }, this);

            return html;
        };

        var getRawDataHtml = function(blanks) {
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
                this.hdrLabels.forEach(function (label) {
                    if (shade) {
                        css = 'fom-number shaded';
                        shade = false;
                    }
                    else {
                        css = 'fom-number';
                        shade = true;
                    }
                    data = this.rawData[label][index];
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
                this.xlsExport.push(exportRow);

            } while (found);

            return html;
        };

        var parseRawData = function(data) {

            // No data
            if (data.rows.length < 1)
                return;

            // Process data to collapse rows
            data.rows.forEach(function (row) {
                var colName, replicate;
                for (var col in row) {
                    if (row.hasOwnProperty(col)) {
                        colName = col.split('::')[0];
                        replicate = col.split('::')[1];
                        if (replicate != null && (colName !== 'NULL' || row.SampleType === "blank")) {
                            if (row[col] != null) {
                                if (!this.rawData[colName]) {
                                    this.rawData[colName] = [];
                                    this.hdrLabels.push(colName);
                                }

                                this.rawData[colName].push({
                                    'value': row[col].toFixed(2),
                                    'bias': row['Bias']?row['Bias'].toFixed(2):null,
                                    'exclude': row['ExcludeFromCalibration'] ? row['ExcludeFromCalibration'] : 'false'
                                })
                            }
                        }
                    }
                }
            }, this);

            // Sort columns
            this.hdrLabels.sort(function(a, b){return a-b;});
        };

        var parseSummaryData = function (data) {

            // No data
            if (data.rows.length < 1)
                return;

            // Process data to collapse rows
            data.rows.forEach(function (row) {
                var colName, rowName;
                for (var col in row) {
                    if (row.hasOwnProperty(col)) {
                        colName = col.split("::")[0];
                        rowName = col.split("::")[1];
                        if (rowName != null && (colName !== 'NULL' || row.SampleType === "blank")) {
                            if (!this.summaryData[colName]) {
                                this.summaryData[colName] = {};
                            }

                            if (row[col] != null) {
                                this.summaryData[colName][rowName] = row[col].toFixed(2);
                            }
                            else {
                                this.summaryData[colName][rowName] = "";
                            }
                        }
                    }
                }
            }, this);
        };

        var handleData = function (blanks) {

            displayHeader(blanks);
            displayBody(blanks);

            if (this.sampleType === 'standard')
                createLoqStats();

            if (this.sampleType === 'blank')
                createLodStats();

            if (this.callback != null)
                this.callback();
        };

        var createFomTable = function(sampleType, callback) {
            this.rawData = {};
            this.summaryData = {};
            this.hdrLabels = [];

            this.sampleType = sampleType;
            this.callback = callback;
            var multi = new LABKEY.MultiRequest();
            var filter = [LABKEY.Filter.create('RunId', this.runId),
                LABKEY.Filter.create('MoleculeId', this.moleculeId),
                LABKEY.Filter.create('SampleType', sampleType)
            ];

            multi.add(LABKEY.Query.selectRows, {
                schemaName: 'targetedms',
                queryName: 'FiguresOfMeritPivot',
                filterArray: filter,
                scope: this,
                success: parseRawData,
                failure: function (response) {
                    LABKEY.Utils.alert('Error', response.exception);
                }
            });

            multi.add(LABKEY.Query.selectRows, {
                schemaName: 'targetedms',
                queryName: 'FiguresOfMeritSummary',
                filterArray: filter,
                scope: this,
                success: parseSummaryData,
                failure: function (response) {
                    LABKEY.Utils.alert('Error', response.exception);
                }
            });

            multi.send(function() {
                handleData(sampleType === "blank");
            }, this);
        };

        var createLoqStats = function() {
            var loq = 'NA', uloq = 'NA', bias, cv, label;
            var hdrs = this.hdrLabels.reverse();

            for (var i=0; i<hdrs.length; i++) {
                label = hdrs[i];

                // All excluded data points
                if (!this.summaryData[label]) {
                    continue;
                }
                bias = Math.abs(Number(this.summaryData[label]["Bias"]));
                cv = Math.abs(Number(this.summaryData[label]["CV"]));
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

            this.xlsExport.push([]);
            this.xlsExport.push(['Bias Limit: ' + this.biasLimit + '%']);
            this.xlsExport.push(['CV Limit: ' + (this.cvLimit ? (this.cvLimit + '%') : 'N/A')]);
            this.xlsExport.push(['LOQ: ' + loq + ' ' + units]);
            this.xlsExport.push(['ULOQ: ' + uloq + ' ' + units]);
        };

        var createLodStats = function () {
            var lodValue = "NA";

            if (this.lodCalculation != "none") {
                var mean = "NA", stddev = 0;
                if (this.summaryData["NULL"] && this.summaryData["NULL"]["Mean"]) {
                    mean = Number(this.summaryData["NULL"]["Mean"]);
                }

                if (this.summaryData["NULL"] && this.summaryData["NULL"]["StdDev"]) {
                    stddev = Number(this.summaryData["NULL"]["StdDev"]);
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

            this.xlsExport.push([]);
            this.xlsExport.push(['LOD: ' + lodValue]);
            this.xlsExport.push(['Calculation: ' + calculation]);
        };

        var afterLoad = function() {
            LABKEY.Utils.signalWebDriverTest('targetedms-fom-loaded');
        };

        var createQcFomTable = function() {
            createFomTable('qc', afterLoad);
        };

        var createStandardFomTable = function(callback) {
            createFomTable('standard', callback);
        };

        var createBlankFomTable = function(callback) {
            createFomTable('blank', callback);
        };

        createStandardFomTable(function() {createBlankFomTable(createQcFomTable)});

    }(jQuery);


</script>


