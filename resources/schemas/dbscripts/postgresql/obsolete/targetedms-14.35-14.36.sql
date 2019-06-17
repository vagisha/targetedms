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















