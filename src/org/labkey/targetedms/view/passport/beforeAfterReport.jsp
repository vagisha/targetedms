<%@ page import="org.labkey.api.settings.AppProps" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.targetedms.model.passport.IKeyword" %>
<%@ page import="org.labkey.targetedms.model.passport.IProtein" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.targetedms.model.passport.IPeptide" %>
<%@ page import="org.labkey.api.util.HtmlString" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>

<%@ page extends="org.labkey.api.jsp.JspBase" %>

<%
    final String contextPath = AppProps.getInstance().getContextPath();
%>

<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("vis/lib/d3-3.5.17.min.js");
        dependencies.add("passport/js/util.js");
        dependencies.add("passport/js/settings.js");
        dependencies.add("passport/js/beforeAfter/protein.js");
        dependencies.add("passport/js/peakareachart.js");
        dependencies.add("passport/js/proteinbar.js");
        dependencies.add("internal/jQuery");
        dependencies.add("passport/jQuery/jquery-ui.min.css");
        dependencies.add("passport/jQuery/jquery-ui.min.js");
        dependencies.add("passport/css/protein.css");
        dependencies.add("passport/css/peakareachart.css");
        dependencies.add("TargetedMS/js/svgChart.js");
        dependencies.add("TargetedMS/css/svgChart.css");
    }
%>

<%
    JspView<?> me = (JspView<?>) HttpView.currentView();
    IProtein protein = (IProtein)me.getModelBean();
    boolean hasBeforeAndAfter = false;
    for (IPeptide iPeptide : protein.getPep())
    {
        if (iPeptide.getBeforeIntensity() > 0 || iPeptide.getAfterIntensity() > 0)
        {
            hasBeforeAndAfter = true;
            break;
        }
    }
    %>
    <!--START IMPORTS-->

<script type="text/javascript">
    var proteinJSON = <%=protein.getJSON(true).getJavaScriptFragment(2)%>
    document.addEventListener("DOMContentLoaded", function() {
        protein.initialize();
    });
    var chromatogramUrl = "<%=h(urlFor(TargetedMSController.PrecursorChromatogramChartAction.class))%>";
    var showPeptideUrl = "<%=h(urlFor(TargetedMSController.ShowPeptideAction.class))%>";
</script>
<!--END IMPORTS-->

<!-- PROTEIN INFO HEADER START -->
<div id="passportContainer">
    <!-- PROTEIN INFO HEADER END -->
    <%if(protein.getPep() != null && protein.getPep().size() != 0) {%>
    <!-- FILTER OPTIONS START -->
        <div id="filterContainer"><img src="<%=h(contextPath)%>/passport/img/filtericon.png" id="filtericon"/>
            <h1>Filter Options</h1>
            <div class="filterBox">
                <h2>Peptides:&nbsp;
                    <span id="filteredPeptideCount">
                        <green><%=protein.getPep().size()%></green>/<%=protein.getPep().size()%>
                    </span>
                    <span id="copytoclipboard" clipboard="" style="color:rgb(85, 26, 139); cursor:pointer;" title="Copy filtered peptide list to clipboard">Copy</span>
                </h2>
                <p>
                    <label for="peptideSort">Sort by:&nbsp;</label>
                    <select id="peptideSort" name="peptideSort">
                        <option value="intensity">Intensity</option>
                        <option value="sequencelocation">Sequence Location</option>
                    </select>
                </p>
                <% if (hasBeforeAndAfter) { %>
                <p>
                    <label for="filterdeg">Degradation:&nbsp;</label>
                    <input id="filterdeg" type="text" name="filterdeg" readonly="readonly" style="border:0; color:#A01C00; background-color: transparent; font-weight:bold;"/>
                    <div id="rangesliderdeg" class="slider-range"></div>
                </p>
                <% } %>
                <p>
                    <label for="filterpeplength">Sequence length:&nbsp;</label>
                    <input id="filterpeplength" type="text" name="filterpeplength" readonly="readonly" style="border:0; color:#A01C00;  background-color: transparent; font-weight:bold;"/>
                <div id="rangesliderlength" class="slider-range"></div>
                </p>
            </div>
            <div id="pepListBox">
                <ul id="livepeptidelist"></ul>
            </div>
            <%if(protein.getFeatures() != null && protein.getFeatures().size() > 0) {%>
                <div class="filterBox" style="margin-left:10px; padding-left:20px;">
                    <h2>Features:</h2>
                    <input id="showFeatures" type="checkbox" name="showFeatures" readonly="readonly" style="border:0; color:#A01C00; font-weight:bold;"/>
                    <label for="showFeatures">-&nbsp;Select All</label>
                    <br/>
                    <ul id="featuresList"></ul>
                </div>
            <%}%>
            <button id="formreset" type="button">Reset</button>
        </div>
    <!-- FILTER OPTIONS END -->
    <!-- CHART START -->
        <div id="chart"></div>
        <div id="peptide"></div>
        <div id="protein"></div>
    <!-- CHART END -->

        <div id="selectedPeptideChromatogramContainer">
            <span width="350" height="400" id="selectedPeptideChromatogramBefore"></span>
            <span width="350" height="400" id="selectedPeptideChromatogramAfter"></span>
            <span width="350" height="400" id="selectedPeptideChromatogram"></span>
        </div>
        <div class="peptideDetails"><a id="selectedPeptideLink" href="">View peptide details</a></div>
    </div>
    <%}%>