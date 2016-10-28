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

package org.labkey.targetedms.view;

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.DataRegion;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.view.GridView;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.query.GeneralMoleculePrecursorChromatogramsTableInfo;
import org.springframework.validation.Errors;

/**
 * User: vsharma
 * Date: 5/7/12
 * Time: 4:17 PM
 */
public class PeptidePrecursorChromatogramsView extends GridView
{
    public PeptidePrecursorChromatogramsView(Peptide peptide, TargetedMSSchema schema,
                                             TargetedMSController.ChromatogramForm form,
                                             Errors errors)
    {

        super(makeDataRegion(peptide, schema, form), errors);
        QuerySettings settings = new QuerySettings(getViewContext(), "Peptide and Precursor chromatograms");
        settings.setMaxRows(10);
        getDataRegion().setSettings(settings);
    }

    private static DataRegion makeDataRegion(Peptide peptide, TargetedMSSchema schema,
                                             TargetedMSController.ChromatogramForm form)
    {
        GeneralMoleculePrecursorChromatogramsTableInfo tableInfo = new GeneralMoleculePrecursorChromatogramsTableInfo(peptide, schema, form);
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
