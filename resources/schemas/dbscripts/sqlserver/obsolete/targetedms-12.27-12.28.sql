/*
 * Copyright (c) 2012-2013 LabKey Corporation
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
ALTER TABLE targetedms.PeptideGroup ADD Modified DATETIME;
GO
UPDATE targetedms.PeptideGroup SET Modified=(SELECT Modified FROM targetedms.Runs runs where runs.Id = RunId);


-- Add a "RepresentativeDataState" column to PeptideGroup.  This can take 4 values:
-- 0 = NotRepresentative; 1 = Representative; 2 = Representative_Deprecated; 3 = Conflicted
ALTER TABLE targetedms.PeptideGroup ADD RepresentativeDataState INT NOT NULL DEFAULT 0;
GO
UPDATE targetedms.PeptideGroup SET RepresentativeDataState=1 WHERE ActiveRepresentativeData=1;
UPDATE targetedms.PeptideGroup SET RepresentativeDataState=3
       FROM  targetedms.Runs AS runs
       WHERE runs.Id = RunId
       AND   ActiveRepresentativeData=0
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
-- Microsoft implemented DEFAULT as a type of a constraint rather than a column property.
-- Cannot drop a column that has a DEFAULT constraint.  The constraint has to be dropped first.
-- It is a pain to drop the constraint if it wasn't created with a specific name.
-- Solution from:
-- http://stackoverflow.com/questions/1430456/how-to-drop-sql-default-constraint-without-knowing-its-name

declare @Command  nvarchar(1000)
select @Command = 'ALTER TABLE targetedms.PeptideGroup drop constraint ' + d.name
from sys.tables t
  join    sys.default_constraints d
    on d.parent_object_id = t.object_id
  join    sys.columns c
   on c.object_id = t.object_id
    and c.column_id = d.parent_column_id
where t.name = 'PeptideGroup'
  and c.name = 'ActiveRepresentativeData'

print @Command
execute (@Command)
GO
ALTER TABLE targetedms.PeptideGroup DROP COLUMN ActiveRepresentativeData;
