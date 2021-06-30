protein =
{
    selectedPrecursor: null,
    settings: null,
    longestPeptide: 0,
    precursors: null,
    projects: null,
    features: null,
    sequence: null,
    refreshTimer: null,

    /** precursorChromInfoId can be false to indicate the UI shouldn't scroll, true to scroll to the first chromatogram, or a specific id to scroll to it */
    selectPrecursor: function(precursorId, precursorChromInfoId) {
        if(precursorId == null)
            return;

        if(protein.selectedPrecursor && protein.selectedPrecursor.PrecursorId != null && protein.selectedPrecursor.PrecursorId === precursorId)
            return;

        let precursor = null;
        for (let i = 0; i < protein.precursors.length; i++) {
            if (protein.precursors[i].PrecursorId === precursorId) {
                precursor = protein.precursors[i];
                break;
            }
        }

        protein.selectedPrecursor = precursor;

        $('#seriesLegend').empty();

        const chromParent = $('#chromatograms');
        chromParent.empty();

        const calCurveElement = $('#calibrationCurve');
        calCurveElement.empty();

        const figuresOfMeritElement = $('#figuresOfMerit');
        figuresOfMeritElement.empty();

        let currentTimepoint = null;
        let childHtml = '<table class="chromatogramGrid"><tr><td/>';

        var groupings = [];

        protein.selectedPrecursor.ReplicateInfo.forEach(function(replicate) {
            if (groupings.indexOf(replicate.Grouping) === -1) {
                groupings.push(replicate.Grouping);
            }
        });

        groupings.forEach(function(grouping) {
            childHtml += '<td style="text-align: center; font-weight: bold">' + LABKEY.Utils.encodeHtml(grouping) + '</td>'
        });

        childHtml += '</tr><tr>';

        currentTimepoint = null;
        protein.selectedPrecursor.ReplicateInfo.forEach(function(replicate) {
            if (replicate.Timepoint !== currentTimepoint) {
                // Wrap to the next line
                currentTimepoint = replicate.Timepoint;
                childHtml += '</tr><tr><td style="font-weight: bold;">' + LABKEY.Utils.encodeHtml(currentTimepoint) + '</td>';
            }

            const id = 'chrom' + replicate.PrecursorChromInfoId;
            childHtml += '<td><a target="' + id + '"></a><span id="' + id + '" style="width: 275px; height: 315px"></span></td>\n';
        });

        childHtml += '</tr><tr><td colspan="' + (groupings.length + 1) + '"><div id="seriesLegend" style="width: 100%"></div>' +
                '<div style="text-align: center"><a href="' + showPeptideUrl + 'id=' + protein.selectedPrecursor.PeptideId + '">Show peptide details</a></div></td></tr></table>';
        chromParent.append(childHtml);

        $('a[name*=\'Chromatograms\'] > span').text('Chromatograms for ' + precursor.FullDescription);

        protein.selectedPrecursor.ReplicateInfo.forEach(function(replicate) {
            const parentElement = $('#chrom' + replicate.PrecursorChromInfoId);
            LABKEY.targetedms.SVGChart.requestAndRenderSVG(chromatogramUrl + "id=" + replicate.PrecursorChromInfoId + "&syncY=true&syncX=false&chartWidth=275&chartHeight=300",
                    parentElement[0],
                    $('#seriesLegend')[0],
                    false,
                    false
            );
        });

        if (precursorChromInfoId) {
            if (precursorChromInfoId === true) {
                precursorChromInfoId = protein.selectedPrecursor.ReplicateInfo[0].PrecursorChromInfoId;
            }
            // Scroll to the chromatogram plot
            window.location.hash = "#chrom" + precursorChromInfoId;
        }

        if (protein.selectedPrecursor.CalibrationCurveId) {
            new LABKEY.WebPart({
                partName: 'Targeted MS Calibration Curve',
                renderTo: 'calibrationCurveDiv',
                frame: 'portal',
                titleHref: LABKEY.ActionURL.buildURL('targetedms', 'showCalibrationCurve', LABKEY.ActionURL.getContainer(), {calibrationCurveId: precursor.CalibrationCurveId}),
                partConfig: {
                    calibrationCurveId: precursor.CalibrationCurveId
                }
            }).render();

            new LABKEY.WebPart({
                partName: 'Targeted MS Figures of Merit',
                renderTo: 'figuresOfMeritDiv',
                frame: 'portal',
                titleHref: LABKEY.ActionURL.buildURL('targetedms', 'showFiguresOfMerit', LABKEY.ActionURL.getContainer(), {generalMoleculeId: precursor.PeptideId}),
                partConfig: {
                    generalMoleculeId: precursor.PeptideId
                }
            }).render();
        }
    },

    calcStats: function(rows, getter) {
        const result = LABKEY.vis.Stat.summary(rows, getter);
        result.sd = LABKEY.vis.Stat.getStdDev(result.sortedValues);
        result.mean = LABKEY.vis.Stat.getMean(result.sortedValues);
        result.cv = result.Q2 ? (result.sd / result.Q2) : null;
        return result;
    },

    calcMeanCV: function(rows, grouping) {
        const grouped = LABKEY.vis.groupData(rows, function (row) {
            return row[grouping];
        });
        let cvSum = 0;
        let count = 0;
        const cvs = [];

        for (let groupName in grouped) {
            if (grouped.hasOwnProperty(groupName)) {
                const rowsForGroup = grouped[groupName];
                const stats = this.calcStats(rowsForGroup, function (row) {
                    return row[protein.settings.areaProperty];
                });
                cvs.push(stats.cv);
                cvSum += stats.cv;
                count++;
            }
        }
        const summary = this.calcStats(cvs, function (value) {
            return value;
        });
        mean = cvSum / count;
        return summary;
    },

    // Render an integer charge value to a string
    renderCharge: function(charge) {
        if (charge === 1) {
            return '+';
        }
        else if (charge === 2) {
            return '++';
        }
        else if (charge === 3) {
            return '+++';
        }
        else if (charge === -1) {
            return '-';
        }
        else if (charge === -2) {
            return '--';
        }
        else if (charge === -3) {
            return '---';
        }
        return (charge >= 0 ? '+' : '') + charge;
    },

    initialize: function(data) {

        protein.projects = proteinJSON.projects;
        protein.features = proteinJSON.features;
        protein.sequence = proteinJSON.sequence;
        protein.preferredname = proteinJSON.preferredname;

        protein.precursors = [];

        // Abbreviate the peptide sequences like Skyline
        data.rows.forEach(function(row) {
            row.dataType = 'Peptide';
            row.fragment = row.ModifiedSequence;
        }, this);
        LABKEY.targetedms.QCPlotLegendHelper.setupLegendPrefixes(data.rows, 3);

        const precursorGrouped = LABKEY.vis.groupData(data.rows, function (row) {
            return row.PrecursorId;
        });

        let hasCalibratedArea = false;
        let hasNormalizedArea = false;

        Object.keys(precursorGrouped).forEach(function(precursorId ) {
            const precursorRows = precursorGrouped[precursorId];
            const precursor = precursorRows[0];

            const precursorData = {
                Sequence: precursor.PeptideSequence,
                FullDescription: precursor.ModifiedSequence + ' ' + protein.renderCharge(precursor.Charge) + ', ' + precursor.Mz.toFixed(4),
                TotalArea: 0,
                CalibratedArea: 0,
                NormalizedArea: 0,
                StartIndex: precursor.StartIndex,
                EndIndex: precursor.EndIndex,
                Enabled: true,
                PrecursorId: precursor.PrecursorId,
                ChromatogramId: precursor.PrecursorChromInfoId,
                PeptideId: precursor.PeptideId,
                CalibrationCurveId: precursor.CalibrationCurveId,

                ModifiedSequence: precursor.ModifiedSequence,
                AbbreviatedLabel: LABKEY.targetedms.QCPlotLegendHelper.getUniquePrefix(precursor.fragment, true) + ' ' + protein.renderCharge(precursor.Charge),
                ReplicateInfo: precursorRows
            };

            // Sum the total area across replicates
            precursorRows.forEach(function(precursorRow) {
                precursorData.TotalArea += precursorRow.TotalArea;
                precursorData.CalibratedArea += precursorRow.CalibratedArea;
                precursorData.NormalizedArea += precursorRow.NormalizedArea;
                hasCalibratedArea = hasCalibratedArea || precursorRow.CalibratedArea;
                hasNormalizedArea = hasNormalizedArea || precursorRow.NormalizedArea;
            });

            const timepointGrouped = LABKEY.vis.groupData(precursorRows, function (row) {
                return row.Timepoint;
            });

            Object.keys(timepointGrouped).forEach(function(timepoint) {
                let grouping = 1;
                for (let i = 0; i < timepointGrouped[timepoint].length; i++) {
                    if (timepointGrouped[timepoint][i].Grouping === null) {
                        timepointGrouped[timepoint][i].Grouping = 'Run ' + grouping++;
                    }
                }
            });

            protein.precursors.push(precursorData);
        });

        const refreshFunction = function () {
            protein.updateUI();
        };

        // callback for the chart settings
        // when settings are changed this get called which updates data and UI
        const updateData = function () {
            for (let i = 0; i < protein.precursors.length; i++) {
                const peptide = protein.precursors[i];
                const bounds = protein.settings.getSequenceBounds();

                peptide.Enabled = peptide.Sequence.length >= bounds.start && peptide.Sequence.length <= bounds.end;
            }
            const sortBy = protein.settings.getSortBy();
            if (sortBy === "Sequence Location") {
                protein.precursors.sort(function (a, b) {
                    return a.StartIndex - b.StartIndex;
                });
            }

            const sortValue = "Area";
            if (sortBy === "Intensity") {
                protein.precursors.sort(function (a, b) {
                    return b[sortValue] - a[sortValue];
                });
            }
            const clipboardPeptides = [];
            protein.precursors.forEach(function (a) {
                if (a.Enabled) {
                    clipboardPeptides.push(a.Sequence) // add to copy clipboard feature
                }
            });
            $("#copytoclipboard").attr("clipboard", clipboardPeptides.join("\r"));

            setFilteredPeptideCount();

            function setFilteredPeptideCount() {
                let activePeptides = 0;
                protein.precursors.forEach(function (a) {
                    if (a.Enabled)
                        activePeptides++
                });

                $("#filteredPeptideCount > green").text(activePeptides);
            }

            // Coalesce updates to the rest of the plot because the slider can rapidly fire many updates
            if (protein.refreshTimer) {
                window.clearTimeout(protein.refreshTimer);
            }

            protein.refreshTimer = window.setTimeout(refreshFunction, 500);
        };

        protein.settings = new Settings(updateData);
        protein.settings.hasCalibratedArea = hasCalibratedArea;
        protein.settings.hasNormalizedArea = hasNormalizedArea;

        // Fall back to normalized or total if we don't have other options
        if (!hasCalibratedArea) {
            $('#valueType').val(hasNormalizedArea ? 'normalizedArea' : 'totalArea')
        }

        $('#totalCVCheckbox').change(refreshFunction);
        $('#intraCVCheckbox').change(refreshFunction);
        $('#interCVCheckbox').change(refreshFunction);
        $("#valueType").change(refreshFunction);

        protein.longestPeptide = 0;
        protein.precursors.forEach(function(p) {
            if(p.Sequence.length > protein.longestPeptide)
                protein.longestPeptide = p.Sequence.length;
        });

        protein.settings.changeSequenceLength(0, protein.longestPeptide);
        protein.setJqueryEventListeners();
        protein.settings.update();
        if(protein != null && data.rows.length > 0) {
            $(window).resize(function() { protein.updateUI() });
            protein.selectPrecursor(protein.precursors[0].PrecursorId, false);
        }
    },

    clearElement: function(element) {
        while (element.firstChild) {
            element.removeChild(element.firstChild);
        }
    },

    updateUI: function() {

        const parentWidth = window.innerWidth - 80;

        this.clearElement(document.getElementById("intensityChart"));
        this.clearElement(document.getElementById("cvChart"));
        this.clearElement(document.getElementById("cvTableBody"));

        if (this.precursors && this.precursors.length) {
            let plotData = [];
            this.precursors.forEach(function(precursor) {

                let enabled = true;
                protein.precursors.forEach(function(pep) {
                    if (pep.PeptideId === precursor.PeptideId) {
                        enabled = pep.Enabled;
                    }
                });

                precursor.ReplicateInfo.forEach(function(replicateInfo) {
                    plotData.push({
                        timepoint: replicateInfo.Timepoint,
                        totalArea: replicateInfo.TotalArea,
                        calibratedArea: replicateInfo.CalibratedArea,
                        normalizedArea: replicateInfo.NormalizedArea,
                        grouping: replicateInfo.Grouping,
                        replicate: replicateInfo.Replicate,
                        sequence: precursor.ModifiedSequence,
                        peptideSequence: precursor.Sequence,
                        xLabel: precursor.AbbreviatedLabel,
                        charge: replicateInfo.Charge,
                        mz: replicateInfo.Mz,
                        precursorId: replicateInfo.PrecursorId,
                        peptideId: replicateInfo.PeptideId,
                        startIndex: replicateInfo.StartIndex,
                        precursorChromInfoId: replicateInfo.PrecursorChromInfoId,
                        enabled: precursor.Enabled
                    });
                });
            });

            const groupedByPrecursorId = LABKEY.vis.groupData(plotData, function (row) {
                return row.precursorId
            });

            const medians = {};
            const cvLineData = [];
            const summaryDataTable = [];
            const cvs = {};

            const showTotal = $('#totalCVCheckbox')[0].checked;
            const showIntra = $('#intraCVCheckbox')[0].checked;
            const showInter = $('#interCVCheckbox')[0].checked;

            protein.settings.areaProperty = $('#valueType')[0].value;

            if (protein.settings.areaProperty === 'calibratedArea' && !protein.settings.hasCalibratedArea) {
                $('#noCalibratedValuesError').css('display', '');
                protein.settings.areaProperty = 'totalArea';
            }
            else {
                $('#noCalibratedValuesError').css('display', 'none');
            }

            if (protein.settings.areaProperty === 'normalizedArea' && !protein.settings.hasNormalizedArea) {
                $('#noNormalizedValuesError').css('display', '');
                protein.settings.areaProperty = 'totalArea';
            }
            else {
                $('#noNormalizedValuesError').css('display', 'none');
            }

            Object.keys(groupedByPrecursorId).forEach(function(precursorId) {
                const rowsForPrecursor = groupedByPrecursorId[precursorId];
                const row = rowsForPrecursor[0];
                const fullStats = protein.calcStats(rowsForPrecursor, function (x) {
                    return x[protein.settings.areaProperty];
                });
                medians[precursorId] = fullStats.Q2;

                const timepointCV = protein.calcMeanCV(rowsForPrecursor, 'timepoint');
                const groupingCV = protein.calcMeanCV(rowsForPrecursor, 'grouping');

                const totalCV = Math.sqrt(timepointCV.mean * timepointCV.mean + groupingCV.mean * groupingCV.mean) * 100;

                if (row.enabled) {
                    let sharedValues = {
                        sequence: row.xLabel,
                        fullDescription: row.sequence + ' ' + protein.renderCharge(row.charge) + ', ' + row.mz.toFixed(4),
                        precursorId: row.precursorId
                    }

                    if (showTotal) {
                        cvLineData.push({
                            ...sharedValues,
                            cvMean: totalCV,
                            cvType: 'Total CV'
                        });
                    }
                    if (showIntra) {
                        cvLineData.push({
                            ...sharedValues,
                            cvMean: timepointCV.mean * 100,
                            cvStdDev: timepointCV.sd * 100,
                            cvType: 'Average intra-day CV'
                        });
                    }
                    if (showInter) {
                        cvLineData.push({
                            ...sharedValues,
                            cvMean: groupingCV.mean * 100,
                            cvStdDev: groupingCV.sd * 100,
                            cvType: 'Average inter-day CV'
                        });
                    }
                }
                cvs[precursorId] = totalCV;

                summaryDataTable.push({
                    precursorChromInfoId: row.precursorChromInfoId,
                    precursorId: precursorId,
                    sequence: row.sequence,
                    peptideSequence: row.peptideSequence,
                    charge: row.charge,
                    mz: row.mz,
                    StartIndex: row.startIndex,
                    groupingCV: groupingCV.mean,
                    totalCV: totalCV,
                    timepointCV: timepointCV.mean,
                    fullStats: fullStats,
                    enabled: row.enabled
                });
            });

            const sortBy = protein.settings.getSortBy();
            let sortFunction;
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

            plotData = plotData.filter(row => row.enabled);

            plotData.sort(sortFunction);
            cvLineData.sort(sortFunction);
            summaryDataTable.sort(sortFunction);

            let tableHTML = '';
            summaryDataTable.forEach(function(row) {
                tableHTML += '<tr' + (row.enabled ? '' : ' style="text-decoration: line-through; background-color: LightGray"') + '>' +
                        '<td colspan="2"><a href="#chrom' + row.precursorChromInfoId + '" onclick="protein.selectPrecursor(' + row.precursorId + ', true)">' + LABKEY.Utils.encodeHtml(row.sequence) + '</a></td>' +
                        '<td>' + LABKEY.Utils.encodeHtml((row.charge >= 0 ? '+' : '') + row.charge) + '</td>' +
                        '<td style="text-align: right">' + LABKEY.Utils.encodeHtml(row.mz.toFixed(4)) + '</td>' +
                        '<td style="text-align: right">' + (row.StartIndex ? row.StartIndex : '') + '</td>' +
                        '<td style="text-align: right">' + row.peptideSequence.length + '</td>' +
                        '<td style="text-align: right; ' + (row.groupingCV > 0.2 ? 'color: darkred; font-weight: bold' : '') + '">' + LABKEY.Utils.encodeHtml((row.groupingCV * 100).toFixed(1)) + '%</td>' +
                        '<td style="text-align: right; ' + (row.timepointCV > 0.2 ? 'color: darkred; font-weight: bold' : '') + '">' + LABKEY.Utils.encodeHtml((row.timepointCV * 100).toFixed(1)) + '%</td>' +
                        '<td style="text-align: right; ' + (row.totalCV > 0.2 ? 'color: darkred; font-weight: bold' : '') + '">' + LABKEY.Utils.encodeHtml((row.totalCV).toFixed(1)) + '%</td>' +
                        '<td style="text-align: right">' + LABKEY.Utils.encodeHtml(row.fullStats.Q2.toExponential(3)) + '</td>' +
                        '<td style="text-align: right">' + LABKEY.Utils.encodeHtml(row.fullStats.max.toExponential(3)) + '</td>' +
                        '<td style="text-align: right">' + LABKEY.Utils.encodeHtml(row.fullStats.min.toExponential(3)) + '</td>' +
                        '</tr>';
            });
            document.getElementById('cvTableBody').innerHTML = tableHTML;


            const pointLayer = new LABKEY.vis.Layer({
                geom: new LABKEY.vis.Geom.Point({
                    position: 'sequential',
                    color: 'timepoint',
                    shape: 'grouping',
                    opacity: 0.6,
                    size: 4
                }),
                aes: {
                    hoverText: function (row) {
                        return row[protein.settings.areaProperty].toExponential(3) + '\n' +
                                row.sequence + protein.renderCharge(row.charge) + ', ' + row.mz.toFixed(4) + '\n' +
                                'Timepoint: ' + row.timepoint + '\n' +
                                'Grouping: ' + row.grouping;
                    },
                    pointClickFn: function (event, row) {
                        protein.selectPrecursor(row.precursorId, row.precursorChromInfoId);
                    }
                }
            });

            const boxLayer = new LABKEY.vis.Layer({
                geom: new LABKEY.vis.Geom.Boxplot({
                    showOutliers: false,
                    opacity: 0.8,
                    clickFn: function (event, row) {
                        for (let i = 0; i < protein.precursors.length; i++) {
                            if (protein.precursors[i].AbbreviatedLabel === row.name) {
                                protein.selectPrecursor(protein.precursors[i].PrecursorId, true);
                            }
                        }
                    }
                }),
                aes: {
                    hoverText: function (x, stats) {
                        const sd = LABKEY.vis.Stat.getStdDev(stats.sortedValues);

                        return 'Peptide: ' + x +
                                '\nMin: ' + stats.min.toExponential(3) +
                                '\nMax: ' + stats.max.toExponential(3) +
                                '\nMean: ' + stats.Q2.toExponential(3) +
                                '\nStd dev: ' + sd.toExponential(3) +
                                '\n%CV: ' + (stats.Q2 ? ((sd / stats.Q2) * 100).toFixed(1) : 'N/A');
                    }
                }
            });

            const intensityPlot = new LABKEY.vis.Plot({
                renderTo: 'intensityChart',
                rendererType: 'd3',
                clipRect: true,
                width: Math.min(parentWidth, 250 * (this.precursors.length + 1)),
                height: 400,
                gridLineColor: '#777777',
                data: plotData,
                fontFamily: '13px',
                gridLinesVisible: 'none',
                layers: [boxLayer, pointLayer],
                aes: {
                    yLeft: protein.settings.areaProperty,
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
            intensityPlot.render();

            LABKEY.targetedms.SVGChart.attachPlotExportIcons('intensityChart', 'Peak Area - ' + protein.preferredname, 800, 0);

            const barAes = {
                yLeft: 'cvMean',
                x: 'cvType',
                xSub: 'sequence',
                color: 'cvType'
            };

            if (cvLineData.length) {

                const cvBarLayer = new LABKEY.vis.Layer({
                    geom: new LABKEY.vis.Geom.BarPlot({
                        clickFn: function (event, row) {
                            for (let i = 0; i < protein.precursors.length; i++) {
                                if (protein.precursors[i].AbbreviatedLabel === row.sequence) {
                                    protein.selectPrecursor(protein.precursors[i].PrecursorId, true);
                                }
                            }
                        },
                        hoverFn: function (row) {
                            return row.cvType + ': ' + row.cvMean.toFixed(1) + '%\n' +
                                    row.fullDescription +
                                    (row.cvStdDev === undefined ? '' :
                                            ('\nCV StdDev: ' + row.cvStdDev.toFixed(1) + '%'));
                        }
                    }),
                    data: cvLineData,
                    aes: barAes
                });


                const cvPlot = new LABKEY.vis.Plot({
                    renderTo: 'cvChart',
                    rendererType: 'd3',
                    width: Math.min(parentWidth, 250 * (this.precursors.length + 1)),
                    height: 400,
                    data: cvLineData,
                    fontFamily: '13px',
                    gridLinesVisible: 'none',
                    aes: barAes,
                    layers: [cvBarLayer],
                    scales: {
                        x: {
                            scaleType: 'discrete'
                        },
                        xSub: {
                            scaleType: 'discrete'
                        },
                        y: {
                            scaleType: 'continuous',
                            trans: 'linear',
                            tickFormat: function (d) {
                                if (d === 0)
                                    return 0;
                                return d.toFixed(0) + '%';
                            },
                            domain: [0, null]
                        }
                    }
                });
                cvPlot.render();
                LABKEY.targetedms.SVGChart.attachPlotExportIcons('cvChart', 'Coefficient of Variation - ' + protein.preferredname, 800, 0);
            }
        }
        else {
            document.getElementById('cvTableBody').innerHTML =
                    '<tr><td colspan="12">No data to show. Replicates must have a Day annotation and either have no Sample Type set, or be marked as being Quality Controls.</td></tr>';
        }
    },

    // Sets listeners of dom objects that need listeners
    setJqueryEventListeners: function() {
        // copy to clipboard action
        $( "#copytoclipboard" ).click(function() {
            copyTextToClipboard($( "#copytoclipboard" ).attr("clipboard"))
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
                max: protein.longestPeptide,
                values: [0, protein.longestPeptide],
                slide: function (event, ui) {
                    $("#filterpeplength").text(ui.values[0] + " - " + ui.values[1]);
                    protein.settings.changeSequenceLength(ui.values[0], ui.values[1])
                }
            });
            $("#filterpeplength").text($("#rangesliderlength").slider("values", 0) +
                    " - " + $("#rangesliderlength").slider("values", 1));
        });
    }
};