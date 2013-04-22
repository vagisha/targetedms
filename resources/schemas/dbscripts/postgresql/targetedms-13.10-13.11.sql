/*
 * Copyright (c) 2013 LabKey Corporation
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

ALTER TABLE targetedms.Peptide ADD COLUMN PeptideModifiedSequence VARCHAR(255);
ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN MaxHeight REAL;
ALTER TABLE targetedms.PrecursorChromInfo RENAME COLUMN LibraryDtop TO LibraryDotp;
ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN IsotopeDotp REAL;
ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN AverageMassErrorPPM REAL;
ALTER TABLE targetedms.TransitionChromInfo ADD COLUMN MassErrorPPM REAL;
