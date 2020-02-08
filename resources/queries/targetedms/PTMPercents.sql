
SELECT
  StructuralModId AS Modification,
  SUM(ModifiedAreaProportion) AS PercentModified,
  MIN(Id) AS Id @hidden,
  Sequence,
  PreviousAA @hidden,
  NextAA @hidden,
  PeptideModifiedSequence @hidden,
  SampleName,
  SUBSTRING(Sequence, IndexAA + 1, 1) || (StartIndex + IndexAA + 1) AS SiteLocation,
  PeptideGroupId
FROM PTMPercentsPrepivot
GROUP BY
  SampleName,
  Sequence,
  PreviousAA,
  NextAA,
  PeptideModifiedSequence,
  StartIndex,
  PeptideGroupId,
  IndexAA,
  StructuralModId
PIVOT PercentModified BY SampleName