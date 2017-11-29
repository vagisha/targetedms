
SELECT
  MoleculeId,
  MoleculeName,
  PeptideName,
  RunId,
  SampleType,
  Bias,
  MAX(ReplicateConcentration) as ReplicateConcentration,
  AnalyteConcentration,
  Units
FROM FiguresOfMerit

GROUP BY MoleculeId, MoleculeName, PeptideName, RunId, SampleType, Bias, AnalyteConcentration, Units

PIVOT ReplicateConcentration BY AnalyteConcentration
