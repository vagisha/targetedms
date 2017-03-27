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

    colors: {
        unknown: 'black',
        standard: 'gray',
        qc: 'green'},


    initComponent : function() {
        Ext4.tip.QuickTipManager.init();
        this.callParent();

        this.width = window.innerWidth - 100;
        this.addPlot();
    },

    getSlopeIntersect: function(scope, point) {
        return (point.y - scope.data.calibrationCurve.intercept) / scope.data.calibrationCurve.slope;
    },

    getPointToLineLayer: function(scope, point) {
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
            // name: 'CD4+ (cells/mm3)',
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

    addPlot : function() {
        var me = this;

        // This is a dummy layer to be overwritten by the line layer when selecting a point
        this.selectedPointLayer = new LABKEY.vis.Layer({
            geom: new LABKEY.vis.Geom.Path({size: 3, opacity: 0}),
            data: [],
            aes: {
                y: function(row){
                    return row.y;
                },
                x: function(row) {
                    return row.x;
                }
            }
        });

        this.plot = new LABKEY.vis.Plot({
            renderTo: this.renderTo,
            rendererType: 'd3',
            width: this.width,
            // width: this.plotWidth - 30,
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
                        y: function(row){
                            return row.y;
                        },
                        x: function(row) {
                            return (row.y - me.data.calibrationCurve.intercept) / me.data.calibrationCurve.slope;
                        }
                    }
                }),
                new LABKEY.vis.Layer({
                    geom: new LABKEY.vis.Geom.Point({size: 5}),
                    aes: {
                        y: function (row) {
                            if(me.minY === null || me.minY > row.y)
                                me.minY = row.y;

                            return row.y
                        },
                        x: function (row) {
                            return row.x
                        },
                        pointClickFn: function(event, data){
                            // var legend = me.getLegendDataSlopeCalculations(me);
                            // legend.push({text: 'Concentration: ' +
                            //     me.formatLegendValue(me.getSlopeIntersect(me, data)),
                            //     color: 'white', separator: true});
                            // legend = me.getLegendDataInfo(me).concat(legend);

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
                        hoverText: function(row){
                            return 'Name: ' + row.name + '\nNormalized Area: ' + me.formatLegendValue(row.y) + '\nCalculated Concentration: ' + me.formatLegendValue(row.x);}
                    }
                })
            ],
            aes: {
                color: function(row){
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
                    domain: [this.data.calibrationCurve.minY,this.data.calibrationCurve.maxY]
                }
            },
            legendData: this.getLegendDataInfo(me).concat(this.getLegendDataSlopeCalculations(me)),
            legendNoWrap: true
        });

        this.plot.render();
    },

    getLegendDataPointCalculations: function(scope, point){

        return [
            {text: 'Selected Point', separator: true},
            {text: 'Replicate: ' + point.name, color: 'white'},
            {text: 'Peak Area Ratio: ' + scope.formatLegendValue(point.y), color: 'white'},
            {text: 'Concentration: ' + scope.formatLegendValue(point.x), color: 'white'},
            {text: 'Calc. Concentration: ' + scope.formatLegendValue(scope.getSlopeIntersect(scope, point)), color: 'white'}
        ]
    },

    getLegendDataSlopeCalculations: function(scope){
        return [
            {text: 'Calibration Curve', separator: true},
            {text: 'Slope: ' + scope.formatLegendValue(this.data.calibrationCurve.slope), color: 'white'},
            {text: 'Intercept: ' + scope.formatLegendValue(this.data.calibrationCurve.intercept), color: 'white'},
            {text: 'rSquared: ' + scope.formatLegendValue(this.data.calibrationCurve.rSquared), color: 'white'},
            {text: '', separator: true}
        ];
    },

    getLegendDataInfo: function(scope){
        return [
            {text: 'Standard', color: scope.colors['standard']},
            {text: 'QC', color: scope.colors['qc']},
            {text: 'Unknown', color: scope.colors['unknown']},
            {text: '', separator: true}
        ];
    },

    formatLegendValue: function(value) {
        return Math.round(value * 100000) / 100000;
    }
});