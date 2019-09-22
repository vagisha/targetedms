/*
 * Copyright (c) 2019 LabKey Corporation
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

/* targetedms-0.00-12.20.sql */

CREATE SCHEMA targetedms;

-- iRTScale table to store iRT scale information
CREATE TABLE targetedms.iRTScale
(
    Id SERIAL NOT NULL,
    Container ENTITYID NOT NULL,
    Created TIMESTAMP,
    CreatedBy INT,

    CONSTRAINT PK_iRTScale PRIMARY KEY (Id),
    CONSTRAINT FK_iRTScale_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId)
);
CREATE INDEX IX_iRTScale_Container ON targetedms.iRTScale (Container);

CREATE TABLE targetedms.Runs
(
    _ts TIMESTAMP DEFAULT now(),
    Id SERIAL NOT NULL,
    CreatedBy USERID,
    Created TIMESTAMP,
    ModifiedBy USERID,
    Modified TIMESTAMP,
    Owner USERID NULL,

    Container ENTITYID NOT NULL,
    EntityId ENTITYID NOT NULL,
    Description VARCHAR(300),
    FileName VARCHAR(300),
    Status VARCHAR(200),
    StatusId INT NOT NULL DEFAULT 0,
    Deleted BOOLEAN NOT NULL DEFAULT '0',
    ExperimentRunLSID LSIDType NULL,

    PeptideGroupCount INT NOT NULL DEFAULT 0,
    PeptideCount INT NOT NULL DEFAULT 0,
    PrecursorCount INT NOT NULL DEFAULT 0,
    TransitionCount INT NOT NULL DEFAULT 0,
    RepresentativeDataState INT NOT NULL DEFAULT 0,
    DataId INT,
    iRTScaleId INT,
    SoftwareVersion VARCHAR(50),
    FormatVersion VARCHAR(10),

    CONSTRAINT PK_Runs PRIMARY KEY (Id)
);

ALTER TABLE targetedms.Runs ADD CONSTRAINT FK_Runs_Data FOREIGN KEY (DataId) REFERENCES exp.Data(RowId);
ALTER TABLE targetedms.Runs ADD CONSTRAINT FK_Runs_iRTScaleId FOREIGN KEY (iRTScaleId) REFERENCES targetedms.iRTScale(Id);
CREATE INDEX IX_Runs_iRTScaleId ON targetedms.Runs (iRTScaleId);
CREATE INDEX IX_Runs_Container ON targetedms.Runs (Container);


-- ----------------------------------------------------------------------------
-- Transition Settings
-- ----------------------------------------------------------------------------
CREATE TABLE targetedms.Predictor
(
    Id SERIAL NOT NULL,
    Name VARCHAR(100),
    StepSize REAL,
    StepCount INT,

    CONSTRAINT PK_Predictor PRIMARY KEY (Id)
);

CREATE TABLE targetedms.TransitionPredictionSettings
(
    RunId INT NOT NULL,
    PrecursorMassType VARCHAR(20),
    ProductMassType VARCHAR(20),
    OptimizeBy VARCHAR(10) NOT NULL,
    CePredictorId INT,
    DpPredictorId INT,

    CONSTRAINT PK_TransitionPredictionSettings PRIMARY KEY (RunId),
    CONSTRAINT FK_TransitionPredictionSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);

CREATE TABLE targetedms.TransitionInstrumentSettings
(
    RunId INT NOT NULL,
    DynamicMin BOOLEAN,
    MinMz INT NOT NULL,
    MaxMz INT NOT NULL,
    MzMatchTolerance REAL NOT NULL,
    MinTime INT,
    MaxTime INT,
    MaxTransitions INT,

    CONSTRAINT PK_TransitionInstrumentSettings PRIMARY KEY (RunId),
    CONSTRAINT FK_TransitionInstrumentSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);

CREATE TABLE targetedms.TransitionFullScanSettings
(
    RunId INT NOT NULL,
    PrecursorFilter REAL,
    PrecursorLeftFilter REAL,
    PrecursorRightFilter REAL,
    ProductMassAnalyzer VARCHAR(20),
    ProductRes REAL,
    ProductResMz REAL,
    PrecursorIsotopes VARCHAR(10),
    PrecursorIsotopeFilter REAL,
    PrecursorMassAnalyzer VARCHAR(20),
    PrecursorRes REAL,
    PrecursorResMz REAL,
    ScheduleFilter BOOLEAN,
    -- AcquisitionMethod can be one of 'none', 'Targeted', 'DIA
    AcquisitionMethod VARCHAR(10),
    -- RetentionTimeFilterType can be one of 'none', 'scheduling_windows', 'ms2_ids'
    RetentionTimeFilterType VARCHAR(20),
    RetentionTimeFilterLength REAL,

    CONSTRAINT PK_TransitionFullScanSettings PRIMARY KEY (RunId),
    CONSTRAINT FK_TransitionFullScanSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);

CREATE TABLE targetedms.IsotopeEnrichment
(
    Id SERIAL NOT NULL,
    RunId INT NOT NULL,
    Symbol VARCHAR(10),
    PercentEnrichment REAL,
    Name VARCHAR(100),

    CONSTRAINT PK_IsotopeEnrichment PRIMARY KEY (Id),
    CONSTRAINT FK_IsotopeEnrichment_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);
CREATE INDEX IX_IsotopeEnrichment_RunId ON targetedms.IsotopeEnrichment (RunId);



-- ----------------------------------------------------------------------------
-- Peptide Settings
-- ----------------------------------------------------------------------------
CREATE TABLE targetedms.RetentionTimePredictionSettings
(
    RunId INT NOT NULL,
    CalculatorName VARCHAR(200),
    IsIrt BOOLEAN,
    RegressionSlope REAL,
    RegressionIntercept REAL,
    PredictorName VARCHAR(200),
    TimeWindow REAL,
    UseMeasuredRts BOOLEAN,
    MeasuredRtWindow REAL,
    IrtDatabasePath VARCHAR(500),

    CONSTRAINT PK_RetentionTimePredictionSettings PRIMARY KEY (RunId),
    CONSTRAINT FK_RetentionTimePredictionSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);



-- ----------------------------------------------------------------------------
-- Instrument, Replicate and SampleFile
-- ----------------------------------------------------------------------------
CREATE TABLE targetedms.Instrument
(
    Id SERIAL NOT NULL,
    RunId INT NOT NULL,
    Model VARCHAR(300),
    IonizationType VARCHAR(300),
    Analyzer VARCHAR(300),
    Detector VARCHAR(300),

    CONSTRAINT PK_Instrument PRIMARY KEY (Id),
    CONSTRAINT FK_Instrument_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);

CREATE INDEX IX_Instrument_RunId ON targetedms.Instrument (RunId);

CREATE TABLE targetedms.Replicate
(
    Id SERIAL NOT NULL,
    RunId INT NOT NULL,
    Name VARCHAR(100) NOT NULL,
    CePredictorId INT,
    DpPredictorId INT,

    CONSTRAINT PK_Replicate PRIMARY KEY (Id),
    CONSTRAINT FK_Replicate_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id),
    CONSTRAINT FK_Replicate_PredictorCe FOREIGN KEY (CePredictorId) REFERENCES targetedms.Predictor(Id),
    CONSTRAINT FK_Replicate_PredictorDp FOREIGN KEY (DpPredictorId) REFERENCES targetedms.Predictor(Id)
);

CREATE INDEX IX_Replicate_RunId ON targetedms.Replicate(RunId);
CREATE INDEX IX_Replicate_CePredictorId ON targetedms.Replicate(CePredictorId);
CREATE INDEX IX_Replicate_DpPredictorId ON targetedms.Replicate(DpPredictorId);

CREATE TABLE targetedms.SampleFile
(
    Id SERIAL NOT NULL,
    ReplicateId INT NOT NULL,
    FilePath VARCHAR(500) NOT NULL,
    SampleName VARCHAR(300) NOT NULL,
    SkylineId VARCHAR(300),
    AcquiredTime TIMESTAMP,
    ModifiedTime TIMESTAMP,
    InstrumentId INT,

    CONSTRAINT PK_SampleFile PRIMARY KEY (Id),
    CONSTRAINT FK_SampleFile_Replicate FOREIGN KEY (ReplicateId) REFERENCES targetedms.Replicate(Id),
    CONSTRAINT FK_SampleFile_Instrument FOREIGN KEY (InstrumentId) REFERENCES targetedms.Instrument(Id)
);

CREATE INDEX IX_SampleFile_ReplicateId ON targetedms.SampleFile(ReplicateId);

-- ----------------------------------------------------------------------------
-- Peptide Group
-- ----------------------------------------------------------------------------
CREATE TABLE targetedms.PeptideGroup
(
    Id SERIAL NOT NULL,
    RunId INT NOT NULL,
    Label CHAR(255) NOT NULL,
    Description TEXT,
    SequenceId INTEGER,
    Decoy BOOLEAN,
    Note TEXT,
    Modified TIMESTAMP,

    -- 0 = NotRepresentative, 1 = Representative_Protein, 2 = Representative_Peptide
    RepresentativeDataState INT NOT NULL DEFAULT 0,
    Name VARCHAR(255),
    Accession VARCHAR(50),
    PreferredName VARCHAR(50),
    Gene VARCHAR(500),
    Species VARCHAR(255),
    AltDescription TEXT,

    CONSTRAINT PK_PeptideGroup PRIMARY KEY (Id),
    CONSTRAINT FK_PeptideGroup_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);
CREATE INDEX IX_PeptideGroup_RunId ON targetedms.PeptideGroup(RunId);

-- ALternative proteins
CREATE TABLE targetedms.Protein
(
    Id SERIAL NOT NULL,
    PeptideGroupId INT NOT NULL,
    LabkeySequenceId INT NOT NULL,
    Name VARCHAR(50) NOT NULL,
    Description TEXT NULL ,

    CONSTRAINT PK_Protein PRIMARY KEY (Id),
    CONSTRAINT FK_Protein_PeptideGroup FOREIGN KEY (PeptideGroupId) REFERENCES targetedms.PeptideGroup(Id)
);
CREATE INDEX IX_Protein_PeptideGroupId ON targetedms.Protein(PeptideGroupId);

-- ----------------------------------------------------------------------------
-- Peptide
-- ----------------------------------------------------------------------------
CREATE TABLE targetedms.Peptide
(
    Id SERIAL NOT NULL,
    PeptideGroupId INT NOT NULL,
    Sequence VARCHAR(100),
    StartIndex INT,
    EndIndex INT,
    PreviousAa CHAR(1),
    NextAa CHAR(1),
    CalcNeutralMass DOUBLE PRECISION,
    NumMissedCleavages INT,
    Rank INTEGER,
    RtCalculatorScore REAL,
    PredictedRetentionTime REAL,
    AvgMeasuredRetentionTime REAL,
    Decoy BOOLEAN,
    Note TEXT,
    PeptideModifiedSequence VARCHAR(255),
    StandardType VARCHAR(20),
    ExplicitRetentionTime REAL,

    CONSTRAINT PK_Peptide PRIMARY KEY (Id),
    CONSTRAINT FK_Peptide_PeptideGroup FOREIGN KEY (PeptideGroupId) REFERENCES targetedms.PeptideGroup(Id)
);
CREATE INDEX IX_Peptide_Sequence ON targetedms.Peptide (Sequence);
CREATE INDEX IX_Peptide_PeptideGroupId ON targetedms.Peptide(PeptideGroupId);

