
SELECT
  RunId,
  MoleculeId,
  AnalyteConcentration,
  SampleType,
  AVG(ReplicateConcentration) as Mean,
  COALESCE (STDDEV(ReplicateConcentration), 0) as "StdDev",
  COALESCE ((100 * STDDEV(ReplicateConcentration)) / AVG(ReplicateConcentration), 0) as CV,
  (100 * (AVG(ReplicateConcentration) - AnalyteConcentration)) / AnalyteConcentration as Bias
FROM FiguresOfMerit

GROUP BY RunId, MoleculeId, SampleType, AnalyteConcentration

PIVOT Mean, "StdDev", CV, Bias BY AnalyteConcentration