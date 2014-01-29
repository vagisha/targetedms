/*
 * Copyright (c) 2013 LabKey Corporation
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

CREATE TABLE targetedms.ExperimentAnnotations
(
    -- standard fields
    _ts TIMESTAMP,
    Id INT IDENTITY(1, 1) NOT NULL,
    Container ENTITYID NOT NULL,
    CreatedBy USERID,
    Created DATETIME,
    ModifiedBy USERID,
    Modified DATETIME,

    Title NVARCHAR(250),
    Organism NVARCHAR(100),
    ExperimentDescription NVARCHAR(MAX),
    SampleDescription NVARCHAR(MAX),
    Instrument NVARCHAR(250),
    SpikeIn BIT,
    Citation NVARCHAR(MAX),
    Abstract NVARCHAR(MAX),
    PublicationLink NVARCHAR(MAX)

    CONSTRAINT PK_ExperimentAnnotations PRIMARY KEY (Id)
);
CREATE INDEX IX_ExperimentAnnotations_Container ON targetedms.ExperimentAnnotations (Container);

CREATE TABLE targetedms.ExperimentAnnotationsRun
(
  Id INT IDENTITY(1, 1) NOT NULL,
  RunId INT NOT NULL,
  ExperimentAnnotationsId INT NOT NULL,
  CreatedBy USERID,
  Created TIMESTAMP,

  CONSTRAINT PK_ExperimentAnnotationsRun PRIMARY KEY (Id),
  CONSTRAINT FK_ExperimentAnnotationsRun_ExperimentAnnotationsId FOREIGN KEY (ExperimentAnnotationsId) REFERENCES targetedms.ExperimentAnnotations(Id),
  CONSTRAINT FK_ExperimentAnnotationsRun_RunId FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);
CREATE INDEX IX_ExperimentAnnotationsRun_RunId ON targetedms.ExperimentAnnotationsRun (RunId);
CREATE INDEX IX_ExperimentAnnotationsRun_ExperimentAnnotationsId ON targetedms.ExperimentAnnotationsRun (ExperimentAnnotationsId);


