
CREATE TABLE targetedms.GuideSet
(
  RowId SERIAL NOT NULL,
  Container ENTITYID NOT NULL,
  CreatedBy USERID,
  Created TIMESTAMP,
  ModifiedBy USERID,
  Modified TIMESTAMP,
  TrainingStart TIMESTAMP NOT NULL,
  TrainingEnd TIMESTAMP NOT NULL,
  Comment TEXT,

  CONSTRAINT PK_GuideSet PRIMARY KEY (RowId)
);