ALTER TABLE targetedms.GeneralMolecule ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.CalibrationCurve ALTER COLUMN GeneralMoleculeId TYPE bigint;
ALTER TABLE targetedms.FoldChange ALTER COLUMN GeneralMoleculeId TYPE bigint;
ALTER TABLE targetedms.GeneralMoleculeAnnotation ALTER COLUMN GeneralMoleculeId TYPE bigint;
ALTER TABLE targetedms.GeneralMoleculeChromInfo ALTER COLUMN GeneralMoleculeId TYPE bigint;
ALTER TABLE targetedms.GeneralPrecursor ALTER COLUMN GeneralMoleculeId TYPE bigint;
ALTER TABLE targetedms.Molecule ALTER COLUMN Id TYPE bigint;
ALTER TABLE targetedms.Peptide ALTER COLUMN Id TYPE bigint;
ALTER TABLE targetedms.PeptideIsotopeModification ALTER COLUMN PeptideId TYPE bigint;
ALTER TABLE targetedms.PeptideStructuralModification ALTER COLUMN PeptideId TYPE bigint;