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

import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.sling.jcr.jackrabbit.accessmanager.GetAcl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AclTest extends AccessFormPageTest {
    static final String ACL_JSON = "{\n"
            + "  \"testUser1\":{\n"
            + "    \"principal\":\"testUser1\",\n"
            + "    \"order\":0,\n"
            + "    \"privileges\":{\n"
            + "      \"jcr:read\":{\n"
            + "        \"allow\":true\n"
            + "      },\n"
            + "      \"rep:write\":{\n"
            + "        \"allow\":true\n"
            + "      }\n"
            + "    }\n"
            + "  },\n"
            + "  \"testGroup1\":{\n"
            + "    \"principal\":\"testGroup1\",\n"
            + "    \"order\":0,\n"
            + "    \"privileges\":{\n"
            + "      \"jcr:read\":{\n"
            + "        \"allow\":true\n"
            + "      },\n"
            + "      \"rep:write\":{\n"
            + "        \"deny\":true\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";

    protected Session jcrSession;

    private Acl aclPage = null;

    @Override
    protected AccessFormPage createPageModel() {
        aclPage = new Acl();
        return aclPage;
    }

    @Override
    @BeforeEach
    void beforeEach() throws RepositoryException {
        super.beforeEach();

        aclPage.getAcl = Mockito.mock(GetAcl.class);
        jcrSession = Mockito.mock(JackrabbitSession.class);
        Mockito.when(aclPage.request.getResourceResolver().adaptTo(Session.class)).thenReturn(jcrSession);
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.Acl#getPrincipals()}.
     */
    @Test
    void testGetPrincipals() throws RepositoryException {
        PrincipalManager principalMgr = Mockito.mock(PrincipalManager.class);
        Mockito.when(((JackrabbitSession)jcrSession).getPrincipalManager()).thenReturn(principalMgr);
        Mockito.when(principalMgr.getPrincipal("testUser1")).thenReturn(() -> "testUser1");
        Mockito.when(principalMgr.getPrincipal("testGroup1")).thenReturn(new TestGroupPrincipal("testGroup1"));

        JsonObject aclJson;
        try (StringReader strReader = new StringReader(ACL_JSON);
                JsonReader jsonReader = Json.createReader(strReader)) {
            aclJson = jsonReader.readObject();
        }
        Mockito.when(aclPage.getAcl.getAcl(jcrSession, aclPage.resource.getPath()))
            .thenReturn(aclJson);

        Collection<PrincipalPrivilege> principals = aclPage.getPrincipals();
        assertNotNull(principals);
        assertEquals(2, principals.size());
        Map<String, PrincipalPrivilege> asMap = new HashMap<>();
        principals.stream().forEach(item -> asMap.put(item.getName(), item));

        PrincipalPrivilege candidateUser = asMap.get("testUser1");
        assertNotNull(candidateUser);
        assertEquals("testUser1", candidateUser.getName());
        assertTrue(candidateUser.isAllow());
        assertFalse(candidateUser.isDeny());
        assertFalse(candidateUser.getIsGroup());

        PrincipalPrivilege candidateGroup = asMap.get("testGroup1");
        assertNotNull(candidateGroup);
        assertEquals("testGroup1", candidateGroup.getName());
        assertTrue(candidateGroup.isAllow());
        assertTrue(candidateGroup.isDeny());
        assertTrue(candidateGroup.getIsGroup());
    }

}
