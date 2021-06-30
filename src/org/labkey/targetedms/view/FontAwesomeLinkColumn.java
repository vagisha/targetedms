package org.labkey.targetedms.view;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.PageFlowUtil;

public class FontAwesomeLinkColumn extends DataColumn
{
    private final String _icon;
    private final String _tooltip;

    public FontAwesomeLinkColumn(ColumnInfo col, String icon, String tooltip)
    {
        super(col);
        _icon = icon;
        _tooltip = tooltip;
    }

    @Override
    public @NotNull HtmlString getFormattedHtml(RenderContext ctx)
    {
        return HtmlString.unsafe("<i class=\"fa " + _icon + "\" title=\"" + PageFlowUtil.filter(_tooltip) + "\"></i>");
    }

    @Override
    public String getTitle(RenderContext ctx)
    {
        return null;
    }

    @Override
    public boolean isSortable()
    {
        return false;
    }

    @Override
    public boolean isFilterable()
    {
        return false;
    }
}
