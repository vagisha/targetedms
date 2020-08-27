---------------- TransitionChromInfoId --------------------
-----------------------------------------------------------

-- Drop Indexes
DROP INDEX targetedms.TransitionAreaRatio.IX_TransitionAreaRatio_TransitionChromInfoId;
DROP INDEX targetedms.TransitionAreaRatio.IX_TransitionAreaRatio_TransitionChromInfoStdId;
DROP INDEX targetedms.TransitionChromInfoAnnotation.IX_TransitionChromInfoAnnotation_TransitionChromInfoId;
GO

-- Drop Constraints
ALTER TABLE targetedms.TransitionAreaRatio DROP CONSTRAINT FK_TransitionAreaRatio_TransitionChromInfoId;
ALTER TABLE targetedms.TransitionAreaRatio DROP CONSTRAINT FK_TransitionAreaRatio_TransitionChromInfoStdId;
ALTER TABLE targetedms.TransitionChromInfoAnnotation DROP CONSTRAINT FK_TransitionChromInfoAnnotation_TransitionChromInfo;
ALTER TABLE targetedms.TransitionChromInfoAnnotation DROP CONSTRAINT UQ_TransitionChromInfoAnnotation_Name_TransitionChromInfo;
GO

-- Change SampleFileId
ALTER TABLE targetedms.TransitionChromInfo DROP CONSTRAINT PK_TransitionChromInfo;
GO
ALTER TABLE targetedms.TransitionChromInfo ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.TransitionChromInfo ADD CONSTRAINT PK_TransitionChromInfo PRIMARY KEY (Id);

-- Change Columns
ALTER TABLE targetedms.TransitionAreaRatio ALTER COLUMN TransitionChromInfoId bigint NOT NULL;
ALTER TABLE targetedms.TransitionAreaRatio ALTER COLUMN TransitionChromInfoStdId bigint NOT NULL;
ALTER TABLE targetedms.TransitionChromInfoAnnotation ALTER COLUMN TransitionChromInfoId bigint NOT NULL;
GO

-- Add back Constraints
ALTER TABLE targetedms.TransitionAreaRatio ADD CONSTRAINT FK_TransitionAreaRatio_TransitionChromInfoId FOREIGN KEY (TransitionChromInfoId) REFERENCES targetedms.TransitionChromInfo(Id);
ALTER TABLE targetedms.TransitionAreaRatio ADD CONSTRAINT FK_TransitionAreaRatio_TransitionChromInfoStdId FOREIGN KEY (TransitionChromInfoStdId) REFERENCES targetedms.TransitionChromInfo(Id);
ALTER TABLE targetedms.TransitionChromInfoAnnotation ADD CONSTRAINT FK_TransitionChromInfoAnnotation_TransitionChromInfo FOREIGN KEY (TransitionChromInfoId) REFERENCES targetedms.TransitionChromInfo(Id);
ALTER TABLE targetedms.TransitionChromInfoAnnotation ADD CONSTRAINT UQ_TransitionChromInfoAnnotation_Name_TransitionChromInfo UNIQUE (Name, TransitionChromInfoId);
GO

-- Add back Indexes
CREATE INDEX IX_TransitionChromInfoAnnotation_TransitionChromInfoId ON targetedms.TransitionChromInfoAnnotation(TransitionChromInfoId);
CREATE INDEX IX_TransitionAreaRatio_TransitionChromInfoId ON targetedms.TransitionAreaRatio (TransitionChromInfoId);
CREATE INDEX IX_TransitionAreaRatio_TransitionChromInfoStdId ON targetedms.TransitionAreaRatio (TransitionChromInfoStdId);
GO


---------------- GeneralMoleculeChromInfoId --------------------
----------------------------------------------------------------

-- Drop Indexes
DROP INDEX targetedms.PrecursorChromInfo.IX_PrecursorChromInfo_GeneralMoleculeChromInfoId;
DROP INDEX targetedms.PeptideAreaRatio.IX_PeptideAreaRatio_PeptideChromInfoId;
DROP INDEX targetedms.PeptideAreaRatio.IX_PeptideAreaRatio_PeptideChromInfoStdId;
GO

-- Drop Constraints
ALTER TABLE targetedms.PrecursorChromInfo DROP CONSTRAINT FK_PrecursorChromInfo_GMChromInfo;
ALTER TABLE targetedms.PeptideAreaRatio DROP CONSTRAINT FK_PeptideAreaRatio_PeptideChromInfoId;
ALTER TABLE targetedms.PeptideAreaRatio DROP CONSTRAINT FK_PeptideAreaRatio_PeptideChromInfoStdId;
GO

