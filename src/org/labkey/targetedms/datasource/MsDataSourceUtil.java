package org.labkey.targetedms.datasource;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.files.FileContentService;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.targetedms.ISampleFile;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.JunitUtil;
import org.labkey.api.util.TestContext;
import org.labkey.api.util.UnexpectedException;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSModule;
import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.parser.Instrument;
import org.labkey.targetedms.parser.SampleFile;
import org.labkey.targetedms.query.InstrumentManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.labkey.targetedms.datasource.MsDataSource.isZip;

public class MsDataSourceUtil
{
    private static final MsDataSourceUtil instance = new MsDataSourceUtil();

    private MsDataSourceUtil() {}

    public static MsDataSourceUtil getInstance()
    {
        return instance;
    }

    public RawDataInfo getDownloadInfo(@NotNull SampleFile sampleFile, @NotNull Container container)
    {
        ExperimentService expSvc = ExperimentService.get();
        if(expSvc == null)
        {
            return null;
        }

        // We will look for raw data files only in @files/RawFiles
        Path rawFilesDir = getRawFilesDir(container);
        if(rawFilesDir == null)
        {
            return null;
        }

        ExpData expData = getDataForSampleFile(sampleFile, container, rawFilesDir, expSvc, false);

        Long size = null;
        if(expData != null)
        {
            Path dataPath = expData.getFilePath();
            if(dataPath != null && Files.exists(dataPath))
            {
                try
                {
                    if(!Files.isDirectory(dataPath))
                    {
                        size = Files.size(dataPath);
                        return new RawDataInfo(expData, size, true);
                    }
                    else
                    {
                        if(!FileUtil.hasCloudScheme(dataPath))
                        {
                            // Displayed size will be bigger than the size of the downloaded zip
                            size = FileUtils.sizeOfDirectory(dataPath.toFile());
                        }
                        return new RawDataInfo(expData, size, false);
                    }
                }
                catch (IOException e)
                {
                    throw UnexpectedException.wrap(e, "Error getting size of " + dataPath);
                }
            }
        }
        return null;
    }

    private Path getRawFilesDir(Container c)
    {
        FileContentService fcs = FileContentService.get();
        if(fcs != null)
        {
            Path fileRoot = fcs.getFileRootPath(c, FileContentService.ContentType.files);
            if (fileRoot != null)
            {
                return fileRoot.resolve(TargetedMSService.RAW_FILES_DIR);
            }
        }
        return null;
    }

    private ExpData getDataForSampleFile(SampleFile sampleFile, Container container, Path rawFilesDir, ExperimentService expSvc, boolean validateZip)
    {
        List<? extends ExpData> expDatas = getExpData(sampleFile.getFileName(), container, rawFilesDir, expSvc);

        if(expDatas.size() == 0)
        {
            return null;
        }

        MsDataSource sourceType = null;
        List<ExpData> notZipDatas = new ArrayList<>();
        for(ExpData data: expDatas)
        {
            // Prefer to return a zip source if we found one
            if(isZip(data.getName()))
            {
                if(validateZip && sourceType == null)
                {
                    // We will need the source type to validate the zip. Get the source type from the file extension
                    // and (if required) the instrument associated with the sample file
                    sourceType = getMsDataSource(sampleFile);
                }
                if(!validateZip || (data.getFilePath() != null && sourceType.isValidPath(data.getFilePath())))
                {
                    return data;
                }
            }
            else
            {
                notZipDatas.add(data);
            }
        }

        if(sourceType == null)
        {
            sourceType = getMsDataSource(sampleFile);
        }
        for(ExpData data: notZipDatas)
        {
            if(sourceType.isValidData(data, expSvc))
            {
                return data;
            }
        }

        return null;
    }

