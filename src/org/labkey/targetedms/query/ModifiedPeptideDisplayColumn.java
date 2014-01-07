/*
 * Copyright (c) 2012-2013 LabKey Corporation
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


/**
 * User: vsharma
 * Date: 4/23/12
 * Time: 2:34 PM
 */
public class ModifiedPeptideDisplayColumn extends DataColumn
{
    private ActionURL _linkUrl;
    private final boolean _isPeptide;
    private ModifiedPeptideHtmlMaker _htmlMaker = new ModifiedPeptideHtmlMaker();

    private final FieldKey _idColumnFieldKey;

    public static final String PEPTIDE_COLUMN_NAME = "ModifiedPeptideDisplayColumn";
    public static final String PRECURSOR_COLUMN_NAME = "ModifiedPrecursorDisplayColumn";

    public ModifiedPeptideDisplayColumn(ColumnInfo colInfo, ActionURL url, boolean isPeptide)
    {
        super(colInfo);
        _idColumnFieldKey = colInfo.getFieldKey();

        _linkUrl = url;
        _isPeptide = isPeptide;

        setTextAlign("left");
    }

    @Override
    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
    {
        Object id = ctx.get(_idColumnFieldKey); // Primary key from the Peptide or Precursor table
        if(null == id)
            return;

        String html = getPeptideHtml((Integer) id);
        out.write(html);
    }

    private String getPeptideHtml(int id)
    {
        String peptideHtml = _isPeptide ? _htmlMaker.getHtml(PeptideManager.get(id)) : _htmlMaker.getHtml(PrecursorManager.get(id));

         _linkUrl.replaceParameter("id", String.valueOf(id));

        String iconPath = "";
        String title = "";
        if(_isPeptide)
        {
            title = "Peptide Details";
            iconPath = IconFactory.getPeptideIconPath(id);
        }
        else
        {
            title = "Precursor Details";
            iconPath = IconFactory.getPrecursorIconPath(id);
        }
        StringBuilder imgHtml = new StringBuilder();
        imgHtml.append("<a href=\"").append(_linkUrl.getLocalURIString()).append("\" title=\"").append(title).append("\">");
        imgHtml.append("<img src=\"").append(iconPath).append("\"").append(" width=\"18\" height=\"16\" style=\"margin-right: 5px;\"/>");
        imgHtml.append("</a>");


        return imgHtml.toString() + "<a href=\""+_linkUrl.getLocalURIString()+"\" style=\"color: #000000\">" + peptideHtml + "</a>";
    }
}
