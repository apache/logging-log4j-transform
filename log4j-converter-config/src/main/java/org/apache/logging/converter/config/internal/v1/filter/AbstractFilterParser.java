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
package org.apache.logging.converter.config.internal.v1.filter;

import java.util.function.Supplier;
import org.apache.logging.converter.config.internal.ComponentUtils.ConfigurationNodeBuilder;
import org.apache.logging.converter.config.internal.v1.AbstractComponentParser;
import org.apache.logging.converter.config.spi.ConfigurationNode;

/**
 * Common base for all filter parsers.
 */
public abstract class AbstractFilterParser<T extends Supplier<ConfigurationNode>> extends AbstractComponentParser<T> {

    static final String ACCEPT_ON_MATCH_PARAM = "AcceptOnMatch";

    private static final String ACCEPT = "ACCEPT";
    private static final String DENY = "DENY";
    private static final String NEUTRAL = "NEUTRAL";

    private static final String ON_MATCH = "onMatch";
    private static final String ON_MISMATCH = "onMismatch";

    static void addOnMatchAndMismatch(ConfigurationNodeBuilder builder, boolean acceptOnMatch) {
        builder.addAttribute(ON_MATCH, acceptOnMatch ? ACCEPT : DENY).addAttribute(ON_MISMATCH, NEUTRAL);
    }
}
