/*
 * Copyright (c) 2018-2019 LabKey Corporation
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

/* targetedms-15.30-15.31.sql */

CREATE TABLE targetedms.AutoQCPing (
  CreatedBy USERID,
  Created TIMESTAMP,
  ModifiedBy USERID,
  Modified TIMESTAMP,
  Container ENTITYID NOT NULL,

  CONSTRAINT PK_AutoQCPing PRIMARY KEY (Container)
);

/* targetedms-15.31-15.32.sql */

ALTER TABLE targetedms.PrecursorChromInfo DROP COLUMN TotalAreaNormalized;
ALTER TABLE targetedms.TransitionChromInfo DROP COLUMN AreaNormalized;

/* targetedms-15.32-15.33.sql */

/* targetedms-15.32-15.33.sql - New Data model as per 'Small molecule support' spec */

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

/* targetedms-15.33-15.34.sql */

ALTER TABLE targetedms.Runs ADD COLUMN DocumentGUID ENTITYID;

/* targetedms-15.34-15.35.sql */

ALTER TABLE targetedms.moleculeprecursor ADD COLUMN IonFormula VARCHAR(100);
ALTER TABLE targetedms.moleculeprecursor ADD COLUMN CustomIonName VARCHAR(100);
ALTER TABLE targetedms.moleculeprecursor ADD COLUMN MassMonoisotopic DOUBLE PRECISION NOT NULL;
ALTER TABLE targetedms.moleculeprecursor ADD COLUMN MassAverage DOUBLE PRECISION NOT NULL;

/* targetedms-15.35-15.36.sql */

ALTER TABLE targetedms.Precursor ALTER COLUMN ModifiedSequence TYPE VARCHAR(300);
UPDATE targetedms.Peptide SET Sequence='' WHERE Sequence IS NULL;
ALTER TABLE targetedms.Peptide ALTER COLUMN Sequence SET NOT NULL;