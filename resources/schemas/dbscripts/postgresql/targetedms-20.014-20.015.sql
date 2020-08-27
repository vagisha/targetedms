---------------- TransitionChromInfoId --------------------
-----------------------------------------------------------
ALTER TABLE targetedms.TransitionChromInfo ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.TransitionAreaRatio ALTER COLUMN TransitionChromInfoId TYPE bigint;
ALTER TABLE targetedms.TransitionAreaRatio ALTER COLUMN TransitionChromInfoStdId TYPE bigint;
ALTER TABLE targetedms.TransitionChromInfoAnnotation ALTER COLUMN TransitionChromInfoId TYPE bigint;

---------------- GeneralMoleculeChromInfoId --------------------
----------------------------------------------------------------
ALTER TABLE targetedms.GeneralMoleculeChromInfo ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.PrecursorChromInfo ALTER COLUMN GeneralMoleculeChromInfoId TYPE bigint;
ALTER TABLE targetedms.PeptideAreaRatio ALTER COLUMN PeptideChromInfoId TYPE bigint;
ALTER TABLE targetedms.PeptideAreaRatio ALTER COLUMN PeptideChromInfoStdId TYPE bigint;

---------------- StructuralModLossId --------------------
---------------------------------------------------------
ALTER TABLE targetedms.StructuralModLoss ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.TransitionLoss ALTER COLUMN StructuralModLossId TYPE bigint;

---------------- StructuralModId ------------------------------
----------------------------------------------------------------
ALTER TABLE targetedms.StructuralModification ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.PeptideStructuralModification ALTER COLUMN StructuralModId TYPE bigint;
ALTER TABLE targetedms.RunStructuralModification ALTER COLUMN StructuralModId TYPE bigint;
ALTER TABLE targetedms.StructuralModLoss ALTER COLUMN StructuralModId TYPE bigint;

------------------ QuantificationSettingsId ------------------------------
--------------------------------------------------------------------------
ALTER TABLE targetedms.QuantificationSettings ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.CalibrationCurve ALTER COLUMN QuantificationSettingsId TYPE bigint;

------------------ GroupComparisonSettingsId ------------------------------
---------------------------------------------------------------------------
ALTER TABLE targetedms.GroupComparisonSettings ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.FoldChange ALTER COLUMN GroupComparisonSettingsId TYPE bigint;

------------------ IsolationSchemeId ------------------------------
--------------------------------------------------------------------
ALTER TABLE targetedms.IsolationScheme ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.IsolationWindow ALTER COLUMN IsolationSchemeId TYPE bigint;

------------------ ListDefinitionId ------------------------------
------------------------------------------------------------------
ALTER TABLE targetedms.ListDefinition ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.ListColumnDefinition ALTER COLUMN ListDefinitionId TYPE bigint;
ALTER TABLE targetedms.ListItem ALTER COLUMN ListDefinitionId TYPE bigint;

------------------ ListItemId ------------------------------
------------------------------------------------------------
ALTER TABLE targetedms.ListItem ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.ListItemValue ALTER COLUMN ListItemId TYPE bigint;

------------------ DriftTimePredictionSettingsId ------------------------------
-------------------------------------------------------------------------------
ALTER TABLE targetedms.DriftTimePredictionSettings ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.MeasuredDriftTime ALTER COLUMN DriftTimePredictionSettingsId TYPE bigint;

------------------ PredictorId ------------------------------
-------------------------------------------------------------
ALTER TABLE targetedms.Predictor ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.Replicate ALTER COLUMN CePredictorId TYPE bigint;
ALTER TABLE targetedms.Replicate ALTER COLUMN DpPredictorId TYPE bigint;
ALTER TABLE targetedms.PredictorSettings ALTER COLUMN PredictorId TYPE bigint;
ALTER TABLE targetedms.transitionpredictionsettings ALTER COLUMN CePredictorId TYPE bigint;
ALTER TABLE targetedms.transitionpredictionsettings ALTER COLUMN DpPredictorId TYPE bigint;


