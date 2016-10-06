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

    abstract String getIconTitle();

    abstract String getCellDataHtml(RenderContext ctx);

    boolean removeLinkDefaultColor()
    {
        return true;
    }

    private String makeIconHtml(String iconPath, String title)
    {
        if(StringUtils.isBlank(iconPath))
        {
            return "";
        }

        if(title == null) title = "";
        StringBuilder imgHtml = new StringBuilder();
        imgHtml.append("<a href=\"").append(_linkUrl.getLocalURIString()).append("\" title=\"").append(PageFlowUtil.filter(title)).append("\">");
        imgHtml.append("<img src=\"").append(PageFlowUtil.filter(iconPath)).append("\"").append(" width=\"16\" height=\"16\" style=\"margin-right: 5px;\"/>");
        imgHtml.append("</a>");
        return imgHtml.toString();
    }

    @Override
    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
    {
        String iconHtml = makeIconHtml(getIconPath(), getIconTitle());
        String cellDataHtml = getCellDataHtml(ctx);

        if(cellDataHtml == null && iconHtml == null)
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
            }
            StringBuilder wLink = new StringBuilder("<a href=\""+_linkUrl.getLocalURIString()+ "\"");
            if(removeLinkDefaultColor()) wLink.append(" style=\"color: #000000\"");
            wLink.append(">" + cellDataHtml + "</a>");

            cellDataHtml = wLink.toString();
        }
        out.write("<nobr>"
                + iconHtml
                + cellDataHtml
                + "</nobr>");
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
        String getIconTitle()
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
