ALTER TABLE targetedms.QCMetricConfiguration ADD COLUMN YAxisLabel1 VARCHAR(200);
ALTER TABLE targetedms.QCMetricConfiguration ADD COLUMN YAxisLabel2 VARCHAR(200);

UPDATE targetedms.QCMetricConfiguration
SET YAxisLabel1 = 'Minutes'
WHERE Name = 'Full Width at Base (FWB)';

UPDATE targetedms.QCMetricConfiguration
SET YAxisLabel1 = 'Minutes'
WHERE Name = 'Full Width at Half Maximum (FWHM)';

UPDATE targetedms.QCMetricConfiguration
SET YAxisLabel1 = 'Ratio'
WHERE Name = 'Light/Heavy Ratio';

UPDATE targetedms.QCMetricConfiguration
SET YAxisLabel1 = 'PPM'
WHERE Name = 'Mass Accuracy';

UPDATE targetedms.QCMetricConfiguration
SET YAxisLabel1 = 'Area'
WHERE Name = 'Peak Area';

UPDATE targetedms.QCMetricConfiguration
SET YAxisLabel1 = 'Minutes'
WHERE Name = 'Retention Time';

UPDATE targetedms.QCMetricConfiguration
SET YAxisLabel1 = 'Precursor Area'
WHERE Name = 'Transition & Precursor Areas';

UPDATE targetedms.QCMetricConfiguration
SET YAxisLabel2 = 'Transition Area'
WHERE Name = 'Transition & Precursor Areas';

UPDATE targetedms.QCMetricConfiguration
SET YAxisLabel1 = 'Area'
WHERE Name = 'TIC Area';

UPDATE targetedms.QCMetricConfiguration
SET YAxisLabel1 = 'Ratio'
WHERE Name = 'Transition/Precursor Area Ratio';