/*
 * Copyright (c) 2013-2019 LabKey Corporation
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
package org.labkey.targetedms.chromlib;

import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.parser.GeneralPrecursor;
import org.labkey.targetedms.parser.MoleculePrecursor;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.parser.PrecursorChromInfo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.labkey.targetedms.chromlib.BaseDaoImpl.readDouble;

/**
 * User: vsharma
 * Date: 12/31/12
 * Time: 9:25 AM
 */
public class LibPrecursor extends AbstractLibEntity
{
    // Shared fields
    private long _peptideId;
    protected String _isotopeLabel;
    protected double _mz;
    protected int _charge;
    protected Double _collisionEnergy;
    protected Double _declusteringPotential;
    protected Double _totalArea;
    protected byte[] _chromatogram;
    protected int _uncompressedSize;
    protected int _chromatogramFormat;
    protected long _sampleFileId;

    protected Double _explicitIonMobility;
    protected Double _ccs;
    protected Double _ionMobilityMS1;
    protected Double _ionMobilityFragment;

    protected Double _ionMobilityWindow;
    protected String _ionMobilityType;

    protected Integer _numTransitions;
    protected Integer _numPoints;
    protected Double _averageMassErrorPPM;
    private String _explicitIonMobilityUnits;
    private Double _explicitCcsSqa;
    private Double _explicitCompensationVoltage;

    protected List<LibPrecursorRetentionTime> _retentionTimes;
    protected List<LibTransition> _transitions;


    // Proteomics fields
    private Double _neutralMass;
    private String _modifiedSequence;
    private List<LibPrecursorIsotopeModification> _isotopeModifications;

    // Small molecule fields
    private String _adduct;


    public LibPrecursor() {}

    private LibPrecursor(GeneralPrecursor<?> p, Map<Long, String> isotopeLabelMap,
                                PrecursorChromInfo bestChromInfo, TargetedMSRun run, Map<Long, Integer> sampleFileIdMap)
    {
        String isotopeLabel = isotopeLabelMap.get(p.getIsotopeLabelId());
        if(isotopeLabel == null)
        {
            throw new IllegalStateException("Isotope label name not found for Id "+p.getIsotopeLabelId());
        }
        setIsotopeLabel(isotopeLabel);
        setMz(p.getMz());
        setCharge(p.getCharge());
        setCollisionEnergy(p.getCollisionEnergy());
        setDeclusteringPotential(p.getDeclusteringPotential());
        setExplicitIonMobility(p.getExplicitIonMobility());
        setExplicitIonMobilityUnits(p.getExplicitIonMobilityUnits());
        setExplicitCcsSqa(p.getExplicitCcsSqa());
        setExplicitCompensationVoltage(p.getExplicitCompensationVoltage());

        if (bestChromInfo != null)
        {
            setTotalArea(bestChromInfo.getTotalArea() == null ? 0.0 : bestChromInfo.getTotalArea());
            setChromatogram(bestChromInfo.getChromatogramBytes(run));
            setUncompressedSize(bestChromInfo.getUncompressedSize());
            setChromatogramFormat(bestChromInfo.getChromatogramFormat());
            setNumTransitions(bestChromInfo.getNumTransitions());
            setNumPoints(bestChromInfo.getNumPoints());
            setAverageMassErrorPPM(bestChromInfo.getAverageMassErrorPPM());
            setCcs(bestChromInfo.getCcs());
            setIonMobilityFragment(bestChromInfo.getIonMobilityFragment());
            setIonMobilityMS1(bestChromInfo.getIonMobilityMs1());
            setIonMobilityType(bestChromInfo.getIonMobilityType());
            setIonMobilityWindow(bestChromInfo.getIonMobilityWindow());

            long sampleFileId = bestChromInfo.getSampleFileId();
            Integer libSampleFileId = sampleFileIdMap.get(sampleFileId);
            if(libSampleFileId == null)
            {
                throw new IllegalStateException("Could not find an Id in the library for sample file Id "+sampleFileId);
            }
            setSampleFileId(libSampleFileId.intValue());
        }
        else
        {
            setTotalArea(0.0);
            setNumTransitions(0);
            setNumPoints(0);
        }
    }


    public LibPrecursor(Precursor precursor, Map<Long, String> isotopeLabelMap, PrecursorChromInfo bestChromInfo, TargetedMSRun run, Map<Long, Integer> sampleFileIdMap)
    {
        this((GeneralPrecursor<?>) precursor, isotopeLabelMap, bestChromInfo, run, sampleFileIdMap);
        setNeutralMass(precursor.getNeutralMass());
        setModifiedSequence(precursor.getModifiedSequence());
    }

