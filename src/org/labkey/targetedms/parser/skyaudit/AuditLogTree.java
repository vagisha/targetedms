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

public class AuditLogTree implements Iterable<AuditLogTree>
{
    public static final String NULL_STRING = "(null)";

    private HashMap<String, AuditLogTree> _children = new HashMap<String, AuditLogTree>();
    private String _entryHash;
    private GUID _documentGUID;
    private String _parentEntryHash;
    private Integer _entryId;

    public AuditLogTree(Integer p_entryId, GUID p_documentGUID, String p_entryHash, String p_parentEntryHash){
        _entryHash = p_entryHash;
        _documentGUID = p_documentGUID;
        _parentEntryHash = p_parentEntryHash;
        _entryId = p_entryId;
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

    public AuditLogTree addChild(AuditLogTree p_child)
    {
        if(!_children.containsKey(p_child._entryHash))
            _children.put(p_child.getEntryHash(), p_child);
        return p_child;
    }

    public int addChildren(Iterable<AuditLogTree> p_children)
    {
        int i = 0;
        for(AuditLogTree c : p_children)
        {
            addChild(c);
            i++;
        }
        return i;
    }

    public boolean hasChild(AuditLogTree p_entry)
    {
        return _children.containsKey(p_entry._entryHash);
    }
    public boolean hasChild(String p_entryHash){
        return _children.containsKey(p_entryHash);
    }
    public AuditLogTree getChild(String p_entryHash) {
        if(_children.containsKey(p_entryHash))
            return _children.get(p_entryHash);
        else
            return null;
    }

    public int getTreeSize(){
        int result = 1;     //to count the root.
        return getTreeSizeRecursive(0) + 1;
    }

    private int getTreeSizeRecursive(int p_size){
        int s = 0;
        for(AuditLogTree t : this)
            s += t.getTreeSizeRecursive(p_size);
        return p_size + _children.size() + s;
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
            return (this._entryHash == ((AuditLogTree)o)._entryHash);
        else
            return false;
    }

}
