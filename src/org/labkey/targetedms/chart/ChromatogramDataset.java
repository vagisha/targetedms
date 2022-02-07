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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.ChartColor;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataRegionSelection;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.security.User;
import org.labkey.api.util.Formats;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.model.PrecursorChromInfoPlus;
import org.labkey.targetedms.model.PrecursorComparator;
import org.labkey.targetedms.parser.Chromatogram;
import org.labkey.targetedms.parser.GeneralMolecule;
import org.labkey.targetedms.parser.GeneralMoleculeChromInfo;
import org.labkey.targetedms.parser.GeneralPrecursor;
import org.labkey.targetedms.parser.GeneralTransition;
import org.labkey.targetedms.parser.Molecule;
import org.labkey.targetedms.parser.MoleculePrecursor;
import org.labkey.targetedms.parser.MoleculeTransition;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.PeptideGroup;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.parser.PrecursorChromInfo;
import org.labkey.targetedms.parser.Replicate;
import org.labkey.targetedms.parser.SampleFile;
import org.labkey.targetedms.parser.SampleFileChromInfo;
import org.labkey.targetedms.parser.Transition;
import org.labkey.targetedms.parser.TransitionChromInfo;
import org.labkey.targetedms.parser.TransitionSettings;
import org.labkey.targetedms.query.MoleculeManager;
import org.labkey.targetedms.query.MoleculePrecursorManager;
import org.labkey.targetedms.query.MoleculeTransitionManager;
import org.labkey.targetedms.query.PeptideManager;
import org.labkey.targetedms.query.PrecursorManager;
import org.labkey.targetedms.query.ReplicateManager;
import org.labkey.targetedms.query.TransitionManager;
import org.labkey.targetedms.query.TransitionManager.TransitionChromInfoAndQuantitative;
import org.labkey.targetedms.view.spectrum.LibrarySpectrumMatchGetter;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * User: vsharma
 * Date: 7/18/2014
 * Time: 2:04 PM
 */
public abstract class ChromatogramDataset
{
    private static final Logger LOG = LogManager.getLogger(ChromatogramDataset.class);

    XYSeriesCollection _jfreeDataset;
    Double _maxDisplayIntensity; // This is set only when we are synchronizing plots on intensity
    Double _minDisplayRt;
    Double _maxDisplayRt;
    boolean _syncRt;
    boolean _syncIntensity;
    double _maxDatasetIntensity; // max intensity across all the traces in the displayed range
    protected TargetedMSRun _run;
    protected TransitionSettings.FullScanSettings _fullScanSettings;

    private Integer _intensityScale;

    // A boolean array for tracking quantitative series (precursor or transition) in a dataset.
    // Quantitative series are rendered with a solid line, non-quantitative ones with a dashed line.
    boolean[] _quantative;

    protected final Container _container;
    protected final User _user;


