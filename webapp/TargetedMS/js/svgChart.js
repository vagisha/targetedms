if (!LABKEY.targetedms) {
    LABKEY.targetedms = {};
}

if (!LABKEY.targetedms.SVGChart) {
    LABKEY.targetedms.SVGChart = {
        requestAndRenderSVG: function (originalUrl, targetElement, legendElement, labelElement, titleTransformer) {
            if (titleTransformer === undefined) {
                titleTransformer = function(x) { return x };
            }

            var url = originalUrl  + '&format=json' + (legendElement ? '&legend=false' : '');

            targetElement.innerText = 'Loading...';
            if (targetElement.className.indexOf('exportable-plot') < 0) {
                targetElement.className += ' exportable-plot';
            }

            LABKEY.Ajax.request({
                url: url,
                scope: this,
                success: function (xhr) {
                    var parsedResponse = JSON.parse(xhr.responseText);

                    var plotAndTitleHtml = '';
                    if (titleTransformer) {
                        plotAndTitleHtml += '<div style="text-align: center; word-break: break-word">' +
                                LABKEY.Utils.encodeHtml(titleTransformer(parsedResponse.title)).split('\n').join('<br>') +
                                '</div>';
                    }

                    plotAndTitleHtml += parsedResponse.svg;

                    if (parsedResponse.xLabel)
                    {
                        if (labelElement) {
                            labelElement.innerText = parsedResponse.xLabel;
                        }
                        else if (labelElement !== false) {
                            plotAndTitleHtml += '<div style="text-align: center">' + LABKEY.Utils.encodeHtml(parsedResponse.xLabel).split('\n').join('<br>') + '</div>';
                        }
                    }

                    targetElement.innerHTML = plotAndTitleHtml;

                    if (legendElement) {
                        var legend = parsedResponse.series;
                        var h = '<table align="center"><tr>';
                        for (var i = 0; i < legend.length; i++) {
                            h += (i % 3 === 0 && i !== 0) ? '</tr><tr>' : '';
                            h += '<td style="white-space:nowrap; padding: 0 10px"><span style="color: #' + legend[i].color + '">&block; &nbsp;</span>' + LABKEY.Utils.encodeHtml(legend[i].label) + '</td> ';
                        }
                        h += '</tr></table>';
                        legendElement.innerHTML = h;
                    }

                    this.createExportIcon(targetElement.id, 'fa-file-pdf-o', 'Export to PDF', 0, 300, function() { document.location = originalUrl + '&format=pdf' });
                    this.createExportIcon(targetElement.id, 'fa-file-image-o', 'Export to PNG', 1, 300, function() { document.location = originalUrl + '&format=pngDownload' });
                }
            });
        },

        attachPlotExportIcons : function(id, plotTitle, plotWidth, extraMargin)
        {
            this.createExportIcon(id, 'fa-file-pdf-o', 'Export to PDF', 0, plotWidth, function(){
                LABKEY.targetedms.SVGChart.exportChartToImage(id, extraMargin, LABKEY.vis.SVGConverter.FORMAT_PDF, plotTitle);
            });

            this.createExportIcon(id, 'fa-file-image-o', 'Export to PNG', 1, plotWidth, function(){
                LABKEY.targetedms.SVGChart.exportChartToImage(id, extraMargin, LABKEY.vis.SVGConverter.FORMAT_PNG, plotTitle);
            });
        },

        createExportIcon : function(divId, iconCls, tooltip, indexFromLeft, plotWidth, callbackFn)
        {
            let element = document.getElementById(divId);
            var leftPositionPx = (indexFromLeft * 30) + element.getBoundingClientRect().left,
                    exportIconDivId = divId + iconCls,
                    html = '<div id="' + exportIconDivId + '" class="export-icon" style="left: ' + leftPositionPx + 'px;">'
                            + '<i class="fa ' + iconCls + '"></i></div>';

            $('#' + divId).prepend(html);

            $('#' + exportIconDivId).click(this, callbackFn);
        },

        exportChartToImage : function(svgDivId, extraMargin, type, fileName)
        {
            var svgStr = this.getExportSVGStr(svgDivId, extraMargin),
                    exportType = type || LABKEY.vis.SVGConverter.FORMAT_PDF;
            LABKEY.vis.SVGConverter.convert(svgStr, exportType, fileName);
        },

        getExportSVGStr: function(svgDivId, extraWidth)
        {
            var targetSvg = $('#' + svgDivId)[0];
            if (targetSvg.tagName.toLowerCase() !== 'svg') {
                targetSvg = $('#' + svgDivId + '> svg')[0];
            }
            var oldWidth = targetSvg.getBoundingClientRect().width;
            // temporarily increase svg size to allow exporting of legends that's outside svg
            if (extraWidth)
                targetSvg.setAttribute('width', oldWidth + extraWidth);
            var svgStr = LABKEY.vis.SVGConverter.svgToStr(targetSvg);
            if (extraWidth)
                targetSvg.setAttribute('width', oldWidth);
            svgStr = svgStr.replace(/visibility="hidden"/g, 'visibility="visible"');
            return svgStr;
        }
    }
}



