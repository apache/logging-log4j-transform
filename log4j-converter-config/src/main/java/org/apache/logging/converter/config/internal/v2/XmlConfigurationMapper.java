/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.converter.config.internal.v2;

import aQute.bnd.annotation.Resolution;
import aQute.bnd.annotation.spi.ServiceProvider;
import com.sun.xml.txw2.output.IndentingXMLStreamWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.logging.converter.config.ConfigurationConverterException;
import org.apache.logging.converter.config.internal.ConfigurationNodeImpl;
import org.apache.logging.converter.config.spi.ConfigurationMapper;
import org.apache.logging.converter.config.spi.ConfigurationNode;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

@ServiceProvider(value = ConfigurationMapper.class, resolution = Resolution.MANDATORY)
public class XmlConfigurationMapper implements ConfigurationMapper {

    private static final String DISABLE_DOCTYPE_DECLARATION = "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String LOG4J_NAMESPACE_URI = "https://logging.apache.org/xml/ns";
    private static final String LOG4J_SCHEMA_LOCATION =
            LOG4J_NAMESPACE_URI + " https://logging.apache.org/xml/ns/log4j-config-2.xsd";
    private static final String LOG4J_V2_XML_FORMAT = "v2:xml";

    @Override
    public ConfigurationNode parse(InputStream inputStream) throws IOException {
        DocumentBuilder documentBuilder = createDocumentBuilder();
        // The default error handler pollutes `System.err`
        documentBuilder.setErrorHandler(new ThrowingErrorHandler());
        try {
            Document document = documentBuilder.parse(inputStream);
            Element configurationElement = document.getDocumentElement();
            if (!isLog4jNamespace(configurationElement)) {
                throw new ConfigurationConverterException("Wrong configuration file namespace: expecting `"
                        + LOG4J_NAMESPACE_URI + "`, found `" + configurationElement.getNamespaceURI() + "`");
            }
            return parseComplexElement(configurationElement);
        } catch (SAXException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw new IOException("Unable to parse configuration file.", e);
        }
    }

