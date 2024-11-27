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
package example;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.logging.converter.config.ConfigurationConverter;
import org.apache.logging.converter.config.ConfigurationConverterException;

public final class Main {

    public static void main(String[] args) {
        if (args.length != 4) {
            System.err.println("Usage: java -jar main.jar <inputFile> <inputFormat> <outputFile> <outputFormat>");
            System.exit(1);
        }
        try {
            convert(args[0], args[1], args[2], args[3]);
        } catch (ConfigurationConverterException | IOException e) {
            System.err.println("Failed to convert " + args[0] + " to " + args[2] + ".");
            e.printStackTrace(System.err);
        }
    }

    // tag::convert[]
    private static void convert(String inputFile, String inputFormat, String outputFile, String outputFormat)
            throws ConfigurationConverterException, IOException {
        Path input = Paths.get(inputFile);
        Path output = Paths.get(outputFile);
        try (InputStream inputStream = Files.newInputStream(input);
             OutputStream outputStream = Files.newOutputStream(output)) {
            ConfigurationConverter converter = ConfigurationConverter.getInstance();
            converter.convert(inputStream, inputFormat, outputStream, outputFormat);
        }
    }
    // end::convert[]

    private Main() {}
}
