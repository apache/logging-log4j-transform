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

import static org.apache.logging.converter.config.internal.StringUtils.decapitalize;

import java.lang.invoke.MethodHandle;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.converter.config.ConfigurationConverterException;
import org.apache.logging.converter.config.internal.ComponentUtils;
import org.apache.logging.converter.config.internal.ComponentUtils.ConfigurationNodeBuilder;
import org.apache.logging.converter.config.internal.StringUtils;
import org.apache.logging.converter.config.spi.ConfigurationNode;
import org.jspecify.annotations.Nullable;

/**
 * Common base for all file appender parsers.
 */
public abstract class AbstractFileAppenderParser<T extends AbstractFileAppenderParser.AbstractFileAppenderBuilder>
        extends AbstractAppenderParser<T> {

    private static final String FILE_PARAM = "File";

    protected static final Map<String, MethodHandle> FILE_ATTRIBUTE_MAP = attributeMapBuilder(
                    AbstractFileAppenderParser.AbstractFileAppenderBuilder.class)
            .add(APPEND_PARAM)
            .add(BUFFERED_IO_PARAM)
            .add(BUFFER_SIZE_PARAM)
            .add(FILE_PARAM)
            .add(IMMEDIATE_FLUSH_PARAM)
            .add(THRESHOLD_PARAM)
            .get();

    public abstract static class AbstractFileAppenderBuilder extends AbstractAppenderParser.AppenderBuilder {

        private String append = StringUtils.TRUE;
        private boolean bufferedIO = false;
        private String bufferSize = "8192";
        private @Nullable String file;
        private boolean immediateFlush = true;

        protected AbstractFileAppenderBuilder(String name) {
            super(name, true);
        }

        public void setAppend(String append) {
            this.append = append;
        }

        public void setBufferedIO(String bufferedIO) {
            this.bufferedIO = StringUtils.parseBoolean(bufferedIO);
        }

        public void setBufferSize(String bufferSize) {
            this.bufferSize = bufferSize;
        }

        protected String getRequiredFile() {
            return Objects.requireNonNull(file);
        }

        public void setFile(String file) {
            this.file = file;
        }

        public void setImmediateFlush(String immediateFlush) {
            this.immediateFlush = StringUtils.parseBoolean(immediateFlush);
        }

        protected void addFileAttributes(ConfigurationNodeBuilder builder) {
            if (bufferedIO) {
                immediateFlush = false;
            }
            if (file == null) {
                throw new ConfigurationConverterException("No file specified for appender " + getName());
            }
            builder.addAttribute("name", getName())
                    .addAttribute(decapitalize(APPEND_PARAM), append)
                    .addAttribute("bufferedIo", bufferedIO)
                    .addAttribute(decapitalize(BUFFER_SIZE_PARAM), bufferSize)
                    .addAttribute(decapitalize(IMMEDIATE_FLUSH_PARAM), immediateFlush)
                    .addAttribute("fileName", file);
        }

        @Override
        public ConfigurationNode get() {
            ConfigurationNodeBuilder builder = ComponentUtils.newNodeBuilder().setPluginName("File");
            addFileAttributes(builder);
            addStandardChildren(builder);
            return builder.get();
        }
    }
}
