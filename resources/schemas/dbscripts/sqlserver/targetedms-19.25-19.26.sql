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

CREATE TABLE targetedms.ListDefinition
(
    Id INT IDENTITY(1,1) NOT NULL,
    RunId INT NOT NULL,
    Name NVARCHAR(max) NOT NULL,
    PkColumnIndex INT NULL,
    DisplayColumnIndex INT NULL,
    CONSTRAINT PK_List PRIMARY KEY(Id),
    CONSTRAINT FK_List_RunId FOREIGN KEY(RunId) REFERENCES targetedms.Runs(Id)
);
CREATE TABLE targetedms.ListColumnDefinition
(
    Id INT IDENTITY(1,1) NOT NULL,
    ListDefinitionId INT NOT NULL,
    ColumnIndex INT NOT NULL,
    AnnotationType NVARCHAR(20) NOT NULL,
    Name NVARCHAR(max) NOT NULL,
    Lookup NVARCHAR(max) NULL,
    CONSTRAINT PK_ListColumn PRIMARY KEY(Id),
    CONSTRAINT FK_ListColumn_ListDefinitionId FOREIGN KEY(ListDefinitionId) REFERENCES targetedms.ListDefinition(Id),
    CONSTRAINT UQ_ListColumn_ListDefinitionId_ColumnIndex UNIQUE(ListDefinitionId, ColumnIndex)
);

CREATE TABLE targetedms.ListItem
(
    Id INT IDENTITY(1,1)  NOT NULL,
    ListDefinitionId INT NOT NULL,
    CONSTRAINT PK_ListItem PRIMARY KEY(Id),
    CONSTRAINT FK_ListItem_ListDefinitionId FOREIGN KEY(ListDefinitionId) REFERENCES targetedms.ListDefinition(Id)
);

CREATE TABLE targetedms.ListItemValue
(
    Id INT IDENTITY(1,1)  NOT NULL,
    ListItemId INT NOT NULL,
    ColumnIndex INT NOT NULL,
    TextValue NVARCHAR(max) NULL,
    NumericValue FLOAT NULL,
    CONSTRAINT PK_ListItemValue PRIMARY KEY(Id),
    CONSTRAINT FK_ListItemValue_ListItem FOREIGN KEY(ListItemId) REFERENCES targetedms.ListItem(Id),
    CONSTRAINT UQ_ListItemValue_ListItemId_ColumnIndex UNIQUE(ListItemId, ColumnIndex)
);
