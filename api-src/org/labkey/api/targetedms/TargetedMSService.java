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
package org.labkey.api.targetedms;

import org.labkey.api.data.Container;
import org.labkey.api.data.TableCustomizer;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.ExperimentRunType;
import org.labkey.api.exp.XarFormatException;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.api.targetedms.model.SampleFileInfo;
import org.labkey.api.view.ViewBackgroundInfo;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * User: vsharma
 * Date: 8/26/2015
 * Time: 11:34 AM
 */
public interface TargetedMSService
{
    static TargetedMSService get()
    {
        return ServiceRegistry.get().getService(TargetedMSService.class);
    }

    static void setInstance(TargetedMSService impl)
    {
        ServiceRegistry.get().registerService(TargetedMSService.class, impl);
    }

    enum FolderType
    {
        Experiment, Library, LibraryProtein, QC, Undefined
    }

    String MODULE_NAME = "TargetedMS";
    String FOLDER_TYPE_NAME = "Targeted MS";
    String FOLDER_TYPE_PROP_NAME = "TargetedMS Folder Type"; // module property name
    String RAW_FILES_DIR = "RawFiles";
    String RAW_FILES_TAB = "Raw Data";

    ITargetedMSRun getRun(int runId, Container container);
    ITargetedMSRun getRunByFileName(String fileName, Container container);
    List<ITargetedMSRun> getRuns(Container container);
    ITargetedMSRun getRunByLsid(String lsid, Container container);
    List<? extends SkylineAnnotation> getReplicateAnnotations(Container container);
    void registerSkylineDocumentImportListener(SkylineDocumentImportListener skyLineDocumentImportListener);
    List<SkylineDocumentImportListener> getSkylineDocumentImportListener();
    void registerTargetedMSFolderTypeListener(TargetedMSFolderTypeListener listener);
    List<TargetedMSFolderTypeListener> getTargetedMSFolderTypeListeners();
    List<SampleFileInfo> getSampleFiles(Container container, User user, Integer sampleFileLimit);
    TargetedMSService.FolderType getFolderType(Container container);

    ExperimentRunType getExperimentRunType();
    UserSchema getUserSchema( User user, Container c);
    TableInfo getTableInfoRuns();
    TableInfo getTableInfoPeptideGroup();
    TableInfo getTableInfoGeneralMolecule();
    List<String> getSampleFilePaths(int runId);
    List<? extends IModification.IStructuralModification> getStructuralModificationsUsedInRun(int runId);
    List<? extends IModification.IIsotopeModification> getIsotopeModificationsUsedInRun(int runId);

    // Add table customizers for the protein / peptide / modification search results
    void addProteinSearchResultCustomizer(TableCustomizer customizer);
    List<TableCustomizer> getProteinSearchResultCustomizer();
    void addPeptideSearchResultCustomizers(TableCustomizer customizer);
    List<TableCustomizer> getPeptideSearchResultCustomizers();
    void addModificationSearchResultCustomizer(TableCustomizer columnInfo);
    List<TableCustomizer> getModificationSearchResultCustomizers();

    /** @return rowId for pipeline job that will perform the import asynchronously */
    Integer importSkylineDocument(ViewBackgroundInfo info, Path skylinePath) throws XarFormatException, PipelineValidationException;
}
