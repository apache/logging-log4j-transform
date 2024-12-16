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

import java.util.AbstractMap;
import java.util.Map;
import java.util.Properties;

/**
 * Represents a subset of configuration properties.
 */
public final class PropertiesSubset {

    private final Map.Entry<String, Properties> entry;

    public static PropertiesSubset of(final String prefix, final Properties properties) {
        return new PropertiesSubset(prefix, properties);
    }

    private PropertiesSubset(final String prefix, final Properties properties) {
        this.entry = new AbstractMap.SimpleImmutableEntry<>(prefix, properties);
    }

    /**
     * The common prefix of this subset in the global configuration properties.
     * <p>
     *     Used for debugging messages.
     * </p>
     */
    public String getPrefix() {
        return entry.getKey();
    }

    /**
     * A subset of the global configuration properties with {@link #getPrefix()} removed.
     */
    public Properties getProperties() {
        return entry.getValue();
    }
}
