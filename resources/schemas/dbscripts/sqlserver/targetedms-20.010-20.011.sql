---------------- PeptideGroupId --------------------
----------------------------------------------------
DROP INDEX targetedms.Protein.IX_Protein_PeptideGroupId;
DROP INDEX targetedms.PeptideGroupAnnotation.IX_PeptideGroupAnnotation_PeptideGroupId;
DROP INDEX targetedms.FoldChange.IX_FoldChange_PeptideGroupId;
DROP INDEX targetedms.GeneralMolecule.IX_Peptide_PeptideGroupId;
GO

----- Drop constraints to PeptideGroup PK
ALTER TABLE targetedms.FoldChange DROP CONSTRAINT FK_FoldChange_PeptideGroup;
ALTER TABLE targetedms.PeptideGroupAnnotation DROP CONSTRAINT FK_PeptideGroupAnnotation_PeptideGroup;
ALTER TABLE targetedms.PeptideGroupAnnotation DROP CONSTRAINT UQ_PeptideGroupAnnotation_Name_PeptideGroup;
ALTER TABLE targetedms.GeneralMolecule DROP CONSTRAINT FK_Peptide_PeptideGroup;
ALTER TABLE targetedms.Protein DROP CONSTRAINT FK_Protein_PeptideGroup;
ALTER TABLE targetedms.PeptideGroupAnnotation DROP CONSTRAINT PK_PeptideGroupAnnotation;

---- Alter PeptideGroup PK
ALTER TABLE targetedms.PeptideGroup DROP CONSTRAINT PK_PeptideGroup;
GO
ALTER TABLE targetedms.PeptideGroup ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.PeptideGroup ADD CONSTRAINT PK_PeptideGroup PRIMARY KEY (Id);
GO

-- Change Columns
ALTER TABLE targetedms.FoldChange ALTER COLUMN PeptideGroupId bigint;
ALTER TABLE targetedms.PeptideGroupAnnotation ALTER COLUMN PeptideGroupId bigint NOT NULL;
ALTER TABLE targetedms.PeptideGroupAnnotation ALTER COLUMN Id bigint NOT NULL;
ALTER TABLE targetedms.GeneralMolecule ALTER COLUMN PeptideGroupId bigint NOT NULL;
ALTER TABLE targetedms.Protein ALTER COLUMN PeptideGroupId bigint NOT NULL;

-- Add back DK constraints PeptideGroup PK
ALTER TABLE targetedms.FoldChange ADD CONSTRAINT FK_FoldChange_PeptideGroup FOREIGN KEY (PeptideGroupId) REFERENCES targetedms.PeptideGroup(Id);
ALTER TABLE targetedms.GeneralMolecule ADD CONSTRAINT FK_Peptide_PeptideGroup FOREIGN KEY (PeptideGroupId) REFERENCES targetedms.PeptideGroup(Id);
ALTER TABLE targetedms.PeptideGroupAnnotation ADD CONSTRAINT UQ_PeptideGroupAnnotation_Name_PeptideGroup UNIQUE (Name, PeptideGroupId);
ALTER TABLE targetedms.PeptideGroupAnnotation ADD CONSTRAINT PK_PeptideGroupAnnotation PRIMARY KEY (Id);
ALTER TABLE targetedms.Protein ADD CONSTRAINT FK_Protein_PeptideGroup FOREIGN KEY (PeptideGroupId) REFERENCES targetedms.PeptideGroup(Id)
ALTER TABLE targetedms.PeptideGroupAnnotation ADD CONSTRAINT FK_PeptideGroupAnnotation_PeptideGroup FOREIGN KEY (PeptideGroupId) REFERENCES targetedms.PeptideGroup(Id);
GO

CREATE INDEX IX_Protein_PeptideGroupId ON targetedms.Protein(PeptideGroupId);
CREATE INDEX IX_PeptideGroupAnnotation_PeptideGroupId ON targetedms.PeptideGroupAnnotation(PeptideGroupId);
CREATE INDEX IX_FoldChange_PeptideGroupId ON targetedms.FoldChange(PeptideGroupId);
CREATE INDEX IX_Peptide_PeptideGroupId ON targetedms.GeneralMolecule(PeptideGroupId);
GO