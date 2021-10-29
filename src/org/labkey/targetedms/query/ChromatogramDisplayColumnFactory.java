/*
 * Copyright (c) 2012-2018 LabKey Corporation
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
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.GUID;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.view.ChromatogramsDataRegion;
import org.springframework.web.servlet.mvc.Controller;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import java.util.function.Consumer;

/**
 * User: vsharma
 * Date: 5/3/12
 * Time: 9:10 PM
 */
public class ChromatogramDisplayColumnFactory implements DisplayColumnFactory
{
    private final Container _container;
    private final Type _type;
    private final int _chartWidth;
    private final int _chart_height;
    private final Consumer<ActionURL> _urlCustomizer;
    private final boolean _syncY;
    private final boolean _syncX;
    private final boolean _splitGraph;
    private final boolean _showOptimizationPeaks;
    private final String _annotationsFilter;
    private final String _replicatesFilter;

    public static final int CHART_WIDTH = 400;
    public static final int CHART_HEIGHT = 400;

    public enum Type
    {
        GeneralMoleculeSampleLookup(TargetedMSController.GeneralMoleculeChromatogramChartAction.class)
                {
                    @Override
                    public FieldKey getSampleNameFieldKey(FieldKey parentFieldKey)
                    {
                        return new FieldKey(new FieldKey(parentFieldKey, "SampleFileId"), "SampleName");
                    }
                },
        GeneralMoleculePeer(TargetedMSController.GeneralMoleculeChromatogramChartAction.class)
                {
                    @Override
                    public FieldKey getSampleNameFieldKey(FieldKey parentFieldKey)
                    {
                        return new FieldKey(parentFieldKey, "sample");
                    }
                },
        PrecursorSampleLookup(TargetedMSController.PrecursorChromatogramChartAction.class)
                {
                    @Override
                    public FieldKey getSampleNameFieldKey(FieldKey parentFieldKey)
                    {
                        return new FieldKey(new FieldKey(parentFieldKey, "SampleFileId"), "SampleName");
                    }
                },
        PrecursorPeer(TargetedMSController.PrecursorChromatogramChartAction.class)
                {
                    @Override
                    public FieldKey getSampleNameFieldKey(FieldKey parentFieldKey)
                    {
                        return new FieldKey(parentFieldKey, "sample");
                    }
                },
        Group(TargetedMSController.GroupChromatogramChartAction.class)
                {
                    @Override
                    public FieldKey getSampleNameFieldKey(FieldKey parentFieldKey)
                    {
                        return new FieldKey(parentFieldKey, "Id");
                    }
                };

        private final Class<? extends Controller> _actionClass;

        Type(Class<? extends Controller> actionClass)
        {
            _actionClass = actionClass;
        }

        public Class<? extends Controller> getActionClass()
        {
            return _actionClass;
        }

        public abstract FieldKey getSampleNameFieldKey(FieldKey parentFieldKey);
    }

    public ChromatogramDisplayColumnFactory(Container container, Type type)
    {
        this(container, type, CHART_WIDTH, CHART_HEIGHT, null, false, false, false, false, null, null);
    }

    public ChromatogramDisplayColumnFactory(Container container, Type type, int chartWidth, int chartHeight)
    {
        this(container, type, chartWidth, chartHeight, null, false, false, false, false, null, null);
    }

    public ChromatogramDisplayColumnFactory(Container container, Type type, int chartWidth, int chartHeight, Consumer<ActionURL> urlCustomizer)
    {
        this(container, type, chartWidth, chartHeight, urlCustomizer, false, false, false, false, null, null);
    }

    public ChromatogramDisplayColumnFactory(Container container, Type type,
                                            int chartWidth, int chartHeight, Consumer<ActionURL> urlCustomizer,
                                            boolean syncIntensity, boolean syncRt,
                                            boolean splitGraph, boolean showOptimizationPeaks,
                                            String annotationsFilter, String replicatesFilter)
    {
        _container = container;
        _type = type;
        _chartWidth = chartWidth;
        _chart_height = chartHeight;
        _urlCustomizer = urlCustomizer == null ? (x) -> {} : urlCustomizer;
        _syncY = syncIntensity;
        _syncX = syncRt;
        _splitGraph = splitGraph;
        _showOptimizationPeaks = showOptimizationPeaks;
        _annotationsFilter = annotationsFilter;
        _replicatesFilter = replicatesFilter;
    }

    @Override
    public DisplayColumn createRenderer(ColumnInfo colInfo)
    {
        return new DataColumn(colInfo) {

            @Override
            public boolean isFilterable()
            {
                return false;
            }

            @Override
            public boolean isSortable()
            {
                return false;
            }

            @Override
            public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
            {
                Object id = getValue(ctx);  // Primary key from the relevant table
                if(null == id)
                    return;

                ActionURL chromAction = new ActionURL(_type.getActionClass(), _container);

                _urlCustomizer.accept(chromAction);
                chromAction.addParameter("id", String.valueOf(id));
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
                    highlight = String.valueOf(id).equals(HttpView.currentRequest().getParameter("chromInfoId"));
                }

                String sampleName = ctx.get(_type.getSampleNameFieldKey(getBoundColumn().getFieldKey().getParent()), String.class);

                String domId = GUID.makeGUID();
                String domLabelId = domId + "-label";

                ChromatogramsDataRegion dataRegion = (ChromatogramsDataRegion)ctx.getCurrentRegion();

                String html = "<a name=\"ChromInfo" + id + "\"></a>";
                html += "<div alt=\"Chromatogram " + PageFlowUtil.filter(sampleName) + "\" style=\"border: " + (highlight ? "beige" : "white") +
                        " solid 8px; width:" + (_chartWidth + 16) + "px; min-height:" + (_chart_height + 50) + "px\" id=\"" + PageFlowUtil.filter(domId) + "\"></div>" +
                        "<div style=\"text-align: center\" id=\"" + PageFlowUtil.filter(domLabelId) + "\"></div>";

                dataRegion.addSVG(chromAction.getLocalURIString(), domId, domLabelId);
                out.write(html);
            }

            @Override
            public void addQueryFieldKeys(Set<FieldKey> keys)
            {
                super.addQueryFieldKeys(keys);
                keys.add(_type.getSampleNameFieldKey(getBoundColumn().getFieldKey().getParent()));
            }
        };
    }

    public static int calculateChartHeight(int transitionCount)
    {
        if (transitionCount > 10)
        {
            return 300 + transitionCount * 10;
        }

        return CHART_HEIGHT;
    }
}
