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
GO

-- iRTScale table to store iRT scale information.
CREATE TABLE targetedms.iRTScale
(
    Id INT IDENTITY(1, 1) NOT NULL,
    Container ENTITYID NOT NULL,
    Created DATETIME,
    CreatedBy INT,

    CONSTRAINT PK_iRTScale PRIMARY KEY (Id),
    CONSTRAINT FK_iRTScale_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId)
);
CREATE INDEX IX_iRTScale_Container ON targetedms.iRTScale (Container);

CREATE TABLE targetedms.Runs
(
    _ts TIMESTAMP,
    Id INT IDENTITY(1, 1) NOT NULL,
    CreatedBy USERID,
    Created DATETIME,
    ModifiedBy USERID,
    Modified DATETIME,
    Owner USERID NULL,

    Container ENTITYID NOT NULL,
    EntityId ENTITYID NOT NULL,
    Description NVARCHAR(300),
    FileName NVARCHAR(300),
    Status NVARCHAR(200),
    StatusId INT NOT NULL DEFAULT 0,
    Deleted BIT NOT NULL DEFAULT 0,
    ExperimentRunLSID LSIDType NULL,

    PeptideGroupCount INT NOT NULL DEFAULT 0,
    PeptideCount INT NOT NULL DEFAULT 0,
    PrecursorCount INT NOT NULL DEFAULT 0,
    TransitionCount INT NOT NULL DEFAULT 0,
    RepresentativeDataState INT NOT NULL DEFAULT 0,
    DataId INT,
    iRTScaleId INT,
    SoftwareVersion NVARCHAR(50),
    FormatVersion NVARCHAR(10),

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
    Id INT IDENTITY(1, 1) NOT NULL,
    Name NVARCHAR(100),
    StepSize REAL,
    StepCount INT,

    CONSTRAINT PK_Predictor PRIMARY KEY (Id)
);

CREATE TABLE targetedms.TransitionPredictionSettings
(
    RunId INT NOT NULL,
    PrecursorMassType NVARCHAR(20),
    ProductMassType NVARCHAR(20),
    OptimizeBy NVARCHAR(10) NOT NULL,
    CePredictorId INT,
    DpPredictorId INT,

    CONSTRAINT PK_TransitionPredictionSettings PRIMARY KEY (RunId),
    CONSTRAINT FK_TransitionPredictionSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);

CREATE TABLE targetedms.TransitionInstrumentSettings
(
    RunId INT NOT NULL,
    DynamicMin BIT,
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
    ProductMassAnalyzer NVARCHAR(20),
    ProductRes REAL,
    ProductResMz REAL,
    PrecursorIsotopes NVARCHAR(10),
    PrecursorIsotopeFilter REAL,
    PrecursorMassAnalyzer NVARCHAR(20),
    PrecursorRes REAL,
    PrecursorResMz REAL,
    ScheduleFilter BIT,
    -- AcquisitionMethod can be one of 'none', 'Targeted', 'DIA
    AcquisitionMethod NVARCHAR(10),
    -- RetentionTimeFilterType can be one of 'none', 'scheduling_windows', 'ms2_ids'
    RetentionTimeFilterType NVARCHAR(20),
    RetentionTimeFilterLength REAL,

    CONSTRAINT PK_TransitionFullScanSettings PRIMARY KEY (RunId),
    CONSTRAINT FK_TransitionFullScanSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);

CREATE TABLE targetedms.IsotopeEnrichment
(
    Id INT IDENTITY(1, 1) NOT NULL,
    RunId INT NOT NULL,
    Symbol NVARCHAR(10),
    PercentEnrichment REAL,
    Name NVARCHAR(100),

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
    CalculatorName NVARCHAR(200),
    IsIrt BIT,
    RegressionSlope REAL,
    RegressionIntercept REAL,
    PredictorName NVARCHAR(200),
    TimeWindow REAL,
    UseMeasuredRts BIT,
    MeasuredRtWindow REAL,
    IrtDatabasePath NVARCHAR(500),

    CONSTRAINT PK_RetentionTimePredictionSettings PRIMARY KEY (RunId),
    CONSTRAINT FK_RetentionTimePredictionSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);



-- ----------------------------------------------------------------------------
-- Instrument, Replicate and SampleFile
-- ----------------------------------------------------------------------------
CREATE TABLE targetedms.Instrument
(
    Id INT IDENTITY(1, 1) NOT NULL,
    RunId INT NOT NULL,
    Model NVARCHAR(300),
    IonizationType NVARCHAR(300),
    Analyzer NVARCHAR(300),
    Detector NVARCHAR(300),

    CONSTRAINT PK_Instrument PRIMARY KEY (Id),
    CONSTRAINT FK_Instrument_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);

CREATE INDEX IX_Instrument_RunId ON targetedms.Instrument (RunId);

CREATE TABLE targetedms.Replicate
(
    Id INT IDENTITY(1, 1) NOT NULL,
    RunId INT NOT NULL,
    Name NVARCHAR(100) NOT NULL,
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
    Id INT IDENTITY(1, 1) NOT NULL,
    ReplicateId INT NOT NULL,
    FilePath NVARCHAR(500) NOT NULL,
    SampleName NVARCHAR(300) NOT NULL,
    SkylineId NVARCHAR(300) NULL,
    AcquiredTime DATETIME,
    ModifiedTime DATETIME,
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
    Id INT IDENTITY(1, 1) NOT NULL,
    RunId INT NOT NULL,
    Label NVARCHAR(255) NOT NULL,
    Description TEXT,
    SequenceId INTEGER,
    Decoy BIT,
    Note TEXT,
    Modified DATETIME,

    -- 0 = NotRepresentative, 1 = Representative_Protein, 2 = Representative_Peptide
    RepresentativeDataState INT NOT NULL DEFAULT 0,
    Name NVARCHAR(255),
    Accession NVARCHAR(50),
    PreferredName NVARCHAR(50),
    Gene NVARCHAR(500),
    Species NVARCHAR(255),
    AltDescription TEXT,

    CONSTRAINT PK_PeptideGroup PRIMARY KEY (Id),
    CONSTRAINT FK_PeptideGroup_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);
CREATE INDEX IX_PeptideGroup_RunId ON targetedms.PeptideGroup(RunId);

-- ALternative proteins
CREATE TABLE targetedms.Protein
(
    Id INT IDENTITY(1, 1) NOT NULL,
    PeptideGroupId INT NOT NULL,
    LabkeySequenceId INT NOT NULL,
    Name NVARCHAR(50) NOT NULL,
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
    Id INT IDENTITY(1, 1) NOT NULL,
    PeptideGroupId INT NOT NULL,
    Sequence NVARCHAR(100),
    StartIndex INT,
    EndIndex INT,
    PreviousAa CHAR(1),
    NextAa CHAR(1),
    CalcNeutralMass float NULL,
    NumMissedCleavages INT NULL,
    Rank INTEGER,
    RtCalculatorScore REAL,
    PredictedRetentionTime REAL,
    AvgMeasuredRetentionTime REAL,
    Decoy BIT,
    Note TEXT,
    PeptideModifiedSequence NVARCHAR(255),
    StandardType NVARCHAR(20),
    ExplicitRetentionTime REAL,

    CONSTRAINT PK_Peptide PRIMARY KEY (Id),
    CONSTRAINT FK_Peptide_PeptideGroup FOREIGN KEY (PeptideGroupId) REFERENCES targetedms.PeptideGroup(Id)
);
CREATE INDEX IX_Peptide_Sequence ON targetedms.Peptide (Sequence);
CREATE INDEX IX_Peptide_PeptideGroupId ON targetedms.Peptide(PeptideGroupId);

CREATE TABLE targetedms.PeptideChromInfo
(
    Id INT IDENTITY(1, 1) NOT NULL,
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
    Id INT IDENTITY(1, 1) NOT NULL,
    PeptideId INT  NOT NULL,
    IsotopeLabelId INT,
    Mz FLOAT,
    Charge INT NOT NULL,
    NeutralMass FLOAT NULL,
    ModifiedSequence NVARCHAR(300),
    CollisionEnergy REAL,
    DeclusteringPotential REAL,
    Decoy BIT,
    DecoyMassShift REAL,
    Note TEXT,
    Modified DATETIME,
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
    Id INT IDENTITY(1, 1) NOT NULL,
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
    Identified NVARCHAR(10),
    LibraryDotp REAL,
    OptimizationStep INT,
    UserSet NVARCHAR(20),
    NOTE TEXT,
    Chromatogram IMAGE,
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
    Id INT IDENTITY(1, 1) NOT NULL,
    PrecursorId INT NOT NULL,
    Mz FLOAT,
    Charge INT,
    NeutralMass FLOAT,
    NeutralLossMass FLOAT,
    FragmentType NVARCHAR(10),
    FragmentOrdinal INT,
    CleavageAa CHAR(1),
    LibraryRank INT,
    LibraryIntensity REAL,
    IsotopeDistIndex INT,
    IsotopeDistRank INT,
    IsotopeDistProportion REAL,
    Decoy BIT,
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
    Id INT IDENTITY(1, 1) NOT NULL,
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
    FwhmDegenerate BIT,
    Truncated BIT,
    PeakRank INT,
    Identified NVARCHAR(10),
    OptimizationStep INT,
    UserSet NVARCHAR(20),
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
    Id INT IDENTITY(1, 1) NOT NULL,
    Name NVARCHAR(30) NOT NULL,
    Cut NVARCHAR(20) NULL,
    NoCut NVARCHAR(20),
    Sense NVARCHAR(10) NULL,

    CutC NVARCHAR(20),
    NoCutC NVARCHAR(20),
    CutN NVARCHAR(20),
    NoCutN NVARCHAR(20),

    CONSTRAINT PK_Enzyme PRIMARY KEY (Id)
);

CREATE TABLE targetedms.RunEnzyme
(
    EnzymeId INT NOT NULL,
    RunId INT NOT NULL,
    MaxMissedCleavages INT,
    ExcludeRaggedEnds BIT,

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
    Id INT IDENTITY(1, 1) NOT NULL,
    RunId INT NOT NULL,
    Name NVARCHAR(50) NOT NULL,
    Standard BIT NOT NULL,

    CONSTRAINT PK_IsotopeLabel PRIMARY KEY (Id),
    CONSTRAINT FK_IsotopeLabel_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);
CREATE INDEX IX_IsotopeLabel_RunId ON targetedms.IsotopeLabel (RunId);

CREATE TABLE targetedms.StructuralModification
(
    Id INT IDENTITY(1, 1) NOT NULL,
    Name NVARCHAR(100) NOT NULL,
    AminoAcid VARCHAR(30),
    Terminus CHAR(1),
    Formula NVARCHAR(50),
    MassDiffMono FLOAT,
    MassDiffAvg FLOAT,
    UnimodId INTEGER,

    CONSTRAINT PK_StructuralModification PRIMARY KEY (Id),
);

CREATE TABLE targetedms.StructuralModLoss
(
    Id INT IDENTITY(1, 1) NOT NULL,
    StructuralModId INT NOT NULL,
    Formula NVARCHAR(50),
    MassDiffMono FLOAT,
    MassDiffAvg FLOAT,
    Inclusion NVARCHAR(10)

    CONSTRAINT PK_StructuralModLoss PRIMARY KEY (Id),
    CONSTRAINT FK_StructuralModLoss_StructuralModification FOREIGN KEY (StructuralModId) REFERENCES targetedms.StructuralModification(Id)
);
CREATE INDEX IX_StructuralModification_StructuralModId ON targetedms.StructuralModLoss (StructuralModId);

CREATE TABLE targetedms.RunStructuralModification
(
    StructuralModId INT NOT NULL,
    RunId INT NOT NULL,
    ExplicitMod BIT,
    variable BIT NOT NULL DEFAULT 0,

    CONSTRAINT PK_RunStructuralModification PRIMARY KEY (StructuralModId, RunId),
    CONSTRAINT FK_RunStructuralModification_StructuralModification FOREIGN KEY (StructuralModId) REFERENCES targetedms.StructuralModification(Id),
    CONSTRAINT FK_RunStructuralModification_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);
CREATE INDEX IX_RunStructuralModification_RunId ON targetedms.RunStructuralModification (RunId);

CREATE TABLE targetedms.IsotopeModification
(
    Id INT IDENTITY(1, 1) NOT NULL,
    Name NVARCHAR(100) NOT NULL,
    AminoAcid CHAR(1),
    Terminus CHAR(1),
    Formula NVARCHAR(50) NULL,
    MassDiffMono FLOAT,
    MassDiffAvg FLOAT,
    Label13C BIT,
    Label15N BIT,
    Label18O BIT,
    Label2H BIT,
    UnimodId INTEGER,

    CONSTRAINT PK_IsotopeModification PRIMARY KEY (Id)
);

CREATE TABLE targetedms.RunIsotopeModification
(
    IsotopeModId INT NOT NULL,
    RunId INT NOT NULL,
    IsotopeLabelId INT NOT NULL,
    ExplicitMod BIT,
    RelativeRt NVARCHAR(20),

    CONSTRAINT PK_RunIsotopeModification PRIMARY KEY (IsotopeModId, RunId, IsotopeLabelId),
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
    Id INT IDENTITY(1, 1) NOT NULL,
    PeptideId INT NOT NULL,
    StructuralModId INT NOT NULL,
    IndexAa INT NOT NULL,
    MassDiff FLOAT,

    CONSTRAINT PK_PeptideStructuralModification PRIMARY KEY (Id),
    CONSTRAINT FK_PeptideStructuralModification_Peptide FOREIGN KEY (PeptideId) REFERENCES targetedms.Peptide(Id),
    CONSTRAINT FK_PeptideStructuralModification_StructuralModification FOREIGN KEY (StructuralModId) REFERENCES targetedms.StructuralModification(Id)
);
CREATE INDEX IX_PeptideStructuralModification_PeptideId ON targetedms.PeptideStructuralModification (PeptideId);
CREATE INDEX IX_PeptideStructuralModification_StructuralModId ON targetedms.PeptideStructuralModification (StructuralModId);

CREATE TABLE targetedms.PeptideIsotopeModification
(
    Id INT IDENTITY(1, 1) NOT NULL,
    PeptideId INT NOT NULL,
    IsotopeModId INT NOT NULL,
    IndexAa INT NOT NULL,
    MassDiff FLOAT,

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
    Id INT IDENTITY(1, 1) NOT NULL,
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
    Id INT IDENTITY(1, 1) NOT NULL,
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
    Id INT IDENTITY(1, 1) NOT NULL,
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
    Id INT IDENTITY(1, 1) NOT NULL,
    Type NVARCHAR(10) NOT NULL,  -- One of NIST, GPM or Bibliospec
    Score1Name NVARCHAR(20),
    Score2Name NVARCHAR(20),
    Score3Name NVARCHAR(20),

    CONSTRAINT PK_LibrarySource PRIMARY KEY (Id)
);

CREATE TABLE targetedms.LibrarySettings
(
    RunId INT NOT NULL,
    Pick NVARCHAR(10),
    RankType NVARCHAR(20),
    PeptideCount INT,

    CONSTRAINT PK_LibrarySettings PRIMARY KEY (RunId),
    CONSTRAINT FK_LibrarySettings_RunId FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);

CREATE TABLE targetedms.SpectrumLibrary
(
    Id INT IDENTITY(1, 1) NOT NULL,
    RunId INT NOT NULL,
    LibrarySourceId INT NOT NULL,
    LibraryType NVARCHAR(20) NOT NULL, -- One of 'bibliospec', 'bibliospec_lite', 'xhunter', 'nist', 'spectrast'.
    Name NVARCHAR(300) NOT NULL,
    FileNameHint NVARCHAR(100),
    SkylineLibraryId NVARCHAR(200) NULL,
    Revision NVARCHAR(10),

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
     Id INT IDENTITY(1, 1) NOT NULL,
     TransitionId INT NOT NULL,
     OptimizationType NVARCHAR(10) NOT NULL,
     OptValue REAL NOT NULL,

     CONSTRAINT PK_TransitionOptimization PRIMARY KEY (Id),
     CONSTRAINT FK_TransitionOptimization_TransitionId FOREIGN KEY (TransitionId) REFERENCES targetedms.Transition(Id)
);
CREATE INDEX IX_TransitionOptimization_TransitionId ON targetedms.TransitionOptimization (TransitionId);

CREATE TABLE targetedms.TransitionLoss
(
    Id INT IDENTITY(1, 1) NOT NULL,
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
    Id INT IDENTITY(1, 1) NOT NULL,
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
    Id INT IDENTITY(1, 1) NOT NULL,
    PeptideGroupId INT NOT NULL,
    Name NVARCHAR(255) NOT NULL,
    Value NVARCHAR(255) NOT NULL,

    CONSTRAINT PK_PeptideGroupAnnotation PRIMARY KEY (Id),
    CONSTRAINT FK_PeptideGroupAnnotation_PeptideGroup FOREIGN KEY (PeptideGroupId) REFERENCES targetedms.PeptideGroup(Id),
    CONSTRAINT UQ_PeptideGroupAnnotation_Name_PeptideGroup UNIQUE (Name, PeptideGroupId)
);

CREATE TABLE targetedms.PrecursorAnnotation
(
    Id INT IDENTITY(1, 1) NOT NULL,
    PrecursorId INT NOT NULL,
    Name NVARCHAR(255) NOT NULL,
    Value NVARCHAR(255) NOT NULL,

    CONSTRAINT PK_PrecursorAnnotation PRIMARY KEY (Id),
    CONSTRAINT FK_PrecursorAnnotation_Precursor FOREIGN KEY (PrecursorId) REFERENCES targetedms.Precursor(Id),
    CONSTRAINT UQ_PrecursorAnnotation_Name_Precursor UNIQUE (Name, PrecursorId)
);

CREATE TABLE targetedms.PrecursorChromInfoAnnotation
(
    Id INT IDENTITY(1, 1) NOT NULL,
    PrecursorChromInfoId INT NOT NULL,
    Name NVARCHAR(255) NOT NULL,
    Value NVARCHAR(255) NOT NULL,

    CONSTRAINT PK_PrecursorChromInfoAnnotation PRIMARY KEY (Id),
    CONSTRAINT FK_PrecursorChromInfoAnnotation_PrecursorChromInfo FOREIGN KEY (PrecursorChromInfoId) REFERENCES targetedms.PrecursorChromInfo(Id),
    CONSTRAINT UQ_PrecursorChromInfoAnnotation_Name_PrecursorChromInfo UNIQUE (Name, PrecursorChromInfoId)
);

CREATE TABLE targetedms.TransitionAnnotation
(
    Id INT IDENTITY(1, 1) NOT NULL,
    TransitionId INT NOT NULL,
    Name NVARCHAR(255) NOT NULL,
    Value NVARCHAR(255) NOT NULL,

    CONSTRAINT PK_TransitionAnnotation PRIMARY KEY (Id),
    CONSTRAINT FK_TransitionAnnotation_Transition FOREIGN KEY (TransitionId) REFERENCES targetedms.Transition(Id),
    CONSTRAINT UQ_TransitionAnnotation_Name_Transition UNIQUE (Name, TransitionId)
);

CREATE TABLE targetedms.TransitionChromInfoAnnotation
(
    Id INT IDENTITY(1, 1) NOT NULL,
    TransitionChromInfoId INT NOT NULL,
    Name NVARCHAR(255) NOT NULL,
    Value NVARCHAR(255) NOT NULL,

    CONSTRAINT PK_TransitionChromInfoAnnotation PRIMARY KEY (Id),
    CONSTRAINT FK_TransitionChromInfoAnnotation_TransitionChromInfo FOREIGN KEY (TransitionChromInfoId) REFERENCES targetedms.TransitionChromInfo(Id),
    CONSTRAINT UQ_TransitionChromInfoAnnotation_Name_TransitionChromInfo UNIQUE (Name, TransitionChromInfoId)
);

CREATE TABLE targetedms.PeptideAnnotation
(
    Id INT IDENTITY(1, 1) NOT NULL,
    PeptideId INT NOT NULL,
    Name NVARCHAR(255) NOT NULL,
    Value NVARCHAR(255) NOT NULL,

    CONSTRAINT PK_PeptideAnnotation PRIMARY KEY (Id),
    CONSTRAINT FK_PeptideAnnotation_Peptide FOREIGN KEY (PeptideId) REFERENCES targetedms.Peptide(Id),
    CONSTRAINT UQ_PeptideAnnotation_Name_Peptide UNIQUE (Name, PeptideId)
);

CREATE TABLE targetedms.PredictorSettings
(
    Id INT IDENTITY(1, 1) NOT NULL,
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
    Id INT IDENTITY(1, 1) NOT NULL,
    ReplicateId INT NOT NULL,
    Name NVARCHAR(255) NOT NULL,
    Value NVARCHAR(255) NOT NULL,

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
    Id INT IDENTITY(1, 1) NOT NULL,
    RunId INT NOT NULL,
    Name NVARCHAR(255) NOT NULL,
    Targets NVARCHAR(255),
    Type NVARCHAR(20),

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
    Id INT IDENTITY(1, 1) NOT NULL,
    ModifiedSequence NVARCHAR(100) NOT NULL,
    iRTStandard BIT NOT NULL,
    iRTValue FLOAT NOT NULL,
    iRTScaleId INT NOT NULL,
    Created DATETIME,
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
    Id INT IDENTITY(1, 1) NOT NULL,
    Container ENTITYID NOT NULL,
    CreatedBy USERID,
    Created DATETIME,
    ModifiedBy USERID,
    Modified DATETIME,

    Title NVARCHAR(MAX),
    Organism NVARCHAR(100),
    ExperimentDescription NVARCHAR(MAX),
    SampleDescription NVARCHAR(MAX),
    Instrument NVARCHAR(250),
    SpikeIn BIT,
    Citation NVARCHAR(MAX),
    Abstract NVARCHAR(MAX),
    PublicationLink NVARCHAR(MAX),
    ExperimentId INT NOT NULL DEFAULT 0,
    JournalCopy BIT NOT NULL DEFAULT 0,
    IncludeSubfolders BIT NOT NULL DEFAULT 0,

    CONSTRAINT PK_ExperimentAnnotations PRIMARY KEY (Id)
);
CREATE INDEX IX_ExperimentAnnotations_Container ON targetedms.ExperimentAnnotations (Container);
CREATE INDEX IX_ExperimentAnnotations_ExperimentId ON targetedms.ExperimentAnnotations(ExperimentId);

ALTER TABLE targetedms.ExperimentAnnotations ADD CONSTRAINT FK_ExperimentAnnotations_Experiment FOREIGN KEY (ExperimentId) REFERENCES exp.Experiment(RowId);
ALTER TABLE targetedms.ExperimentAnnotations ADD CONSTRAINT FK_ExperimentAnnotations_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId);

/* targetedms-14.10-14.20.sql */

CREATE TABLE targetedms.IsolationScheme
(
    Id INT IDENTITY(1, 1) NOT NULL,
    RunId INT NOT NULL,
    Name NVARCHAR(100) NOT NULL,
    PrecursorFilter REAL,
    PrecursorLeftFilter REAL,
    PrecursorRightFilter REAL,
    SpecialHandling NVARCHAR(50), -- Can be one of "Multiplexed", "MSe", "All Ions", "Overlap", "Overlap Multiplexed". Any others?
    WindowsPerScan INT,

    CONSTRAINT PK_IsolationScheme PRIMARY KEY (Id),
    CONSTRAINT FK_IsolationScheme_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);
CREATE INDEX IX_IsolationScheme_RunId ON targetedms.IsolationScheme (RunId);

CREATE TABLE targetedms.IsolationWindow
(
    Id INT IDENTITY(1, 1) NOT NULL,
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
    Id INT IDENTITY(1, 1) NOT NULL,
    RunId INT NOT NULL,
    UseSpectralLibraryDriftTimes BIT,
    SpectralLibraryDriftTimesResolvingPower REAL,
    PredictorName NVARCHAR(200),
    ResolvingPower REAL,

    CONSTRAINT PK_DriftTimePredictionSettings PRIMARY KEY (Id),
    CONSTRAINT FK_DriftTimePredictionSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);
CREATE INDEX IX_DriftTimePredictionSettings_RunId ON targetedms.DriftTimePredictionSettings(RunId);

CREATE TABLE targetedms.MeasuredDriftTime
(
    Id INT IDENTITY(1, 1) NOT NULL,
    DriftTimePredictionSettingsId INT NOT NULL,
    ModifiedSequence NVARCHAR(255) NOT NULL,
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
    Created DATETIME,
    ModifiedBy USERID,
    Modified DATETIME,

    Id INT IDENTITY(1, 1) NOT NULL,
    Name NVARCHAR(255) NOT NULL,
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
    Created DATETIME,

    JournalId INT NOT NULL,
    ExperimentAnnotationsId INT NOT NULL,
    ShortAccessURL EntityId NOT NULL,
    ShortCopyURL EntityId NOT NULL,
    Copied DATETIME,


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
    Id INT IDENTITY(1, 1) NOT NULL,
    Container ENTITYID NOT NULL,
    CreatedBy USERID,
    Created DATETIME,
    ModifiedBy USERID,
    Modified DATETIME,
    Name NVARCHAR(100),
    Description NVARCHAR(MAX),
    Color VARCHAR(6) NOT NULL,

    CONSTRAINT PK_QCAnnotationType PRIMARY KEY (Id),
    CONSTRAINT FK_QCAnnotationType_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId),
    CONSTRAINT UQ_QCAnnotationType_ContainerName UNIQUE (Container, Name)
);

CREATE TABLE targetedms.QCAnnotation
(
    Id INT IDENTITY(1, 1) NOT NULL,
    Container ENTITYID NOT NULL,
    CreatedBy USERID,
    Created DATETIME,
    ModifiedBy USERID,
    Modified DATETIME,
    QCAnnotationTypeId INT NOT NULL,
    Description NVARCHAR(MAX),
    Date DATETIME NOT NULL,

    CONSTRAINT PK_QCAnnotation PRIMARY KEY (Id),
    CONSTRAINT FK_QCAnnotation_QCAnnotationType FOREIGN KEY (QCAnnotationTypeId) REFERENCES targetedms.QCAnnotationType(Id)
);

-- Poke a few rows into the /Shared project
EXEC core.executeJavaInitializationCode 'populateDefaultAnnotationTypes';

-- ----------------------------------------------------------------------------
-- Molecule
-- ----------------------------------------------------------------------------
CREATE TABLE targetedms.Molecule
(
  PeptideId INT NOT NULL,
  IonFormula NVARCHAR(100),
  CustomIonName NVARCHAR(100),
  MassMonoisotopic FLOAT NOT NULL,
  MassAverage FLOAT NOT NULL,

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
  MassMonoisotopic FLOAT NOT NULL,
  MassAverage FLOAT NOT NULL,

  CONSTRAINT PK_MoleculeTransition PRIMARY KEY (TransitionId),
  CONSTRAINT FK_MoleculeTransition_Transition FOREIGN KEY (TransitionId) REFERENCES targetedms.Transition(Id)
);

/* targetedms-15.10-15.20.sql */

CREATE TABLE targetedms.GuideSet
(
  RowId INT IDENTITY(1, 1) NOT NULL,
  Container ENTITYID NOT NULL,
  CreatedBy USERID,
  Created DATETIME,
  ModifiedBy USERID,
  Modified DATETIME,
  TrainingStart DATETIME NOT NULL,
  TrainingEnd DATETIME NOT NULL,
  Comment NVARCHAR(MAX),

  CONSTRAINT PK_GuideSet PRIMARY KEY (RowId)
);

/* targetedms-15.30-16.10.sql */

CREATE TABLE targetedms.AutoQCPing (
  CreatedBy USERID,
  Created DATETIME,
  ModifiedBy USERID,
  Modified DATETIME,
  Container ENTITYID NOT NULL,

  CONSTRAINT PK_AutoQCPing PRIMARY KEY (Container)
);

ALTER TABLE targetedms.PrecursorChromInfo DROP COLUMN TotalAreaNormalized;
ALTER TABLE targetedms.TransitionChromInfo DROP COLUMN AreaNormalized;

/* New Data model as per 'Small molecule support' spec */

/* Rename Peptide table to GeneralMolecule table */
EXEC sp_rename 'targetedms.Peptide', 'GeneralMolecule';
GO

ALTER TABLE targetedms.Precursor DROP CONSTRAINT FK_Precursor_Peptide;
GO

ALTER TABLE targetedms.PeptideAnnotation DROP CONSTRAINT FK_PeptideAnnotation_Peptide;
GO

ALTER TABLE targetedms.PeptideChromInfo DROP CONSTRAINT FK_PeptideChromInfo_Peptide;
GO

ALTER TABLE targetedms.Molecule DROP CONSTRAINT FK_Molecule_Peptide;
GO

ALTER TABLE targetedms.PeptideIsotopeModification DROP CONSTRAINT FK_PeptideIsotopeModification_Peptide;
GO

ALTER TABLE targetedms.PeptideStructuralModification DROP CONSTRAINT FK_PeptideStructuralModification_Peptide;
GO

ALTER TABLE targetedms.GeneralMolecule DROP CONSTRAINT PK_Peptide;
GO

ALTER TABLE targetedms.GeneralMolecule ADD CONSTRAINT PK_GMId PRIMARY KEY (Id);
GO

