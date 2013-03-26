<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.targetedms.search.ModificationSearchBean" %>
<%@ page import="org.labkey.api.util.UniqueID" %>
<%@ page import="org.labkey.api.view.template.ClientDependency" %>
<%@ page import="java.util.LinkedHashSet" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
  public LinkedHashSet<ClientDependency> getClientDependencies()
  {
      LinkedHashSet<ClientDependency> resources = new LinkedHashSet<ClientDependency>();
      resources.add(ClientDependency.fromFilePath("Ext4ClientApi"));
      return resources;
  }
%>
<%
    JspView<ModificationSearchBean> me = (JspView<ModificationSearchBean>) HttpView.currentView();
    ModificationSearchBean bean = me.getModelBean();
    ViewContext ctx = me.getViewContext();

    String initSearchType = bean.getForm().getSearchType() != null ? bean.getForm().getSearchType() : "deltaMass";
    String initAminoAcids = bean.getForm().getAminoAcids() != null ? bean.getForm().getAminoAcids() : "";
    Integer initDeltaMass = bean.getForm().getDeltaMass() != null ? bean.getForm().getDeltaMass() : null;

    ActionURL modificationSearchUrl = new ActionURL(TargetedMSController.ModificationSearchAction.class, getViewContext().getContainer());

    String renderId = "modification-search-form-" + UniqueID.getRequestScopedUID(HttpView.currentRequest());
%>
<div id=<%=q(renderId)%>></div>

