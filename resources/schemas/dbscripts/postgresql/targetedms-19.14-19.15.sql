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

CREATE TABLE targetedms.AuditLogEntry (
    entryId serial not null,
    documentGUID entityid not null,
    entryHash varchar(100) not null,
    versionId int4 null,
    createTimestamp timestamp not null,
    timezoneOffset int4 not null,
    userName  varchar(100) not null,
    formatVersion varchar(100) not null,
    parentEntryHash varchar(100) null,
    reason varchar(1000) NULL,
    extraInfo varchar NULL,
    CONSTRAINT pk_auditLogEntry PRIMARY KEY (entryId),
    CONSTRAINT fk_auditLogEntry_runs FOREIGN KEY (versionId) REFERENCES targetedms.runs(id)
);

CREATE UNIQUE INDEX uix_auditLogEntry_document on targetedms.AuditLogEntry USING btree (documentGUID, entryHash);


CREATE TABLE targetedms.AuditLogMessage(
  messageId serial not null,
  orderNumber int4 not null,
  entryId int4 not null,
  messageType varchar(50) not null,
  enText varchar null,
  expandedText varchar null,
  reason varchar(1000) null,
  CONSTRAINT pk_auditLogMessage PRIMARY KEY (messageId),
  CONSTRAINT fk_auditLogMessage_entry FOREIGN KEY (entryId) REFERENCES targetedms.AuditLogEntry(entryId)
);

CREATE UNIQUE INDEX uix_auditLogMessage_entry on targetedms.AuditLogMessage USING btree (entryId, orderNumber);