    @NotNull
    private List<? extends ExpData> getExpData(String fileName, Container container, Path pathPrefix, ExperimentService expSvc)
    {
        String pathPrefixString = FileUtil.pathToString(pathPrefix); // Encoded URI string

        TableInfo expDataTInfo = expSvc.getTinfoData();
        SimpleFilter filter = MsDataSource.getExpDataFilter(container, pathPrefixString);
        filter.addCondition(FieldKey.fromParts("name"), fileName, CompareType.STARTS_WITH);

        // Get the rowId and name of matching rows.
        Map<Integer, String> expDatas = new TableSelector(expDataTInfo,
                expDataTInfo.getColumns("RowId", "Name"), filter, null).getValueMap();

        List<Integer> expDataIds = new ArrayList<>();
        // Look for the file and file.zip (e.g. sample_1.raw and sample_1.raw.zip)
        expDatas.entrySet().stream()
                           .filter(e -> fileName.equals(e.getValue()) || (isZip(e.getValue()) && fileName.equals(FileUtil.getBaseName(e.getValue()))))
                           .forEach(e -> expDataIds.add(e.getKey()));

        return expSvc.getExpDatas(expDataIds);
    }

    @NotNull
    private MsDataSource getMsDataSource(ISampleFile sampleFile)
    {
        List<MsDataSource> sourceTypes = MsDataSourceTypes.getSourceForName(sampleFile.getFileName());
        if(sourceTypes.size() == 1)
        {
            return sourceTypes.get(0);
        }
        else if(sourceTypes.size() > 1)
        {
            // We can get more than one source type by filename extension lookup. e.g. .raw extension is used both by Thermo and Waters
            // Try to resolve by looking up the instrument on which the data was acquired.
            if(sampleFile.getInstrumentId() != null)
            {
                Instrument instrument = InstrumentManager.getInstrument(sampleFile.getInstrumentId());
                MsDataSource source = getSourceForInstrument(instrument);
                if(source != null)
                {
                    return source;
                }
            }
            // We cannot resolve to a single source even after instrument lookup
            if(sourceTypes.size() > 1)
            {
                MsMultiDataSource multiSource = new MsMultiDataSource();
                for(MsDataSource source: sourceTypes)
                {
                    multiSource.addSource(source);
                }
                return multiSource;
            }
        }
        return MsDataSourceTypes.UNKNOWN;
    }

    /**
     *
     * @param sampleFiles list of sample files for which we should check if data exists
     * @param container container where we should look for data
     * @return list of sample files for which data was found
     */
    @NotNull
    public List<? extends ISampleFile> getSampleFilesWithData(@NotNull List<? extends ISampleFile> sampleFiles, @NotNull Container container)
    {
        List<ISampleFile> dataFound = new ArrayList<>();

        Path rawFilesDir = getRawFilesDir(container);
        if (rawFilesDir == null || !Files.exists(rawFilesDir))
        {
            return dataFound;
        }

        ExperimentService expSvc = ExperimentService.get();
        if(expSvc == null)
        {
            return dataFound;
        }

        for(ISampleFile sampleFile: sampleFiles)
        {
            MsDataSource dataSource = getMsDataSource(sampleFile);

            String fileName = sampleFile.getFileName();
            if(hasData(fileName, dataSource, container, rawFilesDir, expSvc))
            {
                dataFound.add(sampleFile);
            }
        }
        return dataFound;
    }

    private boolean hasData(String fileName, MsDataSource dataSource, Container container, Path rawFilesDir, ExperimentService expSvc)
    {
        List<? extends ExpData> expDatas = getExpData(fileName, container, rawFilesDir, expSvc);

        if(expDatas.size() > 0)
        {
            for (ExpData data : expDatas)
            {
                if (dataSource.isValidNameAndData(data, expSvc))
                {
                   return true;
                }
            }
        }
        else
        {
            FileContentService fcs = FileContentService.get();
            if (fcs != null && !fcs.isCloudRoot(container))
            {
                // No matches found in exp.data.  Look on the filesystem
                return dataExists(fileName, dataSource, rawFilesDir);
            }
        }
        return false;
    }

