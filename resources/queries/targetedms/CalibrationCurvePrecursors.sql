/*
 * Copyright (c) 2017-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
SELECT
  cc.Id AS CalibrationCurve,
  hpci.Id AS HeavyPrecursorChromInfo,
  hpci.SampleFileId,
  hpre.Charge,
  lpci.Id AS LightPrecursorChromInfo,
  case WHEN hpci.TotalArea <>0 THEN
  (lpci.TotalArea / hpci.TotalArea) else null end AS Ratio

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