CREATE TABLE targetedms.PeptideChromInfo
(
    Id SERIAL NOT NULL,
    PeptideId INT NOT NULL,
    SampleFileId INT NOT NULL,
    PeakCountRatio REAL NOT NULL,
    RetentionTime REAL,

    CONSTRAINT PK_PeptideChromInfo PRIMARY KEY (Id),
    CONSTRAINT FK_PeptideChromInfo_Peptide FOREIGN KEY (PeptideId) REFERENCES targetedms.Peptide(Id),
    CONSTRAINT FK_PeptideChromInfo_SampleFile FOREIGN KEY (SampleFileId) REFERENCES targetedms.SampleFile(Id)
);
CREATE INDEX IX_PeptideChromInfo_PeptideId ON targetedms.PeptideChromInfo(PeptideId);
CREATE INDEX IX_PeptideChromInfo_SampleFileId ON targetedms.PeptideChromInfo(SampleFileId);


-- ----------------------------------------------------------------------------
-- Precursor
-- ----------------------------------------------------------------------------
CREATE TABLE targetedms.Precursor (
    Id SERIAL NOT NULL,
    PeptideId INT  NOT NULL,
    IsotopeLabelId INT,
    Mz DOUBLE PRECISION NOT NULL,
    Charge INT NOT NULL,
    NeutralMass DOUBLE PRECISION,
    ModifiedSequence VARCHAR(300),
    CollisionEnergy REAL,
    DeclusteringPotential REAL,
    Decoy BOOLEAN,
    DecoyMassShift REAL,
    Note TEXT,
    Modified TIMESTAMP,
    -- 0 = NotRepresentative; 1 = Representative; 2 = Representative_Deprecated; 3 = Conflicted
    RepresentativeDataState INT NOT NULL DEFAULT 0,
    ExplicitCollisionEnergy REAL,
    ExplicitDriftTimeMsec REAL,
    ExplicitDriftTimeHighEnergyOffsetMsec REAL,

    CONSTRAINT PK_Precursor PRIMARY KEY (Id),
	CONSTRAINT FK_Precursor_Peptide FOREIGN KEY (PeptideId) REFERENCES targetedms.Peptide(Id)
);
CREATE INDEX IX_Precursor_PeptideId ON targetedms.Precursor(PeptideId);
CREATE INDEX IX_Precursor_IsotopeLabelId ON targetedms.Precursor(IsotopeLabelId);


CREATE TABLE targetedms.PrecursorChromInfo
(
    Id SERIAL NOT NULL,
    PrecursorId INT  NOT NULL,
    SampleFileId INT NOT NULL,
    PeptideChromInfoId INT NOT NULL,
    BestRetentionTime REAL,
    MinStartTime REAL,
    MaxEndTime REAL,
    TotalArea REAL,
    TotalAreaNormalized REAL,
    TotalBackground REAL,
    MaxFwhm REAL,
    PeakCountRatio REAL,
    NumTruncated INT,
    Identified VARCHAR(10),
    LibraryDotp REAL,
    OptimizationStep INT,
    UserSet VARCHAR(20),
    NOTE TEXT,
    Chromatogram BYTEA,
    NumTransitions INT,
    NumPoints INT,

    UncompressedSize INT,
    MaxHeight REAL,
    IsotopeDotp REAL,
    AverageMassErrorPPM REAL,

    CONSTRAINT PK_PrecursorChromInfo PRIMARY KEY (Id),
    CONSTRAINT FK_PrecursorChromInfo_Precursor FOREIGN KEY (PrecursorId) REFERENCES targetedms.Precursor(Id),
    CONSTRAINT FK_PrecursorChromInfo_SampleFile FOREIGN KEY (SampleFileId) REFERENCES targetedms.SampleFile(Id),
    CONSTRAINT FK_PrecursorChromInfo_PeptideChromInfo FOREIGN KEY (PeptideChromInfoId) REFERENCES targetedms.PeptideChromInfo(Id)
);
CREATE INDEX IX_PrecursorChromInfo_PrecursorId ON targetedms.PrecursorChromInfo(PrecursorId);
CREATE INDEX IX_PrecursorChromInfo_SampleFileId ON targetedms.PrecursorChromInfo(SampleFileId);
CREATE INDEX IX_PrecursorChromInfo_PeptideChromInfoId ON targetedms.PrecursorChromInfo(PeptideChromInfoId);

-- ----------------------------------------------------------------------------
-- Transition
-- ----------------------------------------------------------------------------
CREATE TABLE targetedms.Transition (
    Id SERIAL NOT NULL,
    PrecursorId INT NOT NULL,
    Mz DOUBLE PRECISION,
    Charge INT,
    NeutralMass DOUBLE PRECISION,
    NeutralLossMass DOUBLE PRECISION,
    FragmentType VARCHAR(20),
    FragmentOrdinal INT,
    CleavageAa CHAR(1),
    LibraryRank INT,
    LibraryIntensity REAL,
    IsotopeDistIndex INT,
    IsotopeDistRank INT,
    IsotopeDistProportion REAL,
    Decoy BOOLEAN,
    DecoyMassShift REAL,
    Note TEXT,
    massindex INT,
    MeasuredIonName VARCHAR(20),

    CONSTRAINT PK_Transition PRIMARY KEY (Id),
	CONSTRAINT FK_Transition_Precursor FOREIGN KEY (PrecursorId) REFERENCES targetedms.Precursor(Id)
);
CREATE INDEX IX_Transition_PrecursorId ON targetedms.Transition(PrecursorId);

CREATE TABLE targetedms.TransitionChromInfo
(
    Id SERIAL NOT NULL,
    TransitionId INT  NOT NULL,
    SampleFileId INT NOT NULL,
    PrecursorChromInfoId INT NOT NULL,
    RetentionTime REAL,
    StartTime REAL,
    EndTime REAL,
    Height REAL,
    Area REAL,
    AreaNormalized REAL,
    Background REAL,
    Fwhm REAL,
    FwhmDegenerate BOOLEAN,
    Truncated BOOLEAN,
    PeakRank INT,
    Identified VARCHAR(10),
    OptimizationStep INT,
    UserSet VARCHAR(20),
    NOTE TEXT,
    MassErrorPPM REAL,
    -- Remember which index within the chromatogram data we used for each TransitionChromInfo
    chromatogramindex INT,

    CONSTRAINT PK_TransitionChromInfo PRIMARY KEY (Id),
    CONSTRAINT FK_TransitionChromInfo_Transition FOREIGN KEY (TransitionId) REFERENCES targetedms.Transition(Id),
    CONSTRAINT FK_TransitionChromInfo_SampleFile FOREIGN KEY (SampleFileId) REFERENCES targetedms.SampleFile(Id),
    CONSTRAINT FK_TransitionChromInfo_PrecursorChromInfo FOREIGN KEY (PrecursorChromInfoId) REFERENCES targetedms.PrecursorChromInfo(Id)
);
CREATE INDEX IX_TransitionChromInfo_TransitionId ON targetedms.TransitionChromInfo(TransitionId);
CREATE INDEX IX_TransitionChromInfo_SampleFileId ON targetedms.TransitionChromInfo(SampleFileId);
CREATE INDEX IX_TransitionChromInfo_PrecursorChromInfoId ON targetedms.TransitionChromInfo(PrecursorChromInfoId);

-- ----------------------------------------------------------------------------
-- Enzyme
-- ----------------------------------------------------------------------------
CREATE TABLE targetedms.Enzyme
(
    Id SERIAL NOT NULL,
    Name VARCHAR(30) NOT NULL,
    Cut VARCHAR(20),
    NoCut VARCHAR(20),
    Sense CHAR(1),

    CutC VARCHAR(20),
    NoCutC VARCHAR(20),
    CutN VARCHAR(20),
    NoCutN VARCHAR(20),

    CONSTRAINT PK_Enzyme PRIMARY KEY (Id)
);

CREATE TABLE targetedms.RunEnzyme
(
    EnzymeId INT NOT NULL,
    RunId INT NOT NULL,
    MaxMissedCleavages INT,
    ExcludeRaggedEnds BOOLEAN,

    CONSTRAINT PK_RunEnzyme PRIMARY KEY (EnzymeId, RunId),
    CONSTRAINT FK_RunEnzyme_Enzyme FOREIGN KEY (EnzymeId) REFERENCES targetedms.Enzyme(Id),
    CONSTRAINT FK_RunEnzyme_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);

-- ----------------------------------------------------------------------------
-- Modifications
-- ----------------------------------------------------------------------------
CREATE TABLE targetedms.ModificationSettings
(
    RunId INT NOT NULL,
    MaxVariableMods INT NOT NULL,
    MaxNeutralLosses INT NOT NULL,

    CONSTRAINT PK_ModificationSettings PRIMARY KEY (RunId),
    CONSTRAINT FK_ModificationSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);

CREATE TABLE targetedms.IsotopeLabel
(
    Id SERIAL NOT NULL,
    RunId INT NOT NULL,
    Name VARCHAR(50) NOT NULL,
    Standard BOOLEAN NOT NULL,

    CONSTRAINT PK_IsotopeLabel PRIMARY KEY (Id),
    CONSTRAINT FK_IsotopeLabel_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);
CREATE INDEX IX_IsotopeLabel_RunId ON targetedms.IsotopeLabel (RunId);

CREATE TABLE targetedms.StructuralModification
(
    Id SERIAL NOT NULL,
    Name VARCHAR(100) NOT NULL,
    AminoAcid CHAR(30),
    Terminus CHAR(1),
    Formula VARCHAR(50),
    MassDiffMono DOUBLE PRECISION,
    MassDiffAvg DOUBLE PRECISION,
    UnimodId INTEGER,

    CONSTRAINT PK_StructuralModification PRIMARY KEY (Id)
);

CREATE TABLE targetedms.StructuralModLoss
(
    Id SERIAL NOT NULL,
    StructuralModId INT NOT NULL,
    Formula VARCHAR(50),
    MassDiffMono DOUBLE PRECISION,
    MassDiffAvg DOUBLE PRECISION,
    Inclusion VARCHAR(10),

    CONSTRAINT PK_StructuralModLoss PRIMARY KEY (Id),
    CONSTRAINT FK_StructuralModLoss_StructuralModification FOREIGN KEY (StructuralModId) REFERENCES targetedms.StructuralModification(Id)
);
CREATE INDEX IX_StructuralModification_StructuralModId ON targetedms.StructuralModLoss (StructuralModId);

CREATE TABLE targetedms.RunStructuralModification
(
    StructuralModId INT NOT NULL,
    RunId INT NOT NULL,
    ExplicitMod BOOLEAN,
    variable BOOLEAN NOT NULL DEFAULT false,

    CONSTRAINT PK_RunStructuralModification PRIMARY KEY (StructuralModId, RunId),
    CONSTRAINT FK_RunStructuralModification_StructuralModification FOREIGN KEY (StructuralModId) REFERENCES targetedms.StructuralModification(Id),
    CONSTRAINT FK_RunStructuralModification_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);
CREATE INDEX IX_RunStructuralModification_RunId ON targetedms.RunStructuralModification (RunId);

