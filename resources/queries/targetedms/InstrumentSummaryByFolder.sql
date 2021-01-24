SELECT
       COUNT(ReplicateId.RunId) AS SkylineDocumentCount,
       COUNT(DISTINCT ReplicateId) AS ReplicateCount,
       MIN(AcquiredTime) AS FirstAcquisition,
       MAX(AcquiredTime) AS LastAcquisition,
       ReplicateId.RunId.Container,
       InstrumentSerialNumber
FROM targetedms.SampleFile
GROUP BY
         ReplicateId.RunId.Container,
         InstrumentSerialNumber