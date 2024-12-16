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

import static org.apache.logging.converter.config.internal.ComponentUtils.newNodeBuilder;
import static org.apache.logging.converter.config.internal.PropertiesUtils.extractProperty;
import static org.apache.logging.converter.config.internal.PropertiesUtils.extractSubset;
import static org.apache.logging.converter.config.internal.PropertiesUtils.isNotEmpty;
import static org.apache.logging.converter.config.internal.PropertiesUtils.partitionOnCommonPrefixes;

import aQute.bnd.annotation.Resolution;
import aQute.bnd.annotation.spi.ServiceProvider;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.converter.config.ConfigurationConverterException;
import org.apache.logging.converter.config.internal.ComponentUtils;
import org.apache.logging.converter.config.internal.ComponentUtils.ConfigurationNodeBuilder;
import org.apache.logging.converter.config.internal.PropertiesUtils;
import org.apache.logging.converter.config.spi.ConfigurationNode;
import org.apache.logging.converter.config.spi.ConfigurationParser;
import org.apache.logging.converter.config.spi.PropertiesSubset;
import org.apache.logging.log4j.util.Strings;

@ServiceProvider(value = ConfigurationParser.class, resolution = Resolution.MANDATORY)
public class PropertiesV2ConfigurationParser implements ConfigurationParser {

    private static final String LOG4J_V2_PROPERTIES_FORMAT = "v2:properties";

    private static final String INT_LEVEL_ATTRIBUTE = "intLevel";
    private static final String LEVEL_AND_REFS_ATTRIBUTE = "levelAndRefs";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String REF_ATTRIBUTE = "ref";
    private static final String TYPE_ATTRIBUTE = "type";
    private static final String VALUE_ATTRIBUTE = "value";

    private static final String APPENDERS_PLUGIN_NAME = "Appenders";
    private static final String CONFIGURATION_PLUGIN_NAME = "Configuration";
    private static final String CUSTOM_LEVEL_PLUGIN_NAME = "CustomLevel";
    private static final String CUSTOM_LEVELS_PLUGIN_NAME = "CustomLevels";
    private static final String APPENDER_REF_PLUGIN_NAME = "AppenderRef";
    private static final String ASYNC_LOGGER_PLUGIN_NAME = "AsyncLogger";
    private static final String ASYNC_ROOT_PLUGIN_NAME = "AsyncRoot";
    private static final String LOGGER_PLUGIN_NAME = "Logger";
    private static final String LOGGERS_PLUGIN_NAME = "Loggers";
    private static final String PROPERTIES_PLUGIN_NAME = "Properties";
    private static final String PROPERTY_PLUGIN_NAME = "Property";
    private static final String ROOT_PLUGIN_NAME = "Root";
    private static final String SCRIPTS_PLUGIN_NAME = "Scripts";

    private static final String COMMA_SEPARATOR = "\\s*,\\s*";

    @Override
    public String getInputFormat() {
        return LOG4J_V2_PROPERTIES_FORMAT;
    }

    @Override
    public String getInputFormatDescription() {
        return "Log4j Core 2 Properties configuration format.";
    }

