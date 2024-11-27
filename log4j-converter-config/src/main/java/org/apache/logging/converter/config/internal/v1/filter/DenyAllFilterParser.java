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

import aQute.bnd.annotation.spi.ServiceProvider;
import java.lang.invoke.MethodHandle;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.logging.converter.config.internal.ComponentUtils;
import org.apache.logging.converter.config.internal.v1.AbstractComponentParser;
import org.apache.logging.converter.config.spi.ConfigurationNode;
import org.apache.logging.converter.config.spi.v1.Log4j1ComponentParser;
import org.apache.logging.converter.config.spi.v1.PropertiesSubset;
import org.w3c.dom.Element;

/**
 * Parses a
 * <a href="https://logging.apache.org/log4j/1.x/apidocs/org/apache/log4j/varia/DenyAllFilter.html">DenyAllFilter</a>
 * configuration.
 */
@ServiceProvider(Log4j1ComponentParser.class)
public class DenyAllFilterParser extends AbstractComponentParser<DenyAllFilterParser.DenyAllFilterBuilder> {

    @Override
    public String getClassName() {
        return "org.apache.log4j.varia.DenyAllFilter";
    }

    @Override
    protected DenyAllFilterBuilder createBuilder(Element element) {
        return new DenyAllFilterBuilder();
    }

    @Override
    protected DenyAllFilterBuilder createBuilder(PropertiesSubset properties) {
        return new DenyAllFilterBuilder();
    }

    @Override
    protected Map<String, ? extends MethodHandle> getAttributeMap() {
        return Collections.emptyMap();
    }

    public static final class DenyAllFilterBuilder implements Supplier<ConfigurationNode> {

        @Override
        public ConfigurationNode get() {
            return ComponentUtils.newNodeBuilder()
                    .setPluginName("DenyAllFilter")
                    .get();
        }
    }
}
