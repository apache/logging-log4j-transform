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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * Generates source code for custom or extended logger wrappers.
 * <h2>Usage</h2>
 * <h3>Extended logger</h3>
 * <p>
 * To generate source code for an extended logger that adds custom log levels to the existing ones run:
 * </p>
 * <pre>
 * {@code java -jar log4j-generator.jar extendedLogger <logger.class.name> <CUSTOMLEVEL>=<WEIGHT>
 * [CUSTOMLEVEL2=WEIGHT2 [CUSTOMLEVEL3=WEIGHT3] ...]}
 * </pre>
 * <p>
 * Example:
 * </p>
 * <pre>
 * {@code java -jar log4j-generator.jar extendedLogger com.mycomp.ExtLogger DIAG=350 NOTICE=450 VERBOSE=550}
 * </pre>
 * <h3>Custom logger</h3>
 * <p>
 * To generate source code for a custom logger that replaces the existing log levels with custom ones run:
 * </p>
 * <pre>
 * {@code java -jar log4j-generator.jar customLogger <logger.class.name> <CUSTOMLEVEL>=<WEIGHT>
 * [CUSTOMLEVEL2=WEIGHT2 [CUSTOMLEVEL3=WEIGHT3] ...]}
 * </pre>
 * <p>
 * Example:
 * </p>
 * {@code java -jar log4j-generator.jar customLogger com.mycomp.MyLogger DEFCON1=350 DEFCON2=450 DEFCON3=550}
 */
@Command(name = "generate")
public final class Generate {
    // Implementation note:
    // The generated code is in the user's namespace which has its own versioning scheme, so
    // any @since tags in the generated code deliberately mention "Log4j-2.x" rather than just the log4j version number.

    static final String PACKAGE_DECLARATION = "package %s;%n%n";

    enum Type {
        CUSTOM {
            @Override
            String imports() {
                // @formatter:off
                return ""
                        + "import org.apache.logging.log4j.Level;%n"
                        + "import org.apache.logging.log4j.LogManager;%n"
                        + "import org.apache.logging.log4j.Logger;%n"
                        + "import org.apache.logging.log4j.Marker;%n"
                        + "import org.apache.logging.log4j.message.Message;%n"
                        + "import org.apache.logging.log4j.message.MessageFactory;%n"
                        + "import org.apache.logging.log4j.spi.AbstractLogger;%n"
                        + "import org.apache.logging.log4j.spi.ExtendedLoggerWrapper;%n"
                        + "import org.apache.logging.log4j.util.MessageSupplier;%n"
                        + "import org.apache.logging.log4j.util.Supplier;%n"
                        + "%n";
                // @formatter:on
            }

            @Override
            String declaration() {
                // @formatter:off
                return ""
                        + "/**%n"
                        + " * Custom Logger interface with convenience methods for%n"
                        + " * %s%n"
                        + " * <p>Compatible with Log4j 2.6 or higher.</p>%n"
                        + " */%n"
                        + "public final class %s {%n"
                        + "    private final ExtendedLoggerWrapper logger;%n"
                        + "%n";
                // @formatter:on
            }

            @Override
            String constructor() {
                // @formatter:off
                return ""
                        + "%n"
                        + "    private %s(final Logger logger) {%n"
                        + "        this.logger = new ExtendedLoggerWrapper((AbstractLogger) logger, logger.getName(), "
                        + "logger.getMessageFactory());%n"
                        + "    }%n";
                // @formatter:on
            }
        },
        EXTEND {
            @Override
            String imports() {
                // @formatter:off
                return ""
                        + "import org.apache.logging.log4j.Level;%n"
                        + "import org.apache.logging.log4j.LogManager;%n"
                        + "import org.apache.logging.log4j.Logger;%n"
                        + "import org.apache.logging.log4j.Marker;%n"
                        + "import org.apache.logging.log4j.message.Message;%n"
                        + "import org.apache.logging.log4j.message.MessageFactory;%n"
                        + "import org.apache.logging.log4j.spi.AbstractLogger;%n"
                        + "import org.apache.logging.log4j.spi.ExtendedLoggerWrapper;%n"
                        + "import org.apache.logging.log4j.util.MessageSupplier;%n"
                        + "import org.apache.logging.log4j.util.Supplier;%n"
                        + "%n";
                // @formatter:on
            }

            @Override
            String declaration() {
                // @formatter:off
                return ""
                        + "/**%n"
                        + " * Extended Logger interface with convenience methods for%n"
                        + " * %s%n"
                        + " * <p>Compatible with Log4j 2.6 or higher.</p>%n"
                        + " */%n"
                        + "public final class %s extends ExtendedLoggerWrapper {%n"
                        + "    private static final long serialVersionUID = " + System.nanoTime() + "L;%n"
                        + "    private final ExtendedLoggerWrapper logger;%n"
                        + "%n";
                // @formatter:on
            }

            @Override
            String constructor() {
                // @formatter:off
                return ""
                        + "%n"
                        + "    private %s(final Logger logger) {%n"
                        + "        super((AbstractLogger) logger, logger.getName(), logger.getMessageFactory());%n"
                        + "        this.logger = this;%n"
                        + "    }%n";
                // @formatter:on
            }
        };

