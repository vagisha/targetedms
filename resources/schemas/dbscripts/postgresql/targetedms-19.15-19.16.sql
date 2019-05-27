CREATE TABLE targetedms.QCEmailNotifications
(
  userId          USERID,
  enabled         BOOLEAN,
  outliers        INT,
  samples         INT,

  Created         TIMESTAMP,
  CreatedBy       USERID,
  Modified        TIMESTAMP,
  ModifiedBy      USERID,
  Container       ENTITYID NOT NULL,

  CONSTRAINT PK_QCEmailNotifications PRIMARY KEY (userId, Container),
  CONSTRAINT FK_QCEmailNotifications FOREIGN KEY (Container) REFERENCES core.Containers(EntityId)
);

CREATE INDEX IX_targetedms_qcEmailNotifications_Container ON targetedms.QCEmailNotifications (Container);