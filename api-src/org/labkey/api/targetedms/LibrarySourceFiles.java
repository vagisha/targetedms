package org.labkey.api.targetedms;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LibrarySourceFiles
{
    private final ISpectrumLibrary _library;
    private List<LibSourceFile> _sourceFiles;

    public LibrarySourceFiles(@NotNull ISpectrumLibrary library)
    {
        _library = library;
        _sourceFiles = new ArrayList<>();
    }

    public ISpectrumLibrary getLibrary()
    {
        return _library;
    }

    public void addSource(LibSourceFile source)
    {
        _sourceFiles.add(source);
    }

    public List<LibSourceFile> getSourceFiles()
    {
        return Collections.unmodifiableList(_sourceFiles);
    }
}
