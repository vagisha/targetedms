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
SELECT core.executeJavaInitializationCode('populateDefaultAnnotationTypes');

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

/* targetedms-17.10-17.20.sql */

ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN ChromatogramFormat INT;

CREATE TABLE targetedms.QCMetricExclusion
(
    Id SERIAL NOT NULL,
    CreatedBy USERID,
    Created TIMESTAMP,
    ModifiedBy USERID,
    Modified TIMESTAMP,

    ReplicateId INT NOT NULL,
    MetricId INT, -- allow NULL to indicate exclusion of replicate for all metrics

    CONSTRAINT PK_QCMetricExclusion PRIMARY KEY (Id),
    CONSTRAINT FK_QCMetricExclusion_ReplicateId FOREIGN KEY (ReplicateId) REFERENCES targetedms.Replicate (Id),
    CONSTRAINT FK_QCMetricExclusion_MetricId FOREIGN KEY (MetricId) REFERENCES targetedms.QCMetricConfiguration (Id),
    CONSTRAINT UQ_QCMetricExclusion_Replicate_Metric UNIQUE (ReplicateId, MetricId)
);
CREATE INDEX IX_QCMetricExclusion_ReplicateId ON targetedms.QCMetricExclusion(ReplicateId);
CREATE INDEX IX_QCMetricExclusion_MetricId ON targetedms.QCMetricExclusion(MetricId);

ALTER TABLE targetedms.ExperimentAnnotations ADD COLUMN sourceExperimentId INT;
ALTER TABLE targetedms.ExperimentAnnotations ADD COLUMN sourceExperimentPath VARCHAR(1000);
ALTER TABLE targetedms.ExperimentAnnotations ADD COLUMN shortUrl entityId;

CREATE INDEX IX_ExperimentAnnotations_SourceExperimentId ON targetedms.ExperimentAnnotations (sourceExperimentId);
ALTER TABLE targetedms.ExperimentAnnotations ADD CONSTRAINT UQ_ExperimentAnnotations_ShortUrl UNIQUE (shortUrl);
ALTER TABLE targetedms.ExperimentAnnotations ADD CONSTRAINT FK_ExperimentAnnotations_ShortUrl FOREIGN KEY (shorturl)
REFERENCES core.shorturl (entityId);

/* targetedms-17.20-17.30.sql */

ALTER TABLE targetedms.Runs ADD COLUMN ReplicateCount INT;
UPDATE targetedms.Runs SET ReplicateCount = (SELECT COUNT(r.id) FROM targetedms.Replicate r WHERE r.RunId = targetedms.Runs.Id);

ALTER TABLE targetedms.TransitionChromInfo ADD COLUMN PointsAcrossPeak INT;

/* targetedms-17.30-18.10.sql */

ALTER TABLE targetedms.ReplicateAnnotation ALTER COLUMN Value TYPE VARCHAR(500);

/* targetedms-18.10-18.20.sql */

ALTER TABLE targetedms.Runs ADD COLUMN SkydDataId INT;

ALTER TABLE targetedms.Runs ADD CONSTRAINT FK_Runs_SkydData FOREIGN KEY (SkydDataId) REFERENCES exp.Data(RowId);


ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN ChromatogramOffset BIGINT;
ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN ChromatogramLength INT;

ALTER TABLE targetedms.Runs ADD COLUMN CalibrationCurveCount INT;
UPDATE targetedms.Runs SET CalibrationCurveCount = (SELECT COUNT(c.id) FROM targetedms.CalibrationCurve c WHERE c.RunId = targetedms.Runs.Id);

ALTER TABLE targetedms.GeneralMoleculeChromInfo ADD COLUMN ExcludeFromCalibration BOOLEAN;
UPDATE targetedms.GeneralMoleculeChromInfo SET ExcludeFromCalibration = false;

ALTER TABLE targetedms.QuantificationSettings ADD COLUMN MaxLOQBias FLOAT;
ALTER TABLE targetedms.QuantificationSettings ADD COLUMN MaxLOQCV FLOAT;
ALTER TABLE targetedms.QuantificationSettings ADD COLUMN LODCalculation VARCHAR(50);

ALTER TABLE targetedms.ExperimentAnnotations ADD COLUMN Keywords VARCHAR(200);
ALTER TABLE targetedms.ExperimentAnnotations ADD COLUMN LabHead USERID;
ALTER TABLE targetedms.ExperimentAnnotations ADD COLUMN LabHeadAffiliation VARCHAR(200);
ALTER TABLE targetedms.ExperimentAnnotations ADD COLUMN Submitter USERID;
ALTER TABLE targetedms.ExperimentAnnotations ADD COLUMN SubmitterAffiliation VARCHAR(200);
ALTER TABLE targetedms.ExperimentAnnotations ADD COLUMN pxid VARCHAR(10);

ALTER TABLE targetedms.JournalExperiment ADD COLUMN PxidRequested BOOLEAN NOT NULL DEFAULT '0';
ALTER TABLE targetedms.JournalExperiment ADD COLUMN KeepPrivate BOOLEAN NOT NULL DEFAULT '1';

ALTER TABLE targetedms.spectrumlibrary ALTER COLUMN Name TYPE VARCHAR(400);

/* targetedms-18.20-18.30.sql */

ALTER TABLE targetedms.experimentannotations ALTER COLUMN Organism TYPE VARCHAR(300);
ALTER TABLE targetedms.Replicate ADD COLUMN SampleDilutionFactor DOUBLE PRECISION;

/* targetedms-18.30-19.10.sql */

UPDATE exp.data a set sourceapplicationid = (SELECT d.sourceapplicationid from exp.data d where d.runid = a.runid AND d.name LIKE '%.zip')
WHERE a.sourceapplicationid IS NULL AND a.runid IS NOT NULL AND a.name LIKE '%.skyd';

-- Add a column to store the size of the Skyline document
ALTER TABLE targetedms.runs ADD COLUMN DocumentSize BIGINT;

ALTER TABLE targetedms.precursorchrominfo ADD COLUMN qvalue REAL;
ALTER TABLE targetedms.precursorchrominfo ADD COLUMN zscore REAL;

/* targetedms-19.10-19.20.sql */

CREATE TABLE targetedms.QCEnabledMetrics
(
  metric          INT,
  enabled         BIT,
  lowerBound      DOUBLE PRECISION,
  upperBound      DOUBLE PRECISION,
  cusumLimit      DOUBLE PRECISION,

  Created         TIMESTAMP,
  CreatedBy       USERID,
  Modified        TIMESTAMP,
  ModifiedBy      USERID,
  Container       ENTITYID NOT NULL,

  CONSTRAINT PK_QCEnabledMetrics PRIMARY KEY (metric),
  CONSTRAINT FK_QCEnabledMetrics_Metric FOREIGN KEY (metric) REFERENCES targetedms.qcmetricconfiguration(Id),
  CONSTRAINT FK_QCEnabledMetrics_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId)
);

DROP TABLE targetedms.QCEnabledMetrics;

CREATE TABLE targetedms.QCEnabledMetrics
(
  metric          INT,
  enabled         BOOLEAN,
  lowerBound      DOUBLE PRECISION,
  upperBound      DOUBLE PRECISION,
  cusumLimit      DOUBLE PRECISION,

  Created         TIMESTAMP,
  CreatedBy       USERID,
  Modified        TIMESTAMP,
  ModifiedBy      USERID,
  Container       ENTITYID NOT NULL,

  CONSTRAINT PK_QCEnabledMetrics PRIMARY KEY (metric),
  CONSTRAINT FK_QCEnabledMetrics_Metric FOREIGN KEY (metric) REFERENCES targetedms.qcmetricconfiguration(Id),
  CONSTRAINT FK_QCEnabledMetrics_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId)
);

CREATE INDEX IX_targetedms_qcEnabledMetrics_Container ON targetedms.QCEnabledMetrics (Container);

ALTER TABLE targetedms.QCEnabledMetrics DROP CONSTRAINT PK_QCEnabledMetrics;

ALTER TABLE targetedms.QCEnabledMetrics ADD CONSTRAINT PK_QCEnabledMetrics PRIMARY KEY (metric, Container);

CREATE TABLE targetedms.AuditLogEntry (
    entryId serial not null,
    documentGUID entityid not null,
    entryHash varchar(100) not null,
    versionId int4 null,
    createTimestamp timestamp not null,
    timezoneOffset int4 not null,
    userName  varchar(100) not null,
    formatVersion varchar(100) not null,
    parentEntryHash varchar(100) null,
    reason varchar(1000) NULL,
    extraInfo varchar NULL,
    CONSTRAINT pk_auditLogEntry PRIMARY KEY (entryId),
    CONSTRAINT fk_auditLogEntry_runs FOREIGN KEY (versionId) REFERENCES targetedms.runs(id)
);

CREATE UNIQUE INDEX uix_auditLogEntry_document on targetedms.AuditLogEntry USING btree (documentGUID, entryHash);


CREATE TABLE targetedms.AuditLogMessage(
  messageId serial not null,
  orderNumber int4 not null,
  entryId int4 not null,
  messageType varchar(50) not null,
  enText varchar null,
  expandedText varchar null,
  reason varchar(1000) null,
  CONSTRAINT pk_auditLogMessage PRIMARY KEY (messageId),
  CONSTRAINT fk_auditLogMessage_entry FOREIGN KEY (entryId) REFERENCES targetedms.AuditLogEntry(entryId)
);

CREATE UNIQUE INDEX uix_auditLogMessage_entry on targetedms.AuditLogMessage USING btree (entryId, orderNumber);

UPDATE targetedms.qcmetricconfiguration SET name='Transition & Precursor Areas' WHERE name='Transition/Precursor Areas';

ALTER TABLE targetedms.qcmetricconfiguration ADD COLUMN PrecursorScoped BOOLEAN NOT NULL DEFAULT TRUE;

DROP TABLE IF EXISTS targetedms.QCEmailNotifications;

-- Increase the length of the Gene column. The gene field can contain all possible gene names that a protein product is associated with. This can get really long.
ALTER TABLE targetedms.PeptideGroup ALTER COLUMN gene TYPE VARCHAR(2000);

CREATE INDEX uix_auditLogEntry_version on targetedms.AuditLogEntry USING btree (versionId);

-- ----------------------------------------------------------------------------
-- Updates to MeasuredDriftTime
-- ----------------------------------------------------------------------------
ALTER TABLE targetedms.MeasuredDriftTime ALTER COLUMN DriftTime DROP NOT NULL;
-- From Brian Pratt about the charge field: either a simple number or an addition description-
-- 1, -4, [M+H]. But no hard limit to adduct string. Typically short though.
-- Longest one there seems to be [M+IsoProp+Na+H] (17 characters) though most come in below 10
ALTER TABLE targetedms.MeasuredDriftTime ALTER COLUMN Charge TYPE VARCHAR(30);
ALTER TABLE targetedms.MeasuredDriftTime ADD Ccs DOUBLE PRECISION;
ALTER TABLE targetedms.MeasuredDriftTime ADD IonMobility DOUBLE PRECISION;
ALTER TABLE targetedms.MeasuredDriftTime ADD HighEnergyIonMobilityOffset DOUBLE PRECISION;
-- From Brian Pratt about the ion_mobility_units field: Worst case is 23 characters, for Bruker:  inverse_K0_Vsec_per_cm2
ALTER TABLE targetedms.MeasuredDriftTime ADD IonMobilityUnits VARCHAR(30);

