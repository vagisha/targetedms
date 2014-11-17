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

/* targetedms-12.20-12.21.sql */

ALTER TABLE targetedms.runs ADD RepresentativeDataState INT NOT NULL DEFAULT 0;

ALTER TABLE targetedms.peptidegroup ADD ActiveRepresentativeData BIT NOT NULL DEFAULT 0;

/* targetedms-12.21-12.22.sql */

ALTER TABLE  targetedms.spectrumlibrary ALTER COLUMN skylinelibraryid NVARCHAR(200) NULL

/* targetedms-12.22-12.23.sql */

ALTER TABLE targetedms.enzyme ALTER COLUMN name VARCHAR(30);

/* targetedms-12.23-12.24.sql */

ALTER TABLE targetedms.transition ADD massindex INT;

ALTER TABLE targetedms.runstructuralmodification ADD variable BIT NOT NULL DEFAULT 0;

/* targetedms-12.24-12.25.sql */

CREATE TABLE targetedms.ReplicateAnnotation
(
    Id INT IDENTITY(1, 1) NOT NULL,
    ReplicateId INT NOT NULL,
    Name NVARCHAR(255) NOT NULL,
    Value NVARCHAR(255) NOT NULL,

    CONSTRAINT PK_ReplicateAnnotation PRIMARY KEY (Id),
    CONSTRAINT FK_ReplicateAnnotation_Replicate FOREIGN KEY (ReplicateId) REFERENCES targetedms.Replicate(Id),
    CONSTRAINT UQ_ReplicateAnnotation_Name_Repicate UNIQUE (Name, ReplicateId)
);

/* targetedms-12.25-12.26.sql */

ALTER TABLE targetedms.PeptideGroup ALTER COLUMN label NVARCHAR(255);

/* targetedms-12.26-12.27.sql */

ALTER TABLE targetedms.Runs ADD DataId INT;
GO
UPDATE targetedms.Runs SET DataId =(SELECT data.RowId FROM exp.Data data, exp.ExperimentRun expRun WHERE expRun.RowId=data.RunId AND expRun.lsid = ExperimentRunLsid);

ALTER TABLE targetedms.Runs ADD CONSTRAINT FK_Runs_Data FOREIGN KEY (DataId) REFERENCES exp.Data(RowId);

ALTER TABLE targetedms.Runs DROP COLUMN Path;

/* targetedms-12.27-12.28.sql */

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

/* targetedms-12.28-12.29.sql */

-- Add a "Modified" column to Precursor
ALTER TABLE targetedms.Precursor ADD Modified DATETIME;

-- Add a "RepresentativeDataState" column to Precursor.  This can take 4 values:
-- 0 = NotRepresentative; 1 = Representative; 2 = Representative_Deprecated; 3 = Conflicted
ALTER TABLE targetedms.Precursor ADD RepresentativeDataState INT NOT NULL DEFAULT 0;

-- AnnotationSettings table to store annotation settings.
-- Name: Name of the annotation
-- Targets: Comma-separated list of one or more of protein, peptide, precursor, transition, replicate, precursor_result, transition_result
-- Type:  One of text, number, true_false, value_list
CREATE TABLE targetedms.AnnotationSettings
(
    Id INT IDENTITY(1, 1) NOT NULL,
    RunId INT NOT NULL,
    Name NVARCHAR(255) NOT NULL,
    Targets NVARCHAR(255),
    Type NVARCHAR(20),

    CONSTRAINT PK_AnnotationSettings PRIMARY KEY (Id),
    CONSTRAINT FK_AnnotationSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);
CREATE INDEX IX_AnnotationSettings_RunId ON targetedms.AnnotationSettings (RunId);