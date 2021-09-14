ALTER TABLE targetedms.SampleFile ADD IRTSlope REAL;
ALTER TABLE targetedms.SampleFile ADD IRTIntercept REAL;
ALTER TABLE targetedms.SampleFile ADD IRTCorrelation REAL;
GO

declare @rootIdentity ENTITYID;
select @rootIdentity = [EntityId] FROM [core].[Containers] WHERE Parent is null

INSERT INTO targetedms.QCMetricConfiguration (Container, Name, Series1Label, Series1SchemaName, Series1QueryName, PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
    (@rootIdentity, 'iRT Slope', 'iRT Slope', 'targetedms', 'QCRunMetric_iRTSlope', 0, 'QCRunMetricEnabled_iRTSlope', 'targetedms');

INSERT INTO targetedms.QCMetricConfiguration (Container, Name, Series1Label, Series1SchemaName, Series1QueryName, PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
    (@rootIdentity, 'iRT Intercept', 'iRT Intercept', 'targetedms', 'QCRunMetric_iRTIntercept', 0, 'QCRunMetricEnabled_iRTIntercept', 'targetedms');

INSERT INTO targetedms.QCMetricConfiguration (Container, Name, Series1Label, Series1SchemaName, Series1QueryName, PrecursorScoped, EnabledQueryName, EnabledSchemaName) VALUES
    (@rootIdentity, 'iRT Correlation', 'iRT Correlation', 'targetedms', 'QCRunMetric_iRTCorrelation', 0, 'QCRunMetricEnabled_iRTCorrelation', 'targetedms');

UPDATE targetedms.QCMetricConfiguration SET EnabledQueryName = 'QCRunMetricEnabled_ticArea', EnabledSchemaName = 'targetedms' WHERE Name = 'TIC Area';
UPDATE targetedms.QCMetricConfiguration SET EnabledQueryName = 'QCMetricEnabled_massAccuracy', EnabledSchemaName = 'targetedms' WHERE Name = 'Mass Accuracy';