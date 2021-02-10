/*
 * Copyright (c) 2012-2019 LabKey Corporation
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

package org.labkey.targetedms.parser;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.util.UnexpectedException;
import org.labkey.targetedms.TargetedMSModule;
import org.labkey.targetedms.TargetedMSRun;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: vsharma
 * Date: 4/16/12
 * Time: 3:39 PM
 */
public class PrecursorChromInfo extends AbstractChromInfo
{
    private static final Logger LOG = LogManager.getLogger(PrecursorChromInfo.class);

    private long _precursorId;
    private long _generalMoleculeChromInfoId;

    private Double _bestRetentionTime;
    private Double _minStartTime;
    private Double _maxEndTime;
    private Double _totalArea;
    private Double _totalBackground;
    private Double _maxFwhm;
    private Double _maxHeight;
    private Double _averageMassErrorPPM;
    private Double _bestMassErrorPPM;
    private Double _peakCountRatio;
    private Integer _numTruncated;
    private String _identified;
    private Double _libraryDotP;
    private Double _isotopeDotP;
    private Integer _optimizationStep;
    private String _userSet;
    private String _note;
    private Double _qvalue;
    private Double _zscore;

    private byte[] _chromatogram;
    private int _numTransitions;

    private Double _ccs;
    private Double _ionMobilityMs1;
    private Double _ionMobilityFragment;
    private Double _ionMobilityWindow;
    private String _ionMobilityType;

    private List<Integer> _transitionChromatogramIndices;

    public PrecursorChromInfo()
    {
    }

    public PrecursorChromInfo(Container c)
    {
        super(c);
    }

    public long getPrecursorId()
    {
        return _precursorId;
    }

    public void setPrecursorId(long precursorId)
    {
        _precursorId = precursorId;
    }

    public long getGeneralMoleculeChromInfoId()
    {
        return _generalMoleculeChromInfoId;
    }

    public void setGeneralMoleculeChromInfoId(long generalmoleculechrominfoid)
    {
        _generalMoleculeChromInfoId = generalmoleculechrominfoid;
    }

    public Double getBestRetentionTime()
    {
        return _bestRetentionTime;
    }

    public void setBestRetentionTime(Double bestRetentionTime)
    {
        _bestRetentionTime = bestRetentionTime;
    }

    public Double getMinStartTime()
    {
        return _minStartTime;
    }

    public void setMinStartTime(Double minStartTime)
    {
        _minStartTime = minStartTime;
    }

    public Double getMaxEndTime()
    {
        return _maxEndTime;
    }

    public void setMaxEndTime(Double maxEndTime)
    {
        _maxEndTime = maxEndTime;
    }

    public Double getTotalArea()
    {
        return _totalArea;
    }

    public void setTotalArea(Double totalArea)
    {
        _totalArea = totalArea;
    }

    public Double getTotalBackground()
    {
        return _totalBackground;
    }

    public void setTotalBackground(Double totalBackground)
    {
        _totalBackground = totalBackground;
    }

    public Double getMaxFwhm()
    {
        return _maxFwhm;
    }

    public void setMaxFwhm(Double maxFwhm)
    {
        _maxFwhm = maxFwhm;
    }

    public Double getMaxHeight()
    {
        return _maxHeight;
    }

    public void setMaxHeight(Double maxHeight)
    {
        _maxHeight = maxHeight;
    }

    public Double getAverageMassErrorPPM()
    {
        return _averageMassErrorPPM;
    }

    public void setAverageMassErrorPPM(Double averageMassErrorPPM)
    {
        _averageMassErrorPPM = averageMassErrorPPM;
    }

    public Double getBestMassErrorPPM()
    {
        return _bestMassErrorPPM;
    }

    public void setBestMassErrorPPM(Double bestMassErrorPPM)
    {
        _bestMassErrorPPM = bestMassErrorPPM;
    }

    public Double getPeakCountRatio()
    {
        return _peakCountRatio;
    }

    public void setPeakCountRatio(Double peakCountRatio)
    {
        _peakCountRatio = peakCountRatio;
    }

    public Integer getNumTruncated()
    {
        return _numTruncated;
    }

    public void setNumTruncated(Integer numTruncated)
    {
        _numTruncated = numTruncated;
    }

    public String getIdentified()
    {
        return _identified;
    }

    public void setIdentified(String identified)
    {
        _identified = identified;
    }

    public Double getLibraryDotP()
    {
        return _libraryDotP;
    }

    public void setLibraryDotP(Double libraryDotP)
    {
        _libraryDotP = libraryDotP;
    }

    public Double getIsotopeDotP()
    {
        return _isotopeDotP;
    }

    public void setIsotopeDotP(Double isotopeDotP)
    {
        _isotopeDotP = isotopeDotP;
    }

    public Integer getOptimizationStep()
    {
        return _optimizationStep;
    }

    public void setOptimizationStep(Integer optimizationStep)
    {
        _optimizationStep = optimizationStep;
    }

    public boolean isOptimizationPeak()
    {
        return _optimizationStep != null;
    }

    public String getUserSet()
    {
        return _userSet;
    }

