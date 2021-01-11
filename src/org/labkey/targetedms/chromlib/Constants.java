/*
 * Copyright (c) 2013-2019 LabKey Corporation
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
package org.labkey.targetedms.chromlib;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * User: vsharma
 * Date: 12/31/12
 * Time: 2:26 PM
 */
class Constants
{
    private Constants() {}

    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public static final String LIB_FILE_DIR = "targetedMSLib";
    public static final String CHROM_LIB_FILE_NAME = "chromlib";
    public static final String CHROM_LIB_FILE_EXT = "clib";

    public static final String SCHEMA_VERSION = "2.0";

    public enum Table
    {
        LibInfo,
        IsotopeModification,
        SampleFile,

        // Proteomics
        StructuralModification,
        StructuralModLoss,
        Protein,
        Peptide,
        PeptideStructuralModification,
        Precursor,
        PrecursorIsotopeModification,
        PrecursorRetentionTime,
        Transition,

        // Small molecule
        MoleculeList,
        Molecule,
        MoleculePrecursor,
        MoleculePrecursorRetentionTime,
        MoleculeTransition,

        IrtLibrary
    }

    public enum Column
    {
        Id("INTEGER PRIMARY KEY"),

        PanoramaServer("VARCHAR(255) NOT NULL"),
        Container("VARCHAR(255) NOT NULL"),
        Created("TEXT NOT NULL"),
        SchemaVersion("VARCHAR(50) NOT NULL"),
        LibraryRevision("INTEGER NOT NULL"),
        Proteins("INTEGER NOT NULL"),
        Peptides("INTEGER NOT NULL"),
        Precursors("INTEGER NOT NULL"),
        Transitions("INTEGER NOT NULL"),
        MoleculeLists("INTEGER NOT NULL"),
        Molecules("INTEGER NOT NULL"),

        FilePath("VARCHAR(500) NOT NULL"),
        SampleName("VARCHAR(300) NOT NULL"),
        AcquiredTime("TEXT"),
        ModifiedTime("TEXT"),
        InstrumentIonizationType("VARCHAR(100)"),
        InstrumentAnalyzer("VARCHAR(100)"),
        InstrumentDetector("VARCHAR(100)"),

        Name,
        AminoAcid,
        Terminus("CHAR(1)"),
        Formula("VARCHAR(50)"),
        MassDiffMono("DOUBLE"),
        MassDiffAvg("DOUBLE"),
        UnimodId("INTEGER"),
        Variable("TINYINT NOT NULL"),
        ExplicitMod("TINYINT"),

        StructuralModId("INTEGER NOT NULL", Table.StructuralModification, Id),

        IsotopeLabel("VARCHAR(50) NOT NULL"),
        Label13C("TINYINT"),
        Label15N("TINYINT"),
        Label18O("TINYINT"),
        Label2H("TINYINT"),

        Description("TEXT"),
        Sequence,

        ProteinId("INTEGER", Table.Protein, Id),
        StartIndex("INTEGER"),
        EndIndex("INTEGER"),
        PreviousAa("CHAR(1)"),
        NextAa("CHAR(1)"),
        CalcNeutralMass("DOUBLE NOT NULL"),
        NumMissedCleavages("INTEGER NOT NULL"),

        IonFormula("VARCHAR(100)"),
        CustomIonName("VARCHAR(100)"),
        MassMonoisotopic("DOUBLE"),
        MassAverage("DOUBLE"),

        PeptideId("INTEGER NOT NULL", Table.Peptide, Id),
        IndexAa("INTEGER NOT NULL"),
        MassDiff("DOUBLE NOT NULL"),

        MoleculeListId("INTEGER", Table.MoleculeList, Id),
        MoleculeId("INTEGER NOT NULL", Table.Molecule, Id),
        MoleculeAccession("VARCHAR(500)"),

        Mz,
        Charge,
        NeutralMass,
        ModifiedSequence("TEXT NOT NULL"),
        CollisionEnergy("DOUBLE"),
        DeclusteringPotential("DOUBLE"),
        TotalArea("DOUBLE"),
        NumPoints("INTEGER"),
        NumTransitions("INTEGER"),
        AverageMassErrorPPM("DOUBLE"),
        Chromatogram("BLOB"),
        UncompressedSize("INTEGER"),
        ChromatogramFormat("INTEGER"),
        ExplicitIonMobility("DOUBLE"),
        CCS("DOUBLE"),
        IonMobilityMS1("DOUBLE"),
        IonMobilityFragment("DOUBLE"),
        IonMobilityWindow("DOUBLE"),
        IonMobilityType("VARCHAR(200)"),
        ExplicitIonMobilityUnits("VARCHAR(200)"),
        ExplicitCcsSqa("DOUBLE"),
        ExplicitCompensationVoltage("DOUBLE"),
        PrecursorConcentration("DOUBLE"),
        DriftTimeMs1("DOUBLE"),
        DriftTimeFragment("DOUBLE"),
        DriftTimeWindow("DOUBLE"),

