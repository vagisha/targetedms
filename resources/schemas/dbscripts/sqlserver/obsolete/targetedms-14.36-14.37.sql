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