/* targetedms-19.20-19.30.sql */

ALTER TABLE targetedms.Runs ADD COLUMN AuditLogEntriesCount INT4 DEFAULT 0 NOT NULL;

WITH rootIdentity as (select EntityId as theIdentity FROM core.Containers WHERE Parent is null)

INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName,Series2Label,Series2SchemaName,Series2QueryName,PrecursorScoped) VALUES
((select theIdentity from rootIdentity), 'TIC Area','TIC Area','targetedms','QCRunMetric_ticArea',NULL , NULL , NULL , FALSE );

ALTER TABLE targetedms.SampleFile ADD COLUMN TicArea DOUBLE PRECISION;

ALTER TABLE targetedms.GeneralMolecule ADD COLUMN AttributeGroupId VARCHAR(100);

ALTER TABLE targetedms.Replicate ALTER COLUMN Name TYPE VARCHAR(200);

ALTER TABLE targetedms.SampleFile ADD COLUMN InstrumentSerialNumber VARCHAR(200);
ALTER TABLE targetedms.SampleFile ADD COLUMN SampleId VARCHAR(200);

CREATE TABLE targetedms.ListDefinition
(
    Id SERIAL NOT NULL,
    RunId INT NOT NULL,
    Name TEXT NOT NULL,
    PkColumnIndex INT NULL,
    DisplayColumnIndex INT NULL,
    CONSTRAINT PK_List PRIMARY KEY(Id),
    CONSTRAINT FK_List_RunId FOREIGN KEY(RunId) REFERENCES targetedms.Runs(Id)
);
CREATE TABLE targetedms.ListColumnDefinition
(
    Id SERIAL NOT NULL,
    ListDefinitionId INT NOT NULL,
    ColumnIndex INT NOT NULL,
    AnnotationType VARCHAR(20) NOT NULL,
    Name TEXT NOT NULL,
    Lookup TEXT NULL,
    CONSTRAINT PK_ListColumn PRIMARY KEY(Id),
    CONSTRAINT FK_ListColumn_ListDefinitionId FOREIGN KEY(ListDefinitionId) REFERENCES targetedms.ListDefinition(Id),
    CONSTRAINT UQ_ListColumn_ListDefinitionId_ColumnIndex UNIQUE(ListDefinitionId, ColumnIndex)
);

CREATE TABLE targetedms.ListItem
(
    Id SERIAL NOT NULL,
    ListDefinitionId INT NOT NULL,
    CONSTRAINT PK_ListItem PRIMARY KEY(Id),
    CONSTRAINT FK_ListItem_ListDefinitionId FOREIGN KEY(ListDefinitionId) REFERENCES targetedms.ListDefinition(Id)
);

CREATE TABLE targetedms.ListItemValue
(
    Id SERIAL NOT NULL,
    ListItemId INT NOT NULL,
    ColumnIndex INT NOT NULL,
    TextValue TEXT NULL,
    NumericValue DOUBLE PRECISION NULL,
    CONSTRAINT PK_ListItemValue PRIMARY KEY(Id),
    CONSTRAINT FK_ListItemValue_ListItem FOREIGN KEY(ListItemId) REFERENCES targetedms.ListItem(Id),
    CONSTRAINT UQ_ListItemValue_ListItemId_ColumnIndex UNIQUE(ListItemId, ColumnIndex)
);

ALTER TABLE targetedms.QCMetricConfiguration ADD COLUMN EnabledQueryName VARCHAR(200);
ALTER TABLE targetedms.QCMetricConfiguration ADD COLUMN EnabledSchemaName VARCHAR(200);

WITH rootIdentity as (select EntityId as theIdentity FROM core.Containers WHERE Parent is null)
INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName,Series2Label,Series2SchemaName,Series2QueryName,PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
  ((select theIdentity from rootIdentity), 'Isotopologue LOD', 'LOD','targetedms', 'QCMetric_IsotopologuePrecursorLOD', NULL, NULL, NULL, TRUE, 'QCMetricEnabled_IsotopologuePrecursorLOD', 'targetedms');

WITH rootIdentity as (select EntityId as theIdentity FROM core.Containers WHERE Parent is null)
INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName,Series2Label,Series2SchemaName,Series2QueryName,PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
  ((select theIdentity from rootIdentity), 'Isotopologue LOQ', 'LOQ', 'targetedms', 'QCMetric_IsotopologuePrecursorLOQ', NULL, NULL, NULL, TRUE, 'QCMetricEnabled_IsotopologuePrecursorLOQ', 'targetedms');

WITH rootIdentity as (select EntityId as theIdentity FROM core.Containers WHERE Parent is null)
INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName,Series2Label,Series2SchemaName,Series2QueryName,PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
  ((select theIdentity from rootIdentity), 'Isotopologue Accuracy', 'Accuracy', 'targetedms', 'QCMetric_IsotopologuePrecursorAccuracy', NULL, NULL, NULL, TRUE, 'QCMetricEnabled_IsotopologuePrecursorAccuracy', 'targetedms');

WITH rootIdentity as (select EntityId as theIdentity FROM core.Containers WHERE Parent is null)
INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName,Series2Label,Series2SchemaName,Series2QueryName,PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
  ((select theIdentity from rootIdentity), 'Isotopologue Regression RSquared', 'Coefficient', 'targetedms', 'QCMetric_IsotopologuePrecursorRSquared', NULL, NULL, NULL, TRUE, 'QCMetricEnabled_IsotopologuePrecursorRSquared', 'targetedms');

UPDATE targetedms.QCMetricConfiguration SET EnabledQueryName = 'QCMetricEnabled_lhRatio', EnabledSchemaName ='targetedms' WHERE Series1QueryName = 'QCMetric_lhRatio';

ALTER TABLE targetedms.runs ALTER COLUMN SoftwareVersion TYPE VARCHAR(200);

ALTER TABLE targetedms.Runs ADD COLUMN ListCount INT DEFAULT 0 NOT NULL;

ALTER TABLE targetedms.AnnotationSettings ADD COLUMN Lookup TEXT;

