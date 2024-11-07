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
package org.apache.logging.log4j.converter.plugins.internal;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;

final class JacksonUtils {

    static void assertArrayStart(final JsonParser parser) throws IOException {
        parser.nextToken();
        assertCurrentToken(parser, JsonToken.START_ARRAY, "start of JSON array");
    }

    static void assertArrayEnd(final JsonParser parser) throws IOException {
        assertCurrentToken(parser, JsonToken.END_ARRAY, "end of JSON array");
    }

    static void assertObjectStart(final JsonParser parser) throws IOException {
        parser.nextToken();
        assertCurrentToken(parser, JsonToken.START_OBJECT, "start of JSON object");
    }

    static void assertObjectEnd(final JsonParser parser) throws IOException {
        assertCurrentToken(parser, JsonToken.END_OBJECT, "end of JSON object");
    }

    static void assertCurrentToken(final JsonParser parser, final JsonToken token, final String expectingMessage)
            throws IOException {
        if (parser.currentToken() != token) {
            throw new IOException(String.format(
                    "Parser error at %s: expecting %s, but found '%s'.",
                    parser.currentLocation(), expectingMessage, parser.currentName()));
        }
    }

    private JacksonUtils() {}
}
