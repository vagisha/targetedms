/*
 * Copyright (c) 2019 LabKey Corporation
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

CREATE TABLE targetedms.SampleFileChromInfo
(
  Id INT IDENTITY(1, 1) NOT NULL,
  SampleFileId integer NOT NULL,
  StartTime real,
  EndTime real,
  TextId NVARCHAR(512),
  NumPoints integer,
  UncompressedSize integer,
  Container entityid NOT NULL,
  ChromatogramFormat integer,
  ChromatogramOffset bigint,
  ChromatogramLength integer,
  CONSTRAINT PK_SampleFileChromInfo PRIMARY KEY (Id),
  CONSTRAINT FK_SampleFileChromInfo_SampleFile FOREIGN KEY (SampleFileId)
  REFERENCES targetedms.SampleFile(Id)
);

CREATE INDEX IDX_SampleFileChromInfo_Container ON targetedms.SampleFileChromInfo(container);

CREATE INDEX IDX_SampleFileChromInfo_SampleFileId ON targetedms.SampleFileChromInfo(samplefileid);

