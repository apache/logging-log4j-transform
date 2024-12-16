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
package org.apache.logging.converter.config.internal.v1.filter;

import static org.apache.logging.converter.config.internal.StringUtils.parseBoolean;

import aQute.bnd.annotation.spi.ServiceProvider;
import java.lang.invoke.MethodHandle;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.logging.converter.config.internal.ComponentUtils;
import org.apache.logging.converter.config.internal.ComponentUtils.ConfigurationNodeBuilder;
import org.apache.logging.converter.config.spi.ConfigurationNode;
import org.apache.logging.converter.config.spi.PropertiesSubset;
import org.apache.logging.converter.config.spi.v1.Log4j1ComponentParser;
import org.w3c.dom.Element;

/**
 * Parses a
 * <a href="https://logging.apache.org/log4j/1.x/apidocs/org/apache/log4j/varia/LevelMatchFilter.html">LevelMatchFilter</a>
 * configuration.
 */
@ServiceProvider(Log4j1ComponentParser.class)
public class LevelMatchFilterParser extends AbstractFilterParser<LevelMatchFilterParser.LevelMatchFilterBuilder> {

    private static final String ACCEPT_ON_MATCH = "AcceptOnMatch";
    private static final String LEVEL_TO_MATCH = "LevelToMatch";

    private static final Map<String, MethodHandle> ATTRIBUTE_MAP = attributeMapBuilder(
                    LevelMatchFilterParser.LevelMatchFilterBuilder.class)
            .add(ACCEPT_ON_MATCH)
            .add(LEVEL_TO_MATCH)
            .get();

    @Override
    public String getClassName() {
        return "org.apache.log4j.varia.LevelMatchFilter";
    }

    @Override
    protected LevelMatchFilterBuilder createBuilder(Element element) {
        return new LevelMatchFilterBuilder();
    }

    @Override
    protected LevelMatchFilterBuilder createBuilder(PropertiesSubset properties) {
        return new LevelMatchFilterBuilder();
    }

    @Override
    protected Map<String, ? extends MethodHandle> getAttributeMap() {
        return ATTRIBUTE_MAP;
    }

    public static final class LevelMatchFilterBuilder implements Supplier<ConfigurationNode> {

        private boolean acceptOnMatch = false;
        private String level = "ERROR";

        public void setAcceptOnMatch(String acceptOnMatch) {
            this.acceptOnMatch = parseBoolean(acceptOnMatch);
        }

        public void setLevelToMatch(String level) {
            this.level = level;
        }

        @Override
        public ConfigurationNode get() {
            ConfigurationNodeBuilder builder = ComponentUtils.newNodeBuilder()
                    .setPluginName("LevelMatchFilter")
                    .addAttribute("level", level);
            addOnMatchAndMismatch(builder, acceptOnMatch);
            return builder.get();
        }
    }
}
