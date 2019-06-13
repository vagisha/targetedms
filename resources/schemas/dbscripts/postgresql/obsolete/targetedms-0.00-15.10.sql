/*
 * Copyright (c) 2017-2019 LabKey Corporation
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
    Path VARCHAR(500),
    FileName VARCHAR(300),
    Status VARCHAR(200),
    StatusId INT NOT NULL DEFAULT 0,
    Deleted BOOLEAN NOT NULL DEFAULT '0',
    ExperimentRunLSID LSIDType NULL,

    PeptideGroupCount INT NOT NULL DEFAULT 0,
    PeptideCount INT NOT NULL DEFAULT 0,
    PrecursorCount INT NOT NULL DEFAULT 0,
    TransitionCount INT NOT NULL DEFAULT 0,

    CONSTRAINT PK_Runs PRIMARY KEY (Id)
);



-- ----------------------------------------------------------------------------
-- Transition Settings
-- ----------------------------------------------------------------------------
CREATE TABLE targetedms.Predictor
(
    Id SERIAL NOT NULL,
    Name VARCHAR(100),
    StepSize INT,
    StepCount INT,
    Charge INT,
    Slope REAL,
    Intercept REAL,

    CONSTRAINT PK_Predictor PRIMARY KEY (Id)
);

CREATE TABLE targetedms.TransitionPredictionSettings
(
    RunId INT NOT NULL,
    PrecursorMassType VARCHAR(5),
    ProductMassType VARCHAR(5),
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

    CONSTRAINT PK_TransitionFullScanSettings PRIMARY KEY (RunId),
    CONSTRAINT FK_TransitionFullScanSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);

CREATE TABLE targetedms.IsotopeEnrichment
(
    Id SERIAL NOT NULL,
    RunId INT NOT NULL,
    Symbol VARCHAR(10),
    PercentEnrichment REAL,

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
    CalculatorName VARCHAR(100),
    IsIrt BOOLEAN,
    RegressionSlope REAL,
    RegressionIntercept REAL,

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
    SkylineId VARCHAR(300) NOT NULL,
    AcquiredTime TIMESTAMP,
    ModifiedTime TIMESTAMP,
    InstrumentId INT,

    CONSTRAINT PK_SampleFile PRIMARY KEY (Id),
    CONSTRAINT FK_SampleFile_Replicate FOREIGN KEY (ReplicateId) REFERENCES targetedms.Replicate(Id),
    CONSTRAINT FK_SampleFile_Instrument FOREIGN KEY (InstrumentId) REFERENCES targetedms.Instrument(Id)
);

CREATE INDEX IX_SampleFile_ReplicateId ON targetedms.SampleFile(ReplicateId);



-- ----------------------------------------------------------------------------
-- Annotations
-- ----------------------------------------------------------------------------
CREATE TABLE targetedms.Annotation
(
    Id SERIAL NOT NULL,
    Name VARCHAR(255) NOT NULL,
    Value VARCHAR(255) NOT NULL,

    CONSTRAINT PK_Annotation PRIMARY KEY (Id)
);



-- ----------------------------------------------------------------------------
-- Peptide Group
-- ----------------------------------------------------------------------------
CREATE TABLE targetedms.PeptideGroup
(
    Id SERIAL NOT NULL,
    RunId INT NOT NULL,
    Label CHAR(10) NOT NULL,
    Description TEXT,
    SequenceId INTEGER,
    Decoy BOOLEAN,
    Note VARCHAR(255),

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

CREATE TABLE targetedms.PeptideGroupAnnotation
(
    AnnotationId INT NOT NULL,
    PeptideGroupId INT NOT NULL,

    CONSTRAINT PK_PeptideGroupAnnotation PRIMARY KEY (AnnotationId,PeptideGroupId),
    CONSTRAINT FK_PeptideGroupAnnotation_Annotation FOREIGN KEY (AnnotationId) REFERENCES targetedms.Annotation(Id),
    CONSTRAINT FK_PeptideGroupAnnotation_PeptideGroup FOREIGN KEY (PeptideGroupId) REFERENCES targetedms.PeptideGroup(Id)
);



-- ----------------------------------------------------------------------------
-- Peptide
-- ----------------------------------------------------------------------------
CREATE TABLE targetedms.Peptide
(
    Id SERIAL NOT NULL,
    PeptideGroupId INT NOT NULL,
    Sequence VARCHAR(100) NOT NULL,
    StartIndex INT,
    EndIndex INT,
    PreviousAa CHAR(1),
    NextAa CHAR(1),
    CalcNeutralMass REAL NOT NULL,
    NumMissedCleavages INT NOT NULL,
    Rank INTEGER,
    RtCalculatorScore REAL,
    PredictedRetentionTime REAL,
    AvgMeasuredRetentionTime REAL,
    Decoy BOOLEAN,
    Note VARCHAR(255),

    CONSTRAINT PK_Peptide PRIMARY KEY (Id),
    CONSTRAINT FK_Peptide_PeptideGroup FOREIGN KEY (PeptideGroupId) REFERENCES targetedms.PeptideGroup(Id)
);
CREATE INDEX IX_Peptide_Sequence ON targetedms.Peptide (Sequence);
CREATE INDEX IX_Peptide_PeptideGroupId ON targetedms.Peptide(PeptideGroupId);

CREATE TABLE targetedms.PeptideAnnotation
(
    AnnotationId INT NOT NULL,
    PeptideId INT NOT NULL,

    CONSTRAINT PK_PeptideAnnotation PRIMARY KEY (AnnotationId,PeptideId),
    CONSTRAINT FK_PeptideAnnotation_Annotation FOREIGN KEY (AnnotationId) REFERENCES targetedms.Annotation(Id),
    CONSTRAINT FK_PeptideAnnotation_Peptide FOREIGN KEY (PeptideId) REFERENCES targetedms.Peptide(Id)
);

CREATE TABLE targetedms.PeptideChromInfo
(
    Id SERIAL NOT NULL,
    PeptideId INT  NOT NULL,
    SampleFileId INT NOT NULL,
    PeakCountRatio REAL NOT NULL,
    RetentionTime REAL,
    PredictedRetentionTime REAL,
    RatioToStandard REAL,

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
    Mz REAL NOT NULL,
    Charge INT NOT NULL,
    NeutralMass REAL NOT NULL,
    ModifiedSequence VARCHAR(100) NOT NULL,
    CollisionEnergy REAL,
    DeclusteringPotential REAL,
    Decoy BOOLEAN,
    DecoyMassShift REAL,
    Note VARCHAR(255),

    CONSTRAINT PK_Precursor PRIMARY KEY (Id),
	CONSTRAINT FK_Precursor_Peptide FOREIGN KEY (PeptideId) REFERENCES targetedms.Peptide(Id)
);
CREATE INDEX IX_Precursor_PeptideId ON targetedms.Precursor(PeptideId);

CREATE TABLE targetedms.PrecursorAnnotation
(
    AnnotationId INT NOT NULL,
    PrecursorId INT NOT NULL,

    CONSTRAINT PK_PrecursorAnnotation PRIMARY KEY (AnnotationId,PrecursorId),
    CONSTRAINT FK_PrecursorAnnotation_Annotation FOREIGN KEY (AnnotationId) REFERENCES targetedms.Annotation(Id),
    CONSTRAINT FK_PrecursorAnnotation_Precursor FOREIGN KEY (PrecursorId) REFERENCES targetedms.Precursor(Id)
);

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
    Identified BOOLEAN,
    LibraryDtop REAL,
    OptimizationStep INT,
    UserSet BOOLEAN,
    NOTE VARCHAR(500),

    CONSTRAINT PK_PrecursorChromInfo PRIMARY KEY (Id),
    CONSTRAINT FK_PrecursorChromInfo_Precursor FOREIGN KEY (PrecursorId) REFERENCES targetedms.Precursor(Id),
    CONSTRAINT FK_PrecursorChromInfo_SampleFile FOREIGN KEY (SampleFileId) REFERENCES targetedms.SampleFile(Id),
    CONSTRAINT FK_PrecursorChromInfo_PeptideChromInfo FOREIGN KEY (PeptideChromInfoId) REFERENCES targetedms.PeptideChromInfo(Id)
);
CREATE INDEX IX_PrecursorChromInfo_PrecursorId ON targetedms.PrecursorChromInfo(PrecursorId);
CREATE INDEX IX_PrecursorChromInfo_SampleFileId ON targetedms.PrecursorChromInfo(SampleFileId);
CREATE INDEX IX_PrecursorChromInfo_PeptideChromInfoId ON targetedms.PrecursorChromInfo(PeptideChromInfoId);

CREATE TABLE targetedms.PrecursorChromInfoAnnotation
(
    AnnotationId INT NOT NULL,
    PrecursorChromInfoId INT NOT NULL,

    CONSTRAINT PK_PrecursorChromInfoAnnotation PRIMARY KEY (AnnotationId,PrecursorChromInfoId),
    CONSTRAINT FK_PrecursorChromInfoAnnotation_Annotation FOREIGN KEY (AnnotationId) REFERENCES targetedms.Annotation(Id),
    CONSTRAINT FK_PrecursorChromInfoAnnotation_Precursor FOREIGN KEY (PrecursorChromInfoId) REFERENCES targetedms.PrecursorChromInfo(Id)
);



-- ----------------------------------------------------------------------------
-- Transition
-- ----------------------------------------------------------------------------
CREATE TABLE targetedms.Transition (
    Id SERIAL NOT NULL,
    PrecursorId INT NOT NULL,
    Mz REAL,
    Charge INT,
    NeutralMass REAL,
    NeutralLossMass REAL,
    FragmentType VARCHAR(10) NOT NULL,
    FragmentOrdinal INT,
    CleavageAa CHAR(1),
    LibraryRank INT,
    LibraryIntensity REAL,
    IsotopeDistIndex INT,
    IsotopeDistRank INT,
    IsotopeDistProportion REAL,
    Decoy BOOLEAN,
    DecoyMassShift REAL,
    Note VARCHAR(255),

    CONSTRAINT PK_Transition PRIMARY KEY (Id),
	CONSTRAINT FK_Transition_Precursor FOREIGN KEY (PrecursorId) REFERENCES targetedms.Precursor(Id)
);
CREATE INDEX IX_Transition_PrecursorId ON targetedms.Transition(PrecursorId);

CREATE TABLE targetedms.TransitionAnnotation
(
    AnnotationId INT NOT NULL,
    PrecursorId INT NOT NULL,

    CONSTRAINT PK_TransitionAnnotation PRIMARY KEY (AnnotationId,PrecursorId),
    CONSTRAINT FK_TransitionAnnotation_Annotation FOREIGN KEY (AnnotationId) REFERENCES targetedms.Annotation(Id),
    CONSTRAINT FK_TransitionAnnotation_Precursor FOREIGN KEY (PrecursorId) REFERENCES targetedms.Precursor(Id)
);

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
    Identified BOOLEAN,
    OptimizationStep INT,
    UserSet BOOLEAN,
    NOTE VARCHAR(500),

    CONSTRAINT PK_TransitionChromInfo PRIMARY KEY (Id),
    CONSTRAINT FK_TransitionChromInfo_Transition FOREIGN KEY (TransitionId) REFERENCES targetedms.Transition(Id),
    CONSTRAINT FK_TransitionChromInfo_SampleFile FOREIGN KEY (SampleFileId) REFERENCES targetedms.SampleFile(Id),
    CONSTRAINT FK_TransitionChromInfo_PrecursorChromInfo FOREIGN KEY (PrecursorChromInfoId) REFERENCES targetedms.PrecursorChromInfo(Id)
);
CREATE INDEX IX_TransitionChromInfo_TransitionId ON targetedms.TransitionChromInfo(TransitionId);
CREATE INDEX IX_TransitionChromInfo_SampleFileId ON targetedms.TransitionChromInfo(SampleFileId);
-- CREATE INDEX IX_TransitionChromInfo_PrecursorChromInfoId ON targetedms.TransitionChromInfo(PrecursorChromInfoId);

CREATE TABLE targetedms.TransitionChromInfoAnnotation
(
    AnnotationId INT NOT NULL,
    TransitionChromInfoId INT NOT NULL,

    CONSTRAINT PK_TransitionChromInfoAnnotation PRIMARY KEY (AnnotationId,TransitionChromInfoId),
    CONSTRAINT FK_TransitionChromInfoAnnotation_Annotation FOREIGN KEY (AnnotationId) REFERENCES targetedms.Annotation(Id),
    CONSTRAINT FK_TransitionChromInfoAnnotation_Transition FOREIGN KEY (TransitionChromInfoId) REFERENCES targetedms.TransitionChromInfo(Id)
);


-- ----------------------------------------------------------------------------
-- Chromatogram (from .skyd files)
-- ----------------------------------------------------------------------------
CREATE TABLE targetedms.Chromatogram
(
    Id SERIAL,
    RunId Int NOT NULL,
    Chromatogram BYTEA NOT NULL,
    NumPoints INT NOT NULL,
    NumTransitions INT NOT NULL,

    CONSTRAINT PK_Chromatogram PRIMARY KEY (Id),
    CONSTRAINT FK_Chromatogram_RunId FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);

ALTER TABLE targetedms.TransitionAnnotation DROP COLUMN PrecursorId;

ALTER TABLE targetedms.TransitionAnnotation ADD COLUMN TransitionId INT NOT NULL;
ALTER TABLE targetedms.TransitionAnnotation ADD CONSTRAINT FK_TransitionAnnotation_Transition FOREIGN KEY (TransitionId) REFERENCES targetedms.Transition(Id);

CREATE INDEX IDX_transitionannotation_transitionid ON targetedms.TransitionAnnotation(TransitionId);

-- Store the bytes of the chromatograms on PrecursorChromInfo
ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN Chromatogram BYTEA;
ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN NumTransitions INT;
ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN NumPoints INT;

-- Remember which index within the chromatogram data we used for each TransitionChromInfo
ALTER TABLE targetedms.TransitionChromInfo ADD COLUMN chromatogramindex INT;

DROP TABLE targetedms.Chromatogram;

-- ----------------------------------------------------------------------------
-- Enzyme
-- ----------------------------------------------------------------------------
CREATE TABLE targetedms.Enzyme
(
    Id SERIAL NOT NULL,
    Name VARCHAR(10) NOT NULL,
    Cut VARCHAR(10) NOT NULL,
    NoCut VARCHAR(10),
    Sense CHAR(1) NOT NULL,

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
    RunId INT NOT NULL,
    Name VARCHAR(100) NOT NULL,
    AminoAcid CHAR(1),
    Terminus CHAR(1),
    Formula VARCHAR(50) NOT NULL,
    MassDiffMono REAL,
    MassDiffAvg REAL,
    UnimodId INTEGER,

    CONSTRAINT PK_StructuralModification PRIMARY KEY (Id),
    CONSTRAINT FK_StructuralModification_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);

CREATE TABLE targetedms.StructuralModLoss
(
    Id SERIAL NOT NULL,
    StructuralModId INT NOT NULL,
    Formula VARCHAR(50),
    MassDiffMono REAL,
    MassDiffAvg REAL,

    CONSTRAINT PK_StructuralModLoss PRIMARY KEY (Id),
    CONSTRAINT FK_StructuralModLoss_StructuralModification FOREIGN KEY (StructuralModId) REFERENCES targetedms.StructuralModification(Id)
);
CREATE INDEX IX_StructuralModification_StructuralModId ON targetedms.StructuralModLoss (StructuralModId);

CREATE TABLE targetedms.RunStructuralModification
(
    StructuralModId INT NOT NULL,
    RunId INT NOT NULL,
    ExplicitMod BOOLEAN,

    CONSTRAINT PK_RunStructuralModification PRIMARY KEY (StructuralModId),
    CONSTRAINT FK_RunStructuralModification_StructuralModification FOREIGN KEY (StructuralModId) REFERENCES targetedms.StructuralModification(Id),
    CONSTRAINT FK_RunStructuralModification_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);
CREATE INDEX IX_RunStructuralModification_RunId ON targetedms.RunStructuralModification (RunId);

CREATE TABLE targetedms.IsotopeModification
(
    Id SERIAL NOT NULL,
    RunId INT NOT NULL,
    Name VARCHAR(100) NOT NULL,
    AminoAcid CHAR(1),
    Terminus CHAR(1),
    Formula VARCHAR(50) NOT NULL,
    MassDiffMono REAL,
    MassDiffAvg REAL,
    Label13C BOOLEAN,
    Label15N BOOLEAN,
    Label18O BOOLEAN,
    Label2H BOOLEAN,
    RelativeRt REAL,
    ExplicitMod BOOLEAN,
    UnimodId INTEGER,

    CONSTRAINT PK_IsotopeModification PRIMARY KEY (Id),
    CONSTRAINT FK_IsotopeModification_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);
CREATE INDEX IX_IsotopeModification_RunId ON targetedms.IsotopeModification (RunId);

CREATE TABLE targetedms.RunIsotopeModification
(
    IsotopeModId INT NOT NULL,
    RunId INT NOT NULL,
    IsotopeLabelId INT NOT NULL,
    ExplicitMod BOOLEAN,

    CONSTRAINT PK_RunIsotopeModification PRIMARY KEY (IsotopeModId),
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
    MassDiff REAL,

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
    MassDiff REAL,

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
    Name VARCHAR(10) NOT NULL,
    FileNameHint VARCHAR(100),
    LibraryId VARCHAR(50) NOT NULL,
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

-- Move the RelativeRt column from IsotopeModification to RunIsotopeModification
ALTER TABLE targetedms.RunIsotopeModification ADD COLUMN RelativeRt VARCHAR(20);
ALTER TABLE targetedms.IsotopeModification DROP COLUMN RelativeRt;

-- Remove RunId columns from StructuralModification and IsotopeModification tables.
-- Modifications are not specific to a single Skyline document, they may be used
-- by multiple documents.
ALTER TABLE targetedms.IsotopeModification DROP COLUMN RunId;
ALTER TABLE targetedms.StructuralModification DROP COLUMN RunId;

-- 'Formula' can be NULL
ALTER TABLE targetedms.IsotopeModification ALTER COLUMN Formula DROP NOT NULL;
ALTER TABLE targetedms.StructuralModification ALTER COLUMN Formula DROP NOT NULL;

-- Add IsotopeLabelId FK
ALTER TABLE targetedms.Precursor ADD CONSTRAINT FK_Precursor_IsotopeLabel FOREIGN KEY (IsotopeLabelId) REFERENCES targetedms.IsotopeLabel(Id);

ALTER TABLE targetedms.PeptideGroup ALTER COLUMN Label TYPE VARCHAR(50);

ALTER TABLE targetedms.SpectrumLibrary ALTER COLUMN LibraryId TYPE VARCHAR(200);
ALTER TABLE targetedms.SpectrumLibrary RENAME COLUMN LibraryId TO SkylineLibraryId;

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

ALTER TABLE targetedms.SpectrumLibrary ALTER COLUMN Name TYPE VARCHAR(200);

-- Don't use a central annotation table - just add the names/values directly on the specific types of annotations
DROP TABLE targetedms.PeptideGroupAnnotation;
DROP TABLE targetedms.PrecursorAnnotation;
DROP TABLE targetedms.PrecursorChromInfoAnnotation;
DROP TABLE targetedms.TransitionAnnotation;
DROP TABLE targetedms.TransitionChromInfoAnnotation;
DROP TABLE targetedms.PeptideAnnotation;
DROP TABLE targetedms.Annotation;

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


ALTER TABLE targetedms.SampleFile ALTER COLUMN SkylineId DROP NOT NULL;

ALTER TABLE targetedms.Precursor ALTER COLUMN ModifiedSequence DROP NOT NULL;

ALTER TABLE targetedms.Predictor DROP COLUMN Charge;
ALTER TABLE targetedms.Predictor DROP COLUMN Slope;
ALTER TABLE targetedms.Predictor DROP COLUMN Intercept;

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

ALTER TABLE targetedms.transitionpredictionsettings ALTER COLUMN precursormasstype TYPE VARCHAR(20);
ALTER TABLE targetedms.transitionpredictionsettings ALTER COLUMN productmasstype TYPE VARCHAR(20);

-- A structural modification can be attached to multiple amino acids, which are stored comma-separated
ALTER TABLE targetedms.structuralmodification ALTER COLUMN aminoacid TYPE VARCHAR(30);

ALTER TABLE targetedms.runstructuralmodification DROP CONSTRAINT pk_runstructuralmodification;

ALTER TABLE targetedms.runstructuralmodification ADD CONSTRAINT pk_runstructuralmodification PRIMARY KEY (structuralmodid, runid);

/* targetedms-12.20-12.30.sql */

