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
  gm.ExcludeFromCalibration,
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
  gm.ExcludeFromCalibration,
  gm.SampleFileId.ReplicateId,
  gm.SampleFileId.ReplicateId.AnalyteConcentration,
  gm.SampleFileId.ReplicateId.SampleType,
  gm.SampleFileId.ReplicateId.RunId.Id,
  gm.SampleFileId.ReplicateId.RunId.FileName,
  gm.SampleFileId.SampleName,
  CAST(qs.Units AS VARCHAR)

