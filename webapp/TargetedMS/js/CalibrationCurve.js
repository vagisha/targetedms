/**
 * Created by Marty on 3/16/2017.
 */

Ext4.define('LABKEY.targetedms.CalibrationCurve', {

    extend: 'Ext.panel.Panel',
    layout: 'fit',
    border: false,

    selectedPointLayer: null,
    minY: null,
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
        this.addPlot();

        var me = this;
        window.addEventListener("resize", function () {
            console.log('resize');
            me.setWidth(me.getPanelSize());
            me.plot.setWidth(me.getWidth());
            me.plot.render();

            // Plot re-renders so need to shrink dots to get back to initial state
            d3.selectAll('a.point path').transition().attr("stroke-width", 1);
        }, false);
    },

    getPanelSize: function () {
        return window.innerWidth - 100;
    },

    getSlopeIntersect: function (scope, point) {
        return (point.y - scope.data.calibrationCurve.intercept) / scope.data.calibrationCurve.slope;
    },

    getPointToLineLayer: function (scope, point) {
        var data = [];
        data.push(point);
        data.push({
            x: scope.getSlopeIntersect(scope, point),
            y: point.y,
            type: point.type
        });

        data.push({
            x: scope.getSlopeIntersect(scope, point),
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

        var minY = this.data.calibrationCurve.minY, maxY = this.data.calibrationCurve.maxY;

        if (minY == null)
            minY = 0;

        if (maxY == null)
            maxY = 0;

        this.plot = new LABKEY.vis.Plot({
            renderTo: this.renderTo,
            rendererType: 'd3',
            width: this.width,
            height: this.plotHeight,
            data: this.data.dataPoints,
            labels: {
                main: {value: this.data.molecule.name},
                y: {value: 'Light:Heavy Peak Area Ratio'},
                x: {value: 'Analyte Concentration (fmol/ml)'}
            },
            layers: [
                this.selectedPointLayer,
                new LABKEY.vis.Layer({
                    geom: new LABKEY.vis.Geom.Path({size: 3, opacity: .4}),
                    aes: {
                        y: function (row) {
                            return row.y;
                        },
                        x: function (row) {
                            return (row.y - me.data.calibrationCurve.intercept) / me.data.calibrationCurve.slope;
                        }
                    }
                }),
                new LABKEY.vis.Layer({
                    geom: new LABKEY.vis.Geom.Point({size: 5}),
                    aes: {
                        y: function (row) {
                            if (me.minY === null || me.minY > row.y)
                                me.minY = row.y;

                            return row.y
                        },
                        x: function (row) {
                            return row.x
                        },
                        pointClickFn: function (event, data) {
                            var legend = me.getLegendDataInfo(me)
                                    .concat(me.getLegendDataSlopeCalculations(me))
                                    .concat(me.getLegendDataPointCalculations(me, data));

                            me.plot.setLegend(legend);

                            me.selectedPointLayer = me.plot.replaceLayer(me.selectedPointLayer, me.getPointToLineLayer(me, data));
                            me.plot.render();

                            // Shrink dots from previous clicks and grow clicked dot
                            d3.selectAll('a.point path').transition().attr("stroke-width", 1);
                            d3.select(event.srcElement).transition().attr("stroke-width", 8);

                            // Transition in line layer visibility
                            d3.selectAll('svg g.layer path[stroke-opacity="0"').transition().attr('stroke-opacity', .5)
                        },
                        hoverText: function (row) {
                            return 'Name: ' + row.name + '\nPeak Area Ratio: ' + me.formatLegendValue(row.y) + '\nConcentration: ' + me.formatLegendValue(row.x);
                        }
                    }
                })
            ],
            aes: {
                color: function (row) {
                    return row.type;
                }
            },
            scales: {
                color: {
                    scaleType: 'discrete',
                    scale: function (group) {
                        if (Ext4.isDefined(me.colors[group]))
                            return me.colors[group];

                        return 'blue';
                    }
                },
                y: {
                    scaleType: 'continuous',
                    trans: 'linear',
                    domain: [minY, maxY]
                }
            },
            legendData: this.getLegendDataInfo(me).concat(this.getLegendDataSlopeCalculations(me)),
            legendNoWrap: true
        });

        this.plot.render();

        this.createExportIcon('png', 'Export to PNG', 0, function () {
            this.exportChartToImage(LABKEY.vis.SVGConverter.FORMAT_PNG);
        });
        this.createExportIcon('pdf', 'Export to PDF', 1, function () {
            this.exportChartToImage(LABKEY.vis.SVGConverter.FORMAT_PDF);
        });
    },

    getLegendDataPointCalculations: function (scope, point) {

        return [
            {text: 'Selected Point', separator: true},
            {text: 'Replicate: ' + point.name, color: 'white'},
            {text: 'Peak Area Ratio: ' + scope.formatLegendValue(point.y), color: 'white'},
            {text: 'Concentration: ' + scope.formatLegendValue(point.x), color: 'white'},
            {
                text: 'Calc. Concentration: ' + scope.formatLegendValue(scope.getSlopeIntersect(scope, point)),
                color: 'white'
            }
        ]
    },

    getLegendDataSlopeCalculations: function (scope) {
        return [
            {text: 'Calibration Curve', separator: true},
            {text: 'Slope: ' + scope.formatLegendValue(this.data.calibrationCurve.slope), color: 'white'},
            {text: 'Intercept: ' + scope.formatLegendValue(this.data.calibrationCurve.intercept), color: 'white'},
            {text: 'rSquared: ' + scope.formatLegendValue(this.data.calibrationCurve.rSquared), color: 'white'},
            {text: '', separator: true}
        ];
    },

    getLegendDataInfo: function (scope) {
        return [
            {text: 'Standard', color: scope.colors['standard']},
            {text: 'QC', color: scope.colors['qc']},
            {text: 'Unknown', color: scope.colors['unknown']},
            {text: '', separator: true}
        ];
    },

    formatLegendValue: function (value) {
        return Math.round(value * 100000) / 100000;
    },

    createExportIcon: function (iconCls, tooltip, indexFromLeft, callbackFn) {
        var iconDiv = Ext4.get(this.renderTo + '-' + iconCls);

        Ext4.create('Ext.tip.ToolTip', {
            target: iconDiv,
            width: 110,
            html: tooltip
        });

        iconDiv.on('click', callbackFn, this);
    },

    exportChartToImage: function (type) {
        var fileName = 'Calibration Curve: ' + this.data.molecule.name,
                exportType = type || LABKEY.vis.SVGConverter.FORMAT_PDF;
        LABKEY.vis.SVGConverter.convert(Ext4.get(this.renderTo).child('svg').dom, exportType, fileName);
    }
});