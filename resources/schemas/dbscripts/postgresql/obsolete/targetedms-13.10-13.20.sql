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

ALTER TABLE targetedms.Peptide ADD COLUMN PeptideModifiedSequence VARCHAR(255);
ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN MaxHeight REAL;
ALTER TABLE targetedms.PrecursorChromInfo RENAME COLUMN LibraryDtop TO LibraryDotp;
ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN IsotopeDotp REAL;
ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN AverageMassErrorPPM REAL;
ALTER TABLE targetedms.TransitionChromInfo ADD COLUMN MassErrorPPM REAL;

/* targetedms-13.12-13.13.sql */

UPDATE core.PortalWebParts
SET Permanent = false
WHERE Name = 'Protein Search' AND Container IN
(SELECT ObjectId FROM prop.PropertySets ps JOIN prop.Properties p ON ps.Set = p.Set
WHERE ps.Category = 'folderType' AND p.Value = 'Targeted MS');

/* targetedms-13.13-13.14.sql */

-- Clear the representative data state for all existing containers
UPDATE targetedms.precursor set representativedatastate = 0;
UPDATE targetedms.peptidegroup set representativedatastate = 0;

SELECT core.executeJavaUpgradeCode('setContainersToExperimentType');

/* targetedms-13.14-13.15.sql */

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

/* targetedms-13.15-13.16.sql */

UPDATE core.PortalWebParts
SET Permanent = false
WHERE Container IN
(SELECT ObjectId FROM prop.PropertySets ps JOIN prop.Properties p ON ps.Set = p.Set
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
