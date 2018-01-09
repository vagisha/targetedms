

<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>

<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("targetedms/css/Pharmacokinetics.css");
        dependencies.add("internal/jQuery");
        dependencies.add("vis/vis");
    }
%>
<style type="text/css">
    .pk-table-stats tbody tr td {
        border : solid 1px lightgrey;
    }
    .pk-table-stats {
        display: inline;
        vertical-align: top;
        padding-right: 25px;
        width: 500px;
    }
    .pk-table-stat{
        width: 100px;
        text-align: right;
    }

    .pk-table-stat{
        width: 100px;
        text-align: right;
    }
</style>
<div class="container-fluid targetedms-pk">
    <div id="targetedms-pk-export" class="export-icon" data-toggle="tooltip" title="Export to Excel">
        <i class="fa fa-file-excel-o" onclick="exportExcel()"></i>
    </div>
    <h3 id="pk-title1"></h3>
    <h4 id="pk-title2"></h4>
    <br>
    <hr>

<labkey:panel >

    <table id="pk-table-input" class="table table-striped table-responsive pk-table-stats"  >
        <thead><tr><td>Time</td><td>C0</td><td>Terminal</td><td>Concentration</td></tr></thead>
    </table>
    <table id="pk-table-stats" class="table table-striped table-responsive pk-table-stats" style="width: 600px">
        <thead><tr><td colspan="3">Statistic</td></tr></thead>
        <tr><td class="pk-table-label">IV CO           </td><td id='IVCO'              class="pk-table-stat"></td><td></td></tr>
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
    <div id="chart"></div><div id="chartLog"></div>
    <table id="pk-table-standard" class="table table-striped table-responsive pk-table">
        <thead id="standard-header" />
        <tbody id="standard-body" />
    </table>
    <hr>
    <table id="pk-table-qc" class="table table-striped table-responsive pk-table">
        <thead id="qc-header" />
        <tbody id="qc-body" />
    </table>
    <div id="chart"></div><div id="chartLog"></div>