CREATE TABLE targetedms.SampleFileChromInfo
(
  Id serial NOT NULL,
  SampleFileId integer NOT NULL,
  StartTime real,
  EndTime real,
  TextId VARCHAR(512),
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

ALTER TABLE targetedms.Replicate ADD COLUMN HasMidasSpectra BOOLEAN;
ALTER TABLE targetedms.Replicate ADD COLUMN BatchName VARCHAR(200);

ALTER TABLE targetedms.SampleFile ADD COLUMN ExplicitGlobalStandardArea DOUBLE PRECISION;
ALTER TABLE targetedms.SampleFile ADD COLUMN IonMobilityType VARCHAR(200);

ALTER TABLE targetedms.GeneralMolecule ADD COLUMN ExplicitRetentionTimeWindow DOUBLE PRECISION;

ALTER TABLE targetedms.Molecule ADD COLUMN MoleculeId VARCHAR(200);

--** changes to GeneralPrecursor **--
ALTER TABLE targetedms.GeneralPrecursor ADD COLUMN Ccs DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralPrecursor ADD COLUMN ExplicitIonMobilityUnits VARCHAR(200);
ALTER TABLE targetedms.GeneralPrecursor ADD COLUMN ExplicitCcsSqa DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralPrecursor ADD COLUMN ExplicitCompensationVoltage DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralPrecursor ADD COLUMN PrecursorConcentration DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralPrecursor RENAME COLUMN ExplicitDriftTimeMsec TO ExplicitIonMobility;
ALTER TABLE targetedms.GeneralPrecursor DROP COLUMN ExplicitCollisionEnergy;
ALTER TABLE targetedms.GeneralPrecursor DROP COLUMN ExplicitDriftTimeHighEnergyOffsetMsec;
ALTER TABLE targetedms.GeneralPrecursor DROP COLUMN Decoy;
ALTER TABLE targetedms.GeneralPrecursor DROP COLUMN Modified;

--** changes to GeneralTransition **--
ALTER TABLE targetedms.GeneralTransition ADD COLUMN Rank INT ;
UPDATE targetedms.GeneralTransition gt
SET Rank = (SELECT libraryRank FROM targetedms.Transition t WHERE t.id = gt.id);
ALTER TABLE targetedms.GeneralTransition ADD COLUMN Intensity DOUBLE PRECISION ;
UPDATE targetedms.GeneralTransition gt
SET Intensity = (SELECT libraryIntensity FROM targetedms.Transition t WHERE t.id = gt.id);
ALTER TABLE targetedms.GeneralTransition ADD COLUMN Quantitative BOOLEAN;
ALTER TABLE targetedms.GeneralTransition ADD COLUMN CollisionEnergy DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralTransition ADD COLUMN DeclusteringPotential DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralTransition RENAME COLUMN SLens TO ExplicitSLens;
ALTER TABLE targetedms.GeneralTransition RENAME COLUMN ConeVoltage TO ExplicitConeVoltage;
ALTER TABLE targetedms.GeneralTransition RENAME COLUMN ExplicitDriftTimeHighEnergyOffsetMSec TO ExplicitIonMobilityHighEnergyOffset;
ALTER TABLE targetedms.GeneralTransition DROP COLUMN ExplicitCompensationVoltage;
ALTER TABLE targetedms.GeneralTransition DROP COLUMN ExplicitDriftTimeMSec;
ALTER TABLE targetedms.GeneralTransition DROP COLUMN Decoy;

ALTER TABLE targetedms.Transition DROP COLUMN LibraryRank;
ALTER TABLE targetedms.Transition DROP COLUMN LibraryIntensity;

ALTER TABLE targetedms.MoleculePrecursor ADD COLUMN MoleculePrecursorId VARCHAR(200);

ALTER TABLE targetedms.MoleculeTransition ADD COLUMN MoleculeTransitionId VARCHAR(200);

ALTER TABLE targetedms.GeneralMoleculeChromInfo ADD COLUMN PredictedRetentionTime DOUBLE PRECISION;

ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN Ccs DOUBLE PRECISION;
ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN DriftTimeMs1 DOUBLE PRECISION;
ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN DriftTimeFragment DOUBLE PRECISION;
ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN DriftTimeWindow DOUBLE PRECISION;
ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN IonMobilityMs1 DOUBLE PRECISION;
ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN IonMobilityFragment DOUBLE PRECISION;
ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN IonMobilityWindow DOUBLE PRECISION;
ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN IonMobilityType VARCHAR(200);

ALTER TABLE targetedms.TransitionChromInfo ADD COLUMN Ccs DOUBLE PRECISION;
ALTER TABLE targetedms.TransitionChromInfo ADD COLUMN DriftTime DOUBLE PRECISION;
ALTER TABLE targetedms.TransitionChromInfo ADD COLUMN DriftTimeWindow DOUBLE PRECISION;
ALTER TABLE targetedms.TransitionChromInfo ADD COLUMN IonMobility DOUBLE PRECISION;
ALTER TABLE targetedms.TransitionChromInfo ADD COLUMN IonMobilityWindow DOUBLE PRECISION;
ALTER TABLE targetedms.TransitionChromInfo ADD COLUMN IonMobilityType VARCHAR(200);
ALTER TABLE targetedms.TransitionChromInfo ADD COLUMN Rank INT;
ALTER TABLE targetedms.TransitionChromInfo ADD COLUMN RankByLevel INT;
ALTER TABLE targetedms.TransitionChromInfo ADD COLUMN ForcedIntegration BOOLEAN;

ALTER TABLE targetedms.GroupComparisonSettings ADD COLUMN AvgTechReplicates BOOLEAN;
ALTER TABLE targetedms.GroupComparisonSettings ADD COLUMN SumTransitions BOOLEAN;
ALTER TABLE targetedms.GroupComparisonSettings ADD COLUMN IncludeInteractionTransitions BOOLEAN;
ALTER TABLE targetedms.GroupComparisonSettings ADD COLUMN SummarizationMethod VARCHAR(200);

ALTER TABLE targetedms.Enzyme ADD COLUMN Semi BOOLEAN;

ALTER TABLE targetedms.SpectrumLibrary ADD COLUMN UseExplicitPeakBounds BOOLEAN;

ALTER TABLE targetedms.IsotopeModification ADD COLUMN Label37Cl BOOLEAN;
ALTER TABLE targetedms.IsotopeModification ADD COLUMN Label81Br BOOLEAN;

CREATE TABLE targetedms.BibliospecLibInfo
(
    Id SERIAL NOT NULL,
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
    Id SERIAL NOT NULL,
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
    Id SERIAL NOT NULL,
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
    Id SERIAL NOT NULL,
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
    Id SERIAL NOT NULL,
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

ALTER TABLE targetedms.SpectrumLibrary ADD PanoramaServer VARCHAR(200);

INSERT INTO targetedms.IsotopeLabel (RunId, Name, Standard)
  SELECT DISTINCT pg.RunId AS RunId, 'Unknown' AS Name, TRUE AS Standard
    FROM targetedms.GeneralPrecursor gp
    INNER JOIN targetedms.GeneralMolecule gm ON gp.GeneralMoleculeId = gm.Id
    INNER JOIN targetedms.PeptideGroup pg ON gm.PeptideGroupId = pg.Id

    WHERE gp.IsotopeLabelId IS NULL;

UPDATE targetedms.GeneralPrecursor SET IsotopeLabelId = (SELECT il.Id FROM
  targetedms.IsotopeLabel il
    INNER JOIN targetedms.PeptideGroup pg ON pg.RunId = il.RunId AND il.Name = 'Unknown' AND il.Standard = TRUE
    INNER JOIN targetedms.GeneralMolecule gm ON gm.PeptideGroupId = pg.Id AND gm.Id = GeneralPrecursor.GeneralMoleculeId
)
WHERE IsotopeLabelId IS NULL;

ALTER TABLE targetedms.GeneralPrecursor ALTER COLUMN IsotopeLabelId SET NOT NULL;

ALTER TABLE targetedms.AuditLogMessage ALTER COLUMN messageType TYPE VARCHAR(100);

-- Add the columns we need to populate
ALTER TABLE targetedms.GeneralMoleculeChromInfo ADD COLUMN ModifiedAreaProportion REAL;
ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN PrecursorModifiedAreaProportion REAL;

-- Create temp tables for perf
CREATE TABLE targetedms.PrecursorGroupings (Grouping VARCHAR(300), PrecursorId INT);
CREATE TABLE targetedms.MoleculeGroupings (Grouping VARCHAR(300), GeneralMoleculeId INT);
CREATE TABLE targetedms.areas (Grouping VARCHAR(300), SampleFileId INT, Area REAL);

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

-- Analyze so that Postgres can choose a good plan
ANALYZE targetedms.PrecursorGroupings;
ANALYZE targetedms.MoleculeGroupings;
ANALYZE targetedms.areas;

-- Populate with the real values
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

ALTER TABLE targetedms.IsotopeModification ALTER COLUMN AminoAcid TYPE VARCHAR(30);

ALTER TABLE targetedms.Molecule ALTER COLUMN MoleculeId TYPE VARCHAR(500);
ALTER TABLE targetedms.MoleculePrecursor ALTER COLUMN MoleculePrecursorId TYPE VARCHAR(500);
ALTER TABLE targetedms.MoleculeTransition ALTER COLUMN MoleculeTransitionId TYPE VARCHAR(500);

ALTER TABLE targetedms.spectrumlibrary DROP COLUMN librarysourceid;
DROP TABLE targetedms.librarysource;

ALTER TABLE targetedms.moleculeprecursor DROP COLUMN moleculeprecursorid;

ALTER TABLE targetedms.LibrarySettings ADD COLUMN ionMatchTolerance DOUBLE PRECISION;

-- Issue 38773: Remove PanoramaPublic tables from the targetedms schema
DROP TABLE IF EXISTS targetedms.JournalExperiment;
DROP TABLE IF EXISTS targetedms.Journal;
DROP TABLE IF EXISTS targetedms.ExperimentAnnotations;

-- Issue 40487: Add a Modified column to targetedms.GeneralPrecursor
ALTER TABLE targetedms.GeneralPrecursor ADD COLUMN Modified TIMESTAMP;

-- The value in targetedms.runs.Modified should be a good substitute so we will use this value to populate the new column
UPDATE targetedms.GeneralPrecursor gp SET Modified = r.Modified
FROM targetedms.runs r
INNER JOIN targetedms.PeptideGroup pg ON pg.RunId = r.id
INNER JOIN targetedms.GeneralMolecule gm ON gm.PeptideGroupId = pg.Id
WHERE gm.Id = gp.GeneralMoleculeId;

ALTER TABLE targetedms.transition add column complexFragmentIon TEXT;

DROP SCHEMA IF EXISTS passport CASCADE;

CREATE TABLE targetedms.keywordcategories (
    id serial NOT NULL,
    categoryid character varying(10),
    label character varying(100) NOT NULL,
    CONSTRAINT pk_keywordcategories PRIMARY KEY (id),
    CONSTRAINT keywordcategories_categoryid_key UNIQUE (categoryid)
);

CREATE TABLE targetedms.keywords (
    id serial NOT NULL,
    keywordid character varying(10) NOT NULL,
    keyword character varying(100) NOT NULL,
    category character varying(10) NOT NULL,
    CONSTRAINT pk_keywords PRIMARY KEY (id),
    CONSTRAINT fk_keyword_category FOREIGN KEY (category)
       REFERENCES targetedms.keywordcategories (categoryid) MATCH SIMPLE
       ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT keywords_keywordid_key UNIQUE (keywordid)
);


INSERT INTO targetedms.keywordcategories VALUES (1, 'KW-9999', 'Biological process');
INSERT INTO targetedms.keywordcategories VALUES (2, 'KW-9998', 'Cellular component');
INSERT INTO targetedms.keywordcategories VALUES (3, 'KW-9997', 'Coding sequence diversity');
INSERT INTO targetedms.keywordcategories VALUES (4, 'KW-9996', 'Developmental stage');
INSERT INTO targetedms.keywordcategories VALUES (5, 'KW-9995', 'Disease');
INSERT INTO targetedms.keywordcategories VALUES (6, 'KW-9994', 'Domain');
INSERT INTO targetedms.keywordcategories VALUES (7, 'KW-9993', 'Ligand');
INSERT INTO targetedms.keywordcategories VALUES (8, 'KW-9992', 'Molecular function');
INSERT INTO targetedms.keywordcategories VALUES (9, 'KW-9991', 'PTM');
INSERT INTO targetedms.keywordcategories VALUES (10, 'KW-9990', 'Technical term');




INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0001', '2Fe-2S', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0002', '3D-structure', 'KW-9990');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0003', '3Fe-4S', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0004', '4Fe-4S', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0005', 'Acetoin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0006', 'Acetoin catabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0007', 'Acetylation', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0008', 'Acetylcholine receptor inhibiting toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0009', 'Actin-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0010', 'Activator', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0011', 'Acute phase', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0012', 'Acyltransferase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0013', 'ADP-ribosylation', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0014', 'AIDS', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0015', 'Albinism', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0016', 'Alginate biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0017', 'Alkaloid metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0019', 'Alkylphosphonate uptake', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0020', 'Allergen', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0021', 'Allosteric enzyme', 'KW-9990');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0022', 'Alpha-amylase inhibitor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0023', 'Alport syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0024', 'Alternative initiation', 'KW-9997');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0025', 'Alternative splicing', 'KW-9997');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0026', 'Alzheimer disease', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0027', 'Amidation', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0028', 'Amino-acid biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0029', 'Amino-acid transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0030', 'Aminoacyl-tRNA synthetase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0031', 'Aminopeptidase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0032', 'Aminotransferase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0034', 'Amyloid', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0035', 'Amyloplast', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0036', 'Amyotrophic lateral sclerosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0037', 'Angiogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0038', 'Ectodermal dysplasia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0039', 'Anion exchange', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0040', 'ANK repeat', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0041', 'Annexin', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0042', 'Antenna complex', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0043', 'Tumor suppressor', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0044', 'Antibiotic', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0045', 'Antibiotic biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0046', 'Antibiotic resistance', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0047', 'Antifreeze protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0049', 'Antioxidant', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0050', 'Antiport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0051', 'Antiviral defense', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0052', 'Apoplast', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0053', 'Apoptosis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0054', 'Arabinose catabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0055', 'Arginine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0056', 'Arginine metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0057', 'Aromatic amino acid biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0058', 'Aromatic hydrocarbons catabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0059', 'Arsenical resistance', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0060', 'Ascorbate biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0061', 'Asparagine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0062', 'Aspartic protease inhibitor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0063', 'Aspartyl esterase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0064', 'Aspartyl protease', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0065', 'Atherosclerosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0066', 'ATP synthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0067', 'ATP-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0068', 'Autocatalytic cleavage', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0069', 'Autoimmune encephalomyelitis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0070', 'Autoimmune uveitis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0071', 'Autoinducer synthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0072', 'Autophagy', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0073', 'Auxin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0075', 'B-cell activation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0076', 'Bacteriochlorophyll', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0077', 'Bacteriochlorophyll biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0078', 'Bacteriocin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0079', 'Bacteriocin immunity', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0080', 'Bacteriocin transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0081', 'Bacteriolytic enzyme', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0082', 'Bait region', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0083', 'Bardet-Biedl syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0084', 'Basement membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0085', 'Behavior', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0086', 'Bence-Jones protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0087', 'Bernard Soulier syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0088', 'Bile acid catabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0089', 'Bile pigment', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0090', 'Biological rhythms', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0091', 'Biomineralization', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0092', 'Biotin', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0093', 'Biotin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0094', 'Blood coagulation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0095', 'Blood group antigen', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0100', 'Branched-chain amino acid biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0101', 'Branched-chain amino acid catabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0102', 'Bromination', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0103', 'Bromodomain', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0104', 'Cadmium', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0105', 'Cadmium resistance', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0106', 'Calcium', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0107', 'Calcium channel', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0108', 'Calcium channel impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0109', 'Calcium transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0111', 'Calcium/phospholipid-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0112', 'Calmodulin-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0113', 'Calvin cycle', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0114', 'cAMP', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0115', 'cAMP biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0116', 'cAMP-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0117', 'Actin capping', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0118', 'Viral capsid assembly', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0119', 'Carbohydrate metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0120', 'Carbon dioxide fixation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0121', 'Carboxypeptidase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0122', 'Cardiomyopathy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0123', 'Cardiotoxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0124', 'Carnitine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0125', 'Carotenoid biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0127', 'Catecholamine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0128', 'Catecholamine metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0129', 'CBS domain', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0130', 'Cell adhesion', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0131', 'Cell cycle', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0132', 'Cell division', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0133', 'Cell shape', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0134', 'Cell wall', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0135', 'Cellulose biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0136', 'Cellulose degradation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0137', 'Centromere', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0138', 'CF(0)', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0139', 'CF(1)', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0140', 'cGMP', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0141', 'cGMP biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0142', 'cGMP-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0143', 'Chaperone', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0144', 'Charcot-Marie-Tooth disease', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0145', 'Chemotaxis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0146', 'Chitin degradation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0147', 'Chitin-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0148', 'Chlorophyll', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0149', 'Chlorophyll biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0150', 'Chloroplast', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0151', 'Chlorosome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0152', 'Cholesterol biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0153', 'Cholesterol metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0155', 'Chromate resistance', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0156', 'Chromatin regulator', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0157', 'Chromophore', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0158', 'Chromosome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0159', 'Chromosome partition', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0160', 'Chromosomal rearrangement', 'KW-9997');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0161', 'Chronic granulomatous disease', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0162', 'Chylomicron', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0163', 'Citrate utilization', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0164', 'Citrullination', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0165', 'Cleavage on pair of basic residues', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0166', 'Nematocyst', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0167', 'Capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0168', 'Coated pit', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0169', 'Cobalamin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0170', 'Cobalt', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0171', 'Cobalt transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0172', 'Cockayne syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0173', 'Coenzyme A biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0174', 'Coenzyme M biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0175', 'Coiled coil', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0176', 'Collagen', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0177', 'Collagen degradation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0178', 'Competence', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0179', 'Complement alternate pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0180', 'Complement pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0181', 'Complete proteome', 'KW-9990');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0182', 'Cone-rod dystrophy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0183', 'Conidiation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0184', 'Conjugation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0186', 'Copper', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0187', 'Copper transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0188', 'Copulatory plug', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0190', 'Covalent protein-DNA linkage', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0191', 'Covalent protein-RNA linkage', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0192', 'Crown gall tumor', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0193', 'Cuticle', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0194', 'Cyanelle', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0195', 'Cyclin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0196', 'Cycloheximide resistance', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0197', 'Cyclosporin', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0198', 'Cysteine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0199', 'Cystinuria', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0200', 'Cytadherence', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0201', 'Cytochrome c-type biogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0202', 'Cytokine', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0203', 'Cytokinin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0204', 'Cytolysis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0205', 'Cytosine metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0206', 'Cytoskeleton', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0208', 'D-amino acid', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0209', 'Deafness', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0210', 'Decarboxylase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0211', 'Defensin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0213', 'Dejerine-Sottas syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0214', 'Dental caries', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0215', 'Deoxyribonucleotide synthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0216', 'Detoxification', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0217', 'Developmental protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0218', 'Diabetes insipidus', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0219', 'Diabetes mellitus', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0220', 'Diaminopimelate biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0221', 'Differentiation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0222', 'Digestion', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0223', 'Dioxygenase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0224', 'Dipeptidase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0225', 'Disease mutation', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0226', 'DNA condensation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0227', 'DNA damage', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0228', 'DNA excision', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0229', 'DNA integration', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0230', 'DNA invertase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0231', 'Viral genome packaging', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0233', 'DNA recombination', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0234', 'DNA repair', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0235', 'DNA replication', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0236', 'DNA replication inhibitor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0237', 'DNA synthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0238', 'DNA-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0239', 'DNA-directed DNA polymerase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0240', 'DNA-directed RNA polymerase', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0241', 'Down syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0242', 'Dwarfism', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0243', 'Dynein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0244', 'Early protein', 'KW-9996');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0245', 'EGF-like domain', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0248', 'Ehlers-Danlos syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0249', 'Electron transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0250', 'Elliptocytosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0251', 'Elongation factor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0254', 'Endocytosis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0255', 'Endonuclease', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0256', 'Endoplasmic reticulum', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0257', 'Endorphin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0259', 'Enterobactin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0260', 'Enterotoxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0261', 'Viral envelope protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0263', 'Epidermolysis bullosa', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0265', 'Erythrocyte maturation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0266', 'Ethylene biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0267', 'Excision nuclease', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0268', 'Exocytosis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0269', 'Exonuclease', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0270', 'Exopolysaccharide synthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0271', 'Exosome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0272', 'Extracellular matrix', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0273', 'Eye lens protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0274', 'FAD', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0275', 'Fatty acid biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0276', 'Fatty acid metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0278', 'Fertilization', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0280', 'Fibrinolysis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0281', 'Fimbrium', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0282', 'Flagellum', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0283', 'Flagellar rotation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0284', 'Flavonoid biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0285', 'Flavoprotein', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0286', 'Flight', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0287', 'Flowering', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0288', 'FMN', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0289', 'Folate biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0290', 'Folate-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0291', 'Formylation', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0292', 'Fruit ripening', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0293', 'Fruiting body', 'KW-9996');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0294', 'Fucose metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0295', 'Fungicide', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0297', 'G-protein coupled receptor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0298', 'Galactitol metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0299', 'Galactose metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0301', 'Gamma-carboxyglutamic acid', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0302', 'Gap protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0303', 'Gap junction', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0304', 'Gas vesicle', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0305', 'Gaseous exchange', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0306', 'Gastrulation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0307', 'Gaucher disease', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0308', 'Genetically modified food', 'KW-9990');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0309', 'Germination', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0311', 'Gluconate utilization', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0312', 'Gluconeogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0313', 'Glucose metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0314', 'Glutamate biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0315', 'Glutamine amidotransferase', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0316', 'Glutaricaciduria', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0317', 'Glutathione biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0318', 'Glutathionylation', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0319', 'Glycerol metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0320', 'Glycogen biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0321', 'Glycogen metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0322', 'Glycogen storage disease', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0323', 'Glycolate pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0324', 'Glycolysis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0325', 'Glycoprotein', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0326', 'Glycosidase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0327', 'Glycosome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0328', 'Glycosyltransferase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0329', 'Glyoxylate bypass', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0330', 'Glyoxysome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0331', 'Gangliosidosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0332', 'GMP biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0333', 'Golgi apparatus', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0334', 'Gonadal differentiation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0335', 'Gout', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0336', 'GPI-anchor', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0337', 'GPI-anchor biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0338', 'Growth arrest', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0339', 'Growth factor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0340', 'Growth factor binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0341', 'Growth regulation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0342', 'GTP-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0343', 'GTPase activation', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0344', 'Guanine-nucleotide releasing factor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0345', 'HDL', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0346', 'Stress response', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0347', 'Helicase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0348', 'Hemagglutinin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0349', 'Heme', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0350', 'Heme biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0351', 'Hemoglobin-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0353', 'Hemolymph clotting', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0354', 'Hemolysis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0355', 'Hemophilia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0356', 'Hemostasis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0357', 'Heparan sulfate', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0358', 'Heparin-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0359', 'Herbicide resistance', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0360', 'Hereditary hemolytic anemia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0361', 'Hereditary multiple exostoses', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0362', 'Hereditary nonpolyposis colorectal cancer', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0363', 'Hermansky-Pudlak syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0364', 'Heterocyst', 'KW-9996');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0367', 'Hirschsprung disease', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0368', 'Histidine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0369', 'Histidine metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0370', 'Holoprosencephaly', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0371', 'Homeobox', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0372', 'Hormone', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0373', 'Hyaluronic acid', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0374', 'Hybridoma', 'KW-9990');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0375', 'Hydrogen ion transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0376', 'Hydrogen peroxide', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0377', 'Hydrogenosome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0378', 'Hydrolase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0379', 'Hydroxylation', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0380', 'Hyperlipidemia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0381', 'Hypersensitive response', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0382', 'Hypotensive agent', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0385', 'Hypusine', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0386', 'Hypusine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0387', 'Ice nucleation', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0388', 'IgA-binding protein', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0389', 'IgE-binding protein', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0390', 'IgG-binding protein', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0391', 'Immunity', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0392', 'Immunoglobulin C region', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0393', 'Immunoglobulin domain', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0394', 'Immunoglobulin V region', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0395', 'Inflammatory response', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0396', 'Initiation factor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0398', 'Inositol biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0399', 'Innate immunity', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0401', 'Integrin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0403', 'Intermediate filament', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0404', 'Intron homing', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0405', 'Iodination', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0406', 'Ion transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0407', 'Ion channel', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0408', 'Iron', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0409', 'Iron storage', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0410', 'Iron transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0411', 'Iron-sulfur', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0412', 'Isoleucine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0413', 'Isomerase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0414', 'Isoprene biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0415', 'Karyogamy', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0416', 'Keratin', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0417', 'Keratinization', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0418', 'Kinase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0419', 'Kinetoplast', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0420', 'Kringle', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0421', 'Lactation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0422', 'Lactose biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0423', 'Lactose metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0424', 'Laminin EGF-like domain', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0425', 'Lantibiotic', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0426', 'Late protein', 'KW-9996');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0427', 'LDL', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0428', 'Leader peptide', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0429', 'Leber hereditary optic neuropathy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0430', 'Lectin', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0431', 'Leigh syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0432', 'Leucine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0433', 'Leucine-rich repeat', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0434', 'Leukotriene biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0435', 'Li-Fraumeni syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0436', 'Ligase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0437', 'Light-harvesting polypeptide', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0438', 'Lignin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0439', 'Lignin degradation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0440', 'LIM domain', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0441', 'Lipid A biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0442', 'Lipid degradation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0443', 'Lipid metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0444', 'Lipid biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0445', 'Lipid transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0446', 'Lipid-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0448', 'Lipopolysaccharide biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0449', 'Lipoprotein', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0450', 'Lipoyl', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0451', 'Lissencephaly', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0452', 'Lithium', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0454', 'Long QT syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0455', 'Luminescence', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0456', 'Lyase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0457', 'Lysine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0458', 'Lysosome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0460', 'Magnesium', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0461', 'Malaria', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0462', 'Maltose metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0463', 'Mandelate pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0464', 'Manganese', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0465', 'Mannose-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0466', 'Maple syrup urine disease', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0467', 'Mast cell degranulation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0468', 'Viral matrix protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0469', 'Meiosis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0470', 'Melanin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0471', 'Melatonin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0472', 'Membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0473', 'Membrane attack complex', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0474', 'Menaquinone biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0475', 'Mercuric resistance', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0476', 'Mercury', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0477', 'Merozoite', 'KW-9996');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0478', 'Metachromatic leukodystrophy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0479', 'Metal-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0480', 'Metal-thiolate cluster', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0481', 'Metalloenzyme inhibitor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0482', 'Metalloprotease', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0483', 'Metalloprotease inhibitor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0484', 'Methanogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0485', 'Methanol utilization', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0486', 'Methionine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0487', 'Methotrexate resistance', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0488', 'Methylation', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0489', 'Methyltransferase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0490', 'MHC I', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0491', 'MHC II', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0492', 'Microsome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0493', 'Microtubule', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0494', 'Milk protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0495', 'Mineral balance', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0496', 'Mitochondrion', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0497', 'Mitogen', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0498', 'Mitosis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0499', 'Mobility protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0500', 'Molybdenum', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0501', 'Molybdenum cofactor biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0502', 'Monoclonal antibody', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0503', 'Monooxygenase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0504', 'Morphogen', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0505', 'Motor protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0506', 'mRNA capping', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0507', 'mRNA processing', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0508', 'mRNA splicing', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0509', 'mRNA transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0510', 'Mucopolysaccharidosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0511', 'Multifunctional enzyme', 'KW-9990');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0514', 'Muscle protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0515', 'Mutator protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0517', 'Myogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0518', 'Myosin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0519', 'Myristate', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0520', 'NAD', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0521', 'NADP', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0523', 'Neurodegeneration', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0524', 'Neurogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0525', 'Neuronal ceroid lipofuscinosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0527', 'Neuropeptide', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0528', 'Neurotoxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0529', 'Neurotransmitter', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0530', 'Neurotransmitter biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0531', 'Neurotransmitter degradation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0532', 'Neurotransmitter transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0533', 'Nickel', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0534', 'Nitrate assimilation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0535', 'Nitrogen fixation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0536', 'Nodulation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0539', 'Nucleus', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0540', 'Nuclease', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0542', 'Nucleomorph', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0543', 'Viral nucleoprotein', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0544', 'Nucleosome core', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0545', 'Nucleotide biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0546', 'Nucleotide metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0547', 'Nucleotide-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0548', 'Nucleotidyltransferase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0549', 'Nylon degradation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0550', 'Obesity', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0551', 'Lipid droplet', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0552', 'Olfaction', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0553', 'Oncogene', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0554', 'One-carbon metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0555', 'Opioid peptide', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0556', 'Organic radical', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0558', 'Oxidation', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0560', 'Oxidoreductase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0561', 'Oxygen transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0562', 'Pair-rule protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0563', 'Paired box', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0564', 'Palmitate', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0566', 'Pantothenate biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0568', 'Pathogenesis-related protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0570', 'Pentose shunt', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0571', 'Peptide transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0572', 'Peptidoglycan-anchor', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0573', 'Peptidoglycan synthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0574', 'Periplasm', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0575', 'Peroxidase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0576', 'Peroxisome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0577', 'PHA biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0578', 'Host cell lysis by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0581', 'Phagocytosis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0582', 'Pharmaceutical', 'KW-9990');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0583', 'PHB biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0584', 'Phenylalanine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0585', 'Phenylalanine catabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0586', 'Phenylketonuria', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0587', 'Phenylpropanoid metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0588', 'Pheromone', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0589', 'Pheromone response', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0590', 'Pheromone-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0592', 'Phosphate transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0593', 'Phospholipase A2 inhibitor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0594', 'Phospholipid biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0595', 'Phospholipid degradation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0596', 'Phosphopantetheine', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0597', 'Phosphoprotein', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0598', 'Phosphotransferase system', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0599', 'Photoprotein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0600', 'Photoreceptor protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0601', 'Photorespiration', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0602', 'Photosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0603', 'Photosystem I', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0604', 'Photosystem II', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0605', 'Phycobilisome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0607', 'Phytochrome signaling pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0608', 'Pigment', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0611', 'Plant defense', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0614', 'Plasmid', 'KW-9990');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0615', 'Plasmid copy control', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0616', 'Plasmid partition', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0617', 'Plasminogen activation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0618', 'Plastoquinone', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0620', 'Polyamine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0621', 'Polymorphism', 'KW-9997');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0622', 'Neuropathy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0624', 'Polysaccharide degradation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0625', 'Polysaccharide transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0626', 'Porin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0627', 'Porphyrin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0628', 'Postsynaptic cell membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0629', 'Postsynaptic neurotoxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0630', 'Potassium', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0631', 'Potassium channel', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0632', 'Potassium channel impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0633', 'Potassium transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0634', 'PQQ', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0635', 'Pregnancy', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0636', 'Prenylation', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0637', 'Prenyltransferase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0638', 'Presynaptic neurotoxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0639', 'Primosome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0640', 'Prion', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0641', 'Proline biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0642', 'Proline metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0643', 'Prostaglandin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0644', 'Prostaglandin metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0645', 'Protease', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0646', 'Protease inhibitor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0647', 'Proteasome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0648', 'Protein biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0649', 'Protein kinase inhibitor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0650', 'Protein phosphatase inhibitor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0651', 'Protein splicing', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0652', 'Protein synthesis inhibitor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0653', 'Protein transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0654', 'Proteoglycan', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0655', 'Prothrombin activator', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0656', 'Proto-oncogene', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0657', 'Pseudohermaphroditism', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0658', 'Purine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0659', 'Purine metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0660', 'Purine salvage', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0661', 'Putrescine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0662', 'Pyridine nucleotide biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0663', 'Pyridoxal phosphate', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0664', 'Pyridoxine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0665', 'Pyrimidine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0666', 'Pyrogen', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0668', 'Pyropoikilocytosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0669', 'Pyrrolysine', 'KW-9997');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0670', 'Pyruvate', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0671', 'Queuosine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0672', 'Quinate metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0673', 'Quorum sensing', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0674', 'Reaction center', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0675', 'Receptor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0676', 'Redox-active center', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0677', 'Repeat', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0678', 'Repressor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0679', 'Respiratory chain', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0680', 'Restriction system', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0681', 'Retinal protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0682', 'Retinitis pigmentosa', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0683', 'Retinol-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0684', 'Rhamnose metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0685', 'Rhizomelic chondrodysplasia punctata', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0686', 'Riboflavin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0687', 'Ribonucleoprotein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0688', 'Ribosomal frameshifting', 'KW-9997');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0689', 'Ribosomal protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0690', 'Ribosome biogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0691', 'RNA editing', 'KW-9997');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0692', 'RNA repair', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0693', 'Viral RNA replication', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0694', 'RNA-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0695', 'RNA-directed DNA polymerase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0696', 'RNA-directed RNA polymerase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0697', 'Rotamase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0698', 'rRNA processing', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0699', 'rRNA-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0701', 'S-layer', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0702', 'S-nitrosylation', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0703', 'Sarcoplasmic reticulum', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0704', 'Schiff base', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0705', 'SCID', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0708', 'Seed storage protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0709', 'Segmentation polarity protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0711', 'Selenium', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0712', 'Selenocysteine', 'KW-9997');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0713', 'Self-incompatibility', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0716', 'Sensory transduction', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0717', 'Septation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0718', 'Serine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0719', 'Serine esterase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0720', 'Serine protease', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0721', 'Serine protease homolog', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0722', 'Serine protease inhibitor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0723', 'Serine/threonine-protein kinase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0724', 'Serotonin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0726', 'Sexual differentiation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0727', 'SH2 domain', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0728', 'SH3 domain', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0729', 'SH3-binding', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0730', 'Sialic acid', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0731', 'Sigma factor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0732', 'Signal', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0733', 'Signal recognition particle', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0734', 'Signal transduction inhibitor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0735', 'Signal-anchor', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0736', 'Signalosome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0737', 'Silk protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0738', 'Voltage-gated sodium channel impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0739', 'Sodium transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0740', 'Sodium/potassium transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0741', 'SOS mutagenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0742', 'SOS response', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0744', 'Spermatogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0745', 'Spermidine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0746', 'Sphingolipid metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0747', 'Spliceosome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0748', 'Sporozoite', 'KW-9996');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0749', 'Sporulation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0750', 'Starch biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0751', 'Stargardt disease', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0752', 'Steroid biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0753', 'Steroid metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0754', 'Steroid-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0755', 'Steroidogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0756', 'Sterol biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0757', 'Stickler syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0758', 'Storage protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0759', 'Streptomycin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0762', 'Sugar transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0763', 'Sulfate respiration', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0764', 'Sulfate transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0765', 'Sulfation', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0766', 'Superantigen', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0767', 'Surface film', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0768', 'Sushi', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0769', 'Symport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0770', 'Synapse', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0771', 'Synaptosome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0772', 'Systemic lupus erythematosus', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0776', 'Taste-modifying protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0777', 'Teichoic acid biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0778', 'Tellurium resistance', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0779', 'Telomere', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0780', 'Terminal addition', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0783', 'Tetrahydrobiopterin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0784', 'Thiamine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0785', 'Thiamine catabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0786', 'Thiamine pyrophosphate', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0787', 'Thick filament', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0788', 'Thiol protease', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0789', 'Thiol protease inhibitor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0791', 'Threonine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0792', 'Thrombophilia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0793', 'Thylakoid', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0795', 'Thyroid hormone', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0796', 'Tight junction', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0797', 'Tissue remodeling', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0798', 'TonB box', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0799', 'Topoisomerase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0800', 'Toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0801', 'TPQ', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0802', 'TPR repeat', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0804', 'Transcription', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0805', 'Transcription regulation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0806', 'Transcription termination', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0807', 'Transducer', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0808', 'Transferase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0809', 'Transit peptide', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0810', 'Translation regulation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0811', 'Translocation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0812', 'Transmembrane', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0813', 'Transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0814', 'Transposable element', 'KW-9990');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0815', 'Transposition', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0816', 'Tricarboxylic acid cycle', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0817', 'Trimethoprim resistance', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0818', 'Triplet repeat expansion', 'KW-9997');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0819', 'tRNA processing', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0820', 'tRNA-binding', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0821', 'Trypanosomiasis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0822', 'Tryptophan biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0823', 'Tryptophan catabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0824', 'TTQ', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0825', 'Tumor antigen', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0826', 'Tungsten', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0827', 'Tyrosine biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0828', 'Tyrosine catabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0829', 'Tyrosine-protein kinase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0830', 'Ubiquinone', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0831', 'Ubiquinone biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0832', 'Ubl conjugation', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0833', 'Ubl conjugation pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0834', 'Unfolded protein response', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0835', 'Urea cycle', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0836', 'Usher syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0837', 'Vanadium', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0838', 'Vasoactive', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0839', 'Vasoconstrictor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0840', 'Vasodilator', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0842', 'Viral occlusion body', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0843', 'Virulence', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0844', 'Vision', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0845', 'Vitamin A', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0846', 'Cobalamin', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0847', 'Vitamin C', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0848', 'Vitamin D', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0850', 'VLDL', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0851', 'Voltage-gated channel', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0852', 'von Willebrand disease', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0853', 'WD repeat', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0855', 'Whooping cough', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0856', 'Williams-Beuren syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0857', 'Xeroderma pigmentosum', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0858', 'Xylan degradation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0859', 'Xylose metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0861', 'Zellweger syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0862', 'Zinc', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0863', 'Zinc-finger', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0864', 'Zinc transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0865', 'Zymogen', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0866', 'Nonsense-mediated mRNA decay', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0867', 'MELAS syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0868', 'Chloride', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0869', 'Chloride channel', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0870', 'Voltage-gated chloride channel impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0871', 'Bacteriocin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0872', 'Ion channel impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0873', 'Pyrrolidone carboxylic acid', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0874', 'Quinone', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0875', 'Capsule', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0876', 'Taxol biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0877', 'Alternative promoter usage', 'KW-9997');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0878', 'Amphibian defense peptide', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0879', 'Wnt signaling pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0880', 'Kelch repeat', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0881', 'Chlorophyll catabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0882', 'Thioester bond', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0883', 'Thioether bond', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0884', 'PQQ biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0885', 'CTQ', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0886', 'LTQ', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0887', 'Epilepsy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0888', 'Threonine protease', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0889', 'Transcription antitermination', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0890', 'Hereditary spastic paraplegia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0891', 'Chondrogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0892', 'Osteogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0893', 'Thyroid hormones biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0894', 'Sodium channel', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0895', 'ERV', 'KW-9990');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0896', 'Oogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0897', 'Waardenburg syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0898', 'Cataract', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0899', 'Viral immunoevasion', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0900', 'Congenital disorder of glycosylation', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0901', 'Leber congenital amaurosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0902', 'Two-component regulatory system', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0903', 'Direct protein sequencing', 'KW-9990');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0904', 'Protein phosphatase', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0905', 'Primary microcephaly', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0906', 'Nuclear pore complex', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0907', 'Parkinson disease', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0908', 'Parkinsonism', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0909', 'Hibernation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0910', 'Bartter syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0911', 'Desmin-related myopathy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0912', 'Congenital muscular dystrophy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0913', 'Age-related macular degeneration', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0914', 'Notch signaling pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0915', 'Sodium', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0916', 'Viral movement protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0917', 'Virion maturation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0918', 'Phosphonate transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0919', 'Taste', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0920', 'Virion tegument', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0921', 'Nickel transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0922', 'Interferon antiviral system evasion', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0923', 'Fanconi anemia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0924', 'Ammonia transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0925', 'Oxylipin biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0926', 'Vacuole', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0927', 'Auxin signaling pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0928', 'Hypersensitive response elicitation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0929', 'Antimicrobial', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0930', 'Antiviral protein', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0931', 'ER-Golgi transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0932', 'Cytokinin signaling pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0933', 'Apicoplast', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0934', 'Plastid', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0935', 'Progressive external ophthalmoplegia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0936', 'Ethylene signaling pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0937', 'Abscisic acid biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0938', 'Abscisic acid signaling pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0939', 'Gibberellin signaling pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0940', 'Short QT syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0941', 'Suppressor of RNA silencing', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0942', 'Mucolipidosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0943', 'RNA-mediated gene silencing', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0944', 'Nitration', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0945', 'Host-virus interaction', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0946', 'Virion', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0947', 'Limb-girdle muscular dystrophy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0948', 'Aicardi-Goutieres syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0949', 'S-adenosyl-L-methionine', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0950', 'Spinocerebellar ataxia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0951', 'Familial hemophagocytic lymphohistiocytosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0952', 'Extinct organism protein', 'KW-9990');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0953', 'Lacrimo-auriculo-dento-digital syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0954', 'Congenital adrenal hyperplasia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0955', 'Glaucoma', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0956', 'Kallmann syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0957', 'Chromoplast', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0958', 'Peroxisome biogenesis disorder', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0959', 'Myotoxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0960', 'Knottin', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0961', 'Cell wall biogenesis/degradation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0962', 'Peroxisome biogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0963', 'Cytoplasm', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0964', 'Secreted', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0965', 'Cell junction', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0966', 'Cell projection', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0967', 'Endosome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0968', 'Cytoplasmic vesicle', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0969', 'Cilium', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0970', 'Cilium biogenesis/degradation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0971', 'Glycation', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0972', 'Capsule biogenesis/degradation', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0973', 'c-di-GMP', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0974', 'Archaeal flagellum', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0975', 'Bacterial flagellum', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0976', 'Atrial septal defect', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0977', 'Ichthyosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0978', 'Insecticide resistance', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0979', 'Joubert syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0980', 'Senior-Loken syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0981', 'Meckel syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0982', 'Primary hypomagnesemia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0983', 'Nephronophthisis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0984', 'Congenital hypothyroidism', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0985', 'Congenital erythrocytosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0986', 'Amelogenesis imperfecta', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0987', 'Osteopetrosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0988', 'Intrahepatic cholestasis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0989', 'Craniosynostosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0990', 'Primary ciliary dyskinesia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0991', 'Mental retardation', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0992', 'Brugada syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0993', 'Aortic aneurysm', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0994', 'Organellar chromatophore', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0995', 'Kinetochore', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0996', 'Nickel insertion', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0997', 'Cell inner membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0998', 'Cell outer membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-0999', 'Mitochondrion inner membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1000', 'Mitochondrion outer membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1001', 'Plastid inner membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1002', 'Plastid outer membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1003', 'Cell membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1004', 'Congenital myasthenic syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1005', 'Bacterial flagellum biogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1006', 'Bacterial flagellum protein export', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1007', 'Palmoplantar keratoderma', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1008', 'Amyloidosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1009', 'Hearing', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1010', 'Non-syndromic deafness', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1011', 'Dyskeratosis congenita', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1012', 'Kartagener syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1013', 'Microphthalmia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1014', 'Congenital stationary night blindness', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1015', 'Disulfide bond', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1016', 'Hypogonadotropic hypogonadism', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1017', 'Isopeptide bond', 'KW-9991');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1018', 'Complement activation lectin pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1020', 'Atrial fibrillation', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1021', 'Pontocerebellar hypoplasia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1022', 'Congenital generalized lipodystrophy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1023', 'Dystonia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1024', 'Diamond-Blackfan anemia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1025', 'Mitosome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1026', 'Leukodystrophy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1027', 'Lead', 'KW-9993');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1028', 'Ionotropic glutamate receptor inhibitor', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1029', 'Fimbrium biogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1030', 'Host cell inner membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1031', 'Host cell junction', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1032', 'Host cell membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1033', 'Host cell outer membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1034', 'Host cell projection', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1035', 'Host cytoplasm', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1036', 'Host cytoplasmic vesicle', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1037', 'Host cytoskeleton', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1038', 'Host endoplasmic reticulum', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1039', 'Host endosome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1040', 'Host Golgi apparatus', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1041', 'Host lipid droplet', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1042', 'Host lysosome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1043', 'Host membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1044', 'Host microsome', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1045', 'Host mitochondrion', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1046', 'Host mitochondrion inner membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1047', 'Host mitochondrion outer membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1048', 'Host nucleus', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1049', 'Host periplasm', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1050', 'Host thylakoid', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1051', 'Host synapse', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1052', 'Target cell membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1053', 'Target membrane', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1054', 'Niemann-Pick disease', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1055', 'Congenital dyserythropoietic anemia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1056', 'Heterotaxy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1057', 'Nemaline myopathy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1058', 'Asthma', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1059', 'Peters anomaly', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1060', 'Myofibrillar myopathy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1061', 'Dermonecrotic toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1062', 'Cushing syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1063', 'Hypotrichosis', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1064', 'Adaptive immunity', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1065', 'Osteogenesis imperfecta', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1066', 'Premature ovarian failure', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1067', 'Emery-Dreifuss muscular dystrophy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1068', 'Hemolytic uremic syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1069', 'Brassinosteroid biosynthesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1070', 'Brassinosteroid signaling pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1071', 'Ligand-gated ion channel', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1072', 'Activation of host autophagy by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1073', 'Activation of host caspases by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1074', 'Activation of host NF-kappa-B by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1075', 'Inhibition of eukaryotic host translation factors by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1077', 'G0/G1 host cell cycle checkpoint dysregulation by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1078', 'G1/S host cell cycle checkpoint dysregulation by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1079', 'Host G2/M cell cycle arrest by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1080', 'Inhibition of host adaptive immune response by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1081', 'Inhibition of host apoptosis by viral BCL2-like protein', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1082', 'Inhibition of host apoptosis by viral FLIP-like protein', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1083', 'Inhibition of host autophagy by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1084', 'Inhibition of host tetherin by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1085', 'Inhibition of host caspases by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1086', 'Inhibition of host chemokines by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1087', 'Inhibition of host complement factors by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1088', 'Inhibition of host RIG-I by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1089', 'Inhibition of host MDA5 by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1090', 'Inhibition of host innate immune response by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1091', 'Inhibition of host interferon receptors by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1092', 'Inhibition of host IRF3 by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1093', 'Inhibition of host IRF7 by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1094', 'Inhibition of host IRF9 by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1095', 'Inhibition of host ISG15 by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1096', 'Inhibition of host JAK1 by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1097', 'Inhibition of host MAVS by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1098', 'Inhibition of host mitotic exit by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1099', 'Inhibition of host mRNA nuclear export by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1100', 'Inhibition of host NF-kappa-B by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1101', 'Inhibition of host poly(A)-binding protein by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1102', 'Inhibition of host PKR by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1103', 'Inhibition of host pre-mRNA processing by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1104', 'Inhibition of host RNA polymerase II by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1105', 'Inhibition of host STAT1 by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1106', 'Inhibition of host STAT2 by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1107', 'Inhibition of host TAP by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1108', 'Inhibition of host tapasin by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1110', 'Inhibition of host TRAFs by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1111', 'Inhibition of eukaryotic host transcription initiation by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1112', 'Inhibition of host TYK2 by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1113', 'Inhibition of host RLR pathway by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1114', 'Inhibition of host interferon signaling pathway by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1115', 'Inhibition of host MHC class I molecule presentation by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1116', 'Inhibition of host MHC class II molecule presentation by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1117', 'Inhibition of host proteasome antigen processing by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1118', 'Modulation of host dendritic cell activity by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1119', 'Modulation of host cell apoptosis by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1120', 'Modulation of host cell cycle by viral cyclin-like protein', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1121', 'Modulation of host cell cycle by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1122', 'Modulation of host chromatin by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1123', 'Modulation of host E3 ubiquitin ligases by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1124', 'Modulation of host immunity by viral IgG Fc receptor-like protein', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1125', 'Evasion of host immunity by viral interleukin-like protein', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1126', 'Modulation of host PP1 activity by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1127', 'Modulation of host ubiquitin pathway by viral deubiquitinase', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1128', 'Modulation of host ubiquitin pathway by viral E3 ligase', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1129', 'Modulation of host ubiquitin pathway by viral ubl', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1130', 'Modulation of host ubiquitin pathway by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1131', 'Modulation of host NK-cell activity by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1132', 'Decay of host mRNAs by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1133', 'Transmembrane helix', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1134', 'Transmembrane beta strand', 'KW-9994');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1135', 'Mitochondrion nucleoid', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1136', 'Bradyzoite', 'KW-9996');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1137', 'Tachyzoite', 'KW-9996');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1138', 'Trophozoite', 'KW-9996');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1139', 'Helical capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1140', 'T=1 icosahedral capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1141', 'T=2 icosahedral capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1142', 'T=3 icosahedral capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1143', 'T=pseudo3 icosahedral capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1144', 'T=4 icosahedral capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1145', 'T=7 icosahedral capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1146', 'T=13 icosahedral capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1147', 'T=16 icosahedral capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1148', 'T=25 icosahedral capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1149', 'T=147 icosahedral capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1150', 'T=169 icosahedral capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1151', 'T=219 icosahedral capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1152', 'Outer capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1153', 'Inner capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1154', 'Intermediate capsid protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1155', 'Translational shunt', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1156', 'RNA translational shunting', 'KW-9997');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1157', 'Cap snatching', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1158', 'RNA termination-reinitiation', 'KW-9997');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1159', 'RNA suppression of termination', 'KW-9997');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1160', 'Virus entry into host cell', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1161', 'Viral attachment to host cell', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1162', 'Viral penetration into host cytoplasm', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1163', 'Viral penetration into host nucleus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1164', 'Virus endocytosis by host', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1165', 'Clathrin-mediated endocytosis of virus by host', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1166', 'Caveolin-mediated endocytosis of virus by host', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1167', 'Clathrin- and caveolin-independent endocytosis of virus by host', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1168', 'Fusion of virus membrane with host membrane', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1169', 'Fusion of virus membrane with host cell membrane', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1170', 'Fusion of virus membrane with host endosomal membrane', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1171', 'Viral genome ejection through host cell envelope', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1172', 'Pore-mediated penetration of viral genome into host cell', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1173', 'Viral penetration via permeabilization of host membrane', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1174', 'Viral penetration via lysis of host organellar membrane', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1175', 'Viral attachment to host cell pilus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1176', 'Cytoplasmic inwards viral transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1177', 'Microtubular inwards viral transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1178', 'Actin-dependent inwards viral transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1179', 'Viral genome integration', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1180', 'Syncytium formation induced by viral infection', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1181', 'Viral primary envelope fusion with host outer nuclear membrane', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1182', 'Viral ion channel', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1183', 'Host cell receptor for virus entry', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1184', 'Jasmonic acid signaling pathway', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1185', 'Reference proteome', 'KW-9990');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1186', 'Ciliopathy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1187', 'Viral budding via the host ESCRT complexes', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1188', 'Virus exit from host cell', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1189', 'Microtubular outwards viral transport', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1190', 'Host gene expression shutoff by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1191', 'Eukaryotic host transcription shutoff by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1192', 'Host mRNA suppression by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1193', 'Eukaryotic host translation shutoff by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1194', 'Viral DNA replication', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1195', 'Viral transcription', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1196', 'IFIT mRNA restriction evasion by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1197', 'Ribosomal skipping', 'KW-9997');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1198', 'Viral budding', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1199', 'Hemostasis impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1200', 'Hemorrhagic toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1201', 'Platelet aggregation inhibiting toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1202', 'Platelet aggregation activating toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1203', 'Blood coagulation cascade inhibiting toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1204', 'Blood coagulation cascade activating toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1205', 'Fibrinolytic toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1206', 'Fibrinogenolytic toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1207', 'Sterol metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1208', 'Phospholipid metabolism', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1209', 'Archaeal flagellum biogenesis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1210', 'Necrosis', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1211', 'Schizophrenia', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1212', 'Corneal dystrophy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1213', 'G-protein coupled receptor impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1214', 'G-protein coupled acetylcholine receptor impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1215', 'Dystroglycanopathy', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1216', 'Complement system impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1217', 'Cell adhesion impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1218', 'Voltage-gated calcium channel impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1219', 'Ryanodine-sensitive calcium-release channel impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1220', 'Voltage-gated potassium channel impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1221', 'Calcium-activated potassium channel impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1222', 'Bradykinin receptor impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1223', 'Inhibition of host TBK1 by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1224', 'Inhibition of host IKBKE by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1225', 'Inhibition of host TLR pathway by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1226', 'Viral baseplate protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1227', 'Viral tail protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1228', 'Viral tail tube protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1229', 'Viral tail sheath protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1230', 'Viral tail fiber protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1231', 'Capsid inner membrane protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1232', 'Capsid decoration protein', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1233', 'Viral attachment to host adhesion receptor', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1234', 'Viral attachment to host entry receptor', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1235', 'Degradation of host cell envelope components during virus entry', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1236', 'Degradation of host peptidoglycans during virus entry', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1237', 'Degradation of host lipopolysaccharides during virus entry', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1238', 'Degradation of host capsule during virus entry', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1239', 'Fusion of virus membrane with host outer membrane', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1240', 'Viral attachment to host cell flagellum', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1241', 'Viral penetration into host cytoplasm via pilus retraction', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1242', 'Viral contractile tail ejection system', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1243', 'Viral long flexible tail ejection system', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1244', 'Viral short tail ejection system', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1245', 'Viral tail assembly', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1246', 'Viral tail fiber assembly', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1247', 'Degradation of host chromosome by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1248', 'Inhibition of host DNA replication by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1249', 'Viral extrusion', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1250', 'Viral genome excision', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1251', 'Viral latency', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1252', 'Latency-replication switch', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1253', 'Viral genome circularization', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1254', 'Modulation of host virulence by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1255', 'Viral exotoxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1256', 'DNA end degradation evasion by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1257', 'CRISPR-cas system evasion by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1258', 'Restriction-modification system evasion by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1259', 'Evasion of bacteria-mediated translation shutoff by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1260', 'Superinfection exclusion', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1261', 'Bacterial host gene expression shutoff by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1262', 'Eukaryotic host gene expression shutoff by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1263', 'Bacterial host transcription shutoff by virus', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1264', 'Viral receptor tropism switching', 'KW-9999');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1265', 'Chloride channel impairing toxin', 'KW-9992');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1266', 'Target cell cytoplasm', 'KW-9998');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1267', 'Proteomics identification', 'KW-9990');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1268', 'Autism spectrum disorder', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1269', 'Autism', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1270', 'Asperger syndrome', 'KW-9995');
INSERT INTO targetedms.keywords VALUES ( DEFAULT, 'KW-1271', 'Inflammasome', 'KW-9998');