/* Create a new Peptide table */
CREATE TABLE targetedms.Peptide
(
  Id INT NOT NULL,
  Sequence NVARCHAR(100),
  StartIndex INT,
  EndIndex INT,
  PreviousAa NCHAR(1),
  NextAa NCHAR(1),
  CalcNeutralMass FLOAT NOT NULL,
  NumMissedCleavages INT NOT NULL,
  Rank INTEGER,
  Decoy Bit,
  PeptideModifiedSequence NVARCHAR(255),
  StandardType NVARCHAR(20),
  CONSTRAINT PK_PeptideId PRIMARY KEY (Id)
);
GO

ALTER TABLE targetedms.Peptide ADD CONSTRAINT FK_Id_GMId FOREIGN KEY (Id) REFERENCES targetedms.GeneralMolecule (Id);
GO

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
                                      --  Can't use PeptideModifiedSequence column here because it was added in 13.3


GO

DROP INDEX targetedms.GeneralMolecule.IX_Peptide_Sequence;
GO

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

CREATE INDEX IX_Peptide_Sequence ON targetedms.Peptide (Sequence);
GO

/** Alter Molecule Table **/
EXEC sp_rename 'targetedms.Molecule.PeptideId', 'Id', 'COLUMN';
GO

ALTER TABLE targetedms.Molecule ADD CONSTRAINT FK_Molecule_Id FOREIGN KEY (Id) REFERENCES targetedms.GeneralMolecule(Id);
GO

/** Rename PeptideChromInfo table to GeneralChromInfo **/
EXEC sp_rename 'targetedms.PeptideChromInfo', 'GeneralMoleculeChromInfo';
GO

/** Modify GeneralChromInfo table to reference GeneralMolecule **/
EXEC sp_rename 'targetedms.GeneralMoleculeChromInfo.PeptideId', 'GeneralMoleculeId', 'COLUMN';
GO

ALTER TABLE targetedms.GeneralMoleculeChromInfo ADD CONSTRAINT FK_ChromInfo_GMId FOREIGN KEY (GeneralMoleculeId) REFERENCES targetedms.GeneralMolecule(Id);
GO

EXEC sp_rename 'targetedms.GeneralMoleculeChromInfo.PK_PeptideChromInfo', 'PK_GMChromInfoId';
GO

CREATE INDEX IX_GeneralMoleculeChromInfo_GMId ON targetedms.GeneralMoleculeChromInfo(GeneralMoleculeId);

/** Rename PeptideAnnotation table to GeneralMoleculeAnnotation **/
EXEC sp_rename 'targetedms.PeptideAnnotation', 'GeneralMoleculeAnnotation';
GO

/** Modify GeneralMoleculeAnnotation table to reference GeneralMolecule **/
EXEC sp_rename 'targetedms.GeneralMoleculeAnnotation.PeptideId', 'GeneralMoleculeId', 'COLUMN';
GO

ALTER TABLE targetedms.GeneralMoleculeAnnotation ADD CONSTRAINT FK_GMAnnotation_GMId FOREIGN KEY (GeneralMoleculeId) REFERENCES targetedms.GeneralMolecule(Id);
GO

CREATE INDEX IX_GeneralMoleculeAnnotation_GeneralMoleculeId ON targetedms.GeneralMoleculeAnnotation(GeneralMoleculeId);

-- /* Rename Precursor table to GeneralPrecursor */
EXEC sp_rename 'targetedms.Precursor', 'GeneralPrecursor';
GO

EXEC sp_rename 'targetedms.GeneralPrecursor.PeptideId', 'GeneralMoleculeId';
GO

ALTER TABLE targetedms.GeneralPrecursor ADD CONSTRAINT FK_GeneralPrecursor_GMId FOREIGN KEY (GeneralMoleculeId) REFERENCES targetedms.GeneralMolecule(Id);
GO

CREATE INDEX IX_Precursor_GMId ON targetedms.GeneralPrecursor (GeneralMoleculeId);
GO

ALTER TABLE targetedms.generalprecursor DROP CONSTRAINT FK_Precursor_IsotopeLabel;
GO

/* Create a new Precursor table */
CREATE TABLE targetedms.Precursor
(
  Id INT NOT NULL,
  IsotopeLabelId INT,
  NeutralMass FLOAT NOT NULL,
  ModifiedSequence NVARCHAR(300) NOT NULL,
  DecoyMassShift REAL,

  CONSTRAINT PK_Precursor_Id PRIMARY KEY (Id),
  CONSTRAINT FK_Precursor_Id FOREIGN KEY (Id) REFERENCES targetedms.GeneralPrecursor(Id),
  CONSTRAINT FK_Precursor_IsotopeLabel FOREIGN KEY (IsotopeLabelId) REFERENCES targetedms.IsotopeLabel (Id)
);
GO

CREATE INDEX IX_Precursor_Id ON targetedms.Precursor(Id);
GO

DROP INDEX targetedms.generalprecursor.IX_Precursor_IsotopeLabelId;

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
                     WHERE gp.ModifiedSequence IS NOT NULL); -- ModifiedSequence will be NULL for pre-existing small molecule data in the GeneralPrecursor table,


GO

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
GO

/* Modify PrecursorChromInfo, PrecursorAnnotation, PrecursorLibInfo */
ALTER TABLE targetedms.PrecursorChromInfo ADD CONSTRAINT FK_PrecursorChromInfo_PrecursorId FOREIGN KEY (PrecursorId) REFERENCES targetedms.GeneralPrecursor(Id);
GO

ALTER TABLE targetedms.PrecursorChromInfo DROP CONSTRAINT FK_PrecursorChromInfo_Precursor;
GO

ALTER TABLE targetedms.PrecursorAnnotation ADD CONSTRAINT FK_PrecursorAnnotation_PrecursorId FOREIGN KEY (PrecursorId) REFERENCES targetedms.GeneralPrecursor(Id);
GO

ALTER TABLE targetedms.PrecursorAnnotation DROP CONSTRAINT FK_PrecursorAnnotation_Precursor;
GO

ALTER TABLE targetedms.PrecursorLibInfo ADD CONSTRAINT FK_PrecursorLibInfo_PrecursorId FOREIGN KEY (PrecursorId) REFERENCES targetedms.GeneralPrecursor(Id);
GO

ALTER TABLE targetedms.PrecursorLibInfo DROP CONSTRAINT FK_PrecursorLibInfo_Precursor;
GO

/* Rename Transition table to GeneralTransition */
EXEC sp_rename 'targetedms.Transition', 'GeneralTransition';
GO

/* Modify GeneralTransition table to reference the GeneralPrecursor table */
EXEC sp_rename 'targetedms.GeneralTransition.PrecursorId', 'GeneralPrecursorId', 'COLUMN';
GO

ALTER TABLE targetedms.GeneralTransition DROP CONSTRAINT FK_Transition_Precursor;
GO

ALTER TABLE targetedms.GeneralTransition ADD CONSTRAINT FK_GeneralTransition_GPId FOREIGN KEY (GeneralPrecursorId) REFERENCES targetedms.GeneralPrecursor(Id);
GO

/* Create a new Transition Table */
CREATE TABLE targetedms.Transition
(
  Id INT NOT NULL,
  NeutralMass double precision,
  NeutralLossMass double precision,
  FragmentOrdinal integer,
  CleavageAa NCHAR(1),
  LibraryRank integer,
  LibraryIntensity real,
  DecoyMassShift real,
  MeasuredIonName NVARCHAR(20),
  CONSTRAINT PK_Transition_Id PRIMARY KEY (Id),
  CONSTRAINT FK_Transition_Id FOREIGN KEY (Id) REFERENCES targetedms.GeneralTransition(Id)
);
GO

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
GO

ALTER TABLE targetedms.GeneralTransition DROP COLUMN NeutralLossMass;
GO

ALTER TABLE targetedms.GeneralTransition DROP COLUMN FragmentOrdinal;
GO

ALTER TABLE targetedms.GeneralTransition DROP COLUMN CleavageAa;
GO

ALTER TABLE targetedms.GeneralTransition DROP COLUMN LibraryRank;
GO

ALTER TABLE targetedms.GeneralTransition DROP COLUMN LibraryIntensity;
GO

ALTER TABLE targetedms.GeneralTransition DROP COLUMN DecoyMassShift;
GO

ALTER TABLE targetedms.GeneralTransition DROP COLUMN MeasuredIonName;
GO

/* Add new columns to GeneralTransition table as per spec */
ALTER TABLE targetedms.GeneralTransition ADD ExplicitCollisionEnergy DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralTransition ADD SLens DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralTransition ADD ConeVoltage DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralTransition ADD ExplicitCompensationVoltage DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralTransition ADD ExplicitDeclusteringPotential DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralTransition ADD ExplicitDriftTimeMSec DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralTransition ADD ExplicitDriftTimeHighEnergyOffsetMSec DOUBLE PRECISION;

/* Modify MoleculeTransition, TransitionChromInfo, TransitionAnnotation to reference the GeneralTransition table */
ALTER TABLE targetedms.MoleculeTransition ADD CONSTRAINT FK_MoleculeTransition_GTId FOREIGN KEY (TransitionId) REFERENCES targetedms.GeneralTransition(Id);
GO

ALTER TABLE targetedms.MoleculeTransition DROP CONSTRAINT FK_MoleculeTransition_Transition;
GO

ALTER TABLE targetedms.TransitionChromInfo ADD CONSTRAINT FK_TransitionChromInfo_GTId FOREIGN KEY (TransitionId) REFERENCES targetedms.GeneralTransition(Id);
GO

ALTER TABLE targetedms.TransitionChromInfo DROP CONSTRAINT FK_TransitionChromInfo_Transition;
GO

ALTER TABLE targetedms.TransitionAnnotation ADD CONSTRAINT FK_TransitionAnnotation_GTId FOREIGN KEY (TransitionId) REFERENCES targetedms.GeneralTransition(Id);
GO

ALTER TABLE targetedms.TransitionAnnotation DROP CONSTRAINT FK_TransitionAnnotation_Transition;
GO

EXEC sp_rename 'targetedms.FK_PeptideChromInfo_SampleFile', 'FK_GMChromInfo_SampleFile';
GO

DROP INDEX targetedms.GeneralMoleculeChromInfo.IX_PeptideChromInfo_PeptideId;
GO

CREATE INDEX IX_GMChromInfo_SampleFileId ON targetedms.GeneralMoleculeChromInfo(samplefileid);
GO

DROP INDEX targetedms.GeneralMoleculeChromInfo.IX_PeptideChromInfo_SampleFileId;
GO

EXEC sp_rename 'targetedms.GeneralMoleculeAnnotation.UQ_PeptideAnnotation_Name_Peptide', 'UQ_GMAnnotation_Name_GMId';
GO

DROP INDEX targetedms.GeneralMoleculeAnnotation.IX_PeptideAnnotation_PeptideId;
GO

EXEC sp_rename 'targetedms.GeneralMoleculeAnnotation.PK_PeptideAnnotation', 'PK_GMAnnotation';
GO

EXEC sp_rename 'targetedms.PrecursorChromInfo.PeptideChromInfoId', 'GeneralMoleculeChromInfoId', 'COLUMN';
GO

EXEC sp_rename 'targetedms.FK_PrecursorChromInfo_PeptideChromInfo', 'FK_PrecursorChromInfo_GMChromInfo';
GO

DROP INDEX targetedms.PrecursorChromInfo.IX_PrecursorChromInfo_PeptideChromInfoId;
GO

CREATE INDEX IX_PrecursorChromInfo_GeneralMoleculeChromInfoId ON targetedms.PrecursorChromInfo (GeneralMoleculeChromInfoId);

ALTER TABLE targetedms.PeptideIsotopeModification ADD CONSTRAINT FK_PeptideIsotopeModification_PeptideId_GMId FOREIGN KEY (PeptideId) REFERENCES targetedms.GeneralMolecule(Id);
ALTER TABLE targetedms.PeptideStructuralModification ADD CONSTRAINT FK_PeptideStructuralModification_PeptideId_GMId FOREIGN KEY (PeptideId) REFERENCES targetedms.GeneralMolecule(Id);
CREATE INDEX IX_Precursor_IsotopeLabelId ON targetedms.Precursor (IsotopeLabelId);

ALTER TABLE targetedms.Runs ADD DocumentGUID ENTITYID;

ALTER TABLE targetedms.moleculeprecursor ADD IonFormula VARCHAR(100);
ALTER TABLE targetedms.moleculeprecursor ADD CustomIonName VARCHAR(100);
ALTER TABLE targetedms.moleculeprecursor ADD MassMonoisotopic FLOAT NOT NULL;
ALTER TABLE targetedms.moleculeprecursor ADD MassAverage FLOAT NOT NULL;

ALTER TABLE targetedms.Precursor ALTER COLUMN ModifiedSequence VARCHAR(300);
GO

DROP INDEX targetedms.Peptide.IX_Peptide_Sequence;
GO

UPDATE targetedms.Peptide SET Sequence='' WHERE Sequence IS NULL;
GO

ALTER TABLE targetedms.Peptide ALTER COLUMN Sequence NVARCHAR(100) NOT NULL;
GO

CREATE INDEX IX_Peptide_Sequence ON targetedms.Peptide (Sequence);
GO

/* targetedms-16.10-16.20.sql */

/* The run related count values are now calculated by the server in TargetedMSSchema.getTargetedMSRunsTable */
--EXEC core.fn_dropifexists 'Runs', 'targetedms', 'DEFAULT', 'PeptideGroupCount';
--ALTER TABLE targetedms.Runs DROP COLUMN PeptideGroupCount;
EXEC core.fn_dropifexists 'Runs', 'targetedms', 'DEFAULT', 'PeptideCount';
ALTER TABLE targetedms.Runs DROP COLUMN PeptideCount;
EXEC core.fn_dropifexists 'Runs', 'targetedms', 'DEFAULT', 'PrecursorCount';
ALTER TABLE targetedms.Runs DROP COLUMN PrecursorCount;
EXEC core.fn_dropifexists 'Runs', 'targetedms', 'DEFAULT', 'TransitionCount';
ALTER TABLE targetedms.Runs DROP COLUMN TransitionCount;

/* missed in sqlserver version of targetedms-16.10-16.11.sql */
EXEC core.fn_dropifexists 'Runs', 'targetedms', 'DEFAULT', 'PeptideGroupCount';
ALTER TABLE targetedms.Runs DROP COLUMN PeptideGroupCount;

ALTER TABLE targetedms.Peptide ALTER COLUMN Sequence NVARCHAR(300);
ALTER TABLE targetedms.Peptide ALTER COLUMN PeptideModifiedSequence NVARCHAR(500);
ALTER TABLE targetedms.Precursor ALTER COLUMN ModifiedSequence NVARCHAR(2500);
ALTER TABLE targetedms.iRTPeptide ALTER COLUMN ModifiedSequence NVARCHAR(500);

ALTER TABLE targetedms.Peptide ALTER COLUMN CalcNeutralMass FLOAT;
ALTER TABLE targetedms.Precursor ALTER COLUMN NeutralMass FLOAT;

-- Skyline-daily 3.5.1.9426 (and patch release of Skyline 3.5) changed the format of the modified_sequence attribute
-- of the <precursor> element to always have a decimal place in the modification mass string.
-- Example: [+80.0] instead of [+80].
-- Replace strings like [+80] in the modified sequence with [+80.0].
-- Example: K[+96.2]VN[-17]K[+34.1]TES[+80]K[+62.1] -> K[+96.2]VN[-17.0]K[+34.1]TES[+80.0]K[+62.1]

-- Note: invocation of Java upgrade code 'updatePrecursorModifiedSequence' has been removed, since we don't upgrade pre-16.2 installations any more

/* targetedms-16.20-16.30.sql */

CREATE TABLE targetedms.QCMetricConfiguration
(
    Id INT IDENTITY(1, 1) NOT NULL,
    CreatedBy USERID,
    Created DATETIME,
    ModifiedBy USERID,
    Modified DATETIME,

    Container ENTITYID NOT NULL,
    Name NVARCHAR(200) NOT NULL ,
    Series1Label NVARCHAR(200) NOT NULL ,
    Series1SchemaName NVARCHAR(200) NOT NULL ,
    Series1QueryName NVARCHAR(200) NOT NULL ,
    Series2Label NVARCHAR(200),
    Series2SchemaName NVARCHAR(200),
    Series2QueryName NVARCHAR(200)

    CONSTRAINT PK_QCMetricConfiguration PRIMARY KEY (Id),
    CONSTRAINT FK_QCMetricConfig_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId),
    CONSTRAINT UQ_QCMetricConfig_Name_Container UNIQUE (Name, Container)

);

declare @rootIdentity ENTITYID;
select @rootIdentity = [EntityId] FROM [core].[Containers] WHERE Parent is null

INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName) VALUES (@rootIdentity, 'Retention Time','Retention Time','targetedms','QCMetric_retentionTime')
INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName) VALUES (@rootIdentity, 'Peak Area','Peak Area','targetedms','QCMetric_peakArea')
INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName) VALUES (@rootIdentity, 'Full Width at Half Maximum (FWHM)','Full Width at Half Maximum (FWHM)','targetedms','QCMetric_fwhm')
INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName) VALUES (@rootIdentity, 'Full Width at Base (FWB)','Full Width at Base (FWB)','targetedms','QCMetric_fwb')
INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName) VALUES (@rootIdentity, 'Light/Heavy Ratio','Light/Heavy Ratio','targetedms','QCMetric_lhRatio')
INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName) VALUES (@rootIdentity, 'Transition/Precursor Area Ratio','Transition/Precursor Area Ratio','targetedms','QCMetric_transitionPrecursorRatio')
INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName,Series2Label,Series2SchemaName,Series2QueryName) VALUES (@rootIdentity, 'Transition/Precursor Areas','Transition Area','targetedms','QCMetric_transitionArea','Precursor Area','targetedms','QCMetric_precursorArea')
INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName) VALUES (@rootIdentity, 'Mass Accuracy','Mass Accuracy','targetedms','QCMetric_massAccuracy')

-- Add column to ReplicateAnnotation to store the source of the annotation (e.g. Skyline or AutoQC)
ALTER TABLE targetedms.ReplicateAnnotation ADD Source NVARCHAR(20) NOT NULL CONSTRAINT DF_ReplicateAnnotation_Source DEFAULT 'Skyline';

-- ExperimentRunLSID references exp.experimentrun.lsid
EXEC core.fn_dropifexists 'Runs','targetedms','INDEX','IX_Runs_ExperimentRunLSID';

CREATE INDEX IX_Runs_ExperimentRunLSID ON targetedms.Runs(ExperimentRunLSID);

ALTER TABLE targetedms.transition ALTER COLUMN MeasuredIonName NVARCHAR(255);

/* IX_Runs_ExperimentRunLSID */

EXEC core.fn_dropifexists 'Runs','targetedms','INDEX','IX_Runs_ExperimentRunLSID';
CREATE INDEX IX_Runs_ExperimentRunLSID ON targetedms.Runs(ExperimentRunLSID, Id);

/* precursorchrominfo.Container */

ALTER TABLE targetedms.precursorchrominfo ADD container ENTITYID;
GO

UPDATE targetedms.precursorchrominfo
SET container =
  (SELECT R.container
   FROM targetedms.samplefile sfile
   INNER JOIN targetedms.replicate rep  ON ( rep.id = sfile.ReplicateId )
   INNER JOIN targetedms.runs r ON ( r.id = rep.RunId )
 WHERE sfile.id = SampleFileId );

ALTER TABLE targetedms.precursorchrominfo ALTER COLUMN container ENTITYID NOT NULL;

CREATE INDEX idx_precursorchrominfo_container ON targetedms.precursorchrominfo (container, id);

GO

/* targetedms-16.30-17.10.sql */

-- Remove all replicates not associated with a sample file.
DELETE FROM targetedms.replicateAnnotation WHERE replicateId IN (SELECT r.id FROM targetedms.replicate r LEFT OUTER JOIN targetedms.sampleFile sf ON(r.Id = sf.ReplicateId) WHERE sf.ReplicateId IS NULL);
DELETE FROM targetedms.replicate WHERE id IN (SELECT r.id FROM targetedms.replicate r LEFT OUTER JOIN targetedms.sampleFile sf ON(r.Id = sf.ReplicateId) WHERE sf.ReplicateId IS NULL);

CREATE TABLE targetedms.GroupComparisonSettings
(
  Id INT IDENTITY(1,1) NOT NULL,
  RunId INT NOT NULL,
  Name NTEXT,
  NormalizationMethod NTEXT,
  ConfidenceLevel DOUBLE PRECISION,
  ControlAnnotation NTEXT,
  ControlValue NTEXT,
  CaseValue NTEXT,
  IdentityAnnotation NTEXT,
  PerProtein BIT,
  CONSTRAINT PK_GroupComparisonSettings PRIMARY KEY (Id),
  CONSTRAINT FK_GroupComparisonSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);
CREATE INDEX IX_GroupComparisonSettings_RunId ON targetedms.GroupComparisonSettings(RunId);

CREATE TABLE targetedms.FoldChange
(
  Id INT IDENTITY(1,1) NOT NULL,
  RunId INT NOT NULL,
  GroupComparisonSettingsId INT NOT NULL,
  PeptideGroupId INT,
  GeneralMoleculeId INT,
  IsotopeLabelId INT,
  MsLevel INT,
  GroupIdentifier NTEXT,
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
  Id INT IDENTITY(1,1) NOT NULL,
  RunId INT NOT NULL,
  RegressionWeighting NVARCHAR(100),
  RegressionFit NVARCHAR(100),
  NormalizationMethod NTEXT,
  MsLevel INT,
  Units NTEXT,
  CONSTRAINT PK_QuantificationSettings PRIMARY KEY (Id),
  CONSTRAINT FK_QuantificationSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);
CREATE INDEX IX_QuantificationSettings_RunId ON targetedms.QuantificationSettings(RunId);

CREATE TABLE targetedms.CalibrationCurve
(
  Id INT IDENTITY(1,1)  NOT NULL,
  RunId INT NOT NULL,
  QuantificationSettingsId INT NOT NULL,
  GeneralMoleculeId INT,
  Slope DOUBLE PRECISION,
  Intercept DOUBLE PRECISION,
  PointCount INT,
  QuadraticCoefficient DOUBLE PRECISION,
  RSquared DOUBLE PRECISION,
  ErrorMessage NTEXT,
  CONSTRAINT PK_CalibrationCurve PRIMARY KEY(Id),
  CONSTRAINT FK_CalibrationCurve_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id),
  CONSTRAINT FK_CalibrationCurve_QuantificationSettings FOREIGN KEY (QuantificationSettingsId) REFERENCES targetedms.QuantificationSettings(Id),
  CONSTRAINT FK_CalibrationCurve_GeneralMolecule FOREIGN KEY(GeneralMoleculeId) REFERENCES targetedms.GeneralMolecule(Id)
);
CREATE INDEX IX_CalibrationCurve_RunId ON targetedms.CalibrationCurve(RunId);
CREATE INDEX IX_CalibrationCurve_QuantificationSettingsId ON targetedms.CalibrationCurve(QuantificationSettingsId);
CREATE INDEX IX_CalibrationCurve_GeneralMoleculeId ON targetedms.CalibrationCurve(GeneralMoleculeId);

ALTER TABLE targetedms.Replicate ADD SampleType NVARCHAR(100);
ALTER TABLE targetedms.Replicate ADD AnalyteConcentration DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralMoleculeChromInfo ADD CalculatedConcentration DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralPrecursor ADD IsotopeLabelId INT
GO
UPDATE gp SET IsotopeLabelId = (SELECT p.IsotopeLabelId FROM targetedms.Precursor p WHERE p.Id = gp.Id) FROM targetedms.GeneralPrecursor gp;
ALTER TABLE targetedms.GeneralPrecursor ADD CONSTRAINT FK_GeneralPrecursor_IsotopeLabel FOREIGN KEY (IsotopeLabelId) REFERENCES targetedms.IsotopeLabel(Id);
CREATE INDEX IX_GeneralPrecursor_IsotopeLabelId ON targetedms.GeneralPrecursor(IsotopeLabelId);
EXEC core.fn_dropifexists 'Precursor', 'targetedms', 'INDEX', 'IX_Precursor_IsotopeLabelId';
ALTER TABLE targetedms.Precursor DROP CONSTRAINT FK_Precursor_IsotopeLabel;
ALTER TABLE targetedms.Precursor DROP COLUMN IsotopeLabelId;
ALTER TABLE targetedms.GeneralMolecule ADD NormalizationMethod NTEXT;
ALTER TABLE targetedms.GeneralMolecule ADD InternalStandardConcentration DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralMolecule ADD ConcentrationMultiplier DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralMolecule ADD StandardType NVARCHAR(100)
GO
UPDATE gm SET StandardType = (SELECT p.StandardType FROM targetedms.Peptide p WHERE p.Id = gm.Id) FROM targetedms.GeneralMolecule gm;
ALTER TABLE targetedms.Peptide DROP COLUMN StandardType;

ALTER TABLE targetedms.Runs ADD PeptideGroupCount INT;
ALTER TABLE targetedms.Runs ADD PeptideCount INT;
ALTER TABLE targetedms.Runs ADD SmallMoleculeCount INT;
ALTER TABLE targetedms.Runs ADD PrecursorCount INT;
ALTER TABLE targetedms.Runs ADD TransitionCount INT;

GO

UPDATE targetedms.Runs SET PeptideGroupCount = (SELECT COUNT(pg.id) FROM targetedms.PeptideGroup pg WHERE pg.RunId = targetedms.Runs.Id);
UPDATE targetedms.Runs SET PeptideCount = (SELECT COUNT(p.id) FROM targetedms.PeptideGroup pg, targetedms.GeneralMolecule gm, targetedms.Peptide p WHERE pg.RunId = targetedms.Runs.Id AND gm.PeptideGroupId = pg.Id AND p.Id = gm.Id);
UPDATE targetedms.Runs SET SmallMoleculeCount = (SELECT COUNT(m.id) FROM targetedms.PeptideGroup pg, targetedms.GeneralMolecule gm, targetedms.molecule m WHERE pg.RunId = targetedms.Runs.Id AND gm.PeptideGroupId = pg.Id AND m.Id = gm.Id);
UPDATE targetedms.Runs SET PrecursorCount = (SELECT COUNT(gp.id) FROM targetedms.PeptideGroup pg, targetedms.GeneralMolecule gm, targetedms.GeneralPrecursor gp WHERE pg.RunId = targetedms.Runs.Id AND gm.PeptideGroupId = pg.Id AND gp.GeneralMoleculeId = gm.Id);
UPDATE targetedms.Runs SET TransitionCount = (SELECT COUNT(gt.id) FROM targetedms.PeptideGroup pg, targetedms.GeneralMolecule gm, targetedms.GeneralPrecursor gp, targetedms.GeneralTransition gt WHERE pg.RunId = targetedms.Runs.Id AND gm.PeptideGroupId = pg.Id AND gp.GeneralMoleculeId = gm.Id AND gt.GeneralPrecursorId = gp.Id);

UPDATE targetedms.precursorchrominfo SET container = r.container
FROM targetedms.samplefile sf
   INNER JOIN targetedms.replicate rep ON (rep.id = sf.replicateId)
   INNER JOIN targetedms.runs r ON (r.Id = rep.runId)
WHERE sampleFileId = sf.id
      AND targetedms.precursorchrominfo.container != r.container;

/* targetedms-17.10-17.20.sql */

ALTER TABLE targetedms.PrecursorChromInfo ADD ChromatogramFormat INT;

CREATE TABLE targetedms.QCMetricExclusion
(
  Id INT IDENTITY(1,1) NOT NULL,
  CreatedBy USERID,
  Created DATETIME,
  ModifiedBy USERID,
  Modified DATETIME,

  ReplicateId INT NOT NULL,
  MetricId INT, -- allow NULL to indicate exclusion of replicate for all metrics

  CONSTRAINT PK_QCMetricExclusion PRIMARY KEY (Id),
  CONSTRAINT FK_QCMetricExclusion_ReplicateId FOREIGN KEY (ReplicateId) REFERENCES targetedms.Replicate (Id),
  CONSTRAINT FK_QCMetricExclusion_MetricId FOREIGN KEY (MetricId) REFERENCES targetedms.QCMetricConfiguration (Id),
  CONSTRAINT UQ_QCMetricExclusion_Replicate_Metric UNIQUE (ReplicateId, MetricId)
);
CREATE INDEX IX_QCMetricExclusion_ReplicateId ON targetedms.QCMetricExclusion(ReplicateId);
CREATE INDEX IX_QCMetricExclusion_MetricId ON targetedms.QCMetricExclusion(MetricId);

ALTER TABLE targetedms.ExperimentAnnotations ADD sourceExperimentId INT;
ALTER TABLE targetedms.ExperimentAnnotations ADD sourceExperimentPath NVARCHAR(1000);
ALTER TABLE targetedms.ExperimentAnnotations ADD shortUrl entityId;

CREATE INDEX IX_ExperimentAnnotations_SourceExperimentId ON targetedms.ExperimentAnnotations (sourceExperimentId);
ALTER TABLE targetedms.ExperimentAnnotations ADD CONSTRAINT UQ_ExperimentAnnotations_ShortUrl UNIQUE (shortUrl);
ALTER TABLE targetedms.ExperimentAnnotations ADD CONSTRAINT FK_ExperimentAnnotations_ShortUrl FOREIGN KEY (shorturl)
REFERENCES core.shorturl (entityId);

/* targetedms-17.20-17.30.sql */

ALTER TABLE targetedms.Runs ADD ReplicateCount INT;

GO

