------------------ ReplicateID ------------------
--------------------------------------------------

-- Drop Indexes
DROP INDEX targetedms.QCMetricExclusion.IX_QCMetricExclusion_ReplicateId;
DROP INDEX targetedms.ReplicateAnnotation.IX_ReplicateAnnotation_ReplicateId;
DROP INDEX targetedms.SampleFile.IX_SampleFile_ReplicateId;
GO

-- Drop Constraints
ALTER TABLE targetedms.QCMetricExclusion DROP CONSTRAINT UQ_QCMetricExclusion_Replicate_Metric;
ALTER TABLE targetedms.QCMetricExclusion DROP CONSTRAINT FK_QCMetricExclusion_ReplicateId;
ALTER TABLE targetedms.ReplicateAnnotation DROP CONSTRAINT FK_ReplicateAnnotation_Replicate;
ALTER TABLE targetedms.ReplicateAnnotation DROP CONSTRAINT UQ_ReplicateAnnotation_Name_Repicate;
ALTER TABLE targetedms.SampleFile DROP CONSTRAINT FK_SampleFile_Replicate;
GO

-- Change ReplicateId
ALTER TABLE targetedms.Replicate DROP CONSTRAINT PK_Replicate;
GO
ALTER TABLE targetedms.Replicate ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.Replicate ADD CONSTRAINT PK_Replicate PRIMARY KEY (Id);
GO

-- Change Columns
ALTER TABLE targetedms.ReplicateAnnotation ALTER COLUMN ReplicateId bigint NOT NULL;
ALTER TABLE targetedms.SampleFile ALTER COLUMN ReplicateId bigint NOT NULL;
ALTER TABLE targetedms.QCMetricExclusion ALTER COLUMN ReplicateId bigint NOT NULL;

-- Add back Constraints
ALTER TABLE targetedms.QCMetricExclusion ADD CONSTRAINT FK_QCMetricExclusion_ReplicateId FOREIGN KEY (ReplicateId) REFERENCES targetedms.Replicate (Id);
ALTER TABLE targetedms.QCMetricExclusion  ADD CONSTRAINT UQ_QCMetricExclusion_Replicate_Metric UNIQUE (ReplicateId, MetricId);
ALTER TABLE targetedms.ReplicateAnnotation  ADD CONSTRAINT UQ_ReplicateAnnotation_Name_Repicate UNIQUE (Name, ReplicateId);
ALTER TABLE targetedms.ReplicateAnnotation  ADD CONSTRAINT FK_ReplicateAnnotation_Replicate FOREIGN KEY (ReplicateId) REFERENCES targetedms.Replicate(Id);
ALTER TABLE targetedms.SampleFile ADD CONSTRAINT FK_SampleFile_Replicate FOREIGN KEY (ReplicateId) REFERENCES targetedms.Replicate(Id)

-- Add back Indexes
CREATE INDEX IX_SampleFile_ReplicateId ON targetedms.SampleFile(ReplicateId);
CREATE INDEX IX_QCMetricExclusion_ReplicateId ON targetedms.QCMetricExclusion(ReplicateId);
CREATE INDEX IX_ReplicateAnnotation_ReplicateId ON targetedms.ReplicateAnnotation (ReplicateId);
GO

------------------ SampleFileID ------------------
--------------------------------------------------

-- Drop Indexes
DROP INDEX targetedms.GeneralMoleculeChromInfo.IX_GMChromInfo_SampleFileId;
DROP INDEX targetedms.PrecursorChromInfo.IX_PrecursorChromInfo_SampleFileId;
DROP INDEX targetedms.SampleFileChromInfo.IDX_SampleFileChromInfo_SampleFileId;
DROP INDEX targetedms.TransitionChromInfo.IX_TransitionChromInfo_SampleFileId;
GO

-- Drop Constraints
ALTER TABLE targetedms.TransitionChromInfo DROP CONSTRAINT FK_TransitionChromInfo_SampleFile;
ALTER TABLE targetedms.GeneralMoleculeChromInfo DROP CONSTRAINT FK_GMChromInfo_SampleFile;
ALTER TABLE targetedms.PrecursorChromInfo DROP CONSTRAINT FK_PrecursorChromInfo_SampleFile;
ALTER TABLE targetedms.SampleFileChromInfo DROP CONSTRAINT FK_SampleFileChromInfo_SampleFile;
GO