    private boolean dataExists(String fileName, MsDataSource sourceType, Path rawFilesDir)
    {
        try
        {
            return Files.walk(rawFilesDir).anyMatch(p -> isSourceMatch(p, fileName, sourceType));
        }
        catch (IOException e)
        {
            throw UnexpectedException.wrap(e,"Error checking for data in sub-directories of " + rawFilesDir);
        }
    }

    private boolean isSourceMatch(Path path, String fileName, MsDataSource sourceType)
    {
        String pathFileName = FileUtil.getFileName(path);
        if(fileName.equals(pathFileName) || (isZip(pathFileName) && fileName.equals(FileUtil.getBaseName(pathFileName))))
        {
           return sourceType.isValidPath(path);
        }
        return false;
    }

    private MsDataSource getSourceForInstrument(Instrument instrument)
    {
        // Try to find an instrument from the PSI-MS instrument list that matches the instrument model.
        // We may not find a match because
        // 1. This is a new instrument model and our instrument list is not current
        // 2. The instrument model may be something general like "Waters instrument model".
        //    This is generally seen in Skyline documents for data from instruments other than Thermo and SCIEX.
        PsiInstruments.PsiInstrument psiInstrument = PsiInstruments.getInstrument(instrument.getModel());
        String vendorOrModel = psiInstrument != null ? psiInstrument.getVendor() : instrument.getModel();
        return MsDataSourceTypes.getSourceForInstrument(vendorOrModel);
    }

    public static class RawDataInfo
    {
        private final ExpData _expData;
        private final Long _size;
        private final boolean _isFile;

        public RawDataInfo(ExpData expData, Long size, boolean isFile)
        {
            _expData = expData;
            _size = size;
            _isFile = isFile;
        }

        public ExpData getExpData()
        {
            return _expData;
        }

        public Long getSize()
        {
            return _size;
        }

        public boolean isFile()
        {
            return _isFile;
        }
    }

    public static class TestCase extends Assert
    {
        private static final String FOLDER_NAME = "TargetedMSDataSourceTest";
        private static final String SKY_FILE_NAME = "msdatasourcetest_9ab4da773526.sky.zip";
        private static final String TEST_DATA_FOLDER = "TargetedMS/Raw Data Test";
        private static User _user;
        private static Container _container;
        private static TargetedMSRun _run;
        private static Instrument _thermo;
        private static Instrument _sciex;
        private static Instrument _waters;
        private static Instrument _agilent;
        private static Instrument _bruker;
        private static Instrument _unknown;

        private static MsDataSourceUtil _util;

        private static final String[] thermoFiles = new String[] {"5Aug2017-FU2-PDK4-PRM2-2_1-01.raw", "5Aug2017-FU2-PDK4-PRM2-4_1-01.raw"};
        private static final String[] sciexFiles = new String [] {"20140807_nsSRM_01.wiff", "20140807_nsSRM_02.wiff"};
        private static final String[] watersData = new String[] {"20200929_CalMatrix_00_A.raw", "20200929_CalMatrix_00_A_flatzip.raw", "20200929_CalMatrix_00_A_nestedzip.raw"};
        private static final String[] brukerData = new String[] {"DK0034-G10_1-D,2_01_1036.d", "DK0034-G10_1-D,2_01_1036_flatzip.d", "DK0034-G10_1-D,2_01_1036_nestedzip.d"};
        private static final String[] agilentData = new String[] {"pTE219_0 hr_R2.d", "pTE219_0 hr_R2_flatzip.d", "pTE219_0 hr_R2_nestedzip.d"};

        @BeforeClass
        public static void setup() throws ExperimentException
        {
            cleanDatabase();

            _user = TestContext.get().getUser();
            _container = ContainerManager.ensureContainer(JunitUtil.getTestContainer(), FOLDER_NAME);

            // Create an entry in the targetedms.runs table
            _run = createRun();

            // Create instruments
            _thermo = createInstrument("TSQ Altis");
            _sciex = createInstrument("Triple Quad 6500");
            _waters = createInstrument("Waters instrument model");
            _bruker = createInstrument("Bruker instrument model");
            _agilent = createInstrument("Agilent instrument model");
            _unknown = createInstrument("UWPR instrument model");

            _util = new MsDataSourceUtil();
        }

