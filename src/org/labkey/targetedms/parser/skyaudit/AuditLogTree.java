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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.util.GUID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class AuditLogTree implements Iterable<AuditLogTree>
{
    public static final String NULL_STRING = "(null)";

    private Map<String, AuditLogTree> _children = new HashMap<>();
    private String _entryHash;
    private GUID _documentGUID;
    private String _parentEntryHash;
    private int _entryId;
    private Integer _versionId;     //runId of the document if this record is the last in that document's log. Null otherwise.

    public AuditLogTree(int pEntryid, GUID pDocumentGUID, String pEntryHash, String pParentEntryHash, Integer pVersionId){
        _entryHash = pEntryHash;
        _documentGUID = pDocumentGUID;
        _parentEntryHash = pParentEntryHash;
        _entryId = pEntryid;
        _versionId = pVersionId;
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

    public int getEntryId()
    {
        return _entryId;
    }

    public Integer getVersionId()
    {
        return _versionId;
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

    /***
     * Finds log entries that belong to the given document version only and not to any other version.
     * This is useful when we need to delete the version and it's log, but some of the entries can be
     * shared with other versions
     * @param versionId  runId of the document version to be deleted
     * @return list of entries that can be deleted safely without corrupting other versions' logs.
     */
    public List<AuditLogTree> deleteList(int versionId){
        List<AuditLogTree> result = new ArrayList<>();
        recursiveDeleteList(versionId, result);
        return result;
    }

    /***
     * Finds log entries that belong to the given document version only and not to any other version.
     * The found entries are removed from the tree and moved to the deletedEntries list.
     * @param versionId run ID of the document being deleted
     * @param deleteEntries list of elements to be physically deleted.
     * @return versionId if this child should be deleted, null otherwise.
     */
    @Nullable
    private Integer recursiveDeleteList(int versionId, List<AuditLogTree> deleteEntries)
    {
        if (_children.size() == 0)      //if this is a leaf
        {
            if (this._versionId == null)
            {
                if(this._entryId != 0)                  //No need to check for the root node.
                {
                    Logger.getLogger(this.getClass().getCanonicalName()).warning(
                            String.format("Audit log entry with ID %d is a leaf but has no version ID. This might be a data corruption.", this._entryId));
                }
                return null;
            }
            else if (versionId == this._versionId)      //check if it is the right version id
                return versionId;
            else
                return null;
        }
        else
        {
            for (Map.Entry<String, AuditLogTree> child : _children.entrySet())
            {       //performing depth-first search because audit log trees are typically deeper than wider.
                if (child.getValue().recursiveDeleteList(versionId, deleteEntries) != null)
                {       //if the child is from the right branch add it to the deletion list and remove from the tree.
                    deleteEntries.add(child.getValue());
                    _children.remove(child.getKey());
                    if (_children.size() == 0 && _versionId == null)      //if there are no other children and the entry does not belong to another version continue down the branch
                        return versionId;
                    else
                        return null;    //otherwise stop because this entry belongs to more than one version.
                }
            }
            return null;    //We didn't find the versionId in any of the children.
        }
    }

    public AuditLogTree findVersionEntry(int pVersionId){
        return recursiveFindVersionEntry(pVersionId);
    }

    private AuditLogTree recursiveFindVersionEntry(int pVersionId){
        if (this._versionId != null && this._versionId == pVersionId)      //check if it is the right version id
            return this;
        else
        {
            for (Map.Entry<String, AuditLogTree> child : _children.entrySet())
            {       //performing depth-first search because audit log trees are typically deeper than wider.
                AuditLogTree res = child.getValue().recursiveFindVersionEntry(pVersionId);
                if(res != null) return res;
            }
            return null;
        }
    }

    @Override
    @NotNull
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
