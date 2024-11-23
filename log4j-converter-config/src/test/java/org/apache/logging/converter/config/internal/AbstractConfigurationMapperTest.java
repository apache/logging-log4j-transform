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

import static org.apache.logging.converter.config.internal.ComponentUtils.newNodeBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.apache.logging.converter.config.spi.ConfigurationNode;
import org.assertj.core.api.AbstractObjectAssert;
import org.jspecify.annotations.Nullable;
import org.opentest4j.AssertionFailedError;

public abstract class AbstractConfigurationMapperTest {

    public static final ConfigurationNode EXAMPLE_V2_CONFIGURATION = newNodeBuilder()
            .setPluginName("Configuration")
            .addAttribute("monitorInterval", "10")
            .addAttribute("name", "Test Configuration")
            .addChild(newNodeBuilder()
                    .setPluginName("Properties")
                    .addChild(newNodeBuilder()
                            .setPluginName("Property")
                            .addAttribute("name", "pattern")
                            .addAttribute("value", "%d [%t] %-5p %c - %m%n%ex")
                            .get())
                    .get())
            .addChild(newNodeBuilder()
                    .setPluginName("CustomLevels")
                    .addChild(newNodeBuilder()
                            .setPluginName("CustomLevel")
                            .addAttribute("name", "CONFIG")
                            .addAttribute("intLevel", "450")
                            .get())
                    .get())
            .addChild(newNodeBuilder()
                    .setPluginName("Appenders")
                    .addChild(newNodeBuilder()
                            .setPluginName("File")
                            .addAttribute("name", "MAIN")
                            .addAttribute("fileName", "main.log")
                            .addChild(newNodeBuilder()
                                    .setPluginName("JsonTemplateLayout")
                                    .get())
                            .get())
                    .addChild(newNodeBuilder()
                            .setPluginName("File")
                            .addAttribute("name", "AUDIT")
                            .addAttribute("fileName", "audit.log")
                            .addChild(newNodeBuilder()
                                    .setPluginName("MarkerFilter")
                                    .addAttribute("marker", "AUDIT")
                                    .get())
                            .addChild(newNodeBuilder()
                                    .setPluginName("PatternLayout")
                                    .addAttribute("pattern", "${pattern}")
                                    .get())
                            .get())
                    .addChild(newNodeBuilder()
                            .setPluginName("Console")
                            .addAttribute("name", "CONSOLE")
                            .addChild(newNodeBuilder()
                                    .setPluginName("Filters")
                                    .addChild(newNodeBuilder()
                                            .setPluginName("ThresholdFilter")
                                            .addAttribute("level", "WARN")
                                            .addAttribute("onMatch", "ACCEPT")
                                            .addAttribute("onMismatch", "NEUTRAL")
                                            .get())
                                    .addChild(newNodeBuilder()
                                            .setPluginName("BurstFilter")
                                            .get())
                                    .get())
                            .addChild(newNodeBuilder()
                                    .setPluginName("JsonTemplateLayout")
                                    .get())
                            .get())
                    .addChild(newNodeBuilder()
                            .setPluginName("File")
                            .addAttribute("name", "DEBUG_LOG")
                            .addAttribute("fileName", "debug.log")
                            .addChild(newNodeBuilder()
                                    .setPluginName("JsonTemplateLayout")
                                    .get())
                            .get())
                    .get())
            .addChild(newNodeBuilder()
                    .setPluginName("Loggers")
                    .addChild(newNodeBuilder()
                            .setPluginName("Root")
                            .addAttribute("level", "INFO")
                            .addChild(newNodeBuilder()
                                    .setPluginName("AppenderRef")
                                    .addAttribute("ref", "MAIN")
                                    .addChild(newNodeBuilder()
                                            .setPluginName("MarkerFilter")
                                            .addAttribute("marker", "PRIVATE")
                                            .get())
                                    .get())
                            .addChild(newNodeBuilder()
                                    .setPluginName("AppenderRef")
                                    .addAttribute("ref", "CONSOLE")
                                    .addChild(newNodeBuilder()
                                            .setPluginName("Filters")
                                            .addChild(newNodeBuilder()
                                                    .setPluginName("ThresholdFilter")
                                                    .addAttribute("level", "WARN")
                                                    .addAttribute("onMatch", "ACCEPT")
                                                    .addAttribute("onMismatch", "NEUTRAL")
                                                    .get())
                                            .addChild(newNodeBuilder()
                                                    .setPluginName("BurstFilter")
                                                    .get())
                                            .get())
                                    .get())
                            .addChild(newNodeBuilder()
                                    .setPluginName("BurstFilter")
                                    .get())
                            .get())
                    .addChild(newNodeBuilder()
                            .setPluginName("Logger")
                            .addAttribute("name", "org.apache.logging")
                            .addAttribute("additivity", "false")
                            .addAttribute("level", "DEBUG")
                            .addChild(newNodeBuilder()
                                    .setPluginName("AppenderRef")
                                    .addAttribute("ref", "AUDIT")
                                    .get())
                            .addChild(newNodeBuilder()
                                    .setPluginName("AppenderRef")
                                    .addAttribute("ref", "DEBUG_LOG")
                                    .get())
                            .addChild(newNodeBuilder()
                                    .setPluginName("Filters")
                                    .addChild(newNodeBuilder()
                                            .setPluginName("ThresholdFilter")
                                            .addAttribute("level", "DEBUG")
                                            .addAttribute("onMatch", "ACCEPT")
                                            .addAttribute("onMismatch", "NEUTRAL")
                                            .get())
                                    .addChild(newNodeBuilder()
                                            .setPluginName("BurstFilter")
                                            .addAttribute("level", "TRACE")
                                            .get())
                                    .get())
                            .get())
                    .get())
            .get();

