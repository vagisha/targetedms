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

-- Change from VARCHAR to NVARCHAR and TEXT to NVARCHAR(MAX)

-- Drop constraint so we can change the column
ALTER TABLE targetedms.QCAnnotationType DROP CONSTRAINT UQ_QCAnnotationType_ContainerName;

ALTER TABLE targetedms.QCAnnotationType ALTER COLUMN Name NVARCHAR(100);
ALTER TABLE targetedms.QCAnnotationType ALTER COLUMN Description NVARCHAR(MAX);

-- Re-add constraint so we can change the column
ALTER TABLE targetedms.QCAnnotationType ADD CONSTRAINT UQ_QCAnnotationType_ContainerName UNIQUE (Container, Name);

ALTER TABLE targetedms.QCAnnotation ALTER COLUMN Description NVARCHAR(MAX);
