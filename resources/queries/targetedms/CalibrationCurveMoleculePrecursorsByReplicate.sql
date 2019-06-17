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
ccp.HeavyPrecursorChromInfo.MoleculePrecursorId.GeneralMoleculeId.CustomIonName,
ccp.charge,
avg(ccp.ratio) as ratioMean,
avg(ccp.HeavyPrecursorChromInfo.BestRetentionTime) bestRtMean,
avg(ccp.LightPrecursorChromInfo.TotalArea) lightTotalAreaMean,
avg(ccp.HeavyPrecursorChromInfo.TotalArea) heavyTotalAreaMean,
avg(ccp.SampleFileId.ReplicateId.AnalyteConcentration) analyteConcentrationMean,
avg(ccp.HeavyPrecursorChromInfo.PeptideChromInfoId.CalculatedConcentration) calculatedConcentrationMean,
rep.id replicateId
FROM CalibrationCurveMoleculePrecursors ccp
JOIN replicate rep ON rep.id = ccp.SampleFileId.ReplicateId
GROUP BY rep.id, ccp.charge, ccp.HeavyPrecursorChromInfo.MoleculePrecursorId.GeneralMoleculeId.CustomIonName