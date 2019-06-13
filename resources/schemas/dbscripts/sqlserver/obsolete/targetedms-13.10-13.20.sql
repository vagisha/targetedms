/*
 * Copyright (c) 2015-2019 LabKey Corporation
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

/* targetedms-13.10-13.11.sql */

ALTER TABLE targetedms.Peptide ADD PeptideModifiedSequence NVARCHAR(255);
ALTER TABLE targetedms.PrecursorChromInfo ADD MaxHeight REAL;
ALTER TABLE targetedms.PrecursorChromInfo ADD IsotopeDotp REAL;
EXEC sp_rename 'targetedms.PrecursorChromInfo.LibraryDtop', 'LibraryDotp', 'COLUMN';
ALTER TABLE targetedms.PrecursorChromInfo ADD AverageMassErrorPPM REAL;
ALTER TABLE targetedms.TransitionChromInfo ADD MassErrorPPM REAL;

/* targetedms-13.12-13.13.sql */

UPDATE core.PortalWebParts
SET Permanent = 0
WHERE Name = 'Protein Search' AND Container IN
(SELECT ObjectId FROM prop.PropertySets ps JOIN prop.Properties p ON ps.[Set] = p.[Set]
WHERE ps.Category = 'folderType' AND p.Value = 'Targeted MS');

/* targetedms-13.13-13.14.sql */

-- Clear the representative data state for all existing containers
UPDATE targetedms.precursor set representativedatastate = 0;
UPDATE targetedms.peptidegroup set representativedatastate = 0;

EXEC core.executeJavaUpgradeCode 'setContainersToExperimentType'
GO

/* targetedms-13.14-13.15.sql */

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

    CONSTRAINT PK_iRTPeptide PRIMARY KEY (Id),
    CONSTRAINT FK_iRTPeptide_iRTScaleId FOREIGN KEY (iRTScaleId) REFERENCES targetedms.iRTScale(Id)
);
CREATE INDEX IX_iRTPeptide_iRTScaleId ON targetedms.iRTPeptide (iRTScaleId);

ALTER TABLE targetedms.Runs ADD iRTScaleId INT;
ALTER TABLE targetedms.Runs ADD CONSTRAINT FK_Runs_iRTScaleId FOREIGN KEY (iRTScaleId) REFERENCES targetedms.iRTScale(Id);
CREATE INDEX IX_Runs_iRTScaleId ON targetedms.Runs (iRTScaleId);

/* targetedms-13.15-13.16.sql */

UPDATE core.PortalWebParts
SET Permanent = 0
WHERE Container IN
(SELECT ObjectId FROM prop.PropertySets ps JOIN prop.Properties p ON ps.[Set] = p.[Set]
WHERE ps.Category = 'folderType' AND p.Value = 'Targeted MS');

/* targetedms-13.16-13.17.sql */

-- We may have multiple light precursors for a given peptide if they have different charge states.
-- They should have the same ModifiedSequence, so it doesn't matter which one we use
UPDATE targetedms.peptide set PeptideModifiedSequence = (
   select MIN(prec.ModifiedSequence) from targetedms.Precursor AS prec, targetedms.IsotopeLabel AS ilabel
   WHERE prec.IsotopeLabelId = ilabel.id AND ilabel.name = 'light' AND prec.PeptideId = targetedms.Peptide.Id
) WHERE PeptideModifiedSequence IS NULL;

/* targetedms-13.17-13.18.sql */

-- Add missing index to speed up deletes
CREATE INDEX IX_TransitionChromInfo_PrecursorChromInfoId
  ON targetedms.TransitionChromInfo(PrecursorChromInfoId);
