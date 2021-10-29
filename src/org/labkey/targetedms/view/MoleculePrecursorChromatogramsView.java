/*
 * Copyright (c) 2016-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.targetedms.view;

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.Molecule;
import org.labkey.targetedms.query.GeneralMoleculePrecursorChromatogramsTableInfo;
import org.springframework.validation.BindException;

import static org.labkey.targetedms.view.ChromatogramsDataRegion.MOLECULE_PRECURSOR_CHROM_DATA_REGION;

public class MoleculePrecursorChromatogramsView extends ChromatogramGridView
{
    public MoleculePrecursorChromatogramsView(Molecule molecule, TargetedMSSchema schema,
                                              TargetedMSController.ChromatogramForm form,
                                              BindException errors, ViewContext viewContext)
    {

        super(makeDataRegion(molecule, schema, form, viewContext), errors);
    }

    private static ChromatogramsDataRegion makeDataRegion(Molecule molecule, TargetedMSSchema schema,
                                             TargetedMSController.ChromatogramForm form, ViewContext viewContext)
    {
        GeneralMoleculePrecursorChromatogramsTableInfo tableInfo = new GeneralMoleculePrecursorChromatogramsTableInfo(molecule, schema, form);
        return new ChromatogramsDataRegion(viewContext,
                tableInfo,
                MOLECULE_PRECURSOR_CHROM_DATA_REGION,
                StringUtils.join(tableInfo.getDisplayColumnNames(), ","));
        }
}