    public void setUserSet(String userSet)
    {
        _userSet = userSet;
    }

    public String getNote()
    {
        return _note;
    }

    public void setNote(String note)
    {
        _note = note;
    }

    public Double getQvalue()
    {
        return _qvalue;
    }

    public void setQvalue(Double qvalue)
    {
        _qvalue = qvalue;
    }

    public Double getZscore()
    {
        return _zscore;
    }

    public void setZscore(Double zscore)
    {
        _zscore = zscore;
    }

    public Double getCcs()
    {
        return _ccs;
    }

    public void setCcs(Double ccs)
    {
        _ccs = ccs;
    }

    public Double getIonMobilityMs1()
    {
        return _ionMobilityMs1;
    }

    public void setIonMobilityMs1(Double ionMobilityMs1)
    {
        _ionMobilityMs1 = ionMobilityMs1;
    }

    public Double getIonMobilityFragment()
    {
        return _ionMobilityFragment;
    }

    public void setIonMobilityFragment(Double ionMobilityFragment)
    {
        _ionMobilityFragment = ionMobilityFragment;
    }

    public Double getIonMobilityWindow()
    {
        return _ionMobilityWindow;
    }

    public void setIonMobilityWindow(Double ionMobilityWindow)
    {
        _ionMobilityWindow = ionMobilityWindow;
    }

    public String getIonMobilityType()
    {
        return _ionMobilityType;
    }

    public void setIonMobilityType(String ionMobilityType)
    {
        _ionMobilityType = ionMobilityType;
    }

    @Override @Nullable
    public byte[] getChromatogram()
    {
        return _chromatogram;
    }

    public void setChromatogram(byte[] chromatogram)
    {
        _chromatogram = chromatogram;
    }

    @Override
    public int getNumTransitions()
    {
        return _numTransitions;
    }

    public void setNumTransitions(int numTransitions)
    {
        _numTransitions = numTransitions;
    }

    @Override
    public Integer getUncompressedSize()
    {
        Integer result = super.getUncompressedSize();
        if (result == null)
        {
            result = (Integer.SIZE / 8) * getNumPoints() * (_numTransitions + 1);
        }

        return result;
    }

    /** Use the module property to decide whether to try fetching from disk*/
    @Nullable @Override
    public Chromatogram createChromatogram(TargetedMSRun run)
    {
        return createChromatogram(run, Boolean.parseBoolean(TargetedMSModule.PREFER_SKYD_FILE_CHROMATOGRAMS_PROPERTY.getEffectiveValue(getContainer())));
    }

    @Override
    public String toString()
    {
        return "PrecursorChromInfo" + _generalMoleculeChromInfoId;
    }

    @Nullable
    public List<Integer> getTransitionChromatogramIndicesList()
    {
        return _transitionChromatogramIndices;
    }

    /**
     * When we don't have the TransitionChromInfos in the DB, we store the indices into the transition chromatogram.
     * It's not needed in SQL queries, so we pack it into a byte array. The first two bytes are a 16-bit integer
     * representing the number of indices included, followed by two bytes for each 16-bit integer index.
     */
    public byte[] getTransitionChromatogramIndices()
    {
        if (_transitionChromatogramIndices == null)
        {
            return null;
        }
        ByteArrayOutputStream bOut = new ByteArrayOutputStream(_transitionChromatogramIndices.size() * 2 + 2);
        try (DataOutputStream dOut = new DataOutputStream(bOut))
        {
            dOut.writeShort(_transitionChromatogramIndices.size());
            for (Integer i : _transitionChromatogramIndices)
            {
                dOut.writeShort(i);
            }
        }
        catch (IOException e)
        {
            throw UnexpectedException.wrap(e);
        }
        return bOut.toByteArray();
    }

    public void setTransitionChromatogramIndices(byte[] bytes)
    {
        if (bytes == null)
        {
            _transitionChromatogramIndices = null;
        }
        else
        {
            try(DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes)))
            {
                _transitionChromatogramIndices = new ArrayList<>();
                int count = in.readUnsignedShort();
                for (int i = 0; i < count; i++)
                {
                    _transitionChromatogramIndices.add(in.readUnsignedShort());
                }
            }
            catch (IOException e)
            {
                LOG.error("Unable to read " + bytes.length + " of transition chromatogram indices");
            }

        }
    }

    /** Fake up enough of a TransitionChromInfo to enable most of the transition chromatogram plotting */
    public TransitionChromInfo makeDummyTransitionChromInfo(int index)
    {
        TransitionChromInfo dummy = new TransitionChromInfo();
        dummy.setStartTime(getMinStartTime());
        dummy.setEndTime(getMaxEndTime());
        dummy.setMassErrorPPM(getBestMassErrorPPM());
        dummy.setChromatogramIndex(getTransitionChromatogramIndicesList().get(index));
        return dummy;
    }

    public void addTransitionChromatogramIndex(int matchIndex)
    {
        if (_transitionChromatogramIndices == null)
        {
            _transitionChromatogramIndices = new ArrayList<>();
        }
        _transitionChromatogramIndices.add(matchIndex);
    }
}