ALTER TABLE targetedms.runs ADD COLUMN RepresentativeDataState INT NOT NULL DEFAULT 0;

ALTER TABLE targetedms.peptidegroup ADD COLUMN ActiveRepresentativeData BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE targetedms.spectrumlibrary ALTER COLUMN skylinelibraryid DROP NOT NULL;

ALTER TABLE targetedms.enzyme ALTER COLUMN name TYPE VARCHAR(30);

ALTER TABLE targetedms.transition ADD COLUMN massindex INT;

ALTER TABLE targetedms.runstructuralmodification ADD COLUMN variable BOOLEAN NOT NULL DEFAULT false;

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

ALTER TABLE targetedms.PeptideGroup ALTER COLUMN label TYPE VARCHAR(255);

ALTER TABLE targetedms.Runs ADD COLUMN DataId INT;

UPDATE targetedms.Runs runs SET DataId=(SELECT data.RowId FROM exp.Data data, exp.ExperimentRun expRun WHERE expRun.RowId=data.RunId AND expRun.lsid=runs.ExperimentRunLsid);

ALTER TABLE targetedms.Runs ADD CONSTRAINT FK_Runs_Data FOREIGN KEY (DataId) REFERENCES exp.Data(RowId);

ALTER TABLE targetedms.Runs DROP COLUMN Path;

