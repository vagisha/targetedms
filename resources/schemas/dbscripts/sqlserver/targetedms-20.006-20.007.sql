-- Issue 40487: Add a Modified column to targetedms.GeneralPrecursor
ALTER TABLE targetedms.GeneralPrecursor ADD Modified DATETIME;
GO

-- The value in targetedms.runs.Modified should be a good substitute so we will use this value to populate the new column
UPDATE targetedms.GeneralPrecursor SET Modified = r.Modified
FROM targetedms.runs r
INNER JOIN targetedms.PeptideGroup pg ON pg.RunId = r.id
INNER JOIN targetedms.GeneralMolecule gm ON gm.PeptideGroupId = pg.Id
WHERE gm.Id = GeneralMoleculeId;






