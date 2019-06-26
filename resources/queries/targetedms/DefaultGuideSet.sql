
SELECT
       0 AS RowId,
       CAST('01/01/1900' AS TIMESTAMP) AS TrainingStart,
       curdate() AS TrainingEnd,
       CAST(NULL AS TIMESTAMP) AS ReferenceEnd