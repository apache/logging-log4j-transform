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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.message.DefaultFlowMessageFactory;
import org.apache.logging.log4j.message.EntryMessage;
import org.apache.logging.log4j.message.ExitMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.util.Supplier;

public class LoggerConversionHandlerExample {

    private static final CharSequence CHAR_SEQUENCE = "(CharSequence)";
    private static final Message MESSAGE = new SimpleMessage("(Message)");
    private static final EntryMessage ENTRY_MESSAGE = new DefaultFlowMessageFactory().newEntryMessage(MESSAGE);
    private static final String STRING = "(String)";
    private static final Object OBJECT = "(Object)";
    private static final Object P0 = "(p0)";
    private static final Object P1 = "(p1)";
    private static final Object P2 = "(p2)";
    private static final Object P3 = "(p3)";
    private static final Object P4 = "(p4)";
    private static final Object P5 = "(p5)";
    private static final Object P6 = "(p6)";
    private static final Object P7 = "(p7)";
    private static final Object P8 = "(p8)";
    private static final Object P9 = "(p9)";
    private static final Object[] PARRAY = {"(...)"};
    private static final Supplier<?>[] SUPPLIERS = {() -> OBJECT};

    @SuppressWarnings("StaticAssignmentOfThrowable")
    private static final Throwable THROWABLE = new RuntimeException();

    private static final Marker MARKER = MarkerManager.getMarker("MARKER");

    private static final Logger logger = LogManager.getLogger();

