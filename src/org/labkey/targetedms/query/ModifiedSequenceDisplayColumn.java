/*
 * Copyright (c) 2012-2014 LabKey Corporation
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
import org.labkey.api.data.DataColumn;
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
public abstract class ModifiedSequenceDisplayColumn extends DataColumn
{
    private final ActionURL _linkUrl;
    private final ModifiedPeptideHtmlMaker _htmlMaker;

    public static final String PEPTIDE_COLUMN_NAME = "ModifiedPeptideDisplayColumn";
    public static final String PRECURSOR_COLUMN_NAME = "ModifiedPrecursorDisplayColumn";

    private final FieldKey _parentFieldKey;

    public ModifiedSequenceDisplayColumn(ColumnInfo colInfo, ActionURL url)
    {
        super(colInfo);


        _linkUrl = url;

        _htmlMaker = new ModifiedPeptideHtmlMaker();

        _parentFieldKey = colInfo.getFieldKey().getParent();
        setTextAlign("left");
    }

    ActionURL getLinkUrl()
    {
        return _linkUrl;
    }

    ModifiedPeptideHtmlMaker getHtmlMaker()
    {
        return _htmlMaker;
    }

    FieldKey getParentFieldKey()
    {
        return _parentFieldKey;
    }

    public abstract String getModifiedSequenceHtml(RenderContext ctx);

    @Override
    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
    {

        String sequenceHtml = getModifiedSequenceHtml(ctx);
        if(sequenceHtml == null)
        {
            super.renderGridCellContents(ctx, out);
            return;
        }

        out.write(sequenceHtml);
    }

    private String makeImageHtml(String iconPath, String title)
    {
        StringBuilder imgHtml = new StringBuilder();
        imgHtml.append("<a href=\"").append(_linkUrl.getLocalURIString()).append("\" title=\"").append(title).append("\">");
        imgHtml.append("<img src=\"").append(iconPath).append("\"").append(" width=\"18\" height=\"16\" style=\"margin-right: 5px;\"/>");
        imgHtml.append("</a>");
        return imgHtml.toString();
    }

    private String combineHtml(String sequenceHtml, String imgHtml)
    {
        return "<nobr>"
                + imgHtml
                + "<a href=\""+_linkUrl.getLocalURIString()+"\" style=\"color: #000000\">" + sequenceHtml + "</a>"
                + "</nobr>";
    }

    public static class PeptideCol extends ModifiedSequenceDisplayColumn
    {

        public PeptideCol(ColumnInfo colInfo, ActionURL url)
        {
            super(colInfo, url);
        }

        @Override
        public void addQueryFieldKeys(Set<FieldKey> keys)
        {
            super.addQueryFieldKeys(keys);
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "Id"));
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "Sequence"));
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "PeptideGroupId/RunId"));
        }

        public String getModifiedSequenceHtml(RenderContext ctx)
        {

            Object peptideId = ctx.get(FieldKey.fromString(super.getParentFieldKey(), "Id"));
            if(null == peptideId)
            {
                return null;
            }

            Object sequence = ctx.get(FieldKey.fromString(super.getParentFieldKey(), "Sequence"));
            if(sequence == null)
            {
               return null;
            }

            Object runId = ctx.get(FieldKey.fromString(super.getParentFieldKey(), "PeptideGroupId/RunId"));
            if(runId == null)
            {
                return null;
            }

            String peptideModifiedSequence = (String)getValue(ctx);

            String peptideHtml = getHtmlMaker().getPeptideHtml((Integer) peptideId, (String) sequence, peptideModifiedSequence, (Integer)runId);

             getLinkUrl().replaceParameter("id", String.valueOf(peptideId));

            String title = "Peptide Details";
            String iconPath = IconFactory.getPeptideIconPath((Integer)peptideId, (Integer)runId);
            String imgHtml = super.makeImageHtml(iconPath, title);

            return super.combineHtml(peptideHtml, imgHtml);
        }
    }

    public static class PrecursorCol extends ModifiedSequenceDisplayColumn
    {
        public PrecursorCol(ColumnInfo colInfo, ActionURL url)
        {
            super(colInfo, url);
        }

        @Override
        public void addQueryFieldKeys(Set<FieldKey> keys)
        {
            super.addQueryFieldKeys(keys);
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "Id"));
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "PeptideId"));
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "PeptideId/Sequence"));
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "IsotopeLabelId"));
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "PeptideId/PeptideGroupId/RunId"));
        }

        @Override
        public String getModifiedSequenceHtml(RenderContext ctx)
        {
            Object precursorId = ctx.get(FieldKey.fromString(super.getParentFieldKey(), "Id"));
            if(precursorId == null)
            {
               return null;
            }

            Object peptideId = ctx.get(FieldKey.fromString(super.getParentFieldKey(), "PeptideId"));
            if(peptideId == null)
            {
                return null;
            }

            Object sequence = ctx.get(FieldKey.fromString(super.getParentFieldKey(), "PeptideId/Sequence"));
            if(sequence == null)
            {
                return null;
            }

            Object isotopeLabelId = ctx.get(FieldKey.fromString(super.getParentFieldKey(), "IsotopeLabelId"));
            if(isotopeLabelId == null)
            {
                return null;
            }

            Object runId = ctx.get(FieldKey.fromString(super.getParentFieldKey(), "PeptideId/PeptideGroupId/RunId"));
            if(runId == null)
            {
                return null;
            }

            String precursorModifiedSequence = (String)getValue(ctx);

            String precursorHtml = getHtmlMaker().getPrecursorHtml((Integer)peptideId,
                                                                  (Integer)isotopeLabelId,
                                                                  (String)sequence,
                                                                  precursorModifiedSequence,
                                                                  (Integer)runId);

             getLinkUrl().replaceParameter("id", String.valueOf(precursorId));

            String iconPath = IconFactory.getPrecursorIconPath((Integer)precursorId, (Integer)runId);
            String title = "Precursor Details";

            String imgHtml = super.makeImageHtml(iconPath, title);

            return super.combineHtml(precursorHtml, imgHtml);
        }
    }
}
