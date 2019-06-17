/*
 * Copyright (c) 2017-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext4.define('LABKEY.targetedms.QCPlotHoverPanel', {
    extend: 'Ext.panel.Panel',
    border: false,

    pointData: null,
    valueName: null,
    metricProps: null,
    originalStatus: 0,
    existingExclusions: null,
    canEdit: false,

    STATE: {
        INCLUDE: 0,
        EXCLUDE_METRIC: 1,
        EXCLUDE_ALL: 2
    },

    initComponent : function() {
        if (this.pointData == null) {
            this.pointData = {};
        }
        if (this.metricProps == null) {
            this.metricProps = {};
        }

        this.items = [];
        this.callParent();
        this.getExistingReplicateExclusions();
    },

    getExistingReplicateExclusions : function() {
        if (Ext4.isNumber(this.pointData['ReplicateId'])) {
            LABKEY.Query.selectRows({
                schemaName: 'targetedms',
                queryName: 'QCMetricExclusion',
                filterArray: [LABKEY.Filter.create('ReplicateId', this.pointData['ReplicateId'])],
                scope: this,
                success: function(data) {
                    this.existingExclusions = data.rows;

                    // set the initial status for this point based on the existing exclusions
                    var metricIds = Ext4.Array.pluck(this.existingExclusions, 'MetricId');
                    if (metricIds.indexOf(null) > -1) {
                        this.originalStatus = this.STATE.EXCLUDE_ALL;
                    }
                    else if (metricIds.indexOf(this.metricProps.id) > -1) {
                        this.originalStatus = this.STATE.EXCLUDE_METRIC;
                    }

                    this.initializePanel();
                }
            });
        }
    },

    initializePanel : function() {
        if (this.pointData[this.valueName + 'Title'] != undefined) {
            this.add(this.getPlotPointDetailField('Metric', this.pointData[this.valueName + 'Title']));
        }

        this.add(this.getPlotPointDetailField(this.pointData['dataType'], this.pointData['fragment'], 'qc-hover-value-break'));

        if (this.valueName.indexOf('CUSUM') > -1) {
            this.add(this.getPlotPointDetailField('Group', 'CUSUMmN' == this.valueName || 'CUSUMvN' == this.valueName ? 'CUSUM-' : 'CUSUM+'));
        }

        this.add(this.getPlotPointDetailField('m/z', this.pointData['mz']));
        this.add(this.getPlotPointDetailField('Acquired', this.pointData['fullDate']));
        if (this.pointData.conversion && this.pointData.rawValue !== undefined && this.valueName.indexOf("CUSUM") === -1) {
            if (this.pointData.conversion === 'percentDeviation') {
                this.add(this.getPlotPointDetailField('Value', this.pointData.rawValue));
                this.add(this.getPlotPointDetailField('Percent of Mean', (this.valueName ? this.pointData[this.valueName] : this.pointData['value']) + '%'))
            }
            else if (this.pointData.conversion === 'standardDeviation') {
                this.add(this.getPlotPointDetailField('Value', this.pointData.rawValue));
                this.add(this.getPlotPointDetailField('Standard Deviations', this.valueName ? this.pointData[this.valueName] : this.pointData['value']))
            }
            else {
                this.add(this.getPlotPointDetailField('Value', this.valueName ? this.pointData[this.valueName] : this.pointData['value']));
            }
        }
        else {
            this.add(this.getPlotPointDetailField('Value', this.valueName ? this.pointData[this.valueName] : this.pointData['value']));
        }
        this.add(this.getPlotPointDetailField('File Path', this.pointData['FilePath'].replace(/\\/g, '\\<wbr>').replace(/\//g, '\/<wbr>')));

        if (this.canEdit) {
            this.add(this.getPlotPointExclusionPanel());
        }
        else {
            this.add(this.getPlotPointDetailField('Status', this.pointData['IgnoreInQC'] ? 'Not included in QC' : 'Included in QC'));
        }

        this.add(Ext4.create('Ext.Component', { html: this.getPlotPointClickLink() }));
    },

    getPlotPointDetailField : function(label, value, includeCls) {
        return Ext4.create('Ext.form.field.Display', {
            cls: 'qc-hover-field' + (Ext4.isString(includeCls) ? ' ' + includeCls : ''),
            fieldLabel: label,
            labelWidth: 75,
            width: 450,
            value: value
        });
    },

    getPlotPointExclusionPanel : function() {
        if (!this.exclusionPanel) {
            this.exclusionPanel = Ext4.create('Ext.form.Panel', {
                border: false,
                margin: '10px 0',
                style: 'border-top: solid #eeeeee 1px; border-bottom: solid #eeeeee 1px;',
                items: [this.getPlotPointExclusionRadioGroup()],
                dockedItems: [{
                    xtype: 'toolbar',
                    dock: 'bottom',
                    ui: 'footer',
                    padding: '0 0 10px 0',
                    items: ['->', this.getPlotPointExclusionSaveBtn()]
                }]
            });
        }

        return this.exclusionPanel;
    },

    getPlotPointExclusionSaveBtn : function() {
        if (!this.exclusionsSaveBtn) {
            this.exclusionsSaveBtn = Ext4.create('Ext.button.Button', {
                text: 'Save',
                disabled: true,
                scope: this,
                handler: function() {
                    var newStatus = this.getPlotPointExclusionRadioGroup().getValue().status;
                    if (newStatus != this.originalStatus) {
                        this.getEl().mask();

                        // Scenarios:
                        // 1 - from include to exclude metric - insert new row with MetricId
                        // 2 - from include to exclude all - delete all for replicate and then insert new row without MetricId
                        // 3 - from exclude metric to include - delete row for MetricId
                        // 4 - from exclude metric to exclude all - delete all for replicate and then insert new row without MetricId
                        // 5 - from exclude all to include - delete all for replicate
                        // 6 - from exclude all to exclude metric - delete all for replicate and then insert new row with MetricId
                        var s1 = this.originalStatus == this.STATE.INCLUDE && newStatus == this.STATE.EXCLUDE_METRIC;
                        var s2 = this.originalStatus == this.STATE.INCLUDE && newStatus == this.STATE.EXCLUDE_ALL;
                        var s3 = this.originalStatus == this.STATE.EXCLUDE_METRIC && newStatus == this.STATE.INCLUDE;
                        var s4 = this.originalStatus == this.STATE.EXCLUDE_METRIC && newStatus == this.STATE.EXCLUDE_ALL;
                        var s5 = this.originalStatus == this.STATE.EXCLUDE_ALL && newStatus == this.STATE.INCLUDE;
                        var s6 = this.originalStatus == this.STATE.EXCLUDE_ALL && newStatus == this.STATE.EXCLUDE_METRIC;

                        var commands = [];

                        if (this.existingExclusions.length > 0) {
                            // for scenarios s2, s4, s5, and s6 - delete all existing exclusions for this replicate
                            if (s2 || s4 || s5 || s6) {
                                commands.push({
                                    schemaName: 'targetedms',
                                    queryName: 'QCMetricExclusion',
                                    command: 'delete',
                                    rows: this.existingExclusions
                                });
                            }

                            // for scenario s3 - delete the existing exclusion for this replicate/metric
                            if (s3) {
                                commands.push({
                                    schemaName: 'targetedms',
                                    queryName: 'QCMetricExclusion',
                                    command: 'delete',
                                    rows: [this.existingExclusions[Ext4.Array.pluck(this.existingExclusions, 'MetricId').indexOf(this.metricProps.id)]]
                                });
                            }
                        }

                        // for scenarios s1 and s6 - insert a new exclusion for this replicate/metric
                        if (s1 || s6) {
                            commands.push({
                                schemaName: 'targetedms',
                                queryName: 'QCMetricExclusion',
                                command: 'insert',
                                rows: [{ ReplicateId: this.pointData['ReplicateId'], MetricId: this.metricProps.id }]
                            });
                        }

                        // for scenarios s2 and s4 - insert a new exclusion for this replicate without a metric value
                        if (s2 || s4) {
                            commands.push({
                                schemaName: 'targetedms',
                                queryName: 'QCMetricExclusion',
                                command: 'insert',
                                rows: [{ ReplicateId: this.pointData['ReplicateId'] }]
                            });
                        }

                        LABKEY.Query.saveRows({
                            commands: commands,
                            scope: this,
                            success: function(data) {
                                // Issue 30343: need to reload the full page because the QC Summary webpart might be
                                // present and need to be updated according to the updated exclusion state.
                                window.location.reload();
                            }
                        });
                    }
                    else {
                        this.fireEvent('close');
                    }
                }
            });
        }

        return this.exclusionsSaveBtn;
    },

    getPlotPointExclusionRadioGroup : function() {
        if (!this.exclusionRadioGroup) {
            this.exclusionRadioGroup = Ext4.create('Ext.form.RadioGroup', {
                cls: 'qc-hover-field',
                fieldLabel: 'Status',
                labelWidth: 75,
                padding: '10px 0 0 0',
                width: 450,
                columns: 1,
                items: [{
                    boxLabel: 'Include in QC', name: 'status',
                    inputValue: this.STATE.INCLUDE,
                    checked: this.originalStatus == this.STATE.INCLUDE
                },{
                    boxLabel: 'Exclude sample from QC for this metric', name: 'status',
                    inputValue: this.STATE.EXCLUDE_METRIC,
                    checked: this.originalStatus == this.STATE.EXCLUDE_METRIC
                },{
                    boxLabel: 'Exclude sample from QC for all metrics', name: 'status',
                    inputValue: this.STATE.EXCLUDE_ALL,
                    checked: this.originalStatus == this.STATE.EXCLUDE_ALL
                }],
                listeners: {
                    scope: this,
                    change: function(cmp, newVal, oldVal) {
                        this.getPlotPointExclusionSaveBtn().setDisabled(newVal.status == this.originalStatus);
                    }
                }
            });
        }

        return this.exclusionRadioGroup;
    },

    getPlotPointClickLink : function() {
        //Choose action target based on precursor type
        var action = this.pointData['dataType'] == 'Peptide' ? "precursorAllChromatogramsChart" : "moleculePrecursorAllChromatogramsChart",
            url = LABKEY.ActionURL.buildURL('targetedms', action, LABKEY.ActionURL.getContainer(), {
                id: this.pointData['PrecursorId'],
                chromInfoId: this.pointData['PrecursorChromInfoId']
            });

        return LABKEY.Utils.textLink({
            text: 'View Chromatogram',
            href: url + '#ChromInfo' + this.pointData['PrecursorChromInfoId']
        });
    }
});