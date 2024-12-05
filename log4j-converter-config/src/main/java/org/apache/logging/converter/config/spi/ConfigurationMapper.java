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

import org.osgi.annotation.versioning.ProviderType;

/**
 * Interface for format provider classes that implement a parser and a writer at the same time.
 * <p>
 *     Implementations of this class should be registered with {@link java.util.ServiceLoader}.
 * </p>
 */
@ProviderType
public interface ConfigurationMapper extends ConfigurationParser, ConfigurationWriter {

    /**
     * A short string that identifies the format supported by this mapper.
     * <p>
     *     For example, "v2:xml" or "v1:properties".
     * </p>
     */
    String getFormat();

    /**
     * A description of the supported format for self-documentation purposes.
     */
    String getFormatDescription();

    @Override
    default String getInputFormat() {
        return getFormat();
    }

    @Override
    default String getInputFormatDescription() {
        return getFormatDescription();
    }

    @Override
    default String getOutputFormat() {
        return getFormat();
    }

    @Override
    default String getOutputFormatDescription() {
        return getFormatDescription();
    }
}
