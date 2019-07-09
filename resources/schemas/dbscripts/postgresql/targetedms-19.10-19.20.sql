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
CREATE TABLE targetedms.QCEnabledMetrics
(
  metric          INT,
  enabled         BIT,
  lowerBound      DOUBLE PRECISION,
  upperBound      DOUBLE PRECISION,
  cusumLimit      DOUBLE PRECISION,

  Created         TIMESTAMP,
  CreatedBy       USERID,
  Modified        TIMESTAMP,
  ModifiedBy      USERID,
  Container       ENTITYID NOT NULL,

  CONSTRAINT PK_QCEnabledMetrics PRIMARY KEY (metric),
  CONSTRAINT FK_QCEnabledMetrics_Metric FOREIGN KEY (metric) REFERENCES targetedms.qcmetricconfiguration(Id),
  CONSTRAINT FK_QCEnabledMetrics_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId)
);

DROP TABLE targetedms.QCEnabledMetrics;

CREATE TABLE targetedms.QCEnabledMetrics
(
  metric          INT,
  enabled         BOOLEAN,
  lowerBound      DOUBLE PRECISION,
  upperBound      DOUBLE PRECISION,
  cusumLimit      DOUBLE PRECISION,

  Created         TIMESTAMP,
  CreatedBy       USERID,
  Modified        TIMESTAMP,
  ModifiedBy      USERID,
  Container       ENTITYID NOT NULL,

  CONSTRAINT PK_QCEnabledMetrics PRIMARY KEY (metric),
  CONSTRAINT FK_QCEnabledMetrics_Metric FOREIGN KEY (metric) REFERENCES targetedms.qcmetricconfiguration(Id),
  CONSTRAINT FK_QCEnabledMetrics_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId)
);

CREATE INDEX IX_targetedms_qcEnabledMetrics_Container ON targetedms.QCEnabledMetrics (Container);

ALTER TABLE targetedms.QCEnabledMetrics DROP CONSTRAINT PK_QCEnabledMetrics;

ALTER TABLE targetedms.QCEnabledMetrics ADD CONSTRAINT PK_QCEnabledMetrics PRIMARY KEY (metric, Container);

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

UPDATE targetedms.qcmetricconfiguration SET name='Transition & Precursor Areas' WHERE name='Transition/Precursor Areas';

ALTER TABLE targetedms.qcmetricconfiguration ADD COLUMN PrecursorScoped BOOLEAN NOT NULL DEFAULT TRUE;

DROP TABLE IF EXISTS targetedms.QCEmailNotifications;

-- Increase the length of the Gene column. The gene field can contain all possible gene names that a protein product is associated with. This can get really long.
ALTER TABLE targetedms.PeptideGroup ALTER COLUMN gene TYPE VARCHAR(2000);

CREATE INDEX uix_auditLogEntry_version on targetedms.AuditLogEntry USING btree (versionId);

-- ----------------------------------------------------------------------------
-- Updates to MeasuredDriftTime
-- ----------------------------------------------------------------------------
ALTER TABLE targetedms.MeasuredDriftTime ALTER COLUMN DriftTime DROP NOT NULL;
-- From Brian Pratt about the charge field: either a simple number or an addition description-
-- 1, -4, [M+H]. But no hard limit to adduct string. Typically short though.
-- Longest one there seems to be [M+IsoProp+Na+H] (17 characters) though most come in below 10
ALTER TABLE targetedms.MeasuredDriftTime ALTER COLUMN Charge TYPE VARCHAR(30);
ALTER TABLE targetedms.MeasuredDriftTime ADD Ccs DOUBLE PRECISION;
ALTER TABLE targetedms.MeasuredDriftTime ADD IonMobility DOUBLE PRECISION;
ALTER TABLE targetedms.MeasuredDriftTime ADD HighEnergyIonMobilityOffset DOUBLE PRECISION;
-- From Brian Pratt about the ion_mobility_units field: Worst case is 23 characters, for Bruker:  inverse_K0_Vsec_per_cm2
ALTER TABLE targetedms.MeasuredDriftTime ADD IonMobilityUnits VARCHAR(30);