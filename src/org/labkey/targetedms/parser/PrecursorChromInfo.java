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

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.cache.BlockingCache;
import org.labkey.api.cache.CacheManager;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.util.Tuple3;
import org.labkey.api.util.UnexpectedException;
import org.labkey.targetedms.TargetedMSModule;
import org.labkey.targetedms.TargetedMSRun;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.zip.DataFormatException;

/**
 * User: vsharma
 * Date: 4/16/12
 * Time: 3:39 PM
 */
public class PrecursorChromInfo extends ChromInfo<PrecursorChromInfoAnnotation>
{
    private Container _container;
    private int _precursorId;
    private int _generalMoleculeChromInfoId;

    private Double _bestRetentionTime;
    private Double _minStartTime;
    private Double _maxEndTime;
    private Double _totalArea;
    private Double _totalAreaNormalized;
    private Double _totalBackground;
    private Double _maxFwhm;
    private Double _maxHeight;
    private Double _averageMassErrorPPM;
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
    private int _numPoints;
    private int _numTransitions;
    private Integer _uncompressedSize;
    /** Starting byte index in the .skyd file */
    private Long _chromatogramOffset;
    /** Number of compressed bytes stored in the .skyd file */
    private Integer _chromatogramLength;
    private int _chromatogramFormat;

