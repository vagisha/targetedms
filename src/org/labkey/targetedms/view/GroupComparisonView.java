/*
 * Copyright (c) 2017-2019 LabKey Corporation
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
package org.labkey.targetedms.view;

import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.GroupComparisonSettings;
import org.labkey.targetedms.query.FoldChangeTable;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Objects;

/**
 * Displays the fold changes which were calculated from a particular {@link GroupComparisonSettings}.
 */
public class GroupComparisonView extends QuantificationView
{
    public static final String DATAREGION_NAME = "group_comparison";
    public static final String DATAREGION_NAME_SM_MOL = DATAREGION_NAME + SMALL_MOLECULE_SUFFIX;
    private GroupComparisonSettings _groupComparisonSettings;

    public GroupComparisonView(ViewContext ctx, TargetedMSSchema schema, Form form, boolean forExport, String dataRegionName)
    {
        super(ctx, schema,
                isSmallMoleculeRegionName(dataRegionName) ? TargetedMSSchema.TABLE_MOLECULE_FOLD_CHANGE : TargetedMSSchema.TABLE_PEPTIDE_FOLD_CHANGE,
                    form, forExport, dataRegionName);
        _groupComparisonSettings = getGroupComparisons(_runId).stream()
                .filter(groupComparisonSettings -> Objects.equals(form.getGroupComparison(), groupComparisonSettings.getName()))
                .findFirst().get();
        setTitle(getTitle(_groupComparisonSettings));
    }

    @Override
    public TableInfo createTable()
    {
        FoldChangeTable table = (FoldChangeTable) _targetedMsSchema.getTable(_tableName);
        table.addCondition(new SimpleFilter(FieldKey.fromParts("RunId"), _runId));
        if (_groupComparisonSettings != null) {
            table.addCondition(table.getRealTable().getColumn("GroupComparisonSettingsId"), _groupComparisonSettings.getId());
        }
        return table;
    }

    public static void addGroupComparisonViewSwitcherMenuItems(TargetedMSController.RunDetailsForm runDetailsForm, NavTree menu) {
        Form groupComparisonForm = null;
        if (runDetailsForm instanceof Form) {
            groupComparisonForm = (Form) runDetailsForm;
        }
        for (GroupComparisonSettings groupComparison : getGroupComparisons(runDetailsForm.getId())) {
            if (groupComparisonForm != null && Objects.equals(groupComparison.getName(), groupComparisonForm.getGroupComparison())) {
                continue;
            }
            ActionURL url = new ActionURL(TargetedMSController.ShowGroupComparisonAction.class, runDetailsForm.getViewContext().getContainer());
            url.addParameter("id", runDetailsForm.getId());
            url.addParameter("groupComparison", groupComparison.getName());
            menu.addChild(getTitle(groupComparison), url);
        }
    }

    private static Collection<GroupComparisonSettings> getGroupComparisons(int runId) {
        return new TableSelector(
                TargetedMSManager.getTableInfoGroupComparisonSettings(),
                new SimpleFilter(FieldKey.fromParts("RunId"), runId), null)
                .getCollection(GroupComparisonSettings.class);

    }

    public static String getTitle(GroupComparisonSettings groupComparisonSettings) {
        return "Group Comparison: " + groupComparisonSettings.getName();
    }

    public static class Form extends TargetedMSController.RunDetailsForm
    {
        String _groupComparison;

        public String getGroupComparison()
        {
            return _groupComparison;
        }

        public void setGroupComparison(String groupComparison)
        {
            _groupComparison = groupComparison;
        }
    }
}
