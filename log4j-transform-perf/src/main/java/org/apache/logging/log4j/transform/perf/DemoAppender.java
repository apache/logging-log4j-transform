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
package org.apache.logging.log4j.transform.perf;

import java.nio.ByteBuffer;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.layout.ByteBufferDestinationHelper;
import org.apache.logging.log4j.core.util.Constants;

/**
 * An Appender that ignores log events but formats
 */
@Plugin(name = DemoAppender.PLUGIN_NAME, category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class DemoAppender extends AbstractAppender implements ByteBufferDestination {

    public static final String PLUGIN_NAME = "Demo";

    private final ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[4096]);
    public long checksum;

    @PluginFactory
    public static DemoAppender createAppender(
            @PluginAttribute(value = "name", defaultString = "DEMO") final String name,
            @PluginElement("Layout") final Layout<?> layout) {
        return new DemoAppender(name, layout);
    }

    private DemoAppender(final String name, final Layout<?> layout) {
        super(name, null, layout, true, Property.EMPTY_ARRAY);
    }

    @Override
    public void append(final LogEvent event) {
        if (Constants.ENABLE_DIRECT_ENCODERS) {
            getLayout().encode(event, this);
            drain(byteBuffer);
        } else {
            final byte[] binary = getLayout().toByteArray(event);
            consume(binary, 0, binary.length);
        }
    }

    @Override
    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    @Override
    public ByteBuffer drain(final ByteBuffer buf) {
        buf.flip();
        consume(buf.array(), buf.position(), buf.limit());
        buf.clear();
        return buf;
    }

    @Override
    public void writeBytes(final ByteBuffer data) {
        ByteBufferDestinationHelper.writeToUnsynchronized(data, this);
    }

    @Override
    public void writeBytes(final byte[] data, final int offset, final int length) {
        ByteBufferDestinationHelper.writeToUnsynchronized(data, offset, length, this);
    }

    private void consume(final byte[] data, final int offset, final int length) {
        // need to do something with the result or the JVM may optimize everything away
        long sum = 0;
        for (int i = offset; i < length; i++) {
            sum += data[i];
        }
        checksum += sum;
    }
}
