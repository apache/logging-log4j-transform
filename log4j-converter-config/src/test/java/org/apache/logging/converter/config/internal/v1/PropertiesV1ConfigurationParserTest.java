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
package org.apache.logging.converter.config.internal.v1;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.apache.logging.converter.config.spi.ConfigurationNode;
import org.apache.logging.converter.config.spi.ConfigurationParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PropertiesV1ConfigurationParserTest extends AbstractV1ConfigurationParserTest {

    @ParameterizedTest
    @ValueSource(strings = {"/v1/log4j-uppercase.properties", "/v1/log4j-lowercase.properties"})
    void convertFromProperties(String resource) throws IOException {
        ConfigurationParser parser = new PropertiesV1ConfigurationParser();
        try (InputStream inputStream = getClass().getResourceAsStream(resource)) {
            ConfigurationNode actual = parser.parse(Objects.requireNonNull(inputStream));
            assertThat(actual).ignoringOrder().isEqualTo(filterOutPlugin(EXAMPLE_V1_CONFIGURATION, "Async"));
        }
    }
}
