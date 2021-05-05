/*
 * Copyright (c) 2015-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define('LABKEY.targetedms.BaseQCPlotPanel', {

    extend: 'Ext.panel.Panel',

    // properties used for the various data queries based on chart metric type
    metricPropArr: [],

    getMetricPropsById: function(id) {
        for (var i = 0; i < this.metricPropArr.length; i++) {
            if (this.metricPropArr[i].id == id) {
                return this.metricPropArr[i];
            }
        }
        return undefined;
    },

    isMultiSeries : function(metricId)
    {
        var metric = Ext4.isNumber(this.metric) ? this.metric : metricId;
        if (Ext4.isNumber(metric))
        {
            var metricProps = this.getMetricPropsById(metric);
            if(metricProps) {
                return Ext4.isDefined(metricProps.series2SchemaName) && Ext4.isDefined(metricProps.series2QueryName);
            }
        }
        return false;
    },

    getPlotWebPartHeader: function(wp, title)
    {
        var html = '<br/>' +
                '<table class="labkey-wp ' + wp + '">' +
                ' <tr class="labkey-wp-header">' +
                '     <th class="labkey-wp-title-left">' +
                '        <span class="labkey-wp-title-text ' +  wp + '-title">'+ Ext4.util.Format.htmlEncode(title) + '</span>' +
                '     </th>' +
                ' </tr>';
        return html;
    },

    addPlotWebPartToPlotDiv: function (id, title, div, wp)
    {
        var html = this.getPlotWebPartHeader(wp, title);
            html += '<tr>' +
                    '     <td class="labkey-wp-body">' +
                    '        <div id="' + id + '" class="chart-render-div"></div>' +
                    '     </td>' +
                    ' </tr>' +
                    '</table>';
        Ext4.get(div).insertHtml('beforeEnd', html);
    },

    addPlotsToPlotDiv: function(ids, title, div, wp)
    {
        if (this.largePlot)
            this.addLargePlotsToPlotDiv(ids, title, div, wp);
        else
            this.addSmallPlotsToPlotDiv(ids, title, div, wp);
    },

    addLargePlotsToPlotDiv: function (ids, title, div, wp)
    {
        var html = this.getPlotWebPartHeader(wp, title);

        Ext4.each(ids, function(plotId){
            html += '<tr>' +
                    '     <td><div id="' + plotId + '" class="chart-render-div"></div></td>' +
                    ' </tr>';
        });
        html += '</table>';
        Ext4.get(div).insertHtml('beforeEnd', html);
    },

    addSmallPlotsToPlotDiv: function (ids, title, div, wp)
    {
        var html = this.getPlotWebPartHeader(wp, title);

        if (ids.length > 0)
        {
            html += ' <tr>' +
                    '     <td><div class="plot-small-layout chart-render-div" id="' + ids[0] + '"></div>';
            if (ids.length > 1)
                html += '<div class="plot-small-layout chart-render-div" id="' + ids[1] + '"></div>';
            html += ' </td></tr>';
        }

        if (ids.length > 2)
        {
            html += ' <tr>' +
                    '     <td><div class="plot-small-layout chart-render-div" id="' + ids[2] + '"></div>';
            if (ids.length > 3)
                html += '<div class="plot-small-layout chart-render-div" id="' + ids[3] + '"></div>';
            html += ' </td></tr>';
        }
        html += '</table>';
        Ext4.get(div).insertHtml('beforeEnd', html);
    },

    setPlotWidth: function (div)
    {
        if (this.plotWidth == null)
        {
            // set the width of the plot webparts based on the first labkey-wp-body element
            this.plotWidth = 900;
            var spacer = 33;
            var wp = document.querySelector('.panel.panel-portal');
            if (wp && (wp.clientWidth - spacer) > this.plotWidth) {
                this.plotWidth = wp.clientWidth - spacer;
            }

            Ext4.get(div).setWidth(this.plotWidth);
        }
    },

    attachPlotExportIcons : function(id, plotTitle, plotIndex, plotWidth, extraMargin)
    {
        this.createExportIcon(id, 'fa-file-pdf-o', 'Export to PDF', 0, plotIndex, plotWidth, function(){
            this.exportChartToImage(id, extraMargin, LABKEY.vis.SVGConverter.FORMAT_PDF, plotTitle);
        });

        this.createExportIcon(id, 'fa-file-image-o', 'Export to PNG', 1, plotIndex, plotWidth, function(){
            this.exportChartToImage(id, extraMargin, LABKEY.vis.SVGConverter.FORMAT_PNG, plotTitle);
        });
    },

    createExportIcon : function(divId, iconCls, tooltip, indexFromLeft, plotIndex, plotWidth, callbackFn)
    {
        var leftPositionPx = (( (!this.largePlot && this.plotTypes && this.plotTypes.length > 1) ? plotIndex % 2 : 0) * plotWidth) + (indexFromLeft * 30) + 60,
            exportIconDivId = divId + iconCls,
            html = '<div id="' + exportIconDivId + '" class="export-icon" style="left: ' + leftPositionPx + 'px;">'
                    + '<i class="fa ' + iconCls + '"></i></div>';

        Ext4.get(divId).insertHtml('afterBegin', html);

        Ext4.create('Ext.tip.ToolTip', {
            target: exportIconDivId,
            html: tooltip
        });

        Ext4.get(exportIconDivId).on('click', callbackFn, this);
    },

    exportChartToImage : function(svgDivId, extraMargin, type, fileName)
    {
        var svgStr = this.getExportSVGStr(svgDivId, extraMargin),
            exportType = type || LABKEY.vis.SVGConverter.FORMAT_PDF;
        LABKEY.vis.SVGConverter.convert(svgStr, exportType, fileName);
    },

    getExportSVGStr: function(svgDivId, extraWidth)
    {
        var svgEls = Ext4.get(svgDivId).select('svg');
        var targetSvg = svgEls.elements[0];
        var oldWidth = targetSvg.getBoundingClientRect().width;
        // temporarily increase svg size to allow exporting of legends that's outside svg
        if (extraWidth)
            targetSvg.setAttribute('width', oldWidth + extraWidth);
        var svgStr = LABKEY.vis.SVGConverter.svgToStr(targetSvg);
        if (extraWidth)
            targetSvg.setAttribute('width', oldWidth);
        svgStr = svgStr.replace(/visibility="hidden"/g, 'visibility="visible"');
        return svgStr;
    },

    failureHandler: function(response) {
        var plotDiv = Ext4.get(this.plotDivId);
        if (plotDiv) {
            if (!response) {
                plotDiv.update("<span>Failure loading data</span>");
            }
            else if (response.message) {
                plotDiv.update("<span>" + Ext4.util.Format.htmlEncode(response.message) + "</span>");
            }
            else {
                plotDiv.update("<span class='labkey-error'>Error: " + Ext4.util.Format.htmlEncode(response.exception) + "</span>");
            }

            plotDiv.unmask();
        }
    },

    queryInitialQcMetrics : function(successCallback,callbackScope) {
        LABKEY.Ajax.request({
            url: LABKEY.ActionURL.buildURL('targetedms', 'GetQCMetricConfigurations.api'),
            method: 'GET',
            success: function(response) {
                this.metricPropArr = Ext4.JSON.decode(response.responseText).configurations;
                successCallback.call(callbackScope);
            },
            failure: LABKEY.Utils.getCallbackWrapper(function(response) {
                this.failureHandler(response);
            }, null, true),
            scope: this
        });
    },

    queryQCInstruments: function(successCallback, callbackScope) {
        LABKEY.Query.executeSql({
            schemaName: 'targetedms',
            sql: 'SELECT DISTINCT instrumentSerialNumber, instrumentId.model FROM samplefile',
            containerFilter: LABKEY.Query.containerFilter.current,
            scope: this,
            success: function (response) {

                if (response.rows && response.rows.length > 0) {
                    this.qcIntrumentsArr = response.rows.map(function (row) {
                        if (row.instrumentSerialNumber && row.model) {
                            return row.instrumentSerialNumber + ' - ' + row.model;
                        }
                        else if (row.instrumentSerialNumber && !row.model) {
                            return row.instrumentSerialNumber;
                        }
                        else if (!row.instrumentSerialNumber && row.model) {
                            return row.model;
                        }
                    })
                }

                successCallback.call(callbackScope);
            },
            failure: this.failureHandler
        });
    },

    formatValue: function(value) {
        return Math.round(value * 10000) / 10000;
    }
});