UPDATE targetedms.Runs SET ReplicateCount = (SELECT COUNT(r.id) FROM targetedms.Replicate r WHERE r.RunId = targetedms.Runs.Id);

ALTER TABLE targetedms.TransitionChromInfo ADD PointsAcrossPeak INT;

/* targetedms-17.30-18.10.sql */

ALTER TABLE targetedms.ReplicateAnnotation ALTER COLUMN Value NVARCHAR(500);

/* targetedms-18.10-18.20.sql */

ALTER TABLE targetedms.Runs ADD SkydDataId INT;

ALTER TABLE targetedms.Runs ADD CONSTRAINT FK_Runs_SkydData FOREIGN KEY (SkydDataId) REFERENCES exp.Data(RowId);

ALTER TABLE targetedms.PrecursorChromInfo ADD ChromatogramOffset BIGINT;
ALTER TABLE targetedms.PrecursorChromInfo ADD ChromatogramLength INT;

ALTER TABLE targetedms.Runs ADD CalibrationCurveCount INT;

GO

UPDATE targetedms.Runs SET CalibrationCurveCount = (SELECT COUNT(c.id) FROM targetedms.CalibrationCurve c WHERE c.RunId = targetedms.Runs.Id);

ALTER TABLE targetedms.GeneralMoleculeChromInfo ADD ExcludeFromCalibration BIT;

GO

UPDATE targetedms.GeneralMoleculeChromInfo SET ExcludeFromCalibration = 0;

ALTER TABLE targetedms.QuantificationSettings ADD MaxLOQBias FLOAT;
ALTER TABLE targetedms.QuantificationSettings ADD MaxLOQCV FLOAT;
ALTER TABLE targetedms.QuantificationSettings ADD LODCalculation NVARCHAR(50);

ALTER TABLE targetedms.ExperimentAnnotations ADD Keywords NVARCHAR(200);
ALTER TABLE targetedms.ExperimentAnnotations ADD LabHead USERID;
ALTER TABLE targetedms.ExperimentAnnotations ADD LabHeadAffiliation NVARCHAR(200);
ALTER TABLE targetedms.ExperimentAnnotations ADD Submitter USERID;
ALTER TABLE targetedms.ExperimentAnnotations ADD SubmitterAffiliation NVARCHAR(200);
ALTER TABLE targetedms.ExperimentAnnotations ADD pxid NVARCHAR(10);

ALTER TABLE targetedms.JournalExperiment ADD PxidRequested BIT NOT NULL DEFAULT '0';
ALTER TABLE targetedms.JournalExperiment ADD KeepPrivate BIT NOT NULL DEFAULT '1';

ALTER TABLE targetedms.spectrumlibrary ALTER COLUMN Name NVARCHAR(400);
GO

/* targetedms-18.20-18.30.sql */

ALTER TABLE targetedms.experimentannotations ALTER COLUMN Organism NVARCHAR(300);
ALTER TABLE targetedms.Replicate ADD SampleDilutionFactor DOUBLE PRECISION;

/* targetedms-18.30-19.10.sql */

UPDATE a set a.sourceapplicationid = (SELECT d.sourceapplicationid from exp.data d where d.runid = a.runid AND d.name LIKE '%.zip')
FROM exp.data AS a
WHERE a.sourceapplicationid IS NULL AND a.runid IS NOT NULL AND a.name LIKE '%.skyd';

-- Add a column to store the size of the Skyline document
ALTER TABLE targetedms.runs ADD DocumentSize BIGINT;

ALTER TABLE targetedms.precursorchrominfo ADD qvalue REAL;
ALTER TABLE targetedms.precursorchrominfo ADD zscore REAL;

/* targetedms-19.10-19.20.sql */

CREATE TABLE targetedms.QCEnabledMetrics
(
  metric          INTEGER,
  enabled         BIT,
  lowerBound      DOUBLE PRECISION,
  upperBound      DOUBLE PRECISION,
  cusumLimit      DOUBLE PRECISION,

  Created         DATETIME,
  CreatedBy       USERID,
  Modified        DATETIME,
  ModifiedBy      USERID,
  Container       ENTITYID NOT NULL,

  CONSTRAINT PK_QCEnabledMetrics PRIMARY KEY (metric),
  CONSTRAINT FK_QCEnabledMetrics_Metric FOREIGN KEY (metric) REFERENCES targetedms.qcmetricconfiguration(Id),
  CONSTRAINT FK_QCEnabledMetrics_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId)
);
GO

CREATE INDEX IX_targetedms_qcEnabledMetrics_Container ON targetedms.QCEnabledMetrics (Container);

ALTER TABLE targetedms.QCEnabledMetrics DROP CONSTRAINT PK_QCEnabledMetrics;
GO

ALTER TABLE targetedms.QCEnabledMetrics ADD CONSTRAINT PK_QCEnabledMetrics PRIMARY KEY (metric, Container);
GO

EXEC core.fn_dropifexists 'AuditLogEntry','targetedms','TABLE', NULL;

CREATE TABLE targetedms.AuditLogEntry (
    entryId INT IDENTITY(1, 1) not null,
    documentGUID ENTITYID not null,
    entryHash NVARCHAR(100) not null,
    versionId int null,
    createTimestamp DATETIME not null,
    timezoneOffset int not null,
    userName  NVARCHAR(100) not null,
    formatVersion NVARCHAR(100) not null,
    parentEntryHash NVARCHAR(100) null,
    reason NVARCHAR(1000) NULL,
    extraInfo NVARCHAR(max) NULL,
    CONSTRAINT pk_auditLogEntry PRIMARY KEY (entryId),
    CONSTRAINT fk_auditLogEntry_runs FOREIGN KEY (versionId) REFERENCES targetedms.runs(id)
);

CREATE UNIQUE INDEX uix_auditLogEntry_document on targetedms.AuditLogEntry(documentGUID, entryHash);

EXEC core.fn_dropifexists 'AuditLogMessage','targetedms','TABLE', NULL;

CREATE TABLE targetedms.AuditLogMessage(
  messageId INT IDENTITY(1, 1) not null,
  orderNumber int not null,
  entryId int not null,
  messageType NVARCHAR(50) not null,
  enText NVARCHAR(max) null,
  expandedText NVARCHAR(max) null,
  reason NVARCHAR(1000) null,
  CONSTRAINT pk_auditLogMessage PRIMARY KEY (messageId),
  CONSTRAINT fk_auditLogMessage_entry FOREIGN KEY (entryId) REFERENCES targetedms.AuditLogEntry(entryId)
);

CREATE UNIQUE INDEX uix_auditLogMessage_entry on targetedms.AuditLogMessage(entryId, orderNumber);

UPDATE targetedms.QCMetricConfiguration SET Name = 'Transition & Precursor Areas' WHERE Name = 'Transition/Precursor Areas';

ALTER TABLE targetedms.qcmetricconfiguration ADD PrecursorScoped BIT NOT NULL DEFAULT 1;

GO

EXEC core.fn_dropifexists 'QCEmailNotifications','targetedms','TABLE', NULL;

-- Increase the length of the Gene column. The gene field can contain all possible gene names that a protein product is associated with. This can get really long.
ALTER TABLE targetedms.PeptideGroup ALTER COLUMN gene NVARCHAR(2000);

CREATE INDEX uix_auditLogEntry_version on targetedms.AuditLogEntry(versionId);

ALTER TABLE targetedms.MeasuredDriftTime ALTER COLUMN DriftTime REAL NULL;
-- From Brian Pratt about the charge field: either a simple number or an addition description-
-- 1, -4, [M+H]. But no hard limit to adduct string. Typically short though.
-- Longest one there seems to be [M+IsoProp+Na+H] (17 characters) though most come in below 10
ALTER TABLE targetedms.MeasuredDriftTime ALTER COLUMN Charge NVARCHAR(30) NOT NULL;
ALTER TABLE targetedms.MeasuredDriftTime ADD Ccs DOUBLE PRECISION;
ALTER TABLE targetedms.MeasuredDriftTime ADD IonMobility DOUBLE PRECISION;
ALTER TABLE targetedms.MeasuredDriftTime ADD HighEnergyIonMobilityOffset DOUBLE PRECISION;
-- From Brian Pratt about the ion_mobility_units field: Worst case is 23 characters, for Bruker:  inverse_K0_Vsec_per_cm2
ALTER TABLE targetedms.MeasuredDriftTime ADD IonMobilityUnits NVARCHAR(30);

/* targetedms-19.20-19.30.sql */

ALTER TABLE targetedms.Runs ADD AuditLogEntriesCount INT DEFAULT 0 NOT NULL;

declare @rootIdentity ENTITYID;
select @rootIdentity = [EntityId] FROM [core].[Containers] WHERE Parent is null

INSERT INTO targetedms.QCMetricConfiguration (Container, Name, Series1Label, Series1SchemaName, Series1QueryName, PrecursorScoped)
VALUES (@rootIdentity, 'TIC Area','TIC Area','targetedms','QCRunMetric_ticArea', 0);

ALTER TABLE targetedms.SampleFile ADD TicArea DOUBLE PRECISION;

ALTER TABLE targetedms.GeneralMolecule ADD AttributeGroupId NVARCHAR(100);

ALTER TABLE targetedms.Replicate ALTER COLUMN Name NVARCHAR(200) NOT NULL;

ALTER TABLE targetedms.SampleFile ADD InstrumentSerialNumber NVARCHAR(200);
ALTER TABLE targetedms.SampleFile ADD SampleId NVARCHAR(200);

CREATE TABLE targetedms.ListDefinition
(
    Id INT IDENTITY(1,1) NOT NULL,
    RunId INT NOT NULL,
    Name NVARCHAR(max) NOT NULL,
    PkColumnIndex INT NULL,
    DisplayColumnIndex INT NULL,
    CONSTRAINT PK_List PRIMARY KEY(Id),
    CONSTRAINT FK_List_RunId FOREIGN KEY(RunId) REFERENCES targetedms.Runs(Id)
);
CREATE TABLE targetedms.ListColumnDefinition
(
    Id INT IDENTITY(1,1) NOT NULL,
    ListDefinitionId INT NOT NULL,
    ColumnIndex INT NOT NULL,
    AnnotationType NVARCHAR(20) NOT NULL,
    Name NVARCHAR(max) NOT NULL,
    Lookup NVARCHAR(max) NULL,
    CONSTRAINT PK_ListColumn PRIMARY KEY(Id),
    CONSTRAINT FK_ListColumn_ListDefinitionId FOREIGN KEY(ListDefinitionId) REFERENCES targetedms.ListDefinition(Id),
    CONSTRAINT UQ_ListColumn_ListDefinitionId_ColumnIndex UNIQUE(ListDefinitionId, ColumnIndex)
);

CREATE TABLE targetedms.ListItem
(
    Id INT IDENTITY(1,1)  NOT NULL,
    ListDefinitionId INT NOT NULL,
    CONSTRAINT PK_ListItem PRIMARY KEY(Id),
    CONSTRAINT FK_ListItem_ListDefinitionId FOREIGN KEY(ListDefinitionId) REFERENCES targetedms.ListDefinition(Id)
);

CREATE TABLE targetedms.ListItemValue
(
    Id INT IDENTITY(1,1)  NOT NULL,
    ListItemId INT NOT NULL,
    ColumnIndex INT NOT NULL,
    TextValue NVARCHAR(max) NULL,
    NumericValue FLOAT NULL,
    CONSTRAINT PK_ListItemValue PRIMARY KEY(Id),
    CONSTRAINT FK_ListItemValue_ListItem FOREIGN KEY(ListItemId) REFERENCES targetedms.ListItem(Id),
    CONSTRAINT UQ_ListItemValue_ListItemId_ColumnIndex UNIQUE(ListItemId, ColumnIndex)
);

ALTER TABLE targetedms.QCMetricConfiguration ADD EnabledQueryName NVARCHAR(200);
ALTER TABLE targetedms.QCMetricConfiguration ADD EnabledSchemaName NVARCHAR(200);
GO

declare @rootIdentity ENTITYID;
select @rootIdentity = [EntityId] FROM [core].[Containers] WHERE Parent is null

INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName,Series2Label,Series2SchemaName,Series2QueryName,PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
  (@rootIdentity, 'Isotopologue LOD', 'LOD','targetedms', 'QCMetric_IsotopologuePrecursorLOD', NULL, NULL, NULL, 1, 'QCMetricEnabled_IsotopologuePrecursorLOD', 'targetedms');

INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName,Series2Label,Series2SchemaName,Series2QueryName,PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
  (@rootIdentity, 'Isotopologue LOQ', 'LOQ', 'targetedms', 'QCMetric_IsotopologuePrecursorLOQ', NULL, NULL, NULL, 1, 'QCMetricEnabled_IsotopologuePrecursorLOQ', 'targetedms');

INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName,Series2Label,Series2SchemaName,Series2QueryName,PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
  (@rootIdentity, 'Isotopologue Accuracy', 'Accuracy', 'targetedms', 'QCMetric_IsotopologuePrecursorAccuracy', NULL, NULL, NULL, 1, 'QCMetricEnabled_IsotopologuePrecursorAccuracy', 'targetedms');

INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName,Series2Label,Series2SchemaName,Series2QueryName,PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
  (@rootIdentity, 'Isotopologue Regression RSquared', 'Coefficient', 'targetedms', 'QCMetric_IsotopologuePrecursorRSquared', NULL, NULL, NULL, 1, 'QCMetricEnabled_IsotopologuePrecursorRSquared', 'targetedms');

UPDATE targetedms.QCMetricConfiguration SET EnabledQueryName = 'QCMetricEnabled_lhRatio', EnabledSchemaName ='targetedms' WHERE Series1QueryName = 'QCMetric_lhRatio';

ALTER TABLE targetedms.runs ALTER COLUMN SoftwareVersion NVARCHAR(200);

ALTER TABLE targetedms.Runs ADD ListCount INT DEFAULT 0 NOT NULL;

ALTER TABLE targetedms.AnnotationSettings ADD Lookup NVARCHAR(MAX);

CREATE TABLE targetedms.SampleFileChromInfo
(
  Id INT IDENTITY(1, 1) NOT NULL,
  SampleFileId integer NOT NULL,
  StartTime real,
  EndTime real,
  TextId NVARCHAR(512),
  NumPoints integer,
  UncompressedSize integer,
  Container entityid NOT NULL,
  ChromatogramFormat integer,
  ChromatogramOffset bigint,
  ChromatogramLength integer,
  CONSTRAINT PK_SampleFileChromInfo PRIMARY KEY (Id),
  CONSTRAINT FK_SampleFileChromInfo_SampleFile FOREIGN KEY (SampleFileId)
  REFERENCES targetedms.SampleFile(Id)
);

CREATE INDEX IDX_SampleFileChromInfo_Container ON targetedms.SampleFileChromInfo(container);

CREATE INDEX IDX_SampleFileChromInfo_SampleFileId ON targetedms.SampleFileChromInfo(samplefileid);

ALTER TABLE targetedms.Replicate ADD HasMidasSpectra BIT;
ALTER TABLE targetedms.Replicate ADD BatchName NVARCHAR(200);

ALTER TABLE targetedms.SampleFile ADD ExplicitGlobalStandardArea DOUBLE PRECISION;
ALTER TABLE targetedms.SampleFile ADD IonMobilityType NVARCHAR(200);

ALTER TABLE targetedms.GeneralMolecule ADD ExplicitRetentionTimeWindow DOUBLE PRECISION;

ALTER TABLE targetedms.Molecule ADD MoleculeId NVARCHAR(200);

--** changes to GeneralPrecursor **--
ALTER TABLE targetedms.GeneralPrecursor ADD Ccs DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralPrecursor ADD ExplicitIonMobilityUnits VARCHAR(200);
ALTER TABLE targetedms.GeneralPrecursor ADD ExplicitCcsSqa DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralPrecursor ADD ExplicitCompensationVoltage DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralPrecursor ADD PrecursorConcentration DOUBLE PRECISION;
EXEC sp_rename 'targetedms.GeneralPrecursor.ExplicitDriftTimeMsec', 'ExplicitIonMobility', 'COLUMN';
ALTER TABLE targetedms.GeneralPrecursor DROP COLUMN ExplicitCollisionEnergy;
ALTER TABLE targetedms.GeneralPrecursor DROP COLUMN ExplicitDriftTimeHighEnergyOffsetMsec;
ALTER TABLE targetedms.GeneralPrecursor DROP COLUMN Decoy;
ALTER TABLE targetedms.GeneralPrecursor DROP COLUMN Modified;

--** changes to GeneralTransition **--
ALTER TABLE targetedms.GeneralTransition ADD Rank INT;
GO

UPDATE targetedms.GeneralTransition
SET Rank = (SELECT libraryRank FROM targetedms.Transition t WHERE t.id = targetedms.GeneralTransition.id);

ALTER TABLE targetedms.GeneralTransition ADD Intensity DOUBLE PRECISION;
Go

UPDATE targetedms.GeneralTransition
SET Intensity = (SELECT libraryIntensity FROM targetedms.Transition t WHERE t.id = targetedms.GeneralTransition.id);

ALTER TABLE targetedms.GeneralTransition ADD Quantitative BIT;
ALTER TABLE targetedms.GeneralTransition ADD CollisionEnergy DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralTransition ADD DeclusteringPotential DOUBLE PRECISION;
EXEC sp_rename 'targetedms.GeneralTransition.SLens', 'ExplicitSLens', 'COLUMN';
EXEC sp_rename 'targetedms.GeneralTransition.ConeVoltage', 'ExplicitConeVoltage', 'COLUMN';
EXEC sp_rename 'targetedms.GeneralTransition.ExplicitDriftTimeHighEnergyOffsetMSec', 'ExplicitIonMobilityHighEnergyOffset', 'COLUMN';
ALTER TABLE targetedms.GeneralTransition DROP COLUMN ExplicitCompensationVoltage;
ALTER TABLE targetedms.GeneralTransition DROP COLUMN ExplicitDriftTimeMSec;
ALTER TABLE targetedms.GeneralTransition DROP COLUMN Decoy;

ALTER TABLE targetedms.Transition DROP COLUMN LibraryRank;
ALTER TABLE targetedms.Transition DROP COLUMN LibraryIntensity;

ALTER TABLE targetedms.MoleculePrecursor ADD MoleculePrecursorId NVARCHAR(200);

ALTER TABLE targetedms.MoleculeTransition ADD MoleculeTransitionId NVARCHAR(200);

ALTER TABLE targetedms.GeneralMoleculeChromInfo ADD PredictedRetentionTime DOUBLE PRECISION;

ALTER TABLE targetedms.PrecursorChromInfo ADD Ccs DOUBLE PRECISION;
ALTER TABLE targetedms.PrecursorChromInfo ADD DriftTimeMs1 DOUBLE PRECISION;
ALTER TABLE targetedms.PrecursorChromInfo ADD DriftTimeFragment DOUBLE PRECISION;
ALTER TABLE targetedms.PrecursorChromInfo ADD DriftTimeWindow DOUBLE PRECISION;
ALTER TABLE targetedms.PrecursorChromInfo ADD IonMobilityMs1 DOUBLE PRECISION;
ALTER TABLE targetedms.PrecursorChromInfo ADD IonMobilityFragment DOUBLE PRECISION;
ALTER TABLE targetedms.PrecursorChromInfo ADD IonMobilityWindow DOUBLE PRECISION;
ALTER TABLE targetedms.PrecursorChromInfo ADD IonMobilityType NVARCHAR(200);

ALTER TABLE targetedms.TransitionChromInfo ADD Ccs DOUBLE PRECISION;
ALTER TABLE targetedms.TransitionChromInfo ADD DriftTime DOUBLE PRECISION;
ALTER TABLE targetedms.TransitionChromInfo ADD DriftTimeWindow DOUBLE PRECISION;
ALTER TABLE targetedms.TransitionChromInfo ADD IonMobility DOUBLE PRECISION;
ALTER TABLE targetedms.TransitionChromInfo ADD IonMobilityWindow DOUBLE PRECISION;
ALTER TABLE targetedms.TransitionChromInfo ADD IonMobilityType NVARCHAR(200);
ALTER TABLE targetedms.TransitionChromInfo ADD Rank INT;
ALTER TABLE targetedms.TransitionChromInfo ADD RankByLevel INTEGER;
ALTER TABLE targetedms.TransitionChromInfo ADD ForcedIntegration BIT;

ALTER TABLE targetedms.GroupComparisonSettings ADD AvgTechReplicates BIT;
ALTER TABLE targetedms.GroupComparisonSettings ADD SumTransitions BIT;
ALTER TABLE targetedms.GroupComparisonSettings ADD IncludeInteractionTransitions BIT;
ALTER TABLE targetedms.GroupComparisonSettings ADD SummarizationMethod NVARCHAR(200);

ALTER TABLE targetedms.Enzyme ADD Semi BIT;

ALTER TABLE targetedms.SpectrumLibrary ADD UseExplicitPeakBounds BIT;

ALTER TABLE targetedms.IsotopeModification ADD Label37Cl BIT;
ALTER TABLE targetedms.IsotopeModification ADD Label81Br BIT;

CREATE TABLE targetedms.BibliospecLibInfo
(
    Id INT IDENTITY(1, 1) NOT NULL,
    PrecursorId INT NOT NULL,
    SpectrumLibraryId INT NOT NULL,
    CountMeasured DOUBLE PRECISION,
    Score DOUBLE PRECISION,
    ScoreType VARCHAR(200),

    CONSTRAINT PK_BibliospecLibInfo PRIMARY KEY (Id),
    CONSTRAINT FK_BibliospecLibInfo_Precursor FOREIGN KEY (PrecursorId) REFERENCES targetedms.Precursor(Id),
    CONSTRAINT FK_BibliospecLibInfo_SpectrumLibrary FOREIGN KEY (SpectrumLibraryId) REFERENCES targetedms.SpectrumLibrary(Id)
);
CREATE INDEX IX_BibliospecLibInfo_PrecursorId ON targetedms.BibliospecLibInfo(PrecursorId);
CREATE INDEX IX_BibliospecLibInfo_SpectrumLibraryId ON targetedms.BibliospecLibInfo(SpectrumLibraryId);

INSERT INTO targetedms.BibliospecLibInfo (PrecursorId, SpectrumLibraryId, CountMeasured, Score, ScoreType)
SELECT PrecursorId, SpectrumLibraryId, Score1, Score2, NULL
FROM targetedms.PrecursorLibInfo pli
INNER JOIN targetedms.SpectrumLibrary sl ON (sl.id = pli.SpectrumLibraryId AND (sl.libraryType='bibliospec' OR sl.libraryType='bibliospec_lite')) ;

CREATE TABLE targetedms.HunterLibInfo
(
    Id INT IDENTITY(1, 1) NOT NULL,
    PrecursorId INT NOT NULL,
    SpectrumLibraryId INT NOT NULL,
    Expect DOUBLE PRECISION,
    ProcessedIntensity DOUBLE PRECISION,

    CONSTRAINT PK_HunterLibInfo PRIMARY KEY (Id),
    CONSTRAINT FK_HunterLibInfo_Precursor FOREIGN KEY (PrecursorId) REFERENCES targetedms.Precursor(Id),
    CONSTRAINT FK_HunterLibInfo_SpectrumLibrary FOREIGN KEY (SpectrumLibraryId) REFERENCES targetedms.SpectrumLibrary(Id)
);
CREATE INDEX IX_HunterLibInfo_PrecursorId ON targetedms.HunterLibInfo(PrecursorId);
CREATE INDEX IX_HunterLibInfo_SpectrumLibraryId ON targetedms.HunterLibInfo(SpectrumLibraryId);

INSERT INTO targetedms.HunterLibInfo (PrecursorId, SpectrumLibraryId, Expect, ProcessedIntensity)
SELECT PrecursorId, SpectrumLibraryId, Score1, Score2
FROM targetedms.PrecursorLibInfo pli
INNER JOIN targetedms.SpectrumLibrary sl ON (sl.id = pli.SpectrumLibraryId AND sl.libraryType='hunter') ;

CREATE TABLE targetedms.NistLibInfo
(
    Id INT IDENTITY(1, 1) NOT NULL,
    PrecursorId INT NOT NULL,
    SpectrumLibraryId INT NOT NULL,
    CountMeasured DOUBLE PRECISION,
    TotalIntensity DOUBLE PRECISION,
    TFRatio DOUBLE PRECISION,

    CONSTRAINT PK_NistLibInfo PRIMARY KEY (Id),
    CONSTRAINT FK_NistLibInfo_Precursor FOREIGN KEY (PrecursorId) REFERENCES targetedms.Precursor(Id),
    CONSTRAINT FK_NistLibInfo_SpectrumLibrary FOREIGN KEY (SpectrumLibraryId) REFERENCES targetedms.SpectrumLibrary(Id)
);
CREATE INDEX IX_NistLibInfo_PrecursorId ON targetedms.NistLibInfo(PrecursorId);
CREATE INDEX IX_NistLibInfo_SpectrumLibraryId ON targetedms.NistLibInfo(SpectrumLibraryId);

INSERT INTO targetedms.NistLibInfo (PrecursorId, SpectrumLibraryId, CountMeasured, TotalIntensity, TFRatio)
SELECT PrecursorId, SpectrumLibraryId, Score1, Score2, Score3
FROM targetedms.PrecursorLibInfo pli
INNER JOIN targetedms.SpectrumLibrary sl ON (sl.id = pli.SpectrumLibraryId AND sl.libraryType='nist') ;

CREATE TABLE targetedms.SpectrastLibInfo
(
    Id INT IDENTITY(1, 1) NOT NULL,
    PrecursorId INT NOT NULL,
    SpectrumLibraryId INT NOT NULL,
    CountMeasured DOUBLE PRECISION,
    TotalIntensity DOUBLE PRECISION,
    TFRatio DOUBLE PRECISION,

    CONSTRAINT PK_SpectrastLibInfo PRIMARY KEY (Id),
    CONSTRAINT FK_SpectrastLibInfo_Precursor FOREIGN KEY (PrecursorId) REFERENCES targetedms.Precursor(Id),
    CONSTRAINT FK_SpectrastLibInfo_SpectrumLibrary FOREIGN KEY (SpectrumLibraryId) REFERENCES targetedms.SpectrumLibrary(Id)
);
CREATE INDEX IX_SpectrastLibInfo_PrecursorId ON targetedms.SpectrastLibInfo(PrecursorId);
CREATE INDEX IX_SpectrastLibInfo_SpectrumLibraryId ON targetedms.SpectrastLibInfo(SpectrumLibraryId);

INSERT INTO targetedms.SpectrastLibInfo (PrecursorId, SpectrumLibraryId, CountMeasured, TotalIntensity, TFRatio)
SELECT PrecursorId, SpectrumLibraryId, Score1, Score2, Score3
FROM targetedms.PrecursorLibInfo pli
INNER JOIN targetedms.SpectrumLibrary sl ON (sl.id = pli.SpectrumLibraryId AND sl.libraryType='spectrast') ;

CREATE TABLE targetedms.ChromatogramLibInfo
(
    Id INT IDENTITY(1, 1) NOT NULL,
    PrecursorId INT NOT NULL,
    SpectrumLibraryId INT NOT NULL,
    PeakArea DOUBLE PRECISION,

    CONSTRAINT PK_ChromatogramLibInfo PRIMARY KEY (Id),
    CONSTRAINT FK_ChromatogramLibInfo_Precursor FOREIGN KEY (PrecursorId) REFERENCES targetedms.Precursor(Id),
    CONSTRAINT FK_ChromatogramLibInfo_SpectrumLibrary FOREIGN KEY (SpectrumLibraryId) REFERENCES targetedms.SpectrumLibrary(Id)
);
CREATE INDEX IX_ChromatogramLibInfo_PrecursorId ON targetedms.ChromatogramLibInfo(PrecursorId);
CREATE INDEX IX_ChromatogramLibInfo_SpectrumLibraryId ON targetedms.ChromatogramLibInfo(SpectrumLibraryId);

DROP TABLE targetedms.PrecursorLibInfo;

ALTER TABLE targetedms.SpectrumLibrary ADD PanoramaServer NVARCHAR(200);

INSERT INTO targetedms.IsotopeLabel (RunId, Name, Standard)
  SELECT DISTINCT pg.RunId AS RunId, 'Unknown' AS Name, 1 AS Standard
    FROM targetedms.GeneralPrecursor gp
    INNER JOIN targetedms.GeneralMolecule gm ON gp.GeneralMoleculeId = gm.Id
    INNER JOIN targetedms.PeptideGroup pg ON gm.PeptideGroupId = pg.Id

    WHERE gp.IsotopeLabelId IS NULL;

UPDATE targetedms.GeneralPrecursor SET IsotopeLabelId = (SELECT il.Id FROM
  targetedms.IsotopeLabel il
    INNER JOIN targetedms.PeptideGroup pg ON pg.RunId = il.RunId AND il.Name = 'Unknown' AND il.Standard = 1
    INNER JOIN targetedms.GeneralMolecule gm ON gm.PeptideGroupId = pg.Id AND gm.Id = GeneralPrecursor.GeneralMoleculeId
)
WHERE IsotopeLabelId IS NULL;

DROP INDEX targetedms.generalprecursor.IX_GeneralPrecursor_IsotopeLabelId;
GO

ALTER TABLE targetedms.GeneralPrecursor ALTER COLUMN IsotopeLabelId INT NOT NULL;

GO

CREATE INDEX IX_GeneralPrecursor_IsotopeLabelId ON targetedms.GeneralPrecursor(IsotopeLabelId);

