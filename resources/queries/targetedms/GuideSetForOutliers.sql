
SELECT RowId,
       TrainingStart,
       TrainingEnd,
       ReferenceEnd
FROM Guideset

UNION

SELECT RowId,
       TrainingStart,
       TrainingEnd,
       ReferenceEnd
FROM DefaultGuideSet
WHERE NOT EXISTS (SELECT 1 FROM GuideSet)