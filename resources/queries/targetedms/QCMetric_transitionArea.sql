/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
  COALESCE(PrecursorId.Id, MoleculePrecursorId.Id) AS PrecursorId,
  Id AS PrecursorChromInfoId,
  SampleFileId AS SampleFileId,
  COALESCE(PrecursorId.ModifiedSequence, MoleculePrecursorId.CustomIonName) AS SeriesLabel,
  CASE WHEN PrecursorId.Id IS NOT NULL THEN 'Peptide' ELSE 'Fragment' END AS DataType,
  TotalNonPrecursorArea AS MetricValue,
  COALESCE(PrecursorId.Mz, MoleculePrecursorId.Mz) AS mz
FROM PrecursorChromInfo