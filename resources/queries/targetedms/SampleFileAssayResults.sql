SELECT
SampleName,
ReplicateId.RunId.FileName,
ReplicateId.RunId.Id,
AcquiredTime,
ReplicateId.RunId.PeptideGroupCount,
ReplicateId.RunId.PeptideCount,
ReplicateId.RunId.SmallMoleculeCount,
ReplicateId.RunId.PrecursorCount,
ReplicateId.RunId.TransitionCount,
ReplicateId.RunId.ReplicateCount
FROM SampleFile
WHERE ReplicateId.RunId.Deleted = FALSE