-- Change GeneralMoleculeChromInfoId
ALTER TABLE targetedms.GeneralMoleculeChromInfo DROP CONSTRAINT PK_GMChromInfoId;
GO
ALTER TABLE targetedms.GeneralMoleculeChromInfo ALTER COLUMN Id BIGINT NOT NULL;
GO
ALTER TABLE targetedms.GeneralMoleculeChromInfo ADD CONSTRAINT PK_GMChromInfoId PRIMARY KEY (Id);

-- Change Columns
ALTER TABLE targetedms.PrecursorChromInfo ALTER COLUMN GeneralMoleculeChromInfoId bigint NOT NULL;
ALTER TABLE targetedms.PeptideAreaRatio ALTER COLUMN PeptideChromInfoId bigint NOT NULL;
ALTER TABLE targetedms.PeptideAreaRatio ALTER COLUMN PeptideChromInfoStdId bigint NOT NULL;
GO

-- Add back Constraints
ALTER TABLE targetedms.PrecursorChromInfo ADD CONSTRAINT FK_PrecursorChromInfo_GMChromInfo FOREIGN KEY (GeneralMoleculeChromInfoId) REFERENCES targetedms.GeneralMoleculeChromInfo(Id);
ALTER TABLE targetedms.PeptideAreaRatio ADD CONSTRAINT FK_PeptideAreaRatio_PeptideChromInfoId FOREIGN KEY (PeptideChromInfoId) REFERENCES targetedms.GeneralMoleculeChromInfo(Id);
ALTER TABLE targetedms.PeptideAreaRatio ADD CONSTRAINT FK_PeptideAreaRatio_PeptideChromInfoStdId FOREIGN KEY (PeptideChromInfoStdId) REFERENCES targetedms.GeneralMoleculeChromInfo(Id);
GO

-- Add back Indexes
CREATE INDEX IX_PrecursorChromInfo_GeneralMoleculeChromInfoId ON targetedms.PrecursorChromInfo(GeneralMoleculeChromInfoId);
CREATE INDEX IX_PeptideAreaRatio_PeptideChromInfoId ON targetedms.PeptideAreaRatio (PeptideChromInfoId);
CREATE INDEX IX_PeptideAreaRatio_PeptideChromInfoStdId ON targetedms.PeptideAreaRatio (PeptideChromInfoStdId);
GO

---------------- StructuralModLossId --------------------
--------------------------------------------------------
-- Drop Indexes
DROP INDEX targetedms.TransitionLoss.IX_TransitionLoss_StructuralModLossId;
GO

-- Drop Constraints
ALTER TABLE targetedms.TransitionLoss DROP CONSTRAINT FK_TransitionLoss_StructuralModLossId;
GO

-- Change SampleFileId
ALTER TABLE targetedms.StructuralModLoss DROP CONSTRAINT PK_StructuralModLoss;
GO
ALTER TABLE targetedms.StructuralModLoss ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.StructuralModLoss ADD CONSTRAINT PK_StructuralModLoss PRIMARY KEY (Id);
GO

-- Change Columns
ALTER TABLE targetedms.TransitionLoss ALTER COLUMN StructuralModLossId bigint NOT NULL;
GO

-- Add back Constraints
ALTER TABLE targetedms.TransitionLoss ADD CONSTRAINT FK_TransitionLoss_StructuralModLossId FOREIGN KEY (StructuralModLossId) REFERENCES targetedms.StructuralModLoss(Id);
GO

-- Add back Indexes
CREATE INDEX IX_TransitionLoss_StructuralModLossId ON targetedms.TransitionLoss (StructuralModLossId);
GO

---------------- StructuralModId ------------------------------
----------------------------------------------------------------
-- Drop Indexes
DROP INDEX targetedms.PeptideStructuralModification.IX_PeptideStructuralModification_StructuralModId;
DROP INDEX targetedms.StructuralModLoss.IX_StructuralModification_StructuralModId;
GO

-- Drop Constraints
ALTER TABLE targetedms.PeptideStructuralModification DROP CONSTRAINT FK_PeptideStructuralModification_StructuralModification;
ALTER TABLE targetedms.RunStructuralModification DROP CONSTRAINT FK_RunStructuralModification_StructuralModification;
ALTER TABLE targetedms.StructuralModLoss DROP CONSTRAINT FK_StructuralModLoss_StructuralModification;
ALTER TABLE targetedms.RunStructuralModification DROP CONSTRAINT PK_RunStructuralModification;
GO