<script type="text/javascript">

    Ext4.onReady(function(){

        // model used to parse unimod.xml for each modification
        Ext4.define('UnimodRecord', {
            extend: 'Ext.data.Model',
            fields: [
                { name: 'title', mapping: '@title' },
                { name: 'full_name', mapping: '@full_name' },
                { name: 'mono_mass', mapping: 'delta@mono_mass' },
                { name: 'avge_mass', mapping: 'delta@avge_mass' }
            ]
        });

        // model used to parse the specified sites for a given unimod modification name
        Ext4.define('UnimodSpecificity', {
            extend: 'Ext.data.Model',
            fields: [
                { name: 'site', mapping: '@site' },
                { name: 'position', mapping: '@position' },
                { name: 'hidden', mapping: '@hidden' },
                { name: 'classification', mapping: '@classification' }
            ]
        });

        Ext4.create('Ext.form.Panel', {
            renderTo: <%=q(renderId)%>,
            standardSubmit: true,
            border: false, frame: false,
            defaults: {
                labelWidth: 120,
                labelStyle: 'background-color: #E1E5E1; padding: 5px;'
            },
            items: [
                {
                    xtype: 'radiogroup',
                    fieldLabel: 'Search Type',
                    columns: 3,
                    width: 550,
                    items: [
                        { boxLabel: 'By Delta Mass', name: 'searchType', inputValue: 'deltaMass', checked: <%=initSearchType.equals("deltaMass")%> },
                        { boxLabel: 'By Custom Name', name: 'searchType', inputValue: 'customName', checked: <%=initSearchType.equals("customName")%>},
                        { boxLabel: 'By Unimod Name', name: 'searchType', inputValue: 'unimodName', checked: <%=initSearchType.equals("unimodName")%> }
                    ],
                    listeners: {
                        scope: this,
                        change: function(cmp, newValue, oldValue) {
                            if (newValue['searchType'])
                            {
                                Ext4.each(cmp.up('form').getForm().getFields().items, function(field){
                                    if (field.searchType)
                                    {
                                        // hide/show form fields based on the selected search type
                                        var show = field.searchType == newValue['searchType'];
                                        field.setVisible(show);

                                        // clear values and invalid states
                                        field.setValue(null);
                                        field.clearInvalid();
                                    }
                                });
                            }
                        }
                    }
                },
                {
                    xtype: 'textfield',
                    fieldLabel: 'Amino Acids',
                    name: 'aminoAcids',
                    allowBlank: false,
                    hidden: <%=!initSearchType.equals("deltaMass")%>,
                    searchType: 'deltaMass',
                    value: <%=q(initAminoAcids)%>
                },
                {
                    xtype: 'numberfield',
                    fieldLabel: 'Delta Mass',
                    name: 'deltaMass',
                    allowBlank: false,
                    allowDecimal: true,
                    decimalPrecision: 0, // round valeus to integers
                    hidden: <%=!initSearchType.equals("deltaMass")%>,
                    searchType: 'deltaMass',
                    value: '<%=initDeltaMass%>'
                },
                {
                    xtype: 'combo',
                    fieldLabel: 'Custom Name',
                    name: 'customName',
                    width: 500,
                    hidden: <%=!initSearchType.equals("customName")%>,
                    value: <%=q(bean.getForm().getCustomName())%>,
                    searchType: 'customName',
                    editable : true,
                    queryMode : 'local',
                    displayField : 'Name',
                    valueField : 'Name',
                    store : Ext4.create('LABKEY.ext4.Store', {
                        schemaName: "targetedms",
                        sql: "SELECT DISTINCT StructuralModId.Name AS Name, StructuralModId.AminoAcid AS AminoAcid, MassDiff FROM PeptideStructuralModification",
                        sort: "Name",
                        autoLoad: true
                    }),
                    listeners: {
                        scope: this,
                        change: function(cmp, newValue, oldValue) {
                            // set the amino acid and delta mass based on the selected custom name record
                            var record = cmp.getStore().findRecord('Name', newValue);
                            cmp.up('form').getForm().findField('aminoAcids').setValue(record ? record.get('AminoAcid') : null);
                            cmp.up('form').getForm().findField('deltaMass').setValue(record ? record.get('MassDiff') : null);
                        }
                    }
                },
                {
                    xtype: 'combo',
                    fieldLabel: 'Unimod Name<%= helpPopup("Unimod", "Unimod is a public domain database, distributed under a copyleft licence: a copyright notice that permits unrestricted redistribution and modification, provided that all copies and derivatives retain the same permissions.") %>',
                    name: 'unimodName',
                    width: 500,
                    hidden: <%=!initSearchType.equals("unimodName")%>,
                    value: <%=q(bean.getForm().getUnimodName())%>,
                    searchType: 'unimodName',
                    editable : true,
                    queryMode : 'local',
                    displayField : 'title',
                    valueField : 'title',
                    store: Ext4.create('Ext.data.Store', {
                        model: 'UnimodRecord',
                        autoLoad: true,
                        proxy: {
                            type: 'ajax',
                            url : LABKEY.contextPath + '/TargetedMS/unimod/unimod.xml',
                            reader: {
                                type: 'xml',
                                namespace: 'umod',
                                root: 'modifications',
                                record: 'mod'
                            }
                        }
                    }),
                    listeners: {
                        scope: this,
                        change: function(cmp, newValue, oldValue) {
                            // set the amino acid and delta mass based on the selected unimod name record
                            var record = cmp.getStore().findRecord('title', newValue);
                            cmp.up('form').getForm().findField('deltaMass').setValue(record ? record.get('mono_mass') : null);
                            cmp.up('form').getForm().findField('aminoAcids').setValue(null);

                            // parse the XML file again for the selected modification name to get the set of specified sites
                            // note: skipping C-term and N-term
                            var modSpecificityStore = Ext4.create('Ext.data.Store', {
                                model: 'UnimodSpecificity',
                                autoLoad: true,
                                proxy: {
                                    type: 'ajax',
                                    url : LABKEY.contextPath + '/TargetedMS/unimod/unimod.xml',
                                    reader: {
                                        type: 'xml',
                                        namespace: 'umod',
                                        root: 'mod[title=' + newValue + ']',
                                        record: 'specificity'
                                    }
                                },
                                listeners: {
                                    scope: this,
                                    load: function(store, records) {
                                        if (records.length > 0)
                                        {
                                            var aminoAcidStr = "";
                                            Ext4.each(records, function(record) {
                                                if (record.get("site") != null && record.get("site").length == 1)
                                                    aminoAcidStr += record.get("site");
                                            });
                                            cmp.up('form').getForm().findField('aminoAcids').setValue(aminoAcidStr);
                                        }
                                    }
                                }
                            });
                        }
                    }
                }
            ],
            buttonAlign: 'left',
            buttons: [{
                text: 'Search',
                formBind: true,
                handler: function(btn) {
                    var values = btn.up('form').getForm().getValues();
                    if (values['aminoAcids'] && values['deltaMass'])
                    {
                        btn.up('form').submit({
                            url: <%=q(modificationSearchUrl.getLocalURIString())%>,
                            params: values
                        });
                    }
                }
            }]
        });
    });

</script>


