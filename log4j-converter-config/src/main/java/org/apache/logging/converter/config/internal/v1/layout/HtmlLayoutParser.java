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
package org.apache.logging.converter.config.internal.v1.layout;

import aQute.bnd.annotation.spi.ServiceProvider;
import java.lang.invoke.MethodHandle;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.logging.converter.config.internal.ComponentUtils;
import org.apache.logging.converter.config.internal.StringUtils;
import org.apache.logging.converter.config.internal.v1.AbstractComponentParser;
import org.apache.logging.converter.config.spi.ConfigurationNode;
import org.apache.logging.converter.config.spi.PropertiesSubset;
import org.apache.logging.converter.config.spi.v1.Log4j1ComponentParser;
import org.w3c.dom.Element;

/**
 * Parses a
 * <a href="https://logging.apache.org/log4j/1.x/apidocs/org/apache/log4j/HTMLLayout.html">HTMLLayout</a>
 * configuration.
 */
@ServiceProvider(Log4j1ComponentParser.class)
public class HtmlLayoutParser extends AbstractComponentParser<HtmlLayoutParser.HtmlLayoutBuilder> {

    private static final String LOCATION_INFO_PARAM = "LocationInfo";
    private static final String TITLE_PARAM = "Title";
    private static final String TITLE_DEFAULT = "Log4J Log Messages";

    private static final Map<String, MethodHandle> ATTRIBUTE_MAP = attributeMapBuilder(HtmlLayoutBuilder.class)
            .add(LOCATION_INFO_PARAM)
            .add(TITLE_PARAM)
            .get();

    @Override
    public String getClassName() {
        return "org.apache.log4j.HTMLLayout";
    }

    @Override
    protected HtmlLayoutBuilder createBuilder(Element element) {
        return new HtmlLayoutBuilder();
    }

    @Override
    protected HtmlLayoutBuilder createBuilder(PropertiesSubset properties) {
        return new HtmlLayoutBuilder();
    }

    @Override
    protected Map<String, ? extends MethodHandle> getAttributeMap() {
        return ATTRIBUTE_MAP;
    }

    public static final class HtmlLayoutBuilder implements Supplier<ConfigurationNode> {

        private String title = TITLE_DEFAULT;
        private String locationInfo = StringUtils.FALSE;

        private HtmlLayoutBuilder() {}

        public void setTitle(String title) {
            this.title = title;
        }

        public void setLocationInfo(String locationInfo) {
            this.locationInfo = locationInfo;
        }

        @Override
        public ConfigurationNode get() {
            ComponentUtils.ConfigurationNodeBuilder builder =
                    ComponentUtils.newNodeBuilder().setPluginName("HtmlLayout");
            builder.addAttribute("title", title);
            builder.addAttribute("locationInfo", locationInfo);
            return builder.get();
        }
    }
}
