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
    Integer initDeltaMass = bean.getForm().getDeltaMass() != 0 ? bean.getForm().getDeltaMass() : null;

    ActionURL modificationSearchUrl = new ActionURL(TargetedMSController.ModificationSearchAction.class, getViewContext().getContainer());

    String renderId = "modification-search-form-" + UniqueID.getRequestScopedUID(HttpView.currentRequest());
%>
<div id=<%=q(renderId)%>></div>

<script type="text/javascript">

    Ext4.onReady(function(){

        Ext4.create('Ext.form.Panel', {
            renderTo: <%=q(renderId)%>,
            standardSubmit: true,
            border: false, frame: false,
            defaults: {
                labelWidth: 110,
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
                        { boxLabel: 'By Unimod Name', name: 'searchType', inputValue: 'unimodName', checked: <%=initSearchType.equals("unimodName")%>, hidden: true }
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
                    hidden: <%=!initSearchType.equals("deltaMass")%>,
                    searchType: 'deltaMass',
                    value: '<%=initDeltaMass%>'
                },
                {
                    xtype: 'combo',
                    fieldLabel: 'Custom Name',
                    name: 'customName',
                    width: 350,
                    hidden: <%=!initSearchType.equals("customName")%>,
                    value: <%=q(bean.getForm().getCustomName())%>,
                    searchType: 'customName',
                    editable : false,
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
                            cmp.up('form').getForm().findField('aminoAcids').setValue(record.get('AminoAcid'));
                            cmp.up('form').getForm().findField('deltaMass').setValue(record.get('MassDiff'));
                        }
                    }
                },
                {
                    xtype: 'combo',
                    fieldLabel: 'Unimod Name',
                    name: 'unimodName',
                    hidden: <%=!initSearchType.equals("unimodName")%>,
                    value: <%=q(bean.getForm().getUnimodName())%>,
                    searchType: 'unimodName',
                    store: null
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


