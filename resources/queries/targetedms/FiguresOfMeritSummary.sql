
SELECT
  RunId,
  MoleculeId,
  AnalyteConcentration,
  SampleType,
  SampleName,
  AVG(ReplicateConcentration) as Mean,
  STDDEV(ReplicateConcentration) as "StdDev",
  (100 * STDDEV(ReplicateConcentration)) / AVG(ReplicateConcentration) as CV,
  (100 * (AVG(ReplicateConcentration) - AnalyteConcentration)) / AnalyteConcentration as Bias
FROM FiguresOfMerit

GROUP BY RunId, MoleculeId, SampleType, SampleName, AnalyteConcentration

PIVOT Mean, "StdDev", CV, Bias BY AnalyteConcentration