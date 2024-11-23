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

import static org.apache.logging.converter.config.internal.PropertiesUtils.extractProperty;
import static org.apache.logging.converter.config.internal.XmlUtils.requireNonEmpty;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.logging.converter.config.ConfigurationConverterException;
import org.apache.logging.converter.config.internal.PropertiesUtils;
import org.apache.logging.converter.config.internal.StringUtils;
import org.apache.logging.converter.config.internal.XmlUtils;
import org.apache.logging.converter.config.spi.ConfigurationNode;
import org.apache.logging.converter.config.spi.v1.Log4j1ComponentParser;
import org.apache.logging.converter.config.spi.v1.Log4j1ParserContext;
import org.apache.logging.converter.config.spi.v1.PropertiesSubset;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Base class for Log4j 1 component parsers.
 */
public abstract class AbstractComponentParser<T extends Supplier<ConfigurationNode>> implements Log4j1ComponentParser {

    // XML tags
    public static final String PARAM_TAG = "param";

    // XML attributes
    public static final String NAME_ATTR = "name";
    public static final String REF_ATTR = "ref";
    public static final String VALUE_ATTR = "value";

    // V1 parameter names
    protected static final String THRESHOLD_PARAM = "Threshold";

    /**
     * Creates a builder for the configuration node.
     *
     * @param element An XML element.
     */
    protected abstract T createBuilder(Element element);

    /**
     * Creates a builder for the configuration node.
     *
     * @param properties A subset of properties.
     */
    protected abstract T createBuilder(PropertiesSubset properties);

    /**
     * A map between Log4j 1 parameter names and setter methods on the builder.
     */
    protected abstract Map<String, ? extends MethodHandle> getAttributeMap();

    @Override
    public final ConfigurationNode parseXml(Element element, Log4j1ParserContext context)
            throws ConfigurationConverterException {
        T builder = createBuilder(element);
        handleChildrenElements(element, context, getAttributeMap(), builder);
        return builder.get();
    }

    @Override
    public final ConfigurationNode parseProperties(PropertiesSubset properties, Log4j1ParserContext context)
            throws ConfigurationConverterException {
        T builder = createBuilder(properties);
        handleChildrenProperties(properties, context, getAttributeMap(), builder);
        return builder.get();
    }

    /**
     * Handles all child elements, except {@code <param>}.
     *
     * @param childElement     An XML element.
     * @param context          The parser context.
     * @param componentBuilder A builder for the configuration node.
     */
    protected void handleUnknownElement(Element childElement, Log4j1ParserContext context, T componentBuilder) {
        XmlUtils.throwUnknownElement(childElement);
    }

    /**
     * Handles subsets of properties that are not known attributes.
     *
     * @param properties A subset of properties to handle.
     * @param context The parser context.
     * @param componentBuilder A builder for the configuration node.
     */
    protected void handleUnknownProperties(
            PropertiesSubset properties, Log4j1ParserContext context, T componentBuilder) {
        PropertiesUtils.throwIfNotEmpty(properties);
    }

    /**
     * Handles configuration attributes and nested elements.
     *
     * @param element An XML element.
     * @param context A reference to the parser context.
     * @param attributeMap A map from attribute names to setters of the component builder.
     * @param componentBuilder A component builder.
     * @throws ConfigurationConverterException If a parsing error occurs.
     */
    private void handleChildrenElements(
            Node element,
            Log4j1ParserContext context,
            Map<String, ? extends MethodHandle> attributeMap,
            T componentBuilder)
            throws ConfigurationConverterException {
        // Counts children by tag name for error handling purposes
        XmlUtils.forEachChild(element, (Consumer<? super Element>) childElement -> {
            String nodeName = childElement.getNodeName();
            if (nodeName.equals(PARAM_TAG)) {
                // Handle attributes
                String name = childElement.getAttribute(NAME_ATTR);
                MethodHandle attributeSetter = attributeMap.get(name);
                if (attributeSetter == null) {
                    attributeSetter = attributeMap.get(StringUtils.capitalize(name));
                }
                if (attributeSetter != null) {
                    String value = childElement.getAttribute(VALUE_ATTR);
                    if (value.isEmpty()) {
                        throw new ConfigurationConverterException("No value specified for attribute " + name);
                    }
                    invokeAttributeSetter(componentBuilder, attributeSetter, name, value);
                } else {
                    throw new ConfigurationConverterException("Unsupported configuration attribute " + name
                            + " at path " + XmlUtils.getXPathExpression(childElement) + ".");
                }
            } else {
                handleUnknownElement(childElement, context, componentBuilder);
            }
        });
    }