-- Change SampleFileId
ALTER TABLE targetedms.SampleFile DROP CONSTRAINT PK_SampleFile;
GO
ALTER TABLE targetedms.SampleFile ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.SampleFile ADD CONSTRAINT PK_SampleFile PRIMARY KEY (Id);
GO

-- Change Columns
ALTER TABLE targetedms.TransitionChromInfo ALTER COLUMN SampleFileId bigint NOT NULL;
ALTER TABLE targetedms.GeneralMoleculeChromInfo ALTER COLUMN SampleFileId BIGINT NOT NULL;
ALTER TABLE targetedms.PrecursorChromInfo ALTER COLUMN SampleFileId bigint NOT NULL;
ALTER TABLE targetedms.SampleFileChromInfo ALTER COLUMN SampleFileId bigint NOT NULL;
GO

-- Add back Constraints
ALTER TABLE targetedms.TransitionChromInfo ADD CONSTRAINT FK_TransitionChromInfo_SampleFile FOREIGN KEY (SampleFileId) REFERENCES targetedms.SampleFile(Id);
ALTER TABLE targetedms.GeneralMoleculeChromInfo ADD CONSTRAINT FK_GMChromInfo_SampleFile FOREIGN KEY (SampleFileId) REFERENCES targetedms.SampleFile(Id);
ALTER TABLE targetedms.PrecursorChromInfo ADD CONSTRAINT FK_PrecursorChromInfo_SampleFile FOREIGN KEY (SampleFileId) REFERENCES targetedms.SampleFile(Id)
ALTER TABLE targetedms.SampleFileChromInfo ADD CONSTRAINT FK_SampleFileChromInfo_SampleFile FOREIGN KEY (SampleFileId) REFERENCES targetedms.SampleFile(Id)
GO

-- Add back Indexes
CREATE INDEX IX_GMChromInfo_SampleFileId ON targetedms.GeneralMoleculeChromInfo(samplefileid);
CREATE INDEX IX_PrecursorChromInfo_SampleFileId ON targetedms.PrecursorChromInfo(SampleFileId);
CREATE INDEX IDX_SampleFileChromInfo_SampleFileId ON targetedms.SampleFileChromInfo(samplefileid);
CREATE INDEX IX_TransitionChromInfo_SampleFileId ON targetedms.TransitionChromInfo(SampleFileId);
GO

------------------ SpectrumLibraryID ------------------
-------------------------------------------------------

-- Drop Indexes
DROP INDEX targetedms.BibliospecLibInfo.IX_BibliospecLibInfo_SpectrumLibraryId;
DROP INDEX targetedms.ChromatogramLibInfo.IX_ChromatogramLibInfo_SpectrumLibraryId;
DROP INDEX targetedms.HunterLibInfo.IX_HunterLibInfo_SpectrumLibraryId;
DROP INDEX targetedms.NistLibInfo.IX_NistLibInfo_SpectrumLibraryId;
DROP INDEX targetedms.SpectrastLibInfo.IX_SpectrastLibInfo_SpectrumLibraryId;
GO

-- Drop Constraints
ALTER TABLE targetedms.BibliospecLibInfo DROP CONSTRAINT FK_BibliospecLibInfo_SpectrumLibrary;
ALTER TABLE targetedms.ChromatogramLibInfo DROP CONSTRAINT FK_ChromatogramLibInfo_SpectrumLibrary;
ALTER TABLE targetedms.HunterLibInfo DROP CONSTRAINT FK_HunterLibInfo_SpectrumLibrary;
ALTER TABLE targetedms.NistLibInfo DROP CONSTRAINT FK_NistLibInfo_SpectrumLibrary;
ALTER TABLE targetedms.SpectrastLibInfo DROP CONSTRAINT FK_SpectrastLibInfo_SpectrumLibrary;
GO

-- Change SLId
ALTER TABLE targetedms.SpectrumLibrary DROP CONSTRAINT PK_SpectrumLibrary;
GO
ALTER TABLE targetedms.SpectrumLibrary ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.SpectrumLibrary ADD CONSTRAINT PK_SpectrumLibrary PRIMARY KEY (Id);
GO