CREATE TABLE targetedms.IsotopeModification
(
    Id SERIAL NOT NULL,
    Name VARCHAR(100) NOT NULL,
    AminoAcid CHAR(1),
    Terminus CHAR(1),
    Formula VARCHAR(50),
    MassDiffMono DOUBLE PRECISION,
    MassDiffAvg DOUBLE PRECISION,
    Label13C BOOLEAN,
    Label15N BOOLEAN,
    Label18O BOOLEAN,
    Label2H BOOLEAN,
    UnimodId INTEGER,

    CONSTRAINT PK_IsotopeModification PRIMARY KEY (Id)
);

CREATE TABLE targetedms.RunIsotopeModification
(
    IsotopeModId INT NOT NULL,
    RunId INT NOT NULL,
    IsotopeLabelId INT NOT NULL,
    ExplicitMod BOOLEAN,
    RelativeRt VARCHAR(20),

    CONSTRAINT PK_RunIsotopeModification PRIMARY KEY (isotopemodid, runid, isotopelabelid),
    CONSTRAINT FK_RunIsotopeModification_IsotopeModification FOREIGN KEY (IsotopeModId) REFERENCES targetedms.IsotopeModification(Id),
    CONSTRAINT FK_RunIsotopeModification_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id),
    CONSTRAINT FK_RunIsotopeModification_IsotopeLabel FOREIGN KEY (IsotopeLabelId) REFERENCES targetedms.IsotopeLabel(Id)
);
CREATE INDEX IX_RunIsotopeModification_RunId ON targetedms.RunIsotopeModification (RunId);
CREATE INDEX IX_RunIsotopeModification_IsotopeLabelId ON targetedms.RunIsotopeModification (IsotopeLabelId);


-- ----------------------------------------------------------------------------
-- Peptide Modifications
-- ----------------------------------------------------------------------------
CREATE TABLE targetedms.PeptideStructuralModification
(
    Id SERIAL NOT NULL,
    PeptideId INT NOT NULL,
    StructuralModId INT NOT NULL,
    IndexAa INT NOT NULL,
    MassDiff DOUBLE PRECISION,

    CONSTRAINT PK_PeptideStructuralModification PRIMARY KEY (Id),
    CONSTRAINT FK_PeptideStructuralModification_Peptide FOREIGN KEY (PeptideId) REFERENCES targetedms.Peptide(Id),
    CONSTRAINT FK_PeptideStructuralModification_StructuralModification FOREIGN KEY (StructuralModId) REFERENCES targetedms.StructuralModification(Id)
);
CREATE INDEX IX_PeptideStructuralModification_PeptideId ON targetedms.PeptideStructuralModification (PeptideId);
CREATE INDEX IX_PeptideStructuralModification_StructuralModId ON targetedms.PeptideStructuralModification (StructuralModId);

CREATE TABLE targetedms.PeptideIsotopeModification
(
    Id SERIAL NOT NULL,
    PeptideId INT NOT NULL,
    IsotopeModId INT NOT NULL,
    IndexAa INT NOT NULL,
    MassDiff DOUBLE PRECISION,

    CONSTRAINT PK_PeptideIsotopeModification PRIMARY KEY (Id),
    CONSTRAINT FK_PeptideIsotopeModification_Peptide FOREIGN KEY (PeptideId) REFERENCES targetedms.Peptide(Id),
    CONSTRAINT FK_PeptideIsotopeModification_IsotopeModification FOREIGN KEY (IsotopeModId) REFERENCES targetedms.IsotopeModification(Id)
);
CREATE INDEX IX_PeptideIsotopeModification_PeptideId ON targetedms.PeptideIsotopeModification (PeptideId);
CREATE INDEX IX_PeptideIsotopeModification_IsotopeModId ON targetedms.PeptideIsotopeModification (IsotopeModId);



-- ----------------------------------------------------------------------------
-- Peak Area Ratios
-- ----------------------------------------------------------------------------
CREATE TABLE targetedms.TransitionAreaRatio
(
    Id SERIAL NOT NULL,
    TransitionChromInfoId INT NOT NULL,
    TransitionChromInfoStdId INT NOT NULL,
    IsotopeLabelId INT NOT NULL,
    IsotopeLabelStdId INT NOT NULL,
    AreaRatio REAL NOT NULL,

    CONSTRAINT PK_TransitionAreaRatio PRIMARY KEY (Id),
    CONSTRAINT FK_TransitionAreaRatio_TransitionChromInfoId FOREIGN KEY (TransitionChromInfoId) REFERENCES targetedms.TransitionChromInfo(Id),
    CONSTRAINT FK_TransitionAreaRatio_TransitionChromInfoStdId FOREIGN KEY (TransitionChromInfoStdId) REFERENCES targetedms.TransitionChromInfo(Id),
    CONSTRAINT FK_TransitionAreaRatio_IsotopeLabelId FOREIGN KEY (IsotopeLabelId) REFERENCES targetedms.IsotopeLabel(Id),
    CONSTRAINT FK_TransitionAreaRatio_IsotopeLabelStdId FOREIGN KEY (IsotopeLabelStdId) REFERENCES targetedms.IsotopeLabel(Id)
);
CREATE INDEX IX_TransitionAreaRatio_TransitionChromInfoId ON targetedms.TransitionAreaRatio (TransitionChromInfoId);
CREATE INDEX IX_TransitionAreaRatio_TransitionChromInfoStdId ON targetedms.TransitionAreaRatio (TransitionChromInfoStdId);
CREATE INDEX IX_TransitionAreaRatio_IsotopeLabelId ON targetedms.TransitionAreaRatio (IsotopeLabelId);
CREATE INDEX IX_TransitionAreaRatio_IsotopeLabelStdId ON targetedms.TransitionAreaRatio (IsotopeLabelStdId);

CREATE TABLE targetedms.PrecursorAreaRatio
(
    Id SERIAL NOT NULL,
    PrecursorChromInfoId INT NOT NULL,
    PrecursorChromInfoStdId INT NOT NULL,
    IsotopeLabelId INT NOT NULL,
    IsotopeLabelStdId INT NOT NULL,
    AreaRatio REAL NOT NULL,

    CONSTRAINT PK_PrecursorAreaRatio PRIMARY KEY (Id),
    CONSTRAINT FK_PrecursorAreaRatio_PrecursorChromInfoId FOREIGN KEY (PrecursorChromInfoId) REFERENCES targetedms.PrecursorChromInfo(Id),
    CONSTRAINT FK_PrecursorAreaRatio_PrecursorChromInfoStdId FOREIGN KEY (PrecursorChromInfoStdId) REFERENCES targetedms.PrecursorChromInfo(Id),
    CONSTRAINT FK_PrecursorAreaRatio_IsotopeLabelId FOREIGN KEY (IsotopeLabelId) REFERENCES targetedms.IsotopeLabel(Id),
    CONSTRAINT FK_PrecursorAreaRatio_IsotopeLabelStdId FOREIGN KEY (IsotopeLabelStdId) REFERENCES targetedms.IsotopeLabel(Id)
);
CREATE INDEX IX_PrecursorAreaRatio_PrecursorChromInfoId ON targetedms.PrecursorAreaRatio (PrecursorChromInfoId);
CREATE INDEX IX_PrecursorAreaRatio_PrecursorChromInfoStdId ON targetedms.PrecursorAreaRatio (PrecursorChromInfoStdId);
CREATE INDEX IX_PrecursorAreaRatio_IsotopeLabelId ON targetedms.PrecursorAreaRatio (IsotopeLabelId);
CREATE INDEX IX_PrecursorAreaRatio_IsotopeLabelStdId ON targetedms.PrecursorAreaRatio (IsotopeLabelStdId);

CREATE TABLE targetedms.PeptideAreaRatio
(
    Id SERIAL NOT NULL,
    PeptideChromInfoId INT NOT NULL,
    PeptideChromInfoStdId INT NOT NULL,
    IsotopeLabelId INT NOT NULL,
    IsotopeLabelStdId INT NOT NULL,
    AreaRatio REAL NOT NULL,

    CONSTRAINT PK_PeptideAreaRatio PRIMARY KEY (Id),
    CONSTRAINT FK_PeptideAreaRatio_PeptideChromInfoId FOREIGN KEY (PeptideChromInfoId) REFERENCES targetedms.PeptideChromInfo(Id),
    CONSTRAINT FK_PeptideAreaRatio_PeptideChromInfoStdId FOREIGN KEY (PeptideChromInfoStdId) REFERENCES targetedms.PeptideChromInfo(Id),
    CONSTRAINT FK_PeptideAreaRatio_IsotopeLabelId FOREIGN KEY (IsotopeLabelId) REFERENCES targetedms.IsotopeLabel(Id),
    CONSTRAINT FK_PeptideAreaRatio_IsotopeLabelStdId FOREIGN KEY (IsotopeLabelStdId) REFERENCES targetedms.IsotopeLabel(Id)
);
CREATE INDEX IX_PeptideAreaRatio_PeptideChromInfoId ON targetedms.PeptideAreaRatio (PeptideChromInfoId);
CREATE INDEX IX_PeptideAreaRatio_PeptideChromInfoStdId ON targetedms.PeptideAreaRatio (PeptideChromInfoStdId);
CREATE INDEX IX_PeptideAreaRatio_IsotopeLabelId ON targetedms.PeptideAreaRatio (IsotopeLabelId);
CREATE INDEX IX_PeptideAreaRatio_IsotopeLabelStdId ON targetedms.PeptideAreaRatio (IsotopeLabelStdId);



-- ----------------------------------------------------------------------------
-- Spectrum Libraries
-- ----------------------------------------------------------------------------
CREATE TABLE targetedms.LibrarySource
(
    Id SERIAL NOT NULL,
    Type VARCHAR(10) NOT NULL,  -- One of NIST, GPM or Bibliospec
    Score1Name VARCHAR(20),
    Score2Name VARCHAR(20),
    Score3Name VARCHAR(20),

    CONSTRAINT PK_LibrarySource PRIMARY KEY (Id)
);

CREATE TABLE targetedms.LibrarySettings
(
    RunId INT NOT NULL,
    Pick VARCHAR(10),
    RankType VARCHAR(20),
    PeptideCount INT,

    CONSTRAINT PK_LibrarySettings PRIMARY KEY (RunId),
    CONSTRAINT FK_LibrarySettings_RunId FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);

CREATE TABLE targetedms.SpectrumLibrary
(
    Id SERIAL NOT NULL,
    RunId INT NOT NULL,
    LibrarySourceId INT NOT NULL,
    LibraryType VARCHAR(20) NOT NULL, -- One of 'bibliospec', 'bibliospec_lite', 'xhunter', 'nist', 'spectrast'.
    Name VARCHAR(200) NOT NULL,
    FileNameHint VARCHAR(100),
    SkylineLibraryId VARCHAR(200),
    Revision VARCHAR(10),

    CONSTRAINT PK_SpectrumLibrary PRIMARY KEY (Id),
    CONSTRAINT FK_SpectrumLibrary_RunId FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id),
    CONSTRAINT FK_SpectrumLibrary_LibrarySourceId FOREIGN KEY (LibrarySourceId) REFERENCES targetedms.LibrarySource(Id)
);
CREATE INDEX IX_SpectrumLibrary_RunId ON targetedms.SpectrumLibrary (RunId);
CREATE INDEX IX_SpectrumLibrary_LibrarySourceId ON targetedms.SpectrumLibrary (LibrarySourceId);



