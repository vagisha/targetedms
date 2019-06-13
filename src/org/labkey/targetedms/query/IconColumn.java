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
package org.labkey.targetedms.query;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.targetedms.view.IconFactory;

import java.util.Set;

/**
 * Created by vsharma on 9/30/2016.
 */
public abstract class IconColumn extends DataColumn
{
     private final FieldKey _parentFieldKey;

    public IconColumn(@NotNull ColumnInfo colInfo)
    {
        super(colInfo);

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

    private String getIconHtml(String iconPath)
    {
        if(StringUtils.isBlank(iconPath))
        {
            return "";
        }

        StringBuilder imgHtml = new StringBuilder("<img src=\"");
        imgHtml.append(PageFlowUtil.filter(iconPath)).append("\"").append(" title=\"" + PageFlowUtil.filter(getLinkTitle()) +"\"").append(" width=\"16\" height=\"16\" style=\"margin-right: 5px;\"/>");
        return imgHtml.toString();
    }

    @Override
    public @NotNull String getFormattedValue(RenderContext ctx)
    {
        String iconHtml = getIconHtml(getIconPath());
        String cellDataHtml = getCellDataHtml(ctx);
        return "<nobr>" + iconHtml + (cellDataHtml == null ? "" : cellDataHtml) + "</nobr>";
    }

    @Override
    public @NotNull String getCssStyle(RenderContext ctx)
    {
        if(removeLinkDefaultColor())
        {
            return "color: #000000";
        }

        return super.getCssStyle(ctx);
    }

    public static class MoleculeDisplayCol extends IconColumn
    {
        public MoleculeDisplayCol(ColumnInfo colInfo)
        {
            super(colInfo);
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
            return PageFlowUtil.filter(ctx.get(getColumnInfo().getFieldKey(), String.class));
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
