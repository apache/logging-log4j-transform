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
 * <a href="https://logging.apache.org/log4j/1.x/apidocs/org/apache/log4j/varia/LevelRangeFilter.html">LevelRangeFilter</a>
 * configuration.
 */
@ServiceProvider(Log4j1ComponentParser.class)
public class LevelRangeFilterParser extends AbstractFilterParser<LevelRangeFilterParser.LevelRangeFilterBuilder> {

    private static final String LEVEL_MAX_PARAM = "LevelMax";
    private static final String LEVEL_MIN_PARAM = "LevelMin";

    private static final Map<String, MethodHandle> ATTRIBUTE_MAP = attributeMapBuilder(LevelRangeFilterBuilder.class)
            .add(ACCEPT_ON_MATCH_PARAM)
            .add(LEVEL_MAX_PARAM)
            .add(LEVEL_MIN_PARAM)
            .get();

    @Override
    public String getClassName() {
        return "org.apache.log4j.varia.LevelRangeFilter";
    }

    @Override
    protected LevelRangeFilterBuilder createBuilder(Element element) {
        return new LevelRangeFilterBuilder();
    }

    @Override
    protected LevelRangeFilterBuilder createBuilder(PropertiesSubset properties) {
        return new LevelRangeFilterBuilder();
    }

    @Override
    protected Map<String, ? extends MethodHandle> getAttributeMap() {
        return ATTRIBUTE_MAP;
    }

    public static final class LevelRangeFilterBuilder implements Supplier<ConfigurationNode> {

        private boolean acceptOnMatch = false;
        private String levelMax = "OFF";
        private String levelMin = "ALL";

        public void setAcceptOnMatch(String acceptOnMatch) {
            this.acceptOnMatch = parseBoolean(acceptOnMatch);
        }

        public void setLevelMax(String level) {
            this.levelMax = level;
        }

        public void setLevelMin(String levelMin) {
            this.levelMin = levelMin;
        }

        @Override
        public ConfigurationNode get() {
            // log4j1 order: ALL < TRACE < DEBUG < ... < FATAL < OFF
            // log4j2 order: ALL > TRACE > DEBUG > ... > FATAL > OFF
            ConfigurationNodeBuilder builder = ComponentUtils.newNodeBuilder()
                    .setPluginName("LevelRangeFilter")
                    .addAttribute("minLevel", levelMax)
                    .addAttribute("maxLevel", levelMin);
            addOnMatchAndMismatch(builder, acceptOnMatch);
            return builder.get();
        }
    }
}
