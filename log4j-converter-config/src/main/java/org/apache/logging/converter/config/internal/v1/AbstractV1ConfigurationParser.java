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
package org.apache.logging.converter.config.internal.v1;

import aQute.bnd.annotation.Cardinality;
import aQute.bnd.annotation.spi.ServiceConsumer;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import org.apache.logging.converter.config.ConfigurationConverterException;
import org.apache.logging.converter.config.spi.ConfigurationParser;
import org.apache.logging.converter.config.spi.v1.Log4j1ComponentParser;
import org.apache.logging.converter.config.spi.v1.Log4j1ParserContext;

@ServiceConsumer(value = Log4j1ComponentParser.class, cardinality = Cardinality.MULTIPLE)
public abstract class AbstractV1ConfigurationParser implements ConfigurationParser, Log4j1ParserContext {

    private static final Map<String, Log4j1ComponentParser> componentParsers = new HashMap<>();

    protected AbstractV1ConfigurationParser() {
        ServiceLoader.load(Log4j1ComponentParser.class)
                .forEach(parser -> componentParsers.put(parser.getClassName(), parser));
    }

    @Override
    public Log4j1ComponentParser getParserForClass(String className) {
        Log4j1ComponentParser parser = componentParsers.get(className);
        if (parser == null) {
            throw new ConfigurationConverterException("Unsupported Log4j 1 component class: " + className);
        }
        return parser;
    }
}
