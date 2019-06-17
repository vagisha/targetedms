/*
 * Copyright (c) 2016-2019 LabKey Corporation
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

/* The run related count values are now calculated by the server in TargetedMSSchema.getTargetedMSRunsTable */
ALTER TABLE targetedms.Runs DROP COLUMN PeptideGroupCount;
ALTER TABLE targetedms.Runs DROP COLUMN PeptideCount;
ALTER TABLE targetedms.Runs DROP COLUMN PrecursorCount;
ALTER TABLE targetedms.Runs DROP COLUMN TransitionCount;

/* FK from targetedms.Peptide to targetedms.GeneralMolecule wasn't applied (issue 25789) */
ALTER TABLE targetedms.Peptide ADD CONSTRAINT FK_Id_GMId FOREIGN KEY (Id) REFERENCES targetedms.GeneralMolecule (Id);