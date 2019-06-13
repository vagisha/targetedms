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
UPDATE targetedms.precursorchrominfo SET container = r.container
FROM targetedms.samplefile sf
   INNER JOIN targetedms.replicate rep ON (rep.id = sf.replicateId)
   INNER JOIN targetedms.runs r ON (r.Id = rep.runId)
WHERE sampleFileId = sf.id
      AND targetedms.precursorchrominfo.container != r.container;
