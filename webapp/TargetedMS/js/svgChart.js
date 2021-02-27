if (!LABKEY.targetedms) {
    LABKEY.targetedms = {};
}

if (!LABKEY.targetedms.SVGChart) {
    LABKEY.targetedms.SVGChart = {
        createExportIcon: function (divId, iconCls, tooltip, url) {
            return '<span id="' + divId + iconCls + '" ><a href="' + LABKEY.Utils.encodeHtml(url) + '"' +
                            '<i class="fa ' + iconCls + '" title="' + tooltip + '"></i></a></span>';
        },
        requestAndRenderSVG: function (originalUrl, targetElement, legendElement, labelElement) {
            var url = originalUrl  + '&format=json' + (legendElement ? '&legend=false' : '');

            targetElement.innerText = 'Loading...';

            LABKEY.Ajax.request({
                url: url,
                scope: this,
                success: function (xhr) {
                    var parsedResponse = JSON.parse(xhr.responseText);

                    var plotAndTitleHtml = '<div style="text-align: center">' +
                            LABKEY.Utils.encodeHtml(parsedResponse.title).split('\n').join('<br>') + ' ' +
                            this.createExportIcon(targetElement.id, 'fa-file-pdf-o', 'Export to PDF', originalUrl + '&format=pdf') + ' ' +
                            this.createExportIcon(targetElement.id, 'fa-file-image-o', 'Export to PNG', originalUrl + '&format=pngDownload') +
                            '</div>';
                    plotAndTitleHtml += parsedResponse.svg;

                    if (parsedResponse.xLabel)
                    {
                        if (labelElement) {
                            labelElement.innerText = parsedResponse.xLabel;
                        }
                        else {
                            plotAndTitleHtml += '<div style="text-align: center">' + LABKEY.Utils.encodeHtml(parsedResponse.xLabel).split('\n').join('<br>') + '</div>';
                        }
                    }

                    targetElement.innerHTML = plotAndTitleHtml;

                    if (legendElement) {
                        var legend = parsedResponse.series;
                        var h = '<table><tr>';
                        for (var i = 0; i < legend.length; i++) {
                            h += (i % 3 === 0 && i !== 0) ? '</tr><tr>' : '';
                            h += '<td style="white-space:nowrap; padding: 0px 10px"><span style="color: #' + legend[i].color + '">&block; &nbsp;</span>' + LABKEY.Utils.encodeHtml(legend[i].label) + '</td> ';
                        }
                        h += '</tr></table>';
                        legendElement.innerHTML = h;
                    }
                }
            });
        }
    }
}



