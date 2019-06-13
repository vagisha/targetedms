<%
/*
 * Copyright (c) 2018-2019 LabKey Corporation
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
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ taglib prefix="h" uri="http://www.labkey.org/taglib" %>
<%
    JspView<TargetedMSController.PKForm> me = (JspView<TargetedMSController.PKForm>) HttpView.currentView();
    TargetedMSController.PKForm bean = me.getModelBean();
%>

<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("targetedms/css/FiguresOfMerit.css");
        dependencies.add("targetedms/css/Pharmacokinetics.css");
        dependencies.add("internal/jQuery");
        dependencies.add("vis/vis");
    }
%>

<div class="container-fluid targetedms-fom">
    <h3 id="pk-title1"></h3>
    <h4 id="pk-title2"></h4>
    <h4 id="pk-title3"></h4>
    <br/>
<%
    for (String subgroup : bean.getSampleGroupNames() )
    {
        String subgroupTitle = "Subgroup: " + (subgroup != null ? subgroup : "");
%>
<labkey:panel title="<%=subgroupTitle%>" type="portal">
    <div id="targetedms-fom-export" class="export-icon" data-toggle="tooltip" title="Export to Excel">
        <i class="fa fa-file-excel-o" onclick="exportExcel('<%=h(subgroup)%>')"></i>
    </div>

    <labkey:panel title="Statistics">
    <table id="pk-table-input-<%=h(subgroup)%>" class="table table-striped table-responsive pk-table-stats"  >
        <thead><tr><td>Time</td><td>C0</td><td>Terminal</td><td>Concentration</td><td>Count</td><td>Std Dev</td></tr></thead>
    </table>
    <table id="pk-table-stats-<%=h(subgroup)%>" class="table table-striped table-responsive pk-table-stats" style="width: 600px">
        <thead><tr><td colspan="3">Statistic</td></tr></thead>
        <tr><td class="pk-table-label">Route           </td><td id="Route-<%=h(subgroup)%>"             class="pk-table-stat"></td><td></td></tr>
        <tr><td class="pk-table-label">Dose            </td><td id="Dose-<%=h(subgroup)%>"              class="pk-table-stat"></td><td id="DoseUnits-<%=h(subgroup)%>"></td></tr>
        <tr><td class="pk-table-label">IV CO           </td><td id="IVC0-<%=h(subgroup)%>"              class="pk-table-stat"></td><td></td></tr>
        <tr><td class="pk-table-label">k':             </td><td id="k-<%=h(subgroup)%>"                 class="pk-table-stat"></td><td></td></tr>
        <tr><td class="pk-table-label">%AUC Extrap:    </td><td id="AUCExtrap-<%=h(subgroup)%>"         class="pk-table-stat"></td><td></td></tr>
        <tr><td class="pk-table-label">MRT (0-inf):    </td><td id="Mrt_Zero_Inf-<%=h(subgroup)%>"      class="pk-table-stat"></td><td>hr</td></tr>
        <tr><td class="pk-table-label">MRT (0-t):      </td><td id="Mrt_Zero_T-<%=h(subgroup)%>"        class="pk-table-stat"></td><td>hr</td></tr>
        <tr><td class="pk-table-label">CL (0-inf):     </td><td id="Cl_Zero_Inf-<%=h(subgroup)%>"       class="pk-table-stat"></td><td>ml/min/kg</td></tr>
        <tr><td class="pk-table-label">CL (0-t):       </td><td id="Cl_Zero_T-<%=h(subgroup)%>"         class="pk-table-stat"></td><td>ml/min/kg</td></tr>
        <tr><td class="pk-table-label">Vdss (0-inf):   </td><td id="Vdss_Zero_Inf-<%=h(subgroup)%>"     class="pk-table-stat"></td><td>L/kg</td></tr>
        <tr><td class="pk-table-label">Vdss (0-t):     </td><td id="Vdss_Zero_T-<%=h(subgroup)%>"       class="pk-table-stat"></td><td>L/kg</td></tr>
        <tr><td class="pk-table-label">T1/2:           </td><td id="T1_2-<%=h(subgroup)%>"              class="pk-table-stat"></td><td>hr</td></tr>
        <tr><td class="pk-table-label">Effective T1/2: </td><td id="Effective_T1_2-<%=h(subgroup)%>"    class="pk-table-stat"></td><td>hr</td></tr>
    </table>
    <div id="nonIVC0Controls-<%=h(subgroup)%>" hidden="true">
        <span id="nonIVC0Controls-Warn-<%=h(subgroup)%>" class="labkey-error"><h4>WARNING: Please enter a non-IV C0 and recalculate.</h4></span>
        non-IV C0
        <input type="number" id="nonIVC0-<%=h(subgroup)%>" label="non-IV C0"/>
        <button id="btnNonIVC0-<%=h(subgroup)%>" onclick="updateStatsForNonIVC0('<%=h(subgroup)%>')">Recalculate</button>
    </div>
    </labkey:panel>

    <labkey:panel title="Charts">
    <div id="chart-<%=h(subgroup)%>"></div>
    <div id="chartLog-<%=h(subgroup)%>"></div>
    </labkey:panel>

    <labkey:panel title="Data">
    <table id="pk-table-standard-<%=h(subgroup)%>" class="table table-striped table-responsive pk-table">
        <thead id="standard-header-<%=h(subgroup)%>" />
        <tbody id="standard-body-<%=h(subgroup)%>" />
        <tfoot id="standard-footer-<%=h(subgroup)%>"/>
    </table>
    </labkey:panel>
</labkey:panel>
    <%}%>
</div>
<script type="application/javascript">
    +function ($) {

        var moleculeId = <%=bean.getGeneralMoleculeId()%>;
        var peptide='';
        var ion='';
        var fileName='';
        var timeRows={};
        var subgroups={};
        var dose = null;
        var doseUnits = null;
        var roa = null;
        var lr;
        var initialValues = {};

        const checkBoxC0 = ".checkboxC0";
        const checkBoxTerminal = ".terminal";

        //Spec ID: 31940 Panorama Partners - Figures of merit and PK calcs
        var timeRowZero = {Time: 0, Concentration: null};

        LABKEY.Utils.onReady(function() {
            LABKEY.Ajax.request({
                url: LABKEY.ActionURL.buildURL('targetedms', 'pharmacokineticsOptions.api'),
                method : 'POST',
                params: {moleculeId: moleculeId},
                scope: this,
                success: LABKEY.Utils.getCallbackWrapper(function(response)
                {
                    initialValues = response.subgroups;
                    createPKTable();
                }, this, false)
            });
        });

        function persistPKOptions(subgroup)
        {
            // save the selection options for this subgroup so the next time it is viewed, those values are used
            // as the initial default values for the form fields
            if (LABKEY.user.canUpdate) {
                var json = {moleculeId: moleculeId, subgroups: {}};
                json.subgroups[subgroup] = {
                    nonIVC0: getNonIVC0BySubgroup(subgroup),
                    c0: getCheckedBySubgroup(checkBoxC0, subgroup, 'time', true),
                    terminal: getCheckedBySubgroup(checkBoxTerminal, subgroup, 'time', true)
                };

                LABKEY.Ajax.request({
                    url: LABKEY.ActionURL.buildURL('targetedms', 'pharmacokineticsOptions.api'),
                    method: 'POST',
                    jsonData: json
                });
            }
        }

        function parseRawData(data, subgroup) {
            dose = null;
            doseUnits = null;
            roa = null;

            if (!data.rows || data.rows.length === 0) {
                $('#pk-title3').html("No data to show");
                return;
            }

            var message = '', sep = '';
            data.rows.forEach(function(timeRow) {
                if (timeRow.Time === undefined || timeRow.Time == null) {
                    message = "The replicate annotation named Time is missing or has no value.";
                    sep = '<br/>';
                }
                if (dose == null) {
                    dose = timeRow.Dose;
                }
                if (doseUnits == null) {
                    doseUnits = timeRow.DoseUnits;
                }
                if (roa== null) {
                    roa = timeRow.ROA;
                }
            });

            if (dose == null){
                message += sep + "The replicate annotation named Dose is missing, has no value, or has different values within a subgroup.";
                sep = '<br/>';
            }

            if (doseUnits == null) {
                message += sep + "The replicate annotation named DoseUnits is missing, has no value, or has different values within a subgroup.";
                sep = '<br/>';
            }

            if (roa == null) {
                message += sep + "The replicate annotation named ROA is missing, has no value, or has different values within a subgroup.";
                sep = '<br/>';
            }

            if(data.rows[0].Time !== 0){
                data.rows.unshift(timeRowZero);
            }

            timeRows[subgroup] = [];
            data.rows.forEach(function(timeRow){
                peptide = timeRow.Peptide;
                ion = timeRow.ionName;
                fileName = timeRow.FileName;
                var count = timeRow.ConcentrationCount;
                if(count === undefined){
                    count = '';
                }
                const item = {
                    time: timeRow.Time,
                    conc: timeRow.Concentration,
                    stdDev: timeRow.StandardDeviation,
                    count:  count
                };
                item.pkConc = item.conc;
                if (item.pkConc != null) {
                    item.lnCp = Math.log(item.pkConc);
                }
                item.concxt = item.time * item.pkConc;
                timeRows[subgroup].push(item)
            });

            if (peptide !== '' && peptide != null) {
                $('#pk-title1').html("Peptide: " + peptide);
            }
            else {
                $('#pk-title1').html("Molecule: " + ion);
            }

            $('#pk-title2').html("Skyline File: " + fileName);

            if (message.length > 0) {
                $('#pk-title3').html(message);
                return false;
            }

            subgroups[subgroup]={};
            var isIV = roa === "IV";
            if(isIV){
                subgroups[subgroup]['isIV'] = true;
            }else{
                $('#nonIVC0Controls-' + subgroup).removeAttr('hidden');
                subgroups[subgroup]['isIV'] = false;
            }

            $('#Dose-' + subgroup).html(dose);
            $('#DoseUnits-' + subgroup).html(doseUnits);
            $('#Route-' + subgroup).html(roa);

            // initial values to use from a previously persisted selection for this container/moleculeId/subgroup
            var initValues = initialValues[subgroup];

            // set the nonIVC0 value if we have one from the initial values
            if (initValues && initValues.nonIVC0 && initValues.nonIVC0.length > 0) {
                $('#nonIVC0-' + subgroup).val(initValues.nonIVC0);
            }

            timeRows[subgroup].forEach(function (row, index) {
                var checkedC0, checkedT;

                function isSelectedC0ByDefault() {
                    if(isIV){
                        return (!initValues && (index > 0 && index < 4))
                    }
                    return (!initValues && index < 3);
                }

// use initial values if they exist, default to selecting the first 3 time values for c0
                if ((initValues && initValues.c0.indexOf(row.time.toString()) > -1) || isSelectedC0ByDefault()) {
                    checkedC0 = 'checked';
                }
                // use initial values if they exist, default to selecting the last 3 time values for terminal
                if ((initValues && initValues.terminal.indexOf(row.time.toString()) > -1) || (!initValues && index > timeRows[subgroup].length - 4)) {
                    checkedT = 'checked';
                }

                $("<tr>" +
                        "<td class='pk-table-stat pk-table-time'>" + row.time + "</td>" +
                        "<td ><input type='checkbox' rowIndex= " + index + " " + checkedC0 + " class='checkboxC0' subgroup='" + subgroup + "' /></td>" +
                        "<td ><input type='checkbox' rowIndex= " + index + " " + checkedT + " class='terminal' subgroup='" + subgroup + "'/></td>" +
                        "<td class='pk-table-stat'>" + statRound(row.conc) + "</td>" +
                        "<td class='pk-table-stat'>" + row.count + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(row.stdDev) + "</td>" +
                        "</tr>").appendTo("#pk-table-input-" + subgroup);
            });

            $("<tr>" +"<td>inf</td><td ></td><td ></td><td ></td>" +
                    "<td class='pk-table-stat' id='infLinDeltaAUC-" + subgroup + "'></td>" +
                    "<td class='pk-table-stat' id='infAUCuMhrL-" + subgroup + "'></td><td></td>" +
                    "<td class='pk-table-stat' id='infLogLinAUCuMhrL-" + subgroup + "'></td><td></td>" +
                    "<td class='pk-table-stat' id='infCumulativeCorrectPartAUC-" + subgroup + "'></td><td></td>" +
                    "<td class='pk-table-stat' id='infLinDeltaAUMC-" + subgroup + "'></td>" +
                    "<td class='pk-table-stat' id='infCumulativeLinAUMC-" + subgroup + "'></td><td></td>" +
                    "<td class='pk-table-stat' id='infCumulativeLogLinAUMC-" + subgroup + "'></td><td></td>" +
                    "<td class='pk-table-stat' id='infCorrectAUMC-" + subgroup + "'></td>" +
                    "</tr>").appendTo("#standard-footer-" + subgroup);

            updateStats(checkBoxC0,subgroup);
            populateDerivedDataTable(subgroup);
            updateStats(checkBoxTerminal,subgroup);
            return true;
        }

        function populateDerivedDataTable(subgroup) {
             this.hdrLabels = ["Time","Concentration","PK Conc","In(Cp)", "LinDeltaAUC", "AUCuMhrL",
                "LogLinDeltaAUC", "LogLinAUCuMhrL","Correct Part AUC", "Correct Cumulative AUC",
                "Conc x t","LinDeltaAUMC", "CumulativeLinAUMC", "LogLinDeltaAUMC", "CumulativeLogLinAUMC",
                "CorrectDeltaAUMC", "CorrectAUMC"];

            $("#standard-header-" + subgroup).empty();
            this.hdrLabels.forEach(function (label) {
                $("<td>" + label + "</td>").appendTo("#standard-header-" + subgroup);
            });

            $("#standard-body-" + subgroup).empty();
            timeRows[subgroup].forEach(function (row, index) {
                $("<tr>" +
                        "<td class='pk-table-stat'>" + row.time + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(row.conc) + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(row.pkConc) + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(row.lnCp) + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(getLinDeltaAUC(index, subgroup)) + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(getAUCuMhrL(index, subgroup)) + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(getLogLinDeltaAUC(index, subgroup)) + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(getLogLinAUCuMhrL(index, subgroup)) + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(getCorrectPartAUC(index, subgroup)) + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(getCumulativeCorrectPartAUC(index, subgroup)) + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(row.concxt) + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(getLinDeltaAUMC(index, subgroup)) + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(getCumulativeLinAUMC(index, subgroup)) + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(getLogLinDeltaAUMC(index, subgroup)) + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(getCumulativeLogLinAUMC(index, subgroup)) + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(getCorrectDeltaAUMC(index, subgroup)) + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(getCorrectAUMC(index, subgroup)) + "</td>" +
                        "</tr>").appendTo("#standard-body-" + subgroup);
            });
        }

        function statRound(value){
            if(!value){
                return '';
            }
            return LABKEY.Utils.roundNumber(value,3).toLocaleString(undefined, {minimumFractionDigits: 3,maximumFractionDigits: 3});
        }

        function getLinearRegression (y,x) {
            var lr = {};
            var n = y.length;
            var sum_x = 0;
            var sum_y = 0;
            var sum_xy = 0;
            var sum_xx = 0;
            var sum_yy = 0;

            for (var i = 0; i < y.length; i++) {
                sum_x += x[i];
                sum_y += y[i];
                sum_xy += (x[i]*y[i]);
                sum_xx += (x[i]*x[i]);
                sum_yy += (y[i]*y[i]);
            }

            lr['slope'] = -1*(n * sum_xy - sum_x * sum_y) / (n*sum_xx - sum_x * sum_x);
            lr['intercept'] = (sum_y + lr.slope * sum_x)/n;
            lr['r2'] = Math.pow((n*sum_xy - sum_x*sum_y)/Math.sqrt((n*sum_xx-sum_x*sum_x)*(n*sum_yy-sum_y*sum_y)),2);

            return lr;
        }

        updateStatsForNonIVC0 = function(subgroup) {
            updateStats(checkBoxC0, subgroup);
            updateStats(checkBoxTerminal, subgroup);
            persistPKOptions(subgroup);
        };

        function getCheckedBySubgroup(timeFrame, subgroup, prop, includeZeroTime) {
            var vals = [];
            $(timeFrame + '[subgroup="' + subgroup + '"]:checked').each(function (index, box) {
                var row = timeRows[subgroup][box.getAttribute('rowIndex')];
                if (includeZeroTime || row.time != 0) {
                    vals.push(row[prop]);
                }
            });
            return vals;
        }

        function getNonIVC0BySubgroup(subgroup) {
            return $('#nonIVC0-' + subgroup).val();
        }
        
        function updateStats(timeFrame, subgroup) {
            var x = getCheckedBySubgroup(timeFrame, subgroup, 'time', false);
            var y = getCheckedBySubgroup(timeFrame, subgroup, 'lnCp', false);
            lr = getLinearRegression(y, x, subgroup);

            var c0 = lr.intercept;
            if (subgroups[subgroup].isIV === false){
                c0= getNonIVC0BySubgroup(subgroup);
                //show warning if no value provided
                if(c0 === ''){
                    $('#nonIVC0Controls-' + subgroup + ' span').removeAttr("hidden", "true");
                }else{
                    $('#nonIVC0Controls-' + subgroup + ' span').attr("hidden", "true");
                }
            }

            timeRows[subgroup][0].pkConc = Math.exp(c0);
            timeRows[subgroup][0].lnCp =  Math.log(Math.exp(c0));

            if (timeFrame === checkBoxC0) {
                $('#IVC0-' + subgroup).html(statRound(lr.intercept));
            }
            else {
                $('#k-' + subgroup).html(statRound(lr.slope));
                $('#AUCExtrap-' + subgroup).html(statRound(getAUCExtrap(lr, subgroup)));
                $('#Mrt_Zero_Inf-' + subgroup).html(statRound(getMrt_Zero_Inf(lr, subgroup)));
                $('#Mrt_Zero_T-' + subgroup).html(statRound(getMrt_Zero_T(subgroup)));
                $('#Cl_Zero_Inf-' + subgroup).html(statRound(getCL_Zero_Inf(lr, subgroup)));
                $('#Cl_Zero_T-' + subgroup).html(statRound(getCL_Zero_T(subgroup)));
                $('#Vdss_Zero_Inf-' + subgroup).html(statRound(getVdss_Zero_Inf(lr, subgroup)));
                $('#Vdss_Zero_T-' + subgroup).html(statRound(getVdss_Zero_T(lr, subgroup)));
                $('#T1_2-' + subgroup).html(statRound(getT1_2(lr, subgroup)));
                $('#Effective_T1_2-' + subgroup).html(statRound(getEffectiveT1_2(lr, subgroup)));
                $('#' + subgroup).html();

                $('#infLinDeltaAUC-' + subgroup ).html(statRound(getInfLinDeltaAUC(lr, subgroup)));
                $('#infAUCuMhrL-' + subgroup ).html(statRound(getInfAUCuMhrL(lr, subgroup)));
                $('#infLogLinAUCuMhrL-' + subgroup ).html(statRound(getInfLogLinAUCuMhrL(lr, subgroup)));
                $('#infCumulativeCorrectPartAUC-' + subgroup ).html(statRound(getInfCumulativeCorrectPartAUC(lr, subgroup)));
                $('#infLinDeltaAUMC-' + subgroup ).html(statRound(getInfLinDeltaAUMC(lr, subgroup)));
                $('#infCumulativeLinAUMC-' + subgroup ).html(statRound(getInfCumulativeLinAUMC(lr, subgroup)));
                $('#infCumulativeLogLinAUMC-' + subgroup ).html(statRound(getInfCumulativeLogLinAUMC(lr, subgroup)));
                $('#infCorrectAUMC-' + subgroup ).html(statRound(getInfCorrectAUMC(lr, subgroup)));
            }
            populateDerivedDataTable(subgroup);
        }

        $(document).on("click",checkBoxC0,function () {
            var subgroup = this.getAttribute("subgroup");
            updateStats.call(this,checkBoxC0, subgroup);
            persistPKOptions(subgroup);
        });

        $(document).on("click",checkBoxTerminal,function () {
            var subgroup = this.getAttribute("subgroup");
            updateStats.call(this,checkBoxTerminal, subgroup);
            persistPKOptions(subgroup);
        });

        var createPKTable = function() {
            this.rawData = {};
            this.summaryData = {};

            LABKEY.Query.selectRows( {
                schemaName: 'targetedms',
                queryName: 'Pharmacokinetics',
                filterArray: [LABKEY.Filter.create('MoleculeId', moleculeId)],
                sort : ['SubGroup,Time'],
                scope: this,
                success: function (data) {
                    var subgroupData = {rows:[]};
                    if (!data.rows || data.rows.length === 0) {
                        parseRawData(subgroupData);
                        return;
                    }

                    //break up data rows into collections by subgroup
                    //possibly use  _.groupBy()
                    var subgroup = data.rows[0].SubGroup;
                    data.rows.forEach(function (row,index) {
                            if (row.SubGroup === subgroup) {
                                subgroupData.rows.push(row);
                            }
                            else {
                                //build collection of next subgroup
                                if (parseRawData(subgroupData, subgroup)) {
                                    showCharts(subgroup);
                                }
                                subgroup = row.SubGroup;
                                subgroupData.rows = [row];
                            }

                            if (index === data.rows.length - 1 || subgroup != row.SubGroup) {
                                if (parseRawData(subgroupData, subgroup)) {
                                    showCharts(subgroup);
                                }
                            }
                    });
                },
                failure: function (response) {
                    LABKEY.Utils.alert(response);
                }
            });
        };

        function getTableHeaders() {
            var headers = [];
            this.hdrLabels.forEach(function (label) {
                headers.push(label);
            });
            return headers;
        }

        function getTableRows(tableId){
            var myRows = [];
            $(tableId + " tr").each(function() {
                var cells = $(this).find("td");
                var row = [];
                cells.each(function(index) {
                    if (this.hasChildNodes() && this.firstChild.hasAttribute && this.firstChild.checked) {
                        row.push('x')
                    }
                    else {
                        if (isNaN(this.innerText.replace(/,/g,''))) {
                            row.push(this.innerText);
                        }
                        else {
                            if (index===0) {
                                row.push(parseFloat(this.innerText));//Time has a different format
                            }
                            else {
                                row.push({ value: parseFloat(this.innerText.replace(/,/g,'')), formatString: '#,##0.000'});
                            }
                        }
                    }
                });
                myRows.push(row);
            });
            return myRows;
        }

        this.exportExcel = function(subgroup) {

            var sheet1Data = [
                    ['Peptide: ' + peptide],
                    ['Skyline File: ' + fileName],
                    ['Subgroup: ' + subgroup]
            ];
            sheet1Data.push(['']);
            sheet1Data.push(['AUC Calculation']);
            sheet1Data.push(['']);

            getTableRows("#pk-table-stats-" + subgroup).forEach(function(row){
                sheet1Data.push(row);
            });
            sheet1Data.push(['','']);
            getTableRows("#pk-table-input-" + subgroup).forEach(function(row){
                sheet1Data.push(row);
            });

            var sheet2Data = [getTableHeaders()];

             getTableRows("#standard-body-" + subgroup).forEach(function(row){
                 sheet2Data.push(row);
             });

            LABKEY.Utils.convertToExcel({
                fileName : fileName.split(".")[0] + '_' + peptide + '.xlsx',
                sheets: [{
                    name: 'Statistics',
                    data: sheet1Data
                },{
                    name: 'Data',
                    data: sheet2Data
                }]
            });
        };

        function showCharts(subgroup) {

            var labResultsPlotConfig = {
                rendererType: 'd3',
                renderTo: 'chart-' + subgroup ,
                labels: {
                    x: {value: "Time"},
                    y: {value: "Concentration"},
                    main: {value: "Linear"}
                },
                width: 1200,
                height: 500,
                clipRect: true,
                data: timeRows[subgroup],
                layers: [
                    new LABKEY.vis.Layer({
                        geom: new LABKEY.vis.Geom.Path({})
                    }),
                    new LABKEY.vis.Layer({
                        geom: new LABKEY.vis.Geom.Point()
                    })
                ],
                aes: {
                    x: 'time',
                    y: 'conc'
                },
                scales: {
                    x: {scaleType: 'continuous'},
                    y: {scaleType: 'continuous'}
                }
            };

            var labResultsPlot = new LABKEY.vis.Plot(labResultsPlotConfig);
            labResultsPlot.render();

            labResultsPlotConfig.renderTo =  'chartLog-' + subgroup;
            labResultsPlotConfig.scales.y.trans = 'log';
            labResultsPlotConfig.labels.main.value = 'Log-Linear';
            var labResultsPlotLog = new LABKEY.vis.Plot(labResultsPlotConfig);
            labResultsPlotLog.render();
        }

        function getInfLinDeltaAUC(lr,subgroup) {
            return timeRows[subgroup][timeRows[subgroup].length -1].conc/lr.slope
        }

        //The comment at the top of each function refers to the related Excel formula from the sample spreadsheet
        //referenced in Spec ID: 31940 Panorama Partners - Figures of merit and PK calcs
        function getLinDeltaAUC(index, subgroup){
            //    (C4+C5)*(A5-A4)/2
            if (index === 0) {
                return 0;
            }
            return ((timeRows[subgroup][index-1].pkConc + timeRows[subgroup][index].pkConc)*(timeRows[subgroup][index].time-timeRows[subgroup][index-1].time))/2
        }

        function getLogLinDeltaAUC(index, subgroup){
            //    =(A5-A4)*(C4-C5)/(D4-D5)
            if (index === 0) {
                return 0;
            }
            const timeRow = timeRows[subgroup][index];
            const timeRowPrevious = timeRows[subgroup][index-1];
            return ((timeRow.time-timeRowPrevious.time)*(timeRowPrevious.pkConc - timeRow.pkConc))/(timeRowPrevious.lnCp-timeRow.lnCp)
        }

        function getAUCuMhrL(index, subgroup){
            //    =J5+I6
            if (index === 0) {
                return 0;
            }
            if (index === 1) {
                return getLinDeltaAUC(index, subgroup)
            }
            return getLinDeltaAUC(index, subgroup) + getAUCuMhrL(index -1, subgroup);
        }

        function getLogLinAUCuMhrL(index, subgroup){
            //    =L4+K5
            if (index === 0) {
                return 0;
            }
            return getLogLinAUCuMhrL(index -1, subgroup) +  getLogLinDeltaAUC(index, subgroup);
        }

        function getLinDeltaAUMC(index, subgroup){
            //    =(O4+O5)*(A5-A4)/2
            if (index === 0) {
                return 0;
            }
            return ((timeRows[subgroup][index-1].concxt + timeRows[subgroup][index].concxt)*(timeRows[subgroup][index].time-timeRows[subgroup][index-1].time))/2
        }

        function getCumulativeLinAUMC(index, subgroup){
            //    =Q4+P5
            if (index === 0) {
                return 0;
            }
            return getCumulativeLinAUMC(index-1, subgroup) + getLinDeltaAUMC(index, subgroup);
        }

        function getCumulativeCorrectPartAUC(index, subgroup){
            //    =N4+M5
            if (index === 0) {
                return 0;
            }
            return getCumulativeCorrectPartAUC(index-1, subgroup) + getCorrectPartAUC(index, subgroup);
        }

        function getCumulativeLogLinAUMC(index, subgroup){
            //    =S4+R5
            if (index === 0) {
                return 0;
            }
            return getCumulativeLogLinAUMC(index-1, subgroup) + getLogLinDeltaAUMC(index, subgroup);
        }

        function getInfAUCuMhrL(lr, subgroup){
            return getAUCuMhrL(timeRows[subgroup].length -1, subgroup) + getInfLinDeltaAUC(lr, subgroup);
        }

        function getCorrectPartAUC(index, subgroup) {
            //    =IF(C5>=C4,I5,K5)
            if (index === 0) {
                return 0;
            }
            const timeRow = timeRows[subgroup][index];
            const timeRowPrevious = timeRows[subgroup][index-1];

            if (timeRow.pkConc >= timeRowPrevious.pkConc) {
                return getLinDeltaAUC(index, subgroup);
            }
            return getLogLinDeltaAUC(index, subgroup);
        }

        function getLogLinDeltaAUMC(index, subgroup) {
            // =(A5-A4)*
            //  ((O5-O4)/(LN(C5/C4)))-
            //  ((A5-A4)^2)*(C5-C4)/(LN(C5/C4)^2)
            if (index === 0) {
                return 0;
            }
            const timeRow = timeRows[subgroup][index];
            const timeRowPrevious = timeRows[subgroup][index-1];
            const timeDiff = (timeRow.time - timeRowPrevious.time);
            const concxtDiff = timeRow.concxt - timeRowPrevious.concxt;
            const pkconcRatio = timeRow.pkConc / timeRowPrevious.pkConc;
            const pkconcDiff = timeRow.pkConc - timeRowPrevious.pkConc;
            return timeDiff * ((concxtDiff /(Math.log(pkconcRatio))))//(A5-A4)*((O5-O4)/(LN(C5/C4)))
                    - ((Math.pow(timeDiff, 2))* (pkconcDiff) )//-((A5-A4)^2)*(C5-C4)
                    / (Math.pow(Math.log(pkconcRatio), 2))      // /(LN(C5/C4)^2)
                    ;
        }

        function getCorrectDeltaAUMC(index, subgroup) {
            //=IF(C5>=C4,P5,R5)
            if (index === 0) {
                return 0;
            }
            const timeRow = timeRows[subgroup][index];
            const timeRowPrevious = timeRows[subgroup][index-1];

            if (timeRow.pkConc>= timeRowPrevious.pkConc) {
                return getLinDeltaAUMC(index, subgroup);
            }
            return getLogLinDeltaAUMC(index, subgroup);
        }

        function getCorrectAUMC(index, subgroup) {
            //=U4+T5
            if (index === 0) {
                return 0;
            }
            return getCorrectAUMC(index-1, subgroup) + getCorrectDeltaAUMC(index, subgroup);
        }

        function getInfLogLinAUCuMhrL(lr, subgroup){
            return getLogLinAUCuMhrL(timeRows[subgroup].length -1, subgroup) + getInfLinDeltaAUC(lr, subgroup);
        }

        function getInfLinDeltaAUMC(lr, subgroup) {
            //    =(C21/B27^2)+(O21/B27)
            const timeRow = timeRows[subgroup][timeRows[subgroup].length -1];
            return (timeRow.pkConc/(lr.slope*lr.slope)) + (timeRow.concxt/lr.slope);//todo where is the negative coming from
        }

        function getInfCumulativeLinAUMC(lr, subgroup){
            //=Q21+P23
            return getCumulativeLinAUMC(timeRows[subgroup].length -1, subgroup) + getInfLinDeltaAUMC(lr, subgroup);
        }

        function getInfCumulativeCorrectPartAUC(lr, subgroup){
            //=N21+I23
            return getCumulativeCorrectPartAUC(timeRows[subgroup].length -1, subgroup) + getInfLinDeltaAUC(lr, subgroup);
        }

        function getInfCumulativeLogLinAUMC(lr, subgroup){
            //=S21+P23
            return getCumulativeLogLinAUMC(timeRows[subgroup].length -1, subgroup) + getInfLinDeltaAUMC(lr, subgroup);
        }

        function getInfCorrectAUMC(lr, subgroup){
            //=U21+P23
            return getCorrectAUMC(timeRows[subgroup].length -1, subgroup) + getInfLinDeltaAUMC(lr, subgroup);
        }

        function getAUCExtrap(lr, subgroup){
            //    =I23/N23*100
            return getInfLinDeltaAUC(lr, subgroup)/getInfCumulativeCorrectPartAUC(lr, subgroup)*100;
        }

        function getMrt_Zero_Inf(lr, subgroup){
            // =U23/N23
            return getInfCorrectAUMC(lr, subgroup)/getInfCumulativeCorrectPartAUC(lr, subgroup);
        }

        function getMrt_Zero_T(subgroup){
            // =U21/N21
            return getCorrectAUMC(timeRows[subgroup].length -1, subgroup)/getCumulativeCorrectPartAUC(timeRows[subgroup].length -1, subgroup);
        }

        function getCL_Zero_Inf(lr, subgroup){
            // =B26/N23*1/60
            return (dose/getInfCumulativeCorrectPartAUC(lr, subgroup))/60;
        }

        function getCL_Zero_T(subgroup){
            // =B26/N21*1/60
            return (dose/getCumulativeCorrectPartAUC(timeRows[subgroup].length -1, subgroup))/60;
        }

        function getVdss_Zero_Inf(lr, subgroup){
            // =(P30*60/1000)*P28
            return (getCL_Zero_Inf(lr, subgroup)*60/1000)*getMrt_Zero_Inf(lr, subgroup);
        }

        function getVdss_Zero_T(lr, subgroup){
            // =(P31*60/1000)*P29
            return (getCL_Zero_T(subgroup)*60/1000)*getMrt_Zero_T(subgroup);
        }

        function getT1_2(lr, subgroup){
            // =LN(2)/B27
            return Math.log(2)/lr.slope;
        }

        function getEffectiveT1_2(lr, subgroup){
            // =LN(2)*P32/P30*1000/60
            return Math.log(2)*getVdss_Zero_Inf(lr, subgroup)/getCL_Zero_Inf(lr, subgroup)*1000/60;
        }
    }(jQuery);
</script>
