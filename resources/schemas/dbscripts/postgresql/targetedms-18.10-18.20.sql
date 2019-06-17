/*
 * Copyright (c) 2018-2019 LabKey Corporation
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

/* targetedms-18.10-18.11.sql */

ALTER TABLE targetedms.Runs ADD COLUMN SkydDataId INT;

ALTER TABLE targetedms.Runs ADD CONSTRAINT FK_Runs_SkydData FOREIGN KEY (SkydDataId) REFERENCES exp.Data(RowId);


ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN ChromatogramOffset BIGINT;
ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN ChromatogramLength INT;

/* targetedms-18.11-18.12.sql */

ALTER TABLE targetedms.Runs ADD COLUMN CalibrationCurveCount INT;
UPDATE targetedms.Runs SET CalibrationCurveCount = (SELECT COUNT(c.id) FROM targetedms.CalibrationCurve c WHERE c.RunId = targetedms.Runs.Id);

ALTER TABLE targetedms.GeneralMoleculeChromInfo ADD COLUMN ExcludeFromCalibration BOOLEAN;
UPDATE targetedms.GeneralMoleculeChromInfo SET ExcludeFromCalibration = false;

/* targetedms-18.12-18.13.sql */

ALTER TABLE targetedms.QuantificationSettings ADD COLUMN MaxLOQBias FLOAT;
ALTER TABLE targetedms.QuantificationSettings ADD COLUMN MaxLOQCV FLOAT;
ALTER TABLE targetedms.QuantificationSettings ADD COLUMN LODCalculation VARCHAR(50);

/* targetedms-18.13-18.14.sql */

ALTER TABLE targetedms.ExperimentAnnotations ADD COLUMN Keywords VARCHAR(200);
ALTER TABLE targetedms.ExperimentAnnotations ADD COLUMN LabHead USERID;
ALTER TABLE targetedms.ExperimentAnnotations ADD COLUMN LabHeadAffiliation VARCHAR(200);
ALTER TABLE targetedms.ExperimentAnnotations ADD COLUMN Submitter USERID;
ALTER TABLE targetedms.ExperimentAnnotations ADD COLUMN SubmitterAffiliation VARCHAR(200);
ALTER TABLE targetedms.ExperimentAnnotations ADD COLUMN pxid VARCHAR(10);

ALTER TABLE targetedms.JournalExperiment ADD COLUMN PxidRequested BOOLEAN NOT NULL DEFAULT '0';
ALTER TABLE targetedms.JournalExperiment ADD COLUMN KeepPrivate BOOLEAN NOT NULL DEFAULT '1';

/* targetedms-18.14-18.15.sql */

ALTER TABLE targetedms.spectrumlibrary ALTER COLUMN Name TYPE VARCHAR(400);