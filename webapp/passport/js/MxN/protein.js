/**
 * // Created by Yuval Boss yuval (at) uw.edu | 4/4/16
 */
var barChartData = []; // d3 peptide chart DO NOT MODIFY ONLY SORT
var peakAreaDiv, proteinBarDiv; // viz module div containers

protein =
{
    selectedPrecursor: null,
    settings: null,
    longestPeptide: 0,
    peptides: null,
    precursors: null,
    projects: null,
    features: null,
    sequence: null,
    UI: {
        features: {
            colors: {},
            updateUI: function () {
                if( $('.feature-aa').tooltip().data("tooltipset")) {
                    $('.feature-aa').tooltip('disable');
                    var visibleFeatures = protein.settings.getVisibleFeatures();
                    $('.feature-aa').css('background-color', '#FFF');
                    visibleFeatures.forEach(function(type){
                        $("."+type).tooltip('enable');
                        $("."+type).removeAttr('style');
                        $("."+type).css('background-color', protein.UI.features.colors[type]);
                    });
                }
            },

            initialize: function() {
                if(protein.features == null || protein.features.length === 0)
                    return;
                var allColors = ["#e6194b","#3cb44b","#ffe119","#0082c8","#f58231","#911eb4","#46f0f0","#f032e6","#d2f53c","#fabebe",
                    "#008080","#e6beff","#aa6e28","#fffac8","#800000","#aaffc3","#808000","#ffd8b1","#000080","#000000"];

                // var allFeatures= {
                //     "Chain":"#e6194b",
                //     "Disulfide bond":"",
                //     "Domain":"",
                //     "Glycosylation site":"",
                //     "Lipid moiety-binding region":"",
                //     "Modified residue":"",
                //     "Mutagenesis site":"",
                //     "Region of interest":"",
                //     "Sequence conflict":"",
                //     "Sequence variant":"#f032e6",
                //     "Signal peptide":""};


                var uniqueFeatureTypes = [];
                protein.features.forEach(function(feature) {
                   if(!contains(uniqueFeatureTypes, feature.type))
                       uniqueFeatureTypes.push(feature.type);
                });

                // sort alphabetically
                uniqueFeatureTypes.sort(function(a, b){
                    if(a < b) return -1;
                    if(a > b) return 1;
                    return 0;
                });
                var colorIndex = 0;
                uniqueFeatureTypes.forEach(function(type) {
                    var featureId = "feature-" + type.split(" ").join("");
                    // create colors
                    if(colorIndex < allColors.length -1) {
                        protein.UI.features.colors[featureId] = allColors[colorIndex];
                        colorIndex++;
                    } else {
                        protein.UI.features.colors[featureId] = "#808080"; // if out of colors rest will be grey
                    }


                    // create dom elements
                    var checkbox = document.createElement('input');
                    checkbox.type = "checkbox";
                    checkbox.name = type;
                    checkbox.value = LABKEY.Utils.encodeHtml(featureId);
                    checkbox.id = featureId+"-checkbox";
                    checkbox.className = "featureCheckboxItem";
                    checkbox.setAttribute("color", protein.UI.features.colors[featureId]);

                    var label = document.createElement('Label');
                    label.setAttribute("for",checkbox.id);
                    label.innerHTML = capitalizeFirstLetter(type);
                    label.setAttribute("style", "padding-left: 5px; border-left: 5px solid "+protein.UI.features.colors[featureId] +";");

                    var listItem = document.createElement("li");
                    listItem.className = "featureListItem";

                    listItem.appendChild(checkbox);
                    listItem.appendChild(label);

                    document.getElementById("featuresList").appendChild(listItem);
                });

                // add checkbox change event
                $(".featureCheckboxItem").change(function() {
                    if(this.checked) {
                        protein.settings.addFeatureVisible(this.value);
                        // if all are manually checked
                        if ($('.featureCheckboxItem:checked').length === $('.featureCheckboxItem').length) {
                            $("#showFeatures").prop('checked', true);
                        }
                    } else {
                        protein.settings.removeFeatureVisible(this.value);
                        $("#showFeatures").prop('checked', false);
                    }
                });

                // check/uncheck all features event listener
                $("#showFeatures").change(function() {
                    if(this.checked) {
                        $(".featureCheckboxItem").each(function() {
                            $(this).prop('checked', true).trigger("change");
                        });
                    } else {
                        $(".featureCheckboxItem").prop('checked', false).trigger('change');
                    }
                });

                // tooltip
                $( ".feature-aa" ).tooltip({
                    items: ".feature-aa",
                    tooltipClass:"feature-aa-tooltip",
                    content: function() {
                        var element = $( this );
                        if ( element.attr( "index" ) != null) {
                            var ptm = protein.features[element.attr( "index" )];
                            if(ptm.variation != null) {
                                var text = element.text() + " > "+ptm.variation.toString();
                                if(ptm.description !== "")
                                    return text + "<br /><span style='color:#a6a6a6;'>" + LABKEY.Utils.encodeHtml(ptm.description) + "</span>";
                            }

                            return ptm.type[0].toUpperCase() + ptm.type.slice(1) + "<br /><span style='color:#a6a6a6;'>" + LABKEY.Utils.encodeHtml(ptm.description) + "</span>";
                        }
                    }
                }).data("tooltipset", true);
            }
        }
    },

    getSelectedPrecursor: function() {
        return protein.selectedPrecursor;
    },

    requestSVG: function(chromId, svgParentId) {
        var parentElement = $('#' + svgParentId);
        LABKEY.targetedms.SVGChart.requestAndRenderSVG(chromatogramUrl + "id=" + chromId + "&syncY=true&syncX=false&chartWidth=250&chartHeight=400", parentElement[0], $('#seriesLegend')[0])
    },

    /** precursorChromInfoId can be false to indicate the UI shouldn't scroll, true to scroll to the first chromatogram, or a specific id to scroll to it */
    selectPrecursor: function(precursorId, precursorChromInfoId) {
        if(precursorId == null)
            return;

        if(protein.selectedPrecursor && protein.selectedPrecursor.PrecursorId != null && protein.selectedPrecursor.PrecursorId === precursorId)
            return;

        var precursor = null;
        for (var i = 0; i < protein.precursors.length; i++) {
            if (protein.precursors[i].precursorId === precursorId) {
                precursor = protein.precursors[i];
                break;
            }
        }

        protein.selectedPrecursor = precursor;

        $('#seriesLegend').empty();

        var chromParent = $('#chromatograms');
        chromParent.empty();

        for (var j = 0; j < protein.selectedPrecursor.replicateInfo.length; j++) {
            var replicate = protein.selectedPrecursor.replicateInfo[j];
            var id = 'chrom' + replicate.PrecursorChromInfoId;
            chromParent.append('<a target="id"></a><span id="' + id + '" style="width: 350; height: 400"></span>');
            this.requestSVG(replicate.PrecursorChromInfoId, id);
        }

        $('#selectedPeptideLink').attr("href", showPeptideUrl + "id=" + protein.selectedPrecursor.peptideId);

        if (precursorChromInfoId) {
            if (precursorChromInfoId === true) {
                precursorChromInfoId = protein.selectedPrecursor.replicateInfo[0].PrecursorChromInfoId;
            }
            // Scroll to the chromatogram plot
            window.location.hash = "#chrom" + precursorChromInfoId;
        }
    },

    calcStats: function(rows) {
        var result = LABKEY.vis.Stat.summary(rows, function (row) {
            return row.intensity
        });
        result.sd = LABKEY.vis.Stat.getStdDev(result.sortedValues);
        result.cv = result.Q2 ? (result.sd / result.Q2) : null;
        return result;
    },

    calcMeanCV: function(rows, grouping) {
        var grouped = LABKEY.vis.groupData(rows, function (row) {
            return row[grouping];
        });
        var cvSum = 0;
        var count = 0;
        for (var groupName in grouped) {
            var rowsForGroup = grouped[groupName];
            var stats = this.calcStats(rowsForGroup);
            cvSum += stats.cv;
            count++;
        }
        return cvSum / count;
    },

    initialize: function(data) {

        protein.projects = proteinJSON.projects;
        protein.features = proteinJSON.features;
        protein.sequence = proteinJSON.sequence;

        protein.peptides = [];
        protein.precursors = [];

        var peptideGrouped = LABKEY.vis.groupData(data.rows, function (row) {
            return row.PeptideId;
        });

        for (var peptideId in peptideGrouped) {
            var peptideRows = peptideGrouped[peptideId];


            protein.peptides.push(peptide);
        }

        var precursorGrouped = LABKEY.vis.groupData(data.rows, function (row) {
            return row.PrecursorId;
        });

        for (var precursorId in precursorGrouped) {

            var precursorRows = precursorGrouped[precursorId];

            var charge = precursorRows[0].Charge;
            var mz = precursorRows[0].Mz;

            var precursor = {
                sequence: precursorRows[0].PeptideSequence + ' ' + (charge >= 0 ? '+' : '') + charge + ' ' + mz.toFixed(2),
                replicateInfo: precursorRows
            }

            var totalArea = 0;
            for (var i = 0; i < precursorRows.length; i++) {
                totalArea += precursorRows[i].TotalArea;
            }

            barChartData.push({
                "Sequence": precursor.sequence,
                "Total Area": totalArea,
                "StartIndex": precursorRows[0].StartIndex,
                "EndIndex": precursorRows[0].EndIndex,
                "Enabled": true,
                "PrecursorId": precursorRows[0].PrecursorId,
                "ChromatogramId": precursorRows[0].PrecursorChromInfoId,
                "PeptideId": precursorRows[0].PeptideId
            });

            var timepointGrouped = LABKEY.vis.groupData(precursorRows, function (row) {
                return row.Timepoint;
            });

            for (var timepoint in timepointGrouped) {
                var grouping = 1;
                for (var i = 0; i < timepointGrouped[timepoint].length; i++) {
                    if (timepointGrouped[timepoint][i].Grouping === null) {
                        timepointGrouped[timepoint][i].Grouping = 'Run ' + grouping++;
                    }
                }
            }

            var seq = precursorRows[0].PeptideSequence;
            var precursor = {
                peptideSequence: seq,
                modifiedSequence: precursorRows[0].ModifiedSequence,
                mz: mz,
                peptideId: precursorRows[0].PeptideId,
                precursorId: precursorRows[0].PrecursorId,
                charge: charge,
                abbreviated: seq.substring(0, Math.min(seq.length, 3)) + ' ' + (charge >= 0 ? '+' : '') + charge + ' ' +mz.toFixed(2),
                replicateInfo: precursorRows
            }

            protein.precursors.push(precursor);
        }

        // callback for the chart settings
        // when settings are changed this get called which updates data and UI
        var updateData = function() {
            for(var i = 0; i < barChartData.length; i++) {
                var peptide = barChartData[i];
                var bounds = protein.settings.getSequenceBounds();

                if(peptide["Sequence"].length >= bounds.start && peptide["Sequence"].length <= bounds.end) {
                    peptide["Enabled"] = true;
                } else {
                    peptide["Enabled"] = false;
                    continue;
                }
            }
            var sortBy = protein.settings.getSortBy();
            if (sortBy === "Sequence Location") {
                barChartData.sort(function(a, b) {
                    return a["StartIndex"] - b["StartIndex"];
                });
            }

            var sortValue = "Total Area";
            if (sortBy === "Intensity") {
                barChartData.sort(function(a, b) {
                    return b[sortValue] - a[sortValue];
                });
            }
            $("#livepeptidelist").empty();
            var clipboardPeptides = [];
            barChartData.forEach(function(a) {
                if(a["Enabled"]) {
                    $("#livepeptidelist").append('<li class="'+ a.Sequence+'-text"><span style="color:#A6B890;">&block;&nbsp;</span>'+ LABKEY.Utils.encodeHtml(a.Sequence)+ '</li>');
                    clipboardPeptides.push(a.Sequence) // add to copy clipboard feature
                }
                else
                    $("#livepeptidelist").append('<li class="'+ a.Sequence+'-text"><span style="color:#B9485A;">&block;&nbsp;</span>'+ LABKEY.Utils.encodeHtml(a.Sequence)+ '</li>');
            });
            $("#copytoclipboard").attr("clipboard", clipboardPeptides.join("\r"));

            setFilteredPeptideCount();
            function setFilteredPeptideCount() {
                var activePeptides = 0;
                barChartData.forEach(function(a) {
                    if(a["Enabled"])
                        activePeptides++
                });

                $("#filteredPeptideCount > green").text(activePeptides);
            }
            protein.UI.features.updateUI();
            protein.updateUI();
        };

        protein.settings = new Settings(updateData);

        if(protein.projects != null) {
            for(var i = 0; i < protein.projects.length; i++) {
                new Project(protein.projects[i]);
            }
        }

        longestPeptide = 0;
        barChartData.forEach(function(p) {
            if(p["Sequence"].length > longestPeptide)
                longestPeptide = p["Sequence"].length;
        });

        protein.settings.changeSequenceLength(0, longestPeptide);
        protein.setJqueryEventListeners();
        protein.UI.features.initialize();
        protein.settings.update();
        peakAreaDiv = document.getElementById("chart"); // initial set
        proteinBarDiv = document.getElementById("protein"); // initial set
        if(protein != null && data.rows.length > 0) {
            $(window).resize(function() { protein.updateUI() });
            protein.selectPrecursor(barChartData[0].PrecursorId, false);
        }
    },

    clearElement: function(element) {
        while (element.firstChild) {
            element.removeChild(element.firstChild);
        }
    },

    updateUI: function() {

        var parentWidth = parseInt(d3.select('#intensityChart').style('width'), 10);

        this.clearElement(document.getElementById("intensityChart"));
        this.clearElement(document.getElementById("cvChart"));
        this.clearElement(document.getElementById("cvTableBody"));

        if (this.precursors && this.precursors.length) {
            var plotData = [];
            for (var i = 0; i < this.precursors.length; i++) {
                var abbreviatedSequence = this.precursors[i].abbreviated;
                for (var r = 0; r < this.precursors[i].replicateInfo.length; r++) {
                    var replicateInfo = this.precursors[i].replicateInfo[r];
                    plotData.push({
                        timepoint: replicateInfo.Timepoint,
                        intensity: replicateInfo.TotalArea,
                        grouping: replicateInfo.Grouping,
                        replicate: replicateInfo.Replicate,
                        sequence: this.precursors[i].modifiedSequence,
                        xLabel: abbreviatedSequence,
                        charge: replicateInfo.Charge,
                        mz: replicateInfo.Mz,
                        precursorId: replicateInfo.PrecursorId,
                        peptideId: replicateInfo.peptideId,
                        precursorChromInfoId: replicateInfo.PrecursorChromInfoId
                    });
                }
            }

            var groupedByPrecursorId = LABKEY.vis.groupData(plotData, function (row) {
                return row.precursorId
            });

            var medians = {};
            var cvLineData = [];
            var cvs = {};

            var tableHTML = '';

            for (var precursorId in groupedByPrecursorId) {
                var rowsForPrecursor = groupedByPrecursorId[precursorId];
                var row = rowsForPrecursor[0];
                var fullStats = this.calcStats(rowsForPrecursor);
                medians[precursorId] = fullStats.Q2;

                var timepointCV = this.calcMeanCV(rowsForPrecursor, 'timepoint');
                var groupingCV = this.calcMeanCV(rowsForPrecursor, 'grouping');

                var totalCV = Math.sqrt(timepointCV * timepointCV + groupingCV * groupingCV);

                cvLineData.push({sequence: row.xLabel, precursorId: row.precursorId, StartIndex: row.StartIndex, cv: totalCV, timepoint: 'Total CV'});
                cvLineData.push({sequence: row.xLabel, precursorId: row.precursorId, StartIndex: row.StartIndex, cv: timepointCV, timepoint: 'Intra-day CV'});
                cvLineData.push({sequence: row.xLabel, precursorId: row.precursorId, StartIndex: row.StartIndex, cv: groupingCV, timepoint: 'Inter-day CV'});
                cvs[precursorId] = totalCV;

                tableHTML += '<tr>' +
                        '<td><a href="#chrom' + row.precursorChromInfoId + '" onclick="protein.selectPrecursor(' + precursorId + ', true)">' + LABKEY.Utils.encodeHtml(row.sequence) + '</a></td>' +
                        '<td>' + LABKEY.Utils.encodeHtml((row.charge >= 0 ? '+' : '') + row.charge) + '</td>' +
                        '<td style="text-align: right">' + LABKEY.Utils.encodeHtml(row.mz.toFixed(2)) + '</td>' +
                        '<td style="text-align: right">' + LABKEY.Utils.encodeHtml((groupingCV * 100).toFixed(1)) + '%</td>' +
                        '<td style="text-align: right">' + LABKEY.Utils.encodeHtml((timepointCV * 100).toFixed(1)) + '%</td>' +
                        '<td style="text-align: right">' + LABKEY.Utils.encodeHtml((totalCV * 100).toFixed(1)) + '%</td>' +
                        '<td style="text-align: right">' + LABKEY.Utils.encodeHtml(fullStats.Q2.toExponential(3)) + '</td>' +
                        '<td style="text-align: right">' + LABKEY.Utils.encodeHtml(fullStats.max.toExponential(3)) + '</td>' +
                        '<td style="text-align: right">' + LABKEY.Utils.encodeHtml(fullStats.min.toExponential(3)) + '</td>' +
                        '</tr>';
            }

            document.getElementById('cvTableBody').innerHTML = tableHTML;

            var sortBy = this.settings.getSortBy();
            var sortFunction;
            if (sortBy === "Sequence Location") {
                sortFunction = function (a, b) { return a.StartIndex - b.StartIndex };
            }

            if (sortBy === "Intensity") {
                sortFunction = function (a, b) {
                    return medians[b.precursorId] - medians[a.precursorId];
                };
            }

            if (sortBy === "Coefficient of Variation") {
                sortFunction = function (a, b) {
                    return cvs[b.precursorId] - cvs[a.precursorId];
                };
            }

            plotData.sort(sortFunction);
            cvLineData.sort(sortFunction);

            var s = this;

            var pointLayer = new LABKEY.vis.Layer({
                geom: new LABKEY.vis.Geom.Point({
                    position: 'sequential',
                    color: 'timepoint',
                    shape: 'grouping',
                    opacity: 0.6,
                    size: 4
                }),
                aes: {
                    hoverText: function (row) {
                        return row.intensity.toExponential(3) + ' ' + row.timepoint + ' ' + row.grouping;
                    },
                    pointClickFn: function(event, row) {
                        s.selectPrecursor(row.precursorId, row.precursorChromInfoId);
                    }
                }
            });

            var boxLayer = new LABKEY.vis.Layer({
                geom: new LABKEY.vis.Geom.Boxplot({
                    showOutliers: false,
                    opacity: 0.8,
                    clickFn: function(event, row) {
                        for (var i = 0; i < protein.precursors.length; i++) {
                            if (protein.precursors[i].abbreviated === row.name) {
                                s.selectPrecursor(protein.precursors[i].precursorId, true);
                            }
                        }
                    }
                }),
                aes: {
                    hoverText: function (x, stats) {
                        var sd = LABKEY.vis.Stat.getStdDev(stats.sortedValues);

                        return 'Peptide: ' + x +
                                '\nMin: ' + stats.min.toExponential(3) +
                                '\nMax: ' + stats.max.toExponential(3) +
                                '\nMean: ' + stats.Q2.toExponential(3) +
                                '\nStd dev: ' + sd.toExponential(3) +
                                '\n%CV: ' + (stats.Q2 ? ((sd / stats.Q2) * 100).toFixed(1) : 'N/A');
                    }
                }
            });

            var plot = new LABKEY.vis.Plot({
                renderTo: 'intensityChart',
                rendererType: 'd3',
                clipRect: true,
                width: Math.min(parentWidth, 250 * (this.precursors.length + 1)),
                height: 400,
                gridLineColor: '#777777',
                labels: {
                    yLeft: {value: 'Peak Area'},
                    yRight: {value: '% Coefficient of Variation'}
                },
                data: plotData,
                gridLinesVisible: 'none',
                layers: [boxLayer, pointLayer],
                aes: {
                    yLeft: 'intensity',
                    x: 'xLabel',
                    color: 'timepoint',
                    shape: 'grouping'
                },
                scales: {
                    x: {
                        scaleType: 'discrete'
                    },
                    yLeft: {
                        scaleType: 'continuous',
                        trans: 'linear',
                        tickFormat: function (d) {
                            if (d === 0)
                                return 0;
                            return d.toExponential();
                        }
                    }
                }
            });

            plot.render();

            var plot2 = new LABKEY.vis.BarPlot({
                renderTo: 'cvChart',
                rendererType: 'd3',
                width: Math.min(parentWidth, 250 * (this.precursors.length + 1)),
                height: 400,
                labels: {
                    yLeft: {value: '% Coefficient of Variation'}
                },
                options: {
                    clickFn: function(event, row) {
                        for (var i = 0; i < protein.precursors.length; i++) {
                            if (protein.precursors[i].abbreviated === row.subLabel) {
                                s.selectPrecursor(protein.precursors[i].precursorId, true);
                            }
                        }
                    }
                },
                data: cvLineData,
                gridLinesVisible: 'none',
                aes: {
                    y: 'cv',
                    x: 'timepoint',
                    xSub: 'sequence',
                },
                scales: {
                    x: {
                        scaleType: 'discrete',
                    },
                    y: {
                        scaleType: 'continuous',
                        trans: 'linear',
                        tickFormat: function (d) {
                            if (d === 0)
                                return 0;
                            return (d * 100).toFixed(0);
                        }
                    }
                }
            });

            plot2.render();
        }

    },

    // Sets listeners of dom objects that need listeners
    setJqueryEventListeners: function() {
        $(".feature-aa").click(function(){
            var index = $(this).attr("index");
        });

        // copy to clipboard action
        $( "#copytoclipboard" ).click(function(d) {
            copyTextToClipboard($( "#copytoclipboard" ).attr("clipboard"))
        });

        // filter reset button action
        $( "#formreset" ).click(function() {
            // reset combo box 'Sort By'
            $('#peptideSort').prop('selectedIndex', 0);
            $('#peptideSort').trigger('change');
            // reset degradation slider
            var $rangesliderdeg = $("#rangesliderdeg");
            $rangesliderdeg.slider("values", 0, 0);
            $rangesliderdeg.slider("values", 1, 100);
            $("#filterdeg").val("0% - 100%");
            protein.settings.changeDegradation(0, 100)
            // reset peptide length slider
            var $rangesliderlength = $("#rangesliderlength");
            $rangesliderlength.slider("values", 0, 0);
            $rangesliderlength.slider("values", 1, longestPeptide);
            $("#filterpeplength").val("0 - " + longestPeptide);
            protein.settings.changeSequenceLength(0, longestPeptide);
            // reset checkbox
            $('#showFeatures').prop('checked', false).trigger("change");
        });
        // initialize sliding ranger bar in Filter Options
        $(function () {
            $("#rangesliderdeg").slider({
                range: true,
                min: 0,
                max: 100,
                values: [0, 100],
                slide: function (event, ui) {
                    $("#filterdeg").val(ui.values[0] + "% - " + ui.values[1] + "%");
                    protein.settings.changeDegradation(ui.values[0], ui.values[1])
                }
            });
            $("#filterdeg").val($("#rangesliderdeg").slider("values", 0) +
                    "% - " + $("#rangesliderdeg").slider("values", 1) + "%");

            $("#rangesliderlength").slider({
                range: true,
                min: 0,
                max: longestPeptide,
                values: [0, longestPeptide],
                slide: function (event, ui) {
                    $("#filterpeplength").val(ui.values[0] + " - " + ui.values[1]);
                    protein.settings.changeSequenceLength(ui.values[0], ui.values[1])
                }
            });
            $("#filterpeplength").val($("#rangesliderlength").slider("values", 0) +
                    " - " + $("#rangesliderlength").slider("values", 1));
        });
    }
};