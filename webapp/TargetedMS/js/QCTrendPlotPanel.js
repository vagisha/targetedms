/*
 * Copyright (c) 2016-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

/**
 * Class to create a panel for displaying the R plot for the trending of retention times, peak areas, and other
 * values for the selected graph parameters.
 */
Ext4.define('LABKEY.targetedms.QCTrendPlotPanel', {

    extend: 'LABKEY.targetedms.BaseQCPlotPanel',
    mixins: {helper: 'LABKEY.targetedms.QCPlotHelperWrapper'},
    header: false,
    border: false,
    labelAlign: 'left',
    items: [],
    defaults: {
        xtype: 'panel',
        border: false
    },

    // properties specific to this TargetedMS QC plot implementation
    yAxisScale: 'linear',
    metric: null,
    plotTypes: ['Levey-Jennings'],
    largePlot: false,
    dateRangeOffset: 0,
    minAcquiredTime: null,
    maxAcquiredTime: null,
    startDate: null,
    endDate: null,
    groupedX: false,
    singlePlot: false,
    showExcluded: false,
    plotWidth: null,
    enableBrushing: false,
    havePlotOptionsChanged: false,
    selectedAnnotations: {},

    // Max number of plots/series to show per page
    maxCount: 50,

    initComponent : function() {
        Ext4.tip.QuickTipManager.init();

        this.callParent();

        // min and max acquired date must be provided
        if (this.minAcquiredTime == null || this.maxAcquiredTime == null)
            Ext4.get(this.plotDivId).update("<span class='labkey-error'>Unable to render report. Missing min and max AcquiredTime from data query.</span>");
        else
        {
            // Load replicate annotations in the callback.
            this.queryInitialQcMetrics(this.queryContainerReplicateAnnotations, this);
        }
    },

    queryInitialPlotOptions : function()
    {
        // If there are URL parameters (i.e. from Pareto Plot click), set those as initial values as well.
        LABKEY.Ajax.request({
            url: LABKEY.ActionURL.buildURL('targetedms', 'leveyJenningsPlotOptions.api'),
            method: 'POST',
            scope: this,
            success: LABKEY.Utils.getCallbackWrapper(function(response)
            {
                // convert the boolean and integer values from strings
                var initValues = {};
                Ext4.iterate(response.properties, function(key, value)
                {
                    if (value === "true" || value === "false")
                    {
                        value = value === "true";
                    }
                    else if (value != undefined && value.length > 0 && !isNaN(Number(value)))
                    {
                        value = +value;
                    }
                    else if (key == 'plotTypes') // convert string to array
                    {
                        value = value.split(',');
                    }
                    if(key === 'selectedAnnotations')
                    {
                        var annotations = {};

                        var a = value.split(',');
                        for(var i = 0; i < a.length; i++)
                        {
                            var b = a[i].split(":");
                            var name = b[0];
                            var val = b[1];
                            var selected = annotations[name];
                            if(!selected)
                            {
                                selected = [];
                                annotations[name] = selected;
                            }

                            selected.push(val);
                        }
                        initValues[key] = annotations;
                    }
                    else
                    {
                        initValues[key] = value;
                    }

                });

                // apply any URL parameters to the initial values
                Ext4.apply(initValues, this.getInitialValuesFromUrlParams());

                // Initialize the form
                this.initPlotForm(initValues);
            }, this, false)
        });
    },

    queryContainerReplicateAnnotations : function()
    {
        LABKEY.Ajax.request({
            url: LABKEY.ActionURL.buildURL('targetedms', 'GetContainerReplicateAnnotations.api'),
            method: 'GET',
            scope: this,
            success: LABKEY.Utils.getCallbackWrapper(function(response)
            {
                var annotationNodes = [];
                Ext4.iterate(response.replicateAnnotations, function(annotation)
                {
                    var annotValueNodes = [];
                    annotation.values.forEach(function(value){
                        var valueNode = {text: value, leaf: true, iconCls: 'tree-node-noicon', checked: false};
                        annotValueNodes.push(valueNode);
                    });
                    var annotNode = {text: annotation.name, expanded: true, iconCls: 'tree-node-noicon', children: annotValueNodes};
                    annotationNodes.push(annotNode);
                });
                this.replicateAnnotationsNodes = annotationNodes;

                // Load persisted plot options for logged in users.
                this.queryInitialPlotOptions();

            }, this, false),
            failure: LABKEY.Utils.getCallbackWrapper(function (response)
            {
                this.failureHandler(response);
            }, null, true)
        });
    },


    calculateStartDateByOffset : function()
    {
        if (this.dateRangeOffset > 0)
        {
            var todayMinusOffset = new Date();
            todayMinusOffset.setDate(todayMinusOffset.getDate() - this.dateRangeOffset);
            return todayMinusOffset;
        }

        return this.minAcquiredTime;
    },

    calculateEndDateByOffset : function()
    {
        if (this.dateRangeOffset > 0)
            return new Date();

        return this.maxAcquiredTime;
    },

    initPlotForm : function(initValues)
    {
        // apply the initial values to the panel object so they are used in form field initialization
        Ext4.apply(this, initValues);

        // if we have a dateRangeOffset, we need to calculate the start and end date
        if (this.dateRangeOffset > -1)
        {
            this.startDate = this.formatDate(this.calculateStartDateByOffset());
            this.endDate = this.formatDate(this.calculateEndDateByOffset());
        }

        // initialize the form panel toolbars and display the plot
        this.add(this.initPlotFormToolbars());

        this.displayTrendPlot();
    },

    initPlotFormToolbars : function()
    {
        var toolbarArr = [
            { tbar: this.getMainPlotOptionsToolbar() },
            { tbar: this.getCustomDateRangeToolbar() },
            { tbar: this.getFirstPlotOptionsToolbar() },
            { tbar: this.getSecondPlotOptionsToolbar() },
            { tbar: this.getThirdPlotOptionsToolbar() },
            { tbar: this.getGuideSetMessageToolbar() }
        ];

        if(this.replicateAnnotationsNodes.length > 0)
        {
            toolbarArr.splice(2, 0, {tbar: this.getAnnotationFiltersToolbar()});
            toolbarArr.splice(3, 0, {tbar: this.getSelectedAnnotationsToolbar()});
        }
        return toolbarArr;
    },

    getFirstPlotOptionsToolbar: function()
    {
        if (!this.plotTypeOptionsToolbar)
        {
            this.plotTypeOptionsToolbar = Ext4.create('Ext.toolbar.Toolbar', {
                ui: 'footer',
                cls: 'levey-jennings-toolbar',
                layout: { pack: 'center' },
                items: [
                    this.getPlotSizeOptions(),
                    {xtype: 'tbspacer'}, {xtype: 'tbseparator'}, {xtype: 'tbspacer'},
                    this.getPlotTypeOptions(true),
                    this.getScaleCombo()
                ],
                listeners: {
                    scope: this,
                    render: function(cmp)
                    {
                        cmp.doLayout();
                    }
                }
            });
        }
        return this.plotTypeOptionsToolbar;
    },

    getSecondPlotOptionsToolbar: function() {
      if(!this.plotTypeWithoutYOptionsToolbar)
      {
          this.plotTypeWithoutYOptionsToolbar =  Ext4.create('Ext.toolbar.Toolbar', {
              ui: 'footer',
              cls: 'levey-jennings-toolbar',
              layout: { pack: 'center' },
              items: [
                  {xtype: 'tbspacer'},
                  {xtype: 'tbspacer'},
                  {xtype: 'tbspacer'},
                  {xtype: 'tbspacer'},
                  this.getPlotTypeOptions(false)
              ],
              listeners: {
                  scope: this,
                  render: function(cmp)
                  {
                      cmp.doLayout();
                  }
              }
          });
      }
      return this.plotTypeWithoutYOptionsToolbar;
    },

    getPlotSizeOptions: function()
    {
        var plotSizeRadio = [{
            boxLabel  : 'Small',
            name      : 'largePlot',
            inputValue: false,
            checked   : !this.largePlot
        },{
            boxLabel  : 'Large',
            name      : 'largePlot',
            inputValue: true,
            checked   : this.largePlot
        }];

        return {
            xtype: 'radiogroup',
            fieldLabel: 'Plot Size',
            columns: 2,
            id: 'plot-size-radio-group',
            width: 180,
            items: plotSizeRadio,
            disabled: this.plotTypes.length < 2,
            cls: 'plot-size-radio-group',
            listeners: {
                scope: this,
                change: function(cmp, newVal, oldVal)
                {
                    this.largePlot = newVal.largePlot;
                    this.havePlotOptionsChanged = true;

                    this.setBrushingEnabled(false);
                    this.displayTrendPlot();
                }
            }
        }
    },

    getPlotTypeOptions: function(withYOptions)
    {
        var plotTypeCheckBoxes = [];
        var me = this;
        Ext4.each(LABKEY.targetedms.QCPlotHelperBase[withYOptions ? 'qcPlotTypesWithYOptions' : 'qcPlotTypesWithoutYOptions'], function(plotType){
            plotTypeCheckBoxes.push({
                boxLabel: plotType,
                name: (withYOptions ? 'plotTypes' : 'plotTypesWithoutYOptions'),
                inputValue: plotType,
                cls: 'qc-plot-type-checkbox',
                checked: this.isPlotTypeSelected(plotType),
                listeners: {
                    render: function(cmp)
                    {
                        cmp.getEl().on('mouseover', function () {
                            var calloutMgr = hopscotch.getCalloutManager();
                            calloutMgr.removeAllCallouts();
                            calloutMgr.createCallout({
                                id: Ext4.id(),
                                target: cmp.getEl().dom,
                                placement: 'top',
                                width: 300,
                                xOffset: -250,
                                arrowOffset: 270,
                                showCloseButton: false,
                                title: plotType + ' Plot Type',
                                content: me.getPlotTypeHelpTooltip(plotType)
                            });
                        }, this);

                        cmp.getEl().on('mouseout', function() {
                            hopscotch.getCalloutManager().removeAllCallouts();
                        }, this);
                    }
                }
            });
        }, this);

        return {
            xtype: 'checkboxgroup',
            fieldLabel: (withYOptions ? 'QC Plot Type' : undefined),
            columns: plotTypeCheckBoxes.length,
            items: plotTypeCheckBoxes,
            cls: 'plot-type-checkbox-group',
            id: (withYOptions ? 'qc-plot-type-with-y-options' : 'qc-plot-types'),
            width: (withYOptions ? 300 : undefined),
            listeners: {
                scope: this,
                change: function(cmp, newVal, oldVal)
                {
                    var newValues = newVal[withYOptions ? 'plotTypes' : 'plotTypesWithoutYOptions'];
                    var otherPlotTypeOptions = (withYOptions ? 'plotTypesWithoutYOptions' : 'plotTypes');

                    this.plotTypes = newValues ? Ext4.isArray(newValues) ? newValues : [newValues] : [];
                    var options = Ext4.getCmp((withYOptions ? 'qc-plot-types' : 'qc-plot-type-with-y-options')).getValue();
                    if (options && options[otherPlotTypeOptions]) {
                        if (Ext4.isArray(options[otherPlotTypeOptions])) {
                            this.plotTypes = this.plotTypes.concat(options[otherPlotTypeOptions]);
                        } else {
                            this.plotTypes.push(options[otherPlotTypeOptions]);
                        }
                    }
                    this.havePlotOptionsChanged = true;
                    Ext4.getCmp('plot-size-radio-group').setDisabled(this.plotTypes.length < 2);
                    this.setBrushingEnabled(false);
                    this.displayTrendPlot();
                }
            }
        }
    },

    getMainPlotOptionsToolbar : function()
    {
        if (!this.mainPlotOptionsToolbar)
        {
            this.mainPlotOptionsToolbar = Ext4.create('Ext.toolbar.Toolbar', {
                ui: 'footer',
                cls: 'levey-jennings-toolbar',
                padding: 10,
                layout: { pack: 'center' },
                items: [
                    this.getMetricCombo(),
                    {xtype: 'tbspacer'}, {xtype: 'tbseparator'}, {xtype: 'tbspacer'},
                    this.getDateRangeCombo()
                ]
            });
        }

        return this.mainPlotOptionsToolbar;
    },

    getThirdPlotOptionsToolbar : function()
    {
        if (!this.otherPlotOptionsToolbar)
        {
            var  toolbarItems = [];

            // only add the create guide set button if the user has the proper permissions to insert/update guide sets
            if (this.canUserEdit())
            {
                toolbarItems.push(this.getGuideSetCreateButton());
                toolbarItems.push({xtype: 'tbspacer'}, {xtype: 'tbseparator'}, {xtype: 'tbspacer'});
            }

            toolbarItems.push(this.getGroupedXCheckbox());
            toolbarItems.push({xtype: 'tbspacer'}, {xtype: 'tbseparator'}, {xtype: 'tbspacer'});
            toolbarItems.push(this.getSinglePlotCheckbox());
            toolbarItems.push({xtype: 'tbspacer'}, {xtype: 'tbseparator'}, {xtype: 'tbspacer'});
            toolbarItems.push(this.getShowExcludedCheckbox());
            // toolbarItems.push({xtype: 'tbspacer'}, {xtype: 'tbseparator'}, {xtype: 'tbspacer'});
            // toolbarItems.push(this.getShowPlotLegendButton());

            this.otherPlotOptionsToolbar = Ext4.create('Ext.toolbar.Toolbar', {
                ui: 'footer',
                cls: 'levey-jennings-toolbar',
                layout: { pack: 'center' },
                padding: '0 10px 10px 10px',
                items: toolbarItems
            });
        }

        return this.otherPlotOptionsToolbar;
    },

    getAnnotationFiltersToolbar : function()
    {
        if (!this.annotationFiltersToolbar)
        {
            this.annotationFiltersToolbar = Ext4.create('Ext.toolbar.Toolbar', {
                ui: 'footer',
                cls: 'levey-jennings-toolbar',
                padding: '0 10px 10px 10px',
                layout: { pack: 'center' },
                items: [
                    this.getAnnotationListTree(),
                    {xtype: 'tbspacer'},
                    this.getApplyAnnotationFiltersButton(),
                    {xtype: 'tbspacer'},
                    this.getClearAnnotationFiltersButton()
                ]
            });

            if(this.replicateAnnotationsNodes.length > 0)
            {
                var annotationsTree = this.getAnnotationListTree();
                var rootNode = annotationsTree.getRootNode();
                var annotations = this.selectedAnnotations;
                if(Object.keys(annotations).length > 0) {
                    rootNode.cascadeBy(function (node) {
                        if (!node.isRoot() && !node.isLeaf()) {
                            var annotationName = node.get('text');
                            var selected = annotations[annotationName];
                            if (selected) {
                                for (var i = 0; i < selected.length; i++) {
                                    var child = node.findChild('text', selected[i]);
                                    if (child) {
                                        child.set('checked', true);
                                    }
                                }
                            }
                        }
                    });
                    this.clearAnnotationFiltersButton.show();
                }
            }
            this.annotationFiltersToolbar.setVisible(this.replicateAnnotationsNodes.length > 0);
        }

        return this.annotationFiltersToolbar;
    },

    getSelectedAnnotationsToolbar: function()
    {
        if (!this.selectedAnnotationsToolbar)
        {
            this.selectedAnnotationsToolbar = Ext4.create('Ext.toolbar.Toolbar', {
                ui: 'footer',
                cls: 'levey-jennings-toolbar',
                padding: '0 0 0 0',
                layout: {pack: 'center'},
                hidden: true,
                items: []
            });

            if(Object.keys(this.selectedAnnotations).length > 0)
            {
                this.updateSelectedAnnotationsToolbar();
            }
        }
        return this.selectedAnnotationsToolbar;
    },

    getCustomDateRangeToolbar : function()
    {
        if (!this.customDateRangeToolbar)
        {
            this.customDateRangeToolbar = Ext4.create('Ext.toolbar.Toolbar', {
                ui: 'footer',
                cls: 'levey-jennings-toolbar',
                padding: '0 10px 10px 10px',
                hidden: this.dateRangeOffset > -1,
                layout: { pack: 'center' },
                items: [
                    this.getStartDateField(), {xtype: 'tbspacer'},
                    this.getEndDateField(), {xtype: 'tbspacer'},
                    this.getApplyDateRangeButton()
                ]
            });
        }

        return this.customDateRangeToolbar;
    },

    getGuideSetMessageToolbar : function()
    {
        if (!this.guideSetMessageToolbar)
        {
            this.guideSetMessageToolbar = Ext4.create('Ext.toolbar.Toolbar', {
                ui: 'footer',
                cls: 'guideset-toolbar-msg',
                hidden: true,
                layout: { pack: 'center' },
                items: [{
                    xtype: 'box',
                    itemId: 'GuideSetMessageToolBar',
                    html: 'Please click and drag in the plot to select the guide set training date range.'
                }]
            });
        }

        return this.guideSetMessageToolbar;
    },

    isValidQCPlotType: function(plotType)
    {
        var valid = false;
        Ext4.each(LABKEY.targetedms.QCPlotHelperBase.qcPlotTypesWithYOptions.concat(LABKEY.targetedms.QCPlotHelperBase.qcPlotTypesWithoutYOptions), function(type){
            if (plotType == type)
            {
                valid = true;
                return;
            }
        });
        return valid;
    },

    getInitialValuesFromUrlParams : function()
    {
        var urlParams = LABKEY.ActionURL.getParameters(),
            paramValues = {},
            alertMessage = '', sep = '',
            paramValue,
            metric;

        paramValue = urlParams['metric'];
        if (paramValue != undefined)
        {
            metric = this.validateMetricId(paramValue);
            if(metric == null)
            {
                alertMessage += "Invalid Metric, reverting to default metric.";
                sep = ' ';
            }
            else
            {
                paramValues['metric'] = metric;
            }
        }

        if (urlParams['startDate'] != undefined)
        {
            paramValue = new Date(urlParams['startDate']);
            if(paramValue == "Invalid Date")
            {
                alertMessage += sep + "Invalid Start Date, reverting to default start date.";
                sep = ' ';
            }
            else
            {
                paramValues['dateRangeOffset'] = -1; // force to custom date range selection
                paramValues['startDate'] = this.formatDate(Ext4.Date.parse(urlParams['startDate'], LABKEY.Utils.getDateTimeFormatWithMS()));
            }
        }

        if (urlParams['endDate'] != undefined)
        {
            paramValue = new Date(urlParams['endDate']);
            if(paramValue == "Invalid Date")
            {
                alertMessage += sep + "Invalid End Date, reverting to default end date.";
            }
            else
            {
                paramValues['dateRangeOffset'] = -1; // force to custom date range selection
                paramValues['endDate'] = this.formatDate(Ext4.Date.parse(urlParams['endDate'], LABKEY.Utils.getDateTimeFormatWithMS()));
            }
        }

        paramValue = urlParams['plotTypes'];
        if (paramValue != undefined)
        {
            var plotTypes = [];
            if (!Ext4.isArray(paramValue))
                paramValue = paramValue.split(',');

            Ext4.each(paramValue, function (value)
            {
                if (this.isValidQCPlotType(value.trim()))
                    plotTypes.push(value.trim());
            }, this);


            if (plotTypes.length == 0)
            {
                alertMessage += sep + "Invalid Plot Type, reverting to default plot type.";
            }
            else
            {
                paramValues['plotTypes'] = plotTypes;
            }
        }

        paramValue = urlParams['largePlot'];
        if (paramValue !== undefined && paramValue !== null)
        {
            paramValues['largePlot'] = paramValue.toString().toLowerCase() === 'true';
        }

        if (alertMessage.length > 0)
        {
            LABKEY.Utils.alert('Invalid URL Parameter(s)', alertMessage);
        }
        else if (Object.keys(paramValues).length > 0)
        {
            this.havePlotOptionsChanged = true;
            return paramValues;
        }

        return null;
    },

    validateMetricId : function(id)
    {
        for (var i = 0; i < this.metricPropArr.length; i++)
        {
            if (this.metricPropArr[i].id == id)
            {
                return this.metricPropArr[i].id;
            }
        }
        return null;
    },

    getYAxisOptions: function () {
        return {
            fields: ['value', 'display'],
            data: [['linear', 'Linear'], ['log', 'Log'], ['percentDeviation', 'Percent of Mean'], ['standardDeviation', 'Standard Deviations']]
        }
    },

    getScaleCombo : function()
    {
        if (!this.scaleCombo)
        {
            this.scaleCombo = Ext4.create('Ext.form.field.ComboBox', {
                id: 'scale-combo-box',
                width: 255,
                labelWidth: 80,
                fieldLabel: 'Y-Axis Scale',
                triggerAction: 'all',
                mode: 'local',
                store: Ext4.create('Ext.data.ArrayStore', this.getYAxisOptions()),
                valueField: 'value',
                displayField: 'display',
                value: this.yAxisScale,
                forceSelection: true,
                editable: false,
                listeners: {
                    scope: this,
                    change: function(cmp, newVal, oldVal)
                    {
                        this.yAxisScale = newVal;
                        this.havePlotOptionsChanged = true;

                        // call processPlotData instead of renderPlots so that we recalculate min y-axis scale for log
                        this.setLoadingMsg();
                        this.processPlotData(this.plotDataRows);
                    }
                }
            });
        }

        return this.scaleCombo;
    },

    getDateRangeCombo : function()
    {
        if (!this.dateRangeCombo)
        {
            this.dateRangeCombo = Ext4.create('Ext.form.field.ComboBox', {
                id: 'daterange-combo-box',
                width: 225,
                labelWidth: 75,
                fieldLabel: 'Date Range',
                triggerAction: 'all',
                mode: 'local',
                store: Ext4.create('Ext.data.ArrayStore', {
                    fields: ['value', 'display'],
                    data: [
                        [0, 'All dates'],
                        [7, 'Last 7 days'],
                        [15, 'Last 15 days'],
                        [30, 'Last 30 days'],
                        [90, 'Last 90 days'],
                        [180, 'Last 180 days'],
                        [365, 'Last 365 days'],
                        [-1, 'Custom range']
                    ]
                }),
                valueField: 'value',
                displayField: 'display',
                value: this.dateRangeOffset,
                forceSelection: true,
                editable: false,
                listeners: {
                    scope: this,
                    change: function(cmp, newVal, oldVal)
                    {
                        this.dateRangeOffset = newVal;
                        this.havePlotOptionsChanged = true;

                        var showCustomRangeItems = this.dateRangeOffset == -1;
                        this.getCustomDateRangeToolbar().setVisible(showCustomRangeItems);

                        if (!showCustomRangeItems)
                        {
                            // either use the min and max values based on the data
                            // or calculate range based on today's date and the offset
                            this.startDate = this.formatDate(this.calculateStartDateByOffset());
                            this.endDate = this.formatDate(this.calculateEndDateByOffset());

                            this.setBrushingEnabled(false);
                            this.displayTrendPlot();
                        }
                    }
                }
            });
        }

        return this.dateRangeCombo;
    },

    getStartDateField : function()
    {
        if (!this.startDateField)
        {
            this.startDateField = Ext4.create('Ext.form.field.Date', {
                id: 'start-date-field',
                width: 180,
                labelWidth: 65,
                fieldLabel: 'Start Date',
                value: this.startDate,
                allowBlank: false,
                format: 'Y-m-d',
                listeners: {
                    scope: this,
                    validitychange: function (df, isValid)
                    {
                        this.getApplyDateRangeButton().setDisabled(!isValid);
                    }
                }
            });
        }

        return this.startDateField;
    },

    getEndDateField : function()
    {
        if (!this.endDateField)
        {
            this.endDateField = Ext4.create('Ext.form.field.Date', {
                id: 'end-date-field',
                width: 175,
                labelWidth: 60,
                fieldLabel: 'End Date',
                value: this.endDate,
                allowBlank: false,
                format: 'Y-m-d',
                listeners: {
                    scope: this,
                    validitychange: function (df, isValid)
                    {
                        this.getApplyDateRangeButton().setDisabled(!isValid);
                    }
                }
            });
        }

        return this.endDateField;
    },

    getApplyDateRangeButton : function()
    {
        if (!this.applyFilterButton)
        {
            this.applyFilterButton = Ext4.create('Ext.button.Button', {
                text: 'Apply',
                handler: this.applyGraphFilterBtnClick,
                scope: this
            });
        }

        return this.applyFilterButton;
    },

    assignDefaultMetricIfNull: function ()
    {
        if (this.metric == null || isNaN(Number(this.metric)) || !this.getMetricPropsById(this.metric)) {
            var targetIndex = 0;
            for (var i = 0; i < this.metricPropArr.length; i++) {
                if (this.metricPropArr[i].name == 'Retention Time') {
                    targetIndex = i;
                }
            }
            if(this.metricPropArr.length > 0) {
                this.metric = this.metricPropArr[targetIndex].id;
            }
        }
    },

    getMetricCombo : function()
    {
        if (!this.metricField)
        {
            this.assignDefaultMetricIfNull();

            this.metricField = Ext4.create('Ext.form.field.ComboBox', {
                id: 'metric-type-field',
                width: 350,
                labelWidth: 50,
                fieldLabel: 'Metric',
                triggerAction: 'all',
                mode: 'local',
                store: Ext4.create('Ext.data.Store', {
                    fields: ['id', 'name'],
                    sorters: [{property: 'name'}],
                    data: this.metricPropArr
                }),
                valueField: 'id',
                displayField: 'name',
                value: this.metric,
                forceSelection: true,
                editable: false,
                listeners: {
                    scope: this,
                    change: function(cmp, newVal, oldVal)
                    {
                        this.metric = newVal;
                        this.havePlotOptionsChanged = true;

                        this.setBrushingEnabled(false);
                        this.displayTrendPlot();
                    }
                }
            });
        }

        return this.metricField;
    },

    getAnnotationListTree : function()
    {
        if (!this.annotationFiltersField)
        {
            var store = Ext4.create('Ext.data.TreeStore', {
                root: {expanded: false, children: this.replicateAnnotationsNodes},
            });

            this.annotationFiltersField = Ext4.create('Ext.tree.Panel', {
                id: 'annotation-filter-field',
                width: 440,
                height: 150,
                title: 'Replicate Annotations',
                store: store,
                rootVisible: false,
                titleCollapse: true,
                collapsed: true,
                collapsible: true,
                useArrows: true,
                lines: false,
            });
        }

        return this.annotationFiltersField;
    },

    getApplyAnnotationFiltersButton : function()
    {
        if (!this.applyAnnotationFiltersButton)
        {
            this.applyAnnotationFiltersButton = Ext4.create('Ext.button.Button', {
                text: 'Apply',
                handler: this.applyAnnotationFiltersBtnClick,
                scope: this
            });
        }

        return this.applyAnnotationFiltersButton;
    },

    getClearAnnotationFiltersButton : function()
    {
        if (!this.clearAnnotationFiltersButton)
        {
            this.clearAnnotationFiltersButton = Ext4.create('Ext.button.Button', {
                text: 'Clear',
                handler: this.clearAnnotationFiltersBtnClick,
                scope: this,
                hidden: true
            });
        }

        return this.clearAnnotationFiltersButton;
    },

    getGroupedXCheckbox : function()
    {
        if (!this.groupedXCheckbox)
        {
            this.groupedXCheckbox = Ext4.create('Ext.form.field.Checkbox', {
                id: 'grouped-x-field',
                boxLabel: 'Group X-Axis Values by Date',
                checked: this.groupedX,
                listeners: {
                    scope: this,
                    change: function(cb, newValue, oldValue)
                    {
                        this.groupedX = newValue;
                        this.havePlotOptionsChanged = true;

                        this.setBrushingEnabled(false);
                        this.setLoadingMsg();
                        this.getAnnotationData();
                    }
                }
            });
        }

        return this.groupedXCheckbox;
    },

    getSinglePlotCheckbox : function()
    {
        if (!this.peptidesInSinglePlotCheckbox)
        {
            this.peptidesInSinglePlotCheckbox = Ext4.create('Ext.form.field.Checkbox', {
                id: 'peptides-single-plot',
                boxLabel: 'Show All Series in a Single Plot',
                checked: this.singlePlot,
                listeners: {
                    scope: this,
                    change: function(cb, newValue, oldValue)
                    {
                        this.singlePlot = newValue;
                        this.havePlotOptionsChanged = true;

                        this.setBrushingEnabled(false);
                        this.setLoadingMsg();
                        this.getAnnotationData();
                    }
                }
            });
        }

        return this.peptidesInSinglePlotCheckbox;
    },

    getShowExcludedCheckbox : function()
    {
        if (!this.showExcludedPointsCheckbox)
        {
            this.showExcludedPointsCheckbox = Ext4.create('Ext.form.field.Checkbox', {
                id: 'show-excluded-points',
                boxLabel: 'Show Excluded Points',
                checked: this.showExcluded,
                listeners: {
                    scope: this,
                    change: function(cb, newValue, oldValue)
                    {
                        this.showExcluded = newValue;
                        this.havePlotOptionsChanged = true;

                        this.setLoadingMsg();
                        this.getAnnotationData();
                    }
                }
            });
        }

        return this.showExcludedPointsCheckbox;
    },

    getGuideSetCreateButton : function()
    {
        if (!this.createGuideSetToggleButton)
        {
            this.createGuideSetToggleButton = Ext4.create('Ext.button.Button', {
                text: 'Create Guide Set',
                tooltip: 'Enable/disable guide set creation mode',
                disabled: this.groupedX || this.singlePlot || this.isMultiSeries(),
                enableToggle: true,
                handler: function(btn) {
                    this.setBrushingEnabled(btn.pressed);
                },
                scope: this
            });
        }

        return this.createGuideSetToggleButton;
    },

    getShowPlotLegendButton : function()
    {
        if (!this.showPlotLegendButton)
        {
            var cmpId = Ext4.id();
            this.showPlotLegendButton = Ext4.create('Ext.button.Button', {
                text: 'View Legend',
                tooltip: 'View legends used for all plots',
                enableToggle: true,
                handler: function (btn)
                {
                    var plotHeight = this.singlePlot ? 500 : 300;
                    var me = this;
                    if (!btn.pressed)
                    {
                        if (this.plotLegendPopup)
                        {
                            this.plotLegendPopup.destroy();
                        }
                        return;
                    }

                    this.plotLegendPopup = Ext4.create('Ext.window.Window', {
                        buttonAlign: 'right',
                        width: 300,
                        height: plotHeight + 50,
                        border: false,
                        closable: false,
                        title: 'Legends',
                        draggable: true,
                        resizable: false,
                        cls: 'headerlegendpopup',
                        items: [{
                            html: {
                                tag: 'div', id: cmpId, width: '300', height: '\'' + plotHeight + '\''
                            }
                        }],
                        buttons: [{
                            text: 'Close',
                            onClick: function ()
                            {
                                me.plotLegendPopup.destroy();
                                btn.toggle();
                            }
                        }],
                        listeners: {
                            show: {
                                fn: function (cmp)
                                {
                                    this.lastPlotConfig.renderTo = cmpId;
                                    this.lastPlotConfig.height = plotHeight;
                                    var plot = LABKEY.vis.TrendingLinePlot(this.lastPlotConfig);
                                    plot.renderer.initCanvas();
                                    plot.grid = {topEdge: 30, rightEdge: 0};
                                    plot.renderer.renderLegend();
                                    cmp.doLayout();

                                }, scope: this
                            }
                        }

                    });

                    this.plotLegendPopup.show();
                },
                scope: this
            });
        }

        return this.showPlotLegendButton;
    },

    setBrushingEnabled : function(enabled) {
        // we don't currently allow creation of guide sets in single plot mode, grouped x-axis mode, or multi series mode
        this.getGuideSetCreateButton().setDisabled(this.groupedX || this.singlePlot || this.isMultiSeries());

        this.enableBrushing = enabled;
        this.clearPlotBrush();
        this.setPlotBrushingDisplayStyle();
        this.toggleGuideSetMsgDisplay();
        this.getGuideSetCreateButton().toggle(enabled);
    },

    setLoadingMsg : function() {
        Ext4.get(this.plotDivId).update("");
        Ext4.get(this.plotDivId).mask("Loading...");
    },

    displayTrendPlot: function() {
        hopscotch.getCalloutManager().removeAllCallouts();

        this.updateSelectedAnnotations();
        this.setLoadingMsg();
        this.getDistinctPrecursors();
    },

    getDistinctPrecursors: function() {

        this.assignDefaultMetricIfNull();

        var metricProps = this.getMetricPropsById(this.metric);

        if(metricProps) {
            var series1Sql = "SELECT SeriesLabel FROM " + metricProps.series1SchemaName + "." + metricProps.series1QueryName,
                    series2Sql = this.isMultiSeries() ? " UNION SELECT SeriesLabel FROM " + metricProps.series2SchemaName + "." + metricProps.series2QueryName : '',
                    separator = ' WHERE ';

            // CAST as DATE to ignore time portion of value
            if (this.startDate) {
                series1Sql += separator + "CAST(SampleFileId.AcquiredTime AS DATE) >= '" + this.startDate + "'";
                if (series2Sql.length > 0)
                    series2Sql += separator + "CAST(SampleFileId.AcquiredTime AS DATE) >= '" + this.startDate + "'";

                separator = " AND ";
            }
            if (this.endDate) {
                series1Sql += separator + "CAST(SampleFileId.AcquiredTime AS DATE) <= '" + this.endDate + "'";
                if (series2Sql.length > 0)
                    series2Sql += separator + "CAST(SampleFileId.AcquiredTime AS DATE) <= '" + this.endDate + "'";
            }

            var sql = "SELECT DISTINCT SeriesLabel FROM (\n" + series1Sql + series2Sql + "\n) X ORDER BY SeriesLabel";

            LABKEY.Query.executeSql({
                schemaName: 'targetedms',
                sql: sql,
                sort: 'SeriesLabel',
                scope: this,
                success: function (data) {
                    this.pagingStartIndex = 0;
                    this.pagingEndIndex = this.maxCount;

                    // stash the set of precursor series labels for use with the plot rendering
                    this.allPrecursors = Ext4.Array.pluck(data.rows, 'SeriesLabel');
                    this.setPrecursorsForPage();
                    this.getAnnotationData();
                },
                failure: this.failureHandler
            });
        }
        else {
            Ext4.get(this.plotDivId).update("There are no enabled QC Metric Configurations.");
        }
    },

    setPrecursorsForPage: function() {
        if (Ext4.isNumeric(LABKEY.ActionURL.getParameter('qcPlots.offset')))
            this.pagingStartIndex = parseInt(LABKEY.ActionURL.getParameter('qcPlots.offset'));

        if (this.pagingStartIndex < 0)
            this.pagingStartIndex = 0;
        else if (this.pagingStartIndex > this.allPrecursors.length)
            this.pagingStartIndex = this.allPrecursors.length - this.maxCount;

        this.pagingEndIndex = Math.min(this.pagingStartIndex + this.maxCount, this.allPrecursors.length);

        this.precursors = Ext4.Array.slice(this.allPrecursors, this.pagingStartIndex, this.pagingEndIndex);

        this.updatePaginationDiv();
    },

    updatePaginationDiv: function() {
        var exceedsPageLimit = this.allPrecursors.length > this.maxCount;

        var displayHtml = "", sep = "";
        if (exceedsPageLimit) {
            displayHtml += this.getPaginationTxt();
            sep = "&nbsp;&nbsp;&nbsp;";
            displayHtml += sep + this.getPaginationBtns();
        }
        Ext4.get(this.plotPaginationDivId).update(displayHtml);
        Ext4.get(this.plotPaginationDivId).setStyle("display", exceedsPageLimit ? "block" : "none");

        this.attachPagingListeners();
    },

    getPaginationTxt: function() {
        return "Showing <b>" + (this.pagingStartIndex+1) + " - " + this.pagingEndIndex + "</b> of <b>"
                + this.allPrecursors.length + "</b> precursors";
    },

    getPaginationBtns: function() {
        var btnHtml = '';

        btnHtml += '<span class="qc-paging-prev ' + (this.pagingStartIndex > 0 ? 'qc-paging-icon-enabled' : 'qc-paging-icon-disabled')
                + '"><i class="fa fa-angle-left"></i></span>';

        btnHtml += '<span class="qc-paging-next ' + (this.pagingEndIndex < this.allPrecursors.length ? 'qc-paging-icon-enabled' : 'qc-paging-icon-disabled')
                + '"><i class="fa fa-angle-right"></i></span>';

        return btnHtml;
    },

    attachPagingListeners: function() {
        var prevBtn = Ext4.DomQuery.selectNode('.qc-paging-prev');
        if (prevBtn && this.pagingStartIndex > 0) {
            Ext4.get(prevBtn).on('click', function() {
                window.location = LABKEY.ActionURL.buildURL(LABKEY.ActionURL.getController(), LABKEY.ActionURL.getAction(), null,
                    Ext4.apply(LABKEY.ActionURL.getParameters(), {'qcPlots.offset': Math.max(0, this.pagingStartIndex - this.maxCount)}));
            }, this);
        }

        var nextBtn = Ext4.DomQuery.selectNode('.qc-paging-next');
        if (nextBtn && this.pagingEndIndex < this.allPrecursors.length) {
            Ext4.get(nextBtn).on('click', function() {
                window.location = LABKEY.ActionURL.buildURL(LABKEY.ActionURL.getController(), LABKEY.ActionURL.getAction(), null,
                    Ext4.apply(LABKEY.ActionURL.getParameters(), {'qcPlots.offset': this.pagingEndIndex}));
            }, this);
        }
    },

    getAnnotationData: function() {
        var config = this.getReportConfig();

        var annotationSql = "SELECT qca.Date, qca.Description, qca.Created, qca.CreatedBy.DisplayName, qcat.Name, qcat.Color FROM qcannotation qca JOIN qcannotationtype qcat ON qcat.Id = qca.QCAnnotationTypeId";

        // Filter on start/end dates
        var separator = " WHERE ";
        if (config.StartDate) {
            annotationSql += separator + "Date >= '" + config.StartDate + "'";
            separator = " AND ";
        }
        if (config.EndDate) {
            annotationSql += separator + "Date <= '" + config.EndDate + "'";
        }

        LABKEY.Query.executeSql({
            schemaName: 'targetedms',
            sql: annotationSql,
            sort: 'Date',
            containerFilter: LABKEY.Query.containerFilter.currentPlusProjectAndShared,
            scope: this,
            success: this.processAnnotationData,
            failure: this.failureHandler
        });
    },

    processAnnotationData: function(data) {
        if(data) {
            this.annotationData = data.rows;
            this.annotationShape = LABKEY.vis.Scale.Shape()[4]; // 0: circle, 1: triangle, 2: square, 3: diamond, 4: X

            var dateCount = {};
            this.legendData = [];

            // if more than one type of legend present, add a legend header for annotations
        if (this.annotationData.length > 0 && (this.singlePlot || this.showMeanCUSUMPlot() || this.showVariableCUSUMPlot()))
        {
                this.legendData.push({
                    text: 'Annotations',
                    separator: true
                });
            }

        for (var i = 0; i < this.annotationData.length; i++)
        {
                var annotation = this.annotationData[i];
                var annotationDate = this.formatDate(Ext4.Date.parse(annotation['Date'], LABKEY.Utils.getDateTimeFormatWithMS()), !this.groupedX);

                // track if we need to stack annotations that fall on the same date
                if (!dateCount[annotationDate]) {
                    dateCount[annotationDate] = 0;
                }
                annotation.yStepIndex = dateCount[annotationDate];
                dateCount[annotationDate]++;

                // get unique annotation names and colors for the legend
            if (Ext4.Array.pluck(this.legendData, "text").indexOf(annotation['Name']) == -1)
            {
                    this.legendData.push({
                        text: annotation['Name'],
                        color: '#' + annotation['Color'],
                        shape: this.annotationShape
                    });
                }
            }

            this.prepareAndRenderQCPlot();
        }
    },

    getExportSVGStr: function(btn, extraMargin)
    {
        var svgStr = this.callParent([btn, extraMargin]);

        // issue 25066: pdf export has artifact of the brush resize handlers
        svgStr = svgStr.replace('class="e-resize-handle-rect"', 'class="e-resize-handle-rect" visibility="hidden"');
        svgStr = svgStr.replace('class="w-resize-handle-rect"', 'class="w-resize-handle-rect" visibility="hidden"');

        return svgStr;
    },

    showInvalidLogMsg : function(id, toShow)
    {
        if (toShow)
        {
            Ext4.get(id).update("<span style='font-style: italic;'>Log scale invalid for values &le; 0. "
                    + "Reverting to linear y-axis scale.</span>");
        }
    },

    plotPointHover : function(event, row, layerSel, point, valueName) {
        var showHoverTask = new Ext4.util.DelayedTask(),
            metricProps = this.getMetricPropsById(this.metric),
            me = this;

        showHoverTask.delay(500, function() {
            var calloutMgr = hopscotch.getCalloutManager(),
                hopscotchId = Ext4.id(),
                contentDivId = Ext4.id(),
                shiftLeft = (event.clientX || event.x) > (Ext4.getBody().getWidth() / 2),
                config = {
                    id: hopscotchId,
                    showCloseButton: true,
                    bubbleWidth: 450,
                    placement: 'top',
                    xOffset: shiftLeft ? -428 : -53,
                    arrowOffset: shiftLeft ? 410 : 35,
                    yOffset: me.canUserEdit() ? -375 : -270,
                    target: point,
                    content: '<div id="' + contentDivId + '"></div>',
                    onShow: function() {
                        me.attachHopscotchMouseClose();

                        Ext4.create('LABKEY.targetedms.QCPlotHoverPanel', {
                            renderTo: contentDivId,
                            pointData: row,
                            valueName: valueName,
                            metricProps: metricProps,
                            canEdit: me.canUserEdit(),
                            listeners: {
                                scope: me,
                                close: function() {
                                    calloutMgr.removeAllCallouts();
                                }
                            }
                        });
                    }
                };

            calloutMgr.removeAllCallouts();
            calloutMgr.createCallout(config);
        });

        // cancel the hover details show event if the user was just
        // passing over the point without stopping for X amount of time
        Ext4.get(point).on('mouseout', function() {
            showHoverTask.cancel();
        }, this);
    },

    attachHopscotchMouseClose: function() {
        var closeTask = new Ext4.util.DelayedTask();
        var h = Ext4.select('.hopscotch-bubble-container');

        // on mouseout call the delayed task to close the callout
        h.on('mouseout', function() {
            closeTask.delay(1000, function() {
                hopscotch.getCalloutManager().removeAllCallouts();
            });
        });

        // if the mouseover happens again for this element before the delay, cancel it to keep callout open
        h.on('mouseover', function() {
            closeTask.cancel();
        });
    },

    plotBrushStartEvent : function(plot) {
        this.clearPlotBrush(plot);
    },

    plotBrushEvent : function(extent, plot, layers) {
        Ext4.each(layers, function(layer){
            var points = layer.selectAll('.point path');
            if (points[0].length > 0)
            {
                var colorAcc = function(d) {
                    var x = plot.scales.x.scale(d.seqValue);
                    d.isInSelection = (x > extent[0][0] && x < extent[1][0]);
                    return d.isInSelection ? 'rgba(20, 204, 201, 1)' : '#000000';
                };

                points.attr('fill', colorAcc).attr('stroke', colorAcc);
            }
        });
    },

    plotBrushEndEvent : function(data, extent, plot) {
        var selectedPoints = Ext4.Array.filter(data, function(point){ return point.isInSelection; });
        this.plotBrushSelection = {plot: plot, points: selectedPoints};

        // add the guide set create and cancel buttons over the brushed region
        if (selectedPoints.length > 0)
        {
            var me = this;
            var xMid = extent[0][0] + (extent[1][0] - extent[0][0]) / 2;

            var createBtn = this.createGuideSetSvgButton(plot, 'Create', xMid - 57, 50);
            createBtn.on('click', function() {
                me.createGuideSetBtnClick();
            });

            var cancelBtn = this.createGuideSetSvgButton(plot, 'Cancel', xMid + 3, 49);
            cancelBtn.on('click', function () {
                me.clearPlotBrush(plot);
                plot.clearBrush();
                me.setBrushingEnabled(false);
            });

            this.bringSvgElementToFront(plot, "g.guideset-svg-button");
        }
    },

    plotBrushClearEvent : function(data, plot) {
        this.plotBrushSelection = undefined;
    },

    canUserEdit : function() {
        return LABKEY.user.canInsert && LABKEY.user.canUpdate;
    },

    allowGuideSetBrushing : function() {
        return this.canUserEdit() && !this.groupedX;
    },

    createGuideSetSvgButton : function(plot, text, xLeftPos, width) {
        var yRange = plot.scales.yLeft.range;
        var yTopPos = yRange[1] + (yRange[0] - yRange[1]) / 2 - 10;

        var svgBtn = this.getSvgElForPlot(plot).append('g')
                .attr('class', 'guideset-svg-button');

        svgBtn.append('rect')
                .attr('x', xLeftPos).attr('y', yTopPos).attr('rx', 5).attr('ry', 5)
                .attr('width', width).attr('height', 20)
                .style({'fill': '#ffffff', 'stroke': '#b4b4b4'});

        svgBtn.append('text').text(text)
                .attr('x', xLeftPos + 5).attr('y', yTopPos + 14)
                .style({'fill': '#126495', 'font-size': '10px', 'font-weight': 'bold', 'text-transform': 'uppercase'});

        return svgBtn;
    },

    setPlotBrushingDisplayStyle : function() {
        // hide the brushing related components for all plots if not in "create guide set" mode
        var displayStyle = this.enableBrushing ? 'inline' : 'none';
        d3.selectAll('.brush').style({'display': displayStyle});
        d3.selectAll('.x-axis-handle').style({'display': displayStyle});
    },

    clearPlotBrush : function(plot) {
        // clear any create/cancel buttons and brush areas from other plots
        if (this.plotBrushSelection) {
            this.getSvgElForPlot(this.plotBrushSelection.plot).selectAll(".guideset-svg-button").remove();

            if (this.plotBrushSelection.plot != plot) {
                this.plotBrushSelection.plot.clearBrush();
            }
        }
    },

    getSvgElForPlot : function(plot) {
        return d3.select('#' + plot.renderTo + ' svg');
    },

    toggleGuideSetMsgDisplay : function() {
        var toolbarMsg = this.down('#GuideSetMessageToolBar');
        toolbarMsg.up('toolbar').setVisible(this.enableBrushing);
    },

    addGuideSetTrainingRangeToPlot : function(plot, precursorInfo) {
        var me = this;
        var guideSetTrainingData = [];

        // find the x-axis starting and ending index based on the guide set information attached to each data point
        Ext4.Object.each(this.guideSetDataMap, function(guideSetId, guideSetData)
        {
            // only compare guide set info for matching precursor fragment
            if (!this.singlePlot && guideSetData.Series[precursorInfo.fragment] == undefined) {
                return true; // continue
            }

            var seriesTypes = [];
            for (var series in guideSetData.Series[precursorInfo.fragment]) {
                if (guideSetData.Series[precursorInfo.fragment].hasOwnProperty(series)) {
                    seriesTypes.push(series);
                }
            }

            var gs = {GuideSetId: guideSetId,
                      series: seriesTypes[0]};
            for (var j = 0; j < precursorInfo.data.length; j++)
            {
                // only use data points that match the GuideSet RowId and are in the training set range
                if (precursorInfo.data[j].guideSetId == gs.GuideSetId && precursorInfo.data[j].inGuideSetTrainingRange)
                {
                    if (gs.StartIndex == undefined)
                    {
                        gs.StartIndex = precursorInfo.data[j].seqValue;
                    }
                    gs.EndIndex = precursorInfo.data[j].seqValue;
                }
            }

            if (gs.StartIndex != undefined)
            {
                guideSetTrainingData.push(gs);
            }
        }, this);

        if (guideSetTrainingData.length > 0)
        {
            // add a "shaded" rect to indicate which points in the plot are part of the guide set training range
            var binWidth = (plot.grid.rightEdge - plot.grid.leftEdge) / (plot.scales.x.scale.domain().length);
            var yRange = plot.scales.yLeft.range;

            var xAcc = function (d) {
                return plot.scales.x.scale(d.StartIndex) - (binWidth/2);
            };

            var widthAcc = function (d) {
                return plot.scales.x.scale(d.EndIndex) - plot.scales.x.scale(d.StartIndex) + binWidth;
            };

            var guideSetTrainingRange = this.getSvgElForPlot(plot).selectAll("rect.training").data(guideSetTrainingData)
                .enter().append("rect").attr("class", "training")
                .attr('x', xAcc).attr('y', yRange[1])
                .attr('width', widthAcc).attr('height', yRange[0] - yRange[1])
                .attr('stroke', '#000000').attr('stroke-opacity', 0.1)
                .attr('fill', '#000000').attr('fill-opacity', 0.1);

            guideSetTrainingRange.append("title").text(function (d) {
                var guideSetInfo = me.guideSetDataMap[d.GuideSetId],
                    seriesGuideSetInfo = guideSetInfo.Series[precursorInfo.fragment][d.series],
                    numRecs = seriesGuideSetInfo ? seriesGuideSetInfo.NumRecords : 0,
                    showGuideSetStats = !me.singlePlot && numRecs > 0,
                    mean, stdDev, percentCV;

                if (showGuideSetStats)
                {
                    mean = me.formatNumeric(seriesGuideSetInfo.Mean);
                    stdDev = me.formatNumeric(seriesGuideSetInfo.StandardDev);
                    percentCV = me.formatNumeric((stdDev / mean) * 100);
                }

                return "Guide Set ID: " + d.GuideSetId + ","
                    + "\nStart: " + me.formatDate(Ext4.Date.parse(guideSetInfo.TrainingStart, LABKEY.Utils.getDateTimeFormatWithMS()), true)
                    + ",\nEnd: " + me.formatDate(Ext4.Date.parse(guideSetInfo.TrainingEnd, LABKEY.Utils.getDateTimeFormatWithMS()), true)
                    + (showGuideSetStats ? ",\n# Runs: " + numRecs : "")
                    + (showGuideSetStats ? ",\nMean: " + mean : "")
                    + (showGuideSetStats ? ",\nStd Dev: " + stdDev : "")
                    + (showGuideSetStats ? ",\n%CV: " + percentCV : "")
                    + (guideSetInfo.Comment ? (",\nComment: " + guideSetInfo.Comment) : "");
            });
        }

        // Issue 32277: need to move the data points in front of the guide set range display
        // so that points can be interacted with (i.e. hover to exclude, see details, etc.)
        this.bringSvgElementToFront(plot, "a.point");
    },

    bringSvgElementToFront: function(plot, selector) {
        this.getSvgElForPlot(plot).selectAll(selector)
            .each(function() {
               this.parentNode.parentNode.appendChild(this.parentNode);
            });
    },

    addAnnotationsToPlot: function(plot, precursorInfo) {
        var me = this;

        var xAxisLabels = Ext4.Array.pluck(precursorInfo.data, "fullDate");
        if (this.groupedX)
        {
            xAxisLabels = [];

            // determine the annotation index based on the "date" but unique values are based on "groupedXTick"
            var prevGroupedXTick = null;
            Ext4.each(precursorInfo.data, function(row)
            {
                if (row['groupedXTick'] != prevGroupedXTick)
                {
                    xAxisLabels.push(row['date']);
                }
                prevGroupedXTick = row['groupedXTick'];
            });
        }

        // use direct D3 code to inject the annotation icons to the rendered SVG
        var xAcc = function(d) {
            var annotationDate = me.formatDate(Ext4.Date.parse(d['Date'], LABKEY.Utils.getDateTimeFormatWithMS()), !me.groupedX);
            return plot.scales.x.scale(xAxisLabels.indexOf(annotationDate));
        };
        var yAcc = function(d) {
            return plot.scales.yLeft.range[1] - (d['yStepIndex'] * 12) - 12;
        };
        var transformAcc = function(d){
            return 'translate(' + xAcc(d) + ',' + yAcc(d) + ')';
        };
        var colorAcc = function(d) {
            return '#' + d['Color'];
        };
        var annotations = this.getSvgElForPlot(plot).selectAll("path.annotation").data(this.annotationData)
            .enter().append("path").attr("class", "annotation")
            .attr("d", this.annotationShape(5)).attr('transform', transformAcc)
            .style("fill", colorAcc).style("stroke", colorAcc);

        // add hover text for the annotation details
        annotations.append("title")
            .text(function(d) {
                return "Created By: " + d['DisplayName'] + ", "
                        + "\nType: " + d['Name'] + ", "
                    + "\nDate: " + me.formatDate(Ext4.Date.parse(d['Date'], LABKEY.Utils.getDateTimeFormatWithMS()), true) + ", "
                    + "\nDescription: " + d['Description'];
            });

        // add some mouseover effects for fun
        var mouseOn = function(pt, strokeWidth) {
            d3.select(pt).transition().duration(800).attr("stroke-width", strokeWidth).ease("elastic");
        };
        var mouseOff = function(pt) {
            d3.select(pt).transition().duration(800).attr("stroke-width", 1).ease("elastic");
        };
        annotations.on("mouseover", function(){ return mouseOn(this, 3); });
        annotations.on("mouseout", function(){ return mouseOff(this); });
    },

    formatDate: function(d, includeTime) {
        if (d instanceof Date) {
            if (includeTime) {
                return Ext4.util.Format.date(d, 'Y-m-d H:i:s');
            }
            else {
                return Ext4.util.Format.date(d, 'Y-m-d');
            }
        }
        else if (typeof(d) === 'string' && d.length === 19) {
            // support format of strings like "2013-08-27 14:45:49"
            return includeTime ? d : d.substring(0, d.indexOf(' '));
        }
        else {
            return d;
        }
    },

    formatNumeric: function(val) {
        if (LABKEY.vis.isValid(val)) {
            if (val > 100000 || val < -100000) {
                return val.toExponential(3);
            }
            return parseFloat(val.toFixed(3));
        }
        return "N/A";
    },

    getReportConfig: function() {
        var config = { metric: this.metric };

        if (this.startDate) {
            config['StartDate'] = this.formatDate(this.startDate);
        }
        if (this.endDate) {
            config['EndDate'] = this.formatDate(this.endDate);
        }

        return config;
    },

    applyGraphFilterBtnClick: function() {
        var startDateRawValue = this.getStartDateField().getRawValue(),
            startDateValue = this.getStartDateField().getValue(),
            endDateRawValue = this.getEndDateField().getRawValue(),
            endDateValue = this.getEndDateField().getValue();

        // make sure that at least one filter field is not null
        if (startDateRawValue == '' && endDateRawValue == '')
        {
            Ext4.Msg.show({
                title:'ERROR',
                msg: 'Please enter a value for filtering.',
                buttons: Ext4.Msg.OK,
                icon: Ext4.MessageBox.ERROR
            });
        }
        // verify that the start date is not after the end date
        else if (startDateValue > endDateValue && endDateValue != '')
        {
            Ext4.Msg.show({
                title:'ERROR',
                msg: 'Please enter an end date that does not occur before the start date.',
                buttons: Ext4.Msg.OK,
                icon: Ext4.MessageBox.ERROR
            });
        }
        else
        {
            // get date values without the time zone info
            this.startDate = startDateRawValue;
            this.endDate = endDateRawValue;
            this.havePlotOptionsChanged = true;

            this.setBrushingEnabled(false);
            this.displayTrendPlot();
        }
    },

    applyAnnotationFiltersBtnClick: function()
    {
        // make sure that at least one filter is selected
        if (this.getAnnotationListTree().getChecked().length == 0)
        {
            Ext4.Msg.show({
                title:'ERROR',
                msg: 'Please select a replicate annotation.',
                buttons: Ext4.Msg.OK,
                icon: Ext4.MessageBox.ERROR
            });
        }

        else
        {
            this.setBrushingEnabled(false);
            this.displayTrendPlot();
        }
    },

    updateSelectedAnnotations: function() {

        if(!this.annotationFiltersField)
            return;

        var filters = this.annotationFiltersField.getChecked();

        this.selectedAnnotations = {};

        for(var i = 0; i < filters.length; i++)
        {
            var annotation = filters[i];
            var annotationName = annotation.parentNode.get('text');
            var annotationValue = annotation.get('text');

            var selected = this.selectedAnnotations[annotationName];
            if(!selected)
            {
                selected = [];
                this.selectedAnnotations[annotationName] = selected;
            }

            selected.push(annotationValue);
        }

        if(Object.keys(this.selectedAnnotations).length > 0)
        {
            this.clearAnnotationFiltersButton.show();
        }
        else
        {
            this.clearAnnotationFiltersButton.hide();
        }

        this.updateSelectedAnnotationsToolbar();
        this.havePlotOptionsChanged = true;
        this.annotationFiltersField.collapse();
    },

    updateSelectedAnnotationsToolbar: function()
    {
        var selectedAnnotationsTb = this.selectedAnnotationsToolbar;
        if(!selectedAnnotationsTb)
            return;

        selectedAnnotationsTb.removeAll();
        var selectedDisplay = '';
        var and = '';
        Ext4.Object.each(this.selectedAnnotations, function(name, values)
        {
            selectedDisplay += and;
            and = 'AND ';
            selectedDisplay += (name + ' (');
            for(var i = 0; i < values.length; i++)
            {
                if(i > 0) selectedDisplay += ' OR ';
                selectedDisplay += values[i];
            }
            selectedDisplay += ') ';
        });
        if(selectedDisplay.length > 0)
        {
            selectedDisplay = "Selected annotations: " + selectedDisplay;
            selectedAnnotationsTb.add(selectedDisplay);
            selectedAnnotationsTb.show();
        }
        else
        {
            selectedAnnotationsTb.hide();
        }
    },

    clearAnnotationFiltersBtnClick: function() {

        this.selectedAnnotations = {};
        var annotationsTree = this.getAnnotationListTree();
        var records = annotationsTree.getChecked();

        if(records.length == 0)
        {
            return;
        }

        for(var i = 0; i < records.length; i++)
        {
            records[i].set('checked', false);
        }

        this.havePlotOptionsChanged = true;
        this.clearAnnotationFiltersButton.hide();

        this.setBrushingEnabled(false);
        this.displayTrendPlot();
    },
    
    createGuideSetBtnClick: function() {
        var minGuideSetPointCount = 5; // to warn user if less than this many points are selected for the new guide set

        if (this.plotBrushSelection && this.plotBrushSelection.points.length > 0)
        {
            var startDate = this.plotBrushSelection.points[0]['fullDate'];
            var endDate = this.plotBrushSelection.points[this.plotBrushSelection.points.length - 1]['fullDate'];

            if (this.plotBrushSelection.points.length < minGuideSetPointCount) {
                Ext4.Msg.show({
                    title:'Create Guide Set Warning',
                    icon: Ext4.MessageBox.WARNING,
                    msg: 'Fewer than ' + minGuideSetPointCount + ' data points were selected for the new guide set, which may not be statistically significant. Would you like to proceed anyway?',
                    buttons: Ext4.Msg.YESNO,
                    scope: this,
                    fn: function(btnId, text, opt){
                        if(btnId == 'yes'){
                            this.insertNewGuideSet(startDate, endDate);
                        }
                    }
                });
            }
            else {
                this.insertNewGuideSet(startDate, endDate);
            }
        }
    },

    insertNewGuideSet : function(startDate, endDate) {
        LABKEY.Query.insertRows({
            schemaName: 'targetedms',
            queryName: 'GuideSet',
            rows: [{TrainingStart: startDate, TrainingEnd: endDate}],
            success: function(data) {
                this.plotBrushSelection = undefined;
                this.setBrushingEnabled(false);

                // issue 26019: since guide sets won't be created that often and we now remember plot option selections,
                // force page reload for new guide set creation this allows the sample file information to be updated
                // easily in the QC Summary webpart (which is commonly displayed on the same page as this plot).
                window.location.reload();
            },
            failure: function(response) {
                Ext4.Msg.show({
                    title:'Error Creating Guide Set',
                    icon: Ext4.MessageBox.ERROR,
                    msg: response.exception,
                    buttons: Ext4.Msg.OK
                });
            },
            scope: this
        })
    },

    persistSelectedFormOptions : function()
    {
        if (this.havePlotOptionsChanged)
        {
            this.havePlotOptionsChanged = false;
            LABKEY.Ajax.request({
                url: LABKEY.ActionURL.buildURL('targetedms', 'leveyJenningsPlotOptions.api'),
                method: 'POST',
                params: this.getSelectedPlotFormOptions()
            });
        }
    },

    getSelectedPlotFormOptions : function()
    {
        var annotationsProp = [];

        Ext4.Object.each(this.selectedAnnotations, function(name, values)
        {
            for(var i = 0; i < values.length; i++)
            {
                annotationsProp.push(name + ":" + values[i]);
            }
        });

        var props = {
            metric: this.metric,
            plotTypes: this.plotTypes,
            largePlot: this.largePlot,
            yAxisScale: this.yAxisScale,
            groupedX: this.groupedX,
            singlePlot: this.singlePlot,
            showExcluded: this.showExcluded,
            dateRangeOffset: this.dateRangeOffset,
            selectedAnnotations: annotationsProp
        };

        // set start and end date to null unless we are
        props.startDate = this.dateRangeOffset == -1 ? this.formatDate(this.startDate) : null;
        props.endDate = this.dateRangeOffset == -1 ? this.formatDate(this.endDate) : null;

        return props;
    },

    getMaxStackedAnnotations : function() {
        if (this.annotationData.length > 0) {
            return Math.max.apply(Math, (Ext4.Array.pluck(this.annotationData, "yStepIndex"))) + 1;
        }
        return 0;
    },

    getColorRange: function()
    {
        return LABKEY.vis.Scale.ColorDiscrete().concat(LABKEY.vis.Scale.DarkColorDiscrete());
    }

});
