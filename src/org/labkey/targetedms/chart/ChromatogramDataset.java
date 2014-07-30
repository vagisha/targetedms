package org.labkey.targetedms.chart;

import org.jfree.data.xy.XYSeriesCollection;
import org.labkey.api.util.Formats;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * User: vsharma
 * Date: 7/18/2014
 * Time: 2:04 PM
 */
public interface ChromatogramDataset
{
    public XYSeriesCollection getJFreeDataset();

    public String getChartTitle();

    // Start of the peak integration boundary. Shown as a vertical dotted line
    public Double getPeakStartTime();

    // End of the peak integration boundary. Shown as a vertical dotted line
    public Double getPeakEndTime();

    // Upper bound for intensity axis.  Used when we are syncing the intensity axis across plots from all replicates
    public Double getMaxDisplayIntensity();

    // Lower bound for retention time axis. Used when we are syncing the retention time axis across plots from all replicates.
    public Double getMinDisplayRetentionTime();

    // Upper bound for the retention time axis. Used when we are syncing the retention time axis across plots from all replicates.
    public Double getMaxDisplayRetentionTime();

    public List<ChartAnnotation> getChartAnnotations();

    public Color getSeriesColor(int seriesIndex);

    public int getIntensityScale();

    public void build();

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

    class Range
    {
        private final double _minRt;
        private final double _maxRt;

        Range(double minRt, double maxRt)
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

    abstract class AbstractDataset implements ChromatogramDataset
    {
        XYSeriesCollection _jfreeDataset;
        Double _maxDisplayIntensity;
        Double _minDisplayRt;
        Double _maxDisplayRt;

        private Integer _intensityScale;

        public XYSeriesCollection getJFreeDataset()
        {
           return _jfreeDataset;
        }

        public Double getMaxDisplayIntensity()
        {
            return _maxDisplayIntensity == null ? null : _maxDisplayIntensity;
        }

        public Double getMinDisplayRetentionTime()
        {
            return _minDisplayRt;
        }

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

        abstract double getMaxDatasetIntensity();

        public int getIntensityScale()
        {
            if(_intensityScale == null)
            {
                double quotient = _maxDisplayIntensity == null ? (getMaxDatasetIntensity() / 1000) : (_maxDisplayIntensity / 1000);
                _intensityScale = quotient < 1 ? 1 : (quotient > 1000 ? 1000000 : 1000);
            }
            return _intensityScale;
        }
    }
}
