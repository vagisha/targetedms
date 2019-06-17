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
--EXEC core.fn_dropifexists 'Runs', 'targetedms', 'DEFAULT', 'PeptideGroupCount';
--ALTER TABLE targetedms.Runs DROP COLUMN PeptideGroupCount;
EXEC core.fn_dropifexists 'Runs', 'targetedms', 'DEFAULT', 'PeptideCount';
ALTER TABLE targetedms.Runs DROP COLUMN PeptideCount;
EXEC core.fn_dropifexists 'Runs', 'targetedms', 'DEFAULT', 'PrecursorCount';
ALTER TABLE targetedms.Runs DROP COLUMN PrecursorCount;
EXEC core.fn_dropifexists 'Runs', 'targetedms', 'DEFAULT', 'TransitionCount';
ALTER TABLE targetedms.Runs DROP COLUMN TransitionCount;