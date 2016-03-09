package org.labkey.targetedms.view;

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.DataRegion;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.view.GridView;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.Molecule;
import org.labkey.targetedms.query.GeneralMoleculePrecursorChromatogramsTableInfo;
import org.springframework.validation.Errors;

public class MoleculePrecursorChromatogramsView extends GridView
{
    public MoleculePrecursorChromatogramsView(Molecule molecule, TargetedMSSchema schema,
                                             TargetedMSController.ChromatogramForm form,
                                             Errors errors)
    {

        super(makeDataRegion(molecule, schema, form), errors);
        QuerySettings settings = new QuerySettings(getViewContext(), "Molecule and Molecule Precursor chromatograms");
        settings.setMaxRows(10);
        getDataRegion().setSettings(settings);
    }

    private static DataRegion makeDataRegion(Molecule molecule, TargetedMSSchema schema,
                                             TargetedMSController.ChromatogramForm form)
    {
        GeneralMoleculePrecursorChromatogramsTableInfo tableInfo = new GeneralMoleculePrecursorChromatogramsTableInfo(molecule, schema, form);
        DataRegion dRegion = new DataRegion();
        dRegion.setTable(tableInfo);
        dRegion.addColumns(tableInfo, StringUtils.join(tableInfo.getDisplayColumnNames(), ","));
        dRegion.setShadeAlternatingRows(false);
        dRegion.setShowPagination(true);
        dRegion.setShowPaginationCount(true);
        ButtonBar bar = new ButtonBar();
        dRegion.setButtonBar(bar);

        return dRegion;
    }
}
