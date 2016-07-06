SELECT
  COALESCE(PrecursorId.Id, MoleculePrecursorId.Id) AS PrecursorId,
  Id AS PrecursorChromInfoId,
  SampleFileId AS SampleFileId,
  COALESCE(PrecursorId.ModifiedSequence, MoleculePrecursorId.CustomIonName) AS SeriesLabel,
  CASE WHEN PrecursorId.Id IS NOT NULL THEN 'Peptide' ELSE 'Fragment' END AS DataType,
  TotalArea AS MetricValue
FROM PrecursorChromInfo