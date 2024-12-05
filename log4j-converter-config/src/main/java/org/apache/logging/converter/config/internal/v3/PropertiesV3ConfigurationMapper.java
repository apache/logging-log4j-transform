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
package org.apache.logging.converter.config.internal.v3;

import aQute.bnd.annotation.Resolution;
import aQute.bnd.annotation.spi.ServiceProvider;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import java.io.IOException;
import java.io.InputStream;
import org.apache.logging.converter.config.internal.v2.AbstractJacksonConfigurationMapper;
import org.apache.logging.converter.config.spi.ConfigurationMapper;
import org.apache.logging.converter.config.spi.ConfigurationNode;

@ServiceProvider(value = ConfigurationMapper.class, resolution = Resolution.MANDATORY)
public class PropertiesV3ConfigurationMapper extends AbstractJacksonConfigurationMapper {

    private static final String LOG4J_V3_PROPERTIES_FORMAT = "v3:properties";

    public PropertiesV3ConfigurationMapper() {
        super(
                JavaPropsMapper.builder()
                        .enable(JsonParser.Feature.ALLOW_COMMENTS)
                        .build(),
                false);
    }

    @Override
    public String getFormat() {
        return LOG4J_V3_PROPERTIES_FORMAT;
    }

    @Override
    public String getFormatDescription() {
        return "Log4j Core 3 Properties configuration format.";
    }

    @Override
    public ConfigurationNode parse(InputStream inputStream) throws IOException {
        return super.parse(inputStream);
    }

    @Override
    protected boolean requiresExplicitTypeAttribute(ConfigurationNode configurationNode) {
        // Empty nodes need a `type` attribute in the Properties format
        return configurationNode.getAttributes().isEmpty()
                && configurationNode.getChildren().isEmpty();
    }
}
