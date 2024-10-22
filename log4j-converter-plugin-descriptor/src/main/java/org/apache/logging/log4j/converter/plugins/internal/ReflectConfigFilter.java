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

import static org.apache.logging.log4j.converter.plugins.internal.JacksonUtils.assertArrayEnd;
import static org.apache.logging.log4j.converter.plugins.internal.JacksonUtils.assertArrayStart;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Set;

public class ReflectConfigFilter {

    private static final String TYPE_NAME = "name";

    private final Set<String> includeClassNames;

    public ReflectConfigFilter(final Set<String> includeClassNames) {
        this.includeClassNames = includeClassNames;
    }

    public void filter(final JsonParser input, final JsonGenerator output) throws IOException {
        assertArrayStart(input);
        output.writeStartArray();
        // Read and filter entries
        while (input.nextToken() == JsonToken.START_OBJECT) {
            final JsonNode node = input.readValueAsTree();
            final JsonNode nameNode = node.get(TYPE_NAME);
            if (nameNode != null) {
                final String name = nameNode.asText();
                // Include all the plugin visitors
                if (name.startsWith("org.apache.logging.log4j.core.config.plugins.visitors")
                        || includeClassNames.contains(name)) {
                    output.writeTree(node);
                }
            }
        }
        assertArrayEnd(input);
        output.writeEndArray();
    }
}
