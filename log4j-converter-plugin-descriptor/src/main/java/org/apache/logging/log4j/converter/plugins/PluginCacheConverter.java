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
package org.apache.logging.log4j.converter.plugins;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import org.apache.logging.log4j.converter.plugins.internal.PluginDescriptors.Namespace;
import org.apache.logging.log4j.converter.plugins.internal.PluginDescriptors.PluginDescriptor;
import org.apache.logging.log4j.converter.plugins.internal.ReflectConfigFilter;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "convert")
public class PluginCacheConverter {

    private static final JsonFactory jsonFactory = new ObjectMapper().getFactory();

    public static void main(final String[] args) {
        System.exit(new CommandLine(new PluginCacheConverter()).execute(args));
    }

    @Command
    public void toJson(
            @Parameters(description = "Classpath containing Log4j Core plugins") final String classPath,
            @Parameters(description = "Output file", defaultValue = "Log4j2Plugins.json") final File outputFile)
            throws IOException {
        new PluginDescriptorToJsonConverter(classPath, outputFile).call();
    }

    @Command
    public void fromJson(
            @Parameters(description = "Input JSON file") final File input,
            @Parameters(description = "Output `Log4j2Plugins.dat` file", defaultValue = "Log4j2Plugins.dat")
                    final File output)
            throws IOException {
        new JsonToPluginDescriptorConverter(input, output).call();
    }

    @Command
    public void filterReflectConfig(
            @Parameters(description = "Plugin descriptor (as JSON)") final File pluginDescriptor,
            @Parameters(description = "Reflection configuration") final File reflectConfigInput)
            throws IOException {
        new ReflectConfigTransformer(pluginDescriptor, reflectConfigInput).call();
    }

    private static final class PluginDescriptorToJsonConverter implements Callable<@Nullable Void> {

        private static final String PLUGIN_CACHE_FILE =
                "META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat";

        private final ClassLoader classLoader;

        private final File outputFile;

        public PluginDescriptorToJsonConverter(final String classPath, final File outputFile) {
            final URL[] urls = validateInput(classPath);
            this.classLoader =
                    AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> new URLClassLoader(urls));
            this.outputFile = outputFile;
        }

        @Override
        public @Nullable Void call() throws IOException {
            try (final OutputStream output = Files.newOutputStream(outputFile.toPath());
                    final JsonGenerator generator = jsonFactory.createGenerator(output)) {
                final PluginDescriptor pluginDescriptor = new PluginDescriptor();
                // Read all `Log4j2Plugins.dat` file on the classpath
                for (final URL url : Collections.list(classLoader.getResources(PLUGIN_CACHE_FILE))) {
                    try (final BufferedInputStream input = new BufferedInputStream(url.openStream())) {
                        pluginDescriptor.readPluginDescriptor(input);
                    }
                }
                // Write JSON file
                pluginDescriptor.withBuilderHierarchy(classLoader).toJson(generator);
            }
            return null;
        }

        private static URL[] validateInput(final String classPath) {
            final String[] classPathElements = classPath.split(File.pathSeparator, -1);
            return Arrays.stream(classPathElements)
                    .map(classPathElement -> {
                        final Path inputPath = Paths.get(classPathElement);
                        if (!Files.isRegularFile(inputPath)) {
                            throw new IllegalArgumentException("Input file " + inputPath + " is not a file.");
                        }
                        if (!inputPath.getFileName().toString().endsWith(".jar")) {
                            throw new IllegalArgumentException(
                                    "Invalid input file, only JAR files are supported: " + inputPath);
                        }
                        try {
                            return inputPath.toUri().toURL();
                        } catch (final MalformedURLException e) {

                            throw new IllegalArgumentException(
                                    "Innput file " + inputPath + " can not be accessed as URL.", e);
                        }
                    })
                    .toArray(URL[]::new);
        }
    }

    private static class JsonToPluginDescriptorConverter implements Callable<@Nullable Void> {

        private final Path input;
        private final Path output;

        public JsonToPluginDescriptorConverter(final File input, final File output) {
            this.input = input.toPath();
            this.output = output.toPath();
        }

        @Override
        public @Nullable Void call() throws IOException {
            try (final InputStream inputStream = Files.newInputStream(input);
                    final JsonParser parser = jsonFactory.createParser(inputStream);
                    final OutputStream outputStream = Files.newOutputStream(output)) {
                final PluginDescriptor pluginDescriptor = new PluginDescriptor();
                // Input JSON
                pluginDescriptor.readJson(parser);
                // Output `Log4j2Plugins.dat` file
                pluginDescriptor.toPluginDescriptor(outputStream);
            }
            return null;
        }
    }

    private static class ReflectConfigTransformer implements Callable<@Nullable Void> {

        private final Path pluginDescriptorPath;
        private final Path input;
        private final Path output;

        public ReflectConfigTransformer(final File pluginDescriptorPath, final File input) {
            this.pluginDescriptorPath = pluginDescriptorPath.toPath();
            this.input = input.toPath();
            this.output = Paths.get("reflect-config.json");
        }

        @Override
        public @Nullable Void call() throws IOException {
            final PluginDescriptor pluginDescriptor = new PluginDescriptor();
            // Read plugin descriptor
            try (final InputStream inputStream = Files.newInputStream(pluginDescriptorPath);
                    final JsonParser parser = jsonFactory.createParser(inputStream)) {
                pluginDescriptor.readJson(parser);
            }
            // Find all referenced classes
            final Set<String> classNames = new HashSet<>();
            pluginDescriptor.getNamespaces().flatMap(Namespace::getPlugins).forEach(p -> {
                classNames.add(p.getClassName());
                classNames.addAll(p.getBuilderHierarchy());
            });
            final ReflectConfigFilter filter = new ReflectConfigFilter(classNames);
            try (final InputStream inputStream = Files.newInputStream(input);
                    final JsonParser parser = jsonFactory.createParser(inputStream);
                    final OutputStream outputStream = Files.newOutputStream(output);
                    final JsonGenerator generator = jsonFactory.createGenerator(outputStream)) {
                filter.filter(parser, generator);
            }
            return null;
        }
    }
}
