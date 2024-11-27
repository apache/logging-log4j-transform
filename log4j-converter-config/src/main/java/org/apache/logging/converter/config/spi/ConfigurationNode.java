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
package org.apache.logging.converter.config.spi;

import java.util.List;
import java.util.Map;
import org.osgi.annotation.versioning.ProviderType;

/**
 * A simplified view of a Log4j Core 2 configuration subtree.
 * <p>
 *     It provides a subset of the information contained in the
 *     <a href="https://logging.apache.org/log4j/2.x/javadoc/log4j-core/org/apache/logging/log4j/core/config/Node.html">o.a.l.l.core.config.Node</a>
 *     class.
 * </p>
 * @see <a href="https://logging.apache.org/log4j/2.x/manual/configuration.html#configuration-syntax">Configuration Syntax</a>
 * for an explanation of the used terms.
 */
@ProviderType
public interface ConfigurationNode {

    /**
     * The name of the plugin configured by this node.
     *
     * @see <a href="https://logging.apache.org/log4j/2.x/manual/plugins.html">Log4j Core Plugins</a> for more details
     */
    String getPluginName();

    /**
     * The configuration attributes
     */
    Map<String, String> getAttributes();

    /**
     * The nested configuration elements.
     */
    List<? extends ConfigurationNode> getChildren();
}
