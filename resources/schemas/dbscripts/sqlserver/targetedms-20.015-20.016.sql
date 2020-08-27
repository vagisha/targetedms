ALTER TABLE targetedms.AnnotationSettings DROP CONSTRAINT PK_AnnotationSettings;
GO
ALTER TABLE targetedms.AnnotationSettings ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.AnnotationSettings ADD CONSTRAINT PK_AnnotationSettings PRIMARY KEY (Id);
GO

------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.IsolationWindow DROP CONSTRAINT PK_IsolationWindow;
GO
ALTER TABLE targetedms.IsolationWindow ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.IsolationWindow ADD CONSTRAINT PK_IsolationWindow PRIMARY KEY (Id);
GO

-------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.ChromatogramLibInfo DROP CONSTRAINT PK_ChromatogramLibInfo;
GO
ALTER TABLE targetedms.ChromatogramLibInfo ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.ChromatogramLibInfo ADD CONSTRAINT PK_ChromatogramLibInfo PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.GeneralMoleculeAnnotation DROP CONSTRAINT PK_GMAnnotation;
GO
ALTER TABLE targetedms.GeneralMoleculeAnnotation ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.GeneralMoleculeAnnotation ADD CONSTRAINT PK_GMAnnotation PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------

ALTER TABLE targetedms.IsotopeEnrichment DROP CONSTRAINT PK_IsotopeEnrichment;
GO
ALTER TABLE targetedms.IsotopeEnrichment ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.IsotopeEnrichment ADD CONSTRAINT PK_IsotopeEnrichment PRIMARY KEY (Id);
GO