GO

ALTER TABLE targetedms.AuditLogMessage ALTER COLUMN messageType NVARCHAR(100);

-- Add the columns we need to populate
ALTER TABLE targetedms.GeneralMoleculeChromInfo ADD ModifiedAreaProportion REAL;
ALTER TABLE targetedms.PrecursorChromInfo ADD PrecursorModifiedAreaProportion REAL;
GO

-- Create temp tables for perf
CREATE TABLE targetedms.PrecursorGroupings (Grouping NVARCHAR(300), PrecursorId INT);
CREATE TABLE targetedms.MoleculeGroupings (Grouping NVARCHAR(300), GeneralMoleculeId INT);
CREATE TABLE targetedms.areas (Grouping NVARCHAR(300), SampleFileId INT, Area REAL);
GO

-- Populate the temp tables
INSERT INTO targetedms.PrecursorGroupings (Grouping, PrecursorId)
    (SELECT DISTINCT
        COALESCE(gm.AttributeGroupId, p.Sequence, m.CustomIonName, m.IonFormula) AS Grouping,
        pci.PrecursorId
     FROM targetedms.PrecursorChromInfo pci INNER JOIN
              targetedms.GeneralPrecursor gp ON gp.Id = pci.PrecursorId INNER JOIN
              targetedms.GeneralMolecule gm ON gp.GeneralMoleculeId = gm.Id LEFT OUTER JOIN
              targetedms.Molecule m ON gm.Id = m.Id LEFT OUTER JOIN
              targetedms.Peptide p ON p.id = gp.GeneralMoleculeId);

INSERT INTO targetedms.MoleculeGroupings (Grouping, GeneralMoleculeId)
    (SELECT DISTINCT
       g.grouping,
       gp.GeneralMoleculeId
     FROM targetedms.PrecursorGroupings g INNER JOIN
            targetedms.GeneralPrecursor gp ON gp.Id = g.PrecursorId);

INSERT INTO targetedms.areas (Grouping, SampleFileId, Area)
    (SELECT
            g.grouping,
            pci.SampleFileId,
            SUM(pci.TotalArea) AS Area
     FROM targetedms.PrecursorChromInfo pci INNER JOIN
              targetedms.PrecursorGroupings g ON pci.PrecursorId =  g.PrecursorId
     GROUP BY g.grouping, pci.SampleFileId);

-- Create indices to make querying efficient
CREATE INDEX IX_PrecursorGroupings ON targetedms.PrecursorGroupings (PrecursorId, Grouping);
CREATE INDEX IX_MoleculeGroupings ON targetedms.MoleculeGroupings (GeneralMoleculeId, Grouping);
CREATE INDEX IX_areas ON targetedms.areas (Grouping, SampleFileId);
GO

UPDATE targetedms.PrecursorChromInfo SET PrecursorModifiedAreaProportion =
                                             (SELECT CASE WHEN X.PrecursorAreaInReplicate = 0 THEN NULL ELSE TotalArea / X.PrecursorAreaInReplicate END
                                              FROM
                                                   (SELECT Area AS PrecursorAreaInReplicate
                                                    FROM targetedms.areas a INNER JOIN
                                                             targetedms.PrecursorGroupings g ON a.grouping = g.grouping
                                                    WHERE g.PrecursorId = targetedms.PrecursorChromInfo.PrecursorId AND
                                                          a.SampleFileId = targetedms.PrecursorChromInfo.SampleFileId) X);

UPDATE targetedms.GeneralMoleculeChromInfo SET ModifiedAreaProportion =
                                                   (SELECT CASE WHEN X.MoleculeAreaInReplicate = 0 THEN NULL ELSE
                                                               (SELECT SUM(TotalArea) FROM targetedms.PrecursorChromInfo pci
                                                                WHERE pci.GeneralMoleculeChromInfoId = targetedms.GeneralMoleculeChromInfo.Id)
                                                                    / X.MoleculeAreaInReplicate END
                                                    FROM (
                                                         SELECT SUM(a.Area) AS MoleculeAreaInReplicate
                                                         FROM targetedms.areas a INNER JOIN
                                                                  targetedms.MoleculeGroupings g ON a.grouping = g.grouping
                                                         WHERE g.GeneralMoleculeId = targetedms.GeneralMoleculeChromInfo.GeneralMoleculeId
                                                           AND a.SampleFileId = targetedms.GeneralMoleculeChromInfo.SampleFileId) X);

-- Get rid of the temp tables
DROP TABLE targetedms.PrecursorGroupings;
DROP TABLE targetedms.MoleculeGroupings;
DROP TABLE targetedms.Areas;

ALTER TABLE targetedms.IsotopeModification ALTER COLUMN AminoAcid NVARCHAR(30);

ALTER TABLE targetedms.Molecule ALTER COLUMN MoleculeId NVARCHAR(500);
ALTER TABLE targetedms.MoleculePrecursor ALTER COLUMN MoleculePrecursorId NVARCHAR(500);
ALTER TABLE targetedms.MoleculeTransition ALTER COLUMN MoleculeTransitionId NVARCHAR(500);

-- Drop the FK constraint
ALTER TABLE targetedms.spectrumlibrary DROP CONSTRAINT fk_spectrumlibrary_librarysourceid;
-- Drop the index before dropping the column
DROP INDEX targetedms.spectrumlibrary.IX_SpectrumLibrary_LibrarySourceId;
GO

ALTER TABLE targetedms.spectrumlibrary DROP COLUMN librarysourceid;
DROP TABLE targetedms.librarysource;

ALTER TABLE targetedms.moleculeprecursor DROP COLUMN moleculeprecursorid;

ALTER TABLE targetedms.LibrarySettings ADD ionMatchTolerance DOUBLE PRECISION;

-- Issue 38773: Remove PanoramaPublic tables from the targetedms schema
DROP TABLE targetedms.JournalExperiment;
DROP TABLE targetedms.Journal;
DROP TABLE targetedms.ExperimentAnnotations;

-- Issue 40487: Add a Modified column to targetedms.GeneralPrecursor
ALTER TABLE targetedms.GeneralPrecursor ADD Modified DATETIME;
GO

-- The value in targetedms.runs.Modified should be a good substitute so we will use this value to populate the new column
UPDATE targetedms.GeneralPrecursor SET Modified = r.Modified
FROM targetedms.runs r
INNER JOIN targetedms.PeptideGroup pg ON pg.RunId = r.id
INNER JOIN targetedms.GeneralMolecule gm ON gm.PeptideGroupId = pg.Id
WHERE gm.Id = GeneralMoleculeId;

ALTER TABLE targetedms.transition add complexFragmentIon NVARCHAR(MAX);

CREATE TABLE targetedms.keywordcategories (
  id INT IDENTITY(1, 1) NOT NULL,
  categoryid NVARCHAR(10),
  label NVARCHAR(100) NOT NULL,
  CONSTRAINT pk_keywordcategories PRIMARY KEY (id),
  CONSTRAINT keywordcategories_categoryid_key UNIQUE (categoryid)
);
GO

CREATE TABLE targetedms.keywords (
 id INT IDENTITY(1, 1) NOT NULL,
 keywordid NVARCHAR(10) NOT NULL,
 keyword NVARCHAR(100) NOT NULL,
 category NVARCHAR(10) NOT NULL,
 CONSTRAINT pk_keywords PRIMARY KEY (id),
 CONSTRAINT fk_keyword_category FOREIGN KEY (category) REFERENCES targetedms.keywordcategories (categoryid),
 CONSTRAINT keywords_keywordid_key UNIQUE (keywordid)
);
GO


INSERT INTO targetedms.keywordcategories VALUES ( 'KW-9999', 'Biological process');
INSERT INTO targetedms.keywordcategories VALUES ( 'KW-9998', 'Cellular component');
INSERT INTO targetedms.keywordcategories VALUES ( 'KW-9997', 'Coding sequence diversity');
INSERT INTO targetedms.keywordcategories VALUES ( 'KW-9996', 'Developmental stage');
INSERT INTO targetedms.keywordcategories VALUES ( 'KW-9995', 'Disease');
INSERT INTO targetedms.keywordcategories VALUES ( 'KW-9994', 'Domain');
INSERT INTO targetedms.keywordcategories VALUES ( 'KW-9993', 'Ligand');
INSERT INTO targetedms.keywordcategories VALUES ( 'KW-9992', 'Molecular function');
INSERT INTO targetedms.keywordcategories VALUES ( 'KW-9991', 'PTM');
INSERT INTO targetedms.keywordcategories VALUES ( 'KW-9990', 'Technical term');
GO

