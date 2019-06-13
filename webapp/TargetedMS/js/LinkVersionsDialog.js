/**
 *
 * Copyright (c) 2015-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 *
 * Created by binalpatel on 8/7/15.
 */

Ext4.define('LABKEY.targetedms.LinkedVersions', {

    extend: 'Ext.panel.Panel',

    asGrid: false, // used for save dialog to create/update method chains
    asView: false, // used for displaying method chain information on document details page

    statics: {
        showDialog : function() {
            LABKEY.DataRegion.getSelected({
                selectionKey: LABKEY.DataRegions.TargetedMSRuns.selectionKey,
                success: function(selection) {
                    Ext4.create('LABKEY.targetedms.LinkedVersions', {
                        selectedRowIds: selection.selected,
                        asGrid: true
                    });
                }
            });
        },

        // FOR SELENIUM TESTING
        moveGridRow : function(fromIndex, toIndex) {
            var store = Ext4.getCmp('LinkVersionsSaveGrid').getStore(),
                record = store.getAt(fromIndex);

            if (Ext4.isDefined(record)) {
                store.removeAt(fromIndex);
                store.insert(toIndex, record);
            }
        }
    },

    constructor: function(config) {
        if (!Ext4.isArray(config.selectedRowIds)) {
            console.error("'selectedRowIds' is not an array.");
            return;
        }

        if (!Ext4.isDefined(config.selectedRowIds)) {
            console.error("'selectedRowIds' is not defined.");
            return;
        }

        if (!config.asGrid && !config.asView) {
            console.error("must be defined as either asGrid: true or asView: true.");
            return;
        }

        if (config.asGrid && config.selectedRowIds.length < 2) {
            console.error("'selectedRowIds' array length should be greater than 2. At least two documents should be selected.");
            return;
        }

        if (config.asView && !Ext4.isString(config.divId)) {
            console.error("'divId' must be defined for asView.");
            return;
        }

        this.callParent([config]);
    },

    initComponent : function() {
        Ext4.tip.QuickTipManager.init();

        // query to get all runs associated with the selected runs, i.e. already in a linked chain with the selected runs
        LABKEY.Ajax.request({
            url: LABKEY.ActionURL.buildURL('targetedms', 'getLinkVersions.api', null, {
                selectedRowIds: this.selectedRowIds,
                includeSelected: this.asGrid
            }),
            scope: this,
            success: LABKEY.Utils.getCallbackWrapper(function(response) {
                this.selectedRowIds = response.linkedRowIds;
                this.getDocumentDataForSelectedRowIds();
            }, this, false)
        });

        this.callParent();
    },

    getDocumentDataForSelectedRowIds : function() {
        LABKEY.Query.selectRows({
            schemaName: 'targetedms',
            queryName: 'targetedmsruns',
            columns: this.getBaseColumnNames(),
            scope: this,
            sort: 'Created', // by default get the run ordering by created/imported date
            filterArray: [LABKEY.Filter.create('rowId', this.selectedRowIds.join(';'), LABKEY.Filter.Types.IN)],
            success: function(data) {
                this.addSortOrderToData(data);

                if (this.asGrid) {
                    this.showGridSaveDialog(data);
                }
                else if (this.asView) {
                    this.showDetailsView(data);
                }
            },
            failure: this.failureHandler
        });
    },

    getBaseColumns : function() {
        return [
            // These 'dataIndex' look into the model
            {text: 'ExpRunRowId', dataIndex: 'RowId', hidden: true},
            {text: 'ID', dataIndex: 'File/Id', hidden: true},
            {text: 'Document Name', dataIndex: 'File/FileName', flex: 10, menuDisabled: true, sortable: false, scope: this, renderer: function(value, metadata, record){
                var val = Ext4.String.htmlEncode(value),
                    url = LABKEY.ActionURL.buildURL('targetedms', 'showPrecursorList', null, {id: record.get('File/Id')});

                var highlight = this.highlightedRowIds && this.highlightedRowIds.indexOf(record.data['RowId']) != -1;

                if (this.asView) {
                    return '<a href="' + url + '">' + (highlight ? '<strong>' : '') + val + (highlight ? '</strong>' : '') + '</a>';
                }
                return (highlight ? '<strong>' : '') + val + (highlight ? '</strong>' : '');
            }},
            {text: 'Imported', dataIndex: 'Created', xtype: 'datecolumn', format: 'm/d/Y g:i A', width: 160, menuDisabled: true, sortable: false},
            {text: 'Imported By', dataIndex: 'CreatedBy/DisplayName', width: 100, menuDisabled: true, sortable: false, renderer: 'htmlEncode'},
            {text: 'Note', dataIndex: 'Flag/Comment', menuDisabled: true, sortable: false, renderer: 'htmlEncode'},
            {text: 'Proteins', dataIndex: 'File/Proteins', menuDisabled: true, sortable: false, align: 'right'},
            {text: 'Precursors', dataIndex: 'File/Precursors', menuDisabled: true, sortable: false, align: 'right'},
            {text: 'Transitions', dataIndex: 'File/Transitions', menuDisabled: true, sortable: false, align: 'right'},
            {text: 'Replicates', dataIndex: 'File/Replicates', menuDisabled: true, sortable: false, align: 'right'},
            {text: 'Replaced By', dataIndex: 'ReplacedByRun', hidden: true},
            {text: 'Replaces', dataIndex: 'ReplacesRun', hidden: true},
            {text: 'SortOrder', dataIndex: 'SortOrder', hidden: true}
        ];
    },

    getBaseColumnNames : function() {
        return Ext4.Array.pluck(this.getBaseColumns(), 'dataIndex');
    },

    getGridRemoveColumn : function(store) {
        return {
            xtype: 'templatecolumn',
            text: 'Remove',
            width: 67,
            align: 'center',
            menuDisabled: true,
            sortable: false,
            draggable: false,
            tpl: new Ext4.XTemplate(
                '<span class="{[this.getCls(values)]}" data-qtip="Remove the document from its existing document method chain."></span>',
                {
                    getCls : function(values) {
                        return values['ReplacedByRun'] + values['ReplacesRun'] > 0 ? 'fa fa-times remove-link-version' : '';
                    }
                }
            ),
            hidden: !this.hasDocumentInExistingChain(store)
        };
    },

    getGridColumns : function(store) {
        var columns = [this.getGridRemoveColumn(store)];
        return columns.concat(this.getBaseColumns());
    },

    createGrid : function(data) {
        var store = Ext4.create('Ext.data.Store', {
            fields: this.getBaseColumnNames(),
            sorters: [{property: 'SortOrder', direction: 'ASC'}],
            data: data.rows
        });

        return Ext4.create('Ext.grid.Panel', {
            id: 'LinkVersionsSaveGrid',
            cls: 'link-version-grid',
            padding: 15,
            width: 1250,
            maxHeight: 300,
            autoScoll: true,
            store: store,
            viewConfig: {
                plugins: {
                    ptype: 'gridviewdragdrop',
                    dragText: 'Drag and drop to reorder.'
                },
                getRowClass: function(record, index) {
                    // add CSS class to those rows that are in an existing chain, so we can style them to stand out
                    var replacedBy = record.get('ReplacedByRun');
                    var replaces = record.get('ReplacesRun');
                    if (replacedBy + replaces > 0) {
                        return 'link-version-exists';
                    }
                }
            },
            columns: this.getGridColumns(store),
            listeners: {
                scope: this,
                cellclick: this.removeColumnCellClick
            }
        });
    },

    removeColumnCellClick : function(grid, td, cellIndex, record, tr, rowIndex, e) {
        // 'Remove' column listener to remove a record from an existing method chain
        if (cellIndex == 0 && e.target.className.indexOf('remove-link-version') > 0) {
            Ext4.Msg.confirm('Remove Confirmation', 'Are you sure you want to remove <b>' + record.get('File/FileName')
                    + '</b> from its existing method chain?',
                function(btnId) {
                    if (btnId == 'yes') {
                        var win = grid.up('window');

                        win.getEl().mask('Remove...');
                        LABKEY.Ajax.request({
                            url: LABKEY.ActionURL.buildURL('targetedms', 'removeLinkVersion.api', null, {rowId: record.get('RowId')}),
                            method : 'POST',
                            success: LABKEY.Utils.getCallbackWrapper(function(response) {
                                //close the dialog and reload the page
                                win.close();
                                window.location.reload();
                            }, this, false),
                            failure: LABKEY.Utils.getCallbackWrapper(function(response) {
                                LABKEY.Utils.alert("Error", response.exception);
                                win.getEl().unmask();
                            }, this, true)
                        });
                    }
                }, this);
        }
    },

    hasDocumentInExistingChain: function(store) {
        return store.sum('ReplacedByRun') + store.sum('ReplacesRun') > 0;
    },

    showGridSaveDialog : function(data) {
        var grid = this.createGrid(data);

        // if we have a run that is part of an existing chain, the sum of the ReplacedByRun and ReplacesRun columns will be > 0
        var footerText = 'Drag and drop the documents to reorder the chain. The first version should be at the top, and the most recent at the bottom.';
        if (this.hasDocumentInExistingChain(grid.getStore())) {
            footerText += ' <span>Bold</span> indicates a document that is part of an existing method chain. Saving will replace any existing association.';
        }

        var win = Ext4.create('Ext.window.Window', {
            modal: true,
            title: 'Link Versions',
            border: false,
            autoShow: true,
            items: [grid],
            dockedItems: [{
                xtype: 'toolbar',
                dock: 'bottom',
                ui: 'footer',
                padding: '0 10px 15px 15px',
                items: [{
                    xtype: 'box',
                    cls: 'link-version-footer',
                    width: 750,
                    html: footerText
                }, '->', {
                    text: 'Cancel',
                    width: 75,
                    handler: function() {
                        win.close();
                    }
                },{
                    text: 'Save',
                    width: 75,
                    scope: this,
                    handler: function() {
                        this.saveLinkedVersions(win);
                    }
                }]
            }]
        });
    },

    saveLinkedVersions : function(win) {

        var store = win.down('grid').getStore(),
            orderedRecords = store.getRange(),
            updateRows = [];

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
            win.getEl().mask('Saving...');
            LABKEY.Ajax.request({
                url: LABKEY.ActionURL.buildURL('targetedms', 'saveLinkVersions.api'),
                method: 'POST',
                jsonData: {runs: updateRows},
                headers: { 'Content-Type' : 'application/json' },
                success: LABKEY.Utils.getCallbackWrapper(function(response) {
                    //close the dialog and reload the page
                    win.close();
                    window.location.reload();
                }, this, false),
                failure: LABKEY.Utils.getCallbackWrapper(function(response) {
                    LABKEY.Utils.alert("Error", response.exception);
                    win.getEl().unmask();
                }, this, true)
            });
        }
    },

    addSortOrderToData: function(data) {
        var replacedByMap = {}, rowIdIndexMap = {}, index = 0, linkedRunId = null;

        var sortOrder = data.rows.length;
        Ext4.each(data.rows, function(row){
            // originally set the order by desc created date
            row.SortOrder = sortOrder++;

            // keep track of the map from RowId to the run that replaces it
            replacedByMap[row.RowId] = row.ReplacedByRun;

            // keep track fo the first in the link
            if (row.ReplacedByRun !== null && row.ReplacesRun === null) {
                linkedRunId = row.RowId;
            }

            // indicate the current run (last in the link) in the name
            if (row.ReplacedByRun === null && row.ReplacesRun !== null) {
                row['File/FileName'] = row['File/FileName'] + ' (CURRENT)';
            }

            rowIdIndexMap[row.RowId] = index++;
        });

        // if the runs are in a link, use the ReplacedByRun info
        var sortOrder = 0;
        while (linkedRunId !== null) {
            data.rows[rowIdIndexMap[linkedRunId]].SortOrder = sortOrder++;
            linkedRunId = replacedByMap[linkedRunId];
        }
    },

    showDetailsView : function(data) {
        var columns = this.getBaseColumns();

        Ext4.get(this.divId).setHTML('');

        if (this.selectedRowIds.length > 0) {
            Ext4.create('Ext.grid.Panel', {
                renderTo: this.divId,
                cls: 'link-version-grid',
                disableSelection: true,
                columns: columns,
                store: Ext4.create('Ext.data.Store', {
                    fields: Ext4.Array.pluck(columns, 'dataIndex'),
                    sorters: [{property: 'SortOrder', direction: 'ASC'}],
                    data: data.rows
                })
            });
        }
        else {
            Ext4.create('Ext.Component', {
                renderTo: this.divId,
                html: 'No other versions available.'
            });
        }
    }
});