-- Add a "Modified" column to PeptideGroup
ALTER TABLE targetedms.PeptideGroup ADD COLUMN Modified TIMESTAMP;
UPDATE targetedms.PeptideGroup pepgrp SET modified=(SELECT Modified FROM targetedms.Runs runs where runs.Id = pepgrp.RunId);


-- Add a "RepresentativeDataState" column to PeptideGroup.  This can take 4 values:
-- 0 = NotRepresentative; 1 = Representative; 2 = Representative_Deprecated; 3 = Conflicted
ALTER TABLE targetedms.PeptideGroup ADD COLUMN RepresentativeDataState INT NOT NULL DEFAULT 0;
UPDATE targetedms.PeptideGroup SET RepresentativeDataState=1 WHERE ActiveRepresentativeData=TRUE;
UPDATE targetedms.PeptideGroup AS pepgrp SET RepresentativeDataState=3
       FROM  targetedms.Runs AS runs
       WHERE pepgrp.RunId = runs.Id
       AND   pepgrp.ActiveRepresentativeData=FALSE
       AND   runs.RepresentativeDataState=1;

-- Change the order of values in RepresentativeDataState column of Runs, and add new values
-- RepresentativeProtein and RepresentativePeptide. The new state order is
-- 0 = NotRepresentative, 1 = Representative_Protein, 2 = Representative_Peptide
-- Old order was:
-- 0 = NotRepresentative, 1 = Conflicted, 2 = Representative
-- Remove the old "Conflicted" state since we will get that from the PeptideGroup or Precursor tables.
-- Mark any existing "Conflicted" runs as Representative_Protein.
UPDATE targetedms.Runs SET RepresentativeDataState=1 WHERE RepresentativeDataState=2;

