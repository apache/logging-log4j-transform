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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import org.apache.logging.converter.config.internal.DefaultConfigurationConverter;

/**
 * Service class to convert between different logging configuration formats.
 */
public interface ConfigurationConverter {

    /**
     * A default implementation of {@link ConfigurationConverter} that uses {@link java.util.ServiceLoader} to load additional formats.
     * @see org.apache.logging.converter.config.spi.ConfigurationMapper
     */
    static ConfigurationConverter getInstance() {
        return DefaultConfigurationConverter.INSTANCE;
    }

    /**
     * Converts a logging configuration file from one format to another.
     *
     * @param inputStream The input configuration file, never {@code null}.
     * @param inputFormat The input format. Must be one of the formats returned by {@link #getSupportedInputFormats()}.
     * @param outputStream The output configuration file, never {@code null}.
     * @param outputFormat The output format. Must be one of the formats returned by {@link #getSupportedOutputFormats()}.
     * @throws ConfigurationConverterException If any kind of error occurs during the conversion process.
     */
    void convert(InputStream inputStream, String inputFormat, OutputStream outputStream, String outputFormat)
            throws ConfigurationConverterException;

    /**
     * Returns the list of supported input formats.
     */
    Set<String> getSupportedInputFormats();

    /**
     * Returns the list of supported output formats.
     */
    Set<String> getSupportedOutputFormats();
}
