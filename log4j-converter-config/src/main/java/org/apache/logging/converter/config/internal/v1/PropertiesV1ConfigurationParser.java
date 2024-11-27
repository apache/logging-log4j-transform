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

import static org.apache.logging.converter.config.internal.PropertiesUtils.extractProperty;
import static org.apache.logging.converter.config.internal.PropertiesUtils.extractSubset;
import static org.apache.logging.converter.config.internal.PropertiesUtils.partitionOnCommonPrefixes;

import aQute.bnd.annotation.Resolution;
import aQute.bnd.annotation.spi.ServiceProvider;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.logging.converter.config.ConfigurationConverterException;
import org.apache.logging.converter.config.internal.ComponentUtils;
import org.apache.logging.converter.config.internal.ComponentUtils.ConfigurationNodeBuilder;
import org.apache.logging.converter.config.internal.PropertiesUtils;
import org.apache.logging.converter.config.spi.ConfigurationNode;
import org.apache.logging.converter.config.spi.ConfigurationParser;
import org.apache.logging.converter.config.spi.v1.PropertiesSubset;

@ServiceProvider(value = ConfigurationParser.class, resolution = Resolution.MANDATORY)
public class PropertiesV1ConfigurationParser extends AbstractV1ConfigurationParser {

    private static final String ADDITIVITY_PREFIX = "log4j.additivity";
    private static final String APPENDER_PREFIX = "log4j.appender";
    private static final String CATEGORY_PREFIX = "log4j.category";
    private static final String LOGGER_PREFIX = "log4j.logger";

    private static final String ROOT_CATEGORY_KEY = "log4j.rootCategory";
    private static final String ROOT_LOGGER_KEY = "log4j.rootLogger";
    private static final String THRESHOLD_KEY = "log4j.threshold";

    private static final String LOG4J_V1_PROPERTIES_FORMAT = "v1:properties";

    @Override
    public String getInputFormat() {
        return LOG4J_V1_PROPERTIES_FORMAT;
    }

    @Override
    public ConfigurationNode parse(InputStream inputStream) throws IOException {
        Properties properties = new Properties();
        properties.load(inputStream);
        return parse(PropertiesSubset.of("", properties));
    }

    private ConfigurationNode parse(PropertiesSubset properties) {
        ConfigurationNodeBuilder builder = ComponentUtils.newNodeBuilder().setPluginName("Configuration");

        String level = extractProperty(properties, THRESHOLD_KEY);

        PropertiesSubset appendersProperties = extractSubset(properties, APPENDER_PREFIX);
        ConfigurationNode appenders = parseAppenders(appendersProperties);

        ConfigurationNodeBuilder loggersNodeBuilder =
                ComponentUtils.newNodeBuilder().setPluginName("Loggers");
        loggersNodeBuilder.addChild(parseRootLogger(properties));
        parseLoggers(properties).forEach(loggersNodeBuilder::addChild);
        ConfigurationNode loggers = loggersNodeBuilder.get();

        // Whatever is left, are user properties
        builder.addChild(parseProperties(properties));
        builder.addChild(appenders);
        if (level != null) {
            builder.addChild(ComponentUtils.newThresholdFilter(level));
        }
        builder.addChild(loggers);
        return builder.get();
    }

    private ConfigurationNode parseAppenders(PropertiesSubset appendersProperties) {
        ConfigurationNodeBuilder appendersBuilder =
                ComponentUtils.newNodeBuilder().setPluginName("Appenders");
        partitionOnCommonPrefixes(appendersProperties)
                .forEach(appenderProperties -> appendersBuilder.addChild(
                        AbstractComponentParser.parseConfigurationElement(this, appenderProperties)));
        return appendersBuilder.get();
    }

    private ConfigurationNode parseRootLogger(PropertiesSubset globalProperties) {
        PropertiesSubset rootProperties = extractSubset(globalProperties, ROOT_LOGGER_KEY);
        String levelAndRefs = extractProperty(rootProperties, "");
        PropertiesUtils.throwIfNotEmpty(rootProperties);
        // Check rootCategory
        rootProperties = extractSubset(rootProperties, ROOT_CATEGORY_KEY);
        if (levelAndRefs == null) {
            levelAndRefs = extractProperty(globalProperties, "");
        }
        PropertiesUtils.throwIfNotEmpty(rootProperties);
        if (levelAndRefs == null) {
            throw new ConfigurationConverterException("No root logger configuration found!");
        }
        LoggerConfig loggerConfig = new LoggerConfig("");
        loggerConfig.setLevelAndRefs(levelAndRefs);
        return loggerConfig.buildRoot();
    }

    private List<ConfigurationNode> parseLoggers(PropertiesSubset globalProperties) {
        List<ConfigurationNode> loggers = new ArrayList<>();
        Map<String, LoggerConfig> loggerConfigs = new HashMap<>();
        // Handle `log4j.logger`
        extractSubset(globalProperties, LOGGER_PREFIX)
                .getProperties()
                .forEach((key, levelAndRefs) -> loggerConfigs.compute((String) key, (name, oldConfig) -> {
                    LoggerConfig config = new LoggerConfig(name);
                    config.setLevelAndRefs((String) levelAndRefs);
                    return config;
                }));
        // Handler `log4j.catetory`
        extractSubset(globalProperties, CATEGORY_PREFIX)
                .getProperties()
                .forEach((key, levelAndRefs) -> loggerConfigs.compute((String) key, (name, oldConfig) -> {
                    if (oldConfig != null) {
                        throw new ConfigurationConverterException(String.format(
                                "Configuration file contains both a '%s.%s' and '%s.%s' key.",
                                LOGGER_PREFIX, key, CATEGORY_PREFIX, key));
                    }
                    LoggerConfig config = new LoggerConfig(name);
                    config.setLevelAndRefs((String) levelAndRefs);
                    return config;
                }));
        // Handle `log4j.additivity`
        extractSubset(globalProperties, ADDITIVITY_PREFIX)
                .getProperties()
                .forEach((key, additivity) -> loggerConfigs.compute((String) key, (name, oldConfig) -> {
                    LoggerConfig config = oldConfig != null ? oldConfig : new LoggerConfig(name);
                    config.setAdditivity((String) additivity);
                    return config;
                }));
        loggerConfigs.values().stream().map(LoggerConfig::buildLogger).forEach(loggers::add);
        return loggers;
    }

    private ConfigurationNode parseProperties(PropertiesSubset globalProperties) {
        ConfigurationNodeBuilder propertiesBuilder =
                ComponentUtils.newNodeBuilder().setPluginName("Properties");
        globalProperties.getProperties().forEach((name, value) -> {
            ConfigurationNodeBuilder builder = ComponentUtils.newNodeBuilder()
                    .setPluginName("Property")
                    .addAttribute("name", (String) name)
                    .addAttribute("value", (String) value);
            propertiesBuilder.addChild(builder.get());
        });
        return propertiesBuilder.get();
    }
}