-- ----------------------------------------------------------------------------
-- Transition related tables
-- ----------------------------------------------------------------------------
CREATE TABLE targetedms.TransitionOptimization
(
     Id SERIAL NOT NULL,
     TransitionId INT NOT NULL,
     OptimizationType VARCHAR(10) NOT NULL,
     OptValue REAL NOT NULL,

     CONSTRAINT PK_TransitionOptimization PRIMARY KEY (Id),
     CONSTRAINT FK_TransitionOptimization_TransitionId FOREIGN KEY (TransitionId) REFERENCES targetedms.Transition(Id)
);
CREATE INDEX IX_TransitionOptimization_TransitionId ON targetedms.TransitionOptimization (TransitionId);

CREATE TABLE targetedms.TransitionLoss
(
    Id SERIAL NOT NULL,
    TransitionId INT NOT NULL,
    StructuralModLossId INT NOT NULL,

    CONSTRAINT PK_TransitionLoss PRIMARY KEY (Id),
    CONSTRAINT FK_TransitionLoss_TransitionId FOREIGN KEY (TransitionId) REFERENCES targetedms.Transition(Id),
    CONSTRAINT FK_TransitionLoss_StructuralModLossId FOREIGN KEY (StructuralModLossId) REFERENCES targetedms.StructuralModLoss(Id)
);
CREATE INDEX IX_TransitionLoss_TransitionId ON targetedms.TransitionLoss (TransitionId);
CREATE INDEX IX_TransitionLoss_StructuralModLossId ON targetedms.TransitionLoss (StructuralModLossId);

-- Add IsotopeLabelId FK
ALTER TABLE targetedms.Precursor ADD CONSTRAINT FK_Precursor_IsotopeLabel FOREIGN KEY (IsotopeLabelId) REFERENCES targetedms.IsotopeLabel(Id);

INSERT INTO targetedms.LibrarySource (type, score1name) VALUES ('BiblioSpec', 'count_measured');
INSERT INTO targetedms.LibrarySource (type, score1name, score2name) VALUES ('GPM', 'expect', 'processed_intensity');
INSERT INTO targetedms.LibrarySource (type, score1name, score2name, score3name) VALUES ('NIST', 'expect', 'total_intensity', 'tfratio');

CREATE TABLE targetedms.PrecursorLibInfo
(
    Id SERIAL NOT NULL,
    PrecursorId INT NOT NULL,
    SpectrumLibraryId INT NOT NULL,
    Score1 REAL,
    Score2 REAL,
    Score3 REAL,

    CONSTRAINT PK_PrecursorLibInfo PRIMARY KEY (Id),
    CONSTRAINT FK_PrecursorLibInfo_Precursor FOREIGN KEY (PrecursorId) REFERENCES targetedms.Precursor(Id),
    CONSTRAINT FK_PrecursorLibInfo_SpectrumLibrary FOREIGN KEY (SpectrumLibraryId) REFERENCES targetedms.SpectrumLibrary(Id)
);
CREATE INDEX IX_PrecursorLibInfo_PrecursorId ON targetedms.PrecursorLibInfo(PrecursorId);
CREATE INDEX IX_PrecursorLibInfo_SpectrumLibraryId ON targetedms.PrecursorLibInfo(SpectrumLibraryId);

CREATE TABLE targetedms.PeptideGroupAnnotation
(
    Id SERIAL NOT NULL,
    PeptideGroupId INT NOT NULL,
    Name VARCHAR(255) NOT NULL,
    Value VARCHAR(255) NOT NULL,

    CONSTRAINT PK_PeptideGroupAnnotation PRIMARY KEY (Id),
    CONSTRAINT FK_PeptideGroupAnnotation_PeptideGroup FOREIGN KEY (PeptideGroupId) REFERENCES targetedms.PeptideGroup(Id),
    CONSTRAINT UQ_PeptideGroupAnnotation_Name_PeptideGroup UNIQUE (Name, PeptideGroupId)
);

CREATE TABLE targetedms.PrecursorAnnotation
(
    Id SERIAL NOT NULL,
    PrecursorId INT NOT NULL,
    Name VARCHAR(255) NOT NULL,
    Value VARCHAR(255) NOT NULL,

    CONSTRAINT PK_PrecursorAnnotation PRIMARY KEY (Id),
    CONSTRAINT FK_PrecursorAnnotation_Precursor FOREIGN KEY (PrecursorId) REFERENCES targetedms.Precursor(Id),
    CONSTRAINT UQ_PrecursorAnnotation_Name_Precursor UNIQUE (Name, PrecursorId)
);

CREATE TABLE targetedms.PrecursorChromInfoAnnotation
(
    Id SERIAL NOT NULL,
    PrecursorChromInfoId INT NOT NULL,
    Name VARCHAR(255) NOT NULL,
    Value VARCHAR(255) NOT NULL,

    CONSTRAINT PK_PrecursorChromInfoAnnotation PRIMARY KEY (Id),
    CONSTRAINT FK_PrecursorChromInfoAnnotation_PrecursorChromInfo FOREIGN KEY (PrecursorChromInfoId) REFERENCES targetedms.PrecursorChromInfo(Id),
    CONSTRAINT UQ_PrecursorChromInfoAnnotation_Name_PrecursorChromInfo UNIQUE (Name, PrecursorChromInfoId)
);

CREATE TABLE targetedms.TransitionAnnotation
(
    Id SERIAL NOT NULL,
    TransitionId INT NOT NULL,
    Name VARCHAR(255) NOT NULL,
    Value VARCHAR(255) NOT NULL,

    CONSTRAINT PK_TransitionAnnotation PRIMARY KEY (Id),
    CONSTRAINT FK_TransitionAnnotation_Transition FOREIGN KEY (TransitionId) REFERENCES targetedms.Transition(Id),
    CONSTRAINT UQ_TransitionAnnotation_Name_Transition UNIQUE (Name, TransitionId)
);

CREATE TABLE targetedms.TransitionChromInfoAnnotation
(
    Id SERIAL NOT NULL,
    TransitionChromInfoId INT NOT NULL,
    Name VARCHAR(255) NOT NULL,
    Value VARCHAR(255) NOT NULL,

    CONSTRAINT PK_TransitionChromInfoAnnotation PRIMARY KEY (Id),
    CONSTRAINT FK_TransitionChromInfoAnnotation_TransitionChromInfo FOREIGN KEY (TransitionChromInfoId) REFERENCES targetedms.TransitionChromInfo(Id),
    CONSTRAINT UQ_TransitionChromInfoAnnotation_Name_TransitionChromInfo UNIQUE (Name, TransitionChromInfoId)
);

CREATE TABLE targetedms.PeptideAnnotation
(
    Id SERIAL NOT NULL,
    PeptideId INT NOT NULL,
    Name VARCHAR(255) NOT NULL,
    Value VARCHAR(255) NOT NULL,

    CONSTRAINT PK_PeptideAnnotation PRIMARY KEY (Id),
    CONSTRAINT FK_PeptideAnnotation_Peptide FOREIGN KEY (PeptideId) REFERENCES targetedms.Peptide(Id),
    CONSTRAINT UQ_PeptideAnnotation_Name_Peptide UNIQUE (Name, PeptideId)
);

CREATE TABLE targetedms.PredictorSettings
(
    Id SERIAL NOT NULL,
    PredictorId INT NOT NULL,
    Charge INT,
    Slope REAL,
    Intercept REAL,

    CONSTRAINT PK_PredictorSettings PRIMARY KEY (Id),
    CONSTRAINT UQ_PredictorSettings UNIQUE (PredictorId, Charge),
    CONSTRAINT FK_PredictorSettings_PredictorId FOREIGN KEY (PredictorId) REFERENCES targetedms.Predictor(Id)
);

/* targetedms-12.20-12.30.sql */

CREATE TABLE targetedms.ReplicateAnnotation
(
    Id SERIAL NOT NULL,
    ReplicateId INT NOT NULL,
    Name VARCHAR(255) NOT NULL,
    Value VARCHAR(255) NOT NULL,

    CONSTRAINT PK_ReplicateAnnotation PRIMARY KEY (Id),
    CONSTRAINT FK_ReplicateAnnotation_Replicate FOREIGN KEY (ReplicateId) REFERENCES targetedms.Replicate(Id),
    CONSTRAINT UQ_ReplicateAnnotation_Name_Repicate UNIQUE (Name, ReplicateId)
);


-- AnnotationSettings table to store annotation settings.
-- Name: Name of the annotation
-- Targets: Comma-separated list of one or more of protein, peptide, precursor, transition, replicate, precursor_result, transition_result
-- Type:  One of text, number, true_false, value_list
CREATE TABLE targetedms.AnnotationSettings
(
    Id SERIAL NOT NULL,
    RunId INT NOT NULL,
    Name VARCHAR(255) NOT NULL,
    Targets VARCHAR(255),
    Type VARCHAR(20),

    CONSTRAINT PK_AnnotationSettings PRIMARY KEY (Id),
    CONSTRAINT FK_AnnotationSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);
CREATE INDEX IX_AnnotationSettings_RunId ON targetedms.AnnotationSettings (RunId);

/* targetedms-12.30-13.10.sql */

-- Add indices on annotation tables to speed up deletes
CREATE INDEX IX_PrecursorChromInfoAnnotation_PrecursorChromInfoId ON targetedms.PrecursorChromInfoAnnotation(PrecursorChromInfoId);

CREATE INDEX IX_TransitionChromInfoAnnotation_TransitionChromInfoId ON targetedms.TransitionChromInfoAnnotation(TransitionChromInfoId);

CREATE INDEX IX_PeptideAnnotation_PeptideId ON targetedms.PeptideAnnotation(PeptideId);

CREATE INDEX IX_PrecursorAnnotation_PrecursorId ON targetedms.PrecursorAnnotation(PrecursorId);

CREATE INDEX IX_PeptideGroupAnnotation_PeptideGroupId ON targetedms.PeptideGroupAnnotation(PeptideGroupId);

CREATE INDEX IX_TransitionAnnotation_TransitionId ON targetedms.TransitionAnnotation(TransitionId);

/* targetedms-13.10-13.20.sql */


-- iRTPeptide table to store iRT peptide information.
-- ModifiedSequence: the optionally chemically modified peptide sequence
CREATE TABLE targetedms.iRTPeptide
(
    Id SERIAL NOT NULL,
    ModifiedSequence VARCHAR(100) NOT NULL,
    iRTStandard BOOLEAN NOT NULL,
    iRTValue FLOAT NOT NULL,
    iRTScaleId INT NOT NULL,
    Created TIMESTAMP,
    CreatedBy INT,
    ImportCount INT NOT NULL,
    TimeSource INT,

    CONSTRAINT PK_iRTPeptide PRIMARY KEY (Id),
    CONSTRAINT FK_iRTPeptide_iRTScaleId FOREIGN KEY (iRTScaleId) REFERENCES targetedms.iRTScale(Id)
);
CREATE INDEX IX_iRTPeptide_iRTScaleId ON targetedms.iRTPeptide (iRTScaleId);
ALTER TABLE targetedms.iRTPeptide ADD CONSTRAINT UQ_iRTPeptide_SequenceAndScale UNIQUE (irtScaleId, ModifiedSequence);

