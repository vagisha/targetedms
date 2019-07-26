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
package org.labkey.targetedms;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.labkey.api.data.BaseSelector;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.util.GUID;
import org.labkey.api.util.JunitUtil;
import org.labkey.api.util.TestContext;
import org.labkey.targetedms.parser.skyaudit.AuditLogEntry;
import org.labkey.targetedms.parser.skyaudit.AuditLogException;
import org.labkey.targetedms.parser.skyaudit.AuditLogMessageExpander;
import org.labkey.targetedms.parser.skyaudit.AuditLogParsingException;
import org.labkey.targetedms.parser.skyaudit.AuditLogTree;
import org.labkey.targetedms.parser.skyaudit.SkylineAuditLogParser;
import org.labkey.targetedms.parser.skyaudit.SkylineAuditLogSecurityManager;
import org.labkey.targetedms.parser.skyaudit.TestRun;
import org.labkey.targetedms.parser.skyaudit.UnitTestUtil;

import javax.validation.constraints.NotNull;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;


public class SkylineAuditLogManager
{

    private Logger _logger;
    private SkylineAuditLogSecurityManager _securityMgr;

    private class AuditLogImportContext{
        File _logFile;
        GUID _documentGUID;
        AuditLogTree _logTree;
        SkylineAuditLogParser _parser = null;
        Integer _runId;
        MessageDigest _rootHash = null;
    }

    public SkylineAuditLogManager(@NotNull Container pContainer, @NotNull User pUser) throws AuditLogException{
        _logger = Logger.getLogger(this.getClass());
        _securityMgr = new SkylineAuditLogSecurityManager(pContainer, pUser);
    }


    public int importAuditLogFile(@NotNull File pAuditLogFile, @NotNull GUID pDocumentGUID, int pRunId) throws AuditLogException
    {
        AuditLogImportContext context = new AuditLogImportContext();
        context._logFile = pAuditLogFile;
        context._documentGUID = pDocumentGUID;
        context._runId = pRunId;
        try
        {
            context._rootHash = MessageDigest.getInstance("SHA1");
        }
        catch (NoSuchAlgorithmException e)
        {
            _securityMgr.reportErrorForIntegrityLevel("Cannot verify root hash because SHA1 algorithm is not available.",
                    SkylineAuditLogSecurityManager.INTEGRITY_LEVEL.ANY);
        }
        if(verifyPreRequisites(context)) {
            int entriesCount = persistAuditLog(context);
            verifyPostRequisites(context);
            return entriesCount;
        }
        else
            return 0;
    }


