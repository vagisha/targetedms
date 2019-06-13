/*
 * Copyright (c) 2016-2019 LabKey Corporation
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

/* targetedms-14.20-14.21.sql */

ALTER TABLE targetedms.Enzyme ALTER COLUMN Cut NVARCHAR(10) NULL;
ALTER TABLE targetedms.Enzyme ALTER COLUMN Sense NVARCHAR(10) NULL;
ALTER TABLE targetedms.Enzyme ADD CutC NVARCHAR(10);
ALTER TABLE targetedms.Enzyme ADD NoCutC NVARCHAR(10);
ALTER TABLE targetedms.Enzyme ADD CutN NVARCHAR(10);
ALTER TABLE targetedms.Enzyme ADD NoCutN NVARCHAR(10);

/* targetedms-14.21-14.22.sql */

ALTER TABLE targetedms.PeptideGroup ADD AltDescription TEXT;

ALTER TABLE targetedms.Transition ADD MeasuredIonName VARCHAR(20);

/* targetedms-14.22-14.23.sql */

ALTER TABLE targetedms.PrecursorChromInfo ADD UncompressedSize INT;

/* targetedms-14.23-14.24.sql */

ALTER TABLE targetedms.ExperimentAnnotations ADD ExperimentId INT NOT NULL DEFAULT 0;
ALTER TABLE targetedms.ExperimentAnnotations ADD JournalCopy BIT NOT NULL DEFAULT 0;
ALTER TABLE targetedms.ExperimentAnnotations ADD IncludeSubfolders BIT NOT NULL DEFAULT 0;
CREATE INDEX IX_ExperimentAnnotations_ExperimentId ON targetedms.ExperimentAnnotations(ExperimentId);

DROP TABLE targetedms.ExperimentAnnotationsRun;
DELETE FROM targetedms.ExperimentAnnotations WHERE Id NOT IN
(SELECT ea.Id from targetedms.ExperimentAnnotations AS ea INNER JOIN core.Containers AS c ON ea.Container = c.entityId);

/* Run Java code to create a new entry in exp.experiment for each entry in targetedms.ExperimentAnnotations */
EXEC core.executeJavaUpgradeCode 'updateExperimentAnnotations'
GO

ALTER TABLE targetedms.ExperimentAnnotations ADD CONSTRAINT FK_ExperimentAnnotations_Experiment FOREIGN KEY (ExperimentId) REFERENCES exp.Experiment(RowId);
ALTER TABLE targetedms.ExperimentAnnotations ADD CONSTRAINT FK_ExperimentAnnotations_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId);

/* Add all runs in an experiment's container to exp.runlist */
INSERT INTO exp.runlist (experimentid, experimentrunid, created, createdby)
(SELECT e.rowid, er.rowId, e.Created, e.CreatedBy
FROM targetedms.ExperimentAnnotations ea
INNER JOIN exp.experiment e ON (e.rowid = ea.ExperimentId)
INNER JOIN exp.experimentrun er ON (er.container = ea.Container));



CREATE TABLE targetedms.Journal
(
    _ts TIMESTAMP,
    CreatedBy USERID,
    Created DATETIME,
    ModifiedBy USERID,
    Modified DATETIME,

    Id INT IDENTITY(1, 1) NOT NULL,
    Name NVARCHAR(255) NOT NULL,
    LabkeyGroupId INT NOT NULL,
    Project EntityId NOT NULL,

    CONSTRAINT PK_Journal PRIMARY KEY (Id),
    CONSTRAINT UQ_Journal_Name UNIQUE(Name),
    CONSTRAINT FK_Journal_Principals FOREIGN KEY (LabkeyGroupId) REFERENCES core.Principals(UserId),
    CONSTRAINT FK_Journal_Containers FOREIGN KEY (Project) REFERENCES core.Containers(EntityId)
);
CREATE INDEX IX_Journal_LabkeyGroupId ON targetedms.Journal(LabkeyGroupId);
CREATE INDEX IX_Journal_Project ON targetedms.Journal(Project);

CREATE TABLE targetedms.JournalExperiment
(
    _ts TIMESTAMP,
    CreatedBy USERID,
    Created DATETIME,

    JournalId INT NOT NULL,
    ExperimentAnnotationsId INT NOT NULL,
    ShortAccessURL EntityId NOT NULL,
    ShortCopyURL EntityId NOT NULL,
    Copied DATETIME,


    CONSTRAINT PK_JournalExperiment PRIMARY KEY (JournalId, ExperimentAnnotationsId),
    CONSTRAINT FK_JournalExperiment_Journal FOREIGN KEY (JournalId) REFERENCES targetedms.Journal(Id),
    CONSTRAINT FK_JournalExperiment_ExperimentAnnotations FOREIGN KEY (ExperimentAnnotationsId) REFERENCES targetedms.ExperimentAnnotations(Id),
    CONSTRAINT FK_JournalExperiment_ShortUrl_Access FOREIGN KEY (ShortAccessURL) REFERENCES core.ShortUrl(EntityId),
    CONSTRAINT FK_JournalExperiment_ShortUrl_Copy FOREIGN KEY (ShortCopyURL) REFERENCES core.ShortUrl(EntityId)
);
CREATE INDEX IX_JournalExperiment_ShortAccessURL ON targetedms.JournalExperiment(ShortAccessURL);
CREATE INDEX IX_JournalExperiment_ShortCopyURL ON targetedms.JournalExperiment(ShortCopyURL);

/* targetedms-14.24-14.25.sql */

--TransitionChromInfo's Identified column can now be one of 'true', 'false' or 'aligned'
ALTER TABLE targetedms.TransitionChromInfo ADD Identified_temp NVARCHAR(20);
GO
UPDATE targetedms.TransitionChromInfo SET Identified_temp = (CASE WHEN Identified = 1 THEN 'true'
                                                          WHEN Identified = 0 THEN 'false'
                                                          ELSE NULL END);
GO
ALTER TABLE targetedms.TransitionChromInfo DROP COLUMN Identified;
GO
EXEC sp_rename 'targetedms.TransitionChromInfo.Identified_temp', 'Identified', 'COLUMN';


--PrecursorChromInfo's Identified column can now be one of 'true', 'false' or 'aligned'
ALTER TABLE targetedms.PrecursorChromInfo ADD Identified_temp NVARCHAR(20);
GO
UPDATE targetedms.PrecursorChromInfo SET Identified_temp = (CASE WHEN Identified = 1 THEN 'true'
                                                          WHEN Identified = 0 THEN 'false'
                                                          ELSE NULL END);
GO
ALTER TABLE targetedms.PrecursorChromInfo DROP COLUMN Identified;
GO
EXEC sp_rename 'targetedms.PrecursorChromInfo.Identified_temp', 'Identified', 'COLUMN';