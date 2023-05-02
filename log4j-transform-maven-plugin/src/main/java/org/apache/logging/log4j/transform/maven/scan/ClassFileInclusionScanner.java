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

import java.nio.file.Path;
import java.util.Set;

import org.apache.logging.log4j.weaver.Constants;

public interface ClassFileInclusionScanner {

    static final String DEFAULT_INCLUSION_PATTERN = "**/*.class";
    static final String DEFAULT_EXCLUSION_PATTERN = "**/*" + Constants.LOCATION_CACHE_SUFFIX + ".class";

    /**
     * Finds class files matching a specific condition.
     *
     * @param sourceDir path to the folder where to search files
     * @param targetDir an auxiliary folder
     * @return a set of relative paths to file in {@code sourceDir}
     */
    Set<Path> getIncludedClassFiles(Path sourceDir, Path targetDir);
}
