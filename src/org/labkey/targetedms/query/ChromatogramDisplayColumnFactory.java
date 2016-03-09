/*
 * Copyright (c) 2012-2016 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.targetedms.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.parser.SampleFile;

import java.io.IOException;
import java.io.Writer;

/**
 * User: vsharma
 * Date: 5/3/12
 * Time: 9:10 PM
 */
public class ChromatogramDisplayColumnFactory implements DisplayColumnFactory
{
    private Container _container;
    private TYPE _type;
    private final int _chartWidth;
    private final int _chart_height;
    private final boolean _syncY;
    private final boolean _syncX;
    private final boolean _splitGraph;
    private final boolean _showOptimizationPeaks;
    private final String _annotationsFilter;
    private final String _replicatesFilter;

    public static enum TYPE
    {
        GENERAL_MOLECULE,
        PRECURSOR
    }

    public ChromatogramDisplayColumnFactory(Container container, TYPE type)
    {
        this(container, type, 400, 400, false, false, false, false, null, null);
    }

    public ChromatogramDisplayColumnFactory(Container container, TYPE type,
                                            int chartWidth, int chartHeight,
                                            boolean syncIntensity, boolean syncRt,
                                            boolean splitGraph, boolean showOptimizationPeaks,
                                            String annotationsFilter, String replicatesFilter)
    {
        _container = container;
        _type = type;
        _chartWidth = chartWidth;
        _chart_height = chartHeight;
        _syncY = syncIntensity;
        _syncX = syncRt;
        _splitGraph = splitGraph;
        _showOptimizationPeaks = showOptimizationPeaks;
        _annotationsFilter = annotationsFilter;
        _replicatesFilter = replicatesFilter;
    }

    public DisplayColumn createRenderer(ColumnInfo colInfo)
    {
        return new DataColumn(colInfo) {
            @Override
            public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
            {
                Object Id = getValue(ctx);  // Primary key from the relevant table
                if(null == Id)
                    return;

                ActionURL chromAction = null;
                SampleFile sampleFile = null;
                switch (_type)
                {
                    case GENERAL_MOLECULE:
                        chromAction = new ActionURL(TargetedMSController.GeneralMoleculeChromatogramChartAction.class, _container);
                        sampleFile = ReplicateManager.getSampleFileForGeneralMoleculeChromInfo((Integer) Id);
                        break;
                    case PRECURSOR:
                        chromAction = new ActionURL(TargetedMSController.PrecursorChromatogramChartAction.class, _container);
                        sampleFile = ReplicateManager.getSampleFileForPrecursorChromInfo((Integer) Id);
                        break;
                }

                chromAction.addParameter("id", String.valueOf(Id));
                chromAction.addParameter("chartWidth", String.valueOf(_chartWidth));
                chromAction.addParameter("chartHeight", String.valueOf(_chart_height));
                chromAction.addParameter("syncY", String.valueOf(_syncY));
                chromAction.addParameter("syncX", String.valueOf(_syncX));
                chromAction.addParameter("splitGraph", String.valueOf(_splitGraph));
                chromAction.addParameter("showOptimizationPeaks", String.valueOf(_showOptimizationPeaks));
                chromAction.addParameter("annotationsFilter", String.valueOf(_annotationsFilter));
                chromAction.addParameter("replicatesFilter", String.valueOf(_replicatesFilter));

                // Figure out if we should highlight this chromatogram
                boolean highlight = false;
                if (HttpView.hasCurrentView())
                {
                    highlight = String.valueOf(Id).equals(HttpView.currentRequest().getParameter("chromInfoId"));
                }

                String imgLink = "<a name=\"ChromInfo" + Id + "\"><img style=\"border: " + (highlight ? "beige" : "white") + " solid 8px\" src=\"" + chromAction.getLocalURIString() + "\" alt=\"Chromatogram "+sampleFile.getSampleName()+"\"></a>";
                out.write(imgLink);
            }
        };
    }
}
