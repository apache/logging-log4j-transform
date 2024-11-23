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
import org.apache.logging.converter.config.ConfigurationConverterException;
import org.apache.logging.converter.config.internal.ComponentUtils;
import org.apache.logging.converter.config.internal.ComponentUtils.ConfigurationNodeBuilder;
import org.apache.logging.converter.config.spi.ConfigurationNode;
import org.apache.logging.converter.config.spi.v1.Log4j1ComponentParser;
import org.apache.logging.converter.config.spi.v1.PropertiesSubset;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * Parses a
 * <a href="https://logging.apache.org/log4j/1.x/apidocs/org/apache/log4j/varia/StringMatchFilter.html">StringMatchFilter</a>
 * configuration.
 */
@ServiceProvider(Log4j1ComponentParser.class)
public class StringMatchFilterParser extends AbstractFilterParser<StringMatchFilterParser.StringMatchFilterBuilder> {

    private static final String STRING_TO_MATCH = "StringToMatch";

    private static final Map<String, MethodHandle> ATTRIBUTE_MAP = attributeMapBuilder(StringMatchFilterBuilder.class)
            .add(ACCEPT_ON_MATCH_PARAM)
            .add(STRING_TO_MATCH)
            .get();

    @Override
    public String getClassName() {
        return "org.apache.log4j.varia.StringMatchFilter";
    }

    @Override
    protected StringMatchFilterBuilder createBuilder(Element element) {
        return new StringMatchFilterBuilder();
    }

    @Override
    protected StringMatchFilterBuilder createBuilder(PropertiesSubset properties) {
        return new StringMatchFilterBuilder();
    }

    @Override
    protected Map<String, ? extends MethodHandle> getAttributeMap() {
        return ATTRIBUTE_MAP;
    }

    public static final class StringMatchFilterBuilder implements Supplier<ConfigurationNode> {

        private boolean acceptOnMatch = false;
        private @Nullable String stringToMatch = null;

        public void setAcceptOnMatch(String acceptOnMatch) {
            this.acceptOnMatch = parseBoolean(acceptOnMatch);
        }

        public void setStringToMatch(String stringToMatch) {
            this.stringToMatch = stringToMatch;
        }

        @Override
        public ConfigurationNode get() {
            if (stringToMatch == null) {
                throw new ConfigurationConverterException("Missing required '" + STRING_TO_MATCH + "' attribute");
            }
            ConfigurationNodeBuilder builder = ComponentUtils.newNodeBuilder()
                    .setPluginName("StringMatchFilter")
                    .addAttribute("text", stringToMatch);
            addOnMatchAndMismatch(builder, acceptOnMatch);
            return builder.get();
        }
    }
}
