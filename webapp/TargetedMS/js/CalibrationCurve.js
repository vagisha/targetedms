/*
 * Copyright (c) 2017-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
/**
 * Created by Marty on 3/16/2017.
 */

Ext4.define('LABKEY.targetedms.CalibrationCurve', {

    extend: 'Ext.panel.Panel',
    layout: 'fit',
    border: false,

    selectedPointLayer: null,
    plotHeight: 500,
    minWidth: 800,

    colors: {
        unknown: 'black',
        standard: 'gray',
        qc: 'green'
    },

    initComponent: function () {
        Ext4.tip.QuickTipManager.init();
        this.callParent();

        this.width = this.getPanelSize();

        this.minY = this.data.calibrationCurve.minY || 0;
        this.maxY = this.data.calibrationCurve.maxY || 0;
        this.minX = this.data.calibrationCurve.minX || 0;
        this.maxX = this.data.calibrationCurve.maxX || 0;

        // Ensure plot goes to max x axis for selected point calculations
        var calcMaxX = this.getQuadraticIntersect(this, this.maxY);
        if (calcMaxX > this.maxX)
            this.maxX = calcMaxX;

        this.addCurvePoints();

        this.refreshPlot();

        var me = this;
        window.addEventListener("resize", function () {
            // Issue 43532 - may have been loaded even if we don't have a curve to plot
            if (me.plot) {
                me.setWidth(me.getPanelSize());
                me.plot.setWidth(me.getWidth());
                me.plot.render();

                // Plot re-renders so need to shrink dots to get back to initial state
                d3.selectAll('a.point path').transition().attr("stroke-width", 1);
            }
        }, false);
    },

    refreshPlot: function() {
        // Clear out child <svg> elements
        let children = document.getElementById(this.renderTo).childNodes;
        for (let i = 0; i < children.length; ) {
            if (children[i].localName === 'svg') {
                children[i].parentNode.removeChild(children[i]);
            }
            else {
                i++;
            }
        }
        this.addPlot();
    },

    // Add points for quadratic calculated concentration curve
    addCurvePoints: function () {
        var curvePts = 50;
        var x, y;
        var increment = (this.maxX - this.minX) / curvePts;

        this.data.curvePoints = [];
        for (var pt = 0; pt <= curvePts; pt++) {
            x = this.minX + (pt * increment);
            y = this.data.calibrationCurve.quadraticCoefficient * (x * x) + this.data.calibrationCurve.slope * x
                    + this.data.calibrationCurve.intercept;

            this.data.curvePoints.push({x:x, y:y});
        }
    },

    getPanelSize: function () {
        return window.innerWidth - 100;
    },

    // Given y, solve for x
    getQuadraticIntersect: function (scope, y) {
        var a = scope.data.calibrationCurve.quadraticCoefficient;
        var b = scope.data.calibrationCurve.slope;
        var c = scope.data.calibrationCurve.intercept;

        var intersect;
        if (a !== 0) { //Quadratic
            intersect = ((-1 * b) + Math.sqrt((b * b) - (4 * a * (c - y)))) / (2 * a);
        }
        else { //Linear
            intersect = (y - c) / b;
        }
        return intersect;
    },

    getPointToLineLayer: function (scope, point) {
        var data = [];
        data.push(point);
        data.push({
            x: scope.getQuadraticIntersect(scope, point.y),
            y: point.y,
            type: point.type
        });

        data.push({
            x: scope.getQuadraticIntersect(scope, point.y),
            y: scope.minY,
            type: point.type
        });

        return new LABKEY.vis.Layer({
            geom: new LABKEY.vis.Geom.Path({size: 3, opacity: 0, color: 'red'}),
            aes: {
                y: function (row) {
                    return row.y
                },
                x: function (row) {
                    return row.x
                }
            },
            data: data
        })
    },

    addPlot: function () {
        var me = this;

        if (this.data.calibrationCurve.errorMessage) {
            document.getElementById(this.renderTo).innerText = this.data.calibrationCurve.errorMessage;
            return;
        }

        // This is a dummy layer to be overwritten by the line layer when selecting a point
        this.selectedPointLayer = new LABKEY.vis.Layer({
            geom: new LABKEY.vis.Geom.Path({size: 3, opacity: 0}),
            data: [],
            aes: {
                y: function (row) {
                    return row.y;
                },
                x: function (row) {
                    return row.x;
                }
            }
        });

        var units = "";
        if (this.data.calibrationCurve.units != null)
            units = "(" + this.data.calibrationCurve.units + ")";

        this.plot = new LABKEY.vis.Plot({
            renderTo: this.renderTo,
            rendererType: 'd3',
            width: this.width,
            height: this.plotHeight,
            labels: {
                main: {value: this.data.molecule.name},
                y: {value: 'Normalized Peak Areas'},
                x: {value: 'Analyte Concentration ' + units}
            },
            layers: [
                this.selectedPointLayer,
                new LABKEY.vis.Layer({
                    data: this.data.curvePoints,
                    geom: new LABKEY.vis.Geom.Path({size: 3, opacity: .4}),
                    aes: {
                        y: 'y',
                        x: 'x'
                    }
                }),
                new LABKEY.vis.Layer({
                    data: this.data.dataPoints,
                    geom: new LABKEY.vis.Geom.Point({size: 5, opacity: 0.75}),
                    aes: {
                        y: 'y',
                        x: 'x',
                        pointClickFn: function (event, data) {
                            var legend = me.getLegendDataInfo(me)
                                    .concat(me.getLegendDataSlopeCalculations(me))
                                    .concat(me.getLegendDataPointCalculations(me, data));

                            me.plot.setLegend(legend);

                            var lineLayer = me.getPointToLineLayer(me, data);

                            me.plot.replaceLayer(me.selectedPointLayer, lineLayer);
                            me.selectedPointLayer = lineLayer;
                            me.plot.render();

                            // Shrink dots from previous clicks and grow clicked dot
                            d3.selectAll('a.point path').transition().attr("stroke-width", 1);
                            d3.select(event.srcElement).transition().attr("stroke-width", 8);

                            // Transition in line layer visibility
                            d3.selectAll('svg g.layer path[stroke-opacity="0"').transition().attr('stroke-opacity', .5)
                        },
                        hoverText: function (row) {
                            return 'Name: ' + row.name + '\nPeak Area: ' + me.formatLegendValue(row.y, true) + '\nConcentration: ' + me.formatLegendValue(row.x) + (row.excluded ? '\nExcluded from calibration' : '');
                        }
                    }
                })
            ],
            aes: {
                color: function (row) {
                    return row.type;
                },
                shape: function (row) {
                    return row.excluded ? 'Excluded' : 'Included';
                }
            },
            scales: {
                color: {
                    scaleType: 'discrete',
                    scale: function (group) {
                        if (Ext4.isDefined(me.colors[group]))
                            return me.colors[group];

                        return 'blue';
                    },
                },
                y: {
                    scaleType: 'continuous',
                    trans: document.getElementById('calCurveYScale').value,
                    domain: [me.minY, me.maxY],
                    tickFormat: function (d) {
                        if (d < 1000 && d > 0.001)
                            return d;
                        return d.toExponential();
                    }
                },
                x: {
                    trans: document.getElementById('calCurveXScale').value
                }
            },
            legendData: this.getLegendDataInfo(me).concat(this.getLegendDataSlopeCalculations(me)),
            legendNoWrap: true
        });

        this.plot.render();

        LABKEY.targetedms.SVGChart.attachPlotExportIcons(this.renderTo, 'Calibration Curve: ' + this.data.molecule.name, 800, 0);
    },

    getLegendDataPointCalculations: function (scope, point) {

        var result = [
            {text: 'Selected Point', separator: true},
            {text: 'Replicate: ' + point.name, color: 'white'},
            {text: 'Peak Area: ' + scope.formatLegendValue(point.y, true), color: 'white'},
            {text: 'Concentration: ' + scope.formatLegendValue(point.x), color: 'white'},
            {
                text: 'Calc. Concentration: ' + scope.formatLegendValue(scope.getQuadraticIntersect(scope, point.y)),
                color: 'white'
            }
        ];
        if (point.excluded) {
            result.push({text: 'Excluded from calibration', color: 'white'});
        }
        return result
    },

    getLegendDataSlopeCalculations: function (scope) {
        var result = [
            {text: 'Calibration Curve', separator: true},
            {text: 'Regression Fit: ' + Ext4.util.Format.htmlEncode(this.data.calibrationCurve.regressionFit), color: 'white'},
            {text: 'Norm. Method: ' + Ext4.util.Format.htmlEncode(this.data.calibrationCurve.normalizationMethod), color: 'white'},
            {text: 'Regression Weighting: ' + Ext4.util.Format.htmlEncode(this.data.calibrationCurve.regressionWeighting), color: 'white'},
            {text: 'MS Level: ' + (this.data.msLevel > 0 ? this.data.msLevel : 'All'), color: 'white'},
            {text: '', separator: true},
            {text: 'Slope: ' + scope.formatLegendValue(this.data.calibrationCurve.slope, true), color: 'white'},
            {text: 'Intercept: ' + scope.formatLegendValue(this.data.calibrationCurve.intercept, true), color: 'white'}
        ];
        if (this.data.calibrationCurve.quadraticCoefficient && this.data.calibrationCurve.quadraticCoefficient !== 0.0) {
            result.push({text: 'Quadratic Coefficient: ' + scope.formatLegendValue(this.data.calibrationCurve.quadraticCoefficient), color: 'white'});
        }
        result.push({text: 'rSquared: ' + scope.formatLegendValue(this.data.calibrationCurve.rSquared), color: 'white'});
        result.push({text: '', separator: true});
        return result;
    },

    getLegendDataInfo: function (scope) {
        return [
            {text: 'Standard', color: scope.colors['standard'], shape:LABKEY.vis.Scale.Shape()[0]},
            {text: 'QC', color: scope.colors['qc'], shape:LABKEY.vis.Scale.Shape()[0]},
            {text: 'Unknown', color: scope.colors['unknown'], shape:LABKEY.vis.Scale.Shape()[0]},
            {text: '', separator: true},
            {text: 'Excluded', color: scope.colors['standard'], shape: LABKEY.vis.Scale.Shape()[1]},
            {text: '', separator: true}
        ];
    },

    formatLegendValue: function (value, exp) {
        var rounded = Math.round(value * 100000) / 100000;
        // Use scientific notation if the number is large or very small
        if (exp && (rounded > 10000 || rounded < -10000 || (rounded > -0.00001 && rounded < 0.00001)))
            rounded.toExponential(4)
        return rounded;
    }
});