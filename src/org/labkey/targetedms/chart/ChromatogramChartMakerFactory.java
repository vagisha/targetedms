/*
 * Copyright (c) 2012-2014 LabKey Corporation
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
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.labkey.targetedms.model.PrecursorChromInfoPlus;
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

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                                                  pChromInfo.getAverageMassErrorPPM(),
                                                  tChromInfo.getHeight() / 1000,
                                                  0);
        }
        return chartMaker.make();
    }

    public static JFreeChart createPrecursorChromChart(PrecursorChromInfo pChromInfo, boolean syncIntensity, boolean syncMz, boolean isSplitGraph)
    {
        Chromatogram chromatogram = pChromInfo.createChromatogram();
        Precursor precursor = PrecursorManager.get(pChromInfo.getPrecursorId());

        List<TransChromInfoPlusTransition> tciList = new ArrayList<>(chromatogram.getTransitionsCount());

        for(int chromatogramIndex = 0; chromatogramIndex < chromatogram.getTransitionsCount(); chromatogramIndex++)
        {
            List<TransitionChromInfo> tChromInfoList = TransitionManager.getTransitionChromInfoList(pChromInfo.getId(), chromatogramIndex);
            if(tChromInfoList == null || tChromInfoList.size() == 0)
            continue;
            for(TransitionChromInfo tChromInfo: tChromInfoList)
            {
                Transition transition = TransitionManager.get(tChromInfo.getTransitionId());
                tciList.add(new TransChromInfoPlusTransition(tChromInfo, transition));
            }
        }

        Collections.sort(tciList, new TransitionComparator());
        List<TransitionChromDataset> datasets = getTransitionChromDatasets(pChromInfo, syncMz,syncIntensity, isSplitGraph);
        // If the split graph setting is unchecked returns chart with full tciList/dataset.
        if(datasets.size() == 1)
        {
            JFreeChart chart =  getChart(pChromInfo, datasets.get(0), syncIntensity,syncMz, tciList,precursor,isSplitGraph,0);
            return chart;
        }
        else
        {
        List<TransChromInfoPlusTransition> tciPrecursor = new ArrayList<>();
        List<TransChromInfoPlusTransition> tciFragment = new ArrayList<>();
        for(TransChromInfoPlusTransition tci:tciList)
        {
            if(tci.getTransition().isPrecursorIon())
            {
                tciPrecursor.add(tci);
            } else
            {
                tciFragment.add(tci);
            }
        }

        // datasets.get(x) contains a TransitionChromDataset with all the necesary information to build the chart.
        JFreeChart chartPrecursor = getChart(pChromInfo,datasets.get(0),syncIntensity,syncMz, tciPrecursor,precursor, isSplitGraph, 0);
        JFreeChart chartFragment = getChart(pChromInfo,datasets.get(1),syncIntensity,syncMz, tciFragment,precursor, isSplitGraph,tciPrecursor.size());

        if(tciFragment.size() == 0 && tciPrecursor.size() != 0)
            return chartPrecursor;
        if(tciPrecursor.size() == 0 && tciFragment.size() != 0)
            return  chartFragment;
        else
        {
            NumberAxis domain = (NumberAxis) chartFragment.getXYPlot().getDomainAxis();
            domain.setVerticalTickLabels(true);

            double upperMargin = 0.10; // Upper margin added to current upper margin to ensure that the point on the graphs doesn't go off the graph.
            chartFragment.getXYPlot().getRangeAxis().setUpperMargin(chartFragment.getXYPlot().getRangeAxis().getUpperMargin() + upperMargin);
            chartPrecursor.getXYPlot().getRangeAxis().setUpperMargin(chartPrecursor.getXYPlot().getRangeAxis().getUpperMargin() + upperMargin);

            // Sets label font for both plots.
            chartFragment.getXYPlot().getRangeAxis().setLabelFont(new Font("Tahoma", Font.BOLD, 11));
            chartPrecursor.getXYPlot().getRangeAxis().setLabelFont(new Font("Tahoma", Font.BOLD, 11));

            // Create Plot and Combine both XYPlots.
            final CombinedDomainXYPlot plot = new CombinedDomainXYPlot();
            plot.setGap(4.0);
            plot.setDomainAxis(domain);

            // Combines both charts into one plot.
            plot.add(chartPrecursor.getXYPlot(), 1);
            plot.add(chartFragment.getXYPlot(), 1);

            plot.setOrientation(PlotOrientation.VERTICAL);
            JFreeChart jchart = new JFreeChart(chartFragment.getTitle().getText(),
                    ChromatogramChartMaker.DefaultTitleFont, plot, true);
            jchart.setBackgroundPaint(Color.WHITE);
            return jchart;
        }
    }
    }

    private static List<TransitionChromDataset> getTransitionChromDatasets(PrecursorChromInfo pChromInfo, boolean syncMz, boolean syncIntensity, boolean splitGraph)
    {
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

        List<TransChromInfoPlusTransition> tciList = new ArrayList<>(chromatogram.getTransitionsCount());

        for(int chromatogramIndex = 0; chromatogramIndex < chromatogram.getTransitionsCount(); chromatogramIndex++)
        {
            List<TransitionChromInfo> tChromInfoList = TransitionManager.getTransitionChromInfoList(pChromInfo.getId(), chromatogramIndex);
            if(tChromInfoList == null || tChromInfoList.size() == 0)
                continue;
            for(TransitionChromInfo tChromInfo: tChromInfoList)
            {
                Transition transition = TransitionManager.get(tChromInfo.getTransitionId());
                tciList.add(new TransChromInfoPlusTransition(tChromInfo, transition));
            }
        }

        // If is split graph is set to true this will return list of two charts, otherwise list with one chart.
        if(!splitGraph)
        {
            TransitionChromDataset dataset = getDataset(chromatogram, tciList, syncMz, minPrecAllReplRt, maxPrecAllReplRt, pciMinStartTime, pciMaxStartTime, 0);
            List<TransitionChromDataset> list = new ArrayList<>();
            list.add(dataset);
            return list;
        }
        else
        {
            List<TransChromInfoPlusTransition> tciPrecursor = new ArrayList<>();
            List<TransChromInfoPlusTransition> tciFragment = new ArrayList<>();
            for(TransChromInfoPlusTransition tci:tciList)
            {
                if(tci.getTransition().isPrecursorIon())
                {
                    tciPrecursor.add(tci);
                    continue;
                }
                tciFragment.add(tci);
            }

            Collections.sort(tciList, new TransitionComparator());

            TransitionChromDataset precursorDataset = getDataset(chromatogram, tciPrecursor, syncMz, minPrecAllReplRt, maxPrecAllReplRt, pciMinStartTime, pciMaxStartTime, 0);
            TransitionChromDataset productDataset = getDataset(chromatogram, tciFragment, syncMz, minPrecAllReplRt, maxPrecAllReplRt, pciMinStartTime, pciMaxStartTime, precursorDataset.getJfreeDataset().getSeriesCount());

            List<TransitionChromDataset> list = new ArrayList<>();
            list.add(precursorDataset);
            list.add(productDataset);
            return list;
        }
    }

    // Generates the TransitionChromDataset and all including information for each tciList passed in.  Doesn't seperate precursor and product ions.
    private static  TransitionChromDataset getDataset(Chromatogram chromatogram, List<TransChromInfoPlusTransition> tciList, boolean syncMz, float minPrecAllReplRt,
                                  float maxPrecAllReplRt, Double pciMinStartTime, Double pciMaxStartTime, int transitionColorOffset)
    {
        XYSeriesCollection dataset = new XYSeriesCollection();
        Double bestTransitionHeight = 0.0;
        int bestTransitionSeriesIndex = 0;
        int seriesIndex = 0;
        for(TransChromInfoPlusTransition chromInfoPlusTransition: tciList)
        {
            Double height = addTransitionAsSeries(dataset, chromatogram, chromInfoPlusTransition.getTransChromInfo().getChromatogramIndex(),
                    (syncMz || pciMinStartTime == null ? minPrecAllReplRt : pciMinStartTime.floatValue()),
                        (syncMz || pciMaxStartTime == null ? maxPrecAllReplRt : pciMaxStartTime.floatValue()),
                    LabelFactory.transitionLabel(chromInfoPlusTransition.getTransition()));

            if(height > bestTransitionHeight)
            {
                bestTransitionHeight = height;
                bestTransitionSeriesIndex = seriesIndex;
            }
            seriesIndex++;
        }

        // Creates and sets values for the new TransitionChromDataset.
        TransitionChromDataset tcDataset = new TransitionChromDataset(dataset);
        tcDataset.setBestTransitionHeight(bestTransitionHeight);
        tcDataset.setBestTransitionSeriesIndex(bestTransitionSeriesIndex+transitionColorOffset);
        tcDataset.setMaxPrecAllReplRt(maxPrecAllReplRt);
        tcDataset.setMinPrecAllReplRt(minPrecAllReplRt);

        return tcDataset;
    }

    // Contains dataset as well as the color offset, and the min/max precursor replicate retention times.
    static class TransitionChromDataset
    {
        private final XYSeriesCollection _jfreeDataset;
        private float _minPrecAllReplRt;
        private float _maxPrecAllReplRt;
        private int _bestTransitionSeriesIndex;

        Double getBestTransitionHeight()
        {
            return _bestTransitionHeight;
        }

        void setBestTransitionHeight(Double bestTransitionHeight)
        {
            _bestTransitionHeight = bestTransitionHeight;
        }

        private Double _bestTransitionHeight;

        int getBestTransitionSeriesIndex()
        {
            return _bestTransitionSeriesIndex;
        }

        void setBestTransitionSeriesIndex(int bestTransitionSeriesIndex)
        {
            _bestTransitionSeriesIndex = bestTransitionSeriesIndex;
        }


        public TransitionChromDataset(XYSeriesCollection jfreeDataset)
        {
            _jfreeDataset = jfreeDataset;
        }

        XYSeriesCollection getJfreeDataset()
        {
            return _jfreeDataset;
        }

        float getMinPrecAllReplRt()
        {
            return _minPrecAllReplRt;
        }

        void setMinPrecAllReplRt(float minPrecAllReplRt)
        {
            _minPrecAllReplRt = minPrecAllReplRt;
        }

        float getMaxPrecAllReplRt()
        {
            return _maxPrecAllReplRt;
        }

        void setMaxPrecAllReplRt(float maxPrecAllReplRt)
        {
            _maxPrecAllReplRt = maxPrecAllReplRt;
        }
    }

    private static JFreeChart getChart(PrecursorChromInfo pChromInfo,
                                       TransitionChromDataset dataset,
                                       boolean syncIntensity,
                                       boolean syncMz,
                                       List<TransChromInfoPlusTransition> tciList,
                                       Precursor precursor,
                                       boolean isSplitGraph, int offset)
    {
        int bestTransitionSeriesIndex = dataset.getBestTransitionSeriesIndex(); // Split Graph mode, sets color index of annotation point.

        ChromatogramChartMaker chartMaker = new ChromatogramChartMaker(ChromatogramChartMaker.TYPE.PRECURSOR);
        chartMaker.setDataset(dataset.getJfreeDataset());
        chartMaker.setTitle(LabelFactory.precursorChromInfoChartLabel(pChromInfo));
        if(pChromInfo.getMinStartTime() != null)
            chartMaker.setPeakStartTime(pChromInfo.getMinStartTime());
        if(pChromInfo.getMaxEndTime() != null)
            chartMaker.setPeakEndtime(pChromInfo.getMaxEndTime());
        if(pChromInfo.getBestRetentionTime() != null)
            chartMaker.addRetentionTimeAnnotation(pChromInfo.getBestRetentionTime(),
                    pChromInfo.getAverageMassErrorPPM(),
                    (dataset.getBestTransitionHeight()) / 1000,
                    bestTransitionSeriesIndex);

        if(syncIntensity)
        {
            // get the height of the tallest transition peak for this peptide over all replicates
            double maxIntensity;
            if(isSplitGraph && tciList.get(0).getTransition().isPrecursorIon())
            {
                maxIntensity =  TransitionManager.getMaxTransitionIntensity(precursor.getPeptideId(), Transition.Type.PRECURSOR);
            }
            else if(isSplitGraph && !tciList.get(0).getTransition().isPrecursorIon())
            {maxIntensity = TransitionManager.getMaxTransitionIntensity(precursor.getPeptideId(), Transition.Type.PRODUCT);}
            else
            {
                maxIntensity = TransitionManager.getMaxTransitionIntensity(precursor.getPeptideId(), Transition.Type.ALL);
            }
            chartMaker.setMaxIntensity(maxIntensity);
        }
        if(syncMz)
        {
            chartMaker.setMinRt(dataset.getMinPrecAllReplRt());
            chartMaker.setMaxRt(dataset.getMaxPrecAllReplRt());
        }
        return chartMaker.make(offset);
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

    private static class TransitionComparator implements Comparator<TransChromInfoPlusTransition>
    {
        private static Map<String, Integer> ionOrder;
        static{
            ionOrder = new HashMap<>();
            ionOrder.put("precursor", 1);
            ionOrder.put("y", 2);
            ionOrder.put("b", 3);
            ionOrder.put("z", 4);
            ionOrder.put("c", 5);
            ionOrder.put("x", 6);
            ionOrder.put("a", 7);
        }

        @Override
        public int compare(TransChromInfoPlusTransition o1, TransChromInfoPlusTransition o2)
        {
            Transition t1 = o1.getTransition();
            Transition t2 = o2.getTransition();
            int result = ionOrder.get(t1.getFragmentType()).compareTo(ionOrder.get(t2.getFragmentType()));
            if(result == 0)
            {
                if(t1.isPrecursorIon() && t2.isPrecursorIon())
                {
                    // Precursor ions are ordered M, M+1, M+2.
                    return t1.getMassIndex().compareTo(t2.getMassIndex());
                }
                else
                {
                    result = t1.getCharge().compareTo(t2.getCharge());
                    if(result == 0)
                    {
                        // c-term fragment ions are displayed in reverse order -- y9, y8, y7 etc.
                        // n-term fragment ions are displayed in forward order -- b1, b2, b3 etc.
                        if(t1.isNterm() && t2.isNterm())
                        {
                            result = t1.getFragmentOrdinal().compareTo(t2.getFragmentOrdinal());
                            if(result != 0)  return result;
                        }
                        else if(t1.isCterm() && t2.isCterm())
                        {
                            result = t2.getFragmentOrdinal().compareTo(t1.getFragmentOrdinal());
                            if(result != 0)  return result;
                        }
                        return Double.valueOf(t2.getMz()).compareTo(t1.getMz());
                    }
                    return result;
                }
            }
            return result;
        }
    }

    private static double addTransitionAsSeries(XYSeriesCollection dataset, Chromatogram chromatogram, int seriesIndex,
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

        double maxHeight = 0;
        for (int i = 0; i < times.length; i++)
        {
            if(times[i] < minTime)
                continue;
            if(times[i] > maxTime)
                break;
            series.add(times[i], intensities[i] / 1000);

            maxHeight = Math.max(maxHeight, intensities[i]);
        }
        dataset.addSeries(series);
        return maxHeight;
    }

    public static JFreeChart createPeptideChromChart(PeptideChromInfo pepChromInfo, boolean syncIntensity, boolean syncMz)
    {
        // Get the precursor chrom infos for the peptide
        List<PrecursorChromInfoPlus> precursorChromInfoList = PrecursorManager.getPrecursorChromInfosForPeptide(pepChromInfo.getPeptideId(), pepChromInfo.getSampleFileId());
        Collections.sort(precursorChromInfoList, new PrecursorChromInfoPlus.PrecursorChromInfoComparator());

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

        for(int i = 0; i < precursorChromInfoList.size(); i++)
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
                                                      pChromInfo.getAverageMassErrorPPM(),
                                                      maxHeight,
                                                      i);

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
