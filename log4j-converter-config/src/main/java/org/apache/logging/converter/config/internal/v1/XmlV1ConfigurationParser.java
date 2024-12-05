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
package org.apache.logging.converter.config.internal.v1;

import static org.apache.logging.converter.config.internal.XmlUtils.throwUnknownElement;
import static org.apache.logging.converter.config.internal.v1.AbstractComponentParser.NAME_ATTR;
import static org.apache.logging.converter.config.internal.v1.AbstractComponentParser.VALUE_ATTR;

import aQute.bnd.annotation.Resolution;
import aQute.bnd.annotation.spi.ServiceProvider;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import javax.xml.parsers.DocumentBuilder;
import org.apache.logging.converter.config.internal.ComponentUtils;
import org.apache.logging.converter.config.internal.ComponentUtils.ConfigurationNodeBuilder;
import org.apache.logging.converter.config.internal.XmlUtils;
import org.apache.logging.converter.config.spi.ConfigurationNode;
import org.apache.logging.converter.config.spi.ConfigurationParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

@ServiceProvider(value = ConfigurationParser.class, resolution = Resolution.MANDATORY)
public class XmlV1ConfigurationParser extends AbstractV1ConfigurationParser {

    private static final String APPENDER_TAG = "appender";
    private static final String APPENDER_REF_TAG = "appender-ref";
    private static final String CONFIGURATION_TAG = "log4j:configuration";
    private static final String OLD_CONFIGURATION_TAG = "configuration";
    private static final String LEVEL_TAG = "level";
    private static final String OLD_LEVEL_TAG = "priority";
    private static final String LOGGER_TAG = "logger";
    private static final String OLD_LOGGER_TAG = "category";
    private static final String ROOT_TAG = "root";

    private static final String ADDITIVITY_ATTR = "additivity";
    private static final String REF_ATTR = "ref";
    private static final String THRESHOLD_ATTR = "threshold";

    private static final String LOG4J_V1_XML_FORMAT = "v1:xml";

    @Override
    public String getInputFormat() {
        return LOG4J_V1_XML_FORMAT;
    }

    @Override
    public String getInputFormatDescription() {
        return "Log4j 1 XML configuration file format.";
    }

    @Override
    public ConfigurationNode parse(InputStream inputStream) throws IOException {
        DocumentBuilder documentBuilder = XmlUtils.createDocumentBuilderV1();
        try {
            Document document = documentBuilder.parse(inputStream);
            return parse(document.getDocumentElement());
        } catch (SAXException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw new IOException("Unable to parse configuration file.", e);
        }
    }

    private ConfigurationNode parse(Element configurationElement) {
        ConfigurationNodeBuilder builder = ComponentUtils.newNodeBuilder().setPluginName("Configuration");
        switch (configurationElement.getTagName()) {
            case CONFIGURATION_TAG:
            case OLD_CONFIGURATION_TAG:
                break;
            default:
                throwUnknownElement(configurationElement);
        }

        String level = configurationElement.getAttribute(THRESHOLD_ATTR);

        ConfigurationNodeBuilder appendersBuilder =
                ComponentUtils.newNodeBuilder().setPluginName("Appenders");

        LoggerConfig rootLogger = new LoggerConfig("");
        rootLogger.setLevel("DEBUG");
        Collection<LoggerConfig> loggerConfigs = new ArrayList<>();
        XmlUtils.childStream(configurationElement).forEach(childElement -> {
            switch (childElement.getTagName()) {
                case APPENDER_TAG:
                    appendersBuilder.addChild(parseAppender(childElement));
                    break;
                case ROOT_TAG:
                    parseLoggerChildren(rootLogger, childElement);
                    break;
                case LOGGER_TAG:
                case OLD_LOGGER_TAG:
                    LoggerConfig loggerConfig = new LoggerConfig(childElement.getAttribute(NAME_ATTR));
                    loggerConfigs.add(parseLoggerChildren(loggerConfig, childElement));
                    break;
                default:
                    throwUnknownElement(childElement);
            }
        });
        builder.addChild(appendersBuilder.get());

        if (!level.isEmpty()) {
            builder.addChild(ComponentUtils.newThresholdFilter(level.toUpperCase(Locale.ROOT)));
        }

        ConfigurationNodeBuilder loggersNodeBuilder =
                ComponentUtils.newNodeBuilder().setPluginName("Loggers");
        loggersNodeBuilder.addChild(rootLogger.buildRoot());
        loggerConfigs.stream().map(LoggerConfig::buildLogger).forEach(loggersNodeBuilder::addChild);
        builder.addChild(loggersNodeBuilder.get());

        return builder.get();
    }

    private ConfigurationNode parseAppender(Element appenderElement) {
        return AbstractComponentParser.parseConfigurationElement(this, appenderElement);
    }

    private LoggerConfig parseLoggerChildren(LoggerConfig loggerConfig, Element element) {
        String additivity = element.getAttribute(ADDITIVITY_ATTR);
        if (!additivity.isEmpty()) {
            loggerConfig.setAdditivity(additivity);
        }
        XmlUtils.forEachChild(element, childElement -> {
            String nodeName = childElement.getTagName();
            switch (nodeName) {
                case APPENDER_REF_TAG:
                    loggerConfig.addAppenderRef(childElement.getAttribute(REF_ATTR));
                    break;
                case LEVEL_TAG:
                case OLD_LEVEL_TAG:
                    loggerConfig.setLevel(childElement.getAttribute(VALUE_ATTR));
                    break;
                default:
                    throwUnknownElement(childElement);
            }
        });
        return loggerConfig;
    }
}
