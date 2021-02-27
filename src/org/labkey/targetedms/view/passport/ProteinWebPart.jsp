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
        dependencies.add("vis/vis");
        dependencies.add("passport/js/util.js");
        dependencies.add("passport/js/settings.js");
        dependencies.add("passport/js/protein.js");
        dependencies.add("passport/js/peakareachart.js");
        dependencies.add("passport/js/project.js");
        dependencies.add("passport/js/proteinbar.js");
        dependencies.add("internal/jQuery");
        dependencies.add("passport/jQuery/jquery-ui.min.css");
        dependencies.add("passport/jQuery/jquery-ui.min.js");
        dependencies.add("TargetedMS/js/svgChart.js");

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
    // Temporary bridge between original and new reporting approach
    boolean showCV = me.getViewContext().getActionURL().getParameter("showNewPlot") != null;
    %>
    <!--START IMPORTS-->

<script type="text/javascript">
    LABKEY.requiresCss("passport/css/protein.css");
    LABKEY.requiresCss("passport/css/peakareachart.css");
</script>
<script type="text/javascript">
    var proteinJSON = <%=protein.getJSON().getJavaScriptFragment(2)%>
    document.addEventListener("DOMContentLoaded", function() {
        protein.initialize();
    });
    var chromatogramUrl = "<%=h(urlFor(TargetedMSController.PrecursorChromatogramChartAction.class))%>";
    var showPeptideUrl = "<%=h(urlFor(TargetedMSController.ShowPeptideAction.class))%>";
</script>
<!--END IMPORTS-->

<!-- PROTEIN INFO HEADER START -->
<div id="passportContainer">
    <div id="basicproteininfo">
        <h2 id="proteinName"><%=h(protein.getName())%>
            <a href="<%=h(urlFor(TargetedMSController.DownloadDocumentAction.class).addParameter("id", protein.getFile().getRunId()))%>">
                <img src="<%=h(contextPath)%>/passport/img/download.png" style="width:30px; height:30px; margin-left:5px;" alt="Download Skyline dataset" title="Download Skyline dataset">
            </a><sub id="dataUploaded">Data Uploaded: <%=h(formatDate(protein.getFile().getCreatedDate()))%></sub>
        </h2>
        <p id="apiLinks">Sources:&nbsp;<a href="<%=h(urlFor(TargetedMSController.ShowProteinAction.class).addParameter("id", protein.getPepGroupId()))%>">Panorama</a> &#8759; <a href="https://www.uniprot.org/uniprot/<%=h(protein.getAccession())%>">Uniprot</a></p>
        <ul style="max-width:300px;"><!-- Color Scheme: http://paletton.com/#uid=72X0X0kCyk3sipxvvmIKxgXRodf-->
            <li style="border-left: 6px solid #A01C00">
                <span>Protein:&nbsp;</span><%=h(protein.getPreferredname())%></li>
            <li style="border-left: 6px solid #0B1A6D">
                <span>Gene:&nbsp;</span><%=h(protein.getGene())%></li>
            <li style="border-left: 6px solid #00742B">
                <span>Organism:&nbsp;</span><%=h(protein.getSpecies())%></li>
            <%
                List<IKeyword> molecularFunctions = new ArrayList<>();
                List<IKeyword> biologicalProcesses = new ArrayList<>();
                List<IKeyword> keywords = protein.getKeywords();
                for (IKeyword keyword : keywords)
                {
                    if (keyword.categoryId.equals("KW-9999"))
                        biologicalProcesses.add(keyword);
                    if (keyword.categoryId.equals("KW-9992"))
                        molecularFunctions.add(keyword);
                }
            %>

            <%if(biologicalProcesses.size() > 0) {%>
            <li style="border-left: 6px solid #A07200">
                <span title="Biological process">Biological process:&nbsp;</span><br/>
                <%for(int i = 0; i < biologicalProcesses.size(); i++){%>
                <a href="https://www.uniprot.org/keywords/<%=h(biologicalProcesses.get(i).id)%>" target="_blank" rel="noopener noreferrer"><%=h(biologicalProcesses.get(i).label)%></a><%if(i!= biologicalProcesses.size()-1) {%>,&nbsp;<%}%><%}%>
            </li>
            <%}%>
            <%if(molecularFunctions.size() > 0) {%>
            <li style="border-left: 6px solid #A07200">
                <span title="Molecular function">Molecular function:&nbsp;</span><br/>
                <%for(int i = 0; i < molecularFunctions.size(); i++){%>
                <a href="https://www.uniprot.org/keywords/<%=h(molecularFunctions.get(i).id)%>" target="_blank" rel="noopener noreferrer"><%=h(molecularFunctions.get(i).label)%></a>
                <%if(i != molecularFunctions.size()-1) {%>, <%}%>
                <%}%>
            </li>
            <%}%>

        </ul>
        <ul id="sequenceDisplay">
            <li style="border-left: 6px solid #550269">
                <span>Sequence:</span>
                <div id="sequenceDisplayTableContainer">
                    <table><tbody>
                        <%
                            List<HtmlString> seqSegs = protein.getProtSeqHTML();
                            for(int i = 0; i < seqSegs.size(); i++) {
                                if(i % 10 == 0) {%>
                                    <%if(i > 0) {%>
                                        </tr>
                                    <%}%>
                                    <tr style="text-align:right;">
                                        <%for(int j = i; j < i+10; j++) {
                                            if(j+2 > seqSegs.size()) {%>
                                                <td><%=protein.getSequence().length()%></td>
                                            <%break;
                                            } else {%>
                                                <td> <%=(j+1)*10%></td>
                                        <%}%>

                                        <%}%>
                                    </tr>
                                    <%%>
                                        <tr  style="text-align:left;">
                                    <%%>
                                <%}%>
                                <td>
                                    <%=seqSegs.get(i)%>
                                </td>
                                <%if(i == seqSegs.size() -1) {%>
                                    </tr>
                                <%}%>
                            <%
                            }
                            %>
                    </tbody>
                        </table>
                    </div>
                </li>
            </ul>
        </div>
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
                        <% if (showCV) { %>
                        <option value="cv">Coefficient of Variation</option>
                        <% } %>
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
        <div id="visChart"></div>
        <div id="peptide"></div>
        <div id="protein"></div>
    <!-- CHART END -->

        <div id="selectedPeptideChromatogramContainer">
            <div>
                <span width="350" height="400" id="selectedPeptideChromatogramBefore"></span>
                <span width="350" height="400" id="selectedPeptideChromatogramAfter"></span>
                <span width="350" height="400" id="selectedPeptideChromatogram"></span>
            </div>
        </div>
        <div id="seriesLegend" style="width: 500px; margin: auto"></div>
        <div id="peptideinfo" class="peptideDetails"></div>
        <div class="peptideDetails"><a id="selectedPeptideLink" href="">View peptide details</a></div>
    </div>
    <%}%>