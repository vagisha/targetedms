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

/* targetedms-14.30-14.31.sql */

CREATE TABLE targetedms.QCAnnotationType
(
    Id INT IDENTITY(1, 1) NOT NULL,
    Container ENTITYID NOT NULL,
    CreatedBy USERID,
    Created DATETIME,
    ModifiedBy USERID,
    Modified DATETIME,
    Name VARCHAR(100) NOT NULL,
    Description TEXT,
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
    Description TEXT NOT NULL,
    Date DATETIME NOT NULL,

    CONSTRAINT PK_QCAnnotation PRIMARY KEY (Id),
    CONSTRAINT FK_QCAnnotation_QCAnnotationType FOREIGN KEY (QCAnnotationTypeId) REFERENCES targetedms.QCAnnotationType(Id)
);

-- Poke a few rows into the /Shared project
EXEC core.executeJavaUpgradeCode 'populateDefaultAnnotationTypes';

/* targetedms-14.31-14.32.sql */

-- Change from VARCHAR to NVARCHAR and TEXT to NVARCHAR(MAX)

-- Drop constraint so we can change the column
ALTER TABLE targetedms.QCAnnotationType DROP CONSTRAINT UQ_QCAnnotationType_ContainerName;

ALTER TABLE targetedms.QCAnnotationType ALTER COLUMN Name NVARCHAR(100);
ALTER TABLE targetedms.QCAnnotationType ALTER COLUMN Description NVARCHAR(MAX);

-- Re-add constraint so we can change the column
ALTER TABLE targetedms.QCAnnotationType ADD CONSTRAINT UQ_QCAnnotationType_ContainerName UNIQUE (Container, Name);

ALTER TABLE targetedms.QCAnnotation ALTER COLUMN Description NVARCHAR(MAX);

/* targetedms-14.32-14.33.sql */

UPDATE targetedms.QCAnnotationType SET Color = '990000' WHERE Color = 'FF0000';
UPDATE targetedms.QCAnnotationType SET Color = '009900' WHERE Color = '00FF00';
UPDATE targetedms.QCAnnotationType SET Color = '000099' WHERE Color = '0000FF';

/* targetedms-14.33-14.34.sql */

ALTER TABLE targetedms.ExperimentAnnotations ALTER COLUMN Title NVARCHAR(MAX);

/* targetedms-14.34-14.35.sql */

-- ----------------------------------------------------------------------------
-- Peptide
-- ----------------------------------------------------------------------------
ALTER TABLE targetedms.Peptide ALTER COLUMN calcNeutralMass FLOAT;

-- ----------------------------------------------------------------------------
-- Precursor
-- ----------------------------------------------------------------------------
ALTER TABLE targetedms.Precursor ALTER COLUMN Mz FLOAT;
ALTER TABLE targetedms.Precursor ALTER COLUMN NeutralMass FLOAT;

-- ----------------------------------------------------------------------------
-- Transition
-- ----------------------------------------------------------------------------
ALTER TABLE targetedms.Transition ALTER COLUMN Mz FLOAT;
ALTER TABLE targetedms.Transition ALTER COLUMN NeutralMass FLOAT;
ALTER TABLE targetedms.Transition ALTER COLUMN NeutralLossMass FLOAT;


-- ----------------------------------------------------------------------------
-- StructuralModification
-- ----------------------------------------------------------------------------
ALTER TABLE targetedms.StructuralModification ALTER COLUMN massDiffMono FLOAT;
ALTER TABLE targetedms.StructuralModification ALTER COLUMN massDiffAvg FLOAT;

-- ----------------------------------------------------------------------------
-- StructuralModLoss
-- ----------------------------------------------------------------------------
ALTER TABLE targetedms.StructuralModLoss ALTER COLUMN massDiffMono FLOAT;
ALTER TABLE targetedms.StructuralModLoss ALTER COLUMN massDiffAvg FLOAT;

-- ----------------------------------------------------------------------------
-- IsotopeModification
-- ----------------------------------------------------------------------------
ALTER TABLE targetedms.IsotopeModification ALTER COLUMN MassDiffMono FLOAT;
ALTER TABLE targetedms.IsotopeModification ALTER COLUMN MassDiffAvg FLOAT;

-- ----------------------------------------------------------------------------
-- PeptideStructuralModification
-- ----------------------------------------------------------------------------
ALTER TABLE targetedms.PeptideStructuralModification ALTER COLUMN massDiff FLOAT;

-- ----------------------------------------------------------------------------
-- IsotopeStructuralModification
-- ----------------------------------------------------------------------------
ALTER TABLE targetedms.PeptideIsotopeModification ALTER COLUMN massDiff FLOAT;

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
ALTER TABLE targetedms.Transition ALTER COLUMN FragmentType NVARCHAR(20);

-- ----------------------------------------------------------------------------
-- MeasuredDriftTime
-- ----------------------------------------------------------------------------
ALTER TABLE targetedms.MeasuredDriftTime ADD HighEnergyDriftTimeOffset REAL;

-- ----------------------------------------------------------------------------
-- StructuralModLoss -- inlcusion can be one of "Library", "Never", "Always"
-- ----------------------------------------------------------------------------
ALTER TABLE targetedms.StructuralModLoss ADD Inclusion NVARCHAR(10);


-- ----------------------------------------------------------------------------
-- PeptideChromInfo -- the following two columns are not part of the
--                     <peptide_result> element in the Skyline XML schema.
-- ----------------------------------------------------------------------------
ALTER TABLE targetedms.PeptideChromInfo DROP COLUMN PredictedRetentionTime;
ALTER TABLE targetedms.PeptideChromInfo DROP COLUMN RatioToStandard;


-- PeptideGroup.gene -- this column (source: Uniprot) can contain several gene names.
ALTER TABLE targetedms.PeptideGroup ALTER COLUMN gene NVARCHAR(255);

-- Enzyme
ALTER TABLE targetedms.Enzyme ALTER COLUMN Cut NVARCHAR(20);
ALTER TABLE targetedms.Enzyme ALTER COLUMN NoCut NVARCHAR(20);
ALTER TABLE targetedms.Enzyme ALTER COLUMN CutC NVARCHAR(20);
ALTER TABLE targetedms.Enzyme ALTER COLUMN NoCutC NVARCHAR(20);
ALTER TABLE targetedms.Enzyme ALTER COLUMN CutN NVARCHAR(20);
ALTER TABLE targetedms.Enzyme ALTER COLUMN NoCutN NVARCHAR(20);

/* targetedms-14.36-14.37.sql */

-- ----------------------------------------------------------------------------
-- Peptide
-- ----------------------------------------------------------------------------
-- Drop the following NOT NULL constraints till we migrate to a normalized schema.
ALTER TABLE targetedms.Peptide ALTER COLUMN Sequence NVARCHAR(100) NULL;
ALTER TABLE targetedms.Peptide ALTER COLUMN CalcNeutralMass FLOAT NULL;
ALTER TABLE targetedms.Peptide ALTER COLUMN NumMissedCleavages INT NULL;


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
-- Precursor
-- ----------------------------------------------------------------------------
-- Drop the following NOT NULL constraints till we migrate to a normalized schema.
ALTER TABLE targetedms.Precursor ALTER COLUMN NeutralMass FLOAT NULL;


-- ----------------------------------------------------------------------------
-- Transition
-- ----------------------------------------------------------------------------
-- Drop the following NOT NULL constraints till we migrate to a normalized schema.
ALTER TABLE targetedms.Transition ALTER COLUMN FragmentType NVARCHAR(10) NULL;


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