    public static ConfigurationNodeAssert assertThat(ConfigurationNode node) {
        return new ConfigurationNodeAssert(node, false);
    }

    public static final class ConfigurationNodeAssert
            extends AbstractObjectAssert<ConfigurationNodeAssert, ConfigurationNode> {

        final boolean ignoreOrder;

        private ConfigurationNodeAssert(ConfigurationNode configurationNode, boolean ignoreOrder) {
            super(configurationNode, ConfigurationNodeAssert.class);
            this.ignoreOrder = ignoreOrder;
        }

        public ConfigurationNodeAssert ignoringOrder() {
            return new ConfigurationNodeAssert(this.actual, true);
        }

        public ConfigurationNodeAssert isEqualTo(ConfigurationNode expected) {
            ConfigurationNodeDifference difference = compare("$", expected, actual);
            if (difference != null) {
                String message = String.format(
                        "Expecting configuration nodes to be equal, but actual node at path `%s`:\n%s\nwas different from expected node at that path:\n%s",
                        difference.prefix, difference.actual, difference.expected);
                throw new AssertionFailedError(message, expected, actual);
            }
            return this;
        }

        private @Nullable ConfigurationNodeDifference compare(
                String prefix, @Nullable ConfigurationNode expected, @Nullable ConfigurationNode actual) {
            if (expected == null
                    || actual == null
                    || !expected.getPluginName().equals(actual.getPluginName())
                    || !expected.getAttributes().equals(actual.getAttributes())) {
                return new ConfigurationNodeDifference(prefix, expected, actual);
            }
            List<ConfigurationNode> expectedChildren = new ArrayList<>(expected.getChildren());
            List<ConfigurationNode> actualChildren = new ArrayList<>(actual.getChildren());
            if (ignoreOrder) {
                // This comparator is good enough for now
                Comparator<ConfigurationNode> comparator = Comparator.comparing(ConfigurationNode::getPluginName)
                        .thenComparing(
                                node -> Objects.toString(node.getAttributes().get("name"), ""));
                expectedChildren.sort(comparator);
                actualChildren.sort(comparator);
            }
            Iterator<? extends ConfigurationNode> expectedIterator = expectedChildren.iterator();
            Iterator<? extends ConfigurationNode> actualIterator = actualChildren.iterator();
            ConfigurationNode currentExpected;
            ConfigurationNode currentActual;
            while (true) {
                currentExpected = expectedIterator.hasNext() ? expectedIterator.next() : null;
                currentActual = actualIterator.hasNext() ? actualIterator.next() : null;
                if (currentExpected == null) {
                    return currentActual != null ? new ConfigurationNodeDifference(prefix, expected, actual) : null;
                }
                ConfigurationNodeDifference difference =
                        compare(prefix + "." + currentExpected.getPluginName(), currentExpected, currentActual);
                if (difference != null) {
                    return difference;
                }
            }
        }
    }

    private static final class ConfigurationNodeDifference {

        private final String prefix;

        @Nullable
        private final ConfigurationNode expected;

        @Nullable
        private final ConfigurationNode actual;

        private ConfigurationNodeDifference(
                String prefix, @Nullable ConfigurationNode expected, @Nullable ConfigurationNode actual) {
            this.prefix = prefix;
            this.expected = expected;
            this.actual = actual;
        }
    }
}
