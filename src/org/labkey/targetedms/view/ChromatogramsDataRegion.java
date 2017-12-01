/*
 * Copyright (c) 2012-2017 LabKey Corporation
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

package org.labkey.targetedms.view;

import org.labkey.api.collections.ResultSetRowMapFactory;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DetailsColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.MenuButton;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.Results;
import org.labkey.api.data.ShowRows;
import org.labkey.api.data.UpdateColumn;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.query.ChromatogramGridQuerySettings;

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * User: vsharma
 * Date: 4/28/12
 * Time: 5:40 PM
 */
public class ChromatogramsDataRegion extends DataRegion
{
    public static final String PRECURSOR_CHROM_DATA_REGION = "PrecursorChromatograms";
    public static final String PEPTIDE_CHROM_DATA_REGION = "PeptideChromatograms";

    public ChromatogramsDataRegion(ViewContext context, FilteredTable tableInfo, String name)
    {
        setTable(tableInfo);
        addColumns(tableInfo, "Id");

        ChromatogramGridQuerySettings settings = new ChromatogramGridQuerySettings(context, name);
        setSettings(settings);

        populateButtonBar();

        setShadeAlternatingRows(false);
    }

    protected void populateButtonBar()
    {
        ButtonBar bar = new ButtonBar();
        bar.add(createPageSizeMenuButton());
        bar.add(createRowSizeMenuButton());
        setButtonBar(bar);
    }

    protected int renderTableContents(RenderContext ctx, Writer out, boolean showRecordSelectors, List<DisplayColumn> renderers) throws SQLException, IOException
    {
        Results results = ctx.getResults();

        // unwrap for efficient use of ResultSetRowMapFactory
        ResultSet rs = results.getResultSet();
        ResultSetRowMapFactory factory = ResultSetRowMapFactory.create(rs);
        int rowIndex = 0;

        int maxRowSize = getSettings().getMaxRowSize();
        int count = 0;
        assert rs != null;

        while (rs.next())
        {
            if(count == 0)
            {
                out.write("<tr");
                String rowClass = getRowClass(ctx, rowIndex);
                if (rowClass != null)
                    out.write(" class=\"" + rowClass + "\"");
                out.write(">");
                out.write("<td><table cellpadding=\"0\" cellspacing=\"0\"><tr>");
            }
            ctx.setRow(factory.getRowMap(rs));
            renderTableRow(ctx, out, showRecordSelectors, renderers, rowIndex++);
            count++;
            if(count == maxRowSize)
            {
                out.write("</tr></table></td>");
                out.write("</tr>\n");
                count = 0;
            }
        }
        if(count != 0)
        {
            while(count < maxRowSize)
            {
                out.write("<td style=\"border:0;\"></td>");
                count++;
            }
            out.write("</tr></table>");
            out.write("</tr>\n");
        }

        rs.close();
        return rowIndex;
    }

    protected void renderTableRow(RenderContext ctx, Writer out, boolean showRecordSelectors, List<DisplayColumn> renderers, int rowIndex) throws SQLException, IOException
    {
        DisplayColumn detailsColumn = getDetailsUpdateColumn(ctx, renderers, true);
        DisplayColumn updateColumn = getDetailsUpdateColumn(ctx, renderers, false);

        if (showRecordSelectors || (detailsColumn != null || updateColumn != null))
            renderActionColumn(ctx, out, rowIndex, showRecordSelectors, detailsColumn, updateColumn);

        for (DisplayColumn renderer : renderers)
            if (renderer.isVisible(ctx))
            {
                if (renderer instanceof DetailsColumn || renderer instanceof UpdateColumn)
                    continue;

                renderer.renderGridDataCell(ctx, out);
            }

    }

    public ChromatogramGridQuerySettings getSettings()
    {
        return (ChromatogramGridQuerySettings) super.getSettings();
    }

    protected MenuButton createRowSizeMenuButton()
    {
        int maxRowSize = getSettings().getMaxRowSize();

        MenuButton pageSizeMenu = new MenuButton("Row Size", getName() + ".Menu.RowSize");

        // insert current maxRows into sorted list of possible sizes
        List<Integer> sizes = new LinkedList<>(Arrays.asList(1, 2, 3, 4, 5, 10));
        if (maxRowSize > 0)
        {
            int index = Collections.binarySearch(sizes, maxRowSize);
            if (index < 0)
            {
                sizes.add(-index-1, maxRowSize);
            }
        }

        URLHelper target = getSettings().getSortFilterURL();
        target.deleteParameter("maxRowSize");

        for (Integer rowSize : sizes)
        {
            URLHelper url = target.clone();
            url.replaceParameter("maxRowSize", String.valueOf(rowSize));

            boolean checked = rowSize == maxRowSize;
            NavTree item = pageSizeMenu.addMenuItem(String.valueOf(rowSize) + " per row",
                                                    url.toString(), null, checked);
            item.setId("Row Size:" + rowSize);
        }

        return pageSizeMenu;
    }

    protected MenuButton createPageSizeMenuButton()
    {
        final int maxRows = getMaxRows();

        MenuButton pageSizeMenu = new MenuButton("Paging", getName() + ".Menu.PageSize")
        {
            final boolean showingAll = getShowRows() == ShowRows.ALL;

            @Override
            public void render(RenderContext ctx, Writer out) throws IOException
            {
                addSeparator();

                NavTree item = addMenuItem("Show All", null, getJavaScriptObjectReference() + ".showAll();", showingAll);
                item.setId("Page Size:All");

                super.render(ctx, out);
            }
        };

        // insert current maxRows into sorted list of possible sizes
        List<Integer> sizes = new LinkedList<>(Arrays.asList(10, 20, 50, 100));
        if (maxRows > 0)
        {
            int index = Collections.binarySearch(sizes, maxRows);
            if (index < 0)
            {
                sizes.add(-index-1, maxRows);
            }
        }

        for (Integer pageSize : sizes)
        {
            boolean checked = pageSize == maxRows;
            NavTree item = pageSizeMenu.addMenuItem(String.valueOf(pageSize) + " per page", null,
                    getJavaScriptObjectReference() + ".setMaxRows(" + String.valueOf(pageSize) + ");", checked);
            item.setId("Page Size:" + pageSize);
        }

        return pageSizeMenu;
    }
}
