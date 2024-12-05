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
import java.io.OutputStream;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Interface to be implemented by format providers that can write a configuration file format.
 * <p>
 *     Implementations of this class should be registered with {@link java.util.ServiceLoader}.
 * </p>
 */
@ProviderType
public interface ConfigurationWriter {

    /**
     * A short string that identifies the format supported by this writer.
     * <p>
     *     For example, "v2:xml" or "v1:properties".
     * </p>
     */
    String getOutputFormat();

    /**
     * A description of the supported format for self-documentation purposes.
     */
    String getOutputFormatDescription();

    /**
     * Write a tree of configuration nodes to a configuration file.
     * <p>
     *     The returned tree should represent a valid Log4j Core 2 configuration.
     * </p>
     * @param outputStream The output configuration file.
     * @param configuration A tree of configuration nodes.
     * @throws IOException If a writing error occurs.
     */
    void writeConfiguration(OutputStream outputStream, ConfigurationNode configuration) throws IOException;
}
