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
package org.apache.logging.converter.config.internal;

import java.util.regex.Pattern;
import org.apache.logging.converter.config.ConfigurationConverterException;
import org.apache.logging.log4j.util.Strings;

public final class StringUtils {

    private static final Pattern SUBSTITUTION_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    public static final String FALSE = "false";
    public static final String TRUE = "true";

    public static String capitalize(String value) {
        if (Strings.isEmpty(value) || Character.isUpperCase(value.charAt(0))) {
            return value;
        }
        final char[] chars = value.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }

    public static String decapitalize(String value) {
        if (Strings.isEmpty(value) || Character.isLowerCase(value.charAt(0))) {
            return value;
        }
        final char[] chars = value.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    public static boolean parseBoolean(String value) {
        return Boolean.parseBoolean(value.trim());
    }

    public static int parseInteger(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new ConfigurationConverterException("Invalid integer value: " + value, e);
        }
    }

    public static long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new ConfigurationConverterException("Invalid long value: " + value, e);
        }
    }

    public static String convertPropertySubstitution(String value) {
        return SUBSTITUTION_PATTERN.matcher(value).replaceAll("${sys:$1}");
    }

    private StringUtils() {}
}
