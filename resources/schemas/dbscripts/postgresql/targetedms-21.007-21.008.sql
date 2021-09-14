ALTER TABLE targetedms.SampleFile ADD IRTSlope REAL;
ALTER TABLE targetedms.SampleFile ADD IRTIntercept REAL;
ALTER TABLE targetedms.SampleFile ADD IRTCorrelation REAL;

WITH rootIdentity as (select EntityId as theIdentity FROM core.Containers WHERE Parent is null)
INSERT INTO targetedms.QCMetricConfiguration (Container, Name, Series1Label, Series1SchemaName, Series1QueryName, PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
    ((select theIdentity from rootIdentity), 'iRT Slope', 'iRT Slope', 'targetedms', 'QCRunMetric_iRTSlope', false, 'QCRunMetricEnabled_iRTSlope', 'targetedms');

WITH rootIdentity as (select EntityId as theIdentity FROM core.Containers WHERE Parent is null)
INSERT INTO targetedms.QCMetricConfiguration (Container, Name, Series1Label, Series1SchemaName, Series1QueryName, PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
    ((select theIdentity from rootIdentity), 'iRT Intercept', 'iRT Intercept', 'targetedms', 'QCRunMetric_iRTIntercept', false, 'QCRunMetricEnabled_iRTIntercept', 'targetedms');

WITH rootIdentity as (select EntityId as theIdentity FROM core.Containers WHERE Parent is null)
INSERT INTO targetedms.QCMetricConfiguration (Container, Name, Series1Label, Series1SchemaName, Series1QueryName, PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
    ((select theIdentity from rootIdentity), 'iRT Correlation', 'iRT Correlation', 'targetedms', 'QCRunMetric_iRTCorrelation', false, 'QCRunMetricEnabled_iRTCorrelation', 'targetedms');

UPDATE targetedms.QCMetricConfiguration SET EnabledQueryName = 'QCRunMetricEnabled_ticArea', EnabledSchemaName = 'targetedms' WHERE Name = 'TIC Area';
UPDATE targetedms.QCMetricConfiguration SET EnabledQueryName = 'QCMetricEnabled_massAccuracy', EnabledSchemaName = 'targetedms' WHERE Name = 'Mass Accuracy';