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

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.resource.FileResource;
import org.labkey.api.util.GUID;
import org.labkey.api.util.Path;
import org.labkey.targetedms.TargetedMSModule;
import org.labkey.targetedms.parser.XmlUtil;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/***
 * Reads the audit log file, validates it and converts into a sequence
 * of AuditLogEntry instances that can be persisted into the database
 */
public class SkylineAuditLogParser
{
    //------ log root
    public static final String AUDIT_LOG_ROOT = "audit_log_root";
    public static final String FORMAT_VERSION = "format_version";
    public static final String DOCUMENT_HASH = "document_hash";
    public static final String EN_ROOT_HASH = "root_hash";
    public static final String AUDIT_LOG = "audit_log";
    public static final String AUDIT_LOG_ENTRY = "audit_log_entry";
    //--------- log entry
    private static final String SKYLINE_VERSION = "skyline_version";
    private static final String TIME_STAMP = "time_stamp";
    private static final String USER = "user";
    private static final String REASON = "reason";
    private static final String EXTRA_INFO = "extra_info";
    private static final String EXTRA_INFO_ENGLISH = "en_extra_info";
    private static final String UNDO_REDO_MSG = "undo_redo";
    private static final String SUMMARY_MSG = "summary";
    private static final String MESSAGE = "all_info";
    private static final String EN_HASH = "hash";
    //--------- log message
    private static final String MESSAGE_TYPE = "type";
    private static final String MESSAGE_NAME = "name";
    private static final String MESSAGE_TEXT = "en_expanded";
    private static final String MESSAGE_REASON = "reason";


    private static final String SCHEMA_FILE = "schemas/Skyl.xsd";

    private File _file;
    private Logger _logger;
    private XMLStreamReader _stream;
    private FileInputStream _fileStream;

    private String _documentHash;
    private String _enRootHash;
    private String _formatVersion;


    public SkylineAuditLogParser(File logFile, Logger logger)  throws AuditLogException{

        _file = logFile;
        _logger = logger;

        try
        {
            validateXml();
            parseLogHeader();
        }
        catch(Exception e){
            throw new AuditLogException("Error when parsing audit log file.", e);
        }
    }


    private void validateXml() throws IOException, SAXException, AuditLogParsingException {

        //retrieve schema file
        FileInputStream schemaStream;
        if(ModuleLoader.getInstance() != null)
        {   //if we are running web test
            Module module = ModuleLoader.getInstance().getModule(TargetedMSModule.class);
            FileResource schemaResource = (FileResource) module.getModuleResolver().lookup(Path.parse(SCHEMA_FILE));
            if (schemaResource == null)
            {
                throw new AuditLogParsingException("Schema file not found in the module resources.");
            }
            schemaStream = new FileInputStream(schemaResource.getFile());
        }
        else //this is for unit testing
            schemaStream = new FileInputStream(UnitTestUtil.getResourcesFile(SCHEMA_FILE));

        //prepare validator
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(new StreamSource(schemaStream));
        Validator validator = schema.newValidator();

        InputStream auditLogStream = new FileInputStream(_file);
        auditLogStream = new FilterInputStream(new BufferedInputStream(auditLogStream)) {
            @Override
            public void close() {
                try{ super.close();} catch(IOException e){};
            }
        };
        validator.validate(new StreamSource(auditLogStream));
        schemaStream.close();
        auditLogStream.close();
    }

    /***
     * This method parses the beginning of the log: the hashes and audit_log tag and stops at
     * the first log entry, ready to proceed with read/save loop
     * @throws IOException
     * @throws SAXException
     * @throws AuditLogParsingException
     * @throws XMLStreamException
     */
    private void parseLogHeader() throws IOException, SAXException, AuditLogParsingException, XMLStreamException
    {
        _fileStream = new FileInputStream(_file);
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        _stream = inputFactory.createXMLStreamReader(_fileStream);

        //Skipping most XML structure validation since the file passed the schema validation
        int evtType = _stream.nextTag();     //log root element read
        this._formatVersion = _stream.getAttributeValue(null, FORMAT_VERSION);

        while(_stream.hasNext()){
            evtType = _stream.nextTag();
            if(evtType == XMLStreamReader.START_ELEMENT){
                switch(_stream.getLocalName()){
                    case EN_ROOT_HASH:
                        this._enRootHash = _stream.getElementText();
                        break;
                    case DOCUMENT_HASH:
                        this._documentHash = _stream.getElementText();
                        break;
                    case AUDIT_LOG:
                        _stream.nextTag();
                        return;
                }
            }
        }
    }

