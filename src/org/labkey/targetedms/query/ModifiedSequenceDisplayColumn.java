/*
 * Copyright (c) 2014-2016 LabKey Corporation
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
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.FieldKey;
import org.labkey.api.view.ActionURL;
import org.labkey.targetedms.view.IconFactory;
import org.labkey.targetedms.view.ModifiedPeptideHtmlMaker;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;


/**
 * User: vsharma
 * Date: 4/23/12
 * Time: 2:34 PM
 */
public abstract class ModifiedSequenceDisplayColumn extends IconColumn
{
    private final ModifiedPeptideHtmlMaker _htmlMaker;
    String _iconPath;
    String _cellData;

    public static final String PEPTIDE_COLUMN_NAME = "ModifiedPeptideDisplayColumn";
    public static final String PRECURSOR_COLUMN_NAME = "ModifiedPrecursorDisplayColumn";

    public ModifiedSequenceDisplayColumn(ColumnInfo colInfo, ActionURL url)
    {
        super(colInfo, url);

        _htmlMaker = new ModifiedPeptideHtmlMaker();
    }

    ModifiedPeptideHtmlMaker getHtmlMaker()
    {
        return _htmlMaker;
    }

    public abstract void initialize(RenderContext ctx);

    @Override
    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
    {
        initialize(ctx);
        super.renderGridCellContents(ctx, out);
    }

    @Override
    String getIconPath()
    {
        return _iconPath;
    }

    @Override
    String getCellDataHtml(RenderContext ctx)
    {
        return _cellData;
    }

    public static class PeptideCol extends ModifiedSequenceDisplayColumn
    {
        public PeptideCol(ColumnInfo colInfo, ActionURL url)
        {
            super(colInfo, url);
        }

        @Override
        String getLinkTitle()
        {
            return "Peptide Details";
        }

        @Override
        public void addQueryFieldKeys(Set<FieldKey> keys)
        {
            super.addQueryFieldKeys(keys);
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "Id"));
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "Sequence"));
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "Decoy"));
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "StandardType"));
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "PeptideGroupId/RunId"));
        }

        public void initialize(RenderContext ctx)
        {
            Integer peptideId = (Integer)ctx.get(FieldKey.fromString(super.getParentFieldKey(), "Id"));

            String sequence = (String)ctx.get(FieldKey.fromString(super.getParentFieldKey(), "Sequence"));

            Integer runId = (Integer)ctx.get(FieldKey.fromString(super.getParentFieldKey(), "PeptideGroupId/RunId"));

            Boolean decoy = (Boolean)ctx.get(FieldKey.fromString(super.getParentFieldKey(), "Decoy"));
            if(decoy == null)  decoy = Boolean.FALSE;

            String standardType = (String)ctx.get(FieldKey.fromString(super.getParentFieldKey(), "StandardType"));

            String peptideModifiedSequence = (String)getValue(ctx);

            if(peptideId == null || sequence == null || runId == null)
            {
                _cellData = peptideModifiedSequence;
            }
            else
            {
                _cellData = getHtmlMaker().getPeptideHtml(peptideId, sequence, peptideModifiedSequence, runId);
                _iconPath = IconFactory.getPeptideIconPath(peptideId, runId, decoy, standardType);
            }
        }
    }

    public static class PrecursorCol extends ModifiedSequenceDisplayColumn
    {
        public PrecursorCol(ColumnInfo colInfo, ActionURL url)
        {
            super(colInfo, url);
        }

        @Override
        String getLinkTitle()
        {
            return "Precursor Details";
        }

        @Override
        public void addQueryFieldKeys(Set<FieldKey> keys)
        {
            super.addQueryFieldKeys(keys);
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "Id"));
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "PeptideId"));
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "PeptideId/Decoy"));
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "PeptideId/Sequence"));
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "IsotopeLabelId"));
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "PeptideId/PeptideGroupId/RunId"));
        }

        @Override
        public void initialize(RenderContext ctx)
        {
            Integer precursorId = (Integer)ctx.get(FieldKey.fromString(super.getParentFieldKey(), "Id"));

            Integer peptideId = (Integer)ctx.get(FieldKey.fromString(super.getParentFieldKey(), "PeptideId"));

            String sequence = (String) ctx.get(FieldKey.fromString(super.getParentFieldKey(), "PeptideId/Sequence"));

            Integer isotopeLabelId = (Integer) ctx.get(FieldKey.fromString(super.getParentFieldKey(), "IsotopeLabelId"));

            Integer runId = (Integer) ctx.get(FieldKey.fromString(super.getParentFieldKey(), "PeptideId/PeptideGroupId/RunId"));

            Boolean decoy = (Boolean) ctx.get(FieldKey.fromString(super.getParentFieldKey(), "PeptideId/Decoy"));
            if(decoy == null) decoy = Boolean.FALSE;

            String precursorModifiedSequence = (String)getValue(ctx);

            if(precursorId == null || peptideId == null || isotopeLabelId == null || precursorModifiedSequence == null || sequence == null || runId == null)
            {
                _cellData = precursorModifiedSequence;
            }
            else
            {
                _cellData = getHtmlMaker().getPrecursorHtml(peptideId,
                        isotopeLabelId,
                        sequence,
                        precursorModifiedSequence,
                        runId);
                _iconPath = IconFactory.getPrecursorIconPath(precursorId, runId, decoy);
            }
        }
    }
}