        private static TargetedMSRun createRun()
        {
            TargetedMSRun run = new TargetedMSRun();
            run.setContainer(_container);
            run.setFileName(SKY_FILE_NAME);
            Table.insert(_user, TargetedMSManager.getTableInfoRuns(), run);
            assertNotEquals("Id for saved run should not be 0", 0, run.getId());
            return run;
        }

        private static Instrument createInstrument(String model)
        {
            Instrument instrument = new Instrument();
            instrument.setRunId(_run.getId());
            instrument.setModel(model);
            Table.insert(_user, TargetedMSManager.getTableInfoInstrument(), instrument);
            assertNotEquals("Id for saved instrument should not be 0", 0, instrument.getId());
            return instrument;
        }

        @Test
        public void testDataExists() throws IOException
        {
            Path rawDataDir = JunitUtil.getSampleData(ModuleLoader.getInstance().getModule(TargetedMSModule.class), TEST_DATA_FOLDER).toPath();

            testDataExists(thermoFiles, MsDataSourceTypes.THERMO, rawDataDir);
            testDataExists(sciexFiles, MsDataSourceTypes.SCIEX, rawDataDir);
            testDataExists(watersData, MsDataSourceTypes.WATERS, rawDataDir);
            testDataExists(brukerData, MsDataSourceTypes.BRUKER, rawDataDir);
            testDataExists(agilentData, MsDataSourceTypes.AGILENT, rawDataDir);

            testDataNotExists(agilentData, MsDataSourceTypes.BRUKER, rawDataDir); // Agilent data should not validate for Bruker
            testDataNotExists(agilentData, MsDataSourceTypes.WATERS, rawDataDir); // Agilent data should not validate for Waters
            testDataNotExists(watersData, MsDataSourceTypes.THERMO, rawDataDir);  // Waters data should not validate for Thermo

            // The test below fail because MsDataSourceUtil.dataExists() does not validate that the filename matches
            // the given datasource.  It only validates contents of directory based sources, and THERMO is a file-based source.
            // testDataNotExists(sciexFiles, THERMO, rawDataDir);
        }

        private void testDataExists(String[] files, MsDataSource sourceType, Path dataDir)
        {
            String message = "Expected " + (sourceType.isFileSource() ? "file or zip for " : "directory or zip for ") + sourceType.name() + ". File: ";
            for(String file: files)
            {
                assertTrue(message + file, _util.dataExists(file, sourceType, dataDir));
            }
        }

        private void testDataNotExists(String[] files, MsDataSource sourceType, Path dataDir)
        {
            String message = "Unxpected " + (sourceType.isFileSource() ? "file or zip for " : "directory or zip for ") + sourceType.name() + ". File: ";
            for(String file: files)
            {
                assertFalse(message + file, _util.dataExists(file, sourceType, dataDir));
            }
        }

        @Test
        public void testGetDataForSampleFile() throws IOException
        {
            Path rawDataDir = JunitUtil.getSampleData(ModuleLoader.getInstance().getModule(TargetedMSModule.class), TEST_DATA_FOLDER).toPath();
            ExperimentService expSvc = ExperimentService.get();

            testGetDataForFileBasedSampleFiles(thermoFiles, _thermo, MsDataSourceTypes.THERMO.name(), rawDataDir, expSvc);
            testGetDataForFileBasedSampleFiles(sciexFiles, _sciex, MsDataSourceTypes.SCIEX.name(), rawDataDir, expSvc);
            testGetDataForWatersSampleFiles(rawDataDir, expSvc);
            testGetDataForAgilentSampleFiles(rawDataDir, expSvc);
            testGetDataForBrukerSampleFiles(rawDataDir, expSvc);
        }

