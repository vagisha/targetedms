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

import org.labkey.api.data.Container;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryNestingOption;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSSchema;

import java.sql.SQLException;

/**
 * Base class for displaying peptide/molecule level calculations (i.e. fold changes and calibration curves).
 */
public abstract class QuantificationView extends DocumentPrecursorsView
{
    protected static final String SMALL_MOLECULE_SUFFIX = "_sm_mol";

    public QuantificationView(ViewContext ctx, TargetedMSSchema schema, String tableName, TargetedMSController.RunDetailsForm form,
                              boolean forExport, String dataRegionName)
    {
        super(ctx, schema, tableName, form.getId(), forExport,
                new QueryNestingOption(FieldKey.fromParts("PeptideGroupId"),
                        FieldKey.fromParts("PeptideGroupId", "Id"), null), dataRegionName);
    }

    public boolean isSmallMolecule()
    {
        return isSmallMoleculeRegionName(getDataRegionName());
    }

    public static boolean isSmallMoleculeRegionName(String regionName)
    {
        return regionName.endsWith(SMALL_MOLECULE_SUFFIX);
    }

    public static void addQuantificationMenuItems(TargetedMSController.RunDetailsForm runDetailsForm, NavTree menu)
    {
        GroupComparisonView.addGroupComparisonViewSwitcherMenuItems(runDetailsForm, menu);
        CalibrationCurvesView.addCalibrationCurvesViewSwitcherMenuItems(runDetailsForm, menu);
    }

    public static NavTree getViewSwitcherMenu(TargetedMSController.RunDetailsForm form)
    {
        Container container = form.getViewContext().getContainer();
        NavTree menu = new NavTree();
        ActionURL urlTransitionList = new ActionURL(TargetedMSController.ShowTransitionListAction.class, container);
        urlTransitionList.addParameter("id", form.getId());
        ActionURL urlPrecursorList = new ActionURL(TargetedMSController.ShowPrecursorListAction.class, container);
        urlPrecursorList.addParameter("id", form.getId());
        if (form.getDataRegionName().endsWith(SMALL_MOLECULE_SUFFIX))
        {
            menu.addChild(SmallMoleculeTransitionsView.TITLE, urlTransitionList);
            menu.addChild(SmallMoleculePrecursorsView.TITLE, urlPrecursorList);
        }
        else
        {
            menu.addChild(PeptideTransitionsView.TITLE, urlTransitionList);
            menu.addChild(PeptidePrecursorsView.TITLE, urlPrecursorList);
        }
        addQuantificationMenuItems(form, menu);
        return menu;
    }
}
