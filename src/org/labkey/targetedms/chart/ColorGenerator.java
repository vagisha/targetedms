package org.labkey.targetedms.chart;

import java.awt.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Ported from https://github.com/ProteoWizard/pwiz/blob/master/pwiz_tools/Skyline/Model/ColorGenerator.cs
 */
public class ColorGenerator
{
    private static final int COLLISION_THRESHOLD = 30;

    /// <summary>
    /// Generate a color for the given protein.  We try to make colors within a
    /// protein distinguishable, worrying less about differentiation between colors
    /// from different proteins.
    /// </summary>
    public static java.awt.Color getColor(String peptideName, Collection<Color> siblingColors)
    {
        if (peptideName == null)
            return new Color(170, 170, 170);

        // Get hashed color index for this peptide
        int index = getColorIndex(peptideName);

        // Check for collision with other peptides in this protein.  A collision happens
        // when two colors are close enough that they would be hard to distinguish.
        if (siblingColors.size() < COLORS.size())    // collisions can't be avoided beyond the size of the color array
        {
            for (int i = 0; i < COLORS.size(); i++)
            {
                var color = COLORS.get(index);
                boolean collision = false;
                for (Color existingColor : siblingColors)
                {
                    if (Math.abs(color.getRed() - existingColor.getRed()) < COLLISION_THRESHOLD &&
                            Math.abs(color.getGreen() - existingColor.getGreen()) < COLLISION_THRESHOLD &&
                            Math.abs(color.getBlue() - existingColor.getBlue()) < COLLISION_THRESHOLD)
                    {
                        collision = true;
                        break;
                    }
                }
                if (!collision)
                    break;

                // Step to next index value and re-check for collisions.
                index = (index + 1) % COLORS.size();
            }
        }

        return COLORS.get(index);
    }

    private static int getColorIndex(String peptideName)
    {
        // Get hash code for peptide name, then XOR the bytes to make a smaller hash,
        // then modulo to our color array size.
        int hash = getCSharpHashCode(peptideName);
        return ((hash) ^ (hash >> 8) ^ (hash >> 16) ^ (hash >> 24)) % COLORS.size();
    }

    /**
     * Ported from https://referencesource.microsoft.com/#mscorlib/system/string.cs
     */
    private static int getCSharpHashCode(String peptideName)
    {
        int hash1 = 5381;
        int hash2 = hash1;

        int c;
        int index = 0;
        while (index < peptideName.length() && (c = peptideName.charAt(index++)) != 0)
        {
            hash1 = ((hash1 << 5) + hash1) ^ c;
            if (index == peptideName.length())
                break;
            c = peptideName.charAt(index++);
            hash2 = ((hash2 << 5) + hash2) ^ c;
        }

        return hash1 + (hash2 * 1566083941);
    }


