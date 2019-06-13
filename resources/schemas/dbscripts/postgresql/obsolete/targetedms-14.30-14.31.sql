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

CREATE TABLE targetedms.QCAnnotationType
(
    Id SERIAL NOT NULL,
    Container ENTITYID NOT NULL,
    CreatedBy USERID,
    Created TIMESTAMP,
    ModifiedBy USERID,
    Modified TIMESTAMP,
    Name VARCHAR(100) NOT NULL,
    Description TEXT,
    Color VARCHAR(6) NOT NULL,

    CONSTRAINT PK_QCAnnotationType PRIMARY KEY (Id),
    CONSTRAINT FK_QCAnnotationType_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId),
    CONSTRAINT UQ_QCAnnotationType_ContainerName UNIQUE (Container, Name)
);

CREATE TABLE targetedms.QCAnnotation
(
    Id SERIAL NOT NULL,
    Container ENTITYID NOT NULL,
    CreatedBy USERID,
    Created TIMESTAMP,
    ModifiedBy USERID,
    Modified TIMESTAMP,
    QCAnnotationTypeId INT NOT NULL,
    Description TEXT NOT NULL,
    Date TIMESTAMP NOT NULL,

    CONSTRAINT PK_QCAnnotation PRIMARY KEY (Id),
    CONSTRAINT FK_QCAnnotation_QCAnnotationType FOREIGN KEY (QCAnnotationTypeId) REFERENCES targetedms.QCAnnotationType(Id)
);

-- Poke a few rows into the /Shared project
SELECT core.executeJavaUpgradeCode('populateDefaultAnnotationTypes');


