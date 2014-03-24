ALTER TABLE targetedms.iRTPeptide ADD ImportCount INT;
GO
UPDATE targetedms.iRTPeptide SET ImportCount = 1;
GO
ALTER TABLE targetedms.iRTPeptide ALTER COLUMN ImportCount INT NOT NULL;
