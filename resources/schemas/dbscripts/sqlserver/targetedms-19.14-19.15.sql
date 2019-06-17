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
EXEC core.fn_dropifexists 'AuditLogEntry','targetedms','TABLE', NULL;

CREATE TABLE targetedms.AuditLogEntry (
    entryId INT IDENTITY(1, 1) not null,
    documentGUID ENTITYID not null,
    entryHash NVARCHAR(100) not null,
    versionId int null,
    createTimestamp DATETIME not null,
    timezoneOffset int not null,
    userName  NVARCHAR(100) not null,
    formatVersion NVARCHAR(100) not null,
    parentEntryHash NVARCHAR(100) null,
    reason NVARCHAR(1000) NULL,
    extraInfo NVARCHAR(max) NULL,
    CONSTRAINT pk_auditLogEntry PRIMARY KEY (entryId),
    CONSTRAINT fk_auditLogEntry_runs FOREIGN KEY (versionId) REFERENCES targetedms.runs(id)
);

CREATE UNIQUE INDEX uix_auditLogEntry_document on targetedms.AuditLogEntry(documentGUID, entryHash);

EXEC core.fn_dropifexists 'AuditLogMessage','targetedms','TABLE', NULL;

CREATE TABLE targetedms.AuditLogMessage(
  messageId INT IDENTITY(1, 1) not null,
  orderNumber int not null,
  entryId int not null,
  messageType NVARCHAR(50) not null,
  enText NVARCHAR(max) null,
  expandedText NVARCHAR(max) null,
  reason NVARCHAR(1000) null,
  CONSTRAINT pk_auditLogMessage PRIMARY KEY (messageId),
  CONSTRAINT fk_auditLogMessage_entry FOREIGN KEY (entryId) REFERENCES targetedms.AuditLogEntry(entryId)
);

CREATE UNIQUE INDEX uix_auditLogMessage_entry on targetedms.AuditLogMessage(entryId, orderNumber);
