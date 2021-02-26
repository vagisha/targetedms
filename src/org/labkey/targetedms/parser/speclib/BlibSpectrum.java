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

package org.labkey.targetedms.parser.speclib;

/**
 * User: vsharma
 * Date: 5/5/12
 * Time: 10:46 PM
 */
public class BlibSpectrum extends LibSpectrum
{
    // Columns in RefSpectra table of .blib files
    // id|peptideSeq|peptideModSeq|precursorCharge|precursorMZ|prevAA|nextAA|copies|numPeaks
    private int _blibId;
    private String _prevAa;
    private String _nextAa;
    private int _copies;
    private int _numPeaks;
    private Integer _fileId;

    public int getBlibId()
    {
        return _blibId;
    }

    public void setBlibId(int blibId)
    {
        _blibId = blibId;
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

    public Integer getFileId()
    {
        return _fileId;
    }

    public void setFileId(Integer fileId)
    {
        _fileId = fileId;
    }
}
