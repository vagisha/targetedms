<%
/*
 * Copyright (c) 2013-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
%>
<%@ page import="org.labkey.api.util.UniqueID" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.targetedms.search.ModificationSearchBean" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("Ext4ClientApi");
    }
%>
<%
    JspView<ModificationSearchBean> me = (JspView<ModificationSearchBean>) HttpView.currentView();
    ModificationSearchBean bean = me.getModelBean();

    String initSearchType = bean.getForm().getSearchType() != null ? bean.getForm().getSearchType() : "deltaMass";
    String initAminoAcids = bean.getForm().getAminoAcids() != null ? bean.getForm().getAminoAcids() : "";
    Double initDeltaMass = bean.getForm().getDeltaMass() != null ? bean.getForm().getDeltaMass() : null;
    String initNameType = bean.getForm().getModificationNameType() != null ? bean.getForm().getModificationNameType() : "custom";
    String initModSearchPairsStr = bean.getForm().getModSearchPairsStr() != null ? bean.getForm().getModSearchPairsStr() : null;
    boolean initStructuralCheck = (bean.getForm().isStructural() != null && bean.getForm().isStructural()) || initSearchType.equals("deltaMass");
    boolean initIsotopeLabelCheck = (bean.getForm().isIsotopeLabel() != null && bean.getForm().isIsotopeLabel()) || initSearchType.equals("deltaMass");

    ActionURL modificationSearchUrl = new ActionURL(TargetedMSController.ModificationSearchAction.class, getContainer());

    String renderId = "modification-search-form-" + UniqueID.getRequestScopedUID(HttpView.currentRequest());
%>
<div id=<%=q(renderId)%>></div>

<script type="text/javascript">

    Ext4.onReady(function(){

        // model used to parse unimod_PARSED.xml for a set of modifications
        Ext4.define('UnimodRecord', {
            extend: 'Ext.data.Model',
            fields: [
                { name: 'Name', mapping: '@Name' },
                { name: 'AAs', mapping: '@AAs' },
                { name: 'ID', mapping: '@ID' },
                { name: 'DeltaMass', mapping: '@DeltaMass' },
                { name: 'Structural', mapping: '@Structural' },
                { name: 'Hidden', mapping: '@Hidden' }
            ]
        });

        // model used to parse the specified record for a given unimod modification name to get the delta mass
        Ext4.define('UnimodDeltaMass', {
            extend: 'Ext.data.Model',
            fields: [
                { name: 'title', mapping: '@title' },
                { name: 'full_name', mapping: '@full_name' },
                { name: 'mono_mass', mapping: 'delta@mono_mass' },
                { name: 'avge_mass', mapping: 'delta@avge_mass' }
            ]
        });

        var form = Ext4.create('Ext.form.Panel', {
            renderTo: <%=q(renderId)%>,
            standardSubmit: true,
            border: false, frame: false,
            defaults: {
                labelWidth: 150,
                labelStyle: 'background-color: #E0E6EA; padding: 2px 4px; margin: 0;'
            },
            items: [
                { xtype: 'hidden', name: 'X-LABKEY-CSRF', value: LABKEY.CSRF },
                {
                    xtype: 'radiogroup',
                    fieldLabel: 'Search by',
                    columns: 2,
                    width: 460,
                    items: [
                        { boxLabel: 'Delta mass', name: 'searchType', inputValue: 'deltaMass', checked: <%=initSearchType.equals("deltaMass")%> },
                        { boxLabel: 'Modification name', name: 'searchType', inputValue: 'modificationName', checked: <%=initSearchType.equals("modificationName")%>}
                    ],
                    listeners: {
                        scope: this,
                        change: function(cmp, newValue, oldValue) {
                            if (newValue['searchType'])
                            {
                                // hide/show form fields based on the selected search type
                                form.down('textfield[name=aminoAcids]').setVisible(newValue['searchType'] == 'deltaMass');
                                form.down('numberfield[name=deltaMass]').setVisible(newValue['searchType'] == 'deltaMass');
                                form.down('radiogroup[name=modificationNameTypeRadioGroup]').setVisible(newValue['searchType'] == 'modificationName');
                                form.down('checkboxgroup[name=includeCheckboxGroup]').setVisible(newValue['searchType'] == 'modificationName');

                                // hide/show name combos based on searchType and modificationNameType
                                var modificationNameType = form.down('radiogroup[name=modificationNameTypeRadioGroup]').getValue()["modificationNameType"];
                                form.down('combo[name=customName]').setVisible(newValue['searchType'] == 'modificationName' && modificationNameType == 'custom');
                                form.down('combo[name=unimodName]').setVisible(newValue['searchType'] == 'modificationName' && modificationNameType != 'custom');

                                // clear values for text/number fields and combos
                                form.down('textfield[name=aminoAcids]').setValue(null);
                                form.down('numberfield[name=deltaMass]').setValue(null);
                                form.down('hiddenfield[name=modSearchPairsStr]').setValue(null);
                                form.down('combo[name=customName]').setValue(null);
                                form.down('combo[name=unimodName]').setValue(null);


                                var elToUpdate = document.getElementById(<%=q(renderId)%>);
                                // Find the closest Ext.Component and call doComponentLayout()
                                while(elToUpdate && elToUpdate !== document.body)
                                {
                                    var cmp = Ext4.getCmp(elToUpdate.id);
                                    console.log(elToUpdate.id);
                                    console.log(cmp);
                                    if(cmp)
                                    {
                                        cmp.doComponentLayout();
                                        break;
                                    }
                                    elToUpdate = elToUpdate.parentNode;
                                }
                            }
                        }
                    }
                },
                {
                    xtype: 'textfield',
                    fieldLabel: 'Amino acids',
                    name: 'aminoAcids',
                    hidden: <%=!initSearchType.equals("deltaMass")%>,
                    value: <%=q(initAminoAcids)%>
                },
                {
                    xtype: 'numberfield',
                    fieldLabel: 'Delta mass',
                    name: 'deltaMass',
                    hideTrigger: true,
                    allowDecimal: true,
                    decimalPrecision: 1, // round valeus to tenth
                    hidden: <%=!initSearchType.equals("deltaMass")%>,
                    value: '<%=initDeltaMass%>'
                },
                {
                    // hidden text field for searching by pairs of amino acid / delta mass modifications
                    xtype: 'hiddenfield',
                    name: 'modSearchPairsStr',
                    value: <%=q(initModSearchPairsStr)%>
                },
                {
                    xtype: 'radiogroup',
                    fieldLabel: 'Type',
                    columns: 1,
                    name: 'modificationNameTypeRadioGroup',
                    hidden: <%=!initSearchType.equals("modificationName")%>,
                    items: [
                        { boxLabel: 'Names used in imported experiments', name: 'modificationNameType', inputValue: 'custom', checked: <%=initNameType.equals("custom")%> },
                        { boxLabel: 'Common Unimod modifications', name: 'modificationNameType', inputValue: 'unimodCommon', checked: <%=initNameType.equals("unimodCommon")%> },
                        { boxLabel: 'All Unimod modifications', name: 'modificationNameType', inputValue: 'unimodAll', checked: <%=initNameType.equals("unimodAll")%> }
                    ],
                    listeners: {
                        change : function(cmp, newValue) {
                            var value = newValue["modificationNameType"];

                            // hide/show name combos based on radio selection
                            form.down('combo[name=customName]').setVisible(value == "custom");
                            form.down('combo[name=unimodName]').setVisible(value == "unimodCommon" || value == "unimodAll");

                            // clear combo values on radio change
                            form.down('combo[name=customName]').setValue(null);
                            form.down('combo[name=unimodName]').setValue(null);

                            // filter unimod combo based on Common or All selection
                            form.filterComboStore(form.down('combo[name=unimodName]').getStore());
                        }
                    }
                },
                {
                    xtype: 'checkboxgroup',
                    width: 460,
                    fieldLabel: 'Include',
                    name: 'includeCheckboxGroup',
                    hidden: <%=!initSearchType.equals("modificationName")%>,
                    items: [
                        { boxLabel: 'Structural', name: 'structural', inputValue: true, checked: <%=initStructuralCheck%> },
                        { boxLabel: 'Isotope label', name: 'isotopeLabel', inputValue: true, checked: <%=initIsotopeLabelCheck%> }
                    ],
                    listeners: {
                        change: function(cmp, newValue, oldValue) {
                            // filter the custom name store based on the selected types
                            form.filterComboStore(form.down('combo[name=customName]').getStore());

                            // filter the unimod name store based on the selected types
                            form.filterComboStore(form.down('combo[name=unimodName]').getStore());
                        }
                    }
                },
                {
                    xtype: 'combo',
                    fieldLabel: 'Custom name',
                    name: 'customName',
                    width: 500,
                    hidden: <%=!initSearchType.equals("modificationName") || !initNameType.equals("custom")%>,
                    value: <%=q(bean.getForm().getCustomName())%>,
                    editable : true,
                    queryMode : 'local',
                    displayField : 'Name',
                    valueField : 'Name',
                    store : Ext4.create('LABKEY.ext4.Store', {
                        containerFilter: <%=q(bean.getForm().isIncludeSubfolders() ? "CurrentAndSubfolders" : "Current")%>,
                        schemaName: "targetedms",
                        // union of structural and isotope label modifications
                        sql : "SELECT CONVERT('Structural', SQL_VARCHAR) AS Type, Name, GROUP_CONCAT(AminoAcid, '') AS AminoAcid, MassDiff, NULL AS ModSearchPairsStr FROM ("
                        	+ "  SELECT DISTINCT"
                            + "    StructuralModId.Name AS Name,"
                            + "    CASE WHEN StructuralModId.AminoAcid IS NOT NULL THEN StructuralModId.AminoAcid"
                            + "         WHEN IndexAA IS NOT NULL THEN substring(PeptideId.Sequence, IndexAA + 1, 1)"
                            + "    END AS AminoAcid,"
                            + "    MassDiff"
                            + "  FROM PeptideStructuralModification"
                            + ") AS x GROUP BY Name, MassDiff "
                            + "UNION "
                            + "SELECT CONVERT('Isotope Label', SQL_VARCHAR) AS Type, Name, GROUP_CONCAT(AminoAcid, '') AS AminoAcid, MassDiff, NULL AS ModSearchPairsStr FROM ("
                            + "  SELECT DISTINCT"
                            + "    IsotopeModId.Name AS Name,"
                            + "    CASE WHEN IsotopeModId.AminoAcid IS NOT NULL THEN IsotopeModId.AminoAcid"
                            + "         WHEN IndexAA IS NOT NULL THEN substring(PeptideId.Sequence, IndexAA + 1, 1)"
                            + "    END AS AminoAcid,"
                            + "    MassDiff"
                            + "  FROM PeptideIsotopeModification"
                            + ") AS x GROUP BY Name, MassDiff",
                        sort: "Name",
                        autoLoad: true,
                        listeners: {
                            load: function(store) {
                                // Issue 17596: allow for a set of AA / DeltaMass pairs by replacing any duplicate
                                // records (i.e. same modification name) with an entry that sets the "ModSearchPairsStr" property
                                for (var index = 0; index < store.getCount(); index++)
                                {
                                    var rec = store.getAt(index);
                                    var recSearchPairStr = rec.get("AminoAcid") + "," + rec.get("MassDiff");

                                    // if there is a previous record with the same modification name, append the recSearchPairStr to that record and remove this one
                                    var prevIndex = store.find("Name", rec.get("Name"), 0, false, true, true);
                                    if (index > prevIndex)
                                    {
                                        var prevRecord = store.getAt(prevIndex);
                                        prevRecord.set("ModSearchPairsStr", prevRecord.get("ModSearchPairsStr") + ";" + recSearchPairStr);

                                        store.remove(rec);
                                        index--;
                                    }
                                    else
                                    {
                                        rec.set("ModSearchPairsStr", recSearchPairStr);
                                    }
                                }

                                // set initial filter
                                form.filterComboStore(store);
                            }
                        }
                    }),
                    listeners: {
                        change: function(cmp, newValue, oldValue) {
                            // set the modification search pairs for the selected custom name record
                            var record = cmp.getStore().findRecord('Name', newValue);
                            form.down('textfield[name=aminoAcids]').setValue(null);
                            form.down('numberfield[name=deltaMass]').setValue(null);
                            form.down('hiddenfield[name=modSearchPairsStr]').setValue(record ? record.get('ModSearchPairsStr') : null);
                        }
                    }
                },
                {
                    xtype: 'combo',
                    fieldLabel: 'Unimod name:<%= helpPopup("Unimod", "Unimod is a public domain database, distributed under a copyleft licence: a copyright notice that permits unrestricted redistribution and modification, provided that all copies and derivatives retain the same permissions.") %>',
                    labelSeparator: '',
                    name: 'unimodName',
                    width: 500,
                    hidden: <%=!initSearchType.equals("modificationName") || initNameType.equals("custom")%>,
                    value: <%=q(bean.getForm().getUnimodName())%>,
                    editable : true,
                    queryMode : 'local',
                    displayField : 'Name',
                    valueField : 'Name',
                    store: Ext4.create('Ext.data.Store', {
                        model: 'UnimodRecord',
                        autoLoad: true,
                        sorters : [{ property: 'Name' }],
                        proxy: {
                            type: 'ajax',
                            url : LABKEY.contextPath + '/TargetedMS/unimod/unimod_PARSED.xml',
                            reader: { type: 'xml', root: 'modifications', record: 'mod' }
                        },
                        listeners: {
                            load: function(store, records) {
                                // set initial filter
                                form.filterComboStore(store);
                            }
                        }
                    }),
                    listeners: {
                        scope: this,
                        change: function(cmp, newValue, oldValue) {
                            var record = cmp.getStore().findRecord('Name', newValue);

                            // set the amino acid based on the selected unimod name record
                            form.down('textfield[name=aminoAcids]').setValue(record ? record.get('AAs') : null);

                            // parse the unimod.xml file for the delta mass if there isn't one directly on the parsed xml record
                            form.down('numberfield[name=deltaMass]').setValue(null);

                            if (record)
                            {
                                if (record.get('DeltaMass'))
                                {
                                    form.down('numberfield[name=deltaMass]').setValue(record.get('DeltaMass'));
                                }
                                else
                                {
                                    Ext4.create('Ext.data.Store', {
                                        model: 'UnimodDeltaMass',
                                        autoLoad: true,
                                        proxy: {
                                            type: 'ajax',
                                            url : LABKEY.contextPath + '/TargetedMS/unimod/unimod_NO_NAMESPACE.xml',
                                            reader: { type: 'xml', root: 'modifications', record: 'mod[record_id=' + record.get("ID") + ']' }
                                        },
                                        listeners: {
                                            scope: this,
                                            load: function(store, records) {
                                                form.down('numberfield[name=deltaMass]').setValue(records.length == 1 ? records[0].get('mono_mass') : null);
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    }
                },
                {
                    xtype: 'checkbox',
                    name: 'includeSubfolders',
                    fieldLabel: 'Search in subfolders',
                    inputValue: true,
                    <%if(bean.getForm().isJournalSearch()){%>hidden: true,<%}%> // Do not show the checkbox if we are searching in a journal project e.g. Panorama Public
                    checked: <%=bean.getForm().isIncludeSubfolders()%>,
                    listeners: {
                        change: function(cb, newValue) {
                            // set the custom name store's containerFilter
                            form.down('combo[name=customName]').getStore().containerFilter = (newValue ? 'CurrentAndSubfolders' : 'Current');
                            form.down('combo[name=customName]').getStore().load();
                        }
                    }
                },
                {
                    xtype: 'hidden',
                    name: 'journalSearch',
                    value: <%=bean.getForm().isJournalSearch()%>
                }
            ],
            buttonAlign: 'left',
            buttons: [{
                text: 'Search',
                handler: function(btn) {
                    var values = form.getForm().getValues();
                    form.submit({
                        url: <%=q(modificationSearchUrl.getLocalURIString())%>,
                        method: 'GET',
                        params: values
                    });
                }
            }],

            getFilterValues : function() {
                var values = form.down('checkboxgroup[name=includeCheckboxGroup]').getValue();
                if (form.down('radiogroup[name=modificationNameTypeRadioGroup]').getValue()["modificationNameType"] == "unimodCommon")
                    values["common"] = true;

                return values;
            },

            filterComboStore : function(store) {
                var values = form.getFilterValues();

                store.clearFilter();
                store.filterBy(function(record) {
                    var include = true;

                    if (record.get("Type"))
                    {
                        include = (values["structural"] && record.get("Type") == "Structural")
                            || (values["isotopeLabel"] && record.get("Type") == "Isotope label");
                    }
                    else if (record.get("Structural"))
                    {
                        include = (values["structural"] && record.get("Structural") == "true")
                            || (values["isotopeLabel"] && record.get("Structural") == "false");
                    }

                    if (include && record.get("Hidden"))
                    {
                        // Common modification are set as Hidden = false
                        include = !values["common"] || record.get("Hidden") == "false";
                    }

                    return include;
                });
            }
        });
    });

</script>


