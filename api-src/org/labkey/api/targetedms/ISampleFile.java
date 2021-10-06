package org.labkey.api.targetedms;

import java.util.Date;

public interface ISampleFile
{
    String getFileName();
    Long getInstrumentId();
    String getFilePath();
    default String getSampleName() { return null; }
    default Date getAcquiredTime() { return null; }
    default Date getModifiedTime() { return null; }
    default String getSkylineId() { return null; }
    default Double getTicArea() { return null; }
    default String getInstrumentSerialNumber() { return null; }
    default String getSampleId() { return null; }
    default Double getExplicitGlobalStandardArea() { return null; }
    default String getIonMobilityType() { return null; }
}