        PrecursorId("INTEGER NOT NULL", Table.Precursor, Id),
        IsotopeModId("INTEGER NOT NULL", Table.IsotopeModification, Id),

        MoleculePrecursorId("INTEGER NOT NULL", Table.MoleculePrecursor, Id),

        SampleFileId("INTEGER", Table.SampleFile, Id),
        RetentionTime("DOUBLE"),
        StartTime("DOUBLE"),
        EndTime("DOUBLE"),

        NeutralLossMass("DOUBLE"),
        FragmentType("VARCHAR(10) NOT NULL"),
        FragmentOrdinal("INTEGER"),
        MassIndex("INTEGER"),
        Area("DOUBLE"),
        Height("DOUBLE"),
        Fwhm("DOUBLE"),
        MassErrorPPM("DOUBLE"),
        ChromatogramIndex("INTEGER"),

        IrtLibraryId("INTEGER NOT NULL", Table.IrtLibrary, Id),
        PeptideModSeq("TEXT NOT NULL"),
        Irt("DOUBLE"),
        Standard("BOOL"),
        TimeSource("INT");

        private final String definition;
        private final Table _fkTable;
        private final Column _fkColumn;

        Column()
        {
            this(null);
        }

        Column(String definition)
        {
            this(definition, null, null);
        }

        Column(String definition, Table fkTable, Column fkColumn)
        {
            this.definition = definition;
            _fkTable = fkTable;
            _fkColumn = fkColumn;
            if ((_fkTable == null && _fkColumn != null) || (_fkTable != null && _fkColumn == null))
            {
                throw new IllegalArgumentException("Both a table and column must be specified as the foreign key targets");
            }
        }

        public Table getFkTable()
        {
            return _fkTable;
        }

        public Column getFkColumn()
        {
            return _fkColumn;
        }
    }

    public interface ColumnDef
    {
        Column baseColumn();
        String definition();
    }

    public enum LibInfoColumn implements ColumnDef
    {
        PanoramaServer(Column.PanoramaServer),
        Container(Column.Container),
        Created(Column.Created),
        SchemaVersion(Column.SchemaVersion),
        LibraryRevision(Column.LibraryRevision),
        Proteins(Column.Proteins),
        Peptides(Column.Peptides),
        Precursors(Column.Precursors),
        Transitions(Column.Transitions),
        MoleculeLists(Column.MoleculeLists),
        Molecules(Column.Molecules);

        private final Column _column;

        LibInfoColumn(Column column)
        {
            _column = column;
        }

        @Override
        public Column baseColumn()
        {
            return _column;
        }

        @Override
        public String definition()
        {
            return _column.definition;
        }
    }

    public enum SampleFileColumn implements ColumnDef
    {
        Id(Column.Id),
        FilePath(Column.FilePath),
        SampleName(Column.SampleName),
        AcquiredTime(Column.AcquiredTime),
        ModifiedTime(Column.ModifiedTime),
        InstrumentIonizationType(Column.InstrumentIonizationType),
        InstrumentAnalyzer(Column.InstrumentAnalyzer),
        InstrumentDetector(Column.InstrumentDetector);

        private final Column _column;

        SampleFileColumn(Column column)
        {
            _column = column;
        }

        @Override
        public Column baseColumn()
        {
            return _column;
        }

        @Override
        public String definition()
        {
            return _column.definition;
        }
    }

    public enum StructuralModificationColumn implements ColumnDef
    {
        Id(Column.Id),
        Name(Column.Name, "VARCHAR(100) NOT NULL"),
        AminoAcid(Column.AminoAcid, "VARCHAR(30)"),
        Terminus(Column.Terminus),
        Formula(Column.Formula),
        MassDiffMono(Column.MassDiffMono),
        MassDiffAvg(Column.MassDiffAvg),
        UnimodId(Column.UnimodId),
        Variable(Column.Variable),
        ExplicitMod(Column.ExplicitMod);

        private final Column _column;
        private final String _definition;

        StructuralModificationColumn(Column column)
        {
            _column = column;
            _definition = column.definition;
        }
        StructuralModificationColumn(Column column, String definition)
        {
            _column = column;
            _definition = definition;
        }

        @Override
        public Column baseColumn()
        {
            return _column;
        }