    @Override
    public ConfigurationNode parse(InputStream inputStream) throws IOException {
        Properties props = new Properties();
        props.load(inputStream);
        PropertiesSubset rootProperties = PropertiesSubset.of("", props);
        ConfigurationNodeBuilder builder = newNodeBuilder().setPluginName(CONFIGURATION_PLUGIN_NAME);

        PropertiesSubset propertyPlaceholders = extractSubset(rootProperties, "property");
        if (isNotEmpty(propertyPlaceholders)) {
            builder.addChild(processPropertyPlaceholders(propertyPlaceholders));
        }

        PropertiesSubset scriptProperties = extractSubset(rootProperties, "script");
        if (isNotEmpty(scriptProperties)) {
            builder.addChild(processScripts(scriptProperties));
        }
        PropertiesUtils.throwIfNotEmpty(scriptProperties);

        PropertiesSubset customLevels = extractSubset(rootProperties, "customLevel");
        if (isNotEmpty(customLevels)) {
            builder.addChild(processCustomLevels(customLevels));
        }
        PropertiesUtils.throwIfNotEmpty(customLevels);

        // Filters
        PropertiesSubset filtersProperties = extractSubset(rootProperties, "filter");
        String filterNames = extractProperty(rootProperties, "filters");
        final Stream<? extends PropertiesSubset> filtersStream = filterNames != null
                ? partitionOnGivenPrefixes(filtersProperties, filterNames)
                : partitionOnCommonPrefixes(filtersProperties);
        Collection<ConfigurationNode> filters =
                filtersStream.map(p -> processGenericComponent("filter", p)).collect(Collectors.toList());
        if (!filters.isEmpty()) {
            builder.addChild(wrapFilters(filters));
        }
        PropertiesUtils.throwIfNotEmpty(filtersProperties);

        // Appenders
        PropertiesSubset appendersProperties = extractSubset(rootProperties, "appender");
        String appenderNames = extractProperty(rootProperties, "appenders");
        final Stream<? extends PropertiesSubset> appendersStream = appenderNames != null
                ? partitionOnGivenPrefixes(appendersProperties, appenderNames)
                : partitionOnCommonPrefixes(appendersProperties);
        Collection<ConfigurationNode> appenders = appendersStream
                .map(PropertiesV2ConfigurationParser::processAppender)
                .collect(Collectors.toList());
        builder.addChild(createAppenders(appenders));
        PropertiesUtils.throwIfNotEmpty(appendersProperties);

        ConfigurationNodeBuilder loggersBuilder = newNodeBuilder().setPluginName(LOGGERS_PLUGIN_NAME);
        // 1. Start with the root logger
        PropertiesSubset rootLoggerProperties = extractSubset(rootProperties, "rootLogger");
        loggersBuilder.addChild(processRootLogger(rootLoggerProperties));
        PropertiesUtils.throwIfNotEmpty(rootLoggerProperties);
        // 2. The remaining loggers
        PropertiesSubset loggersProperties = extractSubset(rootProperties, "logger");
        String loggersName = extractProperty(rootProperties, "loggers");
        final Stream<? extends PropertiesSubset> loggersStream = loggersName != null
                ? partitionOnGivenPrefixes(loggersProperties, loggersName)
                : partitionOnCommonPrefixes(loggersProperties);
        loggersStream.map(PropertiesV2ConfigurationParser::processLogger).forEach(loggersBuilder::addChild);
        PropertiesUtils.throwIfNotEmpty(loggersProperties);
        // Add the `Loggers` plugin
        builder.addChild(loggersBuilder.get());

        // Extract attributes of Configuration
        for (final String key : rootProperties.getProperties().stringPropertyNames()) {
            if (!key.contains(".")) {
                builder.addAttribute(key, extractProperty(rootProperties, key));
            }
        }
        PropertiesUtils.throwIfNotEmpty(rootProperties);
        return builder.get();
    }

    private static ConfigurationNode processPropertyPlaceholders(PropertiesSubset propertyPlaceholders) {
        Properties props = propertyPlaceholders.getProperties();
        ConfigurationNodeBuilder builder = newNodeBuilder().setPluginName(PROPERTIES_PLUGIN_NAME);
        for (final String key : props.stringPropertyNames()) {
            ConfigurationNodeBuilder builder1 = newNodeBuilder()
                    .setPluginName(PROPERTY_PLUGIN_NAME)
                    .addAttribute(NAME_ATTRIBUTE, key)
                    .addAttribute(VALUE_ATTRIBUTE, props.getProperty(key));
            builder.addChild(builder1.get());
        }
        return builder.get();
    }

    private ConfigurationNode processScripts(PropertiesSubset scripts) {
        ConfigurationNodeBuilder builder = newNodeBuilder().setPluginName(SCRIPTS_PLUGIN_NAME);
        partitionOnCommonPrefixes(scripts)
                .forEach(script -> builder.addChild(processGenericComponent("script", script)));
        return builder.get();
    }

