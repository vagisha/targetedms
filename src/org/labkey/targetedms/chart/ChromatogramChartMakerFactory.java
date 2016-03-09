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

package org.labkey.targetedms.chart;

import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeriesCollection;
import org.labkey.api.data.Container;
import org.labkey.api.security.User;
import org.labkey.targetedms.parser.GeneralMoleculeChromInfo;
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

    public JFreeChart createTransitionChromChart(TransitionChromInfo tChromInfo, PrecursorChromInfo pChromInfo, User user, Container container)
    {
        return new ChromatogramChartMaker().make(new ChromatogramDataset.TransitionDataset(pChromInfo, tChromInfo, user, container));
    }

    public JFreeChart createMoleculeTransitionChromChart(TransitionChromInfo tChromInfo, PrecursorChromInfo pChromInfo, User user, Container container)
    {
        return new ChromatogramChartMaker().make(new ChromatogramDataset.MoleculeTransitionDataset(pChromInfo, tChromInfo, user, container));
    }

    public JFreeChart createPrecursorChromChart(PrecursorChromInfo pChromInfo, User user, Container container)
    {
        if(_showOptimizationPeaks)
        {
            // Split graph setting will be ignored if we are showing optimization peaks.
            return new ChromatogramChartMaker().make(new ChromatogramDataset.PrecursorOptimizationPeakDataset(pChromInfo,
                    _syncIntensity, _syncRt, user, container));

        }
        else if(!_splitGraph)
        {
            return new ChromatogramChartMaker().make(new ChromatogramDataset.PrecursorDataset(pChromInfo, _syncIntensity, _syncRt, user, container));
        }
        else
        {
            ChromatogramDataset.PrecursorDataset precursorIonDataset = new ChromatogramDataset.PrecursorSplitDataset(pChromInfo, _syncIntensity, _syncRt, user, container);
            ChromatogramDataset.PrecursorDataset productIonDataset = new ChromatogramDataset.ProductSplitDataset(pChromInfo, _syncIntensity, _syncRt, user, container) {
                int getSeriesOffset()
                {
                    XYSeriesCollection jfreePrecIonDataset = precursorIonDataset.getJFreeDataset();
                    return jfreePrecIonDataset != null ? jfreePrecIonDataset.getSeriesCount() : 0;
                }
            };
            return new ChromatogramChartMaker().make(precursorIonDataset, productIonDataset);
        }
    }

    public JFreeChart createMoleculePrecursorChromChart(PrecursorChromInfo pChromInfo, User user, Container container)
    {
        return new ChromatogramChartMaker().make(new ChromatogramDataset.MoleculePrecursorDataset(pChromInfo, _syncIntensity, _syncRt, user, container));
    }

    public JFreeChart createPeptideChromChart(GeneralMoleculeChromInfo pepChromInfo, User user, Container container)
    {
        return new ChromatogramChartMaker().make(new ChromatogramDataset.PeptideDataset(pepChromInfo, _syncIntensity, _syncRt, user, container));
    }

    public JFreeChart createMoleculeChromChart(GeneralMoleculeChromInfo molChromInfo, User user, Container container)
    {
        return new ChromatogramChartMaker().make(new ChromatogramDataset.MoleculeDataset(molChromInfo, _syncIntensity, _syncRt, user, container));
    }
}
