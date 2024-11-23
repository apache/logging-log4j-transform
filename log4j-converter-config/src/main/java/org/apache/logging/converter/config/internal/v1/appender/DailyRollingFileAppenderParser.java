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

import aQute.bnd.annotation.spi.ServiceProvider;
import java.lang.invoke.MethodHandle;
import java.util.Map;
import org.apache.logging.converter.config.internal.ComponentUtils;
import org.apache.logging.converter.config.internal.ComponentUtils.ConfigurationNodeBuilder;
import org.apache.logging.converter.config.spi.ConfigurationNode;
import org.apache.logging.converter.config.spi.v1.Log4j1ComponentParser;

/**
 * Parses a
 * <a href="https://logging.apache.org/log4j/1.x/apidocs/org/apache/log4j/DailyRollingFileAppender.html">DailyRollingFileAppender</a>
 * configuration.
 */
@ServiceProvider(Log4j1ComponentParser.class)
public class DailyRollingFileAppenderParser
        extends AbstractFileAppenderParser<DailyRollingFileAppenderParser.DailyRollingFileAppenderBuilder> {

    private static final String DEFAULT_DATE_PATTERN = ".yyyy-MM-dd";
    private static final String DATE_PATTERN_PARAM = "DatePattern";

    private static final Map<String, MethodHandle> ATTRIBUTE_MAP = attributeMapBuilder(
                    DailyRollingFileAppenderBuilder.class)
            .add(DATE_PATTERN_PARAM)
            .addAll(FILE_ATTRIBUTE_MAP)
            .get();

    @Override
    public String getClassName() {
        return "org.apache.log4j.DailyRollingFileAppender";
    }

    @Override
    protected DailyRollingFileAppenderBuilder createBuilder(String name) {
        return new DailyRollingFileAppenderBuilder(name);
    }

    @Override
    protected Map<String, ? extends MethodHandle> getAttributeMap() {
        return ATTRIBUTE_MAP;
    }

    public static final class DailyRollingFileAppenderBuilder
            extends AbstractFileAppenderParser.AbstractFileAppenderBuilder {

        private String datePattern = DEFAULT_DATE_PATTERN;

        private DailyRollingFileAppenderBuilder(String name) {
            super(name);
        }

        public void setDatePattern(String datePattern) {
            this.datePattern = datePattern;
        }

        @Override
        public ConfigurationNode get() {
            ConfigurationNodeBuilder builder = ComponentUtils.newNodeBuilder().setPluginName("RollingFile");
            addFileAttributes(builder);
            addStandardChildren(builder);

            String filePattern = getRequiredFile() + "%d{" + datePattern + "}";
            return builder.addAttribute("filePattern", filePattern)
                    .addChild(createTriggeringPolicy())
                    .get();
        }

        private ConfigurationNode createTriggeringPolicy() {
            return ComponentUtils.newNodeBuilder()
                    .setPluginName("TimeBasedTriggeringPolicy")
                    .addAttribute("modulate", true)
                    .get();
        }
    }
}