ALTER TABLE targetedms.Runs ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.AnnotationSettings ALTER COLUMN RunId TYPE bigint;
ALTER TABLE targetedms.CalibrationCurve ALTER COLUMN RunId TYPE bigint;
ALTER TABLE targetedms.DriftTimePredictionSettings ALTER COLUMN RunId TYPE bigint;
ALTER TABLE targetedms.IsolationScheme ALTER COLUMN RunId TYPE bigint;
ALTER TABLE targetedms.FoldChange ALTER COLUMN RunId TYPE bigint;
ALTER TABLE targetedms.GroupComparisonSettings ALTER COLUMN RunId TYPE bigint;
ALTER TABLE targetedms.Instrument ALTER COLUMN RunId TYPE bigint;
ALTER TABLE targetedms.IsotopeEnrichment ALTER COLUMN RunId TYPE bigint;
ALTER TABLE targetedms.IsotopeLabel ALTER COLUMN RunId TYPE bigint;
ALTER TABLE targetedms.LibrarySettings ALTER COLUMN RunId TYPE bigint;
ALTER TABLE targetedms.ListDefinition ALTER COLUMN RunId TYPE bigint;
ALTER TABLE targetedms.ModificationSettings ALTER COLUMN RunId TYPE bigint;
ALTER TABLE targetedms.PeptideGroup ALTER COLUMN RunId TYPE bigint;
ALTER TABLE targetedms.Replicate ALTER COLUMN RunId TYPE bigint;
ALTER TABLE targetedms.RetentionTimePredictionSettings ALTER COLUMN RunId TYPE bigint;
ALTER TABLE targetedms.RunEnzyme ALTER COLUMN RunId TYPE bigint;
ALTER TABLE targetedms.RunIsotopeModification ALTER COLUMN RunId TYPE bigint ;
ALTER TABLE targetedms.RunStructuralModification ALTER COLUMN RunId TYPE bigint ;
ALTER TABLE targetedms.SpectrumLibrary ALTER COLUMN RunId TYPE bigint;
ALTER TABLE targetedms.TransitionFullScanSettings ALTER COLUMN RunId TYPE bigint ;
ALTER TABLE targetedms.TransitionInstrumentSettings ALTER COLUMN RunId TYPE bigint ;
ALTER TABLE targetedms.TransitionPredictionSettings ALTER COLUMN RunId TYPE bigint ;
ALTER TABLE targetedms.AuditLogEntry ALTER COLUMN versionId TYPE bigint;
ALTER TABLE targetedms.QuantificationSettings ALTER COLUMN RunId TYPE bigint;

