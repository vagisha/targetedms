/*
 * Copyright (c) 2014-2019 LabKey Corporation
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
package org.labkey.targetedms.chart;

import org.jfree.chart.ChartColor;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.labkey.api.data.Container;
import org.labkey.api.pipeline.LocalDirectory;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.security.User;
import org.labkey.api.util.Formats;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSModule;
import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.model.PrecursorChromInfoPlus;
import org.labkey.targetedms.model.PrecursorComparator;
import org.labkey.targetedms.parser.Chromatogram;
import org.labkey.targetedms.parser.GeneralMoleculeChromInfo;
import org.labkey.targetedms.parser.GeneralTransition;
import org.labkey.targetedms.parser.MoleculePrecursor;
import org.labkey.targetedms.parser.MoleculeTransition;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.parser.PrecursorChromInfo;
import org.labkey.targetedms.parser.SampleFile;
import org.labkey.targetedms.parser.Transition;
import org.labkey.targetedms.parser.TransitionChromInfo;
import org.labkey.targetedms.query.MoleculePrecursorManager;
import org.labkey.targetedms.query.MoleculeTransitionManager;
import org.labkey.targetedms.query.PeptideManager;
import org.labkey.targetedms.query.PrecursorManager;
import org.labkey.targetedms.query.ReplicateManager;
import org.labkey.targetedms.query.TransitionManager;
import org.labkey.targetedms.view.spectrum.LibrarySpectrumMatchGetter;

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
 * Date: 7/18/2014
 * Time: 2:04 PM
 */
public abstract class ChromatogramDataset
{
    XYSeriesCollection _jfreeDataset;
    Double _maxDisplayIntensity; // This is set only when we are synchronizing plots on intensity
    Double _minDisplayRt;
    Double _maxDisplayRt;
    boolean _syncRt;
    boolean _syncIntensity;
    double _maxDatasetIntensity; // max intensity across all the traces in the displayed range
    protected TargetedMSRun _run;

    private Integer _intensityScale;

    public abstract String getChartTitle();

    // Start of the peak integration boundary. Shown as a vertical dotted line
    public abstract Double getPeakStartTime();

    // End of the peak integration boundary. Shown as a vertical dotted line
    public abstract Double getPeakEndTime();

    public abstract List<ChartAnnotation> getChartAnnotations();

    public abstract Color getSeriesColor(int seriesIndex);

    public abstract void build();

    public XYSeriesCollection getJFreeDataset()
    {
        return _jfreeDataset;
    }

    // Upper bound for intensity axis.  Used when we are syncing the intensity axis across plots from all replicates
    public Double getMaxDisplayIntensity()
    {
        // This is set only when we are synchronizing plots on intensity
        return _maxDisplayIntensity;
    }

    public int getIntensityScale()
    {
        if(_intensityScale == null)
        {
            double quotient = _maxDisplayIntensity == null ? (_maxDatasetIntensity / 1000) : (_maxDisplayIntensity / 1000);
            _intensityScale = quotient < 1 ? 1 : (quotient > 1000 ? 1000000 : 1000);
        }
        return _intensityScale;
    }

    // Lower bound for retention time axis. Used when we are syncing the retention time axis across plots from all replicates.
    public Double getMinDisplayRetentionTime()
    {
        return _minDisplayRt;
    }

    // Upper bound for the retention time axis. Used when we are syncing the retention time axis across plots from all replicates.
    public Double getMaxDisplayRetentionTime()
    {
        return _maxDisplayRt;
    }

    ChromatogramDataset.ChartAnnotation makePeakApexAnnotation(double retentionTime, Double averageMassErrorPPM,
                                                               double intensity, int seriesIndex)
    {
        String label = Formats.f1.format(retentionTime);

        List<String> labels = new ArrayList<>();
        labels.add(label);
        if (averageMassErrorPPM != null)
            labels.add(Formats.f1.format(averageMassErrorPPM) + " ppm");
        return new ChromatogramDataset.ChartAnnotation(retentionTime, intensity,
                labels, getSeriesColor(seriesIndex));
    }


    final class ChartAnnotation
    {
        private double _retentionTime;
        private double _intensity;
        private List<String> _labels;
        private Color _color;

        public ChartAnnotation(double retentionTime, double intensity, List<String> labels, Color color)
        {
            _retentionTime = retentionTime;
            _intensity = intensity;
            _labels = labels;
            _color = color;
        }

        public double getRetentionTime()
        {
            return _retentionTime;
        }

        public double getIntensity()
        {
            return _intensity;
        }

        public List<String> getLabels()
        {
            return _labels;
        }

        public Color getColor()
        {
            return _color;
        }
    }

    class RtRange
    {
        private final double _minRt;
        private final double _maxRt;

        RtRange(double minRt, double maxRt)
        {
            _minRt = minRt;
            _maxRt = maxRt;
        }

        public double getMinRt()
        {
            return _minRt;
        }

        public double getMaxRt()
        {
            return _maxRt;
        }
    }

