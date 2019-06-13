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
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.resource.FileResource;
import org.labkey.api.util.Path;
import org.labkey.targetedms.TargetedMSModule;
import org.labkey.targetedms.parser.XmlUtil;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/***
 * Class to handle expansion of the messages in case they do not have pre-expanded english text
 */
public class AuditLogMessageExpander
{
    private Map<String, Map<String, String>> _resources;
    private ArrayList<UnaryOperator<String>> _parserFunctions;
    private ArrayList<Predicate<String>> _checkerFunctions;
    private Logger _logger;
    private AuditLogResourceLoader _loader;
    private static final Pattern _formatMatch = Pattern.compile("\\{(([0-9]+):([a-zA-Z0-9_]+))\\}");
    private static final  Pattern _cSharpFormatMatch = Pattern.compile("\\{([0-9]+)\\}");
    private boolean _allMessagesExpanded = true;


    public AuditLogMessageExpander(Logger p_logger){
        _logger = p_logger;
        _loader = new AuditLogResourceLoader(_logger);
        _resources = _loader.loadResources();

        _parserFunctions = new ArrayList<UnaryOperator<String>>() {
            {
                add((s) -> getResource(AuditLogResourceLoader.PROPERTIES, s));
                add((s) -> getResource(AuditLogResourceLoader.PROPERTY_ELEMENTS, s));
                add((s) -> getResource(AuditLogResourceLoader.LOG_STRINGS, s));
                add((s) -> parsePrimitive(s));
                add((s) -> s);      //Parse path
                add((s) -> getResource(AuditLogResourceLoader.COLUMN_CAPTIONS, s));
                add((s) -> getResource(AuditLogResourceLoader.ENUMS, s));
            }
        };

        _checkerFunctions = new ArrayList<Predicate<String>>() {
            {
                add((s) -> _resources.get(AuditLogResourceLoader.PROPERTIES).containsKey(s));
                add((s) -> _resources.get(AuditLogResourceLoader.PROPERTY_ELEMENTS).containsKey(s));
                add((s) -> _resources.get(AuditLogResourceLoader.LOG_STRINGS).containsKey(s));
                add((s) -> true);
                add((s) -> true);      //Parse path
                add((s) -> _resources.get(AuditLogResourceLoader.COLUMN_CAPTIONS).containsKey(s));
                add((s) -> _resources.get(AuditLogResourceLoader.ENUMS).containsKey(s));
            }
        };
    }

    private String getResource(String p_resourceName, String p_elementName){
        Map<String, String> resource = _resources.get(p_resourceName);
        if(resource.containsKey(p_elementName))
            return resource.get(p_elementName);
        else
        {
            _allMessagesExpanded = false;
            return p_elementName;
        }
    }

    private String parsePrimitive(String str) {
        Boolean b = tryParseBoolean(str);
        if (b != null) {
            return b.toString();
        }

        // No point in parsing the string as double/int, since we have no control over the toString culture
        return Quote(str);
    }
    private Boolean tryParseBoolean(String str) {
        if ("True".equals(str))
            return true;
        else if ("False".equals(str))
            return false;
        return null;
    }
    private static Integer tryParseInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String Quote(String str) {
        if (str == null)
            return null;

        return String.format("\"%s\"", str);
    }

    /**
     * Converting C# string format specifications into Java ones
     * @param s     a string containing format specifications
     * @return      string with equivalent java formats.
     */
    private String cSharpToJavaFormatString(String s) {

        if (s == null || s.isEmpty())
            return s;

        Matcher match = _cSharpFormatMatch.matcher(s);
        StringBuilder resultBuilder = new StringBuilder();
        boolean hasMatches = false;
        int last_pos = 0;
        while(match.find()){
            resultBuilder.append(s.substring(last_pos, match.start()));
            resultBuilder.append( String.format("%%%s$s", match.group(1)));
            last_pos = match.end();
            hasMatches = true;
        }
        if(hasMatches)
            return resultBuilder.toString();
        else
            return s;
    }

    public boolean needsExpansion(String str){
        if(str != null)
        {
            Matcher match = _formatMatch.matcher(str);
            return match.find();
        }
        else
            return false;
    }

    public String expandLogString(String str) {
        if (str == null || str.isEmpty())
            return str;

        StringBuilder resultBuilder = new StringBuilder();
        int last_pos = 0;
        boolean hasMatches = false;
        //find all possible expansion tokens
        Matcher match = _formatMatch.matcher(str);
        while(match.find())
        {
            resultBuilder.append(str.substring(last_pos, match.start()));
            //verify that function index is correct and property is in the file
            int functionIndex = Integer.parseInt(match.group(2));
            if (functionIndex <= 6 && _checkerFunctions.get(functionIndex).test(match.group(3)))
            {
                resultBuilder.append(
                        _parserFunctions.get(functionIndex).apply(match.group(3))
                );
                hasMatches = true;
            }
            else{
                _allMessagesExpanded = false;
                _logger.warn(String.format("Audit log expansion token %s cannot be expanded. Either invalid function index or unknown name.", match.group(0)));
                resultBuilder.append(match.group(0));
            }
            last_pos = match.end();
        }
        if(!hasMatches)    //no format expressions, returning the string as is.
            return str;

        return resultBuilder.toString();
    }