    public AuditLogEntry parseLogEntry() throws XMLStreamException, AuditLogParsingException{

        //XmlUtil.skip(_stream, XMLStreamReader.START_ELEMENT, AuditLog.AUDIT_LOG_ENTRY );
        //the _stream must be at the correct position to parse an entry
        assert (_stream.getLocalName().equals(AUDIT_LOG_ENTRY) && _stream.getEventType() == XMLStreamReader.START_ELEMENT) :
                    "Parser is at a wrong position to parse a log entry";

        AuditLogEntry result = new AuditLogEntry();

        result.setFormatVersion(_stream.getAttributeValue(null, SKYLINE_VERSION));

        String time_stamp = _stream.getAttributeValue(null, TIME_STAMP);
        try
        {
            result.parseCreateTimestamp(time_stamp);
        }
        catch(DateTimeParseException e){
            throw new AuditLogParsingException(String.format("Invalid date/time format in audit log file: %s", time_stamp), e);
        }
        result.setUserName(_stream.getAttributeValue(null, USER));

        int messageCount = 0;
        while(_stream.hasNext()){
            switch(_stream.nextTag()){
                case XMLStreamReader.START_ELEMENT:
                    switch(_stream.getLocalName()){
                        case REASON:
                            result.setReason(_stream.getElementText());
                            break;
                        case EXTRA_INFO_ENGLISH:
                            result.setExtraInfo(_stream.getElementText().replace("\n", "\r\n"));
                        case EXTRA_INFO:
                            if(result.getExtraInfo() == null)
                                result.setExtraInfo(_stream.getElementText().replace("\n", "\r\n"));
                            break;
                        case UNDO_REDO_MSG:
                        case SUMMARY_MSG:
                        case MESSAGE:
                            AuditLogMessage msg = this.parseAuditLogMessage();
                            msg.setOrderNumber(messageCount);
                            result._allInfoMessage.add(msg);
                            messageCount++;
                            break;
                        case EN_HASH:
                            result.setEntryHash(_stream.getElementText());
                            break;
                        default:
                            throw new AuditLogParsingException("Unexpected tag encountered:" + _stream.getLocalName());
                    }
                    //XmlUtil.skip(_stream, _stream.END_ELEMENT);
                    break;
                case XMLStreamReader.END_ELEMENT:
                    _stream.nextTag();
                    return result;
            }
        }
        throw new AuditLogParsingException("Element end expected.");
    }

    public boolean hasNextEntry(){
        if(XmlUtil.isEndElement(_stream, XMLStreamReader.END_ELEMENT, AUDIT_LOG)){
            this.abortParsing();
            return false;
        }
        return true;
    }

    private AuditLogMessage parseAuditLogMessage() throws XMLStreamException, AuditLogParsingException{
        LinkedList<String> names = new LinkedList<String>();
        AuditLogMessage result = new AuditLogMessage();
        while(_stream.hasNext()){
            switch(_stream.nextTag()){
                case XMLStreamReader.START_ELEMENT:
                    switch(_stream.getLocalName()){
                        case MESSAGE_TYPE:
                            result._messageType = _stream.getElementText();
                            break;
                        case MESSAGE_TEXT:
                            result._enText = _stream.getElementText();
                            break;
                        case MESSAGE_NAME:
                            names.add(_stream.getElementText());
                            break;
                        case MESSAGE_REASON:
                            result._reason = _stream.getElementText();
                            break;
                    }
                    //XmlUtil.skip(reader, XMLStreamReader.END_ELEMENT);
                    break;
                case XMLStreamReader.END_ELEMENT:
                    result._names = Collections.unmodifiableList(names);
                    return result;
            }
        }
        throw new AuditLogParsingException("Element end expected.");

    }

    //cleanup method to use in exception handlers
    public void abortParsing(){
        try {
            _stream.close();
            _fileStream.close();
        }
        catch(XMLStreamException e){
            _logger.warn("Exception when trying to close audit log XML stream.", e);
        }
        catch(IOException e){
            _logger.warn("Exception when trying to close audit log XML stream.", e);
        }
    }

