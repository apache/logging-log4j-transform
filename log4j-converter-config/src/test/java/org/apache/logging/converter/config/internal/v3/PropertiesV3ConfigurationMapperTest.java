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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.apache.logging.converter.config.internal.AbstractConfigurationMapperTest;
import org.apache.logging.converter.config.spi.ConfigurationMapper;
import org.apache.logging.converter.config.spi.ConfigurationNode;
import org.apache.logging.converter.config.spi.ConfigurationParser;
import org.junit.jupiter.api.Test;

class PropertiesV3ConfigurationMapperTest extends AbstractConfigurationMapperTest {

    @Test
    void convertFromProperties() throws IOException {
        ConfigurationParser parser = new PropertiesV3ConfigurationMapper();
        try (InputStream inputStream = getClass().getResourceAsStream("/v3/log4j2.properties")) {
            ConfigurationNode actual = parser.parse(Objects.requireNonNull(inputStream));
            assertThat(actual).ignoringOrder().isEqualTo(EXAMPLE_V2_CONFIGURATION);
        }
    }

    @Test
    void convertToPropertiesAndBack() throws IOException {
        ConfigurationMapper mapper = new PropertiesV3ConfigurationMapper();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        mapper.writeConfiguration(outputStream, EXAMPLE_V2_CONFIGURATION);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        ConfigurationNode actual = mapper.parse(inputStream);

        assertThat(actual).ignoringOrder().isEqualTo(EXAMPLE_V2_CONFIGURATION);
    }
}
