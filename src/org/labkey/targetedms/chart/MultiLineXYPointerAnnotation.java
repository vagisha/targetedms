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

import org.jfree.chart.annotations.XYPointerAnnotation;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.ui.RectangleEdge;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * The base class {@Link XYPointerAnnotation} draws the arrow at (x,y).
 * The labels are drawn by the draw() method override, which accepts a List of String labels.
 * The labels are drawn one below the other.
 */
public class MultiLineXYPointerAnnotation extends XYPointerAnnotation
{
    private java.util.List<String> _labels = new ArrayList<>();

    /**
     * Creates a new multi line label.
     *
     * @param labels  the label (<code>null</code> permitted).
     * @param x  the x-coordinate (measured against the chart's domain axis).
     * @param y  the y-coordinate (measured against the chart's range axis).
     */
    public MultiLineXYPointerAnnotation(List<String> labels, double x, double y)
    {
        super("", x, y,  Math.PI);  // Draw arrow and blank label.
        _labels = labels;
    }

    private int calculateLineHeight()
    {
        return this.getFont().getSize() + 1;  // Additional padding of 1 added.
    }

    /**
     * Draws the annotations.
     *
     * @param g2  the graphics device.
     * @param plot  the plot.
     * @param dataArea  the data area.
     * @param domainAxis  the domain axis.
     * @param rangeAxis  the range axis.
     * @param rendererIndex  the renderer index.
     * @param info  the plot rendering info.
     */
    public void draw(Graphics2D g2, XYPlot plot, Rectangle2D dataArea,
                     ValueAxis domainAxis, ValueAxis rangeAxis,
                     int rendererIndex,
                     PlotRenderingInfo info) {

        // Base class draws arrow, this override draws the multi line label.
        super.draw(g2, plot, dataArea, domainAxis, rangeAxis, rendererIndex, info);

        PlotOrientation orientation = plot.getOrientation();
        RectangleEdge domainEdge = Plot.resolveDomainAxisLocation(
                plot.getDomainAxisLocation(), orientation);
        RectangleEdge rangeEdge = Plot.resolveRangeAxisLocation(
                plot.getRangeAxisLocation(), orientation);

        double j2DX = domainAxis.valueToJava2D(getX(), dataArea, domainEdge);
        double j2DY = rangeAxis.valueToJava2D(getY(), dataArea, rangeEdge);
        if (orientation == PlotOrientation.HORIZONTAL) {
            double temp = j2DX;
            j2DX = j2DY;
            j2DY = temp;
        }
        double labelX = j2DX + Math.cos(getAngle()) * (getBaseRadius()
                + getLabelOffset());
        double labelY = j2DY + Math.sin(getAngle()) * (getBaseRadius()
                + getLabelOffset());

        int lineHeight = calculateLineHeight();
        // Move labels up one line height if there is more than one label inorder to make room for the second label.
        labelY -= 4; // Addition margin of 4 added so text is not close/on to peak.

        g2.setFont(getFont());

        // Draw the multi line label.
        for (int i = _labels.size() -1; i >=0; i--) {
            String l = _labels.get(i);
            g2.drawString(l,(float) labelX,(float) labelY);
            labelY -= lineHeight;
        }
    }
}