-- Remove the "ActiveRepresentativeData" column from PeptideGroup since we don't need it anymore.
ALTER TABLE targetedms.PeptideGroup DROP COLUMN ActiveRepresentativeData;

-- Add a "Modified" column to Precursor
ALTER TABLE targetedms.Precursor ADD COLUMN Modified TIMESTAMP;

-- Add a "RepresentativeDataState" column to Precursor.  This can take 4 values:
-- 0 = NotRepresentative; 1 = Representative; 2 = Representative_Deprecated; 3 = Conflicted
ALTER TABLE targetedms.Precursor ADD COLUMN RepresentativeDataState INT NOT NULL DEFAULT 0;

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

ALTER TABLE targetedms.Peptide ADD COLUMN PeptideModifiedSequence VARCHAR(255);
ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN MaxHeight REAL;
ALTER TABLE targetedms.PrecursorChromInfo RENAME COLUMN LibraryDtop TO LibraryDotp;
ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN IsotopeDotp REAL;
ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN AverageMassErrorPPM REAL;
ALTER TABLE targetedms.TransitionChromInfo ADD COLUMN MassErrorPPM REAL;

UPDATE core.PortalWebParts
SET Permanent = false
WHERE Name = 'Protein Search' AND Container IN
(SELECT ObjectId FROM prop.PropertySets ps JOIN prop.Properties p ON ps.Set = p.Set
WHERE ps.Category = 'folderType' AND p.Value = 'Targeted MS');

