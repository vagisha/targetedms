package org.labkey.targetedms.parser.skyaudit;


import org.labkey.api.util.GUID;

public class TestRun{
    public int _id;
    public GUID _container;
    public GUID _entityId = new GUID();
    public GUID _documentGUID;

    public int getId() {return _id;}
    public void setId(int pId){_id = pId;}
    public GUID getContainer() {return _container;}
    public GUID getEntityId() {return _entityId;}
    public GUID getDocumentGUID() {return _documentGUID;}
}