CREATE TABLE targetedms.ExperimentAnnotations
(
    -- standard fields
    _ts TIMESTAMP,
    Id SERIAL NOT NULL,
    Container ENTITYID NOT NULL,
    CreatedBy USERID,
    Created TIMESTAMP,
    ModifiedBy USERID,
    Modified TIMESTAMP,

    Title VARCHAR,
    Organism VARCHAR(100),
    ExperimentDescription TEXT,
    SampleDescription TEXT,
    Instrument VARCHAR(250),
    SpikeIn BOOLEAN,
    Citation TEXT,
    Abstract TEXT,
    PublicationLink TEXT,
    ExperimentId INT NOT NULL DEFAULT 0,
    JournalCopy BOOLEAN NOT NULL DEFAULT FALSE,
    IncludeSubfolders BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT PK_ExperimentAnnotations PRIMARY KEY (Id)
);
CREATE INDEX IX_ExperimentAnnotations_Container ON targetedms.ExperimentAnnotations (Container);
CREATE INDEX IX_ExperimentAnnotations_ExperimentId ON targetedms.ExperimentAnnotations(ExperimentId);

ALTER TABLE targetedms.ExperimentAnnotations ADD CONSTRAINT FK_ExperimentAnnotations_Experiment FOREIGN KEY (ExperimentId) REFERENCES exp.Experiment(RowId);
ALTER TABLE targetedms.ExperimentAnnotations ADD CONSTRAINT FK_ExperimentAnnotations_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId);

/* targetedms-14.10-14.20.sql */

CREATE TABLE targetedms.IsolationScheme
(
    Id SERIAL NOT NULL,
    RunId INT NOT NULL,
    Name VARCHAR(100) NOT NULL,
    PrecursorFilter REAL,
    PrecursorLeftFilter REAL,
    PrecursorRightFilter REAL,
    SpecialHandling VARCHAR(50), -- Can be one of "Multiplexed", "MSe", "All Ions", "Overlap", "Overlap Multiplexed". Any others?
    WindowsPerScan INT,

    CONSTRAINT PK_IsolationScheme PRIMARY KEY (Id),
    CONSTRAINT FK_IsolationScheme_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);
CREATE INDEX IX_IsolationScheme_RunId ON targetedms.IsolationScheme (RunId);

CREATE TABLE targetedms.IsolationWindow
(
    Id SERIAL NOT NULL,
    IsolationSchemeId INT NOT NULL,
    WindowStart REAL NOT NULL,
    WindowEnd REAL NOT NULL,
    Target REAL,
    MarginLeft REAL,
    MarginRight REAL,
    Margin REAL,

    CONSTRAINT PK_IsolationWindow PRIMARY KEY (Id),
    CONSTRAINT FK_IsolationWindow_IsolationScheme FOREIGN KEY (IsolationSchemeId) REFERENCES targetedms.IsolationScheme(Id)
);
CREATE INDEX IX_IsolationWindow_IsolationSchemeId ON targetedms.IsolationWindow (IsolationSchemeId);

ALTER TABLE targetedms.PeptideGroup ADD CONSTRAINT FK_PeptideGroup_Sequences FOREIGN KEY(SequenceId) REFERENCES prot.Sequences (seqid);
CREATE INDEX IX_PeptideGroup_SequenceId ON targetedms.PeptideGroup(SequenceId);
CREATE INDEX IX_PeptideGroup_Label ON targetedms.PeptideGroup(Label);

CREATE INDEX IX_ReplicateAnnotation_ReplicateId ON targetedms.ReplicateAnnotation (ReplicateId);

CREATE INDEX IX_RunEnzyme_RunId ON targetedms.RunEnzyme(RunId);

CREATE INDEX IX_SampleFile_InstrumentId ON targetedms.SampleFile(InstrumentId);


-- Add ion mobility settings tables
CREATE TABLE targetedms.DriftTimePredictionSettings
(
    Id SERIAL NOT NULL,
    RunId INT NOT NULL,
    UseSpectralLibraryDriftTimes BOOLEAN,
    SpectralLibraryDriftTimesResolvingPower REAL,
    PredictorName VARCHAR(200),
    ResolvingPower REAL,

    CONSTRAINT PK_DriftTimePredictionSettings PRIMARY KEY (Id),
    CONSTRAINT FK_DriftTimePredictionSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);
CREATE INDEX IX_DriftTimePredictionSettings_RunId ON targetedms.DriftTimePredictionSettings(RunId);

CREATE TABLE targetedms.MeasuredDriftTime
(
    Id SERIAL NOT NULL,
    DriftTimePredictionSettingsId INT NOT NULL,
    ModifiedSequence VARCHAR(255) NOT NULL,
    Charge INT NOT NULL,
    DriftTime REAL NOT NULL,
    HighEnergyDriftTimeOffset REAL,

    CONSTRAINT PK_MeasuredDriftTime PRIMARY KEY (Id),
    CONSTRAINT FK_MeasuredDriftTime_DriftTimePredictionSettings FOREIGN KEY (DriftTimePredictionSettingsId) REFERENCES targetedms.DriftTimePredictionSettings(Id)
);
CREATE INDEX IX_MeasuredDriftTime_DriftTimePredictionSettingsId ON targetedms.MeasuredDriftTime(DriftTimePredictionSettingsId);

/* targetedms-14.20-14.30.sql */

CREATE TABLE targetedms.Journal
(
    _ts TIMESTAMP,
    CreatedBy USERID,
    Created TIMESTAMP,
    ModifiedBy USERID,
    Modified TIMESTAMP,

    Id SERIAL NOT NULL,
    Name VARCHAR(255) NOT NULL,
    LabkeyGroupId INT NOT NULL,
    Project EntityId NOT NULL,

    CONSTRAINT PK_Journal PRIMARY KEY (Id),
    CONSTRAINT UQ_Journal_Name UNIQUE(Name),
    CONSTRAINT FK_Journal_Principals FOREIGN KEY (LabkeyGroupId) REFERENCES core.Principals(UserId),
    CONSTRAINT FK_Journal_Containers FOREIGN KEY (Project) REFERENCES core.Containers(EntityId)
);
CREATE INDEX IX_Journal_LabkeyGroupId ON targetedms.Journal(LabkeyGroupId);
CREATE INDEX IX_Journal_Project ON targetedms.Journal(Project);

CREATE TABLE targetedms.JournalExperiment
(
    _ts TIMESTAMP,
    CreatedBy USERID,
    Created TIMESTAMP,

    JournalId INT NOT NULL,
    ExperimentAnnotationsId INT NOT NULL,
    ShortAccessURL EntityId NOT NULL,
    ShortCopyURL EntityId NOT NULL,
    Copied TIMESTAMP,


    CONSTRAINT PK_JournalExperiment PRIMARY KEY (JournalId, ExperimentAnnotationsId),
    CONSTRAINT FK_JournalExperiment_Journal FOREIGN KEY (JournalId) REFERENCES targetedms.Journal(Id),
    CONSTRAINT FK_JournalExperiment_ExperimentAnnotations FOREIGN KEY (ExperimentAnnotationsId) REFERENCES targetedms.ExperimentAnnotations(Id),
    CONSTRAINT FK_JournalExperiment_ShortUrl_Access FOREIGN KEY (ShortAccessURL) REFERENCES core.ShortUrl(EntityId),
    CONSTRAINT FK_JournalExperiment_ShortUrl_Copy FOREIGN KEY (ShortCopyURL) REFERENCES core.ShortUrl(EntityId)
);
CREATE INDEX IX_JournalExperiment_ShortAccessURL ON targetedms.JournalExperiment(ShortAccessURL);
CREATE INDEX IX_JournalExperiment_ShortCopyURL ON targetedms.JournalExperiment(ShortCopyURL);


/* targetedms-14.30-15.10.sql */

CREATE TABLE targetedms.QCAnnotationType
(
    Id SERIAL NOT NULL,
    Container ENTITYID NOT NULL,
    CreatedBy USERID,
    Created TIMESTAMP,
    ModifiedBy USERID,
    Modified TIMESTAMP,
    Name VARCHAR(100) NOT NULL,
    Description TEXT,
    Color VARCHAR(6) NOT NULL,

    CONSTRAINT PK_QCAnnotationType PRIMARY KEY (Id),
    CONSTRAINT FK_QCAnnotationType_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId),
    CONSTRAINT UQ_QCAnnotationType_ContainerName UNIQUE (Container, Name)
);

CREATE TABLE targetedms.QCAnnotation
(
    Id SERIAL NOT NULL,
    Container ENTITYID NOT NULL,
    CreatedBy USERID,
    Created TIMESTAMP,
    ModifiedBy USERID,
    Modified TIMESTAMP,
    QCAnnotationTypeId INT NOT NULL,
    Description TEXT NOT NULL,
    Date TIMESTAMP NOT NULL,

    CONSTRAINT PK_QCAnnotation PRIMARY KEY (Id),
    CONSTRAINT FK_QCAnnotation_QCAnnotationType FOREIGN KEY (QCAnnotationTypeId) REFERENCES targetedms.QCAnnotationType(Id)
);

-- Poke a few rows into the /Shared project
SELECT core.executeJavaUpgradeCode('populateDefaultAnnotationTypes');

-- ----------------------------------------------------------------------------
-- Molecule
-- ----------------------------------------------------------------------------
CREATE TABLE targetedms.Molecule
(
  PeptideId INT NOT NULL,
  IonFormula VARCHAR(100),
  CustomIonName VARCHAR(100),
  MassMonoisotopic DOUBLE PRECISION NOT NULL,
  MassAverage DOUBLE PRECISION NOT NULL,

  CONSTRAINT PK_Molecule PRIMARY KEY (PeptideId),
  CONSTRAINT FK_Molecule_Peptide FOREIGN KEY (PeptideId) REFERENCES targetedms.Peptide(Id)
);

-- ----------------------------------------------------------------------------
-- MoleculeTransition
-- ----------------------------------------------------------------------------
CREATE TABLE targetedms.MoleculeTransition (
  TransitionId INT NOT NULL,
  IonFormula VARCHAR(100),
  CustomIonName VARCHAR(100),
  MassMonoisotopic DOUBLE PRECISION NOT NULL,
  MassAverage DOUBLE PRECISION NOT NULL,

  CONSTRAINT PK_MoleculeTransition PRIMARY KEY (TransitionId),
  CONSTRAINT FK_MoleculeTransition_Transition FOREIGN KEY (TransitionId) REFERENCES targetedms.Transition(Id)
);

/* targetedms-15.10-15.20.sql */

CREATE TABLE targetedms.GuideSet
(
  RowId SERIAL NOT NULL,
  Container ENTITYID NOT NULL,
  CreatedBy USERID,
  Created TIMESTAMP,
  ModifiedBy USERID,
  Modified TIMESTAMP,
  TrainingStart TIMESTAMP NOT NULL,
  TrainingEnd TIMESTAMP NOT NULL,
  Comment TEXT,

  CONSTRAINT PK_GuideSet PRIMARY KEY (RowId)
);

/* targetedms-15.30-16.10.sql */

