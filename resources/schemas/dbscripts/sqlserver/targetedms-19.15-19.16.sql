CREATE TABLE targetedms.QCEmailNotifications
(
  userId          USERID,
  enabled         BIT,
  outliers        INTEGER,
  samples         INTEGER,

  Created         DATETIME,
  CreatedBy       USERID,
  Modified        DATETIME,
  ModifiedBy      USERID,
  Container       ENTITYID NOT NULL,

  CONSTRAINT PK_QCEmailNotifications PRIMARY KEY (userId, Container),
  CONSTRAINT FK_QCEmailNotifications FOREIGN KEY (Container) REFERENCES core.Containers(EntityId)
);
GO

CREATE INDEX IX_targetedms_qcEmailNotifications_Container ON targetedms.QCEmailNotifications (Container);
GO