    public AuditLogMessage expandMessage(AuditLogMessage msg){

        Object[] parsedNames = msg
                .getNames()
                .stream()
                .map(n -> expandLogString(n))
                .collect(Collectors.toList()).toArray();

        if(_resources.containsKey(AuditLogResourceLoader.LOG_STRINGS))
        {
            Map<String, String> resource = _resources.get(AuditLogResourceLoader.LOG_STRINGS);
            if (resource.containsKey(msg.getMessageType()))
            {
                if(resource.containsKey(msg.getMessageType())){
                    String cSharpFmt = resource.get(msg.getMessageType());
                    String format = cSharpToJavaFormatString(cSharpFmt);
                    msg.setExpandedText(String.format(format, parsedNames));
                }
                else
                    _allMessagesExpanded = false;
            }
        }
        else
            _allMessagesExpanded = false;

        return msg;
    }

    public boolean areResourcesReady() {return _loader.areResourcesReady();}
    public boolean areAllMessagesExpanded() {return _allMessagesExpanded;}

    public class AuditLogResourceLoader
    {

        private static final String DATA = "data";
        private static final String VALUE = "value";
        private static final String NAME = "name";
        private static final String COMMENT = "comment";

        public static final String PROPERTIES = "PropertyNames";
        public static final String PROPERTY_ELEMENTS = "PropertyElementNames";
        public static final String LOG_STRINGS = "AuditLogStrings";
        public static final String COLUMN_CAPTIONS = "ColumnCaptions";
        public static final String ENUMS = "EnumNames";

        public String[] RESOURCE_NAMES = new String[]{ "PropertyNames", "PropertyElementNames", "AuditLogStrings", "ColumnCaptions", "EnumNames" };
        private Logger _logger;
        private boolean _resourcesReady = true;


        public AuditLogResourceLoader(Logger p_logger){
            _logger = p_logger;
        }

        public Map<String, Map<String, String>> loadResources() 

    {
            Map<String, Map<String, String>> result = new HashMap<>();

            for (String resName : RESOURCE_NAMES)
            {
                String resPath = String.format("skyaudit/%s.resx", resName);
                File resourceFile = null;

                try
                {
                    if (ModuleLoader.getInstance() != null)
                    {   //if we are running web test
                        Module module = ModuleLoader.getInstance().getModule(TargetedMSModule.class);
                        FileResource resource = (FileResource) module.getModuleResolver().lookup(Path.parse(resPath));
                        if (resource == null)
                        {
                            _logger.warn(String.format("Cannot find required resource %s. Log file messages will not be expanded.", resName));
                        }
                        else
                            resourceFile = resource.getFile();
                    }
                    else //this is for unit testing
                        resourceFile = UnitTestUtil.getResourcesFile(resPath);

                    if(resourceFile != null)
                    {
                        result.put(resName, this.parseResource(resourceFile));
                    }
                    else
                        _resourcesReady = false;

                }
                catch(Exception e)
                {
                    _logger.warn(String.format("Cannot parse required resource %s. Log file messages will not be expanded.", resName));
                    _logger.warn(e.toString());
                    _resourcesReady = false;
                }
            }
            return result;
        }

        /**
         * Parses the resource XML file and returns key-value map of the resource
         * @param p_file
         * @return
         * @throws IOException
         * @throws XMLStreamException
         */
        private Map<String, String> parseResource(File p_file)  throws IOException, XMLStreamException
        {
            Map<String, String> result = new HashMap<>();

            var stream = new FileInputStream(p_file);
            var inputFactory = XMLInputFactory.newInstance();
            var reader = inputFactory.createXMLStreamReader(stream);

            if (!reader.hasNext())
                throw new IllegalStateException("Invalid resx file");

            int evt = reader.next();
            while(true) {
                if (XmlUtil.isStartElement(reader, evt, DATA)) {
                    var name = XmlUtil.readRequiredAttribute(reader, NAME, DATA);
                    var resourceString = readDataElement(reader);
                    result.put(name, resourceString);
                }

                if (reader.hasNext())
                    evt = reader.next();
                else
                    break;
            }

            stream.close();

            return result;
        }

        private String readDataElement(XMLStreamReader p_reader) throws XMLStreamException {
            XmlUtil.skip(p_reader, XMLStreamReader.START_ELEMENT, VALUE);
            var result = p_reader.getElementText();

            XmlUtil.skip(p_reader, XMLStreamReader.END_ELEMENT, VALUE);
            p_reader.next();
            XmlUtil.skip(p_reader, XMLStreamReader.END_ELEMENT, DATA);
            int evt = p_reader.next();

            if (XmlUtil.isStartElement(p_reader, evt, COMMENT)) {
                XmlUtil.skip(p_reader, XMLStreamReader.END_ELEMENT, COMMENT);
                p_reader.next();
            }
            return result;
        }


        public boolean areResourcesReady()
        {
            return _resourcesReady;
        }
    }

}
