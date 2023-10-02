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
package org.apache.logging.log4j.weaver;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class AbstractConversionHandlerTest {

    protected static Class<?> convertedClass;
    protected static Object testObject;

    protected static void transformClass(String internalName) throws Exception {
        final TestClassLoader testCl = new TestClassLoader();

        final ByteArrayOutputStream dest = new ByteArrayOutputStream();
        final LocationClassConverter converter = new LocationClassConverter(AbstractConversionHandlerTest.class.getClassLoader());
        final LocationCacheGenerator locationCache = new LocationCacheGenerator();

        getNestedClasses(internalName).forEach(classFile -> assertDoesNotThrow(() -> {
            dest.reset();
            converter.convert(Files.newInputStream(classFile), dest, locationCache);
            testCl.defineClass(dest.toByteArray());
        }));
        locationCache.generateClasses().values().forEach(testCl::defineClass);
        convertedClass = testCl.loadClass(internalName.replaceAll("/", "."));
        testObject = assertDoesNotThrow(() -> convertedClass.getConstructor().newInstance());
    }

    private static Stream<Path> getNestedClasses(String internalName) throws Exception {
        final Path topClass = Paths
                .get(AbstractConversionHandlerTest.class.getClassLoader().getResource(internalName + ".class").toURI());
        final String simpleClassName = Paths.get(internalName).getFileName().toString();
        try (Stream<Path> paths = Files.walk(topClass.getParent(), 1)) {
            return paths
                    .filter(p -> {
                        final String nested = p.getFileName().toString();
                        return nested.startsWith(simpleClassName) && nested.endsWith(".class");
                    })
                    .sorted()
                    // We create a shallow-copy to return a `Stream` without leaking file descriptors
                    .collect(Collectors.toList())
                    .stream();
        }

    }

    private static class TestClassLoader extends ClassLoader {

        public TestClassLoader() {
            super(AbstractConversionHandlerTest.class.getClassLoader());
        }

        public Class<?> defineClass(byte[] bytes) {
            return defineClass(null, bytes, 0, bytes.length);
        }
    }
}