    /***
     * Method verifies that all conditions required for the audit log upload are satisfied.
     *
     * @return It returns true if so, false if no log upload is required.
     * @throws AuditLogException is thrown if audit log settings prevent the document upload (should rollback document transaction on catch)
     */
    private boolean verifyPreRequisites(AuditLogImportContext pContext) throws AuditLogException{
//        _logIntegrityLevel = getIntegrityLevel().getValue();

        //verify if log file and GUID are not empty
        if(pContext._logFile == null || !pContext._logFile.exists()){
            if(_securityMgr.getIntegrityLevel() == SkylineAuditLogSecurityManager.INTEGRITY_LEVEL.ANY){
                _logger.warn("Log file is missing. Proceeding without the log.");
                return false;
            }
            else
                throw new AuditLogException("Current log integrity setting do not allow to upload a file without a valid audit log. ");
        }
        if(pContext._documentGUID == null){
            if(_securityMgr.getIntegrityLevel() == SkylineAuditLogSecurityManager.INTEGRITY_LEVEL.ANY){
                _logger.warn("Cannot process the audit log because the document does not have a valid GUID. " +
                        "You are probably using old version if Skyline. Proceeding without the log.");
                return false;
            }
            else
                throw new AuditLogException("Current log integrity setting do not allow to upload a file without a valid document GUID. ");
        }

        //retrieve count of documents with the same GUID
        TableInfo runsTbl = TargetedMSManager.getTableInfoRuns();
        SimpleFilter docFilter = new SimpleFilter();
        docFilter.addCondition(       // get all entries for all versions of this document
                new CompareType.CompareClause(FieldKey.fromParts("documentGuid"), CompareType.EQUAL, pContext._documentGUID.toString())
        );
        docFilter.addCondition(       //except the version we are currently uploading
                new CompareType.CompareClause(FieldKey.fromParts("Id"), CompareType.NEQ, pContext._runId)
        );
        docFilter.addCondition(       //and they are successfully loaded
                new CompareType.CompareClause(FieldKey.fromParts("StatusId"), CompareType.EQUAL, SkylineDocImporter.STATUS_SUCCESS)
        );

        SQLFragment query = new SQLFragment("SELECT count(*) docCount FROM ")
                    .append(runsTbl.getSelectName())
                    .append(" ")
                    .append(docFilter.getSQLFragment(TargetedMSManager.getSqlDialect()));

        Integer docCount = new SqlSelector(TargetedMSManager.getSchema(), query).getObject(Integer.class);
        pContext._logTree = buildLogTree(pContext._documentGUID);

        //verify that the tree is not empty
        if(docCount == 0 || pContext._logTree.getTreeSize() > 1){
            //if document count >0 retrieve audit log tree for this GUID
            try
            {
                //schema validate the log file
                pContext._parser = new SkylineAuditLogParser(pContext._logFile, _logger);
                return true;
            }
            catch(AuditLogException e){
                _securityMgr.reportErrorForIntegrityLevel("Error while parsing the audit log file.",
                        SkylineAuditLogSecurityManager.INTEGRITY_LEVEL.ANY, e);
                return false;
            }
        }
        else
        {
            _securityMgr.reportErrorForIntegrityLevel("Cannot upload the audit log. There are existing versions of this document that do not have audit log information.",
                    SkylineAuditLogSecurityManager.INTEGRITY_LEVEL.ANY);
            return false;
        }
    }

    /***
     * Iterates through the log entries and persists them one by one
     * @return number of audit log entries read from the file.
     */
    private int persistAuditLog(AuditLogImportContext pContext) throws AuditLogException {
        try
        {
            AuditLogMessageExpander expander = new AuditLogMessageExpander(_logger);
            //since entries in the log file are in reverse chronological order we have to
            //read them in the list and then reverse it before the tree processing
            List<AuditLogEntry> entries = new ArrayList<>();
            //while next entry is not null
            while (pContext._parser.hasNextEntry())
            {
                try
                {
                    AuditLogEntry ent = pContext._parser.parseLogEntry();
                    ent.expandEntry(expander);
                    //throw or log the results based on the integrity level setting
                    if (!ent.verifyHash())
                    {
                        _securityMgr.reportErrorForIntegrityLevel(
                                String.format("Hash value verification failed for the log entry timestamped with %s", ent.getOffsetCreateTimestamp().toString()),
                                SkylineAuditLogSecurityManager.INTEGRITY_LEVEL.ANY);
                    }
                    ent.setDocumentGUID(pContext._documentGUID);
                    entries.add(ent);
                    pContext._rootHash.update(ent.getHashString().getBytes(Charset.forName("UTF8")));
                }
                catch (XMLStreamException e)
                {
                    throw new AuditLogException("Error when parsing the audit log file.", e);
                }
                catch(NoSuchAlgorithmException e) {
                    _securityMgr.reportErrorForIntegrityLevel(
                            "Cannot verify entry hash because SHA1 algorithm is not available.",
                            SkylineAuditLogSecurityManager.INTEGRITY_LEVEL.ANY);
                }
                catch(AuditLogParsingException e){
                    _securityMgr.reportErrorForIntegrityLevel(
                            "Error when parsing audit log file.",
                            SkylineAuditLogSecurityManager.INTEGRITY_LEVEL.ANY, e);
                }
            }

            if(pContext._runId != null)       //set the document version id on the chronologically last log entry.
                entries.get(0).setVersionId(pContext._runId);

            Collections.reverse(entries);
            AuditLogTree treePointer = pContext._logTree;
            int persistedEntriesCount = 0;
            for(AuditLogEntry ent : entries)
            {
                //if log tree has the hash
                if (treePointer.hasChild(ent.getEntryHash()))
                {
                    //advance the tree and proceed to the next entry
                    treePointer = treePointer.getChild(ent.getEntryHash());
                }
                else
                {
                    if (treePointer.getParentEntryHash() == null && treePointer.getTreeSize() > 1)
                    {
                        throw new AuditLogException("Invalid audit log. Documents with same GUID should have same first audit log entry.");
                    }
                    // persist the entry and add to the tree
                    ent.setParentEntryHash(treePointer.getEntryHash());
                    ent.persist();
                    persistedEntriesCount++;
                    AuditLogTree newTreeEntry = ent.getTreeEntry();
                    treePointer.addChild(newTreeEntry);
                    //advance the tree
                    treePointer = newTreeEntry;
                }
            }
            if(persistedEntriesCount == 0)      //if no entries were actually saved into the database we are uploading an earlier document version
                entries.get(entries.size() - 1).updateVersionId(pContext._runId);   //and still need to update the terminal entry with the versionId.

            return entries.size();
        }
        finally{
            pContext._parser.abortParsing();
        }
    }

