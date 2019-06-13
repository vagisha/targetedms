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

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.ui.RectangleEdge;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Map;

/**
 * User: vsharma
 * Date: 7/27/2014
 * Time: 2:34 AM
 */
public class RetentionTimeBoxAndWhiskerRenderer extends BoxAndWhiskerRenderer
{

    public RetentionTimeBoxAndWhiskerRenderer()
    {
        setFillBox(true);
        setMeanVisible(false);
        setMedianVisible(true);
        setWhiskerWidth(0.5);
        setUseOutlinePaintForWhiskers(false);
        setMaximumBarWidth(0.35);
        setItemMargin(0.1);
    }

    // Need to override drawVertical and drawHorizontal methods so that we can draw the FWHM line and whisters
    @Override
    public void drawHorizontalItem(Graphics2D g2, CategoryItemRendererState state, Rectangle2D dataArea, CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis, CategoryDataset dataset, int row, int column)
    {
        super.drawHorizontalItem(g2, state, dataArea, plot, domainAxis, rangeAxis, dataset, row, column);

        // Draw the FWHM lines
        // TODO
    }

    // Need to override drawVertical and drawHorizontal methods so that we can draw the FWHM line and whisters
    @Override
    public void drawVerticalItem(Graphics2D g2, CategoryItemRendererState state, Rectangle2D dataArea, CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis, CategoryDataset dataset, int row, int column)
    {
        super.drawVerticalItem(g2, state, dataArea, plot, domainAxis, rangeAxis, dataset, row, column);

        // Draw the FWHM lines
        DefaultBoxAndWhiskerCategoryDataset rtDataset = (DefaultBoxAndWhiskerCategoryDataset) dataset;

        if(!(rtDataset.getItem(row, column) instanceof ComparisonDataset.RetentionTimeDatasetItem))
            return;
        ComparisonDataset.RetentionTimeDatasetItem item = (ComparisonDataset.RetentionTimeDatasetItem) rtDataset.getItem(row, column);

        RectangleEdge location = plot.getRangeAxisEdge();

        Number yMax = item.getFwhmEnd();
        Number yMin = item.getFwhmStart();
        if (yMax != null && yMin != null) {

            double categoryEnd = domainAxis.getCategoryEnd(column,
                    getColumnCount(), dataArea, plot.getDomainAxisEdge());
            double categoryStart = domainAxis.getCategoryStart(column,
                    getColumnCount(), dataArea, plot.getDomainAxisEdge());
            double categoryWidth = categoryEnd - categoryStart;

            double xx = categoryStart;
            int seriesCount = getRowCount();
            int categoryCount = getColumnCount();

            if (seriesCount > 1) {
                double seriesGap = dataArea.getWidth() * getItemMargin()
                        / (categoryCount * (seriesCount - 1));
                double usedWidth = (state.getBarWidth() * seriesCount)
                        + (seriesGap * (seriesCount - 1));
                // offset the start of the boxes if the total width used is smaller
                // than the category width
                double offset = (categoryWidth - usedWidth) / 2;
                xx = xx + offset + (row * (state.getBarWidth() + seriesGap));
            }
            else {
                // offset the start of the box if the box width is smaller than the
                // category width
                double offset = (categoryWidth - state.getBarWidth()) / 2;
                xx = xx + offset;
            }


            double yyMax = rangeAxis.valueToJava2D(yMax.doubleValue(),
                    dataArea, location);
            double yyMin = rangeAxis.valueToJava2D(yMin.doubleValue(),
                    dataArea, location);
            double xxmid = xx + state.getBarWidth() / 2.0;
            double halfW = (state.getBarWidth() / 2.0) * getWhiskerWidth();


            g2.setPaint(Color.BLACK);

            // draw the vertical line
            g2.draw(new Line2D.Double(xxmid, yyMax, xxmid, yyMin));
            // Draw the upper whisker
            g2.draw(new Line2D.Double(xxmid - halfW, yyMax, xxmid + halfW, yyMax));
            // draw the lower whisker
            g2.draw(new Line2D.Double(xxmid - halfW, yyMin, xxmid + halfW, yyMin));

            g2.setStroke(getItemOutlineStroke(row, column));
        }
    }
}
