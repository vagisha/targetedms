/*
 * Copyright (c) 2018-2019 LabKey Corporation
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
  sub.PeptideId,
  sub.MoleculeId,
  sub.Time,
  sub.SubGroup,
  (CASE WHEN MAX(sub.Dose)= MIN(sub.Dose) THEN MAX(sub.Dose) ELSE NULL END) AS Dose,
  (CASE WHEN MAX(sub.DoseUnits)= MIN(sub.DoseUnits) THEN MAX(sub.DoseUnits) ELSE NULL END) AS DoseUnits,
  (CASE WHEN MAX(sub.ROA)= MIN(sub.ROA) THEN MAX(sub.ROA) ELSE NULL END) AS ROA,
  AVG(sub.calculatedConcentration)          AS Concentration,
  group_concat(sub.calculatedConcentration) AS Concentrations,
  MAX(sub.Sequence)                         AS Peptide,
  MAX(sub.Filename)                         AS FileName,
  MAX(sub.ionName)                          AS ionName,
  STDDEV(sub.calculatedConcentration)       AS StandardDeviation,
  COUNT(sub.calculatedConcentration)        AS ConcentrationCount
FROM
  (
    SELECT
      CAST(ci.PeptideId AS VARCHAR(250))            AS PeptideId,
      CAST(ci.MoleculeId AS VARCHAR(250))           AS MoleculeId,
      CAST(ifdefined(rep.Time) AS FLOAT)            AS Time,
      CAST(ifdefined(rep.Dose) AS FLOAT)            AS Dose,
      CAST(ifdefined(rep.DoseUnits) AS VARCHAR(250))AS DoseUnits,
      CAST(ifdefined(rep.SubGroup) AS VARCHAR(250)) AS SubGroup,
      CAST(ifdefined(rep.ROA) AS VARCHAR(250))      AS ROA,
      CAST(ci.calculatedConcentration AS FLOAT)     AS calculatedConcentration,
      CAST(pep.sequence AS VARCHAR(250))            AS sequence,
      CAST(rep.runid.filename AS VARCHAR(250))      AS FileName,
      CAST(ci.MoleculeId.Molecule AS VARCHAR(250))  AS IonName
    FROM

      generalmoleculechrominfo ci
      JOIN samplefile sf ON sf.id = ci.samplefileid
      JOIN replicate rep ON rep.id = sf.replicateid
      LEFT JOIN peptide pep ON pep.id = ci.peptideid
    WHERE (ci.SampleFileId.ReplicateId.SampleType IS NULL OR lower(ci.SampleFileId.ReplicateId.SampleType) = 'unknown')
  ) sub
GROUP BY sub.PeptideId, sub.MoleculeId, sub.SubGroup, sub.Time