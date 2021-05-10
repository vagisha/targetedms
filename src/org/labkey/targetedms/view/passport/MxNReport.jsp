<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.targetedms.model.passport.IProtein" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>

<%@ page extends="org.labkey.api.jsp.JspBase" %>

<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("vis/vis");
        dependencies.add("passport/js/util.js");
        dependencies.add("passport/js/settings.js");
        dependencies.add("passport/js/MxN/protein.js");
        dependencies.add("passport/js/proteinbar.js");
        dependencies.add("internal/jQuery");
        dependencies.add("passport/jQuery/jquery-ui.min.css");
        dependencies.add("passport/jQuery/jquery-ui.min.js");
        dependencies.add("TargetedMS/js/svgChart.js");
        dependencies.add("TargetedMS/css/svgChart.css");
        dependencies.add("TargetedMS/js/QCPlotLegendHelper.js");
        dependencies.add("passport/css/protein.css");
        dependencies.add("passport/css/peakareachart.css");
    }
%>

<%
    JspView<?> me = (JspView<?>) HttpView.currentView();
    IProtein protein = (IProtein)me.getModelBean();
    %>
    <!--START IMPORTS-->

<script type="text/javascript">
    var proteinJSON = <%=protein.getJSON(false).getJavaScriptFragment(2)%>
    document.addEventListener("DOMContentLoaded", function() {
        LABKEY.Query.selectRows({
            schemaName: 'targetedms',
            queryName: 'PassportMxN',
            success: function(data) {
                protein.initialize(data);
            },
            filterArray: [ LABKEY.Filter.create('PepGroupId', <%= protein.getPepGroupId() %>)],
            sort: 'AcquiredTime'
        });

    });
    var chromatogramUrl = "<%=h(urlFor(TargetedMSController.PrecursorChromatogramChartAction.class))%>";
    var showPeptideUrl = "<%=h(urlFor(TargetedMSController.ShowPeptideAction.class))%>";
</script>
<!--END IMPORTS-->

<div style="padding-bottom: 2em">
<table>
    <tr>
        <td>
            <label for="peptideSort">Sort by:&nbsp;</label>
        </td>
        <td style="padding-left: 1em">
            <select id="peptideSort" name="peptideSort">
                <option value="intensity">Intensity</option>
                <option value="sequencelocation">Sequence Location</option>
                <option value="cv">Coefficient of Variation</option>
            </select>
        </td>
        <td style="padding-left: 2em">
            <label for="filterpeplength">Sequence length:&nbsp;</label>
        </td>
        <td>
            <div id="rangesliderlength" class="slider-range"></div>
        </td>
        <td style="padding-left: 1em; width: 6em">
            <span id="filterpeplength" />
        </td>
        <td style="padding-left: 2em">
            Matching precursors:
        </td>
        <td style="padding-left: 1em">
            <span id="filteredPeptideCount">
                <green><%=protein.getPep().size()%></green>/<%=protein.getPep().size()%>
            </span>
        </td>
</tr>
</table>
</div>

<table id="cvTable" class="table-condensed labkey-data-region-legacy labkey-show-borders" style="padding-top: 2em">
    <thead>
    <tr>
        <th class="labkey-column-header">Sequence</th>
        <th class="labkey-column-header" style="text-align: right"><span id="copytoclipboard" clipboard="" style="color:rgb(85, 26, 139); cursor:pointer;" title="Copy filtered peptide list to clipboard">Copy</span></th>
        <th class="labkey-column-header">Charge</th>
        <th class="labkey-column-header" style="text-align: right">mZ</th>
        <th class="labkey-column-header">Start Index</th>
        <th class="labkey-column-header">Length</th>
        <th class="labkey-column-header" style="text-align: right">Inter-day CV</th>
        <th class="labkey-column-header" style="text-align: right">Intra-day CV</th>
        <th class="labkey-column-header" style="text-align: right">Total CV</th>
        <th class="labkey-column-header" style="text-align: right">Mean Intensity</th>
        <th class="labkey-column-header" style="text-align: right">Max Intensity</th>
        <th class="labkey-column-header" style="text-align: right">Min Intensity</th>
    </tr>
    </thead>
    <tbody id="cvTableBody"></tbody>
</table>

