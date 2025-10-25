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

import static org.apache.logging.log4j.weaver.Constants.ENTRY_MESSAGE_TYPE;
import static org.apache.logging.log4j.weaver.Constants.EXIT_MESSAGE_TYPE;
import static org.apache.logging.log4j.weaver.Constants.FLOW_MESSAGE_FACTORY_TYPE;
import static org.apache.logging.log4j.weaver.Constants.LOGGER_TYPE;
import static org.apache.logging.log4j.weaver.Constants.MESSAGE_TYPE;
import static org.apache.logging.log4j.weaver.Constants.OBJECT_ARRAY_TYPE;
import static org.apache.logging.log4j.weaver.Constants.OBJECT_TYPE;
import static org.apache.logging.log4j.weaver.Constants.STACK_TRACE_ELEMENT_ARRAY_TYPE;
import static org.apache.logging.log4j.weaver.Constants.STACK_TRACE_ELEMENT_TYPE;
import static org.apache.logging.log4j.weaver.Constants.STRING_TYPE;
import static org.apache.logging.log4j.weaver.Constants.SUPPLIER_ARRAY_TYPE;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

public class LocationCacheGenerator {

    private static final Type LAMBDA_UTIL_TYPE = Type.getObjectType("org/apache/logging/log4j/util/LambdaUtil");
    private static final Type STRING_FORMATTER_MESSAGE_FACTORY_TYPE =
            Type.getObjectType("org/apache/logging/log4j/message/StringFormatterMessageFactory");
    private static final String LOCATION_FIELD = "locations";

    private final Map<String, LocationCacheContents> locationCacheClasses = new ConcurrentHashMap<>();

    public LocationCacheValue addLocation(
            final String internalClassName, final String methodName, final String fileName, final int lineNumber) {
        final String cacheClassName = getCacheClassName(internalClassName);
        final LocationCacheContents contents =
                locationCacheClasses.computeIfAbsent(cacheClassName, k -> new LocationCacheContents());
        final int index = contents.addLocation(internalClassName, methodName, fileName, lineNumber);
        return new LocationCacheValue(cacheClassName, LOCATION_FIELD, index);
    }

    public Handle createLambda(String internalClassName, SupplierLambdaType type) {
        final String cacheClassName = getCacheClassName(internalClassName);
        final LocationCacheContents contents =
                locationCacheClasses.computeIfAbsent(cacheClassName, k -> new LocationCacheContents());
        contents.addLambda(type);
        final String methodName = type.name().toLowerCase(Locale.US);
        final String methodDescriptor = Type.getMethodDescriptor(MESSAGE_TYPE, type.getArgumentTypes());
        switch (type) {
            case FORMATTED_MESSAGE:
            case ENTRY_MESSAGE_MESSAGE:
            case ENTRY_MESSAGE_STRING_OBJECTS:
            case ENTRY_MESSAGE_STRING_SUPPLIERS:
            case EXIT_MESSAGE_ENTRY_MESSAGE:
            case EXIT_MESSAGE_MESSAGE:
            case EXIT_MESSAGE_OBJECT_ENTRY_MESSAGE:
            case EXIT_MESSAGE_OBJECT_MESSAGE:
            case EXIT_MESSAGE_STRING_OBJECT:
                return new Handle(Opcodes.H_INVOKESTATIC, cacheClassName, methodName, methodDescriptor, false);
            default:
                throw new IllegalArgumentException();
        }
    }

    public Map<String, byte[]> generateClasses() {
        return locationCacheClasses.entrySet().parallelStream()
                .collect(Collectors.toMap(Entry::getKey, e -> generateCacheClass(e.getKey(), e.getValue())));
    }

