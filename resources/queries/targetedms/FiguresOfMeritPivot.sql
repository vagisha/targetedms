
SELECT
  MoleculeId,
  MoleculeName,
  PeptideName,
  RunId,
  FileName,
  SampleType,
  Bias,
  MAX(ReplicateConcentration) as ReplicateConcentration,
  AnalyteConcentration,
  Units,
  SampleName
FROM FiguresOfMerit

GROUP BY MoleculeId, MoleculeName, PeptideName, RunId, SampleType, Bias, AnalyteConcentration, Units, SampleName, FileName

PIVOT ReplicateConcentration BY AnalyteConcentration
