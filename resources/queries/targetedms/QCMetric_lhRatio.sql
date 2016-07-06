SELECT
  COALESCE(PrecursorChromInfoId.PrecursorId.Id, PrecursorChromInfoId.MoleculePrecursorId.Id) AS PrecursorId,
  PrecursorChromInfoId.Id AS PrecursorChromInfoId,
  PrecursorChromInfoId.SampleFileId AS SampleFileId,
  COALESCE(PrecursorChromInfoId.PrecursorId.ModifiedSequence, PrecursorChromInfoId.MoleculePrecursorId.CustomIonName) AS SeriesLabel,
  CASE WHEN PrecursorChromInfoId.PrecursorId.Id IS NOT NULL THEN 'Peptide' ELSE 'Fragment' END AS DataType,
  AreaRatio AS MetricValue
FROM PrecursorAreaRatio