    private static byte[] generateCacheClass(final String innerClassName, final LocationCacheContents contents) {
        final ClassWriter cv = new ClassWriter(0);
        cv.visit(Opcodes.V1_8, 0, innerClassName, null, OBJECT_TYPE.getInternalName(), null);
        // Write locations field
        final List<StackTraceElement> locations = contents.getLocations();
        writeLocations(innerClassName, cv, locations);
        // We add lambdas to this class
        final Set<SupplierLambdaType> lambdas = contents.getLambdas();
        for (final SupplierLambdaType type : lambdas) {
            final InstructionAdapter mv = new InstructionAdapter(cv.visitMethod(
                    Opcodes.ACC_STATIC,
                    type.name().toLowerCase(Locale.US),
                    type.getImplementationMethodDescriptor(),
                    null,
                    null));
            switch (type) {
                case FORMATTED_MESSAGE:
                    writeFormattedMessage(mv);
                    break;
                case ENTRY_MESSAGE_MESSAGE:
                case ENTRY_MESSAGE_STRING_OBJECTS:
                case EXIT_MESSAGE_ENTRY_MESSAGE:
                case EXIT_MESSAGE_MESSAGE:
                case EXIT_MESSAGE_OBJECT_ENTRY_MESSAGE:
                case EXIT_MESSAGE_OBJECT_MESSAGE:
                case EXIT_MESSAGE_STRING_OBJECT:
                    writeEntryExitMessage(mv, type);
                    break;
                case ENTRY_MESSAGE_STRING_SUPPLIERS:
                    writeEntryMessageSuppliers(mv);
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }
        cv.visitEnd();
        return cv.toByteArray();
    }

    private static void writeLocations(
            final String innerClassName, final ClassVisitor cv, final List<StackTraceElement> locations) {
        cv.visitField(Opcodes.ACC_STATIC, LOCATION_FIELD, STACK_TRACE_ELEMENT_ARRAY_TYPE.getInternalName(), null, null)
                .visitEnd();
        final InstructionAdapter mv =
                new InstructionAdapter(cv.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null));
        mv.visitCode();
        mv.visitMaxs(9, 0);
        mv.iconst(locations.size());
        mv.newarray(STACK_TRACE_ELEMENT_TYPE);
        for (int i = 0; i < locations.size(); i++) {
            final StackTraceElement location = locations.get(i);
            mv.dup();
            mv.iconst(i);
            mv.anew(STACK_TRACE_ELEMENT_TYPE);
            mv.dup();
            mv.aconst(location.getClassName());
            mv.aconst(location.getMethodName());
            mv.aconst(location.getFileName());
            mv.iconst(location.getLineNumber());
            mv.invokespecial(
                    STACK_TRACE_ELEMENT_TYPE.getInternalName(),
                    "<init>",
                    Type.getMethodDescriptor(Type.VOID_TYPE, STRING_TYPE, STRING_TYPE, STRING_TYPE, Type.INT_TYPE),
                    false);
            mv.visitInsn(Opcodes.AASTORE);
        }
        mv.putstatic(innerClassName, LOCATION_FIELD, STACK_TRACE_ELEMENT_ARRAY_TYPE.getInternalName());
        mv.areturn(Type.VOID_TYPE);
        mv.visitEnd();
    }

    private static void writeFormattedMessage(final InstructionAdapter mv) {
        mv.visitCode();
        mv.visitMaxs(3, 2);
        mv.getstatic(
                STRING_FORMATTER_MESSAGE_FACTORY_TYPE.getInternalName(),
                "INSTANCE",
                STRING_FORMATTER_MESSAGE_FACTORY_TYPE.getDescriptor());
        mv.load(0, STRING_TYPE);
        mv.load(1, OBJECT_ARRAY_TYPE);
        mv.invokevirtual(
                STRING_FORMATTER_MESSAGE_FACTORY_TYPE.getInternalName(),
                "newMessage",
                Type.getMethodType(MESSAGE_TYPE, STRING_TYPE, OBJECT_ARRAY_TYPE).getDescriptor(),
                false);
        mv.areturn(MESSAGE_TYPE);
        mv.visitEnd();
    }

