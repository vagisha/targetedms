------------------ ReplicateID ------------------
--------------------------------------------------
ALTER TABLE targetedms.Replicate ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.ReplicateAnnotation ALTER COLUMN ReplicateId TYPE bigint;
ALTER TABLE targetedms.SampleFile ALTER COLUMN ReplicateId TYPE bigint;
ALTER TABLE targetedms.QCMetricExclusion ALTER COLUMN ReplicateId TYPE bigint;

------------------ SampleFileID ------------------
--------------------------------------------------
ALTER TABLE targetedms.SampleFile ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.TransitionChromInfo ALTER COLUMN SampleFileId TYPE bigint;
ALTER TABLE targetedms.GeneralMoleculeChromInfo ALTER COLUMN SampleFileId TYPE bigint;
ALTER TABLE targetedms.PrecursorChromInfo ALTER COLUMN SampleFileId TYPE bigint;
ALTER TABLE targetedms.SampleFileChromInfo ALTER COLUMN SampleFileId TYPE bigint;

------------------ SpectrumLibraryID ------------------
-------------------------------------------------------
ALTER TABLE targetedms.SpectrumLibrary ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.BibliospecLibInfo ALTER COLUMN SpectrumLibraryId TYPE bigint;
ALTER TABLE targetedms.ChromatogramLibInfo ALTER COLUMN SpectrumLibraryId TYPE bigint;
ALTER TABLE targetedms.HunterLibInfo ALTER COLUMN SpectrumLibraryId TYPE bigint;
ALTER TABLE targetedms.NistLibInfo ALTER COLUMN SpectrumLibraryId TYPE bigint;
ALTER TABLE targetedms.SpectrastLibInfo ALTER COLUMN SpectrumLibraryId TYPE bigint;

------------------ IsotopeModId -----------------------
-------------------------------------------------------
ALTER TABLE targetedms.IsotopeModification ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.PeptideIsotopeModification ALTER COLUMN IsotopeModId TYPE bigint;
ALTER TABLE targetedms.RunIsotopeModification ALTER COLUMN IsotopeModId TYPE bigint;

------------------ PrecursorChromInfoId ---------------------
-------------------------------------------------------------

ALTER TABLE targetedms.PrecursorChromInfo ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.PrecursorAreaRatio ALTER COLUMN PrecursorChromInfoId TYPE bigint;
ALTER TABLE targetedms.PrecursorAreaRatio ALTER COLUMN PrecursorChromInfoStdId TYPE bigint;
ALTER TABLE targetedms.PrecursorChromInfoAnnotation ALTER COLUMN PrecursorChromInfoId TYPE bigint;
ALTER TABLE targetedms.TransitionChromInfo ALTER COLUMN PrecursorChromInfoId TYPE bigint;

------------------ IsotopeLabelId ---------------------
-------------------------------------------------------
ALTER TABLE targetedms.IsotopeLabel ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.FoldChange ALTER COLUMN IsotopeLabelId TYPE bigint;
ALTER TABLE targetedms.GeneralPrecursor ALTER COLUMN IsotopeLabelId TYPE bigint;
ALTER TABLE targetedms.PeptideAreaRatio ALTER COLUMN IsotopeLabelId TYPE bigint;
ALTER TABLE targetedms.PeptideAreaRatio ALTER COLUMN IsotopeLabelStdId TYPE bigint;
ALTER TABLE targetedms.PrecursorAreaRatio ALTER COLUMN IsotopeLabelId TYPE bigint;
ALTER TABLE targetedms.PrecursorAreaRatio ALTER COLUMN IsotopeLabelStdId TYPE bigint;
ALTER TABLE targetedms.RunIsotopeModification ALTER COLUMN IsotopeLabelId TYPE bigint;
ALTER TABLE targetedms.TransitionAreaRatio ALTER COLUMN IsotopeLabelId TYPE bigint;
ALTER TABLE targetedms.TransitionAreaRatio ALTER COLUMN IsotopeLabelStdId TYPE bigint;