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

import static org.apache.logging.log4j.util.PropertiesUtil.extractSubset;
import static org.apache.logging.log4j.util.PropertiesUtil.partitionOnCommonPrefixes;

import aQute.bnd.annotation.Resolution;
import aQute.bnd.annotation.spi.ServiceProvider;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.Supplier;
import org.apache.logging.converter.config.ConfigurationConverterException;
import org.apache.logging.converter.config.internal.ConfigurationNodeImpl;
import org.apache.logging.converter.config.spi.ConfigurationNode;
import org.apache.logging.converter.config.spi.ConfigurationParser;
import org.apache.logging.log4j.util.Strings;
import org.jspecify.annotations.Nullable;

@ServiceProvider(value = ConfigurationParser.class, resolution = Resolution.MANDATORY)
public class PropertiesV2ConfigurationParser implements ConfigurationParser {

    private static final String LOG4J_V2_PROPERTIES_FORMAT = "v2:properties";

    private static final String INT_LEVEL_ATTRIBUTE = "intLevel";
    private static final String LEVEL_AND_REFS_ATTRIBUTE = "levelAndRefs";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String REF_ATTRIBUTE = "ref";
    private static final String TYPE_ATTRIBUTE = "type";
    private static final String VALUE_ATTRIBUTE = "value";

    private static final String ROOT_LOGGER_NAME = "root";

    private static final String APPENDERS_PLUGIN_NAME = "Appenders";
    private static final String CONFIGURATION_PLUGIN_NAME = "Configuration";
    private static final String CUSTOM_LEVEL_PLUGIN_NAME = "CustomLevel";
    private static final String CUSTOM_LEVELS_PLUGIN_NAME = "CustomLevels";
    private static final String APPENDER_REF_PLUGIN_NAME = "AppenderRef";
    private static final String ASYNC_LOGGER_PLUGIN_NAME = "AsyncLogger";
    private static final String ASYNC_ROOT_PLUGIN_NAME = "AsyncRoot";
    private static final String FILTERS_PLUGIN_NAME = "Filters";
    private static final String LOGGER_PLUGIN_NAME = "Logger";
    private static final String LOGGERS_PLUGIN_NAME = "Loggers";
    private static final String PROPERTIES_PLUGIN_NAME = "Properties";
    private static final String PROPERTY_PLUGIN_NAME = "Property";
    private static final String ROOT_PLUGIN_NAME = "Root";
    private static final String SCRIPTS_PLUGIN_NAME = "Scripts";

    @Override
    public String getInputFormat() {
        return LOG4J_V2_PROPERTIES_FORMAT;
    }

    @Override
    public ConfigurationNode parse(InputStream inputStream) throws IOException {
        Properties rootProperties = new Properties();
        rootProperties.load(inputStream);
        ConfigurationNodeImpl.NodeBuilder builder =
                ConfigurationNodeImpl.newNodeBuilder().setPluginName(CONFIGURATION_PLUGIN_NAME);

        for (final String key : rootProperties.stringPropertyNames()) {
            if (!key.contains(".")) {
                builder.addAttribute(key, remove(rootProperties, key));
            }
        }

        Properties propertyPlaceholders = extractSubset(rootProperties, "property");
        if (!propertyPlaceholders.isEmpty()) {
            builder.addChild(processPropertyPlaceholders(propertyPlaceholders));
        }

        Map<String, Properties> scripts = extractSubsetAndPartition(rootProperties, "script");
        if (!scripts.isEmpty()) {
            builder.addChild(processScripts(scripts));
        }

        Properties customLevels = extractSubset(rootProperties, "customLevel");
        if (!customLevels.isEmpty()) {
            builder.addChild(processCustomLevels(customLevels));
        }

        Map<String, Properties> filters = extractSubsetAndPartition(rootProperties, "filter", "filters");
        if (!filters.isEmpty()) {
            builder.addChild(processFilters("", filters));
        }

        Map<String, Properties> appenders = extractSubsetAndPartition(rootProperties, "appender", "appenders");
        if (!appenders.isEmpty()) {
            builder.addChild(processAppenders(appenders));
        }

        ConfigurationNodeImpl.NodeBuilder loggersBuilder =
                ConfigurationNodeImpl.newNodeBuilder().setPluginName(LOGGERS_PLUGIN_NAME);
        // 1. Start with the root logger
        Properties rootLoggerProperties = extractSubset(rootProperties, "rootLogger");
        String rootLoggerProperty = rootProperties.getProperty("rootLogger");
        if (rootLoggerProperty != null) {
            rootLoggerProperties.put("", rootLoggerProperty);
        }
        loggersBuilder.addChild(processRootLogger(rootLoggerProperties));
        // 2. The remaining loggers
        Map<String, Properties> loggers = extractSubsetAndPartition(rootProperties, "logger", "loggers");
        for (Map.Entry<String, Properties> entry : loggers.entrySet()) {
            if (!ROOT_LOGGER_NAME.equals(entry.getKey())) {
                loggersBuilder.addChild(processLogger(entry.getKey(), entry.getValue()));
            }
        }
        // Add the `Loggers` plugin
        builder.addChild(loggersBuilder.build());

        return builder.build();
    }

