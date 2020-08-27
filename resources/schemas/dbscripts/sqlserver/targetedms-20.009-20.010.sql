-- Dropping all indexes on RunId
DROP INDEX targetedms.AnnotationSettings.IX_AnnotationSettings_RunId;
DROP INDEX targetedms.CalibrationCurve.IX_CalibrationCurve_RunId;
DROP INDEX targetedms.DriftTimePredictionSettings.IX_DriftTimePredictionSettings_RunId;
DROP INDEX targetedms.IsolationScheme.IX_IsolationScheme_RunId;
DROP INDEX targetedms.FoldChange.IX_FoldChange_RunId;
DROP INDEX targetedms.Instrument.IX_Instrument_RunId;
DROP INDEX targetedms.IsotopeEnrichment.IX_IsotopeEnrichment_RunId;
DROP INDEX targetedms.IsotopeLabel.IX_IsotopeLabel_RunId;
DROP INDEX targetedms.PeptideGroup.IX_PeptideGroup_RunId;
DROP INDEX targetedms.Replicate.IX_Replicate_RunId;
DROP INDEX targetedms.RunEnzyme.IX_RunEnzyme_RunId;
DROP INDEX targetedms.RunIsotopeModification.IX_RunIsotopeModification_RunId;
DROP INDEX targetedms.RunStructuralModification.IX_RunStructuralModification_RunId;
DROP INDEX targetedms.SpectrumLibrary.IX_SpectrumLibrary_RunId;
DROP INDEX targetedms.GroupComparisonSettings.IX_GroupComparisonSettings_RunId;
DROP INDEX targetedms.AuditLogEntry.uix_auditLogEntry_version;
DROP INDEX targetedms.QuantificationSettings.IX_QuantificationSettings_RunId;
GO

---- Dropping all constraints to Runs table
ALTER TABLE targetedms.AnnotationSettings DROP CONSTRAINT FK_AnnotationSettings_Runs;
ALTER TABLE targetedms.CalibrationCurve DROP CONSTRAINT FK_CalibrationCurve_Runs;
ALTER TABLE targetedms.DriftTimePredictionSettings DROP CONSTRAINT FK_DriftTimePredictionSettings_Runs;
ALTER TABLE targetedms.GroupComparisonSettings DROP CONSTRAINT FK_GroupComparisonSettings_Runs;
ALTER TABLE targetedms.FoldChange DROP CONSTRAINT FK_FoldChange_Runs;
ALTER TABLE targetedms.IsolationScheme DROP CONSTRAINT FK_IsolationScheme_Runs;
ALTER TABLE targetedms.Instrument DROP CONSTRAINT FK_Instrument_Runs;
ALTER TABLE targetedms.IsotopeEnrichment DROP CONSTRAINT FK_IsotopeEnrichment_Runs;
ALTER TABLE targetedms.IsotopeLabel DROP CONSTRAINT FK_IsotopeLabel_Runs;
ALTER TABLE targetedms.LibrarySettings DROP CONSTRAINT PK_LibrarySettings;
ALTER TABLE targetedms.LibrarySettings DROP CONSTRAINT FK_LibrarySettings_RunId;
ALTER TABLE targetedms.ListDefinition DROP CONSTRAINT FK_List_RunId;
ALTER TABLE targetedms.ModificationSettings DROP CONSTRAINT PK_ModificationSettings;
ALTER TABLE targetedms.ModificationSettings DROP CONSTRAINT FK_ModificationSettings_Runs;
ALTER TABLE targetedms.PeptideGroup DROP CONSTRAINT FK_PeptideGroup_Runs;
ALTER TABLE targetedms.RetentionTimePredictionSettings DROP CONSTRAINT PK_RetentionTimePredictionSettings;
ALTER TABLE targetedms.RetentionTimePredictionSettings DROP CONSTRAINT FK_RetentionTimePredictionSettings_Runs;
ALTER TABLE targetedms.Replicate DROP CONSTRAINT FK_Replicate_Runs;
ALTER TABLE targetedms.RunEnzyme DROP CONSTRAINT PK_RunEnzyme;
ALTER TABLE targetedms.RunEnzyme DROP CONSTRAINT FK_RunEnzyme_Runs;
ALTER TABLE targetedms.RunIsotopeModification DROP CONSTRAINT FK_RunIsotopeModification_Runs;
ALTER TABLE targetedms.RunIsotopeModification DROP CONSTRAINT PK_RunIsotopeModification;
ALTER TABLE targetedms.RunStructuralModification DROP CONSTRAINT PK_RunStructuralModification;
ALTER TABLE targetedms.RunStructuralModification DROP CONSTRAINT FK_RunStructuralModification_Runs;
ALTER TABLE targetedms.SpectrumLibrary DROP CONSTRAINT FK_SpectrumLibrary_RunId;
ALTER TABLE targetedms.TransitionFullScanSettings DROP CONSTRAINT PK_TransitionFullScanSettings;
ALTER TABLE targetedms.TransitionFullScanSettings DROP CONSTRAINT FK_TransitionFullScanSettings_Runs;
ALTER TABLE targetedms.TransitionInstrumentSettings DROP CONSTRAINT PK_TransitionInstrumentSettings;
ALTER TABLE targetedms.TransitionInstrumentSettings DROP CONSTRAINT FK_TransitionInstrumentSettings_Runs;
ALTER TABLE targetedms.TransitionPredictionSettings DROP CONSTRAINT PK_TransitionPredictionSettings;
ALTER TABLE targetedms.TransitionPredictionSettings DROP CONSTRAINT FK_TransitionPredictionSettings_Runs;
ALTER TABLE targetedms.AuditLogEntry DROP CONSTRAINT fk_auditLogEntry_runs;
ALTER TABLE targetedms.QuantificationSettings DROP CONSTRAINT FK_QuantificationSettings_Runs;
GO

