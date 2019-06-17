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