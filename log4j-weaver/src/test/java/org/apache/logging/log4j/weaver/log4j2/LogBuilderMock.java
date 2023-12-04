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

public final class LogBuilderMock implements LogBuilder {
    private final AtomicReference<StackTraceElement> locationRef;
    private final AtomicBoolean called;

    public LogBuilderMock(AtomicReference<StackTraceElement> locationRef, AtomicBoolean called) {
        this.locationRef = locationRef;
        this.called = called;
    }

    @Override
    public LogBuilder withLocation() {
        called.set(true);
        return LogBuilder.super.withLocation();
    }

    @Override
    public LogBuilder withLocation(StackTraceElement location) {
        locationRef.set(location);
        return LogBuilder.super.withLocation(location);
    }
}
