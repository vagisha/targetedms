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

ALTER TABLE targetedms.Runs ADD CalibrationCurveCount INT;

GO

UPDATE targetedms.Runs SET CalibrationCurveCount = (SELECT COUNT(c.id) FROM targetedms.CalibrationCurve c WHERE c.RunId = targetedms.Runs.Id);

ALTER TABLE targetedms.GeneralMoleculeChromInfo ADD ExcludeFromCalibration BIT;

GO

UPDATE targetedms.GeneralMoleculeChromInfo SET ExcludeFromCalibration = 0;
