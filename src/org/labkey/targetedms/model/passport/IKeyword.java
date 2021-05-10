package org.labkey.targetedms.model.passport;

public class IKeyword
{
    public static final String BIOLOGICAL_PROCESS_CATEGORY = "KW-9999";
    public static final String MOLECULAR_FUNCTION_CATEGORY = "KW-9992";

    public final String id;
    public final String categoryId;
    public final String label;
    public final String category;

    public IKeyword(String id, String categoryId, String label, String category)
    {
        this.id = id;
        this.categoryId = categoryId;
        this.label = label;
        this.category = category;
    }
}
