
CREATE TABLE targetedms.GuideSet
(
  RowId INT IDENTITY(1, 1) NOT NULL,
  Container ENTITYID NOT NULL,
  CreatedBy USERID,
  Created DATETIME,
  ModifiedBy USERID,
  Modified DATETIME,
  TrainingStart DATETIME NOT NULL,
  TrainingEnd DATETIME NOT NULL,
  Comment TEXT,

  CONSTRAINT PK_GuideSet PRIMARY KEY (RowId)
);