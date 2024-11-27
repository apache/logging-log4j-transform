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

import static org.apache.logging.converter.config.internal.ComponentUtils.newNodeBuilder;
import static org.apache.logging.converter.config.internal.ComponentUtils.newThresholdFilter;

import java.util.Arrays;
import org.apache.logging.converter.config.internal.AbstractConfigurationMapperTest;
import org.apache.logging.converter.config.internal.ComponentUtils;
import org.apache.logging.converter.config.internal.ComponentUtils.ConfigurationNodeBuilder;
import org.apache.logging.converter.config.spi.ConfigurationNode;

public class AbstractV1ConfigurationParserTest extends AbstractConfigurationMapperTest {

    static ConfigurationNode EXAMPLE_V1_CONFIGURATION = newNodeBuilder()
            .setPluginName("Configuration")
            .addChild(newNodeBuilder()
                    .setPluginName("Properties")
                    .addChild(newNodeBuilder()
                            .setPluginName("Property")
                            .addAttribute("name", "foo")
                            .addAttribute("value", "bar")
                            .get())
                    .addChild(newNodeBuilder()
                            .setPluginName("Property")
                            .addAttribute("name", "log4j.foo")
                            .addAttribute("value", "baz")
                            .get())
                    .get())
            .addChild(newNodeBuilder()
                    .setPluginName("Appenders")
                    .addChild(newNodeBuilder()
                            .setPluginName("Async")
                            .addAttribute("name", "ASYNC")
                            .addAttribute("blocking", true)
                            .addAttribute("bufferSize", 512)
                            .addAttribute("includeLocation", true)
                            .addChild(newAppenderRef("CONSOLE"))
                            .addChild(newAppenderRef("DAILY_ROLLING"))
                            .addChild(newAppenderRef("FILE"))
                            .addChild(newAppenderRef("ROLLING"))
                            .get())
                    .addChild(newNodeBuilder()
                            .setPluginName("Console")
                            .addAttribute("name", "CONSOLE")
                            .addAttribute("follow", "true")
                            .addAttribute("immediateFlush", "false")
                            .addAttribute("target", "SYSTEM_ERR")
                            .addChild(newThresholdFilter("WARN"))
                            .addChild(newSimpleLayout())
                            .get())
                    .addChild(newNodeBuilder()
                            .setPluginName("RollingFile")
                            .addAttribute("name", "DAILY_ROLLING")
                            .addAttribute("append", false)
                            .addAttribute("bufferSize", 1024)
                            .addAttribute("bufferedIo", true)
                            .addAttribute("fileName", "file.log")
                            .addAttribute("filePattern", "file.log%d{.yyyy_MM_dd}")
                            .addAttribute("immediateFlush", false)
                            .addChild(newThresholdFilter("WARN"))
                            .addChild(newSimpleLayout())
                            .addChild(newNodeBuilder()
                                    .setPluginName("TimeBasedTriggeringPolicy")
                                    .addAttribute("modulate", true)
                                    .get())
                            .get())
                    .addChild(newNodeBuilder()
                            .setPluginName("File")
                            .addAttribute("name", "FILE")
                            .addAttribute("append", false)
                            .addAttribute("bufferSize", 1024)
                            .addAttribute("bufferedIo", true)
                            .addAttribute("fileName", "file.log")
                            .addAttribute("immediateFlush", false)
                            .addChild(newThresholdFilter("WARN"))
                            .addChild(newSimpleLayout())
                            .get())
                    .addChild(newNodeBuilder()
                            .setPluginName("RollingFile")
                            .addAttribute("name", "ROLLING")
                            .addAttribute("append", false)
                            .addAttribute("bufferSize", 1024)
                            .addAttribute("bufferedIo", true)
                            .addAttribute("fileName", "file.log")
                            .addAttribute("filePattern", "file.log.%i")
                            .addAttribute("immediateFlush", false)
                            .addChild(newThresholdFilter("WARN"))
                            .addChild(newSimpleLayout())
                            .addChild(newNodeBuilder()
                                    .setPluginName("SizeBasedTriggeringPolicy")
                                    .addAttribute("size", "10.00 GB")
                                    .get())
                            .addChild(newNodeBuilder()
                                    .setPluginName("DefaultRolloverStrategy")
                                    .addAttribute("fileIndex", "min")
                                    .addAttribute("max", "30")
                                    .get())
                            .get())
                    .addChild(newConsoleAppenderBuilder("FILTERS")
                            .addChild(newCompositeFilter(
                                    newNodeBuilder()
                                            .setPluginName("DenyAllFilter")
                                            .get(),
                                    newNodeBuilder()
                                            .setPluginName("LevelMatchFilter")
                                            .addAttribute("onMatch", "ACCEPT")
                                            .addAttribute("onMismatch", "NEUTRAL")
                                            .addAttribute("level", "WARN")
                                            .get(),
                                    newNodeBuilder()
                                            .setPluginName("LevelRangeFilter")
                                            .addAttribute("onMatch", "ACCEPT")
                                            .addAttribute("onMismatch", "NEUTRAL")
                                            .addAttribute("minLevel", "INFO")
                                            .addAttribute("maxLevel", "DEBUG")
                                            .get(),
                                    newNodeBuilder()
                                            .setPluginName("StringMatchFilter")
                                            .addAttribute("onMatch", "ACCEPT")
                                            .addAttribute("onMismatch", "NEUTRAL")
                                            .addAttribute("text", "Hello")
                                            .get()))
                            .addChild(newSimpleLayout())
                            .get())
                    .addChild(newConsoleAppenderBuilder("HTML")
                            .addChild(newNodeBuilder()
                                    .setPluginName("HtmlLayout")
                                    .addAttribute("locationInfo", true)
                                    .addAttribute("title", "Example HTML Layout")
                                    .get())
                            .get())
                    .addChild(newConsoleAppenderBuilder("PATTERN")
                            .addChild(newNodeBuilder()
                                    .setPluginName("PatternLayout")
                                    .addAttribute("pattern", "%d [%t] %-5p %c - %m%n%ex")
                                    .get())
                            .get())
                    .addChild(newConsoleAppenderBuilder("EPATTERN")
                            .addChild(newNodeBuilder()
                                    .setPluginName("PatternLayout")
                                    .addAttribute("pattern", "%d [%t] %-5p %c - %m%n%ex")
                                    .get())
                            .get())
                    .addChild(newConsoleAppenderBuilder("SIMPLE")
                            .addAttribute("name", "SIMPLE")
                            .addChild(newSimpleLayout())
                            .get())
                    .addChild(newConsoleAppenderBuilder("TTCC")
                            .addChild(newNodeBuilder()
                                    .setPluginName("PatternLayout")
                                    .addAttribute(
                                            "pattern",
                                            "%d{yyyy-MM-dd HH:mm:ss,SSS}{UTC} [%t] %p %c %notEmpty{%NDC }- %m%n")
                                    .get())
                            .get())
                    .get())
            .addChild(newThresholdFilter("INFO"))
            .addChild(newNodeBuilder()
                    .setPluginName("Loggers")
                    .addChild(newNodeBuilder()
                            .setPluginName("Root")
                            .addAttribute("level", "INFO")
                            .addChild(newAppenderRef("CONSOLE"))
                            .get())
                    .addChild(newNodeBuilder()
                            .setPluginName("Logger")
                            .addAttribute("additivity", "false")
                            .addAttribute("level", "DEBUG")
                            .addAttribute("name", "org.apache.logging")
                            .addChild(newAppenderRef("CONSOLE"))
                            .addChild(newAppenderRef("DAILY_ROLLING"))
                            .addChild(newAppenderRef("FILE"))
                            .addChild(newAppenderRef("ROLLING"))
                            .get())
                    .get())
            .get();

