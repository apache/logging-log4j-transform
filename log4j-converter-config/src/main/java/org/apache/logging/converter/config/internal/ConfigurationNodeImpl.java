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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import org.apache.logging.converter.config.ConfigurationConverterException;
import org.apache.logging.converter.config.spi.ConfigurationNode;
import org.jspecify.annotations.Nullable;

public final class ConfigurationNodeImpl implements ConfigurationNode {

    private final String pluginName;
    private final Map<String, String> attributes = new TreeMap<>();
    private final List<ConfigurationNode> children = new ArrayList<>();

    public static NodeBuilder newNodeBuilder() {
        return new NodeBuilder();
    }

    private ConfigurationNodeImpl(
            final String pluginName,
            final Map<String, String> attributes,
            final Collection<ConfigurationNode> children) {
        this.pluginName = pluginName;
        this.attributes.putAll(attributes);
        this.children.addAll(children);
    }

    @Override
    public String getPluginName() {
        return pluginName;
    }

    @Override
    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    @Override
    public List<? extends ConfigurationNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    private static void formatTo(ConfigurationNode node, StringBuilder builder, int indent) {
        String indentation = getIndentation(indent);
        builder.append(indentation).append("<").append(node.getPluginName());
        for (final Map.Entry<String, String> entry : node.getAttributes().entrySet()) {
            builder.append(" ")
                    .append(entry.getKey())
                    .append("=\"")
                    .append(entry.getValue())
                    .append("\"");
        }
        builder.append(">\n");
        for (ConfigurationNode child : node.getChildren()) {
            formatTo(child, builder, indent + 1);
            builder.append('\n');
        }
        builder.append(indentation).append("</").append(node.getPluginName()).append(">");
    }

    private static String getIndentation(int indent) {
        return String.join("", Collections.nCopies(indent, "  "));
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        formatTo(this, builder, 0);
        return builder.toString();
    }

    public static final class NodeBuilder implements Supplier<ConfigurationNode> {

        private @Nullable String pluginName;
        private final Map<String, String> attributes = new TreeMap<>();
        private final List<ConfigurationNode> children = new ArrayList<>();

        private NodeBuilder() {}

        public NodeBuilder setPluginName(String pluginName) {
            this.pluginName = pluginName;
            return this;
        }

        public NodeBuilder addAttribute(String key, @Nullable String value) {
            if (value != null) {
                attributes.put(key, value);
            }
            return this;
        }

        public NodeBuilder addChild(ConfigurationNode child) {
            children.add(child);
            return this;
        }

        public ConfigurationNode build() {
            if (pluginName == null) {
                throw new ConfigurationConverterException("No plugin name specified");
            }
            return new ConfigurationNodeImpl(pluginName, attributes, children);
        }

        @Override
        public ConfigurationNode get() {
            return build();
        }
    }
}