    static class PeptideDataset extends GeneralMoleculeDataset
    {
        private PrecursorColorIndexer _colorIndexer;
        public PeptideDataset(GeneralMoleculeChromInfo pepChromInfo, boolean syncIntensity, boolean syncRt, User user, Container container)
        {
            super(pepChromInfo, syncIntensity, syncRt, user, container);
            _colorIndexer = new PrecursorColorIndexer(_run.getId(), _generalMoleculeId, user, container);
        }

        PeptideDataset(int generalMoleculeId, int sampleFileId, boolean syncIntensity, boolean syncRt, User user, Container container)
        {
            super(generalMoleculeId, sampleFileId, syncIntensity, syncRt, user, container);
        }

        @Override
        List<PrecursorChromInfoPlus> getPrecursorChromInfosForGeneralMolecule()
        {
            return PrecursorManager.getPrecursorChromInfosForPeptide(_generalMoleculeId, _sampleFileId, _user, _container);
        }

        protected Color getSeriesColor(PrecursorChromInfo pChromInfo, int seriesIndex)
        {
            return ChartColors.getPrecursorColor(_colorIndexer.getColorIndex(pChromInfo.getPrecursorId(), _user, _container));
        }

        protected String getLabel(PrecursorChromInfo pChromInfo)
        {
            return LabelFactory.precursorLabel(pChromInfo.getPrecursorId());
        }

        @Override
        public String getChartTitle()
        {
            return LabelFactory.peptideChromInfoChartTitle(_pepChromInfo, _container);
        }
    }

    static class MoleculeDataset extends GeneralMoleculeDataset
    {
        private MoleculePrecursorColorIndexer _colorIndexer;

        public MoleculeDataset(GeneralMoleculeChromInfo pepChromInfo, boolean syncIntensity, boolean syncRt, User user, Container container)
        {
            super(pepChromInfo, syncIntensity, syncRt, user, container);
            _colorIndexer = new MoleculePrecursorColorIndexer(_generalMoleculeId, user, container);
        }

        MoleculeDataset(int generalMoleculeId, int sampleFileId, boolean syncIntensity, boolean syncRt, User user, Container container)
        {
            super(generalMoleculeId, sampleFileId, syncIntensity, syncRt, user, container);
        }

        @Override
        List<PrecursorChromInfoPlus> getPrecursorChromInfosForGeneralMolecule()
        {
            return MoleculePrecursorManager.getPrecursorChromInfosForMolecule(_generalMoleculeId, _sampleFileId, _user, _container);
        }

        protected Color getSeriesColor(PrecursorChromInfo pChromInfo, int seriesIndex)
        {
            return ChartColors.getPrecursorColor(_colorIndexer.getColorIndex(pChromInfo.getPrecursorId(), _user, _container));
        }

        protected String getLabel(PrecursorChromInfo pChromInfo)
        {
            return LabelFactory.moleculePrecursorLabel(pChromInfo.getPrecursorId(), _user, _container);
        }

        @Override
        public String getChartTitle()
        {
            return LabelFactory.moleculeChromInfoChartTitle(_pepChromInfo, _container);
        }
    }

    static abstract class GeneralMoleculeDataset extends ChromatogramDataset
    {
        protected Container _container;
        protected User _user;
        protected GeneralMoleculeChromInfo _pepChromInfo;
        protected final int _generalMoleculeId;
        protected final int _sampleFileId;
        private List<ChartAnnotation> _annotations;
        private double _minPeakRt;
        private double _maxPeakRt;

        private Map<Integer, Color> _seriesColors;

        public GeneralMoleculeDataset(GeneralMoleculeChromInfo pepChromInfo, boolean syncIntensity, boolean syncRt, User user, Container container)
        {
            this(pepChromInfo.getGeneralMoleculeId(), pepChromInfo.getSampleFileId(), syncIntensity, syncRt, user, container);
            _pepChromInfo = pepChromInfo;
        }

        GeneralMoleculeDataset(int generalMoleculeId, int sampleFileId, boolean syncIntensity, boolean syncRt, User user, Container container)
        {
            _generalMoleculeId = generalMoleculeId;
            _sampleFileId = sampleFileId;
            _syncIntensity = syncIntensity;
            _syncRt = syncRt;

            _annotations = new ArrayList<>();

            // Create a map of colors to be used for drawing the peaks.
            _seriesColors = new HashMap<>();

            _run = TargetedMSManager.getRunForGeneralMolecule(_generalMoleculeId);
            _user = user;
            _container = container;
        }

        abstract String getLabel(PrecursorChromInfo pChromInfo);

        abstract List<PrecursorChromInfoPlus> getPrecursorChromInfosForGeneralMolecule();

        abstract Color getSeriesColor(PrecursorChromInfo pChromInfo, int seriesIndex);

        Double getMaxPrecursorIntensity()
        {
            return PrecursorManager.getMaxPrecursorIntensity(_generalMoleculeId);
        }