CREATE TABLE targetedms.AutoQCPing (
  CreatedBy USERID,
  Created TIMESTAMP,
  ModifiedBy USERID,
  Modified TIMESTAMP,
  Container ENTITYID NOT NULL,

  CONSTRAINT PK_AutoQCPing PRIMARY KEY (Container)
);

ALTER TABLE targetedms.PrecursorChromInfo DROP COLUMN TotalAreaNormalized;
ALTER TABLE targetedms.TransitionChromInfo DROP COLUMN AreaNormalized;

/* New Data model as per 'Small molecule support' spec */

/* Rename Peptide table to GeneralMolecule table */
ALTER TABLE targetedms.Peptide RENAME TO GeneralMolecule;

/* Create a new Peptide table */
CREATE TABLE targetedms.Peptide
(
  Id INT NOT NULL,
  Sequence VARCHAR(100),
  StartIndex INT,
  EndIndex INT,
  PreviousAa CHAR(1),
  NextAa CHAR(1),
  CalcNeutralMass DOUBLE PRECISION NOT NULL,
  NumMissedCleavages INT NOT NULL,
  Rank INTEGER,
  Decoy BOOLEAN,
  PeptideModifiedSequence VARCHAR(255),
  StandardType VARCHAR(20),
  CONSTRAINT PK_PeptideId PRIMARY KEY (Id)
);

ALTER TABLE targetedms.Peptide ADD CONSTRAINT FK_Id_GMId FOREIGN KEY (Id) REFERENCES targetedms.GeneralMolecule (Id);

/* Migrate data from GeneralMolecule table to newly created Peptide table */
INSERT INTO targetedms.Peptide(
  Id,
  Sequence,
  StartIndex,
  EndIndex,
  PreviousAa,
  NextAa,
  CalcNeutralMass,
  NumMissedCleavages,
  Rank,
  Decoy,
  PeptideModifiedSequence,
  StandardType)
  (SELECT
     gm.Id,
     gm.Sequence,
     gm.StartIndex,
     gm.EndIndex,
     gm.PreviousAa,
     gm.NextAa,
     gm.CalcNeutralMass,
     gm.NumMissedCleavages,
     gm.Rank,
     gm.Decoy,
     gm.PeptideModifiedSequence,
     gm.StandardType
   FROM targetedms.GeneralMolecule gm
   WHERE gm.Sequence IS NOT NULL); -- Sequence will be NULL for pre-existing small molecule data in the GeneralPrecursor table;
                                   -- Can't use PeptideModifiedSequence column here because it was added in 13.3

ALTER TABLE targetedms.GeneralMolecule DROP COLUMN Sequence;
ALTER TABLE targetedms.GeneralMolecule DROP COLUMN StartIndex;
ALTER TABLE targetedms.GeneralMolecule DROP COLUMN EndIndex;
ALTER TABLE targetedms.GeneralMolecule DROP COLUMN PreviousAa;
ALTER TABLE targetedms.GeneralMolecule DROP COLUMN NextAa;
ALTER TABLE targetedms.GeneralMolecule DROP COLUMN CalcNeutralMass;
ALTER TABLE targetedms.GeneralMolecule DROP COLUMN NumMissedCleavages;
ALTER TABLE targetedms.GeneralMolecule DROP COLUMN Rank;
ALTER TABLE targetedms.GeneralMolecule DROP COLUMN Decoy;
ALTER TABLE targetedms.GeneralMolecule DROP COLUMN PeptideModifiedSequence;
ALTER TABLE targetedms.GeneralMolecule DROP COLUMN StandardType;

ALTER TABLE targetedms.GeneralMolecule DROP CONSTRAINT PK_Peptide CASCADE;
CREATE INDEX IX_Peptide_Sequence ON targetedms.Peptide (Sequence);
ALTER TABLE targetedms.GeneralMolecule ADD CONSTRAINT PK_GMId PRIMARY KEY (Id);

/** Alter Molecule Table **/
ALTER TABLE targetedms.Molecule RENAME COLUMN PeptideId To Id;
ALTER TABLE targetedms.Molecule ADD CONSTRAINT FK_Molecule_Id FOREIGN KEY (Id) REFERENCES targetedms.GeneralMolecule(Id);

/** Rename PeptideChromInfo table to GeneralChromInfo **/
ALTER TABLE targetedms.PeptideChromInfo RENAME TO GeneralMoleculeChromInfo;

/** Modify GeneralChromInfo table to reference GeneralMolecule **/
ALTER TABLE targetedms.GeneralMoleculeChromInfo RENAME COLUMN PeptideId TO GeneralMoleculeId;
ALTER TABLE targetedms.GeneralMoleculeChromInfo ADD CONSTRAINT FK_ChromInfo_GMId FOREIGN KEY (GeneralMoleculeId) REFERENCES targetedms.GeneralMolecule(Id);
ALTER INDEX targetedms.PK_PeptideChromInfo RENAME TO PK_GMChromInfoId;
CREATE INDEX IX_GeneralMoleculeChromInfo_GMId ON targetedms.GeneralMoleculeChromInfo(GeneralMoleculeId);

/** Rename PeptideAnnotation table to GeneralMoleculeAnnotation **/
ALTER TABLE targetedms.PeptideAnnotation RENAME TO GeneralMoleculeAnnotation;

/** Modify GeneralMoleculeAnnotation table to reference GeneralMolecule **/
ALTER TABLE targetedms.GeneralMoleculeAnnotation RENAME COLUMN PeptideId TO GeneralMoleculeId;
ALTER TABLE targetedms.GeneralMoleculeAnnotation ADD CONSTRAINT FK_GMAnnotation_GMId FOREIGN KEY (GeneralMoleculeId) REFERENCES targetedms.GeneralMolecule(Id);
CREATE INDEX IX_GeneralMoleculeAnnotation_GeneralMoleculeId ON targetedms.GeneralMoleculeAnnotation(GeneralMoleculeId);

-- /* Rename Precursor table to GeneralPrecursor */
ALTER TABLE targetedms.Precursor RENAME TO GeneralPrecursor;
ALTER TABLE targetedms.GeneralPrecursor RENAME COLUMN PeptideId TO GeneralMoleculeId;
ALTER TABLE targetedms.GeneralPrecursor ADD CONSTRAINT FK_GeneralPrecursor_GMId FOREIGN KEY (GeneralMoleculeId) REFERENCES targetedms.GeneralMolecule(Id);
CREATE INDEX IX_Precursor_GMId ON targetedms.GeneralPrecursor (GeneralMoleculeId);

/* Create a new Precursor table */
CREATE TABLE targetedms.Precursor
(
  Id INT NOT NULL,
  IsotopeLabelId INT,
  NeutralMass DOUBLE PRECISION NOT NULL,
  ModifiedSequence VARCHAR(300) NOT NULL,
  DecoyMassShift REAL,

  CONSTRAINT PK_Precursor_Id PRIMARY KEY (Id),
  CONSTRAINT FK_Precursor_Id FOREIGN KEY (Id) REFERENCES targetedms.GeneralPrecursor(Id),
  CONSTRAINT FK_Precursor_IsotopeLabel FOREIGN KEY (IsotopeLabelId) REFERENCES targetedms.IsotopeLabel (Id)
);

CREATE INDEX IX_Precursor_Id ON targetedms.Precursor(Id);

/* Migrate data from (renamed) GeneralPrecursor table to newly created Precursor table */
INSERT INTO targetedms.Precursor(
  Id,
  IsotopeLabelId,
  NeutralMass,
  ModifiedSequence,
  DecoyMassShift) (SELECT
                     gp.Id,
                     gp.IsotopeLabelId,
                     gp.NeutralMass,
                     gp.ModifiedSequence,
                     gp.DecoyMassShift FROM targetedms.GeneralPrecursor gp
                     WHERE gp.ModifiedSequence IS NOT NULL); -- ModifiedSequence will be NULL for pre-existing small molecule data in the GeneralPrecursor table


/* Modify GeneralPrecursor table */
ALTER TABLE targetedms.GeneralPrecursor DROP COLUMN IsotopeLabelId;
ALTER TABLE targetedms.GeneralPrecursor DROP COLUMN NeutralMass;
ALTER TABLE targetedms.GeneralPrecursor DROP COLUMN ModifiedSequence;
ALTER TABLE targetedms.GeneralPrecursor DROP COLUMN DecoyMassShift;

/* Create table MoleculePrecursor */
CREATE TABLE targetedms.MoleculePrecursor
(
  Id integer NOT NULL,

  CONSTRAINT PK_MoleculePrecursorId PRIMARY KEY (Id),
  CONSTRAINT FK_Id FOREIGN KEY (Id) REFERENCES targetedms.GeneralPrecursor (Id)
);

/* Modify PrecursorChromInfo, PrecursorAnnotation, PrecursorLibInfo */
ALTER TABLE targetedms.PrecursorChromInfo ADD CONSTRAINT FK_PrecursorChromInfo_PrecursorId FOREIGN KEY (PrecursorId) REFERENCES targetedms.GeneralPrecursor(Id);
ALTER TABLE targetedms.PrecursorChromInfo DROP CONSTRAINT FK_PrecursorChromInfo_Precursor;

ALTER TABLE targetedms.PrecursorAnnotation ADD CONSTRAINT FK_PrecursorAnnotation_PrecursorId FOREIGN KEY (PrecursorId) REFERENCES targetedms.GeneralPrecursor(Id);
ALTER TABLE targetedms.PrecursorAnnotation DROP CONSTRAINT FK_PrecursorAnnotation_Precursor;

ALTER TABLE targetedms.PrecursorLibInfo ADD CONSTRAINT FK_PrecursorLibInfo_PrecursorId FOREIGN KEY (PrecursorId) REFERENCES targetedms.GeneralPrecursor(Id);
ALTER TABLE targetedms.PrecursorLibInfo DROP CONSTRAINT FK_PrecursorLibInfo_Precursor;

/* Rename Transition table to GeneralTransition */
ALTER TABLE targetedms.Transition RENAME TO GeneralTransition;

/* Modify GeneralTransition table to reference the GeneralPrecursor table */
ALTER TABLE targetedms.GeneralTransition RENAME COLUMN PrecursorId TO GeneralPrecursorId;
ALTER TABLE targetedms.GeneralTransition DROP CONSTRAINT FK_Transition_Precursor;
ALTER TABLE targetedms.GeneralTransition ADD CONSTRAINT FK_GeneralTransition_GPId FOREIGN KEY (GeneralPrecursorId) REFERENCES targetedms.GeneralPrecursor(Id);

/* Create a new Transition Table */
CREATE TABLE targetedms.Transition
(
  Id INT NOT NULL,
  NeutralMass double precision,
  NeutralLossMass double precision,
  FragmentOrdinal integer,
  CleavageAa character(1),
  LibraryRank integer,
  LibraryIntensity real,
  DecoyMassShift real,
  MeasuredIonName character varying(20),
  CONSTRAINT PK_Transition_Id PRIMARY KEY (Id),
  CONSTRAINT FK_Transition_Id FOREIGN KEY (Id) REFERENCES targetedms.GeneralTransition(Id)
);

