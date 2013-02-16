/*
 * Copyright (c) 2012 LabKey Corporation
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
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.labkey.targetedms.parser.Chromatogram;
import org.labkey.targetedms.parser.PeptideChromInfo;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.parser.PrecursorChromInfo;
import org.labkey.targetedms.parser.SampleFile;
import org.labkey.targetedms.parser.Transition;
import org.labkey.targetedms.parser.TransitionChromInfo;
import org.labkey.targetedms.query.PeptideManager;
import org.labkey.targetedms.query.PrecursorManager;
import org.labkey.targetedms.query.ReplicateManager;
import org.labkey.targetedms.query.TransitionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

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
        Chromatogram chromatogram = pChromInfo.createChromatogram();

        if (tChromInfo.getChromatogramIndex() >= chromatogram.getTransitionsCount())
        {
            throw new IllegalStateException("Requested index " + tChromInfo.getChromatogramIndex() + " but there are only " + chromatogram.getTransitionsCount());
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        addTransitionAsSeries(dataset, chromatogram, tChromInfo.getChromatogramIndex(),
                              pChromInfo.getMinStartTime().floatValue(),
                              pChromInfo.getMaxEndTime().floatValue(),
                              LabelFactory.transitionLabel(tChromInfo.getTransitionId()));

        SampleFile sampleFile = ReplicateManager.getSampleFile(tChromInfo.getSampleFileId());

        ChromatogramChartMaker chartMaker = new ChromatogramChartMaker(ChromatogramChartMaker.TYPE.TRANSITION);
        chartMaker.setDataset(dataset);
        chartMaker.setTitle(sampleFile.getSampleName());
        if(tChromInfo.getStartTime() != null && tChromInfo.getEndTime() != null)
        {
            // markers for peak integration boundaries
            chartMaker.setPeakStartTime(tChromInfo.getStartTime());
            chartMaker.setPeakEndtime(tChromInfo.getEndTime());
        }
        if(tChromInfo.getRetentionTime() != null)
        {
            // marker for retention time
            chartMaker.addRetentionTimeAnnotation(tChromInfo.getRetentionTime(),
                                                  tChromInfo.getHeight() / 1000,
                                                  0);
        }
        return chartMaker.make();
    }

    public static JFreeChart createPrecursorChromChart(PrecursorChromInfo pChromInfo, boolean syncIntensity, boolean syncMz)
    {
        XYSeriesCollection dataset = new XYSeriesCollection();

        Chromatogram chromatogram = pChromInfo.createChromatogram();

        Precursor precursor = PrecursorManager.get(pChromInfo.getPrecursorId());

        // get the min and max retention times of the transitions for this precursor, over all replicates
        float minPrecAllReplRt = 0;
        float maxPrecAllReplRt = 0;

        if(syncMz){
            minPrecAllReplRt = (float) PeptideManager.getMinRetentionTime(precursor.getPeptideId());
            maxPrecAllReplRt = (float) PeptideManager.getMaxRetentionTime(precursor.getPeptideId());
        }

        Double pciMinStartTime = pChromInfo.getMinStartTime();
        Double pciMaxStartTime = pChromInfo.getMaxEndTime();
        // If this precursorChromInfo does not have a minStartTime and maxEndTime,
        // get the minimum minStartTime and maximum maxEndTime for all precursors of this peptide in this replicate.
        if(pciMinStartTime == null)
        {
           pciMinStartTime = PeptideManager.getMinRetentionTime(precursor.getPeptideId(), pChromInfo.getSampleFileId());
        }
        if(pciMaxStartTime == null)
        {
           pciMaxStartTime = PeptideManager.getMaxRetentionTime(precursor.getPeptideId(), pChromInfo.getSampleFileId());
        }

        List<TransChromInfoPlusTransition> tciList = new ArrayList<TransChromInfoPlusTransition>(chromatogram.getTransitionsCount());

        for(int chromatogramIndex = 0; chromatogramIndex < chromatogram.getTransitionsCount(); chromatogramIndex++)
        {
            TransitionChromInfo tChromInfo = TransitionManager.getTransitionChromInfo(pChromInfo.getId(), chromatogramIndex);
            if(tChromInfo == null)
                continue;
            Transition transition = TransitionManager.get(tChromInfo.getTransitionId());
            tciList.add(new TransChromInfoPlusTransition(tChromInfo, transition));
        }

        Collections.sort(tciList, new Comparator<TransChromInfoPlusTransition>()
        {
            @Override
            public int compare(TransChromInfoPlusTransition o1, TransChromInfoPlusTransition o2)
            {
                Transition t1 = o1.getTransition();
                Transition t2 = o2.getTransition();
                Integer t1_idx = t1.isPrecursorIon() ? t1.getMassIndex() : t1.getFragmentOrdinal();
                Integer t2_idx = t2.isPrecursorIon() ? t2.getMassIndex() : t2.getFragmentOrdinal();
                if(t1_idx == null)
                {
                    throw new IllegalArgumentException("Transition (ID "+t1.getId()+") does not have either fragmentOrdinal or massIndex");
                }
                if(t2_idx == null)
                {
                    throw new IllegalArgumentException("Transition (ID "+t2.getId()+") does not have either fragment index or isotopic peak index");
                }
                // Precursor ions are ordered M, M+1, M+2.  Fragment ions are displayed in reverse order -- y9, y8, y7 etc.
                return t1.isPrecursorIon() ? t1_idx.compareTo(t2_idx) : t2_idx.compareTo(t1_idx);
            }
        });

        Double bestTransitionHeight = 0.0;
        int bestTransitionSeriesIndex = 0;
        int seriesIndex = 0;
        for(TransChromInfoPlusTransition chromInfoPlusTransition: tciList)
        {
            addTransitionAsSeries(dataset, chromatogram, chromInfoPlusTransition.getTransChromInfo().getChromatogramIndex(),
                                  (syncMz ? minPrecAllReplRt : pciMinStartTime.floatValue()),
                                  (syncMz ? maxPrecAllReplRt : pciMaxStartTime.floatValue()),
                                  LabelFactory.transitionLabel(chromInfoPlusTransition.getTransition()));

            TransitionChromInfo tChromInfo = chromInfoPlusTransition.getTransChromInfo();
            if(tChromInfo.getHeight() != null && tChromInfo.getHeight() > bestTransitionHeight)
            {
                bestTransitionHeight = tChromInfo.getHeight();
                bestTransitionSeriesIndex = seriesIndex;
            }
            seriesIndex++;
        }

        ChromatogramChartMaker chartMaker = new ChromatogramChartMaker(ChromatogramChartMaker.TYPE.PRECURSOR);
        chartMaker.setDataset(dataset);
        chartMaker.setTitle(LabelFactory.precursorChromInfoChartLabel(pChromInfo));
        if(pChromInfo.getMinStartTime() != null)
            chartMaker.setPeakStartTime(pChromInfo.getMinStartTime());
        if(pChromInfo.getMaxEndTime() != null)
            chartMaker.setPeakEndtime(pChromInfo.getMaxEndTime());
        if(pChromInfo.getBestRetentionTime() != null)
            chartMaker.addRetentionTimeAnnotation(pChromInfo.getBestRetentionTime(),
                                                  (bestTransitionHeight) / 1000,
                                                  bestTransitionSeriesIndex);

        if(syncIntensity)
        {
            // get the height of the tallest transition peak for this peptide over all replicates
            double maxIntensity = TransitionManager.getMaxTransitionIntensity(precursor.getPeptideId());
            chartMaker.setMaxIntensity(maxIntensity);
        }
        if(syncMz)
        {
            chartMaker.setMinRt(minPrecAllReplRt);
            chartMaker.setMaxRt(maxPrecAllReplRt);
        }

        return chartMaker.make();
    }

    private static class TransChromInfoPlusTransition
    {
        private TransitionChromInfo _transChromInfo;
        private Transition _transition;

        public TransChromInfoPlusTransition(TransitionChromInfo transChromInfo, Transition transition)
        {
            _transChromInfo = transChromInfo;
            _transition = transition;
        }

        public TransitionChromInfo getTransChromInfo()
        {
            return _transChromInfo;
        }

        public Transition getTransition()
        {
            return _transition;
        }
    }

    private static void addTransitionAsSeries(XYSeriesCollection dataset, Chromatogram chromatogram, int seriesIndex,
                                              float minTime, float maxTime, String label)
    {
        float[] times = chromatogram.getTimes();
        float[] intensities = chromatogram.getIntensities(seriesIndex);
        assert times.length == intensities.length : "Length of times and intensities don't match";

        XYSeries series = new XYSeries(label);

        // Display chromatogram only around the peak integration boundary.
        float displayWidth = (maxTime - minTime);
        minTime = minTime - displayWidth;
        maxTime = maxTime + displayWidth;

        for (int i = 0; i < times.length; i++)
        {
            if(times[i] < minTime)
                continue;
            if(times[i] > maxTime)
                break;
            series.add(times[i], intensities[i] / 1000);
        }
        dataset.addSeries(series);
    }

    public static JFreeChart createPeptideChromChart(PeptideChromInfo pepChromInfo, boolean syncIntensity, boolean syncMz)
    {
        // Get the precursors for the peptide
        List<PrecursorChromInfo> precursorChromInfoList = PrecursorManager.getPrecursorChromInfosForPeptideChromInfo(pepChromInfo.getId());



        // get the min and max peak integration boundaries.
        Double minRt = Double.MAX_VALUE;
        Double maxRt = 0.0;

        for(PrecursorChromInfo pChromInfo: precursorChromInfoList)
        {
            minRt = pChromInfo.getMinStartTime() != null ? Math.min(minRt, pChromInfo.getMinStartTime()): minRt;
            maxRt = pChromInfo.getMaxEndTime() != null ? Math.max(maxRt, pChromInfo.getMaxEndTime()) : maxRt;
        }

        Double minPeptideRt = Double.MAX_VALUE;
        Double maxPeptideRt = 0.0;
        if(syncMz){
            // get the min and max retention times of the transitions for this peptide, over all replicates
            minPeptideRt = PeptideManager.getMinRetentionTime(pepChromInfo.getPeptideId());
            maxPeptideRt = PeptideManager.getMaxRetentionTime(pepChromInfo.getPeptideId());
        }

        ChromatogramChartMaker chartMaker = new ChromatogramChartMaker(ChromatogramChartMaker.TYPE.PEPTIDE);

        XYSeriesCollection dataset = new XYSeriesCollection();


        Collections.sort(precursorChromInfoList, new Comparator<PrecursorChromInfo>()
        {
            @Override
            public int compare(PrecursorChromInfo o1, PrecursorChromInfo o2)
            {
                return Integer.valueOf(o2.getPrecursorId()).compareTo(o1.getPrecursorId());
            }
        });

        // Add precursors in reverse order so that we get the same colors as Skyline
        for(int i = precursorChromInfoList.size() - 1; i >= 0; i--)
        {
            PrecursorChromInfo pChromInfo = precursorChromInfoList.get(i);

            Chromatogram chromatogram = pChromInfo.createChromatogram();

            // instead of displaying separate peaks for each transition of this precursor
            // we will sum up the intensities and display a single peak for the precursor
            double maxHeight = addPrecursorAsSeries(dataset, chromatogram, pChromInfo.getId(),
                                                    syncMz ? minPeptideRt.floatValue() : minRt.floatValue(),
                                                    syncMz ? maxPeptideRt.floatValue() : maxRt.floatValue(),
                                                    LabelFactory.precursorLabel(pChromInfo.getPrecursorId()));

            if(pChromInfo.getBestRetentionTime() != null)
                chartMaker.addRetentionTimeAnnotation(pChromInfo.getBestRetentionTime(),
                                                      maxHeight,
                                                      // Use precursor indexes in reverse order so that we get the same colors as Skyline.
                                                      precursorChromInfoList.size() - i - 1);

        }


        chartMaker.setDataset(dataset);
        chartMaker.setTitle(LabelFactory.peptideChromInfoChartLabel(pepChromInfo));
        chartMaker.setPeakStartTime(minRt);
        chartMaker.setPeakEndtime(maxRt);

        if(syncIntensity)
        {
            // get the height of the tallest precursor for this peptide over all replicates
            double maxIntensity = PrecursorManager.getMaxPrecursorIntensity(pepChromInfo.getPeptideId());
            chartMaker.setMaxIntensity(maxIntensity);
        }
        if(syncMz)
        {
            chartMaker.setMinRt(minPeptideRt);
            chartMaker.setMaxRt(maxPeptideRt);
        }

        return chartMaker.make();
    }

    private static double addPrecursorAsSeries(XYSeriesCollection dataset, Chromatogram chromatogram,
                                               int precursorChromId,
                                               float minTime, float maxTime, String label)
    {
        float[] times = chromatogram.getTimes();

        XYSeries series = new XYSeries(label);

        // Display chromatogram only around the peak integration boundary.
        float displayWidth = (maxTime - minTime);
        minTime = minTime - displayWidth;
        maxTime = maxTime + displayWidth;

        Set<Integer> transitionChromIndexes = TransitionManager.getTransitionChromatogramIndexes(precursorChromId);

        // sum up the intensities of all transitions of this precursor
        double[] totalIntensities = new double[times.length];
        for(int i = 0; i < chromatogram.getTransitionsCount(); i++)
        {
            if(!transitionChromIndexes.contains(i))
                continue;

            float[] transitionIntensities = chromatogram.getIntensities(i);
            assert times.length == transitionIntensities.length : "Length of times and intensities don't match";

            for (int j = 0; j < times.length; j++)
            {
                if(times[j] < minTime)
                    continue;
                if(times[j] > maxTime)
                    break;
                totalIntensities[j] += transitionIntensities[j];
            }
        }

        double maxHeight = 0;
        for (int i = 0; i < times.length; i++)
        {
            if(times[i] < minTime)
                continue;
            if(times[i] > maxTime)
                break;
            series.add(times[i], totalIntensities[i] / 1000);

            maxHeight = Math.max(maxHeight, totalIntensities[i]);
        }
        dataset.addSeries(series);

        return maxHeight / 1000;
    }
}
