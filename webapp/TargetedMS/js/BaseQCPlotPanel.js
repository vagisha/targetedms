/*
 * Copyright (c) 2015 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
/**
 * Created by binalpatel on 7/22/15.
 */
Ext4.define('LABKEY.targetedms.BaseQCPlotPanel', {

    extend: 'Ext.panel.Panel',

    // properties used for the various data queries based on chart metric type
    chartTypePropArr: [{
        name: 'retentionTime',
        shortName: 'RT',
        title: 'Retention Time',
        baseTableName: 'PrecursorChromInfo',
        baseLkFieldKey: '',
        colName: 'BestRetentionTime'
    },{
        name: 'peakArea',
        shortName: 'PA',
        title: 'Peak Area',
        baseTableName: 'PrecursorChromInfo',
        baseLkFieldKey: '',
        colName: 'TotalArea'
    },{
        name: 'fwhm',
        shortName: 'FWHM',
        title: 'Full Width at Half Maximum (FWHM)',
        baseTableName: 'PrecursorChromInfo',
        baseLkFieldKey: '',
        colName: 'MaxFWHM'
    },{
        name: 'fwb',
        shortName: 'FWB',
        title: 'Full Width at Base (FWB)',
        baseTableName: 'PrecursorChromInfo',
        baseLkFieldKey: '',
        colName: 'MaxFWB'
    },{
        name: 'ratio',
        shortName: 'L/H ratio',
        title: 'Light/Heavy Ratio',
        baseTableName: 'PrecursorAreaRatio',
        baseLkFieldKey: 'PrecursorChromInfoId.',
        colName: 'AreaRatio'
    },{
        name: 'transitionPrecursorRatio',
        shortName: 'T/PA Ratio',
        title: 'Transition/Precursor Area Ratio',
        baseTableName: 'PrecursorChromInfo',
        baseLkFieldKey: '',
        colName: 'TransitionPrecursorRatio'
    },{
        name: 'massAccuracy',
        shortName: 'MA',
        title: 'Mass Accuracy',
        baseTableName: 'PrecursorChromInfo',
        baseLkFieldKey: '',
        colName: 'AverageMassErrorPPM'
    }],

    addPlotWebPartToPlotDiv: function (id, title, div, wp)
    {
        Ext4.get(div).insertHtml('beforeEnd', '<br/>' +
                '<table class="labkey-wp ' + wp + '">' +
                ' <tr class="labkey-wp-header">' +
                '     <th class="labkey-wp-title-left">' +
                '        <span class="labkey-wp-title-text ' +  wp + '-title">'+ Ext4.util.Format.htmlEncode(title) +
                '           <div class="plot-export-btn" id="' + id + '-exportToPDFbutton"></div>' +
                '        </span>' +
                '     </th>' +
                ' </tr><tr>' +
                '     <td class="labkey-wp-body"><div id="' + id + '"></div></</td>' +
                ' </tr>' +
                '</table>'
        );
    },

    setPlotWidth: function (div)
    {
        if (this.plotWidth == null)
        {
            // set the width of the plot webparts based on the first labkey-wp-body element
            this.plotWidth = 900;
            var wp = document.querySelector('.labkey-wp-body');
            if (wp && (wp.clientWidth - 20) > this.plotWidth)
            {
                this.plotWidth = wp.clientWidth - 20;
            }

            Ext4.get(div).setWidth(this.plotWidth);
        }
    },

    createExportToPDFButton: function (id, title, filename)
    {
        new Ext4.Button({
            renderTo: id + "-exportToPDFbutton",
            svgDivId: id,
            icon: LABKEY.contextPath + "/_icons/pdf.gif",
            tooltip: "Export PDF of this plot",
            handler: function (btn)
            {
                var svgEls = Ext4.get(btn.svgDivId).select('svg');
                var svgStr = LABKEY.vis.SVGConverter.svgToStr(svgEls.elements[0]);
                svgStr = svgStr.replace(/visibility="hidden"/g, 'visibility="visible"');
                LABKEY.vis.SVGConverter.convert(svgStr, LABKEY.vis.SVGConverter.FORMAT_PDF, filename);
            },
            scope: this
        });
    },

    failureHandler: function(response) {
        console.log(response);
        if (response.message) {
            Ext4.get(this.plotDivId).update("<span>" + response.message +"</span>");
        }
        else {
            Ext4.get(this.plotDivId).update("<span class='labkey-error'>Error: " + response.exception + "</span>");
        }

        Ext4.get(this.plotDivId).unmask();
    }
});





