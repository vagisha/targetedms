/*
 * Copyright (c) 2019 LabKey Corporation
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
package org.labkey.targetedms.parser.skyaudit;

import org.labkey.api.util.GUID;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AuditLogTree implements Iterable<AuditLogTree>
{
    public static final String NULL_STRING = "(null)";

    private Map<String, AuditLogTree> _children = new HashMap<>();
    private String _entryHash;
    private GUID _documentGUID;
    private String _parentEntryHash;
    private Integer _entryId;

    public AuditLogTree(Integer pEntryid, GUID pDocumentGUID, String pEntryHash, String pParentEntryHash){
        _entryHash = pEntryHash;
        _documentGUID = pDocumentGUID;
        _parentEntryHash = pParentEntryHash;
        _entryId = pEntryid;
    }

    public String getEntryHash()
    {
        return _entryHash;
    }

    public GUID getDocumentGUID()
    {
        return _documentGUID;
    }

    public String getParentEntryHash()
    {
        return _parentEntryHash;
    }

    public Integer getEntryId()
    {
        return _entryId;
    }

    public AuditLogTree addChild(AuditLogTree pChild)
    {
        if(!_children.containsKey(pChild._entryHash))
            _children.put(pChild.getEntryHash(), pChild);
        return pChild;
    }

    public int addChildren(Iterable<AuditLogTree> pChildren)
    {
        int i = 0;
        for(AuditLogTree c : pChildren)
        {
            addChild(c);
            i++;
        }
        return i;
    }

    public boolean hasChild(AuditLogTree pEntry)
    {
        return _children.containsKey(pEntry._entryHash);
    }
    public boolean hasChild(String pEntryHash){
        return _children.containsKey(pEntryHash);
    }
    public AuditLogTree getChild(String pEntryHash) {
        return _children.getOrDefault(pEntryHash, null);
    }

    public int getTreeSize(){
        return getTreeSizeRecursive(0) + 1;
    }

    private int getTreeSizeRecursive(int pSize){
        int s = 0;
        for(AuditLogTree t : this)
            s += t.getTreeSizeRecursive(pSize);
        return pSize + _children.size() + s;
    }

    @Override
    public Iterator<AuditLogTree> iterator()
    {
        return _children.values().iterator();
    }

    @Override
    public int hashCode(){
        if(this._entryHash != null)
            return this._entryHash.hashCode();
        else
            return super.hashCode();
    }

    @Override
    public boolean equals(Object o){
        if( o instanceof AuditLogTree)
        {
            if(this._entryHash == null) return false;
            return (this._entryHash.equals( ((AuditLogTree) o)._entryHash) );
        }
        else
            return false;
    }

}
