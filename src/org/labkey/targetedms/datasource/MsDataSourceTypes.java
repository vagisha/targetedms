package org.labkey.targetedms.datasource;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.FileUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MsDataSourceTypes
{
    static final MsDataSource CONVERTED_DATA_SOURCE = new MsDataFileSource(Arrays.asList(".mzxml", ".mzml", ".mz5", ".mzdata"));
    static final MsDataSource THERMO = new MsDataFileSource("thermo", ".raw");
    static final MsDataSource SCIEX = new MsDataFileSource("sciex", Arrays.asList(".wiff", ".wiff2", ".wiff.scan", ".wiff2.scan"))
    {
        @Override
        public boolean isInstrumentSource(String instrument)
        {
            return super.isInstrumentSource(instrument) || (instrument != null && instrument.toLowerCase().contains("applied biosystems"));
        }
    };
    static final MsDataSource SHIMADZU = new MsDataFileSource("shimadzu", ".lcd");
    static final MsDataDirSource WATERS = new MsDataDirSource("waters", ".raw")
    {
        @Override
        public boolean isExpectedDirContent(Path p)
        {
            return !Files.isDirectory(p) && FileUtil.getFileName(p).matches("^_FUNC.*\\.DAT$");
        }

        @Override
        public SimpleFilter.FilterClause getDirContentsFilterClause()
        {
            return new CompareType.ContainsClause(FieldKey.fromParts("Name"), "")
            {
                @Override
                public SQLFragment toSQLFragment(Map<FieldKey, ? extends ColumnInfo> columnMap, SqlDialect dialect)
                {
                    ColumnInfo colInfo = columnMap != null ? columnMap.get(getFieldKey()) : null;
                    String alias = colInfo != null ? colInfo.getAlias() : getFieldKey().getName();
                    return new SQLFragment(dialect.getColumnSelectName(alias))
                            .append(" ").append(dialect.getCaseInsensitiveLikeOperator()).append(" ? ")
                            .append(sqlEscape())
                            .add(escapeLikePattern("_") + "FUNC%.DAT");
                }
            };
        }
    };

    static final String AGILENT_ACQ_DATA = "AcqData";
    static final MsDataDirSource AGILENT = new MsDataDirSource("agilent", ".d")
    {
        @Override
        public boolean isExpectedDirContent(Path p)
        {
            return Files.isDirectory(p) && FileUtil.getFileName(p).equals(AGILENT_ACQ_DATA);
        }

        @Override
        public SimpleFilter.FilterClause getDirContentsFilterClause()
        {
            return new CompareType.CompareClause(FieldKey.fromParts("Name"), CompareType.EQUAL, AGILENT_ACQ_DATA);
        }
    };

    static final String BRUKER_ANALYSIS_BAF = "analysis.baf";
    static final String BRUKER_ANALYSIS_TDF = "analysis.tdf";
    static final MsDataDirSource BRUKER = new MsDataDirSource("bruker", ".d")
    {
        @Override
        public boolean isExpectedDirContent(Path p)
        {
            return !Files.isDirectory(p) && (FileUtil.getFileName(p).equals(BRUKER_ANALYSIS_BAF) || FileUtil.getFileName(p).equals(BRUKER_ANALYSIS_TDF));
        }

        @Override
        public SimpleFilter.FilterClause getDirContentsFilterClause()
        {
            return new SimpleFilter.OrClause(
                    new CompareType.CompareClause(FieldKey.fromParts("Name"), CompareType.EQUAL, BRUKER_ANALYSIS_BAF),
                    new CompareType.CompareClause(FieldKey.fromParts("Name"), CompareType.EQUAL, BRUKER_ANALYSIS_TDF)
            );
        }
    };

    static final MsDataSource  UNKNOWN = new MsDataSource("unknown", Collections.emptyList())
    {
        @Override
        boolean isValidPath(@NotNull Path path)
        {
            return false;
        }

        @Override
        boolean isValidData(@NotNull ExpData data, ExperimentService expSvc)
        {
            return false;
        }

        @Override
        boolean isFileSource()
        {
            return false;
        }
    };

    private static MsDataSource[] sourceTypes = new MsDataSource[]{
            THERMO, SCIEX, SHIMADZU, // File-based
            WATERS, AGILENT, BRUKER, // Directory-based
            CONVERTED_DATA_SOURCE}; // mzML, mzXML etc.


    private static Map<String, List<MsDataSource>> EXTENSION_SOURCE_MAP = new HashMap<>();
    static
    {
        for (MsDataSource s : sourceTypes)
        {
            for (String ext : s.getExtensions())
            {
                List<MsDataSource> sources = EXTENSION_SOURCE_MAP.computeIfAbsent(ext, k -> new ArrayList<>());
                sources.add(s);
            }
        }
    }

    static List<MsDataSource> getSourceForName(String name)
    {
        // Can return more than one data source type. For example, Bruker and Waters both have .d extension;
        // Thermo and Waters both have .raw extension
        var sources = EXTENSION_SOURCE_MAP.get(extension(name));
        return sources != null ? sources : Collections.emptyList();
    }

    static MsDataSource getSourceForInstrument(String vendorOrModel)
    {
        return Arrays.stream(sourceTypes).filter(s -> s.isInstrumentSource(vendorOrModel)).findFirst().orElse(null);
    }

    private static String extension(String name)
    {
        if(name != null)
        {
            int idx = name.lastIndexOf('.');
            if(idx != -1)
            {
                return name.substring(idx).toLowerCase();
            }
        }
        return "";
    }
}