-- Change StructuralModId
ALTER TABLE targetedms.StructuralModification DROP CONSTRAINT PK_StructuralModification;
GO
ALTER TABLE targetedms.StructuralModification ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.StructuralModification ADD CONSTRAINT PK_StructuralModification PRIMARY KEY (Id);
GO

-- Change Columns
ALTER TABLE targetedms.PeptideStructuralModification ALTER COLUMN StructuralModId bigint NOT NULL;
ALTER TABLE targetedms.RunStructuralModification ALTER COLUMN StructuralModId bigint NOT NULL;
ALTER TABLE targetedms.StructuralModLoss ALTER COLUMN StructuralModId bigint NOT NULL;
GO

-- Add back Constraints
ALTER TABLE targetedms.PeptideStructuralModification  ADD CONSTRAINT FK_PeptideStructuralModification_StructuralModification FOREIGN KEY (StructuralModId) REFERENCES targetedms.StructuralModification(Id);
ALTER TABLE targetedms.RunStructuralModification ADD CONSTRAINT FK_RunStructuralModification_StructuralModification FOREIGN KEY (StructuralModId) REFERENCES targetedms.StructuralModification(Id);
ALTER TABLE targetedms.StructuralModLoss ADD CONSTRAINT FK_StructuralModLoss_StructuralModification FOREIGN KEY (StructuralModId) REFERENCES targetedms.StructuralModification(Id);
ALTER TABLE targetedms.RunStructuralModification ADD CONSTRAINT PK_RunStructuralModification PRIMARY KEY (StructuralModId, RunId)
    GO

-- Add back Indexes
CREATE INDEX IX_PeptideStructuralModification_StructuralModId ON targetedms.PeptideStructuralModification (StructuralModId);
CREATE INDEX IX_StructuralModification_StructuralModId ON targetedms.StructuralModLoss (StructuralModId);
GO


------------------ QuantificationSettingsId ------------------------------
--------------------------------------------------------------------------

-- Drop Indexes
DROP INDEX targetedms.CalibrationCurve.IX_CalibrationCurve_QuantificationSettingsId;
GO

-- Drop Constraints
ALTER TABLE targetedms.CalibrationCurve DROP CONSTRAINT FK_CalibrationCurve_QuantificationSettings;
GO

-- Change QuantificationSettingsId
ALTER TABLE targetedms.QuantificationSettings DROP CONSTRAINT PK_QuantificationSettings;
GO
ALTER TABLE targetedms.QuantificationSettings ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.QuantificationSettings ADD CONSTRAINT PK_QuantificationSettings PRIMARY KEY (Id);
GO

-- Change Columns
ALTER TABLE targetedms.CalibrationCurve ALTER COLUMN QuantificationSettingsId bigint;
GO

-- Add back Constraints
ALTER TABLE targetedms.CalibrationCurve ADD CONSTRAINT FK_CalibrationCurve_QuantificationSettings FOREIGN KEY (QuantificationSettingsId) REFERENCES targetedms.QuantificationSettings(Id);
GO

-- Add back Indexes
CREATE INDEX IX_CalibrationCurve_QuantificationSettingsId ON targetedms.CalibrationCurve(QuantificationSettingsId);
GO

------------------ GroupComparisonSettingsId ------------------------------
---------------------------------------------------------------------------

-- Drop Indexes
DROP INDEX targetedms.FoldChange.IX_FoldChange_GroupComparisonSettingsId;
GO

-- Drop Constraints
ALTER TABLE targetedms.FoldChange DROP CONSTRAINT FK_FoldChange_GroupComparisonSettings;
GO

-- Change GroupComparisonSettingsId
ALTER TABLE targetedms.GroupComparisonSettings DROP CONSTRAINT PK_GroupComparisonSettings;
GO
ALTER TABLE targetedms.GroupComparisonSettings ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.GroupComparisonSettings ADD CONSTRAINT PK_GroupComparisonSettings PRIMARY KEY (Id);
GO

-- Change Columns
ALTER TABLE targetedms.FoldChange ALTER COLUMN GroupComparisonSettingsId bigint NOT NULL;
GO

-- Add back Constraints
ALTER TABLE targetedms.FoldChange ADD CONSTRAINT FK_FoldChange_GroupComparisonSettings FOREIGN KEY (GroupComparisonSettingsId) REFERENCES targetedms.GroupComparisonSettings(Id);
GO

