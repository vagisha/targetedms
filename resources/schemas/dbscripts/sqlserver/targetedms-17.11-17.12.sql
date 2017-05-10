
CREATE TABLE targetedms.QCMetricExclusion
(
  Id INT IDENTITY(1,1) NOT NULL,
  CreatedBy USERID,
  Created DATETIME,
  ModifiedBy USERID,
  Modified DATETIME,

  ReplicateId INT NOT NULL,
  MetricId INT, -- allow NULL to indicate exclusion of replicate for all metrics

  CONSTRAINT PK_QCMetricExclusion PRIMARY KEY (Id),
  CONSTRAINT FK_QCMetricExclusion_ReplicateId FOREIGN KEY (ReplicateId) REFERENCES targetedms.Replicate (Id),
  CONSTRAINT FK_QCMetricExclusion_MetricId FOREIGN KEY (MetricId) REFERENCES targetedms.QCMetricConfiguration (Id),
  CONSTRAINT UQ_QCMetricExclusion_Replicate_Metric UNIQUE (ReplicateId, MetricId)
);
CREATE INDEX IX_QCMetricExclusion_ReplicateId ON targetedms.QCMetricExclusion(ReplicateId);
CREATE INDEX IX_QCMetricExclusion_MetricId ON targetedms.QCMetricExclusion(MetricId);