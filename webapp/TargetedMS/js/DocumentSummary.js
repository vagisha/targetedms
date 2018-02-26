/*
 * Copyright (c) 2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
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

        var items = [];
        items.push({
            xtype: 'box',
            html: '<b>Name:</b> ' + Ext4.String.htmlEncode(this.runName) + (this.fileName == null || this.fileName == this.runName ? '' : (', from file ' + Ext4.String.htmlEncode(this.fileName))) + (this.fileSize == null ? '' : ' (' + this.fileSize + ')')
        });

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
            items: items
        };

        if (this.renameAction != null) {
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

        if (this.fileName != null) {
            options.items.push({
                xtype: 'box',
                html: '<span height="16px" class="edit-views-link fa fa-download"></span>',
                listeners: {
                    click: {
                        element: 'el',
                        fn: function () {
                            if(this.trackEvent && _gaq)
                            {
                                var container = LABKEY.ActionURL.getContainer();
                                _gaq.push(['_trackEvent', 'SkyDocDownload', container, this.fileName]);
                                // http://www.blastam.com/blog/how-to-track-downloads-in-google-analytics
                                // Tell the browser to wait 400ms before going to the download.  This is to ensure
                                // that the GA tracking request goes through. Some browsers will interrupt the tracking
                                // request if the download opens on the same page.
                                var that = this;
                                setTimeout(function(){window.location = that.downloadAction}, 400);
                            }
                            else
                            {
                                window.location = this.downloadAction;
                            }
                        },
                        scope: this
                    }
                },
                autoEl: {
                    tag: 'span',
                    'data-qtip': 'Download File'
                }
            });

        }

        options.items.push({
            xtype: 'box',
            html: '<a href="#">' + LABKEY.Utils.pluralBasic(this.versionCount, 'version') + '</a>',
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

        return options;
    },

    getClickableCount: function (count, label, action, first) {
        return {
            xtype: 'box',
            html: (!first?',&nbsp':'') + '<a href="#">' + LABKEY.Utils.pluralBasic(count, label) + '</a>',
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

        if (this.softwareVersion) {
            items.push({ xtype: 'label', html: '&nbsp;&nbsp;-&nbsp;&nbsp;'});
            items.push({
                xtype: 'box',
                html: Ext4.String.htmlEncode(this.softwareVersion)
            });
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