    public static final BasicStroke LINE = new BasicStroke(2.0f);
    public static final BasicStroke DASH_LINE = new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                                                       1.0f, new float[] {3.0f, 3.0f}, 1.0f);

    public ChromatogramDataset(User u, Container c)
    {
        _container = c;
        _user = u;
    }

    public abstract String getChartTitle();

    // Start of the peak integration boundary. Shown as a vertical dotted line
    public abstract Double getPeakStartTime();

    // End of the peak integration boundary. Shown as a vertical dotted line
    public abstract Double getPeakEndTime();

    public abstract List<ChartAnnotation> getChartAnnotations();

    public abstract Color getSeriesColor(int seriesIndex);

    public Stroke getSeriesStroke(int seriesIndex)
    {
        // Default to solid line if the array is not initialized or we can't index into the array
        boolean isQuantitative = (_quantative == null || _quantative.length <= seriesIndex) || _quantative[seriesIndex];
        return isQuantitative ? LINE : DASH_LINE;
    }

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

    ChromatogramDataset.ChartAnnotation makePeakApexAnnotation(double retentionTime, Double massErrorPpm,
                                                               double intensity, int seriesIndex)
    {
        String label = Formats.f1.format(retentionTime);

        List<String> labels = new ArrayList<>();
        labels.add(label);
        if (massErrorPpm != null)
            labels.add(Formats.f1.format(massErrorPpm) + " ppm");
        return new ChartAnnotation(retentionTime, intensity,
                labels, getSeriesColor(seriesIndex));
    }


    static final class ChartAnnotation
    {
        private final double _retentionTime;
        private final double _intensity;
        private final List<String> _labels;
        private final Color _color;

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

    public static class RtRange
    {
        private final double _minRt;
        private final double _maxRt;

        public RtRange(double minRt, double maxRt)
        {
            _minRt = minRt;
            _maxRt = maxRt;
        }

        public RtRange(double minRt, double maxRt, boolean addMargin, boolean forRtSync)
        {
            if(addMargin && minRt > 0 && maxRt > 0)
            {
                // Margin added on both sides of the peak boundary. Smaller margin if syncing retention time for all the plots.
                double margin = (maxRt - minRt) * (forRtSync ? 0.15 : 1);
                minRt -= margin;
                maxRt += margin;
            }
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

        public boolean isEmpty()
        {
            return _minRt == 0 && _maxRt == 0;
        }

        boolean isAfter(double rt)
        {
            return !isEmpty() && rt < _minRt;
        }

        boolean isBefore(double rt)
        {
            return !isEmpty() && rt > _maxRt;
        }

        boolean contains(double rt)
        {
            return !isEmpty() && rt >= _minRt && rt <= _maxRt;
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

        PeptideDataset(long generalMoleculeId, long sampleFileId, boolean syncIntensity, boolean syncRt, User user, Container container)
        {
            super(generalMoleculeId, sampleFileId, syncIntensity, syncRt, user, container);
        }

        @Override
        List<PrecursorChromInfoPlus> getPrecursorChromInfosForGeneralMolecule()
        {
            return PrecursorManager.getPrecursorChromInfosForPeptide(_generalMoleculeId, _sampleFileId, _user, _container);
        }

        @Override
        protected Color getSeriesColor(PrecursorChromInfo pChromInfo, int seriesIndex)
        {
            return ChartColors.getPrecursorColor(_colorIndexer.getColorIndex(pChromInfo.getPrecursorId(), _user, _container));
        }

        @Override
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

    static class SampleFileDataset extends ChromatogramDataset
    {
        private final SampleFileChromInfo _chromInfo;

        public SampleFileDataset(SampleFileChromInfo chromInfo, User user, Container container)
        {
            super(user, container);
            _chromInfo = chromInfo;
            SampleFile sampleFile = TargetedMSManager.getSampleFile(chromInfo.getSampleFileId(), container);
            Replicate replicate = TargetedMSManager.getReplicate(sampleFile.getReplicateId(), container);
            _run = TargetedMSManager.getRun(replicate.getRunId());
        }

        @Override
        public String getChartTitle()
        {
            return _chromInfo.getTextId() == null ? "Unknown trace" : _chromInfo.getTextId();
        }

        @Override
        public Double getPeakStartTime()
        {
            return null;
        }

        @Override
        public Double getPeakEndTime()
        {
            return null;
        }

        @Override
        public List<ChartAnnotation> getChartAnnotations()
        {
            return Collections.emptyList();
        }

        @Override
        public Color getSeriesColor(int seriesIndex)
        {
            return ChartColors.getPrecursorColor(0);
        }

        @Override
        public void build()
        {
            Chromatogram chromatogram = _chromInfo.createChromatogram(_run);
            if (chromatogram != null)
            {
                _jfreeDataset = new XYSeriesCollection();
                XYSeries series = new XYSeries(getChartTitle());
                float[] times = chromatogram.getTimes();
                float[] intensities = chromatogram.getIntensities(0);
                assert times.length == intensities.length;
                for (int i = 0; i < times.length; i++)
                {
                    series.add(times[i], intensities[i]);
                }
                _jfreeDataset.addSeries(series);
            }
        }
    }

    static class MoleculeDataset extends GeneralMoleculeDataset
    {
        private final MoleculePrecursorColorIndexer _colorIndexer;

        public MoleculeDataset(GeneralMoleculeChromInfo pepChromInfo, boolean syncIntensity, boolean syncRt, User user, Container container)
        {
            super(pepChromInfo, syncIntensity, syncRt, user, container);
            _colorIndexer = new MoleculePrecursorColorIndexer(_generalMoleculeId, user, container);
        }

        @Override
        List<PrecursorChromInfoPlus> getPrecursorChromInfosForGeneralMolecule()
        {
            return MoleculePrecursorManager.getPrecursorChromInfosForMolecule(_generalMoleculeId, _sampleFileId, _user, _container);
        }

        @Override
        protected Color getSeriesColor(PrecursorChromInfo pChromInfo, int seriesIndex)
        {
            return ChartColors.getPrecursorColor(_colorIndexer.getColorIndex(pChromInfo.getPrecursorId(), _user, _container));
        }

        @Override
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

    static abstract class RtRangeDataset extends ChromatogramDataset
    {
        protected double _minPeakRt;
        protected double _maxPeakRt;
        private final List<ChartAnnotation> _annotations = new ArrayList<>();

        /** Create a map of colors to be used for drawing the peaks. */
        protected final Map<Integer, Color> _seriesColors = new HashMap<>();

        public RtRangeDataset(User u, Container c)
        {
            super(u, c);
        }

        protected void addAnnotation(PrecursorChromInfo pChromInfo, PeakInChart peakInChart, int index)
        {
            if(peakInChart.getPeakIntensity() > 0)
            {
                _annotations.add(makePeakApexAnnotation(
                        peakInChart.getPeakRt(),
                        peakInChart.getMassErrorPpm(),
                        peakInChart.getPeakIntensity(),
                        index));
            }
        }

        @Override
        public List<ChartAnnotation> getChartAnnotations()
        {
            return _annotations;
        }

        private static final RtRange DUMMY_RANGE = new RtRange(0, 0);

        protected RtRange summarizeRanges(List<RtRange> ranges)
        {
            double min = ranges.stream().min(Comparator.comparingDouble(RtRange::getMinRt)).orElse(DUMMY_RANGE).getMinRt();
            double max = ranges.stream().max(Comparator.comparingDouble(RtRange::getMaxRt)).orElse(DUMMY_RANGE).getMaxRt();

            return new RtRange(min, max);
        }

        protected RtRange getChromatogramRange(List<PrecursorChromInfoPlus> precursorChromInfoList)
        {
            double minRt = Double.MAX_VALUE;
            double maxRt = 0;

            for(PrecursorChromInfoPlus pChromInfo: precursorChromInfoList)
            {
                // Get the min and max retention times for the precursors of this peptide in a given replicate.
                if(pChromInfo.hasPeakBoundary())
                {
                    minRt = Math.min(minRt, pChromInfo.getMinPeakRt());
                    maxRt = Math.max(maxRt, pChromInfo.getMaxPeakRt());
                }
            }
            _minPeakRt = minRt < Double.MAX_VALUE ? minRt : 0;
            _maxPeakRt = maxRt;

            RtRange displayRange = new RtRange(_minPeakRt, _maxPeakRt, true, _syncRt);

            if(_syncRt){
                // Get the min and max retention times of the precursors for this peptide, over all replicates.
                // TODO: filter this to currently selected replicates
                RtRange peakRtSummary = getRtRangeSummary();
                displayRange = new RtRange(peakRtSummary.getMinRt(), peakRtSummary.getMaxRt(), true, _syncRt);
            }

            if(!displayRange.isEmpty())
            {
                _minDisplayRt = displayRange.getMinRt();
                _maxDisplayRt = displayRange.getMaxRt();
            }
            return displayRange;
        }

        protected abstract RtRange getRtRangeSummary();

        protected PeakInChart addDataToSeries(Chromatogram chromatogram,
                                              PrecursorChromInfoPlus pChromInfo,
                                              RtRange chromatogramRtRange,
                                              int seriesIndex,
                                              XYSeries series)
        {
            float[] times = chromatogram.getTimes();

            List<TransitionChromInfoAndQuantitative> chromInfoList = TransitionManager.getTransitionChromInfoAndQuantitative(pChromInfo, _fullScanSettings);
            // Key in the map is the chromatogram index; used to index into the RT and intensity arrays of the chromatogram.
            // Issue 42518 - May have duplicates for a given index - just choose one
            Map<Integer, TransitionChromInfoAndQuantitative> transitionChromIndexMap = chromInfoList.stream().collect(
                    Collectors.toMap(TransitionChromInfoAndQuantitative::getChromatogramIndex, Function.identity(), (x, y) -> x));

            // We will consider the precursor peak to be "quantitative" if any of its transition peaks are quantitative.
            boolean isQuantitativePrecursor = chromInfoList.stream().anyMatch(TransitionChromInfoAndQuantitative::isQuantitative);
            _quantative[seriesIndex] = isQuantitativePrecursor;

            // sum up the intensities of all transitions of this precursor
            double[] totalIntensities = new double[times.length];

            for(int i = 0; i < chromatogram.getTransitionsCount(); i++)
            {
                if(!transitionChromIndexMap.containsKey(i))
                    continue;

                // Add to the total intensities if the transition peak is quantitative OR if none of the
                // transition peaks for the precursor are quantitative.
                if(!isQuantitativePrecursor || transitionChromIndexMap.get(i).isQuantitative())
                {
                    float[] transitionIntensities = chromatogram.getIntensities(i);
                    assert times.length == transitionIntensities.length : "Length of times and intensities don't match";

                    for (int j = 0; j < times.length; j++)
                    {
                        if(chromatogramRtRange.isAfter(times[j]))
                            continue;
                        if(chromatogramRtRange.isBefore(times[j]))
                            break;
                        totalIntensities[j] += transitionIntensities[j];
                    }
                }
            }

            double maxTraceIntensity = 0;
            double maxPeakIntensity = 0;
            double rtAtPeakApex = 0;
            Double bestMassErrorPpm = pChromInfo.getBestMassErrorPPM();
            if(bestMassErrorPpm == null)
            {
                // If bestMassErrorPpm is not set on the PrecursorChromInfo, try to get it from the quantitative TransitionChromInfos
                TransitionChromInfoAndQuantitative tci = chromInfoList.stream().filter(TransitionChromInfoAndQuantitative::hasHeightAndIsQuantitative)
                        .max(Comparator.comparing(TransitionChromInfoAndQuantitative::getHeight))
                        .orElse(null);
                bestMassErrorPpm = tci != null ? tci.getMassErrorPpm() : null;
            }

            for (int i = 0; i < times.length; i++)
            {
                if(chromatogramRtRange.isAfter(times[i]))
                    continue;
                if(chromatogramRtRange.isBefore(times[i]))
                    break;
                int existingIndex = series.indexOf(times[i]);
                double summedTotal = totalIntensities[i];
                if (existingIndex < 0)
                {
                    series.add(times[i], totalIntensities[i]);
                }
                else
                {
                    XYDataItem existingItem = series.getDataItem(existingIndex);
                    summedTotal = existingItem.getYValue() + totalIntensities[i];
                    existingItem.setY(summedTotal);
                    series.addOrUpdate(existingItem);
                }
                maxTraceIntensity = Math.max(maxTraceIntensity, summedTotal);
                if(pChromInfo.isRtInPeakBoundary(times[i]) && summedTotal > maxPeakIntensity)
                {
                    // Look for the most intense point within the peak integration boundary.
                    maxPeakIntensity = summedTotal;
                    rtAtPeakApex = times[i];
                }
            }

            return new PeakInChart(maxPeakIntensity, rtAtPeakApex, maxTraceIntensity, bestMassErrorPpm);
        }

        abstract Color getSeriesColor(PrecursorChromInfo pChromInfo, int seriesIndex);
    }

    static abstract class GeneralMoleculeDataset extends RtRangeDataset
    {
        protected GeneralMoleculeChromInfo _pepChromInfo;
        protected final long _generalMoleculeId;
        protected final long _sampleFileId;

        public GeneralMoleculeDataset(GeneralMoleculeChromInfo pepChromInfo, boolean syncIntensity, boolean syncRt, User user, Container container)
        {
            this(pepChromInfo.getGeneralMoleculeId(), pepChromInfo.getSampleFileId(), syncIntensity, syncRt, user, container);
            _pepChromInfo = pepChromInfo;
        }

        GeneralMoleculeDataset(long generalMoleculeId, long sampleFileId, boolean syncIntensity, boolean syncRt, User user, Container container)
        {
            super(user, container);
            _generalMoleculeId = generalMoleculeId;
            _sampleFileId = sampleFileId;
            _syncIntensity = syncIntensity;
            _syncRt = syncRt;

            _run = TargetedMSManager.getRunForGeneralMolecule(_generalMoleculeId);

            _fullScanSettings = TargetedMSManager.getTransitionFullScanSettings(_run.getId());
        }

        @Override
        protected RtRange getRtRangeSummary()
        {
            return TransitionManager.getGeneralMoleculeRtRange(_generalMoleculeId);
        }

        abstract String getLabel(PrecursorChromInfo pChromInfo);

        abstract List<PrecursorChromInfoPlus> getPrecursorChromInfosForGeneralMolecule();

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
                // Note: If we are not storing TransitionChromInfos we will not be able to sync the intensity axis for
                // all the plots. MaxHeight for a PrecursorChromInfo is the height of the tallest fragment peak, not the
                // precursor peak which is calculated by summing up the transition peak intensities.
                _maxDisplayIntensity = PrecursorManager.getMaxPrecursorIntensityEstimate(_generalMoleculeId);
            }

            _jfreeDataset = new XYSeriesCollection();

            _quantative = new boolean[precursorChromInfoList.size()];

            for(int i = 0; i < precursorChromInfoList.size(); i++)
            {
                PrecursorChromInfoPlus pChromInfo = precursorChromInfoList.get(i);

                Chromatogram chromatogram = pChromInfo.createChromatogram(_run);
                if (chromatogram != null)
                {
                    XYSeries series = new XYSeries(getLabel(pChromInfo));
                    _jfreeDataset.addSeries(series);
                    // Instead of displaying separate peaks for each transition of this precursor,
                    // we will sum up the intensities and display a single peak for the precursor
                    PeakInChart peakInChart = addDataToSeries(chromatogram, pChromInfo,
                            chromatogramRtRange,
                            i,
                            series);


                    _seriesColors.put(i, getSeriesColor(pChromInfo, i));
                    _maxDatasetIntensity = Math.max(_maxDatasetIntensity, peakInChart.getMaxTraceIntensity());

                    addAnnotation(pChromInfo, peakInChart, i);
                }
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
        public Color getSeriesColor(int seriesIndex)
        {
            return _seriesColors.get(seriesIndex);
        }
    }

    static class PeakInChart
    {
        private final double _peakIntensity; // Max intensity within the peak integration boundary
        private final double _peakRt;        // RT at peak apex (within the peak integration boundary)
        private final double _maxTraceIntensity; // Max intensity in the displayed chromatogram trace.  This
        // may be higher than the max intensity within peak integration boundary.
        private final Double _massErrorPpm;

        private PeakInChart(double peakIntensity, double peakRt, double maxTraceIntensity, Double massErrorPpm)
        {
            _peakRt = peakRt;
            _peakIntensity = peakIntensity;
            _maxTraceIntensity = maxTraceIntensity;
            _massErrorPpm = massErrorPpm;
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

        public Double getMassErrorPpm()
        {
            return _massErrorPpm;
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
        private final boolean _precursorSync;
        private Precursor _precursor;
        private List<LibrarySpectrumMatchGetter.PeptideIdRtInfo> _peptideIdRetentionTimes;

        // The best transition is determined as the quantitative transition with the max intensity
        // within the peak boundary
        protected double _bestTransitionPeakIntensity;
        protected double _bestTransitionRt;
        protected int _bestTransitionSeriesIndex;
        protected Double _bestTransitionPpm;

        public PrecursorDataset(PrecursorChromInfo pChromInfo, boolean syncIntensity, boolean precursorSync, boolean syncRt, User user, Container container)
        {
            super(user, container);
            _run = TargetedMSManager.getRunForPrecursor(pChromInfo.getPrecursorId());
            _pChromInfo = pChromInfo;
            _syncRt = syncRt;
            _syncIntensity = syncIntensity;
            _precursorSync = precursorSync;

            _fullScanSettings = TargetedMSManager.getTransitionFullScanSettings(_run.getId());
        }

        protected long getGeneralMoleculeId()
        {
            if (_precursor == null)
            {
                _precursor = PrecursorManager.getPrecursor(_container, _pChromInfo.getPrecursorId(), _user);
            }
            return _precursor == null ? 0 : _precursor.getGeneralMoleculeId();
        }

        @Nullable
        private GeneralPrecursor getPrecursor()
        {
            getGeneralMoleculeId();
            return _precursor;
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

            if (chromatogram != null)
            {
                // Build the dataset
                buildJFreedataset(chromatogram, chromatogramRtRange);
            }
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
                LOG.warn("Could not find pipeline root for container " + _container.getPath());
                return Collections.emptyList();
            }

        }

        protected void buildJFreedataset(Chromatogram chromatogram, RtRange chromatogramRtRange)
        {
            int transitionCount = chromatogram.getTransitionsCount();
            List<TransChromInfoPlusTransition> tciList = new ArrayList<>(transitionCount);

            for(int chromatogramIndex = 0; chromatogramIndex < transitionCount; chromatogramIndex++)
            {
                List<TransitionChromInfo> tChromInfoList = TransitionManager.getTransitionChromInfoList(_pChromInfo.getId(), chromatogramIndex);

                for (TransitionChromInfo tChromInfo : tChromInfoList)
                {
                    Transition transition = TransitionManager.get(tChromInfo.getTransitionId(), _user, _container);

                    if (include(transition))
                    {
                        tciList.add(new TransChromInfoPlusTransition(tChromInfo, transition));
                    }
                }
            }

            if (tciList.isEmpty() && _pChromInfo.getTransitionChromatogramIndicesList() != null)
            {
                List<Transition> transitions = TransitionManager.getTransitionsForPrecursor(_precursor.getId(), _user, _container);
                if (transitions.size() != _pChromInfo.getTransitionChromatogramIndicesList().size())
                {
                    throw new IllegalStateException("Mismatch in transitions and indices lengths: " + transitions.size() + " vs " + _pChromInfo.getTransitionChromatogramIndicesList().size());
                }
                int index = 0;
                for (Transition transition : transitions)
                {
                    if (include(transition))
                    {
                        tciList.add(new TransChromInfoPlusTransition(_pChromInfo.makeDummyTransitionChromInfo(index), transition));
                    }
                    index++;
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
                        LabelFactory.transitionLabel(chromInfoPlusTransition.getTransition()),
                        chromInfoPlusTransition.getTransition().isQuantitative(_fullScanSettings));
                seriesIndex++;
            }

            initQuantitativeSeriesArray(tciList);
        }

        void initQuantitativeSeriesArray(List<? extends TransitionChromInfoPlusGeneralTransition<?>> chromInfoList)
        {
            if (chromInfoList == null)
            {
                return;
            }
            _quantative = new boolean[chromInfoList.size()];
            int i = 0;
            for(TransitionChromInfoPlusGeneralTransition<?> tci: chromInfoList)
            {
                _quantative[i++] = tci.getTransition().isQuantitative(_fullScanSettings);
            }
        }

        protected void setDatasetValues(TransitionChromInfo transitionChromInfo, Chromatogram chromatogram,
                                        RtRange chromatogramRtRange, int seriesIndex, String label, boolean quantitative)
        {
            PeakInChart tciPeak = addTransitionAsSeries(_jfreeDataset, chromatogram,
                    chromatogramRtRange, transitionChromInfo, label);

            _maxDatasetIntensity = Math.max(_maxDatasetIntensity, tciPeak.getMaxTraceIntensity()); // Max trace intensity in the displayed range

            if(quantitative && tciPeak.getPeakIntensity() > _bestTransitionPeakIntensity)
            {
                // Note: Use the intensity and RT values from the raw data points for plotting.  Don't use getRetentionTime()
                // or getHeight() of the TransitionChromInfo as those values are calculated using interpolated chromatogram numbers.
                // They are close to the raw values but can result in the peak labels being a little off.  An example can be seen
                // in the AreaRatioTestDoc.sky.zip in sampledata. For the peptide ALGSPTKQLLPCEMACNEK the label would be
                // drawn closer to the second most intense point on the peak rather than the most intense point.
                _bestTransitionPeakIntensity = tciPeak.getPeakIntensity();
                _bestTransitionRt = tciPeak.getPeakRt();
                _bestTransitionSeriesIndex = seriesIndex;
                _bestTransitionPpm = tciPeak.getMassErrorPpm();
            }
        }

        private void getMaximumIntensity(long generalMoleculeId)
        {
            _maxDisplayIntensity = null;
            if(_syncIntensity)
            {
                // If we are synchronizing the intensity axis, get the maximum intensity for a transition
                // (of the given type - PRECURSOR, PRODUCT or ALL) over all replicates.
                // TODO: Filter to the currently selected replicates.
                _maxDisplayIntensity = TransitionManager.getMaxTransitionIntensity(generalMoleculeId, _precursorSync ? getPrecursor() : null, getTransitionType());
                if(_maxDisplayIntensity == null)
                {
                    // If we are not saving TransitionChromInfos then get the max value of the MaxHeight on PrecursorChromInfos
                    // This will not take into account the transition type (e.g. "precursor", "product") so is not ideal when
                    // we are showing "split" graphs - separate graphs for "precursor" and "product" fragments. "Precursor"
                    // ions are typically more intense that "product" ions so the axis range for the "product" ion plots wouldb be too large.
                    _maxDisplayIntensity = PrecursorManager.getMaxPrecursorMaxHeight(generalMoleculeId, _precursorSync ? getPrecursor() : null);
                }
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

        private RtRange getChromatogramRange(long generalMoleculeId, PrecursorChromInfo pChromInfo)
        {
            RtRange peakRtSummary;
            if(_syncRt)
            {
                // Get the minimum and maximum RT for the peptide over all the replicates
                peakRtSummary = TransitionManager.getGeneralMoleculeRtRange(generalMoleculeId);
            }
            else
            {
                peakRtSummary = PrecursorManager.getPrecursorPeakRtRange(pChromInfo);
                if(peakRtSummary.isEmpty())
                {
                    // If this precursorChromInfo does not have a minStartTime and maxEndTime AND the startTime and endTime is not set on
                    // any of its transition peaks, then get the minimum minStartTime and maximum maxEndTime across all precursors of this
                    // peptide in this replicate so that we can zoom in the general range where we expect to see the peak.
                    peakRtSummary = TransitionManager.getGeneralMoleculeSampleRtRange(generalMoleculeId, pChromInfo.getSampleFileId());
                }
            }

            RtRange displayRange = new RtRange(peakRtSummary.getMinRt(), peakRtSummary.getMaxRt(), true, _syncRt);
            if(!displayRange.isEmpty())
            {
                _minDisplayRt = displayRange.getMinRt();
                _maxDisplayRt = displayRange.getMaxRt();
            }
            return displayRange;
        }

        // Adds a transition peak to the dataset and returns the max intensity in the displayed chromatogram trace, the
        // max intensity within the beak boundary, and the RT at the peak apex.
        GeneralMoleculeDataset.PeakInChart addTransitionAsSeries(XYSeriesCollection dataset, Chromatogram chromatogram, RtRange rtRange,
                                                                 TransitionChromInfo tci, String label)
        {
            float[] times = chromatogram.getTimes();
            float[] intensities = chromatogram.getIntensities(tci.getChromatogramIndex());
            assert times.length == intensities.length : "Length of times and intensities don't match";

            XYSeries series = new XYSeries(label);

            double maxTraceIntensity = 0; // maximum intensity in the displayed range.

            // The peak apex annotation will be added between minPeakRt and maxPeakRt.
            RtRange peakBoundary = new RtRange(tci.getStartTime() != null ? tci.getStartTime() : 0.0,
                                               tci.getEndTime() != null ? tci.getEndTime() : 0.0);
            double intensityAtPrecursorBestRt = 0;
            double bestRt = 0;
            for (int i = 0; i < times.length; i++)
            {
                if(rtRange.isAfter(times[i]))
                    continue;
                if(rtRange.isBefore(times[i]))
                    break;
                series.add(times[i], intensities[i]);

                if(intensities[i] > maxTraceIntensity)
                {
                    maxTraceIntensity = intensities[i];
                }

                if(peakBoundary.contains(times[i]) && intensities[i] > intensityAtPrecursorBestRt)
                {
                    // Do not try to find a point closest to the RT set on the TransitionChromInfo.  That value is based on
                    // interpolated chromatogram numbers. We are plotting the raw data points.
                    intensityAtPrecursorBestRt = intensities[i];
                    bestRt = times[i];
                }
            }
            dataset.addSeries(series);

            return new PeakInChart(intensityAtPrecursorBestRt, bestRt, maxTraceIntensity, tci.getMassErrorPPM());
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

        public MoleculePrecursorDataset(PrecursorChromInfo pChromInfo, boolean syncIntensity, boolean precursorSync, boolean syncRt, User user, Container container)
        {
            super(pChromInfo, syncIntensity, precursorSync, syncRt, user, container);
        }

        @Override
        protected long getGeneralMoleculeId()
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

            if (tciList.isEmpty() && _pChromInfo.getTransitionChromatogramIndicesList() != null)
            {
                List<MoleculeTransition> transitions = MoleculeTransitionManager.getTransitionsForPrecursor(_molPrecursor.getId(), _user, _container);
                if (transitions.size() != _pChromInfo.getTransitionChromatogramIndicesList().size())
                {
                    throw new IllegalStateException("Mismatch in transitions and indices lengths: " + transitions.size() + " vs " + _pChromInfo.getTransitionChromatogramIndicesList().size());
                }
                int index = 0;
                for (MoleculeTransition transition : transitions)
                {
                    if (include(transition))
                    {
                        tciList.add(new MoleculeTransChromInfoPlusTransition(_pChromInfo.makeDummyTransitionChromInfo(index++), transition));
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
                        LabelFactory.transitionLabel(chromInfoPlusTransition.getTransition()),
                        chromInfoPlusTransition.getTransition().isQuantitative(_fullScanSettings));
                seriesIndex++;
            }

            initQuantitativeSeriesArray(tciList);
        }

        @Override
        public String getChartTitle()
        {
            return LabelFactory.moleculePrecursorChromInfoChartTitle(_pChromInfo, _user, _container );
        }
    }

    static class PrecursorSplitDataset extends PrecursorDataset
    {
        public PrecursorSplitDataset(PrecursorChromInfo pChromInfo, boolean syncIntensity, boolean precursorSync, boolean syncRt, User user, Container container)
        {
            super(pChromInfo, syncIntensity, precursorSync, syncRt, user, container);
        }

        @Override
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
        public ProductSplitDataset(PrecursorChromInfo pChromInfo, boolean syncIntensity, boolean precursorSync, boolean syncRt, User user, Container container)
        {
            super(pChromInfo, syncIntensity, precursorSync, syncRt, user, container);
        }

        @Override
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

    static abstract class TransitionChromInfoPlusGeneralTransition <T extends GeneralTransition>
    {
        TransitionChromInfo _transChromInfo;
        T _transition;

        public TransitionChromInfo getTransChromInfo()
        {
            return _transChromInfo;
        }
        public T getTransition()
        {
            return _transition;
        }
    }

    static class TransChromInfoPlusTransition extends TransitionChromInfoPlusGeneralTransition<Transition>
    {
        public TransChromInfoPlusTransition(TransitionChromInfo transChromInfo, Transition transition)
        {
            _transChromInfo = transChromInfo;
            _transition = transition;
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

    static class MoleculeTransChromInfoPlusTransition extends TransitionChromInfoPlusGeneralTransition<MoleculeTransition>
    {
        public MoleculeTransChromInfoPlusTransition(TransitionChromInfo transChromInfo, MoleculeTransition transition)
        {
            _transChromInfo = transChromInfo;
            _transition = transition;
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
            super(pChromInfo, false, false, false, user, container);
            _pChromInfo = pChromInfo;
            _tChromInfo = tChromInfo;
            _user = user;
            _container = container;
        }

        @Override
        public void build()
        {
            Chromatogram chromatogram = _pChromInfo.createChromatogram(_run);
            if (chromatogram != null)
            {

                if (_tChromInfo.getChromatogramIndex() >= chromatogram.getTransitionsCount())
                {
                    throw new IllegalStateException("Requested chromatogram index " + _tChromInfo.getChromatogramIndex() + " but there are only "
                            + chromatogram.getTransitionsCount() + " transitions.");
                }

                _jfreeDataset = new XYSeriesCollection();
                PeakInChart tciPeak = addTransitionAsSeries(_jfreeDataset, chromatogram,
                        new RtRange(_tChromInfo.getStartTime(), _tChromInfo.getEndTime()),
                        _tChromInfo, getSeriesLabel());

                SampleFile sampleFile = ReplicateManager.getSampleFile(_tChromInfo.getSampleFileId());
                _chartTitle = sampleFile.getSampleName();

                _maxDatasetIntensity = tciPeak.getMaxTraceIntensity(); // max trace intensity in the displayed range
                if (_tChromInfo.getRetentionTime() != null)
                {
                    // Marker for retention time
                    _annotation = makePeakApexAnnotation(tciPeak.getPeakRt(),
                            tciPeak.getMassErrorPpm(),
                            tciPeak.getPeakIntensity(), // max intensity at peak RT
                            0);
                }
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

    public static class GroupDataset extends RtRangeDataset
    {
        private final PeptideGroup _group;
        private final SampleFile _sampleFile;
        private final ViewContext _context;

        private Map<GeneralMolecule, List<PrecursorChromInfoPlus>> _allMolecules;

        public GroupDataset(PeptideGroup group, SampleFile sampleFile, ViewContext context, boolean syncIntensity, boolean syncRt)
        {
            super(context.getUser(), context.getContainer());
            _group = group;
            _sampleFile = sampleFile;
            _context = context;
            _syncIntensity = syncIntensity;
            _syncRt = syncRt;
            _run = TargetedMSManager.getRun(_group.getRunId());
        }

        @Override
        public String getChartTitle()
        {
            return LabelFactory.groupChartTitle(_group, _sampleFile);
        }

        @Override
        public Double getPeakStartTime()
        {
            return null;
        }

        @Override
        public Double getPeakEndTime()
        {
            return null;
        }

        @Override
        protected RtRange getRtRangeSummary()
        {
            List<RtRange> ranges = new ArrayList<>();
            for (GeneralMolecule generalMolecule : _allMolecules.keySet())
            {
                ranges.add(TransitionManager.getGeneralMoleculeRtRange(generalMolecule.getId()));
            }
            return summarizeRanges(ranges);
        }

        @Override
        Color getSeriesColor(PrecursorChromInfo pChromInfo, int seriesIndex)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Color getSeriesColor(int seriesIndex)
        {
            Color result = _seriesColors.get(seriesIndex);
            if (result == null)
            {
                throw new IllegalStateException("No color assigned for " + seriesIndex);
            }
            return result;
        }

        protected List<PrecursorChromInfoPlus> getPrecursorChromInfos()
        {
            _allMolecules = new LinkedHashMap<>();
            String peptideSelectionKey = DataRegionSelection.getSelectionKey(TargetedMSSchema.SCHEMA_NAME, TargetedMSSchema.TABLE_PEPTIDE, null, "Peptides");
            String moleculeSelectionKey = DataRegionSelection.getSelectionKey(TargetedMSSchema.SCHEMA_NAME, TargetedMSSchema.TABLE_MOLECULE, null, "SmallMolecules");

            Set<Long> selectedIds = new HashSet<>();
            selectedIds.addAll(DataRegionSelection.getSelected(_context, peptideSelectionKey, false).stream().map(Long::parseLong).collect(Collectors.toSet()));
            selectedIds.addAll(DataRegionSelection.getSelected(_context, moleculeSelectionKey, false).stream().map(Long::parseLong).collect(Collectors.toSet()));

            for (Molecule molecule : MoleculeManager.getMoleculesForGroup(_group.getId()))
            {
                List<PrecursorChromInfoPlus> chromInfos = MoleculePrecursorManager.getPrecursorChromInfosForMolecule(molecule.getId(), _sampleFile.getId(), _user, _container);
                if (!chromInfos.isEmpty())
                {
                    _allMolecules.put(molecule, chromInfos);
                }
            }
            for (Peptide peptide : PeptideManager.getPeptidesForGroup(_group.getId()))
            {
                List<PrecursorChromInfoPlus> chromInfos = PrecursorManager.getPrecursorChromInfosForPeptide(peptide.getId(), _sampleFile.getId(), _user, _container);
                if (!chromInfos.isEmpty())
                {
                    _allMolecules.put(peptide, chromInfos);
                }
            }

            for (GeneralMolecule generalMolecule : _allMolecules.keySet())
            {
                if (selectedIds.contains(generalMolecule.getId()))
                {
                    // As soon as we find a single match in the selection filter the molecules based on the selected ids
                    Map<GeneralMolecule, List<PrecursorChromInfoPlus>> filtered = new LinkedHashMap<>();
                    for (Map.Entry<GeneralMolecule, List<PrecursorChromInfoPlus>> entry : _allMolecules.entrySet())
                    {
                        if (selectedIds.contains(entry.getKey().getId()))
                        {
                            filtered.put(entry.getKey(), entry.getValue());
                        }
                    }
                    _allMolecules = filtered;
                    break;
                }
            }

            List<PrecursorChromInfoPlus> nonOptimizationPeaks = new ArrayList<>();
            for(List<PrecursorChromInfoPlus> infos : _allMolecules.values())
            {
                for (PrecursorChromInfoPlus info : infos)
                {
                    if(!info.isOptimizationPeak()) // Ignore optimization peaks
                    {
                        nonOptimizationPeaks.add(info);
                    }
                }
            }
            nonOptimizationPeaks.sort(new PrecursorComparator());
            return nonOptimizationPeaks;
        }

        @Override
        public void build()
        {
            // Get the precursor chrom infos for the peptide/molecule
            List<PrecursorChromInfoPlus> precursorChromInfoList = getPrecursorChromInfos();

            if(_syncIntensity)
            {
                // Get the height of the tallest precursor for this peptide/molecule over all replicates
                // TODO: filter this to currently selected replicates
                // Note: If we are not storing TransitionChromInfos we will not be able to sync the intensity axis for
                // all the plots. MaxHeight for a PrecursorChromInfo is the height of the tallest fragment peak, not the
                // precursor peak which is calculated by summing up the transition peak intensities.
                for (GeneralMolecule generalMolecule : _allMolecules.keySet())
                {
                    _maxDisplayIntensity = Math.max(_maxDisplayIntensity == null ? 0 : _maxDisplayIntensity.doubleValue(), PrecursorManager.getMaxPrecursorIntensityEstimate(generalMolecule.getId()));
                }
            }

            _jfreeDataset = new XYSeriesCollection();

            _quantative = new boolean[precursorChromInfoList.size()];

            List<RtRange> ranges = new ArrayList<>();

            int i = 0;
            for (Map.Entry<GeneralMolecule, List<PrecursorChromInfoPlus>> entry : _allMolecules.entrySet())
            {
                GeneralMolecule gm = entry.getKey();

                XYSeries series = new XYSeries(gm.getTextId(), true, false);
                _jfreeDataset.addSeries(series);
                _seriesColors.put(i, ColorGenerator.getColor(gm.getTextId(), _seriesColors.values()));

                // Get the retention time range that should be displayed for this molecule
                RtRange chromatogramRtRange = getChromatogramRange(gm, entry.getValue());
                ranges.add(chromatogramRtRange);

                PeakInChart peakInChart = null;
                for (PrecursorChromInfoPlus info : entry.getValue())
                {
                    Chromatogram chromatogram = info.createChromatogram(_run);
                    if (chromatogram != null)
                    {
                        // Instead of displaying separate peaks for each precursor,
                        // we will sum up the intensities and display a single peak for the molecule
                        peakInChart = addDataToSeries(chromatogram, info,
                                chromatogramRtRange,
                                i,
                                series);

                        _maxDatasetIntensity = Math.max(_maxDatasetIntensity, peakInChart.getMaxTraceIntensity());
                    }
                }
                if (peakInChart != null)
                {
                    addAnnotation(null, peakInChart, i);
                }
                i++;
            }

            RtRange fullRange = summarizeRanges(ranges);
            // Add a little room to the right and left of the full range
            double padding = (fullRange._maxRt - fullRange._minRt) * 0.1;
            _minDisplayRt = fullRange.getMinRt() - padding;
            _maxDisplayRt = fullRange.getMaxRt() + padding;
        }

        private String getLabel(GeneralMolecule gm, PrecursorChromInfoPlus info)
        {
            return gm instanceof Peptide ?
                LabelFactory.precursorLabel(info.getPrecursorId()) :
                LabelFactory.moleculePrecursorLabel(info.getPrecursorId(), _user, _container);
        }

        private RtRange getChromatogramRange(GeneralMolecule generalMolecule, List<PrecursorChromInfoPlus> chromInfos)
        {
            List<RtRange> ranges = new ArrayList<>();
            if (_syncRt)
            {
                // Get the minimum and maximum RT for the molecule across replicates
               ranges.add(TransitionManager.getGeneralMoleculeRtRange(generalMolecule.getId()));
            }
            else
            {
                for (PrecursorChromInfoPlus info : chromInfos)
                {
                    RtRange peakRtSummary = PrecursorManager.getPrecursorPeakRtRange(info);
                    if (peakRtSummary.isEmpty())
                    {
                        // If this precursorChromInfo does not have a minStartTime and maxEndTime AND the startTime and endTime is not set on
                        // any of its transition peaks, then get the minimum minStartTime and maximum maxEndTime across all precursors of this
                        // peptide in this replicate so that we can zoom in the general range where we expect to see the peak.
                        peakRtSummary = TransitionManager.getGeneralMoleculeSampleRtRange(generalMolecule.getId(), info.getSampleFileId());
                    }
                    ranges.add(peakRtSummary);
                }
            }

            RtRange summary = summarizeRanges(ranges);

            return new RtRange(summary._minRt, summary._maxRt, true, _syncRt);
        }
    }
}
