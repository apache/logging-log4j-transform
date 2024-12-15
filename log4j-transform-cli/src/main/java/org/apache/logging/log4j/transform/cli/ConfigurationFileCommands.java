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
package org.apache.logging.log4j.transform.cli;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import org.apache.logging.converter.config.ConfigurationConverter;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
        name = "config-file",
        description = "Handles the transformation of logging configuration files.",
        mixinStandardHelpOptions = true,
        subcommands = {ConfigurationFileCommands.Convert.class, ConfigurationFileCommands.ListFormats.class},
        versionProvider = Main.VersionProvider.class)
public final class ConfigurationFileCommands {

    private static final int PADDING_SIZE = 2;

    public static void main(final String[] args) {
        System.exit(new CommandLine(ConfigurationFileCommands.class).execute(args));
    }

    private ConfigurationFileCommands() {}

    @Command(
            name = "list-formats",
            description = "Lists the supported configuration file formats.",
            mixinStandardHelpOptions = true,
            versionProvider = Main.VersionProvider.class)
    static class ListFormats implements Callable<Integer> {

        private final ConfigurationConverter converter = ConfigurationConverter.getInstance();

        @Override
        public Integer call() {
            Map<String, SupportedFormat> supportedFormatMap = new TreeMap<>();
            converter
                    .getSupportedInputFormats()
                    .forEach(formatName -> supportedFormatMap.compute(formatName, SupportedFormat::enableInput));
            converter
                    .getSupportedOutputFormats()
                    .forEach(formatName -> supportedFormatMap.compute(formatName, SupportedFormat::enableOutput));
            converter
                    .getFormatDescriptions()
                    .forEach((formatName, description) -> supportedFormatMap.compute(
                            formatName, (f, old) -> SupportedFormat.updateDescription(f, description, old)));
            System.out.println("Supported formats:");
            System.out.println();
            formatRows(supportedFormatMap.values()).forEach(System.out::println);
            return 0;
        }

        private static List<? extends CharSequence> formatRows(Collection<SupportedFormat> supportedFormats) {
            List<StringBuilder> rows = new ArrayList<>(supportedFormats.size());
            // Print first column
            int maxLength = 0;
            for (SupportedFormat format : supportedFormats) {
                StringBuilder row = new StringBuilder();
                Strings.appendPadding(row, PADDING_SIZE).append(format.formatName);
                maxLength = Math.max(maxLength, row.length());
                rows.add(row);
            }
            // Print second column
            int i = 0;
            for (SupportedFormat format : supportedFormats) {
                StringBuilder row = rows.get(i++);
                Strings.appendPadding(row, PADDING_SIZE + maxLength - row.length());
                if (format.description != null) {
                    row.append(format.description);
                }
                if (!format.input) {
                    if (format.output) {
                        row.append(" (write-only)");
                    } else {
                        rows.remove(--i);
                    }
                } else if (!format.output) {
                    row.append(" (read-only)");
                }
            }
            return rows;
        }
    }

    @Command(
            name = "convert",
            description = "Converts a logging configuration file to a different format.",
            mixinStandardHelpOptions = true,
            versionProvider = Main.VersionProvider.class)
    static class Convert implements Callable<Integer> {

        private final ConfigurationConverter converter = ConfigurationConverter.getInstance();

        private @Nullable String inputFormat;
        private @Nullable String outputFormat;
        private @Nullable File inputFile;
        private @Nullable File outputFile;

        @Option(
                names = {"-i", "--input-format"},
                description = "The format of the input file.",
                required = true)
        public void setInputFormat(String inputFormat) {
            if (!converter.getSupportedInputFormats().contains(inputFormat)) {
                throw new IllegalArgumentException("Unsupported input format: `" + inputFormat
                        + "`.\nRun `listFormats` for a list of supported formats.");
            }
            this.inputFormat = inputFormat;
        }

        @Option(
                names = {"-o", "--output-format"},
                description = "The format of the output file.",
                required = true)
        public void setOutputFormat(String outputFormat) {
            if (!converter.getSupportedOutputFormats().contains(outputFormat)) {
                throw new IllegalArgumentException("Unsupported output format: `" + outputFormat
                        + "`.\nRun `listFormats` for a list of supported formats.");
            }
            this.outputFormat = outputFormat;
        }

        @Parameters(index = "0", description = "The input logging configuration file.")
        public void setInputFile(File inputFile) {
            if (!Files.exists(inputFile.toPath())) {
                throw new IllegalArgumentException("Input file does not exist: `" + inputFile + "`.");
            }
            this.inputFile = inputFile;
        }

        @Parameters(index = "1", description = "The output logging configuration file.")
        public void setOutputFile(File outputFile) {
            if (Files.exists(outputFile.toPath())) {
                throw new IllegalArgumentException("Output file already exists: `" + outputFile + "`.");
            }
            this.outputFile = outputFile;
        }

        @Override
        public Integer call() throws IOException {
            String inputFormat = requireNonNull(this.inputFormat);
            Path inputPath = requireNonNull(this.inputFile).toPath();
            String outputFormat = requireNonNull(this.outputFormat);
            Path outputPath = requireNonNull(this.outputFile).toPath();
            try (InputStream inputStream = Files.newInputStream(inputPath);
                    OutputStream outputStream = Files.newOutputStream(outputPath)) {
                converter.convert(inputStream, inputFormat, outputStream, outputFormat);
            }
            return 0;
        }
    }

    private static final class SupportedFormat {
        private final String formatName;
        private final @Nullable String description;
        private final boolean input;
        private final boolean output;

        private static SupportedFormat enableInput(String formatName, @Nullable SupportedFormat old) {
            return new SupportedFormat(
                    formatName, old != null ? old.formatName : null, true, old != null && old.output);
        }

        private static SupportedFormat enableOutput(String formatName, @Nullable SupportedFormat old) {
            return new SupportedFormat(formatName, old != null ? old.formatName : null, old != null && old.input, true);
        }

        private static SupportedFormat updateDescription(
                String formatName, String description, @Nullable SupportedFormat old) {
            return new SupportedFormat(formatName, description, old != null && old.input, old != null && old.output);
        }

        private SupportedFormat(
                final String formatName,
                @Nullable final String description,
                final boolean input,
                final boolean output) {
            this.formatName = formatName;
            this.description = description;
            this.input = input;
            this.output = output;
        }
    }
}
