/*
 * Copyright (c) 2014-2019 LabKey Corporation
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

--TransitionChromInfo UserSet can now be one of 'TRUE', 'FALSE', 'IMPORTED', 'REINTEGRATE'
ALTER TABLE targetedms.TransitionChromInfo ADD UserSet_temp NVARCHAR(20);
GO
UPDATE targetedms.TransitionChromInfo SET UserSet_temp = (CASE WHEN UserSet = 1 THEN 'TRUE'
                                                          WHEN UserSet = 0 THEN 'FALSE'
                                                          ELSE NULL END);
GO
ALTER TABLE targetedms.TransitionChromInfo DROP COLUMN UserSet;
GO
EXEC sp_rename 'targetedms.TransitionChromInfo.UserSet_temp', 'UserSet', 'COLUMN';



--PrecursorChromInfo UserSet can now be one of 'TRUE', 'FALSE', 'IMPORTED', 'REINTEGRATE'
ALTER TABLE targetedms.PrecursorChromInfo ADD UserSet_temp NVARCHAR(20);
GO
UPDATE targetedms.PrecursorChromInfo SET UserSet_temp = (CASE WHEN UserSet = 1 THEN 'TRUE'
                                                          WHEN UserSet = 0 THEN 'FALSE'
                                                          ELSE NULL END);
GO
ALTER TABLE targetedms.PrecursorChromInfo DROP COLUMN UserSet;
GO
EXEC sp_rename 'targetedms.PrecursorChromInfo.UserSet_temp', 'UserSet', 'COLUMN';



-- Add ion mobility settings tables
CREATE TABLE targetedms.DriftTimePredictionSettings
(
    Id INT IDENTITY(1, 1) NOT NULL,
    RunId INT NOT NULL,
    UseSpectralLibraryDriftTimes BIT,
    SpectralLibraryDriftTimesResolvingPower REAL,
    PredictorName NVARCHAR(200),
    ResolvingPower REAL,

    CONSTRAINT PK_DriftTimePredictionSettings PRIMARY KEY (Id),
    CONSTRAINT FK_DriftTimePredictionSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);
CREATE INDEX IX_DriftTimePredictionSettings_RunId ON targetedms.DriftTimePredictionSettings(RunId);

CREATE TABLE targetedms.MeasuredDriftTime
(
    Id INT IDENTITY(1, 1) NOT NULL,
    DriftTimePredictionSettingsId INT NOT NULL,
    ModifiedSequence NVARCHAR(255) NOT NULL,
    Charge INT NOT NULL,
    DriftTime REAL NOT NULL,

    CONSTRAINT PK_MeasuredDriftTime PRIMARY KEY (Id),
    CONSTRAINT FK_MeasuredDriftTime_DriftTimePredictionSettings FOREIGN KEY (DriftTimePredictionSettingsId) REFERENCES targetedms.DriftTimePredictionSettings(Id)
);
CREATE INDEX IX_MeasuredDriftTime_DriftTimePredictionSettingsId ON targetedms.MeasuredDriftTime(DriftTimePredictionSettingsId);