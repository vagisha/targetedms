
SELECT
  gm.PeptideId,
  gm.PeptideId.PeptideModifiedSequence as PeptideName,
  gm.MoleculeId,
  gm.MoleculeId.CustomIonName as MoleculeName,
  gm.SampleFileId,
  gm.PeakCountRatio,
  gm.RetentionTime,
  gm.SampleFileId.ReplicateId,
  AVG(gm.CalculatedConcentration) as ReplicateConcentration,
  gm.SampleFileId.ReplicateId.AnalyteConcentration,
  CASE WHEN (gm.SampleFileId.ReplicateId.AnalyteConcentration IS NOT NULL AND gm.SampleFileId.ReplicateId.AnalyteConcentration != 0) THEN
      (100 * (AVG(gm.CalculatedConcentration) - gm.SampleFileId.ReplicateId.AnalyteConcentration) / gm.SampleFileId.ReplicateId.AnalyteConcentration)
  ELSE NULL END as Bias,
  gm.SampleFileId.ReplicateId.SampleType,
  gm.SampleFileId.ReplicateId.RunId.Id as RunId,
  gm.SampleFileId.ReplicateId.RunId.FileName as FileName,
  gm.SampleFileId.SampleName,
  CAST(qs.Units AS VARCHAR) as Units
FROM (SELECT * FROM generalmoleculechrominfo WHERE CalculatedConcentration IS NOT NULL AND abs(CalculatedConcentration) < 1E20) as gm
JOIN QuantificationSettings qs ON gm.SampleFileId.ReplicateId.RunId.Id = qs.RunId.Id

GROUP BY
  gm.PeptideId,
  gm.PeptideId.PeptideModifiedSequence,
  gm.MoleculeId,
  gm.MoleculeId.CustomIonName,
  gm.SampleFileId,
  gm.PeakCountRatio,
  gm.RetentionTime,
  gm.SampleFileId.ReplicateId,
  gm.SampleFileId.ReplicateId.AnalyteConcentration,
  gm.SampleFileId.ReplicateId.SampleType,
  gm.SampleFileId.ReplicateId.RunId.Id,
  gm.SampleFileId.ReplicateId.RunId.FileName,
  gm.SampleFileId.SampleName,
  CAST(qs.Units AS VARCHAR)

