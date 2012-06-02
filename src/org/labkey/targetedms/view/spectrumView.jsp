<%--
  ~ Copyright (c) 2012 LabKey Corporation
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<%@ page import="org.labkey.api.settings.AppProps"%>
<%@ page import="org.labkey.api.util.PageFlowUtil"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView"%>
<%@ page import="org.labkey.targetedms.view.SpectrumViewBean"%>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<SpectrumViewBean> me = (JspView<SpectrumViewBean>) HttpView.currentView();
    SpectrumViewBean bean = me.getModelBean();
%>

<%

if (bean.getSpectrum() != null)
{
    double[] mzs = bean.getSpectrum().getMz();
    float[] intensities = bean.getSpectrum().getIntensity();
%>

<!--[if IE]><script type="text/javascript" src="<%= AppProps.getInstance().getContextPath() %>/MS2/lorikeet_0.3/js/excanvas.min.js"></script><![endif]-->
<script type="text/javascript" src="<%= AppProps.getInstance().getContextPath()%>/MS2/lorikeet_0.3/js/jquery-1.4.2.min.js"></script>
<script type="text/javascript" src="<%= AppProps.getInstance().getContextPath()%>/MS2/lorikeet_0.3/js/jquery-ui-1.8.4.min.js"></script>


<!-- PLACE HOLDER DIV FOR THE SPECTRUM -->
<div id="lorikeet"></div>


<script type="text/javascript">

LABKEY.requiresCss("TargetedMS/lorikeet_0.3/css/lorikeet.css");

LABKEY.requiresScript("MS2/lorikeet_0.3/js/jquery.flot.js");
LABKEY.requiresScript("MS2/lorikeet_0.3/js/jquery.flot.selection.js");

LABKEY.requiresScript("MS2/lorikeet_0.3/js/specview.js");
LABKEY.requiresScript("MS2/lorikeet_0.3/js/peptide.js");
LABKEY.requiresScript("MS2/lorikeet_0.3/js/aminoacid.js");
LABKEY.requiresScript("MS2/lorikeet_0.3/js/ion.js");

$(document).ready(function () {

    /* render the spectrum with the given options */
    $("#lorikeet").specview({sequence: <%= PageFlowUtil.jsString(bean.getPeptide()) %>,
                                precursorMz: 1,
                                staticMods: staticMods,
                                variableMods: varMods,
                                width: 600,
                                charge: <%= bean.getCharge()%>,
                                peaks: peaks,
                                extraPeakSeries: extraPeakSeries
                                });

});

var staticMods = [];
var varMods = [];

// peaks in the scan: [m/z, intensity] pairs.
var peaks = [
    <%
    boolean firstPeak = true;
    for (int i = 0; i < mzs.length; i++)
    {
     %>
        <%= (firstPeak ? "" : ",") %> [<%= mzs[i]%>, <%= intensities[i]%>]
     <%
        firstPeak = false;
    } %>
];
var extraPeakSeries = [];

</script>
<% }
else { %> Spectrum information unavailable.<% }%>
