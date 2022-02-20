SELECT
    sf.*,
    e.ExcludedMetricIds,
    COALESCE(gs.RowId, 0) AS GuideSetId,
    CASE WHEN (sf.AcquiredTime >= gs.TrainingStart AND sf.AcquiredTime <= gs.TrainingEnd) THEN TRUE ELSE FALSE END AS InGuideSetTrainingRange
FROM SampleFile sf
    -- Use -1 to signify that an exclusion is for the whole sample (and therefore applies to all metrics)
    -- See code in SampleFileQCMetadata.java
LEFT OUTER JOIN (SELECT GROUP_CONCAT(COALESCE(MetricId, -1)) AS ExcludedMetricIds, ReplicateId FROM QCMetricExclusion GROUP BY ReplicateId) e
ON sf.ReplicateId = e.ReplicateId
LEFT JOIN GuideSetForOutliers gs
ON ((sf.AcquiredTime >= gs.TrainingStart AND sf.AcquiredTime < gs.ReferenceEnd) OR (sf.AcquiredTime >= gs.TrainingStart AND gs.ReferenceEnd IS NULL))
WHERE sf.AcquiredTime IS NOT NULL