    private void verifyPostRequisites(AuditLogImportContext pContext) throws AuditLogException{
        //verify the document-level hash if required
        byte[] rootHashBytes = pContext._rootHash.digest();
        String rootHashString = new String(Base64.getEncoder().encode(rootHashBytes), Charset.forName("US-ASCII"));
        if(!rootHashString.equals(pContext._parser.getEnRootHash()))
        {
            //throw or log the results based on the integrity level setting
            _securityMgr.reportErrorForIntegrityLevel(
                    "Audit log root hash verification failed",
                    SkylineAuditLogSecurityManager.INTEGRITY_LEVEL.ANY);
        }

    }

    /**
     * Builds a tree of audit log entry tokens representing the document versioning tree
     * @return the root node of the tree
     */
    private AuditLogTree buildLogTree(@NotNull GUID pDocumentGUID) throws AuditLogException {

        TableInfo entryTbl = TargetedMSManager.getTableInfoSkylineAuditLogEntry();

        SQLFragment treeQuery = new SQLFragment("");
        if(TargetedMSManager.getSqlDialect().isPostgreSQL()){
            treeQuery.append("WITH RECURSIVE tree(id, hash, parenthash, versionId, treedepth) AS ");
        }
        else if(TargetedMSManager.getSqlDialect().isSqlServer()){
            treeQuery.append("WITH tree(id, hash, parenthash, versionId, treedepth) AS ");
        }
        else
            throw new AuditLogException("Database type not supported");

        treeQuery.append("( ");

        SimpleFilter entryFilter = new SimpleFilter(FieldKey.fromParts("documentGuid"), "'" + pDocumentGUID.toString() + "'");
        treeQuery.append("SELECT entryid, entryhash, parententryhash, versionId, 1 \n")
                .append("FROM " + entryTbl.getSchema().getName() + "." + entryTbl.getName() + " \n")
                .append("WHERE " + entryFilter.getFilterText() + "\n")
                .append("  AND parententryhash = '" + AuditLogTree.NULL_STRING + "' \n")
             .append(" UNION ALL \n")
                .append(" SELECT entryid, entryhash, parententryhash, e.versionId, treedepth + 1 \n")
                .append("FROM " + entryTbl.getSchema().getName() + "." + entryTbl.getName() + " e \n")
                .append(" JOIN tree ON tree.hash = e.parententryhash \n")
                .append("WHERE " + entryFilter.getFilterText() + "\n")
                 .append(" )\n")
                 .append(" SELECT DISTINCT * FROM tree ORDER BY treedepth");

        BaseSelector.ResultSetHandler<Map<String, AuditLogTree>> resultSetHandler = (rs, conn) -> {
            Map<String, AuditLogTree> result = new HashMap<>(10);

            while(rs.next()){
                String parentHash = rs.getString("parenthash");
                if(rs.wasNull()) parentHash = AuditLogTree.NULL_STRING;
                Integer versionId = rs.getInt("versionId");
                if(rs.wasNull()) versionId = null;

                AuditLogTree node = new AuditLogTree(
                        rs.getInt("id"),
                        pDocumentGUID,
                        rs.getString("hash"),
                        parentHash,
                        versionId
                );
                result.put(node.getEntryHash(), node);
            }
            return  result;
        };

        AuditLogTree root = new AuditLogTree(0, null, AuditLogTree.NULL_STRING, null,null);
        Map<String, AuditLogTree> nodes = new SqlExecutor(entryTbl.getSchema()).executeWithResults(treeQuery, resultSetHandler);
        nodes.put(AuditLogTree.NULL_STRING, root);
        for(Map.Entry<String, AuditLogTree> e : nodes.entrySet()){
            if(e.getValue().getParentEntryHash() != null)
                nodes.get(e.getValue().getParentEntryHash()).addChild(e.getValue());
        }
        return root;
    }

