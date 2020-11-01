package org.labkey.targetedms.chromlib;

public abstract class AbstractLibEntity
{
    private int _id;

    public int getId()
    {
        return _id;
    }

    public void setId(int id)
    {
        _id = id;
    }

    public int getCacheSize()
    {
        return 1;
    }
}