        private void testGetDataForFileBasedSampleFiles(String[] files, Instrument instrument, String instrumentDataDirName,
                                                        Path rawDataDir, ExperimentService expSvc)
        {
            for(String fileName: files)
            {
                // Rows have not been created in exp.data. Should not find any matching rows.
                testNoDataForSampleFile(fileName, instrument, rawDataDir, expSvc);
            }
            // Create rows in exp.data
            for(String fileName: files)
            {
                addData(fileName, rawDataDir, instrumentDataDirName);
            }
            for(String fileName: files)
            {
                // We should find matching rows in exp.data
                testGetDataForSampleFile(fileName, instrument, rawDataDir, expSvc);
            }
        }

        private void testGetDataForWatersSampleFiles(Path rawDataDir, ExperimentService expSvc)
        {
            testGetDataForDirBasedSampleFiles(
                    "20200929_CalMatrix_00_A.raw", // For testing VALID unzipped directory
                    "20200929_CalMatrix_00_A_invalid.raw", // For testing INVALID unzipped directory
                    "20200929_CalMatrix_00_A_flatzip.raw", // For testing VALID ZIP
                    "20200929_CalMatrix_00_A_invalidzip.raw", // For testing INVALID ZIP
                    "_FUNC001.DAT", // Required content in directory
                    _waters, MsDataSourceTypes.WATERS.name(),
                    rawDataDir, expSvc);
        }

        private void testGetDataForAgilentSampleFiles(Path rawDataDir, ExperimentService expSvc)
        {
            testGetDataForDirBasedSampleFiles(
                    "pTE219_0 hr_R2.d", // For testing VALID unzipped directory
                    "pTE219_0 hr_R2_invalid.d", // For testing INVALID unzipped directory
                    "pTE219_0 hr_R2_flatzip.d", // For testing VALID ZIP
                    "pTE219_0 hr_R2_invalidzip.d", // For testing INVALID ZIP
                    MsDataSourceTypes.AGILENT_ACQ_DATA, // Required content in directory
                    _agilent, MsDataSourceTypes.AGILENT.name(),
                    rawDataDir, expSvc);
        }

        private void testGetDataForBrukerSampleFiles(Path rawDataDir, ExperimentService expSvc)
        {
            testGetDataForDirBasedSampleFiles(
                    "DK0034-G10_1-D,2_01_1036.d", // For testing VALID unzipped directory
                    "DK0034-G10_1-D,2_01_1036_invalid.d", // For testing INVALID unzipped directory
                    "DK0034-G10_1-D,2_01_1036_flatzip.d", // For testing VALID ZIP
                    "DK0034-G10_1-D,2_01_1036_invalidzip.d", // For testing INVALID ZIP
                    MsDataSourceTypes.BRUKER_ANALYSIS_BAF, // Required content in directory
                    _bruker, MsDataSourceTypes.BRUKER.name(),
                    rawDataDir, expSvc);
        }

        private void testGetDataForDirBasedSampleFiles(String validDirName, String invalidDirName,
                                                       String validZipName, String invalidZipName,
                                                       String dirContentNameForValidDir, Instrument instrument, String instrumentDataDirName,
                                                       Path rawDataDir, ExperimentService expSvc)
        {
            String[] files = new String[] {validDirName, invalidDirName, validZipName, invalidZipName};
            for(String fileName: files)
            {
                // Rows have not yet been created in exp.data. Should not find any matching rows.
                testNoDataForSampleFile(fileName, instrument, rawDataDir, expSvc);
            }

            // Test valid data directory
            String name = validDirName;
            ExpData saved = addData(name, rawDataDir, instrumentDataDirName);
            testNoDataForSampleFile(name, instrument, rawDataDir, expSvc); // No rows created yet for the required directory content. Should not find any matches
            addData(dirContentNameForValidDir, Objects.requireNonNull(saved.getFilePath()), "");
            testGetDataForSampleFile(name, instrument, rawDataDir, expSvc); // Should find a match

            // Test invalid data directory
            name = invalidDirName;
            saved = addData(name, rawDataDir, instrumentDataDirName);
            testNoDataForSampleFile(name, instrument, rawDataDir, expSvc); // No rows created yet for the required directory content. Should not find any matches
            addData("invalid_" + dirContentNameForValidDir, Objects.requireNonNull(saved.getFilePath()), "");
            testNoDataForSampleFile(name, instrument, rawDataDir, expSvc); // Invalid directory. Should not find a match

            // Test valid zip
            name = validZipName;
            addData(name + ".ZIP", rawDataDir, instrumentDataDirName);
            testGetDataForSampleFile(name, instrument, rawDataDir, expSvc); // Should find a match
            testGetDataForSampleFileValidateZip(name, instrument, rawDataDir, expSvc); // Should find a match and zip validation should pass

            // Test invalid zip
            name = invalidZipName;
            addData(name + ".zip", rawDataDir, instrumentDataDirName);
            testGetDataForSampleFile(name, instrument, rawDataDir, expSvc); // Should find a match since we are not validating the zip file
            testNoDataForSampleFileValidateZip(name, instrument, rawDataDir, expSvc); // Should NOT find a match since zip validation will fail
        }

