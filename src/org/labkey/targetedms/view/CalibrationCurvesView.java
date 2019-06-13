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

import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.data.Sort;
import org.labkey.api.data.UrlColumn;
import org.labkey.api.view.DataView;
import org.labkey.targetedms.calculations.quantification.RegressionFit;
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
import org.labkey.targetedms.parser.QuantificationSettings;
import org.labkey.targetedms.query.CalibrationCurveTable;

import java.sql.SQLException;
import java.util.Collection;

/**
 * Shows a list of calibration curves.  Users can click on the details to see the {@link CalibrationCurveChart}.
 */
public class CalibrationCurvesView extends QuantificationView
{
    public static final String DATAREGION_NAME = "calibration_curves";
    public static final String DATAREGION_NAME_SM_MOL = DATAREGION_NAME + SMALL_MOLECULE_SUFFIX;

    public CalibrationCurvesView(ViewContext ctx, TargetedMSSchema schema, TargetedMSController.RunDetailsForm form,
                                 boolean forExport, String dataRegionName)
    {
        super(ctx, schema,
                isSmallMoleculeRegionName(dataRegionName)
                        ? TargetedMSSchema.TABLE_MOLECULE_CALIBRATION_CURVE
                        : TargetedMSSchema.TABLE_PEPTIDE_CALIBRATION_CURVE,
                form, forExport, dataRegionName);
        setTitle(isSmallMolecule() ? "Molecule Calibration Curves" : "Peptide Calibration Curves");

        if (isSmallMolecule())
        {
            getSettings().setBaseSort(new Sort("GeneralMoleculeId/CustomIonName"));
        }
        else
        {
            getSettings().setBaseSort(new Sort("GeneralMoleculeId/PeptideModifiedSequence"));
        }
    }

    @Override
    protected void setupDataView(DataView ret)
    {
        super.setupDataView(ret);

        ActionURL url = new ActionURL(TargetedMSController.ShowFiguresOfMeritAction.class, getContainer());
        url.addParameter("RunId", "${RunId}");
        url.addParameter("GeneralMoleculeId", "${GeneralMoleculeId}");
        SimpleDisplayColumn urlColumn = new UrlColumn(url.toString(), "Fom");
        ret.getDataRegion().addDisplayColumn(1, urlColumn);

        url = new ActionURL(TargetedMSController.ShowPKAction.class, getContainer());
        url.addParameter("RunId", "${RunId}");
        url.addParameter("GeneralMoleculeId", "${GeneralMoleculeId}");
        urlColumn = new UrlColumn(url.toString(), "PK");
        ret.getDataRegion().addDisplayColumn(2, urlColumn);

    }

    @Override
    public TableInfo createTable()
    {
        CalibrationCurveTable table = (CalibrationCurveTable) _targetedMsSchema.getTable(_tableName, null, true, true);
        table.addCondition(new SimpleFilter(FieldKey.fromParts("RunId"), _runId));
        table.setLocked(true);
        return table;
    }

    public static void addCalibrationCurvesViewSwitcherMenuItems(TargetedMSController.RunDetailsForm runDetailsForm, NavTree menu)
    {
        Collection<QuantificationSettings> quantSettingsList = getQuantificationSettings(runDetailsForm.getId());
        if (!quantSettingsList.stream().filter(setting ->
        {
            RegressionFit regressionFit = RegressionFit.parse(setting.getRegressionFit());
            return null != regressionFit && !RegressionFit.NONE.equals(regressionFit);
        }).findAny().isPresent())
        {
            return;
        }

        ActionURL url = new ActionURL(TargetedMSController.ShowCalibrationCurvesAction.class, runDetailsForm.getViewContext().getContainer());
        url.addParameter("id", runDetailsForm.getId());
        menu.addChild("Calibration Curves", url);
    }

    private static Collection<QuantificationSettings> getQuantificationSettings(int runId)
    {
        return new TableSelector(
                TargetedMSManager.getTableInfoQuantificationSettings(),
                new SimpleFilter(FieldKey.fromParts("RunId"), runId), null)
                .getCollection(QuantificationSettings.class);

    }

}
