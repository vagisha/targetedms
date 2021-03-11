SELECT PrecursorId.Id,
  SampleFileId.ReplicateId.Name AS Replicate,
  SampleFileId.AcquiredTime AS AcquiredTime,
  COALESCE(CAST(ifdefined(SampleFileId.ReplicateId.Day) AS VARCHAR),
      CAST(YEAR(SampleFileId.AcquiredTime) AS VARCHAR) || '-' || (CASE WHEN MONTH(SampleFileId.AcquiredTime) < 10 THEN '0' ELSE '' END) || CAST(MONTH(SampleFileId.AcquiredTime) AS VARCHAR) || '-' || (CASE WHEN DAYOFMONTH(SampleFileId.AcquiredTime) < 10 THEN '0' ELSE '' END) || CAST(DAYOFMONTH(SampleFileId.AcquiredTime) AS VARCHAR))
      AS Timepoint,
  ifdefined(SampleFileId.ReplicateId.SampleGroup) AS Grouping,
  PrecursorId.PeptideId.PeptideGroupId.Label AS ProteinName,
  PrecursorId.PeptideId.PeptideGroupId.SequenceId.SeqId AS seq,
  PrecursorId.PeptideId.Sequence AS PeptideSequence,
  PrecursorId.PeptideId.Id AS PeptideId,
  PrecursorId.Id AS PrecursorId,
  Id AS PanoramaPrecursorId,
  PrecursorId.PeptideId.StandardType,
  PrecursorId.PeptideId.StartIndex,
  PrecursorId.PeptideId.EndIndex,
  TotalArea,
  SampleFileId,
  PrecursorId.PeptideId.PeptideGroupId.id as PepGroupId,
  (SELECT SUM(pci.TotalArea) AS SumArea
   FROM precursorchrominfo AS pci
   WHERE pci.PrecursorId.PeptideId.StandardType='Normalization'
         AND pci.SampleFileId = precursorchrominfo.SampleFileId) AS SumArea
FROM precursorchrominfo
