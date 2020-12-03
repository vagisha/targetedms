/*
 * Copyright (c) 2012-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.targetedms.parser;

import org.apache.commons.io.FilenameUtils;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.targetedms.ISampleFile;
import org.labkey.api.targetedms.model.SampleFileInfo;

import java.util.Date;
import java.util.List;

/**
 * User: jeckels
 * Date: Apr 18, 2012
 */
public class SampleFile extends SkylineEntity implements ISampleFile
{
    private long _replicateId;
    private Long _instrumentId;
    private String _filePath;
    /** User-specified in Skyline */
    private String _sampleName;
    private String _skylineId;
    private Date _acquiredTime;
    private Date _modifiedTime;
    private Double _ticArea;
    private String _instrumentSerialNumber;
    /** Extracted by Skyline from raw file */
    private String _sampleId;
    private Double _explicitGlobalStandardArea;
    private String _ionMobilityType;

    // Calculated values loaded via TargetedMSManager.getSampleFiles()
    private Integer _guideSetId;
    private boolean _ignoreForAllMetric;

    private List<Instrument> _instrumentInfoList;

    public long getReplicateId()
    {
        return _replicateId;
    }

    public void setReplicateId(long replicateId)
    {
        _replicateId = replicateId;
    }

    @Override
    public String getFilePath()
    {
        return _filePath;
    }

    public void setFilePath(String filePath)
    {
        _filePath = filePath;
    }

    @Override
    public String getSampleName()
    {
        return _sampleName;
    }

    public void setSampleName(String sampleName)
    {
        _sampleName = sampleName;
    }

    @Override
    public Date getAcquiredTime()
    {
        return _acquiredTime;
    }

    public void setAcquiredTime(Date acquiredTime)
    {
        _acquiredTime = acquiredTime;
    }

    @Override
    public Date getModifiedTime()
    {
        return _modifiedTime;
    }

    public void setModifiedTime(Date modifiedTime)
    {
        _modifiedTime = modifiedTime;
    }

    @Override
    public Long getInstrumentId()
    {
        return _instrumentId;
    }

    public void setInstrumentId(Long instrumentId)
    {
        _instrumentId = instrumentId;
    }

    @Override
    public String getSkylineId()
    {
        return _skylineId;
    }

    public void setSkylineId(String skylineId)
    {
        _skylineId = skylineId;
    }

    public List<Instrument> getInstrumentInfoList()
    {
        return _instrumentInfoList;
    }

    public void setInstrumentInfoList(List<Instrument> instrumentInfoList)
    {
        _instrumentInfoList = instrumentInfoList;
    }

    @Override
    public Double getTicArea()
    {
        return _ticArea;
    }

    public void setTicArea(Double ticArea)
    {
        _ticArea = ticArea;
    }

    @Override
    public String getInstrumentSerialNumber()
    {
        return _instrumentSerialNumber;
    }

    public void setInstrumentSerialNumber(String instrumentSerialNumber)
    {
        _instrumentSerialNumber = instrumentSerialNumber;
    }

    @Override
    public String getSampleId()
    {
        return _sampleId;
    }

    public void setSampleId(String sampleId)
    {
        _sampleId = sampleId;
    }

    @Override
    public Double getExplicitGlobalStandardArea()
    {
        return _explicitGlobalStandardArea;
    }

    public void setExplicitGlobalStandardArea(Double explicitGlobalStandardArea)
    {
        _explicitGlobalStandardArea = explicitGlobalStandardArea;
    }

    @Override
    public String getIonMobilityType()
    {
        return _ionMobilityType;
    }

    public void setIonMobilityType(String ionMobilityType)
    {
        _ionMobilityType = ionMobilityType;
    }

    public Integer getGuideSetId()
    {
        return _guideSetId;
    }

    public void setGuideSetId(Integer guideSetId)
    {
        _guideSetId = guideSetId;
    }

    public boolean isIgnoreForAllMetric()
    {
        return _ignoreForAllMetric;
    }

    public void setIgnoreForAllMetric(boolean ignoreForAllMetric)
    {
        _ignoreForAllMetric = ignoreForAllMetric;
    }

    public SampleFileInfo toSampleFileInfo()
    {
        return new SampleFileInfo(getId(), getAcquiredTime(), getSampleName(), _guideSetId, _ignoreForAllMetric, getFilePath(), getReplicateId());
    }

    @Override
    /**
     * Returns the filename parsed out from the file path
     */
    public String getFileName()
    {
        return getFileName(_filePath);
    }

    private static String getFileName(String path)
    {
        if(path != null)
        {
            // If the file path has a '?' part remove it
            // Example: 2017_July_10_bivalves_292.raw?centroid_ms2=true.
            int idx = path.indexOf('?');
            path = (idx == -1) ? path : path.substring(0, idx);

            // If the file path has a '|' part for sample name from multi-injection wiff files remove it.
            // Example: D:\Data\CPTAC_Study9s\Site52_041009_Study9S_Phase-I.wiff|Site52_STUDY9S_PHASEI_6ProtMix_QC_07|6
            idx = path.indexOf('|');
            path =  (idx == -1) ? path : path.substring(0, idx);

            return FilenameUtils.getName(path);
        }
        return null;
    }

    public static class TestCase extends Assert
    {
        @Test
        public void testGetFileName()
        {
            // Skyline tracks centroiding, lockmass settings etc. as part of the file_path attribute of the <sample_file>
            // element in .sky files. These are appended at the end of the file path as query parameters.
            // Example: C:\Users\lab\Data\2017_July_10_bivalves_140.raw?centroid_ms1=true&centroid_ms2=true.
            String fileName = "2017_July_10_bivalves_140.raw";
            String path = "C:\\Users\\lab\\Data\\2017-Geoduck-SRM-raw\\" + fileName;
            String pathWithParams = path + "?centroid_ms1=true&centroid_ms2=true";

            assertTrue(fileName.equals(getFileName(path)));
            assertTrue(fileName.equals(getFileName(pathWithParams)));

            // Skyline stores multi-injection wiff file paths as: <wiff_file_path>|<sample_name>|<sample_index>
            // Example: C:\Analyst Data\Projects\CPTAC\Site54_STUDY9S_PHASE1_6ProtMix_090919\Site54_190909_Study9S_PHASE-1.wiff|Site54_STUDY9S_PHASE1_6ProtMix_QC_03|2
            fileName = "Site54_190909_Study9S_PHASE-1.wiff";
            path = "C:\\Analyst Data\\Projects\\CPTAC\\Site54_STUDY9S_PHASE1_6ProtMix_090919\\" + fileName;
            String pathWithSampleInfo = path + "|Site54_STUDY9S_PHASE1_6ProtMix_QC_03|2";

            assertTrue(fileName.equals(getFileName(path)));
            assertTrue(fileName.equals(getFileName(pathWithSampleInfo)));

            // Add a bogus param with a '|' character
            String pathWithSampleInfoAndParams = pathWithSampleInfo + "?centroid_ms1=true&centroid_ms2=true&madeup_param=a|b";

            assertTrue(fileName.equals(getFileName(path)));
            assertTrue(fileName.equals(getFileName(pathWithSampleInfoAndParams)));
        }
    }
}