-- Change Columns
ALTER TABLE targetedms.BibliospecLibInfo ALTER COLUMN SpectrumLibraryId bigint NOT NULL;
ALTER TABLE targetedms.ChromatogramLibInfo ALTER COLUMN SpectrumLibraryId bigint NOT NULL;
ALTER TABLE targetedms.HunterLibInfo ALTER COLUMN SpectrumLibraryId bigint NOT NULL;
ALTER TABLE targetedms.NistLibInfo ALTER COLUMN SpectrumLibraryId bigint NOT NULL;
ALTER TABLE targetedms.SpectrastLibInfo ALTER COLUMN SpectrumLibraryId bigint NOT NULL;
GO

-- Add back Constraints
ALTER TABLE targetedms.BibliospecLibInfo ADD CONSTRAINT FK_BibliospecLibInfo_SpectrumLibrary FOREIGN KEY (SpectrumLibraryId) REFERENCES targetedms.SpectrumLibrary(Id);
ALTER TABLE targetedms.ChromatogramLibInfo ADD CONSTRAINT FK_ChromatogramLibInfo_SpectrumLibrary FOREIGN KEY (SpectrumLibraryId) REFERENCES targetedms.SpectrumLibrary(Id);
ALTER TABLE targetedms.HunterLibInfo ADD CONSTRAINT FK_HunterLibInfo_SpectrumLibrary FOREIGN KEY (SpectrumLibraryId) REFERENCES targetedms.SpectrumLibrary(Id);
ALTER TABLE targetedms.NistLibInfo ADD CONSTRAINT FK_NistLibInfo_SpectrumLibrary FOREIGN KEY (SpectrumLibraryId) REFERENCES targetedms.SpectrumLibrary(Id);
ALTER TABLE targetedms.SpectrastLibInfo ADD CONSTRAINT FK_SpectrastLibInfo_SpectrumLibrary FOREIGN KEY (SpectrumLibraryId) REFERENCES targetedms.SpectrumLibrary(Id);
GO

-- Add back Indexes
CREATE INDEX IX_BibliospecLibInfo_SpectrumLibraryId ON targetedms.BibliospecLibInfo(SpectrumLibraryId);
CREATE INDEX IX_ChromatogramLibInfo_SpectrumLibraryId ON targetedms.ChromatogramLibInfo(SpectrumLibraryId);
CREATE INDEX IX_HunterLibInfo_SpectrumLibraryId ON targetedms.HunterLibInfo(SpectrumLibraryId);
CREATE INDEX IX_NistLibInfo_SpectrumLibraryId ON targetedms.NistLibInfo(SpectrumLibraryId);
CREATE INDEX IX_SpectrastLibInfo_SpectrumLibraryId ON targetedms.SpectrastLibInfo(SpectrumLibraryId);
GO


------------------ IsotopeModId -----------------------
-------------------------------------------------------

-- Drop Indexes
DROP INDEX targetedms.PeptideIsotopeModification.IX_PeptideIsotopeModification_IsotopeModId;
GO

-- Drop Constraints
ALTER TABLE targetedms.PeptideIsotopeModification DROP CONSTRAINT FK_PeptideIsotopeModification_IsotopeModification;
ALTER TABLE targetedms.RunIsotopeModification DROP CONSTRAINT FK_RunIsotopeModification_IsotopeModification;
ALTER TABLE targetedms.RunIsotopeModification DROP CONSTRAINT PK_RunIsotopeModification;
GO

-- Change IsotopeModId
ALTER TABLE targetedms.IsotopeModification DROP CONSTRAINT PK_IsotopeModification;
GO
ALTER TABLE targetedms.IsotopeModification ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.IsotopeModification ADD CONSTRAINT PK_IsotopeModification PRIMARY KEY (Id);
GO

-- Change Columns
ALTER TABLE targetedms.PeptideIsotopeModification ALTER COLUMN IsotopeModId bigint NOT NULL;
ALTER TABLE targetedms.RunIsotopeModification ALTER COLUMN IsotopeModId bigint NOT NULL;

-- Add back Constraints
ALTER TABLE targetedms.PeptideIsotopeModification ADD CONSTRAINT FK_PeptideIsotopeModification_IsotopeModification FOREIGN KEY (IsotopeModId) REFERENCES targetedms.IsotopeModification(Id);
ALTER TABLE targetedms.RunIsotopeModification ADD CONSTRAINT FK_RunIsotopeModification_IsotopeModification FOREIGN KEY (IsotopeModId) REFERENCES targetedms.IsotopeModification(Id);
ALTER TABLE targetedms.RunIsotopeModification ADD CONSTRAINT PK_RunIsotopeModification PRIMARY KEY (IsotopeModId, RunId, IsotopeLabelId);

