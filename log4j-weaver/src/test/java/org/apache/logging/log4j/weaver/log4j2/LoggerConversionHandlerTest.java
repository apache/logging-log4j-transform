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
package org.apache.logging.log4j.weaver.log4j2;

import java.util.stream.Stream;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.apache.logging.log4j.weaver.AbstractConversionHandlerTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@LoggerContextSource("log4j2-test.xml")
public class LoggerConversionHandlerTest extends AbstractConversionHandlerTest {

    private ListAppender appender;

    @BeforeAll
    public static void setup() throws Exception {
        transformClass("org/apache/logging/log4j/weaver/log4j2/LoggerConversionHandlerExample");
    }

    @BeforeEach
    public void setupAppender(final @Named("List") ListAppender appender) {
        this.appender = appender;
    }

    static Stream<String> testLocationConverter() {
        return Stream.of(
                "testFatal",
                "testError",
                "testWarn",
                "testInfo",
                "testDebug",
                "testLog",
                "testFrames",
                "testPrintf",
                "testLogBuilder",
                "testPassthrough",
                "testCatchingThrowing",
                "testTraceEntry",
                "testTraceExit");
    }

    @ParameterizedTest
    @MethodSource
    public void testLocationConverter(final String methodName) throws Exception {
        convertedClass.getMethod(methodName, ListAppender.class).invoke(testObject, appender);
    }
}
