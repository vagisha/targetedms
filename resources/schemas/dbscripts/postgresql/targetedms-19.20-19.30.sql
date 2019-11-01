ALTER TABLE targetedms.Runs ADD COLUMN AuditLogEntriesCount INT4 DEFAULT 0 NOT NULL;

WITH rootIdentity as (select EntityId as theIdentity FROM core.Containers WHERE Parent is null)

INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName,Series2Label,Series2SchemaName,Series2QueryName,PrecursorScoped) VALUES
((select theIdentity from rootIdentity), 'TIC Area','TIC Area','targetedms','QCRunMetric_ticArea',NULL , NULL , NULL , FALSE );

ALTER TABLE targetedms.SampleFile ADD COLUMN TicArea DOUBLE PRECISION;

ALTER TABLE targetedms.GeneralMolecule ADD COLUMN AttributeGroupId VARCHAR(100);

ALTER TABLE targetedms.Replicate ALTER COLUMN Name TYPE VARCHAR(200);

ALTER TABLE targetedms.SampleFile ADD COLUMN InstrumentSerialNumber VARCHAR(200);
ALTER TABLE targetedms.SampleFile ADD COLUMN SampleId VARCHAR(200);

CREATE TABLE targetedms.ListDefinition
(
    Id SERIAL NOT NULL,
    RunId INT NOT NULL,
    Name TEXT NOT NULL,
    PkColumnIndex INT NULL,
    DisplayColumnIndex INT NULL,
    CONSTRAINT PK_List PRIMARY KEY(Id),
    CONSTRAINT FK_List_RunId FOREIGN KEY(RunId) REFERENCES targetedms.Runs(Id)
);
CREATE TABLE targetedms.ListColumnDefinition
(
    Id SERIAL NOT NULL,
    ListDefinitionId INT NOT NULL,
    ColumnIndex INT NOT NULL,
    AnnotationType VARCHAR(20) NOT NULL,
    Name TEXT NOT NULL,
    Lookup TEXT NULL,
    CONSTRAINT PK_ListColumn PRIMARY KEY(Id),
    CONSTRAINT FK_ListColumn_ListDefinitionId FOREIGN KEY(ListDefinitionId) REFERENCES targetedms.ListDefinition(Id),
    CONSTRAINT UQ_ListColumn_ListDefinitionId_ColumnIndex UNIQUE(ListDefinitionId, ColumnIndex)
);

CREATE TABLE targetedms.ListItem
(
    Id SERIAL NOT NULL,
    ListDefinitionId INT NOT NULL,
    CONSTRAINT PK_ListItem PRIMARY KEY(Id),
    CONSTRAINT FK_ListItem_ListDefinitionId FOREIGN KEY(ListDefinitionId) REFERENCES targetedms.ListDefinition(Id)
);

CREATE TABLE targetedms.ListItemValue
(
    Id SERIAL NOT NULL,
    ListItemId INT NOT NULL,
    ColumnIndex INT NOT NULL,
    TextValue TEXT NULL,
    NumericValue DOUBLE PRECISION NULL,
    CONSTRAINT PK_ListItemValue PRIMARY KEY(Id),
    CONSTRAINT FK_ListItemValue_ListItem FOREIGN KEY(ListItemId) REFERENCES targetedms.ListItem(Id),
    CONSTRAINT UQ_ListItemValue_ListItemId_ColumnIndex UNIQUE(ListItemId, ColumnIndex)
);

ALTER TABLE targetedms.QCMetricConfiguration ADD COLUMN EnabledQueryName VARCHAR(200);
ALTER TABLE targetedms.QCMetricConfiguration ADD COLUMN EnabledSchemaName VARCHAR(200);

WITH rootIdentity as (select EntityId as theIdentity FROM core.Containers WHERE Parent is null)
INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName,Series2Label,Series2SchemaName,Series2QueryName,PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
  ((select theIdentity from rootIdentity), 'Isotopologue LOD', 'LOD','targetedms', 'QCMetric_IsotopologuePrecursorLOD', NULL, NULL, NULL, TRUE, 'QCMetricEnabled_IsotopologuePrecursorLOD', 'targetedms');

WITH rootIdentity as (select EntityId as theIdentity FROM core.Containers WHERE Parent is null)
INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName,Series2Label,Series2SchemaName,Series2QueryName,PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
  ((select theIdentity from rootIdentity), 'Isotopologue LOQ', 'LOQ', 'targetedms', 'QCMetric_IsotopologuePrecursorLOQ', NULL, NULL, NULL, TRUE, 'QCMetricEnabled_IsotopologuePrecursorLOQ', 'targetedms');

WITH rootIdentity as (select EntityId as theIdentity FROM core.Containers WHERE Parent is null)
INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName,Series2Label,Series2SchemaName,Series2QueryName,PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
  ((select theIdentity from rootIdentity), 'Isotopologue Accuracy', 'Accuracy', 'targetedms', 'QCMetric_IsotopologuePrecursorAccuracy', NULL, NULL, NULL, TRUE, 'QCMetricEnabled_IsotopologuePrecursorAccuracy', 'targetedms');

WITH rootIdentity as (select EntityId as theIdentity FROM core.Containers WHERE Parent is null)
INSERT INTO targetedms.QCMetricConfiguration (Container, Name,Series1Label,Series1SchemaName,Series1QueryName,Series2Label,Series2SchemaName,Series2QueryName,PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
  ((select theIdentity from rootIdentity), 'Isotopologue Regression RSquared', 'Coefficient', 'targetedms', 'QCMetric_IsotopologuePrecursorRSquared', NULL, NULL, NULL, TRUE, 'QCMetricEnabled_IsotopologuePrecursorRSquared', 'targetedms');

UPDATE targetedms.QCMetricConfiguration SET EnabledQueryName = 'QCMetricEnabled_lhRatio', EnabledSchemaName ='targetedms' WHERE Series1QueryName = 'QCMetric_lhRatio';

ALTER TABLE targetedms.runs ALTER COLUMN SoftwareVersion TYPE VARCHAR(200);

ALTER TABLE targetedms.Runs ADD COLUMN ListCount INT DEFAULT 0 NOT NULL;

ALTER TABLE targetedms.AnnotationSettings ADD COLUMN Lookup TEXT;