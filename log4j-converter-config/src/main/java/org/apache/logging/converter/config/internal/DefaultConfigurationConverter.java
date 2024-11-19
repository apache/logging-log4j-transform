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
package org.apache.logging.converter.config.internal;

import static java.util.Objects.requireNonNull;

import aQute.bnd.annotation.Cardinality;
import aQute.bnd.annotation.Resolution;
import aQute.bnd.annotation.spi.ServiceConsumer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import org.apache.logging.converter.config.ConfigurationConverter;
import org.apache.logging.converter.config.ConfigurationConverterException;
import org.apache.logging.converter.config.spi.ConfigurationMapper;
import org.apache.logging.converter.config.spi.ConfigurationNode;
import org.apache.logging.converter.config.spi.ConfigurationParser;
import org.apache.logging.converter.config.spi.ConfigurationWriter;

@ServiceConsumer(
        value = ConfigurationParser.class,
        cardinality = Cardinality.MULTIPLE,
        resolution = Resolution.OPTIONAL)
@ServiceConsumer(
        value = ConfigurationWriter.class,
        cardinality = Cardinality.MULTIPLE,
        resolution = Resolution.OPTIONAL)
@ServiceConsumer(
        value = ConfigurationMapper.class,
        cardinality = Cardinality.MULTIPLE,
        resolution = Resolution.OPTIONAL)
public class DefaultConfigurationConverter implements ConfigurationConverter {

    private final Map<String, ConfigurationParser> parsers = new HashMap<>();
    private final Map<String, ConfigurationWriter> writers = new HashMap<>();

    public DefaultConfigurationConverter() {
        ServiceLoader.load(ConfigurationParser.class).forEach(parser -> parsers.put(parser.getInputFormat(), parser));
        ServiceLoader.load(ConfigurationWriter.class).forEach(writer -> writers.put(writer.getOutputFormat(), writer));
        ServiceLoader.load(ConfigurationMapper.class).forEach(mapper -> {
            parsers.put(mapper.getInputFormat(), mapper);
            writers.put(mapper.getOutputFormat(), mapper);
        });
    }

    @Override
    public void convert(InputStream inputStream, String inputFormat, OutputStream outputStream, String outputFormat) {
        requireNonNull(inputStream, "inputStream");
        requireNonNull(inputFormat, "inputFormat");
        requireNonNull(outputStream, "outputStream");
        requireNonNull(outputFormat, "outputFormat");

        ConfigurationParser parser = parsers.get(inputFormat);
        if (parser == null) {
            throw new ConfigurationConverterException("The input format `" + inputFormat + "` is not supported.");
        }
        ConfigurationWriter writer = writers.get(outputFormat);
        if (writer == null) {
            throw new ConfigurationConverterException("The output format `" + outputFormat + "` is not supported.");
        }

        try {
            ConfigurationNode configuration = parser.parse(inputStream);
            writer.writeConfiguration(outputStream, configuration);
        } catch (IOException e) {
            throw new ConfigurationConverterException(
                    "Failed to convert configuration from format " + inputFormat + " to format " + outputFormat, e);
        }
    }

    @Override
    public Set<String> getSupportedInputFormats() {
        return Collections.unmodifiableSet(parsers.keySet());
    }

    @Override
    public Set<String> getSupportedOutputFormats() {
        return Collections.unmodifiableSet(writers.keySet());
    }
}
