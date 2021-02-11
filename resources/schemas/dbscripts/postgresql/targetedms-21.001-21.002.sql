
UPDATE targetedms.PrecursorChromInfo SET IonMobilityType = 'drift_time_ms'
    WHERE IonMobilityType IS NULL AND (DriftTimeMS1 IS NOT NULL OR DriftTimeFragment IS NOT NULL OR DriftTimeWindow IS NOT NULL);
UPDATE targetedms.PrecursorChromInfo SET IonMobilityMS1 = DriftTimeMS1
    WHERE IonMobilityMS1 IS NULL AND DriftTimeMS1 IS NOT NULL;
UPDATE targetedms.PrecursorChromInfo SET IonMobilityFragment = DriftTimeFragment
    WHERE IonMobilityFragment IS NULL AND DriftTimeFragment IS NOT NULL;
UPDATE targetedms.PrecursorChromInfo SET IonMobilityWindow = DriftTimeWindow
    WHERE IonMobilityWindow IS NULL AND DriftTimeWindow IS NOT NULL;

ALTER TABLE targetedms.PrecursorChromInfo DROP COLUMN DriftTimeMS1,
                                          DROP COLUMN DriftTimeFragment,
                                          DROP COLUMN DriftTimeWindow;

UPDATE targetedms.TransitionChromInfo SET IonMobilityType = 'drift_time_ms'
    WHERE IonMobilityType IS NULL AND (DriftTime IS NOT NULL OR DriftTimeWindow IS NOT NULL);
UPDATE targetedms.TransitionChromInfo SET IonMobility = DriftTime
    WHERE IonMobility IS NULL AND DriftTime IS NOT NULL;
UPDATE targetedms.TransitionChromInfo SET IonMobilityWindow = DriftTimeWindow
    WHERE IonMobilityWindow IS NULL AND DriftTimeWindow IS NOT NULL;

ALTER TABLE targetedms.TransitionChromInfo DROP COLUMN DriftTime,
                                           DROP COLUMN DriftTimeWindow;
