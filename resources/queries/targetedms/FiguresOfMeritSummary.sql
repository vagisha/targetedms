
SELECT
  RunId,
  MoleculeId,
  AnalyteConcentration,
  SampleType,
  AVG(ReplicateConcentration) as Mean,
  STDDEV(ReplicateConcentration) as "StdDev",
  (100 * STDDEV(ReplicateConcentration)) / AVG(ReplicateConcentration) as CV,
  (100 * (AVG(ReplicateConcentration) - AnalyteConcentration)) / AnalyteConcentration as Bias
FROM FiguresOfMerit
WHERE ExcludeFromCalibration = FALSE

GROUP BY RunId, MoleculeId, SampleType, AnalyteConcentration