------------------ GeneralPrecursorID ---------------
-----------------------------------------------------

-- Drop Indexes
DROP INDEX targetedms.GeneralTransition.IX_Transition_PrecursorId;
DROP INDEX targetedms.PrecursorAnnotation.IX_PrecursorAnnotation_PrecursorId;
DROP INDEX targetedms.PrecursorChromInfo.IX_PrecursorChromInfo_PrecursorId;
GO

-- Drop Constraints
ALTER TABLE targetedms.GeneralTransition DROP CONSTRAINT FK_GeneralTransition_GPId;
ALTER TABLE targetedms.Precursor DROP CONSTRAINT FK_Precursor_Id;
ALTER TABLE targetedms.PrecursorAnnotation DROP CONSTRAINT FK_PrecursorAnnotation_PrecursorId;
ALTER TABLE targetedms.PrecursorAnnotation DROP CONSTRAINT UQ_PrecursorAnnotation_Name_Precursor;
ALTER TABLE targetedms.PrecursorChromInfo DROP CONSTRAINT FK_PrecursorChromInfo_PrecursorId;
ALTER TABLE targetedms.MoleculePrecursor DROP CONSTRAINT FK_Id;
ALTER TABLE targetedms.MoleculePrecursor DROP CONSTRAINT PK_MoleculePrecursorId;
GO

-- GeneralPrecursor -- change Id
ALTER TABLE targetedms.GeneralPrecursor DROP CONSTRAINT PK_Precursor;
GO
ALTER TABLE targetedms.GeneralPrecursor ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.GeneralPrecursor ADD CONSTRAINT PK_Precursor PRIMARY KEY (Id);
GO

-- change Columns
ALTER TABLE targetedms.GeneralTransition ALTER COLUMN GeneralPrecursorId bigint NOT NULL;
ALTER TABLE targetedms.PrecursorAnnotation ALTER COLUMN PrecursorId bigint NOT NULL;
ALTER TABLE targetedms.PrecursorChromInfo ALTER COLUMN PrecursorId bigint NOT NULL;
ALTER TABLE targetedms.MoleculePrecursor ALTER COLUMN Id bigint NOT NULL;
GO

-- Add back constraints
ALTER TABLE targetedms.GeneralTransition ADD CONSTRAINT FK_GeneralTransition_GPId FOREIGN KEY (GeneralPrecursorId) REFERENCES targetedms.GeneralPrecursor(Id);
ALTER TABLE targetedms.PrecursorAnnotation  ADD CONSTRAINT FK_PrecursorAnnotation_PrecursorId FOREIGN KEY (PrecursorId) REFERENCES targetedms.GeneralPrecursor(Id)
ALTER TABLE targetedms.PrecursorAnnotation  ADD CONSTRAINT UQ_PrecursorAnnotation_Name_Precursor UNIQUE (Name, PrecursorId);
ALTER TABLE targetedms.PrecursorChromInfo ADD CONSTRAINT FK_PrecursorChromInfo_PrecursorId FOREIGN KEY (PrecursorId) REFERENCES targetedms.GeneralPrecursor(Id);
ALTER TABLE targetedms.MoleculePrecursor ADD CONSTRAINT FK_Id FOREIGN KEY (Id) REFERENCES targetedms.GeneralPrecursor (Id);
ALTER TABLE targetedms.MoleculePrecursor ADD CONSTRAINT PK_MoleculePrecursorId PRIMARY KEY (Id);
GO

-- Add back Indexes
CREATE INDEX IX_Transition_PrecursorId ON targetedms.GeneralTransition(GeneralPrecursorId);
CREATE INDEX IX_PrecursorAnnotation_PrecursorId ON targetedms.PrecursorAnnotation(PrecursorId);
CREATE INDEX IX_PrecursorChromInfo_PrecursorId ON targetedms.PrecursorChromInfo(PrecursorId);
GO


------------------ PrecursorID ------------------
-------------------------------------------------

-- Drop Indexes
DROP INDEX targetedms.BibliospecLibInfo.IX_BibliospecLibInfo_PrecursorId;
DROP INDEX targetedms.ChromatogramLibInfo.IX_ChromatogramLibInfo_PrecursorId;
DROP INDEX targetedms.HunterLibInfo.IX_HunterLibInfo_PrecursorId;
DROP INDEX targetedms.NistLibInfo.IX_NistLibInfo_PrecursorId;
DROP INDEX targetedms.SpectrastLibInfo.IX_SpectrastLibInfo_PrecursorId;
DROP INDEX targetedms.Precursor.IX_Precursor_Id;
GO

