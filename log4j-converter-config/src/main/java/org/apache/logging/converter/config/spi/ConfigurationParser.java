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
package org.apache.logging.converter.config.spi;

import java.io.IOException;
import java.io.InputStream;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Interface to be implemented by format providers that can parse a configuration file format.
 * <p>
 *     Implementations of this class should be registered with {@link java.util.ServiceLoader}.
 * </p>
 */
@ProviderType
public interface ConfigurationParser {

    /**
     * A short string that identifies the format supported by this parser.
     * <p>
     *     For example, "v2:xml" or "v1:properties".
     * </p>
     */
    String getInputFormat();

    /**
     * Parses a configuration file into a tree of configuration nodes.
     * <p>
     *     The returned tree should represent a valid Log4j Core 2 configuration.
     * </p>
     * @param inputStream The input configuration file.
     * @return A tree of configuration nodes.
     * @throws IOException If a parsing error occurs.
     */
    ConfigurationNode parse(InputStream inputStream) throws IOException;
}
