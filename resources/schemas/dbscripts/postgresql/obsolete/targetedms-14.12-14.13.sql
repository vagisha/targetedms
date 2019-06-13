/*
 * Copyright (c) 2014-2019 LabKey Corporation
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

