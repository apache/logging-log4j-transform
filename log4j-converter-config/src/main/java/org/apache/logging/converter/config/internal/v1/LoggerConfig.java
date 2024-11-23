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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import org.apache.logging.converter.config.internal.ComponentUtils;
import org.apache.logging.converter.config.spi.ConfigurationNode;
import org.jspecify.annotations.Nullable;

class LoggerConfig {

    private static final String INHERITED = "INHERITED";
    private static final String NULL = "NULL";

    private final String name;
    private @Nullable String level;
    private final Collection<String> appenderRefs = new ArrayList<>();
    private @Nullable Boolean additivity;

    LoggerConfig(String name) {
        this.name = name;
    }

    void setLevel(String level) {
        this.level = INHERITED.equalsIgnoreCase(level) || NULL.equalsIgnoreCase(level) ? null : level;
    }

    void addAppenderRef(String appenderRef) {
        appenderRefs.add(appenderRef);
    }

    void setLevelAndRefs(String levelAndRefs) {
        String[] values =
                Arrays.stream(levelAndRefs.split(",", -1)).map(String::trim).toArray(String[]::new);
        if (values.length > 0) {
            setLevel(values[0]);
        }
        appenderRefs.clear();
        for (int i = 1; i < values.length; i++) {
            addAppenderRef(values[i]);
        }
    }

    void setAdditivity(String additivity) {
        this.additivity = Boolean.parseBoolean(additivity);
    }

    ConfigurationNode buildLogger() {
        ComponentUtils.ConfigurationNodeBuilder builder = ComponentUtils.newNodeBuilder()
                .setPluginName("Logger")
                .addAttribute("name", name)
                .addAttribute("level", level);
        if (additivity != null) {
            builder.addAttribute("additivity", additivity);
        }
        appenderRefs.forEach(ref -> builder.addChild(ComponentUtils.newAppenderRef(ref)));
        return builder.get();
    }

    ConfigurationNode buildRoot() {
        ComponentUtils.ConfigurationNodeBuilder builder =
                ComponentUtils.newNodeBuilder().setPluginName("Root");
        fillRemainingParameters(builder);
        return builder.get();
    }

    private void fillRemainingParameters(ComponentUtils.ConfigurationNodeBuilder builder) {
        builder.addAttribute("level", level);
        if (additivity != null) {
            builder.addAttribute("additivity", additivity);
        }
        appenderRefs.forEach(ref -> {
            ComponentUtils.ConfigurationNodeBuilder builder1 =
                    ComponentUtils.newNodeBuilder().setPluginName("AppenderRef").addAttribute("ref", ref);
            builder.addChild(builder1.get());
        });
    }
}