    private static Map<String, Properties> extractSubsetAndPartition(Properties rootProperties, String prefix) {
        return new TreeMap<>(partitionOnCommonPrefixes(extractSubset(rootProperties, prefix)));
    }

    private static Map<String, Properties> extractSubsetAndPartition(
            Properties rootProperties, String prefix, String keysProperty) {
        String keysList = rootProperties.getProperty(keysProperty);
        if (keysList != null) {
            String[] keys = keysList.split(",", -1);
            Map<String, Properties> result = new LinkedHashMap<>();
            for (final String untrimmedKey : keys) {
                String key = untrimmedKey.trim();
                result.put(key, extractSubset(rootProperties, prefix + "." + key));
            }
            return result;
        }
        return extractSubsetAndPartition(rootProperties, prefix);
    }

    private static ConfigurationNode processPropertyPlaceholders(final Properties propertyPlaceholders) {
        ConfigurationNodeImpl.NodeBuilder builder =
                ConfigurationNodeImpl.newNodeBuilder().setPluginName(PROPERTIES_PLUGIN_NAME);
        for (final String key : propertyPlaceholders.stringPropertyNames()) {
            builder.addChild(ConfigurationNodeImpl.newNodeBuilder()
                    .setPluginName(PROPERTY_PLUGIN_NAME)
                    .addAttribute(NAME_ATTRIBUTE, key)
                    .addAttribute(VALUE_ATTRIBUTE, propertyPlaceholders.getProperty(key))
                    .build());
        }
        return builder.build();
    }

    private ConfigurationNode processScripts(Map<String, Properties> scripts) {
        ConfigurationNodeImpl.NodeBuilder builder =
                ConfigurationNodeImpl.newNodeBuilder().setPluginName(SCRIPTS_PLUGIN_NAME);
        for (final Map.Entry<String, Properties> entry : scripts.entrySet()) {
            String scriptPrefix = "script." + entry.getKey();
            Properties scriptProperties = entry.getValue();
            builder.addChild(processGenericComponent(scriptPrefix, "Script", scriptProperties));
        }
        return builder.build();
    }

    private ConfigurationNode processCustomLevels(Properties customLevels) {
        ConfigurationNodeImpl.NodeBuilder builder =
                ConfigurationNodeImpl.newNodeBuilder().setPluginName(CUSTOM_LEVELS_PLUGIN_NAME);
        for (final String key : customLevels.stringPropertyNames()) {
            String value = validateInteger("customLevel." + key, customLevels.getProperty(key));
            builder.addChild(ConfigurationNodeImpl.newNodeBuilder()
                    .setPluginName(CUSTOM_LEVEL_PLUGIN_NAME)
                    .addAttribute(NAME_ATTRIBUTE, key)
                    .addAttribute(INT_LEVEL_ATTRIBUTE, value)
                    .build());
        }
        return builder.build();
    }