    public void testFatal(final ListAppender app) {
        app.clear();
        final String methodName = "testFatal";
        int lineNumber = getLineNumber(); // current line number
        logger.fatal(CHAR_SEQUENCE);
        assertLocationEquals(methodName, ++lineNumber, app);
        logger.fatal(CHAR_SEQUENCE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(MARKER, CHAR_SEQUENCE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(MARKER, CHAR_SEQUENCE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(MESSAGE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(MESSAGE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(MARKER, MESSAGE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(MARKER, MESSAGE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(OBJECT);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(OBJECT, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(MARKER, OBJECT);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(MARKER, OBJECT, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(STRING);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(MARKER, STRING);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(STRING, P0);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(MARKER, STRING, P0);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(STRING, P0, P1);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(MARKER, STRING, P0, P1);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(STRING, P0, P1, P2);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(MARKER, STRING, P0, P1, P2);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(STRING, P0, P1, P2, P3);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(MARKER, STRING, P0, P1, P2, P3);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(STRING, P0, P1, P2, P3, P4);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(MARKER, STRING, P0, P1, P2, P3, P4);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(STRING, P0, P1, P2, P3, P4, P5);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(MARKER, STRING, P0, P1, P2, P3, P4, P5);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(STRING, P0, P1, P2, P3, P4, P5, P6);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(MARKER, STRING, P0, P1, P2, P3, P4, P5, P6);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(STRING, P0, P1, P2, P3, P4, P5, P6, P7);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(MARKER, STRING, P0, P1, P2, P3, P4, P5, P6, P7);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(STRING, P0, P1, P2, P3, P4, P5, P6, P7, P8);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(MARKER, STRING, P0, P1, P2, P3, P4, P5, P6, P7, P8);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(STRING, P0, P1, P2, P3, P4, P5, P6, P7, P8, P9);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(MARKER, STRING, P0, P1, P2, P3, P4, P5, P6, P7, P8, P9);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(STRING, PARRAY);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(MARKER, STRING, PARRAY);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(() -> MESSAGE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(() -> MESSAGE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(MARKER, () -> MESSAGE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(MARKER, () -> MESSAGE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(() -> OBJECT);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(() -> OBJECT, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(MARKER, () -> OBJECT);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(MARKER, () -> OBJECT, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(STRING, SUPPLIERS);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.fatal(MARKER, STRING, SUPPLIERS);
        assertLocationEquals(methodName, lineNumber += 2, app);
    }

    public void testError(final ListAppender app) {
        app.clear();
        final String methodName = "testError";
        int lineNumber = getLineNumber(); // current line number
        logger.error(CHAR_SEQUENCE);
        assertLocationEquals(methodName, ++lineNumber, app);
        logger.error(CHAR_SEQUENCE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(MARKER, CHAR_SEQUENCE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(MARKER, CHAR_SEQUENCE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(MESSAGE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(MESSAGE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(MARKER, MESSAGE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(MARKER, MESSAGE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(OBJECT);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(OBJECT, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(MARKER, OBJECT);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(MARKER, OBJECT, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(STRING);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(MARKER, STRING);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(STRING, P0);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(MARKER, STRING, P0);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(STRING, P0, P1);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(MARKER, STRING, P0, P1);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(STRING, P0, P1, P2);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(MARKER, STRING, P0, P1, P2);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(STRING, P0, P1, P2, P3);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(MARKER, STRING, P0, P1, P2, P3);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(STRING, P0, P1, P2, P3, P4);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(MARKER, STRING, P0, P1, P2, P3, P4);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(STRING, P0, P1, P2, P3, P4, P5);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(MARKER, STRING, P0, P1, P2, P3, P4, P5);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(STRING, P0, P1, P2, P3, P4, P5, P6);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(MARKER, STRING, P0, P1, P2, P3, P4, P5, P6);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(STRING, P0, P1, P2, P3, P4, P5, P6, P7);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(MARKER, STRING, P0, P1, P2, P3, P4, P5, P6, P7);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(STRING, P0, P1, P2, P3, P4, P5, P6, P7, P8);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(MARKER, STRING, P0, P1, P2, P3, P4, P5, P6, P7, P8);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(STRING, P0, P1, P2, P3, P4, P5, P6, P7, P8, P9);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(MARKER, STRING, P0, P1, P2, P3, P4, P5, P6, P7, P8, P9);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(STRING, PARRAY);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(MARKER, STRING, PARRAY);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(() -> MESSAGE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(() -> MESSAGE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(MARKER, () -> MESSAGE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(MARKER, () -> MESSAGE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(() -> OBJECT);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(() -> OBJECT, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(MARKER, () -> OBJECT);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(MARKER, () -> OBJECT, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(STRING, SUPPLIERS);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.error(MARKER, STRING, SUPPLIERS);
        assertLocationEquals(methodName, lineNumber += 2, app);
    }

    public void testWarn(final ListAppender app) {
        app.clear();
        final String methodName = "testWarn";
        int lineNumber = getLineNumber(); // current line number
        logger.warn(CHAR_SEQUENCE);
        assertLocationEquals(methodName, ++lineNumber, app);
        logger.warn(CHAR_SEQUENCE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(MARKER, CHAR_SEQUENCE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(MARKER, CHAR_SEQUENCE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(MESSAGE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(MESSAGE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(MARKER, MESSAGE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(MARKER, MESSAGE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(OBJECT);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(OBJECT, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(MARKER, OBJECT);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(MARKER, OBJECT, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(STRING);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(MARKER, STRING);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(STRING, P0);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(MARKER, STRING, P0);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(STRING, P0, P1);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(MARKER, STRING, P0, P1);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(STRING, P0, P1, P2);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(MARKER, STRING, P0, P1, P2);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(STRING, P0, P1, P2, P3);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(MARKER, STRING, P0, P1, P2, P3);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(STRING, P0, P1, P2, P3, P4);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(MARKER, STRING, P0, P1, P2, P3, P4);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(STRING, P0, P1, P2, P3, P4, P5);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(MARKER, STRING, P0, P1, P2, P3, P4, P5);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(STRING, P0, P1, P2, P3, P4, P5, P6);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(MARKER, STRING, P0, P1, P2, P3, P4, P5, P6);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(STRING, P0, P1, P2, P3, P4, P5, P6, P7);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(MARKER, STRING, P0, P1, P2, P3, P4, P5, P6, P7);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(STRING, P0, P1, P2, P3, P4, P5, P6, P7, P8);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(MARKER, STRING, P0, P1, P2, P3, P4, P5, P6, P7, P8);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(STRING, P0, P1, P2, P3, P4, P5, P6, P7, P8, P9);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(MARKER, STRING, P0, P1, P2, P3, P4, P5, P6, P7, P8, P9);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(STRING, PARRAY);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(MARKER, STRING, PARRAY);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(() -> MESSAGE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(() -> MESSAGE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(MARKER, () -> MESSAGE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(MARKER, () -> MESSAGE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(() -> OBJECT);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(() -> OBJECT, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(MARKER, () -> OBJECT);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(MARKER, () -> OBJECT, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(STRING, SUPPLIERS);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.warn(MARKER, STRING, SUPPLIERS);
        assertLocationEquals(methodName, lineNumber += 2, app);
    }

    public void testInfo(final ListAppender app) {
        app.clear();
        final String methodName = "testInfo";
        int lineNumber = getLineNumber(); // current line number
        logger.info(CHAR_SEQUENCE);
        assertLocationEquals(methodName, ++lineNumber, app);
        logger.info(CHAR_SEQUENCE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(MARKER, CHAR_SEQUENCE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(MARKER, CHAR_SEQUENCE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(MESSAGE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(MESSAGE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(MARKER, MESSAGE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(MARKER, MESSAGE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(OBJECT);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(OBJECT, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(MARKER, OBJECT);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(MARKER, OBJECT, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(STRING);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(MARKER, STRING);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(STRING, P0);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(MARKER, STRING, P0);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(STRING, P0, P1);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(MARKER, STRING, P0, P1);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(STRING, P0, P1, P2);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(MARKER, STRING, P0, P1, P2);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(STRING, P0, P1, P2, P3);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(MARKER, STRING, P0, P1, P2, P3);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(STRING, P0, P1, P2, P3, P4);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(MARKER, STRING, P0, P1, P2, P3, P4);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(STRING, P0, P1, P2, P3, P4, P5);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(MARKER, STRING, P0, P1, P2, P3, P4, P5);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(STRING, P0, P1, P2, P3, P4, P5, P6);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(MARKER, STRING, P0, P1, P2, P3, P4, P5, P6);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(STRING, P0, P1, P2, P3, P4, P5, P6, P7);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(MARKER, STRING, P0, P1, P2, P3, P4, P5, P6, P7);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(STRING, P0, P1, P2, P3, P4, P5, P6, P7, P8);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(MARKER, STRING, P0, P1, P2, P3, P4, P5, P6, P7, P8);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(STRING, P0, P1, P2, P3, P4, P5, P6, P7, P8, P9);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(MARKER, STRING, P0, P1, P2, P3, P4, P5, P6, P7, P8, P9);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(STRING, PARRAY);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(MARKER, STRING, PARRAY);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(() -> MESSAGE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(() -> MESSAGE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(MARKER, () -> MESSAGE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(MARKER, () -> MESSAGE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(() -> OBJECT);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(() -> OBJECT, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(MARKER, () -> OBJECT);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(MARKER, () -> OBJECT, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(STRING, SUPPLIERS);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.info(MARKER, STRING, SUPPLIERS);
        assertLocationEquals(methodName, lineNumber += 2, app);
    }

    public void testDebug(final ListAppender app) {
        app.clear();
        final String methodName = "testDebug";
        int lineNumber = getLineNumber(); // current line number
        logger.debug(CHAR_SEQUENCE);
        assertLocationEquals(methodName, ++lineNumber, app);
        logger.debug(CHAR_SEQUENCE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(MARKER, CHAR_SEQUENCE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(MARKER, CHAR_SEQUENCE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(MESSAGE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(MESSAGE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(MARKER, MESSAGE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(MARKER, MESSAGE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(OBJECT);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(OBJECT, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(MARKER, OBJECT);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(MARKER, OBJECT, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(STRING);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(MARKER, STRING);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(STRING, P0);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(MARKER, STRING, P0);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(STRING, P0, P1);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(MARKER, STRING, P0, P1);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(STRING, P0, P1, P2);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(MARKER, STRING, P0, P1, P2);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(STRING, P0, P1, P2, P3);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(MARKER, STRING, P0, P1, P2, P3);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(STRING, P0, P1, P2, P3, P4);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(MARKER, STRING, P0, P1, P2, P3, P4);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(STRING, P0, P1, P2, P3, P4, P5);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(MARKER, STRING, P0, P1, P2, P3, P4, P5);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(STRING, P0, P1, P2, P3, P4, P5, P6);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(MARKER, STRING, P0, P1, P2, P3, P4, P5, P6);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(STRING, P0, P1, P2, P3, P4, P5, P6, P7);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(MARKER, STRING, P0, P1, P2, P3, P4, P5, P6, P7);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(STRING, P0, P1, P2, P3, P4, P5, P6, P7, P8);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(MARKER, STRING, P0, P1, P2, P3, P4, P5, P6, P7, P8);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(STRING, P0, P1, P2, P3, P4, P5, P6, P7, P8, P9);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(MARKER, STRING, P0, P1, P2, P3, P4, P5, P6, P7, P8, P9);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(STRING, PARRAY);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(MARKER, STRING, PARRAY);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(() -> MESSAGE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(() -> MESSAGE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(MARKER, () -> MESSAGE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(MARKER, () -> MESSAGE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(() -> OBJECT);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(() -> OBJECT, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(MARKER, () -> OBJECT);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(MARKER, () -> OBJECT, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(STRING, SUPPLIERS);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.debug(MARKER, STRING, SUPPLIERS);
        assertLocationEquals(methodName, lineNumber += 2, app);
    }

    public void testTrace(final ListAppender app) {
        app.clear();
        final String methodName = "testTrace";
        int lineNumber = getLineNumber(); // current line number
        logger.trace(CHAR_SEQUENCE);
        assertLocationEquals(methodName, ++lineNumber, app);
        logger.trace(CHAR_SEQUENCE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(MARKER, CHAR_SEQUENCE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(MARKER, CHAR_SEQUENCE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(MESSAGE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(MESSAGE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(MARKER, MESSAGE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(MARKER, MESSAGE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(OBJECT);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(OBJECT, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(MARKER, OBJECT);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(MARKER, OBJECT, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(STRING);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(MARKER, STRING);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(STRING, P0);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(MARKER, STRING, P0);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(STRING, P0, P1);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(MARKER, STRING, P0, P1);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(STRING, P0, P1, P2);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(MARKER, STRING, P0, P1, P2);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(STRING, P0, P1, P2, P3);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(MARKER, STRING, P0, P1, P2, P3);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(STRING, P0, P1, P2, P3, P4);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(MARKER, STRING, P0, P1, P2, P3, P4);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(STRING, P0, P1, P2, P3, P4, P5);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(MARKER, STRING, P0, P1, P2, P3, P4, P5);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(STRING, P0, P1, P2, P3, P4, P5, P6);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(MARKER, STRING, P0, P1, P2, P3, P4, P5, P6);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(STRING, P0, P1, P2, P3, P4, P5, P6, P7);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(MARKER, STRING, P0, P1, P2, P3, P4, P5, P6, P7);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(STRING, P0, P1, P2, P3, P4, P5, P6, P7, P8);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(MARKER, STRING, P0, P1, P2, P3, P4, P5, P6, P7, P8);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(STRING, P0, P1, P2, P3, P4, P5, P6, P7, P8, P9);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(MARKER, STRING, P0, P1, P2, P3, P4, P5, P6, P7, P8, P9);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(STRING, PARRAY);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(MARKER, STRING, PARRAY);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(() -> MESSAGE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(() -> MESSAGE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(MARKER, () -> MESSAGE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(MARKER, () -> MESSAGE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(() -> OBJECT);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(() -> OBJECT, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(MARKER, () -> OBJECT);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(MARKER, () -> OBJECT, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(STRING, SUPPLIERS);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.trace(MARKER, STRING, SUPPLIERS);
        assertLocationEquals(methodName, lineNumber += 2, app);
    }

    public void testLog(final ListAppender app) {
        app.clear();
        final String methodName = "testLog";
        int lineNumber = getLineNumber(); // current line number
        logger.log(Level.INFO, CHAR_SEQUENCE);
        assertLocationEquals(methodName, ++lineNumber, app);
        logger.log(Level.INFO, CHAR_SEQUENCE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, MARKER, CHAR_SEQUENCE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, MARKER, CHAR_SEQUENCE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, MESSAGE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, MESSAGE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, MARKER, MESSAGE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, MARKER, MESSAGE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, OBJECT);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, OBJECT, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, MARKER, OBJECT);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, MARKER, OBJECT, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, STRING);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, MARKER, STRING);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, STRING, P0);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, MARKER, STRING, P0);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, STRING, P0, P1);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, MARKER, STRING, P0, P1);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, STRING, P0, P1, P2);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, MARKER, STRING, P0, P1, P2);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, STRING, P0, P1, P2, P3);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, MARKER, STRING, P0, P1, P2, P3);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, STRING, P0, P1, P2, P3, P4);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, MARKER, STRING, P0, P1, P2, P3, P4);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, STRING, P0, P1, P2, P3, P4, P5);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, MARKER, STRING, P0, P1, P2, P3, P4, P5);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, STRING, P0, P1, P2, P3, P4, P5, P6);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, MARKER, STRING, P0, P1, P2, P3, P4, P5, P6);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, STRING, P0, P1, P2, P3, P4, P5, P6, P7);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, MARKER, STRING, P0, P1, P2, P3, P4, P5, P6, P7);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, STRING, P0, P1, P2, P3, P4, P5, P6, P7, P8);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, MARKER, STRING, P0, P1, P2, P3, P4, P5, P6, P7, P8);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, STRING, P0, P1, P2, P3, P4, P5, P6, P7, P8, P9);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, MARKER, STRING, P0, P1, P2, P3, P4, P5, P6, P7, P8, P9);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, STRING, PARRAY);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, MARKER, STRING, PARRAY);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, () -> MESSAGE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, () -> MESSAGE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, MARKER, () -> MESSAGE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, MARKER, () -> MESSAGE, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, () -> OBJECT);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, () -> OBJECT, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, MARKER, () -> OBJECT);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, MARKER, () -> OBJECT, THROWABLE);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, STRING, SUPPLIERS);
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.log(Level.INFO, MARKER, STRING, SUPPLIERS);
        assertLocationEquals(methodName, lineNumber += 2, app);
    }

    public void testFrames(final ListAppender app) {
        app.clear();
        final String methodName = "testFrames";
        final int lineNumber = getLineNumber(); // Current line
        int i = 0;
        while (i < 2) {
            if (i < 1) {
                logger.debug(OBJECT);
                assertLocationEquals(methodName, lineNumber + 4, app);
            } else {
                logger.debug(OBJECT);
                assertLocationEquals(methodName, lineNumber + 7, app);
            }
            logger.debug(OBJECT);
            assertLocationEquals(methodName, lineNumber + 10, app);
            i++;
        }
        switch (i) {
            case 2:
                logger.debug(OBJECT);
                assertLocationEquals(methodName, lineNumber + 16, app);
                break;
        }
        for (; i >= 0; i--) {
            logger.debug(OBJECT);
            assertLocationEquals(methodName, lineNumber + 21, app);
        }
    }

    public void testPrintf(final ListAppender app) {
        app.clear();
        final String methodName = "testPrintf";
        int lineNumber = getLineNumber(); // Current line
        logger.printf(Level.INFO, "Hello %s.%s", "LoggerConversionHandlerExample", methodName);
        assertLocationEquals(methodName, ++lineNumber, app);
        logger.printf(Level.INFO, MARKER, "Hello %s.%s", "LoggerConversionHandlerExample", methodName);
        assertLocationEquals(methodName, lineNumber += 2, app);
    }

    public void testLogBuilder(final ListAppender app) {
        app.clear();
        final String methodName = "testLogBuilder";
        int lineNumber = getLineNumber(); // Current line
        logger.always().log();
        assertLocationEquals(methodName, ++lineNumber, app);
        logger.atDebug().log();
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.atError().log();
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.atFatal().log();
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.atInfo().log();
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.atLevel(Level.INFO).log();
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.atTrace().log();
        assertLocationEquals(methodName, lineNumber += 2, app);
        logger.atWarn().log();
        assertLocationEquals(methodName, lineNumber += 2, app);
    }

    /**
     * Tests the method calls that should be not modified.
     */
    public void testPassthrough(final ListAppender app) {
        assertTrue(logger.isDebugEnabled());
        assertTrue(logger.isDebugEnabled(MARKER));
        assertTrue(logger.isEnabled(Level.INFO));
        assertTrue(logger.isEnabled(Level.INFO, MARKER));
        assertTrue(logger.isErrorEnabled());
        assertTrue(logger.isErrorEnabled(MARKER));
        assertTrue(logger.isFatalEnabled());
        assertTrue(logger.isFatalEnabled(MARKER));
        assertTrue(logger.isInfoEnabled());
        assertTrue(logger.isInfoEnabled(MARKER));
        assertTrue(logger.isTraceEnabled());
        assertTrue(logger.isTraceEnabled(MARKER));
        assertTrue(logger.isWarnEnabled());
        assertTrue(logger.isWarnEnabled(MARKER));
        assertDoesNotThrow(logger::getName);
        assertDoesNotThrow(logger::getLevel);
        assertDoesNotThrow(logger::getFlowMessageFactory);
        assertDoesNotThrow(logger::getClass);
        app.clear();
        logger.logMessage(Level.INFO, MARKER, LoggerConversionHandlerExample.class.getName(), null, MESSAGE, THROWABLE);
        assertThat(app.getEvents()).hasSize(1);
    }

    public void testCatchingThrowing(final ListAppender app) {
        app.clear();
        final String methodName = "testCatchingThrowing";
        int lineNumber = getLineNumber(); // Current line
        logger.catching(THROWABLE);
        assertThat(app.getEvents()).allMatch(event -> AbstractLogger.CATCHING_MARKER.equals(event.getMarker()));
        assertLocationEquals(methodName, ++lineNumber, app);
        logger.catching(Level.INFO, THROWABLE);
        assertThat(app.getEvents()).allMatch(event -> AbstractLogger.CATCHING_MARKER.equals(event.getMarker()));
        assertLocationEquals(methodName, lineNumber += 3, app);
        assertThat(logger.throwing(THROWABLE)).isInstanceOf(RuntimeException.class);
        assertThat(app.getEvents()).allMatch(event -> AbstractLogger.THROWING_MARKER.equals(event.getMarker()));
        assertLocationEquals(methodName, lineNumber += 3, app);
        assertThat(logger.throwing(THROWABLE)).isInstanceOf(RuntimeException.class);
        assertThat(app.getEvents()).allMatch(event -> AbstractLogger.THROWING_MARKER.equals(event.getMarker()));
        assertLocationEquals(methodName, lineNumber += 3, app);
    }

    public void testTraceEntry(final ListAppender app) {
        app.clear();
        final String methodName = "testTraceEntry";
        int lineNumber = getLineNumber(); // Current line
        EntryMessage entryMessage = logger.traceEntry();
        LogEvent event = assertLocationEquals(methodName, ++lineNumber, app);
        assertThat(event.getMarker()).isEqualTo(AbstractLogger.ENTRY_MARKER);
        assertThat(entryMessage.getMessage()).isNull();

        entryMessage = logger.traceEntry(MESSAGE);
        event = assertLocationEquals(methodName, lineNumber += 5, app);
        assertThat(event.getMarker()).isEqualTo(AbstractLogger.ENTRY_MARKER);
        assertThat(entryMessage.getMessage()).isEqualTo(MESSAGE);

        entryMessage = logger.traceEntry("param1 = {}", PARRAY);
        event = assertLocationEquals(methodName, lineNumber += 5, app);
        assertThat(event.getMarker()).isEqualTo(AbstractLogger.ENTRY_MARKER);
        assertThat(entryMessage.getFormattedMessage()).isEqualTo("Enter param1 = (...)");

        entryMessage = logger.traceEntry(() -> P0, () -> P1);
        event = assertLocationEquals(methodName, lineNumber += 5, app);
        assertThat(event.getMarker()).isEqualTo(AbstractLogger.ENTRY_MARKER);
        assertThat(entryMessage.getFormattedMessage()).isEqualTo("Enter params((p0), (p1))");

        entryMessage = logger.traceEntry("param1 = {}, param2 = {}", () -> P0, () -> P1);
        event = assertLocationEquals(methodName, lineNumber += 5, app);
        assertThat(event.getMarker()).isEqualTo(AbstractLogger.ENTRY_MARKER);
        assertThat(entryMessage.getFormattedMessage()).isEqualTo("Enter param1 = (p0), param2 = (p1)");
    }

    public void testTraceExit(final ListAppender app) {
        app.clear();
        final String methodName = "testTraceExit";
        int lineNumber = getLineNumber(); // Current line
        logger.traceExit();
        LogEvent event = assertLocationEquals(methodName, ++lineNumber, app);
        assertThat(event.getMarker()).isEqualTo(AbstractLogger.EXIT_MARKER);
        assertThat(event.getMessage()).isInstanceOf(ExitMessage.class);
        assertThat(event.getMessage().getFormattedMessage()).isEqualTo("Exit");

        logger.traceExit(ENTRY_MESSAGE);
        event = assertLocationEquals(methodName, lineNumber += 6, app);
        assertThat(event.getMarker()).isEqualTo(AbstractLogger.EXIT_MARKER);
        assertThat(event.getMessage()).isInstanceOf(ExitMessage.class);
        assertThat(event.getMessage().getFormattedMessage()).isEqualTo("Exit (Message)");

        Object result = logger.traceExit(OBJECT);
        assertThat(result).isSameAs(OBJECT);
        event = assertLocationEquals(methodName, lineNumber += 6, app);
        assertThat(event.getMarker()).isEqualTo(AbstractLogger.EXIT_MARKER);
        assertThat(event.getMessage()).isInstanceOf(ExitMessage.class);
        assertThat(event.getMessage().getFormattedMessage()).isEqualTo("Exit with((Object))");

        result = logger.traceExit("result = {}", OBJECT);
        assertThat(result).isSameAs(OBJECT);
        event = assertLocationEquals(methodName, lineNumber += 7, app);
        assertThat(event.getMarker()).isEqualTo(AbstractLogger.EXIT_MARKER);
        assertThat(event.getMessage()).isInstanceOf(ExitMessage.class);
        assertThat(event.getMessage().getFormattedMessage()).isEqualTo("Exit result = (Object)");

        result = logger.traceExit(MESSAGE, OBJECT);
        assertThat(result).isSameAs(OBJECT);
        event = assertLocationEquals(methodName, lineNumber += 7, app);
        assertThat(event.getMarker()).isEqualTo(AbstractLogger.EXIT_MARKER);
        assertThat(event.getMessage()).isInstanceOf(ExitMessage.class);
        assertThat(event.getMessage().getFormattedMessage()).isEqualTo("Exit (Message): (Object)");

        result = logger.traceExit(ENTRY_MESSAGE, OBJECT);
        assertThat(result).isSameAs(OBJECT);
        event = assertLocationEquals(methodName, lineNumber += 7, app);
        assertThat(event.getMarker()).isEqualTo(AbstractLogger.EXIT_MARKER);
        assertThat(event.getMessage()).isInstanceOf(ExitMessage.class);
        assertThat(event.getMessage().getFormattedMessage()).isEqualTo("Exit (Message): (Object)");
    }

    private static LogEvent assertLocationEquals(
            final String methodName, final int lineNumber, final ListAppender app) {
        final List<LogEvent> events = app.getEvents();
        assertThat(events).hasSize(1);
        final LogEvent event = events.get(0);
        assertThat(event.isIncludeLocation()).isFalse();
        assertThat(event.getSource()).isNotNull();
        final StackTraceElement location = event.getSource();
        assertThat(location.getClassName()).isEqualTo(LoggerConversionHandlerExample.class.getName());
        assertThat(location.getMethodName()).isEqualTo(methodName);
        assertThat(location.getFileName()).isEqualTo("LoggerConversionHandlerExample.java");
        assertThat(location.getLineNumber()).isEqualTo(lineNumber);
        app.clear();
        return event;
    }

    private static int getLineNumber() {
        return Thread.currentThread().getStackTrace()[2].getLineNumber();
    }
}
