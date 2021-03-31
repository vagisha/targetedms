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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.pipeline.LocalDirectory;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.security.User;
import org.labkey.api.settings.AppProps;
import org.labkey.api.util.FileUtil;
import org.labkey.api.view.NotFoundException;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.query.ConflictResultsManager;
import org.sqlite.SQLiteConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * User: vsharma
 * Date: 12/18/12
 * Time: 2:32 PM
 */
public class ChromatogramLibraryUtils
{
    private static final String propMapKey = "chromLibRevision";
    public static final int NO_LIB_REVISION = -1;

    public static boolean isRevisionCurrent(Container container, User user, String schemaVersion, int revisionNumber)
    {
        return Constants.SCHEMA_VERSION.equals(schemaVersion) && getCurrentRevision(container, user) == revisionNumber;
    }

    public static int getCurrentRevision(Container container, User user)
    {
        Map<String, String> propMap = PropertyManager.getProperties(container, "TargetedMS");
        if(!propMap.containsKey(propMapKey))
        {
            return NO_LIB_REVISION;
        }
        else
        {
            return Integer.parseInt(propMap.get(propMapKey));
        }
    }

    public static int getLastStableLibRevision(Container container, User user) throws IOException
    {
        int currentRevision = getCurrentRevision(container, user);
        // Get the oldest run that has conflicts
        TargetedMSRun run = ConflictResultsManager.getOldestRunWithConflicts(container);
        if(run == null)
        {
            // No conflicts.  Return the current revision
            return currentRevision;
        }
        else
        {
            // Return the library revision that was created just before the oldest run with conflicts.
            // This should be the last revision that was built with no conflicts.
            for(int i = currentRevision - 1; i > 0; i--)
            {
                Path clibFile = getChromLibFile(container, i);
                if(!Files.exists(clibFile))
                {
                    // If a .clib file does not exist it may be because the user deleted it.  In this case
                    // we cannot be sure that the last stable .clib is still on the server. If, for example, the last
                    // stable version was deleted we do not want the user to download an older library that may have been
                    // built when the folder had conflicts.
                    // Return a revision only if the revision chain is intact.
                    return NO_LIB_REVISION;
                }
                else if (run.getCreated().after(new Date(Files.getLastModifiedTime(clibFile).toMillis())))
                {
                    return i;
                }
            }
        }
        return NO_LIB_REVISION;
    }

    public static int incrementLibraryRevision(Container container, User user, LocalDirectory localDirectory)
    {
        PropertyManager.PropertyMap propMap = PropertyManager.getWritableProperties(container, "TargetedMS", true);
        String revisionVal = propMap.get(propMapKey);
        int newRevision;
        if(revisionVal == null)
        {
            newRevision = 1;
        }
        else
        {
            newRevision = Integer.parseInt(revisionVal) + 1;
        }

        propMap.put(propMapKey, Integer.toString(newRevision));
        propMap.save();

        // write the library to a file every time there is an increment
        writeLibrary(container, user, localDirectory, newRevision);

        return newRevision;
    }

    /** @return the name of the file that downloaders will see */
    public static String getDownloadFileName(Container container, int revision)
    {
        return container.getName() + "_rev" + revision + ".clib";
    }

    /** @return the name of the file that will be stored on disk.
     * Uses the container's RowId instead of name to handle renames gracefully */
    public static Path getChromLibFile(Container container, int revision) throws IOException
    {
        return getChromLibFile(container, revision, false);
    }

    public static Path getChromLibFile(Container container, int revision, boolean createLibdir) throws IOException
    {
        Path chromLibDir = getChromLibDir(container, createLibdir);
        return chromLibDir.resolve(Constants.CHROM_LIB_FILE_NAME+"_"+container.getRowId()+"_rev"+revision+"."+Constants.CHROM_LIB_FILE_EXT);
    }

