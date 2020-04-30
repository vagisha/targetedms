
SELECT RowId,
       TrainingStart,
       TrainingEnd,
       CAST(ReferenceEnd AS TIMESTAMP) AS ReferenceEnd
FROM Guideset

UNION

-- Create an implicit guide set if there aren't any, or if we have samples that were acquired before the first guide set started
SELECT RowId,
       TrainingStart,
       TrainingEnd,
       ReferenceEnd
FROM DefaultGuideSet
WHERE NOT EXISTS (SELECT 1 FROM GuideSet) OR EXISTS (SELECT 1 FROM SampleFile WHERE AcquiredTime < (SELECT MIN(TrainingStart) FROM GuideSet))