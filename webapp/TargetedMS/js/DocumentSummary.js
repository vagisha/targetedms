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

    getClickableCount: function (count, label, action, comma) {
        return {
            xtype: 'label',
            html: '<a>' + LABKEY.Utils.pluralBasic(count, label) + (comma?',':'') + '</a>&nbsp',
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
        return {
            xtype: 'panel',
            layout: {
                type: 'hbox',
                align: 'fit'
            },
            border: false,
            width: 800,
            items: [
                this.getClickableCount(this.peptideGroupCount, 'protein', this.precursorListAction, true),
                this.getClickableCount(this.peptideCount, 'peptide', this.precursorListAction, true),
                this.getClickableCount(this.smallMoleculeCount, 'small molecule', this.precursorListAction + '#Small Molecule Precursor List', true),
                this.getClickableCount(this.precursorCount, 'precursor', this.precursorListAction, true),
                this.getClickableCount(this.transitionCount, 'transition', this.transitionListAction, true),
                this.getClickableCount(this.calibrationCurveCount, 'calibration curve', this.calibrationCurveListAction, false)
            ]
        };
    }

});