ALTER TABLE targetedms.PeptideGroup ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.FoldChange ALTER COLUMN PeptideGroupId TYPE bigint;
ALTER TABLE targetedms.PeptideGroupAnnotation ALTER COLUMN PeptideGroupId TYPE bigint;
ALTER TABLE targetedms.PeptideGroupAnnotation ALTER COLUMN Id TYPE bigint;
ALTER TABLE targetedms.GeneralMolecule ALTER COLUMN PeptideGroupId TYPE bigint;
ALTER TABLE targetedms.Protein ALTER COLUMN PeptideGroupId TYPE bigint;

ALTER TABLE targetedms.GeneralMolecule ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.CalibrationCurve ALTER COLUMN GeneralMoleculeId TYPE bigint;
ALTER TABLE targetedms.FoldChange ALTER COLUMN GeneralMoleculeId TYPE bigint;
ALTER TABLE targetedms.GeneralMoleculeAnnotation ALTER COLUMN GeneralMoleculeId TYPE bigint;
ALTER TABLE targetedms.GeneralMoleculeChromInfo ALTER COLUMN GeneralMoleculeId TYPE bigint;
ALTER TABLE targetedms.GeneralPrecursor ALTER COLUMN GeneralMoleculeId TYPE bigint;
ALTER TABLE targetedms.Molecule ALTER COLUMN Id TYPE bigint;
ALTER TABLE targetedms.Peptide ALTER COLUMN Id TYPE bigint;
ALTER TABLE targetedms.PeptideIsotopeModification ALTER COLUMN PeptideId TYPE bigint;
ALTER TABLE targetedms.PeptideStructuralModification ALTER COLUMN PeptideId TYPE bigint;

