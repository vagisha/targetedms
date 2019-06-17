/*
 * Copyright (c) 2017-2019 LabKey Corporation
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

/* targetedms-15.20-15.21.sql */

/* Store the iRT database file path */
ALTER TABLE targetedms.RetentionTimePredictionSettings ADD COLUMN IrtDatabasePath VARCHAR(500);

ALTER TABLE targetedms.PeptideGroup ALTER COLUMN gene TYPE VARCHAR(500);
ALTER TABLE targetedms.PeptideGroup ALTER COLUMN species TYPE VARCHAR(255);
ALTER TABLE targetedms.predictor ALTER COLUMN stepSize TYPE REAL;