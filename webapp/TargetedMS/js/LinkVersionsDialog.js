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
        LABKEY.Ajax.request({
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
            {text: 'Transitions', dataIndex: 'File/TransitionCount', width: 87, menuDisabled: true, sortable: false, align: 'right'},
            {text: 'Replaced By', dataIndex: 'ReplacedByRun', hidden: true},
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
                    scope: this,
                    handler: function() {
                        this.saveLinkedDocumentVersions(window);
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
    },

    saveLinkedDocumentVersions : function(window) {

        var store = window.down('grid').getStore(),
            orderedRecords = store.getRange(),
            updateRows = [];

        //sort asc. to identify "parent" document with the earliest date and time
        orderedRecords.sort(function(d1, d2){
            var date1 = new Date(d1.data.Created);
            var date2 = new Date (d2.data.Created);
            return date1 - date2;
        });

        // traverse the ordered records to get replacedByRunIds, note: must have at least 2 records
        if (orderedRecords.length > 1) {
            for (var i = 0; i < orderedRecords.length - 1; i++) {
                updateRows.push({
                    RowId: orderedRecords[i].get('RowId'),
                    ReplacedByRun: orderedRecords[i+1].get('RowId') // next record in the store
                });
            }
        }

        if (updateRows.length > 0) {
            console.log(updateRows);
            LABKEY.Ajax.request({
                url: LABKEY.ActionURL.buildURL('targetedms', 'saveLinkVersions.api'),
                method: 'POST',
                jsonData: {runs: updateRows},
                headers: { 'Content-Type' : 'application/json' },
                success: function(response) {
                    //close the dialog
                    window.close();
                    window.location.reload();

                }
            });
        }
    }
});