-- Add back Indexes
CREATE INDEX IX_PeptideIsotopeModification_IsotopeModId ON targetedms.PeptideIsotopeModification (IsotopeModId);
GO


------------------ PrecursorChromInfoId ---------------------
-------------------------------------------------------
-- Drop Indexes
DROP INDEX targetedms.PrecursorAreaRatio.IX_PrecursorAreaRatio_PrecursorChromInfoId;
DROP INDEX targetedms.PrecursorAreaRatio.IX_PrecursorAreaRatio_PrecursorChromInfoStdId;
DROP INDEX targetedms.PrecursorChromInfoAnnotation.IX_PrecursorChromInfoAnnotation_PrecursorChromInfoId;
DROP INDEX targetedms.TransitionChromInfo.IX_TransitionChromInfo_PrecursorChromInfoId;
DROP INDEX targetedms.PrecursorChromInfo.idx_precursorchrominfo_container;
GO

-- Drop Constraints
ALTER TABLE targetedms.PrecursorAreaRatio DROP CONSTRAINT FK_PrecursorAreaRatio_PrecursorChromInfoId;
ALTER TABLE targetedms.PrecursorAreaRatio DROP CONSTRAINT FK_PrecursorAreaRatio_PrecursorChromInfoStdId;
ALTER TABLE targetedms.PrecursorChromInfoAnnotation DROP CONSTRAINT FK_PrecursorChromInfoAnnotation_PrecursorChromInfo;
ALTER TABLE targetedms.PrecursorChromInfoAnnotation DROP CONSTRAINT UQ_PrecursorChromInfoAnnotation_Name_PrecursorChromInfo
ALTER TABLE targetedms.TransitionChromInfo DROP CONSTRAINT FK_TransitionChromInfo_PrecursorChromInfo;
GO

-- Change PrecursorChromInfoId
ALTER TABLE targetedms.PrecursorChromInfo DROP CONSTRAINT PK_PrecursorChromInfo;
GO
ALTER TABLE targetedms.PrecursorChromInfo ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.PrecursorChromInfo ADD CONSTRAINT PK_PrecursorChromInfo PRIMARY KEY (Id);
GO

-- Change Columns
ALTER TABLE targetedms.PrecursorAreaRatio ALTER COLUMN PrecursorChromInfoId bigint NOT NULL;
ALTER TABLE targetedms.PrecursorAreaRatio ALTER COLUMN PrecursorChromInfoStdId bigint NOT NULL;
ALTER TABLE targetedms.PrecursorChromInfoAnnotation ALTER COLUMN PrecursorChromInfoId bigint NOT NULL;
ALTER TABLE targetedms.TransitionChromInfo ALTER COLUMN PrecursorChromInfoId bigint NOT NULL;
GO

-- Add back Constraints
ALTER TABLE targetedms.PrecursorAreaRatio ADD CONSTRAINT FK_PrecursorAreaRatio_PrecursorChromInfoId FOREIGN KEY (PrecursorChromInfoId) REFERENCES targetedms.PrecursorChromInfo(Id);
ALTER TABLE targetedms.PrecursorAreaRatio ADD CONSTRAINT FK_PrecursorAreaRatio_PrecursorChromInfoStdId FOREIGN KEY (PrecursorChromInfoStdId) REFERENCES targetedms.PrecursorChromInfo(Id);
ALTER TABLE targetedms.PrecursorChromInfoAnnotation ADD CONSTRAINT FK_PrecursorChromInfoAnnotation_PrecursorChromInfo FOREIGN KEY (PrecursorChromInfoId) REFERENCES targetedms.PrecursorChromInfo(Id);
ALTER TABLE targetedms.PrecursorChromInfoAnnotation ADD CONSTRAINT UQ_PrecursorChromInfoAnnotation_Name_PrecursorChromInfo UNIQUE (Name, PrecursorChromInfoId);
ALTER TABLE targetedms.TransitionChromInfo ADD CONSTRAINT FK_TransitionChromInfo_PrecursorChromInfo FOREIGN KEY (PrecursorChromInfoId) REFERENCES targetedms.PrecursorChromInfo(Id);
GO