</div>
<script type="application/javascript">
    +function ($) {

        var params = LABKEY.ActionURL.getParameters();

        this.moleculeId = params['GeneralMoleculeId'];
        var peptide='';
        var fileName='';
        var timeRows=[];

        //These are the values from the sample spreadsheet, sheet IVPO, refenced in
        //Spec ID: 31940 Panorama Partners - Figures of merit and PK calcs
        var timeRowsMock=[
            {time: 0 , conc :  0.006689              }
            ,{time: .133333333 , conc :  0.006184700 }
            ,{time: 0.25       , conc :  0.006025200 }
            ,{time: 0.5        , conc :  0.004984600 }
            ,{time: 0.75       , conc :  0.003525100 }
            ,{time: 1          , conc :  0.004663500 }
            ,{time: 1.5        , conc :  0.002800500 }
            ,{time: 2          , conc :  0.001523200 }
            ,{time: 2.5        , conc :  0.000978510 }
            ,{time: 3          , conc :  0.000924980 }
            ,{time: 3.5        , conc :  0.000632570 }
            ,{time: 4          , conc :  0.000447090 }
            ,{time: 4.5        , conc :  0.000438690 }
            ,{time: 5          , conc :  0.000209690 }
            ,{time: 5.5        , conc :  0.000260050 }
            ,{time: 6          , conc :  0.000158830 }
            ,{time: 7          , conc :  0.000124520 }
            ,{time: 8          , conc :  0.000082918 }];

        var dose = .53;



        function parseRawData(data) {
            // Used for development to load values from sample spreadsheet
            // $.each(timeRowsMock, function(index, timeRow){
            //     const item = {
            //                 time: timeRow.time,
            //                 conc: timeRow.conc
            //             }
            //         item.pkConc = item.conc;//todo add logic base on "IV" for first conc?
            //         item.inCp = Math.log(item.pkConc);
            //         item.concxt = item.time * item.pkConc;
            //     timeRows.push(
            //             item
            //     )
            // })
            //
            $.each(data.rows, function(index, timeRow){
                console.log(timeRow);
                peptide = timeRow.Peptide;
                fileName = timeRow.FileName;
                const item = {
                            time: timeRow.Time,
                            conc: timeRow.Concentration
                        }
                    item.pkConc = item.conc;//todo add logic base on "IV" for first conc?
                    item.inCp = Math.log(item.pkConc);
                    item.concxt = item.time * item.pkConc;
                timeRows.push(
                        item
                )
            })
            $('#pk-title1').html("Peptide: " + peptide);
            $('#pk-title2').html("Skyline File: " + fileName);
            //todo set default C0 and Terminal value(s)
            $.each(timeRows,function (index, row) {
                console.log(row);
                var checkedC0;
                if(index ===0 ) {checkedC0='checked';}
                var checkedT;
                if(index === timeRows.length - 1) {checkedT='checked';}

                $("<tr>" +
                        "<td class='pk-table-stat'>" + row.time + "</td>" +
                        "<td ><input type='checkbox' rowIndex= " + index + " " + checkedC0 + " class='checkboxC0' /></td>" +
                        "<td ><input type='checkbox' rowIndex= " + index + " " + checkedT + " class='terminal' /></td>" +
                        "<td class='pk-table-stat'>" + statRound(row.conc) + "</td>" +
                        "</tr>")
                        .appendTo("#pk-table-input");
                $("<tr>" +
                        "<td>" + row.time + "</td>" +
                        "<td>" + LABKEY.Utils.roundNumber(row.conc,6) + "</td>" +
                        "<td>" + LABKEY.Utils.roundNumber(row.pkConc,6) + "</td>" +
                        "<td>" + LABKEY.Utils.roundNumber(row.inCp,4) + "</td>" +
                        "<td>" + LABKEY.Utils.roundNumber(getLinDeltaAUC(index),8) + "</td>" +
                        "<td>" + LABKEY.Utils.roundNumber(getAUCuMhrL(index),8) + "</td>" +
                        "<td>" + LABKEY.Utils.roundNumber(getLogLinDeltaAUC(index),8) + "</td>" +
                        "<td>" + LABKEY.Utils.roundNumber(getLogLinAUCuMhrL(index),8) + "</td>" +
                        "<td>" + LABKEY.Utils.roundNumber(getCorrectPartAUC(index),8) + "</td>" +
                        "<td>" + LABKEY.Utils.roundNumber(getCumulativeCorrectPartAUC(index),8) + "</td>" +
                        "<td>" + LABKEY.Utils.roundNumber(row.concxt,8) + "</td>" +
                        "<td>" + LABKEY.Utils.roundNumber(getLinDeltaAUMC(index),8) + "</td>" +
                        "<td>" + LABKEY.Utils.roundNumber(getCumulativeLinAUMC(index),8) + "</td>" +
                        "<td>" + LABKEY.Utils.roundNumber(getLogLinDeltaAUMC(index),8) + "</td>" +
                        "<td>" + LABKEY.Utils.roundNumber(getCumulativeLogLinAUMC(index),8) + "</td>" +
                        "<td>" + LABKEY.Utils.roundNumber(getCorrectDeltaAUMC(index),8) + "</td>" +
                        "<td>" + LABKEY.Utils.roundNumber(getCorrectAUMC(index),8) + "</td>" +
                        "</tr>")
                        .appendTo("#standard-body");

            })
            $("<tr>" +"<td>inf</td><td ></td><td  id='infLinDeltaAUC'>" +
                    "</td><td  id='infAUCuMhrL'></td><td ></td><td id='infLogLinAUCuMhrL'></td><td></td>" +
                    "<td id='infCumulativeCorrectPartAUC'></td><td></td><td id='infLinDeltaAUMC'></td>" +
                    "<td id='infCumulativeLinAUMC'></td><td></td><td id='infCumulativeLogLinAUMC'></td><td></td>" +
                    "<td id='infCorrectAUMC'></td>" +
                    "</tr>").appendTo("#standard-body");
            updateStats(checkBoxC0);
            updateStats(checkBoxTerminal);
        }

        function statRound(value){
            if(!value){
                return '';
            }
            return LABKEY.Utils.roundNumber(value,3).toFixed(3);
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
            x = [];
            y = [];
            $(timeFrame + ":checked").each(function (index, box) {
                var row = timeRows[box.getAttribute('rowIndex')];
                x.push(row.time);
                y.push(row.inCp);
            })
            lr = getLinearRegression(y, x);
            if(timeFrame === checkBoxC0)
            {
                $('#IVCO').html(LABKEY.Utils.roundNumber(lr.intercept,3)); $('#IVCOEXP').html(LABKEY.Utils.roundNumber((Math.exp(lr.intercept)),4));
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
                })

            LABKEY.Query.selectRows( {
                schemaName: 'targetedms',
                queryName: 'Pharmacokinetics',
                filterArray: [LABKEY.Filter.create('MoleculeId', params['GeneralMoleculeId'])],
                sort : 'Time',
                scope: this,
                success: function (data) {
                    parseRawData(data);
                    showCharts();
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
            })
            return headers;
        }

        function getTableRows(tableId){
            var myRows = [];
            $(tableId + " tr").each(function(index) {
                $cells = $(this).find("td");
                var row = [];
                $cells.each(function() {
                    if(this.hasChildNodes() && this.firstChild.hasAttribute && this.firstChild.checked){
                        row.push('x')
                    }else {
                        row.push(this.innerText);
                    }
                });
                myRows.push(row);
            });
            return myRows;
        }

        this.exportExcel = function() {

            var sheet1Data = [
                    ['Peptide: ', peptide],
                    ['Skyline File: ', fileName],
            ];
            sheet1Data.push(['','']);
            sheet1Data.push(['AUC Calculation','']);
            sheet1Data.push(['','']);

            getTableRows("#pk-table-stats").forEach(function(row){
                sheet1Data.push(row);
            })
            sheet1Data.push(['','']);
            getTableRows("#pk-table-input").forEach(function(row){
                sheet1Data.push(row);
            })

            var sheet2Data = [getTableHeaders()];

             getTableRows("#standard-body").forEach(function(row){
                 sheet2Data.push(row);
             })

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
                    scaleType: 'continuous',
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
        //refenced in Spec ID: 31940 Panorama Partners - Figures of merit and PK calcs
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
            return ((timeRow.time-timeRowPrevious.time)*(timeRowPrevious.pkConc - timeRow.pkConc))/(timeRowPrevious.inCp-timeRow.inCp)
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

        function getMrt_Zero_T(lr){
            // =U21/N21
            return getCorrectAUMC(timeRows.length -1)/getCumulativeCorrectPartAUC(timeRows.length -1);
        }

        function getCL_Zero_Inf(lr){
            // =B26/N23*1/60
            //todo need Dose (B26). User entered? In Skyline file?
            return (dose/getInfCumulativeCorrectPartAUC(lr))/60;
        }

        function getCL_Zero_T(lr){
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
