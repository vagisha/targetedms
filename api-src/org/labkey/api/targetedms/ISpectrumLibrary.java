package org.labkey.api.targetedms;

public interface ISpectrumLibrary
{
    long getId();
    long getRunId();
    String getName();
    String getFileNameHint();
    String getSkylineLibraryId();
    String getRevision();
    String getLibraryType();
}