        @Override
        public String definition()
        {
            return _definition;
        }
    }

    public enum StructuralModLossColumn implements ColumnDef
    {
        Id(Column.Id),
        StructuralModId(Column.StructuralModId),
        Formula(Column.Formula),
        MassDiffMono(Column.MassDiffMono),
        MassDiffAvg(Column.MassDiffAvg);

        private final Column _column;

        StructuralModLossColumn(Column column)
        {
            _column = column;
        }

        @Override
        public Column baseColumn()
        {
            return _column;
        }

        @Override
        public String definition()
        {
            return _column.definition;
        }
    }

    public enum IsotopeModificationColumn implements ColumnDef
    {
        Id(Column.Id),
        Name(Column.Name, "VARCHAR(100) NOT NULL"),
        IsotopeLabel(Column.IsotopeLabel),
        AminoAcid(Column.AminoAcid, "VARCHAR(30)"),
        Terminus(Column.Terminus),
        Formula(Column.Formula),
        MassDiffMono(Column.MassDiffMono),
        MassDiffAvg(Column.MassDiffAvg),
        Label13C(Column.Label13C),
        Label15N(Column.Label15N),
        Label18O(Column.Label18O),
        Label2H(Column.Label2H),
        UnimodId(Column.UnimodId);

        private final Column _column;
        private final String _definition;

        IsotopeModificationColumn(Column column)
        {
            _column = column;
            _definition = column.definition;
        }
        IsotopeModificationColumn(Column column, String definition)
        {
            _column = column;
            _definition = definition;
        }

        @Override
        public Column baseColumn()
        {
            return _column;
        }

        @Override
        public String definition()
        {
            return _definition;
        }
    }

    public enum ProteinColumn implements ColumnDef
    {
        Id(Column.Id),
        Name(Column.Name, "VARCHAR(250) NOT NULL"),
        Description(Column.Description),
        Sequence(Column.Sequence, "TEXT");

        private final Column _column;
        private final String _definition;

        ProteinColumn(Column column)
        {
            _column = column;
            _definition = column.definition;
        }
        ProteinColumn(Column column, String definition)
        {
            _column = column;
            _definition = definition;
        }
        @Override
        public Column baseColumn()
        {
            return _column;
        }

        @Override
        public String definition()
        {
            return _definition;
        }
    }

    public enum MoleculeListColumn implements ColumnDef
    {
        Id(Column.Id),
        Name(Column.Name, "VARCHAR(250) NOT NULL"),
        Description(Column.Description);

        private final Column _column;
        private final String _definition;

        MoleculeListColumn(Column column)
        {
            _column = column;
            _definition = column.definition;
        }
        MoleculeListColumn(Column column, String definition)
        {
            _column = column;
            _definition = definition;
        }
        @Override
        public Column baseColumn()
        {
            return _column;
        }

        @Override
        public String definition()
        {
            return _definition;
        }
    }

    public enum PeptideColumn implements ColumnDef
    {
        Id(Column.Id),
        ProteinId(Column.ProteinId),
        Sequence(Column.Sequence, "VARCHAR(100)"),
        StartIndex(Column.StartIndex),
        EndIndex(Column.EndIndex),
        PreviousAa(Column.PreviousAa),
        NextAa(Column.NextAa),
        CalcNeutralMass(Column.CalcNeutralMass),
        NumMissedCleavages(Column.NumMissedCleavages);

        private final Column _column;
        private final String _definition;

        PeptideColumn(Column column)
        {
            _column = column;
            _definition = column.definition;
        }
        PeptideColumn(Column column, String definition)
        {
            _column = column;
            _definition = definition;
        }

        @Override
        public Column baseColumn()
        {
            return _column;
        }

        @Override
        public String definition()
        {
            return _definition;
        }
    }

    public enum PeptideStructuralModificationColumn implements ColumnDef
    {
        Id(Column.Id),
        PeptideId(Column.PeptideId),
        StructuralModId(Column.StructuralModId),
        IndexAa(Column.IndexAa),
        MassDiff(Column.MassDiff);

        private final Column _column;

        PeptideStructuralModificationColumn(Column column)
        {
            _column = column;
        }

        @Override
        public Column baseColumn()
        {
            return _column;
        }

        @Override
        public String definition()
        {
            return _column.definition;
        }
    }

