SELECT
gs.RowId AS GuideSetId, gs.TrainingStart, gs.TrainingEnd, gs.ReferenceEnd,
Sequence,
COUNT(MaxFWB) AS NumRecords,
AVG(MaxFWB) AS Mean,
STDDEV(MaxFWB) AS StandardDev
FROM guideset gs
LEFT JOIN (
   SELECT PrecursorId.Id AS PrecursorId,
   PrecursorId.ModifiedSequence AS Sequence,
   SampleFileId.AcquiredTime AS AcquiredTime,
   MaxFWB
   FROM precursorchrominfo
) as p ON p.AcquiredTime >= gs.TrainingStart AND p.AcquiredTime <= gs.TrainingEnd
GROUP BY gs.RowId, gs.TrainingStart, gs.TrainingEnd, gs.ReferenceEnd, p.Sequence