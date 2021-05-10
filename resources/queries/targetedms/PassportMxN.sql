SELECT
    PrecursorId.Id,
    SampleFileId.ReplicateId.Name AS Replicate,
    SampleFileId.AcquiredTime AS AcquiredTime,
    COALESCE(ifdefined(SampleFileId.ReplicateId.Day),
      ifdefined(SampleFileId.ReplicateId.SampleGroup),
      YEAR(SampleFileId.AcquiredTime) || '-' ||
        CASE WHEN MONTH(SampleFileId.AcquiredTime) < 10 THEN '0' ELSE '' END || MONTH(SampleFileId.AcquiredTime) || '-' ||
        CASE WHEN DAYOFMONTH(SampleFileId.AcquiredTime) < 10 THEN '0' ELSE '' END || DAYOFMONTH(SampleFileId.AcquiredTime))
      AS Timepoint,
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
    PrecursorId.PeptideId.PeptideGroupId.id as PepGroupId,
    cc.Id AS CalibrationCurveId
FROM precursorchrominfo pci
    LEFT OUTER JOIN CalibrationCurve cc ON PrecursorId.PeptideId = cc.GeneralMoleculeId
WHERE TotalArea IS NOT NULL