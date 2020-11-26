package org.labkey.targetedms.datasource;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class MsMultiDataSource extends MsDataSource
{
    private List<MsDataDirSource> _dirSources;
    private List<MsDataFileSource> _fileSources;

    MsMultiDataSource()
    {
        super("unknown", Collections.emptyList());
        _fileSources = new ArrayList<>();
        _dirSources = new ArrayList<>();
    }

    public List<MsDataDirSource> getDirSources()
    {
        return _dirSources;
    }

    public List<MsDataFileSource> getFileSources()
    {
        return _fileSources;
    }

    void addSource(MsDataSource source)
    {
        if(source instanceof MsDataFileSource)
        {
            _fileSources.add((MsDataFileSource) source);
        }
        else if(source instanceof  MsDataDirSource)
        {
            _dirSources.add((MsDataDirSource) source);
        }
    }

    @Override
    public boolean isFileSource()
    {
        return _dirSources.isEmpty();
    }

    @Override
    public boolean isValidNameAndPath(@NotNull Path path)
    {
        return _dirSources.stream().anyMatch(s -> s.isValidNameAndPath(path)) ||
                _fileSources.stream().anyMatch(s -> s.isValidNameAndPath(path));
    }

    @Override
    boolean isValidPath(@NotNull Path path)
    {
        return _dirSources.stream().anyMatch(s -> s.isValidPath(path)) ||
                _fileSources.stream().anyMatch(s -> s.isValidPath(path));
    }

    @Override
    public boolean isValidNameAndData(@NotNull ExpData data, @NotNull ExperimentService expSvc)
    {
        return _dirSources.stream().anyMatch(s -> s.isValidNameAndData(data, expSvc)) ||
                _fileSources.stream().anyMatch(s -> s.isValidNameAndData(data, expSvc));
    }

    @Override
    boolean isValidData(@NotNull ExpData data, ExperimentService expSvc)
    {
        return _dirSources.stream().anyMatch(s -> s.isValidData(data, expSvc)) ||
                _fileSources.stream().anyMatch(s -> s.isValidData(data, expSvc));
    }

    @Override
    public String name()
    {
        List<String> names = new ArrayList<>();
        _dirSources.forEach(s -> names.add(s.name()));
        _fileSources.forEach(s -> names.add(s.name()));
        return StringUtils.join(names, ",");
    }

    @Override
    public List<String> getExtensions()
    {
        List<String> allExtensions = new ArrayList<>();
        _dirSources.forEach(s -> allExtensions.addAll(s.getExtensions()));
        _fileSources.forEach(s -> allExtensions.addAll(s.getExtensions()));
        return allExtensions;
    }
}