        @Override
        public void build()
        {
            // Get the precursor chrom infos for the peptide/molecule
            List<PrecursorChromInfoPlus> precursorChromInfoList = getPrecursorChromInfos();

            // Get the retention time range that should be displayed for the chromatogram
            RtRange chromatogramRtRange = getChromatogramRange(precursorChromInfoList);

            if(_syncIntensity)
            {
                // Get the height of the tallest precursor for this peptide/molecule over all replicates
                // TODO: filter this to currently selected replicates
                _maxDisplayIntensity = getMaxPrecursorIntensity();
            }

            _jfreeDataset = new XYSeriesCollection();

            for(int i = 0; i < precursorChromInfoList.size(); i++)
            {
                PrecursorChromInfo pChromInfo = precursorChromInfoList.get(i);

                Chromatogram chromatogram = pChromInfo.createChromatogram(_run);

                // Instead of displaying separate peaks for each transition of this precursor,
                // we will sum up the intensities and display a single peak for the precursor
                PeakInChart peakInChart = addPrecursorAsSeries(_jfreeDataset, chromatogram, pChromInfo,
                        chromatogramRtRange,
                        getLabel(pChromInfo),
                        i);
                _maxDatasetIntensity = Math.max(_maxDatasetIntensity, peakInChart.getMaxTraceIntensity());

                addAnnotation(pChromInfo, peakInChart, i);
            }
        }

        protected void addAnnotation(PrecursorChromInfo pChromInfo, PeakInChart peakInChart, int index)
        {
            if(pChromInfo.getBestRetentionTime() != null)
            {
                _annotations.add(makePeakApexAnnotation(
                        peakInChart.getPeakRt(),
                        pChromInfo.getAverageMassErrorPPM(),
                        peakInChart.getPeakIntensity(),
                        index));
            }
        }

        protected List<PrecursorChromInfoPlus> getPrecursorChromInfos()
        {
            List<PrecursorChromInfoPlus> precursorChromInfoList = getPrecursorChromInfosForGeneralMolecule();

            List<PrecursorChromInfoPlus> nonOptimizationPeaks = new ArrayList<>();
            for(PrecursorChromInfoPlus pChromInfo: precursorChromInfoList)
            {
                if(!pChromInfo.isOptimizationPeak()) // Ignore optimization peaks
                {
                    nonOptimizationPeaks.add(pChromInfo);
                }
            }
            nonOptimizationPeaks.sort(new PrecursorComparator());
            return nonOptimizationPeaks;
        }

        private RtRange getChromatogramRange(List<PrecursorChromInfoPlus> precursorChromInfoList)
        {
            double minRt = Double.MAX_VALUE;
            double maxRt = 0;
            for(PrecursorChromInfo pChromInfo: precursorChromInfoList)
            {
                // Get the min and max retention times for the precursors of this peptide in a given replicate.
                minRt = pChromInfo.getMinStartTime() != null ? Math.min(minRt, pChromInfo.getMinStartTime()): minRt;
                maxRt = pChromInfo.getMaxEndTime() != null ? Math.max(maxRt, pChromInfo.getMaxEndTime()) : maxRt;
            }
            _minPeakRt = minRt < Double.MAX_VALUE ? minRt : 0;
            _maxPeakRt = maxRt;
            // Padding to be added on either side of the displayed range.
            double margin = _maxPeakRt - _minPeakRt;

            if(_syncRt){
                // Get the min and max retention times of the precursors for this peptide, over all replicates.
                // TODO: filter this to currently selected replicates
                _minDisplayRt = PeptideManager.getMinRetentionTime(_generalMoleculeId);
                _maxDisplayRt = PeptideManager.getMaxRetentionTime(_generalMoleculeId);
                if(_minDisplayRt == null) _minDisplayRt = 0.0;
                if(_maxDisplayRt == null) _maxDisplayRt = 0.0;

                margin = (_maxDisplayRt - _minDisplayRt) * 0.15;
            }
            else
            {
                _minDisplayRt = _minPeakRt;
                _maxDisplayRt = _maxPeakRt;
            }

            _minDisplayRt = _minDisplayRt - margin;
            _maxDisplayRt = _maxDisplayRt + margin;

            return new RtRange(_minDisplayRt, _maxDisplayRt);
        }

