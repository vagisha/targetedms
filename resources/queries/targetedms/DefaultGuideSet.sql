
SELECT
       0 AS RowId,
       COALESCE(MIN(AcquiredTime), curdate()) AS TrainingStart,
       COALESCE(MAX(AcquiredTime), curdate()) AS TrainingEnd,
       -- ReferenceEnd should be null if there's no subsequent guide set, or the value of the start of the next guide
       -- set if there is
       (SELECT MIN(TrainingStart) FROM GuideSet) AS ReferenceEnd
FROM targetedms.SampleFile WHERE AcquiredTime < COALESCE((SELECT MIN(TrainingStart) FROM GuideSet), curdate())