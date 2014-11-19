/*
 * Copyright (c) 2011-2014 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext.namespace('LABKEY');

/**
* User: cnathe
* Date: Sept 20, 2011
*/

/**
 * Class to create a panel for displaying the R plot for the trending of EC50, AUC, and High MFI values for the selected graph parameters.
 *
 * @params titration
 * @params assayName
 */
LABKEY.LeveyJenningsTrendPlotPanel = Ext.extend(Ext.FormPanel, {
    constructor : function(config){
        // apply some Ext panel specific properties to the config
        Ext.apply(config, {
            items: [],
            header: false,
            bodyStyle: 'background-color:#EEEEEE',
            labelAlign: 'left',
            width: 900,
            border: false,
            cls: 'extContainer',
            yAxisScale: 'linear',
            chartType: 'retentionTime'
        });

        this.addEvents('reportFilterApplied', 'togglePdfBtn');

        LABKEY.LeveyJenningsTrendPlotPanel.superclass.constructor.call(this, config);
    },

    initComponent : function() {
        this.ANY_FIELD = '[ANY]';  // constant value used for turning filtering off

        this.plotRenderedHtml = null;
        this.pdfHref = null;
        if (!this.startDate)
            this.startDate = null;
        if (!this.endDate)
            this.endDate = null;

        // initialize the y-axis scale combo for the top toolbar
        this.scaleLabel = new Ext.form.Label({text: 'Y-Axis Scale:'});
        this.scaleCombo = new Ext.form.ComboBox({
            id: 'scale-combo-box',
            width: 75,
            triggerAction: 'all',
            mode: 'local',
            store: new Ext.data.ArrayStore({
                fields: ['value', 'display'],
                data: [['linear', 'Linear'], ['log', 'Log']]
            }),
            valueField: 'value',
            displayField: 'display',
            value: 'linear',
            forceSelection: true,
            editable: false,
            listeners: {
                scope: this,
                'select': function(cmp, newVal, oldVal) {
                    this.yAxisScale = cmp.getValue();
                    this.setTabsToRender();
                    this.displayTrendPlot();
                }
            }
        });

        // initialize the date range selection fields for the top toolbar
        this.startDateLabel = new Ext.form.Label({text: 'Start Date:'});
        this.startDateField = new Ext.form.DateField({
            id: 'start-date-field',
            value: this.startDate,
            format:  'Y-m-d',
            listeners: {
                scope: this,
                'valid': function (df) {
                    if (df.getValue() != '')
                        this.applyFilterButton.enable();
                },
                'invalid': function (df, msg) {
                    this.applyFilterButton.disable();
                }
            }
        });
        this.endDateLabel = new Ext.form.Label({text: 'End Date:'});
        this.endDateField = new Ext.form.DateField({
            id: 'end-date-field',
            value: this.endDate,
            format:  'Y-m-d',
            listeners: {
                scope: this,
                'valid': function (df) {
                    if (df.getValue() != '')
                        this.applyFilterButton.enable();
                },
                'invalid': function (df, msg) {
                    this.applyFilterButton.disable();
                }
            }
        });

        this.chartTypeLabel = new Ext.form.Label({text: 'Chart Type:'});
        this.chartTypeField = new Ext.form.ComboBox({
            id: 'chart-type-field',
            triggerAction: 'all',
            mode: 'local',
            store: new Ext.data.ArrayStore({
                fields: ['value', 'display'],
                data: [
                    ['retentionTime', 'Retention Time']
                    , ['peakArea', 'Peak Area']
                    , ['fwhm', 'Full Width at Half Maximum (FWHM)']
                    , ['fwb', 'Full Width at Base (FWB)']
                ]
            }),
            valueField: 'value',
            displayField: 'display',
            value: 'retentionTime',
            width: 270,
            forceSelection: true,
            editable: false,
            listeners: {
                scope: this,
                'select': function(cmp, newVal, oldVal) {
                    this.chartType = cmp.getValue();
                    this.setTabsToRender();
                    this.displayTrendPlot();
                }
            }
        });

        // initialize the refesh graph button
        this.applyFilterButton = new Ext.Button({
            disabled: true,
            text: 'Apply',
            handler: this.applyGraphFilter,
            scope: this
        });

        var tbspacer = {xtype: 'tbspacer', width: 5};

        var items = [
            this.scaleLabel, tbspacer,
            this.scaleCombo, tbspacer,
            {xtype: 'tbseparator'}, tbspacer,
            this.startDateLabel, tbspacer,
            this.startDateField, tbspacer,
            this.endDateLabel, tbspacer,
            this.endDateField, tbspacer,
            this.chartTypeLabel, tbspacer,
            this.chartTypeField, tbspacer
        ];
        items.push(this.applyFilterButton);

        this.tbar = new Ext.Toolbar({
            height: 30,
            buttonAlign: 'center',
            items: items
        });

//        // initialize the tab panel that will show the trend plots
//        this.ec504plPanel = new Ext.Panel({
//            itemId: "retentionTimes",
//            title: "Retention Times",
//            html: "<div id='retentionTimesTrendPlotDiv' class='retentionTimesTrendPlot'>To begin, choose an Antigen, Isotype, and Conjugate from the panel to the left and click the Apply button.</div>",
//            deferredRender: false,
//            listeners: {
//                scope: this,
//                'activate': this.activateTrendPlotPanel
//            }
//        });
//
//        this.trendTabPanel = new Ext.TabPanel({
//            autoScroll: true,
//            activeTab: 0,
//            defaults: {
//                height: 308,
//                padding: 5
//            },
//            items: [this.ec504plPanel]
//        });
//        this.items.push(this.trendTabPanel);

        // add an additional panel to render the PDF export link HTML to (this will always be hidden)
        this.pdfPanel = new Ext.Panel({hidden: true});
        this.items.push(this.pdfPanel);

        LABKEY.LeveyJenningsTrendPlotPanel.superclass.initComponent.call(this);

        this.displayTrendPlot();
    },

    // function called by the JSP when the graph params are selected and the "Apply" button is clicked
    graphParamsSelected: function() {
        // remove any previously entered values from the start date, end date, etc. fields
        this.clearGraphFilter();

        // show the trending tab panel and date range selection toolbar
        this.enable();

        this.setTabsToRender();
        this.displayTrendPlot();
    },

    activateTrendPlotPanel: function(panel) {
        // if the graph params have been selected and the trend plot for this panel hasn't been loaded, then call displayTrendPlot
        this.displayTrendPlot();
    },

    setTabsToRender: function() {
        // something about the report has changed and the plot needs to be set to re-render
        this.plotRenderedHtml = null;
        this.pdfHref = null;
        this.fireEvent('togglePdfBtn', false);
    },

    displayTrendPlot: function() {
        // determine which tab is selected to know which div to update
//        var plotType = this.trendTabPanel.getActiveTab().itemId;
        var trendDiv = 'hiddenPlotPanel';
        Ext.get(trendDiv).update('Loading...');

        if (this.plotRenderedHtml)
        {
            Ext.get(trendDiv).update(this.plotRenderedHtml);
        }
        else
        {
            // build the config object of the properties that will be needed by the R report
            var config = {
                reportId: 'module:targetedms/LeveyJenningsTrendPlot.r',
                showSection: 'Levey-Jennings Trend Plot',
                chartType: this.chartType
            };
            // provide either a start and end date or the max number of rows to display
            if (!this.startDate && !this.endDate){
                config['MaxRows'] = this.defaultRowSize;
            } else {
                if (this.startDate) {
                    config['StartDate'] = Ext.util.Format.date(this.startDate, 'Y-m-d');
                }
                if (this.endDate) {
                    config['EndDate'] = Ext.util.Format.date(this.endDate, 'Y-m-d');
                }
            }
            // add config for plotting in log scale
            if (this.yAxisScale == 'log')
                config['AsLog'] =  true;

            var sql = 'SELECT DISTINCT PrecursorId.ModifiedSequence AS Sequence FROM PrecursorChromInfo';
            var separator = ' WHERE ';
            if (this.startDate)
            {
                sql += separator + "PeptideChromInfoId.SampleFileId.AcquiredTime >= '" + Ext.util.Format.date(this.startDate, 'Y-m-d') + "'";
                separator = " AND ";
            }
            if (this.endDate)
            {
                sql += separator + "PeptideChromInfoId.SampleFileId.AcquiredTime <= '" + Ext.util.Format.date(this.endDate, 'Y-m-d') + "'";
                separator = " AND ";
            }

            LABKEY.Query.executeSql({
                schemaName: 'targetedms',
                sql: sql,
                scope: this,
                success: function(data) {
                    if (data.rows.length == 0)
                        Ext.get(trendDiv).update("Error: there were no records found.");

                    this.precursors = [];

                    for (var i = 0; i < data.rows.length; i++) {
                        this.precursors.push(data.rows[i].Sequence);
                    }

                    // call and display the Report webpart
                    new LABKEY.WebPart({
                        partName: 'Report',
                        renderTo: trendDiv,
                        frame: 'none',
                        partConfig: config,
                        success: function() {
                            // store the HTML for the src plot image (image will be shifted to the relevant plot for other plot types)
                            this.plotRenderedHtml = Ext.getDom(trendDiv).innerHTML;

                            var parentPanel = Ext.get('tiledPlotPanel');
                            var childElement;
                            while (childElement = parentPanel.first()) {
                                childElement.remove();
                            }

                            for (var i = 0; i < this.precursors.length; i++)
                            {
                                var precursor = this.precursors[i];
                                var styleId = "precursorPlot" + i;
                                Ext.util.CSS.removeStyleSheet(styleId);
                                Ext.util.CSS.createStyleSheet('.precursorGraph' + i +' img { top: ' + (-300 * i) + 'px; clip: rect(' + (300 * i) + 'px, 810px, ' + ((i + 1) * 300) + 'px, 0px)}', styleId);
                                parentPanel.insertHtml('beforeEnd', '<br/><table class="labkey-wp"><tr class="labkey-wp-header"><th class="labkey-wp-title-left"><span class="labkey-wp-title-text">' + Ext.util.Format.htmlEncode(precursor) + '</span></th></tr><tr><td class="labkey-wp-body"><div class="precursorGraphHolder precursorGraph' + i + '">' + this.plotRenderedHtml + '</div></</td></tr></table>');
                            }
                        },
                        failure: function(response) {
                            Ext.get(trendDiv).update("Error: " + response.statusText);
                        },
                        scope: this
                    }).render();
                },
                failure: function(response) {
                    Ext.get(trendDiv).update("Error: " + response.exception);
                }
            });

            // call the R plot code again to get a PDF output version of the plot
            var pdfConfig = {};
            Ext.apply(pdfConfig, config);
            pdfConfig['PdfOut'] = true;
            new LABKEY.WebPart({
                   partName: 'Report',
                   renderTo: this.pdfPanel.getId(),
                   frame: 'none',
                   partConfig: pdfConfig,
                   success: function() {
                       // ugly way of getting the href for the pdf file (to be used when the user clicks the export pdf button)
                       if (Ext.getDom(this.pdfPanel.getId()))
                       {
                           var html = Ext.getDom(this.pdfPanel.getId()).innerHTML;
                           this.pdfHref = html.substring(html.indexOf('href="') + 6, html.indexOf('">PDF output file'));
                           this.pdfHref = this.pdfHref.replace(/&amp;/g, "&");
                           this.fireEvent('togglePdfBtn', true);
                       }

                   },
                   failure: function(response){},
                   scope: this
            }).render();
            
        }
    },

    applyGraphFilter: function() {
        // make sure that at least one filter field is not null
        if (this.startDateField.getRawValue() == '' && this.endDateField.getRawValue() == '')
        {
            Ext.Msg.show({
                title:'ERROR',
                msg: 'Please enter a value for filtering.',
                buttons: Ext.Msg.OK,
                icon: Ext.MessageBox.ERROR
            });
        }
        // verify that the start date is not after the end date
        else if (this.startDateField.getValue() > this.endDateField.getValue() && this.endDateField.getValue() != '')
        {
            Ext.Msg.show({
                title:'ERROR',
                msg: 'Please enter an end date that does not occur before the start date.',
                buttons: Ext.Msg.OK,
                icon: Ext.MessageBox.ERROR
            });
        }
        else
        {
            // get date values without the time zone info
            this.startDate = this.startDateField.getRawValue();
            this.endDate = this.endDateField.getRawValue();

            this.setTabsToRender();
            this.displayTrendPlot();
            this.fireEvent('reportFilterApplied', this.startDate, this.endDate);
        }
    },

    clearGraphFilter: function() {
        this.startDate = null;
        this.startDateField.reset();
        this.endDate = null;
        this.endDateField.reset();
        this.applyFilterButton.disable();

        this.setTabsToRender();
        this.displayTrendPlot();
        this.fireEvent('reportFilterApplied', this.startDate, this.endDate);
    },

    getPdfHref: function() {
        return this.pdfHref ? this.pdfHref : null;
    },

    getStartDate: function() {
        return this.startDate ? this.startDate : null;
    },

    getEndDate: function() {
        return this.endDate ? this.endDate : null;
    },

    getArrayStoreData: function(rows, colName) {
        var storeData = [ [this.ANY_FIELD, this.ANY_FIELD] ];
        Ext.each(rows, function(row){
            var value = row[colName];
            storeData.push([value, value == null ? '[Blank]' : value]);
        });
        return storeData;
    }
});