-- Clear the representative data state for all existing containers
UPDATE targetedms.precursor set representativedatastate = 0;
UPDATE targetedms.peptidegroup set representativedatastate = 0;

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

    CONSTRAINT PK_iRTPeptide PRIMARY KEY (Id),
    CONSTRAINT FK_iRTPeptide_iRTScaleId FOREIGN KEY (iRTScaleId) REFERENCES targetedms.iRTScale(Id)
);
CREATE INDEX IX_iRTPeptide_iRTScaleId ON targetedms.iRTPeptide (iRTScaleId);

ALTER TABLE targetedms.Runs ADD iRTScaleId INT;
ALTER TABLE targetedms.Runs ADD CONSTRAINT FK_Runs_iRTScaleId FOREIGN KEY (iRTScaleId) REFERENCES targetedms.iRTScale(Id);
CREATE INDEX IX_Runs_iRTScaleId ON targetedms.Runs (iRTScaleId);

UPDATE core.PortalWebParts
SET Permanent = false
WHERE Container IN
(SELECT ObjectId FROM prop.PropertySets ps JOIN prop.Properties p ON ps.Set = p.Set
WHERE ps.Category = 'folderType' AND p.Value = 'Targeted MS');

-- We may have multiple light precursors for a given peptide if they have different charge states.
-- They should have the same ModifiedSequence, so it doesn't matter which one we use
UPDATE targetedms.peptide set PeptideModifiedSequence = (
   select MIN(prec.ModifiedSequence) from targetedms.Precursor AS prec, targetedms.IsotopeLabel AS ilabel
   WHERE prec.IsotopeLabelId = ilabel.id AND ilabel.name = 'light' AND prec.PeptideId = targetedms.Peptide.Id
) WHERE PeptideModifiedSequence IS NULL;

-- Add missing index to speed up deletes
CREATE INDEX IX_TransitionChromInfo_PrecursorChromInfoId
  ON targetedms.TransitionChromInfo(PrecursorChromInfoId);

/* targetedms-13.20-13.30.sql */

INSERT INTO prot.identTypes (name, entryDate) SELECT 'Skyline', current_timestamp
WHERE NOT EXISTS (SELECT 1 from prot.identTypes where name='Skyline');

ALTER TABLE targetedms.PeptideGroup ADD COLUMN Name VARCHAR(255);

ALTER TABLE targetedms.PeptideGroup ALTER COLUMN Note TYPE TEXT;
ALTER TABLE targetedms.Peptide ALTER COLUMN Note TYPE TEXT;
ALTER TABLE targetedms.Precursor ALTER COLUMN Note TYPE TEXT;
ALTER TABLE targetedms.PrecursorChromInfo ALTER COLUMN Note TYPE TEXT;
ALTER TABLE targetedms.Transition ALTER COLUMN Note TYPE TEXT;
ALTER TABLE targetedms.TransitionChromInfo ALTER COLUMN Note TYPE TEXT;

ALTER TABLE targetedms.Precursor ALTER COLUMN ModifiedSequence TYPE VARCHAR(300);

ALTER TABLE targetedms.isotopemodification DROP COLUMN ExplicitMod;
ALTER TABLE targetedms.runisotopemodification DROP CONSTRAINT pk_runisotopemodification;
ALTER TABLE targetedms.runisotopemodification ADD CONSTRAINT pk_runisotopemodification PRIMARY KEY (isotopemodid, runid);

/* targetedms-13.30-14.10.sql */

ALTER TABLE targetedms.runisotopemodification DROP CONSTRAINT pk_runisotopemodification;
ALTER TABLE targetedms.runisotopemodification ADD CONSTRAINT pk_runisotopemodification PRIMARY KEY (isotopemodid, runid, isotopelabelid);

