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

UPDATE a set a.sourceapplicationid = (SELECT d.sourceapplicationid from exp.data d where d.runid = a.runid AND d.name LIKE '%.zip')
FROM exp.data AS a
WHERE a.sourceapplicationid IS NULL AND a.runid IS NOT NULL AND a.name LIKE '%.skyd';

-- Add a column to store the size of the Skyline document
ALTER TABLE targetedms.runs ADD DocumentSize BIGINT;

-- Populate the DocumentSize column for existing rows in targetedms.runs
EXEC core.executeJavaUpgradeCode 'addDocumentSize';

ALTER TABLE targetedms.precursorchrominfo ADD qvalue REAL;
ALTER TABLE targetedms.precursorchrominfo ADD zscore REAL;