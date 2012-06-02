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
import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.QueryDefinition;
import org.labkey.api.query.QueryException;
import org.labkey.api.query.QueryService;
import org.labkey.api.security.User;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.PeptideSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * User: vsharma
 * Date: 5/3/12
 * Time: 12:18 PM
 */
public class PeptidePrecursorChromatogramsTableInfo extends FilteredTable
{
    public PeptidePrecursorChromatogramsTableInfo(Container container, User user, Peptide peptide,
                                                  TargetedMSController.ChromatogramForm form)
    {
        super(getPivotByPrecursorChromInfoTable(container, user), container);
        wrapAllColumns(true);

        addPeptideFilter(peptide);

        ColumnInfo pepChromCol = getColumn("pepciId");
        pepChromCol.setLabel("");
        pepChromCol.setDisplayColumnFactory(new ChromatogramDisplayColumnFactory(
                                                        container,
                                                        ChromatogramDisplayColumnFactory.TYPE.PEPTIDE,
                                                        form.getChartWidth(),
                                                        form.getChartHeight(),
                                                        form.isSyncY(),
                                                        form.isSyncX())
                                            );


        for(ColumnInfo colInfo: getPrecursorChromInfoColumns())
        {
            colInfo.setDisplayColumnFactory(new ChromatogramDisplayColumnFactory(
                                                        container,
                                                        ChromatogramDisplayColumnFactory.TYPE.PRECURSOR,
                                                        form.getChartWidth(),
                                                        form.getChartHeight(),
                                                        form.isSyncY(),
                                                        form.isSyncX())
                                            );
            colInfo.setLabel("");
        }
    }

    private void addPeptideFilter(Peptide peptide)
    {
        addCondition(new SimpleFilter("peptideId", peptide.getId()));
    }

    private List<ColumnInfo> getPrecursorChromInfoColumns()
    {
        return getColumns().subList(5, getColumns().size());
    }

    public List<String> getDisplayColumnNames()
    {
        List<String> colNames = new ArrayList<String>();
        colNames.add("pepciId");
        for(ColumnInfo colInfo: getPrecursorChromInfoColumns())
        {
            String colName = colInfo.getName();
            int idx = colName.indexOf(':');
            if(idx != -1)
            {
                colName = colName.substring(0, idx);
            }
            if(PeptideSettings.IsotopeLabel.LIGHT.equalsIgnoreCase(colName))
            {
               colNames.add(1, colInfo.getName());
            }
            else
                colNames.add(colInfo.getName());
        }
        return colNames;
    }

    private static TableInfo getPivotByPrecursorChromInfoTable(Container container, User user)
    {
        String sql =
            "SELECT\n"+
            "  rep.Name AS replicate,\n"+
            "  sample.SampleName AS sample,\n" +
            "  label.Name AS label,\n" +
            "  pepci.Id AS pepciId,\n" +
            "  pepci.PeptideId AS peptideId,\n" +
            "  MIN(preci.Id) AS preciId\n" +

            "FROM\n"+
            "  " + TargetedMSManager.getTableInfoReplicate() + " rep,\n" +
            "  " + TargetedMSManager.getTableInfoSampleFile() + " sample,\n"+
            "  " + TargetedMSManager.getTableInfoIsotopeLabel() + " label,\n"+
            "  " + TargetedMSManager.getTableInfoPeptideChromInfo() + " pepci,\n"+
            "  " + TargetedMSManager.getTableInfoPrecursorChromInfo() + "  preci,\n"+
            "  " + TargetedMSManager.getTableInfoPrecursor() + "  pre\n"+
            "WHERE\n"+
            "  rep.Id = sample.ReplicateId\n" +
            "  AND\n" +
            "  sample.Id = pepci.SampleFileId\n" +
//            "  AND\n" +
//            "  pep.Id = pepci.PeptideId\n" +
            "  AND\n" +
            "  pepci.Id = preci.PeptideChromInfoId\n" +
            "  AND\n" +
            "  pre.Id = preci.PrecursorId\n" +
            "  AND\n" +
            "  pre.IsotopeLabelId = label.Id\n" +
            "GROUP BY rep.Name, sample.SampleName, pepci.Id, label.Name, pepci.PeptideId\n" +
            "PIVOT preciId BY label";


        QueryDefinition qdef = QueryService.get().createQueryDef(user, container, TargetedMSSchema.SCHEMA_NAME,
                                                                 "PeptideChromInfo_pivotByIsotopeLabel");
        qdef.setSql(sql);
        qdef.setIsHidden(true);

        List<QueryException> errors = new ArrayList<QueryException>();
        TableInfo tableInfo = qdef.getTable(errors, true);

        if (!errors.isEmpty())
        {
            StringBuilder sb = new StringBuilder();

            for (QueryException qe : errors)
            {
                sb.append(qe.getMessage()).append('\n');
            }
            throw new IllegalStateException(sb.toString());
        }
        return tableInfo;
    }
}
