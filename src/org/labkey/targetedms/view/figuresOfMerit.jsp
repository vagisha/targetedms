

<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.HttpView" %>
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
            <labkey:panel title="Concentrations in Standards">
                <table id="fom-table-standard" class="table table-responsive fom-table">
                    <thead id="standard-header"/>
                    <tbody id="standard-body"/>
                </table>
                <div id="bias-limit"></div>
                <div id="loq-stat"></div>
                <div id="uloq-stat"></div>
            </labkey:panel>
            <labkey:panel title="Concentrations in Quality Control">
                <table id="fom-table-qc" class="table table-responsive fom-table">
                    <thead id="qc-header"/>
                    <tbody id="qc-body"/>
                </table>
            </labkey:panel>

        </div>
    </div>
    <div id="targetedms-afterload" hidden></div>
</div>

<script type="text/javascript">

    +function($) {
        this.$ = $;

        this.moleculeId = <%=bean.getGeneralMoleculeId()%>;
        this.moleculeName = "<%=bean.getMoleculeName()%>";
        this.peptideName = "<%=bean.getPeptideName()%>";
        this.fileName = "<%=bean.getFileName()%>";
        this.sampleFiles = "<%=bean.getSampleFiles()%>";
        this.runId = <%=bean.getRunId()%>;
        this.biasLimit = 30;  // percent
        this.xlsExport = [];
        this.title = "Molecule ID: " + this.moleculeId;
        this.sampleListCollapsed = true;

        if (this.peptideName !== "null") {
            this.title = "Peptide: " + this.peptideName;
        }
        else if (this.moleculeName !== "null") {
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
        }

        var displayHeader = function() {
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

            this.xlsExport.push([]);
            this.xlsExport.push([title2]);

            var colHdrs = [];
            var hdr = "";
            if (this.hdrLabels === null || this.hdrLabels.length < 1) {
                $('#' + this.sampleType + '-header').html("No data of this type");
                colHdrs.push("No data of this type");
                hdr += "No data of this type";
            }
            else {
                colHdrs.push("");
                hdr += '<tr><td>Expected Concentration</td>';
                var units = '';

                if (this.Units)
                    units = ' (' + this.Units + ')';

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

                    hdr += '<td class=\'' + css + ' left\'>' + label + units + '</td>' + '<td class=\'' + css + ' right\'>Bias (%)</td>';
                    colHdrs.push(label + units);
                    colHdrs.push("Bias (%)");
                }, this);
                hdr += '</tr>';
            }

            $('#' + this.sampleType + '-header').html(hdr);
            this.xlsExport.push(colHdrs);
        };

        var displayBody = function() {
            if (this.hdrLabels != null && this.hdrLabels.length > 0) {
                var html = getRawDataHtml();
                html += getSummaryHtml();
                $('#' + this.sampleType + '-body').html(html);
            }
        };

        var getSummaryHtml = function() {

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
                        html += '<td class=\'' + css + ' left\'>' + (data.length ? data.length : "") + '</td><td class=\'' + css + ' right\'></td>';
                        exportRow.push(data.length ? data.length : "");
                        exportRow.push("");
                    }
                    else {
                        summaryValue = this.summaryData[col][stat];
                        if (summaryValue === "") {
                            summaryValue = "NA";
                        }
                        html += '<td class=\'' + css + ' left\'>' + summaryValue + '</td><td class=\''+ css + ' right\'> </td>';

                        if (summaryValue === "NA") {
                            exportRow.push(summaryValue);
                        }
                        else {
                            exportRow.push(Number(summaryValue));
                        }
                        exportRow.push("");
                    }
                }, this);
                html += '</tr>';
                this.xlsExport.push(exportRow);
            }, this);

            return html;
        };

        var getRawDataHtml = function() {
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
                        html += '<td class=\'' + css + ' left\'>' + data.value + '</td><td class=\'' + css + ' right\'>' + data.bias + '</td>';
                        exportRow.push(Number(data.value));
                        exportRow.push(Number(data.bias));
                    }
                    else {
                        html += '<td class=\'' + css + ' left\'></td><td class=\'' + css + ' right\'></td>';
                        exportRow.push("");
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
                        if (replicate != null && colName !== 'NULL') {
                            if (row[col] != null) {
                                if (!this.rawData[colName]) {
                                    this.rawData[colName] = [];
                                    this.hdrLabels.push(colName);
                                }

                                this.rawData[colName].push({
                                    'value': row[col].toFixed(2),
                                    'bias': row['Bias'].toFixed(2)
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
                        if (colName !== "NULL" && rowName != null) {
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

        var handleData = function () {

            displayHeader();
            displayBody();

            if (this.sampleType === 'standard')
                createLoqStats();

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

            multi.send(handleData, this);
        };

        var createLoqStats = function() {
            var loq = 'NA', uloq = 'NA', bias;
            this.hdrLabels.forEach(function(label, index) {
                bias = Math.abs(Number(this.summaryData[label]["Bias"]));
                if (bias <= this.biasLimit) {
                    if (loq === 'NA') {
                        loq = label;
                    }
                    else {
                        uloq = label;
                    }
                }
            }, this);

            var units = this.Units ? this.Units : '';
            $('#bias-limit').html('Bias Limit: ' + this.biasLimit + '%');
            $('#loq-stat').html('LOQ: ' + loq + ' ' + units);
            $('#uloq-stat').html('ULOQ: ' + uloq + ' ' + units);

            this.xlsExport.push([]);
            this.xlsExport.push(['Bias Limit: ' + this.biasLimit + '%']);
            this.xlsExport.push(['LOQ: ' + loq + ' ' + units]);
            this.xlsExport.push(['ULOQ: ' + uloq + ' ' + units]);
        };

        var afterLoad = function() {
            $('#targetedms-afterload').html('LoadingDone')
        };

        var createQcFomTable = function() {
            createFomTable('qc', afterLoad);
        };

        var createStandardFomTable = function(callback) {
            createFomTable('standard', callback);
        };

        createStandardFomTable(createQcFomTable);

    }(jQuery);


</script>


