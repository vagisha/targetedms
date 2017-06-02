SELECT
  cc.Id AS CalibrationCurve,
  hpci.Id AS HeavyPrecursorChromInfo,
  hpci.SampleFileId,
  hpre.Charge,
  lpci.Id AS LightPrecursorChromInfo,
  hpci.TotalArea / lpci.TotalArea AS Ratio

FROM
  targetedms.Peptide hpep
INNER JOIN
  targetedms.Precursor hpre
ON hpre.GeneralMoleculeId = hpep.Id AND hpre.IsotopeLabelId.name = 'heavy'
INNER JOIN
  targetedms.PrecursorChromInfo hpci
ON hpci.PrecursorId = hpre.Id
INNER JOIN
  targetedms.CalibrationCurve cc

ON cc.GeneralMoleculeId = hpep.Id

INNER JOIN
  targetedms.Peptide lpep
ON cc.GeneralMoleculeId = lpep.Id
INNER JOIN
  targetedms.Precursor lpre
ON lpre.GeneralMoleculeId = lpep.Id AND lpre.IsotopeLabelId.name = 'light'
INNER JOIN
  targetedms.PrecursorChromInfo lpci
ON lpci.PrecursorId = lpre.Id AND hpci.SampleFileId = lpci.SampleFileId AND hpre.Charge = lpre.Charge
