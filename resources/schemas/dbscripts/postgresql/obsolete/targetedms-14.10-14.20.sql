/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
/* targetedms-14.10-14.11.sql */

ALTER TABLE targetedms.iRTPeptide ADD COLUMN ImportCount INT;

UPDATE targetedms.iRTPeptide SET ImportCount = 1;

ALTER TABLE targetedms.iRTPeptide ALTER COLUMN ImportCount SET NOT NULL;

/* targetedms-14.11-14.12.sql */

ALTER TABLE targetedms.iRTPeptide ADD COLUMN TimeSource INT;

/* targetedms-14.12-14.13.sql */

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

/* targetedms-14.13-14.14.sql */

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

/* targetedms-14.14-14.15.sql */

ALTER TABLE targetedms.iRTPeptide ADD CONSTRAINT UQ_iRTPeptide_SequenceAndScale UNIQUE (irtScaleId, ModifiedSequence);

/* targetedms-14.15-14.16.sql */

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