------------------ PrecursorID ------------------
-------------------------------------------------
ALTER TABLE targetedms.Precursor ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.BibliospecLibInfo ALTER COLUMN PrecursorId TYPE bigint;
ALTER TABLE targetedms.ChromatogramLibInfo ALTER COLUMN PrecursorId TYPE bigint;
ALTER TABLE targetedms.HunterLibInfo ALTER COLUMN PrecursorId TYPE bigint;
ALTER TABLE targetedms.NistLibInfo ALTER COLUMN PrecursorId TYPE bigint;
ALTER TABLE targetedms.SpectrastLibInfo ALTER COLUMN PrecursorId TYPE bigint;

------------------ GeneralPrecursorID ---------------
-----------------------------------------------------
ALTER TABLE targetedms.GeneralPrecursor ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.GeneralTransition ALTER COLUMN GeneralPrecursorId TYPE bigint;
ALTER TABLE targetedms.PrecursorAnnotation ALTER COLUMN PrecursorId TYPE bigint;
ALTER TABLE targetedms.PrecursorChromInfo ALTER COLUMN PrecursorId TYPE bigint;
ALTER TABLE targetedms.MoleculePrecursor ALTER COLUMN Id TYPE bigint;

------------------ GeneralTransitionID ---------------
-----------------------------------------------------
ALTER TABLE targetedms.GeneralTransition ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.MoleculeTransition ALTER COLUMN TransitionId TYPE bigint;
ALTER TABLE targetedms.Transition ALTER COLUMN Id TYPE bigint;
ALTER TABLE targetedms.TransitionAnnotation ALTER COLUMN TransitionId TYPE bigint;
ALTER TABLE targetedms.TransitionChromInfo ALTER COLUMN TransitionId TYPE bigint;
ALTER TABLE targetedms.TransitionLoss ALTER COLUMN TransitionId TYPE bigint;
ALTER TABLE targetedms.TransitionOptimization ALTER COLUMN TransitionId TYPE bigint;