/* Migrate data from (renamed) GeneralTransition to newly created Transition table */
INSERT INTO targetedms.Transition(
  Id,
  NeutralMass,
  NeutralLossMass,
  FragmentOrdinal,
  CleavageAa,
  LibraryRank,
  LibraryIntensity,
  DecoyMassShift,
  MeasuredIonName) (SELECT
                      gt.Id,
                      gt.NeutralMass,
                      gt.NeutralLossMass,
                      gt.FragmentOrdinal,
                      gt.CleavageAa,
                      gt.LibraryRank,
                      gt.LibraryIntensity,
                      gt.DecoyMassShift,
                      gt.MeasuredIonName FROM targetedms.GeneralTransition gt);

/* Drop columns from GeneralTransition table */
ALTER TABLE targetedms.GeneralTransition DROP COLUMN NeutralMass;
ALTER TABLE targetedms.GeneralTransition DROP COLUMN NeutralLossMass;
ALTER TABLE targetedms.GeneralTransition DROP COLUMN FragmentOrdinal;
ALTER TABLE targetedms.GeneralTransition DROP COLUMN CleavageAa;
ALTER TABLE targetedms.GeneralTransition DROP COLUMN LibraryRank;
ALTER TABLE targetedms.GeneralTransition DROP COLUMN LibraryIntensity;
ALTER TABLE targetedms.GeneralTransition DROP COLUMN DecoyMassShift;
ALTER TABLE targetedms.GeneralTransition DROP COLUMN MeasuredIonName;

/* Add new columns to GeneralTransition table as per spec */
ALTER TABLE targetedms.GeneralTransition ADD COLUMN ExplicitCollisionEnergy DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralTransition ADD COLUMN SLens DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralTransition ADD COLUMN ConeVoltage DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralTransition ADD COLUMN ExplicitCompensationVoltage DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralTransition ADD COLUMN ExplicitDeclusteringPotential DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralTransition ADD COLUMN ExplicitDriftTimeMSec DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralTransition ADD COLUMN ExplicitDriftTimeHighEnergyOffsetMSec DOUBLE PRECISION;

/* Modify MoleculeTransition, TransitionChromInfo, TransitionAnnotation to reference the GeneralTransition table */
ALTER TABLE targetedms.MoleculeTransition ADD CONSTRAINT FK_MoleculeTransition_GTId FOREIGN KEY (TransitionId) REFERENCES targetedms.GeneralTransition(Id);
ALTER TABLE targetedms.MoleculeTransition DROP CONSTRAINT FK_MoleculeTransition_Transition;

ALTER TABLE targetedms.TransitionChromInfo ADD CONSTRAINT FK_TransitionChromInfo_GTId FOREIGN KEY (TransitionId) REFERENCES targetedms.GeneralTransition(Id);
ALTER TABLE targetedms.TransitionChromInfo DROP CONSTRAINT FK_TransitionChromInfo_Transition;

ALTER TABLE targetedms.TransitionAnnotation ADD CONSTRAINT FK_TransitionAnnotation_GTId FOREIGN KEY (TransitionId) REFERENCES targetedms.GeneralTransition(Id);
ALTER TABLE targetedms.TransitionAnnotation DROP CONSTRAINT FK_TransitionAnnotation_Transition;

ALTER TABLE targetedms.GeneralMoleculeChromInfo DROP CONSTRAINT FK_PeptideChromInfo_SampleFile;
ALTER TABLE targetedms.GeneralMoleculeChromInfo ADD CONSTRAINT FK_GMChromInfo_SampleFile FOREIGN KEY (SampleFileId) REFERENCES targetedms.SampleFile(Id);
DROP INDEX targetedms.IX_PeptideChromInfo_PeptideId;
CREATE INDEX IX_GMChromInfo_SampleFileId ON targetedms.GeneralMoleculeChromInfo(samplefileid);
DROP INDEX targetedms.IX_PeptideChromInfo_SampleFileId;

ALTER TABLE targetedms.GeneralMoleculeAnnotation DROP CONSTRAINT UQ_PeptideAnnotation_Name_Peptide;
ALTER TABLE targetedms.GeneralMoleculeAnnotation ADD CONSTRAINT UQ_GMAnnotation_Name_GMId UNIQUE (Name, GeneralMoleculeId);
DROP INDEX targetedms.IX_PeptideAnnotation_PeptideId;

ALTER INDEX targetedms.PK_PeptideAnnotation RENAME TO PK_GMAnnotation;

ALTER TABLE targetedms.PrecursorChromInfo RENAME COLUMN PeptideChromInfoId TO GeneralMoleculeChromInfoId;
ALTER TABLE targetedms.PrecursorChromInfo DROP CONSTRAINT FK_PrecursorChromInfo_PeptideChromInfo;
ALTER TABLE targetedms.PrecursorChromInfo ADD CONSTRAINT FK_PrecursorChromInfo_GMChromInfo FOREIGN KEY (GeneralMoleculeChromInfoId) REFERENCES targetedms.GeneralMoleculeChromInfo(Id);
DROP INDEX targetedms.IX_PrecursorChromInfo_PeptideChromInfoId;
CREATE INDEX IX_PrecursorChromInfo_GeneralMoleculeChromInfoId ON targetedms.PrecursorChromInfo (GeneralMoleculeChromInfoId);

DROP INDEX targetedms.IX_Precursor_PeptideId;

ALTER TABLE targetedms.PeptideIsotopeModification ADD CONSTRAINT FK_PeptideIsotopeModification_PeptideId_GMId FOREIGN KEY (PeptideId) REFERENCES targetedms.GeneralMolecule(Id);
ALTER TABLE targetedms.PeptideStructuralModification ADD CONSTRAINT FK_PeptideStructuralModification_PeptideId_GMId FOREIGN KEY (PeptideId) REFERENCES targetedms.GeneralMolecule(Id);
CREATE INDEX IX_Precursor_IsotopeLabelId ON targetedms.Precursor (IsotopeLabelId);

ALTER TABLE targetedms.Runs ADD COLUMN DocumentGUID ENTITYID;

ALTER TABLE targetedms.moleculeprecursor ADD COLUMN IonFormula VARCHAR(100);
ALTER TABLE targetedms.moleculeprecursor ADD COLUMN CustomIonName VARCHAR(100);
ALTER TABLE targetedms.moleculeprecursor ADD COLUMN MassMonoisotopic DOUBLE PRECISION NOT NULL;
ALTER TABLE targetedms.moleculeprecursor ADD COLUMN MassAverage DOUBLE PRECISION NOT NULL;

ALTER TABLE targetedms.Precursor ALTER COLUMN ModifiedSequence TYPE VARCHAR(300);
UPDATE targetedms.Peptide SET Sequence='' WHERE Sequence IS NULL;
ALTER TABLE targetedms.Peptide ALTER COLUMN Sequence SET NOT NULL;

/* targetedms-16.10-16.20.sql */

/* The run related count values are now calculated by the server in TargetedMSSchema.getTargetedMSRunsTable */
ALTER TABLE targetedms.Runs DROP COLUMN PeptideGroupCount;
ALTER TABLE targetedms.Runs DROP COLUMN PeptideCount;
ALTER TABLE targetedms.Runs DROP COLUMN PrecursorCount;
ALTER TABLE targetedms.Runs DROP COLUMN TransitionCount;

/* FK from targetedms.Peptide to targetedms.GeneralMolecule wasn't applied (issue 25789) */
ALTER TABLE targetedms.Peptide ADD CONSTRAINT FK_Id_GMId FOREIGN KEY (Id) REFERENCES targetedms.GeneralMolecule (Id);

ALTER TABLE targetedms.Peptide ALTER COLUMN Sequence TYPE VARCHAR(300);
ALTER TABLE targetedms.Peptide ALTER COLUMN PeptideModifiedSequence TYPE VARCHAR(500);
ALTER TABLE targetedms.Precursor ALTER COLUMN ModifiedSequence TYPE VARCHAR(2500);
ALTER TABLE targetedms.iRTPeptide ALTER COLUMN ModifiedSequence TYPE VARCHAR(500);

ALTER TABLE targetedms.Peptide ALTER COLUMN CalcNeutralMass TYPE DOUBLE PRECISION;
ALTER TABLE targetedms.Precursor ALTER COLUMN NeutralMass TYPE DOUBLE PRECISION;

-- Skyline-daily 3.5.1.9426 (and patch release of Skyline 3.5) changed the format of the modified_sequence attribute
-- of the <precursor> element to always have a decimal place in the modification mass string.
-- Example: [+80.0] instead of [+80].
-- Replace strings like [+80] in the modified sequence with [+80.0].
-- Example: K[+96.2]VN[-17]K[+34.1]TES[+80]K[+62.1] -> K[+96.2]VN[-17.0]K[+34.1]TES[+80.0]K[+62.1]
UPDATE targetedms.precursor SET ModifiedSequence = (REGEXP_REPLACE(ModifiedSequence, '(\[[+-]\d+)\]', '\1.0]', 'g'));

/* targetedms-16.20-16.30.sql */

CREATE TABLE targetedms.QCMetricConfiguration
(
    Id SERIAL NOT NULL,
    CreatedBy USERID,
    Created TIMESTAMP,
    ModifiedBy USERID,
    Modified TIMESTAMP,

    Container ENTITYID NOT NULL,
    Name VARCHAR(200) NOT NULL ,
    Series1Label VARCHAR(200) NOT NULL ,
    Series1SchemaName VARCHAR(200) NOT NULL ,
    Series1QueryName VARCHAR(200) NOT NULL ,
    Series2Label VARCHAR(200),
    Series2SchemaName VARCHAR(200),
    Series2QueryName VARCHAR(200),

    CONSTRAINT PK_QCMetricConfiguration PRIMARY KEY (Id),
    CONSTRAINT FK_QCMetricConfig_Containers FOREIGN KEY (Container) REFERENCES core.Containers (EntityId),
    CONSTRAINT UQ_QCMetricConfig_Name_Container UNIQUE (Name, Container)
);


WITH rootIdentity as (select EntityId as theIdentity FROM core.Containers WHERE Parent is null)

INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName,Series2Label,Series2SchemaName,Series2QueryName) VALUES
    ((select theIdentity from rootIdentity), 'Retention Time','Retention Time','targetedms','QCMetric_retentionTime',NULL , NULL , NULL ),
    ((select theIdentity from rootIdentity), 'Peak Area','Peak Area','targetedms','QCMetric_peakArea',NULL , NULL , NULL ),
    ((select theIdentity from rootIdentity), 'Full Width at Half Maximum (FWHM)','Full Width at Half Maximum (FWHM)','targetedms','QCMetric_fwhm',NULL , NULL , NULL ),
    ((select theIdentity from rootIdentity), 'Full Width at Base (FWB)','Full Width at Base (FWB)','targetedms','QCMetric_fwb',NULL , NULL , NULL ),
    ((select theIdentity from rootIdentity), 'Light/Heavy Ratio','Light/Heavy Ratio','targetedms','QCMetric_lhRatio',NULL , NULL , NULL ),
    ((select theIdentity from rootIdentity), 'Transition/Precursor Area Ratio','Transition/Precursor Area Ratio','targetedms','QCMetric_transitionPrecursorRatio',NULL , NULL , NULL ),
    ((select theIdentity from rootIdentity), 'Transition/Precursor Areas','Transition Area','targetedms','QCMetric_transitionArea','Precursor Area','targetedms','QCMetric_precursorArea'),
    ((select theIdentity from rootIdentity), 'Mass Accuracy','Mass Accuracy','targetedms','QCMetric_massAccuracy',NULL , NULL , NULL );

