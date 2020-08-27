------------------ GeneralMoleculeID ---------------
-----------------------------------------------------

-- Drop Indexes to GMId
DROP INDEX targetedms.CalibrationCurve.IX_CalibrationCurve_GeneralMoleculeId;
DROP INDEX targetedms.FoldChange.IX_FoldChange_GeneralMoleculeId;
DROP INDEX targetedms.GeneralMoleculeAnnotation.IX_GeneralMoleculeAnnotation_GeneralMoleculeId;
DROP INDEX targetedms.GeneralMoleculeChromInfo.IX_GeneralMoleculeChromInfo_GMId;
DROP INDEX targetedms.GeneralPrecursor.IX_Precursor_GMId;
DROP INDEX targetedms.GeneralPrecursor.IX_Precursor_PeptideId; -- Duplicate Index with IX_Precursor_GMId
DROP INDEX targetedms.PeptideIsotopeModification.IX_PeptideIsotopeModification_PeptideId;
DROP INDEX targetedms.PeptideStructuralModification.IX_PeptideStructuralModification_PeptideId;
GO

-- Drop Constraints in other tables to GMId
ALTER TABLE targetedms.CalibrationCurve DROP CONSTRAINT FK_CalibrationCurve_GeneralMolecule;
ALTER TABLE targetedms.FoldChange DROP CONSTRAINT FK_FoldChange_GeneralMolecule;
ALTER TABLE targetedms.GeneralMoleculeAnnotation DROP CONSTRAINT FK_GMAnnotation_GMId;
ALTER TABLE targetedms.GeneralMoleculeChromInfo DROP CONSTRAINT FK_ChromInfo_GMId;
ALTER TABLE targetedms.GeneralPrecursor DROP CONSTRAINT FK_GeneralPrecursor_GMId;
ALTER TABLE targetedms.Molecule DROP CONSTRAINT FK_Molecule_Id;
ALTER TABLE targetedms.Molecule DROP CONSTRAINT PK_Molecule;
ALTER TABLE targetedms.Peptide DROP CONSTRAINT PK_PeptideId;
ALTER TABLE targetedms.Peptide DROP CONSTRAINT FK_Id_GMId;
ALTER TABLE targetedms.PeptideIsotopeModification DROP CONSTRAINT FK_PeptideIsotopeModification_PeptideId_GMId;
ALTER TABLE targetedms.PeptideStructuralModification DROP CONSTRAINT FK_PeptideStructuralModification_PeptideId_GMId;
ALTER TABLE targetedms.GeneralMoleculeAnnotation DROP CONSTRAINT UQ_GMAnnotation_Name_GMId;
GO

-- Alter GM Id
ALTER TABLE targetedms.GeneralMolecule DROP CONSTRAINT PK_GMId;
GO
ALTER TABLE targetedms.GeneralMolecule ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.GeneralMolecule ADD CONSTRAINT PK_GMId PRIMARY KEY (Id);
GO

-- change columns
ALTER TABLE targetedms.CalibrationCurve ALTER COLUMN GeneralMoleculeId bigint;
ALTER TABLE targetedms.FoldChange ALTER COLUMN GeneralMoleculeId bigint;
ALTER TABLE targetedms.GeneralMoleculeAnnotation ALTER COLUMN GeneralMoleculeId bigint NOT NULL;
ALTER TABLE targetedms.GeneralMoleculeChromInfo ALTER COLUMN GeneralMoleculeId BIGINT NOT NULL;
ALTER TABLE targetedms.GeneralPrecursor ALTER COLUMN GeneralMoleculeId bigint NOT NULL;
ALTER TABLE targetedms.Molecule ALTER COLUMN Id bigint NOT NULL;
ALTER TABLE targetedms.Peptide ALTER COLUMN Id bigint NOT NULL;
ALTER TABLE targetedms.PeptideIsotopeModification ALTER COLUMN PeptideId bigint NOT NULL;
ALTER TABLE targetedms.PeptideStructuralModification ALTER COLUMN PeptideId bigint NOT NULL;
GO

-- Add back FK constraints to GMId in tables
ALTER TABLE targetedms.CalibrationCurve ADD CONSTRAINT FK_CalibrationCurve_GeneralMolecule FOREIGN KEY(GeneralMoleculeId) REFERENCES targetedms.GeneralMolecule(Id);
ALTER TABLE targetedms.FoldChange ADD CONSTRAINT FK_FoldChange_GeneralMolecule FOREIGN KEY (GeneralMoleculeId) REFERENCES targetedms.GeneralMolecule(Id);
ALTER TABLE targetedms.GeneralMoleculeAnnotation ADD CONSTRAINT FK_GMAnnotation_GMId FOREIGN KEY (GeneralMoleculeId) REFERENCES targetedms.GeneralMolecule(Id);
ALTER TABLE targetedms.GeneralMoleculeChromInfo ADD CONSTRAINT FK_ChromInfo_GMId FOREIGN KEY (GeneralMoleculeId) REFERENCES targetedms.GeneralMolecule(Id);
ALTER TABLE targetedms.GeneralPrecursor ADD CONSTRAINT FK_GeneralPrecursor_GMId FOREIGN KEY (GeneralMoleculeId) REFERENCES targetedms.GeneralMolecule(Id);
ALTER TABLE targetedms.Molecule ADD CONSTRAINT PK_Molecule PRIMARY KEY (Id);
ALTER TABLE targetedms.Molecule ADD CONSTRAINT FK_Molecule_Id FOREIGN KEY (Id) REFERENCES targetedms.GeneralMolecule(Id);
ALTER TABLE targetedms.Peptide ADD CONSTRAINT PK_PeptideId PRIMARY KEY (Id);
ALTER TABLE targetedms.Peptide ADD CONSTRAINT FK_Id_GMId FOREIGN KEY (Id) REFERENCES targetedms.GeneralMolecule (Id);
ALTER TABLE targetedms.PeptideIsotopeModification ADD CONSTRAINT FK_PeptideIsotopeModification_PeptideId_GMId FOREIGN KEY (PeptideId) REFERENCES targetedms.GeneralMolecule(Id);
ALTER TABLE targetedms.PeptideStructuralModification ADD CONSTRAINT FK_PeptideStructuralModification_PeptideId_GMId FOREIGN KEY (PeptideId) REFERENCES targetedms.GeneralMolecule(Id);
ALTER TABLE targetedms.GeneralMoleculeAnnotation ADD CONSTRAINT UQ_GMAnnotation_Name_GMId UNIQUE (Name, GeneralMoleculeId);
GO

-- Add back Indexes
CREATE INDEX IX_CalibrationCurve_GeneralMoleculeId ON targetedms.CalibrationCurve(GeneralMoleculeId);
CREATE INDEX IX_FoldChange_GeneralMoleculeId ON targetedms.FoldChange(GeneralMoleculeId);
CREATE INDEX IX_GeneralMoleculeAnnotation_GeneralMoleculeId ON targetedms.GeneralMoleculeAnnotation(GeneralMoleculeId);
CREATE INDEX IX_GeneralMoleculeChromInfo_GMId ON targetedms.GeneralMoleculeChromInfo(GeneralMoleculeId);
CREATE INDEX IX_Precursor_GMId ON targetedms.GeneralPrecursor (GeneralMoleculeId);
CREATE INDEX IX_PeptideIsotopeModification_PeptideId ON targetedms.PeptideIsotopeModification (PeptideId);
CREATE INDEX IX_PeptideStructuralModification_PeptideId ON targetedms.PeptideStructuralModification (PeptideId);
GO