------------------ ReplicateID ------------------
--------------------------------------------------
ALTER TABLE targetedms.Replicate ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.ReplicateAnnotation ALTER COLUMN ReplicateId TYPE bigint;
ALTER TABLE targetedms.SampleFile ALTER COLUMN ReplicateId TYPE bigint;
ALTER TABLE targetedms.QCMetricExclusion ALTER COLUMN ReplicateId TYPE bigint;

------------------ SampleFileID ------------------
--------------------------------------------------
ALTER TABLE targetedms.SampleFile ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.TransitionChromInfo ALTER COLUMN SampleFileId TYPE bigint;
ALTER TABLE targetedms.GeneralMoleculeChromInfo ALTER COLUMN SampleFileId TYPE bigint;
ALTER TABLE targetedms.PrecursorChromInfo ALTER COLUMN SampleFileId TYPE bigint;
ALTER TABLE targetedms.SampleFileChromInfo ALTER COLUMN SampleFileId TYPE bigint;

------------------ SpectrumLibraryID ------------------
-------------------------------------------------------
ALTER TABLE targetedms.SpectrumLibrary ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.BibliospecLibInfo ALTER COLUMN SpectrumLibraryId TYPE bigint;
ALTER TABLE targetedms.ChromatogramLibInfo ALTER COLUMN SpectrumLibraryId TYPE bigint;
ALTER TABLE targetedms.HunterLibInfo ALTER COLUMN SpectrumLibraryId TYPE bigint;
ALTER TABLE targetedms.NistLibInfo ALTER COLUMN SpectrumLibraryId TYPE bigint;
ALTER TABLE targetedms.SpectrastLibInfo ALTER COLUMN SpectrumLibraryId TYPE bigint;