    /**
     * Deletes audit log entries belonging to the given document version. It will not delete entries that
     * belong to more than one version. If the given run has no GUID it does nothing.
     * @param pRunId of the document version to delete
     */
    public void deleteDocumentVersionLog(int pRunId) throws AuditLogException
    {
        SQLFragment query = new SQLFragment(String.format("SELECT documentGUID FROM targetedms.Runs WHERE Id = %d", pRunId));
        String objGUID = new SqlSelector(TargetedMSManager.getSchema(), query).getObject(String.class);
        if(objGUID == null) return;     //nothing to delete if there is no document or the document has no GUID
        AuditLogTree root = buildLogTree(new GUID(objGUID));
        List<Integer> deleteEntryIds = root.deleteList(pRunId).stream().map(AuditLogTree::getEntryId).collect(Collectors.toList());

        if(deleteEntryIds.size() > 0)
        {
            SimpleFilter entryFilter = new SimpleFilter(
                    new SimpleFilter.InClause(FieldKey.fromParts("entryId"), deleteEntryIds, false)
            );
            Table.delete(TargetedMSManager.getTableInfoSkylineAuditLogMessage(), entryFilter);
            Table.delete(TargetedMSManager.getTableInfoSkylineAuditLogEntry(), entryFilter);
        }
        else{
            AuditLogTree versionTree = root.findVersionEntry(pRunId);
            if(versionTree != null)
            {
                SQLFragment sqlUpdate = new SQLFragment("UPDATE targetedms.AuditLogEntry SET versionId = NULL WHERE entryId = ?");
                sqlUpdate.add(versionTree.getEntryId());
                new SqlExecutor(TargetedMSManager.getSchema()).execute(sqlUpdate);
            }
        }
    }

    /**
     * Deletes all log entries for this document based on the documentGUID.
     * This assumes that all versions of the document are being deleted.
     * @param pRunId of one of the versions of the document to be deleted
     */
    public void deleteDocumentLog(int pRunId)
    {
        SQLFragment sqlGetGuid = new SQLFragment("SELECT documentGUID FROM targetedms.Runs WHERE Id = ?");
        sqlGetGuid.add(pRunId);

        String res = new SqlSelector(TargetedMSManager.getSchema(), sqlGetGuid).getObject(String.class);
        if(res != null)
        {
            deleteDocumentLog(new GUID(res));
        }
    }

    public void deleteDocumentLog(@NotNull GUID pDocumentGUID)
    {
        SQLFragment sqlDeleteMessages = new SQLFragment("DELETE FROM targetedms.AuditLogMessage " +
                                "WHERE entryId IN (SELECT entryId from targetedms.AuditLogEntry WHERE documentGUID = ?)");
        sqlDeleteMessages.add(pDocumentGUID.toString());
        SQLFragment sqlDeleteEntries = new SQLFragment("DELETE FROM targetedms.AuditLogMessage WHERE documentGUID = ?");
        sqlDeleteEntries.add(pDocumentGUID.toString());

        SqlExecutor exec = new SqlExecutor(TargetedMSSchema.getSchema());

        exec.execute(sqlDeleteMessages);
        exec.execute(sqlDeleteEntries);
    }

