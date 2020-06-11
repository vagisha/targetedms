package org.labkey.api.targetedms;

import java.util.Collections;
import java.util.List;

public class BlibSourceFiles
{
    private List<String> spectrumSourceFiles;
    private List<String> idFiles;

    public BlibSourceFiles(List<String> spectrumSourceFiles, List<String> idFiles)
    {
        this.spectrumSourceFiles = spectrumSourceFiles;
        this.idFiles = idFiles;
    }

    public List<String> getSpectrumSourceFiles()
    {
        return Collections.unmodifiableList(spectrumSourceFiles);
    }

    public List<String> getIdFiles()
    {
        return Collections.unmodifiableList(idFiles);
    }
}
