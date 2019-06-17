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

/* targetedms-16.30-16.31.sql */

-- Remove all replicates not associated with a sample file.
DELETE FROM targetedms.replicateAnnotation WHERE replicateId IN (SELECT r.id FROM targetedms.replicate r LEFT OUTER JOIN targetedms.sampleFile sf ON(r.Id = sf.ReplicateId) WHERE sf.ReplicateId IS NULL);
DELETE FROM targetedms.replicate WHERE id IN (SELECT r.id FROM targetedms.replicate r LEFT OUTER JOIN targetedms.sampleFile sf ON(r.Id = sf.ReplicateId) WHERE sf.ReplicateId IS NULL);

/* targetedms-16.31-16.32.sql */

CREATE TABLE targetedms.GroupComparisonSettings
(
  Id INT IDENTITY(1,1) NOT NULL,
  RunId INT NOT NULL,
  Name NTEXT,
  NormalizationMethod NTEXT,
  ConfidenceLevel DOUBLE PRECISION,
  ControlAnnotation NTEXT,
  ControlValue NTEXT,
  CaseValue NTEXT,
  IdentityAnnotation NTEXT,
  PerProtein BIT,
  CONSTRAINT PK_GroupComparisonSettings PRIMARY KEY (Id),
  CONSTRAINT FK_GroupComparisonSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);
CREATE INDEX IX_GroupComparisonSettings_RunId ON targetedms.GroupComparisonSettings(RunId);

CREATE TABLE targetedms.FoldChange
(
  Id INT IDENTITY(1,1) NOT NULL,
  RunId INT NOT NULL,
  GroupComparisonSettingsId INT NOT NULL,
  PeptideGroupId INT,
  GeneralMoleculeId INT,
  IsotopeLabelId INT,
  MsLevel INT,
  GroupIdentifier NTEXT,
  Log2FoldChange DOUBLE PRECISION,
  AdjustedPValue DOUBLE PRECISION,
  StandardError DOUBLE PRECISION,
  DegreesOfFreedom INT,
  CONSTRAINT PK_FoldChange PRIMARY KEY(Id),
  CONSTRAINT FK_FoldChange_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id),
  CONSTRAINT FK_FoldChange_GroupComparisonSettings FOREIGN KEY (GroupComparisonSettingsId) REFERENCES targetedms.GroupComparisonSettings(Id),
  CONSTRAINT FK_FoldChange_IsotopeLabel FOREIGN KEY (IsotopeLabelId) REFERENCES targetedms.IsotopeLabel(Id),
  CONSTRAINT FK_FoldChange_PeptideGroup FOREIGN KEY (PeptideGroupId) REFERENCES targetedms.PeptideGroup(Id),
  CONSTRAINT FK_FoldChange_GeneralMolecule FOREIGN KEY (GeneralMoleculeId) REFERENCES targetedms.GeneralMolecule(Id)
);
CREATE INDEX IX_FoldChange_RunId ON targetedms.FoldChange(RunId);
CREATE INDEX IX_FoldChange_GroupComparisonSettingsId ON targetedms.FoldChange(GroupComparisonSettingsId);
CREATE INDEX IX_FoldChange_IsotopeLabelId ON targetedms.FoldChange(IsotopeLabelId);
CREATE INDEX IX_FoldChange_PeptideGroupId ON targetedms.FoldChange(PeptideGroupId);
CREATE INDEX IX_FoldChange_GeneralMoleculeId ON targetedms.FoldChange(GeneralMoleculeId);

CREATE TABLE targetedms.QuantificationSettings
(
  Id INT IDENTITY(1,1) NOT NULL,
  RunId INT NOT NULL,
  RegressionWeighting NVARCHAR(100),
  RegressionFit NVARCHAR(100),
  NormalizationMethod NTEXT,
  MsLevel INT,
  Units NTEXT,
  CONSTRAINT PK_QuantificationSettings PRIMARY KEY (Id),
  CONSTRAINT FK_QuantificationSettings_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id)
);
CREATE INDEX IX_QuantificationSettings_RunId ON targetedms.QuantificationSettings(RunId);

CREATE TABLE targetedms.CalibrationCurve
(
  Id INT IDENTITY(1,1)  NOT NULL,
  RunId INT NOT NULL,
  QuantificationSettingsId INT NOT NULL,
  GeneralMoleculeId INT,
  Slope DOUBLE PRECISION,
  Intercept DOUBLE PRECISION,
  PointCount INT,
  QuadraticCoefficient DOUBLE PRECISION,
  RSquared DOUBLE PRECISION,
  ErrorMessage NTEXT,
  CONSTRAINT PK_CalibrationCurve PRIMARY KEY(Id),
  CONSTRAINT FK_CalibrationCurve_Runs FOREIGN KEY (RunId) REFERENCES targetedms.Runs(Id),
  CONSTRAINT FK_CalibrationCurve_QuantificationSettings FOREIGN KEY (QuantificationSettingsId) REFERENCES targetedms.QuantificationSettings(Id),
  CONSTRAINT FK_CalibrationCurve_GeneralMolecule FOREIGN KEY(GeneralMoleculeId) REFERENCES targetedms.GeneralMolecule(Id)
);
CREATE INDEX IX_CalibrationCurve_RunId ON targetedms.CalibrationCurve(RunId);
CREATE INDEX IX_CalibrationCurve_QuantificationSettingsId ON targetedms.CalibrationCurve(QuantificationSettingsId);
CREATE INDEX IX_CalibrationCurve_GeneralMoleculeId ON targetedms.CalibrationCurve(GeneralMoleculeId);

