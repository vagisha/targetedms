/*
 * Copyright (c) 2012 LabKey Corporation
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

-- Add a "Modified" column to PeptideGroup
ALTER TABLE targetedms.PeptideGroup ADD COLUMN Modified TIMESTAMP;
UPDATE targetedms.PeptideGroup pepgrp SET modified=(SELECT Modified FROM targetedms.Runs runs where runs.Id = pepgrp.RunId);


-- Add a "RepresentativeDataState" column to PeptideGroup.  This can take 4 values:
-- 0 = NotRepresentative; 1 = Representative; 2 = Representative_Deprecated; 3 = Conflicted
ALTER TABLE targetedms.PeptideGroup ADD COLUMN RepresentativeDataState INT NOT NULL DEFAULT 0;
UPDATE targetedms.PeptideGroup SET RepresentativeDataState=1 WHERE ActiveRepresentativeData=TRUE;
UPDATE targetedms.PeptideGroup AS pepgrp SET RepresentativeDataState=3
       FROM  targetedms.Runs AS runs
       WHERE pepgrp.RunId = runs.Id
       AND   pepgrp.ActiveRepresentativeData=FALSE
       AND   runs.RepresentativeDataState=1;

-- Change the order of values in RepresentativeDataState column of Runs, and add new values
-- RepresentativeProtein and RepresentativePeptide. The new state order is
-- 0 = NotRepresentative, 1 = Representative_Protein, 2 = Representative_Peptide
-- Old order was:
-- 0 = NotRepresentative, 1 = Conflicted, 2 = Representative
-- Remove the old "Conflicted" state since we will get that from the PeptideGroup or Precursor tables.
-- Mark any existing "Conflicted" runs as Representative_Protein.
UPDATE targetedms.Runs SET RepresentativeDataState=1 WHERE RepresentativeDataState=2;

-- Remove the "ActiveRepresentativeData" column from PeptideGroup since we don't need it anymore.
ALTER TABLE targetedms.PeptideGroup DROP COLUMN ActiveRepresentativeData;




