/*
 * Copyright (c) 2019 LabKey Corporation
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
       qmc.id,
       qmc.name,
       qmc.Series1Label,
       qmc.Series1SchemaName,
       qmc.Series1QueryName,
       qmc.Series2Label,
       qmc.Series2SchemaName,
       qmc.Series2QueryName,
       qmc.PrecursorScoped,
       qmc.Container, -- including to lock out editing pre-configured qc metrics,
       qmc.EnabledQueryName,
       qmc.EnabledSchemaName,
       qem.Enabled,
       CASE WHEN qem.metric IS NULL THEN FALSE
            ELSE TRUE END AS Inserted,
       qmc.TraceValue,
       qmc.TimeValue,
       qmc.TraceName,
       qmc.YAxisLabel1,
       qmc.YAxisLabel2
FROM
      qcmetricconfiguration qmc
FULL JOIN   qcenabledmetrics qem
       ON   qem.metric=qmc.id