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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.converter.plugins.internal.PluginDescriptors.Namespace;
import org.apache.logging.log4j.converter.plugins.internal.PluginDescriptors.PluginDescriptor;
import org.apache.logging.log4j.converter.plugins.internal.ReflectConfigFilter;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "convertPlugin")
public class PluginCacheConverter {

    private static final String PLUGIN_DESCRIPTOR_FILE =
            "META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat";
    private static final String PLUGIN_DESCRIPTOR_JSON_FILE = "Log4j2Plugins.json";

    private static final JsonFactory jsonFactory = new ObjectMapper().getFactory();

    public static void main(final String[] args) {
        System.exit(new CommandLine(new PluginCacheConverter()).execute(args));
    }

    @Command
    public void toJson(
            @Parameters(description = "Classpath containing Log4j Core plugins") final List<String> classPath,
            @Option(names = {"-o", "--outputDirectory"}) final File outputDirectory)
            throws IOException {
        new PluginDescriptorToJsonConverter(classPath, outputDirectory).call();
    }

    @Command
    public void fromJson(
            @Parameters(description = "Input `Log4j2Plugins.json` JSON file") final File input,
            @Option(names = {"-o", "--outputDirectory"}) final File outputDirectory)
            throws IOException {
        new JsonToPluginDescriptorConverter(input, outputDirectory).call();
    }

    @Command
    public void filterReflectConfig(
            @Parameters(description = "Plugin descriptor (as JSON)") final File pluginDescriptor,
            @Parameters(description = "Classpath containing GraalVM descriptors") final List<String> classPath,
            @Option(names = {"-o", "--outputDirectory"}) final File outputDirectory)
            throws IOException {
        new ReflectConfigTransformer(pluginDescriptor, classPath, outputDirectory).call();
    }

    private static Collection<Path> validateClassPath(final Collection<String> classPath) {
        return classPath.stream()
                .flatMap(classPathElement -> Arrays.stream(classPathElement.split(File.pathSeparator, -1)))
                .map(classPathElement -> {
                    final Path inputPath = Paths.get(classPathElement);
                    if (!Files.isRegularFile(inputPath)) {
                        throw new IllegalArgumentException("Input file " + inputPath + " is not a file.");
                    }
                    if (!inputPath.getFileName().toString().endsWith(".jar")) {
                        throw new IllegalArgumentException(
                                "Invalid input file, only JAR files are supported: " + inputPath);
                    }
                    return inputPath;
                })
                .collect(Collectors.toList());
    }

