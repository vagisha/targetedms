SELECT stats.GuideSetId,
'RT' AS Metric,
SUM(CASE WHEN X.Value > (stats.Mean + (3 * stats.StandardDev)) OR X.Value < (stats.Mean - (3 * stats.StandardDev)) THEN 1 ELSE 0 END) AS NonConformers
FROM (
  SELECT PrecursorId.ModifiedSequence AS Sequence,
  PeptideChromInfoId.SampleFileId.AcquiredTime AS AcquiredTime,
  BestRetentionTime AS Value
  FROM PrecursorChromInfo
) X
LEFT JOIN GuideSetRetentionTimeStats stats
  ON X.Sequence = stats.Sequence
  AND ((X.AcquiredTime >= stats.TrainingStart AND X.AcquiredTime < stats.ReferenceEnd)
    OR (X.AcquiredTime >= stats.TrainingStart AND stats.ReferenceEnd IS NULL))
WHERE stats.GuideSetId IS NOT NULL
GROUP BY stats.GuideSetId

UNION SELECT stats.GuideSetId,
'PA' AS Metric,
SUM(CASE WHEN X.Value > (stats.Mean + (3 * stats.StandardDev)) OR X.Value < (stats.Mean - (3 * stats.StandardDev)) THEN 1 ELSE 0 END) AS NonConformers
FROM (
  SELECT PrecursorId.ModifiedSequence AS Sequence,
  PeptideChromInfoId.SampleFileId.AcquiredTime AS AcquiredTime,
  TotalArea AS Value
  FROM PrecursorChromInfo
) X
LEFT JOIN GuideSetPeakAreaStats stats
  ON X.Sequence = stats.Sequence
  AND ((X.AcquiredTime >= stats.TrainingStart AND X.AcquiredTime < stats.ReferenceEnd)
    OR (X.AcquiredTime >= stats.TrainingStart AND stats.ReferenceEnd IS NULL))
WHERE stats.GuideSetId IS NOT NULL
GROUP BY stats.GuideSetId

UNION SELECT stats.GuideSetId,
'FWHM' AS Metric,
SUM(CASE WHEN X.Value > (stats.Mean + (3 * stats.StandardDev)) OR X.Value < (stats.Mean - (3 * stats.StandardDev)) THEN 1 ELSE 0 END) AS NonConformers
 FROM (SELECT PrecursorId.ModifiedSequence AS Sequence,
       PeptideChromInfoId.SampleFileId.AcquiredTime AS AcquiredTime,
       MaxFWHM AS Value FROM PrecursorChromInfo
) X
LEFT JOIN GuideSetFWHMStats stats
  ON X.Sequence = stats.Sequence
  AND ((X.AcquiredTime >= stats.TrainingStart AND X.AcquiredTime < stats.ReferenceEnd)
    OR (X.AcquiredTime >= stats.TrainingStart AND stats.ReferenceEnd IS NULL))
WHERE stats.GuideSetId IS NOT NULL
GROUP BY stats.GuideSetId

UNION SELECT stats.GuideSetId,
'FWB' As Metric,
SUM(CASE WHEN X.Value > (stats.Mean + (3 * stats.StandardDev)) OR X.Value < (stats.Mean - (3 * stats.StandardDev)) THEN 1 ELSE 0 END) AS NonConformers
 FROM (SELECT PrecursorId.ModifiedSequence AS Sequence,
       PeptideChromInfoId.SampleFileId.AcquiredTime AS AcquiredTime,
       PeptideChromInfoId.SampleFileId.FilePath AS FilePath,
       (MaxEndTime - MinStartTime) AS Value FROM PrecursorChromInfo) X
 LEFT JOIN GuideSetFWBStats stats
ON X.Sequence = stats.Sequence AND ((X.AcquiredTime >= stats.TrainingStart
AND X.AcquiredTime < stats.ReferenceEnd) OR (X.AcquiredTime >= stats.TrainingStart AND stats.ReferenceEnd IS NULL))
WHERE stats.GuideSetId IS NOT NULL
GROUP BY stats.GuideSetId

UNION SELECT stats.GuideSetId,
'L/H ratio' As Metric,
SUM(CASE WHEN X.Value > (stats.Mean + (3 * stats.StandardDev)) OR X.Value < (stats.Mean - (3 * stats.StandardDev)) THEN 1 ELSE 0 END) AS NonConformers
 FROM (SELECT PrecursorChromInfoId.PrecursorId.ModifiedSequence AS Sequence,
       PrecursorChromInfoId.PeptideChromInfoId.SampleFileId.AcquiredTime AS AcquiredTime,
       PrecursorChromInfoId.PeptideChromInfoId.SampleFileId.FilePath AS FilePath,
       AreaRatio AS Value FROM PrecursorAreaRatio) X
 LEFT JOIN GuideSetLHRatioStats stats
ON X.Sequence = stats.Sequence AND ((X.AcquiredTime >= stats.TrainingStart
AND X.AcquiredTime < stats.ReferenceEnd) OR (X.AcquiredTime >= stats.TrainingStart AND stats.ReferenceEnd IS NULL))
WHERE stats.GuideSetId IS NOT NULL
GROUP BY stats.GuideSetId

UNION SELECT stats.GuideSetId,
'T/PA Ratio' As Metric,
SUM(CASE WHEN X.Value > (stats.Mean + (3 * stats.StandardDev)) OR X.Value < (stats.Mean - (3 * stats.StandardDev)) THEN 1 ELSE 0 END) AS NonConformers
 FROM (SELECT PrecursorId.ModifiedSequence AS Sequence,
       PeptideChromInfoId.SampleFileId.AcquiredTime AS AcquiredTime,
       PeptideChromInfoId.SampleFileId.FilePath AS FilePath,
       transitionPrecursorRatio AS Value FROM PrecursorChromInfo) X
 LEFT JOIN GuideSetTPRatioStats stats
ON X.Sequence = stats.Sequence AND ((X.AcquiredTime >= stats.TrainingStart
AND X.AcquiredTime < stats.ReferenceEnd) OR (X.AcquiredTime >= stats.TrainingStart AND stats.ReferenceEnd IS NULL))
WHERE stats.GuideSetId IS NOT NULL
GROUP BY stats.GuideSetId