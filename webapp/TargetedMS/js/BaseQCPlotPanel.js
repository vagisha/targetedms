/**
 * Created by binalpatel on 7/22/15.
 */
Ext4.define('LABKEY.targetedms.BaseQCPlotPanel', {

    extend: 'Ext.panel.Panel',

    addPlotWebPartToPlotDiv: function (id, title, div, wp)
    {
        var tableClass =  + wp;
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

    createExportToPDFButton: function (id, title)
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
                LABKEY.vis.SVGConverter.convert(svgStr, LABKEY.vis.SVGConverter.FORMAT_PDF, title);
            },
            scope: this
        });
    }
});