-- ExperimentRunLSID references exp.experimentrun.lsid
EXEC core.fn_dropifexists 'Runs','targetedms','INDEX','IX_Runs_ExperimentRunLSID';
GO

--------------------- Runs -----------------------------
ALTER TABLE targetedms.Runs DROP CONSTRAINT PK_Runs;
GO
ALTER TABLE targetedms.Runs ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.Runs ADD CONSTRAINT PK_Runs PRIMARY KEY (Id);
GO
---------------------------------------------------------

--- Changing
ALTER TABLE targetedms.AnnotationSettings ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.CalibrationCurve ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.DriftTimePredictionSettings ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.IsolationScheme ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.FoldChange ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.GroupComparisonSettings ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.Instrument ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.IsotopeEnrichment ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.IsotopeLabel ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.LibrarySettings ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.ListDefinition ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.ModificationSettings ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.PeptideGroup ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.Replicate ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.RetentionTimePredictionSettings ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.RunEnzyme ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.RunIsotopeModification ALTER COLUMN RunId bigint NOT NULL ;
ALTER TABLE targetedms.RunStructuralModification ALTER COLUMN RunId bigint NOT NULL ;
ALTER TABLE targetedms.SpectrumLibrary ALTER COLUMN RunId bigint NOT NULL;
ALTER TABLE targetedms.TransitionFullScanSettings ALTER COLUMN RunId bigint NOT NULL ;
ALTER TABLE targetedms.TransitionInstrumentSettings ALTER COLUMN RunId bigint NOT NULL ;
ALTER TABLE targetedms.TransitionPredictionSettings ALTER COLUMN RunId bigint NOT NULL ;
ALTER TABLE targetedms.AuditLogEntry ALTER COLUMN versionId bigint;
ALTER TABLE targetedms.QuantificationSettings ALTER COLUMN RunId bigint NOT NULL;
GO

