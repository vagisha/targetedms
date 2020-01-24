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

INSERT INTO targetedms.IsotopeLabel (RunId, Name, Standard)
  SELECT DISTINCT pg.RunId AS RunId, 'Unknown' AS Name, 1 AS Standard
    FROM targetedms.GeneralPrecursor gp
    INNER JOIN targetedms.GeneralMolecule gm ON gp.GeneralMoleculeId = gm.Id
    INNER JOIN targetedms.PeptideGroup pg ON gm.PeptideGroupId = pg.Id

    WHERE gp.IsotopeLabelId IS NULL;

UPDATE targetedms.GeneralPrecursor SET IsotopeLabelId = (SELECT il.Id FROM
  targetedms.IsotopeLabel il
    INNER JOIN targetedms.PeptideGroup pg ON pg.RunId = il.RunId AND il.Name = 'Unknown' AND il.Standard = 1
    INNER JOIN targetedms.GeneralMolecule gm ON gm.PeptideGroupId = pg.Id AND gm.Id = GeneralPrecursor.GeneralMoleculeId
)
WHERE IsotopeLabelId IS NULL;

DROP INDEX targetedms.generalprecursor.IX_GeneralPrecursor_IsotopeLabelId;
GO

ALTER TABLE targetedms.GeneralPrecursor ALTER COLUMN IsotopeLabelId INT NOT NULL;

GO

CREATE INDEX IX_GeneralPrecursor_IsotopeLabelId ON targetedms.GeneralPrecursor(IsotopeLabelId);

GO

ALTER TABLE targetedms.AuditLogMessage ALTER COLUMN messageType NVARCHAR(100);