        private PeakInChart addPrecursorAsSeries(XYSeriesCollection dataset, Chromatogram chromatogram,
                                                 PrecursorChromInfo pChromInfo,
                                                 RtRange chromatogramRtRange, String label,
                                                 int seriesIndex)
        {
            float[] times = chromatogram.getTimes();

            XYSeries series = new XYSeries(label);

            // Display chromatogram only in the given range. This may be more than just the points within the
            // peak integration boundary of this precursor.
            double minTime = chromatogramRtRange.getMinRt();
            double maxTime = chromatogramRtRange.getMaxRt();

            Set<Integer> transitionChromIndexes = TransitionManager.getTransitionChromatogramIndexes(pChromInfo.getId());

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

            double maxTraceIntensity = 0;
            double maxPeakIntensity = 0;
            double rtAtPeakApex = 0;
            // RT at peak start and end for this precursor
            double minPeakRt = pChromInfo.getMinStartTime() != null ? pChromInfo.getMinStartTime() : 0;
            double maxPeakRt = pChromInfo.getMaxEndTime() != null ? pChromInfo.getMaxEndTime() : 0;
            for (int i = 0; i < times.length; i++)
            {
                if(times[i] < minTime)
                    continue;
                if(times[i] > maxTime)
                    break;
                series.add(times[i], totalIntensities[i]);
                maxTraceIntensity = Math.max(maxTraceIntensity, totalIntensities[i]);
                if(times[i] >= minPeakRt && times[i] <= maxPeakRt)
                {
                    // Look for the most intense point within the peak integration boundary.
                    if(totalIntensities[i] > maxPeakIntensity)
                    {
                        maxPeakIntensity = totalIntensities[i];
                        rtAtPeakApex = times[i];
                    }
                }
            }
            dataset.addSeries(series);

            _seriesColors.put(seriesIndex, getSeriesColor(pChromInfo, seriesIndex));

            return new PeakInChart(maxPeakIntensity, rtAtPeakApex, maxTraceIntensity);
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
            return  _seriesColors.get(seriesIndex);
        }

        class PeakInChart
        {
            private final double _peakIntensity; // Max intensity within the peak integration boundary
            private final double _peakRt;        // RT at peak apex (within the peak integration boundary)
            private final double _maxTraceIntensity; // Max intensity in the displayed chromatogram trace.  This
            // may be higher than the max intensity within peak integration boundary.

            private PeakInChart(double peakIntensity, double peakRt, double maxTraceIntensity)
            {
                _peakRt = peakRt;
                _peakIntensity = peakIntensity;
                _maxTraceIntensity = maxTraceIntensity;
            }

            public double getPeakRt()
            {
                return _peakRt;
            }

            public double getPeakIntensity()
            {
                return _peakIntensity;
            }

            public double getMaxTraceIntensity()
            {
                return _maxTraceIntensity;
            }
        }
    }

    static class PrecursorOptimizationPeakDataset extends PeptideDataset
    {
        private double _bestTotalHeight;
        private final PrecursorChromInfo _precursorChromInfo;
        private PeakInChart _bestPeakInChart;
        private PrecursorChromInfo _bestPrecursorChromInfo;
        private int _bestPeakSeriesIndex;

        public PrecursorOptimizationPeakDataset(PrecursorChromInfo precursorChromInfo, boolean syncIntensity, boolean syncRt,
                                                User user, Container container)
        {
            super(PrecursorManager.getPrecursor(container, precursorChromInfo.getPrecursorId(), user).getGeneralMoleculeId(),
                    precursorChromInfo.getSampleFileId(),
                  syncIntensity,
                  syncRt, user, container);

            _precursorChromInfo = precursorChromInfo;
        }

        @Override
        public void build()
        {
            super.build();
            // Add a single annotation for the tallest peak.
            if(_bestPeakInChart != null)
            {
                super.addAnnotation(_bestPrecursorChromInfo, _bestPeakInChart, _bestPeakSeriesIndex);
            }
        }

        @Override
        protected List<PrecursorChromInfoPlus> getPrecursorChromInfos()
        {
            // We have the ID of the precursorChromInfo that is not an optimization peak.
            List<PrecursorChromInfoPlus> precursorChromInfoList = PrecursorManager.getPrecursorChromInfosForGeneralMoleculeChromInfo(
                    _precursorChromInfo.getGeneralMoleculeChromInfoId(),
                    _precursorChromInfo.getPrecursorId(),
                    _precursorChromInfo.getSampleFileId(), _user, _container);

            precursorChromInfoList.sort((o1, o2) ->
            {
                Integer step1 = o1.getOptimizationStep() == null ? 0 : o1.getOptimizationStep();
                Integer step2 = o2.getOptimizationStep() == null ? 0 : o2.getOptimizationStep();
                return step1.compareTo(step2);
            });
            return precursorChromInfoList;
        }

        @Override
        protected void addAnnotation(PrecursorChromInfo pChromInfo, PeakInChart peakInChart, int index)
        {
            if(pChromInfo.getBestRetentionTime() != null)
            {
                // Don't add any annotations here. Record the peak with the maximum total height (sum up the height attribute
                // of the transitionChromInfos for this precursorChromInfo).
                List<TransitionChromInfo> tciList = TransitionManager.getTransitionChromInfoList(pChromInfo.getId());
                double totalHeight = 0;
                for(TransitionChromInfo tci: tciList)
                {
                    Double height = tci.getHeight();
                    if(height != null) totalHeight += tci.getHeight();
                }

                if(_bestPeakInChart == null || _bestTotalHeight < totalHeight)
                {
                    _bestPeakInChart = peakInChart;
                    _bestPrecursorChromInfo = pChromInfo;
                    _bestPeakSeriesIndex = index;
                    _bestTotalHeight = totalHeight;
                }
            }
        }

