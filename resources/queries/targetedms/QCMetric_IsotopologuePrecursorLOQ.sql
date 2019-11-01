SELECT
       COALESCE(precursorchrominfoid.PrecursorId.Id, precursorchrominfoid.MoleculePrecursorId.Id) AS PrecursorId,
       precursorchrominfoid AS PrecursorChromInfoId,
       precursorchrominfoid.SampleFileId AS SampleFileId,
       COALESCE(precursorchrominfoid.PrecursorId.ModifiedSequence, precursorchrominfoid.MoleculePrecursorId.CustomIonName) || (CASE WHEN COALESCE(precursorchrominfoid.PrecursorId.Charge, precursorchrominfoid.MoleculePrecursorId.Charge) > 0 THEN ' +' ELSE ' ' END) || CAST(COALESCE(precursorchrominfoid.PrecursorId.Charge, precursorchrominfoid.MoleculePrecursorId.Charge) AS VARCHAR) AS SeriesLabel,
       CASE WHEN precursorchrominfoid.PrecursorId.Id IS NOT NULL THEN 'Peptide' ELSE 'Fragment' END AS DataType,
       CAST(Value AS DOUBLE) AS MetricValue,
       COALESCE(precursorchrominfoid.PrecursorId.Mz, precursorchrominfoid.MoleculePrecursorId.Mz) AS mz
FROM PrecursorChromInfoAnnotation

-- Pull only for unmodified variant
WHERE Name='LOQ'  AND precursorchrominfoid.PrecursorId.ModifiedSequence NOT LIKE '%]%'