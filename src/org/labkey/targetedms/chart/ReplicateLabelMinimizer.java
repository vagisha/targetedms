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

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* User: vsharma
* Date: 7/23/2014
* Time: 2:32 PM
*/
public class ReplicateLabelMinimizer
{
    private static final char LABEL_SEP_CHAR = '_';
    private static final String ELIPSIS = "...";
    private static final String ELIPSIS_PATTERN = "\\.\\.\\.";
    private static final char[] SPACE_CHARS = new char[] { '_', '-', ' ', '.', ',' };
    private static final String SPACE_CHAR_PATTERN = "[_\\- .,]";
    private enum ReplaceLocation {start, end, middle}

    private ReplicateLabelMinimizer() {}

    public static Map<String, String> minimize(List<String> labels)
    {
        if(labels == null || labels.size() == 0)
            return Collections.emptyMap();

        List<String> normalizedLabels = normalizeLabels(labels);

        String[] labelParts = normalizedLabels.get(0).split(String.valueOf(LABEL_SEP_CHAR));
        if(labelParts.length == 1)
        {
            return originalLabels(labels);
        }

        // If all labels start with the first part
        String replaceString = labelParts[0];
        String partFirst = replaceString + LABEL_SEP_CHAR;
        boolean allStartWith = labelsContain(normalizedLabels, partFirst, ReplaceLocation.start);
        if(allStartWith)
        {
            return updateLabels(labels, replaceString, ReplaceLocation.start);
        }

        // If all labels end with the last part
        replaceString = labelParts[labelParts.length - 1];
        String partLast = LABEL_SEP_CHAR + replaceString;
        boolean allEndWith = labelsContain(normalizedLabels, partLast, ReplaceLocation.end);
        if(allEndWith)
        {
            return updateLabels(labels, replaceString, ReplaceLocation.end);
        }

        for (int i = 1 ; i < labelParts.length - 1; i++)
        {
            replaceString = labelParts[i];
            if (StringUtils.isBlank(replaceString))
                continue;
            String partMiddle = LABEL_SEP_CHAR + replaceString + LABEL_SEP_CHAR;
            // If all labels contain the middle part
            boolean allContain = labelsContain(normalizedLabels, partMiddle, ReplaceLocation.middle);
            if (allContain)
            {
                return updateLabels(labels, replaceString, ReplaceLocation.middle);
            }
        }
        return originalLabels(labels);
    }

    private static Map<String, String> originalLabels(List<String> labels)
    {
        Map<String, String> displayLabels = new HashMap<>();
        for(String label: labels)
        {
            displayLabels.put(label, label);
        }
        return displayLabels;
    }

    private static List<String> normalizeLabels(List<String> labels)
    {
        List<String> normalized = new ArrayList<>(labels.size());
        for(Object label: labels)
        {
            normalized.add(normalizeLabel((String) label));
        }
        return normalized;
    }

    private static String normalizeLabel(String label)
    {
        String normalized = label.replaceAll(ELIPSIS_PATTERN, String.valueOf(LABEL_SEP_CHAR));
        normalized = normalized.replaceAll(SPACE_CHAR_PATTERN, String.valueOf(LABEL_SEP_CHAR));
        return normalized;
    }

    private static boolean labelsContain(List<String> normalizedLabels, String substring, ReplaceLocation location)
    {
        for(String label: normalizedLabels)
        {
            switch (location)
            {
                case start:
                    if (!label.startsWith(substring))
                    {
                        return false;
                    }
                    break;
                case end:
                    if (!label.endsWith(substring))
                    {
                        return false;
                    }
                    break;
                case middle:
                    if (!label.contains(substring))
                    {
                        return false;
                    }
                    break;
            }
        }
        return true;
    }

    private static Map<String, String> updateLabels(List<String> labels, String replaceString, ReplaceLocation location)
    {
        Map<String, String> displayLabels = new HashMap<>();
        for (String label: labels)
        {
            String newLabel = removeString(label, replaceString, location);
            displayLabels.put(label, newLabel);
        }
        return displayLabels;
    }

