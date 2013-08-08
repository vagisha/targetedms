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








