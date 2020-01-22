ALTER TABLE targetedms.Replicate ADD COLUMN HasMidasSpectra BOOLEAN;
ALTER TABLE targetedms.Replicate ADD COLUMN BatchName VARCHAR(200);

ALTER TABLE targetedms.SampleFile ADD COLUMN ExplicitGlobalStandardArea DOUBLE PRECISION;
ALTER TABLE targetedms.SampleFile ADD COLUMN IonMobilityType VARCHAR(200);

ALTER TABLE targetedms.GeneralMolecule ADD COLUMN ExplicitRetentionTimeWindow DOUBLE PRECISION;

ALTER TABLE targetedms.Molecule ADD COLUMN MoleculeId VARCHAR(200);

--** changes to GeneralPrecursor **--
ALTER TABLE targetedms.GeneralPrecursor ADD COLUMN Ccs DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralPrecursor ADD COLUMN ExplicitIonMobilityUnits VARCHAR(200);
ALTER TABLE targetedms.GeneralPrecursor ADD COLUMN ExplicitCcsSqa DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralPrecursor ADD COLUMN ExplicitCompensationVoltage DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralPrecursor ADD COLUMN PrecursorConcentration DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralPrecursor RENAME COLUMN ExplicitDriftTimeMsec TO ExplicitIonMobility;
ALTER TABLE targetedms.GeneralPrecursor DROP COLUMN ExplicitCollisionEnergy;
ALTER TABLE targetedms.GeneralPrecursor DROP COLUMN ExplicitDriftTimeHighEnergyOffsetMsec;
ALTER TABLE targetedms.GeneralPrecursor DROP COLUMN Decoy;
ALTER TABLE targetedms.GeneralPrecursor DROP COLUMN Modified;

--** changes to GeneralTransition **--
ALTER TABLE targetedms.GeneralTransition ADD COLUMN Rank INT ;
UPDATE targetedms.GeneralTransition gt
SET Rank = (SELECT libraryRank FROM targetedms.Transition t WHERE t.id = gt.id);
ALTER TABLE targetedms.GeneralTransition ADD COLUMN Intensity DOUBLE PRECISION ;
UPDATE targetedms.GeneralTransition gt
SET Intensity = (SELECT libraryIntensity FROM targetedms.Transition t WHERE t.id = gt.id);
ALTER TABLE targetedms.GeneralTransition ADD COLUMN Quantitative BOOLEAN;
ALTER TABLE targetedms.GeneralTransition ADD COLUMN CollisionEnergy DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralTransition ADD COLUMN DeclusteringPotential DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralTransition RENAME COLUMN SLens TO ExplicitSLens;
ALTER TABLE targetedms.GeneralTransition RENAME COLUMN ConeVoltage TO ExplicitConeVoltage;
ALTER TABLE targetedms.GeneralTransition RENAME COLUMN ExplicitDriftTimeHighEnergyOffsetMSec TO ExplicitIonMobilityHighEnergyOffset;
ALTER TABLE targetedms.GeneralTransition DROP COLUMN ExplicitCompensationVoltage;
ALTER TABLE targetedms.GeneralTransition DROP COLUMN ExplicitDriftTimeMSec;
ALTER TABLE targetedms.GeneralTransition DROP COLUMN Decoy;

ALTER TABLE targetedms.Transition DROP COLUMN LibraryRank;
ALTER TABLE targetedms.Transition DROP COLUMN LibraryIntensity;

ALTER TABLE targetedms.MoleculePrecursor ADD COLUMN MoleculePrecursorId VARCHAR(200);

ALTER TABLE targetedms.MoleculeTransition ADD COLUMN MoleculeTransitionId VARCHAR(200);

ALTER TABLE targetedms.GeneralMoleculeChromInfo ADD COLUMN PredictedRetentionTime DOUBLE PRECISION;

ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN Ccs DOUBLE PRECISION;
ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN DriftTimeMs1 DOUBLE PRECISION;
ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN DriftTimeFragment DOUBLE PRECISION;
ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN DriftTimeWindow DOUBLE PRECISION;
ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN IonMobilityMs1 DOUBLE PRECISION;
ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN IonMobilityFragment DOUBLE PRECISION;
ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN IonMobilityWindow DOUBLE PRECISION;
ALTER TABLE targetedms.PrecursorChromInfo ADD COLUMN IonMobilityType VARCHAR(200);

ALTER TABLE targetedms.TransitionChromInfo ADD COLUMN Ccs DOUBLE PRECISION;
ALTER TABLE targetedms.TransitionChromInfo ADD COLUMN DriftTime DOUBLE PRECISION;
ALTER TABLE targetedms.TransitionChromInfo ADD COLUMN DriftTimeWindow DOUBLE PRECISION;
ALTER TABLE targetedms.TransitionChromInfo ADD COLUMN IonMobility DOUBLE PRECISION;
ALTER TABLE targetedms.TransitionChromInfo ADD COLUMN IonMobilityWindow DOUBLE PRECISION;
ALTER TABLE targetedms.TransitionChromInfo ADD COLUMN IonMobilityType VARCHAR(200);
ALTER TABLE targetedms.TransitionChromInfo ADD COLUMN Rank INT;
ALTER TABLE targetedms.TransitionChromInfo ADD COLUMN RankByLevel INT;
ALTER TABLE targetedms.TransitionChromInfo ADD COLUMN ForcedIntegration BOOLEAN;

ALTER TABLE targetedms.GroupComparisonSettings ADD COLUMN AvgTechReplicates BOOLEAN;
ALTER TABLE targetedms.GroupComparisonSettings ADD COLUMN SumTransitions BOOLEAN;
ALTER TABLE targetedms.GroupComparisonSettings ADD COLUMN IncludeInteractionTransitions BOOLEAN;
ALTER TABLE targetedms.GroupComparisonSettings ADD COLUMN SummarizationMethod VARCHAR(200);

ALTER TABLE targetedms.Enzyme ADD COLUMN Semi BOOLEAN;

ALTER TABLE targetedms.SpectrumLibrary ADD COLUMN UseExplicitPeakBounds BOOLEAN;

ALTER TABLE targetedms.IsotopeModification ADD COLUMN Label37Cl BOOLEAN;
ALTER TABLE targetedms.IsotopeModification ADD COLUMN Label81Br BOOLEAN;