    private static void writeEntryExitMessage(final InstructionAdapter mv, final SupplierLambdaType type) {
        final Type[] args = type.getArgumentTypes();
        mv.visitCode();
        mv.visitMaxs(args.length, args.length);
        mv.load(0, LOGGER_TYPE);
        mv.invokeinterface(
                LOGGER_TYPE.getInternalName(),
                "getFlowMessageFactory",
                Type.getMethodDescriptor(FLOW_MESSAGE_FACTORY_TYPE));
        for (int i = 1; i < args.length; i++) {
            mv.load(i, args[i]);
        }
        final boolean isEntry = type.name().startsWith("ENTRY");
        final String methodName = isEntry ? "newEntryMessage" : "newExitMessage";
        mv.invokeinterface(
                FLOW_MESSAGE_FACTORY_TYPE.getInternalName(),
                methodName,
                Type.getMethodDescriptor(
                        isEntry ? ENTRY_MESSAGE_TYPE : EXIT_MESSAGE_TYPE, Arrays.copyOfRange(args, 1, args.length)));
        mv.areturn(MESSAGE_TYPE);
        mv.visitEnd();
    }

    private static void writeEntryMessageSuppliers(final InstructionAdapter mv) {
        mv.visitCode();
        mv.visitMaxs(3, 3);
        mv.load(0, LOGGER_TYPE);
        mv.invokeinterface(
                LOGGER_TYPE.getInternalName(),
                "getFlowMessageFactory",
                Type.getMethodDescriptor(FLOW_MESSAGE_FACTORY_TYPE));
        mv.load(1, STRING_TYPE);
        mv.load(2, SUPPLIER_ARRAY_TYPE);
        mv.invokestatic(
                LAMBDA_UTIL_TYPE.getInternalName(),
                "getAll",
                Type.getMethodDescriptor(OBJECT_ARRAY_TYPE, SUPPLIER_ARRAY_TYPE),
                false);
        mv.invokeinterface(
                FLOW_MESSAGE_FACTORY_TYPE.getInternalName(),
                "newEntryMessage",
                Type.getMethodDescriptor(ENTRY_MESSAGE_TYPE, STRING_TYPE, OBJECT_ARRAY_TYPE));
        mv.areturn(MESSAGE_TYPE);
        mv.visitEnd();
    }

    private static String getCacheClassName(final String internalClassName) {
        return StringUtils.substringBefore(internalClassName, '$') + Constants.LOCATION_CACHE_SUFFIX;
    }

    public static Path getCacheClassFile(final Path classFile) {
        final Path fileName = classFile.getFileName();
        if (fileName == null) {
            throw new IllegalArgumentException("The 'classFile' parameter is an empty path.");
        }
        final String cacheFileName =
                LocationCacheGenerator.getCacheClassName(StringUtils.removeEnd(fileName.toString(), ".class"))
                        + ".class";
        return classFile.resolveSibling(cacheFileName);
    }

    public static class LocationCacheValue {
        private final String internalClassName;
        private final String fieldName;
        private final int index;

        private LocationCacheValue(String internalClassName, String fieldName, int index) {
            super();
            this.internalClassName = internalClassName;
            this.fieldName = fieldName;
            this.index = index;
        }

        public String getInternalClassName() {
            return internalClassName;
        }

        public Type getType() {
            return Type.getObjectType(internalClassName);
        }

        public String getFieldName() {
            return fieldName;
        }

        public int getIndex() {
            return index;
        }
    }

    /**
     * Describes the methods and fields of a specific location cache class.
     *
     */
    private static class LocationCacheContents {
        private final List<StackTraceElement> locations = new CopyOnWriteArrayList<>();
        private final Set<SupplierLambdaType> lambdas = EnumSet.noneOf(SupplierLambdaType.class);

        public int addLocation(
                final String internalClassName, final String methodName, final String fileName, final int lineNumber) {
            final StackTraceElement location =
                    new StackTraceElement(internalClassName.replaceAll("/", "."), methodName, fileName, lineNumber);
            locations.add(location);
            return locations.indexOf(location);
        }

        public List<StackTraceElement> getLocations() {
            return locations;
        }

        public boolean addLambda(SupplierLambdaType type) {
            return lambdas.add(type);
        }

        public Set<SupplierLambdaType> getLambdas() {
            return lambdas;
        }
    }
}
