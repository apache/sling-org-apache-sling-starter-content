/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.starter.access.models;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import java.util.Arrays;
import java.util.Collections;

import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.RestrictionDefinition;
import org.apache.jackrabbit.value.ValueFactoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestrictionItemTest {

    private RestrictionItem itemNamesRi;

    protected RestrictionItem createRestrictionItem(String name, Type<?> type, Object value) {
        return createRestrictionItem(name, type, value, false);
    }

    protected RestrictionItem createRestrictionItem(String name, Type<?> type, Object value, boolean isManditory) {
        RestrictionDefinition globRd = Mockito.mock(RestrictionDefinition.class);
        Mockito.when(globRd.getName()).thenReturn(name);
        Mockito.when(globRd.getRequiredType()).thenAnswer(new Answer<Type<?>>() {
            @Override
            public Type<?> answer(InvocationOnMock invocation) throws Throwable {
                return type;
            }
        });
        if (isManditory) {
            Mockito.when(globRd.isMandatory()).thenReturn(isManditory);
        }
        return new RestrictionItem(globRd, value, false);
    }

    @BeforeEach
    void beforeEach() {
        itemNamesRi = createRestrictionItem(
                AccessControlConstants.REP_ITEM_NAMES, Type.STRINGS, new String[] {"name1", "name2"});
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.RestrictionItem#getName()}.
     */
    @Test
    void testGetName() {
        assertEquals(AccessControlConstants.REP_ITEM_NAMES, itemNamesRi.getName());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.RestrictionItem#isMultiValue()}.
     */
    @Test
    void testIsMultiValue() {
        assertTrue(itemNamesRi.isMultiValue());

        // also a non-multivalue one
        RestrictionItem globRi = createRestrictionItem(AccessControlConstants.REP_GLOB, Type.STRING, "glob1");
        assertFalse(globRi.isMultiValue());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.RestrictionItem#getValue()}.
     */
    @Test
    void testGetValue() throws RepositoryException {
        assertEquals("name1", itemNamesRi.getValue());

        // also a non-multivalue one
        RestrictionItem globRi = createRestrictionItem(AccessControlConstants.REP_GLOB, Type.STRING, "glob1");
        assertEquals("glob1", globRi.getValue());

        // also a Value instead of String
        ValueFactory vf = ValueFactoryImpl.getInstance();
        globRi = createRestrictionItem(AccessControlConstants.REP_GLOB, Type.STRING, vf.createValue("glob1"));
        assertEquals("glob1", globRi.getValue());

        // also a Value[] instead of String[]
        itemNamesRi = createRestrictionItem(AccessControlConstants.REP_ITEM_NAMES, Type.STRINGS, new Value[] {
            vf.createValue("name1"), vf.createValue("name2")
        });
        assertEquals("name1", itemNamesRi.getValue());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.RestrictionItem#getValues()}.
     */
    @Test
    void testGetValues() throws RepositoryException {
        assertEquals(Arrays.asList("name1", "name2"), itemNamesRi.getValues());

        // also a non-multivalue one
        RestrictionItem globRi = createRestrictionItem(AccessControlConstants.REP_GLOB, Type.STRING, "glob1");
        assertEquals(Collections.singletonList("glob1"), globRi.getValues());

        // also a Value instead of String
        ValueFactory vf = ValueFactoryImpl.getInstance();
        globRi = createRestrictionItem(AccessControlConstants.REP_GLOB, Type.STRING, vf.createValue("glob1"));
        assertEquals(Collections.singletonList("glob1"), globRi.getValues());

        // also a Value[] instead of String[]
        itemNamesRi = createRestrictionItem(AccessControlConstants.REP_ITEM_NAMES, Type.STRINGS, new Value[] {
            vf.createValue("name1"), vf.createValue("name2")
        });
        assertEquals(Arrays.asList("name1", "name2"), itemNamesRi.getValues());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.RestrictionItem#isExists()}.
     */
    @Test
    void testIsExists() {
        assertFalse(itemNamesRi.isExists());
        itemNamesRi.setExists(true);
        assertTrue(itemNamesRi.isExists());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.RestrictionItem#setExists(boolean)}.
     */
    @Test
    void testSetExists() {
        itemNamesRi.setExists(true);
        assertTrue(itemNamesRi.isExists());
        itemNamesRi.setExists(false);
        assertFalse(itemNamesRi.isExists());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.RestrictionItem#isMandatory()}.
     */
    @Test
    void testIsMandatory() {
        assertFalse(itemNamesRi.isMandatory());

        // also a mandatory one
        RestrictionItem globRi = createRestrictionItem(AccessControlConstants.REP_GLOB, Type.STRING, "glob1", true);
        assertTrue(globRi.isMandatory());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.RestrictionItem#toString()}.
     */
    @Test
    void testToString() {
        assertNotNull(itemNamesRi.toString());
    }
}