        @Override
        protected String getLabel(PrecursorChromInfo pChromInfo)
        {
            return LabelFactory.precursorChromInfoLabel(pChromInfo);
        }

        @Override
        public String getChartTitle()
        {
            return LabelFactory.precursorChromInfoChartTitle(_precursorChromInfo);
        }

        @Override
        protected Color getSeriesColor(PrecursorChromInfo pChromInfo, int seriesIndex)
        {
            if(pChromInfo.isOptimizationPeak())
            {
                return ChartColors.getTransitionColor(seriesIndex);
            }
            else
            {
               return ChartColors.getPrecursorColor(0); // Red
            }
        }
    }

    static class PrecursorDataset extends ChromatogramDataset
    {
        protected final PrecursorChromInfo _pChromInfo;
        private Precursor _precursor;
        private List<LibrarySpectrumMatchGetter.PeptideIdRtInfo> _peptideIdRetentionTimes;

        // The best transition is determined as the transition with the max intensity at the
        // bestRetentionTime set on the PrecursorChromInfo (_pChromInfo)
        protected double _bestTransitionPeakIntensity;
        protected double _bestTransitionRt;
        protected int _bestTransitionSeriesIndex;
        protected Double _bestTransitionPpm;
        protected Container _container;
        protected User _user;

        public PrecursorDataset(PrecursorChromInfo pChromInfo, boolean syncIntensity, boolean syncRt, User user, Container container)
        {
            _run = TargetedMSManager.getRunForPrecursor(pChromInfo.getPrecursorId());
            _pChromInfo = pChromInfo;
            _syncRt = syncRt;
            _syncIntensity = syncIntensity;
            _user = user;
            _container = container;
        }

        protected int getGeneralMoleculeId()
        {
            if (_precursor == null)
            {
                _precursor = PrecursorManager.getPrecursor(_container, _pChromInfo.getPrecursorId(), _user);
            }
            return _precursor == null ? 0 : _precursor.getGeneralMoleculeId();
        }

        @Override
        public void build()
        {
            Chromatogram chromatogram = _pChromInfo.createChromatogram(_run);

            // If this plot is being synced with plots for other replicates on the intensity axis, get the
            // maximum range for the intensity axis.
            getMaximumIntensity(getGeneralMoleculeId());

            // Get the retention time range that should be displayed for the chromatogram
            RtRange chromatogramRtRange = getChromatogramRange(getGeneralMoleculeId(), _pChromInfo);

            // Get retention times for any peptide ID matches
            _peptideIdRetentionTimes = getPeptideIdRetentionTimes();

           // Build the dataset
            buildJFreedataset(chromatogram, chromatogramRtRange);
        }

        protected List<LibrarySpectrumMatchGetter.PeptideIdRtInfo> getPeptideIdRetentionTimes()
        {
            SampleFile sampleFile = ReplicateManager.getSampleFile(_pChromInfo.getSampleFileId());

            // TODO: May want to move LocalDirectory up to controller, where others are created. Sharing probably desired.
            // But a bunch of actions filter down to this single chokepoint, so I'm putting it here for now (dave 2/10/18)
            PipeRoot root = PipelineService.get().getPipelineRootSetting(_container);
            if (null != root)
            {
                return LibrarySpectrumMatchGetter.getPeptideIdRts(_precursor, sampleFile, root.getContainer());
            }
            else
            {
                throw new IllegalStateException("Pipeline root not found.");
            }

        }

        protected void buildJFreedataset(Chromatogram chromatogram, RtRange chromatogramRtRange)
        {
            int transitionCount = chromatogram.getTransitionsCount();
            List<TransChromInfoPlusTransition> tciList = new ArrayList<>(transitionCount);

            for(int chromatogramIndex = 0; chromatogramIndex < transitionCount; chromatogramIndex++)
            {
                List<TransitionChromInfo> tChromInfoList = TransitionManager.getTransitionChromInfoList(_pChromInfo.getId(), chromatogramIndex);
                if(tChromInfoList.isEmpty())
                    continue;
                for(TransitionChromInfo tChromInfo: tChromInfoList)
                {
                    Transition transition = TransitionManager.get(tChromInfo.getTransitionId(), _user, _container);

                    if(include(transition))
                    {
                        tciList.add(new TransChromInfoPlusTransition(tChromInfo, transition));
                    }
                }
            }
            // Sort according to the ion order used in Skyline
            tciList.sort(new TransChromInfoPlusTransitionComparator());

            _jfreeDataset = new XYSeriesCollection();
            _maxDatasetIntensity = 0.0;
            _bestTransitionPeakIntensity = 0.0;
            _bestTransitionSeriesIndex = 0;
            int seriesIndex = 0;
            for(TransChromInfoPlusTransition chromInfoPlusTransition: tciList)
            {
                setDatasetValues(chromInfoPlusTransition.getTransChromInfo(), chromatogram, chromatogramRtRange, seriesIndex,
                        LabelFactory.transitionLabel(chromInfoPlusTransition.getTransition()));
                seriesIndex++;
            }

            if(_bestTransitionPpm == null)
            {
                _bestTransitionPpm = _pChromInfo.getAverageMassErrorPPM();
            }
        }