ALTER TABLE targetedms.Replicate ADD SampleType NVARCHAR(100);
ALTER TABLE targetedms.Replicate ADD AnalyteConcentration DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralMoleculeChromInfo ADD CalculatedConcentration DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralPrecursor ADD IsotopeLabelId INT
GO
UPDATE gp SET IsotopeLabelId = (SELECT p.IsotopeLabelId FROM targetedms.Precursor p WHERE p.Id = gp.Id) FROM targetedms.GeneralPrecursor gp;
ALTER TABLE targetedms.GeneralPrecursor ADD CONSTRAINT FK_GeneralPrecursor_IsotopeLabel FOREIGN KEY (IsotopeLabelId) REFERENCES targetedms.IsotopeLabel(Id);
CREATE INDEX IX_GeneralPrecursor_IsotopeLabelId ON targetedms.GeneralPrecursor(IsotopeLabelId);
EXEC core.fn_dropifexists 'Precursor', 'targetedms', 'INDEX', 'IX_Precursor_IsotopeLabelId';
ALTER TABLE targetedms.Precursor DROP CONSTRAINT FK_Precursor_IsotopeLabel;
ALTER TABLE targetedms.Precursor DROP COLUMN IsotopeLabelId;
ALTER TABLE targetedms.GeneralMolecule ADD NormalizationMethod NTEXT;
ALTER TABLE targetedms.GeneralMolecule ADD InternalStandardConcentration DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralMolecule ADD ConcentrationMultiplier DOUBLE PRECISION;
ALTER TABLE targetedms.GeneralMolecule ADD StandardType NVARCHAR(100)
GO
UPDATE gm SET StandardType = (SELECT p.StandardType FROM targetedms.Peptide p WHERE p.Id = gm.Id) FROM targetedms.GeneralMolecule gm;
ALTER TABLE targetedms.Peptide DROP COLUMN StandardType;

/* targetedms-16.32-16.33.sql */

ALTER TABLE targetedms.Runs ADD PeptideGroupCount INT;
ALTER TABLE targetedms.Runs ADD PeptideCount INT;
ALTER TABLE targetedms.Runs ADD SmallMoleculeCount INT;
ALTER TABLE targetedms.Runs ADD PrecursorCount INT;
ALTER TABLE targetedms.Runs ADD TransitionCount INT;

GO

UPDATE targetedms.Runs SET PeptideGroupCount = (SELECT COUNT(pg.id) FROM targetedms.PeptideGroup pg WHERE pg.RunId = targetedms.Runs.Id);
UPDATE targetedms.Runs SET PeptideCount = (SELECT COUNT(p.id) FROM targetedms.PeptideGroup pg, targetedms.GeneralMolecule gm, targetedms.Peptide p WHERE pg.RunId = targetedms.Runs.Id AND gm.PeptideGroupId = pg.Id AND p.Id = gm.Id);
UPDATE targetedms.Runs SET SmallMoleculeCount = (SELECT COUNT(m.id) FROM targetedms.PeptideGroup pg, targetedms.GeneralMolecule gm, targetedms.molecule m WHERE pg.RunId = targetedms.Runs.Id AND gm.PeptideGroupId = pg.Id AND m.Id = gm.Id);
UPDATE targetedms.Runs SET PrecursorCount = (SELECT COUNT(gp.id) FROM targetedms.PeptideGroup pg, targetedms.GeneralMolecule gm, targetedms.GeneralPrecursor gp WHERE pg.RunId = targetedms.Runs.Id AND gm.PeptideGroupId = pg.Id AND gp.GeneralMoleculeId = gm.Id);
UPDATE targetedms.Runs SET TransitionCount = (SELECT COUNT(gt.id) FROM targetedms.PeptideGroup pg, targetedms.GeneralMolecule gm, targetedms.GeneralPrecursor gp, targetedms.GeneralTransition gt WHERE pg.RunId = targetedms.Runs.Id AND gm.PeptideGroupId = pg.Id AND gp.GeneralMoleculeId = gm.Id AND gt.GeneralPrecursorId = gp.Id);

/* targetedms-16.33-16.34.sql */

UPDATE targetedms.precursorchrominfo SET container = r.container
FROM targetedms.samplefile sf
   INNER JOIN targetedms.replicate rep ON (rep.id = sf.replicateId)
   INNER JOIN targetedms.runs r ON (r.Id = rep.runId)
WHERE sampleFileId = sf.id
      AND targetedms.precursorchrominfo.container != r.container;