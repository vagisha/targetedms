package org.labkey.targetedms.parser;

/**
 * User: vsharma
 * Date: 11/26/12
 * Time: 7:23 PM
 */
public class AnnotationSetting extends SkylineEntity
{
    private int _runId;
    private String _name;
    private String _type;
    private String _targets;

    public int getRunId()
    {
        return _runId;
    }

    public void setRunId(int runId)
    {
        _runId = runId;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public String getType()
    {
        return _type;
    }

    public void setType(String type)
    {
        _type = type;
    }

    public String getTargets()
    {
        return _targets;
    }

    public void setTargets(String targets)
    {
        _targets = targets;
    }
}
