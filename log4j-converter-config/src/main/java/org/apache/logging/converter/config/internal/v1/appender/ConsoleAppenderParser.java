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
import java.util.Map;
import org.apache.logging.converter.config.ConfigurationConverterException;
import org.apache.logging.converter.config.internal.ComponentUtils;
import org.apache.logging.converter.config.internal.ComponentUtils.ConfigurationNodeBuilder;
import org.apache.logging.converter.config.internal.StringUtils;
import org.apache.logging.converter.config.spi.ConfigurationNode;
import org.apache.logging.converter.config.spi.v1.Log4j1ComponentParser;

/**
 * Parses a
 * <a href="https://logging.apache.org/log4j/1.x/apidocs/org/apache/log4j/ConsoleAppender.html">ConsoleAppender</a>
 * configuration.
 */
@ServiceProvider(Log4j1ComponentParser.class)
public class ConsoleAppenderParser extends AbstractAppenderParser<ConsoleAppenderParser.ConsoleAppenderBuilder> {

    private static final String V1_SYSTEM_OUT = "System.out";
    private static final String V1_SYSTEM_ERR = "System.err";
    private static final String V2_SYSTEM_OUT = "SYSTEM_OUT";
    private static final String V2_SYSTEM_ERR = "SYSTEM_ERR";

    // V1 configuration parameters
    private static final String TARGET_PARAM = "Target";
    private static final String FOLLOW_PARAM = "Follow";

    private static final Map<String, MethodHandle> ATTRIBUTE_MAP = attributeMapBuilder(ConsoleAppenderBuilder.class)
            .add(FOLLOW_PARAM)
            .add(IMMEDIATE_FLUSH_PARAM)
            .add(TARGET_PARAM)
            .add(THRESHOLD_PARAM)
            .get();

    @Override
    public String getClassName() {
        return "org.apache.log4j.ConsoleAppender";
    }

    @Override
    protected ConsoleAppenderBuilder createBuilder(String name) {
        return new ConsoleAppenderBuilder(name);
    }

    @Override
    protected Map<String, ? extends MethodHandle> getAttributeMap() {
        return ATTRIBUTE_MAP;
    }

    public static final class ConsoleAppenderBuilder extends AbstractAppenderParser.AppenderBuilder {

        private String follow = StringUtils.FALSE;
        private String immediateFlush = StringUtils.TRUE;
        private String v1Target = V1_SYSTEM_OUT;

        private ConsoleAppenderBuilder(String name) {
            super(name, true);
        }

        public void setFollow(String follow) {
            this.follow = follow;
        }

        public void setImmediateFlush(String immediateFlush) {
            this.immediateFlush = immediateFlush;
        }

        public void setTarget(String v1Target) {
            switch (v1Target) {
                case V1_SYSTEM_OUT:
                case V1_SYSTEM_ERR:
                    this.v1Target = v1Target;
                    break;
                default:
                    throw new ConfigurationConverterException(
                            "Invalid target attribute `" + v1Target + "` for ConsoleAppender " + getName());
            }
        }

        @Override
        public ConfigurationNode get() {
            ConfigurationNodeBuilder builder = ComponentUtils.newNodeBuilder()
                    .setPluginName("Console")
                    .addAttribute("name", getName())
                    .addAttribute(decapitalize(FOLLOW_PARAM), follow)
                    .addAttribute(decapitalize(IMMEDIATE_FLUSH_PARAM), immediateFlush)
                    .addAttribute(
                            decapitalize(TARGET_PARAM), V1_SYSTEM_OUT.equals(v1Target) ? V2_SYSTEM_OUT : V2_SYSTEM_ERR);
            return addStandardChildren(builder).get();
        }
    }
}
