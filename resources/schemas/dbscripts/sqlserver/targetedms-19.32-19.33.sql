CREATE TABLE targetedms.BibliospecLibInfo
(
    Id INT IDENTITY(1, 1) NOT NULL,
    PrecursorId INT NOT NULL,
    SpectrumLibraryId INT NOT NULL,
    CountMeasured DOUBLE PRECISION,
    Score DOUBLE PRECISION,
    ScoreType VARCHAR(200),

    CONSTRAINT PK_BibliospecLibInfo PRIMARY KEY (Id),
    CONSTRAINT FK_BibliospecLibInfo_Precursor FOREIGN KEY (PrecursorId) REFERENCES targetedms.Precursor(Id),
    CONSTRAINT FK_BibliospecLibInfo_SpectrumLibrary FOREIGN KEY (SpectrumLibraryId) REFERENCES targetedms.SpectrumLibrary(Id)
);
CREATE INDEX IX_BibliospecLibInfo_PrecursorId ON targetedms.BibliospecLibInfo(PrecursorId);
CREATE INDEX IX_BibliospecLibInfo_SpectrumLibraryId ON targetedms.BibliospecLibInfo(SpectrumLibraryId);

INSERT INTO targetedms.BibliospecLibInfo (PrecursorId, SpectrumLibraryId, CountMeasured, Score, ScoreType)
SELECT PrecursorId, SpectrumLibraryId, Score1, Score2, NULL
FROM targetedms.PrecursorLibInfo pli
INNER JOIN targetedms.SpectrumLibrary sl ON (sl.id = pli.SpectrumLibraryId AND (sl.libraryType='bibliospec' OR sl.libraryType='bibliospec_lite')) ;

CREATE TABLE targetedms.HunterLibInfo
(
    Id INT IDENTITY(1, 1) NOT NULL,
    PrecursorId INT NOT NULL,
    SpectrumLibraryId INT NOT NULL,
    Expect DOUBLE PRECISION,
    ProcessedIntensity DOUBLE PRECISION,

    CONSTRAINT PK_HunterLibInfo PRIMARY KEY (Id),
    CONSTRAINT FK_HunterLibInfo_Precursor FOREIGN KEY (PrecursorId) REFERENCES targetedms.Precursor(Id),
    CONSTRAINT FK_HunterLibInfo_SpectrumLibrary FOREIGN KEY (SpectrumLibraryId) REFERENCES targetedms.SpectrumLibrary(Id)
);
CREATE INDEX IX_HunterLibInfo_PrecursorId ON targetedms.HunterLibInfo(PrecursorId);
CREATE INDEX IX_HunterLibInfo_SpectrumLibraryId ON targetedms.HunterLibInfo(SpectrumLibraryId);

INSERT INTO targetedms.HunterLibInfo (PrecursorId, SpectrumLibraryId, Expect, ProcessedIntensity)
SELECT PrecursorId, SpectrumLibraryId, Score1, Score2
FROM targetedms.PrecursorLibInfo pli
INNER JOIN targetedms.SpectrumLibrary sl ON (sl.id = pli.SpectrumLibraryId AND sl.libraryType='hunter') ;

CREATE TABLE targetedms.NistLibInfo
(
    Id INT IDENTITY(1, 1) NOT NULL,
    PrecursorId INT NOT NULL,
    SpectrumLibraryId INT NOT NULL,
    CountMeasured DOUBLE PRECISION,
    TotalIntensity DOUBLE PRECISION,
    TFRatio DOUBLE PRECISION,

    CONSTRAINT PK_NistLibInfo PRIMARY KEY (Id),
    CONSTRAINT FK_NistLibInfo_Precursor FOREIGN KEY (PrecursorId) REFERENCES targetedms.Precursor(Id),
    CONSTRAINT FK_NistLibInfo_SpectrumLibrary FOREIGN KEY (SpectrumLibraryId) REFERENCES targetedms.SpectrumLibrary(Id)
);
CREATE INDEX IX_NistLibInfo_PrecursorId ON targetedms.NistLibInfo(PrecursorId);
CREATE INDEX IX_NistLibInfo_SpectrumLibraryId ON targetedms.NistLibInfo(SpectrumLibraryId);

INSERT INTO targetedms.NistLibInfo (PrecursorId, SpectrumLibraryId, CountMeasured, TotalIntensity, TFRatio)
SELECT PrecursorId, SpectrumLibraryId, Score1, Score2, Score3
FROM targetedms.PrecursorLibInfo pli
INNER JOIN targetedms.SpectrumLibrary sl ON (sl.id = pli.SpectrumLibraryId AND sl.libraryType='nist') ;

CREATE TABLE targetedms.SpectrastLibInfo
(
    Id INT IDENTITY(1, 1) NOT NULL,
    PrecursorId INT NOT NULL,
    SpectrumLibraryId INT NOT NULL,
    CountMeasured DOUBLE PRECISION,
    TotalIntensity DOUBLE PRECISION,
    TFRatio DOUBLE PRECISION,

    CONSTRAINT PK_SpectrastLibInfo PRIMARY KEY (Id),
    CONSTRAINT FK_SpectrastLibInfo_Precursor FOREIGN KEY (PrecursorId) REFERENCES targetedms.Precursor(Id),
    CONSTRAINT FK_SpectrastLibInfo_SpectrumLibrary FOREIGN KEY (SpectrumLibraryId) REFERENCES targetedms.SpectrumLibrary(Id)
);
CREATE INDEX IX_SpectrastLibInfo_PrecursorId ON targetedms.SpectrastLibInfo(PrecursorId);
CREATE INDEX IX_SpectrastLibInfo_SpectrumLibraryId ON targetedms.SpectrastLibInfo(SpectrumLibraryId);

INSERT INTO targetedms.SpectrastLibInfo (PrecursorId, SpectrumLibraryId, CountMeasured, TotalIntensity, TFRatio)
SELECT PrecursorId, SpectrumLibraryId, Score1, Score2, Score3
FROM targetedms.PrecursorLibInfo pli
INNER JOIN targetedms.SpectrumLibrary sl ON (sl.id = pli.SpectrumLibraryId AND sl.libraryType='spectrast') ;

CREATE TABLE targetedms.ChromatogramLibInfo
(
    Id INT IDENTITY(1, 1) NOT NULL,
    PrecursorId INT NOT NULL,
    SpectrumLibraryId INT NOT NULL,
    PeakArea DOUBLE PRECISION,

    CONSTRAINT PK_ChromatogramLibInfo PRIMARY KEY (Id),
    CONSTRAINT FK_ChromatogramLibInfo_Precursor FOREIGN KEY (PrecursorId) REFERENCES targetedms.Precursor(Id),
    CONSTRAINT FK_ChromatogramLibInfo_SpectrumLibrary FOREIGN KEY (SpectrumLibraryId) REFERENCES targetedms.SpectrumLibrary(Id)
);
CREATE INDEX IX_ChromatogramLibInfo_PrecursorId ON targetedms.ChromatogramLibInfo(PrecursorId);
CREATE INDEX IX_ChromatogramLibInfo_SpectrumLibraryId ON targetedms.ChromatogramLibInfo(SpectrumLibraryId);

DROP TABLE targetedms.PrecursorLibInfo;
