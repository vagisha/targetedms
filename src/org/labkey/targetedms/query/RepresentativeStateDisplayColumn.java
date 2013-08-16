package org.labkey.targetedms.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.targetedms.parser.RepresentativeDataState;

import java.io.IOException;
import java.io.Writer;

/**
 * User: vsharma
 * Date: 8/5/13
 * Time: 8:32 PM
 */
public class RepresentativeStateDisplayColumn extends DataColumn
{
    public RepresentativeStateDisplayColumn (ColumnInfo columnInfo)
    {
        super(columnInfo);
    }

    @Override
    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
    {
        Object representativeState = getValue(ctx);
        if (representativeState == null)
            return;

        if (RepresentativeDataState.Representative.getLabel().equals(representativeState.toString()))
        {
            out.write("<span style='color:green;'>" + representativeState.toString() + "</span>");
        }
        else if (RepresentativeDataState.Conflicted.getLabel().equals(representativeState.toString()))
        {
            out.write("<span style='color:red;'>" + representativeState.toString() + "</span>");
        }
        else
        {
            out.write(representativeState.toString());
        }
    }
}