        abstract String imports();

        abstract String declaration();

        abstract String constructor();
    }

    static final String FQCN_FIELD = "" + "    private static final String FQCN = %s.class.getName();%n";

    static final String LEVEL_FIELD = "" + "    private static final Level %s = Level.forName(\"%s\", %d);%n";

    static final String FACTORY_METHODS = ""
            // @formatter:off
            + "%n"
            + "    /**%n"
            + "     * Returns a custom Logger with the name of the calling class.%n"
            + "     * %n"
            + "     * @return The custom Logger for the calling class.%n"
            + "     */%n"
            + "    public static CLASSNAME create() {%n"
            + "        final Logger wrapped = LogManager.getLogger();%n"
            + "        return new CLASSNAME(wrapped);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Returns a custom Logger using the fully qualified name of the Class as%n"
            + "     * the Logger name.%n"
            + "     * %n"
            + "     * @param loggerName The Class whose name should be used as the Logger name.%n"
            + "     *            If null it will default to the calling class.%n"
            + "     * @return The custom Logger.%n"
            + "     */%n"
            + "    public static CLASSNAME create(final Class<?> loggerName) {%n"
            + "        final Logger wrapped = LogManager.getLogger(loggerName);%n"
            + "        return new CLASSNAME(wrapped);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Returns a custom Logger using the fully qualified name of the Class as%n"
            + "     * the Logger name.%n"
            + "     * %n"
            + "     * @param loggerName The Class whose name should be used as the Logger name.%n"
            + "     *            If null it will default to the calling class.%n"
            + "     * @param messageFactory The message factory is used only when creating a%n"
            + "     *            logger, subsequent use does not change the logger but will log%n"
            + "     *            a warning if mismatched.%n"
            + "     * @return The custom Logger.%n"
            + "     */%n"
            + "    public static CLASSNAME create(final Class<?> loggerName, final MessageFactory"
            + " messageFactory) {%n"
            + "        final Logger wrapped = LogManager.getLogger(loggerName, messageFactory);%n"
            + "        return new CLASSNAME(wrapped);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Returns a custom Logger using the fully qualified class name of the value%n"
            + "     * as the Logger name.%n"
            + "     * %n"
            + "     * @param value The value whose class name should be used as the Logger%n"
            + "     *            name. If null the name of the calling class will be used as%n"
            + "     *            the logger name.%n"
            + "     * @return The custom Logger.%n"
            + "     */%n"
            + "    public static CLASSNAME create(final Object value) {%n"
            + "        final Logger wrapped = LogManager.getLogger(value);%n"
            + "        return new CLASSNAME(wrapped);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Returns a custom Logger using the fully qualified class name of the value%n"
            + "     * as the Logger name.%n"
            + "     * %n"
            + "     * @param value The value whose class name should be used as the Logger%n"
            + "     *            name. If null the name of the calling class will be used as%n"
            + "     *            the logger name.%n"
            + "     * @param messageFactory The message factory is used only when creating a%n"
            + "     *            logger, subsequent use does not change the logger but will log%n"
            + "     *            a warning if mismatched.%n"
            + "     * @return The custom Logger.%n"
            + "     */%n"
            + "    public static CLASSNAME create(final Object value, final MessageFactory messageFactory) {%n"
            + "        final Logger wrapped = LogManager.getLogger(value, messageFactory);%n"
            + "        return new CLASSNAME(wrapped);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Returns a custom Logger with the specified name.%n"
            + "     * %n"
            + "     * @param name The logger name. If null the name of the calling class will%n"
            + "     *            be used.%n"
            + "     * @return The custom Logger.%n"
            + "     */%n"
            + "    public static CLASSNAME create(final String name) {%n"
            + "        final Logger wrapped = LogManager.getLogger(name);%n"
            + "        return new CLASSNAME(wrapped);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Returns a custom Logger with the specified name.%n"
            + "     * %n"
            + "     * @param name The logger name. If null the name of the calling class will%n"
            + "     *            be used.%n"
            + "     * @param messageFactory The message factory is used only when creating a%n"
            + "     *            logger, subsequent use does not change the logger but will log%n"
            + "     *            a warning if mismatched.%n"
            + "     * @return The custom Logger.%n"
            + "     */%n"
            + "    public static CLASSNAME create(final String name, final MessageFactory messageFactory) {%n"
            + "        final Logger wrapped = LogManager.getLogger(name, messageFactory);%n"
            + "        return new CLASSNAME(wrapped);%n"
            + "    }%n";
    // @formatter:on

