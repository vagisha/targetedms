/*
 * Copyright (c) 2015-2019 LabKey Corporation
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
package org.labkey.targetedms;

import org.labkey.api.data.Container;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.ExperimentRunType;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.targetedms.IModification;
import org.labkey.api.targetedms.ITargetedMSRun;
import org.labkey.api.targetedms.SkylineDocumentImportListener;
import org.labkey.api.targetedms.SkylineAnnotation;
import org.labkey.api.targetedms.TargetedMSFolderTypeListener;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.targetedms.model.SampleFileInfo;
import org.labkey.targetedms.query.ModificationManager;
import org.labkey.targetedms.query.ReplicateManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * User: vsharma
 * Date: 8/26/2015
 * Time: 2:31 PM
 */
public class TargetedMSServiceImpl implements TargetedMSService
{
    private List<SkylineDocumentImportListener> _skylineDocumentImportListeners = new ArrayList<>();
    private List<TargetedMSFolderTypeListener> _targetedMsFolderTypeListeners = new ArrayList<>();

    @Override
    public ITargetedMSRun getRun(int runId, Container container)
    {
        TargetedMSRun run = TargetedMSManager.getRun(runId);
        if(run != null && run.getContainer().equals(container))
        {
            return run;
        }
        return null;
    }

    @Override
    public ITargetedMSRun getRunByFileName(String fileName, Container container)
    {
        return TargetedMSManager.getRunByFileName(fileName, container);
    }

    @Override
    public List<ITargetedMSRun> getRuns(Container container)
    {
        return Arrays.asList(TargetedMSManager.getRunsInContainer(container));
    }

    @Override
    public List<? extends SkylineAnnotation> getReplicateAnnotations(Container container)
    {
        return ReplicateManager.getReplicateAnnotationNameValues(container);
    }

    @Override
    public void registerSkylineDocumentImportListener(SkylineDocumentImportListener listener)
    {
        _skylineDocumentImportListeners.add(listener);
    }

    @Override
    public List<SkylineDocumentImportListener> getSkylineDocumentImportListener()
    {
        return _skylineDocumentImportListeners;
    }

    @Override
    public Map<String, SampleFileInfo> getSampleFiles(Container container, User user, Integer sampleFileLimit)
    {
        return TargetedMSManager.get().getSampleFiles(container, user, sampleFileLimit);
    }

    @Override
    public FolderType getFolderType(Container container)
    {
        return TargetedMSManager.getFolderType(container);
    }

    @Override
    public TableInfo getTableInfoRuns()
    {
        return TargetedMSManager.getTableInfoRuns();
    }

    @Override
    public TableInfo getTableInfoPeptideGroup()
    {
        return TargetedMSManager.getTableInfoPeptideGroup();
    }

    @Override
    public List<String> getSampleFilePaths(int runId)
    {
        return ReplicateManager.getSampleFilePaths(runId);
    }

    @Override
    public List<? extends IModification.IStructuralModification> getStructuralModificationsUsedInRun(int runId)
    {
        return ModificationManager.getStructuralModificationsUsedInRun(runId);
    }

    @Override
    public List<? extends IModification.IIsotopeModification> getIsotopeModificationsUsedInRun(int runId)
    {
        return ModificationManager.getIsotopeModificationsUsedInRun(runId);
    }

    @Override
    public ITargetedMSRun getRunByLsid(String lsid, Container container)
    {
        return TargetedMSManager.getRunByLsid(lsid, container);
    }

    @Override
    public ExperimentRunType getExperimentRunType()
    {
        return TargetedMSModule.EXP_RUN_TYPE;
    }

    @Override
    public UserSchema getUserSchema(User user, Container c)
    {
        return new TargetedMSSchema(user, c);
    }

    @Override
    public void registerTargetedMSFolderTypeListener(TargetedMSFolderTypeListener listener)
    {
        _targetedMsFolderTypeListeners.add(listener);
    }

    @Override
    public List<TargetedMSFolderTypeListener> getTargetedMSFolderTypeListeners()
    {
        return _targetedMsFolderTypeListeners;
    }
}