    private ConfigurationNode processCustomLevels(PropertiesSubset customLevels) {
        ConfigurationNodeBuilder builder = newNodeBuilder().setPluginName(CUSTOM_LEVELS_PLUGIN_NAME);
        for (final String key : customLevels.getProperties().stringPropertyNames()) {
            String value =
                    validateInteger("customLevel." + key, Objects.requireNonNull(extractProperty(customLevels, key)));
            ConfigurationNodeBuilder builder1 = newNodeBuilder()
                    .setPluginName(CUSTOM_LEVEL_PLUGIN_NAME)
                    .addAttribute(NAME_ATTRIBUTE, key)
                    .addAttribute(INT_LEVEL_ATTRIBUTE, value);
            builder.addChild(builder1.get());
        }
        return builder.get();
    }

    private static String validateInteger(String key, String value) {
        try {
            Integer.parseInt(value);
            return value;
        } catch (NumberFormatException e) {
            throw new ConfigurationConverterException("Invalid integer value `" + value + "` for key `" + key + "`", e);
        }
    }

    private static ConfigurationNode createAppenders(Iterable<? extends ConfigurationNode> appenders) {
        ConfigurationNodeBuilder builder = newNodeBuilder().setPluginName(APPENDERS_PLUGIN_NAME);
        for (ConfigurationNode appender : appenders) {
            builder.addChild(appender);
        }
        return builder.get();
    }

    private static ConfigurationNode processAppender(PropertiesSubset properties) {
        ConfigurationNodeBuilder builder = newNodeBuilder()
                .setPluginName(getRequiredAttribute(
                        properties,
                        TYPE_ATTRIBUTE,
                        () -> "No type attribute provided for Appender " + properties.getPrefix()))
                .addAttribute(
                        NAME_ATTRIBUTE,
                        getRequiredAttribute(
                                properties,
                                NAME_ATTRIBUTE,
                                () -> "No name attribute provided for Appender " + properties.getPrefix()));

        addFiltersToComponent(properties, builder);
        processRemainingProperties(properties, builder);
        return builder.get();
    }

    private static ConfigurationNode processLogger(PropertiesSubset properties) {
        ConfigurationNodeBuilder builder = newNodeBuilder()
                .addAttribute(LEVEL_AND_REFS_ATTRIBUTE, extractProperty(properties, ""))
                .addAttribute(
                        NAME_ATTRIBUTE,
                        getRequiredAttribute(
                                properties,
                                NAME_ATTRIBUTE,
                                () -> "No name attribute provided for Logger " + properties.getPrefix()));

        String type = extractProperty(properties, TYPE_ATTRIBUTE);
        if (ASYNC_LOGGER_PLUGIN_NAME.equalsIgnoreCase(type)) {
            builder.setPluginName(ASYNC_LOGGER_PLUGIN_NAME);
        } else if (type != null) {
            throw new ConfigurationConverterException(
                    "Unknown logger type `" + type + "` for logger " + properties.getPrefix());
        } else {
            builder.setPluginName(LOGGER_PLUGIN_NAME);
        }

        addAppenderRefsToComponent(properties, builder);
        addFiltersToComponent(properties, builder);
        processRemainingProperties(properties, builder);

        return builder.get();
    }

    private static ConfigurationNode wrapFilters(Collection<? extends ConfigurationNode> filters) {
        if (filters.isEmpty()) {
            throw new IllegalArgumentException("No filters provided");
        }
        return filters.size() > 1
                ? ComponentUtils.newCompositeFilter(filters)
                : filters.iterator().next();
    }

    private static void addAppenderRefsToComponent(PropertiesSubset properties, ConfigurationNodeBuilder builder) {
        PropertiesSubset appenderRefProperties = extractSubset(properties, "appenderRef");
        partitionOnCommonPrefixes(appenderRefProperties).forEach(p -> builder.addChild(processAppenderRef(p)));
        PropertiesUtils.throwIfNotEmpty(appenderRefProperties);
    }

