/*
 * Copyright (c) 2017 LabKey Corporation
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

ALTER TABLE targetedms.Runs ADD SkydDataId INT;

ALTER TABLE targetedms.Runs ADD CONSTRAINT FK_Runs_SkydData FOREIGN KEY (SkydDataId) REFERENCES exp.Data(RowId);

ALTER TABLE targetedms.PrecursorChromInfo ADD ChromatogramOffset BIGINT;
ALTER TABLE targetedms.PrecursorChromInfo ADD ChromatogramLength INT;

/* targetedms-18.11-18.12.sql */

ALTER TABLE targetedms.Runs ADD CalibrationCurveCount INT;

GO

UPDATE targetedms.Runs SET CalibrationCurveCount = (SELECT COUNT(c.id) FROM targetedms.CalibrationCurve c WHERE c.RunId = targetedms.Runs.Id);

ALTER TABLE targetedms.GeneralMoleculeChromInfo ADD ExcludeFromCalibration BIT;

GO

UPDATE targetedms.GeneralMoleculeChromInfo SET ExcludeFromCalibration = 0;

/* targetedms-18.12-18.13.sql */

ALTER TABLE targetedms.QuantificationSettings ADD MaxLOQBias FLOAT;
ALTER TABLE targetedms.QuantificationSettings ADD MaxLOQCV FLOAT;
ALTER TABLE targetedms.QuantificationSettings ADD LODCalculation NVARCHAR(50);

/* targetedms-18.13-18.14.sql */

ALTER TABLE targetedms.ExperimentAnnotations ADD Keywords NVARCHAR(200);
ALTER TABLE targetedms.ExperimentAnnotations ADD LabHead USERID;
ALTER TABLE targetedms.ExperimentAnnotations ADD LabHeadAffiliation NVARCHAR(200);
ALTER TABLE targetedms.ExperimentAnnotations ADD Submitter USERID;
ALTER TABLE targetedms.ExperimentAnnotations ADD SubmitterAffiliation NVARCHAR(200);
ALTER TABLE targetedms.ExperimentAnnotations ADD pxid NVARCHAR(10);

ALTER TABLE targetedms.JournalExperiment ADD PxidRequested BIT NOT NULL DEFAULT '0';
ALTER TABLE targetedms.JournalExperiment ADD KeepPrivate BIT NOT NULL DEFAULT '1';

/* targetedms-18.14-18.15.sql */

ALTER TABLE targetedms.spectrumlibrary ALTER COLUMN Name NVARCHAR(400);