    public LibPrecursor(MoleculePrecursor p, Map<Long, String> isotopeLabelMap, PrecursorChromInfo chromInfo,
                                TargetedMSRun run, Map<Long, Integer> sampleFileIdMap)
    {
        this((GeneralPrecursor<?>) p, isotopeLabelMap, chromInfo, run, sampleFileIdMap);
        setAdduct(p.getAdduct());
    }


    public LibPrecursor(ResultSet rs) throws SQLException
    {
        setId(rs.getInt(Constants.PrecursorColumn.Id.baseColumn().name()));
        setPeptideId(rs.getInt(Constants.PrecursorColumn.PeptideId.baseColumn().name()));
        setIsotopeLabel(rs.getString(Constants.PrecursorColumn.IsotopeLabel.baseColumn().name()));
        setMz(rs.getDouble(Constants.PrecursorColumn.Mz.baseColumn().name()));
        setCharge(rs.getInt(Constants.PrecursorColumn.Charge.baseColumn().name()));
        setNeutralMass(rs.getDouble(Constants.PrecursorColumn.NeutralMass.baseColumn().name()));
        setModifiedSequence(rs.getString(Constants.PrecursorColumn.ModifiedSequence.baseColumn().name()));
        setCollisionEnergy(readDouble(rs, Constants.PrecursorColumn.CollisionEnergy.baseColumn().name()));
        setDeclusteringPotential(readDouble(rs, Constants.PrecursorColumn.DeclusteringPotential.baseColumn().name()));
        setTotalArea(rs.getDouble(Constants.PrecursorColumn.TotalArea.baseColumn().name()));
        setNumTransitions(rs.getInt(Constants.PrecursorColumn.NumTransitions.baseColumn().name()));
        setNumPoints(rs.getInt(Constants.PrecursorColumn.NumPoints.baseColumn().name()));
        setAverageMassErrorPPM(rs.getDouble(Constants.PrecursorColumn.AverageMassErrorPPM.baseColumn().name()));
        setSampleFileId(rs.getInt(Constants.PrecursorColumn.SampleFileId.baseColumn().name()));
        setChromatogram(rs.getBytes(Constants.PrecursorColumn.Chromatogram.baseColumn().name()));
        setExplicitIonMobility(readDouble(rs, Constants.PrecursorColumn.ExplicitIonMobility.baseColumn().name()));
        setCcs(readDouble(rs, Constants.PrecursorColumn.CCS.baseColumn().name()));
        setIonMobilityMS1(readDouble(rs, Constants.PrecursorColumn.IonMobilityMS1.baseColumn().name()));
        setIonMobilityFragment(readDouble(rs, Constants.PrecursorColumn.IonMobilityFragment.baseColumn().name()));
        setIonMobilityWindow(readDouble(rs, Constants.PrecursorColumn.IonMobilityWindow.baseColumn().name()));
        setIonMobilityType(rs.getString(Constants.PrecursorColumn.IonMobilityType.baseColumn().name()));
        setExplicitIonMobilityUnits(rs.getString(Constants.PrecursorColumn.ExplicitIonMobilityUnits.baseColumn().name()));
        setExplicitCcsSqa(readDouble(rs, Constants.PrecursorColumn.ExplicitCcsSqa.baseColumn().name()));
        setExplicitCompensationVoltage(readDouble(rs, Constants.PrecursorColumn.ExplicitCompensationVoltage.baseColumn().name()));

        // Small molecule
        setAdduct(rs.getString(Constants.PrecursorColumn.Adduct.baseColumn().name()));
    }

    public long getPeptideId()
    {
        return _peptideId;
    }

    public void setPeptideId(long peptideId)
    {
        _peptideId = peptideId;
    }

    public Double getNeutralMass()
    {
        return _neutralMass;
    }

    public void setNeutralMass(Double neutralMass)
    {
        _neutralMass = neutralMass;
    }

    public String getModifiedSequence()
    {
        return _modifiedSequence;
    }

    public void setModifiedSequence(String modifiedSequence)
    {
        _modifiedSequence = modifiedSequence;
    }



    public void addIsotopeModification(LibPrecursorIsotopeModification isotopeModification)
    {
        if(_isotopeModifications == null)
        {
            _isotopeModifications = new ArrayList<>();
        }
        _isotopeModifications.add(isotopeModification);
    }

    public List<LibPrecursorIsotopeModification> getIsotopeModifications()
    {
        if(_isotopeModifications == null)
            return Collections.emptyList();
        else
            return Collections.unmodifiableList(_isotopeModifications);
    }

    public String getIsotopeLabel()
    {
        return _isotopeLabel;
    }

    public void setIsotopeLabel(String isotopeLabel)
    {
        _isotopeLabel = isotopeLabel;
    }

    public double getMz()
    {
        return _mz;
    }

    public void setMz(double mz)
    {
        _mz = mz;
    }

    public int getCharge()
    {
        return _charge;
    }

    public void setCharge(int charge)
    {
        _charge = charge;
    }

