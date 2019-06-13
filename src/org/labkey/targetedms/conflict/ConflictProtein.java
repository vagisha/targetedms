/*
 * Copyright (c) 2012-2019 LabKey Corporation
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
package org.labkey.targetedms.conflict;

/**
 * User: vsharma
 * Date: 9/20/12
 * Time: 2:24 PM
 */
public class ConflictProtein
{
    private int _newProteinId;
    private int _newProteinRunId;
    private String _newRunFile;
    private String _newProteinLabel;
    private int _oldProteinId;
    private int _oldProteinRunId;
    private String _oldRunFile;
    private String _oldProteinLabel;

    public ConflictProtein() {}

    public int getNewProteinId()
    {
        return _newProteinId;
    }

    public void setNewProteinId(int newProteinId)
    {
        _newProteinId = newProteinId;
    }

    public int getNewProteinRunId()
    {
        return _newProteinRunId;
    }

    public void setNewProteinRunId(int newProteinRunId)
    {
        _newProteinRunId = newProteinRunId;
    }

    public String getNewProteinLabel()
    {
        return _newProteinLabel;
    }

    public void setNewProteinLabel(String newProteinLabel)
    {
        _newProteinLabel = newProteinLabel;
    }

    public int getOldProteinId()
    {
        return _oldProteinId;
    }

    public void setOldProteinId(int oldProteinId)
    {
        _oldProteinId = oldProteinId;
    }

    public int getOldProteinRunId()
    {
        return _oldProteinRunId;
    }

    public String getNewRunFile()
    {
        return _newRunFile;
    }

    public void setNewRunFile(String newRunFile)
    {
        _newRunFile = newRunFile;
    }

    public String getOldRunFile()
    {
        return _oldRunFile;
    }

    public void setOldRunFile(String oldRunFile)
    {
        _oldRunFile = oldRunFile;
    }

    public void setOldProteinRunId(int oldProteinRunId)
    {
        _oldProteinRunId = oldProteinRunId;
    }

    public String getOldProteinLabel()
    {
        return _oldProteinLabel;
    }

    public void setOldProteinLabel(String oldProteinLabel)
    {
        _oldProteinLabel = oldProteinLabel;
    }
}
