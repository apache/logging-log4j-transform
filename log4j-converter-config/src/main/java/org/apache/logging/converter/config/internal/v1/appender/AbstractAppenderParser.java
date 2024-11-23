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
package org.apache.logging.converter.config.internal.v1.appender;

import static org.apache.logging.converter.config.internal.ComponentUtils.newCompositeFilter;
import static org.apache.logging.converter.config.internal.ComponentUtils.newThresholdFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.apache.logging.converter.config.ConfigurationConverterException;
import org.apache.logging.converter.config.internal.ComponentUtils.ConfigurationNodeBuilder;
import org.apache.logging.converter.config.internal.PropertiesUtils;
import org.apache.logging.converter.config.internal.v1.AbstractComponentParser;
import org.apache.logging.converter.config.spi.ConfigurationNode;
import org.apache.logging.converter.config.spi.v1.Log4j1ParserContext;
import org.apache.logging.converter.config.spi.v1.PropertiesSubset;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * Common base for all appender parsers.
 */
public abstract class AbstractAppenderParser<T extends AbstractAppenderParser.AppenderBuilder>
        extends AbstractComponentParser<T> {

    // XML tags
    private static final String FILTER_TAG = "filter";
    private static final String LAYOUT_TAG = "layout";

    // parameters
    protected static final String APPEND_PARAM = "Append";
    protected static final String BUFFERED_IO_PARAM = "BufferedIO";
    protected static final String BUFFER_SIZE_PARAM = "BufferSize";
    protected static final String IMMEDIATE_FLUSH_PARAM = "ImmediateFlush";

    protected static String getAppenderName(PropertiesSubset properties) {
        return PropertiesUtils.getLastComponent(properties.getPrefix());
    }

    protected static String getAppenderName(Element appenderElement) {
        String name = appenderElement.getAttribute(NAME_ATTR);
        if (name.isEmpty()) {
            throw new ConfigurationConverterException("No name specified for appender " + appenderElement.getTagName());
        }
        return name;
    }

    @Override
    protected final T createBuilder(Element element) {
        return createBuilder(getAppenderName(element));
    }

    @Override
    protected final T createBuilder(PropertiesSubset properties) {
        return createBuilder(getAppenderName(properties));
    }

    /**
     * Creates a configuration node builder.
     *
     * @param name The name of the appender.
     */
    protected abstract T createBuilder(String name);

    @Override
    protected void handleUnknownElement(Element childElement, Log4j1ParserContext context, T componentBuilder)
            throws ConfigurationConverterException {
        String nodeName = childElement.getTagName();
        if (nodeName.equals(LAYOUT_TAG)) {
            componentBuilder.setLayout(parseConfigurationElement(context, childElement));
        } else if (nodeName.equals(FILTER_TAG)) {
            componentBuilder.addFilter(parseConfigurationElement(context, childElement));
        } else {
            handleUnknownElement(childElement, context, componentBuilder);
        }
    }

    @Override
    protected void handleUnknownProperties(PropertiesSubset properties, Log4j1ParserContext context, T componentBuilder)
            throws ConfigurationConverterException {
        String key = PropertiesUtils.getLastComponent(properties.getPrefix());
        if (key.equals(LAYOUT_TAG)) {
            componentBuilder.setLayout(parseConfigurationElement(context, properties));
        } else if (key.equals(FILTER_TAG)) {
            PropertiesUtils.partitionOnCommonPrefixes(properties)
                    .forEach(filterProperties ->
                            componentBuilder.addFilter(parseConfigurationElement(context, filterProperties)));
        } else {
            super.handleUnknownProperties(properties, context, componentBuilder);
        }
    }

    public abstract static class AppenderBuilder implements Supplier<ConfigurationNode> {

        private final String name;
        private final boolean requiresLayout;

        private @Nullable String threshold = null;
        private final List<ConfigurationNode> filters = new ArrayList<>();
        private @Nullable ConfigurationNode layout;

        protected AppenderBuilder(String name, boolean requiresLayout) {
            this.name = name;
            this.requiresLayout = requiresLayout;
        }

        public void setThreshold(String level) {
            this.threshold = level;
        }

        public void addFilter(ConfigurationNode filter) {
            filters.add(filter);
        }

        public void setLayout(ConfigurationNode layout) {
            this.layout = layout;
        }

        protected String getName() {
            return name;
        }

        protected ConfigurationNodeBuilder addStandardChildren(ConfigurationNodeBuilder builder) {
            if (threshold != null) {
                filters.add(0, newThresholdFilter(threshold));
            }
            if (!filters.isEmpty()) {
                builder.addChild(filters.size() > 1 ? newCompositeFilter(filters) : filters.get(0));
            }
            if (layout != null) {
                builder.addChild(layout);
            } else if (requiresLayout) {
                throw new ConfigurationConverterException("No layout provided for appender " + name);
            }
            return builder;
        }
    }
}
