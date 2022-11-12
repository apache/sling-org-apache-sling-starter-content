/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
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
package org.apache.sling.starter.access.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.RestrictionDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

class RestrictionItemTest {

    private RestrictionItem itemNamesRi;

    protected RestrictionItem createSingleValueRestrictionItem() {
        RestrictionDefinition globRd = Mockito.mock(RestrictionDefinition.class);
        Mockito.when(globRd.getName()).thenReturn(AccessControlConstants.REP_GLOB);
        Mockito.when(globRd.getRequiredType()).thenAnswer(new Answer<Type<?>>() {
            @Override
            public Type<?> answer(InvocationOnMock invocation) throws Throwable {
                return Type.STRING;
            }
        });
        RestrictionItem globRi = new RestrictionItem(globRd, "glob1", false);
        return globRi;
    }

    @BeforeEach
    void beforeEach() {
        RestrictionDefinition rd = Mockito.mock(RestrictionDefinition.class);
        Mockito.when(rd.getName()).thenReturn(AccessControlConstants.REP_ITEM_NAMES);
        Mockito.when(rd.getRequiredType()).thenAnswer(new Answer<Type<?>>() {
            @Override
            public Type<?> answer(InvocationOnMock invocation) throws Throwable {
                return Type.STRINGS;
            }
        });

        itemNamesRi = new RestrictionItem(rd, new String[] {"name1", "name2"}, false);
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
        RestrictionItem globRi = createSingleValueRestrictionItem();
        assertFalse(globRi.isMultiValue());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.RestrictionItem#getValue()}.
     */
    @Test
    void testGetValue() throws RepositoryException {
        assertEquals("name1", itemNamesRi.getValue());

        // also a non-multivalue one
        RestrictionItem globRi = createSingleValueRestrictionItem();
        assertEquals("glob1", globRi.getValue());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.RestrictionItem#getValues()}.
     */
    @Test
    void testGetValues() throws RepositoryException {
        assertEquals(Arrays.asList("name1", "name2"), itemNamesRi.getValues());

        // also a non-multivalue one
        RestrictionItem globRi = createSingleValueRestrictionItem();
        assertEquals(Collections.singletonList("glob1"), globRi.getValues());
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
        RestrictionDefinition mandatoryRd = Mockito.mock(RestrictionDefinition.class);
        Mockito.when(mandatoryRd.getName()).thenReturn("test:mandatory1");
        Mockito.when(mandatoryRd.isMandatory()).thenReturn(true);
        assertTrue(mandatoryRd.isMandatory());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.RestrictionItem#toString()}.
     */
    @Test
    void testToString() {
        assertNotNull(itemNamesRi.toString());
    }

}
