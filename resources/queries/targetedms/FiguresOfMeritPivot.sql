
SELECT
  MoleculeId,
  MoleculeName,
  PeptideName,
  RunId,
  SampleType,
  Bias,
  MAX(ReplicateConcentration) as ReplicateConcentration,
  AnalyteConcentration,
  Units,
  SampleName
FROM FiguresOfMerit

GROUP BY MoleculeId, MoleculeName, PeptideName, RunId, SampleType, Bias, AnalyteConcentration, Units, SampleName

PIVOT ReplicateConcentration BY AnalyteConcentration
