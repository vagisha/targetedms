SELECT
  PeptideId,
  PeptideId.PeptideModifiedSequence as PeptideName,
  MoleculeId AS GeneralMoleculeId,
  MoleculeId.CustomIonName as MoleculeName,
  GROUP_CONCAT(DISTINCT SampleFileId.SampleName, ', ') as SampleFiles,
	SampleFileId.ReplicateId.RunId.Id as RunId,
  SampleFileId.ReplicateId.RunId.FileName as FileName
FROM generalmoleculechrominfo
GROUP BY
  PeptideId,
  PeptideId.PeptideModifiedSequence,
  MoleculeId,
  MoleculeId.CustomIonName,
	SampleFileId.ReplicateId.RunId.Id,
  SampleFileId.ReplicateId.RunId.FileName