<%
/*
 * Copyright (c) 2012-2015 LabKey Corporation
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
<%@ page import="org.labkey.api.settings.AppProps"%>
<%@ page import="org.labkey.api.util.PageFlowUtil"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView"%>
<%@ page import="org.labkey.targetedms.view.spectrum.LibrarySpectrumMatch" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<LibrarySpectrumMatch> me = (JspView<LibrarySpectrumMatch>) HttpView.currentView();
    LibrarySpectrumMatch bean = me.getModelBean();
%>

<%

if (bean.getSpectrum() != null)
{

%>

<!--[if IE]><script type="text/javascript" src="<%=getContextPath()%>/TargetedMS/lorikeet_0.3/js/excanvas.min.js"></script><![endif]-->
<script type="text/javascript" src="<%=getContextPath()%>/MS2/lorikeet_0.3/js/jquery-1.4.2.min.js"></script>
<script type="text/javascript" src="<%=getContextPath()%>/MS2/lorikeet_0.3/js/jquery-ui-1.8.4.min.js"></script>
<script type="text/javascript" src="<%=getContextPath()%>/TargetedMS/lorikeet_0.3/js/specview.js"></script>
<script type="text/javascript" src="<%=getContextPath()%>/TargetedMS/lorikeet_0.3/js/peptide.js"></script>
<script type="text/javascript" src="<%=getContextPath()%>/TargetedMS/lorikeet_0.3/js/aminoacid.js"></script>
<script type="text/javascript" src="<%=getContextPath()%>/TargetedMS/lorikeet_0.3/js/ion.js"></script>
<script type="text/javascript" src="<%=getContextPath()%>/TargetedMS/lorikeet_0.3/js/jquery.flot.js"></script>
<script type="text/javascript" src="<%=getContextPath()%>/TargetedMS/lorikeet_0.3/js/jquery.flot.selection.js"></script>
<script type="text/javascript" src="<%=getContextPath()%>/TargetedMS/lorikeet_0.3/js/peptide.js"></script>
<script type="text/javascript" src="<%=getContextPath()%>/TargetedMS/lorikeet_0.3/js/peptide.js"></script>
<script type="text/javascript" src="<%=getContextPath()%>/TargetedMS/lorikeet_0.3/js/peptide.js"></script>

<!-- PLACE HOLDER DIV FOR THE SPECTRUM -->
<div id="<%=bean.getLorikeetId()%>"></div>


<script type="text/javascript">

LABKEY.requiresCss("TargetedMS/lorikeet_0.3/css/lorikeet.css");

$(document).ready(function () {

    /* render the spectrum with the given options */
    $("#<%=bean.getLorikeetId()%>").specview({sequence: <%= PageFlowUtil.jsString(bean.getPeptide()) %>,
                                precursorMz: 1,
                                staticMods: <%= bean.getStructuralModifications()%>,
                                variableMods: <%= bean.getVariableModifications()%>,
                                maxNeutralLossCount: <%= bean.getMaxNeutralLosses()%>,
                                width: 600,
                                charge: <%= bean.getCharge()%>,
                                // peaks in the scan: [m/z, intensity] pairs.
                                peaks: <%= bean.getPeaks()%>,
                                extraPeakSeries: [],
                                peakDetect: false
                                });

    //var canvas = $("#lorikeet_0 canvas")[0];
    //alert(canvas.toDataURL("image/png"));

});

</script>
<% }
else { %> Spectrum information unavailable.<% }%>
