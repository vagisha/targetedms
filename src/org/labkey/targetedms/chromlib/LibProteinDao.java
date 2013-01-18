package org.labkey.targetedms.chromlib;

import org.labkey.targetedms.chromlib.Constants.ProteinColumn;
import org.labkey.targetedms.chromlib.Constants.Table;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
/**
 * User: vsharma
 * Date: 1/2/13
 * Time: 10:15 PM
 */
public class LibProteinDao extends BaseDaoImpl<LibProtein>
{
    private final Dao<LibPeptide> _peptideDao;

    public LibProteinDao(Dao<LibPeptide> peptideDao)
    {
        _peptideDao = peptideDao;
    }

    @Override
    public void save(LibProtein protein, Connection connection) throws SQLException
    {
        if(protein != null)
        {
            super.save(protein, connection);

            if(_peptideDao != null)
            {
                for(LibPeptide peptide: protein.getPeptides())
                {
                    peptide.setProteinId(protein.getId());
                }
                _peptideDao.saveAll(protein.getPeptides(), connection);
            }
        }
    }

    @Override
    protected void setValuesInStatement(LibProtein protein, PreparedStatement stmt) throws SQLException
    {
        int colIndex = 1;
        stmt.setString(colIndex++, protein.getName());
        stmt.setString(colIndex++, protein.getDescription());
        stmt.setString(colIndex, protein.getSequence());
    }

    @Override
    public String getTableName()
    {
        return Table.Protein.name();
    }

    @Override
    protected Constants.ColumnDef[] getColumns()
    {
        return ProteinColumn.values();
    }

    @Override
    public void saveAll(List<LibProtein> proteins, Connection connection) throws SQLException
    {
        if(proteins != null && proteins.size() > 0)
        {
            super.saveAll(proteins, connection);

            List<LibPeptide> peptideList = new ArrayList<LibPeptide>();

            if(_peptideDao != null)
            {
                for(LibProtein protein: proteins)
                {
                    for(LibPeptide peptide: protein.getPeptides())
                    {
                        peptide.setProteinId(protein.getId());
                        peptideList.add(peptide);
                    }
                }
                _peptideDao.saveAll(peptideList, connection);
            }
        }
    }

    @Override
    public List<LibProtein> queryForForeignKey(String foreignKeyColumn, int foreignKeyValue, Connection connection) throws SQLException
    {
        throw new UnsupportedOperationException(getTableName()+" does not have a foreign key");
    }

    protected List<LibProtein> parseQueryResult(ResultSet rs) throws SQLException
    {
        List<LibProtein> proteins = new ArrayList<LibProtein>();
        while(rs.next())
        {
            LibProtein protein = new LibProtein();
            protein.setId(rs.getInt(ProteinColumn.Id.colName()));
            protein.setName(rs.getString(ProteinColumn.Name.colName()));
            protein.setDescription(rs.getString(ProteinColumn.Description.colName()));
            protein.setSequence(rs.getString(ProteinColumn.Sequence.colName()));

            proteins.add(protein);
        }
        return proteins;
    }

    public void loadPeptides(LibProtein protein, Connection connection) throws SQLException
    {
        List<LibPeptide> peptides = _peptideDao.queryForForeignKey(Constants.PeptideColumn.ProteinId.colName(), protein.getId(), connection);
        for(LibPeptide peptide: peptides)
        {
            protein.addPeptide(peptide);
        }
    }
}
