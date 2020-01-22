ALTER TABLE targetedms.Replicate ADD HasMidasSpectra BIT;
ALTER TABLE targetedms.Replicate ADD BatchName NVARCHAR(200);

ALTER TABLE targetedms.SampleFile ADD ExplicitGlobalStandardArea DOUBLE PRECISION;
ALTER TABLE targetedms.SampleFile ADD IonMobilityType NVARCHAR(200);

ALTER TABLE targetedms.GeneralMolecule ADD ExplicitRetentionTimeWindow DOUBLE PRECISION;

ALTER TABLE targetedms.Molecule ADD MoleculeId NVARCHAR(200);

--** changes to GeneralPrecursor **--
ALTER TABLE targetedms.GeneralPrecursor ADD Ccs DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralPrecursor ADD ExplicitIonMobilityUnits VARCHAR(200);
ALTER TABLE targetedms.GeneralPrecursor ADD ExplicitCcsSqa DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralPrecursor ADD ExplicitCompensationVoltage DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralPrecursor ADD PrecursorConcentration DOUBLE PRECISION;
EXEC sp_rename 'targetedms.GeneralPrecursor.ExplicitDriftTimeMsec', 'ExplicitIonMobility', 'COLUMN';
ALTER TABLE targetedms.GeneralPrecursor DROP COLUMN ExplicitCollisionEnergy;
ALTER TABLE targetedms.GeneralPrecursor DROP COLUMN ExplicitDriftTimeHighEnergyOffsetMsec;
ALTER TABLE targetedms.GeneralPrecursor DROP COLUMN Decoy;
ALTER TABLE targetedms.GeneralPrecursor DROP COLUMN Modified;

--** changes to GeneralTransition **--
ALTER TABLE targetedms.GeneralTransition ADD Rank INT;
GO

UPDATE targetedms.GeneralTransition
SET Rank = (SELECT libraryRank FROM targetedms.Transition t WHERE t.id = targetedms.GeneralTransition.id);

ALTER TABLE targetedms.GeneralTransition ADD Intensity DOUBLE PRECISION;
Go

UPDATE targetedms.GeneralTransition
SET Intensity = (SELECT libraryIntensity FROM targetedms.Transition t WHERE t.id = targetedms.GeneralTransition.id);

ALTER TABLE targetedms.GeneralTransition ADD Quantitative BIT;
ALTER TABLE targetedms.GeneralTransition ADD CollisionEnergy DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralTransition ADD DeclusteringPotential DOUBLE PRECISION;
EXEC sp_rename 'targetedms.GeneralTransition.SLens', 'ExplicitSLens', 'COLUMN';
EXEC sp_rename 'targetedms.GeneralTransition.ConeVoltage', 'ExplicitConeVoltage', 'COLUMN';
EXEC sp_rename 'targetedms.GeneralTransition.ExplicitDriftTimeHighEnergyOffsetMSec', 'ExplicitIonMobilityHighEnergyOffset', 'COLUMN';
ALTER TABLE targetedms.GeneralTransition DROP COLUMN ExplicitCompensationVoltage;
ALTER TABLE targetedms.GeneralTransition DROP COLUMN ExplicitDriftTimeMSec;
ALTER TABLE targetedms.GeneralTransition DROP COLUMN Decoy;

ALTER TABLE targetedms.Transition DROP COLUMN LibraryRank;
ALTER TABLE targetedms.Transition DROP COLUMN LibraryIntensity;

ALTER TABLE targetedms.MoleculePrecursor ADD MoleculePrecursorId NVARCHAR(200);

ALTER TABLE targetedms.MoleculeTransition ADD MoleculeTransitionId NVARCHAR(200);

ALTER TABLE targetedms.GeneralMoleculeChromInfo ADD PredictedRetentionTime DOUBLE PRECISION;

ALTER TABLE targetedms.PrecursorChromInfo ADD Ccs DOUBLE PRECISION;
ALTER TABLE targetedms.PrecursorChromInfo ADD DriftTimeMs1 DOUBLE PRECISION;
ALTER TABLE targetedms.PrecursorChromInfo ADD DriftTimeFragment DOUBLE PRECISION;
ALTER TABLE targetedms.PrecursorChromInfo ADD DriftTimeWindow DOUBLE PRECISION;
ALTER TABLE targetedms.PrecursorChromInfo ADD IonMobilityMs1 DOUBLE PRECISION;
ALTER TABLE targetedms.PrecursorChromInfo ADD IonMobilityFragment DOUBLE PRECISION;
ALTER TABLE targetedms.PrecursorChromInfo ADD IonMobilityWindow DOUBLE PRECISION;
ALTER TABLE targetedms.PrecursorChromInfo ADD IonMobilityType NVARCHAR(200);

ALTER TABLE targetedms.TransitionChromInfo ADD Ccs DOUBLE PRECISION;
ALTER TABLE targetedms.TransitionChromInfo ADD DriftTime DOUBLE PRECISION;
ALTER TABLE targetedms.TransitionChromInfo ADD DriftTimeWindow DOUBLE PRECISION;
ALTER TABLE targetedms.TransitionChromInfo ADD IonMobility DOUBLE PRECISION;
ALTER TABLE targetedms.TransitionChromInfo ADD IonMobilityWindow DOUBLE PRECISION;
ALTER TABLE targetedms.TransitionChromInfo ADD IonMobilityType NVARCHAR(200);
ALTER TABLE targetedms.TransitionChromInfo ADD Rank INT;
ALTER TABLE targetedms.TransitionChromInfo ADD RankByLevel INTEGER;
ALTER TABLE targetedms.TransitionChromInfo ADD ForcedIntegration BIT;

ALTER TABLE targetedms.GroupComparisonSettings ADD AvgTechReplicates BIT;
ALTER TABLE targetedms.GroupComparisonSettings ADD SumTransitions BIT;
ALTER TABLE targetedms.GroupComparisonSettings ADD IncludeInteractionTransitions BIT;
ALTER TABLE targetedms.GroupComparisonSettings ADD SummarizationMethod NVARCHAR(200);

ALTER TABLE targetedms.Enzyme ADD Semi BIT;

ALTER TABLE targetedms.SpectrumLibrary ADD UseExplicitPeakBounds BIT;

ALTER TABLE targetedms.IsotopeModification ADD Label37Cl BIT;
ALTER TABLE targetedms.IsotopeModification ADD Label81Br BIT;