-- Add column to ReplicateAnnotation to store the source of the annotation (e.g. Skyline or AutoQC)
ALTER TABLE targetedms.ReplicateAnnotation ADD COLUMN Source VARCHAR(20) NOT NULL DEFAULT 'Skyline';

-- ExperimentRunLSID references exp.experimentrun.lsid
SELECT core.fn_dropifexists('Runs','targetedms','INDEX','IX_Runs_ExperimentRunLSID');

CREATE INDEX IX_Runs_ExperimentRunLSID ON targetedms.Runs(ExperimentRunLSID);

ALTER TABLE targetedms.transition ALTER COLUMN MeasuredIonName TYPE VARCHAR(255);

/* IX_Runs_ExperimentRunLSID */

SELECT core.fn_dropifexists('Runs','targetedms','INDEX','IX_Runs_ExperimentRunLSID');
CREATE INDEX IX_Runs_ExperimentRunLSID ON targetedms.Runs(ExperimentRunLSID, Id);

/* precursorchrominfo.Container */

ALTER TABLE targetedms.precursorchrominfo ADD COLUMN container ENTITYID;

UPDATE targetedms.precursorchrominfo
SET container =
  (SELECT R.container
   FROM targetedms.samplefile sfile
   INNER JOIN targetedms.replicate rep  ON ( rep.id = sfile.ReplicateId )
   INNER JOIN targetedms.runs r ON ( r.id = rep.RunId )
 WHERE sfile.id = SampleFileId );

ALTER TABLE targetedms.precursorchrominfo ALTER COLUMN container SET NOT NULL;

CREATE INDEX idx_precursorchrominfo_container ON targetedms.precursorchrominfo (container, id);

/* targetedms-16.30-17.10.sql */

-- Remove all replicates not associated with a sample file.
DELETE FROM targetedms.replicateAnnotation WHERE replicateId IN (SELECT r.id FROM targetedms.replicate r LEFT OUTER JOIN targetedms.sampleFile sf ON(r.Id = sf.ReplicateId) WHERE sf.ReplicateId IS NULL);
DELETE FROM targetedms.replicate WHERE id IN (SELECT r.id FROM targetedms.replicate r LEFT OUTER JOIN targetedms.sampleFile sf ON(r.Id = sf.ReplicateId) WHERE sf.ReplicateId IS NULL);

CREATE TABLE targetedms.GroupComparisonSettings
(
  Id SERIAL NOT NULL,
  RunId INT NOT NULL,
  Name TEXT,
  NormalizationMethod TEXT,
  ConfidenceLevel DOUBLE PRECISION,
  ControlAnnotation TEXT,
  ControlValue TEXT,
  CaseValue TEXT,
  IdentityAnnotation TEXT,
  PerProtein BOOLEAN,
  CONSTRAINT PK_GroupComparisonSettings PRIMARY KEY (Id),
  CONSTRAINT FK_GroupComparisonSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);
CREATE INDEX IX_GroupComparisonSettings_RunId ON targetedms.GroupComparisonSettings(RunId);

CREATE TABLE targetedms.FoldChange
(
  Id SERIAL NOT NULL,
  RunId INT NOT NULL,
  GroupComparisonSettingsId INT NOT NULL,
  PeptideGroupId INT,
  GeneralMoleculeId INT,
  IsotopeLabelId INT,
  MsLevel INT,
  GroupIdentifier TEXT,
  Log2FoldChange DOUBLE PRECISION,
  AdjustedPValue DOUBLE PRECISION,
  StandardError DOUBLE PRECISION,
  DegreesOfFreedom INT,
  CONSTRAINT PK_FoldChange PRIMARY KEY(Id),
  CONSTRAINT FK_FoldChange_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id),
  CONSTRAINT FK_FoldChange_GroupComparisonSettings FOREIGN KEY (GroupComparisonSettingsId) REFERENCES targetedms.GroupComparisonSettings(Id),
  CONSTRAINT FK_FoldChange_IsotopeLabel FOREIGN KEY (IsotopeLabelId) REFERENCES targetedms.IsotopeLabel(Id),
  CONSTRAINT FK_FoldChange_PeptideGroup FOREIGN KEY (PeptideGroupId) REFERENCES targetedms.PeptideGroup(Id),
  CONSTRAINT FK_FoldChange_GeneralMolecule FOREIGN KEY (GeneralMoleculeId) REFERENCES targetedms.GeneralMolecule(Id)
);
CREATE INDEX IX_FoldChange_RunId ON targetedms.FoldChange(RunId);
CREATE INDEX IX_FoldChange_GroupComparisonSettingsId ON targetedms.FoldChange(GroupComparisonSettingsId);
CREATE INDEX IX_FoldChange_IsotopeLabelId ON targetedms.FoldChange(IsotopeLabelId);
CREATE INDEX IX_FoldChange_PeptideGroupId ON targetedms.FoldChange(PeptideGroupId);
CREATE INDEX IX_FoldChange_GeneralMoleculeId ON targetedms.FoldChange(GeneralMoleculeId);

CREATE TABLE targetedms.QuantificationSettings
(
  Id SERIAL NOT NULL,
  RunId INT NOT NULL,
  RegressionWeighting VARCHAR(100),
  RegressionFit VARCHAR(100),
  NormalizationMethod TEXT,
  MsLevel INT,
  Units TEXT,
  CONSTRAINT PK_QuantificationSettings PRIMARY KEY (Id),
  CONSTRAINT FK_QuantificationSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);
CREATE INDEX IX_QuantificationSettings_RunId ON targetedms.QuantificationSettings(RunId);

CREATE TABLE targetedms.CalibrationCurve
(
  Id SERIAL NOT NULL,
  RunId INT NOT NULL,
  QuantificationSettingsId INT NOT NULL,
  GeneralMoleculeId INT,
  Slope DOUBLE PRECISION,
  Intercept DOUBLE PRECISION,
  PointCount INT,
  QuadraticCoefficient DOUBLE PRECISION,
  RSquared DOUBLE PRECISION,
  ErrorMessage TEXT,
  CONSTRAINT PK_CalibrationCurve PRIMARY KEY(Id),
  CONSTRAINT FK_CalibrationCurve_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id),
  CONSTRAINT FK_CalibrationCurve_QuantificationSettings FOREIGN KEY (QuantificationSettingsId) REFERENCES targetedms.QuantificationSettings(Id),
  CONSTRAINT FK_CalibrationCurve_GeneralMolecule FOREIGN KEY(GeneralMoleculeId) REFERENCES targetedms.GeneralMolecule(Id)
);
CREATE INDEX IX_CalibrationCurve_RunId ON targetedms.CalibrationCurve(RunId);
CREATE INDEX IX_CalibrationCurve_QuantificationSettingsId ON targetedms.CalibrationCurve(QuantificationSettingsId);
CREATE INDEX IX_CalibrationCurve_GeneralMoleculeId ON targetedms.CalibrationCurve(GeneralMoleculeId);

ALTER TABLE targetedms.Replicate ADD COLUMN SampleType VARCHAR(100);
ALTER TABLE targetedms.Replicate ADD COLUMN AnalyteConcentration DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralMoleculeChromInfo ADD COLUMN CalculatedConcentration DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralPrecursor ADD COLUMN IsotopeLabelId INT;
UPDATE targetedms.GeneralPrecursor gp SET IsotopeLabelId = (SELECT p.IsotopeLabelId FROM targetedms.Precursor p WHERE p.Id = gp.Id);
ALTER TABLE targetedms.GeneralPrecursor ADD CONSTRAINT FK_GeneralPrecursor_IsotopeLabel FOREIGN KEY (IsotopeLabelId) REFERENCES targetedms.IsotopeLabel(Id);
CREATE INDEX IX_GeneralPrecursor_IsotopeLabelId ON targetedms.GeneralPrecursor(IsotopeLabelId);
SELECT core.fn_dropifexists('Precursor', 'targetedms', 'INDEX', 'IX_Precursor_IsotopeLabelId');
ALTER TABLE targetedms.Precursor DROP CONSTRAINT FK_Precursor_IsotopeLabel;
ALTER TABLE targetedms.Precursor DROP COLUMN IsotopeLabelId;
ALTER TABLE targetedms.GeneralMolecule ADD COLUMN NormalizationMethod VARCHAR(255);
ALTER TABLE targetedms.GeneralMolecule ADD COLUMN InternalStandardConcentration DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralMolecule ADD COLUMN ConcentrationMultiplier DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralMolecule ADD COLUMN StandardType VARCHAR(100);
UPDATE targetedms.GeneralMolecule gm SET StandardType = (SELECT p.StandardType FROM targetedms.Peptide p WHERE p.Id = gm.Id);
ALTER TABLE targetedms.Peptide DROP COLUMN StandardType;

ALTER TABLE targetedms.Runs ADD COLUMN PeptideGroupCount INT;
ALTER TABLE targetedms.Runs ADD COLUMN PeptideCount INT;
ALTER TABLE targetedms.Runs ADD COLUMN SmallMoleculeCount INT;
ALTER TABLE targetedms.Runs ADD COLUMN PrecursorCount INT;
ALTER TABLE targetedms.Runs ADD COLUMN TransitionCount INT;

UPDATE targetedms.Runs SET PeptideGroupCount = (SELECT COUNT(pg.id) FROM targetedms.PeptideGroup pg WHERE pg.RunId = targetedms.Runs.Id);
UPDATE targetedms.Runs SET PeptideCount = (SELECT COUNT(p.id) FROM targetedms.PeptideGroup pg, targetedms.GeneralMolecule gm, targetedms.Peptide p WHERE pg.RunId = targetedms.Runs.Id AND gm.PeptideGroupId = pg.Id AND p.Id = gm.Id);
UPDATE targetedms.Runs SET SmallMoleculeCount = (SELECT COUNT(m.id) FROM targetedms.PeptideGroup pg, targetedms.GeneralMolecule gm, targetedms.molecule m WHERE pg.RunId = targetedms.Runs.Id AND gm.PeptideGroupId = pg.Id AND m.Id = gm.Id);
UPDATE targetedms.Runs SET PrecursorCount = (SELECT COUNT(gp.id) FROM targetedms.PeptideGroup pg, targetedms.GeneralMolecule gm, targetedms.GeneralPrecursor gp WHERE pg.RunId = targetedms.Runs.Id AND gm.PeptideGroupId = pg.Id AND gp.GeneralMoleculeId = gm.Id);
UPDATE targetedms.Runs SET TransitionCount = (SELECT COUNT(gt.id) FROM targetedms.PeptideGroup pg, targetedms.GeneralMolecule gm, targetedms.GeneralPrecursor gp, targetedms.GeneralTransition gt WHERE pg.RunId = targetedms.Runs.Id AND gm.PeptideGroupId = pg.Id AND gp.GeneralMoleculeId = gm.Id AND gt.GeneralPrecursorId = gp.Id);

UPDATE targetedms.precursorchrominfo pci SET container = r.container
FROM targetedms.samplefile sf
  INNER JOIN targetedms.replicate rep ON (rep.id = sf.replicateId)
  INNER JOIN targetedms.runs r ON (r.Id = rep.runId)
WHERE pci.sampleFileId = sf.id
      AND pci.container != r.container;