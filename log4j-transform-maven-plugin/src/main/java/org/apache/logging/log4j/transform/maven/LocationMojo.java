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
package org.apache.logging.log4j.transform.maven;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.transform.maven.scan.ClassFileInclusionScanner;
import org.apache.logging.log4j.transform.maven.scan.SimpleInclusionScanner;
import org.apache.logging.log4j.weaver.LocationCacheGenerator;
import org.apache.logging.log4j.weaver.LocationClassConverter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Generates location information for use with Log4j2.
 */
@Mojo(
        name = "process-classes",
        defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        threadSafe = true,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class LocationMojo extends AbstractMojo {

    private static final String LOG4J_GROUP_ID = "org.apache.logging.log4j";
    private static final String LOG4J_API_ARTIFACT_ID = "log4j-api";
    private static final ArtifactVersion MIN_SUPPORTED_VERSION = new DefaultArtifactVersion("2.20.0");
    private static final URL[] EMPTY_URL_ARRAY = new URL[0];

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The directory containing class files to process.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true, readonly = false)
    private File sourceDirectory;

    /**
     * The directory containing processed files.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true, readonly = false)
    private File outputDirectory;

    /**
     * A list of inclusion filters for the processor.
     */
    @Parameter
    private Set<String> includes = new HashSet<>();

    /**
     * A list of exclusion filters for the processor.
     */
    @Parameter
    private Set<String> excludes = new HashSet<>();

    /**
     * Sets the granularity in milliseconds of the last modification date for
     * testing whether a class file needs weaving.
     */
    @Parameter(property = "lastModGranularityMs", defaultValue = "0")
    private int staleMillis;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if ("pom".equals(project.getPackaging())) {
            getLog().info("Skipping project with packaging \"pom\".");
            return;
        }
        if (!validateLog4jVersion()) {
            return;
        }

        final Path sourceDirectory = this.sourceDirectory.toPath();
        final Path outputDirectory = this.outputDirectory.toPath();
        final LocationCacheGenerator locationCache = new LocationCacheGenerator();
        final LocationClassConverter converter = new LocationClassConverter(getProjectDependencies());

        try {
            final Set<Path> staleClassFiles =
                    getClassFileInclusionScanner().getIncludedClassFiles(sourceDirectory, outputDirectory);
            staleClassFiles.stream()
                    .collect(Collectors.groupingBy(LocationCacheGenerator::getCacheClassFile))
                    .values()
                    .parallelStream()
                    .forEach(p -> convertClassfiles(p, converter, locationCache));

            locationCache.generateClasses().forEach(this::saveClassFile);
        } catch (WrappedIOException e) {
            throw new MojoExecutionException("An I/O error occurred.", e.getCause());
        }
    }

    private void convertClassfiles(
            List<Path> classFiles, LocationClassConverter converter, LocationCacheGenerator locationCache) {
        final Path sourceDirectory = this.sourceDirectory.toPath();
        classFiles.sort(Path::compareTo);
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try {
            for (final Path classFile : classFiles) {
                buf.reset();
                try (final InputStream src = Files.newInputStream(sourceDirectory.resolve(classFile))) {
                    converter.convert(src, buf, locationCache);
                }
                saveClassFile(classFile, buf.toByteArray());
            }
        } catch (IOException e) {
            throw new WrappedIOException(e);
        }
    }

    private void saveClassFile(String internalClassName, byte[] data) {
        try {
            final Path outputDirectory = this.outputDirectory.toPath();
            Files.write(outputDirectory.resolve(internalClassName + ".class"), data);
        } catch (IOException e) {
            throw new WrappedIOException(e);
        }
    }

    private void saveClassFile(Path dest, byte[] data) {
        try {
            final Path outputDirectory = this.outputDirectory.toPath();
            Files.write(outputDirectory.resolve(dest), data);
        } catch (IOException e) {
            throw new WrappedIOException(e);
        }
    }

    protected ClassFileInclusionScanner getClassFileInclusionScanner() {
        if (includes.isEmpty() && excludes.isEmpty()) {
            return new SimpleInclusionScanner(staleMillis, getLog());
        }

        final Set<String> actualIncludes = includes.isEmpty()
                ? Collections.singleton(ClassFileInclusionScanner.DEFAULT_INCLUSION_PATTERN)
                : includes;

        // We always exclude Log4j2 cache files
        final Set<String> actualExcludes = new HashSet<>(excludes);
        actualExcludes.add(ClassFileInclusionScanner.DEFAULT_EXCLUSION_PATTERN);

        return new SimpleInclusionScanner(staleMillis, actualIncludes, actualExcludes, getLog());
    }

    private static class WrappedIOException extends RuntimeException {

        private static final long serialVersionUID = 4290527889488735839L;

        private WrappedIOException(IOException cause) {
            super(cause);
        }
    }

    private boolean validateLog4jVersion() throws MojoExecutionException {
        Optional<Artifact> artifact = project.getArtifacts().stream()
                .filter(a -> LOG4J_GROUP_ID.equals(a.getGroupId()) && LOG4J_API_ARTIFACT_ID.equals(a.getArtifactId()))
                .findAny();
        Artifact log4jApi;
        if (artifact.isPresent()) {
            log4jApi = artifact.get();
        } else {
            getLog().info("Skipping project. Log4j is not being used.");
            return false;
        }
        try {
            if (MIN_SUPPORTED_VERSION.compareTo(log4jApi.getSelectedVersion()) > 0) {
                throw new MojoExecutionException("Log4j2 API version " + MIN_SUPPORTED_VERSION
                        + " required. Selected version: " + log4jApi.getSelectedVersion());
            }
            // Transitive dependency
            if (!project.getDependencyArtifacts().contains(log4jApi)) {
                getLog().warn("Log4j2 API should not be a transitive dependency.");
            }
        } catch (OverConstrainedVersionException e) {
            throw new MojoExecutionException("Can not determine `log4j-api` version.", e);
        }
        return true;
    }

    private ClassLoader getProjectDependencies() throws MojoExecutionException {
        Set<Artifact> artifacts = project.getArtifacts();
        List<URL> urls = new ArrayList<>(artifacts.size() + 1);
        try {
            urls.add(sourceDirectory.toURI().toURL());
            for (Artifact artifact : artifacts) {
                urls.add(artifact.getFile().toURI().toURL());
            }
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("An error occurred, while resolving the project's classpath.", e);
        }
        return new URLClassLoader(urls.toArray(EMPTY_URL_ARRAY));
    }
}
