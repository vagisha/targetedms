/*
 * Copyright (c) 2016-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
/**
 * Created by iansigmon on 4/13/16.
 */


/**
 * Open a dialog to enter some basic info prior to making the request to the Clustergrammer service
 */
Ext4.define('LABKEY.targetedms.Clustergrammer', {

    extend: 'Ext.panel.Panel',
    statics: {
        showDialog: function () {
            LABKEY.DataRegion.getSelected({
                selectionKey: LABKEY.DataRegions.TargetedMSRuns.selectionKey,
                success: function (selection) {
                    Ext4.create('LABKEY.targetedms.Clustergrammer', {
                        selectedRowIds: selection.selected
                    });
                }
            });
        }
    },
    fileColumnName: 'File/FileName',
    initComponent : function() {
        Ext4.tip.QuickTipManager.init();
        this.callParent();

        LABKEY.Query.selectRows({
            schemaName: 'targetedms',
            queryName: 'targetedmsruns',
            columns: [this.fileColumnName],
            scope: this,
            filterArray: [LABKEY.Filter.create('rowId', this.selectedRowIds.join(';'), LABKEY.Filter.Types.IN)],
            success: function(data) {
                this.showGridSaveDialog(data);
            },
            failure: this.failureHandler
        });
    },

    getDefaultTitle: function(data){
        return data.rows[0][this.fileColumnName] + ' Heat Map';
    },

    getDefaultDescription: function(data){
        var defaultDescription = 'Generated ' + Ext4.Date.format(new Date(), 'm/d/Y g:i A') + ' from:\n';
        data.rows.forEach(function(row) {
            defaultDescription += row[this.fileColumnName] + '\n';
        },this);

        return defaultDescription;
    },

    showGridSaveDialog : function(data) {
        var panel = this;
        var win = Ext4.create('Ext.window.Window', {
            modal: true,
            title: 'Clustergrammer Heat Map',
            border: false,
            autoShow: true,
            minWidth: 400,
            layout: 'anchor',
            items:[{
                xtype: 'form',
                border: false,
                frame: false,
                items: [{    //Add title entry box
                    xtype: 'textfield',
                    fieldLabel: 'Report Title',
                    padding: '10 10 0 10',
                    anchor: '100%',
                    id: 'reportTitleEditor',
                    allowBlank: false,
                    listeners: {
                        render: function (editor) {
                            this.titleEditor = editor;
                            editor.setValue(panel.getDefaultTitle(data));
                        },
                        scope: this
                    }
                }, {       //Add description editor
                    xtype: 'textarea',
                    fieldLabel: 'Description',
                    padding: '0 10 0 10',
                    anchor: '100%',
                    id: 'reportDescriptionEditor',
                    listeners: {
                        render: function (editor) {
                            editor.setValue(panel.getDefaultDescription(data));
                            this.descriptionEditor = editor;
                        },
                        scope: this
                    }
                }
                ],
                dockedItems: [{
                    xtype: 'toolbar',
                    dock: 'bottom',
                    ui: 'footer',
                    padding: '0 10px 15px 15px',
                    items: ['->', {
                        text: 'Save',
                        width: 75,
                        scope: this,
                        formBind: true,
                        handler: function () {
                            Ext4.Msg.confirm(
                                    'Publish to Clustergrammer',
                                    'Clustergrammer is a third party service, all data sent will be publicly accessible.\nDo you wish to continue?',
                                    function (val) {
                                        if (val == 'yes') {
                                            var mask = new Ext4.LoadMask(win, {msg: 'Sending...'});
                                            mask.show();
                                            LABKEY.Ajax.request({
                                                url: LABKEY.ActionURL.buildURL('targetedms', 'clustergrammerHeatMap.api', null),
                                                method : 'POST',
                                                jsonData: {
                                                    title: this.titleEditor.getValue(),
                                                    description: this.descriptionEditor.getValue(),
                                                    selectedIds: this.selectedRowIds
                                                },
                                                scope: this,
                                                success: LABKEY.Utils.getCallbackWrapper(function (response) {
                                                    mask.hide();
                                                    win.close();

                                                    var msg = Ext4.Msg.show({
                                                        title: 'Heat Map Generation Successful',
                                                        msg: 'Heat map successfully created. Navigating...',
                                                        closeable: false
                                                    });

                                                    setTimeout(function () {
                                                        //redirect to generated heatmap
                                                        msg.hide();
                                                        window.location = response.heatMapURL;
                                                        // window.open(response.heatMapURL); //TODO: Open in new tab, gets blocked by pop-up blockers
                                                    }, 2000);
                                                }),
                                                failure: function (request) {
                                                    mask.hide();
                                                    win.close();
                                                    var json = Ext.decode(request.responseText);
                                                    if (json.exception) {
                                                        Ext.Msg.alert("Error", json.exception);
                                                    }
                                                    else {
                                                        Ext.Msg.alert("Error", "Heat map generation failed.");
                                                    }
                                                }
                                            });
                                        }

                                    }, this
                            );
                        }
                    }, {
                        text: 'Cancel',
                        width: 75,
                        handler: function () {
                            win.close();
                        }
                    }]
                }]
            }]
        });
    }
});