    public String getDocumentHash()
    {
        return _documentHash;
    }

    public String getEnRootHash()
    {
        return _enRootHash;
    }


    //--------------------------------------------
    public static class TestCase extends Assert{

        private File _logFile;
        private SkylineAuditLogParser _parser;
        private static Logger _logger;
        public final static String SYS_PROPERTY_CWD = "user.dir";
        public final static String SKYLINE_LOG_EXTENSION = "skyl";

        private static GUID _docGUID = new GUID("add8ea9c-0b32-1037-a00c-1e459cb1acac");

        @BeforeClass
        public static void GetTestFile(){

            _logger = Logger.getLogger(TestCase.class.getPackageName() + ".test");
            UnitTestUtil.cleanupDatabase(_docGUID);
        }

        @Test
        public void testLogParser()  throws XMLStreamException, AuditLogException, AuditLogParsingException, IOException
        {
            List<AuditLogEntry> entries = new LinkedList<>();

            File f_zip = UnitTestUtil.getSampleDataFile("AuditLogFiles/MethodEdit_v6.2.zip");
            _logFile = UnitTestUtil.extractLogFromZip(f_zip, _logger);
            _parser = new SkylineAuditLogParser(_logFile, _logger);
            Assert.assertNotNull(_parser.getEnRootHash());

            AuditLogMessageExpander expander = new AuditLogMessageExpander(_logger);
            AuditLogEntry prevEntry = null;

            while(_parser.hasNextEntry())
            {
                AuditLogEntry ent = _parser.parseLogEntry();
                ent.setDocumentGUID(_docGUID);
                if(prevEntry != null)
                    ent.setParentEntryHash(prevEntry.getEntryHash());
                entries.add(ent.expandEntry(expander));
                _logger.info(ent.toString());
                //all messages in this file should have expanded text
                //ent.persist();

                for(AuditLogMessage msg : ent.getAllInfoMessage())
                {
                    Assert.assertNotNull(msg.getExpandedText());
                    Assert.assertNotNull(msg.getEnText());
                    Assert.assertNotEquals("", msg.getExpandedText());
                }
                prevEntry = ent;
            }
//            Assert.assertEquals("Settings > Peptide Settings -- Library > Libraries : Settings > Peptide Settings -- Library > Libraries",
//                    entries.get(1).getAllInfoMessage().get(2).getExpandedText());


            Assert.assertTrue(expander.areAllMessagesExpanded());
            Assert.assertTrue(expander.areResourcesReady());
            Assert.assertNotNull(entries.get(2).getExtraInfo());
            Assert.assertNull(entries.get(1).getExtraInfo());
//
            Assert.assertEquals(11, entries.size());
            Assert.assertEquals(6, entries.get(5).getAllInfoMessage().size());
            Assert.assertTrue(entries.get(0).canBeHashed(expander));
        }

        @Test
        public void testInvalidXmlFile() {
            try
            {
                File logFile = UnitTestUtil.getSampleDataFile("AuditLogFiles/InvalidSchemaTest.skyl");
                SkylineAuditLogParser parser = new SkylineAuditLogParser(logFile, _logger);
                Assert.assertTrue("Expected file validation failure but it succeeded.", false);
            }
            catch(Exception  e) {
                Assert.assertTrue(true);
                _logger.info(e.toString());
            }
        }

        //TODO: test invalid expansion: missing resource file or missing token in a file.
        @Test
        public void testMissingResourceName() throws XMLStreamException, AuditLogException, AuditLogParsingException, IOException
        {
            _logFile = UnitTestUtil.getSampleDataFile("AuditLogFiles/InvalidResourceTest.skyl");
            _parser = new SkylineAuditLogParser(_logFile, _logger);

            AuditLogMessageExpander expander = new AuditLogMessageExpander(_logger);
            List<AuditLogEntry> entries = new LinkedList<>();

            while(_parser.hasNextEntry())
            {
                AuditLogEntry ent = _parser.parseLogEntry();
                entries.add(ent.expandEntry(expander));
                _logger.info(ent.toString());
            }

            Assert.assertTrue(expander.areResourcesReady());
            Assert.assertFalse(expander.areAllMessagesExpanded());
        }
        //TODO: Validate against different files.
    }


}
