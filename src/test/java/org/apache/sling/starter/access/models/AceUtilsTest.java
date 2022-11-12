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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AceUtilsTest {

    /**
     * Test method for {@link org.apache.sling.starter.access.models.AceUtils#getPrivilegeLongestPathMap(javax.jcr.Session)}.
     */
    @Test
    void testGetPrivilegeLongestPathMap() throws RepositoryException {
        Session session = Mockito.mock(Session.class);
        AccessControlManager acm = Mockito.mock(AccessControlManager.class);
        Mockito.when(session.getAccessControlManager()).thenReturn(acm);

        Privilege repReadNodes = createMockPrivilege(PrivilegeConstants.REP_READ_NODES, new Privilege[0]);
        Privilege repReadProperties = createMockPrivilege(PrivilegeConstants.REP_READ_PROPERTIES, new Privilege[0]);
        Privilege jcrRead = createMockPrivilege(PrivilegeConstants.JCR_READ, new Privilege[] {
                repReadNodes,
                repReadProperties
        });
        Privilege jcrReadAccessControl = createMockPrivilege(PrivilegeConstants.JCR_READ_ACCESS_CONTROL, new Privilege[0]);

        Privilege all = createMockPrivilege(PrivilegeConstants.JCR_ALL, new Privilege[] {
                jcrRead,
                jcrReadAccessControl
        });
        Mockito.when(acm.privilegeFromName(PrivilegeConstants.JCR_ALL)).thenReturn(all);

        Map<Privilege, String> privilegeLongestPathMap = AceUtils.getPrivilegeLongestPathMap(session);
        assertNotNull(privilegeLongestPathMap);
        assertEquals(PrivilegeConstants.JCR_ALL, privilegeLongestPathMap.get(all));
        assertEquals(String.format("%s/%s", PrivilegeConstants.JCR_ALL, PrivilegeConstants.JCR_READ), 
                privilegeLongestPathMap.get(jcrRead));
        assertEquals(String.format("%s/%s/%s", PrivilegeConstants.JCR_ALL, PrivilegeConstants.JCR_READ, PrivilegeConstants.REP_READ_NODES), 
                privilegeLongestPathMap.get(repReadNodes));
        assertEquals(String.format("%s/%s/%s", PrivilegeConstants.JCR_ALL, PrivilegeConstants.JCR_READ, PrivilegeConstants.REP_READ_PROPERTIES), 
                privilegeLongestPathMap.get(repReadProperties));
        assertEquals(String.format("%s/%s", PrivilegeConstants.JCR_ALL, PrivilegeConstants.JCR_READ_ACCESS_CONTROL), 
                privilegeLongestPathMap.get(jcrReadAccessControl));
    }

    protected static Privilege createMockPrivilege(String name, Privilege[] dap) {
        Privilege p = Mockito.mock(Privilege.class);
        Mockito.when(p.getName()).thenReturn(name);
        Mockito.when(p.getDeclaredAggregatePrivileges()).thenReturn(dap);
        Mockito.when(p.getAggregatePrivileges()).thenReturn(dap);
        if (dap != null && dap.length > 0) {
            Mockito.when(p.isAggregate()).thenReturn(true);
        }
        return p;
    }

}
