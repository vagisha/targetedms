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
                //console.log("url", url);
                console.log(Ext4.decode(response.responseText));

                LABKEY.Query.selectRows({
                    schemaName: 'targetedms',
                    queryName: 'targetedmsruns',
                    columns: ['File/FileName', 'Created', 'CreatedBy/DisplayName', 'File/PeptideGroupCount', 'File/PrecursorCount', 'File/TransitionCount'],
                    scope: this,
                    filterArray: [LABKEY.Filter.create('rowId', this.selectedRowIds.join(';'), LABKEY.Filter.Types.IN)],
                    success: this.getLinkedDocuments,
                    failure: this.failureHandler
                });
            }
        });

        this.callParent();
    },

    getLinkedDocuments : function(data)
    {
        var dataStore = Ext4.create('Ext.data.Store', {
            fields:['File/FileName',
            'Created',
            'CreatedBy/DisplayName',
            'File/PeptideGroupCount',
            'File/PrecursorCount',
            'File/TransitionCount'],
            data: data
        });

        var grid = Ext4.create('Ext.grid.Panel', {
            store: dataStore,
            forcefit: true,
            columns: [
                {
                    xtype: 'actioncolumn',
                    title: 'Remove',
                    width: 25,
                    icon: LABKEY.contextPath + '/_images/delete.png',
                    tooltip: 'Delete',
                    handler: function(grid, rowIndex, colIndex) {
                        console.log('Delete!');
                    }
                },
                // These 'dataIndex' look into the model
                {text: 'Document Name', dataIndex: 'File/FileName'},
                {text: 'Imported', dataIndex:'Created'},
                {text: 'Imported By', dataIndex:'CreatedBy/DisplayName'},
                //{text: 'Note', }
                {text: 'Proteins', dataIndex: 'File/PeptideGroupCount'},
                {text: 'Precursors', dataIndex: 'File/PrecursorCount'},
                {text: 'Transitions', dataIndex: 'File/TransitionCount'}
            ]
        });

        var window = Ext4.create('Ext.window.Window', {
            autoShow: true,
            height: 400,
            width: 1000,
            items: [grid]
        });
    }
});
