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
import org.apache.logging.converter.config.spi.v1.Log4j1ComponentParser;

/**
 * Parses a
 * <a href="https://logging.apache.org/log4j/1.x/apidocs/org/apache/log4j/FileAppender.html">FileAppender</a>
 * configuration.
 */
@ServiceProvider(Log4j1ComponentParser.class)
public class FileAppenderParser extends AbstractFileAppenderParser<FileAppenderParser.FileAppenderBuilder> {
    @Override
    public String getClassName() {
        return "org.apache.log4j.FileAppender";
    }

    @Override
    protected FileAppenderBuilder createBuilder(String name) {
        return new FileAppenderBuilder(name);
    }

    @Override
    protected Map<String, ? extends MethodHandle> getAttributeMap() {
        return FILE_ATTRIBUTE_MAP;
    }

    public static final class FileAppenderBuilder extends AbstractFileAppenderParser.AbstractFileAppenderBuilder {

        private FileAppenderBuilder(String name) {
            super(name);
        }
    }
}
