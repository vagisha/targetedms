package org.labkey.targetedms.model;

import java.util.Comparator;

/**
 * User: vsharma
 * Date: 9/5/2014
 * Time: 4:00 PM
 */
public class PrecursorComparator implements Comparator<PrecursorComparator.Comparable>
{
    @Override
    public int compare(Comparable o1, Comparable o2)
    {
        int result = Integer.valueOf(o1.getCharge()).compareTo(o2.getCharge());
        if(result != 0)
        {
            return result;
        }

        return Integer.valueOf(o1.getIsotopeLabelId()).compareTo(o2.getIsotopeLabelId());
    }

    public static interface Comparable
    {
        public int getCharge();
        public int getIsotopeLabelId();
    }
}
