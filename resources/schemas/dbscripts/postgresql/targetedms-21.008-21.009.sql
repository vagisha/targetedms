WITH rootIdentity as (select EntityId as theIdentity FROM core.Containers WHERE Parent is null)
INSERT INTO targetedms.QCMetricConfiguration (Container, Name, Series1Label, Series1SchemaName, Series1QueryName, PrecursorScoped) VALUES
    ((select theIdentity from rootIdentity), 'Precursor Area', 'Precursor Area', 'targetedms', 'QCMetric_precursorArea', true);


WITH rootIdentity as (select EntityId as theIdentity FROM core.Containers WHERE Parent is null)
INSERT INTO targetedms.QCMetricConfiguration (Container, Name, Series1Label, Series1SchemaName, Series1QueryName, PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
    ((select theIdentity from rootIdentity), 'Transition Area', 'Transition Area', 'targetedms', 'QCMetric_transitionArea', true, 'QCMetricEnabled_transitionArea', 'targetedms');

UPDATE
    targetedms.QCMetricConfiguration
SET
    Name = 'Total Peak Area (Precursor + Transition)',
    EnabledQueryName = 'QCMetricEnabled_transitionArea',
    EnabledSchemaName = 'targetedms',
    Series1Label = 'Total Peak Area'
WHERE Name = 'Peak Area' OR Name = 'Total Peak Area (Precursor + Transition)';

UPDATE
    targetedms.QCMetricConfiguration
SET
    EnabledQueryName = 'QCMetricEnabled_transitionArea',
    EnabledSchemaName = 'targetedms'
WHERE Name = 'Transition/Precursor Area Ratio';

UPDATE
    targetedms.QCMetricConfiguration
SET
    EnabledQueryName = 'QCMetricEnabled_transitionArea',
    EnabledSchemaName = 'targetedms'
WHERE Name = 'Transition & Precursor Areas';