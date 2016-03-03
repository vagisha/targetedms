/*
 * Copyright (c) 2012-2016 LabKey Corporation
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

import com.drew.lang.annotations.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.QueryDefinition;
import org.labkey.api.query.QueryException;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.SchemaKey;
import org.labkey.api.security.User;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.GeneralMolecule;
import org.labkey.targetedms.parser.PeptideSettings;
import org.labkey.targetedms.parser.ReplicateAnnotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * User: vsharma
 * Date: 5/3/12
 * Time: 12:18 PM
 */
public class PeptidePrecursorChromatogramsTableInfo extends FilteredTable<TargetedMSSchema>
{
    private static final String _peptideChromInfoCol = "pepciId";

    public PeptidePrecursorChromatogramsTableInfo(TargetedMSSchema schema, GeneralMolecule generalMolecule,
                                                  TargetedMSController.ChromatogramForm form)
    {
        super(getPivotByPrecursorChromInfoTable(schema.getContainer(), schema.getUser(), generalMolecule, form.getAnnotationFilter(), form.getReplicatesFilterList()), schema);
        wrapAllColumns(true);
        ColumnInfo pepChromCol = getColumn(_peptideChromInfoCol);
        pepChromCol.setLabel("");

        pepChromCol.setDisplayColumnFactory(new ChromatogramDisplayColumnFactory(
                                                        schema.getContainer(),
                                                        ChromatogramDisplayColumnFactory.TYPE.PEPTIDE,
                                                        form.getChartWidth(),
                                                        form.getChartHeight(),
                                                        form.isSyncY(),
                                                        form.isSyncX(),
                                                        form.isSplitGraph(),
                                                        form.isShowOptimizationPeaks(),
                                                        form.getAnnotationsFilter(),
                                                        form.getReplicatesFilter()
                                                        ));


        for(ColumnInfo colInfo: getPrecursorChromInfoColumns())
        {
            colInfo.setDisplayColumnFactory(new ChromatogramDisplayColumnFactory(
                                                        schema.getContainer(),
                                                        ChromatogramDisplayColumnFactory.TYPE.PRECURSOR,
                                                        form.getChartWidth(),
                                                        form.getChartHeight(),
                                                        form.isSyncY(),
                                                        form.isSyncX(),
                                                        form.isSplitGraph(),
                                                        form.isShowOptimizationPeaks(),
                                                        form.getAnnotationsFilter(),
                                                        form.getReplicatesFilter()
                                            ));
            colInfo.setLabel("");
        }
    }

    private List<ColumnInfo> getPrecursorChromInfoColumns()
    {
        return getColumns().subList(4, getColumns().size());
    }

    public List<String> getDisplayColumnNames()
    {
        List<String> colNames = new ArrayList<>();
        colNames.add(_peptideChromInfoCol);
		// Sort the precursor chrom info columns
        List<ColumnInfo> colInfoList = new ArrayList<>(getPrecursorChromInfoColumns());
        Collections.sort(colInfoList, new Comparator<ColumnInfo>()
        {
            @Override
            public int compare(ColumnInfo o1, ColumnInfo o2)
            {
                return o1.getName().compareTo(o2.getName());
            }
        });

		// Add the light labeled precursor chrom info first.
        int lightColIndex = 1;
        for(ColumnInfo colInfo: colInfoList)
        {
            String colName = colInfo.getName();
            int idx = colName.indexOf(':');
            if(idx != -1)
            {
                colName = colName.substring(0, idx);
            }
            if(colName.startsWith(PeptideSettings.IsotopeLabel.LIGHT))
            {
                colNames.add(lightColIndex, colInfo.getName());
                lightColIndex++;
            }
            else
                colNames.add(colInfo.getName());
        }
        return colNames;
    }

    private static TableInfo getPivotByPrecursorChromInfoTable(Container container, User user, GeneralMolecule generalMolecule,@Nullable List<ReplicateAnnotation> filterAnnotations, @Nullable List<Integer> replicatesFilter)
    {
        SQLFragment sql = new SQLFragment("SELECT");
        sql.append(" replicate, sample,isotopecharge, ").append(_peptideChromInfoCol).append(", MIN(preciId) AS preciId FROM");
        sql.append(" ( SELECT");
        sql.append(" SampleFileId.ReplicateId.Name AS replicate");
        sql.append(", SampleFileId.SampleName AS sample");
        sql.append(", GeneralMoleculeChromInfoId AS ").append(_peptideChromInfoCol);
        sql.append(", (PrecursorId.IsotopeLabelId.Name || CAST(PrecursorId.Charge AS VARCHAR)) AS isotopecharge");
        sql.append(", Id AS preciId");
        sql.append(" FROM ");
        sql.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "pci");
        sql.append(" WHERE PrecursorId.GeneralMoleculeId=").append(generalMolecule.getId());
        sql.append(" AND OptimizationStep IS NULL "); // Ignore precursorChromInfos for optimization peaks (e.g. Collision energy optimization)
        if(replicatesFilter != null && replicatesFilter.size() != 0)
        {
            sql.append(" AND ");
            sql.append("( ");
            String replicateIds = StringUtils.join(replicatesFilter,",");
            sql.append("SampleFileId.ReplicateId IN ("+replicateIds+")");
            sql.append(")");
        }
        if(filterAnnotations != null)
        {
            sql.append(" AND ")
               .append(" SampleFileId.ReplicateId IN (SELECT replicateId FROM ")
               .append(TargetedMSManager.getTableInfoReplicateAnnotation(), "repAnnot")
               .append(" WHERE ");
            boolean first = true;
            for(ReplicateAnnotation annotation: filterAnnotations)
            {
                if(!first)
                {
                    sql.append(" OR ");
                }
                sql.append(" (name = '" + annotation.getName() +"'  ")
                   .append("  AND value = '" + annotation.getValue()+"')");

                first = false;
            }
            sql.append(")");
        }
        sql.append(" ) X");
        sql.append(" GROUP BY replicate, sample, ").append(_peptideChromInfoCol).append(", isotopecharge")
           .append(" PIVOT preciId BY isotopecharge");

        QueryDefinition qdef = QueryService.get().createQueryDef(user, container, SchemaKey.fromString(TargetedMSSchema.SCHEMA_NAME),
                                                                 "PeptideChromInfo_pivotByIsotopeLabelCharge");
        qdef.setSql(sql.getSQL());
        qdef.setIsHidden(true);

        List<QueryException> errors = new ArrayList<>();
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
