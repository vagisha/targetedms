/*
 * Copyright (c) 2012-2016 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.targetedms.parser.blib;

import org.apache.commons.io.FilenameUtils;
import org.labkey.api.util.Formats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: vsharma
 * Date: 5/5/12
 * Time: 10:46 PM
 */
public class BlibSpectrum
{
    // Columns in RefSpectra table of .blib files
    // id|peptideSeq|peptideModSeq|precursorCharge|precursorMZ|prevAA|nextAA|copies|numPeaks
    private int _blibId;
    private String _peptideSeq;
    private String _peptideModSeq;
    private int _precursorCharge;
    private double _precursorMz;
    private String _prevAa;
    private String _nextAa;
    private int _copies;
    private int _numPeaks;
    private Double _retentionTime;
    private Integer _fileId;
    private String _sourceFile;

    private List<Peak> _peakList;

    private List<RedundantSpectrum> _redundantSpectrumList;

    public int getBlibId()
    {
        return _blibId;
    }

    public void setBlibId(int blibId)
    {
        _blibId = blibId;
    }

    public String getPeptideSeq()
    {
        return _peptideSeq;
    }

    public void setPeptideSeq(String peptideSeq)
    {
        _peptideSeq = peptideSeq;
    }

    public String getPeptideModSeq()
    {
        return _peptideModSeq;
    }

    public void setPeptideModSeq(String peptideModSeq)
    {
        _peptideModSeq = peptideModSeq;
    }

    public int getPrecursorCharge()
    {
        return _precursorCharge;
    }

    public void setPrecursorCharge(int precursorCharge)
    {
        _precursorCharge = precursorCharge;
    }

    public double getPrecursorMz()
    {
        return _precursorMz;
    }

    public void setPrecursorMz(double precursorMz)
    {
        _precursorMz = precursorMz;
    }

    public String getPrevAa()
    {
        return _prevAa;
    }

    public void setPrevAa(String prevAa)
    {
        _prevAa = prevAa;
    }

    public String getNextAa()
    {
        return _nextAa;
    }

    public void setNextAa(String nextAa)
    {
        _nextAa = nextAa;
    }

    public int getCopies()
    {
        return _copies;
    }

    public void setCopies(int copies)
    {
        _copies = copies;
    }

    public int getNumPeaks()
    {
        return _numPeaks;
    }

    public void setNumPeaks(int numPeaks)
    {
        _numPeaks = numPeaks;
    }

    public Double getRetentionTime()
    {
        return _retentionTime;
    }

    public String getRetentionTimeF2()
    {
        return _retentionTime == null ? null : Formats.f2.format(_retentionTime);
    }

    public void setRetentionTime(Double retentionTime)
    {
        _retentionTime = retentionTime;
    }

    public Integer getFileId()
    {
        return _fileId;
    }

    public void setFileId(Integer fileId)
    {
        _fileId = fileId;
    }

    public String getSourceFile()
    {
        return _sourceFile;
    }

    public String getSourceFileName()
    {
        return (_sourceFile != null ) ? FilenameUtils.getName(_sourceFile) : "";
    }

    public void setSourceFile(String sourceFile)
    {
        _sourceFile = sourceFile;
    }

    void setMzAndIntensity (double[] mzArr, float[] intensityArr)
    {

        _peakList = new ArrayList<>(mzArr.length);

        for(int i = 0; i < mzArr.length; i++)
        {
            _peakList.add(new Peak(mzArr[i], intensityArr[i]));
        }

        // Sort the list
        Collections.sort(_peakList);
    }

    public List<Peak> getPeaks()
    {
        return Collections.unmodifiableList(_peakList);
    }

    public List<RedundantSpectrum> getRedundantSpectrumList()
    {
        return _redundantSpectrumList == null ? Collections.emptyList() : Collections.unmodifiableList(_redundantSpectrumList);
    }

    public void setRedundantSpectrumList(List<RedundantSpectrum> redundantSpectrumList)
    {
        _redundantSpectrumList = redundantSpectrumList;
    }

    public static class Peak implements Comparable<Peak>
    {
        private final double _mz;
        private final float _intensity;

        public Peak(double mz, float intensity)
        {
            _mz = mz;
            _intensity = intensity;
        }

        public double getMz()
        {
            return _mz;
        }

        public float getIntensity()
        {
            return _intensity;
        }

        @Override
        public int compareTo(Peak o)
        {
            return Double.valueOf(this.getMz()).compareTo(o.getMz());
        }
    }

    public static class RedundantSpectrum
    {
        private int _redundantRefSpectrumId;
        private String _sourceFile;
        private Double _retentionTime;
        private boolean _bestSpectrum;

        public int getRedundantRefSpectrumId()
        {
            return _redundantRefSpectrumId;
        }

        public void setRedundantRefSpectrumId(int redundantRefSpectrumId)
        {
            _redundantRefSpectrumId = redundantRefSpectrumId;
        }

        public String getSourceFile()
        {
            return _sourceFile;
        }

        public String getSourceFileName()
        {
            return (_sourceFile != null ) ? FilenameUtils.getName(_sourceFile) : "";
        }

        public void setSourceFile(String sourceFile)
        {
            _sourceFile = sourceFile;
        }

        public Double getRetentionTime()
        {
            return _retentionTime;
        }

        public String getRetentionTimeF2()
        {
            return _retentionTime == null ? null : Formats.f2.format(_retentionTime);
        }

        public void setRetentionTime(Double retentionTime)
        {
            _retentionTime = retentionTime;
        }

        public boolean isBestSpectrum()
        {
            return _bestSpectrum;
        }

        public void setBestSpectrum(boolean bestSpectrum)
        {
            _bestSpectrum = bestSpectrum;
        }
    }
}
