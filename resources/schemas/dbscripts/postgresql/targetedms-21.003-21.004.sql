ALTER TABLE targetedms.QCMetricConfiguration ADD COLUMN TraceValue REAL;
ALTER TABLE targetedms.QCMetricConfiguration ADD COLUMN TimeValue REAL;
ALTER TABLE targetedms.QCMetricConfiguration ADD COLUMN TraceName VARCHAR(200);

CREATE TABLE targetedms.QCTraceMetricValues
(
    Id              SERIAL NOT NULL ,
    metric          INT,
    value           REAL,
    sampleFileId    BIGINT,

    CONSTRAINT PK_QCTraceMetricValues PRIMARY KEY (Id),
    CONSTRAINT FK_QCTraceMetricValues_Metric FOREIGN KEY (metric) REFERENCES targetedms.QCMetricConfiguration(Id),
    CONSTRAINT FK_QCTraceMetricValues_SampleFile FOREIGN KEY (sampleFileId) REFERENCES targetedms.SampleFile(Id)
);

CREATE INDEX IX_QCTraceMetricValues_SampleFile ON targetedms.QCTraceMetricValues(sampleFileId);
CREATE INDEX IX_QCTraceMetricValues_Metric ON targetedms.QCTraceMetricValues(metric);