-- Drop Constraints
ALTER TABLE targetedms.BibliospecLibInfo DROP CONSTRAINT FK_BibliospecLibInfo_Precursor;
ALTER TABLE targetedms.ChromatogramLibInfo DROP CONSTRAINT FK_ChromatogramLibInfo_Precursor;
ALTER TABLE targetedms.HunterLibInfo DROP CONSTRAINT FK_HunterLibInfo_Precursor;
ALTER TABLE targetedms.NistLibInfo DROP CONSTRAINT FK_NistLibInfo_Precursor;
ALTER TABLE targetedms.SpectrastLibInfo DROP CONSTRAINT FK_SpectrastLibInfo_Precursor;
GO

-- Change Precursor
ALTER TABLE targetedms.Precursor DROP CONSTRAINT PK_Precursor_Id;
GO
ALTER TABLE targetedms.Precursor ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.Precursor ADD CONSTRAINT PK_Precursor_Id PRIMARY KEY (Id);
GO

-- Change columns
ALTER TABLE targetedms.BibliospecLibInfo ALTER COLUMN PrecursorId bigint NOT NULL;
ALTER TABLE targetedms.ChromatogramLibInfo ALTER COLUMN PrecursorId bigint NOT NULL;
ALTER TABLE targetedms.HunterLibInfo ALTER COLUMN PrecursorId bigint NOT NULL;
ALTER TABLE targetedms.NistLibInfo ALTER COLUMN PrecursorId bigint NOT NULL;
ALTER TABLE targetedms.SpectrastLibInfo ALTER COLUMN PrecursorId bigint NOT NULL;
GO

-- Add back Constraints
ALTER TABLE targetedms.BibliospecLibInfo ADD CONSTRAINT FK_BibliospecLibInfo_Precursor FOREIGN KEY (PrecursorId) REFERENCES targetedms.Precursor(Id);
ALTER TABLE targetedms.ChromatogramLibInfo ADD CONSTRAINT FK_ChromatogramLibInfo_Precursor FOREIGN KEY (PrecursorId) REFERENCES targetedms.Precursor(Id);
ALTER TABLE targetedms.HunterLibInfo ADD CONSTRAINT FK_HunterLibInfo_Precursor FOREIGN KEY (PrecursorId) REFERENCES targetedms.Precursor(Id);
ALTER TABLE targetedms.NistLibInfo ADD CONSTRAINT FK_NistLibInfo_Precursor FOREIGN KEY (PrecursorId) REFERENCES targetedms.Precursor(Id);
ALTER TABLE targetedms.SpectrastLibInfo ADD CONSTRAINT FK_SpectrastLibInfo_Precursor FOREIGN KEY (PrecursorId) REFERENCES targetedms.Precursor(Id);
ALTER TABLE targetedms.Precursor ADD CONSTRAINT FK_Precursor_Id FOREIGN KEY (Id) REFERENCES targetedms.GeneralPrecursor(Id);
GO

-- Add back Indexes
CREATE INDEX IX_BibliospecLibInfo_PrecursorId ON targetedms.BibliospecLibInfo(PrecursorId);
CREATE INDEX IX_ChromatogramLibInfo_PrecursorId ON targetedms.ChromatogramLibInfo(PrecursorId);
CREATE INDEX IX_HunterLibInfo_PrecursorId ON targetedms.HunterLibInfo(PrecursorId);
CREATE INDEX IX_NistLibInfo_PrecursorId ON targetedms.NistLibInfo(PrecursorId);
CREATE INDEX IX_SpectrastLibInfo_PrecursorId ON targetedms.SpectrastLibInfo(PrecursorId);
CREATE INDEX IX_Precursor_Id ON targetedms.Precursor(Id);
GO


------------------ GeneralTransitionID ---------------
-----------------------------------------------------

-- Drop Indexes
DROP INDEX targetedms.TransitionAnnotation.IX_TransitionAnnotation_TransitionId;
DROP INDEX targetedms.TransitionChromInfo.IX_TransitionChromInfo_TransitionId;
DROP INDEX targetedms.TransitionChromInfoAnnotation.IX_TransitionChromInfoAnnotation_TransitionChromInfoId;
DROP INDEX targetedms.TransitionLoss.IX_TransitionLoss_TransitionId;
DROP INDEX targetedms.TransitionOptimization.IX_TransitionOptimization_TransitionId;
GO