    static ConfigurationNode filterOutPlugin(ConfigurationNode node, String pluginName) {
        ConfigurationNodeBuilder builder = newNodeBuilder().setPluginName(node.getPluginName());
        node.getAttributes().forEach(builder::addAttribute);
        for (ConfigurationNode child : node.getChildren()) {
            if (!pluginName.equals(child.getPluginName())) {
                builder.addChild(filterOutPlugin(child, pluginName));
            }
        }
        return builder.get();
    }

    private static ConfigurationNode newSimpleLayout() {
        return newNodeBuilder()
                .setPluginName("PatternLayout")
                .addAttribute("alwaysWriteExceptions", false)
                .addAttribute("pattern", "%p - %m%n")
                .get();
    }

    private static ConfigurationNode newAppenderRef(String ref) {
        return ComponentUtils.newAppenderRef(ref);
    }

    private static ConfigurationNode newCompositeFilter(ConfigurationNode... filters) {
        return ComponentUtils.newCompositeFilter(Arrays.asList(filters));
    }

    private static ConfigurationNodeBuilder newConsoleAppenderBuilder(String name) {
        return newNodeBuilder()
                .setPluginName("Console")
                .addAttribute("name", name)
                .addAttribute("follow", false)
                .addAttribute("immediateFlush", true)
                .addAttribute("target", "SYSTEM_OUT");
    }
}