    @Override
    public void writeConfiguration(OutputStream outputStream, ConfigurationNode configuration) throws IOException {
        XMLStreamWriter streamWriter = createStreamWriter(outputStream);
        try {
            streamWriter.writeStartDocument();
            streamWriter.setDefaultNamespace(LOG4J_NAMESPACE_URI);
            streamWriter.writeStartElement(LOG4J_NAMESPACE_URI, configuration.getPluginName());
            // Register the namespaces
            streamWriter.writeDefaultNamespace(LOG4J_NAMESPACE_URI);
            streamWriter.writeNamespace("xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
            streamWriter.writeAttribute(
                    XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "schemaLocation", LOG4J_SCHEMA_LOCATION);
            // Write the content of the file
            writeNodeContentToStreamWriter(configuration, streamWriter);
            streamWriter.writeEndElement();
            streamWriter.writeEndDocument();
            streamWriter.flush();
        } catch (XMLStreamException e) {
            throw new IOException("Unable to write configuration.", e);
        } finally {
            closeStreamWriter(streamWriter);
        }
    }

    private static void closeStreamWriter(XMLStreamWriter streamWriter) throws IOException {
        try {
            streamWriter.close();
        } catch (XMLStreamException e) {
            throw new IOException("Unable to close stream writer.", e);
        }
    }

    @Override
    public String getFormat() {
        return LOG4J_V2_XML_FORMAT;
    }

    private static DocumentBuilder createDocumentBuilder() throws IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        enableFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING);
        enableFeature(factory, DISABLE_DOCTYPE_DECLARATION);
        try {
            factory.setXIncludeAware(false);
        } catch (UnsupportedOperationException e) {
            throw new IOException(
                    "Failed to disable XInclude on DocumentBuilderFactory "
                            + factory.getClass().getName(),
                    e);
        }
        try {
            return factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IOException(
                    "Failed to create DocumentBuilder using DocumentBuilderFactory "
                            + factory.getClass().getName(),
                    e);
        }
    }

    private static XMLStreamWriter createStreamWriter(OutputStream outputStream) throws IOException {
        XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
        try {
            outputFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);
            return new IndentingXMLStreamWriter(outputFactory.createXMLStreamWriter(outputStream));
        } catch (XMLStreamException e) {
            throw new IOException(
                    "Failed to create XMLStreamWriter using XMLOutputFactory "
                            + outputFactory.getClass().getName(),
                    e);
        }
    }

    private static void enableFeature(DocumentBuilderFactory factory, String feature) throws IOException {
        try {
            factory.setFeature(feature, true);
        } catch (ParserConfigurationException e) {
            throw new IOException(
                    "Failed to enable '" + feature + "' feature on DocumentBuilderFactory "
                            + factory.getClass().getName(),
                    e);
        }
    }

    /**
     * Transforms an XML element into a Log4j configuration node.
     */
    private static ConfigurationNode parseComplexElement(Element element) {
        ConfigurationNodeImpl.NodeBuilder builder = ConfigurationNodeImpl.newNodeBuilder();
        // Handle child elements
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            processChildElement(childNodes.item(i), builder);
        }
        // Handle child attributes
        NamedNodeMap nodeMap = element.getAttributes();
        processAttributes(nodeMap, builder);
        return builder.setPluginName(element.getTagName()).build();
    }

    /**
     * Handles child nodes of an XML element.
     *
     * @param childNode An XML node.
     * @param builder A {@link ConfigurationNode} builder.
     */
    private static void processChildElement(org.w3c.dom.Node childNode, ConfigurationNodeImpl.NodeBuilder builder) {
        if (isLog4jNamespace(childNode) && childNode instanceof Element) {
            Element childElement = (Element) childNode;
            if (isComplexElement(childElement)) {
                builder.addChild(parseComplexElement(childElement));
                return;
            }
            String value = getSimpleElementValue(childElement);
            // An empty Simple Element is probably a plugin, e.g. `<JsonTemplateLayout/>`
            if (value.isEmpty()) {
                builder.addChild(parseComplexElement(childElement));
            } else {
                builder.addAttribute(childElement.getNodeName(), value);
            }
        }
    }

    /**
     * Checks if an element is an XSD Complex Element
     *
     * @return {@code true} if the element has child elements or attributes.
     */
    private static boolean isComplexElement(org.w3c.dom.Node element) {
        NodeList nodeList = element.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (nodeList.item(i) instanceof Element) {
                return true;
            }
        }
        return element.getAttributes().getLength() > 0;
    }

    private static String getSimpleElementValue(org.w3c.dom.Node element) {
        String value = element.getTextContent();
        return value == null ? "" : value.trim();
    }

    /**
     * Handles attributes of an XML element.
     *
     * @param nodeMap A collection of XML attributes.
     * @param builder A {@link ConfigurationNode} builder.
     */
    private static void processAttributes(NamedNodeMap nodeMap, ConfigurationNodeImpl.NodeBuilder builder) {
        for (int i = 0; i < nodeMap.getLength(); i++) {
            org.w3c.dom.Node item = nodeMap.item(i);
            if (isLog4jNamespace(item) && item instanceof Attr) {
                Attr attr = (Attr) item;
                builder.addAttribute(attr.getName(), attr.getValue());
            }
        }
    }

    /**
     * Writes the content of {@code node} to the {@link XMLStreamWriter}.
     * <p>
     *     This method assumes that the {@link XMLStreamWriter#writeStartElement} and {@link XMLStreamWriter#writeEndElement()}
     *     are handled by the caller.
     * </p>
     */
    private static void writeNodeContentToStreamWriter(
            ConfigurationNode configurationNode, XMLStreamWriter streamWriter) throws XMLStreamException {
        for (Map.Entry<String, String> attribute :
                configurationNode.getAttributes().entrySet()) {
            streamWriter.writeAttribute(attribute.getKey(), attribute.getValue());
        }
        for (ConfigurationNode child : configurationNode.getChildren()) {
            // Empty element with attributes
            if (child.getChildren().isEmpty()) {
                streamWriter.writeEmptyElement(LOG4J_NAMESPACE_URI, child.getPluginName());
                writeNodeContentToStreamWriter(child, streamWriter);
            } else {
                streamWriter.writeStartElement(LOG4J_NAMESPACE_URI, child.getPluginName());
                writeNodeContentToStreamWriter(child, streamWriter);
                streamWriter.writeEndElement();
            }
        }
    }

    private static boolean isLog4jNamespace(Node node) {
        @Nullable String namespaceUri = node.getNamespaceURI();
        return namespaceUri == null || namespaceUri.equals(LOG4J_NAMESPACE_URI);
    }

    private static class ThrowingErrorHandler implements ErrorHandler {
        @Override
        public void warning(SAXParseException exception) {}

        @Override
        public void error(SAXParseException exception) throws SAXException {
            throwOnParseException(exception);
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            throwOnParseException(exception);
        }

        private void throwOnParseException(SAXParseException exception) throws SAXException {
            IOException ioException = new IOException(
                    String.format(
                            "Invalid configuration file content at line %d, column %d: %s",
                            exception.getLineNumber(), exception.getColumnNumber(), exception.getMessage()),
                    exception);
            throw new SAXException(ioException);
        }
    }
}