    public Double getCollisionEnergy()
    {
        return _collisionEnergy;
    }

    public void setCollisionEnergy(Double collisionEnergy)
    {
        _collisionEnergy = collisionEnergy;
    }

    public Double getDeclusteringPotential()
    {
        return _declusteringPotential;
    }

    public void setDeclusteringPotential(Double declusteringPotential)
    {
        _declusteringPotential = declusteringPotential;
    }

    public Double getTotalArea()
    {
        return _totalArea;
    }

    public void setTotalArea(Double totalArea)
    {
        _totalArea = totalArea;
    }

    public byte[] getChromatogram()
    {
        return _chromatogram;
    }

    public void setChromatogram(byte[] chromatogram)
    {
        _chromatogram = chromatogram;
    }

    public int getUncompressedSize()
    {
        return _uncompressedSize;
    }

    public void setUncompressedSize(int uncompressedSize)
    {
        _uncompressedSize = uncompressedSize;
    }

    public int getChromatogramFormat()
    {
        return _chromatogramFormat;
    }

    public void setChromatogramFormat(int chromatogramFormat)
    {
        _chromatogramFormat = chromatogramFormat;
    }

    public long getSampleFileId()
    {
        return _sampleFileId;
    }

    public void setSampleFileId(long sampleFileId)
    {
        _sampleFileId = sampleFileId;
    }

    public Double getExplicitIonMobility()
    {
        return _explicitIonMobility;
    }

    public void setExplicitIonMobility(Double explicitIonMobility)
    {
        _explicitIonMobility = explicitIonMobility;
    }

    public Double getCcs()
    {
        return _ccs;
    }

    public void setCcs(Double ccs)
    {
        _ccs = ccs;
    }

    public Double getIonMobilityMS1()
    {
        return _ionMobilityMS1;
    }

    public void setIonMobilityMS1(Double ionMobilityMS1)
    {
        _ionMobilityMS1 = ionMobilityMS1;
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

    public void setIonMobilityType(String ionMobilityType)
    {
        _ionMobilityType = ionMobilityType;
    }

    public String getIonMobilityType()
    {
        return _ionMobilityType;
    }

    public void setAverageMassErrorPPM(Double averageMassErrorPPM)
    {
        _averageMassErrorPPM = averageMassErrorPPM;
    }

    public Double getAverageMassErrorPPM()
    {
        return _averageMassErrorPPM;
    }

    public void setNumTransitions(Integer numTransitions)
    {
        _numTransitions = numTransitions;
    }

    public Integer getNumTransitions()
    {
        return _numTransitions;
    }

    public void setNumPoints(Integer numPoints)
    {
        _numPoints = numPoints;
    }

    public Integer getNumPoints()
    {
        return _numPoints;
    }



    public void addRetentionTime(LibPrecursorRetentionTime retentionTime)
    {
        if(_retentionTimes == null)
        {
            _retentionTimes = new ArrayList<>();
        }
        _retentionTimes.add(retentionTime);
    }

    public List<LibPrecursorRetentionTime> getRetentionTimes()
    {
        if(_retentionTimes == null)
            return Collections.emptyList();
        else
            return Collections.unmodifiableList(_retentionTimes);
    }

    public void addTransition(LibTransition transition)
    {
        if(_transitions == null)
        {
            _transitions = new ArrayList<>();
        }
        _transitions.add(transition);
    }

    public List<LibTransition> getTransitions()
    {
        if(_transitions == null)
            return Collections.emptyList();
        else
            return Collections.unmodifiableList(_transitions);
    }

    @Override
    public int getCacheSize()
    {
        return super.getCacheSize() +
                getTransitions().stream().mapToInt(AbstractLibEntity::getCacheSize).sum() +
                getRetentionTimes().stream().mapToInt(AbstractLibEntity::getCacheSize).sum() +
                getIsotopeModifications().stream().mapToInt(AbstractLibEntity::getCacheSize).sum();
    }

    public void setExplicitIonMobilityUnits(String explicitIonMobilityUnits)
    {
        _explicitIonMobilityUnits = explicitIonMobilityUnits;
    }

    public String getExplicitIonMobilityUnits()
    {
        return _explicitIonMobilityUnits;
    }

    public void setExplicitCcsSqa(Double explicitCcsSqa)
    {
        _explicitCcsSqa = explicitCcsSqa;
    }

    public Double getExplicitCcsSqa()
    {
        return _explicitCcsSqa;
    }

    public void setExplicitCompensationVoltage(Double explicitCompensationVoltage)
    {
        _explicitCompensationVoltage = explicitCompensationVoltage;
    }

    public Double getExplicitCompensationVoltage()
    {
        return _explicitCompensationVoltage;
    }

    public String getAdduct()
    {
        return _adduct;
    }

    public void setAdduct(String adduct)
    {
        _adduct = adduct;
    }

}