INSERT INTO targetedms.keywords VALUES (  'KW-0001', '2Fe-2S', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0002', '3D-structure', 'KW-9990');
INSERT INTO targetedms.keywords VALUES (  'KW-0003', '3Fe-4S', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0004', '4Fe-4S', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0005', 'Acetoin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0006', 'Acetoin catabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0007', 'Acetylation', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0008', 'Acetylcholine receptor inhibiting toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0009', 'Actin-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0010', 'Activator', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0011', 'Acute phase', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0012', 'Acyltransferase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0013', 'ADP-ribosylation', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0014', 'AIDS', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0015', 'Albinism', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0016', 'Alginate biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0017', 'Alkaloid metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0019', 'Alkylphosphonate uptake', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0020', 'Allergen', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0021', 'Allosteric enzyme', 'KW-9990');
INSERT INTO targetedms.keywords VALUES (  'KW-0022', 'Alpha-amylase inhibitor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0023', 'Alport syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0024', 'Alternative initiation', 'KW-9997');
INSERT INTO targetedms.keywords VALUES (  'KW-0025', 'Alternative splicing', 'KW-9997');
INSERT INTO targetedms.keywords VALUES (  'KW-0026', 'Alzheimer disease', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0027', 'Amidation', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0028', 'Amino-acid biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0029', 'Amino-acid transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0030', 'Aminoacyl-tRNA synthetase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0031', 'Aminopeptidase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0032', 'Aminotransferase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0034', 'Amyloid', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0035', 'Amyloplast', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0036', 'Amyotrophic lateral sclerosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0037', 'Angiogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0038', 'Ectodermal dysplasia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0039', 'Anion exchange', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0040', 'ANK repeat', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0041', 'Annexin', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0042', 'Antenna complex', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0043', 'Tumor suppressor', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0044', 'Antibiotic', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0045', 'Antibiotic biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0046', 'Antibiotic resistance', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0047', 'Antifreeze protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0049', 'Antioxidant', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0050', 'Antiport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0051', 'Antiviral defense', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0052', 'Apoplast', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0053', 'Apoptosis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0054', 'Arabinose catabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0055', 'Arginine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0056', 'Arginine metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0057', 'Aromatic amino acid biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0058', 'Aromatic hydrocarbons catabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0059', 'Arsenical resistance', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0060', 'Ascorbate biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0061', 'Asparagine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0062', 'Aspartic protease inhibitor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0063', 'Aspartyl esterase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0064', 'Aspartyl protease', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0065', 'Atherosclerosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0066', 'ATP synthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0067', 'ATP-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0068', 'Autocatalytic cleavage', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0069', 'Autoimmune encephalomyelitis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0070', 'Autoimmune uveitis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0071', 'Autoinducer synthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0072', 'Autophagy', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0073', 'Auxin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0075', 'B-cell activation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0076', 'Bacteriochlorophyll', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0077', 'Bacteriochlorophyll biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0078', 'Bacteriocin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0079', 'Bacteriocin immunity', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0080', 'Bacteriocin transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0081', 'Bacteriolytic enzyme', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0082', 'Bait region', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0083', 'Bardet-Biedl syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0084', 'Basement membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0085', 'Behavior', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0086', 'Bence-Jones protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0087', 'Bernard Soulier syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0088', 'Bile acid catabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0089', 'Bile pigment', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0090', 'Biological rhythms', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0091', 'Biomineralization', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0092', 'Biotin', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0093', 'Biotin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0094', 'Blood coagulation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0095', 'Blood group antigen', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0100', 'Branched-chain amino acid biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0101', 'Branched-chain amino acid catabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0102', 'Bromination', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0103', 'Bromodomain', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0104', 'Cadmium', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0105', 'Cadmium resistance', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0106', 'Calcium', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0107', 'Calcium channel', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0108', 'Calcium channel impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0109', 'Calcium transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0111', 'Calcium/phospholipid-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0112', 'Calmodulin-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0113', 'Calvin cycle', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0114', 'cAMP', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0115', 'cAMP biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0116', 'cAMP-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0117', 'Actin capping', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0118', 'Viral capsid assembly', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0119', 'Carbohydrate metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0120', 'Carbon dioxide fixation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0121', 'Carboxypeptidase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0122', 'Cardiomyopathy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0123', 'Cardiotoxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0124', 'Carnitine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0125', 'Carotenoid biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0127', 'Catecholamine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0128', 'Catecholamine metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0129', 'CBS domain', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0130', 'Cell adhesion', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0131', 'Cell cycle', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0132', 'Cell division', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0133', 'Cell shape', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0134', 'Cell wall', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0135', 'Cellulose biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0136', 'Cellulose degradation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0137', 'Centromere', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0138', 'CF(0)', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0139', 'CF(1)', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0140', 'cGMP', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0141', 'cGMP biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0142', 'cGMP-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0143', 'Chaperone', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0144', 'Charcot-Marie-Tooth disease', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0145', 'Chemotaxis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0146', 'Chitin degradation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0147', 'Chitin-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0148', 'Chlorophyll', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0149', 'Chlorophyll biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0150', 'Chloroplast', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0151', 'Chlorosome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0152', 'Cholesterol biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0153', 'Cholesterol metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0155', 'Chromate resistance', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0156', 'Chromatin regulator', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0157', 'Chromophore', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0158', 'Chromosome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0159', 'Chromosome partition', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0160', 'Chromosomal rearrangement', 'KW-9997');
INSERT INTO targetedms.keywords VALUES (  'KW-0161', 'Chronic granulomatous disease', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0162', 'Chylomicron', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0163', 'Citrate utilization', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0164', 'Citrullination', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0165', 'Cleavage on pair of basic residues', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0166', 'Nematocyst', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0167', 'Capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0168', 'Coated pit', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0169', 'Cobalamin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0170', 'Cobalt', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0171', 'Cobalt transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0172', 'Cockayne syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0173', 'Coenzyme A biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0174', 'Coenzyme M biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0175', 'Coiled coil', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0176', 'Collagen', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0177', 'Collagen degradation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0178', 'Competence', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0179', 'Complement alternate pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0180', 'Complement pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0181', 'Complete proteome', 'KW-9990');
INSERT INTO targetedms.keywords VALUES (  'KW-0182', 'Cone-rod dystrophy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0183', 'Conidiation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0184', 'Conjugation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0186', 'Copper', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0187', 'Copper transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0188', 'Copulatory plug', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0190', 'Covalent protein-DNA linkage', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0191', 'Covalent protein-RNA linkage', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0192', 'Crown gall tumor', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0193', 'Cuticle', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0194', 'Cyanelle', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0195', 'Cyclin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0196', 'Cycloheximide resistance', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0197', 'Cyclosporin', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0198', 'Cysteine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0199', 'Cystinuria', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0200', 'Cytadherence', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0201', 'Cytochrome c-type biogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0202', 'Cytokine', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0203', 'Cytokinin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0204', 'Cytolysis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0205', 'Cytosine metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0206', 'Cytoskeleton', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0208', 'D-amino acid', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0209', 'Deafness', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0210', 'Decarboxylase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0211', 'Defensin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0213', 'Dejerine-Sottas syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0214', 'Dental caries', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0215', 'Deoxyribonucleotide synthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0216', 'Detoxification', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0217', 'Developmental protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0218', 'Diabetes insipidus', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0219', 'Diabetes mellitus', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0220', 'Diaminopimelate biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0221', 'Differentiation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0222', 'Digestion', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0223', 'Dioxygenase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0224', 'Dipeptidase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0225', 'Disease mutation', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0226', 'DNA condensation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0227', 'DNA damage', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0228', 'DNA excision', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0229', 'DNA integration', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0230', 'DNA invertase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0231', 'Viral genome packaging', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0233', 'DNA recombination', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0234', 'DNA repair', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0235', 'DNA replication', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0236', 'DNA replication inhibitor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0237', 'DNA synthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0238', 'DNA-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0239', 'DNA-directed DNA polymerase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0240', 'DNA-directed RNA polymerase', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0241', 'Down syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0242', 'Dwarfism', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0243', 'Dynein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0244', 'Early protein', 'KW-9996');
INSERT INTO targetedms.keywords VALUES (  'KW-0245', 'EGF-like domain', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0248', 'Ehlers-Danlos syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0249', 'Electron transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0250', 'Elliptocytosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0251', 'Elongation factor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0254', 'Endocytosis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0255', 'Endonuclease', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0256', 'Endoplasmic reticulum', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0257', 'Endorphin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0259', 'Enterobactin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0260', 'Enterotoxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0261', 'Viral envelope protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0263', 'Epidermolysis bullosa', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0265', 'Erythrocyte maturation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0266', 'Ethylene biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0267', 'Excision nuclease', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0268', 'Exocytosis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0269', 'Exonuclease', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0270', 'Exopolysaccharide synthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0271', 'Exosome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0272', 'Extracellular matrix', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0273', 'Eye lens protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0274', 'FAD', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0275', 'Fatty acid biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0276', 'Fatty acid metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0278', 'Fertilization', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0280', 'Fibrinolysis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0281', 'Fimbrium', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0282', 'Flagellum', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0283', 'Flagellar rotation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0284', 'Flavonoid biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0285', 'Flavoprotein', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0286', 'Flight', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0287', 'Flowering', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0288', 'FMN', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0289', 'Folate biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0290', 'Folate-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0291', 'Formylation', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0292', 'Fruit ripening', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0293', 'Fruiting body', 'KW-9996');
INSERT INTO targetedms.keywords VALUES (  'KW-0294', 'Fucose metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0295', 'Fungicide', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0297', 'G-protein coupled receptor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0298', 'Galactitol metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0299', 'Galactose metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0301', 'Gamma-carboxyglutamic acid', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0302', 'Gap protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0303', 'Gap junction', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0304', 'Gas vesicle', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0305', 'Gaseous exchange', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0306', 'Gastrulation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0307', 'Gaucher disease', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0308', 'Genetically modified food', 'KW-9990');
INSERT INTO targetedms.keywords VALUES (  'KW-0309', 'Germination', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0311', 'Gluconate utilization', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0312', 'Gluconeogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0313', 'Glucose metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0314', 'Glutamate biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0315', 'Glutamine amidotransferase', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0316', 'Glutaricaciduria', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0317', 'Glutathione biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0318', 'Glutathionylation', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0319', 'Glycerol metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0320', 'Glycogen biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0321', 'Glycogen metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0322', 'Glycogen storage disease', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0323', 'Glycolate pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0324', 'Glycolysis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0325', 'Glycoprotein', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0326', 'Glycosidase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0327', 'Glycosome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0328', 'Glycosyltransferase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0329', 'Glyoxylate bypass', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0330', 'Glyoxysome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0331', 'Gangliosidosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0332', 'GMP biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0333', 'Golgi apparatus', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0334', 'Gonadal differentiation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0335', 'Gout', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0336', 'GPI-anchor', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0337', 'GPI-anchor biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0338', 'Growth arrest', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0339', 'Growth factor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0340', 'Growth factor binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0341', 'Growth regulation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0342', 'GTP-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0343', 'GTPase activation', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0344', 'Guanine-nucleotide releasing factor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0345', 'HDL', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0346', 'Stress response', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0347', 'Helicase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0348', 'Hemagglutinin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0349', 'Heme', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0350', 'Heme biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0351', 'Hemoglobin-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0353', 'Hemolymph clotting', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0354', 'Hemolysis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0355', 'Hemophilia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0356', 'Hemostasis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0357', 'Heparan sulfate', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0358', 'Heparin-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0359', 'Herbicide resistance', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0360', 'Hereditary hemolytic anemia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0361', 'Hereditary multiple exostoses', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0362', 'Hereditary nonpolyposis colorectal cancer', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0363', 'Hermansky-Pudlak syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0364', 'Heterocyst', 'KW-9996');
INSERT INTO targetedms.keywords VALUES (  'KW-0367', 'Hirschsprung disease', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0368', 'Histidine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0369', 'Histidine metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0370', 'Holoprosencephaly', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0371', 'Homeobox', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0372', 'Hormone', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0373', 'Hyaluronic acid', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0374', 'Hybridoma', 'KW-9990');
INSERT INTO targetedms.keywords VALUES (  'KW-0375', 'Hydrogen ion transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0376', 'Hydrogen peroxide', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0377', 'Hydrogenosome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0378', 'Hydrolase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0379', 'Hydroxylation', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0380', 'Hyperlipidemia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0381', 'Hypersensitive response', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0382', 'Hypotensive agent', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0385', 'Hypusine', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0386', 'Hypusine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0387', 'Ice nucleation', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0388', 'IgA-binding protein', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0389', 'IgE-binding protein', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0390', 'IgG-binding protein', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0391', 'Immunity', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0392', 'Immunoglobulin C region', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0393', 'Immunoglobulin domain', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0394', 'Immunoglobulin V region', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0395', 'Inflammatory response', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0396', 'Initiation factor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0398', 'Inositol biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0399', 'Innate immunity', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0401', 'Integrin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0403', 'Intermediate filament', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0404', 'Intron homing', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0405', 'Iodination', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0406', 'Ion transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0407', 'Ion channel', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0408', 'Iron', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0409', 'Iron storage', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0410', 'Iron transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0411', 'Iron-sulfur', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0412', 'Isoleucine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0413', 'Isomerase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0414', 'Isoprene biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0415', 'Karyogamy', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0416', 'Keratin', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0417', 'Keratinization', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0418', 'Kinase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0419', 'Kinetoplast', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0420', 'Kringle', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0421', 'Lactation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0422', 'Lactose biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0423', 'Lactose metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0424', 'Laminin EGF-like domain', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0425', 'Lantibiotic', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0426', 'Late protein', 'KW-9996');
INSERT INTO targetedms.keywords VALUES (  'KW-0427', 'LDL', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0428', 'Leader peptide', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0429', 'Leber hereditary optic neuropathy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0430', 'Lectin', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0431', 'Leigh syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0432', 'Leucine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0433', 'Leucine-rich repeat', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0434', 'Leukotriene biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0435', 'Li-Fraumeni syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0436', 'Ligase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0437', 'Light-harvesting polypeptide', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0438', 'Lignin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0439', 'Lignin degradation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0440', 'LIM domain', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0441', 'Lipid A biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0442', 'Lipid degradation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0443', 'Lipid metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0444', 'Lipid biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0445', 'Lipid transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0446', 'Lipid-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0448', 'Lipopolysaccharide biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0449', 'Lipoprotein', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0450', 'Lipoyl', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0451', 'Lissencephaly', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0452', 'Lithium', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0454', 'Long QT syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0455', 'Luminescence', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0456', 'Lyase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0457', 'Lysine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0458', 'Lysosome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0460', 'Magnesium', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0461', 'Malaria', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0462', 'Maltose metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0463', 'Mandelate pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0464', 'Manganese', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0465', 'Mannose-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0466', 'Maple syrup urine disease', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0467', 'Mast cell degranulation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0468', 'Viral matrix protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0469', 'Meiosis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0470', 'Melanin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0471', 'Melatonin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0472', 'Membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0473', 'Membrane attack complex', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0474', 'Menaquinone biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0475', 'Mercuric resistance', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0476', 'Mercury', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0477', 'Merozoite', 'KW-9996');
INSERT INTO targetedms.keywords VALUES (  'KW-0478', 'Metachromatic leukodystrophy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0479', 'Metal-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0480', 'Metal-thiolate cluster', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0481', 'Metalloenzyme inhibitor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0482', 'Metalloprotease', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0483', 'Metalloprotease inhibitor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0484', 'Methanogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0485', 'Methanol utilization', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0486', 'Methionine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0487', 'Methotrexate resistance', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0488', 'Methylation', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0489', 'Methyltransferase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0490', 'MHC I', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0491', 'MHC II', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0492', 'Microsome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0493', 'Microtubule', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0494', 'Milk protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0495', 'Mineral balance', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0496', 'Mitochondrion', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0497', 'Mitogen', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0498', 'Mitosis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0499', 'Mobility protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0500', 'Molybdenum', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0501', 'Molybdenum cofactor biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0502', 'Monoclonal antibody', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0503', 'Monooxygenase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0504', 'Morphogen', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0505', 'Motor protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0506', 'mRNA capping', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0507', 'mRNA processing', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0508', 'mRNA splicing', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0509', 'mRNA transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0510', 'Mucopolysaccharidosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0511', 'Multifunctional enzyme', 'KW-9990');
INSERT INTO targetedms.keywords VALUES (  'KW-0514', 'Muscle protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0515', 'Mutator protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0517', 'Myogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0518', 'Myosin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0519', 'Myristate', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0520', 'NAD', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0521', 'NADP', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0523', 'Neurodegeneration', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0524', 'Neurogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0525', 'Neuronal ceroid lipofuscinosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0527', 'Neuropeptide', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0528', 'Neurotoxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0529', 'Neurotransmitter', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0530', 'Neurotransmitter biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0531', 'Neurotransmitter degradation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0532', 'Neurotransmitter transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0533', 'Nickel', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0534', 'Nitrate assimilation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0535', 'Nitrogen fixation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0536', 'Nodulation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0539', 'Nucleus', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0540', 'Nuclease', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0542', 'Nucleomorph', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0543', 'Viral nucleoprotein', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0544', 'Nucleosome core', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0545', 'Nucleotide biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0546', 'Nucleotide metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0547', 'Nucleotide-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0548', 'Nucleotidyltransferase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0549', 'Nylon degradation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0550', 'Obesity', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0551', 'Lipid droplet', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0552', 'Olfaction', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0553', 'Oncogene', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0554', 'One-carbon metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0555', 'Opioid peptide', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0556', 'Organic radical', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0558', 'Oxidation', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0560', 'Oxidoreductase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0561', 'Oxygen transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0562', 'Pair-rule protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0563', 'Paired box', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0564', 'Palmitate', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0566', 'Pantothenate biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0568', 'Pathogenesis-related protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0570', 'Pentose shunt', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0571', 'Peptide transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0572', 'Peptidoglycan-anchor', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0573', 'Peptidoglycan synthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0574', 'Periplasm', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0575', 'Peroxidase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0576', 'Peroxisome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0577', 'PHA biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0578', 'Host cell lysis by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0581', 'Phagocytosis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0582', 'Pharmaceutical', 'KW-9990');
INSERT INTO targetedms.keywords VALUES (  'KW-0583', 'PHB biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0584', 'Phenylalanine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0585', 'Phenylalanine catabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0586', 'Phenylketonuria', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0587', 'Phenylpropanoid metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0588', 'Pheromone', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0589', 'Pheromone response', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0590', 'Pheromone-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0592', 'Phosphate transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0593', 'Phospholipase A2 inhibitor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0594', 'Phospholipid biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0595', 'Phospholipid degradation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0596', 'Phosphopantetheine', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0597', 'Phosphoprotein', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0598', 'Phosphotransferase system', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0599', 'Photoprotein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0600', 'Photoreceptor protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0601', 'Photorespiration', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0602', 'Photosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0603', 'Photosystem I', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0604', 'Photosystem II', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0605', 'Phycobilisome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0607', 'Phytochrome signaling pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0608', 'Pigment', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0611', 'Plant defense', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0614', 'Plasmid', 'KW-9990');
INSERT INTO targetedms.keywords VALUES (  'KW-0615', 'Plasmid copy control', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0616', 'Plasmid partition', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0617', 'Plasminogen activation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0618', 'Plastoquinone', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0620', 'Polyamine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0621', 'Polymorphism', 'KW-9997');
INSERT INTO targetedms.keywords VALUES (  'KW-0622', 'Neuropathy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0624', 'Polysaccharide degradation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0625', 'Polysaccharide transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0626', 'Porin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0627', 'Porphyrin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0628', 'Postsynaptic cell membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0629', 'Postsynaptic neurotoxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0630', 'Potassium', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0631', 'Potassium channel', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0632', 'Potassium channel impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0633', 'Potassium transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0634', 'PQQ', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0635', 'Pregnancy', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0636', 'Prenylation', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0637', 'Prenyltransferase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0638', 'Presynaptic neurotoxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0639', 'Primosome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0640', 'Prion', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0641', 'Proline biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0642', 'Proline metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0643', 'Prostaglandin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0644', 'Prostaglandin metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0645', 'Protease', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0646', 'Protease inhibitor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0647', 'Proteasome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0648', 'Protein biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0649', 'Protein kinase inhibitor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0650', 'Protein phosphatase inhibitor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0651', 'Protein splicing', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0652', 'Protein synthesis inhibitor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0653', 'Protein transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0654', 'Proteoglycan', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0655', 'Prothrombin activator', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0656', 'Proto-oncogene', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0657', 'Pseudohermaphroditism', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0658', 'Purine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0659', 'Purine metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0660', 'Purine salvage', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0661', 'Putrescine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0662', 'Pyridine nucleotide biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0663', 'Pyridoxal phosphate', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0664', 'Pyridoxine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0665', 'Pyrimidine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0666', 'Pyrogen', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0668', 'Pyropoikilocytosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0669', 'Pyrrolysine', 'KW-9997');
INSERT INTO targetedms.keywords VALUES (  'KW-0670', 'Pyruvate', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0671', 'Queuosine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0672', 'Quinate metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0673', 'Quorum sensing', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0674', 'Reaction center', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0675', 'Receptor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0676', 'Redox-active center', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0677', 'Repeat', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0678', 'Repressor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0679', 'Respiratory chain', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0680', 'Restriction system', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0681', 'Retinal protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0682', 'Retinitis pigmentosa', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0683', 'Retinol-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0684', 'Rhamnose metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0685', 'Rhizomelic chondrodysplasia punctata', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0686', 'Riboflavin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0687', 'Ribonucleoprotein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0688', 'Ribosomal frameshifting', 'KW-9997');
INSERT INTO targetedms.keywords VALUES (  'KW-0689', 'Ribosomal protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0690', 'Ribosome biogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0691', 'RNA editing', 'KW-9997');
INSERT INTO targetedms.keywords VALUES (  'KW-0692', 'RNA repair', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0693', 'Viral RNA replication', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0694', 'RNA-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0695', 'RNA-directed DNA polymerase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0696', 'RNA-directed RNA polymerase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0697', 'Rotamase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0698', 'rRNA processing', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0699', 'rRNA-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0701', 'S-layer', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0702', 'S-nitrosylation', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0703', 'Sarcoplasmic reticulum', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0704', 'Schiff base', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0705', 'SCID', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0708', 'Seed storage protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0709', 'Segmentation polarity protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0711', 'Selenium', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0712', 'Selenocysteine', 'KW-9997');
INSERT INTO targetedms.keywords VALUES (  'KW-0713', 'Self-incompatibility', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0716', 'Sensory transduction', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0717', 'Septation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0718', 'Serine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0719', 'Serine esterase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0720', 'Serine protease', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0721', 'Serine protease homolog', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0722', 'Serine protease inhibitor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0723', 'Serine/threonine-protein kinase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0724', 'Serotonin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0726', 'Sexual differentiation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0727', 'SH2 domain', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0728', 'SH3 domain', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0729', 'SH3-binding', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0730', 'Sialic acid', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0731', 'Sigma factor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0732', 'Signal', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0733', 'Signal recognition particle', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0734', 'Signal transduction inhibitor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0735', 'Signal-anchor', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0736', 'Signalosome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0737', 'Silk protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0738', 'Voltage-gated sodium channel impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0739', 'Sodium transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0740', 'Sodium/potassium transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0741', 'SOS mutagenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0742', 'SOS response', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0744', 'Spermatogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0745', 'Spermidine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0746', 'Sphingolipid metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0747', 'Spliceosome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0748', 'Sporozoite', 'KW-9996');
INSERT INTO targetedms.keywords VALUES (  'KW-0749', 'Sporulation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0750', 'Starch biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0751', 'Stargardt disease', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0752', 'Steroid biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0753', 'Steroid metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0754', 'Steroid-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0755', 'Steroidogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0756', 'Sterol biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0757', 'Stickler syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0758', 'Storage protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0759', 'Streptomycin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0762', 'Sugar transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0763', 'Sulfate respiration', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0764', 'Sulfate transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0765', 'Sulfation', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0766', 'Superantigen', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0767', 'Surface film', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0768', 'Sushi', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0769', 'Symport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0770', 'Synapse', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0771', 'Synaptosome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0772', 'Systemic lupus erythematosus', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0776', 'Taste-modifying protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0777', 'Teichoic acid biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0778', 'Tellurium resistance', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0779', 'Telomere', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0780', 'Terminal addition', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0783', 'Tetrahydrobiopterin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0784', 'Thiamine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0785', 'Thiamine catabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0786', 'Thiamine pyrophosphate', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0787', 'Thick filament', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0788', 'Thiol protease', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0789', 'Thiol protease inhibitor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0791', 'Threonine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0792', 'Thrombophilia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0793', 'Thylakoid', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0795', 'Thyroid hormone', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0796', 'Tight junction', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0797', 'Tissue remodeling', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0798', 'TonB box', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0799', 'Topoisomerase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0800', 'Toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0801', 'TPQ', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0802', 'TPR repeat', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0804', 'Transcription', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0805', 'Transcription regulation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0806', 'Transcription termination', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0807', 'Transducer', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0808', 'Transferase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0809', 'Transit peptide', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0810', 'Translation regulation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0811', 'Translocation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0812', 'Transmembrane', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0813', 'Transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0814', 'Transposable element', 'KW-9990');
INSERT INTO targetedms.keywords VALUES (  'KW-0815', 'Transposition', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0816', 'Tricarboxylic acid cycle', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0817', 'Trimethoprim resistance', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0818', 'Triplet repeat expansion', 'KW-9997');
INSERT INTO targetedms.keywords VALUES (  'KW-0819', 'tRNA processing', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0820', 'tRNA-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0821', 'Trypanosomiasis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0822', 'Tryptophan biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0823', 'Tryptophan catabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0824', 'TTQ', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0825', 'Tumor antigen', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0826', 'Tungsten', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0827', 'Tyrosine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0828', 'Tyrosine catabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0829', 'Tyrosine-protein kinase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0830', 'Ubiquinone', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0831', 'Ubiquinone biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0832', 'Ubl conjugation', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0833', 'Ubl conjugation pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0834', 'Unfolded protein response', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0835', 'Urea cycle', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0836', 'Usher syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0837', 'Vanadium', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0838', 'Vasoactive', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0839', 'Vasoconstrictor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0840', 'Vasodilator', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0842', 'Viral occlusion body', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0843', 'Virulence', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0844', 'Vision', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0845', 'Vitamin A', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0846', 'Cobalamin', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0847', 'Vitamin C', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0848', 'Vitamin D', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0850', 'VLDL', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0851', 'Voltage-gated channel', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0852', 'von Willebrand disease', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0853', 'WD repeat', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0855', 'Whooping cough', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0856', 'Williams-Beuren syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0857', 'Xeroderma pigmentosum', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0858', 'Xylan degradation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0859', 'Xylose metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0861', 'Zellweger syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0862', 'Zinc', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0863', 'Zinc-finger', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0864', 'Zinc transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0865', 'Zymogen', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0866', 'Nonsense-mediated mRNA decay', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0867', 'MELAS syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0868', 'Chloride', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0869', 'Chloride channel', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0870', 'Voltage-gated chloride channel impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0871', 'Bacteriocin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0872', 'Ion channel impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0873', 'Pyrrolidone carboxylic acid', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0874', 'Quinone', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0875', 'Capsule', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0876', 'Taxol biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0877', 'Alternative promoter usage', 'KW-9997');
INSERT INTO targetedms.keywords VALUES (  'KW-0878', 'Amphibian defense peptide', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0879', 'Wnt signaling pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0880', 'Kelch repeat', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0881', 'Chlorophyll catabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0882', 'Thioester bond', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0883', 'Thioether bond', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0884', 'PQQ biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0885', 'CTQ', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0886', 'LTQ', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0887', 'Epilepsy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0888', 'Threonine protease', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0889', 'Transcription antitermination', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0890', 'Hereditary spastic paraplegia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0891', 'Chondrogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0892', 'Osteogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0893', 'Thyroid hormones biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0894', 'Sodium channel', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0895', 'ERV', 'KW-9990');
INSERT INTO targetedms.keywords VALUES (  'KW-0896', 'Oogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0897', 'Waardenburg syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0898', 'Cataract', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0899', 'Viral immunoevasion', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0900', 'Congenital disorder of glycosylation', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0901', 'Leber congenital amaurosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0902', 'Two-component regulatory system', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0903', 'Direct protein sequencing', 'KW-9990');
INSERT INTO targetedms.keywords VALUES (  'KW-0904', 'Protein phosphatase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0905', 'Primary microcephaly', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0906', 'Nuclear pore complex', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0907', 'Parkinson disease', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0908', 'Parkinsonism', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0909', 'Hibernation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0910', 'Bartter syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0911', 'Desmin-related myopathy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0912', 'Congenital muscular dystrophy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0913', 'Age-related macular degeneration', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0914', 'Notch signaling pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0915', 'Sodium', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0916', 'Viral movement protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0917', 'Virion maturation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0918', 'Phosphonate transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0919', 'Taste', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0920', 'Virion tegument', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0921', 'Nickel transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0922', 'Interferon antiviral system evasion', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0923', 'Fanconi anemia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0924', 'Ammonia transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0925', 'Oxylipin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0926', 'Vacuole', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0927', 'Auxin signaling pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0928', 'Hypersensitive response elicitation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0929', 'Antimicrobial', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0930', 'Antiviral protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0931', 'ER-Golgi transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0932', 'Cytokinin signaling pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0933', 'Apicoplast', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0934', 'Plastid', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0935', 'Progressive external ophthalmoplegia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0936', 'Ethylene signaling pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0937', 'Abscisic acid biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0938', 'Abscisic acid signaling pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0939', 'Gibberellin signaling pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0940', 'Short QT syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0941', 'Suppressor of RNA silencing', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0942', 'Mucolipidosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0943', 'RNA-mediated gene silencing', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0944', 'Nitration', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0945', 'Host-virus interaction', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0946', 'Virion', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0947', 'Limb-girdle muscular dystrophy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0948', 'Aicardi-Goutieres syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0949', 'S-adenosyl-L-methionine', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0950', 'Spinocerebellar ataxia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0951', 'Familial hemophagocytic lymphohistiocytosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0952', 'Extinct organism protein', 'KW-9990');
INSERT INTO targetedms.keywords VALUES (  'KW-0953', 'Lacrimo-auriculo-dento-digital syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0954', 'Congenital adrenal hyperplasia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0955', 'Glaucoma', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0956', 'Kallmann syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0957', 'Chromoplast', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0958', 'Peroxisome biogenesis disorder', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0959', 'Myotoxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-0960', 'Knottin', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-0961', 'Cell wall biogenesis/degradation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0962', 'Peroxisome biogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0963', 'Cytoplasm', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0964', 'Secreted', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0965', 'Cell junction', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0966', 'Cell projection', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0967', 'Endosome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0968', 'Cytoplasmic vesicle', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0969', 'Cilium', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0970', 'Cilium biogenesis/degradation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0971', 'Glycation', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-0972', 'Capsule biogenesis/degradation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0973', 'c-di-GMP', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-0974', 'Archaeal flagellum', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0975', 'Bacterial flagellum', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0976', 'Atrial septal defect', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0977', 'Ichthyosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0978', 'Insecticide resistance', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0979', 'Joubert syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0980', 'Senior-Loken syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0981', 'Meckel syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0982', 'Primary hypomagnesemia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0983', 'Nephronophthisis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0984', 'Congenital hypothyroidism', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0985', 'Congenital erythrocytosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0986', 'Amelogenesis imperfecta', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0987', 'Osteopetrosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0988', 'Intrahepatic cholestasis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0989', 'Craniosynostosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0990', 'Primary ciliary dyskinesia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0991', 'Mental retardation', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0992', 'Brugada syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0993', 'Aortic aneurysm', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-0994', 'Organellar chromatophore', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0995', 'Kinetochore', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0996', 'Nickel insertion', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-0997', 'Cell inner membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0998', 'Cell outer membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-0999', 'Mitochondrion inner membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1000', 'Mitochondrion outer membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1001', 'Plastid inner membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1002', 'Plastid outer membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1003', 'Cell membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1004', 'Congenital myasthenic syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1005', 'Bacterial flagellum biogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1006', 'Bacterial flagellum protein export', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1007', 'Palmoplantar keratoderma', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1008', 'Amyloidosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1009', 'Hearing', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1010', 'Non-syndromic deafness', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1011', 'Dyskeratosis congenita', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1012', 'Kartagener syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1013', 'Microphthalmia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1014', 'Congenital stationary night blindness', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1015', 'Disulfide bond', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-1016', 'Hypogonadotropic hypogonadism', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1017', 'Isopeptide bond', 'KW-9991');
INSERT INTO targetedms.keywords VALUES (  'KW-1018', 'Complement activation lectin pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1020', 'Atrial fibrillation', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1021', 'Pontocerebellar hypoplasia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1022', 'Congenital generalized lipodystrophy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1023', 'Dystonia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1024', 'Diamond-Blackfan anemia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1025', 'Mitosome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1026', 'Leukodystrophy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1027', 'Lead', 'KW-9993');
INSERT INTO targetedms.keywords VALUES (  'KW-1028', 'Ionotropic glutamate receptor inhibitor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-1029', 'Fimbrium biogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1030', 'Host cell inner membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1031', 'Host cell junction', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1032', 'Host cell membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1033', 'Host cell outer membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1034', 'Host cell projection', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1035', 'Host cytoplasm', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1036', 'Host cytoplasmic vesicle', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1037', 'Host cytoskeleton', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1038', 'Host endoplasmic reticulum', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1039', 'Host endosome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1040', 'Host Golgi apparatus', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1041', 'Host lipid droplet', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1042', 'Host lysosome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1043', 'Host membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1044', 'Host microsome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1045', 'Host mitochondrion', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1046', 'Host mitochondrion inner membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1047', 'Host mitochondrion outer membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1048', 'Host nucleus', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1049', 'Host periplasm', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1050', 'Host thylakoid', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1051', 'Host synapse', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1052', 'Target cell membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1053', 'Target membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1054', 'Niemann-Pick disease', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1055', 'Congenital dyserythropoietic anemia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1056', 'Heterotaxy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1057', 'Nemaline myopathy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1058', 'Asthma', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1059', 'Peters anomaly', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1060', 'Myofibrillar myopathy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1061', 'Dermonecrotic toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-1062', 'Cushing syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1063', 'Hypotrichosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1064', 'Adaptive immunity', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1065', 'Osteogenesis imperfecta', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1066', 'Premature ovarian failure', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1067', 'Emery-Dreifuss muscular dystrophy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1068', 'Hemolytic uremic syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1069', 'Brassinosteroid biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1070', 'Brassinosteroid signaling pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1071', 'Ligand-gated ion channel', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-1072', 'Activation of host autophagy by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1073', 'Activation of host caspases by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1074', 'Activation of host NF-kappa-B by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1075', 'Inhibition of eukaryotic host translation factors by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1077', 'G0/G1 host cell cycle checkpoint dysregulation by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1078', 'G1/S host cell cycle checkpoint dysregulation by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1079', 'Host G2/M cell cycle arrest by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1080', 'Inhibition of host adaptive immune response by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1081', 'Inhibition of host apoptosis by viral BCL2-like protein', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1082', 'Inhibition of host apoptosis by viral FLIP-like protein', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1083', 'Inhibition of host autophagy by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1084', 'Inhibition of host tetherin by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1085', 'Inhibition of host caspases by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1086', 'Inhibition of host chemokines by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1087', 'Inhibition of host complement factors by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1088', 'Inhibition of host RIG-I by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1089', 'Inhibition of host MDA5 by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1090', 'Inhibition of host innate immune response by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1091', 'Inhibition of host interferon receptors by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1092', 'Inhibition of host IRF3 by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1093', 'Inhibition of host IRF7 by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1094', 'Inhibition of host IRF9 by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1095', 'Inhibition of host ISG15 by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1096', 'Inhibition of host JAK1 by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1097', 'Inhibition of host MAVS by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1098', 'Inhibition of host mitotic exit by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1099', 'Inhibition of host mRNA nuclear export by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1100', 'Inhibition of host NF-kappa-B by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1101', 'Inhibition of host poly(A)-binding protein by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1102', 'Inhibition of host PKR by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1103', 'Inhibition of host pre-mRNA processing by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1104', 'Inhibition of host RNA polymerase II by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1105', 'Inhibition of host STAT1 by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1106', 'Inhibition of host STAT2 by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1107', 'Inhibition of host TAP by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1108', 'Inhibition of host tapasin by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1110', 'Inhibition of host TRAFs by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1111', 'Inhibition of eukaryotic host transcription initiation by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1112', 'Inhibition of host TYK2 by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1113', 'Inhibition of host RLR pathway by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1114', 'Inhibition of host interferon signaling pathway by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1115', 'Inhibition of host MHC class I molecule presentation by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1116', 'Inhibition of host MHC class II molecule presentation by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1117', 'Inhibition of host proteasome antigen processing by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1118', 'Modulation of host dendritic cell activity by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1119', 'Modulation of host cell apoptosis by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1120', 'Modulation of host cell cycle by viral cyclin-like protein', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1121', 'Modulation of host cell cycle by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1122', 'Modulation of host chromatin by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1123', 'Modulation of host E3 ubiquitin ligases by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1124', 'Modulation of host immunity by viral IgG Fc receptor-like protein', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1125', 'Evasion of host immunity by viral interleukin-like protein', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1126', 'Modulation of host PP1 activity by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1127', 'Modulation of host ubiquitin pathway by viral deubiquitinase', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1128', 'Modulation of host ubiquitin pathway by viral E3 ligase', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1129', 'Modulation of host ubiquitin pathway by viral ubl', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1130', 'Modulation of host ubiquitin pathway by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1131', 'Modulation of host NK-cell activity by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1132', 'Decay of host mRNAs by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1133', 'Transmembrane helix', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-1134', 'Transmembrane beta strand', 'KW-9994');
INSERT INTO targetedms.keywords VALUES (  'KW-1135', 'Mitochondrion nucleoid', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1136', 'Bradyzoite', 'KW-9996');
INSERT INTO targetedms.keywords VALUES (  'KW-1137', 'Tachyzoite', 'KW-9996');
INSERT INTO targetedms.keywords VALUES (  'KW-1138', 'Trophozoite', 'KW-9996');
INSERT INTO targetedms.keywords VALUES (  'KW-1139', 'Helical capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1140', 'T=1 icosahedral capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1141', 'T=2 icosahedral capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1142', 'T=3 icosahedral capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1143', 'T=pseudo3 icosahedral capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1144', 'T=4 icosahedral capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1145', 'T=7 icosahedral capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1146', 'T=13 icosahedral capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1147', 'T=16 icosahedral capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1148', 'T=25 icosahedral capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1149', 'T=147 icosahedral capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1150', 'T=169 icosahedral capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1151', 'T=219 icosahedral capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1152', 'Outer capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1153', 'Inner capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1154', 'Intermediate capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1155', 'Translational shunt', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-1156', 'RNA translational shunting', 'KW-9997');
INSERT INTO targetedms.keywords VALUES (  'KW-1157', 'Cap snatching', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1158', 'RNA termination-reinitiation', 'KW-9997');
INSERT INTO targetedms.keywords VALUES (  'KW-1159', 'RNA suppression of termination', 'KW-9997');
INSERT INTO targetedms.keywords VALUES (  'KW-1160', 'Virus entry into host cell', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1161', 'Viral attachment to host cell', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1162', 'Viral penetration into host cytoplasm', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1163', 'Viral penetration into host nucleus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1164', 'Virus endocytosis by host', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1165', 'Clathrin-mediated endocytosis of virus by host', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1166', 'Caveolin-mediated endocytosis of virus by host', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1167', 'Clathrin- and caveolin-independent endocytosis of virus by host', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1168', 'Fusion of virus membrane with host membrane', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1169', 'Fusion of virus membrane with host cell membrane', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1170', 'Fusion of virus membrane with host endosomal membrane', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1171', 'Viral genome ejection through host cell envelope', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1172', 'Pore-mediated penetration of viral genome into host cell', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1173', 'Viral penetration via permeabilization of host membrane', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1174', 'Viral penetration via lysis of host organellar membrane', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1175', 'Viral attachment to host cell pilus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1176', 'Cytoplasmic inwards viral transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1177', 'Microtubular inwards viral transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1178', 'Actin-dependent inwards viral transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1179', 'Viral genome integration', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1180', 'Syncytium formation induced by viral infection', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1181', 'Viral primary envelope fusion with host outer nuclear membrane', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1182', 'Viral ion channel', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-1183', 'Host cell receptor for virus entry', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-1184', 'Jasmonic acid signaling pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1185', 'Reference proteome', 'KW-9990');
INSERT INTO targetedms.keywords VALUES (  'KW-1186', 'Ciliopathy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1187', 'Viral budding via the host ESCRT complexes', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1188', 'Virus exit from host cell', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1189', 'Microtubular outwards viral transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1190', 'Host gene expression shutoff by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1191', 'Eukaryotic host transcription shutoff by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1192', 'Host mRNA suppression by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1193', 'Eukaryotic host translation shutoff by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1194', 'Viral DNA replication', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1195', 'Viral transcription', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1196', 'IFIT mRNA restriction evasion by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1197', 'Ribosomal skipping', 'KW-9997');
INSERT INTO targetedms.keywords VALUES (  'KW-1198', 'Viral budding', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1199', 'Hemostasis impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-1200', 'Hemorrhagic toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-1201', 'Platelet aggregation inhibiting toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-1202', 'Platelet aggregation activating toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-1203', 'Blood coagulation cascade inhibiting toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-1204', 'Blood coagulation cascade activating toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-1205', 'Fibrinolytic toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-1206', 'Fibrinogenolytic toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-1207', 'Sterol metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1208', 'Phospholipid metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1209', 'Archaeal flagellum biogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1210', 'Necrosis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1211', 'Schizophrenia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1212', 'Corneal dystrophy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1213', 'G-protein coupled receptor impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-1214', 'G-protein coupled acetylcholine receptor impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-1215', 'Dystroglycanopathy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1216', 'Complement system impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-1217', 'Cell adhesion impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-1218', 'Voltage-gated calcium channel impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-1219', 'Ryanodine-sensitive calcium-release channel impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-1220', 'Voltage-gated potassium channel impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-1221', 'Calcium-activated potassium channel impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-1222', 'Bradykinin receptor impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-1223', 'Inhibition of host TBK1 by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1224', 'Inhibition of host IKBKE by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1225', 'Inhibition of host TLR pathway by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1226', 'Viral baseplate protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1227', 'Viral tail protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1228', 'Viral tail tube protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1229', 'Viral tail sheath protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1230', 'Viral tail fiber protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1231', 'Capsid inner membrane protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1232', 'Capsid decoration protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1233', 'Viral attachment to host adhesion receptor', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1234', 'Viral attachment to host entry receptor', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1235', 'Degradation of host cell envelope components during virus entry', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1236', 'Degradation of host peptidoglycans during virus entry', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1237', 'Degradation of host lipopolysaccharides during virus entry', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1238', 'Degradation of host capsule during virus entry', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1239', 'Fusion of virus membrane with host outer membrane', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1240', 'Viral attachment to host cell flagellum', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1241', 'Viral penetration into host cytoplasm via pilus retraction', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1242', 'Viral contractile tail ejection system', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-1243', 'Viral long flexible tail ejection system', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-1244', 'Viral short tail ejection system', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-1245', 'Viral tail assembly', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1246', 'Viral tail fiber assembly', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1247', 'Degradation of host chromosome by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1248', 'Inhibition of host DNA replication by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1249', 'Viral extrusion', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1250', 'Viral genome excision', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1251', 'Viral latency', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1252', 'Latency-replication switch', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1253', 'Viral genome circularization', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1254', 'Modulation of host virulence by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1255', 'Viral exotoxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-1256', 'DNA end degradation evasion by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1257', 'CRISPR-cas system evasion by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1258', 'Restriction-modification system evasion by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1259', 'Evasion of bacteria-mediated translation shutoff by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1260', 'Superinfection exclusion', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1261', 'Bacterial host gene expression shutoff by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1262', 'Eukaryotic host gene expression shutoff by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1263', 'Bacterial host transcription shutoff by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1264', 'Viral receptor tropism switching', 'KW-9999');
INSERT INTO targetedms.keywords VALUES (  'KW-1265', 'Chloride channel impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES (  'KW-1266', 'Target cell cytoplasm', 'KW-9998');
INSERT INTO targetedms.keywords VALUES (  'KW-1267', 'Proteomics identification', 'KW-9990');
INSERT INTO targetedms.keywords VALUES (  'KW-1268', 'Autism spectrum disorder', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1269', 'Autism', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1270', 'Asperger syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES (  'KW-1271', 'Inflammasome', 'KW-9998');
GO

-- Dropping all indexes on RunId
DROP INDEX targetedms.AnnotationSettings.IX_AnnotationSettings_RunId;
DROP INDEX targetedms.CalibrationCurve.IX_CalibrationCurve_RunId;
DROP INDEX targetedms.DriftTimePredictionSettings.IX_DriftTimePredictionSettings_RunId;
DROP INDEX targetedms.IsolationScheme.IX_IsolationScheme_RunId;
DROP INDEX targetedms.FoldChange.IX_FoldChange_RunId;
DROP INDEX targetedms.Instrument.IX_Instrument_RunId;
DROP INDEX targetedms.IsotopeEnrichment.IX_IsotopeEnrichment_RunId;
DROP INDEX targetedms.IsotopeLabel.IX_IsotopeLabel_RunId;
DROP INDEX targetedms.PeptideGroup.IX_PeptideGroup_RunId;
DROP INDEX targetedms.Replicate.IX_Replicate_RunId;
DROP INDEX targetedms.RunEnzyme.IX_RunEnzyme_RunId;
DROP INDEX targetedms.RunIsotopeModification.IX_RunIsotopeModification_RunId;
DROP INDEX targetedms.RunStructuralModification.IX_RunStructuralModification_RunId;
DROP INDEX targetedms.SpectrumLibrary.IX_SpectrumLibrary_RunId;
DROP INDEX targetedms.GroupComparisonSettings.IX_GroupComparisonSettings_RunId;
DROP INDEX targetedms.AuditLogEntry.uix_auditLogEntry_version;
DROP INDEX targetedms.QuantificationSettings.IX_QuantificationSettings_RunId;
GO

---- Dropping all constraints to Runs table
ALTER TABLE targetedms.AnnotationSettings DROP CONSTRAINT FK_AnnotationSettings_Runs;
ALTER TABLE targetedms.CalibrationCurve DROP CONSTRAINT FK_CalibrationCurve_Runs;
ALTER TABLE targetedms.DriftTimePredictionSettings DROP CONSTRAINT FK_DriftTimePredictionSettings_Runs;
ALTER TABLE targetedms.GroupComparisonSettings DROP CONSTRAINT FK_GroupComparisonSettings_Runs;
ALTER TABLE targetedms.FoldChange DROP CONSTRAINT FK_FoldChange_Runs;
ALTER TABLE targetedms.IsolationScheme DROP CONSTRAINT FK_IsolationScheme_Runs;
ALTER TABLE targetedms.Instrument DROP CONSTRAINT FK_Instrument_Runs;
ALTER TABLE targetedms.IsotopeEnrichment DROP CONSTRAINT FK_IsotopeEnrichment_Runs;
ALTER TABLE targetedms.IsotopeLabel DROP CONSTRAINT FK_IsotopeLabel_Runs;
ALTER TABLE targetedms.LibrarySettings DROP CONSTRAINT PK_LibrarySettings;
ALTER TABLE targetedms.LibrarySettings DROP CONSTRAINT FK_LibrarySettings_RunId;
ALTER TABLE targetedms.ListDefinition DROP CONSTRAINT FK_List_RunId;
ALTER TABLE targetedms.ModificationSettings DROP CONSTRAINT PK_ModificationSettings;
ALTER TABLE targetedms.ModificationSettings DROP CONSTRAINT FK_ModificationSettings_Runs;
ALTER TABLE targetedms.PeptideGroup DROP CONSTRAINT FK_PeptideGroup_Runs;
ALTER TABLE targetedms.RetentionTimePredictionSettings DROP CONSTRAINT PK_RetentionTimePredictionSettings;
ALTER TABLE targetedms.RetentionTimePredictionSettings DROP CONSTRAINT FK_RetentionTimePredictionSettings_Runs;
ALTER TABLE targetedms.Replicate DROP CONSTRAINT FK_Replicate_Runs;
ALTER TABLE targetedms.RunEnzyme DROP CONSTRAINT PK_RunEnzyme;
ALTER TABLE targetedms.RunEnzyme DROP CONSTRAINT FK_RunEnzyme_Runs;
ALTER TABLE targetedms.RunIsotopeModification DROP CONSTRAINT FK_RunIsotopeModification_Runs;
ALTER TABLE targetedms.RunIsotopeModification DROP CONSTRAINT PK_RunIsotopeModification;
ALTER TABLE targetedms.RunStructuralModification DROP CONSTRAINT PK_RunStructuralModification;
ALTER TABLE targetedms.RunStructuralModification DROP CONSTRAINT FK_RunStructuralModification_Runs;
ALTER TABLE targetedms.SpectrumLibrary DROP CONSTRAINT FK_SpectrumLibrary_RunId;
ALTER TABLE targetedms.TransitionFullScanSettings DROP CONSTRAINT PK_TransitionFullScanSettings;
ALTER TABLE targetedms.TransitionFullScanSettings DROP CONSTRAINT FK_TransitionFullScanSettings_Runs;
ALTER TABLE targetedms.TransitionInstrumentSettings DROP CONSTRAINT PK_TransitionInstrumentSettings;
ALTER TABLE targetedms.TransitionInstrumentSettings DROP CONSTRAINT FK_TransitionInstrumentSettings_Runs;
ALTER TABLE targetedms.TransitionPredictionSettings DROP CONSTRAINT PK_TransitionPredictionSettings;
ALTER TABLE targetedms.TransitionPredictionSettings DROP CONSTRAINT FK_TransitionPredictionSettings_Runs;
ALTER TABLE targetedms.AuditLogEntry DROP CONSTRAINT fk_auditLogEntry_runs;
ALTER TABLE targetedms.QuantificationSettings DROP CONSTRAINT FK_QuantificationSettings_Runs;
GO

-- ExperimentRunLSID references exp.experimentrun.lsid
EXEC core.fn_dropifexists 'Runs','targetedms','INDEX','IX_Runs_ExperimentRunLSID';
GO

--------------------- Runs -----------------------------
ALTER TABLE targetedms.Runs DROP CONSTRAINT PK_Runs;
GO
ALTER TABLE targetedms.Runs ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.Runs ADD CONSTRAINT PK_Runs PRIMARY KEY (Id);
GO
---------------------------------------------------------

--- Changing
ALTER TABLE targetedms.AnnotationSettings ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.CalibrationCurve ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.DriftTimePredictionSettings ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.IsolationScheme ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.FoldChange ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.GroupComparisonSettings ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.Instrument ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.IsotopeEnrichment ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.IsotopeLabel ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.LibrarySettings ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.ListDefinition ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.ModificationSettings ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.PeptideGroup ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.Replicate ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.RetentionTimePredictionSettings ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.RunEnzyme ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.RunIsotopeModification ALTER COLUMN RunId bigint NOT NULL ;
ALTER TABLE targetedms.RunStructuralModification ALTER COLUMN RunId bigint NOT NULL ;
ALTER TABLE targetedms.SpectrumLibrary ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.TransitionFullScanSettings ALTER COLUMN RunId bigint NOT NULL ;
ALTER TABLE targetedms.TransitionInstrumentSettings ALTER COLUMN RunId bigint NOT NULL ;
ALTER TABLE targetedms.TransitionPredictionSettings ALTER COLUMN RunId bigint NOT NULL ;
ALTER TABLE targetedms.AuditLogEntry ALTER COLUMN versionId bigint;
ALTER TABLE targetedms.QuantificationSettings ALTER COLUMN RunId bigint NOT NULL;
GO

--- Adding back FK Constraints on Runs
ALTER TABLE targetedms.AnnotationSettings ADD CONSTRAINT FK_AnnotationSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.CalibrationCurve ADD CONSTRAINT FK_CalibrationCurve_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.DriftTimePredictionSettings ADD CONSTRAINT FK_DriftTimePredictionSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.FoldChange ADD CONSTRAINT FK_FoldChange_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.GroupComparisonSettings ADD CONSTRAINT FK_GroupComparisonSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
ALTER TABLE targetedms.IsolationScheme ADD CONSTRAINT FK_IsolationScheme_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.Instrument ADD CONSTRAINT FK_Instrument_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.IsotopeEnrichment ADD CONSTRAINT FK_IsotopeEnrichment_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
ALTER TABLE targetedms.IsotopeLabel ADD CONSTRAINT FK_IsotopeLabel_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
ALTER TABLE targetedms.LibrarySettings ADD CONSTRAINT PK_LibrarySettings PRIMARY KEY (RunId);
ALTER TABLE targetedms.LibrarySettings ADD CONSTRAINT FK_LibrarySettings_RunId FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.ListDefinition ADD CONSTRAINT FK_List_RunId FOREIGN KEY(RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.ModificationSettings ADD CONSTRAINT FK_ModificationSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.ModificationSettings ADD CONSTRAINT PK_ModificationSettings PRIMARY KEY (RunId);
ALTER TABLE targetedms.PeptideGroup ADD CONSTRAINT FK_PeptideGroup_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.RetentionTimePredictionSettings ADD CONSTRAINT FK_RetentionTimePredictionSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.RetentionTimePredictionSettings ADD CONSTRAINT PK_RetentionTimePredictionSettings PRIMARY KEY (RunId)
ALTER TABLE targetedms.Replicate ADD CONSTRAINT FK_Replicate_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.RunEnzyme ADD CONSTRAINT FK_RunEnzyme_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.RunEnzyme ADD CONSTRAINT PK_RunEnzyme PRIMARY KEY (EnzymeId, RunId);
ALTER TABLE targetedms.RunIsotopeModification ADD CONSTRAINT FK_RunIsotopeModification_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.RunIsotopeModification ADD CONSTRAINT PK_RunIsotopeModification PRIMARY KEY (IsotopeModId, RunId, IsotopeLabelId);
ALTER TABLE targetedms.RunStructuralModification ADD CONSTRAINT PK_RunStructuralModification PRIMARY KEY (StructuralModId, RunId);
ALTER TABLE targetedms.RunStructuralModification ADD CONSTRAINT FK_RunStructuralModification_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.SpectrumLibrary ADD CONSTRAINT FK_SpectrumLibrary_RunId FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.TransitionFullScanSettings ADD CONSTRAINT FK_TransitionFullScanSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.TransitionFullScanSettings ADD CONSTRAINT PK_TransitionFullScanSettings PRIMARY KEY (RunId);
ALTER TABLE targetedms.TransitionInstrumentSettings ADD CONSTRAINT FK_TransitionInstrumentSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.TransitionInstrumentSettings ADD CONSTRAINT PK_TransitionInstrumentSettings PRIMARY KEY (RunId);
ALTER TABLE targetedms.TransitionPredictionSettings ADD CONSTRAINT FK_TransitionPredictionSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.TransitionPredictionSettings ADD CONSTRAINT PK_TransitionPredictionSettings PRIMARY KEY (RunId);
ALTER TABLE targetedms.AuditLogEntry ADD CONSTRAINT fk_auditLogEntry_runs FOREIGN KEY (versionId) REFERENCES targetedms.runs(id);
ALTER TABLE targetedms.QuantificationSettings ADD CONSTRAINT FK_QuantificationSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
GO

-- Creating Indexes on Runs.RunId in tables
CREATE INDEX IX_AnnotationSettings_RunId ON targetedms.AnnotationSettings (RunId);
CREATE INDEX IX_CalibrationCurve_RunId ON targetedms.CalibrationCurve(RunId);
CREATE INDEX IX_DriftTimePredictionSettings_RunId ON targetedms.DriftTimePredictionSettings(RunId);
CREATE INDEX IX_IsolationScheme_RunId ON targetedms.IsolationScheme (RunId);
CREATE INDEX IX_FoldChange_RunId ON targetedms.FoldChange(RunId);
CREATE INDEX IX_Instrument_RunId ON targetedms.Instrument (RunId);
CREATE INDEX IX_IsotopeEnrichment_RunId ON targetedms.IsotopeEnrichment (RunId);
CREATE INDEX IX_IsotopeLabel_RunId ON targetedms.IsotopeLabel (RunId);
CREATE INDEX IX_PeptideGroup_RunId ON targetedms.PeptideGroup(RunId);
CREATE INDEX IX_Replicate_RunId ON targetedms.Replicate(RunId);
CREATE INDEX IX_RunEnzyme_RunId ON targetedms.RunEnzyme(RunId);
CREATE INDEX IX_RunIsotopeModification_RunId ON targetedms.RunIsotopeModification (RunId);
CREATE INDEX IX_RunStructuralModification_RunId ON targetedms.RunStructuralModification (RunId);
CREATE INDEX IX_SpectrumLibrary_RunId ON targetedms.SpectrumLibrary (RunId);
CREATE INDEX IX_GroupComparisonSettings_RunId ON targetedms.GroupComparisonSettings(RunId);
CREATE INDEX uix_auditLogEntry_version on targetedms.AuditLogEntry(versionId);
CREATE INDEX IX_QuantificationSettings_RunId ON targetedms.QuantificationSettings(RunId);
CREATE INDEX IX_Runs_ExperimentRunLSID ON targetedms.Runs(ExperimentRunLSID);
GO

---------------- PeptideGroupId --------------------
----------------------------------------------------
DROP INDEX targetedms.Protein.IX_Protein_PeptideGroupId;
DROP INDEX targetedms.PeptideGroupAnnotation.IX_PeptideGroupAnnotation_PeptideGroupId;
DROP INDEX targetedms.FoldChange.IX_FoldChange_PeptideGroupId;
DROP INDEX targetedms.GeneralMolecule.IX_Peptide_PeptideGroupId;
GO

----- Drop constraints to PeptideGroup PK
ALTER TABLE targetedms.FoldChange DROP CONSTRAINT FK_FoldChange_PeptideGroup;
ALTER TABLE targetedms.PeptideGroupAnnotation DROP CONSTRAINT FK_PeptideGroupAnnotation_PeptideGroup;
ALTER TABLE targetedms.PeptideGroupAnnotation DROP CONSTRAINT UQ_PeptideGroupAnnotation_Name_PeptideGroup;
ALTER TABLE targetedms.GeneralMolecule DROP CONSTRAINT FK_Peptide_PeptideGroup;
ALTER TABLE targetedms.Protein DROP CONSTRAINT FK_Protein_PeptideGroup;
ALTER TABLE targetedms.PeptideGroupAnnotation DROP CONSTRAINT PK_PeptideGroupAnnotation;

---- Alter PeptideGroup PK
ALTER TABLE targetedms.PeptideGroup DROP CONSTRAINT PK_PeptideGroup;
GO
ALTER TABLE targetedms.PeptideGroup ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.PeptideGroup ADD CONSTRAINT PK_PeptideGroup PRIMARY KEY (Id);
GO

-- Change Columns
ALTER TABLE targetedms.FoldChange ALTER COLUMN PeptideGroupId bigint;
ALTER TABLE targetedms.PeptideGroupAnnotation ALTER COLUMN PeptideGroupId bigint NOT NULL;
ALTER TABLE targetedms.PeptideGroupAnnotation ALTER COLUMN Id bigint NOT NULL;
ALTER TABLE targetedms.GeneralMolecule ALTER COLUMN PeptideGroupId bigint NOT NULL;
ALTER TABLE targetedms.Protein ALTER COLUMN PeptideGroupId bigint NOT NULL;

-- Add back DK constraints PeptideGroup PK
ALTER TABLE targetedms.FoldChange ADD CONSTRAINT FK_FoldChange_PeptideGroup FOREIGN KEY (PeptideGroupId) REFERENCES targetedms.PeptideGroup(Id);
ALTER TABLE targetedms.GeneralMolecule ADD CONSTRAINT FK_Peptide_PeptideGroup FOREIGN KEY (PeptideGroupId) REFERENCES targetedms.PeptideGroup(Id);
ALTER TABLE targetedms.PeptideGroupAnnotation ADD CONSTRAINT UQ_PeptideGroupAnnotation_Name_PeptideGroup UNIQUE (Name, PeptideGroupId);
ALTER TABLE targetedms.PeptideGroupAnnotation ADD CONSTRAINT PK_PeptideGroupAnnotation PRIMARY KEY (Id);
ALTER TABLE targetedms.Protein ADD CONSTRAINT FK_Protein_PeptideGroup FOREIGN KEY (PeptideGroupId) REFERENCES targetedms.PeptideGroup(Id)
ALTER TABLE targetedms.PeptideGroupAnnotation ADD CONSTRAINT FK_PeptideGroupAnnotation_PeptideGroup FOREIGN KEY (PeptideGroupId) REFERENCES targetedms.PeptideGroup(Id);
GO

CREATE INDEX IX_Protein_PeptideGroupId ON targetedms.Protein(PeptideGroupId);
CREATE INDEX IX_PeptideGroupAnnotation_PeptideGroupId ON targetedms.PeptideGroupAnnotation(PeptideGroupId);
CREATE INDEX IX_FoldChange_PeptideGroupId ON targetedms.FoldChange(PeptideGroupId);
CREATE INDEX IX_Peptide_PeptideGroupId ON targetedms.GeneralMolecule(PeptideGroupId);
GO

------------------ GeneralMoleculeID ---------------
-----------------------------------------------------

-- Drop Indexes to GMId
DROP INDEX targetedms.CalibrationCurve.IX_CalibrationCurve_GeneralMoleculeId;
DROP INDEX targetedms.FoldChange.IX_FoldChange_GeneralMoleculeId;
DROP INDEX targetedms.GeneralMoleculeAnnotation.IX_GeneralMoleculeAnnotation_GeneralMoleculeId;
DROP INDEX targetedms.GeneralMoleculeChromInfo.IX_GeneralMoleculeChromInfo_GMId;
DROP INDEX targetedms.GeneralPrecursor.IX_Precursor_GMId;
DROP INDEX targetedms.GeneralPrecursor.IX_Precursor_PeptideId; -- Duplicate Index with IX_Precursor_GMId
DROP INDEX targetedms.PeptideIsotopeModification.IX_PeptideIsotopeModification_PeptideId;
DROP INDEX targetedms.PeptideStructuralModification.IX_PeptideStructuralModification_PeptideId;
GO

-- Drop Constraints in other tables to GMId
ALTER TABLE targetedms.CalibrationCurve DROP CONSTRAINT FK_CalibrationCurve_GeneralMolecule;
ALTER TABLE targetedms.FoldChange DROP CONSTRAINT FK_FoldChange_GeneralMolecule;
ALTER TABLE targetedms.GeneralMoleculeAnnotation DROP CONSTRAINT FK_GMAnnotation_GMId;
ALTER TABLE targetedms.GeneralMoleculeChromInfo DROP CONSTRAINT FK_ChromInfo_GMId;
ALTER TABLE targetedms.GeneralPrecursor DROP CONSTRAINT FK_GeneralPrecursor_GMId;
ALTER TABLE targetedms.Molecule DROP CONSTRAINT FK_Molecule_Id;
ALTER TABLE targetedms.Molecule DROP CONSTRAINT PK_Molecule;
ALTER TABLE targetedms.Peptide DROP CONSTRAINT PK_PeptideId;
ALTER TABLE targetedms.Peptide DROP CONSTRAINT FK_Id_GMId;
ALTER TABLE targetedms.PeptideIsotopeModification DROP CONSTRAINT FK_PeptideIsotopeModification_PeptideId_GMId;
ALTER TABLE targetedms.PeptideStructuralModification DROP CONSTRAINT FK_PeptideStructuralModification_PeptideId_GMId;
ALTER TABLE targetedms.GeneralMoleculeAnnotation DROP CONSTRAINT UQ_GMAnnotation_Name_GMId;
GO

-- Alter GM Id
ALTER TABLE targetedms.GeneralMolecule DROP CONSTRAINT PK_GMId;
GO
ALTER TABLE targetedms.GeneralMolecule ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.GeneralMolecule ADD CONSTRAINT PK_GMId PRIMARY KEY (Id);
GO

-- change columns
ALTER TABLE targetedms.CalibrationCurve ALTER COLUMN GeneralMoleculeId bigint;
ALTER TABLE targetedms.FoldChange ALTER COLUMN GeneralMoleculeId bigint;
ALTER TABLE targetedms.GeneralMoleculeAnnotation ALTER COLUMN GeneralMoleculeId bigint NOT NULL;
ALTER TABLE targetedms.GeneralMoleculeChromInfo ALTER COLUMN GeneralMoleculeId BIGINT NOT NULL;
ALTER TABLE targetedms.GeneralPrecursor ALTER COLUMN GeneralMoleculeId bigint NOT NULL;
ALTER TABLE targetedms.Molecule ALTER COLUMN Id bigint NOT NULL;
ALTER TABLE targetedms.Peptide ALTER COLUMN Id bigint NOT NULL;
ALTER TABLE targetedms.PeptideIsotopeModification ALTER COLUMN PeptideId bigint NOT NULL;
ALTER TABLE targetedms.PeptideStructuralModification ALTER COLUMN PeptideId bigint NOT NULL;
GO

-- Add back FK constraints to GMId in tables
ALTER TABLE targetedms.CalibrationCurve ADD CONSTRAINT FK_CalibrationCurve_GeneralMolecule FOREIGN KEY(GeneralMoleculeId) REFERENCES targetedms.GeneralMolecule(Id);
ALTER TABLE targetedms.FoldChange ADD CONSTRAINT FK_FoldChange_GeneralMolecule FOREIGN KEY (GeneralMoleculeId) REFERENCES targetedms.GeneralMolecule(Id);
ALTER TABLE targetedms.GeneralMoleculeAnnotation ADD CONSTRAINT FK_GMAnnotation_GMId FOREIGN KEY (GeneralMoleculeId) REFERENCES targetedms.GeneralMolecule(Id);
ALTER TABLE targetedms.GeneralMoleculeChromInfo ADD CONSTRAINT FK_ChromInfo_GMId FOREIGN KEY (GeneralMoleculeId) REFERENCES targetedms.GeneralMolecule(Id);
ALTER TABLE targetedms.GeneralPrecursor ADD CONSTRAINT FK_GeneralPrecursor_GMId FOREIGN KEY (GeneralMoleculeId) REFERENCES targetedms.GeneralMolecule(Id);
ALTER TABLE targetedms.Molecule ADD CONSTRAINT PK_Molecule PRIMARY KEY (Id);
ALTER TABLE targetedms.Molecule ADD CONSTRAINT FK_Molecule_Id FOREIGN KEY (Id) REFERENCES targetedms.GeneralMolecule(Id);
ALTER TABLE targetedms.Peptide ADD CONSTRAINT PK_PeptideId PRIMARY KEY (Id);
ALTER TABLE targetedms.Peptide ADD CONSTRAINT FK_Id_GMId FOREIGN KEY (Id) REFERENCES targetedms.GeneralMolecule (Id);
ALTER TABLE targetedms.PeptideIsotopeModification ADD CONSTRAINT FK_PeptideIsotopeModification_PeptideId_GMId FOREIGN KEY (PeptideId) REFERENCES targetedms.GeneralMolecule(Id);
ALTER TABLE targetedms.PeptideStructuralModification ADD CONSTRAINT FK_PeptideStructuralModification_PeptideId_GMId FOREIGN KEY (PeptideId) REFERENCES targetedms.GeneralMolecule(Id);
ALTER TABLE targetedms.GeneralMoleculeAnnotation ADD CONSTRAINT UQ_GMAnnotation_Name_GMId UNIQUE (Name, GeneralMoleculeId);
GO

-- Add back Indexes
CREATE INDEX IX_CalibrationCurve_GeneralMoleculeId ON targetedms.CalibrationCurve(GeneralMoleculeId);
CREATE INDEX IX_FoldChange_GeneralMoleculeId ON targetedms.FoldChange(GeneralMoleculeId);
CREATE INDEX IX_GeneralMoleculeAnnotation_GeneralMoleculeId ON targetedms.GeneralMoleculeAnnotation(GeneralMoleculeId);
CREATE INDEX IX_GeneralMoleculeChromInfo_GMId ON targetedms.GeneralMoleculeChromInfo(GeneralMoleculeId);
CREATE INDEX IX_Precursor_GMId ON targetedms.GeneralPrecursor (GeneralMoleculeId);
CREATE INDEX IX_PeptideIsotopeModification_PeptideId ON targetedms.PeptideIsotopeModification (PeptideId);
CREATE INDEX IX_PeptideStructuralModification_PeptideId ON targetedms.PeptideStructuralModification (PeptideId);
GO

------------------ GeneralPrecursorID ---------------
-----------------------------------------------------

-- Drop Indexes
DROP INDEX targetedms.GeneralTransition.IX_Transition_PrecursorId;
DROP INDEX targetedms.PrecursorAnnotation.IX_PrecursorAnnotation_PrecursorId;
DROP INDEX targetedms.PrecursorChromInfo.IX_PrecursorChromInfo_PrecursorId;
GO

-- Drop Constraints
ALTER TABLE targetedms.GeneralTransition DROP CONSTRAINT FK_GeneralTransition_GPId;
ALTER TABLE targetedms.Precursor DROP CONSTRAINT FK_Precursor_Id;
ALTER TABLE targetedms.PrecursorAnnotation DROP CONSTRAINT FK_PrecursorAnnotation_PrecursorId;
ALTER TABLE targetedms.PrecursorAnnotation DROP CONSTRAINT UQ_PrecursorAnnotation_Name_Precursor;
ALTER TABLE targetedms.PrecursorChromInfo DROP CONSTRAINT FK_PrecursorChromInfo_PrecursorId;
ALTER TABLE targetedms.MoleculePrecursor DROP CONSTRAINT FK_Id;
ALTER TABLE targetedms.MoleculePrecursor DROP CONSTRAINT PK_MoleculePrecursorId;
GO

-- GeneralPrecursor -- change Id
ALTER TABLE targetedms.GeneralPrecursor DROP CONSTRAINT PK_Precursor;
GO
ALTER TABLE targetedms.GeneralPrecursor ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.GeneralPrecursor ADD CONSTRAINT PK_Precursor PRIMARY KEY (Id);
GO

-- change Columns
ALTER TABLE targetedms.GeneralTransition ALTER COLUMN GeneralPrecursorId bigint NOT NULL;
ALTER TABLE targetedms.PrecursorAnnotation ALTER COLUMN PrecursorId bigint NOT NULL;
ALTER TABLE targetedms.PrecursorChromInfo ALTER COLUMN PrecursorId bigint NOT NULL;
ALTER TABLE targetedms.MoleculePrecursor ALTER COLUMN Id bigint NOT NULL;
GO

-- Add back constraints
ALTER TABLE targetedms.GeneralTransition ADD CONSTRAINT FK_GeneralTransition_GPId FOREIGN KEY (GeneralPrecursorId) REFERENCES targetedms.GeneralPrecursor(Id);
ALTER TABLE targetedms.PrecursorAnnotation  ADD CONSTRAINT FK_PrecursorAnnotation_PrecursorId FOREIGN KEY (PrecursorId) REFERENCES targetedms.GeneralPrecursor(Id)
ALTER TABLE targetedms.PrecursorAnnotation  ADD CONSTRAINT UQ_PrecursorAnnotation_Name_Precursor UNIQUE (Name, PrecursorId);
ALTER TABLE targetedms.PrecursorChromInfo ADD CONSTRAINT FK_PrecursorChromInfo_PrecursorId FOREIGN KEY (PrecursorId) REFERENCES targetedms.GeneralPrecursor(Id);
ALTER TABLE targetedms.MoleculePrecursor ADD CONSTRAINT FK_Id FOREIGN KEY (Id) REFERENCES targetedms.GeneralPrecursor (Id);
ALTER TABLE targetedms.MoleculePrecursor ADD CONSTRAINT PK_MoleculePrecursorId PRIMARY KEY (Id);
GO

-- Add back Indexes
CREATE INDEX IX_Transition_PrecursorId ON targetedms.GeneralTransition(GeneralPrecursorId);
CREATE INDEX IX_PrecursorAnnotation_PrecursorId ON targetedms.PrecursorAnnotation(PrecursorId);
CREATE INDEX IX_PrecursorChromInfo_PrecursorId ON targetedms.PrecursorChromInfo(PrecursorId);
GO


------------------ PrecursorID ------------------
-------------------------------------------------

-- Drop Indexes
DROP INDEX targetedms.BibliospecLibInfo.IX_BibliospecLibInfo_PrecursorId;
DROP INDEX targetedms.ChromatogramLibInfo.IX_ChromatogramLibInfo_PrecursorId;
DROP INDEX targetedms.HunterLibInfo.IX_HunterLibInfo_PrecursorId;
DROP INDEX targetedms.NistLibInfo.IX_NistLibInfo_PrecursorId;
DROP INDEX targetedms.SpectrastLibInfo.IX_SpectrastLibInfo_PrecursorId;
DROP INDEX targetedms.Precursor.IX_Precursor_Id;
GO

-- Drop Constraints
ALTER TABLE targetedms.BibliospecLibInfo DROP CONSTRAINT FK_BibliospecLibInfo_Precursor;
ALTER TABLE targetedms.ChromatogramLibInfo DROP CONSTRAINT FK_ChromatogramLibInfo_Precursor;
ALTER TABLE targetedms.HunterLibInfo DROP CONSTRAINT FK_HunterLibInfo_Precursor;
ALTER TABLE targetedms.NistLibInfo DROP CONSTRAINT FK_NistLibInfo_Precursor;
ALTER TABLE targetedms.SpectrastLibInfo DROP CONSTRAINT FK_SpectrastLibInfo_Precursor;
GO

-- Change Precursor
ALTER TABLE targetedms.Precursor DROP CONSTRAINT PK_Precursor_Id;
GO
ALTER TABLE targetedms.Precursor ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.Precursor ADD CONSTRAINT PK_Precursor_Id PRIMARY KEY (Id);
GO

-- Change columns
ALTER TABLE targetedms.BibliospecLibInfo ALTER COLUMN PrecursorId bigint NOT NULL;
ALTER TABLE targetedms.ChromatogramLibInfo ALTER COLUMN PrecursorId bigint NOT NULL;
ALTER TABLE targetedms.HunterLibInfo ALTER COLUMN PrecursorId bigint NOT NULL;
ALTER TABLE targetedms.NistLibInfo ALTER COLUMN PrecursorId bigint NOT NULL;
ALTER TABLE targetedms.SpectrastLibInfo ALTER COLUMN PrecursorId bigint NOT NULL;
GO

-- Add back Constraints
ALTER TABLE targetedms.BibliospecLibInfo ADD CONSTRAINT FK_BibliospecLibInfo_Precursor FOREIGN KEY (PrecursorId) REFERENCES targetedms.Precursor(Id);
ALTER TABLE targetedms.ChromatogramLibInfo ADD CONSTRAINT FK_ChromatogramLibInfo_Precursor FOREIGN KEY (PrecursorId) REFERENCES targetedms.Precursor(Id);
ALTER TABLE targetedms.HunterLibInfo ADD CONSTRAINT FK_HunterLibInfo_Precursor FOREIGN KEY (PrecursorId) REFERENCES targetedms.Precursor(Id);
ALTER TABLE targetedms.NistLibInfo ADD CONSTRAINT FK_NistLibInfo_Precursor FOREIGN KEY (PrecursorId) REFERENCES targetedms.Precursor(Id);
ALTER TABLE targetedms.SpectrastLibInfo ADD CONSTRAINT FK_SpectrastLibInfo_Precursor FOREIGN KEY (PrecursorId) REFERENCES targetedms.Precursor(Id);
ALTER TABLE targetedms.Precursor ADD CONSTRAINT FK_Precursor_Id FOREIGN KEY (Id) REFERENCES targetedms.GeneralPrecursor(Id);
GO

-- Add back Indexes
CREATE INDEX IX_BibliospecLibInfo_PrecursorId ON targetedms.BibliospecLibInfo(PrecursorId);
CREATE INDEX IX_ChromatogramLibInfo_PrecursorId ON targetedms.ChromatogramLibInfo(PrecursorId);
CREATE INDEX IX_HunterLibInfo_PrecursorId ON targetedms.HunterLibInfo(PrecursorId);
CREATE INDEX IX_NistLibInfo_PrecursorId ON targetedms.NistLibInfo(PrecursorId);
CREATE INDEX IX_SpectrastLibInfo_PrecursorId ON targetedms.SpectrastLibInfo(PrecursorId);
CREATE INDEX IX_Precursor_Id ON targetedms.Precursor(Id);
GO


------------------ GeneralTransitionID ---------------
-----------------------------------------------------

-- Drop Indexes
DROP INDEX targetedms.TransitionAnnotation.IX_TransitionAnnotation_TransitionId;
DROP INDEX targetedms.TransitionChromInfo.IX_TransitionChromInfo_TransitionId;
DROP INDEX targetedms.TransitionChromInfoAnnotation.IX_TransitionChromInfoAnnotation_TransitionChromInfoId;
DROP INDEX targetedms.TransitionLoss.IX_TransitionLoss_TransitionId;
DROP INDEX targetedms.TransitionOptimization.IX_TransitionOptimization_TransitionId;
GO

-- Drop Constraints
ALTER TABLE targetedms.MoleculeTransition DROP CONSTRAINT PK_MoleculeTransition;
ALTER TABLE targetedms.MoleculeTransition DROP CONSTRAINT FK_MoleculeTransition_GTId;
ALTER TABLE targetedms.TransitionOptimization DROP CONSTRAINT FK_TransitionOptimization_TransitionId;
ALTER TABLE targetedms.Transition DROP CONSTRAINT FK_Transition_Id;
ALTER TABLE targetedms.TransitionAnnotation DROP CONSTRAINT FK_TransitionAnnotation_GTId;
ALTER TABLE targetedms.TransitionAnnotation DROP CONSTRAINT UQ_TransitionAnnotation_Name_Transition;
ALTER TABLE targetedms.TransitionChromInfo DROP CONSTRAINT FK_TransitionChromInfo_GTId;
ALTER TABLE targetedms.TransitionLoss DROP CONSTRAINT FK_TransitionLoss_TransitionId;
ALTER TABLE targetedms.Transition DROP CONSTRAINT PK_Transition_Id;
GO

-- Change GTId
ALTER TABLE targetedms.GeneralTransition DROP CONSTRAINT PK_Transition;
GO
ALTER TABLE targetedms.GeneralTransition ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.GeneralTransition ADD CONSTRAINT PK_Transition PRIMARY KEY (Id);
GO

-- Change Columns
ALTER TABLE targetedms.MoleculeTransition ALTER COLUMN TransitionId bigint NOT NULL;
ALTER TABLE targetedms.Transition ALTER COLUMN Id bigint NOT NULL;
ALTER TABLE targetedms.TransitionAnnotation ALTER COLUMN TransitionId bigint NOT NULL;
ALTER TABLE targetedms.TransitionChromInfo ALTER COLUMN TransitionId bigint NOT NULL;
ALTER TABLE targetedms.TransitionLoss ALTER COLUMN TransitionId bigint NOT NULL;
ALTER TABLE targetedms.TransitionOptimization ALTER COLUMN TransitionId bigint NOT NULL;
GO

-- Add back Constraints
ALTER TABLE targetedms.MoleculeTransition ADD CONSTRAINT PK_MoleculeTransition PRIMARY KEY (TransitionId);
ALTER TABLE targetedms.MoleculeTransition ADD CONSTRAINT FK_MoleculeTransition_GTId FOREIGN KEY (TransitionId) REFERENCES targetedms.GeneralTransition(Id);
ALTER TABLE targetedms.Transition ADD CONSTRAINT PK_Transition_Id PRIMARY KEY (Id);
ALTER TABLE targetedms.Transition ADD CONSTRAINT FK_Transition_Id FOREIGN KEY (Id) REFERENCES targetedms.GeneralTransition(Id);
ALTER TABLE targetedms.TransitionAnnotation ADD CONSTRAINT FK_TransitionAnnotation_GTId FOREIGN KEY (TransitionId) REFERENCES targetedms.GeneralTransition(Id);
ALTER TABLE targetedms.TransitionAnnotation ADD CONSTRAINT UQ_TransitionAnnotation_Name_Transition UNIQUE (Name, TransitionId);
ALTER TABLE targetedms.TransitionChromInfo ADD CONSTRAINT FK_TransitionChromInfo_GTId FOREIGN KEY (TransitionId) REFERENCES targetedms.GeneralTransition(Id);
ALTER TABLE targetedms.TransitionLoss ADD CONSTRAINT FK_TransitionLoss_TransitionId FOREIGN KEY (TransitionId) REFERENCES targetedms.Transition(Id);
ALTER TABLE targetedms.TransitionOptimization ADD CONSTRAINT FK_TransitionOptimization_TransitionId FOREIGN KEY (TransitionId) REFERENCES targetedms.Transition(Id);
GO

-- Add back Indexes
CREATE INDEX IX_TransitionAnnotation_TransitionId ON targetedms.TransitionAnnotation(TransitionId);
CREATE INDEX IX_TransitionChromInfoAnnotation_TransitionChromInfoId ON targetedms.TransitionChromInfoAnnotation(TransitionChromInfoId);
CREATE INDEX IX_TransitionLoss_TransitionId ON targetedms.TransitionLoss (TransitionId);
CREATE INDEX IX_TransitionOptimization_TransitionId ON targetedms.TransitionOptimization (TransitionId);
CREATE INDEX IX_TransitionChromInfo_TransitionId ON targetedms.TransitionChromInfo(TransitionId);
GO

------------------ ReplicateID ------------------
--------------------------------------------------

-- Drop Indexes
DROP INDEX targetedms.QCMetricExclusion.IX_QCMetricExclusion_ReplicateId;
DROP INDEX targetedms.ReplicateAnnotation.IX_ReplicateAnnotation_ReplicateId;
DROP INDEX targetedms.SampleFile.IX_SampleFile_ReplicateId;
GO

-- Drop Constraints
ALTER TABLE targetedms.QCMetricExclusion DROP CONSTRAINT UQ_QCMetricExclusion_Replicate_Metric;
ALTER TABLE targetedms.QCMetricExclusion DROP CONSTRAINT FK_QCMetricExclusion_ReplicateId;
ALTER TABLE targetedms.ReplicateAnnotation DROP CONSTRAINT FK_ReplicateAnnotation_Replicate;
ALTER TABLE targetedms.ReplicateAnnotation DROP CONSTRAINT UQ_ReplicateAnnotation_Name_Repicate;
ALTER TABLE targetedms.SampleFile DROP CONSTRAINT FK_SampleFile_Replicate;
GO

-- Change ReplicateId
ALTER TABLE targetedms.Replicate DROP CONSTRAINT PK_Replicate;
GO
ALTER TABLE targetedms.Replicate ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.Replicate ADD CONSTRAINT PK_Replicate PRIMARY KEY (Id);
GO

-- Change Columns
ALTER TABLE targetedms.ReplicateAnnotation ALTER COLUMN ReplicateId bigint NOT NULL;
ALTER TABLE targetedms.SampleFile ALTER COLUMN ReplicateId bigint NOT NULL;
ALTER TABLE targetedms.QCMetricExclusion ALTER COLUMN ReplicateId bigint NOT NULL;

-- Add back Constraints
ALTER TABLE targetedms.QCMetricExclusion ADD CONSTRAINT FK_QCMetricExclusion_ReplicateId FOREIGN KEY (ReplicateId) REFERENCES targetedms.Replicate (Id);
ALTER TABLE targetedms.QCMetricExclusion  ADD CONSTRAINT UQ_QCMetricExclusion_Replicate_Metric UNIQUE (ReplicateId, MetricId);
ALTER TABLE targetedms.ReplicateAnnotation  ADD CONSTRAINT UQ_ReplicateAnnotation_Name_Repicate UNIQUE (Name, ReplicateId);
ALTER TABLE targetedms.ReplicateAnnotation  ADD CONSTRAINT FK_ReplicateAnnotation_Replicate FOREIGN KEY (ReplicateId) REFERENCES targetedms.Replicate(Id);
ALTER TABLE targetedms.SampleFile ADD CONSTRAINT FK_SampleFile_Replicate FOREIGN KEY (ReplicateId) REFERENCES targetedms.Replicate(Id)

-- Add back Indexes
CREATE INDEX IX_SampleFile_ReplicateId ON targetedms.SampleFile(ReplicateId);
CREATE INDEX IX_QCMetricExclusion_ReplicateId ON targetedms.QCMetricExclusion(ReplicateId);
CREATE INDEX IX_ReplicateAnnotation_ReplicateId ON targetedms.ReplicateAnnotation (ReplicateId);
GO

------------------ SampleFileID ------------------
--------------------------------------------------

-- Drop Indexes
DROP INDEX targetedms.GeneralMoleculeChromInfo.IX_GMChromInfo_SampleFileId;
DROP INDEX targetedms.PrecursorChromInfo.IX_PrecursorChromInfo_SampleFileId;
DROP INDEX targetedms.SampleFileChromInfo.IDX_SampleFileChromInfo_SampleFileId;
DROP INDEX targetedms.TransitionChromInfo.IX_TransitionChromInfo_SampleFileId;
GO

-- Drop Constraints
ALTER TABLE targetedms.TransitionChromInfo DROP CONSTRAINT FK_TransitionChromInfo_SampleFile;
ALTER TABLE targetedms.GeneralMoleculeChromInfo DROP CONSTRAINT FK_GMChromInfo_SampleFile;
ALTER TABLE targetedms.PrecursorChromInfo DROP CONSTRAINT FK_PrecursorChromInfo_SampleFile;
ALTER TABLE targetedms.SampleFileChromInfo DROP CONSTRAINT FK_SampleFileChromInfo_SampleFile;
GO

-- Change SampleFileId
ALTER TABLE targetedms.SampleFile DROP CONSTRAINT PK_SampleFile;
GO
ALTER TABLE targetedms.SampleFile ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.SampleFile ADD CONSTRAINT PK_SampleFile PRIMARY KEY (Id);
GO

-- Change Columns
ALTER TABLE targetedms.TransitionChromInfo ALTER COLUMN SampleFileId bigint NOT NULL;
ALTER TABLE targetedms.GeneralMoleculeChromInfo ALTER COLUMN SampleFileId BIGINT NOT NULL;
ALTER TABLE targetedms.PrecursorChromInfo ALTER COLUMN SampleFileId bigint NOT NULL;
ALTER TABLE targetedms.SampleFileChromInfo ALTER COLUMN SampleFileId bigint NOT NULL;
GO

-- Add back Constraints
ALTER TABLE targetedms.TransitionChromInfo ADD CONSTRAINT FK_TransitionChromInfo_SampleFile FOREIGN KEY (SampleFileId) REFERENCES targetedms.SampleFile(Id);
ALTER TABLE targetedms.GeneralMoleculeChromInfo ADD CONSTRAINT FK_GMChromInfo_SampleFile FOREIGN KEY (SampleFileId) REFERENCES targetedms.SampleFile(Id);
ALTER TABLE targetedms.PrecursorChromInfo ADD CONSTRAINT FK_PrecursorChromInfo_SampleFile FOREIGN KEY (SampleFileId) REFERENCES targetedms.SampleFile(Id)
ALTER TABLE targetedms.SampleFileChromInfo ADD CONSTRAINT FK_SampleFileChromInfo_SampleFile FOREIGN KEY (SampleFileId) REFERENCES targetedms.SampleFile(Id)
GO

-- Add back Indexes
CREATE INDEX IX_GMChromInfo_SampleFileId ON targetedms.GeneralMoleculeChromInfo(samplefileid);
CREATE INDEX IX_PrecursorChromInfo_SampleFileId ON targetedms.PrecursorChromInfo(SampleFileId);
CREATE INDEX IDX_SampleFileChromInfo_SampleFileId ON targetedms.SampleFileChromInfo(samplefileid);
CREATE INDEX IX_TransitionChromInfo_SampleFileId ON targetedms.TransitionChromInfo(SampleFileId);
GO

------------------ SpectrumLibraryID ------------------
-------------------------------------------------------

-- Drop Indexes
DROP INDEX targetedms.BibliospecLibInfo.IX_BibliospecLibInfo_SpectrumLibraryId;
DROP INDEX targetedms.ChromatogramLibInfo.IX_ChromatogramLibInfo_SpectrumLibraryId;
DROP INDEX targetedms.HunterLibInfo.IX_HunterLibInfo_SpectrumLibraryId;
DROP INDEX targetedms.NistLibInfo.IX_NistLibInfo_SpectrumLibraryId;
DROP INDEX targetedms.SpectrastLibInfo.IX_SpectrastLibInfo_SpectrumLibraryId;
GO

-- Drop Constraints
ALTER TABLE targetedms.BibliospecLibInfo DROP CONSTRAINT FK_BibliospecLibInfo_SpectrumLibrary;
ALTER TABLE targetedms.ChromatogramLibInfo DROP CONSTRAINT FK_ChromatogramLibInfo_SpectrumLibrary;
ALTER TABLE targetedms.HunterLibInfo DROP CONSTRAINT FK_HunterLibInfo_SpectrumLibrary;
ALTER TABLE targetedms.NistLibInfo DROP CONSTRAINT FK_NistLibInfo_SpectrumLibrary;
ALTER TABLE targetedms.SpectrastLibInfo DROP CONSTRAINT FK_SpectrastLibInfo_SpectrumLibrary;
GO

-- Change SLId
ALTER TABLE targetedms.SpectrumLibrary DROP CONSTRAINT PK_SpectrumLibrary;
GO
ALTER TABLE targetedms.SpectrumLibrary ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.SpectrumLibrary ADD CONSTRAINT PK_SpectrumLibrary PRIMARY KEY (Id);
GO

-- Change Columns
ALTER TABLE targetedms.BibliospecLibInfo ALTER COLUMN SpectrumLibraryId bigint NOT NULL;
ALTER TABLE targetedms.ChromatogramLibInfo ALTER COLUMN SpectrumLibraryId bigint NOT NULL;
ALTER TABLE targetedms.HunterLibInfo ALTER COLUMN SpectrumLibraryId bigint NOT NULL;
ALTER TABLE targetedms.NistLibInfo ALTER COLUMN SpectrumLibraryId bigint NOT NULL;
ALTER TABLE targetedms.SpectrastLibInfo ALTER COLUMN SpectrumLibraryId bigint NOT NULL;
GO

-- Add back Constraints
ALTER TABLE targetedms.BibliospecLibInfo ADD CONSTRAINT FK_BibliospecLibInfo_SpectrumLibrary FOREIGN KEY (SpectrumLibraryId) REFERENCES targetedms.SpectrumLibrary(Id);
ALTER TABLE targetedms.ChromatogramLibInfo ADD CONSTRAINT FK_ChromatogramLibInfo_SpectrumLibrary FOREIGN KEY (SpectrumLibraryId) REFERENCES targetedms.SpectrumLibrary(Id);
ALTER TABLE targetedms.HunterLibInfo ADD CONSTRAINT FK_HunterLibInfo_SpectrumLibrary FOREIGN KEY (SpectrumLibraryId) REFERENCES targetedms.SpectrumLibrary(Id);
ALTER TABLE targetedms.NistLibInfo ADD CONSTRAINT FK_NistLibInfo_SpectrumLibrary FOREIGN KEY (SpectrumLibraryId) REFERENCES targetedms.SpectrumLibrary(Id);
ALTER TABLE targetedms.SpectrastLibInfo ADD CONSTRAINT FK_SpectrastLibInfo_SpectrumLibrary FOREIGN KEY (SpectrumLibraryId) REFERENCES targetedms.SpectrumLibrary(Id);
GO

-- Add back Indexes
CREATE INDEX IX_BibliospecLibInfo_SpectrumLibraryId ON targetedms.BibliospecLibInfo(SpectrumLibraryId);
CREATE INDEX IX_ChromatogramLibInfo_SpectrumLibraryId ON targetedms.ChromatogramLibInfo(SpectrumLibraryId);
CREATE INDEX IX_HunterLibInfo_SpectrumLibraryId ON targetedms.HunterLibInfo(SpectrumLibraryId);
CREATE INDEX IX_NistLibInfo_SpectrumLibraryId ON targetedms.NistLibInfo(SpectrumLibraryId);
CREATE INDEX IX_SpectrastLibInfo_SpectrumLibraryId ON targetedms.SpectrastLibInfo(SpectrumLibraryId);
GO


------------------ IsotopeModId -----------------------
-------------------------------------------------------

-- Drop Indexes
DROP INDEX targetedms.PeptideIsotopeModification.IX_PeptideIsotopeModification_IsotopeModId;
GO

-- Drop Constraints
ALTER TABLE targetedms.PeptideIsotopeModification DROP CONSTRAINT FK_PeptideIsotopeModification_IsotopeModification;
ALTER TABLE targetedms.RunIsotopeModification DROP CONSTRAINT FK_RunIsotopeModification_IsotopeModification;
ALTER TABLE targetedms.RunIsotopeModification DROP CONSTRAINT PK_RunIsotopeModification;
GO

-- Change IsotopeModId
ALTER TABLE targetedms.IsotopeModification DROP CONSTRAINT PK_IsotopeModification;
GO
ALTER TABLE targetedms.IsotopeModification ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.IsotopeModification ADD CONSTRAINT PK_IsotopeModification PRIMARY KEY (Id);
GO

-- Change Columns
ALTER TABLE targetedms.PeptideIsotopeModification ALTER COLUMN IsotopeModId bigint NOT NULL;
ALTER TABLE targetedms.RunIsotopeModification ALTER COLUMN IsotopeModId bigint NOT NULL;

-- Add back Constraints
ALTER TABLE targetedms.PeptideIsotopeModification ADD CONSTRAINT FK_PeptideIsotopeModification_IsotopeModification FOREIGN KEY (IsotopeModId) REFERENCES targetedms.IsotopeModification(Id);
ALTER TABLE targetedms.RunIsotopeModification ADD CONSTRAINT FK_RunIsotopeModification_IsotopeModification FOREIGN KEY (IsotopeModId) REFERENCES targetedms.IsotopeModification(Id);
ALTER TABLE targetedms.RunIsotopeModification ADD CONSTRAINT PK_RunIsotopeModification PRIMARY KEY (IsotopeModId, RunId, IsotopeLabelId);

-- Add back Indexes
CREATE INDEX IX_PeptideIsotopeModification_IsotopeModId ON targetedms.PeptideIsotopeModification (IsotopeModId);
GO


------------------ PrecursorChromInfoId ---------------------
-------------------------------------------------------
-- Drop Indexes
DROP INDEX targetedms.PrecursorAreaRatio.IX_PrecursorAreaRatio_PrecursorChromInfoId;
DROP INDEX targetedms.PrecursorAreaRatio.IX_PrecursorAreaRatio_PrecursorChromInfoStdId;
DROP INDEX targetedms.PrecursorChromInfoAnnotation.IX_PrecursorChromInfoAnnotation_PrecursorChromInfoId;
DROP INDEX targetedms.TransitionChromInfo.IX_TransitionChromInfo_PrecursorChromInfoId;
DROP INDEX targetedms.PrecursorChromInfo.idx_precursorchrominfo_container;
GO

-- Drop Constraints
ALTER TABLE targetedms.PrecursorAreaRatio DROP CONSTRAINT FK_PrecursorAreaRatio_PrecursorChromInfoId;
ALTER TABLE targetedms.PrecursorAreaRatio DROP CONSTRAINT FK_PrecursorAreaRatio_PrecursorChromInfoStdId;
ALTER TABLE targetedms.PrecursorChromInfoAnnotation DROP CONSTRAINT FK_PrecursorChromInfoAnnotation_PrecursorChromInfo;
ALTER TABLE targetedms.PrecursorChromInfoAnnotation DROP CONSTRAINT UQ_PrecursorChromInfoAnnotation_Name_PrecursorChromInfo
ALTER TABLE targetedms.TransitionChromInfo DROP CONSTRAINT FK_TransitionChromInfo_PrecursorChromInfo;
GO

-- Change PrecursorChromInfoId
ALTER TABLE targetedms.PrecursorChromInfo DROP CONSTRAINT PK_PrecursorChromInfo;
GO
ALTER TABLE targetedms.PrecursorChromInfo ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.PrecursorChromInfo ADD CONSTRAINT PK_PrecursorChromInfo PRIMARY KEY (Id);
GO

-- Change Columns
ALTER TABLE targetedms.PrecursorAreaRatio ALTER COLUMN PrecursorChromInfoId bigint NOT NULL;
ALTER TABLE targetedms.PrecursorAreaRatio ALTER COLUMN PrecursorChromInfoStdId bigint NOT NULL;
ALTER TABLE targetedms.PrecursorChromInfoAnnotation ALTER COLUMN PrecursorChromInfoId bigint NOT NULL;
ALTER TABLE targetedms.TransitionChromInfo ALTER COLUMN PrecursorChromInfoId bigint NOT NULL;
GO

-- Add back Constraints
ALTER TABLE targetedms.PrecursorAreaRatio ADD CONSTRAINT FK_PrecursorAreaRatio_PrecursorChromInfoId FOREIGN KEY (PrecursorChromInfoId) REFERENCES targetedms.PrecursorChromInfo(Id);
ALTER TABLE targetedms.PrecursorAreaRatio ADD CONSTRAINT FK_PrecursorAreaRatio_PrecursorChromInfoStdId FOREIGN KEY (PrecursorChromInfoStdId) REFERENCES targetedms.PrecursorChromInfo(Id);
ALTER TABLE targetedms.PrecursorChromInfoAnnotation ADD CONSTRAINT FK_PrecursorChromInfoAnnotation_PrecursorChromInfo FOREIGN KEY (PrecursorChromInfoId) REFERENCES targetedms.PrecursorChromInfo(Id);
ALTER TABLE targetedms.PrecursorChromInfoAnnotation ADD CONSTRAINT UQ_PrecursorChromInfoAnnotation_Name_PrecursorChromInfo UNIQUE (Name, PrecursorChromInfoId);
ALTER TABLE targetedms.TransitionChromInfo ADD CONSTRAINT FK_TransitionChromInfo_PrecursorChromInfo FOREIGN KEY (PrecursorChromInfoId) REFERENCES targetedms.PrecursorChromInfo(Id);
GO

-- Add back Indexes
CREATE INDEX IX_PrecursorAreaRatio_PrecursorChromInfoId ON targetedms.PrecursorAreaRatio (PrecursorChromInfoId);
CREATE INDEX IX_PrecursorAreaRatio_PrecursorChromInfoStdId ON targetedms.PrecursorAreaRatio (PrecursorChromInfoStdId);
CREATE INDEX IX_PrecursorChromInfoAnnotation_PrecursorChromInfoId ON targetedms.PrecursorChromInfoAnnotation(PrecursorChromInfoId);
CREATE INDEX IX_TransitionChromInfo_PrecursorChromInfoId ON targetedms.TransitionChromInfo(PrecursorChromInfoId);
CREATE INDEX idx_precursorchrominfo_container ON targetedms.precursorchrominfo (container, id);
GO

------------------ IsotopeLabelId ---------------------
-------------------------------------------------------

-- Drop Indexes
DROP INDEX targetedms.FoldChange.IX_FoldChange_IsotopeLabelId;
DROP INDEX targetedms.GeneralPrecursor.IX_GeneralPrecursor_IsotopeLabelId;
DROP INDEX targetedms.PeptideAreaRatio.IX_PeptideAreaRatio_IsotopeLabelId;
DROP INDEX targetedms.PeptideAreaRatio.IX_PeptideAreaRatio_IsotopeLabelStdId;
DROP INDEX targetedms.PrecursorAreaRatio.IX_PrecursorAreaRatio_IsotopeLabelId;
DROP INDEX targetedms.PrecursorAreaRatio.IX_PrecursorAreaRatio_IsotopeLabelStdId;
DROP INDEX targetedms.RunIsotopeModification.IX_RunIsotopeModification_IsotopeLabelId;
DROP INDEX targetedms.TransitionAreaRatio.IX_TransitionAreaRatio_IsotopeLabelId;
DROP INDEX targetedms.TransitionAreaRatio.IX_TransitionAreaRatio_IsotopeLabelStdId;
GO

-- Drop Constraints
ALTER TABLE targetedms.FoldChange DROP CONSTRAINT FK_FoldChange_IsotopeLabel;
ALTER TABLE targetedms.GeneralPrecursor DROP CONSTRAINT FK_GeneralPrecursor_IsotopeLabel;
ALTER TABLE targetedms.PeptideAreaRatio DROP CONSTRAINT FK_PeptideAreaRatio_IsotopeLabelId;
ALTER TABLE targetedms.PeptideAreaRatio DROP CONSTRAINT FK_PeptideAreaRatio_IsotopeLabelStdId;
ALTER TABLE targetedms.PrecursorAreaRatio DROP CONSTRAINT FK_PrecursorAreaRatio_IsotopeLabelId;
ALTER TABLE targetedms.PrecursorAreaRatio DROP CONSTRAINT FK_PrecursorAreaRatio_IsotopeLabelStdId;
ALTER TABLE targetedms.TransitionAreaRatio DROP CONSTRAINT FK_TransitionAreaRatio_IsotopeLabelId;
ALTER TABLE targetedms.RunIsotopeModification DROP CONSTRAINT FK_RunIsotopeModification_IsotopeLabel;
ALTER TABLE targetedms.RunIsotopeModification DROP CONSTRAINT PK_RunIsotopeModification;
ALTER TABLE targetedms.TransitionAreaRatio DROP CONSTRAINT FK_TransitionAreaRatio_IsotopeLabelStdId;

-- Change IsotopeLabelId
ALTER TABLE targetedms.IsotopeLabel DROP CONSTRAINT PK_IsotopeLabel;
GO
ALTER TABLE targetedms.IsotopeLabel ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.IsotopeLabel ADD CONSTRAINT PK_IsotopeLabel PRIMARY KEY (Id);
GO

-- Change Columns
ALTER TABLE targetedms.FoldChange ALTER COLUMN IsotopeLabelId bigint;
ALTER TABLE targetedms.GeneralPrecursor ALTER COLUMN IsotopeLabelId bigint;
ALTER TABLE targetedms.PeptideAreaRatio ALTER COLUMN IsotopeLabelId bigint NOT NULL;
ALTER TABLE targetedms.PeptideAreaRatio ALTER COLUMN IsotopeLabelStdId bigint NOT NULL;
ALTER TABLE targetedms.PrecursorAreaRatio ALTER COLUMN IsotopeLabelId bigint NOT NULL;
ALTER TABLE targetedms.PrecursorAreaRatio ALTER COLUMN IsotopeLabelStdId bigint NOT NULL;
ALTER TABLE targetedms.RunIsotopeModification ALTER COLUMN IsotopeLabelId bigint NOT NULL;
ALTER TABLE targetedms.TransitionAreaRatio ALTER COLUMN IsotopeLabelId bigint NOT NULL;
ALTER TABLE targetedms.TransitionAreaRatio ALTER COLUMN IsotopeLabelStdId bigint NOT NULL;
GO

-- Add back Constraints
ALTER TABLE targetedms.FoldChange ADD CONSTRAINT FK_FoldChange_IsotopeLabel FOREIGN KEY (IsotopeLabelId) REFERENCES targetedms.IsotopeLabel(Id);
ALTER TABLE targetedms.GeneralPrecursor ADD CONSTRAINT FK_GeneralPrecursor_IsotopeLabel FOREIGN KEY (IsotopeLabelId) REFERENCES targetedms.IsotopeLabel(Id);
ALTER TABLE targetedms.PeptideAreaRatio ADD CONSTRAINT FK_PeptideAreaRatio_IsotopeLabelId FOREIGN KEY (IsotopeLabelId) REFERENCES targetedms.IsotopeLabel(Id);
ALTER TABLE targetedms.PeptideAreaRatio ADD CONSTRAINT FK_PeptideAreaRatio_IsotopeLabelStdId FOREIGN KEY (IsotopeLabelStdId) REFERENCES targetedms.IsotopeLabel(Id);
ALTER TABLE targetedms.PrecursorAreaRatio ADD CONSTRAINT FK_PrecursorAreaRatio_IsotopeLabelId FOREIGN KEY (IsotopeLabelId) REFERENCES targetedms.IsotopeLabel(Id);
ALTER TABLE targetedms.PrecursorAreaRatio ADD CONSTRAINT FK_PrecursorAreaRatio_IsotopeLabelStdId FOREIGN KEY (IsotopeLabelStdId) REFERENCES targetedms.IsotopeLabel(Id);
ALTER TABLE targetedms.RunIsotopeModification ADD CONSTRAINT PK_RunIsotopeModification PRIMARY KEY (IsotopeModId, RunId, IsotopeLabelId);
ALTER TABLE targetedms.RunIsotopeModification ADD CONSTRAINT FK_RunIsotopeModification_IsotopeLabel FOREIGN KEY (IsotopeLabelId) REFERENCES targetedms.IsotopeLabel(Id);
ALTER TABLE targetedms.TransitionAreaRatio ADD CONSTRAINT FK_TransitionAreaRatio_IsotopeLabelId FOREIGN KEY (IsotopeLabelId) REFERENCES targetedms.IsotopeLabel(Id);
ALTER TABLE targetedms.TransitionAreaRatio ADD CONSTRAINT FK_TransitionAreaRatio_IsotopeLabelStdId FOREIGN KEY (IsotopeLabelStdId) REFERENCES targetedms.IsotopeLabel(Id);
GO

-- Add back Indexes
CREATE INDEX IX_FoldChange_IsotopeLabelId ON targetedms.FoldChange(IsotopeLabelId);
CREATE INDEX IX_GeneralPrecursor_IsotopeLabelId ON targetedms.GeneralPrecursor(IsotopeLabelId);
CREATE INDEX IX_PeptideAreaRatio_IsotopeLabelId ON targetedms.PeptideAreaRatio (IsotopeLabelId);
CREATE INDEX IX_PeptideAreaRatio_IsotopeLabelStdId ON targetedms.PeptideAreaRatio (IsotopeLabelStdId);
CREATE INDEX IX_PrecursorAreaRatio_IsotopeLabelId ON targetedms.PrecursorAreaRatio (IsotopeLabelId);
CREATE INDEX IX_PrecursorAreaRatio_IsotopeLabelStdId ON targetedms.PrecursorAreaRatio (IsotopeLabelStdId);
CREATE INDEX IX_RunIsotopeModification_IsotopeLabelId ON targetedms.RunIsotopeModification (IsotopeLabelId);
CREATE INDEX IX_TransitionAreaRatio_IsotopeLabelId ON targetedms.TransitionAreaRatio (IsotopeLabelId);
CREATE INDEX IX_TransitionAreaRatio_IsotopeLabelStdId ON targetedms.TransitionAreaRatio (IsotopeLabelStdId);
GO

---------------- TransitionChromInfoId --------------------
-----------------------------------------------------------

-- Drop Indexes
DROP INDEX targetedms.TransitionAreaRatio.IX_TransitionAreaRatio_TransitionChromInfoId;
DROP INDEX targetedms.TransitionAreaRatio.IX_TransitionAreaRatio_TransitionChromInfoStdId;
DROP INDEX targetedms.TransitionChromInfoAnnotation.IX_TransitionChromInfoAnnotation_TransitionChromInfoId;
GO

-- Drop Constraints
ALTER TABLE targetedms.TransitionAreaRatio DROP CONSTRAINT FK_TransitionAreaRatio_TransitionChromInfoId;
ALTER TABLE targetedms.TransitionAreaRatio DROP CONSTRAINT FK_TransitionAreaRatio_TransitionChromInfoStdId;
ALTER TABLE targetedms.TransitionChromInfoAnnotation DROP CONSTRAINT FK_TransitionChromInfoAnnotation_TransitionChromInfo;
ALTER TABLE targetedms.TransitionChromInfoAnnotation DROP CONSTRAINT UQ_TransitionChromInfoAnnotation_Name_TransitionChromInfo;
GO

-- Change SampleFileId
ALTER TABLE targetedms.TransitionChromInfo DROP CONSTRAINT PK_TransitionChromInfo;
GO
ALTER TABLE targetedms.TransitionChromInfo ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.TransitionChromInfo ADD CONSTRAINT PK_TransitionChromInfo PRIMARY KEY (Id);

-- Change Columns
ALTER TABLE targetedms.TransitionAreaRatio ALTER COLUMN TransitionChromInfoId bigint NOT NULL;
ALTER TABLE targetedms.TransitionAreaRatio ALTER COLUMN TransitionChromInfoStdId bigint NOT NULL;
ALTER TABLE targetedms.TransitionChromInfoAnnotation ALTER COLUMN TransitionChromInfoId bigint NOT NULL;
GO

-- Add back Constraints
ALTER TABLE targetedms.TransitionAreaRatio ADD CONSTRAINT FK_TransitionAreaRatio_TransitionChromInfoId FOREIGN KEY (TransitionChromInfoId) REFERENCES targetedms.TransitionChromInfo(Id);
ALTER TABLE targetedms.TransitionAreaRatio ADD CONSTRAINT FK_TransitionAreaRatio_TransitionChromInfoStdId FOREIGN KEY (TransitionChromInfoStdId) REFERENCES targetedms.TransitionChromInfo(Id);
ALTER TABLE targetedms.TransitionChromInfoAnnotation ADD CONSTRAINT FK_TransitionChromInfoAnnotation_TransitionChromInfo FOREIGN KEY (TransitionChromInfoId) REFERENCES targetedms.TransitionChromInfo(Id);
ALTER TABLE targetedms.TransitionChromInfoAnnotation ADD CONSTRAINT UQ_TransitionChromInfoAnnotation_Name_TransitionChromInfo UNIQUE (Name, TransitionChromInfoId);
GO

-- Add back Indexes
CREATE INDEX IX_TransitionChromInfoAnnotation_TransitionChromInfoId ON targetedms.TransitionChromInfoAnnotation(TransitionChromInfoId);
CREATE INDEX IX_TransitionAreaRatio_TransitionChromInfoId ON targetedms.TransitionAreaRatio (TransitionChromInfoId);
CREATE INDEX IX_TransitionAreaRatio_TransitionChromInfoStdId ON targetedms.TransitionAreaRatio (TransitionChromInfoStdId);
GO


---------------- GeneralMoleculeChromInfoId --------------------
----------------------------------------------------------------

-- Drop Indexes
DROP INDEX targetedms.PrecursorChromInfo.IX_PrecursorChromInfo_GeneralMoleculeChromInfoId;
DROP INDEX targetedms.PeptideAreaRatio.IX_PeptideAreaRatio_PeptideChromInfoId;
DROP INDEX targetedms.PeptideAreaRatio.IX_PeptideAreaRatio_PeptideChromInfoStdId;
GO

-- Drop Constraints
ALTER TABLE targetedms.PrecursorChromInfo DROP CONSTRAINT FK_PrecursorChromInfo_GMChromInfo;
ALTER TABLE targetedms.PeptideAreaRatio DROP CONSTRAINT FK_PeptideAreaRatio_PeptideChromInfoId;
ALTER TABLE targetedms.PeptideAreaRatio DROP CONSTRAINT FK_PeptideAreaRatio_PeptideChromInfoStdId;
GO

-- Change GeneralMoleculeChromInfoId
ALTER TABLE targetedms.GeneralMoleculeChromInfo DROP CONSTRAINT PK_GMChromInfoId;
GO
ALTER TABLE targetedms.GeneralMoleculeChromInfo ALTER COLUMN Id BIGINT NOT NULL;
GO
ALTER TABLE targetedms.GeneralMoleculeChromInfo ADD CONSTRAINT PK_GMChromInfoId PRIMARY KEY (Id);

-- Change Columns
ALTER TABLE targetedms.PrecursorChromInfo ALTER COLUMN GeneralMoleculeChromInfoId bigint NOT NULL;
ALTER TABLE targetedms.PeptideAreaRatio ALTER COLUMN PeptideChromInfoId bigint NOT NULL;
ALTER TABLE targetedms.PeptideAreaRatio ALTER COLUMN PeptideChromInfoStdId bigint NOT NULL;
GO

-- Add back Constraints
ALTER TABLE targetedms.PrecursorChromInfo ADD CONSTRAINT FK_PrecursorChromInfo_GMChromInfo FOREIGN KEY (GeneralMoleculeChromInfoId) REFERENCES targetedms.GeneralMoleculeChromInfo(Id);
ALTER TABLE targetedms.PeptideAreaRatio ADD CONSTRAINT FK_PeptideAreaRatio_PeptideChromInfoId FOREIGN KEY (PeptideChromInfoId) REFERENCES targetedms.GeneralMoleculeChromInfo(Id);
ALTER TABLE targetedms.PeptideAreaRatio ADD CONSTRAINT FK_PeptideAreaRatio_PeptideChromInfoStdId FOREIGN KEY (PeptideChromInfoStdId) REFERENCES targetedms.GeneralMoleculeChromInfo(Id);
GO

-- Add back Indexes
CREATE INDEX IX_PrecursorChromInfo_GeneralMoleculeChromInfoId ON targetedms.PrecursorChromInfo(GeneralMoleculeChromInfoId);
CREATE INDEX IX_PeptideAreaRatio_PeptideChromInfoId ON targetedms.PeptideAreaRatio (PeptideChromInfoId);
CREATE INDEX IX_PeptideAreaRatio_PeptideChromInfoStdId ON targetedms.PeptideAreaRatio (PeptideChromInfoStdId);
GO

---------------- StructuralModLossId --------------------
--------------------------------------------------------
-- Drop Indexes
DROP INDEX targetedms.TransitionLoss.IX_TransitionLoss_StructuralModLossId;
GO

-- Drop Constraints
ALTER TABLE targetedms.TransitionLoss DROP CONSTRAINT FK_TransitionLoss_StructuralModLossId;
GO

-- Change SampleFileId
ALTER TABLE targetedms.StructuralModLoss DROP CONSTRAINT PK_StructuralModLoss;
GO
ALTER TABLE targetedms.StructuralModLoss ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.StructuralModLoss ADD CONSTRAINT PK_StructuralModLoss PRIMARY KEY (Id);
GO

-- Change Columns
ALTER TABLE targetedms.TransitionLoss ALTER COLUMN StructuralModLossId bigint NOT NULL;
GO

-- Add back Constraints
ALTER TABLE targetedms.TransitionLoss ADD CONSTRAINT FK_TransitionLoss_StructuralModLossId FOREIGN KEY (StructuralModLossId) REFERENCES targetedms.StructuralModLoss(Id);
GO

-- Add back Indexes
CREATE INDEX IX_TransitionLoss_StructuralModLossId ON targetedms.TransitionLoss (StructuralModLossId);
GO

---------------- StructuralModId ------------------------------
----------------------------------------------------------------
-- Drop Indexes
DROP INDEX targetedms.PeptideStructuralModification.IX_PeptideStructuralModification_StructuralModId;
DROP INDEX targetedms.StructuralModLoss.IX_StructuralModification_StructuralModId;
GO

-- Drop Constraints
ALTER TABLE targetedms.PeptideStructuralModification DROP CONSTRAINT FK_PeptideStructuralModification_StructuralModification;
ALTER TABLE targetedms.RunStructuralModification DROP CONSTRAINT FK_RunStructuralModification_StructuralModification;
ALTER TABLE targetedms.StructuralModLoss DROP CONSTRAINT FK_StructuralModLoss_StructuralModification;
ALTER TABLE targetedms.RunStructuralModification DROP CONSTRAINT PK_RunStructuralModification;
GO

-- Change StructuralModId
ALTER TABLE targetedms.StructuralModification DROP CONSTRAINT PK_StructuralModification;
GO
ALTER TABLE targetedms.StructuralModification ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.StructuralModification ADD CONSTRAINT PK_StructuralModification PRIMARY KEY (Id);
GO

-- Change Columns
ALTER TABLE targetedms.PeptideStructuralModification ALTER COLUMN StructuralModId bigint NOT NULL;
ALTER TABLE targetedms.RunStructuralModification ALTER COLUMN StructuralModId bigint NOT NULL;
ALTER TABLE targetedms.StructuralModLoss ALTER COLUMN StructuralModId bigint NOT NULL;
GO

-- Add back Constraints
ALTER TABLE targetedms.PeptideStructuralModification  ADD CONSTRAINT FK_PeptideStructuralModification_StructuralModification FOREIGN KEY (StructuralModId) REFERENCES targetedms.StructuralModification(Id);
ALTER TABLE targetedms.RunStructuralModification ADD CONSTRAINT FK_RunStructuralModification_StructuralModification FOREIGN KEY (StructuralModId) REFERENCES targetedms.StructuralModification(Id);
ALTER TABLE targetedms.StructuralModLoss ADD CONSTRAINT FK_StructuralModLoss_StructuralModification FOREIGN KEY (StructuralModId) REFERENCES targetedms.StructuralModification(Id);
ALTER TABLE targetedms.RunStructuralModification ADD CONSTRAINT PK_RunStructuralModification PRIMARY KEY (StructuralModId, RunId)
    GO

-- Add back Indexes
CREATE INDEX IX_PeptideStructuralModification_StructuralModId ON targetedms.PeptideStructuralModification (StructuralModId);
CREATE INDEX IX_StructuralModification_StructuralModId ON targetedms.StructuralModLoss (StructuralModId);
GO


------------------ QuantificationSettingsId ------------------------------
--------------------------------------------------------------------------

-- Drop Indexes
DROP INDEX targetedms.CalibrationCurve.IX_CalibrationCurve_QuantificationSettingsId;
GO

-- Drop Constraints
ALTER TABLE targetedms.CalibrationCurve DROP CONSTRAINT FK_CalibrationCurve_QuantificationSettings;
GO

-- Change QuantificationSettingsId
ALTER TABLE targetedms.QuantificationSettings DROP CONSTRAINT PK_QuantificationSettings;
GO
ALTER TABLE targetedms.QuantificationSettings ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.QuantificationSettings ADD CONSTRAINT PK_QuantificationSettings PRIMARY KEY (Id);
GO

-- Change Columns
ALTER TABLE targetedms.CalibrationCurve ALTER COLUMN QuantificationSettingsId bigint;
GO

-- Add back Constraints
ALTER TABLE targetedms.CalibrationCurve ADD CONSTRAINT FK_CalibrationCurve_QuantificationSettings FOREIGN KEY (QuantificationSettingsId) REFERENCES targetedms.QuantificationSettings(Id);
GO

-- Add back Indexes
CREATE INDEX IX_CalibrationCurve_QuantificationSettingsId ON targetedms.CalibrationCurve(QuantificationSettingsId);
GO

------------------ GroupComparisonSettingsId ------------------------------
---------------------------------------------------------------------------

-- Drop Indexes
DROP INDEX targetedms.FoldChange.IX_FoldChange_GroupComparisonSettingsId;
GO

-- Drop Constraints
ALTER TABLE targetedms.FoldChange DROP CONSTRAINT FK_FoldChange_GroupComparisonSettings;
GO

-- Change GroupComparisonSettingsId
ALTER TABLE targetedms.GroupComparisonSettings DROP CONSTRAINT PK_GroupComparisonSettings;
GO
ALTER TABLE targetedms.GroupComparisonSettings ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.GroupComparisonSettings ADD CONSTRAINT PK_GroupComparisonSettings PRIMARY KEY (Id);
GO

-- Change Columns
ALTER TABLE targetedms.FoldChange ALTER COLUMN GroupComparisonSettingsId bigint NOT NULL;
GO

-- Add back Constraints
ALTER TABLE targetedms.FoldChange ADD CONSTRAINT FK_FoldChange_GroupComparisonSettings FOREIGN KEY (GroupComparisonSettingsId) REFERENCES targetedms.GroupComparisonSettings(Id);
GO

-- Add back Indexes
CREATE INDEX IX_FoldChange_GroupComparisonSettingsId ON targetedms.FoldChange(GroupComparisonSettingsId);
GO

------------------ IsolationSchemeId ------------------------------
--------------------------------------------------------------------
-- Drop Indexes
DROP INDEX targetedms.IsolationWindow.IX_IsolationWindow_IsolationSchemeId;
GO

-- Drop Constraints
ALTER TABLE targetedms.IsolationWindow DROP CONSTRAINT FK_IsolationWindow_IsolationScheme;
GO

-- Change IsolationSchemeId
ALTER TABLE targetedms.IsolationScheme DROP CONSTRAINT PK_IsolationScheme;
GO
ALTER TABLE targetedms.IsolationScheme ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.IsolationScheme ADD CONSTRAINT PK_IsolationScheme PRIMARY KEY (Id);
GO

-- Change Columns
ALTER TABLE targetedms.IsolationWindow ALTER COLUMN IsolationSchemeId bigint NOT NULL;
GO

-- Add back Constraints
ALTER TABLE targetedms.IsolationWindow ADD CONSTRAINT FK_IsolationWindow_IsolationScheme FOREIGN KEY (IsolationSchemeId) REFERENCES targetedms.IsolationScheme(Id);
GO

-- Add back Indexes
CREATE INDEX IX_IsolationWindow_IsolationSchemeId ON targetedms.IsolationWindow (IsolationSchemeId);
GO

------------------ ListDefinitionId ------------------------------
------------------------------------------------------------------

-- Drop Constraints
ALTER TABLE targetedms.ListColumnDefinition DROP CONSTRAINT FK_ListColumn_ListDefinitionId;
ALTER TABLE targetedms.ListColumnDefinition DROP CONSTRAINT UQ_ListColumn_ListDefinitionId_ColumnIndex;
ALTER TABLE targetedms.ListItem DROP CONSTRAINT FK_ListItem_ListDefinitionId;
GO

-- Change ListDefinitionId
ALTER TABLE targetedms.ListDefinition DROP CONSTRAINT PK_List;
GO

ALTER TABLE targetedms.ListDefinition ALTER COLUMN Id bigint NOT NULL;
GO

ALTER TABLE targetedms.ListDefinition ADD CONSTRAINT PK_List PRIMARY KEY(Id);
GO

-- Change Columns
ALTER TABLE targetedms.ListColumnDefinition ALTER COLUMN ListDefinitionId bigint NOT NULL;
ALTER TABLE targetedms.ListItem ALTER COLUMN ListDefinitionId bigint NOT NULL;
GO

-- Add back Constraints
ALTER TABLE targetedms.ListColumnDefinition ADD CONSTRAINT FK_ListColumn_ListDefinitionId FOREIGN KEY(ListDefinitionId) REFERENCES targetedms.ListDefinition(Id);
ALTER TABLE targetedms.ListColumnDefinition ADD CONSTRAINT UQ_ListColumn_ListDefinitionId_ColumnIndex UNIQUE(ListDefinitionId, ColumnIndex);
ALTER TABLE targetedms.ListItem ADD CONSTRAINT FK_ListItem_ListDefinitionId FOREIGN KEY(ListDefinitionId) REFERENCES targetedms.ListDefinition(Id);
GO

------------------ ListItemId ------------------------------
------------------------------------------------------------

-- Drop Constraints
ALTER TABLE targetedms.ListItemValue DROP CONSTRAINT FK_ListItemValue_ListItem;
ALTER TABLE targetedms.ListItemValue DROP CONSTRAINT UQ_ListItemValue_ListItemId_ColumnIndex;
GO

-- Change ListItemId
ALTER TABLE targetedms.ListItem DROP CONSTRAINT PK_ListItem;
GO

ALTER TABLE targetedms.ListItem ALTER COLUMN Id bigint NOT NULL;
GO

ALTER TABLE targetedms.ListItem ADD CONSTRAINT PK_ListItem PRIMARY KEY(Id);
GO

-- Change Columns
ALTER TABLE targetedms.ListItemValue ALTER COLUMN ListItemId bigint NOT NULL;
GO

-- Add back Constraints
ALTER TABLE targetedms.ListItemValue ADD CONSTRAINT FK_ListItemValue_ListItem FOREIGN KEY(ListItemId) REFERENCES targetedms.ListItem(Id);
ALTER TABLE targetedms.ListItemValue ADD CONSTRAINT UQ_ListItemValue_ListItemId_ColumnIndex UNIQUE(ListItemId, ColumnIndex);
GO

------------------ DriftTimePredictionSettingsId ------------------------------
-------------------------------------------------------------------------------

-- Drop Indexes
DROP INDEX targetedms.MeasuredDriftTime.IX_MeasuredDriftTime_DriftTimePredictionSettingsId;
GO

-- Drop Constraints
ALTER TABLE targetedms.MeasuredDriftTime DROP CONSTRAINT FK_MeasuredDriftTime_DriftTimePredictionSettings;
GO

-- Change DriftTimePredictionSettingsId
ALTER TABLE targetedms.DriftTimePredictionSettings DROP CONSTRAINT PK_DriftTimePredictionSettings;
GO

ALTER TABLE targetedms.DriftTimePredictionSettings ALTER COLUMN Id bigint NOT NULL;
GO

ALTER TABLE targetedms.DriftTimePredictionSettings ADD CONSTRAINT PK_DriftTimePredictionSettings PRIMARY KEY(Id);
GO

-- Change Columns
ALTER TABLE targetedms.MeasuredDriftTime ALTER COLUMN DriftTimePredictionSettingsId bigint;
GO

-- Add back Constraints
ALTER TABLE targetedms.MeasuredDriftTime ADD CONSTRAINT FK_MeasuredDriftTime_DriftTimePredictionSettings FOREIGN KEY (DriftTimePredictionSettingsId) REFERENCES targetedms.DriftTimePredictionSettings(Id);
GO

-- Add back Indexes
CREATE INDEX IX_MeasuredDriftTime_DriftTimePredictionSettingsId ON targetedms.MeasuredDriftTime(DriftTimePredictionSettingsId);
GO

------------------ PredictorId ------------------------------
-------------------------------------------------------------

-- Drop Indexes
DROP INDEX targetedms.Replicate.IX_Replicate_CePredictorId;
DROP INDEX targetedms.Replicate.IX_Replicate_DpPredictorId;
GO

-- Drop Constraints
ALTER TABLE targetedms.Replicate DROP CONSTRAINT FK_Replicate_PredictorCe;
ALTER TABLE targetedms.Replicate DROP CONSTRAINT FK_Replicate_PredictorDp;
ALTER TABLE targetedms.PredictorSettings DROP CONSTRAINT FK_PredictorSettings_PredictorId;
ALTER TABLE targetedms.PredictorSettings DROP CONSTRAINT UQ_PredictorSettings;
GO

-- Change PredictorId
ALTER TABLE targetedms.Predictor DROP CONSTRAINT PK_Predictor;
GO

ALTER TABLE targetedms.Predictor ALTER COLUMN Id bigint NOT NULL;
GO

ALTER TABLE targetedms.Predictor ADD CONSTRAINT PK_Predictor PRIMARY KEY(Id);
GO

-- Change Columns
ALTER TABLE targetedms.Replicate ALTER COLUMN CePredictorId bigint;
ALTER TABLE targetedms.Replicate ALTER COLUMN DpPredictorId bigint;
ALTER TABLE targetedms.PredictorSettings ALTER COLUMN PredictorId bigint NOT NULL;
ALTER TABLE targetedms.transitionpredictionsettings ALTER COLUMN CePredictorId bigint;
ALTER TABLE targetedms.transitionpredictionsettings ALTER COLUMN DpPredictorId bigint;
GO

-- Add back Constraints
ALTER TABLE targetedms.Replicate ADD CONSTRAINT FK_Replicate_PredictorCe FOREIGN KEY (CePredictorId) REFERENCES targetedms.Predictor(Id);
ALTER TABLE targetedms.Replicate ADD CONSTRAINT FK_Replicate_PredictorDp FOREIGN KEY (DpPredictorId) REFERENCES targetedms.Predictor(Id);
ALTER TABLE targetedms.PredictorSettings ADD CONSTRAINT UQ_PredictorSettings UNIQUE (PredictorId, Charge);
ALTER TABLE targetedms.PredictorSettings ADD CONSTRAINT FK_PredictorSettings_PredictorId FOREIGN KEY (PredictorId) REFERENCES targetedms.Predictor(Id);
GO

-- Add back Indexes
CREATE INDEX IX_Replicate_CePredictorId ON targetedms.Replicate(CePredictorId);
CREATE INDEX IX_Replicate_DpPredictorId ON targetedms.Replicate(DpPredictorId);
GO

ALTER TABLE targetedms.AnnotationSettings DROP CONSTRAINT PK_AnnotationSettings;
GO
ALTER TABLE targetedms.AnnotationSettings ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.AnnotationSettings ADD CONSTRAINT PK_AnnotationSettings PRIMARY KEY (Id);
GO

------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.IsolationWindow DROP CONSTRAINT PK_IsolationWindow;
GO
ALTER TABLE targetedms.IsolationWindow ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.IsolationWindow ADD CONSTRAINT PK_IsolationWindow PRIMARY KEY (Id);
GO

-------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.ChromatogramLibInfo DROP CONSTRAINT PK_ChromatogramLibInfo;
GO
ALTER TABLE targetedms.ChromatogramLibInfo ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.ChromatogramLibInfo ADD CONSTRAINT PK_ChromatogramLibInfo PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.GeneralMoleculeAnnotation DROP CONSTRAINT PK_GMAnnotation;
GO
ALTER TABLE targetedms.GeneralMoleculeAnnotation ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.GeneralMoleculeAnnotation ADD CONSTRAINT PK_GMAnnotation PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------

ALTER TABLE targetedms.IsotopeEnrichment DROP CONSTRAINT PK_IsotopeEnrichment;
GO
ALTER TABLE targetedms.IsotopeEnrichment ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.IsotopeEnrichment ADD CONSTRAINT PK_IsotopeEnrichment PRIMARY KEY (Id);
GO


--------------------------------------------------------------------------------------------------
DROP INDEX targetedms.SampleFile.IX_SampleFile_InstrumentId;
GO
ALTER TABLE targetedms.SampleFile DROP CONSTRAINT FK_SampleFile_Instrument;
ALTER TABLE targetedms.Instrument DROP CONSTRAINT PK_Instrument;
GO
ALTER TABLE targetedms.Instrument ALTER COLUMN Id bigint NOT NULL;
ALTER TABLE targetedms.SampleFile ALTER COLUMN InstrumentId bigint;
GO
ALTER TABLE targetedms.Instrument ADD CONSTRAINT PK_Instrument PRIMARY KEY (Id);
ALTER TABLE targetedms.SampleFile ADD CONSTRAINT FK_SampleFile_Instrument FOREIGN KEY (InstrumentId) REFERENCES targetedms.Instrument(Id);
GO
CREATE INDEX IX_SampleFile_InstrumentId ON targetedms.SampleFile(InstrumentId);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.ListColumnDefinition DROP CONSTRAINT PK_ListColumn;
GO
ALTER TABLE targetedms.ListColumnDefinition ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.ListColumnDefinition ADD CONSTRAINT PK_ListColumn PRIMARY KEY(Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.ListItemValue DROP CONSTRAINT FK_ListItemValue_ListItem;
ALTER TABLE targetedms.ListItem DROP CONSTRAINT PK_ListItem;
GO
ALTER TABLE targetedms.ListItem ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.ListItem ADD CONSTRAINT PK_ListItem PRIMARY KEY(Id);
ALTER TABLE targetedms.ListItemValue ADD CONSTRAINT FK_ListItemValue_ListItem FOREIGN KEY(ListItemId) REFERENCES targetedms.ListItem(Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.ListItemValue DROP CONSTRAINT PK_ListItemValue;
GO
ALTER TABLE targetedms.ListItemValue ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.ListItemValue ADD CONSTRAINT PK_ListItemValue PRIMARY KEY(Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.MeasuredDriftTime DROP CONSTRAINT PK_MeasuredDriftTime;
GO
ALTER TABLE targetedms.MeasuredDriftTime ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.MeasuredDriftTime ADD CONSTRAINT PK_MeasuredDriftTime PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.BibliospecLibInfo DROP CONSTRAINT pk_bibliospeclibinfo;
GO
ALTER TABLE targetedms.BibliospecLibInfo ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.BibliospecLibInfo ADD CONSTRAINT pk_bibliospeclibinfo PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.ChromatogramLibInfo DROP CONSTRAINT PK_ChromatogramLibInfo;
GO
ALTER TABLE targetedms.ChromatogramLibInfo ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.ChromatogramLibInfo ADD CONSTRAINT PK_ChromatogramLibInfo PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.HunterLibInfo DROP CONSTRAINT pk_hunterlibinfo;
GO
ALTER TABLE targetedms.HunterLibInfo ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.HunterLibInfo ADD CONSTRAINT pk_hunterlibinfo PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.NistLibInfo DROP CONSTRAINT PK_NistLibInfo;
GO
ALTER TABLE targetedms.NistLibInfo ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.NistLibInfo ADD CONSTRAINT PK_NistLibInfo PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.SpectrastLibInfo DROP CONSTRAINT PK_SpectrastLibInfo;
GO
ALTER TABLE targetedms.SpectrastLibInfo ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.SpectrastLibInfo ADD CONSTRAINT PK_SpectrastLibInfo PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.PeptideAreaRatio DROP CONSTRAINT PK_PeptideAreaRatio;
GO
ALTER TABLE targetedms.PeptideAreaRatio ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.PeptideAreaRatio ADD CONSTRAINT PK_PeptideAreaRatio PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.PeptideGroupAnnotation DROP CONSTRAINT PK_PeptideGroupAnnotation;
GO
ALTER TABLE targetedms.PeptideGroupAnnotation ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.PeptideGroupAnnotation  ADD CONSTRAINT PK_PeptideGroupAnnotation PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.PeptideIsotopeModification DROP CONSTRAINT PK_PeptideIsotopeModification;
GO
ALTER TABLE targetedms.PeptideIsotopeModification ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.PeptideIsotopeModification  ADD CONSTRAINT PK_PeptideIsotopeModification PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.PeptideStructuralModification DROP CONSTRAINT PK_PeptideStructuralModification;
GO
ALTER TABLE targetedms.PeptideStructuralModification ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.PeptideStructuralModification  ADD CONSTRAINT PK_PeptideStructuralModification PRIMARY KEY (Id);
GO
--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.PrecursorAnnotation DROP CONSTRAINT PK_PrecursorAnnotation;
GO
ALTER TABLE targetedms.PrecursorAnnotation ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.PrecursorAnnotation  ADD CONSTRAINT PK_PrecursorAnnotation PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.PrecursorAreaRatio DROP CONSTRAINT PK_PrecursorAreaRatio;
GO
ALTER TABLE targetedms.PrecursorAreaRatio ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.PrecursorAreaRatio ADD CONSTRAINT PK_PrecursorAreaRatio PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.PrecursorChromInfoAnnotation DROP CONSTRAINT PK_PrecursorChromInfoAnnotation;
GO
ALTER TABLE targetedms.PrecursorChromInfoAnnotation ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.PrecursorChromInfoAnnotation ADD CONSTRAINT PK_PrecursorChromInfoAnnotation PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.ReplicateAnnotation DROP CONSTRAINT PK_ReplicateAnnotation;
GO
ALTER TABLE targetedms.ReplicateAnnotation ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.ReplicateAnnotation  ADD CONSTRAINT PK_ReplicateAnnotation PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.SampleFileChromInfo DROP CONSTRAINT PK_SampleFileChromInfo;
GO
ALTER TABLE targetedms.SampleFileChromInfo ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.SampleFileChromInfo ADD CONSTRAINT PK_SampleFileChromInfo PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.SpectrastLibInfo DROP CONSTRAINT PK_SpectrastLibInfo;
GO
ALTER TABLE targetedms.SpectrastLibInfo ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.SpectrastLibInfo ADD CONSTRAINT PK_SpectrastLibInfo PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.TransitionAnnotation DROP CONSTRAINT PK_TransitionAnnotation;
GO
ALTER TABLE targetedms.TransitionAnnotation ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.TransitionAnnotation ADD CONSTRAINT PK_TransitionAnnotation PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.TransitionAreaRatio DROP CONSTRAINT PK_TransitionAreaRatio;
GO
ALTER TABLE targetedms.TransitionAreaRatio ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.TransitionAreaRatio ADD CONSTRAINT PK_TransitionAreaRatio PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.TransitionAreaRatio DROP CONSTRAINT FK_TransitionAreaRatio_TransitionChromInfoStdId;
ALTER TABLE targetedms.TransitionChromInfoAnnotation DROP CONSTRAINT PK_TransitionChromInfoAnnotation;
GO
ALTER TABLE targetedms.TransitionChromInfoAnnotation ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.TransitionChromInfoAnnotation ADD CONSTRAINT PK_TransitionChromInfoAnnotation PRIMARY KEY (Id)
ALTER TABLE targetedms.TransitionAreaRatio ADD CONSTRAINT FK_TransitionAreaRatio_TransitionChromInfoStdId FOREIGN KEY (TransitionChromInfoStdId) REFERENCES targetedms.TransitionChromInfo(Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.TransitionLoss DROP CONSTRAINT PK_TransitionLoss;
GO
ALTER TABLE targetedms.TransitionLoss ALTER COLUMN Id bigint;
GO
ALTER TABLE targetedms.TransitionLoss ADD CONSTRAINT PK_TransitionLoss PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.TransitionOptimization DROP CONSTRAINT PK_TransitionOptimization;
GO
ALTER TABLE targetedms.TransitionOptimization ALTER COLUMN Id bigint;
GO
ALTER TABLE targetedms.TransitionOptimization ADD CONSTRAINT PK_TransitionOptimization PRIMARY KEY (Id);
GO

ALTER TABLE targetedms.PrecursorChromInfo ADD TransitionChromatogramIndices IMAGE;

ALTER TABLE targetedms.PrecursorChromInfo ADD BestMassErrorPPM Real;