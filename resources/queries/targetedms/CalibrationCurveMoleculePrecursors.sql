SELECT
  cc.Id AS CalibrationCurve,
  hpci.Id AS HeavyPrecursorChromInfo,
  hpci.SampleFileId,
  hpre.Charge,
  lpci.Id AS LightPrecursorChromInfo,
  lpci.TotalArea / hpci.TotalArea AS Ratio
FROM
  targetedms.Molecule hmol
INNER JOIN
  targetedms.MoleculePrecursor hpre
ON hpre.GeneralMoleculeId = hmol.Id AND hpre.IsotopeLabelId.name = 'heavy'
INNER JOIN
  targetedms.PrecursorChromInfo hpci
ON hpci.MoleculePrecursorId = hpre.Id
INNER JOIN
  targetedms.CalibrationCurve cc
ON cc.GeneralMoleculeId = hmol.Id

INNER JOIN
  targetedms.Molecule lmol
ON cc.GeneralMoleculeId = lmol.Id
INNER JOIN
  targetedms.MoleculePrecursor lpre
ON lpre.GeneralMoleculeId = lmol.Id AND lpre.IsotopeLabelId.name = 'light'
INNER JOIN
  targetedms.PrecursorChromInfo lpci
ON lpci.MoleculePrecursorId = lpre.Id AND hpci.SampleFileId = lpci.SampleFileId AND hpre.Charge = lpre.Charge