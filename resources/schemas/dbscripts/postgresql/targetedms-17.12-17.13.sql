ALTER TABLE targetedms.ExperimentAnnotations ADD COLUMN sourceExperimentId INT;
ALTER TABLE targetedms.ExperimentAnnotations ADD COLUMN sourceExperimentPath VARCHAR(1000);
ALTER TABLE targetedms.ExperimentAnnotations ADD COLUMN shortUrl entityId;

CREATE INDEX IX_ExperimentAnnotations_SourceExperimentId ON targetedms.ExperimentAnnotations (sourceExperimentId);
ALTER TABLE targetedms.ExperimentAnnotations ADD CONSTRAINT UQ_ExperimentAnnotations_ShortUrl UNIQUE (shortUrl);
ALTER TABLE targetedms.ExperimentAnnotations ADD CONSTRAINT FK_ExperimentAnnotations_ShortUrl FOREIGN KEY (shorturl)
REFERENCES core.shorturl (entityId);

SELECT core.executeJavaUpgradeCode('updateExperimentAnnotations');