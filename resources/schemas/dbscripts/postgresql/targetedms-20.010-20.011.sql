ALTER TABLE targetedms.PeptideGroup ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.FoldChange ALTER COLUMN PeptideGroupId TYPE bigint;
ALTER TABLE targetedms.PeptideGroupAnnotation ALTER COLUMN PeptideGroupId TYPE bigint;
ALTER TABLE targetedms.PeptideGroupAnnotation ALTER COLUMN Id TYPE bigint;
ALTER TABLE targetedms.GeneralMolecule ALTER COLUMN PeptideGroupId TYPE bigint;
ALTER TABLE targetedms.Protein ALTER COLUMN PeptideGroupId TYPE bigint;