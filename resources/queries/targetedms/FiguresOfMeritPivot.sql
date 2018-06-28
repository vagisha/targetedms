
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
  SampleName,
  ExcludeFromCalibration
FROM FiguresOfMerit

GROUP BY MoleculeId, MoleculeName, PeptideName, RunId, SampleType, Bias, AnalyteConcentration, Units, SampleName, FileName, ExcludeFromCalibration

PIVOT ReplicateConcentration BY AnalyteConcentration
