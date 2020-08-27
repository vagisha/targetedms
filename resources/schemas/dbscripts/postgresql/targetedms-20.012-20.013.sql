
------------------ PrecursorID ------------------
-------------------------------------------------
ALTER TABLE targetedms.Precursor ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.BibliospecLibInfo ALTER COLUMN PrecursorId TYPE bigint;
ALTER TABLE targetedms.ChromatogramLibInfo ALTER COLUMN PrecursorId TYPE bigint;
ALTER TABLE targetedms.HunterLibInfo ALTER COLUMN PrecursorId TYPE bigint;
ALTER TABLE targetedms.NistLibInfo ALTER COLUMN PrecursorId TYPE bigint;
ALTER TABLE targetedms.SpectrastLibInfo ALTER COLUMN PrecursorId TYPE bigint;

------------------ GeneralPrecursorID ---------------
-----------------------------------------------------
ALTER TABLE targetedms.GeneralPrecursor ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.GeneralTransition ALTER COLUMN GeneralPrecursorId TYPE bigint;
ALTER TABLE targetedms.PrecursorAnnotation ALTER COLUMN PrecursorId TYPE bigint;
ALTER TABLE targetedms.PrecursorChromInfo ALTER COLUMN PrecursorId TYPE bigint;
ALTER TABLE targetedms.MoleculePrecursor ALTER COLUMN Id TYPE bigint;

------------------ GeneralTransitionID ---------------
-----------------------------------------------------
ALTER TABLE targetedms.GeneralTransition ALTER COLUMN Id TYPE bigint;

ALTER TABLE targetedms.MoleculeTransition ALTER COLUMN TransitionId TYPE bigint;
ALTER TABLE targetedms.Transition ALTER COLUMN Id TYPE bigint;
ALTER TABLE targetedms.TransitionAnnotation ALTER COLUMN TransitionId TYPE bigint;
ALTER TABLE targetedms.TransitionChromInfo ALTER COLUMN TransitionId TYPE bigint;
ALTER TABLE targetedms.TransitionLoss ALTER COLUMN TransitionId TYPE bigint;
ALTER TABLE targetedms.TransitionOptimization ALTER COLUMN TransitionId TYPE bigint;