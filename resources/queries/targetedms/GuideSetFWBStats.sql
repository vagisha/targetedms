/*
 * Copyright (c) 2015 LabKey Corporation
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
SELECT gs.RowId AS GuideSetId,
gs.TrainingStart,
gs.TrainingEnd,
gs.ReferenceEnd,
p.Sequence,
COUNT(p.Value) AS NumRecords,
AVG(p.Value) AS Mean,
STDDEV(p.Value) AS StandardDev
FROM guideset gs
LEFT JOIN (
   SELECT PrecursorId.Id AS PrecursorId,
   PrecursorId.ModifiedSequence AS Sequence,
   PeptideChromInfoId.SampleFileId.AcquiredTime AS AcquiredTime,
   (MaxEndTime - MinStartTime) AS Value
   FROM precursorchrominfo
) as p ON p.AcquiredTime >= gs.TrainingStart AND p.AcquiredTime <= gs.TrainingEnd
GROUP BY gs.RowId,
gs.TrainingStart,
gs.TrainingEnd,
gs.ReferenceEnd,
p.Sequence