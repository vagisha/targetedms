/*
 * Copyright (c) 2012-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.targetedms.parser;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.Date;

/**
 * User: vsharma
 * Date: 5/1/12
 * Time: 10:53 PM
 */
public class XmlUtil
{
    private XmlUtil() {}

    public static boolean isStartElement(int eventType)
    {
        return eventType == XMLStreamReader.START_ELEMENT;
    }

    public static boolean isStartElement(XMLStreamReader reader, int eventType, String elementName)
    {
        return eventType == XMLStreamReader.START_ELEMENT && elementName.equalsIgnoreCase(reader.getLocalName());
    }

    public static boolean isElement(XMLStreamReader reader, String elementName)
    {
        return elementName.equalsIgnoreCase(reader.getLocalName());
    }

    public static boolean isEndElement(XMLStreamReader reader, int eventType, String elementName)
    {
        return eventType == XMLStreamReader.END_ELEMENT && elementName.equalsIgnoreCase(reader.getLocalName());
    }

    public static boolean isText(int eventType)
    {
        return eventType == XMLStreamReader.CHARACTERS;
    }

    public static Double readDouble(XMLStreamReader reader, String endElementName) throws XMLStreamException
    {
        StringBuilder sb = new StringBuilder();
        while (reader.hasNext())
        {
            int evtType = reader.next();
            if (evtType == XMLStreamReader.END_ELEMENT && endElementName.equalsIgnoreCase(reader.getLocalName()))
            {
                if (sb.length() == 0)
                {
                    return null;
                }
                return Double.parseDouble(sb.toString().trim());
            }
            if (evtType == XMLStreamReader.CHARACTERS)
            {
                sb.append(reader.getText());
            }
            if (evtType == XMLStreamReader.START_ELEMENT)
            {
                throw new IllegalStateException("Did not expect a nested child element for " + endElementName);
            }
        }
        return null;
    }

    public static Double readDoubleAttribute(XMLStreamReader reader, String attributeName)
    {
        return readDoubleAttribute(reader, attributeName, null);
    }

    public static Double readDoubleAttribute(XMLStreamReader reader, String attributeName, @Nullable Double defaultValue)
    {
        String value = reader.getAttributeValue(null, attributeName);
        if (value == null)
            return defaultValue;
        return Double.parseDouble(value.trim());
    }

    public static double readRequiredDoubleAttribute(XMLStreamReader reader, String attributeName, String elementName)
    {
        String value = readRequiredAttribute(reader, attributeName, elementName);
        return Double.parseDouble(value.trim());
    }

    public static Integer readIntegerAttribute(XMLStreamReader reader, String attributeName)
    {
        return readIntegerAttribute(reader, attributeName, null);
    }

    public static Integer readIntegerAttribute(XMLStreamReader reader, String attributeName, @Nullable Integer defaultValue)
    {
        String value = reader.getAttributeValue(null, attributeName);
        if (value == null)
            return defaultValue;
        return Integer.parseInt(value.trim());
    }

    public static Integer readRequiredIntegerAttribute(XMLStreamReader reader, String attributeName, String elementName)
    {
        String value = readRequiredAttribute(reader, attributeName, elementName);
        return Integer.parseInt(value.trim());
    }

    public static Date readDateAttribute(XMLStreamReader reader, String attributeName)
    {
        String value = reader.getAttributeValue(null, attributeName);
        if (value == null)
            return null;
        // Calendar calendar = DatatypeConverter.parseDateTime(value);
        return (Date) ConvertUtils.convert(value, Date.class);
    }

    public static String readRequiredAttribute(XMLStreamReader reader, String attributeName, String elementName)
    {
        String value = reader.getAttributeValue(null, attributeName);
        if (value == null)
        {
            throw new IllegalStateException("Missing attribute " + attributeName + " for element " + elementName + ".");
        }
        if (StringUtils.isBlank(value))
        {
            throw new IllegalStateException("Attribute " + attributeName + " for element " + elementName + " cannot be empty.");
        }
        return value;
    }

    public static Boolean readBooleanAttribute(XMLStreamReader reader, String attributeName)
    {
        return readBooleanAttribute(reader, attributeName, null);
    }

    public static Boolean readBooleanAttribute(XMLStreamReader reader, String attributeName, @Nullable Boolean defaultValue)
    {
        String value = reader.getAttributeValue(null, attributeName);
        if (value == null)
            return defaultValue;
        return Boolean.parseBoolean(value);
    }

    public static String readAttribute(XMLStreamReader reader, String attributeName)
    {
        return readAttribute(reader, attributeName, null);
    }

    public static String readAttribute(XMLStreamReader reader, String attributeName, @Nullable String defaultValue)
    {
        String value = reader.getAttributeValue(null, attributeName);
        if (value == null)
            return defaultValue;
        return value;
    }


    public static void skip(XMLStreamReader reader, int eventType) throws XMLStreamException {
        skip(reader, eventType, null);
    }

    public static void skip(XMLStreamReader reader, int eventType, String name) throws XMLStreamException {
        var evt = reader.getEventType();
        while (evt != eventType || (name != null &&
                ((eventType == XMLStreamReader.START_ELEMENT || eventType == XMLStreamReader.END_ELEMENT || eventType == XMLStreamReader.ENTITY_REFERENCE)
                        && !name.equals(reader.getLocalName())))) {
            evt = reader.next();
        }
    }

}
