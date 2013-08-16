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

    public ModifiedPeptideDisplayColumn(ColumnInfo colInfo, ActionURL url, boolean isPeptide)
    {
        super(colInfo);
        _idColumnFieldKey = new FieldKey(colInfo.getFieldKey().getParent(), "Id");

        _linkUrl = url;
        _isPeptide = isPeptide;

        if(isPeptide)
        {
            setDescription("Modified Peptide Sequence");
        }
        else
        {
            setDescription("Modified Precursor Sequence");
        }
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
        String html = _isPeptide ? _htmlMaker.getHtml(PeptideManager.get(id)) : _htmlMaker.getHtml(PrecursorManager.get(id));

        if(_linkUrl != null)
        {
            _linkUrl.replaceParameter("id", String.valueOf(id));
            html = "<a href=\""+_linkUrl.getLocalURIString()+"\" style=\"color: #000000\">" + html + "</a>";
        }
        return html;
    }
}
