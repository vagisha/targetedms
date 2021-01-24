SELECT
    sf.InstrumentId.model AS InstrumentName,
    sf.InstrumentSerialNumber AS SerialNumber,
    MIN(sf.AcquiredTime) AS StartDate,
    MAX(sf.AcquiredTime) AS EndDate,
    COUNT(DISTINCT sf.ReplicateId) AS NoOfReplicates,
    sf.InstrumentSerialNumber AS QCFolders
FROM targetedms.SampleFile sf
GROUP BY
         sf.InstrumentSerialNumber,
         sf.InstrumentId.model