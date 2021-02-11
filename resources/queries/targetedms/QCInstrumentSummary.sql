SELECT
    sf.InstrumentId.model AS InstrumentName,
    sf.InstrumentSerialNumber AS SerialNumber,
    MIN(sf.AcquiredTime) AS StartDate,
    MAX(sf.AcquiredTime) AS EndDate,
    COUNT(DISTINCT sf.ReplicateId) AS NoOfReplicates,
    sf.InstrumentSerialNumber AS QCFolders,
    rep.runId  @hidden
FROM targetedms.SampleFile sf
INNER JOIN replicate rep ON sf.replicateId = rep.Id
GROUP BY
         sf.InstrumentSerialNumber,
         sf.InstrumentId.model,
         rep.runId