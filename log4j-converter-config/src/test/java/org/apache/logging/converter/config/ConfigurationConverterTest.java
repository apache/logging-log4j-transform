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
package org.apache.logging.converter.config;

import static org.apache.logging.converter.config.internal.AbstractConfigurationMapperTest.EXAMPLE_V2_CONFIGURATION;
import static org.apache.logging.converter.config.internal.AbstractConfigurationMapperTest.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.logging.converter.config.internal.v2.XmlConfigurationMapper;
import org.apache.logging.converter.config.spi.ConfigurationNode;
import org.apache.logging.converter.config.spi.ConfigurationParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ConfigurationConverterTest {

    private static final String DEFAULT_FORMAT = "v2:xml";

    private final ConfigurationConverter converter = ConfigurationConverter.getInstance();
    private final ConfigurationParser parser = new XmlConfigurationMapper();

    public static Stream<Arguments> conversionToXml() {
        return Stream.of(
                Arguments.of("/v2/log4j2.json", "v2:json"),
                Arguments.of("/v2/log4j2.properties", "v2:properties"),
                Arguments.of("/v2/log4j2.xml", DEFAULT_FORMAT),
                Arguments.of("/v2/log4j2.yaml", "v2:yaml"),
                Arguments.of("/v3/log4j2.properties", "v3:properties"));
    }

    @ParameterizedTest
    @MethodSource
    void conversionToXml(String inputResource, String inputFormat) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(inputResource)) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            converter.convert(Objects.requireNonNull(inputStream), inputFormat, outputStream, DEFAULT_FORMAT);
            // Parse the result and check if it matches
            ConfigurationNode actual = parser.parse(new ByteArrayInputStream(outputStream.toByteArray()));
            assertThat(actual).ignoringOrder().isEqualTo(EXAMPLE_V2_CONFIGURATION);
        }
    }

    @Test
    void supportedFormats() {
        assertThat(converter.getSupportedInputFormats())
                .containsExactlyInAnyOrder("v2:json", "v2:properties", DEFAULT_FORMAT, "v2:yaml", "v3:properties");
        assertThat(converter.getSupportedOutputFormats())
                .containsExactlyInAnyOrder("v2:json", DEFAULT_FORMAT, "v2:yaml", "v3:properties");
    }

    @Test
    void throwOnUnsupportedFormat() {
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);
        OutputStream outputStream = new ByteArrayOutputStream();
        String unsupportedFormat = "unsupportedFormat";
        // Messages may vary, but should explain what happened
        assertThatThrownBy(() -> converter.convert(inputStream, unsupportedFormat, outputStream, DEFAULT_FORMAT))
                .isInstanceOf(ConfigurationConverterException.class)
                .hasMessageContaining("input format")
                .hasMessageContaining(unsupportedFormat)
                .hasMessageContaining("not supported");
        assertThatThrownBy(() -> converter.convert(inputStream, DEFAULT_FORMAT, outputStream, unsupportedFormat))
                .isInstanceOf(ConfigurationConverterException.class)
                .hasMessageContaining("output format")
                .hasMessageContaining(unsupportedFormat)
                .hasMessageContaining("not supported");
    }

    @Test
    void throwOnIOException() {
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);
        OutputStream outputStream = new ByteArrayOutputStream();
        assertThatThrownBy(() -> converter.convert(inputStream, DEFAULT_FORMAT, outputStream, DEFAULT_FORMAT))
                .isInstanceOf(ConfigurationConverterException.class)
                .hasCauseInstanceOf(IOException.class);
    }
}
