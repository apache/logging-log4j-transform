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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;

/**
 * Should be identical to {@link LocationLoggerRuntime}.
 *
 */
public class LocationLoggerCompiletime {

    private static final String MESSAGE = "Compiletime location";
    private final Logger logger;
    private final Marker marker;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public LocationLoggerCompiletime(final Logger logger, final Marker marker) {
        this.logger = logger;
        this.marker = marker;
    }

    public void logNoMarker() {
        logger.info(MESSAGE);
    }

    public void logMarker() {
        logger.info(marker, MESSAGE);
    }

    public void logBuilderNoMarker() {
        logger.atInfo().log(MESSAGE);
    }

    public void logBuilderMarker() {
        logger.atInfo().withMarker(marker).log(MESSAGE);
    }
}
