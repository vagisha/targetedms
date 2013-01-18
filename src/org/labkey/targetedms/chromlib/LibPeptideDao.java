package org.labkey.targetedms.chromlib;

import org.labkey.targetedms.chromlib.Constants.PeptideColumn;
import org.labkey.targetedms.chromlib.Constants.Table;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * User: vsharma
 * Date: 1/2/13
 * Time: 10:15 PM
 */
public class LibPeptideDao extends BaseDaoImpl<LibPeptide>
{
    private final Dao<LibPeptideStructuralModification> _pepStrModDao;
    private final Dao<LibPrecursor> _precursorDao;

    public LibPeptideDao(Dao<LibPeptideStructuralModification> pepStrModDao,
                         Dao<LibPrecursor> precursorDao)
    {
        _pepStrModDao = pepStrModDao;
        _precursorDao = precursorDao;
    }

    @Override
    public void save(LibPeptide peptide, Connection connection) throws SQLException
    {
        if(peptide != null)
        {
            super.save(peptide, connection);

            if(_pepStrModDao != null)
            {
                for(LibPeptideStructuralModification strMod: peptide.getStructuralModifications())
                {
                    strMod.setPeptideId(peptide.getId());
                }
                _pepStrModDao.saveAll(peptide.getStructuralModifications(), connection);
            }
            if(_precursorDao != null)
            {
                for(LibPrecursor precursor: peptide.getPrecursors())
                {
                    precursor.setPeptideId(peptide.getId());
                }
                _precursorDao.saveAll(peptide.getPrecursors(), connection);
            }
        }
    }

    @Override
    protected void setValuesInStatement(LibPeptide peptide, PreparedStatement stmt) throws SQLException
    {
        int colIndex = 1;
        stmt.setObject(colIndex++, peptide.getProteinId(), Types.INTEGER);
        stmt.setObject(colIndex++, peptide.getSequence(), Types.VARCHAR);
        stmt.setObject(colIndex++, peptide.getStartIndex(), Types.INTEGER);
        stmt.setObject(colIndex++, peptide.getEndIndex(), Types.INTEGER);
        stmt.setObject(colIndex++, peptide.getPreviousAa(), Types.CHAR);
        stmt.setObject(colIndex++, peptide.getNextAa(), Types.CHAR);
        stmt.setDouble(colIndex++, peptide.getCalcNeutralMass());
        stmt.setInt(colIndex, peptide.getNumMissedCleavages());
    }

    @Override
    public String getTableName()
    {
        return Table.Peptide.name();
    }

    @Override
    protected Constants.ColumnDef[] getColumns()
    {
        return PeptideColumn.values();
    }

    @Override
    public void saveAll(List<LibPeptide> peptides, Connection connection) throws SQLException
    {
        if(peptides != null && peptides.size() > 0)
        {
            super.saveAll(peptides, connection);

            List<LibPeptideStructuralModification> strModList = new ArrayList<LibPeptideStructuralModification>();
            List<LibPrecursor> precursors = new ArrayList<LibPrecursor>();

            if(_pepStrModDao != null)
            {
                for(LibPeptide peptide: peptides)
                {
                    for(LibPeptideStructuralModification pepStrMod: peptide.getStructuralModifications())
                    {
                        pepStrMod.setPeptideId(peptide.getId());
                        strModList.add(pepStrMod);
                    }
                }
                _pepStrModDao.saveAll(strModList, connection);
            }
            if(_precursorDao != null)
            {
                for(LibPeptide peptide: peptides)
                {
                    for(LibPrecursor precursor: peptide.getPrecursors())
                    {
                        precursor.setPeptideId(peptide.getId());
                        precursors.add(precursor);
                    }
                }
                _precursorDao.saveAll(precursors, connection);
            }

        }
    }

    @Override
    public List<LibPeptide> queryForForeignKey(String foreignKeyColumn, int foreignKeyValue, Connection connection) throws SQLException
    {
        throw new UnsupportedOperationException(getTableName()+" does not have a foreign key");
    }

    protected List<LibPeptide> parseQueryResult(ResultSet rs) throws SQLException
    {
        List<LibPeptide> peptides = new ArrayList<LibPeptide>();
        while(rs.next())
        {
            LibPeptide peptide = new LibPeptide();
            peptide.setId(rs.getInt(PeptideColumn.Id.colName()));
            peptide.setProteinId(readInteger(rs, PeptideColumn.ProteinId.colName()));
            peptide.setSequence(rs.getString(PeptideColumn.Sequence.colName()));
            peptide.setStartIndex(readInteger(rs, PeptideColumn.StartIndex.colName()));
            peptide.setEndIndex(readInteger(rs, PeptideColumn.EndIndex.colName()));
            peptide.setPreviousAa(readCharacter(rs, PeptideColumn.PreviousAa.colName()));
            peptide.setNextAa(readCharacter(rs, PeptideColumn.NextAa.colName()));
            peptide.setCalcNeutralMass(rs.getDouble(PeptideColumn.CalcNeutralMass.colName()));
            peptide.setNumMissedCleavages(rs.getInt(PeptideColumn.NumMissedCleavages.colName()));

            peptides.add(peptide);
        }
        return peptides;
    }

    public void loadPrecursors(LibPeptide peptide, Connection connection) throws SQLException
    {
        List<LibPrecursor> precursors = _precursorDao.queryForForeignKey(Constants.PrecursorColumn.PeptideId.colName(),
                                                                         peptide.getId(),
                                                                         connection);
        for(LibPrecursor precursor: precursors)
        {
            peptide.addPrecursor(precursor);
        }
    }

    public void loadPeptideStructuralMods(LibPeptide peptide, Connection connection) throws SQLException
    {
        List<LibPeptideStructuralModification> precStructuralMods = _pepStrModDao.queryForForeignKey(Constants.PeptideStructuralModificationColumn.PeptideId.colName(),
                                                                                                     peptide.getId(),
                                                                                                     connection);
        for(LibPeptideStructuralModification precStrMod: precStructuralMods)
        {
            peptide.addStructuralModification(precStrMod);
        }
    }
}