        private void testGetDataForSampleFile(String file, Instrument instrument, Path dataDir, ExperimentService expSvc)
        {
            testGetDataForSampleFile(file, instrument, dataDir, expSvc, true, false);
        }

        private void testNoDataForSampleFile(String file, Instrument instrument, Path dataDir, ExperimentService expSvc)
        {
            testGetDataForSampleFile(file, instrument, dataDir, expSvc, false, false);
        }

        private void testGetDataForSampleFileValidateZip(String file, Instrument instrument, Path dataDir, ExperimentService expSvc)
        {
            testGetDataForSampleFile(file, instrument, dataDir, expSvc, true, true);
        }

        private void testNoDataForSampleFileValidateZip(String file, Instrument instrument, Path dataDir, ExperimentService expSvc)
        {
            testGetDataForSampleFile(file, instrument, dataDir, expSvc, false, true);
        }

        private void testGetDataForSampleFile(String file, Instrument instrument, Path dataDir, ExperimentService expSvc, boolean hasExpData, boolean validateZip)
        {
            SampleFile sf = new SampleFile();
            sf.setFilePath("C:\\rawfiles\\" + file);
            sf.setInstrumentId(instrument.getId());
            if(hasExpData)
            {
                String message = "Expected row in exp.data for " + file + (validateZip ? " with zip validation." : "");
                assertNotNull(message, _util.getDataForSampleFile(sf, _container, dataDir, expSvc, validateZip));
            }
            else
            {
                String message = "Unxpected row in exp.data for " + file + (validateZip ? " with zip validation." : "");
                assertNull(message, _util.getDataForSampleFile(sf, _container, dataDir, expSvc, validateZip));
            }
        }

        private ExpData addData(String fileName, Path rawDataDir, String subfolder)
        {
            Lsid lsid = new Lsid(ExperimentService.get().generateGuidLSID(_container, new DataType("UploadedFile")));
            ExpData data = ExperimentService.get().createData(_container, fileName, lsid.toString());

            data.setContainer(_container);
            data.setDataFileURI(rawDataDir.resolve(subfolder).resolve(fileName).toUri());
            data.save(_user);
            return data;
        }

