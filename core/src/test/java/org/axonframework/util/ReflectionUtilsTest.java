/*
 * Copyright (c) 2011. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.util;

import org.junit.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Allard Buijze
 * @since 0.7
 */
public class ReflectionUtilsTest {

    @Test
    public void testFindFieldsInClass() {
        Iterable<Field> actualFields = ReflectionUtils.fieldsOf(SomeSubType.class);
        int t = 0;
        for (Field actual : actualFields) {
            switch (t++) {
                case 0:
                    assertEquals("field3", actual.getName());
                    break;
                case 1:
                case 2:
                    assertTrue("Expected either field1 or field2, but got " + actual.getName(),
                               "field1".equals(actual.getName())
                                       || "field2".equals(actual.getName()));
                    break;
            }
        }
    }

    @Test
    public void testFindMethodsInClass() {
        Iterable<Method> actualMethods = ReflectionUtils.methodsOf(SomeSubType.class);
        int t = 0;
        for (Method actual : actualMethods) {
            switch (t++) {
                case 0:
                    assertEquals("getField3", actual.getName());
                    break;
                case 1:
                case 2:
                    assertTrue("Expected either getField1 or getField2, but got " + actual.getName(),
                               "getField1".equals(actual.getName())
                                       || "getField2".equals(actual.getName()));
                    break;
            }
        }
    }

    @Test
    public void testGetFieldValue() throws NoSuchFieldException {
        Object value = ReflectionUtils.getFieldValue(SomeType.class.getDeclaredField("field1"), new SomeSubType());
        assertEquals("field1", value);
    }

    @Test
    public void testIsAccessible() throws NoSuchFieldException {
        Field field1 = SomeType.class.getDeclaredField("field1");
        Field field2 = SomeType.class.getDeclaredField("field2");
        Field field3 = SomeSubType.class.getDeclaredField("field3");
        assertFalse(ReflectionUtils.isAccessible(field1));
        assertFalse(ReflectionUtils.isAccessible(field2));
        assertTrue(ReflectionUtils.isAccessible(field3));
    }

    @Test
    public void testfindFieldValuesOfType_Nulls() {
        ContainsCollectionsType item = new ContainsCollectionsType(null, null, null);
        assertEquals(2, ReflectionUtils.findFieldValuesOfType(item, String.class).size());
    }

    @Test
    public void testfindFieldValuesOfType_WithCollections() {
        ContainsCollectionsType item = new ContainsCollectionsType(Arrays.asList("one"),
                                                                   Collections.singletonMap("two",
                                                                                            "three"),
                                                                   Collections.singleton("four"));
        assertEquals(6, ReflectionUtils.findFieldValuesOfType(item, String.class).size());
    }

    private static class SomeType {

        private String field1 = "field1";
        private String field2 = "field2";

        public String getField1() {
            return field1;
        }

        public String getField2() {
            return field2;
        }
    }

    public static class SomeSubType extends SomeType {

        public int field3 = 3;

        public int getField3() {
            return field3;
        }
    }

    public static class ContainsCollectionsType extends SomeType {

        private List<String> listOfStrings;
        private Map<String, String> mapOfStringToString;
        private Set<String> setOfStrings;

        public ContainsCollectionsType(List<String> listOfStrings, Map<String, String> mapOfStringToString,
                                       Set<String> setOfStrings) {
            this.listOfStrings = listOfStrings;
            this.mapOfStringToString = mapOfStringToString;
            this.setOfStrings = setOfStrings;
        }

        public List<String> getListOfStrings() {
            return listOfStrings;
        }

        public Map<String, String> getMapOfStringToString() {
            return mapOfStringToString;
        }

        public Set<String> getSetOfStrings() {
            return setOfStrings;
        }
    }

}
