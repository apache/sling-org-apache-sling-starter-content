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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrincipalPrivilegeTest {

    private PrincipalPrivilege pp;

    @BeforeEach
    void beforeEach() {
        pp = new PrincipalPrivilege(() -> "testUser1");
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.PrincipalPrivilege#getName()}.
     */
    @Test
    void testGetName() {
        assertEquals("testUser1", pp.getName());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.PrincipalPrivilege#getPrivilegesDisplayName()}.
     */
    @Test
    void testGetPrivilegesDisplayName() {
        assertEquals("", pp.getPrivilegesDisplayName());
        pp.setAllow(true);
        assertEquals("allow", pp.getPrivilegesDisplayName());
        pp.setDeny(true);
        assertEquals("allow / deny", pp.getPrivilegesDisplayName());
        pp.setAllow(false);
        assertEquals("deny", pp.getPrivilegesDisplayName());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.PrincipalPrivilege#isAllow()}.
     */
    @Test
    void testIsAllow() {
        assertFalse(pp.isAllow());
        pp.setAllow(true);
        assertTrue(pp.isAllow());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.PrincipalPrivilege#setAllow(boolean)}.
     */
    @Test
    void testSetAllow() {
        pp.setAllow(true);
        assertTrue(pp.isAllow());
        pp.setAllow(false);
        assertFalse(pp.isAllow());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.PrincipalPrivilege#isDeny()}.
     */
    @Test
    void testIsDeny() {
        assertFalse(pp.isDeny());
        pp.setDeny(true);
        assertTrue(pp.isDeny());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.PrincipalPrivilege#setDeny(boolean)}.
     */
    @Test
    void testSetDeny() {
        pp.setDeny(true);
        assertTrue(pp.isDeny());
        pp.setDeny(false);
        assertFalse(pp.isDeny());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.PrincipalPrivilege#getIsGroup()}.
     */
    @Test
    void testGetIsGroup() {
        assertFalse(pp.getIsGroup());

        PrincipalPrivilege gp = new PrincipalPrivilege(new TestGroupPrincipal("testGroup1"));
        assertTrue(gp.getIsGroup());
    }
}
