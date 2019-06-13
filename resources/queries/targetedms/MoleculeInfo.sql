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