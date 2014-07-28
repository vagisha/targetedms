package org.labkey.targetedms.chart;

import org.labkey.targetedms.parser.PeptideSettings;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.query.IsotopeLabelManager;
import org.labkey.targetedms.query.PrecursorManager;

import java.util.List;

/**
 * User: vsharma
 * Date: 7/26/2014
 * Time: 10:56 PM
 */
public class PrecursorColorIndexer
{
    private int _lightLabelId = Integer.MAX_VALUE;
    private int _minCharge = Integer.MAX_VALUE;
    private int _isotopeLabelCount;

    public PrecursorColorIndexer(int runId)
    {
        this(runId, 0);
    }

    public PrecursorColorIndexer(int runId, int peptideId)
    {
        List<PeptideSettings.IsotopeLabel> labels = IsotopeLabelManager.getIsotopeLabels(runId);
        for(PeptideSettings.IsotopeLabel label: labels)
        {
            _lightLabelId = Math.min(_lightLabelId, label.getId());
        }
        _isotopeLabelCount = labels.size();

        if(peptideId > 0)
        {
            List<Precursor> precursors = PrecursorManager.getPrecursorsForPeptide(peptideId);
            for (Precursor precursor : precursors)
            {
                _minCharge = Math.min(_minCharge, precursor.getCharge());
            }
        }
    }

    public void setMinCharge(int charge)
    {
        _minCharge = charge;
    }

    public int getColorIndex(int precursorId)
    {
        // CONSIDER caching the colors, as they will be the same for all the replicates
        Precursor precursor = PrecursorManager.get(precursorId);
        return getColorIndex(precursor.getIsotopeLabelId(), precursor.getCharge());
    }

    public int getColorIndex(int isotopeLabelId, int charge)
    {
        return (charge - _minCharge) * _isotopeLabelCount + (isotopeLabelId - _lightLabelId);

    }
}
