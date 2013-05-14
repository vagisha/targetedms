-- Clear the representative data state for all existing containers
UPDATE targetedms.precursor set representativedatastate = 0;
UPDATE targetedms.peptidegroup set representativedatastate = 0;

SELECT core.executeJavaUpgradeCode('setContainersToExperimentType');
