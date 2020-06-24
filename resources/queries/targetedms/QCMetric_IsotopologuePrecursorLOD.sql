SELECT
       precursorchrominfoid AS PrecursorChromInfoId,
       precursorchrominfoid.SampleFileId AS SampleFileId,
       CAST(Value AS DOUBLE) AS MetricValue
FROM PrecursorChromInfoAnnotation

-- Pull only for unmodified variant
WHERE Name='LOD' AND precursorchrominfoid.PrecursorId.ModifiedSequence NOT LIKE '%]%'