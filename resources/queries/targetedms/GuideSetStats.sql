
PARAMETERS (METRIC VARCHAR DEFAULT 'metricName')
SELECT gs.RowId AS GuideSetId,
gs.TrainingStart,
gs.TrainingEnd,
gs.ReferenceEnd,
COALESCE(p.Sequence, p2.Sequence) AS Sequence,
COUNT(COALESCE(p.Value, p2.Value)) AS NumRecords,
AVG(COALESCE(p.Value, p2.Value)) AS Mean,
STDDEV(COALESCE(p.Value, p2.Value)) AS StandardDev
FROM guideset gs
-- in most cases, the metric is coming from the targetedms.precursorchrominfo table
LEFT JOIN (
   SELECT PrecursorId.Id AS PrecursorId,
   PrecursorId.ModifiedSequence AS Sequence,
   SampleFileId.AcquiredTime AS AcquiredTime,
   CASE
    WHEN METRIC = 'retentionTime' THEN BestRetentionTime
    WHEN METRIC = 'peakArea' THEN TotalArea
    WHEN METRIC = 'fwhm' THEN MaxFWHM
    WHEN METRIC = 'fwb' THEN MaxFWB
    WHEN METRIC = 'transitionPrecursorRatio' THEN TransitionPrecursorRatio
    WHEN METRIC = 'precursorArea' THEN TotalPrecursorArea
    WHEN METRIC = 'nonPrecursorArea' THEN TotalNonPrecursorArea
    WHEN METRIC = 'massAccuracy' THEN AverageMassErrorPPM
    ELSE NULL
   END AS Value
   FROM precursorchrominfo
) as p ON METRIC != 'ratio' AND p.AcquiredTime >= gs.TrainingStart AND p.AcquiredTime <= gs.TrainingEnd
-- one metric comes from the targetedms.precursorarearatio table
LEFT JOIN (
   SELECT PrecursorChromInfoId.PrecursorId.Id AS PrecursorId,
   PrecursorChromInfoId.PrecursorId.ModifiedSequence AS Sequence,
   PrecursorChromInfoId.SampleFileId.AcquiredTime AS AcquiredTime,
   AreaRatio AS Value
   FROM precursorarearatio
) as p2 ON METRIC = 'ratio' AND p2.AcquiredTime >= gs.TrainingStart AND p2.AcquiredTime <= gs.TrainingEnd
GROUP BY gs.RowId,
gs.TrainingStart,
gs.TrainingEnd,
gs.ReferenceEnd,
p.Sequence,
p2.Sequence