    static final String METHODS = ""
            // @formatter:off
            + "%n"
            + "    /**%n"
            + "     * Logs a message with the specific Marker at the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param marker the marker data specific to this log statement%n"
            + "     * @param msg the message string to be logged%n"
            + "     */%n"
            + "    public void methodName(final Marker marker, final Message msg) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, marker, msg, (Throwable) null);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message with the specific Marker at the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param marker the marker data specific to this log statement%n"
            + "     * @param msg the message string to be logged%n"
            + "     * @param t A Throwable or null.%n"
            + "     */%n"
            + "    public void methodName(final Marker marker, final Message msg, final Throwable t) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, marker, msg, t);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message object with the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param marker the marker data specific to this log statement%n"
            + "     * @param message the message object to log.%n"
            + "     */%n"
            + "    public void methodName(final Marker marker, final Object message) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, marker, message, (Throwable) null);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message CharSequence with the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param marker the marker data specific to this log statement%n"
            + "     * @param message the message CharSequence to log.%n"
            + "     * @since Log4j-2.6%n"
            + "     */%n"
            + "    public void methodName(final Marker marker, final CharSequence message) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, marker, message, (Throwable) null);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message at the {@code CUSTOM_LEVEL} level including the stack trace of%n"
            + "     * the {@link Throwable} {@code t} passed as parameter.%n"
            + "     * %n"
            + "     * @param marker the marker data specific to this log statement%n"
            + "     * @param message the message to log.%n"
            + "     * @param t the exception to log, including its stack trace.%n"
            + "     */%n"
            + "    public void methodName(final Marker marker, final Object message, final Throwable t) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, marker, message, t);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message at the {@code CUSTOM_LEVEL} level including the stack trace of%n"
            + "     * the {@link Throwable} {@code t} passed as parameter.%n"
            + "     * %n"
            + "     * @param marker the marker data specific to this log statement%n"
            + "     * @param message the CharSequence to log.%n"
            + "     * @param t the exception to log, including its stack trace.%n"
            + "     * @since Log4j-2.6%n"
            + "     */%n"
            + "    public void methodName(final Marker marker, final CharSequence message, final Throwable t) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, marker, message, t);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message object with the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param marker the marker data specific to this log statement%n"
            + "     * @param message the message object to log.%n"
            + "     */%n"
            + "    public void methodName(final Marker marker, final String message) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, marker, message, (Throwable) null);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message with parameters at the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param marker the marker data specific to this log statement%n"
            + "     * @param message the message to log; the format depends on the message factory.%n"
            + "     * @param params parameters to the message.%n"
            + "     * @see #getMessageFactory()%n"
            + "     */%n"
            + "    public void methodName(final Marker marker, final String message, final Object... params) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, marker, message, params);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message with parameters at the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param marker the marker data specific to this log statement%n"
            + "     * @param message the message to log; the format depends on the message factory.%n"
            + "     * @param p0 parameter to the message.%n"
            + "     * @see #getMessageFactory()%n"
            + "     * @since Log4j-2.6%n"
            + "     */%n"
            + "    public void methodName(final Marker marker, final String message, final Object p0) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, marker, message, p0);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message with parameters at the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param marker the marker data specific to this log statement%n"
            + "     * @param message the message to log; the format depends on the message factory.%n"
            + "     * @param p0 parameter to the message.%n"
            + "     * @param p1 parameter to the message.%n"
            + "     * @see #getMessageFactory()%n"
            + "     * @since Log4j-2.6%n"
            + "     */%n"
            + "    public void methodName(final Marker marker, final String message, final Object p0, "
            + "final Object p1) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, marker, message, p0, p1);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message with parameters at the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param marker the marker data specific to this log statement%n"
            + "     * @param message the message to log; the format depends on the message factory.%n"
            + "     * @param p0 parameter to the message.%n"
            + "     * @param p1 parameter to the message.%n"
            + "     * @param p2 parameter to the message.%n"
            + "     * @see #getMessageFactory()%n"
            + "     * @since Log4j-2.6%n"
            + "     */%n"
            + "    public void methodName(final Marker marker, final String message, final Object p0, "
            + "final Object p1, final Object p2) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, marker, message, p0, p1, p2);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message with parameters at the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param marker the marker data specific to this log statement%n"
            + "     * @param message the message to log; the format depends on the message factory.%n"
            + "     * @param p0 parameter to the message.%n"
            + "     * @param p1 parameter to the message.%n"
            + "     * @param p2 parameter to the message.%n"
            + "     * @param p3 parameter to the message.%n"
            + "     * @see #getMessageFactory()%n"
            + "     * @since Log4j-2.6%n"
            + "     */%n"
            + "    public void methodName(final Marker marker, final String message, final Object p0, "
            + "final Object p1, final Object p2,%n"
            + "            final Object p3) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, marker, message, p0, p1, p2, p3);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message with parameters at the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param marker the marker data specific to this log statement%n"
            + "     * @param message the message to log; the format depends on the message factory.%n"
            + "     * @param p0 parameter to the message.%n"
            + "     * @param p1 parameter to the message.%n"
            + "     * @param p2 parameter to the message.%n"
            + "     * @param p3 parameter to the message.%n"
            + "     * @param p4 parameter to the message.%n"
            + "     * @see #getMessageFactory()%n"
            + "     * @since Log4j-2.6%n"
            + "     */%n"
            + "    public void methodName(final Marker marker, final String message, final Object p0, "
            + "final Object p1, final Object p2,%n"
            + "            final Object p3, final Object p4) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, marker, message, p0, p1, p2, p3, p4);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message with parameters at the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param marker the marker data specific to this log statement%n"
            + "     * @param message the message to log; the format depends on the message factory.%n"
            + "     * @param p0 parameter to the message.%n"
            + "     * @param p1 parameter to the message.%n"
            + "     * @param p2 parameter to the message.%n"
            + "     * @param p3 parameter to the message.%n"
            + "     * @param p4 parameter to the message.%n"
            + "     * @param p5 parameter to the message.%n"
            + "     * @see #getMessageFactory()%n"
            + "     * @since Log4j-2.6%n"
            + "     */%n"
            + "    public void methodName(final Marker marker, final String message, final Object p0, "
            + "final Object p1, final Object p2,%n"
            + "            final Object p3, final Object p4, final Object p5) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, marker, message, p0, p1, p2, p3, p4, p5);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message with parameters at the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param marker the marker data specific to this log statement%n"
            + "     * @param message the message to log; the format depends on the message factory.%n"
            + "     * @param p0 parameter to the message.%n"
            + "     * @param p1 parameter to the message.%n"
            + "     * @param p2 parameter to the message.%n"
            + "     * @param p3 parameter to the message.%n"
            + "     * @param p4 parameter to the message.%n"
            + "     * @param p5 parameter to the message.%n"
            + "     * @param p6 parameter to the message.%n"
            + "     * @see #getMessageFactory()%n"
            + "     * @since Log4j-2.6%n"
            + "     */%n"
            + "    public void methodName(final Marker marker, final String message, final Object p0, "
            + "final Object p1, final Object p2,%n"
            + "            final Object p3, final Object p4, final Object p5, final Object p6) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, marker, message, p0, p1, p2, p3, p4, p5, p6);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message with parameters at the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param marker the marker data specific to this log statement%n"
            + "     * @param message the message to log; the format depends on the message factory.%n"
            + "     * @param p0 parameter to the message.%n"
            + "     * @param p1 parameter to the message.%n"
            + "     * @param p2 parameter to the message.%n"
            + "     * @param p3 parameter to the message.%n"
            + "     * @param p4 parameter to the message.%n"
            + "     * @param p5 parameter to the message.%n"
            + "     * @param p6 parameter to the message.%n"
            + "     * @param p7 parameter to the message.%n"
            + "     * @see #getMessageFactory()%n"
            + "     * @since Log4j-2.6%n"
            + "     */%n"
            + "    public void methodName(final Marker marker, final String message, final Object p0, "
            + "final Object p1, final Object p2,%n"
            + "            final Object p3, final Object p4, final Object p5, final Object p6,%n"
            + "            final Object p7) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message with parameters at the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param marker the marker data specific to this log statement%n"
            + "     * @param message the message to log; the format depends on the message factory.%n"
            + "     * @param p0 parameter to the message.%n"
            + "     * @param p1 parameter to the message.%n"
            + "     * @param p2 parameter to the message.%n"
            + "     * @param p3 parameter to the message.%n"
            + "     * @param p4 parameter to the message.%n"
            + "     * @param p5 parameter to the message.%n"
            + "     * @param p6 parameter to the message.%n"
            + "     * @param p7 parameter to the message.%n"
            + "     * @param p8 parameter to the message.%n"
            + "     * @see #getMessageFactory()%n"
            + "     * @since Log4j-2.6%n"
            + "     */%n"
            + "    public void methodName(final Marker marker, final String message, final Object p0, "
            + "final Object p1, final Object p2,%n"
            + "            final Object p3, final Object p4, final Object p5, final Object p6,%n"
            + "            final Object p7, final Object p8) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, "
            + "p8);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message with parameters at the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param marker the marker data specific to this log statement%n"
            + "     * @param message the message to log; the format depends on the message factory.%n"
            + "     * @param p0 parameter to the message.%n"
            + "     * @param p1 parameter to the message.%n"
            + "     * @param p2 parameter to the message.%n"
            + "     * @param p3 parameter to the message.%n"
            + "     * @param p4 parameter to the message.%n"
            + "     * @param p5 parameter to the message.%n"
            + "     * @param p6 parameter to the message.%n"
            + "     * @param p7 parameter to the message.%n"
            + "     * @param p8 parameter to the message.%n"
            + "     * @param p9 parameter to the message.%n"
            + "     * @see #getMessageFactory()%n"
            + "     * @since Log4j-2.6%n"
            + "     */%n"
            + "    public void methodName(final Marker marker, final String message, final Object p0, "
            + "final Object p1, final Object p2,%n"
            + "            final Object p3, final Object p4, final Object p5, final Object p6,%n"
            + "            final Object p7, final Object p8, final Object p9) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, "
            + "p8, p9);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message at the {@code CUSTOM_LEVEL} level including the stack trace of%n"
            + "     * the {@link Throwable} {@code t} passed as parameter.%n"
            + "     * %n"
            + "     * @param marker the marker data specific to this log statement%n"
            + "     * @param message the message to log.%n"
            + "     * @param t the exception to log, including its stack trace.%n"
            + "     */%n"
            + "    public void methodName(final Marker marker, final String message, final Throwable t) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, marker, message, t);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs the specified Message at the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param msg the message string to be logged%n"
            + "     */%n"
            + "    public void methodName(final Message msg) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, null, msg, (Throwable) null);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs the specified Message at the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param msg the message string to be logged%n"
            + "     * @param t A Throwable or null.%n"
            + "     */%n"
            + "    public void methodName(final Message msg, final Throwable t) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, null, msg, t);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message object with the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param message the message object to log.%n"
            + "     */%n"
            + "    public void methodName(final Object message) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, null, message, (Throwable) null);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message at the {@code CUSTOM_LEVEL} level including the stack trace of%n"
            + "     * the {@link Throwable} {@code t} passed as parameter.%n"
            + "     * %n"
            + "     * @param message the message to log.%n"
            + "     * @param t the exception to log, including its stack trace.%n"
            + "     */%n"
            + "    public void methodName(final Object message, final Throwable t) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, null, message, t);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message CharSequence with the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param message the message CharSequence to log.%n"
            + "     * @since Log4j-2.6%n"
            + "     */%n"
            + "    public void methodName(final CharSequence message) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, null, message, (Throwable) null);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a CharSequence at the {@code CUSTOM_LEVEL} level including the stack trace of%n"
            + "     * the {@link Throwable} {@code t} passed as parameter.%n"
            + "     * %n"
            + "     * @param message the CharSequence to log.%n"
            + "     * @param t the exception to log, including its stack trace.%n"
            + "     * @since Log4j-2.6%n"
            + "     */%n"
            + "    public void methodName(final CharSequence message, final Throwable t) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, null, message, t);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message object with the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param message the message object to log.%n"
            + "     */%n"
            + "    public void methodName(final String message) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, null, message, (Throwable) null);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message with parameters at the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param message the message to log; the format depends on the message factory.%n"
            + "     * @param params parameters to the message.%n"
            + "     * @see #getMessageFactory()%n"
            + "     */%n"
            + "    public void methodName(final String message, final Object... params) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, null, message, params);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message with parameters at the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param message the message to log; the format depends on the message factory.%n"
            + "     * @param p0 parameter to the message.%n"
            + "     * @see #getMessageFactory()%n"
            + "     * @since Log4j-2.6%n"
            + "     */%n"
            + "    public void methodName(final String message, final Object p0) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, null, message, p0);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message with parameters at the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param message the message to log; the format depends on the message factory.%n"
            + "     * @param p0 parameter to the message.%n"
            + "     * @param p1 parameter to the message.%n"
            + "     * @see #getMessageFactory()%n"
            + "     * @since Log4j-2.6%n"
            + "     */%n"
            + "    public void methodName(final String message, final Object p0, "
            + "final Object p1) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, null, message, p0, p1);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message with parameters at the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param message the message to log; the format depends on the message factory.%n"
            + "     * @param p0 parameter to the message.%n"
            + "     * @param p1 parameter to the message.%n"
            + "     * @param p2 parameter to the message.%n"
            + "     * @see #getMessageFactory()%n"
            + "     * @since Log4j-2.6%n"
            + "     */%n"
            + "    public void methodName(final String message, final Object p0, "
            + "final Object p1, final Object p2) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, null, message, p0, p1, p2);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message with parameters at the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param message the message to log; the format depends on the message factory.%n"
            + "     * @param p0 parameter to the message.%n"
            + "     * @param p1 parameter to the message.%n"
            + "     * @param p2 parameter to the message.%n"
            + "     * @param p3 parameter to the message.%n"
            + "     * @see #getMessageFactory()%n"
            + "     * @since Log4j-2.6%n"
            + "     */%n"
            + "    public void methodName(final String message, final Object p0, "
            + "final Object p1, final Object p2,%n"
            + "            final Object p3) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, null, message, p0, p1, p2, p3);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message with parameters at the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param message the message to log; the format depends on the message factory.%n"
            + "     * @param p0 parameter to the message.%n"
            + "     * @param p1 parameter to the message.%n"
            + "     * @param p2 parameter to the message.%n"
            + "     * @param p3 parameter to the message.%n"
            + "     * @param p4 parameter to the message.%n"
            + "     * @see #getMessageFactory()%n"
            + "     * @since Log4j-2.6%n"
            + "     */%n"
            + "    public void methodName(final String message, final Object p0, "
            + "final Object p1, final Object p2,%n"
            + "            final Object p3, final Object p4) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, null, message, p0, p1, p2, p3, p4);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message with parameters at the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param message the message to log; the format depends on the message factory.%n"
            + "     * @param p0 parameter to the message.%n"
            + "     * @param p1 parameter to the message.%n"
            + "     * @param p2 parameter to the message.%n"
            + "     * @param p3 parameter to the message.%n"
            + "     * @param p4 parameter to the message.%n"
            + "     * @param p5 parameter to the message.%n"
            + "     * @see #getMessageFactory()%n"
            + "     * @since Log4j-2.6%n"
            + "     */%n"
            + "    public void methodName(final String message, final Object p0, "
            + "final Object p1, final Object p2,%n"
            + "            final Object p3, final Object p4, final Object p5) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, null, message, p0, p1, p2, p3, p4, p5);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message with parameters at the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param message the message to log; the format depends on the message factory.%n"
            + "     * @param p0 parameter to the message.%n"
            + "     * @param p1 parameter to the message.%n"
            + "     * @param p2 parameter to the message.%n"
            + "     * @param p3 parameter to the message.%n"
            + "     * @param p4 parameter to the message.%n"
            + "     * @param p5 parameter to the message.%n"
            + "     * @param p6 parameter to the message.%n"
            + "     * @see #getMessageFactory()%n"
            + "     * @since Log4j-2.6%n"
            + "     */%n"
            + "    public void methodName(final String message, final Object p0, "
            + "final Object p1, final Object p2,%n"
            + "            final Object p3, final Object p4, final Object p5, final Object p6) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, null, message, p0, p1, p2, p3, p4, p5, p6);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message with parameters at the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param message the message to log; the format depends on the message factory.%n"
            + "     * @param p0 parameter to the message.%n"
            + "     * @param p1 parameter to the message.%n"
            + "     * @param p2 parameter to the message.%n"
            + "     * @param p3 parameter to the message.%n"
            + "     * @param p4 parameter to the message.%n"
            + "     * @param p5 parameter to the message.%n"
            + "     * @param p6 parameter to the message.%n"
            + "     * @param p7 parameter to the message.%n"
            + "     * @see #getMessageFactory()%n"
            + "     * @since Log4j-2.6%n"
            + "     */%n"
            + "    public void methodName(final String message, final Object p0, "
            + "final Object p1, final Object p2,%n"
            + "            final Object p3, final Object p4, final Object p5, final Object p6,%n"
            + "            final Object p7) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, null, message, p0, p1, p2, p3, p4, p5, p6, p7);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message with parameters at the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param message the message to log; the format depends on the message factory.%n"
            + "     * @param p0 parameter to the message.%n"
            + "     * @param p1 parameter to the message.%n"
            + "     * @param p2 parameter to the message.%n"
            + "     * @param p3 parameter to the message.%n"
            + "     * @param p4 parameter to the message.%n"
            + "     * @param p5 parameter to the message.%n"
            + "     * @param p6 parameter to the message.%n"
            + "     * @param p7 parameter to the message.%n"
            + "     * @param p8 parameter to the message.%n"
            + "     * @see #getMessageFactory()%n"
            + "     * @since Log4j-2.6%n"
            + "     */%n"
            + "    public void methodName(final String message, final Object p0, "
            + "final Object p1, final Object p2,%n"
            + "            final Object p3, final Object p4, final Object p5, final Object p6,%n"
            + "            final Object p7, final Object p8) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, null, message, p0, p1, p2, p3, p4, p5, p6, p7, "
            + "p8);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message with parameters at the {@code CUSTOM_LEVEL} level.%n"
            + "     * %n"
            + "     * @param message the message to log; the format depends on the message factory.%n"
            + "     * @param p0 parameter to the message.%n"
            + "     * @param p1 parameter to the message.%n"
            + "     * @param p2 parameter to the message.%n"
            + "     * @param p3 parameter to the message.%n"
            + "     * @param p4 parameter to the message.%n"
            + "     * @param p5 parameter to the message.%n"
            + "     * @param p6 parameter to the message.%n"
            + "     * @param p7 parameter to the message.%n"
            + "     * @param p8 parameter to the message.%n"
            + "     * @param p9 parameter to the message.%n"
            + "     * @see #getMessageFactory()%n"
            + "     * @since Log4j-2.6%n"
            + "     */%n"
            + "    public void methodName(final String message, final Object p0, "
            + "final Object p1, final Object p2,%n"
            + "            final Object p3, final Object p4, final Object p5, final Object p6,%n"
            + "            final Object p7, final Object p8, final Object p9) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, null, message, p0, p1, p2, p3, p4, p5, p6, p7, "
            + "p8, p9);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message at the {@code CUSTOM_LEVEL} level including the stack trace of%n"
            + "     * the {@link Throwable} {@code t} passed as parameter.%n"
            + "     * %n"
            + "     * @param message the message to log.%n"
            + "     * @param t the exception to log, including its stack trace.%n"
            + "     */%n"
            + "    public void methodName(final String message, final Throwable t) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, null, message, t);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message which is only to be constructed if the logging level is the {@code CUSTOM_LEVEL}"
            + "level.%n"
            + "     *%n"
            + "     * @param msgSupplier A function, which when called, produces the desired log message;%n"
            + "     *            the format depends on the message factory.%n"
            + "     * @since Log4j-2.4%n"
            + "     */%n"
            + "    public void methodName(final Supplier<?> msgSupplier) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, null, msgSupplier, (Throwable) null);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message (only to be constructed if the logging level is the {@code CUSTOM_LEVEL}%n"
            + "     * level) including the stack trace of the {@link Throwable} <code>t</code> passed as parameter.%n"
            + "     *%n"
            + "     * @param msgSupplier A function, which when called, produces the desired log message;%n"
            + "     *            the format depends on the message factory.%n"
            + "     * @param t the exception to log, including its stack trace.%n"
            + "     * @since Log4j-2.4%n"
            + "     */%n"
            + "    public void methodName(final Supplier<?> msgSupplier, final Throwable t) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, null, msgSupplier, t);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message which is only to be constructed if the logging level is the%n"
            + "     * {@code CUSTOM_LEVEL} level with the specified Marker.%n"
            + "     *%n"
            + "     * @param marker the marker data specific to this log statement%n"
            + "     * @param msgSupplier A function, which when called, produces the desired log message;%n"
            + "     *            the format depends on the message factory.%n"
            + "     * @since Log4j-2.4%n"
            + "     */%n"
            + "    public void methodName(final Marker marker, final Supplier<?> msgSupplier) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, marker, msgSupplier, (Throwable) null);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message with parameters which are only to be constructed if the logging level is the%n"
            + "     * {@code CUSTOM_LEVEL} level.%n"
            + "     *%n"
            + "     * @param marker the marker data specific to this log statement%n"
            + "     * @param message the message to log; the format depends on the message factory.%n"
            + "     * @param paramSuppliers An array of functions, which when called, produce the desired log"
            + " message parameters.%n"
            + "     * @since Log4j-2.4%n"
            + "     */%n"
            + "    public void methodName(final Marker marker, final String message, final Supplier<?>..."
            + " paramSuppliers) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, marker, message, paramSuppliers);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message (only to be constructed if the logging level is the {@code CUSTOM_LEVEL}%n"
            + "     * level) with the specified Marker and including the stack trace of the {@link Throwable}%n"
            + "     * <code>t</code> passed as parameter.%n"
            + "     *%n"
            + "     * @param marker the marker data specific to this log statement%n"
            + "     * @param msgSupplier A function, which when called, produces the desired log message;%n"
            + "     *            the format depends on the message factory.%n"
            + "     * @param t A Throwable or null.%n"
            + "     * @since Log4j-2.4%n"
            + "     */%n"
            + "    public void methodName(final Marker marker, final Supplier<?> msgSupplier, final Throwable t) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, marker, msgSupplier, t);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message with parameters which are only to be constructed if the logging level is%n"
            + "     * the {@code CUSTOM_LEVEL} level.%n"
            + "     *%n"
            + "     * @param message the message to log; the format depends on the message factory.%n"
            + "     * @param paramSuppliers An array of functions, which when called, produce the desired log"
            + " message parameters.%n"
            + "     * @since Log4j-2.4%n"
            + "     */%n"
            + "    public void methodName(final String message, final Supplier<?>... paramSuppliers) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, null, message, paramSuppliers);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message which is only to be constructed if the logging level is the%n"
            + "     * {@code CUSTOM_LEVEL} level with the specified Marker. The {@code MessageSupplier} may or may%n"
            + "     * not use the {@link MessageFactory} to construct the {@code Message}.%n"
            + "     *%n"
            + "     * @param marker the marker data specific to this log statement%n"
            + "     * @param msgSupplier A function, which when called, produces the desired log message.%n"
            + "     * @since Log4j-2.4%n"
            + "     */%n"
            + "    public void methodName(final Marker marker, final MessageSupplier msgSupplier) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, marker, msgSupplier, (Throwable) null);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message (only to be constructed if the logging level is the {@code CUSTOM_LEVEL}%n"
            + "     * level) with the specified Marker and including the stack trace of the {@link Throwable}%n"
            + "     * <code>t</code> passed as parameter. The {@code MessageSupplier} may or may not use the%n"
            + "     * {@link MessageFactory} to construct the {@code Message}.%n"
            + "     *%n"
            + "     * @param marker the marker data specific to this log statement%n"
            + "     * @param msgSupplier A function, which when called, produces the desired log message.%n"
            + "     * @param t A Throwable or null.%n"
            + "     * @since Log4j-2.4%n"
            + "     */%n"
            + "    public void methodName(final Marker marker, final MessageSupplier msgSupplier, final "
            + "Throwable t) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, marker, msgSupplier, t);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message which is only to be constructed if the logging level is the%n"
            + "     * {@code CUSTOM_LEVEL} level. The {@code MessageSupplier} may or may not use the%n"
            + "     * {@link MessageFactory} to construct the {@code Message}.%n"
            + "     *%n"
            + "     * @param msgSupplier A function, which when called, produces the desired log message.%n"
            + "     * @since Log4j-2.4%n"
            + "     */%n"
            + "    public void methodName(final MessageSupplier msgSupplier) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, null, msgSupplier, (Throwable) null);%n"
            + "    }%n"
            + "%n"
            + "    /**%n"
            + "     * Logs a message (only to be constructed if the logging level is the {@code CUSTOM_LEVEL}%n"
            + "     * level) including the stack trace of the {@link Throwable} <code>t</code> passed as parameter.%n"
            + "     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the%n"
            + "     * {@code Message}.%n"
            + "     *%n"
            + "     * @param msgSupplier A function, which when called, produces the desired log message.%n"
            + "     * @param t the exception to log, including its stack trace.%n"
            + "     * @since Log4j-2.4%n"
            + "     */%n"
            + "    public void methodName(final MessageSupplier msgSupplier, final Throwable t) {%n"
            + "        logger.logIfEnabled(FQCN, CUSTOM_LEVEL, null, msgSupplier, t);%n"
            + "    }%n";
    // @formatter:on

