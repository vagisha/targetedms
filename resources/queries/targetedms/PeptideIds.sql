SELECT
   PrecursorId.PeptideId.PeptideGroupId @hidden,
   ROUND(AVG(BestRetentionTime), 1) AS RetentionTime,
   MIN((PrecursorId.mz - 1.00727647) * PrecursorId.Charge * (1 + (pci.AverageMassErrorPPM / 1000000 ))) AS MinObservedPeptideMass,
   MAX((PrecursorId.mz - 1.00727647) * PrecursorId.Charge * (1 + (pci.AverageMassErrorPPM / 1000000 ))) AS MaxObservedPeptideMass,
   PrecursorId.NeutralMass AS ExpectedPeptideMass,
   SUBSTRING(PrecursorId.PeptideId.PeptideGroupId.Label, 6, 1) || (PrecursorId.PeptideId.StartIndex + 1) || '-' || (PrecursorId.PeptideId.EndIndex) AS PeptideIdentity,
   PrecursorId.PeptideId.Sequence,
   PrecursorId.ModifiedSequence AS PeptideModifiedSequence @hidden,
   PrecursorId.PeptideId AS Id @hidden,
   (SELECT GROUP_CONCAT(StructuralModId.Name) FROM targetedms.PeptideStructuralModification psm WHERE psm.PeptideId = pci.PrecursorId.PeptideId) AS Modification
FROM
     targetedms.precursorchrominfo pci
GROUP BY
   PrecursorId.ModifiedSequence,
   PrecursorId.NeutralMass,
   PrecursorId.PeptideId,
   PrecursorId.PeptideId.PeptideGroupId,
   PrecursorId.PeptideId.PeptideGroupId.Label,
   PrecursorId.PeptideId.Sequence,
   PrecursorId.PeptideId.StartIndex,
   PrecursorId.PeptideId.EndIndex