ALTER TABLE targetedms.RetentionTimePredictionSettings ADD COLUMN PredictorName VARCHAR(200);
ALTER TABLE targetedms.RetentionTimePredictionSettings ADD COLUMN TimeWindow REAL;
ALTER TABLE targetedms.RetentionTimePredictionSettings ADD COLUMN UseMeasuredRts BOOLEAN;
ALTER TABLE targetedms.RetentionTimePredictionSettings ADD COLUMN MeasuredRtWindow REAL;
ALTER TABLE targetedms.RetentionTimePredictionSettings ALTER COLUMN CalculatorName TYPE VARCHAR(200);

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

    Title VARCHAR(250),
    Organism VARCHAR(100),
    ExperimentDescription TEXT,
    SampleDescription TEXT,
    Instrument VARCHAR(250),
    SpikeIn BOOLEAN,
    Citation TEXT,
    Abstract TEXT,
    PublicationLink TEXT,

    CONSTRAINT PK_ExperimentAnnotations PRIMARY KEY (Id)
);
CREATE INDEX IX_ExperimentAnnotations_Container ON targetedms.ExperimentAnnotations (Container);

CREATE TABLE targetedms.ExperimentAnnotationsRun
(
  Id SERIAL NOT NULL,
  RunId INT NOT NULL,
  ExperimentAnnotationsId INT NOT NULL,
  CreatedBy USERID,
  Created TIMESTAMP,

  CONSTRAINT PK_ExperimentAnnotationsRun PRIMARY KEY (Id),
  CONSTRAINT FK_ExperimentAnnotationsRun_ExperimentAnnotationsId FOREIGN KEY (ExperimentAnnotationsId) REFERENCES targetedms.ExperimentAnnotations(Id),
  CONSTRAINT FK_ExperimentAnnotationsRun_RunId FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);
CREATE INDEX IX_ExperimentAnnotationsRun_RunId ON targetedms.ExperimentAnnotationsRun (RunId);
CREATE INDEX IX_ExperimentAnnotationsRun_ExperimentAnnotationsId ON targetedms.ExperimentAnnotationsRun (ExperimentAnnotationsId);

/* targetedms-14.10-14.20.sql */

ALTER TABLE targetedms.iRTPeptide ADD COLUMN ImportCount INT;

UPDATE targetedms.iRTPeptide SET ImportCount = 1;

ALTER TABLE targetedms.iRTPeptide ALTER COLUMN ImportCount SET NOT NULL;

ALTER TABLE targetedms.iRTPeptide ADD COLUMN TimeSource INT;

ALTER TABLE targetedms.Runs ADD COLUMN SoftwareVersion VARCHAR(50);
ALTER TABLE targetedms.Runs ADD COLUMN FormatVersion VARCHAR(10);

-- StandardType can be one of 'Normalization', 'QC'
ALTER TABLE targetedms.Peptide ADD COLUMN StandardType VARCHAR(20);

ALTER TABLE targetedms.IsotopeEnrichment ADD COLUMN Name VARCHAR(100);

ALTER TABLE targetedms.PeptideGroup ADD COLUMN Accession VARCHAR(50);
ALTER TABLE targetedms.PeptideGroup ADD COLUMN PreferredName VARCHAR(50);
ALTER TABLE targetedms.PeptideGroup ADD COLUMN Gene VARCHAR(50);
ALTER TABLE targetedms.PeptideGroup ADD COLUMN Species VARCHAR(100);

-- AcquisitionMethod can be one of 'none', 'Targeted', 'DIA
ALTER TABLE targetedms.TransitionFullScanSettings ADD COLUMN AcquisitionMethod VARCHAR(10);
-- RetentionTimeFilterType can be one of 'none', 'scheduling_windows', 'ms2_ids'
ALTER TABLE targetedms.TransitionFullScanSettings ADD COLUMN RetentionTimeFilterType VARCHAR(20);
ALTER TABLE targetedms.TransitionFullScanSettings ADD COLUMN RetentionTimeFilterLength REAL;

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

SELECT core.fn_dropifexists('Precursor', 'targetedms', 'INDEX', 'IX_Precursor_IsotopeLabelId');
CREATE INDEX IX_Precursor_IsotopeLabelId ON targetedms.Precursor(IsotopeLabelId);

SELECT core.fn_dropifexists('ReplicateAnnotation', 'targetedms', 'INDEX', 'IX_ReplicateAnnotation_ReplicateId');
CREATE INDEX IX_ReplicateAnnotation_ReplicateId ON targetedms.ReplicateAnnotation (ReplicateId);

CREATE INDEX IX_RunEnzyme_RunId ON targetedms.RunEnzyme(RunId);

SELECT core.fn_dropifexists('Runs', 'targetedms', 'INDEX', 'IX_Runs_Container');
CREATE INDEX IX_Runs_Container ON targetedms.Runs (Container);

CREATE INDEX IX_SampleFile_InstrumentId ON targetedms.SampleFile(InstrumentId);

ALTER TABLE targetedms.iRTPeptide ADD CONSTRAINT UQ_iRTPeptide_SequenceAndScale UNIQUE (irtScaleId, ModifiedSequence);

--TransitionChromInfo UserSet can now be one of 'TRUE', 'FALSE', 'IMPORTED', 'REINTEGRATE'
ALTER TABLE targetedms.TransitionChromInfo ADD UserSet_temp VARCHAR(20);
UPDATE targetedms.TransitionChromInfo SET UserSet_temp =(CASE WHEN UserSet THEN 'TRUE'
                                                              WHEN UserSet IS FALSE THEN 'FALSE'
                                                              ELSE NULL END);
ALTER TABLE targetedms.TransitionChromInfo DROP COLUMN UserSet;
ALTER TABLE targetedms.TransitionChromInfo RENAME UserSet_temp TO UserSet;



