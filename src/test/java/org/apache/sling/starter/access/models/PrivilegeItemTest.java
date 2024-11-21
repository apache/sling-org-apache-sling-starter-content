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

import java.util.Collections;
import java.util.List;

import org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.RestrictionDefinition;
import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrivilegeItemTest {
    private PrivilegeItem jcrReadPi;
    private PrivilegeItem jcrReadNodesPiWithRestrictions;

    protected RestrictionItem createSingleValueRestrictionItem(String name, Object value, boolean exists) {
        RestrictionDefinition rd = Mockito.mock(RestrictionDefinition.class);
        Mockito.when(rd.getName()).thenReturn(name);
        return new RestrictionItem(rd, value, exists);
    }

    @BeforeEach
    void beforeEach() {
        jcrReadPi = new PrivilegeItem(
                PrivilegeConstants.JCR_READ,
                false,
                false,
                String.format("%s/%s", PrivilegeConstants.JCR_ALL, PrivilegeConstants.JCR_READ));

        jcrReadNodesPiWithRestrictions = new PrivilegeItem(
                PrivilegeConstants.REP_READ_NODES,
                false,
                false,
                String.format(
                        "%s/%s/%s",
                        PrivilegeConstants.JCR_ALL, PrivilegeConstants.JCR_READ, PrivilegeConstants.REP_READ_NODES),
                // allowRestrictions
                Collections.singletonList(
                        createSingleValueRestrictionItem(AccessControlConstants.REP_GLOB, "glob1", true)),
                // denyRestrictions
                Collections.singletonList(createSingleValueRestrictionItem(
                        AccessControlConstants.REP_ITEM_NAMES, new String[] {"name1", "name2"}, true)));
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.PrivilegeItem#getName()}.
     */
    @Test
    void testGetName() {
        assertEquals(PrivilegeConstants.JCR_READ, jcrReadPi.getName());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.PrivilegeItem#getNone()}.
     */
    @Test
    void testGetNone() {
        assertTrue(jcrReadPi.getNone());
        jcrReadPi.setGranted(true);
        assertFalse(jcrReadPi.getNone());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.PrivilegeItem#getGranted()}.
     */
    @Test
    void testGetGranted() {
        assertFalse(jcrReadPi.getGranted());
        jcrReadPi.setGranted(true);
        assertTrue(jcrReadPi.getGranted());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.PrivilegeItem#getDenied()}.
     */
    @Test
    void testGetDenied() {
        assertFalse(jcrReadPi.getDenied());
        jcrReadPi.setDenied(true);
        assertTrue(jcrReadPi.getDenied());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.PrivilegeItem#setGranted(boolean)}.
     */
    @Test
    void testSetGranted() {
        jcrReadPi.setGranted(true);
        assertTrue(jcrReadPi.getGranted());
        jcrReadPi.setGranted(false);
        assertFalse(jcrReadPi.getGranted());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.PrivilegeItem#setDenied(boolean)}.
     */
    @Test
    void testSetDenied() {
        jcrReadPi.setDenied(true);
        assertTrue(jcrReadPi.getDenied());
        jcrReadPi.setDenied(false);
        assertFalse(jcrReadPi.getDenied());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.PrivilegeItem#isAllowExists()}.
     */
    @Test
    void testIsAllowExists() {
        assertFalse(jcrReadPi.isAllowExists());
        jcrReadPi.setAllowExists(true);
        assertTrue(jcrReadPi.isAllowExists());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.PrivilegeItem#setAllowExists(boolean)}.
     */
    @Test
    void testSetAllowExists() {
        jcrReadPi.setAllowExists(true);
        assertTrue(jcrReadPi.isAllowExists());
        jcrReadPi.setAllowExists(false);
        assertFalse(jcrReadPi.isAllowExists());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.PrivilegeItem#isDenyExists()}.
     */
    @Test
    void testIsDenyExists() {
        assertFalse(jcrReadPi.isDenyExists());
        jcrReadPi.setDenyExists(true);
        assertTrue(jcrReadPi.isDenyExists());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.PrivilegeItem#setDenyExists(boolean)}.
     */
    @Test
    void testSetDenyExists() {
        jcrReadPi.setDenyExists(true);
        assertTrue(jcrReadPi.isDenyExists());
        jcrReadPi.setDenyExists(false);
        assertFalse(jcrReadPi.isDenyExists());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.PrivilegeItem#getLongestPath()}.
     */
    @Test
    void testGetLongestPath() {
        assertEquals(
                String.format("%s/%s", PrivilegeConstants.JCR_ALL, PrivilegeConstants.JCR_READ),
                jcrReadPi.getLongestPath());
        assertEquals(
                String.format(
                        "%s/%s/%s",
                        PrivilegeConstants.JCR_ALL, PrivilegeConstants.JCR_READ, PrivilegeConstants.REP_READ_NODES),
                jcrReadNodesPiWithRestrictions.getLongestPath());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.PrivilegeItem#addExtraCssClass(java.lang.String)}.
     */
    @Test
    void testAddExtraCssClass() {
        assertEquals("depth1", jcrReadPi.getDepthCss());
        jcrReadPi.addExtraCssClass("extra1");
        assertEquals("depth1 extra1", jcrReadPi.getDepthCss());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.PrivilegeItem#getDepthCss()}.
     */
    @Test
    void testGetDepthCss() {
        assertEquals("depth1", jcrReadPi.getDepthCss());
        assertEquals("depth2", jcrReadNodesPiWithRestrictions.getDepthCss());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.PrivilegeItem#getDepth()}.
     */
    @Test
    void testGetDepth() {
        assertEquals(1, jcrReadPi.getDepth());
        assertEquals(2, jcrReadNodesPiWithRestrictions.getDepth());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.PrivilegeItem#getAllowRestrictions()}.
     */
    @Test
    void testGetAllowRestrictions() {
        assertTrue(jcrReadPi.getAllowRestrictions().isEmpty());

        List<RestrictionItem> allowRestrictions = jcrReadNodesPiWithRestrictions.getAllowRestrictions();
        assertEquals(1, allowRestrictions.size());
        assertEquals(AccessControlConstants.REP_GLOB, allowRestrictions.get(0).getName());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.PrivilegeItem#getDenyRestrictions()}.
     */
    @Test
    void testGetDenyRestrictions() {
        assertTrue(jcrReadPi.getDenyRestrictions().isEmpty());

        List<RestrictionItem> denyRestrictions = jcrReadNodesPiWithRestrictions.getDenyRestrictions();
        assertEquals(1, denyRestrictions.size());
        assertEquals(
                AccessControlConstants.REP_ITEM_NAMES, denyRestrictions.get(0).getName());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.PrivilegeItem#setAllowRestrictions(java.util.List)}.
     */
    @Test
    void testSetAllowRestrictions() {
        assertEquals(Collections.emptyList(), jcrReadPi.getAllowRestrictions());
        List<RestrictionItem> newRestrictions = Collections.singletonList(
                createSingleValueRestrictionItem(AccessControlConstants.REP_GLOB, "glob1", false));
        jcrReadPi.setAllowRestrictions(newRestrictions);
        assertEquals(newRestrictions, jcrReadPi.getAllowRestrictions());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.PrivilegeItem#setDenyRestrictions(java.util.List)}.
     */
    @Test
    void testSetDenyRestrictions() {
        assertEquals(Collections.emptyList(), jcrReadPi.getDenyRestrictions());
        List<RestrictionItem> newRestrictions = Collections.singletonList(
                createSingleValueRestrictionItem(AccessControlConstants.REP_GLOB, "glob1", false));
        jcrReadPi.setDenyRestrictions(newRestrictions);
        assertEquals(newRestrictions, jcrReadPi.getDenyRestrictions());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.PrivilegeItem#toString()}.
     */
    @Test
    void testToString() {
        assertNotNull(jcrReadPi.toString());
        assertNotNull(jcrReadNodesPiWithRestrictions.toString());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.PrivilegeItem#addAllowRestrictionToDelete(java.lang.String)}.
     */
    @Test
    void testAddAllowRestrictionToDelete() {
        jcrReadPi.addAllowRestrictionToDelete(AccessControlConstants.REP_ITEM_NAMES);
        assertEquals(
                Collections.singleton(AccessControlConstants.REP_ITEM_NAMES), jcrReadPi.getAllowRestrictionsToDelete());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.PrivilegeItem#getAllowRestrictionsToDelete()}.
     */
    @Test
    void testGetAllowRestrictionsToDelete() {
        assertEquals(Collections.emptySet(), jcrReadPi.getAllowRestrictionsToDelete());
        jcrReadPi.addAllowRestrictionToDelete(AccessControlConstants.REP_GLOB);
        assertEquals(Collections.singleton(AccessControlConstants.REP_GLOB), jcrReadPi.getAllowRestrictionsToDelete());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.PrivilegeItem#addDenyRestrictionToDelete(java.lang.String)}.
     */
    @Test
    void testAddDenyRestrictionToDelete() {
        jcrReadPi.addDenyRestrictionToDelete(AccessControlConstants.REP_NT_NAMES);
        assertEquals(
                Collections.singleton(AccessControlConstants.REP_NT_NAMES), jcrReadPi.getDenyRestrictionsToDelete());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.PrivilegeItem#getDenyRestrictionsToDelete()}.
     */
    @Test
    void testGetDenyRestrictionsToDelete() {
        assertEquals(Collections.emptySet(), jcrReadPi.getDenyRestrictionsToDelete());
        jcrReadPi.addDenyRestrictionToDelete(AccessControlConstants.REP_PREFIXES);
        assertEquals(
                Collections.singleton(AccessControlConstants.REP_PREFIXES), jcrReadPi.getDenyRestrictionsToDelete());
    }
}