-- Add back Indexes
CREATE INDEX IX_PrecursorAreaRatio_PrecursorChromInfoId ON targetedms.PrecursorAreaRatio (PrecursorChromInfoId);
CREATE INDEX IX_PrecursorAreaRatio_PrecursorChromInfoStdId ON targetedms.PrecursorAreaRatio (PrecursorChromInfoStdId);
CREATE INDEX IX_PrecursorChromInfoAnnotation_PrecursorChromInfoId ON targetedms.PrecursorChromInfoAnnotation(PrecursorChromInfoId);
CREATE INDEX IX_TransitionChromInfo_PrecursorChromInfoId ON targetedms.TransitionChromInfo(PrecursorChromInfoId);
CREATE INDEX idx_precursorchrominfo_container ON targetedms.precursorchrominfo (container, id);
GO

------------------ IsotopeLabelId ---------------------
-------------------------------------------------------

-- Drop Indexes
DROP INDEX targetedms.FoldChange.IX_FoldChange_IsotopeLabelId;
DROP INDEX targetedms.GeneralPrecursor.IX_GeneralPrecursor_IsotopeLabelId;
DROP INDEX targetedms.PeptideAreaRatio.IX_PeptideAreaRatio_IsotopeLabelId;
DROP INDEX targetedms.PeptideAreaRatio.IX_PeptideAreaRatio_IsotopeLabelStdId;
DROP INDEX targetedms.PrecursorAreaRatio.IX_PrecursorAreaRatio_IsotopeLabelId;
DROP INDEX targetedms.PrecursorAreaRatio.IX_PrecursorAreaRatio_IsotopeLabelStdId;
DROP INDEX targetedms.RunIsotopeModification.IX_RunIsotopeModification_IsotopeLabelId;
DROP INDEX targetedms.TransitionAreaRatio.IX_TransitionAreaRatio_IsotopeLabelId;
DROP INDEX targetedms.TransitionAreaRatio.IX_TransitionAreaRatio_IsotopeLabelStdId;
GO

-- Drop Constraints
ALTER TABLE targetedms.FoldChange DROP CONSTRAINT FK_FoldChange_IsotopeLabel;
ALTER TABLE targetedms.GeneralPrecursor DROP CONSTRAINT FK_GeneralPrecursor_IsotopeLabel;
ALTER TABLE targetedms.PeptideAreaRatio DROP CONSTRAINT FK_PeptideAreaRatio_IsotopeLabelId;
ALTER TABLE targetedms.PeptideAreaRatio DROP CONSTRAINT FK_PeptideAreaRatio_IsotopeLabelStdId;
ALTER TABLE targetedms.PrecursorAreaRatio DROP CONSTRAINT FK_PrecursorAreaRatio_IsotopeLabelId;
ALTER TABLE targetedms.PrecursorAreaRatio DROP CONSTRAINT FK_PrecursorAreaRatio_IsotopeLabelStdId;
ALTER TABLE targetedms.TransitionAreaRatio DROP CONSTRAINT FK_TransitionAreaRatio_IsotopeLabelId;
ALTER TABLE targetedms.RunIsotopeModification DROP CONSTRAINT FK_RunIsotopeModification_IsotopeLabel;
ALTER TABLE targetedms.RunIsotopeModification DROP CONSTRAINT PK_RunIsotopeModification;
ALTER TABLE targetedms.TransitionAreaRatio DROP CONSTRAINT FK_TransitionAreaRatio_IsotopeLabelStdId;

-- Change IsotopeLabelId
ALTER TABLE targetedms.IsotopeLabel DROP CONSTRAINT PK_IsotopeLabel;
GO
ALTER TABLE targetedms.IsotopeLabel ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.IsotopeLabel ADD CONSTRAINT PK_IsotopeLabel PRIMARY KEY (Id);
GO

-- Change Columns
ALTER TABLE targetedms.FoldChange ALTER COLUMN IsotopeLabelId bigint;
ALTER TABLE targetedms.GeneralPrecursor ALTER COLUMN IsotopeLabelId bigint;
ALTER TABLE targetedms.PeptideAreaRatio ALTER COLUMN IsotopeLabelId bigint NOT NULL;
ALTER TABLE targetedms.PeptideAreaRatio ALTER COLUMN IsotopeLabelStdId bigint NOT NULL;
ALTER TABLE targetedms.PrecursorAreaRatio ALTER COLUMN IsotopeLabelId bigint NOT NULL;
ALTER TABLE targetedms.PrecursorAreaRatio ALTER COLUMN IsotopeLabelStdId bigint NOT NULL;
ALTER TABLE targetedms.RunIsotopeModification ALTER COLUMN IsotopeLabelId bigint NOT NULL;
ALTER TABLE targetedms.TransitionAreaRatio ALTER COLUMN IsotopeLabelId bigint NOT NULL;
ALTER TABLE targetedms.TransitionAreaRatio ALTER COLUMN IsotopeLabelStdId bigint NOT NULL;
GO