    public static Path getChromLibTempFile(Container container, LocalDirectory localDirectory, int revision) throws IOException
    {
        // Temp file in LocalDirectory (guaranteed to be local File dir)
        File localDir = localDirectory.getLocalDirectoryFile();
        Path chromLibDir = localDir.toPath().resolve(Constants.LIB_FILE_DIR);
        if(!Files.exists(chromLibDir))
            Files.createDirectory(chromLibDir);
        return chromLibDir.resolve(
                        FileUtil.makeFileNameWithTimestamp(Constants.CHROM_LIB_FILE_NAME+"_"+container.getRowId()+"_rev"+revision,
                                                           Constants.CHROM_LIB_FILE_EXT));
    }

    private static Path getChromLibDir(Container container, boolean createLibDir) throws IOException
    {
        PipeRoot root = PipelineService.get().getPipelineRootSetting(container);
        assert root != null;
        Path chromLibDir = root.getRootNioPath().resolve(Constants.LIB_FILE_DIR);
        if(!Files.exists(chromLibDir) && createLibDir)
        {
            Files.createDirectory(chromLibDir);
        }
        return chromLibDir;
    }

    public static void writeLibrary(Container container, User user, LocalDirectory localDirectory, int targetRevision)
    {
        try
        {
            // Grab the panorama Server Url for storage in the library file
            String panoramaServer = AppProps.getInstance().getBaseServerUrl();

            // Get a list of runIds that have active representative data
            List<Long> representativeRunIds = TargetedMSManager.getCurrentRepresentativeRunIds(container);

            // Get the latest library revision.
            int currentRevision = ChromatogramLibraryUtils.getCurrentRevision(container, user);
            int libraryRevision = ( targetRevision != 0) ? targetRevision : currentRevision;

            Path chromLibFile = ChromatogramLibraryUtils.getChromLibFile(container, libraryRevision);

            ContainerChromatogramLibraryWriter writer = new ContainerChromatogramLibraryWriter(
                                                                         panoramaServer,
                                                                         container,
                                                                         representativeRunIds,
                                                                         user);
            writer.writeLibrary(localDirectory, libraryRevision);

            if(!Files.exists(chromLibFile))
            {
                throw new NotFoundException("Chromatogram library file " + chromLibFile + " was not found.");
            }
        }
        catch (java.io.IOException|SQLException exception)
        {
           throw new RuntimeException("There was an error writing a TargetedMS Library archive file.", exception);
        }
    }

    @Nullable
    public static TargetedMSController.ChromLibAnalyteCounts getLibraryAnalyteCounts(Container container, int libRevision) throws IOException, SQLException
    {
        Path archiveFile = ChromatogramLibraryUtils.getChromLibFile(container, libRevision);
        if(!Files.exists(archiveFile))
        {
            return null;
        }

        SQLiteConfig config = new SQLiteConfig();
        config.setReadOnly(true);
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:/" + archiveFile.toAbsolutePath().toString(), config.toProperties()))
        {
            TargetedMSController.ChromLibAnalyteCounts analyteCountsInLib = new TargetedMSController.ChromLibAnalyteCounts();
            analyteCountsInLib.setPeptideGroupCount(getCountsIn(Constants.Table.Protein, conn, null));
            analyteCountsInLib.setPeptideCount(getCountsIn(Constants.Table.Peptide, conn, "Sequence IS NOT NULL"));
            analyteCountsInLib.setMoleculeCount(getCountsIn(Constants.Table.Peptide, conn, "Sequence IS NULL"));
            analyteCountsInLib.setTransitionCount(getCountsIn(Constants.Table.Transition, conn, null));
            return analyteCountsInLib;
        }
    }

    private static int getCountsIn(Constants.Table table, Connection conn, String whereClause)
    {
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT COUNT(*) from " + table.name() + (whereClause == null ? "" : (" WHERE " + whereClause))))
        {
            if (rs.next())
            {
                return rs.getInt(1);
            }
        }
        catch (SQLException ignored) {} // May not have all tables in older library files
        return 0;
    }
}
