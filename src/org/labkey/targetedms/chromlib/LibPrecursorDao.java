package org.labkey.targetedms.chromlib;

import org.labkey.targetedms.chromlib.Constants.PrecursorColumn;
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
public class LibPrecursorDao extends BaseDaoImpl<LibPrecursor>
{
    private final Dao<LibPrecursorIsotopeModification> _precIsotopeModDao;
    private final Dao<LibPrecursorRetentionTime> _precRetentionTimeDao;
    private final Dao<LibTransition> _transitionDao;

    public LibPrecursorDao(Dao<LibPrecursorIsotopeModification> precIsotopeModDao,
                           Dao<LibPrecursorRetentionTime> precRetentionTimeDao,
                           Dao<LibTransition> transitionDao)
    {
        _precIsotopeModDao = precIsotopeModDao;
        _precRetentionTimeDao = precRetentionTimeDao;
        _transitionDao = transitionDao;
    }

    @Override
    public void save(LibPrecursor precursor, Connection connection) throws SQLException
    {
        if(precursor != null)
        {
            super.save(precursor, connection);

            if(_precIsotopeModDao != null)
            {
                for(LibPrecursorIsotopeModification isotopeMod: precursor.getIsotopeModifications())
                {
                    isotopeMod.setPrecursorId(precursor.getId());
                }
                _precIsotopeModDao.saveAll(precursor.getIsotopeModifications(), connection);
            }
            if(_precRetentionTimeDao != null)
            {
                for(LibPrecursorRetentionTime precRetentionTime: precursor.getRetentionTimes())
                {
                    precRetentionTime.setPrecursorId(precursor.getId());
                }
                _precRetentionTimeDao.saveAll(precursor.getRetentionTimes(), connection);
            }
            if(_transitionDao != null)
            {
                for(LibTransition transition: precursor.getTransitions())
                {
                    transition.setPrecursorId(precursor.getId());
                }
                _transitionDao.saveAll(precursor.getTransitions(), connection);
            }
        }
    }

    @Override
    protected void setValuesInStatement(LibPrecursor precursor, PreparedStatement stmt) throws SQLException
    {
        int colIndex = 1;
        stmt.setInt(colIndex++, precursor.getPeptideId());
        stmt.setString(colIndex++, precursor.getIsotopeLabel());
        stmt.setDouble(colIndex++, precursor.getMz());
        stmt.setInt(colIndex++, precursor.getCharge());
        stmt.setDouble(colIndex++, precursor.getNeutralMass());
        stmt.setString(colIndex++, precursor.getModifiedSequence());
        stmt.setObject(colIndex++, precursor.getCollisionEnergy(), Types.DOUBLE);
        stmt.setObject(colIndex++, precursor.getDeclusteringPotential(), Types.DOUBLE);
        stmt.setObject(colIndex++, precursor.getTotalArea(), Types.DOUBLE);
        stmt.setBoolean(colIndex++, precursor.getRepresentative());
        stmt.setObject(colIndex, precursor.getChromatogram(), Types.BLOB);
    }

    @Override
    public String getTableName()
    {
        return Table.Precursor.name();
    }

    @Override
    protected Constants.ColumnDef[] getColumns()
    {
        return PrecursorColumn.values();
    }

