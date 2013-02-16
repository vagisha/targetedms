package org.labkey.targetedms.query;

import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.parser.Instrument;

/**
 * User: vsharma
 * Date: 1/9/13
 * Time: 11:07 PM
 */
public class InstrumentManager
{
    public static Instrument getInstrument(int instrumentId)
    {
        return new TableSelector(TargetedMSManager.getTableInfoInstrument(),
                                 new SimpleFilter(FieldKey.fromParts("Id"), instrumentId),
                                 null)
                                 .getObject(Instrument.class);
    }
}
