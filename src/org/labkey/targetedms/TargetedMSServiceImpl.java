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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.TableCustomizer;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.ExperimentRunType;
import org.labkey.api.exp.XarFormatException;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.targetedms.BlibSourceFile;
import org.labkey.api.targetedms.IModification;
import org.labkey.api.targetedms.ISampleFile;
import org.labkey.api.targetedms.ITargetedMSRun;
import org.labkey.api.targetedms.SkylineAnnotation;
import org.labkey.api.targetedms.SkylineDocumentImportListener;
import org.labkey.api.targetedms.TargetedMSFolderTypeListener;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.targetedms.model.SampleFileInfo;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.targetedms.chromlib.ChromatogramLibraryUtils;
import org.labkey.targetedms.datasource.MsDataSourceUtil;
import org.labkey.targetedms.parser.SampleFile;
import org.labkey.targetedms.parser.speclib.BlibSpectrumReader;
import org.labkey.targetedms.query.ModificationManager;
import org.labkey.targetedms.query.ReplicateManager;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * User: vsharma
 * Date: 8/26/2015
 * Time: 2:31 PM
 */
public class TargetedMSServiceImpl implements TargetedMSService
{
    // CopyOnWriteArrayList is a thread-safe variant of ArrayList in which all mutative operations (add, set, and so on)
    // are implemented by making a fresh copy of the underlying array.
    private List<SkylineDocumentImportListener> _skylineDocumentImportListeners = new CopyOnWriteArrayList<>();
    private List<TargetedMSFolderTypeListener> _targetedMsFolderTypeListeners = new CopyOnWriteArrayList<>();

    private List<TableCustomizer> _peptideSearchCustomizers = new CopyOnWriteArrayList<>();
    private List<TableCustomizer> _proteinSearchCustomizers = new CopyOnWriteArrayList<>();
    private List<TableCustomizer> _modificationSearchCustomizers = new CopyOnWriteArrayList<>();

    @Override
    public ITargetedMSRun getRun(long runId, Container container)
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
        return Collections.unmodifiableList(_skylineDocumentImportListeners);
    }

    @Override
    public List<SampleFileInfo> getSampleFiles(Container container, User user, Integer sampleFileLimit)
    {
        return TargetedMSManager.get().getSampleFileInfos(container, user, sampleFileLimit);
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
    public TableInfo getTableInfoGeneralMolecule()
    {
        return TargetedMSManager.getTableInfoGeneralMolecule();
    }

    @Override
    public TableInfo getTableInfoMolecule()
    {
        return TargetedMSManager.getTableInfoMolecule();
    }

    @Override
    public TableInfo getTableInfoGeneralPrecursor()
    {
        return TargetedMSManager.getTableInfoGeneralPrecursor();
    }

    @Override
    public TableInfo getTableInfoPrecursor()
    {
        return TargetedMSManager.getTableInfoPrecursor();
    }

    @Override
    public TableInfo getTableInfoMoleculePrecursor()
    {
        return TargetedMSManager.getTableInfoMoleculePrecursor();
    }

    @Override
    public List<String> getSampleFilePaths(long runId)
    {
        return ReplicateManager.getSampleFilePaths(runId);
    }

    @Override
    public List<SampleFile> getSampleFiles(long runId)
    {
        return ReplicateManager.getSampleFilesForRun(runId);
    }

    @Override
    public List<? extends IModification.IStructuralModification> getStructuralModificationsUsedInRun(long runId)
    {
        return ModificationManager.getStructuralModificationsUsedInRun(runId);
    }

    @Override
    public List<? extends IModification.IIsotopeModification> getIsotopeModificationsUsedInRun(long runId)
    {
        return ModificationManager.getIsotopeModificationsUsedInRun(runId);
    }

    @Override
    public Map<String, List<BlibSourceFile>> getBlibSourceFiles(ITargetedMSRun run)
    {
        return BlibSpectrumReader.readBlibSourceFiles(run);
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
        return Collections.unmodifiableList(_targetedMsFolderTypeListeners);
    }

    @Override
    public void addProteinSearchResultCustomizer(TableCustomizer customizer)
    {
        if(customizer == null) return;
        _proteinSearchCustomizers.add(customizer);
    }

    @Override
    public List<TableCustomizer> getProteinSearchResultCustomizer()
    {
        return Collections.unmodifiableList(_proteinSearchCustomizers);
    }

    @Override
    public void addPeptideSearchResultCustomizers(TableCustomizer customizer)
    {
        if(customizer == null) return;
        _peptideSearchCustomizers.add(customizer);
    }

    @Override
    public List<TableCustomizer> getPeptideSearchResultCustomizers()
    {
        return Collections.unmodifiableList(_peptideSearchCustomizers);
    }

    @Override
    public void addModificationSearchResultCustomizer(TableCustomizer customizer)
    {
        if(customizer == null) return;
        _modificationSearchCustomizers.add(customizer);
    }

    @Override
    public List<TableCustomizer> getModificationSearchResultCustomizers()
    {
        return Collections.unmodifiableList(_modificationSearchCustomizers);
    }

    @Override
    public Integer importSkylineDocument(ViewBackgroundInfo info, Path skylinePath) throws XarFormatException, PipelineValidationException
    {
        return TargetedMSManager.addRunToQueue(info, skylinePath);
    }

    @Override
    public List<? extends ISampleFile> getSampleFilesWithData(List<? extends ISampleFile> sampleFiles, Container container)
    {
        return MsDataSourceUtil.getInstance().getSampleFilesWithData(sampleFiles, container);
    }

    @Override
    public String getChromLibFileName(@NotNull Container container, int revision)
    {
        return ChromatogramLibraryUtils.getChromLibFileName(container, revision);
    }

    @Override
    public @Nullable Integer parseChromLibRevision(@NotNull String chromLibFileName)
    {
        return ChromatogramLibraryUtils.parseChromLibRevision(chromLibFileName);
    }
}