--PrecursorChromInfo UserSet can now be one of 'TRUE', 'FALSE', 'IMPORTED', 'REINTEGRATE'
ALTER TABLE targetedms.PrecursorChromInfo ADD UserSet_temp VARCHAR(20);
UPDATE targetedms.PrecursorChromInfo SET UserSet_temp =(CASE WHEN UserSet THEN 'TRUE'
                                                         WHEN UserSet IS FALSE THEN 'FALSE'
                                                         ELSE NULL END);
ALTER TABLE targetedms.PrecursorChromInfo DROP COLUMN UserSet;
ALTER TABLE targetedms.PrecursorChromInfo RENAME UserSet_temp TO UserSet;



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

    CONSTRAINT PK_MeasuredDriftTime PRIMARY KEY (Id),
    CONSTRAINT FK_MeasuredDriftTime_DriftTimePredictionSettings FOREIGN KEY (DriftTimePredictionSettingsId) REFERENCES targetedms.DriftTimePredictionSettings(Id)
);
CREATE INDEX IX_MeasuredDriftTime_DriftTimePredictionSettingsId ON targetedms.MeasuredDriftTime(DriftTimePredictionSettingsId);

/* targetedms-14.20-14.30.sql */

ALTER TABLE targetedms.Enzyme ALTER COLUMN Cut DROP NOT NULL;
ALTER TABLE targetedms.Enzyme ALTER COLUMN Sense DROP NOT NULL;
ALTER TABLE targetedms.Enzyme ADD COLUMN CutC VARCHAR(10);
ALTER TABLE targetedms.Enzyme ADD COLUMN NoCutC VARCHAR(10);
ALTER TABLE targetedms.Enzyme ADD COLUMN CutN VARCHAR(10);
ALTER TABLE targetedms.Enzyme ADD COLUMN NoCutN VARCHAR(10);

ALTER TABLE targetedms.PeptideGroup ADD COLUMN AltDescription TEXT;

ALTER TABLE targetedms.Transition ADD COLUMN MeasuredIonName VARCHAR(20);

ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN UncompressedSize INT;

ALTER TABLE targetedms.ExperimentAnnotations ADD ExperimentId INT NOT NULL DEFAULT 0;
ALTER TABLE targetedms.ExperimentAnnotations ADD JournalCopy BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE targetedms.ExperimentAnnotations ADD IncludeSubfolders BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX IX_ExperimentAnnotations_ExperimentId ON targetedms.ExperimentAnnotations(ExperimentId);

DROP TABLE targetedms.ExperimentAnnotationsRun;
DELETE FROM targetedms.ExperimentAnnotations WHERE Id NOT IN
(SELECT ea.Id from targetedms.ExperimentAnnotations AS ea INNER JOIN core.Containers AS c ON ea.Container = c.entityId);

ALTER TABLE targetedms.ExperimentAnnotations ADD CONSTRAINT FK_ExperimentAnnotations_Experiment FOREIGN KEY (ExperimentId) REFERENCES exp.Experiment(RowId);
ALTER TABLE targetedms.ExperimentAnnotations ADD CONSTRAINT FK_ExperimentAnnotations_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId);

/* Add all runs in an experiment's container to exp.runlist */
INSERT INTO exp.runlist
(SELECT e.rowid, er.rowId, e.Created, e.CreatedBy
FROM targetedms.ExperimentAnnotations ea
INNER JOIN exp.experiment e ON (e.rowid = ea.ExperimentId)
INNER JOIN exp.experimentrun er ON (er.container = ea.Container));

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

--TransitionChromInfo's Identified column can now be one of 'true', 'false' or 'aligned'
ALTER TABLE targetedms.TransitionChromInfo ADD Identified_temp VARCHAR(10);
UPDATE targetedms.TransitionChromInfo SET Identified_temp =(CASE WHEN Identified THEN 'true'
                                                              WHEN Identified IS FALSE THEN 'false'
                                                              ELSE NULL END);
ALTER TABLE targetedms.TransitionChromInfo DROP COLUMN Identified;
ALTER TABLE targetedms.TransitionChromInfo RENAME Identified_temp TO Identified;

--PrecursorChromInfo's Identified column can now be one of 'true', 'false' or 'aligned'
ALTER TABLE targetedms.PrecursorChromInfo ADD Identified_temp VARCHAR(10);
UPDATE targetedms.PrecursorChromInfo SET Identified_temp =(CASE WHEN Identified THEN 'true'
                                                         WHEN Identified IS FALSE THEN 'false'
                                                         ELSE NULL END);
ALTER TABLE targetedms.PrecursorChromInfo DROP COLUMN Identified;
ALTER TABLE targetedms.PrecursorChromInfo RENAME Identified_temp TO Identified;

/* targetedms-14.30-14.31.sql */

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

/* targetedms-14.32-14.33.sql */

UPDATE targetedms.QCAnnotationType SET Color = '990000' WHERE Color = 'FF0000';
UPDATE targetedms.QCAnnotationType SET Color = '009900' WHERE Color = '00FF00';
UPDATE targetedms.QCAnnotationType SET Color = '000099' WHERE Color = '0000FF';

/* targetedms-14.33-14.34.sql */

ALTER TABLE targetedms.ExperimentAnnotations ALTER COLUMN Title TYPE VARCHAR;

/* targetedms-14.34-14.35.sql */

-- ----------------------------------------------------------------------------
-- Peptide
-- ----------------------------------------------------------------------------
ALTER TABLE targetedms.Peptide ALTER COLUMN calcNeutralMass TYPE DOUBLE PRECISION;

-- ----------------------------------------------------------------------------
-- Precursor
-- ----------------------------------------------------------------------------
ALTER TABLE targetedms.Precursor ALTER COLUMN Mz TYPE DOUBLE PRECISION;
ALTER TABLE targetedms.Precursor ALTER COLUMN NeutralMass TYPE DOUBLE PRECISION;