    private static final Logger LOG = Logger.getLogger(PrecursorChromInfo.class);
    private static final BlockingCache<Tuple3<Path, Long, Integer>, byte[]> ON_DEMAND_CHROM_CACHE = new BlockingCache<>(CacheManager.getCache(100, CacheManager.HOUR, "SKYD chromatogram cache"), (key, argument) -> {
        Path path = key.first;
        long offset = key.second;
        int length = key.third;

        long startTime = System.currentTimeMillis();
        LOG.debug("Loading chromatogram from " + path + ", offset " + offset + ", length " + length);
        try (SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.READ, StandardOpenOption.SPARSE))
        {
            channel.position(offset);
            java.nio.ByteBuffer byteBuffer = java.nio.ByteBuffer.allocate(length);
            channel.read(byteBuffer);
            byteBuffer.position(0);
            byte[] results = byteBuffer.array();
            LOG.debug("Finished loading from " + path + ", offset " + offset + ", length " + length + " in " + (System.currentTimeMillis() - startTime) + "ms");
            return results;
        }
        catch (NoSuchFileException e)
        {
            // Avoid a separate call to Files.exists() as it adds ~1 second overhead
            LOG.debug("Could not find SKYD file to get chromatogram at path " + path);
            return null;
        }
        catch (RuntimeException e)
        {
            if (e.getMessage() != null && e.getMessage().contains("The specified key does not exist"))
            {
                // Avoid a separate call to Files.exists() as it adds ~1 second overhead
                LOG.debug("Could not find SKYD file to get chromatogram at path " + path + ": " + e.getMessage());
                return null;
            }
            throw e;
        }
        catch (IOException e)
        {
            throw new UnexpectedException(e);
        }
    });

    public PrecursorChromInfo()
    {
    }

    public PrecursorChromInfo(Container c)
    {
        _container = c;
    }

    public Container getContainer()
    {
        return _container;
    }

    public void setContainer(Container container)
    {
        _container = container;
    }

    public int getPrecursorId()
    {
        return _precursorId;
    }

    public void setPrecursorId(int precursorId)
    {
        _precursorId = precursorId;
    }

    public int getGeneralMoleculeChromInfoId()
    {
        return _generalMoleculeChromInfoId;
    }

    public void setGeneralMoleculeChromInfoId(int generalmoleculechrominfoid)
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

    public Double getTotalAreaNormalized()
    {
        return _totalAreaNormalized;
    }

    public void setTotalAreaNormalized(Double totalAreaNormalized)
    {
        _totalAreaNormalized = totalAreaNormalized;
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

    public byte[] getChromatogram()
    {
        return _chromatogram;
    }

    public void setChromatogram(byte[] chromatogram)
    {
        _chromatogram = chromatogram;
    }

    public int getNumPoints()
    {
        return _numPoints;
    }

    public void setNumPoints(int numPoints)
    {
        _numPoints = numPoints;
    }

    public int getNumTransitions()
    {
        return _numTransitions;
    }

    public void setNumTransitions(int numTransitions)
    {
        _numTransitions = numTransitions;
    }

    public Integer getUncompressedSize()
    {
        if (_uncompressedSize != null)
        {
            return _uncompressedSize.intValue();
        }
        // For older data that got saved in the database without a value for uncompressedSize
        return (Integer.SIZE / 8) * _numPoints * (_numTransitions + 1);
    }

    public void setUncompressedSize(Integer uncompressedSize)
    {
        _uncompressedSize = uncompressedSize;
    }

    public Integer getChromatogramFormat()
    {
        return _chromatogramFormat;
    }

    public void setChromatogramFormat(Integer chromatogramFormat)
    {
        _chromatogramFormat = chromatogramFormat == null ? ChromatogramBinaryFormat.Arrays.ordinal() : chromatogramFormat.intValue();
    }

    public Long getChromatogramOffset()
    {
        return _chromatogramOffset;
    }

    public void setChromatogramOffset(Long chromatogramOffset)
    {
        _chromatogramOffset = chromatogramOffset;
    }

    public Integer getChromatogramLength()
    {
        return _chromatogramLength;
    }

    public void setChromatogramLength(Integer chromatogramLength)
    {
        _chromatogramLength = chromatogramLength;
    }

    @Nullable
    public Chromatogram createChromatogram(TargetedMSRun run)
    {
        try
        {
            if (_chromatogramFormat < 0 || _chromatogramFormat >= ChromatogramBinaryFormat.values().length)
            {
                throw new IllegalArgumentException("Unknown format number " + _chromatogramFormat);
            }

            ChromatogramBinaryFormat binaryFormat = ChromatogramBinaryFormat.values()[getChromatogramFormat()];

            byte[] databaseBytes = getChromatogram();
            byte[] compressedBytes = databaseBytes;
            boolean loadFromSkyd = Boolean.parseBoolean(TargetedMSModule.PREFER_SKYD_FILE_CHROMATOGRAMS_PROPERTY.getEffectiveValue(_container));

            if (loadFromSkyd || databaseBytes == null)
            {
                if (run.getSkydDataId() != null && _chromatogramLength != null && _chromatogramOffset != null)
                {
                    ExpData skydData = ExperimentService.get().getExpData(run.getSkydDataId());
                    if (skydData != null)
                    {
                        Path skydPath = skydData.getFilePath();
                        LOG.debug("Attempting to fetch chromatogram bytes (possibly cached) from " + skydPath + " for PrecursorChromInfo " + _generalMoleculeChromInfoId);
                        byte[] onDemandBytes = ON_DEMAND_CHROM_CACHE.get(new Tuple3<>(skydPath, _chromatogramOffset, _chromatogramLength));
                        if (databaseBytes != null && !Arrays.equals(databaseBytes, onDemandBytes))
                        {
                            LOG.error("Chromatogram bytes for PrecursorChromInfo " + _generalMoleculeChromInfoId + " do not match between .skyd and DB. Using database copy. Lengths: " + (onDemandBytes == null ? "null" : Integer.toString(onDemandBytes.length)) + " vs " + databaseBytes.length);
                        }
                        else
                        {
                            compressedBytes = onDemandBytes;
                        }
                    }
                }
                else
                {
                    LOG.debug("No length, offset, and/or SKYD DataId for PrecursorChromInfo " + _generalMoleculeChromInfoId);
                }
            }

            byte[] uncompressedBytes = SkylineBinaryParser.uncompressStoredBytes(compressedBytes, getUncompressedSize(), _numPoints, _numTransitions);
            return binaryFormat.readChromatogram(uncompressedBytes, _numPoints, _numTransitions);
        }
        catch (IOException | DataFormatException exception)
        {
            throw new UnexpectedException(exception);
        }
    }
}