    private static String removeString(String label, String replaceString, ReplaceLocation location)
    {
        int startIndex = -1;
        while ((startIndex = label.indexOf(replaceString, startIndex + 1)) != -1)
        {
            int endIndex = startIndex + replaceString.length();
            // Not start string and does not end with space
            if ((startIndex != 0 && !isSpaceChar(label.charAt(startIndex - 1))) ||
                (startIndex == 0 && location != ReplaceLocation.start))
                continue;

            // Not end string and does not start with space
            if ((endIndex != label.length() && !isSpaceChar(label.charAt(endIndex))) ||
                (endIndex == label.length() && location != ReplaceLocation.end))
                continue;

            boolean elipsisSeen = false;
            boolean middle = true;
            // Check left of the string for the start of the label or a space char
            if (startIndex == 0)
                middle = false;
            else if (startIndex >= ELIPSIS.length() && label.lastIndexOf(ELIPSIS, startIndex) == startIndex - ELIPSIS.length())
                elipsisSeen = true;
            else
                startIndex--;

            // Check right of the string for the end of the label or a space char
            if (endIndex == label.length())
                middle = false;
            else if (label.indexOf(ELIPSIS, endIndex) == endIndex)
                elipsisSeen = true;
            else
                endIndex++;
            StringBuilder newLabel = new StringBuilder(label.substring(0, startIndex));
            // Insert an elipsis, if this is in the middle and no elipsis has been seen
            if (middle && !elipsisSeen && location == ReplaceLocation.middle)
                newLabel.append(ELIPSIS);
            newLabel.append(label.substring(endIndex));
            return newLabel.toString();
        }
        return label;
    }

    private static boolean isSpaceChar(char c)
    {
        for(char sc: SPACE_CHARS)
        {
            if (sc == c)
                return true;
        }
        return false;
    }

    public static class TestCase extends Assert
    {
        @Test
        public void testReplicateNameMinimization()
        {
            ComparisonAxis.ReplicateAxis axis = new ComparisonAxis.ReplicateAxis("Replicates");

            Map<String, String> labels = new HashMap<>();
            labels.put("Prefix_name_1_Suffix", "Prefix_name_1_Suffix");
            labels.put("Prefix_name_2_Suffix", "Prefix_name_2_Suffix");

            int iteration = 0;
            while(axis.minimizeLabels(labels))
            {
                iteration++;
                if(iteration == 1)
                {
                    assertEquals(labels.get("Prefix_name_1_Suffix"), "name_1_Suffix");
                    assertEquals(labels.get("Prefix_name_2_Suffix"), "name_2_Suffix");
                }
                if(iteration == 2)
                {
                    assertEquals(labels.get("Prefix_name_1_Suffix"), "1_Suffix");
                    assertEquals(labels.get("Prefix_name_2_Suffix"), "2_Suffix");
                }
                if(iteration == 3)
                {
                    assertEquals(labels.get("Prefix_name_1_Suffix"), "1");
                    assertEquals(labels.get("Prefix_name_2_Suffix"), "2");
                }
            }
            assertEquals(3, iteration);

            labels.clear();
            labels.put("1,ABC-XYZ_File1", "1,ABC-XYZ_File1");
            labels.put("2_ABC-XYZ_File2", "2_ABC-XYZ_File2");
            iteration = 0;
            while(axis.minimizeLabels(labels))
            {
                iteration++;
                if(iteration == 1)
                {
                    assertEquals(labels.get("1,ABC-XYZ_File1"), "1...XYZ_File1");
                    assertEquals(labels.get("2_ABC-XYZ_File2"), "2...XYZ_File2");
                }
                if(iteration == 2)
                {
                    assertEquals(labels.get("1,ABC-XYZ_File1"), "1...File1");
                    assertEquals(labels.get("2_ABC-XYZ_File2"), "2...File2");
                }
            }
            assertEquals(2, iteration);
        }
    }
}
