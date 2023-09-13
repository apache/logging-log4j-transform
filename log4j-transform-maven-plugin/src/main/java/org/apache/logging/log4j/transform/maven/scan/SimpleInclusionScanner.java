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
package org.apache.logging.log4j.transform.maven.scan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.weaver.LocationCacheGenerator;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.DirectoryScanner;

public class SimpleInclusionScanner implements ClassFileInclusionScanner {

    private static final String[] EMPTY_ARRAY = new String[0];

    private final long lastUpdatedWithinMsecs;
    private final Set<String> sourceIncludes;
    private final Set<String> sourceExcludes;
    private final Log log;

    public SimpleInclusionScanner(long lastUpdateWithinMsecs, Log log) {
        this(lastUpdateWithinMsecs, Collections.singleton(DEFAULT_INCLUSION_PATTERN),
                Collections.singleton(DEFAULT_EXCLUSION_PATTERN), log);
    }

    public SimpleInclusionScanner(long lastUpdateWithinMsecs, Set<String> sourceIncludes, Set<String> sourceExcludes,
            Log log) {
        this.lastUpdatedWithinMsecs = lastUpdateWithinMsecs;
        this.sourceIncludes = new HashSet<>(sourceIncludes);
        this.sourceExcludes = new HashSet<>(sourceExcludes);
        this.log = log;
    }

    @Override
    public Set<Path> getIncludedClassFiles(Path sourceDir, Path targetDir) {
        final Set<Path> potentialSources = scanForSources(sourceDir, sourceIncludes, sourceExcludes);

        return potentialSources.stream().filter(source -> isLocationCacheStale(sourceDir, targetDir, source))
                .collect(Collectors.toSet());
    }

    /**
     * @return a set of relative paths to class files
     */
    private static Set<Path> scanForSources(Path sourceDir, Set<String> sourceIncludes, Set<String> sourceExcludes) {
        final DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(sourceDir.toFile());
        scanner.setIncludes(sourceIncludes.toArray(EMPTY_ARRAY));
        scanner.setExcludes(sourceExcludes.toArray(EMPTY_ARRAY));
        scanner.scan();

        return Stream.of(scanner.getIncludedFiles())
                .map(sourceDir::resolve)
                .collect(Collectors.toSet());
    }

    private boolean isLocationCacheStale(Path sourceDir, Path targetDir, Path source) {
        try {
            final Path target = targetDir.resolve(LocationCacheGenerator.getCacheClassFile(source));
            if (!Files.exists(target)) {
                return true;
            }

            final FileTime sourceModifiedTime = Files.getLastModifiedTime(sourceDir.resolve(source));
            final FileTime targetModifiedTime = Files.getLastModifiedTime(target);
            return targetModifiedTime.toMillis() - sourceModifiedTime.toMillis() > lastUpdatedWithinMsecs;
        } catch (IOException e) {
            log.warn("Unable to open file: " + source, e);
        }
        return false;
    }

}