    private static ConfigurationNode processAppenderRef(PropertiesSubset properties) {
        ConfigurationNodeBuilder builder = newNodeBuilder()
                .setPluginName(APPENDER_REF_PLUGIN_NAME)
                .addAttribute(
                        REF_ATTRIBUTE,
                        getRequiredAttribute(
                                properties,
                                REF_ATTRIBUTE,
                                () -> "No ref attribute provided for AppenderRef " + properties.getPrefix()));

        String level = Strings.trimToNull(extractProperty(properties, "level"));
        if (level != null) {
            builder.addAttribute("level", level);
        }
        addFiltersToComponent(properties, builder);
        processRemainingProperties(properties, builder);
        return builder.get();
    }

    private static void addFiltersToComponent(PropertiesSubset properties, ConfigurationNodeBuilder builder) {
        PropertiesSubset filtersProperties = extractSubset(properties, "filter");
        Collection<ConfigurationNode> filters = partitionOnCommonPrefixes(filtersProperties)
                .map(p -> processGenericComponent("filter", p))
                .collect(Collectors.toList());
        if (!filters.isEmpty()) {
            builder.addChild(wrapFilters(filters));
        }
    }

    private static ConfigurationNode processRootLogger(PropertiesSubset properties) {
        ConfigurationNodeBuilder builder =
                newNodeBuilder().addAttribute(LEVEL_AND_REFS_ATTRIBUTE, extractProperty(properties, ""));

        String type = extractProperty(properties, TYPE_ATTRIBUTE);
        if (ASYNC_ROOT_PLUGIN_NAME.equalsIgnoreCase(type)) {
            builder.setPluginName(ASYNC_ROOT_PLUGIN_NAME);
        } else if (type != null) {
            throw new ConfigurationConverterException("Unknown logger type `" + type + "` for root logger.");
        } else {
            builder.setPluginName(ROOT_PLUGIN_NAME);
        }

        addAppenderRefsToComponent(properties, builder);
        addFiltersToComponent(properties, builder);
        processRemainingProperties(properties, builder);

        return builder.get();
    }

    /**
     * Standard mapping between a properties file and node tree.
     * <p>
     *     The component must have a {@code type} attribute.
     * </p>
     *
     * @param componentCategory Type of expected component. Used only for the exception message.
     * @param properties Component properties with names relative to {@code prefix}.
     */
    private static ConfigurationNode processGenericComponent(String componentCategory, PropertiesSubset properties) {
        ConfigurationNodeBuilder builder = newNodeBuilder();

        builder.setPluginName(getRequiredAttribute(
                properties,
                TYPE_ATTRIBUTE,
                () -> "No type attribute provided for " + componentCategory + " " + properties.getPrefix()));
        processRemainingProperties(properties, builder);
        return builder.get();
    }

    private static void processRemainingProperties(PropertiesSubset properties, ConfigurationNodeBuilder builder) {
        while (isNotEmpty(properties)) {
            String propertyName =
                    properties.getProperties().stringPropertyNames().iterator().next();
            int index = propertyName.indexOf('.');
            if (index > 0) {
                String localPrefix = propertyName.substring(0, index);
                PropertiesSubset componentProperties = extractSubset(properties, localPrefix);
                builder.addChild(processGenericComponent("component", componentProperties));
            } else {
                builder.addAttribute(propertyName, extractProperty(properties, propertyName));
            }
        }
    }

    private static String getRequiredAttribute(
            PropertiesSubset properties, String propertyName, Supplier<String> errorMessageSupplier) {
        String value = extractProperty(properties, propertyName);
        if (Strings.isEmpty(value)) {
            throw new ConfigurationConverterException(errorMessageSupplier.get());
        }
        return value;
    }

    private static Stream<PropertiesSubset> partitionOnGivenPrefixes(PropertiesSubset properties, String prefixes) {
        Stream.Builder<PropertiesSubset> builder = Stream.builder();
        for (String prefix : prefixes.split(COMMA_SEPARATOR, -1)) {
            if (!prefix.isEmpty()) {
                builder.add(extractSubset(properties, prefix));
            }
        }
        return builder.build();
    }
}
