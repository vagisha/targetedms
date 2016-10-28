/*
 * Copyright (c) 2016 LabKey Corporation
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
package org.labkey.targetedms.query;

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.targetedms.view.IconFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

/**
 * Created by vsharma on 9/30/2016.
 */
public abstract class IconColumn extends DataColumn
{
    private final ActionURL _linkUrl;
    private final FieldKey _parentFieldKey;

    public IconColumn(ColumnInfo colInfo, ActionURL url)
    {
        super(colInfo);
        _linkUrl = url;
        _parentFieldKey = colInfo.getFieldKey().getParent();
        setTextAlign("left");
    }

    FieldKey getParentFieldKey()
    {
        return _parentFieldKey;
    }

    abstract String getIconPath();

    abstract String getLinkTitle();

    abstract String getCellDataHtml(RenderContext ctx);

    boolean removeLinkDefaultColor()
    {
        return true;
    }

    private static String getIconHtml(String iconPath)
    {
        if(StringUtils.isBlank(iconPath))
        {
            return "";
        }

        StringBuilder imgHtml = new StringBuilder("<img src=\"");
        imgHtml.append(PageFlowUtil.filter(iconPath)).append("\"").append(" width=\"16\" height=\"16\" style=\"margin-right: 5px;\"/>");
        return imgHtml.toString();
    }

    @Override
    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
    {
        String iconHtml = getIconHtml(getIconPath());
        String cellDataHtml = getCellDataHtml(ctx);

        if(StringUtils.isBlank(cellDataHtml) && StringUtils.isBlank(iconHtml))
        {
            super.renderGridCellContents(ctx, out);
            return;
        }

        if(_linkUrl != null)
        {
            Integer id = (Integer) ctx.get(FieldKey.fromString(getParentFieldKey(), "Id"));
            if(id != null)
            {
                _linkUrl.replaceParameter("id", String.valueOf(id));
                String linkStartTag = getLinkStartTag(_linkUrl.getLocalURIString(), getLinkTitle(), removeLinkDefaultColor());
                cellDataHtml = linkStartTag + cellDataHtml + "</a>";
                iconHtml = linkStartTag + iconHtml + "</a>";
            }
        }
        out.write("<nobr>"
                + iconHtml
                + cellDataHtml
                + "</nobr>");
    }

    private static String getLinkStartTag(String linkUrl, String title, boolean removeLinkDefaultColor)
    {
        StringBuilder aTag = new StringBuilder("<a href=\"" + linkUrl + "\" ");
        aTag.append(" title=\"").append(PageFlowUtil.filter(title)).append("\"");
        if (removeLinkDefaultColor) aTag.append(" style=\"color: #000000\"");
        aTag.append(">");
        return aTag.toString();
    }

    public static class MoleculeDisplayCol extends IconColumn
    {
        public MoleculeDisplayCol(ColumnInfo colInfo, ActionURL url)
        {
            super(colInfo, url);
        }

        @Override
        String getIconPath()
        {
            return IconFactory.getMoleculeIconPath();
        }

        @Override
        String getLinkTitle()
        {
            return "Molecule Details";
        }

        @Override
        String getCellDataHtml(RenderContext ctx)
        {
            return (String)getValue(ctx);
        }

        boolean removeLinkDefaultColor()
        {
            return false;
        }

        @Override
        public void addQueryFieldKeys(Set<FieldKey> keys)
        {
            super.addQueryFieldKeys(keys);
            keys.add(FieldKey.fromString(super.getParentFieldKey(), "Id"));
        }
    }
}
