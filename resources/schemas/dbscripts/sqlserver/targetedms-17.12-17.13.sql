ALTER TABLE targetedms.ExperimentAnnotations ADD sourceExperimentId INT;
ALTER TABLE targetedms.ExperimentAnnotations ADD sourceExperimentPath NVARCHAR(1000);
ALTER TABLE targetedms.ExperimentAnnotations ADD shortUrl entityId;

CREATE INDEX IX_ExperimentAnnotations_SourceExperimentId ON targetedms.ExperimentAnnotations (sourceExperimentId);
ALTER TABLE targetedms.ExperimentAnnotations ADD CONSTRAINT UQ_ExperimentAnnotations_ShortUrl UNIQUE (shortUrl);
ALTER TABLE targetedms.ExperimentAnnotations ADD CONSTRAINT FK_ExperimentAnnotations_ShortUrl FOREIGN KEY (shorturl)
REFERENCES core.shorturl (entityId);

EXEC core.executeJavaUpgradeCode 'updateExperimentAnnotations';