    public enum PrecursorColumn implements ColumnDef
    {
        Id(Column.Id),
        PeptideId(Column.PeptideId),
        IsotopeLabel(Column.IsotopeLabel),
        Mz(Column.Mz, "DOUBLE NOT NULL"),
        Charge(Column.Charge, "INTEGER NOT NULL"),
        NeutralMass(Column.NeutralMass, "DOUBLE NOT NULL"),
        ModifiedSequence(Column.ModifiedSequence),
        CollisionEnergy(Column.CollisionEnergy),
        DeclusteringPotential(Column.DeclusteringPotential),
        TotalArea(Column.TotalArea),
        NumTransitions(Column.NumTransitions),
        NumPoints(Column.NumPoints),
        AverageMassErrorPPM(Column.AverageMassErrorPPM),
        SampleFileId(Column.SampleFileId),
        Chromatogram(Column.Chromatogram),
        UncompressedSize(Column.UncompressedSize),
        ChromatogramFormat(Column.ChromatogramFormat),
        ExplicitIonMobility(Column.ExplicitIonMobility),
        CCS(Column.CCS),
        IonMobilityMS1(Column.IonMobilityMS1),
        IonMobilityFragment(Column.IonMobilityFragment),
        IonMobilityWindow(Column.IonMobilityWindow),
        IonMobilityType(Column.IonMobilityType),
        ExplicitIonMobilityUnits(Column.ExplicitIonMobilityUnits),
        ExplicitCcsSqa(Column.ExplicitCcsSqa),
        ExplicitCompensationVoltage(Column.ExplicitCompensationVoltage),
        PrecursorConcentration(Column.PrecursorConcentration),
        DriftTimeMs1(Column.DriftTimeMs1),
        DriftTimeFragment(Column.DriftTimeFragment),
        DriftTimeWindow(Column.DriftTimeWindow);

        private final Column _column;
        private final String _definition;

        PrecursorColumn(Column column)
        {
            _column = column;
            _definition = column.definition;
        }
        PrecursorColumn(Column column, String definition)
        {
            _column = column;
            _definition = definition;
        }
        @Override
        public Column baseColumn()
        {
            return _column;
        }

        @Override
        public String definition()
        {
            return _definition;
        }
    }

    public enum PrecursorIsotopeModificationColumn implements ColumnDef
    {
        Id(Column.Id),
        PrecursorId(Column.PrecursorId),
        IsotopeModId(Column.IsotopeModId),
        IndexAa(Column.IndexAa),
        MassDiff(Column.MassDiff);

        private final Column _column;

        PrecursorIsotopeModificationColumn(Column column)
        {
            _column = column;
        }

        @Override
        public Column baseColumn()
        {
            return _column;
        }

        @Override
        public String definition()
        {
            return _column.definition;
        }
    }

    public enum PrecursorRetentionTimeColumn implements ColumnDef
    {
        Id(Column.Id),
        PrecursorId(Column.PrecursorId),
        SampleFileId(Column.SampleFileId),
        RetentionTime(Column.RetentionTime),
        StartTime(Column.StartTime),
        EndTime(Column.EndTime);

        private final Column _column;

        PrecursorRetentionTimeColumn(Column column)
        {
            _column = column;
        }

        @Override
        public Column baseColumn()
        {
            return _column;
        }

        @Override
        public String definition()
        {
            return _column.definition;
        }
    }

    public enum TransitionColumn implements ColumnDef
    {
        Id(Column.Id),
        PrecursorId(Column.PrecursorId),
        Mz(Column.Mz, "DOUBLE"),
        Charge(Column.Charge, "INTEGER"),
        NeutralMass(Column.NeutralMass, "DOUBLE"),
        NeutralLossMass(Column.NeutralLossMass),
        FragmentType(Column.FragmentType),
        FragmentOrdinal(Column.FragmentOrdinal),
        MassIndex(Column.MassIndex),
        Area(Column.Area),
        Height(Column.Height),
        Fwhm(Column.Fwhm),
        MassErrorPPM(Column.MassErrorPPM),
        ChromatogramIndex(Column.ChromatogramIndex);

        private final Column _column;
        private final String _definition;

        TransitionColumn(Column column)
        {
            _column = column;
            _definition = column.definition;
        }
        TransitionColumn(Column column, String definition)
        {
            _column = column;
            _definition = definition;
        }
        @Override
        public Column baseColumn()
        {
            return _column;
        }
        @Override
        public String definition()
        {
            return _definition;
        }
    }

    public enum MoleculeColumn implements ColumnDef
    {
        Id(Column.Id),
        MoleculeListId(Column.MoleculeListId),
        IonFormula(Column.IonFormula),
        CustomIonName(Column.CustomIonName),
        MassMonoisotopic(Column.MassMonoisotopic),
        MassAverage(Column.MassAverage),
        MoleculeAccession(Column.MoleculeAccession);

        private final Column _column;
        private final String _definition;

        MoleculeColumn(Column column)
        {
            _column = column;
            _definition = column.definition;
        }
        @Override
        public Column baseColumn()
        {
            return _column;
        }

