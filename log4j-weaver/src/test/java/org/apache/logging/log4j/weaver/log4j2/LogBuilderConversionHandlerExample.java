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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogBuilder;

import static org.assertj.core.api.Assertions.assertThat;

public class LogBuilderConversionHandlerExample {

    public void testWithLocation() {
        // We can't use verify: if `withLocation` is correctly remove, so is
        // verify(...).withLocation()
        final AtomicBoolean called = new AtomicBoolean();
        final AtomicReference<StackTraceElement> locationRef = new AtomicReference<>();
        final LogBuilder logBuilder = new LogBuilderMock(locationRef, called);
        // We remove the call without parameters
        logBuilder.withLocation().log();
        assertThat(called).isFalse();

        final StackTraceElement stackTraceElement = new StackTraceElement(
                LogBuilderConversionHandlerExample.class.getName(), "specialMethod",
                "LogBuilderConversionHandlerExample.java", 1024);
        logBuilder.withLocation(stackTraceElement).log();
        assertThat(locationRef).hasValue(stackTraceElement);
    }

}
