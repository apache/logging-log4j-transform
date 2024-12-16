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
package org.apache.logging.converter.config.spi.v1;

import org.apache.logging.converter.config.ConfigurationConverterException;
import org.apache.logging.converter.config.spi.ConfigurationNode;
import org.apache.logging.converter.config.spi.PropertiesSubset;
import org.w3c.dom.Element;

public interface Log4j1ComponentParser {

    String getClassName();

    /**
     * Parses an XML element of a Log4j 1 configuration file.
     *
     * @param element An XML element.
     * @param context A Log4j 1 parser context.
     * @return A Log4j Core 2 configuration node.
     * @throws ConfigurationConverterException If a parsing exception occurs.
     */
    ConfigurationNode parseXml(Element element, Log4j1ParserContext context) throws ConfigurationConverterException;

    /**
     * Parser a subset of configuration properties.
     *
     * @param properties A subset of configuration properties.
     * @param context A Log4j 1 parser context.
     * @return A Log4j Core 2 configuration node.
     * @throws ConfigurationConverterException If a parsing exception occurs.
     */
    ConfigurationNode parseProperties(PropertiesSubset properties, Log4j1ParserContext context)
            throws ConfigurationConverterException;
}
