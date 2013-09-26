/*
 * Copyright (c) 2012-2013 LabKey Corporation
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
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYPointerAnnotation;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.Layer;
import org.jfree.ui.TextAnchor;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static org.labkey.targetedms.chart.ChromatogramChartMaker.TYPE.TRANSITION;

/**
 * User: vsharma
 * Date: 5/1/12
 * Time: 9:01 AM
 */
class ChromatogramChartMaker
{
    public static enum TYPE
    {
        TRANSITION,
        PRECURSOR,
        PEPTIDE
    }

    private XYSeriesCollection _dataset;
    private String _title;
    private Double _peakStartTime;
    private Double _peakEndtime;
    private List<XYPointerAnnotation> _retentionTimeAnnotations;
    private final TYPE _chromatogramType;
    private double _maxIntensity = 0;
    private double _minRt = 0;
    private double _maxRt = 0;

    public ChromatogramChartMaker(TYPE chromatogramType)
    {
        _chromatogramType = chromatogramType;
        _retentionTimeAnnotations = new ArrayList<>();
    }

    private static final DecimalFormat ROUND_1 = new DecimalFormat("0.0");

    public void setDataset(XYSeriesCollection dataset)
    {
        _dataset = dataset;
    }

    public void setTitle(String title)
    {
        _title = title;
    }

    public void setPeakStartTime(Double peakStartTime)
    {
        _peakStartTime = peakStartTime;
    }

    public void setPeakEndtime(Double peakEndtime)
    {
        _peakEndtime = peakEndtime;
    }

    public void addRetentionTimeAnnotation(double retentionTime, Double massError, double height, int seriesIndex)
    {
        String label = ROUND_1.format(retentionTime);
        // TODO: Can we make this 2-line formatting like in Skyline?
        if (massError != null)
            label += " (" + ROUND_1.format(massError) + " ppm)";
        XYPointerAnnotation pointer = new XYPointerAnnotation(label, retentionTime, height, Math.PI);
            pointer.setTipRadius(3.0);  // The radius from the (x, y) point to the tip of the arrow
            pointer.setBaseRadius(13.0); // The radius from the (x, y) point to the start of the arrow line
            pointer.setArrowLength(13.0);  //The length of the arrow head
            pointer.setLabelOffset(-5.0);
            pointer.setFont(new Font("SansSerif", Font.PLAIN, 10));
        switch (_chromatogramType)
        {
            case TRANSITION:
                pointer.setPaint(ChartColor.RED);
                break;

            case PRECURSOR:
                pointer.setPaint(ChartColors.getTransitionColor(seriesIndex));
                break;

            case PEPTIDE:
                pointer.setPaint(ChartColors.getPrecursorColor(seriesIndex));
                break;
        }
        pointer.setTextAnchor(TextAnchor.BOTTOM_LEFT);

        _retentionTimeAnnotations.add(pointer);
    }

    public void setMaxIntensity(double maxIntensity)
    {
        _maxIntensity = maxIntensity;
    }

    public void setMinRt(double minRt)
    {
        _minRt = minRt;
    }

    public void setMaxRt(double maxRt)
    {
        _maxRt = maxRt;
    }

    public JFreeChart make()
    {
        JFreeChart chart = ChartFactory.createXYLineChart(_title, "Retention Time", "Intensity (10^3)",
                 _dataset, PlotOrientation.VERTICAL, true, false, false);

        setupSeriesColors(chart);

        // hide grid lines
        chart.getXYPlot().setRangeGridlinesVisible(false);
        chart.getXYPlot().setDomainGridlinesVisible(false);

        chart.getPlot().setBackgroundPaint(ChartColor.WHITE);

        if(_peakStartTime != null && _peakEndtime != null)
        {
            // add peak integration boundaries
            IntervalMarker marker = new IntervalMarker(_peakStartTime, _peakEndtime);
            marker.setOutlinePaint(Color.BLACK);
            marker.setPaint(Color.WHITE);
            marker.setOutlineStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1.0f, new float[]{2.0f, 4.0f}, 0.0f));
            chart.getXYPlot().addDomainMarker(marker, Layer.BACKGROUND);
        }

        for(XYPointerAnnotation annotation: _retentionTimeAnnotations)
        {
           chart.getXYPlot().addAnnotation(annotation);
        }

        if(_maxIntensity != 0)
        {
            double intensity = _maxIntensity / 1000;
            double margin = intensity * 0.05;
            chart.getXYPlot().getRangeAxis().setUpperBound(intensity + margin);
        }

        if(_minRt != 0 && _maxRt != 0)
        {
            double range = _maxRt - _minRt;
            double margin = range * 3  * 0.05;
            chart.getXYPlot().getDomainAxis().setLowerBound(_minRt  - margin);
            chart.getXYPlot().getDomainAxis().setUpperBound(_maxRt  + margin);
        }
        chart.getTitle().setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12));

        return chart;
    }

    private void setupSeriesColors(JFreeChart chart)
    {
        XYPlot plot = chart.getXYPlot();
        XYItemRenderer renderer = plot.getRenderer();

        if(_chromatogramType == TRANSITION)
        {
            assert plot.getSeriesCount() == 1 : "Transition chromatogram has "+plot.getSeriesCount()+" series. Expected 1";
            renderer.setSeriesPaint(0, ChartColor.RED);
            renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        }
        else if(_chromatogramType == TYPE.PRECURSOR)
        {
            for(int i = 0; i < plot.getSeriesCount(); i++)
            {
                renderer.setSeriesPaint(i, ChartColors.getTransitionColor(i));
                renderer.setSeriesStroke(i, new BasicStroke(2.0f));
            }
        }
        else if(_chromatogramType == TYPE.PEPTIDE)
        {
            for(int i = 0; i < plot.getSeriesCount(); i++)
            {
                renderer.setSeriesPaint(i, ChartColors.getPrecursorColor(i));
                renderer.setSeriesStroke(i, new BasicStroke(2.0f));
            }
        }
    }
}
