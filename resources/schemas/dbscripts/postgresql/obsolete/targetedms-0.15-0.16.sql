/*
 * Copyright (c) 2012-2013 LabKey Corporation
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

-- Remove RunId columns from StructuralModification and IsotopeModification tables.
-- Modifications are not specific to a single Skyline document, they may be used
-- by multiple documents.
ALTER TABLE targetedms.IsotopeModification DROP COLUMN RunId;
ALTER TABLE targetedms.StructuralModification DROP COLUMN RunId;

-- 'Formula' can be NULL
ALTER TABLE targetedms.IsotopeModification ALTER COLUMN Formula DROP NOT NULL;
ALTER TABLE targetedms.StructuralModification ALTER COLUMN Formula DROP NOT NULL;

-- Add IsotopeLabelId FK
ALTER TABLE targetedms.Precursor ADD CONSTRAINT FK_Precursor_IsotopeLabel FOREIGN KEY (IsotopeLabelId) REFERENCES targetedms.IsotopeLabel(Id);