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

import aQute.bnd.annotation.spi.ServiceProvider;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.apache.logging.converter.config.ConfigurationConverterException;
import org.apache.logging.converter.config.internal.ComponentUtils;
import org.apache.logging.converter.config.internal.ComponentUtils.ConfigurationNodeBuilder;
import org.apache.logging.converter.config.internal.StringUtils;
import org.apache.logging.converter.config.spi.ConfigurationNode;
import org.apache.logging.converter.config.spi.v1.Log4j1ComponentParser;
import org.apache.logging.converter.config.spi.v1.Log4j1ParserContext;
import org.w3c.dom.Element;

/**
 * Parses a
 * <a href="https://logging.apache.org/log4j/1.x/apidocs/org/apache/log4j/AsyncAppender.html">AsyncAppender</a>
 * configuration.
 */
@ServiceProvider(Log4j1ComponentParser.class)
public class AsyncAppenderParser extends AbstractAppenderParser<AsyncAppenderParser.AsyncAppenderBuilder> {

    // XML tags
    private static final String APPENDER_REF_TAG = "appender-ref";

    // Parameters
    private static final String BLOCKING_PARAM = "Blocking";
    private static final String INCLUDE_LOCATION_PARAM = "IncludeLocation";

    private static final Map<String, MethodHandle> ATTRIBUTE_MAP = attributeMapBuilder(
                    AsyncAppenderParser.AsyncAppenderBuilder.class)
            .add(BLOCKING_PARAM)
            .add(BUFFER_SIZE_PARAM)
            .add(INCLUDE_LOCATION_PARAM)
            .add(THRESHOLD_PARAM)
            .get();

    @Override
    public String getClassName() {
        return "org.apache.log4j.AsyncAppender";
    }

    @Override
    protected AsyncAppenderBuilder createBuilder(String appenderName) {
        return new AsyncAppenderBuilder(appenderName);
    }

    @Override
    protected Map<String, ? extends MethodHandle> getAttributeMap() {
        return ATTRIBUTE_MAP;
    }

    @Override
    protected void handleUnknownElement(
            Element childElement, Log4j1ParserContext context, AsyncAppenderBuilder componentBuilder)
            throws ConfigurationConverterException {
        if (childElement.getTagName().equals(APPENDER_REF_TAG)) {
            componentBuilder.addAppenderRef(childElement.getAttribute(REF_ATTR));
        } else {
            super.handleUnknownElement(childElement, context, componentBuilder);
        }
    }

    public static final class AsyncAppenderBuilder extends AbstractAppenderParser.AppenderBuilder {

        private final Collection<String> appenderRefs = new ArrayList<>();
        private String blocking = StringUtils.FALSE;
        private String bufferSize = "1024";
        private String includeLocation = StringUtils.FALSE;

        private AsyncAppenderBuilder(String name) {
            super(name, false);
        }

        public void addAppenderRef(String appenderRef) {
            appenderRefs.add(appenderRef);
        }

        public void setAppenderRefs(String appenderRefs) {
            this.appenderRefs.clear();
            String[] array = appenderRefs.split(",", -1);
            for (String appenderRef : array) {
                this.appenderRefs.add(appenderRef.trim());
            }
        }

        public void setBlocking(String blocking) {
            this.blocking = blocking;
        }

        public void setBufferSize(String bufferSize) {
            this.bufferSize = bufferSize;
        }

        public void setIncludeLocation(String includeLocation) {
            this.includeLocation = includeLocation;
        }

        @Override
        public ConfigurationNode get() {
            ConfigurationNodeBuilder builder = ComponentUtils.newNodeBuilder()
                    .setPluginName("Async")
                    .addAttribute("name", getName())
                    .addAttribute(decapitalize(BLOCKING_PARAM), blocking)
                    .addAttribute(decapitalize(BUFFER_SIZE_PARAM), bufferSize)
                    .addAttribute(decapitalize(INCLUDE_LOCATION_PARAM), includeLocation);
            addStandardChildren(builder);
            appenderRefs.forEach(ref -> builder.addChild(ComponentUtils.newAppenderRef(ref)));
            return builder.get();
        }
    }
}
