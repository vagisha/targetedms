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
    private ChromatogramChartMakerFactory() {}

    public static JFreeChart createTransitionChromChart(TransitionChromInfo tChromInfo, PrecursorChromInfo pChromInfo)
    {
        return new ChromatogramChartMaker().make(new ChromatogramDataset.TransitionDataset(pChromInfo, tChromInfo));
    }

    public static JFreeChart createPrecursorChromChart(PrecursorChromInfo pChromInfo, boolean syncIntensity, boolean syncRt, boolean isSplitGraph)
    {
        if(!isSplitGraph)
        {
            return new ChromatogramChartMaker().make(new ChromatogramDataset.PrecursorDataset(pChromInfo, syncIntensity, syncRt));
        }
        else
        {
            final ChromatogramDataset.PrecursorDataset precursorIonDataset = new ChromatogramDataset.PrecursorSplitDataset(pChromInfo, syncIntensity, syncRt);
            ChromatogramDataset.PrecursorDataset productIonDataset = new ChromatogramDataset.ProductSplitDataset(pChromInfo, syncIntensity, syncRt) {
                int getSeriesOffset()
                {
                    XYSeriesCollection jfreePrecIonDataset = precursorIonDataset.getJFreeDataset();
                    return jfreePrecIonDataset != null ? jfreePrecIonDataset.getSeriesCount() : 0;
                }
            };
            return new ChromatogramChartMaker().make(precursorIonDataset, productIonDataset);
        }
    }

    public static JFreeChart createPeptideChromChart(PeptideChromInfo pepChromInfo, boolean syncIntensity, boolean syncRt)
    {
        return new ChromatogramChartMaker().make(new ChromatogramDataset.PeptideDataset(pepChromInfo, syncIntensity, syncRt));
    }
}
