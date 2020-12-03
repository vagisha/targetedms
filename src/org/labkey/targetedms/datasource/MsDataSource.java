package org.labkey.targetedms.datasource;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.FileUtil;

import java.nio.file.Path;
import java.util.List;

public abstract class MsDataSource
{
    private final List<String> _extensions;
    private final String _instrumentVendor;

    private static final String EXT_ZIP = ".zip";

    MsDataSource(@NotNull String instrumentVendor, @NotNull List<String> extensions)
    {
        _instrumentVendor = instrumentVendor;
        _extensions = extensions;
    }

    public List<String> getExtensions()
    {
        return _extensions;
    }

    public boolean isValidName(String name)
    {
        if(name != null)
        {
            if(isZip(name))
            {
                name = FileUtil.getBaseName(name);
            }
            String nameLc = name.toLowerCase();
            for(String ext: _extensions)
            {
                if(nameLc.endsWith(ext))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /* instrument can be the instrument model (saved in targetedms.instrument) or vendor name from the PSI-MS instrument list */
    public boolean isInstrumentSource(String instrument)
    {
        return instrument != null && instrument.toLowerCase().contains(_instrumentVendor);
    }

    public String toString()
    {
        return name();
    }

    public String name()
    {
        return _instrumentVendor;
    }

    public boolean isValidNameAndPath(@NotNull Path path)
    {
        return isValidName(FileUtil.getFileName(path)) && isValidPath(path);
    }

    public boolean isValidNameAndData(@NotNull ExpData expData, @NotNull ExperimentService expSvc)
    {
        return isValidName(expData.getName()) && isValidData(expData, expSvc);
    }

    abstract boolean isValidPath(@NotNull Path path);
    abstract boolean isValidData(@NotNull ExpData data, ExperimentService expSvc);
    abstract boolean isFileSource();

    static SimpleFilter getExpDataFilter(Container container, String pathPrefix)
    {
        SimpleFilter filter = SimpleFilter.createContainerFilter(container);
        if (!pathPrefix.endsWith("/"))
        {
            pathPrefix = pathPrefix + "/";
        }
        // StartsWithClause is not public.  And CompareClause with CompareType.STARTS_WITH is not the same thing as StartWithClause
        // Use addCondition() instead.
        filter.addCondition(FieldKey.fromParts("datafileurl"), pathPrefix, CompareType.STARTS_WITH);
        return filter;
    }

    static boolean isZip(@NotNull String fileName)
    {
        return fileName.toLowerCase().endsWith(EXT_ZIP);
    }
}