--------------------------------------------------------------------------------------------------
DROP INDEX targetedms.SampleFile.IX_SampleFile_InstrumentId;
GO
ALTER TABLE targetedms.SampleFile DROP CONSTRAINT FK_SampleFile_Instrument;
ALTER TABLE targetedms.Instrument DROP CONSTRAINT PK_Instrument;
GO
ALTER TABLE targetedms.Instrument ALTER COLUMN Id bigint NOT NULL;
ALTER TABLE targetedms.SampleFile ALTER COLUMN InstrumentId bigint;
GO
ALTER TABLE targetedms.Instrument ADD CONSTRAINT PK_Instrument PRIMARY KEY (Id);
ALTER TABLE targetedms.SampleFile ADD CONSTRAINT FK_SampleFile_Instrument FOREIGN KEY (InstrumentId) REFERENCES targetedms.Instrument(Id);
GO
CREATE INDEX IX_SampleFile_InstrumentId ON targetedms.SampleFile(InstrumentId);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.ListColumnDefinition DROP CONSTRAINT PK_ListColumn;
GO
ALTER TABLE targetedms.ListColumnDefinition ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.ListColumnDefinition ADD CONSTRAINT PK_ListColumn PRIMARY KEY(Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.ListItemValue DROP CONSTRAINT FK_ListItemValue_ListItem;
ALTER TABLE targetedms.ListItem DROP CONSTRAINT PK_ListItem;
GO
ALTER TABLE targetedms.ListItem ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.ListItem ADD CONSTRAINT PK_ListItem PRIMARY KEY(Id);
ALTER TABLE targetedms.ListItemValue ADD CONSTRAINT FK_ListItemValue_ListItem FOREIGN KEY(ListItemId) REFERENCES targetedms.ListItem(Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.ListItemValue DROP CONSTRAINT PK_ListItemValue;
GO
ALTER TABLE targetedms.ListItemValue ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.ListItemValue ADD CONSTRAINT PK_ListItemValue PRIMARY KEY(Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.MeasuredDriftTime DROP CONSTRAINT PK_MeasuredDriftTime;
GO
ALTER TABLE targetedms.MeasuredDriftTime ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.MeasuredDriftTime ADD CONSTRAINT PK_MeasuredDriftTime PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.BibliospecLibInfo DROP CONSTRAINT pk_bibliospeclibinfo;
GO
ALTER TABLE targetedms.BibliospecLibInfo ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.BibliospecLibInfo ADD CONSTRAINT pk_bibliospeclibinfo PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.ChromatogramLibInfo DROP CONSTRAINT PK_ChromatogramLibInfo;
GO
ALTER TABLE targetedms.ChromatogramLibInfo ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.ChromatogramLibInfo ADD CONSTRAINT PK_ChromatogramLibInfo PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.HunterLibInfo DROP CONSTRAINT pk_hunterlibinfo;
GO
ALTER TABLE targetedms.HunterLibInfo ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.HunterLibInfo ADD CONSTRAINT pk_hunterlibinfo PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.NistLibInfo DROP CONSTRAINT PK_NistLibInfo;
GO
ALTER TABLE targetedms.NistLibInfo ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.NistLibInfo ADD CONSTRAINT PK_NistLibInfo PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.SpectrastLibInfo DROP CONSTRAINT PK_SpectrastLibInfo;
GO
ALTER TABLE targetedms.SpectrastLibInfo ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.SpectrastLibInfo ADD CONSTRAINT PK_SpectrastLibInfo PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.PeptideAreaRatio DROP CONSTRAINT PK_PeptideAreaRatio;
GO
ALTER TABLE targetedms.PeptideAreaRatio ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.PeptideAreaRatio ADD CONSTRAINT PK_PeptideAreaRatio PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.PeptideGroupAnnotation DROP CONSTRAINT PK_PeptideGroupAnnotation;
GO
ALTER TABLE targetedms.PeptideGroupAnnotation ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.PeptideGroupAnnotation  ADD CONSTRAINT PK_PeptideGroupAnnotation PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.PeptideIsotopeModification DROP CONSTRAINT PK_PeptideIsotopeModification;
GO
ALTER TABLE targetedms.PeptideIsotopeModification ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.PeptideIsotopeModification  ADD CONSTRAINT PK_PeptideIsotopeModification PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.PeptideStructuralModification DROP CONSTRAINT PK_PeptideStructuralModification;
GO
ALTER TABLE targetedms.PeptideStructuralModification ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.PeptideStructuralModification  ADD CONSTRAINT PK_PeptideStructuralModification PRIMARY KEY (Id);
GO
--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.PrecursorAnnotation DROP CONSTRAINT PK_PrecursorAnnotation;
GO
ALTER TABLE targetedms.PrecursorAnnotation ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.PrecursorAnnotation  ADD CONSTRAINT PK_PrecursorAnnotation PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.PrecursorAreaRatio DROP CONSTRAINT PK_PrecursorAreaRatio;
GO
ALTER TABLE targetedms.PrecursorAreaRatio ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.PrecursorAreaRatio ADD CONSTRAINT PK_PrecursorAreaRatio PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.PrecursorChromInfoAnnotation DROP CONSTRAINT PK_PrecursorChromInfoAnnotation;
GO
ALTER TABLE targetedms.PrecursorChromInfoAnnotation ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.PrecursorChromInfoAnnotation ADD CONSTRAINT PK_PrecursorChromInfoAnnotation PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.ReplicateAnnotation DROP CONSTRAINT PK_ReplicateAnnotation;
GO
ALTER TABLE targetedms.ReplicateAnnotation ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.ReplicateAnnotation  ADD CONSTRAINT PK_ReplicateAnnotation PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.SampleFileChromInfo DROP CONSTRAINT PK_SampleFileChromInfo;
GO
ALTER TABLE targetedms.SampleFileChromInfo ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.SampleFileChromInfo ADD CONSTRAINT PK_SampleFileChromInfo PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.SpectrastLibInfo DROP CONSTRAINT PK_SpectrastLibInfo;
GO
ALTER TABLE targetedms.SpectrastLibInfo ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.SpectrastLibInfo ADD CONSTRAINT PK_SpectrastLibInfo PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.TransitionAnnotation DROP CONSTRAINT PK_TransitionAnnotation;
GO
ALTER TABLE targetedms.TransitionAnnotation ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.TransitionAnnotation ADD CONSTRAINT PK_TransitionAnnotation PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.TransitionAreaRatio DROP CONSTRAINT PK_TransitionAreaRatio;
GO
ALTER TABLE targetedms.TransitionAreaRatio ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.TransitionAreaRatio ADD CONSTRAINT PK_TransitionAreaRatio PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.TransitionAreaRatio DROP CONSTRAINT FK_TransitionAreaRatio_TransitionChromInfoStdId;
ALTER TABLE targetedms.TransitionChromInfoAnnotation DROP CONSTRAINT PK_TransitionChromInfoAnnotation;
GO
ALTER TABLE targetedms.TransitionChromInfoAnnotation ALTER COLUMN Id bigint NOT NULL;
GO
ALTER TABLE targetedms.TransitionChromInfoAnnotation ADD CONSTRAINT PK_TransitionChromInfoAnnotation PRIMARY KEY (Id)
ALTER TABLE targetedms.TransitionAreaRatio ADD CONSTRAINT FK_TransitionAreaRatio_TransitionChromInfoStdId FOREIGN KEY (TransitionChromInfoStdId) REFERENCES targetedms.TransitionChromInfo(Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.TransitionLoss DROP CONSTRAINT PK_TransitionLoss;
GO
ALTER TABLE targetedms.TransitionLoss ALTER COLUMN Id bigint;
GO
ALTER TABLE targetedms.TransitionLoss ADD CONSTRAINT PK_TransitionLoss PRIMARY KEY (Id);
GO

--------------------------------------------------------------------------------------------------
ALTER TABLE targetedms.TransitionOptimization DROP CONSTRAINT PK_TransitionOptimization;
GO
ALTER TABLE targetedms.TransitionOptimization ALTER COLUMN Id bigint;
GO
ALTER TABLE targetedms.TransitionOptimization ADD CONSTRAINT PK_TransitionOptimization PRIMARY KEY (Id);
GO