    @Override
    public void saveAll(List<LibPrecursor> precursors, Connection connection) throws SQLException
    {
        if(precursors != null && precursors.size() > 0)
        {
            super.saveAll(precursors, connection);

            List<LibPrecursorIsotopeModification> precIsotopeMods = new ArrayList<LibPrecursorIsotopeModification>();
            List<LibPrecursorRetentionTime> precRetentionTimes = new ArrayList<LibPrecursorRetentionTime>();
            List<LibTransition> transitions = new ArrayList<LibTransition>();

            if(_precIsotopeModDao != null)
            {
                for(LibPrecursor precursor: precursors)
                {
                    for(LibPrecursorIsotopeModification isotopeMod: precursor.getIsotopeModifications())
                    {
                        isotopeMod.setPrecursorId(precursor.getId());
                        precIsotopeMods.add(isotopeMod);
                    }
                }
                _precIsotopeModDao.saveAll(precIsotopeMods, connection);
            }
            if(_precRetentionTimeDao != null)
            {
                for(LibPrecursor precursor: precursors)
                {
                    for(LibPrecursorRetentionTime precRetentionTime: precursor.getRetentionTimes())
                    {
                        precRetentionTime.setPrecursorId(precursor.getId());
                        precRetentionTimes.add(precRetentionTime);
                    }
                }
                _precRetentionTimeDao.saveAll(precRetentionTimes, connection);
            }
            if(_transitionDao != null)
            {
                for(LibPrecursor precursor: precursors)
                {
                    for(LibTransition transition: precursor.getTransitions())
                    {
                        transition.setPrecursorId(precursor.getId());
                        transitions.add(transition);
                    }
                }
                _transitionDao.saveAll(transitions, connection);
            }
        }
    }

    protected List<LibPrecursor> parseQueryResult(ResultSet rs) throws SQLException
    {
        List<LibPrecursor> precursors = new ArrayList<LibPrecursor>();
        while(rs.next())
        {
            LibPrecursor precursor = new LibPrecursor();
            precursor.setId(rs.getInt(PrecursorColumn.Id.colName()));
            precursor.setPeptideId(rs.getInt(PrecursorColumn.PeptideId.colName()));
            precursor.setIsotopeLabel(rs.getString(PrecursorColumn.IsotopeLabel.colName()));
            precursor.setMz(rs.getDouble(PrecursorColumn.Mz.colName()));
            precursor.setCharge(rs.getInt(PrecursorColumn.Charge.colName()));
            precursor.setNeutralMass(rs.getDouble(PrecursorColumn.NeutralMass.colName()));
            precursor.setModifiedSequence(rs.getString(PrecursorColumn.ModifiedSequence.colName()));
            precursor.setCollisionEnergy(readDouble(rs, PrecursorColumn.CollisionEnergy.colName()));
            precursor.setDeclusteringPotential(readDouble(rs, PrecursorColumn.DeclusteringPotential.colName()));
            precursor.setTotalArea(rs.getDouble(PrecursorColumn.TotalArea.colName()));
            precursor.setRepresentative(rs.getBoolean(PrecursorColumn.Representative.colName()));
            precursor.setChromatogram(rs.getBytes(PrecursorColumn.Chromatogram.colName()));

            precursors.add(precursor);
        }
        return precursors;
    }

    public void loadTransitions(LibPrecursor precursor, Connection connection) throws SQLException
    {
        List<LibTransition> transitions = _transitionDao.queryForForeignKey(Constants.TransitionColumn.PrecursorId.colName(),
                                                                            precursor.getId(),
                                                                            connection);
        for(LibTransition transition: transitions)
        {
            precursor.addTransition(transition);
        }
    }

    public void loadPrecursorIsotopeModifications(LibPrecursor precursor, Connection connection) throws SQLException
    {
        List<LibPrecursorIsotopeModification> precIsotopeMods = _precIsotopeModDao.queryForForeignKey(Constants.PrecursorIsotopeModificationColumn.PrecursorId.colName(),
                                                                                                      precursor.getId(),
                                                                                                      connection);
        for(LibPrecursorIsotopeModification precIsoMod: precIsotopeMods)
        {
            precursor.addIsotopeModification(precIsoMod);
        }
    }

    public void loadPrecursorRetentionTimes(LibPrecursor precursor, Connection connection) throws SQLException
    {
        List<LibPrecursorRetentionTime> precRetTimes = _precRetentionTimeDao.queryForForeignKey(Constants.PrecursorRetentionTimeColumn.PrecursorId.colName(),
                                                                                                precursor.getId(),
                                                                                                connection);
        for(LibPrecursorRetentionTime precRt: precRetTimes)
        {
            precursor.addRetentionTime(precRt);
        }
    }
}