    @Option(names = "-f", description = "Output file.")
    private Path outputFile;

    @Spec
    private CommandSpec spec;

    private Generate() {}

    public static class LevelInfo {
        final String name;
        final int intLevel;

        LevelInfo(final String name, final int intLevel) {
            this.name = name;
            this.intLevel = intLevel;
        }
    }

    public static void main(final String[] args) {
        final Generate command = new Generate();
        new CommandLine(command)
                .registerConverter(LevelInfo.class, command::convertLevelInfo)
                .execute(args);
    }

    private LevelInfo convertLevelInfo(final String value) {
        final String[] parts = value.split("=", 2);
        if (parts.length == 2) {
            try {
                final int intLevel = Integer.parseInt(parts[1]);
                return new LevelInfo(parts[0], intLevel);
            } catch (final NumberFormatException ignored) {
                // Fall through to throw instruction
            }
        }
        throw new ParameterException(
                spec.commandLine(),
                String.format("Invalid level value '%s'; expected form CUSTOMLEVEL=WEIGHT%nE.g. AUDIT=50", value));
    }

    private Writer getWriter() throws IOException {
        if (outputFile != null) {
            return Files.newBufferedWriter(outputFile, UTF_8);
        }
        return new FilterWriter(new OutputStreamWriter(System.out, UTF_8)) {

            @Override
            public void close() throws IOException {
                // Don't close the standard output.
            }
        };
    }

