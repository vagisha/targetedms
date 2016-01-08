/*
 * Copyright (c) 2012-2015 LabKey Corporation
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

package org.labkey.targetedms.chart;

import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeriesCollection;
import org.labkey.targetedms.parser.PeptideChromInfo;
import org.labkey.targetedms.parser.PrecursorChromInfo;
import org.labkey.targetedms.parser.TransitionChromInfo;

/**
 * User: vsharma
 * Date: 5/1/12
 * Time: 10:31 AM
 */
public class ChromatogramChartMakerFactory
{
    private boolean _syncIntensity;
    private boolean _syncRt;
    private boolean _splitGraph;
    private boolean _showOptimizationPeaks;

    public void setSyncIntensity(boolean syncIntensity)
    {
        _syncIntensity = syncIntensity;
    }

    public void setSyncRt(boolean syncRt)
    {
        _syncRt = syncRt;
    }

    public void setSplitGraph(boolean splitGraph)
    {
        _splitGraph = splitGraph;
    }

    public void setShowOptimizationPeaks(boolean showOptimizationPeaks)
    {
        _showOptimizationPeaks = showOptimizationPeaks;
    }

    public JFreeChart createTransitionChromChart(TransitionChromInfo tChromInfo, PrecursorChromInfo pChromInfo)
    {
        return new ChromatogramChartMaker().make(new ChromatogramDataset.TransitionDataset(pChromInfo, tChromInfo));
    }

    public JFreeChart createPrecursorChromChart(PrecursorChromInfo pChromInfo)
    {
        if(_showOptimizationPeaks)
        {
            // Split graph setting will be ignored if we are showing optimization peaks.
            return new ChromatogramChartMaker().make(new ChromatogramDataset.PrecursorOptimizationPeakDataset(pChromInfo, _syncIntensity, _syncRt));

        }
        else if(!_splitGraph)
        {
            return new ChromatogramChartMaker().make(new ChromatogramDataset.PrecursorDataset(pChromInfo, _syncIntensity, _syncRt));
        }
        else
        {
            ChromatogramDataset.PrecursorDataset precursorIonDataset = new ChromatogramDataset.PrecursorSplitDataset(pChromInfo, _syncIntensity, _syncRt);
            ChromatogramDataset.PrecursorDataset productIonDataset = new ChromatogramDataset.ProductSplitDataset(pChromInfo, _syncIntensity, _syncRt) {
                int getSeriesOffset()
                {
                    XYSeriesCollection jfreePrecIonDataset = precursorIonDataset.getJFreeDataset();
                    return jfreePrecIonDataset != null ? jfreePrecIonDataset.getSeriesCount() : 0;
                }
            };
            return new ChromatogramChartMaker().make(precursorIonDataset, productIonDataset);
        }
    }

    public JFreeChart createPeptideChromChart(PeptideChromInfo pepChromInfo)
    {
        return new ChromatogramChartMaker().make(new ChromatogramDataset.PeptideDataset(pepChromInfo, _syncIntensity, _syncRt));
    }
}
