SELECT gs.RowId AS GuideSetId,
gs.TrainingStart,
gs.TrainingEnd,
gs.ReferenceEnd,
p.Sequence,
COUNT(p.Value) AS NumRecords,
AVG(p.Value) AS Mean,
STDDEV(p.Value) AS StandardDev
FROM guideset gs
LEFT JOIN (
   SELECT PrecursorId.Id AS PrecursorId,
   PrecursorId.ModifiedSequence AS Sequence,
   PeptideChromInfoId.SampleFileId.AcquiredTime AS AcquiredTime,
   TotalArea AS Value
   FROM precursorchrominfo
) as p ON p.AcquiredTime >= gs.TrainingStart AND p.AcquiredTime <= gs.TrainingEnd
GROUP BY gs.RowId,
gs.TrainingStart,
gs.TrainingEnd,
gs.ReferenceEnd,
p.Sequence