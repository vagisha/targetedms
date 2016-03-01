package org.labkey.targetedms.query;

import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlSelector;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.parser.Molecule;

public class MoleculeManager
{
    private MoleculeManager() {}

    public static Molecule getMolecule(Container c, int id)
    {
        SQLFragment sql = new SQLFragment("SELECT mol.*, gm.* FROM ");
        sql.append(TargetedMSManager.getTableInfoMolecule(), "mol");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "r");
        sql.append(" WHERE ");
        sql.append("gm.PeptideGroupId = pg.Id AND mol.Id = gm.Id AND pg.RunId = r.Id AND r.Deleted = ? AND r.Container = ? AND mol.Id = ?");
        sql.add(false);
        sql.add(c.getId());
        sql.add(id);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Molecule.class);
    }

}