-- ----------------------------------------------------------------------------
-- Transition
-- ----------------------------------------------------------------------------
ALTER TABLE targetedms.Transition ALTER COLUMN Mz TYPE DOUBLE PRECISION;
ALTER TABLE targetedms.Transition ALTER COLUMN NeutralMass TYPE DOUBLE PRECISION;
ALTER TABLE targetedms.Transition ALTER COLUMN NeutralLossMass TYPE DOUBLE PRECISION;


-- ----------------------------------------------------------------------------
-- StructuralModification
-- ----------------------------------------------------------------------------
ALTER TABLE targetedms.StructuralModification ALTER COLUMN massDiffMono TYPE DOUBLE PRECISION;
ALTER TABLE targetedms.StructuralModification ALTER COLUMN massDiffAvg TYPE DOUBLE PRECISION;

-- ----------------------------------------------------------------------------
-- StructuralModLoss
-- ----------------------------------------------------------------------------
ALTER TABLE targetedms.StructuralModLoss ALTER COLUMN massDiffMono TYPE DOUBLE PRECISION;
ALTER TABLE targetedms.StructuralModLoss ALTER COLUMN massDiffAvg TYPE DOUBLE PRECISION;

-- ----------------------------------------------------------------------------
-- IsotopeModification
-- ----------------------------------------------------------------------------
ALTER TABLE targetedms.IsotopeModification ALTER COLUMN MassDiffMono TYPE DOUBLE PRECISION;
ALTER TABLE targetedms.IsotopeModification ALTER COLUMN MassDiffAvg TYPE DOUBLE PRECISION;

-- ----------------------------------------------------------------------------
-- PeptideStructuralModification
-- ----------------------------------------------------------------------------
ALTER TABLE targetedms.PeptideStructuralModification ALTER COLUMN massDiff TYPE DOUBLE PRECISION;

-- ----------------------------------------------------------------------------
-- IsotopeStructuralModification
-- ----------------------------------------------------------------------------
ALTER TABLE targetedms.PeptideIsotopeModification ALTER COLUMN massDiff TYPE DOUBLE PRECISION;

/* targetedms-14.35-14.36.sql */

-- ----------------------------------------------------------------------------
-- Peptide
-- ----------------------------------------------------------------------------
ALTER TABLE targetedms.Peptide ADD ExplicitRetentionTime REAL;

-- ----------------------------------------------------------------------------
-- Precursor
-- ----------------------------------------------------------------------------
ALTER TABLE targetedms.Precursor ADD ExplicitCollisionEnergy REAL;
ALTER TABLE targetedms.Precursor ADD ExplicitDriftTimeMsec REAL;
ALTER TABLE targetedms.Precursor ADD ExplicitDriftTimeHighEnergyOffsetMsec REAL;

-- ----------------------------------------------------------------------------
-- Transition -- fragment_type can be one of "custom_precursor", "custom", "a", "b", "c", "x", "y", "z"
-- ----------------------------------------------------------------------------
ALTER TABLE targetedms.Transition ALTER COLUMN FragmentType TYPE VARCHAR(20);

-- ----------------------------------------------------------------------------
-- MeasuredDriftTime
-- ----------------------------------------------------------------------------
ALTER TABLE targetedms.MeasuredDriftTime ADD HighEnergyDriftTimeOffset REAL;

-- ----------------------------------------------------------------------------
-- StructuralModLoss -- inlcusion can be one of "Library", "Never", "Always"
-- ----------------------------------------------------------------------------
ALTER TABLE targetedms.StructuralModLoss ADD Inclusion VARCHAR(10);


-- ----------------------------------------------------------------------------
-- PeptideChromInfo -- the following two columns are not part of the
--                     <peptide_result> element in the Skyline XML schema.
-- ----------------------------------------------------------------------------
ALTER TABLE targetedms.PeptideChromInfo DROP COLUMN PredictedRetentionTime;
ALTER TABLE targetedms.PeptideChromInfo DROP COLUMN RatioToStandard;


-- PeptideGroup.gene -- this column (source: Uniprot) can contain several gene names.
ALTER TABLE targetedms.PeptideGroup ALTER COLUMN gene TYPE VARCHAR(255);

-- Enzyme
ALTER TABLE targetedms.Enzyme ALTER COLUMN Cut TYPE VARCHAR(20);
ALTER TABLE targetedms.Enzyme ALTER COLUMN NoCut TYPE VARCHAR(20);
ALTER TABLE targetedms.Enzyme ALTER COLUMN CutC TYPE VARCHAR(20);
ALTER TABLE targetedms.Enzyme ALTER COLUMN NoCutC TYPE VARCHAR(20);
ALTER TABLE targetedms.Enzyme ALTER COLUMN CutN TYPE VARCHAR(20);
ALTER TABLE targetedms.Enzyme ALTER COLUMN NoCutN TYPE VARCHAR(20);

/* targetedms-14.36-14.37.sql */

-- ----------------------------------------------------------------------------
-- Peptide
-- ----------------------------------------------------------------------------
-- Drop the following NOT NULL constraints till we migrate to a normalized schema.
ALTER TABLE targetedms.Peptide ALTER COLUMN Sequence DROP NOT NULL;
ALTER TABLE targetedms.Peptide ALTER COLUMN CalcNeutralMass DROP NOT NULL;
ALTER TABLE targetedms.Peptide ALTER COLUMN NumMissedCleavages DROP NOT NULL;



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
-- Precursor
-- ----------------------------------------------------------------------------
-- Drop the following NOT NULL constraints till we migrate to a normalized schema.
ALTER TABLE targetedms.Precursor ALTER COLUMN NeutralMass DROP NOT NULL;



-- ----------------------------------------------------------------------------
-- Transition
-- ----------------------------------------------------------------------------
-- Drop the following NOT NULL constraints till we migrate to a normalized schema.
ALTER TABLE targetedms.Transition ALTER COLUMN FragmentType DROP NOT NULL;


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