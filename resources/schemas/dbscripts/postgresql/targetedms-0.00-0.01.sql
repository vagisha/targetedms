/*
 * Copyright (c) 2011 LabKey Corporation
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

-- Create schema, tables, indexes, and constraints used for TargetedMS module here
-- All SQL VIEW definitions should be created in targetedms-create.sql and dropped in targetedms-drop.sql
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