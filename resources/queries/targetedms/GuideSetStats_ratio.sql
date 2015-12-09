SELECT gs.RowId AS GuideSetId, gs.TrainingStart, gs.TrainingEnd, gs.ReferenceEnd,
Sequence,
COUNT(AreaRatio) AS NumRecords,
AVG(AreaRatio) AS Mean,
STDDEV(AreaRatio) AS StandardDev
FROM guideset gs
LEFT JOIN (
   SELECT PrecursorChromInfoId.PrecursorId.Id AS PrecursorId,
   PrecursorChromInfoId.PrecursorId.ModifiedSequence AS Sequence,
   PrecursorChromInfoId.SampleFileId.AcquiredTime AS AcquiredTime,
   AreaRatio
   FROM precursorarearatio
) as p ON p.AcquiredTime >= gs.TrainingStart AND p.AcquiredTime <= gs.TrainingEnd
GROUP BY gs.RowId, gs.TrainingStart, gs.TrainingEnd, gs.ReferenceEnd, p.Sequence