------------------ IsotopeModId -----------------------
-------------------------------------------------------
ALTER TABLE targetedms.IsotopeModification ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.PeptideIsotopeModification ALTER COLUMN IsotopeModId TYPE bigint;
ALTER TABLE targetedms.RunIsotopeModification ALTER COLUMN IsotopeModId TYPE bigint;

------------------ PrecursorChromInfoId ---------------------
-------------------------------------------------------------

ALTER TABLE targetedms.PrecursorChromInfo ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.PrecursorAreaRatio ALTER COLUMN PrecursorChromInfoId TYPE bigint;
ALTER TABLE targetedms.PrecursorAreaRatio ALTER COLUMN PrecursorChromInfoStdId TYPE bigint;
ALTER TABLE targetedms.PrecursorChromInfoAnnotation ALTER COLUMN PrecursorChromInfoId TYPE bigint;
ALTER TABLE targetedms.TransitionChromInfo ALTER COLUMN PrecursorChromInfoId TYPE bigint;

------------------ IsotopeLabelId ---------------------
-------------------------------------------------------
ALTER TABLE targetedms.IsotopeLabel ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.FoldChange ALTER COLUMN IsotopeLabelId TYPE bigint;
ALTER TABLE targetedms.GeneralPrecursor ALTER COLUMN IsotopeLabelId TYPE bigint;
ALTER TABLE targetedms.PeptideAreaRatio ALTER COLUMN IsotopeLabelId TYPE bigint;
ALTER TABLE targetedms.PeptideAreaRatio ALTER COLUMN IsotopeLabelStdId TYPE bigint;
ALTER TABLE targetedms.PrecursorAreaRatio ALTER COLUMN IsotopeLabelId TYPE bigint;
ALTER TABLE targetedms.PrecursorAreaRatio ALTER COLUMN IsotopeLabelStdId TYPE bigint;
ALTER TABLE targetedms.RunIsotopeModification ALTER COLUMN IsotopeLabelId TYPE bigint;
ALTER TABLE targetedms.TransitionAreaRatio ALTER COLUMN IsotopeLabelId TYPE bigint;
ALTER TABLE targetedms.TransitionAreaRatio ALTER COLUMN IsotopeLabelStdId TYPE bigint;

---------------- TransitionChromInfoId --------------------
-----------------------------------------------------------
ALTER TABLE targetedms.TransitionChromInfo ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.TransitionAreaRatio ALTER COLUMN TransitionChromInfoId TYPE bigint;
ALTER TABLE targetedms.TransitionAreaRatio ALTER COLUMN TransitionChromInfoStdId TYPE bigint;
ALTER TABLE targetedms.TransitionChromInfoAnnotation ALTER COLUMN TransitionChromInfoId TYPE bigint;

---------------- GeneralMoleculeChromInfoId --------------------
----------------------------------------------------------------
ALTER TABLE targetedms.GeneralMoleculeChromInfo ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.PrecursorChromInfo ALTER COLUMN GeneralMoleculeChromInfoId TYPE bigint;
ALTER TABLE targetedms.PeptideAreaRatio ALTER COLUMN PeptideChromInfoId TYPE bigint;
ALTER TABLE targetedms.PeptideAreaRatio ALTER COLUMN PeptideChromInfoStdId TYPE bigint;

---------------- StructuralModLossId --------------------
---------------------------------------------------------
ALTER TABLE targetedms.StructuralModLoss ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.TransitionLoss ALTER COLUMN StructuralModLossId TYPE bigint;

---------------- StructuralModId ------------------------------
----------------------------------------------------------------
ALTER TABLE targetedms.StructuralModification ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.PeptideStructuralModification ALTER COLUMN StructuralModId TYPE bigint;
ALTER TABLE targetedms.RunStructuralModification ALTER COLUMN StructuralModId TYPE bigint;
ALTER TABLE targetedms.StructuralModLoss ALTER COLUMN StructuralModId TYPE bigint;

------------------ QuantificationSettingsId ------------------------------
--------------------------------------------------------------------------
ALTER TABLE targetedms.QuantificationSettings ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.CalibrationCurve ALTER COLUMN QuantificationSettingsId TYPE bigint;

------------------ GroupComparisonSettingsId ------------------------------
---------------------------------------------------------------------------
ALTER TABLE targetedms.GroupComparisonSettings ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.FoldChange ALTER COLUMN GroupComparisonSettingsId TYPE bigint;

------------------ IsolationSchemeId ------------------------------
--------------------------------------------------------------------
ALTER TABLE targetedms.IsolationScheme ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.IsolationWindow ALTER COLUMN IsolationSchemeId TYPE bigint;

------------------ ListDefinitionId ------------------------------
------------------------------------------------------------------
ALTER TABLE targetedms.ListDefinition ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.ListColumnDefinition ALTER COLUMN ListDefinitionId TYPE bigint;
ALTER TABLE targetedms.ListItem ALTER COLUMN ListDefinitionId TYPE bigint;

------------------ ListItemId ------------------------------
------------------------------------------------------------
ALTER TABLE targetedms.ListItem ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.ListItemValue ALTER COLUMN ListItemId TYPE bigint;

------------------ DriftTimePredictionSettingsId ------------------------------
-------------------------------------------------------------------------------
ALTER TABLE targetedms.DriftTimePredictionSettings ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.MeasuredDriftTime ALTER COLUMN DriftTimePredictionSettingsId TYPE bigint;

------------------ PredictorId ------------------------------
-------------------------------------------------------------
ALTER TABLE targetedms.Predictor ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.Replicate ALTER COLUMN CePredictorId TYPE bigint;
ALTER TABLE targetedms.Replicate ALTER COLUMN DpPredictorId TYPE bigint;
ALTER TABLE targetedms.PredictorSettings ALTER COLUMN PredictorId TYPE bigint;
ALTER TABLE targetedms.transitionpredictionsettings ALTER COLUMN CePredictorId TYPE bigint;
ALTER TABLE targetedms.transitionpredictionsettings ALTER COLUMN DpPredictorId TYPE bigint;

ALTER TABLE targetedms.AnnotationSettings ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.IsolationWindow ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.ChromatogramLibInfo ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.GeneralMoleculeAnnotation ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.IsotopeEnrichment ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.Instrument ALTER COLUMN Id TYPE bigint;
ALTER TABLE targetedms.SampleFile ALTER COLUMN InstrumentId TYPE bigint;

ALTER TABLE targetedms.ListColumnDefinition ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.ListItem ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.ListItemValue ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.MeasuredDriftTime ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.BibliospecLibInfo ALTER COLUMN Id TYPE bigint;
ALTER TABLE targetedms.ChromatogramLibInfo ALTER COLUMN Id TYPE bigint;
ALTER TABLE targetedms.HunterLibInfo ALTER COLUMN Id TYPE bigint;
ALTER TABLE targetedms.NistLibInfo ALTER COLUMN Id TYPE bigint;
ALTER TABLE targetedms.SpectrastLibInfo ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.PeptideAreaRatio ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.PeptideGroupAnnotation ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.PeptideIsotopeModification ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.PeptideStructuralModification ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.PrecursorAnnotation ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.PrecursorAreaRatio ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.PrecursorChromInfoAnnotation ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.ReplicateAnnotation ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.SampleFileChromInfo ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.SpectrastLibInfo ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.TransitionAnnotation ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.TransitionAreaRatio ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.TransitionChromInfoAnnotation ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.TransitionLoss ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.TransitionOptimization ALTER COLUMN Id TYPE bigint;

--------------------------------------------------------------------------------------------------
------------------- Update sequences to bigint --------------------------------------------------
-- Issue 41317 : Alter sequence support in pg versions >= 10
CREATE FUNCTION targetedms.handleSequences() RETURNS VOID AS $$
DECLARE
BEGIN
    IF (
        SELECT CAST(current_setting('server_version_num') as INT) >= 100000
       )
    THEN
        EXECUTE
        'ALTER SEQUENCE targetedms.runs_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.predictor_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.peptide_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.instrument_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.replicate_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.samplefile_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.peptidegroup_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.isotopelabel_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.precursorchrominfo_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.transitionchrominfo_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.structuralmodification_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.structuralmodloss_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.isotopemodification_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.peptidestructuralmodification_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.peptideisotopemodification_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.transitionarearatio_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.precursorarearatio_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.peptidearearatio_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.spectrumlibrary_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.transitionoptimization_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.transitionloss_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.peptidegroupannotation_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.precursorannotation_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.precursorchrominfoannotation_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.transitionannotation_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.transitionchrominfoannotation_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.replicateannotation_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.annotationsettings_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.isolationscheme_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.isolationwindow_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.drifttimepredictionsettings_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.measureddrifttime_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.groupcomparisonsettings_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.quantificationsettings_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.listdefinition_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.listcolumndefinition_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.listitem_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.listitemvalue_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.samplefilechrominfo_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.nistlibinfo_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.spectrastlibinfo_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.chromatogramlibinfo_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.hunterlibinfo_id_seq as bigint;';
    END IF;
END
$$ LANGUAGE plpgsql;

SELECT targetedms.handleSequences();

DROP FUNCTION targetedms.handleSequences();

ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN TransitionChromatogramIndices BYTEA;

ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN BestMassErrorPPM Real;