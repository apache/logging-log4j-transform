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
package org.apache.logging.converter.config.internal.v1.layout;

import aQute.bnd.annotation.spi.ServiceProvider;
import java.lang.invoke.MethodHandle;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.logging.converter.config.internal.ComponentUtils;
import org.apache.logging.converter.config.internal.ComponentUtils.ConfigurationNodeBuilder;
import org.apache.logging.converter.config.internal.StringUtils;
import org.apache.logging.converter.config.internal.v1.AbstractComponentParser;
import org.apache.logging.converter.config.spi.ConfigurationNode;
import org.apache.logging.converter.config.spi.v1.Log4j1ComponentParser;
import org.apache.logging.converter.config.spi.v1.PropertiesSubset;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * Parses a
 * <a href="https://logging.apache.org/log4j/1.x/apidocs/org/apache/log4j/TTCCLayout.html">TTCCLayout</a>
 * configuration.
 */
@ServiceProvider(Log4j1ComponentParser.class)
public class TTCCLayoutParser extends AbstractComponentParser<TTCCLayoutParser.TTCCLayoutBuilder> {

    private static final String CATEGORY_PREFIXING_PARAM = "CategoryPrefixing";
    private static final String CONTEXT_PRINTING_PARAM = "ContextPrinting";
    private static final String DATE_FORMAT_PARAM = "DateFormat";
    private static final String THREAD_PRINTING_PARAM = "ThreadPrinting";
    private static final String TIMEZONE_FORMAT_PARAM = "TimeZone";

    private static final String ABSOLUTE = "ABSOLUTE";
    private static final String ABSOLUTE_FORMAT = "HH:mm:ss,SSS";
    private static final String DATE = "DATE";
    private static final String DATE_FORMAT = "dd MMM yyyy HH:mm:ss,SSS";
    private static final String ISO8601 = "ISO8601";
    private static final String ISO8601_FORMAT = "yyyy-MM-dd HH:mm:ss,SSS";
    private static final String NULL = "NULL";
    private static final String RELATIVE = "RELATIVE";

    private static final Map<String, MethodHandle> ATTRIBUTE_MAP = attributeMapBuilder(TTCCLayoutBuilder.class)
            .add(CATEGORY_PREFIXING_PARAM)
            .add(CONTEXT_PRINTING_PARAM)
            .add(DATE_FORMAT_PARAM)
            .add(THREAD_PRINTING_PARAM)
            .add(TIMEZONE_FORMAT_PARAM)
            .get();

    @Override
    public String getClassName() {
        return "org.apache.log4j.TTCCLayout";
    }

    @Override
    protected TTCCLayoutBuilder createBuilder(Element element) {
        return new TTCCLayoutBuilder();
    }

    @Override
    protected TTCCLayoutBuilder createBuilder(PropertiesSubset properties) {
        return new TTCCLayoutBuilder();
    }

    @Override
    protected Map<String, ? extends MethodHandle> getAttributeMap() {
        return ATTRIBUTE_MAP;
    }

    public static final class TTCCLayoutBuilder implements Supplier<ConfigurationNode> {

        private boolean threadPrinting = true;
        private boolean categoryPrefixing = true;
        private boolean contextPrinting = true;

        private @Nullable String dateFormat = RELATIVE;
        private @Nullable String timeZone = null;

        private TTCCLayoutBuilder() {}

        public void setThreadPrinting(String threadPrinting) {
            this.threadPrinting = StringUtils.parseBoolean(threadPrinting);
        }

        public void setCategoryPrefixing(String categoryPrefixing) {
            this.categoryPrefixing = StringUtils.parseBoolean(categoryPrefixing);
        }

        public void setContextPrinting(String contextPrinting) {
            this.contextPrinting = StringUtils.parseBoolean(contextPrinting);
        }

        public void setDateFormat(String dateFormat) {
            switch (dateFormat.toUpperCase(Locale.ROOT)) {
                case ABSOLUTE:
                    this.dateFormat = ABSOLUTE_FORMAT;
                    break;
                case DATE:
                    this.dateFormat = DATE_FORMAT;
                    break;
                case ISO8601:
                    this.dateFormat = ISO8601_FORMAT;
                    break;
                case RELATIVE:
                    this.dateFormat = RELATIVE;
                    break;
                case NULL:
                    this.dateFormat = null;
                    break;
                default:
            }
        }

        public void setTimeZone(String timeZone) {
            this.timeZone = timeZone;
        }

        @Override
        public ConfigurationNode get() {
            ConfigurationNodeBuilder nodeBuilder =
                    ComponentUtils.newNodeBuilder().setPluginName("PatternLayout");
            StringBuilder patternBuilder = new StringBuilder();
            if (dateFormat != null) {
                if (RELATIVE.equalsIgnoreCase(dateFormat)) {
                    patternBuilder.append("%r ");
                } else {
                    patternBuilder.append("%d{").append(dateFormat).append("}");
                    if (timeZone != null) {
                        patternBuilder.append("{").append(timeZone).append("}");
                    }
                    patternBuilder.append(" ");
                }
            }
            if (threadPrinting) {
                patternBuilder.append("[%t] ");
            }
            patternBuilder.append("%p ");
            if (categoryPrefixing) {
                patternBuilder.append("%c ");
            }
            if (contextPrinting) {
                patternBuilder.append("%notEmpty{%NDC }");
            }
            patternBuilder.append("- %m%n");
            nodeBuilder.addAttribute("pattern", patternBuilder.toString());
            return nodeBuilder.get();
        }
    }
}
