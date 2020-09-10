ALTER TABLE targetedms.AnnotationSettings ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.IsolationWindow ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.ChromatogramLibInfo ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.GeneralMoleculeAnnotation ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.IsotopeEnrichment ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.Instrument ALTER COLUMN Id TYPE bigint;
ALTER TABLE targetedms.SampleFile ALTER COLUMN InstrumentId TYPE bigint;

ALTER TABLE targetedms.ListColumnDefinition ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.ListItem ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.ListItemValue ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.MeasuredDriftTime ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.BibliospecLibInfo ALTER COLUMN Id TYPE bigint;
ALTER TABLE targetedms.ChromatogramLibInfo ALTER COLUMN Id TYPE bigint;
ALTER TABLE targetedms.HunterLibInfo ALTER COLUMN Id TYPE bigint;
ALTER TABLE targetedms.NistLibInfo ALTER COLUMN Id TYPE bigint;
ALTER TABLE targetedms.SpectrastLibInfo ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.PeptideAreaRatio ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.PeptideGroupAnnotation ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.PeptideIsotopeModification ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.PeptideStructuralModification ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.PrecursorAnnotation ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.PrecursorAreaRatio ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.PrecursorChromInfoAnnotation ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.ReplicateAnnotation ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.SampleFileChromInfo ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.SpectrastLibInfo ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.TransitionAnnotation ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.TransitionAreaRatio ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.TransitionChromInfoAnnotation ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.TransitionLoss ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.TransitionOptimization ALTER COLUMN Id TYPE bigint;

--------------------------------------------------------------------------------------------------
------------------- Update sequences to bigint --------------------------------------------------
-- Issue 41317 : Alter sequence support in pg versions >= 10
CREATE FUNCTION targetedms.handleSequences() RETURNS VOID AS $$
DECLARE
BEGIN
    IF (
        SELECT CAST(current_setting('server_version_num') as INT) >= 100000
       )
    THEN
        EXECUTE
        'ALTER SEQUENCE targetedms.runs_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.predictor_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.peptide_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.instrument_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.replicate_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.samplefile_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.peptidegroup_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.isotopelabel_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.precursorchrominfo_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.transitionchrominfo_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.structuralmodification_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.structuralmodloss_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.isotopemodification_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.peptidestructuralmodification_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.peptideisotopemodification_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.transitionarearatio_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.precursorarearatio_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.peptidearearatio_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.spectrumlibrary_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.transitionoptimization_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.transitionloss_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.peptidegroupannotation_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.precursorannotation_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.precursorchrominfoannotation_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.transitionannotation_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.transitionchrominfoannotation_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.replicateannotation_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.annotationsettings_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.isolationscheme_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.isolationwindow_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.drifttimepredictionsettings_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.measureddrifttime_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.groupcomparisonsettings_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.quantificationsettings_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.listdefinition_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.listcolumndefinition_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.listitem_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.listitemvalue_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.samplefilechrominfo_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.nistlibinfo_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.spectrastlibinfo_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.chromatogramlibinfo_id_seq as bigint;';
        EXECUTE
        'ALTER SEQUENCE targetedms.hunterlibinfo_id_seq as bigint;';
    END IF;
END
$$ LANGUAGE plpgsql;

SELECT targetedms.handleSequences();

DROP FUNCTION targetedms.handleSequences;