    private static String validateInteger(String key, String value) {
        try {
            Integer.parseInt(value);
            return value;
        } catch (NumberFormatException e) {
            throw new ConfigurationConverterException("Invalid integer value `" + value + "` for key `" + key + "`", e);
        }
    }

    private static ConfigurationNode processFilters(String prefix, Map<String, Properties> filters) {
        if (filters.size() == 1) {
            return processFilter(prefix, filters.entrySet().iterator().next());
        }
        ConfigurationNodeImpl.NodeBuilder builder =
                ConfigurationNodeImpl.newNodeBuilder().setPluginName(FILTERS_PLUGIN_NAME);
        for (final Map.Entry<String, Properties> filterEntry : filters.entrySet()) {
            builder.addChild(processFilter(prefix, filterEntry));
        }
        return builder.build();
    }

    private static ConfigurationNode processFilter(String prefix, Map.Entry<String, ? extends Properties> filterEntry) {
        String actualPrefix = prefix.isEmpty() ? prefix : prefix + ".";
        String filterPrefix = actualPrefix + "filter." + filterEntry.getKey();
        Properties filterProperties = filterEntry.getValue();
        return processGenericComponent(filterPrefix, "Filter", filterProperties);
    }

    private static ConfigurationNode processAppenders(Map<String, Properties> appenders) {
        ConfigurationNodeImpl.NodeBuilder builder =
                ConfigurationNodeImpl.newNodeBuilder().setPluginName(APPENDERS_PLUGIN_NAME);
        for (Map.Entry<String, Properties> entry : appenders.entrySet()) {
            builder.addChild(processAppender(entry.getKey(), entry.getValue()));
        }
        return builder.build();
    }

    private static ConfigurationNode processAppender(String key, Properties properties) {
        String appenderPrefix = "appender." + key;
        ConfigurationNodeImpl.NodeBuilder builder = ConfigurationNodeImpl.newNodeBuilder()
                .setPluginName(getRequiredAttribute(
                        properties, TYPE_ATTRIBUTE, () -> "No type attribute provided for Appender " + appenderPrefix))
                .addAttribute(
                        NAME_ATTRIBUTE,
                        getRequiredAttribute(
                                properties,
                                NAME_ATTRIBUTE,
                                () -> "No name attribute provided for Appender " + appenderPrefix));

        addFiltersToComponent(appenderPrefix, properties, builder);
        processRemainingProperties(appenderPrefix, properties, builder);

        return builder.build();
    }

    private static ConfigurationNode processLogger(String key, Properties properties) {
        ConfigurationNodeImpl.NodeBuilder builder = ConfigurationNodeImpl.newNodeBuilder()
                .addAttribute(LEVEL_AND_REFS_ATTRIBUTE, remove(properties, ""))
                .addAttribute(
                        NAME_ATTRIBUTE,
                        getRequiredAttribute(
                                properties, NAME_ATTRIBUTE, () -> "No name attribute provided for Logger " + key));

        String type = remove(properties, TYPE_ATTRIBUTE);
        if (ASYNC_LOGGER_PLUGIN_NAME.equalsIgnoreCase(type)) {
            builder.setPluginName(ASYNC_LOGGER_PLUGIN_NAME);
        } else if (type != null) {
            throw new ConfigurationConverterException("Unknown logger type `" + type + "` for logger " + key);
        } else {
            builder.setPluginName(LOGGER_PLUGIN_NAME);
        }

        String prefix = "logger." + key;
        addAppenderRefsToComponent(prefix, properties, builder);
        addFiltersToComponent(prefix, properties, builder);
        processRemainingProperties(prefix, properties, builder);

        return builder.build();
    }

    private static void addAppenderRefsToComponent(
            String prefix, Properties properties, ConfigurationNodeImpl.NodeBuilder builder) {
        Map<String, Properties> appenderRefs = extractSubsetAndPartition(properties, "appenderRef");
        for (final Map.Entry<String, Properties> entry : appenderRefs.entrySet()) {
            builder.addChild(processAppenderRef(prefix + ".appenderRef." + entry.getKey(), entry.getValue()));
        }
    }