        @Test
        public void testGetMsDataSource()
        {
            SampleFile sampleFile = new SampleFile();
            MsDataSourceUtil dataSourceUtil = new MsDataSourceUtil();

            // The following file extensions are unambiguous. Will not need an instrument model to resolve to a single data source
            sampleFile.setFilePath("C:\\RawData\\file.mzML");
            assertEquals(MsDataSourceTypes.CONVERTED_DATA_SOURCE, dataSourceUtil.getMsDataSource(sampleFile));
            sampleFile.setFilePath("C:\\RawData\\file.mzXML");
            assertEquals(MsDataSourceTypes.CONVERTED_DATA_SOURCE, dataSourceUtil.getMsDataSource(sampleFile));
            sampleFile.setFilePath("C:\\RawData\\Site54_190909_Study9S_PHASE-1.wiff|Site54_STUDY9S_PHASE1_6ProtMix_QC_03|2");
            assertEquals(MsDataSourceTypes.SCIEX, dataSourceUtil.getMsDataSource(sampleFile));
            sampleFile.setFilePath("C:\\RawData\\file.lcd");
            assertEquals(MsDataSourceTypes.SHIMADZU, dataSourceUtil.getMsDataSource(sampleFile));

            // Ambiguous extensions. Need an instrument model to resolve data source
            // .raw
            sampleFile.setFilePath("C:\\RawData\\file.raw?centroid_ms1=true&centroid_ms2=true");
            sampleFile.setInstrumentId(_thermo.getId());
            assertEquals(MsDataSourceTypes.THERMO, dataSourceUtil.getMsDataSource(sampleFile));
            sampleFile.setInstrumentId(_waters.getId());
            assertEquals(MsDataSourceTypes.WATERS, dataSourceUtil.getMsDataSource(sampleFile));
            // .d
            sampleFile.setFilePath("C:\\RawData\\file.d");
            sampleFile.setInstrumentId(_agilent.getId());
            assertEquals(MsDataSourceTypes.AGILENT, dataSourceUtil.getMsDataSource(sampleFile));
            sampleFile.setInstrumentId(_bruker.getId());
            assertEquals(MsDataSourceTypes.BRUKER, dataSourceUtil.getMsDataSource(sampleFile));


            // With an unknown instrument model we will not be able to resolve the .raw and .d data sources to a single data source type
            sampleFile.setInstrumentId(_unknown.getId());
            MsDataSource sourceType;
            // .raw
            sampleFile.setFilePath("C:\\RawData\\file.raw?centroid_ms1=true&centroid_ms2=true");
            sourceType = dataSourceUtil.getMsDataSource(sampleFile);
            assertTrue(sourceType instanceof MsMultiDataSource);
            MsMultiDataSource multiSourceType = (MsMultiDataSource) sourceType;
            assertEquals("Expected one directory source", 1, multiSourceType.getDirSources().size());
            assertEquals("Expected Waters source", MsDataSourceTypes.WATERS, multiSourceType.getDirSources().get(0));
            assertEquals("Expected one file source", 1, multiSourceType.getFileSources().size());
            assertEquals("Expected Thermo source", MsDataSourceTypes.THERMO, multiSourceType.getFileSources().get(0));

            // .d
            sampleFile.setFilePath("C:\\RawData\\file.d");
            sourceType = dataSourceUtil.getMsDataSource(sampleFile);
            assertTrue(sourceType instanceof MsMultiDataSource);
            multiSourceType = (MsMultiDataSource) sourceType;
            assertEquals("Expected 2 directory sources", 2, multiSourceType.getDirSources().size());
            assertThat(multiSourceType.getDirSources(), hasItem(MsDataSourceTypes.AGILENT));
            assertThat(multiSourceType.getDirSources(), hasItem(MsDataSourceTypes.BRUKER));
            assertEquals("Expected 0 file source", 0, multiSourceType.getFileSources().size());


            sampleFile.setFilePath("C:\\RawData\\unknowntype");
            sourceType = dataSourceUtil.getMsDataSource(sampleFile);
            assertEquals("Expected Unknown datasource", MsDataSourceTypes.UNKNOWN, sourceType);
        }

