package org.labkey.targetedms;

import org.jetbrains.annotations.NonNls;
import org.labkey.api.util.SkipMothershipLogging;

/**
 * Created by Josh on 10/19/2017.
 */
public class PanoramaBadDataException extends RuntimeException implements SkipMothershipLogging
{
    public PanoramaBadDataException(@NonNls String message)
    {
        super(message);
    }
}