    public static class TestCase extends Assert
    {
        private static final String FOLDER_NAME = "TargetedMSAuditLogImportFolder";
        private static final GUID _docGUID = new GUID("50323e78-0e2b-4764-b979-9b71559bbf9f");
        private static Logger _logger;
        private static User _user;
        private static Container _container;

        @BeforeClass
        public static void InitTest(){

            _logger = Logger.getLogger(SkylineAuditLogManager.TestCase.class.getPackageName() + ".test");
            UnitTestUtil.cleanupDatabase(_docGUID);
            _user = TestContext.get().getUser();
            _container = ContainerManager.ensureContainer(JunitUtil.getTestContainer(), FOLDER_NAME);
        }

        private AuditLogTree persistALogFile(String filePath, Integer runId) throws IOException, AuditLogException{
            File fZip = UnitTestUtil.getSampleDataFile(filePath);
            File logFile = UnitTestUtil.extractLogFromZip(fZip, _logger);
            SkylineAuditLogManager importer = new SkylineAuditLogManager(_container, _user);

            importer.importAuditLogFile(logFile, _docGUID, runId);
            return importer.buildLogTree(_docGUID);
        }

        private int getNewRunId(GUID pDocumentGUID){
            TestRun run = new TestRun();
            run._container = _container.getEntityId();
            run._documentGUID = pDocumentGUID;
            Table.insert(_user, TargetedMSManager.getTableInfoRuns(), run);
            _logger.info(String.format("new run is inserted with id %d", run._id));
            return run._id;
        }

        //@Test
        public void BuildTreeTest() throws IOException, AuditLogException
        {

            File fZip = UnitTestUtil.getSampleDataFile("AuditLogFiles/MethodEdit_v1.zip");
            File logFile = UnitTestUtil.extractLogFromZip(fZip, _logger);

            SkylineAuditLogManager importer = new SkylineAuditLogManager(_container, _user);

            importer.importAuditLogFile( logFile, _docGUID, getNewRunId(_docGUID));

            AuditLogTree tree = importer.buildLogTree(_docGUID);
            assertNotNull(tree);
            assertEquals(5, tree.getTreeSize());
        }

        @Test
        public void AddAVersionTest() throws IOException, AuditLogException
        {
            Stack<Integer> runIds = new Stack<>();
            List<String> testFileNames = new ArrayList<>(
                    List.of("MethodEdit_v2.zip", "MethodEdit_v3.zip", "MethodEdit_v3.zip", "MethodEdit_v4.zip",
                            "MethodEdit_v5.1.zip", "MethodEdit_v5.2.zip", "MethodEdit_v6.2.zip"));

            AuditLogTree tree = null;

            for(String fileName : testFileNames){
                _logger.info("AuditLogFiles/" + fileName);
                runIds.push(getNewRunId(_docGUID));
                tree = persistALogFile("AuditLogFiles/" + fileName, runIds.peek());
            }

            assertNotNull(tree);
            assertEquals(14, tree.getTreeSize());

            SkylineAuditLogManager importer = new SkylineAuditLogManager(_container, _user);
            importer.deleteDocumentVersionLog(runIds.pop());
            tree = importer.buildLogTree(_docGUID);
            assertEquals(13, tree.getTreeSize());

            importer.deleteDocumentVersionLog(runIds.pop());
            tree = importer.buildLogTree(_docGUID);
            assertEquals(11, tree.getTreeSize());

            runIds.pop();
            importer.deleteDocumentVersionLog(runIds.pop());
            tree = importer.buildLogTree(_docGUID);
            assertEquals(11, tree.getTreeSize());
        }

        @AfterClass
        public static void cleanup()
        {
            UnitTestUtil.cleanupDatabase(_docGUID);
        }

    }
}