    /**
     * Handles configuration attributes and nested elements.
     *
     * @param properties A subset of properties.
     * @param context A reference to the parser context.
     * @param attributeMap A map from attribute names to setters of the component builder.
     * @param componentBuilder A component builder.
     * @throws ConfigurationConverterException If a parsing error occurs.
     */
    private void handleChildrenProperties(
            PropertiesSubset properties,
            Log4j1ParserContext context,
            Map<String, ? extends MethodHandle> attributeMap,
            T componentBuilder) {
        // Handle attributes
        attributeMap.forEach((attributeName, attributeSetter) -> {
            String value = extractProperty(properties, attributeName);
            if (value == null) {
                value = extractProperty(properties, StringUtils.decapitalize(attributeName));
            }
            if (value != null) {
                invokeAttributeSetter(componentBuilder, attributeSetter, attributeName, value);
            }
        });
        // Handle nested components
        PropertiesUtils.partitionOnCommonPrefixes(properties)
                .forEach(childProperties -> handleUnknownProperties(childProperties, context, componentBuilder));
    }

    private static void invokeAttributeSetter(
            Supplier<ConfigurationNode> builder, MethodHandle setter, String name, String value) {
        try {
            setter.bindTo(builder).invokeExact(value);
        } catch (Throwable e) {
            throw new ConfigurationConverterException("Failed to set attribute " + name, e);
        }
    }

    protected static AttributeMapBuilder attributeMapBuilder(
            Class<? extends Supplier<ConfigurationNode>> componentBuilderClass) {
        return new AttributeMapBuilder(componentBuilderClass);
    }

    protected static ConfigurationNode parseConfigurationElement(Log4j1ParserContext context, Element element) {
        return context.getParserForClass(requireNonEmpty(element, "class")).parseXml(element, context);
    }

    protected static ConfigurationNode parseConfigurationElement(Log4j1ParserContext context, PropertiesSubset subset) {
        String className = extractProperty(subset, "");
        if (className == null) {
            throw new ConfigurationConverterException("The required property '" + subset.getPrefix() + "' is missing.");
        }
        return context.getParserForClass(className).parseProperties(subset, context);
    }

    protected static final class AttributeMapBuilder implements Supplier<Map<String, MethodHandle>> {

        private static final MethodType ATTRIBUTE_SETTER = MethodType.methodType(void.class, String.class);
        private static final MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        private final Map<String, MethodHandle> map;
        private final Class<? extends Supplier<ConfigurationNode>> componentBuilderClass;

        private AttributeMapBuilder(Class<? extends Supplier<ConfigurationNode>> componentBuilderClass) {
            this.map = new HashMap<>();
            this.componentBuilderClass = componentBuilderClass;
        }

        public AttributeMapBuilder add(String name) {
            String setterName = getSetterName(name);
            try {
                map.put(name, lookup.findVirtual(componentBuilderClass, setterName, ATTRIBUTE_SETTER));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new ExceptionInInitializerError(e);
            }
            return this;
        }

        public AttributeMapBuilder addAll(Map<String, ? extends MethodHandle> attributes) {
            map.putAll(attributes);
            return this;
        }

        private static String getSetterName(CharSequence value) {
            StringBuilder builder = new StringBuilder("set");
            int len = value.length();
            if (len > 0) {
                builder.append(Character.toUpperCase(value.charAt(0)));
            }
            if (len > 1) {
                builder.append(value, 1, len);
            }
            return builder.toString();
        }

        @Override
        public Map<String, MethodHandle> get() {
            return Collections.unmodifiableMap(map);
        }
    }
}