    private static ConfigurationNode processAppenderRef(String prefix, Properties properties) {
        ConfigurationNodeImpl.NodeBuilder builder = ConfigurationNodeImpl.newNodeBuilder()
                .setPluginName(APPENDER_REF_PLUGIN_NAME)
                .addAttribute(
                        REF_ATTRIBUTE,
                        getRequiredAttribute(
                                properties,
                                REF_ATTRIBUTE,
                                () -> "No ref attribute provided for AppenderRef " + prefix));

        String level = Strings.trimToNull(remove(properties, "level"));
        if (level != null) {
            builder.addAttribute("level", level);
        }

        addFiltersToComponent(prefix, properties, builder);
        processRemainingProperties(prefix, properties, builder);

        return builder.build();
    }

    private static void addFiltersToComponent(
            String prefix, Properties properties, ConfigurationNodeImpl.NodeBuilder builder) {
        Map<String, Properties> filters = extractSubsetAndPartition(properties, "filter");
        if (!filters.isEmpty()) {
            builder.addChild(processFilters(prefix, filters));
        }
    }

    private static ConfigurationNode processRootLogger(Properties properties) {
        ConfigurationNodeImpl.NodeBuilder builder =
                ConfigurationNodeImpl.newNodeBuilder().addAttribute(LEVEL_AND_REFS_ATTRIBUTE, remove(properties, ""));

        String type = remove(properties, TYPE_ATTRIBUTE);
        if (ASYNC_ROOT_PLUGIN_NAME.equalsIgnoreCase(type)) {
            builder.setPluginName(ASYNC_ROOT_PLUGIN_NAME);
        } else if (type != null) {
            throw new ConfigurationConverterException("Unknown logger type `" + type + "` for root logger.");
        } else {
            builder.setPluginName(ROOT_PLUGIN_NAME);
        }

        String prefix = "rootLogger";
        addAppenderRefsToComponent(prefix, properties, builder);
        addFiltersToComponent(prefix, properties, builder);
        processRemainingProperties(prefix, properties, builder);

        return builder.build();
    }

    private static @Nullable String remove(final Properties properties, final String key) {
        return (String) properties.remove(key);
    }

    /**
     * Standard mapping between a properties file and node tree.
     * <p>
     *     The component must have a {@code type} attribute.
     * </p>
     *
     * @param prefix Prefix of all the properties in the global prefix file. Used only for the exception message.
     * @param componentCategory Type of expected component. Used only for the exception message.
     * @param properties Component properties with names relative to {@code prefix}.
     */
    private static ConfigurationNode processGenericComponent(
            String prefix, String componentCategory, Properties properties) {
        ConfigurationNodeImpl.NodeBuilder builder = ConfigurationNodeImpl.newNodeBuilder();

        builder.setPluginName(getRequiredAttribute(
                properties,
                TYPE_ATTRIBUTE,
                () -> "No type attribute provided for " + componentCategory + " " + prefix));
        processRemainingProperties(prefix, properties, builder);
        return builder.build();
    }

    private static void processRemainingProperties(
            String prefix, Properties properties, ConfigurationNodeImpl.NodeBuilder builder) {
        while (!properties.isEmpty()) {
            String propertyName = properties.stringPropertyNames().iterator().next();
            int index = propertyName.indexOf('.');
            if (index > 0) {
                String localPrefix = propertyName.substring(0, index);
                String globalPrefix = prefix + "." + propertyName.substring(0, index);
                Properties componentProperties = extractSubset(properties, localPrefix);
                builder.addChild(processGenericComponent(globalPrefix, "component", componentProperties));
            } else {
                builder.addAttribute(propertyName, remove(properties, propertyName));
            }
        }
    }

    private static String getRequiredAttribute(
            Properties properties, String propertyName, Supplier<String> errorMessageSupplier) {
        String value = remove(properties, propertyName);
        if (Strings.isEmpty(value)) {
            throw new ConfigurationConverterException(errorMessageSupplier.get());
        }
        return value;
    }
}