        protected void setDatasetValues(TransitionChromInfo transitionChromInfo, Chromatogram chromatogram,
                                        RtRange chromatogramRtRange, int seriesIndex, String label)
        {
            // bestIntensities[0] = Best intensity in displayed range.
            // bestIntensities[1] = Intensity at the transitionChromInfo's retention time
            double[] bestIntensities = addTransitionAsSeries(_jfreeDataset, chromatogram,
                    chromatogramRtRange, transitionChromInfo, label);

            _maxDatasetIntensity = Math.max(_maxDatasetIntensity, bestIntensities[0]); // Max trace intensity in the displayed range

            double peakHeight = bestIntensities[1];
            if(peakHeight > _bestTransitionPeakIntensity)
            {
                _bestTransitionPeakIntensity = peakHeight;
                _bestTransitionRt = transitionChromInfo.getRetentionTime();
                _bestTransitionSeriesIndex = seriesIndex;
                _bestTransitionPpm = transitionChromInfo.getMassErrorPPM();
            }
        }

        private void getMaximumIntensity(int generalMoleculeId)
        {
            _maxDisplayIntensity = null;
            if(_syncIntensity)
            {
                // If we are synchronizing the intensity axis, get the maximum intensity for a transition
                // (of the given type - PRECURSOR, PRODUCT or ALL) over all replicates.
                // TODO: Filter to the currently selected replicates.
                _maxDisplayIntensity = TransitionManager.getMaxTransitionIntensity(generalMoleculeId, getTransitionType());
            }
        }

        Transition.Type getTransitionType()
        {
            return Transition.Type.ALL;
        }

        <T extends GeneralTransition> boolean include (T transition)
        {
            return true;
        }

        private RtRange getChromatogramRange(int generalMoleculeId, PrecursorChromInfo pChromInfo)
        {
            double margin = 0;
            if(_syncRt)
            {
                // Get the minimum and maximum RT for the peptide over all the replicates
                _minDisplayRt = PeptideManager.getMinRetentionTime(generalMoleculeId);
                _maxDisplayRt = PeptideManager.getMaxRetentionTime(generalMoleculeId);
                if(_minDisplayRt == null) _minDisplayRt = 0.0;
                if(_maxDisplayRt == null) _maxDisplayRt = 0.0;
                margin = (_maxDisplayRt - _minDisplayRt) * 0.15;
            }
            else
            {
                Double pciMinStartTime = pChromInfo.getMinStartTime();
                Double pciMaxEndTime = pChromInfo.getMaxEndTime();
                // If this precursorChromInfo does not have a minStartTime and maxEndTime,
                // get the minimum minStartTime and maximum maxEndTime for all precursors of this peptide in this replicate.
                if (pciMinStartTime == null)
                {
                    pciMinStartTime = PeptideManager.getMinRetentionTime(generalMoleculeId, pChromInfo.getSampleFileId());
                }
                if (pciMaxEndTime == null)
                {
                    pciMaxEndTime = PeptideManager.getMaxRetentionTime(generalMoleculeId, pChromInfo.getSampleFileId());
                }
                if(pciMinStartTime != null && pciMaxEndTime != null)
                {
                    _minDisplayRt = pciMinStartTime;
                    _maxDisplayRt = pciMaxEndTime;
                    margin = _maxDisplayRt - _minDisplayRt;
                }
            }
            if(_minDisplayRt != null && _maxDisplayRt != null)
            {
                _minDisplayRt -= margin;
                _maxDisplayRt += margin;
                return new RtRange(_minDisplayRt, _maxDisplayRt);
            }
            else
            {
                return new RtRange(0.0, 0.0);
            }
        }

        // Adds a transition peak to the dataset and returns a max intensity in the displayed trace as well as the peak
        // intensity.
        // [0] -> max intensity in the displayed trace
        // [1] -> intensity at the TransitionChromInfo's best retention time
        double[] addTransitionAsSeries(XYSeriesCollection dataset, Chromatogram chromatogram, RtRange rtRange,
                                       TransitionChromInfo tci, String label)
        {
            float[] times = chromatogram.getTimes();
            float[] intensities = chromatogram.getIntensities(tci.getChromatogramIndex());
            assert times.length == intensities.length : "Length of times and intensities don't match";

            XYSeries series = new XYSeries(label);

            // Display chromatogram only around the peak integration boundary.
            double minTime = rtRange.getMinRt();
            double maxTime = rtRange.getMaxRt();

            double maxTraceIntensity = 0; // maximum intensity in the displayed range.
            Double tciRt = tci.getRetentionTime();
            double diff = Double.MAX_VALUE;

            double minPeakRt = _pChromInfo.getMinStartTime() != null ? _pChromInfo.getMinStartTime() : 0;
            double maxPeakRt = _pChromInfo.getMaxEndTime() != null ? _pChromInfo.getMaxEndTime() : 0;
            double intensityAtPrecursorBestRt = 0;
            for (int i = 0; i < times.length; i++)
            {
                if(times[i] < minTime)
                    continue;
                if(times[i] > maxTime)
                    break;
                series.add(times[i], intensities[i]);

                if(intensities[i] > maxTraceIntensity)
                {
                    maxTraceIntensity = intensities[i];
                }

                if(tciRt !=  null && times[i] >= minPeakRt && times[i] <= maxPeakRt)
                {
                    // If this transitionChromInfo has a RT, look for the intensity at a point closest to the retention
                    // time set on the transition within the peak integration boundary.
                    double diff_local = Math.abs(tciRt - times[i]);
                    if(diff_local < diff)
                    {
                        diff = diff_local;
                        intensityAtPrecursorBestRt = intensities[i];
                    }
                }
            }
            dataset.addSeries(series);
            return new double[] {maxTraceIntensity, intensityAtPrecursorBestRt};
        }