    private void validateLevels(final List<LevelInfo> levels) {
        if (levels == null || levels.isEmpty()) {
            throw new ParameterException(spec.commandLine(), "At least one level parameter is required");
        }
    }

    /**
     * Generates source code for extended logger wrappers that provide convenience methods for the specified custom
     * levels.
     *
     * @param classNameFQN className of the custom logger to generate
     * @param levels a list of NAME=intLevel pair for each custom log
     *            level to generate convenience methods for
     */
    @Command(name = "extendedLogger", description = "Generates a logger with additional log levels.")
    public void generateExtend(
            final @Parameters(description = "Class name to generate", paramLabel = "<className>") String classNameFQN,
            final @Parameters(description = "Additional log levels", paramLabel = "<level>") List<LevelInfo> levels)
            throws IOException {
        validateLevels(levels);
        try (final Writer writer = getWriter()) {
            generateSource(Type.EXTEND, classNameFQN, levels, writer);
            writer.flush();
        }
    }

    /**
     * Generates source code for custom logger wrappers that only provide convenience methods for the specified
     * custom levels, not for the standard built-in levels.
     *
     * @param classNameFQN className of the custom logger to generate
     * @param levels a list of NAME=intLevel pair for each custom log
     *            level to generate convenience methods for
     */
    @Command(name = "customLogger", description = "Generates a logger with custom log methods.")
    public void generateCustom(
            final @Parameters(description = "Class name to generate", paramLabel = "<className>") String classNameFQN,
            final @Parameters(description = "Log levels", paramLabel = "<level>") List<LevelInfo> levels)
            throws IOException {
        validateLevels(levels);
        try (final Writer writer = getWriter()) {
            generateSource(Type.CUSTOM, classNameFQN, levels, writer);
            writer.flush();
        }
    }

