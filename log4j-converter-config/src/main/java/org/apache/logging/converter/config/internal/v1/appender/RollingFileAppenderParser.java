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
import java.util.Formatter;
import java.util.Locale;
import java.util.Map;
import org.apache.logging.converter.config.internal.ComponentUtils;
import org.apache.logging.converter.config.spi.ConfigurationNode;
import org.apache.logging.converter.config.spi.v1.Log4j1ComponentParser;

/**
 * Parses a
 * <a href="https://logging.apache.org/log4j/1.x/apidocs/org/apache/log4j/RollingFileAppender.html">RollingFileAppender</a>
 * configuration.
 */
@ServiceProvider(Log4j1ComponentParser.class)
public class RollingFileAppenderParser
        extends AbstractFileAppenderParser<RollingFileAppenderParser.RollingFileAppenderBuilder> {

    private static final String MAX_BACKUP_INDEX_PARAM = "MaxBackupIndex";
    private static final String MAX_FILE_SIZE_PARAM = "MaxFileSize";

    private static final String DEFAULT_MAX_BACKUP_INDEX = "1";
    private static final long KB = 1024;
    private static final long MB = KB * KB;
    private static final long GB = KB * MB;
    private static final long DEFAULT_MAX_FILE_SIZE = 10 * MB;
    private static final String SIZE_FORMAT = "%.2f %s";

    private static final Map<String, MethodHandle> ATTRIBUTE_MAP = attributeMapBuilder(
                    RollingFileAppenderParser.RollingFileAppenderBuilder.class)
            .add(MAX_BACKUP_INDEX_PARAM)
            .add(MAX_FILE_SIZE_PARAM)
            .addAll(FILE_ATTRIBUTE_MAP)
            .get();

    @Override
    public String getClassName() {
        return "org.apache.log4j.RollingFileAppender";
    }

    @Override
    protected RollingFileAppenderBuilder createBuilder(String name) {
        return new RollingFileAppenderBuilder(name);
    }

    @Override
    protected Map<String, ? extends MethodHandle> getAttributeMap() {
        return ATTRIBUTE_MAP;
    }

    public static final class RollingFileAppenderBuilder
            extends AbstractFileAppenderParser.AbstractFileAppenderBuilder {

        private String maxBackupIndex = DEFAULT_MAX_BACKUP_INDEX;
        private String maxFileSize = Long.toString(DEFAULT_MAX_FILE_SIZE);

        private RollingFileAppenderBuilder(String name) {
            super(name);
        }

        public void setMaxBackupIndex(String maxBackupIndex) {
            this.maxBackupIndex = maxBackupIndex;
        }

        public void setMaxFileSize(String maxFileSize) {
            this.maxFileSize = maxFileSize;
        }

        @Override
        public ConfigurationNode get() {
            ComponentUtils.ConfigurationNodeBuilder builder =
                    ComponentUtils.newNodeBuilder().setPluginName("RollingFile");
            addFileAttributes(builder);
            addStandardChildren(builder);

            String filePattern = getRequiredFile() + ".%i";
            return builder.addAttribute("filePattern", filePattern)
                    .addChild(createTriggeringPolicy())
                    .addChild(createRolloverStrategy())
                    .get();
        }

        private ConfigurationNode createTriggeringPolicy() {
            StringBuilder sizeBuilder = new StringBuilder();
            try {
                long maxFileSize = Long.parseLong(this.maxFileSize);
                Formatter sizeFormatter = new Formatter(sizeBuilder, Locale.ROOT);
                if (maxFileSize > GB) {
                    sizeFormatter.format(SIZE_FORMAT, ((float) maxFileSize) / GB, "GB");
                } else if (maxFileSize > MB) {
                    sizeFormatter.format(SIZE_FORMAT, ((float) maxFileSize) / MB, "MB");
                } else if (maxFileSize > KB) {
                    sizeFormatter.format(SIZE_FORMAT, ((float) maxFileSize) / KB, "KB");
                } else {
                    sizeBuilder.append(maxFileSize);
                }
            } catch (NumberFormatException e) {
                // The value contains property expansions
                sizeBuilder.append(this.maxFileSize);
            }
            return ComponentUtils.newNodeBuilder()
                    .setPluginName("SizeBasedTriggeringPolicy")
                    .addAttribute("size", sizeBuilder.toString())
                    .get();
        }

        private ConfigurationNode createRolloverStrategy() {
            return ComponentUtils.newNodeBuilder()
                    .setPluginName("DefaultRolloverStrategy")
                    .addAttribute("max", maxBackupIndex)
                    .addAttribute("fileIndex", "min")
                    .get();
        }
    }
}