-- Add back Indexes
CREATE INDEX IX_FoldChange_GroupComparisonSettingsId ON targetedms.FoldChange(GroupComparisonSettingsId);
GO

------------------ IsolationSchemeId ------------------------------
--------------------------------------------------------------------
-- Drop Indexes
DROP INDEX targetedms.IsolationWindow.IX_IsolationWindow_IsolationSchemeId;
GO

-- Drop Constraints
ALTER TABLE targetedms.IsolationWindow DROP CONSTRAINT FK_IsolationWindow_IsolationScheme;
GO

-- Change IsolationSchemeId
ALTER TABLE targetedms.IsolationScheme DROP CONSTRAINT PK_IsolationScheme;
GO
ALTER TABLE targetedms.IsolationScheme ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.IsolationScheme ADD CONSTRAINT PK_IsolationScheme PRIMARY KEY (Id);
GO

-- Change Columns
ALTER TABLE targetedms.IsolationWindow ALTER COLUMN IsolationSchemeId bigint NOT NULL;
GO

-- Add back Constraints
ALTER TABLE targetedms.IsolationWindow ADD CONSTRAINT FK_IsolationWindow_IsolationScheme FOREIGN KEY (IsolationSchemeId) REFERENCES targetedms.IsolationScheme(Id);
GO

-- Add back Indexes
CREATE INDEX IX_IsolationWindow_IsolationSchemeId ON targetedms.IsolationWindow (IsolationSchemeId);
GO

------------------ ListDefinitionId ------------------------------
------------------------------------------------------------------

-- Drop Constraints
ALTER TABLE targetedms.ListColumnDefinition DROP CONSTRAINT FK_ListColumn_ListDefinitionId;
ALTER TABLE targetedms.ListColumnDefinition DROP CONSTRAINT UQ_ListColumn_ListDefinitionId_ColumnIndex;
ALTER TABLE targetedms.ListItem DROP CONSTRAINT FK_ListItem_ListDefinitionId;
GO

-- Change ListDefinitionId
ALTER TABLE targetedms.ListDefinition DROP CONSTRAINT PK_List;
GO

ALTER TABLE targetedms.ListDefinition ALTER COLUMN Id bigint NOT NULL;
GO

ALTER TABLE targetedms.ListDefinition ADD CONSTRAINT PK_List PRIMARY KEY(Id);
GO

-- Change Columns
ALTER TABLE targetedms.ListColumnDefinition ALTER COLUMN ListDefinitionId bigint NOT NULL;
ALTER TABLE targetedms.ListItem ALTER COLUMN ListDefinitionId bigint NOT NULL;
GO

-- Add back Constraints
ALTER TABLE targetedms.ListColumnDefinition ADD CONSTRAINT FK_ListColumn_ListDefinitionId FOREIGN KEY(ListDefinitionId) REFERENCES targetedms.ListDefinition(Id);
ALTER TABLE targetedms.ListColumnDefinition ADD CONSTRAINT UQ_ListColumn_ListDefinitionId_ColumnIndex UNIQUE(ListDefinitionId, ColumnIndex);
ALTER TABLE targetedms.ListItem ADD CONSTRAINT FK_ListItem_ListDefinitionId FOREIGN KEY(ListDefinitionId) REFERENCES targetedms.ListDefinition(Id);
GO

------------------ ListItemId ------------------------------
------------------------------------------------------------

-- Drop Constraints
ALTER TABLE targetedms.ListItemValue DROP CONSTRAINT FK_ListItemValue_ListItem;
ALTER TABLE targetedms.ListItemValue DROP CONSTRAINT UQ_ListItemValue_ListItemId_ColumnIndex;
GO

-- Change ListItemId
ALTER TABLE targetedms.ListItem DROP CONSTRAINT PK_ListItem;
GO

ALTER TABLE targetedms.ListItem ALTER COLUMN Id bigint NOT NULL;
GO

ALTER TABLE targetedms.ListItem ADD CONSTRAINT PK_ListItem PRIMARY KEY(Id);
GO

-- Change Columns
ALTER TABLE targetedms.ListItemValue ALTER COLUMN ListItemId bigint NOT NULL;
GO

-- Add back Constraints
ALTER TABLE targetedms.ListItemValue ADD CONSTRAINT FK_ListItemValue_ListItem FOREIGN KEY(ListItemId) REFERENCES targetedms.ListItem(Id);
ALTER TABLE targetedms.ListItemValue ADD CONSTRAINT UQ_ListItemValue_ListItemId_ColumnIndex UNIQUE(ListItemId, ColumnIndex);
GO

