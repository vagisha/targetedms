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
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.text.TextBlock;
import org.jfree.ui.RectangleEdge;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
* User: vsharma
* Date: 7/23/2014
* Time: 2:30 PM
*/
abstract class ComparisonAxis extends CategoryAxis
{
    private Map<String, String> _displayLabels;

    public ComparisonAxis(String label)
    {
        super(label);
    }

    @Override
    protected TextBlock createLabel(Comparable category, float width, RectangleEdge edge, Graphics2D g2)
    {
        String label = getDisplayLabel(category.toString(), g2, width);
        return super.createLabel(label, width, edge, g2);
    }

    private String getDisplayLabel(String category, Graphics2D g2, float width)
    {
        if(_displayLabels == null)
        {
            calculateDisplayLabels(width, g2);
        }
        String displayLabel = _displayLabels.get(category);
        return displayLabel == null ? category : displayLabel;
    }

    protected abstract Map<String, String> getFullLengthLabels(Collection<String> categories);
    protected abstract boolean hasTrimmedLabels();
    protected abstract Map<String, String> getTrimmedLabels(Collection<String> originalLabels);

    private void calculateDisplayLabels(float availableWidth, Graphics2D g2)
    {
        CategoryPlot plot = (CategoryPlot) getPlot();
        java.util.List categories = plot.getCategoriesForAxis(this);

        Map<String, String> originalLabels = getFullLengthLabels(categories);

        FontMetrics fm = g2.getFontMetrics(getTickLabelFont());
        final Font font = fm.getFont();
        final int originalSize = font.getSize();
        final int smallestSize = originalSize; // 8; // Changing the font makes the legend overlap category labels.
        for(int i = originalSize; i >= smallestSize; i--)
        {
            Font newFont = font.deriveFont((float)i);
            if(tryFont(availableWidth, originalLabels.values(), newFont, g2))
            {
                _displayLabels = originalLabels;
                return;
            }
        }

        Map<String, String> trimmedLabels = getFullLengthLabels(categories);
        if(hasTrimmedLabels())
        {
            // Full size labels did not fit. Try minimized labels
            while(minimizeLabels(trimmedLabels))
            {
                for(int i = originalSize; i >= smallestSize; i--)
                {
                    Font newFont = font.deriveFont((float)i);
                    if(tryFont(availableWidth, trimmedLabels.values(), newFont, g2))
                    {
                        _displayLabels = trimmedLabels;
                        return;
                    }
                }
            }
        }

        // If we are here we did not find a combination of labels and font size that fits.
        // Select the smallest font-size and trimmed labels
        setTickLabelFont(font.deriveFont((float)smallestSize));
        _displayLabels = trimmedLabels;
    }

    boolean minimizeLabels(Map<String, String> trimmedLabels)
    {
        Map<String, String> newTrimmedLabels = getTrimmedLabels(trimmedLabels.values());
        // check if the labels have changed from what was given
        for(String key: newTrimmedLabels.keySet())
        {
            if(key.equals(newTrimmedLabels.get(key)))
            {
                return false;
            }
            break; // enough to check only one
        }

        for(String category: trimmedLabels.keySet())
        {
            String oldTrimmed = trimmedLabels.get(category);
            String newTrimmed = newTrimmedLabels.get(oldTrimmed);
            if(newTrimmed != null)
            {
                trimmedLabels.put(category, newTrimmed);
            }
        }
        return true;
    }

    private float getMaxRequiredWidth(FontMetrics fm, Collection<String> labels)
    {
        float maxWidth = Float.MIN_VALUE;
        for (String label: labels) {
            int labelWidth = fm.stringWidth(label);
            maxWidth = Math.max(maxWidth, labelWidth);
        }
        return maxWidth;
    }

    private boolean tryFont(float availableWidth, Collection<String> labels, Font font, Graphics2D g2)
    {
        FontMetrics newFm = g2.getFontMetrics(font);
        float maxWidth = getMaxRequiredWidth(newFm, labels);
        if(maxWidth <= availableWidth)
        {
            setTickLabelFont(font);
            return true;
        }
        return false;
    }

    static class GeneralMoleculeAxis extends ComparisonAxis
    {
        private Map<String, ComparisonCategory> _categoryMap;
        public GeneralMoleculeAxis(String label, Map<String, ComparisonCategory> categoryMap)
        {
            super(label);
            _categoryMap = categoryMap;
        }

        protected Map<String, String> getFullLengthLabels(Collection<String> categories)
        {
            Iterator iterator = categories.iterator();
            Map<String, String> originalLabels = new HashMap<>();
            while (iterator.hasNext()) {
                String category = iterator.next().toString();
                ComparisonCategory pepCategory = _categoryMap.get(category);
                if(pepCategory != null)
                {
                    originalLabels.put(category, pepCategory.getDisplayLabel());
                }
                else
                {
                    originalLabels.put(category, category);
                }
            }
            return originalLabels;
        }

        @Override
        protected boolean hasTrimmedLabels()
        {
            return false;
        }

        @Override
        protected Map<String, String> getTrimmedLabels(Collection<String> originalLabels)
        {
            return getFullLengthLabels(originalLabels);
        }
    }

    static class ReplicateAxis extends ComparisonAxis
    {
        public ReplicateAxis(String label)
        {
            super(label);
        }

        protected Map<String, String> getFullLengthLabels(Collection<String> categories)
        {
            Iterator iterator = categories.iterator();
            Map<String, String> originalLabels = new HashMap<>();
            while (iterator.hasNext()) {
                String category = iterator.next().toString();
                originalLabels.put(category, category);
            }
            return originalLabels;
        }

        @Override
        protected boolean hasTrimmedLabels()
        {
            return true;
        }

        @Override
        protected Map<String, String> getTrimmedLabels(Collection<String> originalLabels)
        {
            List<String> labels = new ArrayList<>(originalLabels);
            return ReplicateLabelMinimizer.minimize(labels);
        }
    }
}
