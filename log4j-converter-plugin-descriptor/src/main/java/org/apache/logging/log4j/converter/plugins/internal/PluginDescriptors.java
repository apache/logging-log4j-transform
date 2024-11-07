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
package org.apache.logging.log4j.converter.plugins.internal;

import static org.apache.logging.log4j.converter.plugins.internal.JacksonUtils.assertArrayEnd;
import static org.apache.logging.log4j.converter.plugins.internal.JacksonUtils.assertArrayStart;
import static org.apache.logging.log4j.converter.plugins.internal.JacksonUtils.assertObjectEnd;
import static org.apache.logging.log4j.converter.plugins.internal.JacksonUtils.assertObjectStart;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class PluginDescriptors {

    private static final String PLUGIN_BUILDER_FACTORY =
            "org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory";
    private static final String PLUGIN_FACTORY = "org.apache.logging.log4j.core.config.plugins.PluginFactory";

    private static final Logger logger = LogManager.getLogger(PluginDescriptors.class);

    public static final class PluginDescriptor {
        private final Map<String, Namespace> namespacesByName;

        public PluginDescriptor() {
            this(new TreeMap<>());
        }

        private PluginDescriptor(final Map<String, Namespace> namespacesByName) {
            this.namespacesByName = namespacesByName;
        }

        /**
         * Reads an additional plugin descriptor.
         *
         * @param input An input stream for a `Log4j2Plugins.dat` file.
         */
        public void readPluginDescriptor(final InputStream input) throws IOException {
            try (final DataInputStream dataInput = new DataInputStream(input)) {
                final int namespaceCount = dataInput.readInt();
                for (int i = 0; i < namespaceCount; i++) {
                    final Namespace namespace = Namespace.fromDataInput(dataInput);
                    namespacesByName.merge(namespace.getName(), namespace, Namespace::merge);
                }
            }
        }

        public void toPluginDescriptor(final OutputStream output) throws IOException {
            try (final DataOutputStream dataOutput = new DataOutputStream(output)) {
                dataOutput.writeInt(namespacesByName.size());
                for (final Namespace namespace : namespacesByName.values()) {
                    namespace.writeDataOutput(dataOutput);
                }
            }
        }

        public void readJson(final JsonParser parser) throws IOException {
            assertObjectStart(parser);
            while (parser.nextToken() == JsonToken.FIELD_NAME) {
                final String namespace = parser.currentName();
                namespacesByName.merge(namespace, Namespace.fromJson(parser, namespace), Namespace::merge);
            }
            assertObjectEnd(parser);
        }

        public PluginDescriptor withBuilderHierarchy(final ClassLoader classLoader) {
            final Map<String, Namespace> namespacesByName = new TreeMap<>(this.namespacesByName);
            namespacesByName.replaceAll((k, v) -> v.withBuilderHierarchy(classLoader));
            return new PluginDescriptor(namespacesByName);
        }

        public void toJson(final JsonGenerator generator) throws IOException {
            generator.writeStartObject();
            for (final Namespace namespace : namespacesByName.values()) {
                generator.writeFieldName(namespace.getName());
                namespace.toJson(generator);
            }
            generator.writeEndObject();
        }

        public Stream<Namespace> getNamespaces() {
            return namespacesByName.values().stream();
        }
    }

    /**
     * Represents a namespace of plugins
     */
    public static final class Namespace {

        private final String name;

        private final Map<String, Plugin> pluginsByClassName;

        private Namespace(final String name, final Map<String, Plugin> pluginsByClassName) {
            this.name = name;
            this.pluginsByClassName = pluginsByClassName;
        }

        public String getName() {
            return name;
        }

        public Stream<Plugin> getPlugins() {
            return pluginsByClassName.values().stream();
        }

        static Namespace fromDataInput(final DataInput dataInput) throws IOException {
            final Map<String, Plugin> pluginsByClassName = new TreeMap<>();
            final String namespace = dataInput.readUTF();
            final int pluginCount = dataInput.readInt();
            for (int i = 0; i < pluginCount; i++) {
                final Plugin plugin = Plugin.fromDataInput(dataInput);
                pluginsByClassName.merge(plugin.getClassName(), plugin, Plugin::merge);
            }
            return new Namespace(namespace, Collections.unmodifiableMap(pluginsByClassName));
        }

        void writeDataOutput(final DataOutput dataOutput) throws IOException {
            dataOutput.writeUTF(name);
            final int pluginCount = countPluginNames(pluginsByClassName.values());
            dataOutput.writeInt(pluginCount);
            for (final Plugin plugin : pluginsByClassName.values()) {
                plugin.toDataOutput(dataOutput);
            }
        }

        static Namespace fromJson(final JsonParser parser, final String namespace) throws IOException {
            final Map<String, Plugin> pluginsByClassName = new TreeMap<>();
            assertObjectStart(parser);
            while (parser.nextToken() == JsonToken.FIELD_NAME) {
                final String className = parser.currentName();
                final Plugin plugin = Plugin.fromJson(parser, className);
                pluginsByClassName.put(className, plugin);
            }
            assertObjectEnd(parser);
            return new Namespace(namespace, Collections.unmodifiableMap(pluginsByClassName));
        }

        void toJson(final JsonGenerator generator) throws IOException {
            generator.writeStartObject();
            for (final Plugin plugin : pluginsByClassName.values()) {
                generator.writeFieldName(plugin.getClassName());
                plugin.toJson(generator);
            }
            generator.writeEndObject();
        }

        Namespace merge(final Namespace other) {
            final Map<String, Plugin> pluginsByClassName = new TreeMap<>(this.pluginsByClassName);
            pluginsByClassName.putAll(other.pluginsByClassName);
            return new Namespace(name, Collections.unmodifiableMap(pluginsByClassName));
        }

        Namespace withBuilderHierarchy(final ClassLoader classLoader) {
            final Map<String, Plugin> newPluginsByClassName = new TreeMap<>();
            pluginsByClassName.forEach((k, v) -> {
                try {
                    newPluginsByClassName.put(k, v.withBuilderHierarchy(classLoader));
                } catch (final PluginNotFoundException e) {
                    // No need to bother the user with the full cause
                    // Since we use Simple Logger, we only print the NoClassDefFound message.
                    logger.warn(
                            "Skipping plugin {} because it can not be loaded: {}",
                            e.getMessage(),
                            e.getCause().toString());
                }
            });
            return new Namespace(name, Collections.unmodifiableMap(newPluginsByClassName));
        }

        private int countPluginNames(final Collection<Plugin> plugins) {
            return plugins.stream()
                    .mapToInt(plugin -> plugin.getPluginNames().size())
                    .sum();
        }
    }

    /**
     * Represents a single plugin.
     */
    public static final class Plugin {

        private static final String BUILDER_HIERARCHY = "builderHierarchy";
        private static final String PLUGIN_NAMES = "pluginNames";
        private static final String ELEMENT_NAME = "elementName";
        private static final String PRINTABLE = "printable";
        private static final String DEFER = "defer";

        /**
         * The name and aliases of the plugin.
         */
        private final Set<String> pluginNames;

        /**
         * The name and all the plugin aliases.
         */
        private final String elementName;

        /**
         * The name of the plugin class.
         * <p>
         *   Only present in the serialized representation.
         *   In JSON the className is encoded as key.
         * </p>
         */
        private final String className;

        private final boolean printable;

        private final boolean defer;

        /**
         * The fully qualified class name of the builder and its ancestor.
         * <p>
         *   Only present in the JSON representation.
         * </p>
         */
        private final List<String> builderHierarchy;

        private Plugin(
                final Set<String> pluginNames,
                final String elementName,
                final String className,
                final boolean printable,
                final boolean defer,
                final List<String> builderHierarchy) {
            this.pluginNames = pluginNames;
            this.elementName = elementName;
            this.className = className;
            this.printable = printable;
            this.defer = defer;
            this.builderHierarchy = builderHierarchy;
        }

        public Set<String> getPluginNames() {
            return Collections.unmodifiableSet(pluginNames);
        }

        public String getClassName() {
            return className;
        }

        public List<String> getBuilderHierarchy() {
            return Collections.unmodifiableList(builderHierarchy);
        }

        static Plugin fromDataInput(final DataInput dataInput) throws IOException {
            final String pluginName = dataInput.readUTF();
            final String className = dataInput.readUTF();
            final String elementName = dataInput.readUTF();
            final boolean printable = dataInput.readBoolean();
            final boolean defer = dataInput.readBoolean();
            return new Plugin(
                    Collections.singleton(pluginName),
                    elementName,
                    className,
                    printable,
                    defer,
                    Collections.emptyList());
        }

        void toDataOutput(final DataOutput dataOutput) throws IOException {
            for (final String pluginName : pluginNames) {
                dataOutput.writeUTF(pluginName);
                dataOutput.writeUTF(className);
                dataOutput.writeUTF(elementName);
                dataOutput.writeBoolean(printable);
                dataOutput.writeBoolean(defer);
            }
        }

        static Plugin fromJson(final JsonParser parser, final String className) throws IOException {
            final Set<String> pluginNames = new TreeSet<>();
            final List<String> builderHierarchy = new ArrayList<>();
            String elementName = null;
            boolean printable = false, defer = false;
            assertObjectStart(parser);
            while (parser.nextToken() == JsonToken.FIELD_NAME) {
                switch (parser.currentName()) {
                    case PLUGIN_NAMES:
                        readArray(parser, pluginNames);
                        break;
                    case BUILDER_HIERARCHY:
                        // read and ignore
                        readArray(parser, builderHierarchy);
                        break;
                    case ELEMENT_NAME:
                        elementName = parser.nextTextValue();
                        break;
                    case DEFER:
                        defer = parser.nextBooleanValue();
                        break;
                    case PRINTABLE:
                        printable = parser.nextBooleanValue();
                        break;
                    default:
                        throw new IOException(
                                "Unknown property " + parser.currentName() + " for Plugin element " + className);
                }
            }
            assertObjectEnd(parser);
            return new Plugin(pluginNames, elementName, className, printable, defer, builderHierarchy);
        }

        void toJson(final JsonGenerator generator) throws IOException {
            generator.writeStartObject();
            // Aliases
            generator.writeArrayFieldStart(PLUGIN_NAMES);
            for (final String pluginName : pluginNames) {
                generator.writeString(pluginName);
            }
            generator.writeEndArray();
            // Simple fields
            generator.writeStringField(ELEMENT_NAME, elementName);
            generator.writeBooleanField(PRINTABLE, printable);
            generator.writeBooleanField(DEFER, defer);
            // Compute the class name of the builder
            generator.writeArrayFieldStart(BUILDER_HIERARCHY);
            for (final String fqcn : builderHierarchy) {
                generator.writeString(fqcn);
            }
            generator.writeEndArray();
            generator.writeEndObject();
        }

        private Plugin merge(final Plugin other) {
            // Intentionally ignore `elementName`
            if (!this.className.equals(other.className)
                    || this.printable != other.printable
                    || this.defer != other.defer) {
                throw new IllegalArgumentException(
                        "Attempting to merge incompatible plugins: " + this + " and " + other);
            }
            final Set<String> pluginNames = new TreeSet<>(this.pluginNames);
            pluginNames.addAll(other.pluginNames);
            return new Plugin(
                    Collections.unmodifiableSet(pluginNames),
                    this.elementName,
                    this.className,
                    this.printable,
                    this.defer,
                    this.builderHierarchy);
        }

        /**
         * Computes the builder hierarchy.
         *
         * @param classLoader Class loader to use to load classes.
         * @return A new plugin with the computed builder hierarchy.
         */
        public Plugin withBuilderHierarchy(final ClassLoader classLoader) {
            final List<String> builderHierarchy = Collections.unmodifiableList(findBuilderClassHierarchy(classLoader));
            return new Plugin(pluginNames, elementName, className, printable, defer, builderHierarchy);
        }

        @Override
        public String toString() {
            return "Plugin{" + "pluginNames="
                    + pluginNames + ", elementName='"
                    + elementName + '\'' + ", className='"
                    + className + '\'' + ", printable="
                    + printable + ", defer="
                    + defer + ", builderHierarchy="
                    + builderHierarchy + '}';
        }

        private List<String> findBuilderClassHierarchy(final ClassLoader classLoader) {
            try {
                final Class<?> pluginClass = classLoader.loadClass(className);
                for (final Method method : pluginClass.getMethods()) {
                    for (final Annotation annotation : method.getAnnotations()) {
                        switch (annotation.annotationType().getName()) {
                            case PLUGIN_FACTORY:
                                // Continue the search until apache/logging-log4j2#3126 is fixed.
                                break;
                            case PLUGIN_BUILDER_FACTORY:
                                return computeClassHierarchy(findBuilderClass(method.getGenericReturnType()));
                        }
                    }
                }
                return Collections.emptyList();
            } catch (final ClassNotFoundException | LinkageError e) {
                throw new PluginNotFoundException(pluginNames, e);
            }
        }

        private static Class<?> findBuilderClass(final Type type) {
            if (type instanceof Class) {
                return ((Class<?>) type);
            }
            if (type instanceof ParameterizedType) {
                return findBuilderClass(((ParameterizedType) type).getRawType());
            }
            if (type instanceof TypeVariable) {
                return findBuilderClass(((TypeVariable<?>) type).getBounds()[0]);
            }
            throw new IllegalArgumentException("Unable to handle reflective type: " + type);
        }

        private static List<String> computeClassHierarchy(final Class<?> clazz) {
            final List<String> classes = new ArrayList<>();
            Class<?> current = clazz;
            while (current != null && !current.equals(Object.class)) {
                classes.add(current.getName());
                current = current.getSuperclass();
            }
            return classes;
        }

        private static void readArray(final JsonParser parser, final Collection<? super String> output)
                throws IOException {
            assertArrayStart(parser);
            while (parser.nextToken() == JsonToken.VALUE_STRING) {
                output.add(parser.getText());
            }
            assertArrayEnd(parser);
        }
    }

    private static final class PluginNotFoundException extends RuntimeException {

        private PluginNotFoundException(final Set<String> pluginNames, final Throwable cause) {
            super(pluginNames.toString(), cause);
        }
    }
}
