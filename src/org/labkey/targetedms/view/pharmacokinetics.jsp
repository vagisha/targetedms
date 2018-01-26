<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>

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
    <div id="targetedms-fom-export" class="export-icon" data-toggle="tooltip" title="Export to Excel">
        <i class="fa fa-file-excel-o" onclick="exportExcel()"></i>
    </div>
    <h3 id="pk-title1"></h3>
    <h4 id="pk-title2"></h4>
    <br>

<labkey:panel title="Statistics">
    <table id="pk-table-input" class="table table-striped table-responsive pk-table-stats"  >
        <thead><tr><td>Time</td><td>C0</td><td>Terminal</td><td>Concentration</td></tr></thead>
    </table>
    <table id="pk-table-stats" class="table table-striped table-responsive pk-table-stats" style="width: 600px">
        <thead><tr><td colspan="3">Statistic</td></tr></thead>
        <tr><td class="pk-table-label">Dose            </td><td id="Dose"              class="pk-table-stat"></td><td id="DoseUnits"></td></tr>
        <tr><td class="pk-table-label">IV CO           </td><td id="IVCO"              class="pk-table-stat"></td><td></td></tr>
        <tr><td class="pk-table-label">k':             </td><td id="k"                 class="pk-table-stat"></td><td></td></tr>
        <tr><td class="pk-table-label">%AUC Extrap:    </td><td id="AUCExtrap"         class="pk-table-stat"></td><td></td></tr>
        <tr><td class="pk-table-label">MRT (0-inf):    </td><td id="Mrt_Zero_Inf"      class="pk-table-stat"></td><td>hr</td></tr>
        <tr><td class="pk-table-label">MRT (0-t):      </td><td id="Mrt_Zero_T"        class="pk-table-stat"></td><td>hr</td></tr>
        <tr><td class="pk-table-label">CL (0-inf):     </td><td id="Cl_Zero_Inf"       class="pk-table-stat"></td><td>ml/min/kg</td></tr>
        <tr><td class="pk-table-label">CL (0-t):       </td><td id="Cl_Zero_T"         class="pk-table-stat"></td><td>ml/min/kg</td></tr>
        <tr><td class="pk-table-label">Vdss (0-inf):   </td><td id="Vdss_Zero_Inf"     class="pk-table-stat"></td><td>L/kg</td></tr>
        <tr><td class="pk-table-label">Vdss (0-t):     </td><td id="Vdss_Zero_T"       class="pk-table-stat"></td><td>L/kg</td></tr>
        <tr><td class="pk-table-label">T1/2:           </td><td id="T1_2"              class="pk-table-stat"></td><td>hr</td></tr>
        <tr><td class="pk-table-label">Effective T1/2: </td><td id="Effective_T1_2"    class="pk-table-stat"></td><td>hr</td></tr>
    </table>
</labkey:panel>
<labkey:panel title="Charts">
    <div id="chart"></div>
    <div id="chartLog"></div>
</labkey:panel>
<labkey:panel title="Data">
    <table id="pk-table-standard" class="table table-striped table-responsive pk-table">
        <thead id="standard-header" />
        <tbody id="standard-body" />
        <tfoot id="standard-footer"/>
    </table>
    <table id="pk-table-qc" class="table table-striped table-responsive pk-table">
        <thead id="qc-header" />
        <tbody id="qc-body" />
    </table>