    // These colors were generated using the SkylinePeptideColorGenerator utility.
    private static final List<Color> COLORS =
            Collections.unmodifiableList(Arrays.asList(
                    new Color(85, 106, 104),
                    new Color(212, 126, 0),
                    new Color(200, 0, 161),
                    new Color(0, 194, 255),
                    new Color(0, 200, 0),
                    new Color(116, 125, 0),
                    new Color(163, 166, 255),
                    new Color(159, 35, 33),
                    new Color(0, 121, 94),
                    new Color(142, 68, 147),
                    new Color(27, 172, 98),
                    new Color(205, 144, 76),
                    new Color(196, 110, 138),
                    new Color(0, 126, 175),
                    new Color(178, 184, 102),
                    new Color(237, 140, 255),
                    new Color(108, 0, 186),
                    new Color(33, 193, 188),
                    new Color(0, 92, 0),
                    new Color(175, 170, 221),
                    new Color(110, 68, 0),
                    new Color(0, 108, 188),
                    new Color(179, 0, 110),
                    new Color(162, 0, 174),
                    new Color(0, 216, 151),
                    new Color(0, 105, 122),
                    new Color(96, 205, 0),
                    new Color(0, 122, 0),
                    new Color(0, 193, 255),
                    new Color(0, 78, 189),
                    new Color(51, 81, 0),
                    new Color(117, 85, 56),
                    new Color(247, 135, 65),
                    new Color(251, 130, 236),
                    new Color(211, 181, 0),
                    new Color(255, 134, 157),
                    new Color(0, 198, 210),
                    new Color(190, 182, 169),
                    new Color(0, 97, 104),
                    new Color(255, 79, 255),
                    new Color(96, 73, 110),
                    new Color(225, 162, 227),
                    new Color(0, 99, 0),
                    new Color(135, 45, 0),
                    new Color(0, 93, 57),
                    new Color(110, 164, 1),
                    new Color(175, 0, 146),
                    new Color(112, 167, 131),
                    new Color(249, 109, 191),
                    new Color(49, 75, 135),
                    new Color(0, 207, 185),
                    new Color(128, 61, 0),
                    new Color(255, 114, 244),
                    new Color(114, 99, 0),
                    new Color(0, 182, 227),
                    new Color(120, 172, 236),
                    new Color(220, 163, 147),
                    new Color(0, 192, 75),
                    new Color(0, 119, 56),
                    new Color(125, 44, 55),
                    new Color(147, 23, 109),
                    new Color(66, 77, 23),
                    new Color(191, 166, 25),
                    new Color(168, 115, 148),
                    new Color(0, 128, 173),
                    new Color(126, 173, 177),
                    new Color(195, 89, 213),
                    new Color(159, 199, 0),
                    new Color(162, 36, 84),
                    new Color(134, 61, 172),
                    new Color(153, 84, 0),
                    new Color(77, 131, 163),
                    new Color(247, 149, 135),
                    new Color(231, 149, 0),
                    new Color(75, 198, 236),
                    new Color(137, 67, 0),
                    new Color(53, 95, 0),
                    new Color(0, 134, 214),
                    new Color(173, 181, 0),
                    new Color(201, 0, 195),
                    new Color(0, 148, 0),
                    new Color(174, 139, 223),
                    new Color(52, 200, 159),
                    new Color(0, 164, 190),
                    new Color(243, 152, 203),
                    new Color(0, 143, 134),
                    new Color(15, 140, 222),
                    new Color(17, 116, 30),
                    new Color(140, 135, 92),
                    new Color(112, 101, 226),
                    new Color(90, 73, 0),
                    new Color(217, 47, 212),
                    new Color(0, 170, 243),
                    new Color(0, 100, 123),
                    new Color(137, 198, 117),
                    new Color(228, 106, 89),
                    new Color(87, 68, 68),
                    new Color(161, 119, 0),
                    new Color(139, 134, 166),
                    new Color(103, 167, 255),
                    new Color(0, 165, 119),
                    new Color(36, 128, 120),
                    new Color(0, 139, 136),
                    new Color(200, 172, 129),
                    new Color(0, 128, 223),
                    new Color(225, 77, 136),
                    new Color(183, 94, 36),
                    new Color(170, 117, 237),
                    new Color(220, 27, 164),
                    new Color(0, 99, 75),
                    new Color(197, 94, 0),
                    new Color(99, 68, 135),
                    new Color(0, 127, 0),
                    new Color(131, 60, 94),
                    new Color(102, 62, 0),
                    new Color(0, 154, 180),
                    new Color(165, 187, 135),
                    new Color(0, 100, 162),
                    new Color(170, 129, 130),
                    new Color(66, 137, 0),
                    new Color(207, 174, 203),
                    new Color(57, 68, 166),
                    new Color(0, 204, 135),
                    new Color(173, 58, 219),
                    new Color(0, 118, 51),
                    new Color(138, 108, 0),
                    new Color(196, 78, 133),
                    new Color(8, 193, 255),
                    new Color(0, 82, 156),
                    new Color(168, 99, 92),
                    new Color(255, 145, 0),
                    new Color(92, 108, 0),
                    new Color(240, 40, 205),
                    new Color(226, 160, 175),
                    new Color(138, 0, 146),
                    new Color(124, 132, 193),
                    new Color(0, 145, 110),
                    new Color(186, 76, 0),
                    new Color(0, 149, 0),
                    new Color(0, 104, 0),
                    new Color(0, 85, 117),
                    new Color(255, 121, 255),
                    new Color(0, 196, 255),
                    new Color(112, 131, 11),
                    new Color(41, 169, 187),
                    new Color(132, 116, 217),
                    new Color(255, 109, 212),
                    new Color(87, 122, 96),
                    new Color(0, 203, 90),
                    new Color(167, 0, 78),
                    new Color(101, 144, 80),
                    new Color(152, 66, 0),
                    new Color(233, 172, 0),
                    new Color(204, 89, 177),
                    new Color(60, 196, 80),
                    new Color(175, 145, 69),
                    new Color(201, 97, 101),
                    new Color(144, 189, 218),
                    new Color(145, 104, 0),
                    new Color(0, 98, 95),
                    new Color(48, 73, 97),
                    new Color(0, 166, 87),
                    new Color(0, 89, 0),
                    new Color(179, 174, 0),
                    new Color(255, 139, 195),
                    new Color(35, 105, 0),
                    new Color(240, 153, 160),
                    new Color(0, 192, 167),
                    new Color(0, 212, 251),
                    new Color(0, 166, 145),
                    new Color(0, 109, 138),
                    new Color(209, 157, 0),
                    new Color(41, 95, 40),
                    new Color(150, 65, 128),
                    new Color(166, 123, 189),
                    new Color(161, 164, 173),
                    new Color(86, 81, 0),
                    new Color(0, 109, 171),
                    new Color(171, 124, 83),
                    new Color(0, 171, 179),
                    new Color(212, 78, 98),
                    new Color(0, 82, 88),
                    new Color(0, 116, 0),
                    new Color(169, 107, 0),
                    new Color(160, 73, 0),
                    new Color(132, 39, 0),
                    new Color(215, 150, 250),
                    new Color(224, 135, 225),
                    new Color(172, 112, 0),
                    new Color(0, 132, 166),
                    new Color(0, 138, 205),
                    new Color(255, 114, 255),
                    new Color(255, 125, 230),
                    new Color(0, 182, 0),
                    new Color(0, 153, 53),
                    new Color(71, 162, 55),
                    new Color(253, 144, 0),
                    new Color(242, 0, 222),
                    new Color(54, 116, 189),
                    new Color(177, 0, 156),
                    new Color(254, 141, 222),
                    new Color(138, 34, 47),
                    new Color(171, 0, 115),
                    new Color(131, 89, 105),
                    new Color(0, 121, 106),
                    new Color(134, 129, 114),
                    new Color(0, 139, 106),
                    new Color(148, 31, 0),
                    new Color(204, 130, 103),
                    new Color(160, 185, 166),
                    new Color(179, 91, 0),
                    new Color(116, 58, 111),
                    new Color(161, 170, 251),
                    new Color(0, 133, 146),
                    new Color(120, 195, 172),
                    new Color(0, 104, 63),
                    new Color(255, 89, 255),
                    new Color(0, 103, 0),
                    new Color(109, 175, 0),
                    new Color(150, 142, 0),
                    new Color(145, 28, 128),
                    new Color(145, 45, 94),
                    new Color(0, 105, 0),
                    new Color(159, 195, 86),
                    new Color(215, 155, 195),
                    new Color(221, 135, 52),
                    new Color(0, 140, 188),
                    new Color(0, 209, 255),
                    new Color(112, 57, 42),
                    new Color(0, 96, 183),
                    new Color(141, 124, 0),
                    new Color(231, 117, 149),
                    new Color(0, 215, 171),
                    new Color(173, 79, 175),
                    new Color(0, 154, 98),
                    new Color(0, 131, 193),
                    new Color(0, 124, 0),
                    new Color(0, 161, 253),
                    new Color(220, 49, 183),
                    new Color(128, 78, 0),
                    new Color(37, 114, 227),
                    new Color(114, 185, 255),
                    new Color(184, 152, 255),
                    new Color(186, 7, 175),
                    new Color(0, 213, 205),
                    new Color(53, 166, 0),
                    new Color(138, 0, 171),
                    new Color(252, 161, 117),
                    new Color(243, 132, 255),
                    new Color(85, 92, 133),
                    new Color(59, 74, 58),
                    new Color(203, 48, 147),
                    new Color(179, 146, 0),
                    new Color(15, 86, 70),
                    new Color(153, 0, 95),
                    new Color(176, 68, 15)
            ));
}
