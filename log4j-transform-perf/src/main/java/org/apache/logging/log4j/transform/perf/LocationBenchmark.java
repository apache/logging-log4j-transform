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
package org.apache.logging.log4j.transform.perf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.logging.log4j.LogBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

/**
 * <p>
 * Compares {@link Logger} and {@link LogBuilder} before and after
 * postprocessing by log4j-weaver.
 * </p>
 * <h2>HOW TO RUN THIS TEST</h2>
 * <ul>
 * <li>single thread:
 *
 * <pre>
 * java -jar target/benchmarks.jar ".*LocationBenchmark*"
 * </pre>
 *
 * </li>
 * <li>multiple threads (for example, 4 threads):
 *
 * <pre>
 * java -jar target/benchmarks.jar ".*LocationBenchmark.*" -t 4
 * </pre>
 *
 * </li>
 * </ul>
 *
 * <h2>AVAILABLE PARAMETERS</h2> The following parameters are available:
 * <ul>
 * <li>{@code fileName}, uses the give file name instead of the default
 * "target/benchmark.log". E.g.:
 *
 * <pre>
 * java -jar target/benchmarks.jar ".*LocationBenchmark*" -p fileName=/dev/null
 * </pre>
 *
 * </li>
 * <li>{@code useDemoAppender}, uses a demo appender that performs all appender
 * actions, except actually writing to a stream.</li>
 * </ul>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class LocationBenchmark {

    @Param("target/benchmark.log")
    private String fileName;

    @Param("false")
    private boolean useDemoAppender;

    private Marker marker;
    private Logger logger;
    private LocationLoggerRuntime runtime;
    private LocationLoggerCompiletime compiletime;

    @Setup
    public void setUp() {
        if (useDemoAppender) {
            System.setProperty("log4j2.configurationFile", "log4j2-demo.xml");
        } else {
            System.setProperty("log4j2.configurationFile", "log4j2-file.xml");
            System.setProperty("LOG_FILE", fileName);
        }
        logger = LogManager.getLogger(getClass());
        marker = MarkerManager.getMarker("TestMarker");
        runtime = new LocationLoggerRuntime(logger, marker);
        compiletime = new LocationLoggerCompiletime(logger, marker);
    }

    @TearDown
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public void tearDown() throws IOException {
        System.clearProperty("log4j2.configurationFile");
        LogManager.shutdown();
        Path filePath = Paths.get(fileName);
        if (Files.isRegularFile(filePath)) {
            Files.deleteIfExists(filePath);
        }
    }

    @Benchmark
    public void runtimeLogNoMarker() {
        runtime.logNoMarker();
    }

    @Benchmark
    public void runtimeLogMarker() {
        runtime.logMarker();
    }

    @Benchmark
    public void runtimeLogBuilderNoMarker() {
        runtime.logBuilderNoMarker();
    }

    @Benchmark
    public void runtimeLogBuilderMarker() {
        runtime.logBuilderMarker();
    }

    @Benchmark
    public void compiletimeLogNoMarker() {
        compiletime.logNoMarker();
    }

    @Benchmark
    public void compiletimeLogMarker() {
        compiletime.logMarker();
    }

    @Benchmark
    public void compiletimeLogBuilderNoMarker() {
        compiletime.logBuilderNoMarker();
    }

    @Benchmark
    public void compiletimeLogBuilderMarker() {
        compiletime.logBuilderMarker();
    }
}
