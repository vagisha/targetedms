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
  Created DATETIME,
  ModifiedBy USERID,
  Modified DATETIME,
  Container ENTITYID NOT NULL,

  CONSTRAINT PK_AutoQCPing PRIMARY KEY (Container)
);

/* targetedms-15.31-15.32.sql */

ALTER TABLE targetedms.PrecursorChromInfo DROP COLUMN TotalAreaNormalized;
ALTER TABLE targetedms.TransitionChromInfo DROP COLUMN AreaNormalized;

/* targetedms-15.32-15.33.sql */

/* targetedms-15.32-15.33.sql - New Data model as per 'Small molecule support' spec */

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

/* targetedms-15.33-15.34.sql */

ALTER TABLE targetedms.Runs ADD DocumentGUID ENTITYID;

/* targetedms-15.34-15.35.sql */

ALTER TABLE targetedms.moleculeprecursor ADD IonFormula VARCHAR(100);
ALTER TABLE targetedms.moleculeprecursor ADD CustomIonName VARCHAR(100);
ALTER TABLE targetedms.moleculeprecursor ADD MassMonoisotopic FLOAT NOT NULL;
ALTER TABLE targetedms.moleculeprecursor ADD MassAverage FLOAT NOT NULL;

/* targetedms-15.35-15.36.sql */

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