SELECT
       PrecursorChromInfoId,
       x.SampleFileId AS SampleFileId,
       MetricValue
FROM
     (
     SELECT
            AVG(CAST(p.Value AS DOUBLE)) AS MetricValue,
            MIN(p.precursorchrominfoid) AS precursorchrominfoid,
            p.precursorchrominfoid.SampleFileId AS SampleFileId
     FROM PrecursorChromInfoAnnotation p
     WHERE Name = 'PrecursorAccuracy'
     GROUP BY p.precursorchrominfoid.PrecursorId.PeptideId.Sequence, p.precursorchrominfoid.SampleFileId
     ) x
INNER JOIN PrecursorChromInfo pci ON x.precursorchrominfoid = pci.Id
