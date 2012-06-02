/*
 * Copyright (c) 2012 LabKey Corporation
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

-- Create schema, tables, indexes, and constraints used for TargetedMS module here
-- All SQL VIEW definitions should be created in targetedms-create.sql and dropped in targetedms-drop.sql
ALTER TABLE targetedms.TransitionAnnotation DROP COLUMN PrecursorId;

ALTER TABLE targetedms.TransitionAnnotation ADD COLUMN TransitionId INT NOT NULL;
ALTER TABLE targetedms.TransitionAnnotation ADD CONSTRAINT FK_TransitionAnnotation_Transition FOREIGN KEY (TransitionId) REFERENCES targetedms.Transition(Id);

CREATE INDEX IDX_transitionannotation_transitionid ON targetedms.TransitionAnnotation(TransitionId);
