ALTER TABLE targetedms.QCEnabledMetrics DROP CONSTRAINT PK_QCEnabledMetrics;
GO

ALTER TABLE targetedms.QCEnabledMetrics ADD CONSTRAINT PK_QCEnabledMetrics PRIMARY KEY (metric, Container);
GO
