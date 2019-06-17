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
/* targetedms-16.10-16.11.sql */

/* The run related count values are now calculated by the server in TargetedMSSchema.getTargetedMSRunsTable */
--EXEC core.fn_dropifexists 'Runs', 'targetedms', 'DEFAULT', 'PeptideGroupCount';
--ALTER TABLE targetedms.Runs DROP COLUMN PeptideGroupCount;
EXEC core.fn_dropifexists 'Runs', 'targetedms', 'DEFAULT', 'PeptideCount';
ALTER TABLE targetedms.Runs DROP COLUMN PeptideCount;
EXEC core.fn_dropifexists 'Runs', 'targetedms', 'DEFAULT', 'PrecursorCount';
ALTER TABLE targetedms.Runs DROP COLUMN PrecursorCount;
EXEC core.fn_dropifexists 'Runs', 'targetedms', 'DEFAULT', 'TransitionCount';
ALTER TABLE targetedms.Runs DROP COLUMN TransitionCount;

/* targetedms-16.11-16.12.sql */

/* missed in sqlserver version of targetedms-16.10-16.11.sql */
EXEC core.fn_dropifexists 'Runs', 'targetedms', 'DEFAULT', 'PeptideGroupCount';
ALTER TABLE targetedms.Runs DROP COLUMN PeptideGroupCount;

/* targetedms-16.12-16.13.sql */

ALTER TABLE targetedms.Peptide ALTER COLUMN Sequence NVARCHAR(300);
ALTER TABLE targetedms.Peptide ALTER COLUMN PeptideModifiedSequence NVARCHAR(500);
ALTER TABLE targetedms.Precursor ALTER COLUMN ModifiedSequence NVARCHAR(2500);
ALTER TABLE targetedms.iRTPeptide ALTER COLUMN ModifiedSequence NVARCHAR(500);

/* targetedms-16.13-16.14.sql */

ALTER TABLE targetedms.Peptide ALTER COLUMN CalcNeutralMass FLOAT;
ALTER TABLE targetedms.Precursor ALTER COLUMN NeutralMass FLOAT;

/* targetedms-16.14-16.15.sql */

-- Skyline-daily 3.5.1.9426 (and patch release of Skyline 3.5) changed the format of the modified_sequence attribute
-- of the <precursor> element to always have a decimal place in the modification mass string.
-- Example: [+80.0] instead of [+80].
-- Replace strings like [+80] in the modified sequence with [+80.0].
-- Example: K[+96.2]VN[-17]K[+34.1]TES[+80]K[+62.1] -> K[+96.2]VN[-17.0]K[+34.1]TES[+80.0]K[+62.1]
EXEC core.executeJavaUpgradeCode 'updatePrecursorModifiedSequence';