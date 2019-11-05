ALTER TABLE targetedms.Runs ADD AuditLogEntriesCount INT DEFAULT 0 NOT NULL;

declare @rootIdentity ENTITYID;
select @rootIdentity = [EntityId] FROM [core].[Containers] WHERE Parent is null

INSERT INTO targetedms.QCMetricConfiguration (Container, Name, Series1Label, Series1SchemaName, Series1QueryName, PrecursorScoped)
VALUES (@rootIdentity, 'TIC Area','TIC Area','targetedms','QCRunMetric_ticArea', 0);

ALTER TABLE targetedms.SampleFile ADD TicArea DOUBLE PRECISION;

ALTER TABLE targetedms.GeneralMolecule ADD AttributeGroupId NVARCHAR(100);

ALTER TABLE targetedms.Replicate ALTER COLUMN Name NVARCHAR(200) NOT NULL;

ALTER TABLE targetedms.SampleFile ADD InstrumentSerialNumber NVARCHAR(200);
ALTER TABLE targetedms.SampleFile ADD SampleId NVARCHAR(200);

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

ALTER TABLE targetedms.QCMetricConfiguration ADD EnabledQueryName NVARCHAR(200);
ALTER TABLE targetedms.QCMetricConfiguration ADD EnabledSchemaName NVARCHAR(200);
GO

declare @rootIdentity ENTITYID;
select @rootIdentity = [EntityId] FROM [core].[Containers] WHERE Parent is null

INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName,Series2Label,Series2SchemaName,Series2QueryName,PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
  (@rootIdentity, 'Isotopologue LOD', 'LOD','targetedms', 'QCMetric_IsotopologuePrecursorLOD', NULL, NULL, NULL, 1, 'QCMetricEnabled_IsotopologuePrecursorLOD', 'targetedms');

INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName,Series2Label,Series2SchemaName,Series2QueryName,PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
  (@rootIdentity, 'Isotopologue LOQ', 'LOQ', 'targetedms', 'QCMetric_IsotopologuePrecursorLOQ', NULL, NULL, NULL, 1, 'QCMetricEnabled_IsotopologuePrecursorLOQ', 'targetedms');

INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName,Series2Label,Series2SchemaName,Series2QueryName,PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
  (@rootIdentity, 'Isotopologue Accuracy', 'Accuracy', 'targetedms', 'QCMetric_IsotopologuePrecursorAccuracy', NULL, NULL, NULL, 1, 'QCMetricEnabled_IsotopologuePrecursorAccuracy', 'targetedms');

INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName,Series2Label,Series2SchemaName,Series2QueryName,PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
  (@rootIdentity, 'Isotopologue Regression RSquared', 'Coefficient', 'targetedms', 'QCMetric_IsotopologuePrecursorRSquared', NULL, NULL, NULL, 1, 'QCMetricEnabled_IsotopologuePrecursorRSquared', 'targetedms');

UPDATE targetedms.QCMetricConfiguration SET EnabledQueryName = 'QCMetricEnabled_lhRatio', EnabledSchemaName ='targetedms' WHERE Series1QueryName = 'QCMetric_lhRatio';

ALTER TABLE targetedms.runs ALTER COLUMN SoftwareVersion NVARCHAR(200);

ALTER TABLE targetedms.Runs ADD ListCount INT DEFAULT 0 NOT NULL;

ALTER TABLE targetedms.AnnotationSettings ADD Lookup NVARCHAR(MAX);