-- Drop Constraints
ALTER TABLE targetedms.MoleculeTransition DROP CONSTRAINT PK_MoleculeTransition;
ALTER TABLE targetedms.MoleculeTransition DROP CONSTRAINT FK_MoleculeTransition_GTId;
ALTER TABLE targetedms.TransitionOptimization DROP CONSTRAINT FK_TransitionOptimization_TransitionId;
ALTER TABLE targetedms.Transition DROP CONSTRAINT FK_Transition_Id;
ALTER TABLE targetedms.TransitionAnnotation DROP CONSTRAINT FK_TransitionAnnotation_GTId;
ALTER TABLE targetedms.TransitionAnnotation DROP CONSTRAINT UQ_TransitionAnnotation_Name_Transition;
ALTER TABLE targetedms.TransitionChromInfo DROP CONSTRAINT FK_TransitionChromInfo_GTId;
ALTER TABLE targetedms.TransitionLoss DROP CONSTRAINT FK_TransitionLoss_TransitionId;
ALTER TABLE targetedms.Transition DROP CONSTRAINT PK_Transition_Id;
GO

-- Change GTId
ALTER TABLE targetedms.GeneralTransition DROP CONSTRAINT PK_Transition;
GO
ALTER TABLE targetedms.GeneralTransition ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.GeneralTransition ADD CONSTRAINT PK_Transition PRIMARY KEY (Id);
GO

-- Change Columns
ALTER TABLE targetedms.MoleculeTransition ALTER COLUMN TransitionId bigint NOT NULL;
ALTER TABLE targetedms.Transition ALTER COLUMN Id bigint NOT NULL;
ALTER TABLE targetedms.TransitionAnnotation ALTER COLUMN TransitionId bigint NOT NULL;
ALTER TABLE targetedms.TransitionChromInfo ALTER COLUMN TransitionId bigint NOT NULL;
ALTER TABLE targetedms.TransitionLoss ALTER COLUMN TransitionId bigint NOT NULL;
ALTER TABLE targetedms.TransitionOptimization ALTER COLUMN TransitionId bigint NOT NULL;
GO

-- Add back Constraints
ALTER TABLE targetedms.MoleculeTransition ADD CONSTRAINT PK_MoleculeTransition PRIMARY KEY (TransitionId);
ALTER TABLE targetedms.MoleculeTransition ADD CONSTRAINT FK_MoleculeTransition_GTId FOREIGN KEY (TransitionId) REFERENCES targetedms.GeneralTransition(Id);
ALTER TABLE targetedms.Transition ADD CONSTRAINT PK_Transition_Id PRIMARY KEY (Id);
ALTER TABLE targetedms.Transition ADD CONSTRAINT FK_Transition_Id FOREIGN KEY (Id) REFERENCES targetedms.GeneralTransition(Id);
ALTER TABLE targetedms.TransitionAnnotation ADD CONSTRAINT FK_TransitionAnnotation_GTId FOREIGN KEY (TransitionId) REFERENCES targetedms.GeneralTransition(Id);
ALTER TABLE targetedms.TransitionAnnotation ADD CONSTRAINT UQ_TransitionAnnotation_Name_Transition UNIQUE (Name, TransitionId);
ALTER TABLE targetedms.TransitionChromInfo ADD CONSTRAINT FK_TransitionChromInfo_GTId FOREIGN KEY (TransitionId) REFERENCES targetedms.GeneralTransition(Id);
ALTER TABLE targetedms.TransitionLoss ADD CONSTRAINT FK_TransitionLoss_TransitionId FOREIGN KEY (TransitionId) REFERENCES targetedms.Transition(Id);
ALTER TABLE targetedms.TransitionOptimization ADD CONSTRAINT FK_TransitionOptimization_TransitionId FOREIGN KEY (TransitionId) REFERENCES targetedms.Transition(Id);
GO

-- Add back Indexes
CREATE INDEX IX_TransitionAnnotation_TransitionId ON targetedms.TransitionAnnotation(TransitionId);
CREATE INDEX IX_TransitionChromInfoAnnotation_TransitionChromInfoId ON targetedms.TransitionChromInfoAnnotation(TransitionChromInfoId);
CREATE INDEX IX_TransitionLoss_TransitionId ON targetedms.TransitionLoss (TransitionId);
CREATE INDEX IX_TransitionOptimization_TransitionId ON targetedms.TransitionOptimization (TransitionId);
CREATE INDEX IX_TransitionChromInfo_TransitionId ON targetedms.TransitionChromInfo(TransitionId);
GO
