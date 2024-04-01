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
package org.apache.logging.log4j.codegen;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.codegen.Generate.LevelInfo;
import org.apache.logging.log4j.codegen.Generate.Type;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.test.TestLogger;
import org.apache.logging.log4j.util.MessageSupplier;
import org.apache.logging.log4j.util.Strings;
import org.apache.logging.log4j.util.Supplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

@Tag("functional")
@SetSystemProperty(
        key = "log4j2.loggerContextFactory",
        value = "org.apache.logging.log4j.test.TestLoggerContextFactory")
public class GenerateCustomLoggerTest {

    private static final String TEST_SOURCE = "target/test-classes/org/apache/logging/log4j/core/MyCustomLogger.java";

    @AfterAll
    public static void afterClass() {
        File file = new File(TEST_SOURCE);
        final File parent = file.getParentFile();
        if (file.exists()) {
            file.delete();
        }
        file = new File(parent, "MyCustomLogger.class");
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    @SuppressWarnings("ReturnValueIgnored")
    public void testGenerateSource() throws Exception {
        final String CLASSNAME = "org.apache.logging.log4j.core.MyCustomLogger";

        // generate custom logger source
        final List<LevelInfo> levels = Arrays.asList(
                new LevelInfo("DEFCON1", 350), new LevelInfo("DEFCON2", 450), new LevelInfo("DEFCON3", 550));
        final Path testSource = Paths.get(TEST_SOURCE);
        Files.createDirectories(testSource.getParent());
        try (final BufferedWriter writer = Files.newBufferedWriter(testSource, UTF_8)) {
            Generate.generateSource(Type.CUSTOM, CLASSNAME, levels, writer);
        }

        // set up compiler
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        final List<String> errors = new ArrayList<>();
        try (final StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            final Iterable<? extends JavaFileObject> compilationUnits =
                    fileManager.getJavaFileObjectsFromFiles(Collections.singletonList(testSource.toFile()));
            final String classPath = System.getProperty("jdk.module.path");
            final List<String> optionList = new ArrayList<>();
            if (Strings.isNotBlank(classPath)) {
                optionList.add("-classpath");
                optionList.add(classPath);
            }
            // compile generated source
            compiler.getTask(null, fileManager, diagnostics, optionList, null, compilationUnits)
                    .call();

            // check we don't have any compilation errors
            for (final Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                    errors.add(String.format("Compile error: %s%n", diagnostic.getMessage(Locale.getDefault())));
                }
            }
        }
        assertTrue(errors.isEmpty(), errors.toString());

        // load the compiled class
        final Class<?> cls = Class.forName(CLASSNAME);

        // check that all factory methods exist and are static
        assertTrue(Modifier.isStatic(cls.getDeclaredMethod("create").getModifiers()));
        assertTrue(
                Modifier.isStatic(cls.getDeclaredMethod("create", Class.class).getModifiers()));
        assertTrue(
                Modifier.isStatic(cls.getDeclaredMethod("create", Object.class).getModifiers()));
        assertTrue(
                Modifier.isStatic(cls.getDeclaredMethod("create", String.class).getModifiers()));
        assertTrue(Modifier.isStatic(cls.getDeclaredMethod("create", Class.class, MessageFactory.class)
                .getModifiers()));
        assertTrue(Modifier.isStatic(cls.getDeclaredMethod("create", Object.class, MessageFactory.class)
                .getModifiers()));
        assertTrue(Modifier.isStatic(cls.getDeclaredMethod("create", String.class, MessageFactory.class)
                .getModifiers()));

        // check that all log methods exist
        final String[] logMethods = {"defcon1", "defcon2", "defcon3"};
        for (final String name : logMethods) {
            assertDoesNotThrow(() -> {
                cls.getDeclaredMethod(name, Marker.class, Message.class, Throwable.class);
                cls.getDeclaredMethod(name, Marker.class, Object.class, Throwable.class);
                cls.getDeclaredMethod(name, Marker.class, String.class, Throwable.class);
                cls.getDeclaredMethod(name, Marker.class, Message.class);
                cls.getDeclaredMethod(name, Marker.class, Object.class);
                cls.getDeclaredMethod(name, Marker.class, String.class);
                cls.getDeclaredMethod(name, Message.class);
                cls.getDeclaredMethod(name, Object.class);
                cls.getDeclaredMethod(name, String.class);
                cls.getDeclaredMethod(name, Message.class, Throwable.class);
                cls.getDeclaredMethod(name, Object.class, Throwable.class);
                cls.getDeclaredMethod(name, String.class, Throwable.class);
                cls.getDeclaredMethod(name, String.class, Object[].class);
                cls.getDeclaredMethod(name, Marker.class, String.class, Object[].class);

                // 2.4 lambda support
                cls.getDeclaredMethod(name, Marker.class, MessageSupplier.class);
                cls.getDeclaredMethod(name, Marker.class, MessageSupplier.class, Throwable.class);
                cls.getDeclaredMethod(name, Marker.class, String.class, Supplier[].class);
                cls.getDeclaredMethod(name, Marker.class, Supplier.class);
                cls.getDeclaredMethod(name, Marker.class, Supplier.class, Throwable.class);
                cls.getDeclaredMethod(name, MessageSupplier.class);
                cls.getDeclaredMethod(name, MessageSupplier.class, Throwable.class);
                cls.getDeclaredMethod(name, String.class, Supplier[].class);
                cls.getDeclaredMethod(name, Supplier.class);
                cls.getDeclaredMethod(name, Supplier.class, Throwable.class);
            });
        }

        final TestLogger underlying = (TestLogger) LogManager.getLogger("X.Y.Z");

        try {
            // now see if it actually works...
            final Method create = cls.getDeclaredMethod("create", String.class);
            final Object customLogger = create.invoke(null, "X.Y.Z");
            int n = 0;
            for (final String name : logMethods) {
                final Method method = cls.getDeclaredMethod(name, String.class);
                method.invoke(customLogger, "This is message " + n++);
            }

            final List<String> lines = underlying.getEntries();
            for (int i = 0; i < lines.size(); i++) {
                assertEquals(" " + levels.get(i).name + " This is message " + i, lines.get(i));
            }
        } finally {
            underlying.getEntries().clear();
        }
    }
}
