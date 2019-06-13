/*
 * Copyright (c) 2015-2019 LabKey Corporation
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

/* targetedms-13.20-13.21.sql */

INSERT INTO prot.identTypes (name, entryDate) SELECT 'Skyline', current_timestamp
WHERE NOT EXISTS (SELECT 1 from prot.identTypes where name='Skyline');

/* targetedms-13.21-13.22.sql */

ALTER TABLE targetedms.PeptideGroup ADD Name NVARCHAR(255);

ALTER TABLE targetedms.PeptideGroup ALTER COLUMN Note TEXT;
ALTER TABLE targetedms.Peptide ALTER COLUMN Note TEXT;
ALTER TABLE targetedms.Precursor ALTER COLUMN Note TEXT;
ALTER TABLE targetedms.PrecursorChromInfo ALTER COLUMN Note TEXT;
ALTER TABLE targetedms.Transition ALTER COLUMN Note TEXT;
ALTER TABLE targetedms.TransitionChromInfo ALTER COLUMN Note TEXT;

/* targetedms-13.22-13.23.sql */

ALTER TABLE targetedms.precursor ALTER COLUMN modifiedSequence NVARCHAR(300);

ALTER TABLE targetedms.isotopemodification DROP COLUMN explicitmod;
ALTER TABLE targetedms.runisotopemodification DROP CONSTRAINT pk_runisotopemodification;
ALTER TABLE targetedms.runisotopemodification ADD CONSTRAINT pk_runisotopemodification PRIMARY KEY (isotopemodid, runid);