--- Adding back FK Constraints on Runs
ALTER TABLE targetedms.AnnotationSettings ADD CONSTRAINT FK_AnnotationSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.CalibrationCurve ADD CONSTRAINT FK_CalibrationCurve_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.DriftTimePredictionSettings ADD CONSTRAINT FK_DriftTimePredictionSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.FoldChange ADD CONSTRAINT FK_FoldChange_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.GroupComparisonSettings ADD CONSTRAINT FK_GroupComparisonSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
ALTER TABLE targetedms.IsolationScheme ADD CONSTRAINT FK_IsolationScheme_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.Instrument ADD CONSTRAINT FK_Instrument_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.IsotopeEnrichment ADD CONSTRAINT FK_IsotopeEnrichment_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
ALTER TABLE targetedms.IsotopeLabel ADD CONSTRAINT FK_IsotopeLabel_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
ALTER TABLE targetedms.LibrarySettings ADD CONSTRAINT PK_LibrarySettings PRIMARY KEY (RunId);
ALTER TABLE targetedms.LibrarySettings ADD CONSTRAINT FK_LibrarySettings_RunId FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.ListDefinition ADD CONSTRAINT FK_List_RunId FOREIGN KEY(RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.ModificationSettings ADD CONSTRAINT FK_ModificationSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.ModificationSettings ADD CONSTRAINT PK_ModificationSettings PRIMARY KEY (RunId);
ALTER TABLE targetedms.PeptideGroup ADD CONSTRAINT FK_PeptideGroup_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.RetentionTimePredictionSettings ADD CONSTRAINT FK_RetentionTimePredictionSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.RetentionTimePredictionSettings ADD CONSTRAINT PK_RetentionTimePredictionSettings PRIMARY KEY (RunId)
ALTER TABLE targetedms.Replicate ADD CONSTRAINT FK_Replicate_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.RunEnzyme ADD CONSTRAINT FK_RunEnzyme_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.RunEnzyme ADD CONSTRAINT PK_RunEnzyme PRIMARY KEY (EnzymeId, RunId);
ALTER TABLE targetedms.RunIsotopeModification ADD CONSTRAINT FK_RunIsotopeModification_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.RunIsotopeModification ADD CONSTRAINT PK_RunIsotopeModification PRIMARY KEY (IsotopeModId, RunId, IsotopeLabelId);
ALTER TABLE targetedms.RunStructuralModification ADD CONSTRAINT PK_RunStructuralModification PRIMARY KEY (StructuralModId, RunId);
ALTER TABLE targetedms.RunStructuralModification ADD CONSTRAINT FK_RunStructuralModification_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.SpectrumLibrary ADD CONSTRAINT FK_SpectrumLibrary_RunId FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.TransitionFullScanSettings ADD CONSTRAINT FK_TransitionFullScanSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.TransitionFullScanSettings ADD CONSTRAINT PK_TransitionFullScanSettings PRIMARY KEY (RunId);
ALTER TABLE targetedms.TransitionInstrumentSettings ADD CONSTRAINT FK_TransitionInstrumentSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.TransitionInstrumentSettings ADD CONSTRAINT PK_TransitionInstrumentSettings PRIMARY KEY (RunId);
ALTER TABLE targetedms.TransitionPredictionSettings ADD CONSTRAINT FK_TransitionPredictionSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
ALTER TABLE targetedms.TransitionPredictionSettings ADD CONSTRAINT PK_TransitionPredictionSettings PRIMARY KEY (RunId);
ALTER TABLE targetedms.AuditLogEntry ADD CONSTRAINT fk_auditLogEntry_runs FOREIGN KEY (versionId) REFERENCES targetedms.runs(id);
ALTER TABLE targetedms.QuantificationSettings ADD CONSTRAINT FK_QuantificationSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id);
GO

-- Creating Indexes on Runs.RunId in tables
CREATE INDEX IX_AnnotationSettings_RunId ON targetedms.AnnotationSettings (RunId);
CREATE INDEX IX_CalibrationCurve_RunId ON targetedms.CalibrationCurve(RunId);
CREATE INDEX IX_DriftTimePredictionSettings_RunId ON targetedms.DriftTimePredictionSettings(RunId);
CREATE INDEX IX_IsolationScheme_RunId ON targetedms.IsolationScheme (RunId);
CREATE INDEX IX_FoldChange_RunId ON targetedms.FoldChange(RunId);
CREATE INDEX IX_Instrument_RunId ON targetedms.Instrument (RunId);
CREATE INDEX IX_IsotopeEnrichment_RunId ON targetedms.IsotopeEnrichment (RunId);
CREATE INDEX IX_IsotopeLabel_RunId ON targetedms.IsotopeLabel (RunId);
CREATE INDEX IX_PeptideGroup_RunId ON targetedms.PeptideGroup(RunId);
CREATE INDEX IX_Replicate_RunId ON targetedms.Replicate(RunId);
CREATE INDEX IX_RunEnzyme_RunId ON targetedms.RunEnzyme(RunId);
CREATE INDEX IX_RunIsotopeModification_RunId ON targetedms.RunIsotopeModification (RunId);
CREATE INDEX IX_RunStructuralModification_RunId ON targetedms.RunStructuralModification (RunId);
CREATE INDEX IX_SpectrumLibrary_RunId ON targetedms.SpectrumLibrary (RunId);
CREATE INDEX IX_GroupComparisonSettings_RunId ON targetedms.GroupComparisonSettings(RunId);
CREATE INDEX uix_auditLogEntry_version on targetedms.AuditLogEntry(versionId);
CREATE INDEX IX_QuantificationSettings_RunId ON targetedms.QuantificationSettings(RunId);
CREATE INDEX IX_Runs_ExperimentRunLSID ON targetedms.Runs(ExperimentRunLSID);
GO