------------------ DriftTimePredictionSettingsId ------------------------------
-------------------------------------------------------------------------------

-- Drop Indexes
DROP INDEX targetedms.MeasuredDriftTime.IX_MeasuredDriftTime_DriftTimePredictionSettingsId;
GO

-- Drop Constraints
ALTER TABLE targetedms.MeasuredDriftTime DROP CONSTRAINT FK_MeasuredDriftTime_DriftTimePredictionSettings;
GO

-- Change DriftTimePredictionSettingsId
ALTER TABLE targetedms.DriftTimePredictionSettings DROP CONSTRAINT PK_DriftTimePredictionSettings;
GO

ALTER TABLE targetedms.DriftTimePredictionSettings ALTER COLUMN Id bigint NOT NULL;
GO

ALTER TABLE targetedms.DriftTimePredictionSettings ADD CONSTRAINT PK_DriftTimePredictionSettings PRIMARY KEY(Id);
GO

-- Change Columns
ALTER TABLE targetedms.MeasuredDriftTime ALTER COLUMN DriftTimePredictionSettingsId bigint;
GO

-- Add back Constraints
ALTER TABLE targetedms.MeasuredDriftTime ADD CONSTRAINT FK_MeasuredDriftTime_DriftTimePredictionSettings FOREIGN KEY (DriftTimePredictionSettingsId) REFERENCES targetedms.DriftTimePredictionSettings(Id);
GO

-- Add back Indexes
CREATE INDEX IX_MeasuredDriftTime_DriftTimePredictionSettingsId ON targetedms.MeasuredDriftTime(DriftTimePredictionSettingsId);
GO

------------------ PredictorId ------------------------------
-------------------------------------------------------------

-- Drop Indexes
DROP INDEX targetedms.Replicate.IX_Replicate_CePredictorId;
DROP INDEX targetedms.Replicate.IX_Replicate_DpPredictorId;
GO

-- Drop Constraints
ALTER TABLE targetedms.Replicate DROP CONSTRAINT FK_Replicate_PredictorCe;
ALTER TABLE targetedms.Replicate DROP CONSTRAINT FK_Replicate_PredictorDp;
ALTER TABLE targetedms.PredictorSettings DROP CONSTRAINT FK_PredictorSettings_PredictorId;
ALTER TABLE targetedms.PredictorSettings DROP CONSTRAINT UQ_PredictorSettings;
GO

-- Change PredictorId
ALTER TABLE targetedms.Predictor DROP CONSTRAINT PK_Predictor;
GO

ALTER TABLE targetedms.Predictor ALTER COLUMN Id bigint NOT NULL;
GO

ALTER TABLE targetedms.Predictor ADD CONSTRAINT PK_Predictor PRIMARY KEY(Id);
GO

-- Change Columns
ALTER TABLE targetedms.Replicate ALTER COLUMN CePredictorId bigint;
ALTER TABLE targetedms.Replicate ALTER COLUMN DpPredictorId bigint;
ALTER TABLE targetedms.PredictorSettings ALTER COLUMN PredictorId bigint NOT NULL;
ALTER TABLE targetedms.transitionpredictionsettings ALTER COLUMN CePredictorId bigint;
ALTER TABLE targetedms.transitionpredictionsettings ALTER COLUMN DpPredictorId bigint;
GO

-- Add back Constraints
ALTER TABLE targetedms.Replicate ADD CONSTRAINT FK_Replicate_PredictorCe FOREIGN KEY (CePredictorId) REFERENCES targetedms.Predictor(Id);
ALTER TABLE targetedms.Replicate ADD CONSTRAINT FK_Replicate_PredictorDp FOREIGN KEY (DpPredictorId) REFERENCES targetedms.Predictor(Id);
ALTER TABLE targetedms.PredictorSettings ADD CONSTRAINT UQ_PredictorSettings UNIQUE (PredictorId, Charge);
ALTER TABLE targetedms.PredictorSettings ADD CONSTRAINT FK_PredictorSettings_PredictorId FOREIGN KEY (PredictorId) REFERENCES targetedms.Predictor(Id);
GO

-- Add back Indexes
CREATE INDEX IX_Replicate_CePredictorId ON targetedms.Replicate(CePredictorId);
CREATE INDEX IX_Replicate_DpPredictorId ON targetedms.Replicate(DpPredictorId);
GO