    @SuppressFBWarnings(
            value = "FORMAT_STRING_MANIPULATION",
            justification = "The format strings come from constants. The replacement is done for readability.")
    static String generateSource(
            final Type type, final String classNameFQN, final List<LevelInfo> levels, final Writer writer)
            throws IOException {
        final int lastDot = classNameFQN.lastIndexOf('.');
        final String pkg = classNameFQN.substring(0, Math.max(lastDot, 0));
        if (!pkg.isEmpty()) {
            writer.append(String.format(PACKAGE_DECLARATION, pkg));
        }
        writer.append(String.format(type.imports(), ""));
        final String className = classNameFQN.substring(classNameFQN.lastIndexOf('.') + 1);
        final String javadocDescr = javadocDescription(levels);
        writer.append(String.format(type.declaration(), javadocDescr, className));
        writer.append(String.format(FQCN_FIELD, className));
        for (final LevelInfo level : levels) {
            writer.append(String.format(LEVEL_FIELD, level.name, level.name, level.intLevel));
        }
        writer.append(String.format(type.constructor(), className));
        writer.append(String.format(FACTORY_METHODS.replaceAll("CLASSNAME", className), ""));
        for (final LevelInfo level : levels) {
            final String methodName = camelCase(level.name);
            final String phase1 = METHODS.replaceAll("CUSTOM_LEVEL", level.name);
            final String phase2 = phase1.replaceAll("methodName", methodName);
            writer.append(String.format(phase2, ""));
        }

        writer.append('}');
        writer.append(System.getProperty("line.separator"));
        return writer.toString();
    }

    static String javadocDescription(final List<LevelInfo> levels) {
        if (levels.size() == 1) {
            return "the " + levels.get(0).name + " custom log level.";
        }
        final StringBuilder sb = new StringBuilder(512);
        sb.append("the ");
        String sep = "";
        for (int i = 0; i < levels.size(); i++) {
            sb.append(sep);
            sb.append(levels.get(i).name);
            sep = (i == levels.size() - 2) ? " and " : ", ";
        }
        sb.append(" custom log levels.");
        return sb.toString();
    }

    static String camelCase(final String customLevel) {
        final StringBuilder sb = new StringBuilder(customLevel.length());
        boolean lower = true;
        for (final char ch : customLevel.toCharArray()) {
            if (ch == '_') {
                lower = false;
                continue;
            }
            sb.append(lower ? Character.toLowerCase(ch) : Character.toUpperCase(ch));
            lower = true;
        }
        return sb.toString();
    }
}