        @Override
        public List<ChromatogramDataset.ChartAnnotation> getChartAnnotations()
        {
            Double precursorBestRt = _pChromInfo.getBestRetentionTime();
            if(precursorBestRt == null)
                return Collections.emptyList();
            else
            {
                List<ChartAnnotation> annotations = new ArrayList<>();
                annotations.add(makePeakApexAnnotation(
                        _bestTransitionRt,
                        _bestTransitionPpm,
                        _bestTransitionPeakIntensity,
                        _bestTransitionSeriesIndex));

                for(LibrarySpectrumMatchGetter.PeptideIdRtInfo idRtInfo: _peptideIdRetentionTimes)
                {
                    String label = "ID " + Formats.f1.format(idRtInfo.getRt());
                    Color color = idRtInfo.isBestSpectrum() ? Color.red : Color.black;
                    annotations.add(new ChromatogramDataset.ChartAnnotation(idRtInfo.getRt(), Double.MAX_VALUE,
                            Collections.singletonList(label), color));
                }
                return annotations;
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
            return LabelFactory.precursorChromInfoChartTitle(_pChromInfo);
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
    }

    static class MoleculePrecursorDataset extends PrecursorDataset
    {
        private MoleculePrecursor _molPrecursor;

        public MoleculePrecursorDataset(PrecursorChromInfo pChromInfo, boolean syncIntensity, boolean syncRt, User user, Container container)
        {
            super(pChromInfo, syncIntensity, syncRt, user, container);
        }

        @Override
        protected int getGeneralMoleculeId()
        {
            if (_molPrecursor == null)
            {
                _molPrecursor = MoleculePrecursorManager.getPrecursor(_container, _pChromInfo.getPrecursorId(), _user);
            }

            return _molPrecursor == null ? 0 : _molPrecursor.getGeneralMoleculeId();
        }

        @Override
        protected List<LibrarySpectrumMatchGetter.PeptideIdRtInfo> getPeptideIdRetentionTimes()
        {
            return Collections.emptyList();
        }

        @Override
        protected void buildJFreedataset(Chromatogram chromatogram, RtRange chromatogramRtRange)
        {
            int transitionCount = chromatogram.getTransitionsCount();
            List<MoleculeTransChromInfoPlusTransition> tciList = new ArrayList<>(transitionCount);

            for (int chromatogramIndex = 0; chromatogramIndex < transitionCount; chromatogramIndex++)
            {
                List<TransitionChromInfo> tChromInfoList = TransitionManager.getTransitionChromInfoList(_pChromInfo.getId(), chromatogramIndex);
                if (tChromInfoList.isEmpty())
                    continue;
                for (TransitionChromInfo tChromInfo: tChromInfoList)
                {
                    MoleculeTransition transition = MoleculeTransitionManager.get(tChromInfo.getTransitionId(), _user, _container);
                    if(include(transition))
                    {
                        tciList.add(new MoleculeTransChromInfoPlusTransition(tChromInfo, transition));
                    }
                }
            }
            tciList.sort(new MoleculeTransChromInfoPlusTransitionComparator());

            _jfreeDataset = new XYSeriesCollection();
            _maxDatasetIntensity = 0.0;
            _bestTransitionPeakIntensity = 0.0;
            _bestTransitionSeriesIndex = 0;
            int seriesIndex = 0;
            for(MoleculeTransChromInfoPlusTransition chromInfoPlusTransition: tciList)
            {
                setDatasetValues(chromInfoPlusTransition.getTransChromInfo(), chromatogram, chromatogramRtRange, seriesIndex,
                        LabelFactory.transitionLabel(chromInfoPlusTransition.getTransition()));
                seriesIndex++;
            }

            if(_bestTransitionPpm == null)
            {
                _bestTransitionPpm = _pChromInfo.getAverageMassErrorPPM();
            }
        }

        @Override
        public String getChartTitle()
        {
            return LabelFactory.moleculePrecursorChromInfoChartTitle(_pChromInfo, _user, _container );
        }
    }

    static class PrecursorSplitDataset extends PrecursorDataset
    {
        public PrecursorSplitDataset(PrecursorChromInfo pChromInfo, boolean syncIntensity, boolean syncRt, User user, Container container)
        {
            super(pChromInfo, syncIntensity, syncRt, user, container);
        }

        Transition.Type getTransitionType()
        {
            return Transition.Type.PRECURSOR;
        }

        @Override
        <T extends GeneralTransition> boolean include(T transition)
        {
            return transition.isPrecursorIon();
        }
    }

    static class ProductSplitDataset extends PrecursorDataset
    {
        public ProductSplitDataset(PrecursorChromInfo pChromInfo, boolean syncIntensity, boolean syncRt, User user, Container container)
        {
            super(pChromInfo, syncIntensity, syncRt, user, container);
        }

        Transition.Type getTransitionType()
        {
            return Transition.Type.PRODUCT;
        }

        @Override
        <T extends GeneralTransition> boolean include(T transition)
        {
            return !transition.isPrecursorIon();
        }
    }

    static class TransChromInfoPlusTransition
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

    static class TransChromInfoPlusTransitionComparator implements Comparator<TransChromInfoPlusTransition>
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

    static class MoleculeTransChromInfoPlusTransition
    {
        private TransitionChromInfo _transChromInfo;
        private MoleculeTransition _transition;

        public MoleculeTransChromInfoPlusTransition(TransitionChromInfo transChromInfo, MoleculeTransition transition)
        {
            _transChromInfo = transChromInfo;
            _transition = transition;
        }

        public TransitionChromInfo getTransChromInfo()
        {
            return _transChromInfo;
        }

        public MoleculeTransition getTransition()
        {
            return _transition;
        }
    }

    static class MoleculeTransChromInfoPlusTransitionComparator implements Comparator<MoleculeTransChromInfoPlusTransition>
    {
        private final MoleculeTransition.MoleculeTransitionComparator _comparator;

        public MoleculeTransChromInfoPlusTransitionComparator()
        {
            _comparator = new MoleculeTransition.MoleculeTransitionComparator();
        }
        @Override
        public int compare(MoleculeTransChromInfoPlusTransition t1, MoleculeTransChromInfoPlusTransition t2)
        {
            return _comparator.compare(t1.getTransition(), t2.getTransition());
        }
    }

    static class TransitionDataset extends PrecursorDataset
    {
        protected final PrecursorChromInfo _pChromInfo;
        protected final TransitionChromInfo _tChromInfo;

        private String _chartTitle;
        private ChartAnnotation _annotation;
        protected User _user;
        protected Container _container;

        public TransitionDataset(PrecursorChromInfo pChromInfo, TransitionChromInfo tChromInfo, User user, Container container)
        {
            super(pChromInfo, false, false, user, container);
            _pChromInfo = pChromInfo;
            _tChromInfo = tChromInfo;
            _user = user;
            _container = container;
        }

        @Override
        public void build()
        {
            Chromatogram chromatogram = _pChromInfo.createChromatogram(_run);

            if (_tChromInfo.getChromatogramIndex() >= chromatogram.getTransitionsCount())
            {
                throw new IllegalStateException("Requested chromatogram index " + _tChromInfo.getChromatogramIndex() + " but there are only "
                                                + chromatogram.getTransitionsCount() + " transitions.");
            }

            _jfreeDataset = new XYSeriesCollection();
            double[] intensities = addTransitionAsSeries(_jfreeDataset, chromatogram,
                    new RtRange(_tChromInfo.getStartTime(), _tChromInfo.getEndTime()),
                    _tChromInfo, getSeriesLabel());

            SampleFile sampleFile = ReplicateManager.getSampleFile(_tChromInfo.getSampleFileId());
            _chartTitle = sampleFile.getSampleName();

            _maxDatasetIntensity = intensities[0]; // max trace intensity in the displayed range
            if(_tChromInfo.getRetentionTime() != null)
            {
                // Marker for retention time
                _annotation = makePeakApexAnnotation(_tChromInfo.getRetentionTime(),
                        _pChromInfo.getAverageMassErrorPPM(),
                        intensities[1], // max intensity at peak RT
                        0);
            }
        }

        protected String getSeriesLabel()
        {
            return LabelFactory.transitionLabel(TransitionManager.get(_tChromInfo.getTransitionId(), _user, _container));
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
            return _annotation != null ?  Collections.singletonList(_annotation) : Collections.emptyList();
        }

        @Override
        public Color getSeriesColor(int seriesIndex)
        {
            return ChartColor.RED;
        }
    }

    static class MoleculeTransitionDataset extends TransitionDataset
    {
        public MoleculeTransitionDataset(PrecursorChromInfo pChromInfo, TransitionChromInfo tChromInfo, User user, Container container)
        {
            super(pChromInfo, tChromInfo, user, container);
        }

        @Override
        protected String getSeriesLabel()
        {
            return LabelFactory.transitionLabel(MoleculeTransitionManager.get(_tChromInfo.getTransitionId(), _user, _container));
        }
    }
}
