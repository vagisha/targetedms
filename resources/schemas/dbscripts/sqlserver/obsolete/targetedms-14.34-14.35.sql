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



















