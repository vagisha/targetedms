/*
 * Copyright (c) 2015 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
SELECT stats.GuideSetId,
'RT' AS Metric,
'Retention Time' AS MetricLongLabel,
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
'Peak Area' AS MetricLongLabel,
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
'Full Width at Half Maximum (FWHM)' AS MetricLongLabel,
SUM(CASE WHEN X.Value > (stats.Mean + (3 * stats.StandardDev)) OR X.Value < (stats.Mean - (3 * stats.StandardDev)) THEN 1 ELSE 0 END) AS NonConformers
FROM (
  SELECT PrecursorId.ModifiedSequence AS Sequence,
  PeptideChromInfoId.SampleFileId.AcquiredTime AS AcquiredTime,
  MaxFWHM AS Value
  FROM PrecursorChromInfo
) X
LEFT JOIN GuideSetFWHMStats stats
  ON X.Sequence = stats.Sequence
  AND ((X.AcquiredTime >= stats.TrainingStart AND X.AcquiredTime < stats.ReferenceEnd)
    OR (X.AcquiredTime >= stats.TrainingStart AND stats.ReferenceEnd IS NULL))
WHERE stats.GuideSetId IS NOT NULL
GROUP BY stats.GuideSetId

UNION SELECT stats.GuideSetId,
'FWB' As Metric,
'Full Width at Base (FWB)' As MetricLongLabel,
SUM(CASE WHEN X.Value > (stats.Mean + (3 * stats.StandardDev)) OR X.Value < (stats.Mean - (3 * stats.StandardDev)) THEN 1 ELSE 0 END) AS NonConformers
FROM (
  SELECT PrecursorId.ModifiedSequence AS Sequence,
  PeptideChromInfoId.SampleFileId.AcquiredTime AS AcquiredTime,
  (MaxEndTime - MinStartTime) AS Value
  FROM PrecursorChromInfo
) X
LEFT JOIN GuideSetFWBStats stats
  ON X.Sequence = stats.Sequence
  AND ((X.AcquiredTime >= stats.TrainingStart AND X.AcquiredTime < stats.ReferenceEnd)
    OR (X.AcquiredTime >= stats.TrainingStart AND stats.ReferenceEnd IS NULL))
WHERE stats.GuideSetId IS NOT NULL
GROUP BY stats.GuideSetId

UNION SELECT stats.GuideSetId,
'L/H ratio' As Metric,
'Light/Heavy Ratio' As MetricLongLabel,
SUM(CASE WHEN X.Value > (stats.Mean + (3 * stats.StandardDev)) OR X.Value < (stats.Mean - (3 * stats.StandardDev)) THEN 1 ELSE 0 END) AS NonConformers
FROM (
  SELECT PrecursorChromInfoId.PrecursorId.ModifiedSequence AS Sequence,
  PrecursorChromInfoId.PeptideChromInfoId.SampleFileId.AcquiredTime AS AcquiredTime,
  AreaRatio AS Value
  FROM PrecursorAreaRatio
) X
LEFT JOIN GuideSetLHRatioStats stats
  ON X.Sequence = stats.Sequence
  AND ((X.AcquiredTime >= stats.TrainingStart AND X.AcquiredTime < stats.ReferenceEnd)
    OR (X.AcquiredTime >= stats.TrainingStart AND stats.ReferenceEnd IS NULL))
WHERE stats.GuideSetId IS NOT NULL
GROUP BY stats.GuideSetId

UNION SELECT stats.GuideSetId,
'T/PA Ratio' As Metric,
'Transition/Precursor Area Ratio' As MetricLongLabel,
SUM(CASE WHEN X.Value > (stats.Mean + (3 * stats.StandardDev)) OR X.Value < (stats.Mean - (3 * stats.StandardDev)) THEN 1 ELSE 0 END) AS NonConformers
FROM (
  SELECT PrecursorId.ModifiedSequence AS Sequence,
  PeptideChromInfoId.SampleFileId.AcquiredTime AS AcquiredTime,
  transitionPrecursorRatio AS Value
  FROM PrecursorChromInfo
) X
LEFT JOIN GuideSetTPRatioStats stats
  ON X.Sequence = stats.Sequence
  AND ((X.AcquiredTime >= stats.TrainingStart AND X.AcquiredTime < stats.ReferenceEnd)
    OR (X.AcquiredTime >= stats.TrainingStart AND stats.ReferenceEnd IS NULL))
WHERE stats.GuideSetId IS NOT NULL
GROUP BY stats.GuideSetId