</labkey:panel>
</div>
<script type="application/javascript">
    +function ($) {

        var params = LABKEY.ActionURL.getParameters();

        this.moleculeId = params['GeneralMoleculeId'];
        var peptide='';
        var ion='';
        var fileName='';
        var timeRows=[];
        var dose = null;
        var doseUnits = null;


        //These are the values from the sample spreadsheet, sheet IVPO, referenced in
        //Spec ID: 31940 Panorama Partners - Figures of merit and PK calcs
        var timeRowZero = {Time: 0 ,          Concentration :  null        };
        var timeRowsMock=[
             {Time: .133333333 , Concentration :  0.006184700 , Dose: 0.53, DoseUnits: 'mg/ml'}
            ,{Time: 0.25       , Concentration :  0.006025200 , Dose: 0.53, DoseUnits: 'mg/ml'}
            ,{Time: 0.5        , Concentration :  0.004984600 , Dose: 0.53, DoseUnits: 'mg/ml'}
            ,{Time: 0.75       , Concentration :  0.003525100 , Dose: 0.53, DoseUnits: 'mg/ml'}
            ,{Time: 1          , Concentration :  0.004663500 , Dose: 0.53, DoseUnits: 'mg/ml'}
            ,{Time: 1.5        , Concentration :  0.002800500 , Dose: 0.53, DoseUnits: 'mg/ml'}
            ,{Time: 2          , Concentration :  0.001523200 , Dose: 0.53, DoseUnits: 'mg/ml'}
            ,{Time: 2.5        , Concentration :  0.000978510 , Dose: 0.53, DoseUnits: 'mg/ml'}
            ,{Time: 3          , Concentration :  0.000924980 , Dose: 0.53, DoseUnits: 'mg/ml'}
            ,{Time: 3.5        , Concentration :  0.000632570 , Dose: 0.53, DoseUnits: 'mg/ml'}
            ,{Time: 4          , Concentration :  0.000447090 , Dose: 0.53, DoseUnits: 'mg/ml'}
            ,{Time: 4.5        , Concentration :  0.000438690 , Dose: 0.53, DoseUnits: 'mg/ml'}
            ,{Time: 5          , Concentration :  0.000209690 , Dose: 0.53, DoseUnits: 'mg/ml'}
            ,{Time: 5.5        , Concentration :  0.000260050 , Dose: 0.53, DoseUnits: 'mg/ml'}
            ,{Time: 6          , Concentration :  0.000158830 , Dose: 0.53, DoseUnits: 'mg/ml'}
            ,{Time: 7          , Concentration :  0.000124520 , Dose: 0.53, DoseUnits: 'mg/ml'}
            ,{Time: 8          , Concentration :  0.000082918 , Dose: 0.53, DoseUnits: 'mg/ml'}];

        function parseRawData(data) {
            // data.rows = timeRowsMock;


            if(!data.rows || data.rows.length === 0){
                $('#pk-title1').html("No data to show");
                return;
            }

            var message;
            data.rows.forEach(function(timeRow) {
                if (timeRow.Time === undefined || timeRow.Time == null) {
                    message = "The replicate annotation named Time is missing or has no value.";
                }
                if(dose == null){
                    dose = timeRow.Dose;
                }
                if(doseUnits == null){
                    doseUnits = timeRow.DoseUnits;
                }
            });

            if(dose == null){
                var doseMessage = "The replicate annotation named Dose is missing, has no value, or has different values within a subgroup.";
                if(!message){
                    message = doseMessage;
                }
                else{
                    message += doseMessage;
                }
            }

            if(doseUnits == null){
                var doseMessage = "The replicate annotation named DoseUnits is missing, has no value, or has different values within a subgroup.";
                if(!message){
                    message = doseMessage;
                }
                else{
                    message += doseMessage;
                }
            }

            if(message){
                $('#pk-title1').html(message);
                return false;
            }

            if(data.rows[0].Time !== 0){
                data.rows.unshift(timeRowZero);
            }
            data.rows.forEach(function(timeRow){
                peptide = timeRow.Peptide;
                ion = timeRow.ionName;
                fileName = timeRow.FileName;
                const item = {
                            time: timeRow.Time,
                            conc: timeRow.Concentration
                        };
                    item.pkConc = item.conc;
                    if(item.pkConc != null) {
                        item.lnCp = Math.log(item.pkConc);
                    }
                    item.concxt = item.time * item.pkConc;
                timeRows.push(
                        item
                )
            });

            if(peptide !== '' && peptide != null) {
                $('#pk-title1').html("Peptide: " + peptide);
            }else{
                $('#pk-title1').html("Molecule: " + ion);
            }

            $('#pk-title2').html("Skyline File: " + fileName);

            $('#Dose').html(dose);
            $('#DoseUnits').html(doseUnits);

            timeRows.forEach(function (row, index) {
                var checkedC0;
                if(index < 3 ) {checkedC0='checked';}
                var checkedT;
                if(index > timeRows.length - 4) {checkedT='checked';}

                $("<tr>" +
                        "<td class='pk-table-stat'>" + row.time + "</td>" +
                        "<td ><input type='checkbox' rowIndex= " + index + " " + checkedC0 + " class='checkboxC0' /></td>" +
                        "<td ><input type='checkbox' rowIndex= " + index + " " + checkedT + " class='terminal' /></td>" +
                        "<td class='pk-table-stat'>" + statRound(row.conc) + "</td>" +
                        "</tr>")
                        .appendTo("#pk-table-input");
            });



            $("<tr>" +"<td>inf</td><td ></td><td ></td><td ></td>" +
                    "<td class='pk-table-stat' id='infLinDeltaAUC'></td>" +
                    "<td class='pk-table-stat' id='infAUCuMhrL'></td><td></td>" +
                    "<td class='pk-table-stat' id='infLogLinAUCuMhrL'></td><td></td>" +
                    "<td class='pk-table-stat' id='infCumulativeCorrectPartAUC'></td><td></td>" +
                    "<td class='pk-table-stat' id='infLinDeltaAUMC'></td>" +
                    "<td class='pk-table-stat' id='infCumulativeLinAUMC'></td><td></td>" +
                    "<td class='pk-table-stat' id='infCumulativeLogLinAUMC'></td><td></td>" +
                    "<td class='pk-table-stat' id='infCorrectAUMC'></td>" +
                    "</tr>").appendTo("#standard-footer");

            updateStats(checkBoxC0);
            populateDerivedDataTable();
            updateStats(checkBoxTerminal);
            return true;
        }

        function populateDerivedDataTable() {
            $("#standard-body").empty();
            timeRows.forEach(function (row, index) {
                $("<tr>" +
                        "<td class='pk-table-stat'>" + row.time + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(row.conc) + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(row.pkConc) + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(row.lnCp) + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(getLinDeltaAUC(index)) + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(getAUCuMhrL(index)) + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(getLogLinDeltaAUC(index)) + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(getLogLinAUCuMhrL(index)) + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(getCorrectPartAUC(index)) + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(getCumulativeCorrectPartAUC(index)) + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(row.concxt) + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(getLinDeltaAUMC(index)) + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(getCumulativeLinAUMC(index)) + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(getLogLinDeltaAUMC(index)) + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(getCumulativeLogLinAUMC(index)) + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(getCorrectDeltaAUMC(index)) + "</td>" +
                        "<td class='pk-table-stat'>" + statRound(getCorrectAUMC(index)) + "</td>" +
                        "</tr>")
                        .appendTo("#standard-body");
            });
        }

        function statRound(value){
            if(!value){
                return '';
            }
            return LABKEY.Utils.roundNumber(value,3).toLocaleString(undefined, {minimumFractionDigits: 3,maximumFractionDigits: 3});
        }

        function getLinearRegression (y,x){
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


        const checkBoxC0 = ".checkboxC0";
        const checkBoxTerminal = ".terminal";
        var lr;
        function updateStats(timeFrame) {
            var x = [];
            var y = [];
            $(timeFrame + ":checked").each(function (index, box) {
                var row = timeRows[box.getAttribute('rowIndex')];
                if(row.time != 0) {
                    x.push(row.time);
                    y.push(row.lnCp);
                }
            });
            lr = getLinearRegression(y, x);

            timeRows[0].pkConc = Math.exp(lr.intercept);
            timeRows[0].lnCp =  Math.log(Math.exp(lr.intercept));

            if(timeFrame === checkBoxC0)
            {
                $('#IVCO').html(statRound(lr.intercept));
                var ivcoExp = statRound((Math.exp(lr.intercept)));
                $('#IVCOEXP').html(ivcoExp);
            }else{
                $('#k').html(statRound(lr.slope));
                $('#AUCExtrap     ').html(statRound(getAUCExtrap(lr)));
                $('#Mrt_Zero_Inf  ').html(statRound(getMrt_Zero_Inf(lr)));
                $('#Mrt_Zero_T    ').html(statRound(getMrt_Zero_T(lr)));
                $('#Cl_Zero_Inf   ').html(statRound(getCL_Zero_Inf(lr)));
                $('#Cl_Zero_T     ').html(statRound(getCL_Zero_T(lr)));
                $('#Vdss_Zero_Inf ').html(statRound(getVdss_Zero_Inf(lr)));
                $('#Vdss_Zero_T   ').html(statRound(getVdss_Zero_T(lr)));
                $('#T1_2          ').html(statRound(getT1_2(lr)));
                $('#Effective_T1_2').html(statRound(getEffectiveT1_2(lr)));
                $('#').html();

                $('#infLinDeltaAUC').html(statRound(getInfLinDeltaAUC(lr)));
                $('#infAUCuMhrL').html(statRound(getInfAUCuMhrL(lr)));
                $('#infLogLinAUCuMhrL').html(statRound(getInfLogLinAUCuMhrL(lr)));
                $('#infCumulativeCorrectPartAUC').html(statRound(getInfCumulativeCorrectPartAUC(lr)));
                $('#infLinDeltaAUMC').html(statRound(getInfLinDeltaAUMC(lr)));
                $('#infCumulativeLinAUMC').html(statRound(getInfCumulativeLinAUMC(lr)));
                $('#infCumulativeLogLinAUMC').html(statRound(getInfCumulativeLogLinAUMC(lr)));
                $('#infCorrectAUMC').html(statRound(getInfCorrectAUMC(lr)));
            }
            populateDerivedDataTable();
        }

        $(document).on("click",checkBoxC0,function () {
            updateStats.call(this,checkBoxC0);
        });

        $(document).on("click",checkBoxTerminal,function () {
            updateStats.call(this,checkBoxTerminal);
        });

        var createPKTable = function()
        {
            this.rawData = {};
            this.summaryData = {};
            this.hdrLabels = ["Time","Concentration","PK Conc","In(Cp)", "LinDeltaAUC", "AUCuMhrL",
                "LogLinDeltaAUC", "LogLinAUCuMhrL","Correct Part AUC", "Correct Cumulative AUC",
                "Conc x t","LinDeltaAUMC", "CumulativeLinAUMC", "LogLinDeltaAUMC", "CumulativeLogLinAUMC",
                "CorrectDeltaAUMC", "CorrectAUMC"];

                this.hdrLabels.forEach(function (label) {
                    $("<td>" + label + "</td>").appendTo("#standard-header");
                });

            LABKEY.Query.selectRows( {
                schemaName: 'targetedms',
                queryName: 'Pharmacokinetics',
                filterArray: [LABKEY.Filter.create('MoleculeId', params['GeneralMoleculeId'])],
                sort : 'Time',
                scope: this,
                success: function (data) {
                    if(parseRawData(data)){
                        showCharts();
                    };
                },
                failure: function (response) {
                    LABKEY.Utils.alert(response);
                }
            });
        };

        createPKTable();

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
                    if(this.hasChildNodes() && this.firstChild.hasAttribute && this.firstChild.checked){
                        row.push('x')
                    }else {
                        if(isNaN(this.innerText.replace(/,/g,''))) {
                            row.push(this.innerText);
                        }
                        else {
                            if(index===0)
                                row.push(parseFloat(this.innerText));//Time has a different format
                            else
                                row.push({ value: parseFloat(this.innerText.replace(/,/g,'')), formatString: '#,##0.000'});
                        }
                    }
                });
                myRows.push(row);
            });
            return myRows;
        }

        this.exportExcel = function() {

            var sheet1Data = [
                    ['Peptide: ' + peptide],
                    ['Skyline File: ' + fileName]
            ];
            sheet1Data.push(['']);
            sheet1Data.push(['AUC Calculation']);
            sheet1Data.push(['']);

            getTableRows("#pk-table-stats").forEach(function(row){
                sheet1Data.push(row);
            });
            sheet1Data.push(['','']);
            getTableRows("#pk-table-input").forEach(function(row){
                sheet1Data.push(row);
            });

            var sheet2Data = [getTableHeaders()];

             getTableRows("#standard-body").forEach(function(row){
                 sheet2Data.push(row);
             });

            LABKEY.Utils.convertToExcel({
                fileName : fileName.split(".")[0] + '_' + peptide + '.xlsx',
                sheets:
                        [
                            {
                                name: 'Statistics',
                                data: sheet1Data
                            }, {
                            name: 'Data',
                            data: sheet2Data
                        }
                        ]
            });
        };
        function showCharts() {

        var labResultsPlotConfig = {
            rendererType: 'd3',
            renderTo: 'chart',
            labels: {
                x: {value: "Time"},
                y: {value: "Concentration"},
                main: {value: "Linear"}
            },
            width: 1200,
            height: 500,
            clipRect: true,
            data: timeRows,
            layers: [new LABKEY.vis.Layer({
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
                x: {
                    scaleType: 'continuous'
                },
                y: {
                    scaleType: 'continuous'
                }
            }
        };

            var labResultsPlot = new LABKEY.vis.Plot(labResultsPlotConfig);
            labResultsPlot.render();

            labResultsPlotConfig.renderTo = 'chartLog';
            labResultsPlotConfig.scales.y.trans = 'log';
            labResultsPlotConfig.labels.main.value = 'Log-Linear';
            var labResultsPlotLog = new LABKEY.vis.Plot(labResultsPlotConfig);
            labResultsPlotLog.render();
        }
        function getInfLinDeltaAUC(lr) {
            return timeRows[timeRows.length -1].conc/lr.slope
        }

        //The comment at the top of each function refers to the related Excel formula from the sample spreadsheet
        //referenced in Spec ID: 31940 Panorama Partners - Figures of merit and PK calcs
        function getLinDeltaAUC(index){
            //    (C4+C5)*(A5-A4)/2
            if(index===0){
                return 0;
            }
            return ((timeRows[index-1].pkConc + timeRows[index].pkConc)*(timeRows[index].time-timeRows[index-1].time))/2
        }

        function getLogLinDeltaAUC(index){
            //    =(A5-A4)*(C4-C5)/(D4-D5)
            if(index===0){
                return 0;
            }
            const timeRow = timeRows[index];
            const timeRowPrevious = timeRows[index-1];
            return ((timeRow.time-timeRowPrevious.time)*(timeRowPrevious.pkConc - timeRow.pkConc))/(timeRowPrevious.lnCp-timeRow.lnCp)
        }

        function getAUCuMhrL(index){
            //    =J5+I6
            if(index===0){
                return 0;
            }
            if(index===1){
                return getLinDeltaAUC(index)
            }
            return getLinDeltaAUC(index) + getAUCuMhrL(index -1);
        }

        function getLogLinAUCuMhrL(index){
            //    =L4+K5
            if(index===0){
                return 0;
            }
            return getLogLinAUCuMhrL(index -1) +  getLogLinDeltaAUC(index);
        }

        function getLinDeltaAUMC(index){
            //    =(O4+O5)*(A5-A4)/2
            if(index===0){
                return 0;
            }
            return ((timeRows[index-1].concxt + timeRows[index].concxt)*(timeRows[index].time-timeRows[index-1].time))/2
        }

        function getCumulativeLinAUMC(index){
            //    =Q4+P5
            if(index===0){
                return 0;
            }
            return getCumulativeLinAUMC(index-1) + getLinDeltaAUMC(index);
        }

        function getCumulativeCorrectPartAUC(index){
            //    =N4+M5
            if(index===0){
                return 0;
            }
            return getCumulativeCorrectPartAUC(index-1) + getCorrectPartAUC(index);
        }

        function getCumulativeLogLinAUMC(index){
            //    =S4+R5
            if(index===0){
                return 0;
            }
            return getCumulativeLogLinAUMC(index-1) + getLogLinDeltaAUMC(index);
        }

        function getInfAUCuMhrL(lr){
            return getAUCuMhrL(timeRows.length -1) + getInfLinDeltaAUC(lr);
        }

        function getCorrectPartAUC(index) {
            //    =IF(C5>=C4,I5,K5)
            if(index===0){
                return 0;
            }
            const timeRow = timeRows[index];
            const timeRowPrevious = timeRows[index-1];

            if(timeRow.pkConc >= timeRowPrevious.pkConc){
                return getLinDeltaAUC(index);
            }
            return getLogLinDeltaAUC(index);
        }

        function getLogLinDeltaAUMC(index) {
            // =(A5-A4)*
            //  ((O5-O4)/(LN(C5/C4)))-
            //  ((A5-A4)^2)*(C5-C4)/(LN(C5/C4)^2)
            if(index===0){
                return 0;
            }
            const timeRow = timeRows[index];
            const timeRowPrevious = timeRows[index-1];
            const timeDiff = (timeRow.time - timeRowPrevious.time);
            const concxtDiff = timeRow.concxt - timeRowPrevious.concxt;
            const pkconcRatio = timeRow.pkConc / timeRowPrevious.pkConc;
            const pkconcDiff = timeRow.pkConc - timeRowPrevious.pkConc;
            return timeDiff * ((concxtDiff /(Math.log(pkconcRatio))))//(A5-A4)*((O5-O4)/(LN(C5/C4)))
                    - ((Math.pow(timeDiff, 2))* (pkconcDiff) )//-((A5-A4)^2)*(C5-C4)
                    / (Math.pow(Math.log(pkconcRatio), 2))      // /(LN(C5/C4)^2)
                    ;
        }

        function getCorrectDeltaAUMC(index) {
            //=IF(C5>=C4,P5,R5)
            if(index===0){
                return 0;
            }
            const timeRow = timeRows[index];
            const timeRowPrevious = timeRows[index-1];

            if(timeRow.pkConc>= timeRowPrevious.pkConc){
                return getLinDeltaAUMC(index);
            }
            return getLogLinDeltaAUMC(index);

        }

        function getCorrectAUMC(index) {
            //=U4+T5
            if(index===0){
                return 0;
            }
            return getCorrectAUMC(index-1) + getCorrectDeltaAUMC(index);

        }

        function getInfLogLinAUCuMhrL(lr){
            return getLogLinAUCuMhrL(timeRows.length -1) + getInfLinDeltaAUC(lr);
        }

        function getInfLinDeltaAUMC(lr) {
            //    =(C21/B27^2)+(O21/B27)
            const timeRow = timeRows[timeRows.length -1];
            return (timeRow.pkConc/(lr.slope*lr.slope)) + (timeRow.concxt/lr.slope);//todo where is the negative coming from
        }

        function getInfCumulativeLinAUMC(lr){
            //=Q21+P23
            return getCumulativeLinAUMC(timeRows.length -1) + getInfLinDeltaAUMC(lr);
        }

        function getInfCumulativeCorrectPartAUC(lr){
            //=N21+I23
            return getCumulativeCorrectPartAUC(timeRows.length -1) + getInfLinDeltaAUC(lr);
        }

        function getInfCumulativeLogLinAUMC(lr){
            //=S21+P23
            return getCumulativeLogLinAUMC(timeRows.length -1) + getInfLinDeltaAUMC(lr);
        }

        function getInfCorrectAUMC(lr){
            //=U21+P23
            return getCorrectAUMC(timeRows.length -1) + getInfLinDeltaAUMC(lr);
        }

        function getAUCExtrap(lr){
            //    =I23/N23*100
            return getInfLinDeltaAUC(lr)/getInfCumulativeCorrectPartAUC(lr)*100;
        }

        function getMrt_Zero_Inf(lr){
            // =U23/N23
            return getInfCorrectAUMC(lr)/getInfCumulativeCorrectPartAUC(lr);
        }

        function getMrt_Zero_T(){
            // =U21/N21
            return getCorrectAUMC(timeRows.length -1)/getCumulativeCorrectPartAUC(timeRows.length -1);
        }

        function getCL_Zero_Inf(lr){
            // =B26/N23*1/60
            //todo need Dose (B26). User entered? In Skyline file?
            return (dose/getInfCumulativeCorrectPartAUC(lr))/60;
        }

        function getCL_Zero_T(){
            // =B26/N21*1/60
            //todo need Dose (B26). User entered? In Skyline file?
            return (dose/getCumulativeCorrectPartAUC(timeRows.length -1))/60;
        }

        function getVdss_Zero_Inf(lr){
            // =(P30*60/1000)*P28
            //todo need Dose (B26). User entered? In Skyline file?
            return (getCL_Zero_Inf(lr)*60/1000)*getMrt_Zero_Inf(lr);
        }

        function getVdss_Zero_T(lr){
            // =(P31*60/1000)*P29
            //todo need Dose (B26). User entered? In Skyline file?
            return (getCL_Zero_T(lr)*60/1000)*getMrt_Zero_T(lr);
        }

        function getT1_2(lr){
            // =LN(2)/B27
            return Math.log(2)/lr.slope;
        }

        function getEffectiveT1_2(lr){
            // =LN(2)*P32/P30*1000/60
            return Math.log(2)*getVdss_Zero_Inf(lr)/getCL_Zero_Inf(lr)*1000/60;
        }
    }(jQuery);
</script>
