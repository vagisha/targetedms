Ext4.define('LABKEY.targetedms.DocumentSummary', {

    extend: 'Ext.panel.Panel',
    layout: {
        type: 'vbox',
        align: 'fit'
    },
    border: false,

    initComponent : function() {
        Ext4.tip.QuickTipManager.init();

        this.items = this.getItems();
        this.callParent();
    },

    getItems: function(){
        return [this.getNameAndOptions(), this.getCounts()];
    },

    getNameAndOptions: function () {

        if(this.fileName == null)
            this.fileName = "File not found";

        var options = {
            xtype: 'panel',
            layout: {
                type: 'hbox',
                align: 'fit'
            },
            border: false,
            defaults: {
                margins: '0 3 15 0'
            },
            items: [
                {
                    xtype: 'label',
                    html: 'Name: ' + this.fileName+ (this.fileSize == null ? '' : ' (' + this.fileSize + ')')
                }
            ]
        };

        if (this.renameAction != null && this.fileName != "File not found") {
            options.items.push({
                xtype: 'label',
                html: '<span height="16px" class="edit-views-link fa fa-pencil"></span>',
                listeners: {
                    click: {
                        element: 'el',
                        fn: function () {
                            window.location = this.renameAction;
                        },
                        scope: this
                    }
                },
                autoEl: {
                    tag: 'label',
                    'data-qtip': 'Rename File'
                }
            });
        }

        if (this.fileName != "File not found") {
            options.items.push({
                xtype: 'label',
                html: '<span height="16px" class="edit-views-link fa fa-download"></span>',
                listeners: {
                    click: {
                        element: 'el',
                        fn: function () {
                            window.location = this.downloadAction;
                        },
                        scope: this
                    }
                },
                autoEl: {
                    tag: 'label',
                    'data-qtip': 'Download File'
                }
            });

            options.items.push({
                xtype: 'label',
                html: '<a>' + LABKEY.Utils.pluralBasic(this.versionCount, 'version') + '</a>',
                margin: '0 0 0 5',
                listeners: {
                    click: {
                        element: 'el',
                        fn: function () {
                            window.location = this.versionsAction;
                        },
                        scope: this
                    }
                }
            });
        }

        return options;
    },

    getClickableCount: function (count, label, action, first) {
        return {
            xtype: 'label',
            html: (!first?',&nbsp':'') + '<a>' + LABKEY.Utils.pluralBasic(count, label) + '</a>',
            listeners: {
                click: {
                    element: 'el',
                    fn: function(){
                        window.location = action;
                    },
                    scope: this
                }
            }
        }
    },

    getCounts: function () {
        var items = [];

        items.push(this.getClickableCount(this.peptideGroupCount, 'protein', this.precursorListAction, true));
        if (this.peptideCount > 0) {
            items.push(this.getClickableCount(this.peptideCount, 'peptide', this.precursorListAction, false));
        }
        if (this.smallMoleculeCount > 0) {
            items.push(this.getClickableCount(this.smallMoleculeCount, 'small molecule', this.precursorListAction + '#Small Molecule Precursor List', false));
        }
        items.push(this.getClickableCount(this.precursorCount, 'precursor', this.precursorListAction, false));
        items.push(this.getClickableCount(this.transitionCount, 'transition', this.transitionListAction, false));

        items.push({ xtype: 'label', html: '&nbsp;&nbsp;-&nbsp;&nbsp;'});

        items.push(this.getClickableCount(this.replicateCount, 'replicate', this.replicateListAction, true));

        if (this.calibrationCurveCount > 0) {
            items.push(this.getClickableCount(this.calibrationCurveCount, 'calibration curve', this.calibrationCurveListAction, false));
        }

        return {
            xtype: 'panel',
            layout: {
                type: 'hbox',
                align: 'fit'
            },
            border: false,
            width: 800,
            items: items
        };
    }

});