        @Override
        public String definition()
        {
            return _definition;
        }
    }

    public enum MoleculePrecursorColumn implements ColumnDef
    {
        Id(Column.Id),
        MoleculeId(Column.MoleculeId),
        IsotopeLabel(Column.IsotopeLabel),
        Mz(Column.Mz, "DOUBLE NOT NULL"),
        Charge(Column.Charge, "INTEGER NOT NULL"),
        CollisionEnergy(Column.CollisionEnergy),
        DeclusteringPotential(Column.DeclusteringPotential),
        TotalArea(Column.TotalArea),
        NumTransitions(Column.NumTransitions),
        NumPoints(Column.NumPoints),
        AverageMassErrorPPM(Column.AverageMassErrorPPM),
        SampleFileId(Column.SampleFileId),
        Chromatogram(Column.Chromatogram),
        UncompressedSize(Column.UncompressedSize),
        ChromatogramFormat(Column.ChromatogramFormat),
        MassMonoisotopic(Column.MassMonoisotopic),
        MassAverage(Column.MassAverage),
        IonFormula(Column.IonFormula),
        CustomIonName(Column.CustomIonName),
        ExplicitIonMobility(Column.ExplicitIonMobility),
        CCS(Column.CCS),
        IonMobilityMS1(Column.IonMobilityMS1),
        IonMobilityFragment(Column.IonMobilityFragment),
        IonMobilityWindow(Column.IonMobilityWindow),
        IonMobilityType(Column.IonMobilityType),
        ExplicitIonMobilityUnits(Column.ExplicitIonMobilityUnits),
        ExplicitCcsSqa(Column.ExplicitCcsSqa),
        ExplicitCompensationVoltage(Column.ExplicitCompensationVoltage),
        PrecursorConcentration(Column.PrecursorConcentration),
        DriftTimeMs1(Column.DriftTimeMs1),
        DriftTimeFragment(Column.DriftTimeFragment),
        DriftTimeWindow(Column.DriftTimeWindow);

        private final Column _column;
        private final String _definition;

        MoleculePrecursorColumn(Column column)
        {
            _column = column;
            _definition = column.definition;
        }

        MoleculePrecursorColumn(Column column, String definition)
        {
            _column = column;
            _definition = definition;
        }
        @Override
        public Column baseColumn()
        {
            return _column;
        }

        @Override
        public String definition()
        {
            return _definition;
        }
    }

    public enum MoleculePrecursorRetentionTimeColumn implements ColumnDef
    {
        Id(Column.Id),
        MoleculePrecursorId(Column.MoleculePrecursorId),
        SampleFileId(Column.SampleFileId),
        RetentionTime(Column.RetentionTime),
        StartTime(Column.StartTime),
        EndTime(Column.EndTime);

        private final Column _column;

        MoleculePrecursorRetentionTimeColumn(Column column)
        {
            _column = column;
        }

        @Override
        public Column baseColumn()
        {
            return _column;
        }

        @Override
        public String definition()
        {
            return _column.definition;
        }
    }

    public enum MoleculeTransitionColumn implements ColumnDef
    {
        Id(Column.Id),
        MoleculePrecursorId(Column.MoleculePrecursorId),
        Mz(Column.Mz, "DOUBLE"),
        Charge(Column.Charge, "INTEGER"),
        FragmentType(Column.FragmentType),
        MassIndex(Column.MassIndex),
        Area(Column.Area),
        Height(Column.Height),
        Fwhm(Column.Fwhm),
        MassErrorPPM(Column.MassErrorPPM),
        ChromatogramIndex(Column.ChromatogramIndex);

        private final Column _column;
        private final String _definition;

        MoleculeTransitionColumn(Column column)
        {
            _column = column;
            _definition = column.definition;
        }

        MoleculeTransitionColumn(Column column, String definition)
        {
            _column = column;
            _definition = definition;
        }

        @Override
        public Column baseColumn()
        {
            return _column;
        }
        @Override
        public String definition()
        {
            return _definition;
        }
    }



    public enum IrtLibraryColumn implements ColumnDef
    {
        Id(Column.Id),
        PeptideModSeq(Column.PeptideModSeq),
        Standard(Column.Standard),
        Irt(Column.Irt),
        TimeSource(Column.TimeSource);

        private final Column _column;
        private final String _definition;

        IrtLibraryColumn(Column column)
        {
            _column = column;
            _definition = column.definition;
        }

        @Override
        public Column baseColumn()
        {
            return _column;
        }

        @Override
        public String definition()
        {
            return _definition;
        }
    }
}

