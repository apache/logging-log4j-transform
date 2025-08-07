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
package org.apache.logging.converter.config.internal.v2;

import static org.apache.logging.converter.config.internal.ComponentUtils.newNodeBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import org.apache.logging.converter.config.internal.ComponentUtils.ConfigurationNodeBuilder;
import org.apache.logging.converter.config.spi.ConfigurationMapper;
import org.apache.logging.converter.config.spi.ConfigurationNode;
import org.jspecify.annotations.Nullable;

public abstract class AbstractJacksonConfigurationMapper implements ConfigurationMapper {

    private static final String CONFIGURATION_FIELD_NAME = "Configuration";
    private static final String TYPE_ATTRIBUTE = "type";

    private final ObjectMapper mapper;
    private final boolean wrapRootObject;

    protected AbstractJacksonConfigurationMapper(ObjectMapper mapper, boolean wrapRootObject) {
        this.mapper = mapper;
        this.wrapRootObject = wrapRootObject;
    }

    @Override
    public ConfigurationNode parse(InputStream inputStream) throws IOException {
        JsonNode documentNode = mapper.readTree(inputStream);

        final JsonNode configurationNode;
        if (wrapRootObject) {
            configurationNode = documentNode.get(CONFIGURATION_FIELD_NAME);
            if (configurationNode == null || !configurationNode.isObject()) {
                throw new IOException("Unable to find " + CONFIGURATION_FIELD_NAME + " field.");
            }
        } else {
            configurationNode = documentNode;
        }
        return parseObjectNode((ObjectNode) configurationNode, CONFIGURATION_FIELD_NAME);
    }

    @Override
    public void writeConfiguration(OutputStream outputStream, ConfigurationNode configuration) throws IOException {
        JsonNodeFactory nodeFactory = mapper.getNodeFactory();

        ObjectNode configurationNode = convertToObjectNode(configuration, nodeFactory, false);
        final ObjectNode documentNode = wrapRootObject
                ? nodeFactory.objectNode().set(configuration.getPluginName(), configurationNode)
                : configurationNode;
        mapper.writeValue(outputStream, documentNode);
        outputStream.write('\n');
        outputStream.close();
    }

    private static ConfigurationNode parseObjectNode(ObjectNode objectNode, String fieldName) {
        ConfigurationNodeBuilder builder = newNodeBuilder();
        objectNode.fields().forEachRemaining(entry -> {
            String childFieldName = entry.getKey();
            JsonNode childNode = entry.getValue();
            if (childNode.isObject()) {
                builder.addChild(parseObjectNode((ObjectNode) childNode, childFieldName));
            }
            if (childNode.isArray()) {
                processArrayNode((ArrayNode) childNode, childFieldName, builder);
            }
            if (childNode.isValueNode() && !TYPE_ATTRIBUTE.equals(childFieldName)) {
                builder.addAttribute(childFieldName, childNode.asText());
            }
        });
        return builder.setPluginName(getPluginName(objectNode, fieldName)).get();
    }

    private static void processArrayNode(ArrayNode arrayNode, String fieldName, ConfigurationNodeBuilder builder) {
        arrayNode.elements().forEachRemaining(childNode -> {
            if (childNode.isObject()) {
                builder.addChild(parseObjectNode((ObjectNode) childNode, fieldName));
            }
        });
    }

    private static String getPluginName(JsonNode jsonNode, String fieldName) {
        JsonNode typeNode = jsonNode.get(TYPE_ATTRIBUTE);
        String typeAttribute = typeNode != null ? typeNode.textValue() : null;
        return typeAttribute != null ? typeAttribute : fieldName;
    }

    /**
     * Returns true, if the configuration node needs to be serialized with an {@code type} attribute.
     *
     * @param configurationNode A configuration node.
     */
    protected boolean requiresExplicitTypeAttribute(ConfigurationNode configurationNode) {
        return false;
    }

    private ObjectNode convertToObjectNode(
            ConfigurationNode configurationNode, JsonNodeFactory nodeFactory, boolean explicitTypeAttribute) {
        ObjectNode objectNode = nodeFactory.objectNode();
        if (explicitTypeAttribute || requiresExplicitTypeAttribute(configurationNode)) {
            objectNode.set(TYPE_ATTRIBUTE, nodeFactory.textNode(configurationNode.getPluginName()));
        }
        configurationNode.getAttributes().forEach(objectNode::put);
        // If multiple nodes have the same name, we need to use JSON arrays or explicit type attributes.
        // For a given plugin type, the strategy is:
        // * For the first group of nodes with the given type we use an array.
        // * If there is a second group of nodes with the same plugin name, we use an explicit type attribute.
        //
        // This strategy does not require us to reorder the children elements, which is not a functionally neutral
        // operation
        // (e.g., for filters).
        Iterator<? extends ConfigurationNode> childrenIterator =
                configurationNode.getChildren().iterator();
        Collection<String> visitedPluginNames = new HashSet<>();
        Collection<ConfigurationNode> similarChildren = new ArrayList<>();
        @Nullable String similarChildrenPluginName = null;
        int counter = 1;
        while (childrenIterator.hasNext()) {
            ConfigurationNode child = childrenIterator.next();
            String childPluginName = child.getPluginName();
            // If the plugin name is the same, add to the current group
            if (childPluginName.equals(similarChildrenPluginName)) {
                similarChildren.add(child);
                continue;
            }
            // We have a new group
            if (!similarChildren.isEmpty()) {
                // Flush the previous group
                JsonNode childNode = convertToJsonNode(similarChildren, nodeFactory);
                String childKey = childNode instanceof ObjectNode && childNode.get(TYPE_ATTRIBUTE) != null
                        ? "id" + counter++
                        : similarChildrenPluginName;
                objectNode.set(childKey, convertToJsonNode(similarChildren, nodeFactory));
            }
            if (visitedPluginNames.add(childPluginName)) {
                // 1. This is the first group with the given plugin name:
                similarChildrenPluginName = childPluginName;
                similarChildren.clear();
                similarChildren.add(child);
            } else {
                // 2. Use an explicit type attribute
                objectNode.set("id" + counter++, convertToObjectNode(child, nodeFactory, true));
            }
        }
        // Flush the last group
        if (!similarChildren.isEmpty()) {
            JsonNode childNode = convertToJsonNode(similarChildren, nodeFactory);
            String childKey = childNode instanceof ObjectNode && childNode.get(TYPE_ATTRIBUTE) != null
                    ? "id" + counter
                    : similarChildrenPluginName;
            objectNode.set(childKey, convertToJsonNode(similarChildren, nodeFactory));
        }
        return objectNode;
    }

    private JsonNode convertToJsonNode(Collection<? extends ConfigurationNode> node, JsonNodeFactory nodeFactory) {
        if (node.size() == 1) {
            return convertToObjectNode(node.iterator().next(), nodeFactory, false);
        }
        ArrayNode arrayNode = nodeFactory.arrayNode();
        for (ConfigurationNode child : node) {
            arrayNode.add(convertToObjectNode(child, nodeFactory, false));
        }
        return arrayNode;
    }
}
