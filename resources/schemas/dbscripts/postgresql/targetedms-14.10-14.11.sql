ALTER TABLE targetedms.iRTPeptide ADD COLUMN ImportCount INT;

UPDATE targetedms.iRTPeptide SET ImportCount = 1;

ALTER TABLE targetedms.iRTPeptide ALTER COLUMN ImportCount SET NOT NULL;
