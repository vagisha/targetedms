/*
 * Copyright (c) 2014-2018 LabKey Corporation
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

import org.apache.commons.collections4.MultiValuedMap;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.targetedms.view.IconFactory;
import org.labkey.targetedms.view.ModifiedPeptideHtmlMaker;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
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
    HtmlString _cellData;

    public static final String PEPTIDE_COLUMN_NAME = "ModifiedPeptideDisplayColumn";
    public static final String PRECURSOR_COLUMN_NAME = "ModifiedPrecursorDisplayColumn";

    public ModifiedSequenceDisplayColumn(ColumnInfo colInfo)
    {
        super(colInfo);

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
    HtmlString getCellDataHtml(RenderContext ctx)
    {
        return _cellData;
    }

    public static class PeptideDisplayColumnFactory implements DisplayColumnFactory
    {
        private boolean _showNextAndPrevious = false;

        public PeptideDisplayColumnFactory()
        {
        }

        public PeptideDisplayColumnFactory(MultiValuedMap<String, String> map)
        {
            Collection<String> values = map == null ? Collections.emptyList() : map.get("showNextAndPrevious");
            if (!values.isEmpty())
            {
                _showNextAndPrevious = Boolean.valueOf(values.iterator().next());
            }
        }

        @Override
        public DisplayColumn createRenderer(ColumnInfo colInfo)
        {
            return new ModifiedSequenceDisplayColumn.PeptideCol(colInfo, _showNextAndPrevious);
        }
    }

    public static class PeptideCol extends ModifiedSequenceDisplayColumn
    {
        private final boolean _showNextAndPrevious;

        public PeptideCol(ColumnInfo colInfo)
        {
            this(colInfo, false);
        }

        public PeptideCol(ColumnInfo colInfo, boolean showNextAndPrevious)
        {
            super(colInfo);
            _showNextAndPrevious = showNextAndPrevious;
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
            if (_showNextAndPrevious)
            {
                keys.add(FieldKey.fromString(super.getParentFieldKey(), "PreviousAa"));
                keys.add(FieldKey.fromString(super.getParentFieldKey(), "NextAa"));
            }
        }

        @Override
        public void initialize(RenderContext ctx)
        {
            Long peptideId = (Long)ctx.get(FieldKey.fromString(super.getParentFieldKey(), "Id"));

            String sequence = (String)ctx.get(FieldKey.fromString(super.getParentFieldKey(), "Sequence"));

            Long runId = (Long)ctx.get(FieldKey.fromString(super.getParentFieldKey(), "PeptideGroupId/RunId"));

            Boolean decoy = (Boolean)ctx.get(FieldKey.fromString(super.getParentFieldKey(), "Decoy"));
            if(decoy == null)  decoy = Boolean.FALSE;

            String standardType = (String)ctx.get(FieldKey.fromString(super.getParentFieldKey(), "StandardType"));

            String previousAA = _showNextAndPrevious ? (String)ctx.get(FieldKey.fromString(super.getParentFieldKey(), "PreviousAa")) : null;
            String nextAA = _showNextAndPrevious ? (String)ctx.get(FieldKey.fromString(super.getParentFieldKey(), "NextAa")) : null;

            String peptideModifiedSequence = (String)getValue(ctx);

            if(peptideId == null || sequence == null || runId == null)
            {
                _cellData = HtmlString.of(peptideModifiedSequence);
            }
            else
            {
                _cellData = getHtmlMaker().getPeptideHtml(peptideId, sequence, peptideModifiedSequence, runId, previousAA, nextAA);
                _iconPath = IconFactory.getPeptideIconPath(peptideId, runId, decoy, standardType);
            }
        }
    }

    public static class PrecursorCol extends ModifiedSequenceDisplayColumn
    {
        public PrecursorCol(ColumnInfo colInfo)
        {
            super(colInfo);
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
            Long precursorId = (Long)ctx.get(FieldKey.fromString(super.getParentFieldKey(), "Id"));

            Long peptideId = (Long)ctx.get(FieldKey.fromString(super.getParentFieldKey(), "PeptideId"));

            String sequence = (String) ctx.get(FieldKey.fromString(super.getParentFieldKey(), "PeptideId/Sequence"));

            Long isotopeLabelId = (Long) ctx.get(FieldKey.fromString(super.getParentFieldKey(), "IsotopeLabelId"));

            Long runId = (Long) ctx.get(FieldKey.fromString(super.getParentFieldKey(), "PeptideId/PeptideGroupId/RunId"));

            Boolean decoy = (Boolean) ctx.get(FieldKey.fromString(super.getParentFieldKey(), "PeptideId/Decoy"));
            if(decoy == null) decoy = Boolean.FALSE;

            String precursorModifiedSequence = (String)getValue(ctx);

            if(precursorId == null || peptideId == null || isotopeLabelId == null || precursorModifiedSequence == null || sequence == null || runId == null)
            {
                _cellData = HtmlString.of(precursorModifiedSequence);
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
