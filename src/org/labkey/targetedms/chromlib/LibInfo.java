/*
 * Copyright (c) 2013-2019 LabKey Corporation
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
package org.labkey.targetedms.chromlib;

import java.util.Date;

/**
 * User: vsharma
 * Date: 12/26/12
 * Time: 3:48 PM
 */
public class LibInfo
{
    private String _panoramaServer;
    private String _container;
    private Date _created;
    private String _schemaVersion;
    private int _libraryRevision;
    private int _proteins;
    private int _peptides;
    private int _precursors;
    private int _transitions;

    public String getPanoramaServer()
    {
        return _panoramaServer;
    }

    public void setPanoramaServer(String panoramaServer)
    {
        _panoramaServer = panoramaServer;
    }

    public String getContainer()
    {
        return _container;
    }

    public void setContainer(String container)
    {
        _container = container;
    }

    public Date getCreated()
    {
        return _created;
    }

    public void setCreated(Date created)
    {
        _created = created;
    }

    public String getSchemaVersion()
    {
        return _schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion)
    {
        _schemaVersion = schemaVersion;
    }

    public int getLibraryRevision()
    {
        return _libraryRevision;
    }

    public void setLibraryRevision(int libraryRevision)
    {
        _libraryRevision = libraryRevision;
    }

    public int getProteins()
    {
        return _proteins;
    }

    public void setProteins(int proteins)
    {
        _proteins = proteins;
    }

    public int getPeptides()
    {
        return _peptides;
    }

    public void setPeptides(int peptides)
    {
        _peptides = peptides;
    }

    public int getPrecursors()
    {
        return _precursors;
    }

    public void setPrecursors(int precursors)
    {
        _precursors = precursors;
    }

    public int getTransitions()
    {
        return _transitions;
    }

    public void setTransitions(int transitions)
    {
        _transitions = transitions;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LibInfo libInfo = (LibInfo) o;

        if (_libraryRevision != libInfo._libraryRevision) return false;
        if (_peptides != libInfo._peptides) return false;
        if (_precursors != libInfo._precursors) return false;
        if (_proteins != libInfo._proteins) return false;
        if (_transitions != libInfo._transitions) return false;
        if (_container != null ? !_container.equals(libInfo._container) : libInfo._container != null) return false;
        if (_created != null ? !_created.equals(libInfo._created) : libInfo._created != null) return false;
        if (_panoramaServer != null ? !_panoramaServer.equals(libInfo._panoramaServer) : libInfo._panoramaServer != null)
            return false;
        if (_schemaVersion != null ? !_schemaVersion.equals(libInfo._schemaVersion) : libInfo._schemaVersion != null)
            return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = _panoramaServer != null ? _panoramaServer.hashCode() : 0;
        result = 31 * result + (_container != null ? _container.hashCode() : 0);
        result = 31 * result + (_created != null ? _created.hashCode() : 0);
        result = 31 * result + (_schemaVersion != null ? _schemaVersion.hashCode() : 0);
        result = 31 * result + _libraryRevision;
        result = 31 * result + _proteins;
        result = 31 * result + _peptides;
        result = 31 * result + _precursors;
        result = 31 * result + _transitions;
        return result;
    }
}
