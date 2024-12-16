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

import java.util.Comparator;
import java.util.Properties;
import java.util.stream.Stream;
import org.apache.logging.converter.config.ConfigurationConverterException;
import org.apache.logging.converter.config.spi.PropertiesSubset;
import org.jspecify.annotations.Nullable;

public final class PropertiesUtils {

    public static String getLastComponent(String name) {
        int idx = name.lastIndexOf('.');
        return idx == -1 ? name : name.substring(idx + 1);
    }

    public static @Nullable String extractProperty(PropertiesSubset subset, String key) {
        return (String) subset.getProperties().remove(key);
    }

    public static PropertiesSubset extractSubset(PropertiesSubset parentSubset, String childPrefix) {
        Properties parentProperties = parentSubset.getProperties();
        Properties properties =
                org.apache.logging.log4j.util.PropertiesUtil.extractSubset(parentProperties, childPrefix);
        String value = (String) parentProperties.remove(childPrefix);
        if (value != null) {
            properties.setProperty("", value);
        }
        return PropertiesSubset.of(addPrefixes(parentSubset.getPrefix(), childPrefix), properties);
    }

    public static Stream<PropertiesSubset> partitionOnCommonPrefixes(PropertiesSubset parentSubset) {
        String parentPrefix = parentSubset.getPrefix();
        String effectivePrefix = parentPrefix.isEmpty() ? parentPrefix : parentPrefix + ".";
        Stream<PropertiesSubset> partitioned = org.apache.logging.log4j.util.PropertiesUtil.partitionOnCommonPrefixes(
                        parentSubset.getProperties(), true)
                .entrySet()
                .stream()
                .map(entry -> PropertiesSubset.of(effectivePrefix + entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(PropertiesSubset::getPrefix));
        parentSubset.getProperties().clear();
        return partitioned;
    }

    private static String addPrefixes(String left, String right) {
        return left.isEmpty() ? right : right.isEmpty() ? left : left + "." + right;
    }

    public static void throwIfNotEmpty(PropertiesSubset subset) {
        Properties properties = subset.getProperties();
        if (!properties.isEmpty()) {
            String prefix = subset.getPrefix();
            if (properties.size() == 1) {
                throw new ConfigurationConverterException("Unknown configuration property '"
                        + addPrefixes(
                                prefix,
                                properties.stringPropertyNames().iterator().next()) + "'.");
            }
            StringBuilder messageBuilder = new StringBuilder("Unknown configuration properties:");
            properties.stringPropertyNames().stream()
                    .map(k -> addPrefixes(prefix, k))
                    .forEach(k -> messageBuilder.append("\n\t").append(k));
            throw new ConfigurationConverterException(messageBuilder.toString());
        }
    }

    /**
     * @return {@code true} if the subset contains no properties.
     */
    public static boolean isEmpty(PropertiesSubset subset) {
        return subset.getProperties().isEmpty();
    }

    /**
     * @return {@code true} if the subset contains some properties.
     */
    public static boolean isNotEmpty(PropertiesSubset subset) {
        return !isEmpty(subset);
    }

    private PropertiesUtils() {}
}
