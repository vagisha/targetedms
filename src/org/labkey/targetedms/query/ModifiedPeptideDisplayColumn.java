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
import org.labkey.api.view.ActionURL;
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
    private ModifiedPeptideHtmlMaker _htmlMaker = new ModifiedPeptideHtmlMaker();

    private final ColumnInfo _precursorIdCol;

    public ModifiedPeptideDisplayColumn(ColumnInfo colInfo, ColumnInfo precursorIdCol, ActionURL url)
    {
        super(colInfo);

        _precursorIdCol = precursorIdCol;

        _linkUrl = url;

        setCaption("Precursor");
        setDescription("Modified Peptide");
        setTextAlign("left");
    }

    @Override
    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
    {
        Object id = _precursorIdCol.getValue(ctx);  // Primary key from the Precursor table
        if(null == id)
            return;

        String html = getPeptideHtml((Integer) id);
        out.write(html);
    }

    private String getPeptideHtml(int precursorId)
    {
        String html = _htmlMaker.getHtml(PrecursorManager.get(precursorId));

        if(_linkUrl != null)
        {
            _linkUrl.replaceParameter("id", String.valueOf(precursorId));
            html = "<a href=\""+_linkUrl.getLocalURIString()+"\" style=\"color: #000000\">" + html + "</a>";
        }
        return html;
    }
}
