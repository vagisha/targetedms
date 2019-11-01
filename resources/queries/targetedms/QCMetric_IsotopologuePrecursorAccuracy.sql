SELECT
       pci.PrecursorId.Id AS PrecursorId,
       PrecursorChromInfoId,
       x.SampleFileId AS SampleFileId,
       pci.PrecursorId.PeptideId.Sequence || (CASE WHEN pci.PrecursorId.Charge > 0 THEN ' +' ELSE ' ' END) || CAST(pci.PrecursorId.Charge AS VARCHAR) AS SeriesLabel,
       'Peptide' AS DataType,
       MetricValue,
       pci.PrecursorId.Mz AS mz

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
