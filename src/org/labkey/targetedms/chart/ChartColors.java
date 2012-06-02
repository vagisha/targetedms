/*
 * Copyright (c) 2012 LabKey Corporation
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

import java.awt.*;

/**
 * User: vsharma
 * Date: 4/29/12
 * Time: 10:37 PM
 */
public class ChartColors
{
    public static final Color BLUE =        new ChartColor(0,0,255);
    public static final Color BLUE_VIOLET = new ChartColor(138, 43, 226);
    public static final Color BROWN =       new ChartColor(165, 42, 42);
    public static final Color CHOCOLATE =   new ChartColor(210, 105, 30);
    public static final Color DARK_CYAN =   new ChartColor(0, 139, 139);
    public static final Color GREEN =       new ChartColor(0, 128, 0);
    public static final Color ORANGE =      new ChartColor(255, 165, 0);
    public static final Color BLUE_GRAY =   new ChartColor(117, 112, 179);
    public static final Color PURPLE =      new ChartColor(128, 0, 129);
    public static final Color LIME_GREEN =  new ChartColor(50, 205, 50);
    public static final Color GOLD =        new ChartColor(255, 215, 0);
    public static final Color MAGENTA =     new ChartColor(255, 0, 255);
    public static final Color MAROON =      new ChartColor(128, 0, 0);
    public static final Color OLIVE_DRAB =  new ChartColor(107, 142, 35);
    public static final Color ROYAL_BLUE =  new ChartColor(65, 105, 225);

    public static final Color LIGHT_BLUE =  new ChartColor(173, 216, 230);

    // Colors for transition peaks
    private static final Color[] TRANSITIONS = {BLUE,
                                                BLUE_VIOLET,
                                                BROWN,
                                                CHOCOLATE,
                                                DARK_CYAN,
                                                GREEN,
                                                ORANGE,
                                                BLUE_GRAY,
                                                PURPLE,
                                                LIME_GREEN,
                                                GOLD,
                                                MAGENTA,
                                                MAROON,
                                                OLIVE_DRAB,
                                                ROYAL_BLUE
                                              };

    // Colors for precursor peaks
    private static final Color[] PRECURSORS = {Color.RED,
                                               BLUE,
                                               MAROON,
                                               PURPLE,
                                               ORANGE,
                                               GREEN,
                                               Color.YELLOW,
                                               LIGHT_BLUE
                                               };

    // Colors for isotope labels
    private static final Color[] ISOTOPES = PRECURSORS;

    public static Color getTransitionColor(int index)
    {
        return TRANSITIONS[index % TRANSITIONS.length];
    }

    public static Color getPrecursorColor(int index)
    {
        return PRECURSORS[index % PRECURSORS.length];
    }

    public static Color getIsotopeColor(int index)
    {
        return ISOTOPES[index % ISOTOPES.length];
    }
}