    private static void createParentDirectories(final Path path) throws IOException {
        final Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static final class PluginDescriptorToJsonConverter implements Callable<@Nullable Void> {

        private final ClassLoader classLoader;

        private final @Nullable Path outputDirectory;

        PluginDescriptorToJsonConverter(final Collection<String> classPath, final @Nullable File outputDirectory) {
            this.classLoader = AccessController.doPrivileged(
                    (PrivilegedAction<ClassLoader>) () -> new URLClassLoader(validateClassPath(classPath).stream()
                            .map(p -> {
                                try {
                                    return p.toUri().toURL();
                                } catch (final MalformedURLException e) {
                                    throw new IllegalArgumentException(e);
                                }
                            })
                            .toArray(URL[]::new)));
            this.outputDirectory = outputDirectory != null ? outputDirectory.toPath() : null;
        }

        @Override
        public @Nullable Void call() throws IOException {
            final OutputStream output;
            if (outputDirectory != null) {
                Files.createDirectories(outputDirectory);
                output = Files.newOutputStream(outputDirectory.resolve(PLUGIN_DESCRIPTOR_JSON_FILE));
            } else {
                output = System.out;
            }
            try (final JsonGenerator generator = jsonFactory.createGenerator(output)) {
                final PluginDescriptor pluginDescriptor = new PluginDescriptor();
                // Read all `Log4j2Plugins.dat` file on the classpath
                for (final URL url : Collections.list(classLoader.getResources(PLUGIN_DESCRIPTOR_FILE))) {
                    try (final BufferedInputStream input = new BufferedInputStream(url.openStream())) {
                        pluginDescriptor.readPluginDescriptor(input);
                    }
                }
                // Write JSON file
                pluginDescriptor.withBuilderHierarchy(classLoader).toJson(generator);
            } finally {
                if (output != System.out) {
                    output.close();
                }
            }
            return null;
        }
    }

    private static class JsonToPluginDescriptorConverter implements Callable<@Nullable Void> {

        private final Path input;
        private final @Nullable Path outputDirectory;

        JsonToPluginDescriptorConverter(final File input, final @Nullable File outputDirectory) {
            this.input = input.toPath();
            this.outputDirectory = outputDirectory != null ? outputDirectory.toPath() : null;
        }

        @Override
        public @Nullable Void call() throws IOException {
            final OutputStream output;
            if (outputDirectory != null) {
                final Path pluginDescriptorPath = outputDirectory.resolve(PLUGIN_DESCRIPTOR_FILE);
                createParentDirectories(pluginDescriptorPath);
                output = Files.newOutputStream(pluginDescriptorPath);
            } else {
                output = System.out;
            }
            try (final InputStream inputStream = Files.newInputStream(input);
                    final JsonParser parser = jsonFactory.createParser(inputStream)) {
                final PluginDescriptor pluginDescriptor = new PluginDescriptor();
                // Input JSON
                pluginDescriptor.readJson(parser);
                // Output `Log4j2Plugins.dat` file
                pluginDescriptor.toPluginDescriptor(output);
            } finally {
                if (output != System.out) {
                    output.close();
                }
            }
            return null;
        }
    }

    private static class ReflectConfigTransformer implements Callable<@Nullable Void> {

        private final Path pluginDescriptorPath;
        private final Collection<Path> classPath;
        private final @Nullable Path outputDirectory;

        ReflectConfigTransformer(
                final File pluginDescriptorPath,
                final Collection<String> classPath,
                final @Nullable File outputDirectory) {
            this.pluginDescriptorPath = pluginDescriptorPath.toPath();
            this.classPath = validateClassPath(classPath);
            this.outputDirectory = outputDirectory != null ? outputDirectory.toPath() : null;
        }

        void filterReflectConfigInJar(final Path jar, final ReflectConfigFilter filter) throws IOException {
            final URI jarFileSystemRoot;
            try {
                jarFileSystemRoot = new URI("jar", jar.toUri().toASCIIString(), null);
            } catch (final URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
            try (final FileSystem fileSystem = FileSystems.newFileSystem(jarFileSystemRoot, Collections.emptyMap())) {
                final Path rootPath = fileSystem.getPath("/");
                final Path nativeImagePath = rootPath.resolve("META-INF/native-image");
                try (final Stream<Path> paths = Files.walk(nativeImagePath, 3)) {
                    paths.filter(p ->
                                    "reflect-config.json".equals(p.getFileName().toString()))
                            .forEach(p -> filterReflectConfig(rootPath, p, filter));
                }
            }
        }

        void filterReflectConfig(final Path rootPath, final Path reflectConfig, final ReflectConfigFilter filter) {
            try {
                final OutputStream output;
                if (outputDirectory != null) {
                    final String relativePath =
                            rootPath.relativize(reflectConfig).toString();
                    final Path outputPath = outputDirectory.resolve(relativePath);
                    createParentDirectories(outputPath);
                    output = Files.newOutputStream(outputPath);
                } else {
                    output = System.out;
                }
                try (final InputStream inputStream = Files.newInputStream(reflectConfig);
                        final JsonParser parser = jsonFactory.createParser(inputStream);
                        final JsonGenerator generator = jsonFactory.createGenerator(output)) {
                    filter.filter(parser, generator);
                }
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
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
            for (final Path jar : classPath) {
                filterReflectConfigInJar(jar, filter);
            }
            return null;
        }
    }
}
