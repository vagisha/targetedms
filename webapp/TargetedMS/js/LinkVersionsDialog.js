/**
 *
 * Copyright (c) 2015 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 *
 * Created by binalpatel on 8/7/15.
 */

Ext4.define('LABKEY.targetedms.LinkVersionsDialog', {

    extend: 'Ext.panel.Panel',
    title: 'Linked Document Details',
    modal: true,
    border: false,
    width: 800,
    minHeight: 200,
    autoScroll: true,
    resizable: false,
    layout: 'fit',

    statics: {
        showLinkVersionDialog : function() {
            LABKEY.DataRegion.getSelected({
                selectionKey: LABKEY.DataRegions.TargetedMSRuns.selectionKey,
                success: function(selection) {
                    Ext4.create('LABKEY.targetedms.LinkVersionsDialog', {
                        selectedRowIds : selection.selected
                    });
                }
            });
        }
    },

    constructor: function(config) {
        if(!Ext4.isArray(config.selectedRowIds))
        {
            console.error("'selectedRowIds' is not an array.");
            return;
        }
        if(!Ext4.isDefined(config.selectedRowIds))
        {
            console.error("'selectedRowIds' is not defined.");
            return;
        }
        if(config.selectedRowIds.length < 2)
        {
            console.error("'selectedRowIds' array length should be greater than 2. At least two documents should be selected.");
            return;
        }

        this.callParent([config]);
    },

    initComponent : function()
    {
        Ext4.Ajax.request({
            url: LABKEY.ActionURL.buildURL('targetedms', 'linkVersions.api', null, {selectedRowIds: this.selectedRowIds}),
            scope: this,
            success: function(response) {
                console.log(Ext4.decode(response.responseText));

                LABKEY.Query.selectRows({
                    schemaName: 'targetedms',
                    queryName: 'targetedmsruns',
                    columns: this.getLinkedDocumentColumnNames(),
                    scope: this,
                    filterArray: [LABKEY.Filter.create('rowId', this.selectedRowIds.join(';'), LABKEY.Filter.Types.IN)],
                    success: this.showLinkedDocumentWindow,
                    failure: this.failureHandler
                });
            }
        });

        this.callParent();
    },

    getLinkedDocumentGridCoumns : function(data) {
        return [
            {
                xtype: 'actioncolumn',
                text: 'Remove',
                width: 67,
                align: 'center',
                menuDisabled: true,
                sortable: false,
                draggable: false,
                icon: LABKEY.contextPath + '/_images/delete.png',
                hidden: Ext.isDefined(data) ? data.rows.length < 3 : true,
                handler: function (grid, rowIndex, colIndex, item, e, record) {
                    var store = grid.getStore();
                    store.remove(record);

                    // only allow removing a row if there are > 2 items in the store
                    if (store.getCount() < 3) {
                        grid.getHeaderAtIndex(colIndex).hide();
                    }
                }
            },
            // These 'dataIndex' look into the model
            {text: 'Document Name', dataIndex: 'File/FileName', flex: 3, menuDisabled: true, sortable: false},
            {text: 'Imported', dataIndex: 'Created', xtype: 'datecolumn', format: 'm/d/Y', width: 90, menuDisabled: true, sortable: false},
            {text: 'Imported By', dataIndex: 'CreatedBy/DisplayName', width: 100, menuDisabled: true, sortable: false},
            {text: 'Note', dataIndex: 'Flag/Comment', width: 200, menuDisabled: true, sortable: false},
            {text: 'Proteins', dataIndex: 'File/PeptideGroupCount', width: 67, menuDisabled: false, sortable: false, align: 'right'},
            {text: 'Precursors', dataIndex: 'File/PrecursorCount', width: 85, menuDisabled: true, sortable: false, align: 'right'},
            {text: 'Transitions', dataIndex: 'File/TransitionCount', width: 87, menuDisabled: true, sortable: false, align: 'right'}
        ];
    },

    getLinkedDocumentColumnNames : function() {
        var fields = Ext4.Array.pluck(this.getLinkedDocumentGridCoumns(), 'dataIndex');
        fields.shift(); // remove the first element as it will be undefined
        return fields;
    },

    getLinkedDocumentGrid : function(data) {
        return Ext4.create('Ext.grid.Panel', {
            padding: 15,
            width: 950,
            maxHeight: 300,
            autoScoll: true,
            store: Ext4.create('Ext.data.Store', {
                fields: this.getLinkedDocumentColumnNames(),
                data: data
            }),
            viewConfig: {
                plugins: {
                    ptype: 'gridviewdragdrop',
                    dragText: 'Drag and drop to reorder.'
                }
            },
            columns: this.getLinkedDocumentGridCoumns(data)
        });
    },

    showLinkedDocumentWindow : function(data)
    {
        var window = Ext4.create('Ext.window.Window', {
            title: 'Link Versions',
            border: false,
            autoShow: true,
            items: [this.getLinkedDocumentGrid(data)],
            dockedItems: [{
                xtype: 'toolbar',
                dock: 'bottom',
                ui: 'footer',
                padding: '0 10px 15px 15px',
                items: [{
                    xtype: 'box',
                    html: 'Drag and drop the documents to reorder the chain.'
                },'->',{
                    text: 'Save',
                    width: 75,
                    handler: function() {
                        console.log('Save!');
                    }
                },{
                    text: 'Cancel',
                    width: 75,
                    handler: function() {
                        window.close();
                    }
                }]
            }]
        });
    }
});
