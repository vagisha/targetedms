
-- iRTScale table to store iRT scale information.
CREATE TABLE targetedms.iRTScale
(
    Id INT IDENTITY(1, 1) NOT NULL,
    Container ENTITYID NOT NULL,
    Created DATETIME,
    CreatedBy INT,

    CONSTRAINT PK_iRTScale PRIMARY KEY (Id),
    CONSTRAINT FK_iRTScale_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId)
);
CREATE INDEX IX_iRTScale_Container ON targetedms.iRTScale (Container);

-- iRTPeptide table to store iRT peptide information.
-- ModifiedSequence: the optionally chemically modified peptide sequence
CREATE TABLE targetedms.iRTPeptide
(
    Id INT IDENTITY(1, 1) NOT NULL,
    ModifiedSequence NVARCHAR(100) NOT NULL,
    iRTStandard BIT NOT NULL,
    iRTValue FLOAT NOT NULL,
    iRTScaleId INT NOT NULL,
    Created DATETIME,
    CreatedBy INT,

    CONSTRAINT PK_iRTPeptide PRIMARY KEY (Id),
    CONSTRAINT FK_iRTPeptide_iRTScaleId FOREIGN KEY (iRTScaleId) REFERENCES targetedms.iRTScale(Id)
);
CREATE INDEX IX_iRTPeptide_iRTScaleId ON targetedms.iRTPeptide (iRTScaleId);

ALTER TABLE targetedms.Runs ADD iRTScaleId INT;
ALTER TABLE targetedms.Runs ADD CONSTRAINT FK_Runs_iRTScaleId FOREIGN KEY (iRTScaleId) REFERENCES targetedms.iRTScale(Id);
CREATE INDEX IX_Runs_iRTScaleId ON targetedms.Runs (iRTScaleId);
