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
import org.labkey.api.services.ServiceRegistry;
import org.labkey.api.targetedms.model.SampleFileInfo;
import org.labkey.api.targetedms.model.SampleFilePath;
import org.labkey.api.view.ViewBackgroundInfo;

import java.nio.file.Path;
import java.util.List;

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
        Experiment, ExperimentMAM, Library, LibraryProtein, QC, Undefined
    }

    String MODULE_NAME = "TargetedMS";
    String FOLDER_TYPE_NAME = "Targeted MS";
    String FOLDER_TYPE_PROP_NAME = "TargetedMS Folder Type"; // module property name
    String RAW_FILES_DIR = "RawFiles";
    String RAW_FILES_TAB = "Raw Data";
    String CHROM_LIB_FILE_DIR = "targetedMSLib";
    String CHROM_LIB_FILE_BASE_NAME = "chromlib";
    String CHROM_LIB_FILE_EXT = "clib";
    String PROP_CHROM_LIB_REVISION = "chromLibRevision";

    ITargetedMSRun getRun(long runId, Container container);
    @Nullable ITargetedMSRun getRun(long runId, User user);
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
    TableInfo getTableInfoMolecule();
    TableInfo getTableInfoGeneralPrecursor();
    TableInfo getTableInfoPrecursor();
    TableInfo getTableInfoMoleculePrecursor();
    TableInfo getTableInfoPeptideStructuralModification();
    TableInfo getTableInfoPeptideIsotopeModification();
    TableInfo getTableInfoInstrument();

    List<String> getSampleFilePaths(long runId);
    List<? extends ISampleFile> getSampleFiles(long runId);
    List<? extends IModification.IStructuralModification> getStructuralModificationsUsedInRun(long runId);
    List<? extends IModification.IIsotopeModification> getIsotopeModificationsUsedInRun(long runId);

    @Nullable ISpectrumLibrary getLibrary(long id, @Nullable Container container, User user);

    /**
     * @param run ITargetedMSRun object representing a Skyline document
     * @return the spectral libraries used with the given run.
     */
    @NotNull List<? extends ISpectrumLibrary> getLibraries(ITargetedMSRun run);

    /**
     *
     * @param run ITargetedMSRun object representing a Skyline document
     * @param library library
     * @return the Path to the library file within the unzipped directory of the given Skyline document.
     */
    @Nullable Path getLibraryFilePath(ITargetedMSRun run, ISpectrumLibrary library);

    // Add table customizers for the protein / peptide / modification search results
    void addProteinSearchResultCustomizer(TableCustomizer customizer);
    List<TableCustomizer> getProteinSearchResultCustomizer();
    void addPeptideSearchResultCustomizers(TableCustomizer customizer);
    List<TableCustomizer> getPeptideSearchResultCustomizers();
    void addModificationSearchResultCustomizer(TableCustomizer columnInfo);
    List<TableCustomizer> getModificationSearchResultCustomizers();

    /** @return rowId for pipeline job that will perform the import asynchronously */
    Integer importSkylineDocument(ViewBackgroundInfo info, Path skylinePath) throws XarFormatException, PipelineValidationException;

    /**
     * @param sampleFiles list of sample files for which we should do a path lookup on the server
     * @param container container where we should look for the files
     * @param lookupExpData if true, look for a matching row in exp.data before looking on the filesystem
     * @return List of {@link SampleFilePath} objects encapsulating an ISampleFile and its path on the server.
     */
    List<SampleFilePath> getSampleFilePaths(List<? extends ISampleFile> sampleFiles, Container container, boolean lookupExpData);

    /**
     * Returns the name of a chromatogram library file according to the naming pattern used for creating
     * .clib files for a specific revision number in a given container.
     * @param container
     * @param revision
     * @return
     */
    String getChromLibFileName(@NotNull Container container, int revision);

    /**
     * Returns the revision number from the file name if the given file name matches the chromatogram library
     * naming pattern.  Example: chromlib_314_rev5.clib; revision number is 5.
     * Returns null if the given file name does not match the expected pattern.
     * @param chromLibFileName
     * @return
     */
    @Nullable
    Integer parseChromLibRevision(@NotNull String chromLibFileName);
}
