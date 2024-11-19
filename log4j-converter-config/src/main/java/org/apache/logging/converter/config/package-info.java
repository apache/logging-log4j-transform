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
/**
 * Simple API to convert between different logging configuration formats.
 * <p>
 *     For example, to convert from the Log4j Core 2 Properties configuration format to the YAML configuration format,
 *     you can use the following code:
 * </p>
 * <pre>
 * <code>
 * Path input = ...;
 * Path output = ...;
 * try (InputStream inputStream = Files.newInputStream(input);
 *         OutputStream outputStream = Files.newOutputStream(output)) {
 *     ConfigurationConverter converter = ConfigurationConverter.newInstance();
 *     converter.convert(inputStream, "v2:properties", outputStream, "v2:yaml");
 * }
 * </code>
 * </pre>
 */
@NullMarked
@Export
@Version("0.3.0")
package org.apache.logging.converter.config;

import org.jspecify.annotations.NullMarked;
import org.osgi.annotation.bundle.Export;
import org.osgi.annotation.versioning.Version;
