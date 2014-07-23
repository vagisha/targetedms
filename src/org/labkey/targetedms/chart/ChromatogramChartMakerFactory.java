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

import org.jfree.chart.ChartColor;
import org.jfree.chart.JFreeChart;
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
        return new ChromatogramChartMaker().make(new TransitionChromatogramDataset(pChromInfo, tChromInfo));
    }

    public static JFreeChart createPrecursorChromChart(PrecursorChromInfo pChromInfo, boolean syncIntensity, boolean syncRt, boolean isSplitGraph)
    {
        if(!isSplitGraph)
        {
            return new ChromatogramChartMaker().make(new PrecursorChromatogramDataset(pChromInfo, syncIntensity, syncRt));
        }
        else
        {
            final PrecursorChromatogramDataset precursorIonDataset = new SplitChromatogramPrecursorDataset(pChromInfo, syncIntensity, syncRt);
            PrecursorChromatogramDataset productIonDataset = new SplitChromatogramProductDataset(pChromInfo, syncIntensity, syncRt) {
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
        return new ChromatogramChartMaker().make(new PeptideChromatogramDataset(pepChromInfo, syncIntensity, syncRt));
    }

    static class PeptideChromatogramDataset extends ChromatogramDataset.AbstractDataset
    {
        private final PeptideChromInfo _pepChromInfo;
        private final boolean _syncRt;
        private final boolean _syncIntensity;

        private List<ChartAnnotation> _annotations;
        private double _minPeakRt;
        private double _maxPeakRt;
        private double _maxIntensity;

        public PeptideChromatogramDataset(PeptideChromInfo pepChromInfo, boolean syncIntensity, boolean syncRt)
        {
            _pepChromInfo = pepChromInfo;
            _syncIntensity = syncIntensity;
            _syncRt = syncRt;

            _annotations = new ArrayList<>();
        }

        @Override
        public void build()
        {
            // Get the precursor chrom infos for the peptide
            List<PrecursorChromInfoPlus> precursorChromInfoList = PrecursorManager.getPrecursorChromInfosForPeptide(_pepChromInfo.getPeptideId(),
                                                                                                                    _pepChromInfo.getSampleFileId());
            Collections.sort(precursorChromInfoList, new PrecursorChromInfoPlus.PrecursorChromInfoComparator());

            // Get the retention time range that should be displayed for the chromatogram
            Range chromatogramRange = getChromatogramRange(precursorChromInfoList);

            if(_syncIntensity)
            {
                // Get the height of the tallest precursor for this peptide over all replicates
                _maxDisplayIntensity = PrecursorManager.getMaxPrecursorIntensity(_pepChromInfo.getPeptideId());
            }

            _jfreeDataset = new XYSeriesCollection();

            for(int i = 0; i < precursorChromInfoList.size(); i++)
            {
                PrecursorChromInfo pChromInfo = precursorChromInfoList.get(i);

                Chromatogram chromatogram = pChromInfo.createChromatogram();

                // Instead of displaying separate peaks for each transition of this precursor,
                // we will sum up the intensities and display a single peak for the precursor
                _maxIntensity = addPrecursorAsSeries(_jfreeDataset, chromatogram, pChromInfo.getId(),
                        chromatogramRange.getMinRt(),
                        chromatogramRange.getMaxRt(),
                        LabelFactory.precursorLabel(pChromInfo.getPrecursorId()));

                if(pChromInfo.getBestRetentionTime() != null)
                    _annotations.add(makePeakApexAnnotation(pChromInfo.getBestRetentionTime(),
                            pChromInfo.getAverageMassErrorPPM(),
                            _maxIntensity,
                            i));

            }
        }

        private ChromatogramDataset.Range getChromatogramRange(List<PrecursorChromInfoPlus> precursorChromInfoList)
        {
            _minPeakRt = Double.MAX_VALUE;
            _maxPeakRt = 0.0;

            for(PrecursorChromInfo pChromInfo: precursorChromInfoList)
            {
                _minPeakRt = pChromInfo.getMinStartTime() != null ? Math.min(_minPeakRt, pChromInfo.getMinStartTime()): _minPeakRt;
                _maxPeakRt = pChromInfo.getMaxEndTime() != null ? Math.max(_maxPeakRt, pChromInfo.getMaxEndTime()) : _maxPeakRt;
            }

            if(_syncRt){
                // get the min and max retention times of the transitions for this peptide, over all replicates
                double minPeptideRt = PeptideManager.getMinRetentionTime(_pepChromInfo.getPeptideId());
                double maxPeptideRt = PeptideManager.getMaxRetentionTime(_pepChromInfo.getPeptideId());
                return new Range(minPeptideRt, maxPeptideRt);
            }
            else
            {
                return new Range(_minPeakRt, _maxPeakRt);
            }
        }

        private double addPrecursorAsSeries(XYSeriesCollection dataset, Chromatogram chromatogram,
                                            int precursorChromId,
                                            double minTime, double maxTime, String label)
        {
            float[] times = chromatogram.getTimes();

            XYSeries series = new XYSeries(label);

            // Display chromatogram only around the peak integration boundary.
            double displayWidth = (maxTime - minTime);
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
                series.add(times[i], totalIntensities[i]);

                maxHeight = Math.max(maxHeight, totalIntensities[i]);
            }
            dataset.addSeries(series);

            return maxHeight;
        }

        @Override
        public String getChartTitle()
        {
            return LabelFactory.peptideChromInfoChartLabel(_pepChromInfo);
        }

        @Override
        public Double getPeakStartTime()
        {
            return _minPeakRt;
        }

        @Override
        public Double getPeakEndTime()
        {
            return _maxPeakRt;
        }

        @Override
        public List<ChartAnnotation> getChartAnnotations()
        {
            return _annotations;
        }

        @Override
        public Color getSeriesColor(int seriesIndex)
        {
            return ChartColors.getPrecursorColor(seriesIndex);
        }

        @Override
        double getMaxDatasetIntensity()
        {
            return _maxIntensity;
        }
    }

    static class SplitChromatogramPrecursorDataset extends PrecursorChromatogramDataset
    {
        public SplitChromatogramPrecursorDataset(PrecursorChromInfo pChromInfo, boolean syncIntensity, boolean syncRt)
        {
            super(pChromInfo, syncIntensity, syncRt);
        }

        Transition.Type getTransitionType()
        {
            return Transition.Type.PRECURSOR;
        }

        boolean include (Transition transition)
        {
            return transition.isPrecursorIon();
        }
    }

    static class SplitChromatogramProductDataset extends PrecursorChromatogramDataset
    {
        public SplitChromatogramProductDataset(PrecursorChromInfo pChromInfo, boolean syncIntensity, boolean syncRt)
        {
            super(pChromInfo, syncIntensity, syncRt);
        }

        Transition.Type getTransitionType()
        {
            return Transition.Type.PRODUCT;
        }

        boolean include (Transition transition)
        {
            return !transition.isPrecursorIon();
        }
    }

    static class PrecursorChromatogramDataset extends ChromatogramDataset.AbstractDataset
    {
        private final PrecursorChromInfo _pChromInfo;
        private final boolean _syncRt;
        private final boolean _syncIntensity;
        private final Precursor _precursor;

        double _bestTransitionIntensity;
        private int _bestTransitionSeriesIndex;
        private Double _bestTransitionRt;
        private Double _bestTransitionPpm;

        public PrecursorChromatogramDataset(PrecursorChromInfo pChromInfo, boolean syncIntensity, boolean syncRt)
        {
            _pChromInfo = pChromInfo;
            _syncRt = syncRt;
            _syncIntensity = syncIntensity;
            _precursor = PrecursorManager.get(_pChromInfo.getPrecursorId());
        }

        @Override
        public void build()
        {
            Chromatogram chromatogram = _pChromInfo.createChromatogram();

            // If this plot is being synced with plots for other replicates on the intensity axis, get the
            // maximum range for the intensity axis.
            getMaximumIntensity(_precursor);

            // Get the retention time range that should be displayed for the chromatogram
            ChromatogramDataset.Range chromatogramRange = getChromatogramRange(_precursor, _pChromInfo);

            // Build the dataset
            buildJFreedataset(chromatogram, chromatogramRange);
        }

        private void buildJFreedataset(Chromatogram chromatogram, Range chromatogramRange)
        {
            List<TransChromInfoPlusTransition> tciList = new ArrayList<>(chromatogram.getTransitionsCount());

            for(int chromatogramIndex = 0; chromatogramIndex < chromatogram.getTransitionsCount(); chromatogramIndex++)
            {
                List<TransitionChromInfo> tChromInfoList = TransitionManager.getTransitionChromInfoList(_pChromInfo.getId(), chromatogramIndex);
                if(tChromInfoList == null || tChromInfoList.size() == 0)
                    continue;
                for(TransitionChromInfo tChromInfo: tChromInfoList)
                {
                    Transition transition = TransitionManager.get(tChromInfo.getTransitionId());

                    if(include(transition))
                    {
                        tciList.add(new TransChromInfoPlusTransition(tChromInfo, transition));
                    }
                }
            }
            // Sort according to the ion order used in Skyline
            Collections.sort(tciList, new TransChromInfoPlusTransitionComparator());

            _jfreeDataset = new XYSeriesCollection();
            _bestTransitionIntensity = 0.0;
            _bestTransitionRt = _pChromInfo.getBestRetentionTime();
            _bestTransitionSeriesIndex = 0;
            int seriesIndex = 0;
            for(TransChromInfoPlusTransition chromInfoPlusTransition: tciList)
            {
                double[] bestIntensityAndHeight = addTransitionAsSeries(_jfreeDataset, chromatogram,
                        chromInfoPlusTransition.getTransChromInfo().getChromatogramIndex(),
                        chromatogramRange.getMinRt(),
                        chromatogramRange.getMaxRt(),
                        LabelFactory.transitionLabel(chromInfoPlusTransition.getTransition()));

                double height = bestIntensityAndHeight[0];
                if(height > _bestTransitionIntensity)
                {
                    _bestTransitionIntensity = height;
                    _bestTransitionSeriesIndex = seriesIndex;
                    _bestTransitionRt = bestIntensityAndHeight[1];
                    _bestTransitionPpm = chromInfoPlusTransition.getTransChromInfo().getMassErrorPPM();
                }
                seriesIndex++;
            }

            if(_bestTransitionPpm == null)
            {
                _bestTransitionPpm = _pChromInfo.getAverageMassErrorPPM();
            }
        }

        private void getMaximumIntensity(Precursor precursor)
        {
            _maxDisplayIntensity = null;
            if(_syncIntensity)
            {
                // If we are synchronizing the intensity axis, get the maximum intensity for a transition
                // (of the given type - PRECURSOR, PRODUCT or ALL) over all replicates.
                _maxDisplayIntensity = TransitionManager.getMaxTransitionIntensity(precursor.getPeptideId(), getTransitionType());
            }
        }

        Transition.Type getTransitionType()
        {
            return Transition.Type.ALL;
        }

        boolean include (Transition transition)
        {
            return true;
        }

        private ChromatogramDataset.Range getChromatogramRange(Precursor precursor, PrecursorChromInfo pChromInfo)
        {
            if(_syncRt)
            {
                // Get the minimum and maximum RT for the peptide over all the replicates
                double minPrecAllReplRt = PeptideManager.getMinRetentionTime(precursor.getPeptideId());
                double maxPrecAllReplRt = PeptideManager.getMaxRetentionTime(precursor.getPeptideId());

                return new ChromatogramDataset.Range(minPrecAllReplRt, maxPrecAllReplRt);
            }
            else
            {
                Double pciMinStartTime = pChromInfo.getMinStartTime();
                Double pciMaxStartTime = pChromInfo.getMaxEndTime();
                // If this precursorChromInfo does not have a minStartTime and maxEndTime,
                // get the minimum minStartTime and maximum maxEndTime for all precursors of this peptide in this replicate.
                if (pciMinStartTime == null)
                {
                    pciMinStartTime = PeptideManager.getMinRetentionTime(precursor.getPeptideId(), pChromInfo.getSampleFileId());
                }
                if (pciMaxStartTime == null)
                {
                    pciMaxStartTime = PeptideManager.getMaxRetentionTime(precursor.getPeptideId(), pChromInfo.getSampleFileId());
                }
                if(pciMinStartTime != null && pciMaxStartTime != 0)
                {
                    return new ChromatogramDataset.Range(pciMinStartTime, pciMaxStartTime);
                }
                else
                {
                    return new ChromatogramDataset.Range(0.0, 0.0);
                }
            }
        }

        // Adds a transition peak to the dataset, and returns the intensity and retention time of the tallest point in the peak.
        // [0] -> intensity
        // [1] -> retention time
        double[] addTransitionAsSeries(XYSeriesCollection dataset, Chromatogram chromatogram, int chromatogramIndex,
                                       double minTime, double maxTime, String label)
        {
            float[] times = chromatogram.getTimes();
            float[] intensities = chromatogram.getIntensities(chromatogramIndex);
            assert times.length == intensities.length : "Length of times and intensities don't match";

            XYSeries series = new XYSeries(label);

            // Display chromatogram only around the peak integration boundary.
            double displayWidth = (maxTime - minTime);
            minTime = minTime - displayWidth;
            maxTime = maxTime + displayWidth;

            double maxHeight = 0;
            double bestRt = 0;
            for (int i = 0; i < times.length; i++)
            {
                if(times[i] < minTime)
                    continue;
                if(times[i] > maxTime)
                    break;
                series.add(times[i], intensities[i]);

                if(intensities[i] > maxHeight)
                {
                    maxHeight = intensities[i];
                    bestRt = times[i];
                }
            }
            dataset.addSeries(series);
            return new double[] {maxHeight, bestRt};
        }

        @Override
        public List<ChromatogramDataset.ChartAnnotation> getChartAnnotations()
        {
            if(_bestTransitionRt == null)
                return Collections.emptyList();
            else
            {
                return Collections.singletonList(makePeakApexAnnotation(_bestTransitionRt,
                        _bestTransitionPpm,
                        _bestTransitionIntensity,
                        _bestTransitionSeriesIndex));
            }
        }

        @Override
        public Double getPeakStartTime()
        {
            return _pChromInfo.getMinStartTime();
        }

        @Override
        public Double getPeakEndTime()
        {
            return _pChromInfo.getMaxEndTime();
        }

        @Override
        public String getChartTitle()
        {
            return LabelFactory.precursorChromInfoChartLabel(_pChromInfo);
        }

        @Override
        public Color getSeriesColor(int seriesIndex)
        {
            return ChartColors.getTransitionColor(seriesIndex + getSeriesOffset());
        }

        int getSeriesOffset()
        {
            return 0;
        }

        @Override
        double getMaxDatasetIntensity()
        {
            return _bestTransitionIntensity;
        }
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

    private static class TransChromInfoPlusTransitionComparator implements Comparator<TransChromInfoPlusTransition>
    {
        private final Transition.TransitionComparator _comparator;

        public TransChromInfoPlusTransitionComparator()
        {
            _comparator = new Transition.TransitionComparator();
        }
        @Override
        public int compare(TransChromInfoPlusTransition t1, TransChromInfoPlusTransition t2)
        {
            return _comparator.compare(t1.getTransition(), t2.getTransition());
        }
    }

    static class TransitionChromatogramDataset extends PrecursorChromatogramDataset
    {
        private final PrecursorChromInfo _pChromInfo;
        private final TransitionChromInfo _tChromInfo;

        private String _chartTitle;
        private ChartAnnotation _annotation;

        public TransitionChromatogramDataset(PrecursorChromInfo pChromInfo, TransitionChromInfo tChromInfo)
        {
            super(pChromInfo, false, false);
            _pChromInfo = pChromInfo;
            _tChromInfo = tChromInfo;
        }

        @Override
        public void build()
        {
            Chromatogram chromatogram = _pChromInfo.createChromatogram();

            if (_tChromInfo.getChromatogramIndex() >= chromatogram.getTransitionsCount())
            {
                throw new IllegalStateException("Requested index " + _tChromInfo.getChromatogramIndex() + " but there are only " + chromatogram.getTransitionsCount());
            }

            _jfreeDataset = new XYSeriesCollection();
            double[] maxHeightAndRt = addTransitionAsSeries(_jfreeDataset, chromatogram, _tChromInfo.getChromatogramIndex(),
                    _pChromInfo.getMinStartTime().floatValue(),
                    _pChromInfo.getMaxEndTime().floatValue(),
                    LabelFactory.transitionLabel(_tChromInfo.getTransitionId()));

            SampleFile sampleFile = ReplicateManager.getSampleFile(_tChromInfo.getSampleFileId());
            _chartTitle = sampleFile.getSampleName();

            if(_tChromInfo.getRetentionTime() != null)
            {
                // Marker for retention time
                _annotation = makePeakApexAnnotation(_tChromInfo.getRetentionTime(),
                        _pChromInfo.getAverageMassErrorPPM(),
                        maxHeightAndRt[0],
                        0);
            }
        }

        @Override
        public String getChartTitle()
        {
            return _chartTitle;
        }

        @Override
        public Double getPeakStartTime()
        {
            return _tChromInfo.getStartTime();
        }

        @Override
        public Double getPeakEndTime()
        {
            return _tChromInfo.getEndTime();
        }

        @Override
        public List<ChartAnnotation> getChartAnnotations()
        {
            return _annotation != null ?  Collections.singletonList(_annotation) : Collections.<ChartAnnotation>emptyList();
        }

        @Override
        public Color getSeriesColor(int seriesIndex)
        {
            return ChartColor.RED;
        }

        @Override
        double getMaxDatasetIntensity()
        {
            return 0;
        }
    }
}
