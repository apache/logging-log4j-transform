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
package org.apache.logging.log4j.weaver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.logging.log4j.weaver.log4j2.LogBuilderConversionHandler;
import org.apache.logging.log4j.weaver.log4j2.LoggerConversionHandler;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class LocationClassConverter {

    /**
     * Classloader to resolve a class hierarchy.
     */
    private final ClassLoader classpath;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2")
    public LocationClassConverter(ClassLoader classpath) {
        this.classpath = classpath;
    }

    /**
     * Adds location information to a classfile.
     *
     * @param src           original classfile
     * @param dest          transformed classfile
     * @param locationCache a container for location data
     */
    public void convert(InputStream src, OutputStream dest, LocationCacheGenerator locationCache) throws IOException {
        final ClassWriter writer = new PrivateClassWriter(ClassWriter.COMPUTE_FRAMES, classpath);

        final LocationClassVisitor converter = new LocationClassVisitor(writer, locationCache);
        converter.addClassConversionHandler(new LoggerConversionHandler());
        converter.addClassConversionHandler(new LogBuilderConversionHandler());
        new ClassReader(src).accept(converter, ClassReader.EXPAND_FRAMES);

        dest.write(writer.toByteArray());
    }

    private static class PrivateClassWriter extends ClassWriter {

        private final ClassLoader classpath;

        public PrivateClassWriter(int flags, ClassLoader classpath) {
            super(flags);
            this.classpath = classpath;
        }

        @Override
        protected ClassLoader getClassLoader() {
            return classpath;
        }

    }
}
