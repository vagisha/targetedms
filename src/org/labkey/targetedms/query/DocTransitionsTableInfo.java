/*
 * Copyright (c) 2012 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.targetedms.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.IconDisplayColumn;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.settings.AppProps;
import org.labkey.api.view.ActionURL;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

import java.util.ArrayList;

/**
 * User: vsharma
 * Date: Apr 13, 2012
 */
public class DocTransitionsTableInfo extends FilteredTable
{
    private final TargetedMSSchema _schema;

    public DocTransitionsTableInfo(TargetedMSSchema schema)
    {
        super(TargetedMSManager.getTableInfoTransition(), schema.getContainer());
        _schema = schema;

        setName(TargetedMSSchema.TABLE_DOC_TRANSITIONS);

        //wrap all the columns
        wrapAllColumns(true);

        ColumnInfo peptideCol = getColumn("PrecursorId");
        peptideCol.setFk(new LookupForeignKey("Id")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return _schema.getTable(TargetedMSSchema.TABLE_PRECURSOR);
            }
        });

        // Display the fragment as y9 instead of 'y' and '9' in separate columns
        String sql = TargetedMSManager.getSqlDialect().concatenate(ExprColumn.STR_TABLE_ALIAS + ".FragmentType",
                                                                   "CAST(" + ExprColumn.STR_TABLE_ALIAS+".FragmentOrdinal AS VARCHAR)");
        SQLFragment fragmentSql = new SQLFragment(sql);
        ColumnInfo fragment = new ExprColumn(this, "Fragment", fragmentSql, JdbcType.VARCHAR);
        fragment.setTextAlign("Right");
        fragment.setJdbcType(JdbcType.VARCHAR);
        addColumn(fragment);


        ColumnInfo chromatogramsLinkCol = wrapColumn("Chromatograms", getRealTable().getColumn("Id"));
        chromatogramsLinkCol.setIsUnselectable(true);
        chromatogramsLinkCol.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            @Override
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                ActionURL url = new ActionURL(TargetedMSController.TransitionChromatogramChartAction.class, getContainer());
                return new IconDisplayColumn(colInfo, 18, 18, url, "id",
                                             AppProps.getInstance().getContextPath() + "/TargetedMS/images/Fragment.gif");
            }
        });
        addColumn(chromatogramsLinkCol);


        ArrayList<FieldKey> visibleColumns = new ArrayList<FieldKey>();
        visibleColumns.add(FieldKey.fromParts("PrecursorId", "PeptideId", "PeptideGroupId", "Label"));
        visibleColumns.add(FieldKey.fromParts("PrecursorId", "PeptideId", "PeptideGroupId", "Description"));

        // Peptide level information
        visibleColumns.add(FieldKey.fromParts("PrecursorId", "PeptideId", "Sequence"));
        visibleColumns.add(FieldKey.fromParts("PrecursorId", "PeptideId", "NumMissedCleavages"));
        visibleColumns.add(FieldKey.fromParts("PrecursorId", "PeptideId", "CalcNeutralMass"));
        visibleColumns.add(FieldKey.fromParts("PrecursorId", "PeptideId", "Rank"));


        // Precursor level information
        visibleColumns.add(FieldKey.fromParts("PrecursorId", "ModifiedPeptideHtml")); // Modified peptide column
        visibleColumns.add(FieldKey.fromParts("PrecursorId", "IsotopeLabelId", "Name"));
        visibleColumns.add(FieldKey.fromParts("PrecursorId", "NeutralMass"));
        visibleColumns.add(FieldKey.fromParts("PrecursorId", "Mz"));
        visibleColumns.add(FieldKey.fromParts("PrecursorId", "Charge"));

        // Transition level information
        visibleColumns.add(FieldKey.fromParts("Fragment"));
        visibleColumns.add(FieldKey.fromParts("Mz"));
        visibleColumns.add(FieldKey.fromParts("Charge"));
        visibleColumns.add(FieldKey.fromParts("Chromatograms"));

        setDefaultVisibleColumns(visibleColumns);

    }

    public void setRunId(int runId)
    {
        addRunFilter(runId);
    }

    public void addRunFilter(int runId)
    {
        getFilter().deleteConditions("Run");
        SQLFragment sql = new SQLFragment();
        sql.append("Id IN ");

        sql.append("(SELECT trans.Id FROM ");
        sql.append(TargetedMSManager.getTableInfoTransition(), "trans");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoPrecursor(), "prec");
        sql.append(" ON (trans.PrecursorId=prec.Id) ");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
        sql.append(" ON (prec.PeptideId=pep.Id) ");
        sql.append("INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(" ON (pep.PeptideGroupId=pg.Id) ");
        sql.append("WHERE pg.RunId=? ");
        sql.append(")");

        sql.add(runId);

        addCondition(sql, FieldKey.fromParts("Run"));
    }
}
