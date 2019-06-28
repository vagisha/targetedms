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
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.labkey.api.data.BaseSelector;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlExecutor;
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
import org.labkey.targetedms.parser.skyaudit.UnitTestUtil;

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

public class SkylineAuditLogImporter
{

    private Logger _logger;
    private File _logFile;
    private GUID _documentGUID;
    private AuditLogTree _logTree;
    private SkylineAuditLogParser _parser = null;
    private SkylineAuditLogSecurityManager _securityMgr = null;
    private MessageDigest _rootHash = null;

    public SkylineAuditLogImporter(File pLogFile, GUID pDocumentGUID, Container pContainer, User pUser, Logger logger) throws AuditLogException{
        _logger = logger;
        _logFile = pLogFile;
        _documentGUID = pDocumentGUID;
        _securityMgr = new SkylineAuditLogSecurityManager(pContainer, pUser);
        try
        {
            _rootHash = MessageDigest.getInstance("SHA1");
        }
        catch (NoSuchAlgorithmException e)
        {
            _securityMgr.reportErrorForIntegrityLevel("Cannot verify root hash because SHA1 algorithm is not available.",
                        SkylineAuditLogSecurityManager.INTEGRITY_LEVEL.ANY);
        }


    }
    /***
     * Method verifies that all conditions required for the audit log upload are satisfied.
     *
     * @return It returns true if so, false if no log upload is required.
     * @throws AuditLogException is thrown if audit log settings prevent the document upload (should rollback document transaction on catch)
     */
    public boolean verifyPreRequisites() throws AuditLogException{
//        _logIntegrityLevel = getIntegrityLevel().getValue();

        //verify if log file and GUID are not empty
        if(_logFile == null || !_logFile.exists()){
            if(_securityMgr.getIntegrityLevel() == SkylineAuditLogSecurityManager.INTEGRITY_LEVEL.ANY){
                _logger.warn("Log file is missing. Proceeding without the log.");
            }
            else
                throw new AuditLogException("Current log integrity setting do not allow to upload a file without a valid audit log. ");
        }
        if(_documentGUID == null){
            if(_securityMgr.getIntegrityLevel() == SkylineAuditLogSecurityManager.INTEGRITY_LEVEL.ANY){
                _logger.warn("Cannot process the audit log because the document does not have a valid GUID. " +
                        "You are probably using old version if Skyline. Proceeding without the log.");
                return false;
            }
            else
                throw new AuditLogException("Current log integrity setting do not allow to upload a file without a valid document GUID. ");
        }

        //retrieve number of documents with the same GUID
        TableInfo runsTbl = TargetedMSManager.getTableInfoRuns();
        SimpleFilter guidFilter = new SimpleFilter(FieldKey.fromParts("documentGuid"), _documentGUID.toString());

        SQLFragment query = new SQLFragment("SELECT count(*) docCount FROM ").append(runsTbl.getSelectName()).append(" ");
        query.append(guidFilter.getSQLFragment(TargetedMSManager.getSqlDialect()));

        BaseSelector.ResultSetHandler<Integer> resultSetHandler = (rs, conn) -> {
            if(rs.next()){
                return rs.getInt("docCount");
            }
            else
                return 0;
        };
        Integer docCount = new SqlExecutor(runsTbl.getSchema()).executeWithResults(query, resultSetHandler);
        _logTree = buildLogTree();

        //verify that the tree is not empty
        if(docCount == 0 || _logTree.getTreeSize() > 1){
            //if document count >0 retrieve audit log tree for this GUID
            try
            {
                //schema validate the log file
                _parser = new SkylineAuditLogParser(_logFile, _logger);
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
     */
    public void persistAuditLog(Integer runId) throws AuditLogException {
        try
        {
            AuditLogMessageExpander expander = new AuditLogMessageExpander(_logger);
            //since entries in the log file are in reverse chronological order we have to
            //read them in the list and then reverse it before the tree processing
            List<AuditLogEntry> entries = new ArrayList<>();
            //while next entry is not null
            while (_parser.hasNextEntry())
            {
                AuditLogEntry ent = null;
                try
                {
                    ent = _parser.parseLogEntry();
                    ent.expandEntry(expander);
                    //throw or log the results based on the integrity level setting
                    if (!ent.verifyHash())
                    {
                        _securityMgr.reportErrorForIntegrityLevel(
                                String.format("Hash value verification failed for the log entry timestamped with %s", ent.getOffsetCreateTimestamp().toString()),
                                SkylineAuditLogSecurityManager.INTEGRITY_LEVEL.ANY);
                    }
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
                ent.setDocumentGUID(_documentGUID);
                entries.add(ent);
                _rootHash.update(ent.getHashString().getBytes(Charset.forName("UTF8")));
            }

            if(runId != null)       //set the document version od on the chronologicaly last log entry.
                entries.get(0).setVersionId(runId);

            Collections.reverse(entries);
            AuditLogTree treePointer = _logTree;
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
                    AuditLogTree newTreeEntry = ent.getTreeEntry();
                    treePointer.addChild(newTreeEntry);
                    //advance the tree
                    treePointer = newTreeEntry;
                }
            }
        }
        finally{
            _parser.abortParsing();
        }
    }

    public void verifyPostRequisites() throws AuditLogException{
        //verify the document-level hash if required
        byte[] rootHashBytes = _rootHash.digest();
        String rootHashString = new String(Base64.getEncoder().encode(rootHashBytes), Charset.forName("US-ASCII"));
        if(!rootHashString.equals(_parser.getEnRootHash()))
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
    private AuditLogTree buildLogTree() throws AuditLogException {
        TableInfo entryTbl = TargetedMSManager.getTableInfoSkylineAuditLogEntry();


        SQLFragment treeQuery = new SQLFragment("");
        if(TargetedMSManager.getSqlDialect().isPostgreSQL()){
            treeQuery.append("WITH RECURSIVE tree(id, hash, parenthash, treedepth) AS ");
        }
        else if(TargetedMSManager.getSqlDialect().isSqlServer()){
            treeQuery.append("WITH tree(id, hash, parenthash, treedepth) AS ");
        }
        else
            throw new AuditLogException("Database type not supported");

        treeQuery.append("( ");

        SimpleFilter entryFilter = new SimpleFilter(FieldKey.fromParts("documentGuid"), "'" + _documentGUID.toString() + "'");
        treeQuery.append("SELECT entryid, entryhash, parententryhash, 1 \n")
                .append("FROM " + entryTbl.getSchema().getName() + "." + entryTbl.getName() + " \n")
                .append("WHERE " + entryFilter.getFilterText() + "\n")
                .append("  AND parententryhash = '" + AuditLogTree.NULL_STRING + "' \n")
             .append(" UNION ALL \n")
                .append(" SELECT entryid, entryhash, parententryhash, treedepth + 1 \n")
                .append("FROM " + entryTbl.getSchema().getName() + "." + entryTbl.getName() + " e \n")
                .append(" JOIN tree ON tree.hash = e.parententryhash \n")
                .append("WHERE " + entryFilter.getFilterText() + "\n")
                 .append(" )\n")
                 .append(" SELECT DISTINCT * FROM tree ORDER BY treedepth");

        BaseSelector.ResultSetHandler<Map<String, AuditLogTree>> resultSetHandler = (rs, conn) -> {
            Map<String, AuditLogTree> result = new HashMap<>(10);

            while(rs.next()){
                String parentHash = rs.wasNull() ? AuditLogTree.NULL_STRING : rs.getString("parenthash");
                AuditLogTree node = new AuditLogTree(
                        rs.getInt("id"),
                        _documentGUID,
                        rs.getString("hash"),
                        parentHash
                );
                result.put(node.getEntryHash(), node);
            }
            return  result;
        };

        AuditLogTree root = new AuditLogTree(null, null, AuditLogTree.NULL_STRING, null);
        Map<String, AuditLogTree> nodes = new SqlExecutor(entryTbl.getSchema()).executeWithResults(treeQuery, resultSetHandler);
        nodes.put(AuditLogTree.NULL_STRING, root);
        for(Map.Entry<String, AuditLogTree> e : nodes.entrySet()){
            if(e.getValue().getParentEntryHash() != null)
                nodes.get(e.getValue().getParentEntryHash()).addChild(e.getValue());
        }
        return root;
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

            _logger = Logger.getLogger(SkylineAuditLogImporter.TestCase.class.getPackageName() + ".test");
            UnitTestUtil.cleanupDatabase(_docGUID);
            _user = TestContext.get().getUser();
            _container = ContainerManager.ensureContainer(JunitUtil.getTestContainer(), FOLDER_NAME);
        }

        private AuditLogTree persistALogFile(String filePath, Integer runId) throws IOException, AuditLogException{
            File fZip = UnitTestUtil.getSampleDataFile(filePath);
            File logFile = UnitTestUtil.extractLogFromZip(fZip, _logger);
            SkylineAuditLogImporter importer = new SkylineAuditLogImporter( logFile, _docGUID, _container, _user, _logger);

            if(importer.verifyPreRequisites()) {
                importer.persistAuditLog(runId);
                importer.verifyPostRequisites();
                return importer.buildLogTree();
            }
            else
                return null;
        }

        //@Test
        public void BuildTreeTest() throws IOException, AuditLogException
        {

            File fZip = UnitTestUtil.getSampleDataFile("AuditLogFiles/MethodEdit_v1.zip");
            File logFile = UnitTestUtil.extractLogFromZip(fZip, _logger);

            SkylineAuditLogImporter importer = new SkylineAuditLogImporter( logFile, _docGUID, _container, _user, _logger);

            importer.verifyPreRequisites();
            importer.persistAuditLog(null);
            importer.verifyPostRequisites();
            AuditLogTree tree = importer.buildLogTree();
            assertNotNull(tree);
            assertEquals(5, tree.getTreeSize());
        }

        @Test
        public void AddAVersionTest() throws IOException, AuditLogException
        {
            _logger.info("AuditLogFiles/MethodEdit_v2.zip");
            persistALogFile("AuditLogFiles/MethodEdit_v2.zip", null);
            _logger.info("AuditLogFiles/MethodEdit_v3.zip");
            persistALogFile("AuditLogFiles/MethodEdit_v3.zip", null);
            _logger.info("AuditLogFiles/MethodEdit_v4.zip");
            persistALogFile("AuditLogFiles/MethodEdit_v4.zip", null);
            _logger.info("AuditLogFiles/MethodEdit_v5.1.zip");
            persistALogFile("AuditLogFiles/MethodEdit_v5.1.zip", null);
            _logger.info("AuditLogFiles/MethodEdit_v5.2.zip");
            persistALogFile("AuditLogFiles/MethodEdit_v5.2.zip", null);
            _logger.info("AuditLogFiles/MethodEdit_v6.2.zip");
            AuditLogTree tree = persistALogFile("AuditLogFiles/MethodEdit_v6.2.zip", null);
            assertNotNull(tree);
            assertEquals(14, tree.getTreeSize());
        }



    }
}
