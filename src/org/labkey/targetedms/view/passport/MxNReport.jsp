<%@ page import="org.labkey.api.settings.AppProps" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.targetedms.model.passport.IKeyword" %>
<%@ page import="org.labkey.targetedms.model.passport.IProtein" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
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
        dependencies.add("passport/js/MxN/protein.js");
        dependencies.add("passport/js/proteinbar.js");
        dependencies.add("internal/jQuery");
        dependencies.add("passport/jQuery/jquery-ui.min.css");
        dependencies.add("passport/jQuery/jquery-ui.min.js");
        dependencies.add("TargetedMS/js/svgChart.js");
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
    var proteinJSON = <%=protein.getJSON().getJavaScriptFragment(2)%>
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
                <span>Protein:&nbsp;</span><%=h(protein.getPreferredname() != null ? protein.getPreferredname() : protein.getName())%></li>
            <% if (protein.getGene() != null) { %>
                <li style="border-left: 6px solid #0B1A6D">
                    <span>Gene:&nbsp;</span><%=h(protein.getGene())%></li>
            <% } %>
            <% if (protein.getSpecies() != null) { %>
            <li style="border-left: 6px solid #00742B">
                <span>Organism:&nbsp;</span><%=h(protein.getSpecies())%></li>
            <% } %>
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
        <% List<HtmlString> seqSegs = protein.getProtSeqHTML();
        if (!seqSegs.isEmpty()) { %>

            <ul id="sequenceDisplay">
            <li style="border-left: 6px solid #550269">
                <span>Sequence:</span>
                <div id="sequenceDisplayTableContainer">
                    <table><tbody>
                        <%
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
        <% } %>
        </div>
    <!-- PROTEIN INFO HEADER END -->
    <%if(protein.getPep() != null && protein.getPep().size() != 0) {%>
    <!-- FILTER OPTIONS START -->
        <div id="filterContainer"><img src="<%=h(contextPath)%>/passport/img/filtericon.png" id="filtericon"/>
            <h1>Filter Options</h1>
            <div class="filterBox">
                <h2>Precursors:&nbsp;
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
                        <option value="cv">Coefficient of Variation</option>
                    </select>
                </p>
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
        <div id="intensityChart"></div>
        <div id="cvChart"></div>
        <table id="cvTable" class="table-condensed labkey-data-region-legacy labkey-show-borders">
            <thead>
            <tr>
                <th class="labkey-column-header">Sequence</th>
                <th class="labkey-column-header">Charge</th>
                <th class="labkey-column-header" style="text-align: right">mZ</th>
                <th class="labkey-column-header" style="text-align: right">Inter-day CV</th>
                <th class="labkey-column-header" style="text-align: right">Intra-day CV</th>
                <th class="labkey-column-header" style="text-align: right">Total CV</th>
                <th class="labkey-column-header" style="text-align: right">Mean Intensity</th>
                <th class="labkey-column-header" style="text-align: right">Max Intensity</th>
                <th class="labkey-column-header" style="text-align: right">Min Intensity</th>
            </tr>
            </thead>
            <tbody id="cvTableBody" />
        </table>
        <div id="peptide"></div>
        <div id="protein"></div>
    <!-- CHART END -->

        <div id="selectedPeptideChromatogramContainer">
            <div id="chromatograms">
            </div>
        </div>
        <div id="seriesLegend" style="width: 500px; margin: auto"></div>
        <div id="peptideinfo" class="peptideDetails"></div>
        <div class="peptideDetails"><a id="selectedPeptideLink" href="">View peptide details</a></div>
    </div>

<%}%>