        @Test
        public void testGetSourceForInstrument()
        {
            MsDataSourceUtil dataSourceUtil = MsDataSourceUtil.getInstance();
            Instrument instrument = new Instrument();
            assertNull(dataSourceUtil.getSourceForInstrument(instrument));

            // These are some of the values in the "model" column of the targetedms.instrument table on PanoramaWeb
            // Specific model names are only available for Thermo and SCIEX instruments.

            instrument.setModel("Thermo Electron instrument model");
            assertEquals(MsDataSourceTypes.THERMO, dataSourceUtil.getSourceForInstrument(instrument));
            instrument.setModel("Orbitrap Exploris 480");
            assertEquals(MsDataSourceTypes.THERMO, dataSourceUtil.getSourceForInstrument(instrument));
            instrument.setModel("TSQ Altis");
            assertEquals(MsDataSourceTypes.THERMO, dataSourceUtil.getSourceForInstrument(instrument));
            instrument.setModel("TSQ Quantum Ultra AM");
            assertEquals(MsDataSourceTypes.THERMO, dataSourceUtil.getSourceForInstrument(instrument));
            instrument.setModel("ITQ 1100");
            assertEquals(MsDataSourceTypes.THERMO, dataSourceUtil.getSourceForInstrument(instrument));


            instrument.setModel("AB SCIEX instrument model");
            assertEquals(MsDataSourceTypes.SCIEX, dataSourceUtil.getSourceForInstrument(instrument));
            instrument.setModel("SCIEX instrument model");
            assertEquals(MsDataSourceTypes.SCIEX, dataSourceUtil.getSourceForInstrument(instrument));
            instrument.setModel("Applied Biosystems instrument model");
            assertEquals(MsDataSourceTypes.SCIEX, dataSourceUtil.getSourceForInstrument(instrument));
            instrument.setModel("4000 QTRAP");
            assertEquals(MsDataSourceTypes.SCIEX, dataSourceUtil.getSourceForInstrument(instrument));
            instrument.setModel("QTRAP 5500");
            assertEquals(MsDataSourceTypes.SCIEX, dataSourceUtil.getSourceForInstrument(instrument));
            instrument.setModel("QSTAR Elite");
            assertEquals(MsDataSourceTypes.SCIEX, dataSourceUtil.getSourceForInstrument(instrument));
            instrument.setModel("TripleTOF 5600");
            assertEquals(MsDataSourceTypes.SCIEX, dataSourceUtil.getSourceForInstrument(instrument));
            instrument.setModel("TripleTOF 6600");
            assertEquals(MsDataSourceTypes.SCIEX, dataSourceUtil.getSourceForInstrument(instrument));
            instrument.setModel("Triple Quad 4500");
            assertEquals(MsDataSourceTypes.SCIEX, dataSourceUtil.getSourceForInstrument(instrument));
            instrument.setModel("Triple Quad 6500");
            assertEquals(MsDataSourceTypes.SCIEX, dataSourceUtil.getSourceForInstrument(instrument));

            instrument.setModel("Bruker Daltonics maXis series");
            assertEquals(MsDataSourceTypes.BRUKER, dataSourceUtil.getSourceForInstrument(instrument));

            instrument.setModel("Waters instrument model");
            assertEquals(MsDataSourceTypes.WATERS, dataSourceUtil.getSourceForInstrument(instrument));

            instrument.setModel("Agilent instrument model");
            assertEquals(MsDataSourceTypes.AGILENT, dataSourceUtil.getSourceForInstrument(instrument));

            instrument.setModel("Shimadzu instrument model");
            assertEquals(MsDataSourceTypes.SHIMADZU, dataSourceUtil.getSourceForInstrument(instrument));
        }

        private static void cleanDatabase() throws ExperimentException
        {
            TableInfo runsTable = TargetedMSManager.getTableInfoRuns();
            Integer runId = new TableSelector(TargetedMSManager.getTableInfoRuns(), Collections.singletonList(runsTable.getColumn("id")),
                    new SimpleFilter().addCondition(runsTable.getColumn("filename"), SKY_FILE_NAME, CompareType.EQUAL), null).getObject(Integer.class);
            if(runId != null)
            {
                Table.delete(TargetedMSManager.getTableInfoInstrument(),
                        new SimpleFilter(new CompareType.CompareClause(FieldKey.fromParts("runId"), CompareType.EQUAL, runId)));
                Table.delete(TargetedMSManager.getTableInfoRuns(), runId);
            }

            ExperimentService.get().deleteAllExpObjInContainer(_container, _user);
        }

        @AfterClass
        public static void cleanup() throws ExperimentException
        {
            cleanDatabase();
            _run = null;
        }
    }
}