-- Add back Constraints
ALTER TABLE targetedms.FoldChange ADD CONSTRAINT FK_FoldChange_IsotopeLabel FOREIGN KEY (IsotopeLabelId) REFERENCES targetedms.IsotopeLabel(Id);
ALTER TABLE targetedms.GeneralPrecursor ADD CONSTRAINT FK_GeneralPrecursor_IsotopeLabel FOREIGN KEY (IsotopeLabelId) REFERENCES targetedms.IsotopeLabel(Id);
ALTER TABLE targetedms.PeptideAreaRatio ADD CONSTRAINT FK_PeptideAreaRatio_IsotopeLabelId FOREIGN KEY (IsotopeLabelId) REFERENCES targetedms.IsotopeLabel(Id);
ALTER TABLE targetedms.PeptideAreaRatio ADD CONSTRAINT FK_PeptideAreaRatio_IsotopeLabelStdId FOREIGN KEY (IsotopeLabelStdId) REFERENCES targetedms.IsotopeLabel(Id);
ALTER TABLE targetedms.PrecursorAreaRatio ADD CONSTRAINT FK_PrecursorAreaRatio_IsotopeLabelId FOREIGN KEY (IsotopeLabelId) REFERENCES targetedms.IsotopeLabel(Id);
ALTER TABLE targetedms.PrecursorAreaRatio ADD CONSTRAINT FK_PrecursorAreaRatio_IsotopeLabelStdId FOREIGN KEY (IsotopeLabelStdId) REFERENCES targetedms.IsotopeLabel(Id);
ALTER TABLE targetedms.RunIsotopeModification ADD CONSTRAINT PK_RunIsotopeModification PRIMARY KEY (IsotopeModId, RunId, IsotopeLabelId);
ALTER TABLE targetedms.RunIsotopeModification ADD CONSTRAINT FK_RunIsotopeModification_IsotopeLabel FOREIGN KEY (IsotopeLabelId) REFERENCES targetedms.IsotopeLabel(Id);
ALTER TABLE targetedms.TransitionAreaRatio ADD CONSTRAINT FK_TransitionAreaRatio_IsotopeLabelId FOREIGN KEY (IsotopeLabelId) REFERENCES targetedms.IsotopeLabel(Id);
ALTER TABLE targetedms.TransitionAreaRatio ADD CONSTRAINT FK_TransitionAreaRatio_IsotopeLabelStdId FOREIGN KEY (IsotopeLabelStdId) REFERENCES targetedms.IsotopeLabel(Id);
GO

-- Add back Indexes
CREATE INDEX IX_FoldChange_IsotopeLabelId ON targetedms.FoldChange(IsotopeLabelId);
CREATE INDEX IX_GeneralPrecursor_IsotopeLabelId ON targetedms.GeneralPrecursor(IsotopeLabelId);
CREATE INDEX IX_PeptideAreaRatio_IsotopeLabelId ON targetedms.PeptideAreaRatio (IsotopeLabelId);
CREATE INDEX IX_PeptideAreaRatio_IsotopeLabelStdId ON targetedms.PeptideAreaRatio (IsotopeLabelStdId);
CREATE INDEX IX_PrecursorAreaRatio_IsotopeLabelId ON targetedms.PrecursorAreaRatio (IsotopeLabelId);
CREATE INDEX IX_PrecursorAreaRatio_IsotopeLabelStdId ON targetedms.PrecursorAreaRatio (IsotopeLabelStdId);
CREATE INDEX IX_RunIsotopeModification_IsotopeLabelId ON targetedms.RunIsotopeModification (IsotopeLabelId);
CREATE INDEX IX_TransitionAreaRatio_IsotopeLabelId ON targetedms.TransitionAreaRatio (IsotopeLabelId);
CREATE INDEX IX_TransitionAreaRatio_IsotopeLabelStdId ON targetedms.TransitionAreaRatio (IsotopeLabelStdId);
GO

