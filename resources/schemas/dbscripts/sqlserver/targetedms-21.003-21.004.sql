ALTER TABLE targetedms.QCMetricConfiguration ADD TraceValue REAL;
ALTER TABLE targetedms.QCMetricConfiguration ADD TimeValue REAL;
ALTER TABLE targetedms.QCMetricConfiguration ADD TraceName NVARCHAR(200);
GO

CREATE TABLE targetedms.QCTraceMetricValues
(
    Id              INT IDENTITY(1, 1) NOT NULL ,
    metric          INT,
    value           REAL,
    sampleFileId    BIGINT,

    CONSTRAINT PK_QCTraceMetricValues PRIMARY KEY (Id),
    CONSTRAINT FK_QCTraceMetricValues_Metric FOREIGN KEY (metric) REFERENCES targetedms.QCMetricConfiguration(Id),
    CONSTRAINT FK_QCTraceMetricValues_SampleFile FOREIGN KEY (sampleFileId) REFERENCES targetedms.SampleFile(Id)
);

CREATE INDEX IX_QCTraceMetricValues_SampleFile ON targetedms.QCTraceMetricValues(sampleFileId);
CREATE INDEX IX_QCTraceMetricValues_Metric ON targetedms.QCTraceMetricValues(metric);
GO