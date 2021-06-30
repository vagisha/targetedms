SELECT * FROM (

SELECT
    PrecursorId.Id,
    SampleFileId.ReplicateId.Name AS Replicate,
    SampleFileId.AcquiredTime AS AcquiredTime,
    COALESCE(ifdefined(SampleFileId.ReplicateId.Day), ifdefined(SampleFileId.ReplicateId.SampleGroup)) AS Timepoint,
    ifdefined(SampleFileId.ReplicateId.SampleGroup2) AS Grouping,
    PrecursorId.PeptideId.PeptideGroupId.Label AS ProteinName,
    PrecursorId.PeptideId.PeptideGroupId.SequenceId.SeqId AS seq,
    PrecursorId.PeptideId.Sequence AS PeptideSequence,
    PrecursorId.ModifiedSequence,
    PrecursorId.Charge,
    PrecursorId.Mz,
    PrecursorId.PeptideId.Id AS PeptideId,
    PrecursorId.Id AS PrecursorId,
    pci.Id AS PrecursorChromInfoId,
    PrecursorId.PeptideId.StandardType,
    PrecursorId.PeptideId.StartIndex,
    PrecursorId.PeptideId.EndIndex,
    SampleFileId,
    TotalArea,
    pci.SampleFileId.ReplicateId.SampleType AS SampleType,
    CAST(ifdefined(CalibratedArea) AS DOUBLE) AS CalibratedArea,
--     TotalArea * 2 / PrecursorId.Charge + 1000000 AS CalibratedArea,
    CAST(ifdefined(NormalizedArea) AS DOUBLE) AS NormalizedArea,
--     TotalArea * 3 * PrecursorId.Charge AS NormalizedArea,
    PrecursorId.PeptideId.PeptideGroupId.id as PepGroupId,
    cc.Id AS CalibrationCurveId
FROM precursorchrominfo pci
    LEFT OUTER JOIN CalibrationCurve cc ON PrecursorId.PeptideId = cc.GeneralMoleculeId
WHERE
    TotalArea IS NOT NULL

) X

WHERE (SampleType IS NULL OR SampleType IN ('qc'))