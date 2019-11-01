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

ALTER TABLE targetedms.QCMetricConfiguration ADD COLUMN EnabledQueryName VARCHAR(200);
ALTER TABLE targetedms.QCMetricConfiguration ADD COLUMN EnabledSchemaName VARCHAR(200);

WITH rootIdentity as (select EntityId as theIdentity FROM core.Containers WHERE Parent is null)
INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName,Series2Label,Series2SchemaName,Series2QueryName,PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
  ((select theIdentity from rootIdentity), 'Isotopologue LOD', 'LOD','targetedms', 'QCMetric_IsotopologuePrecursorLOD', NULL, NULL, NULL, TRUE, 'QCMetricEnabled_IsotopologuePrecursorLOD', 'targetedms');

WITH rootIdentity as (select EntityId as theIdentity FROM core.Containers WHERE Parent is null)
INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName,Series2Label,Series2SchemaName,Series2QueryName,PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
  ((select theIdentity from rootIdentity), 'Isotopologue LOQ', 'LOQ', 'targetedms', 'QCMetric_IsotopologuePrecursorLOQ', NULL, NULL, NULL, TRUE, 'QCMetricEnabled_IsotopologuePrecursorLOQ', 'targetedms');

WITH rootIdentity as (select EntityId as theIdentity FROM core.Containers WHERE Parent is null)
INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName,Series2Label,Series2SchemaName,Series2QueryName,PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
  ((select theIdentity from rootIdentity), 'Isotopologue Accuracy', 'Accuracy', 'targetedms', 'QCMetric_IsotopologuePrecursorAccuracy', NULL, NULL, NULL, TRUE, 'QCMetricEnabled_IsotopologuePrecursorAccuracy', 'targetedms');

WITH rootIdentity as (select EntityId as theIdentity FROM core.Containers WHERE Parent is null)
INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName,Series2Label,Series2SchemaName,Series2QueryName,PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
  ((select theIdentity from rootIdentity), 'Isotopologue Regression RSquared', 'Coefficient', 'targetedms', 'QCMetric_IsotopologuePrecursorRSquared', NULL, NULL, NULL, TRUE, 'QCMetricEnabled_IsotopologuePrecursorRSquared', 'targetedms');

UPDATE targetedms.QCMetricConfiguration SET EnabledQueryName = 'QCMetricEnabled_lhRatio', EnabledSchemaName ='targetedms' WHERE Series1QueryName = 'QCMetric_lhRatio';

ALTER TABLE targetedms.runs ALTER COLUMN SoftwareVersion TYPE VARCHAR(200);