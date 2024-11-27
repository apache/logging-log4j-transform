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
package org.apache.logging.converter.config.internal;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public final class XmlUtils {

    private static final String DISABLE_DOCTYPE_DECLARATION = "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";
    private static final String EXTERNAL_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities";
    private static final String LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    public static DocumentBuilder createDocumentBuilderV1() throws IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        enableFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING);
        disableFeature(factory, EXTERNAL_GENERAL_ENTITIES);
        disableFeature(factory, EXTERNAL_PARAMETER_ENTITIES);
        disableFeature(factory, LOAD_EXTERNAL_DTD);
        disableXIncludeAware(factory);
        factory.setExpandEntityReferences(false);
        factory.setValidating(false);
        return newDocumentBuilder(factory);
    }

    public static DocumentBuilder createDocumentBuilderV2() throws IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        enableFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING);
        enableFeature(factory, DISABLE_DOCTYPE_DECLARATION);
        disableXIncludeAware(factory);
        return newDocumentBuilder(factory);
    }

    private static void disableXIncludeAware(DocumentBuilderFactory factory) throws IOException {
        try {
            factory.setXIncludeAware(false);
        } catch (UnsupportedOperationException e) {
            throw new IOException(
                    "Failed to disable XInclude on DocumentBuilderFactory "
                            + factory.getClass().getName(),
                    e);
        }
    }

    private static DocumentBuilder newDocumentBuilder(DocumentBuilderFactory factory) throws IOException {
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            // The default error handler pollutes `System.err`
            builder.setErrorHandler(new ThrowingErrorHandler());
            return builder;
        } catch (ParserConfigurationException e) {
            throw new IOException(
                    "Failed to create DocumentBuilder using DocumentBuilderFactory "
                            + factory.getClass().getName(),
                    e);
        }
    }

    public static Stream<Element> childStream(Node parent) {
        NodeList list = parent.getChildNodes();
        return IntStream.range(0, list.getLength())
                .mapToObj(list::item)
                .filter(node -> node.getNodeType() == Node.ELEMENT_NODE)
                .map(Element.class::cast);
    }

    public static void forEachChild(Node parent, Consumer<? super Element> consumer) {
        childStream(parent).forEach(consumer);
    }

    private static void enableFeature(DocumentBuilderFactory factory, String feature) throws IOException {
        setFeature(factory, feature, true);
    }

    private static void disableFeature(DocumentBuilderFactory factory, String feature) throws IOException {
        setFeature(factory, feature, false);
    }

    private static void setFeature(DocumentBuilderFactory factory, String feature, boolean value) throws IOException {
        try {
            factory.setFeature(feature, value);
        } catch (ParserConfigurationException e) {
            throw new IOException(
                    "Failed to enable '" + feature + "' feature on DocumentBuilderFactory "
                            + factory.getClass().getName(),
                    e);
        }
    }

    private XmlUtils() {}

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
