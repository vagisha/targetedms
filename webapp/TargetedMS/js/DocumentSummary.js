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
        var me = this;

        var options = {
            xtype: 'panel',
            layout: {
                type: 'hbox',
                align: 'fit'
            },
            border: false,
            defaults: {
                margins: '0 3 0 0'
            },
            items: [
                {
                    xtype: 'label',
                    html: 'Name: ' + this.fileName
                }
            ]
        };

        if (me.renameAction != null) {
            options.items.push({
                xtype: 'label',
                html: '<span height="16px" class="edit-views-link fa fa-pencil"></span>',
                listeners: {
                    click: {
                        element: 'el',
                        fn: function () {
                            window.location = me.renameAction;
                        }
                    }
                },
                autoEl: {
                    tag: 'label',
                    'data-qtip': 'Rename File'
                }
            });
        }

        options.items = options.items.concat([
                    {
                        xtype: 'label',
                        html: '<span height="16px" class="edit-views-link fa fa-download"></span>',
                        listeners: {
                            click: {
                                element: 'el',
                                fn: function () {
                                    window.location = me.downloadAction;
                                }
                            }
                        },
                        autoEl: {
                            tag: 'label',
                            'data-qtip': 'Download File'
                        }
                    },
                    {
                        xtype: 'label',
                        html: '<a>' + me.versionCount + ' version' + (me.versionCount != 1 ? 's</a>' : '</a>'),
                        margin: '0 0 0 5',
                        listeners: {
                            click: {
                                element: 'el',
                                fn: function () {
                                    window.location = me.versionsAction;
                                }
                            }
                        },
                        autoEl: {
                            tag: 'label',
                            'data-qtip': 'View Versions'
                        }
                    },
                    {
                        xtype: 'label',
                        html: '<br><br>'
                    }
                ]
        );

        return options;
    },

    getCounts: function () {
        var me = this;

        return {
            xtype: 'panel',
            layout: {
                type: 'hbox',
                align: 'fit'
            },
            border: false,
            width: 800,
            items: [
                {
                    xtype: 'label',
                    html: '<a>' + this.peptideGroupCount + ' protein' + (me.peptideGroupCount != 1 ? 's</a>' : '</a>'),
                    listeners: {
                        click: {
                            element: 'el',
                            fn: function(){
                                window.location = me.precursorListAction
                            }
                        }
                    }
                },
                {
                    xtype: 'label',
                    html: ', <a>' + this.peptideCount + ' peptide' + (me.peptideCount != 1 ? 's</a>' : '</a>'),
                    listeners: {
                        click: {
                            element: 'el',
                            fn: function(){
                                window.location = me.precursorListAction
                            }
                        }
                    }
                },
                {
                    xtype: 'label',
                    html: ', <a>' + this.smallMoleculeCount + ' small molecule' + (me.smallMoleculeCount != 1 ? 's</a>' : '</a>'),
                    listeners: {
                        click: {
                            element: 'el',
                            fn: function(){
                                window.location = me.precursorListAction + '#Small Molecule Precursor List'
                            }
                        }
                    }
                },
                {
                    xtype: 'label',
                    html: ', <a>' + this.precursorCount + ' precursor' + (me.precursorCount != 1 ? 's</a>' : '</a>'),
                    listeners: {
                        click: {
                            element: 'el',
                            fn: function(){
                                window.location = me.precursorListAction
                            }
                        }
                    }
                },
                {
                    xtype: 'label',
                    html: ', <a>' + this.transitionCount + ' transition' + (me.transitionCount != 1 ? 's</a>' : '</a>'),
                    listeners: {
                        click: {
                            element: 'el',
                            fn: function(){
                                window.location = me.transitionListAction
                            }
                        }
                    }
                },
                {
                    xtype: 'label',
                    html: ', <a>' + this.calibrationCurveCount + ' calibration curve' + (me.calibrationCurveCount != 1 ? 's</a>' : '</a>'),
                    listeners: {
                        click: {
                            element: 'el',
                            fn: function(){
                                window.location = me.calibrationCurveListAction
                            }
                        }
                    }
                }
            ]
        };
    }

});