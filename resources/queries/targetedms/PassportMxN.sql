SELECT
    PrecursorId.Id,
    SampleFileId.ReplicateId.Name AS Replicate,
    SampleFileId.AcquiredTime AS AcquiredTime,
    COALESCE(ifdefined(SampleFileId.ReplicateId.Day),
      ifdefined(SampleFileId.ReplicateId.SampleGroup),
      YEAR(SampleFileId.AcquiredTime) || '-' || MONTH(SampleFileId.AcquiredTime) || '-' || DAYOFMONTH(SampleFileId.AcquiredTime))
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
    Id AS PrecursorChromInfoId,
    PrecursorId.PeptideId.StandardType,
    PrecursorId.PeptideId.StartIndex,
    PrecursorId.PeptideId.EndIndex,
    SampleFileId,
    TotalArea,
    PrecursorId.PeptideId.PeptideGroupId.id as PepGroupId
FROM precursorchrominfo
WHERE TotalArea IS NOT NULL