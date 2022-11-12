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

import static org.apache.sling.starter.access.models.AceUtilsTest.createMockPrivilege;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.api.security.authorization.PrivilegeManager;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.RestrictionDefinition;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.RestrictionDefinitionImpl;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.RestrictionProvider;
import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.request.builder.impl.RequestParameterImpl;
import org.apache.sling.api.request.builder.impl.RequestParameterMapImpl;
import org.apache.sling.jcr.jackrabbit.accessmanager.GetAce;
import org.apache.sling.jcr.jackrabbit.accessmanager.GetAcl;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AceTest extends AccessFormPageTest {
    private static final String ACE_JSON = "{\n"
            + "  \"principal\":\"testUser1\",\n"
            + "  \"order\":0,\n"
            + "  \"privileges\":{\n"
            + "    \"jcr:read\":{\n"
            + "      \"allow\":true\n"
            + "    },\n"
            + "    \"jcr:write\":{\n"
            + "      \"allow\":true\n"
            + "    }\n"
            + "  }\n"
            + "}";
    private static final String ACE_JSON_WITH_RESTRICTIONS = "{\n"
            + "  \"principal\":\"testUser1\",\n"
            + "  \"order\":0,\n"
            + "  \"privileges\":{\n"
            + "    \"jcr:read\":{\n"
            + "      \"allow\":{\n"
            + "        \"rep:ntNames\":[\n"
            + "          \"name1\",\n"
            + "          \"name2\"\n"
            + "        ]\n"
            + "      }\n"
            + "    },\n"
            + "    \"jcr:write\":{\n"
            + "      \"deny\":{\n"
            + "        \"rep:glob\":\"glob1\"\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";
    private static final String RESTRICTIONS_JSON = "{\n"
            + "        \"rep:ntNames\":[\n"
            + "          \"name1\",\n"
            + "          \"name2\"\n"
            + "        ],\n"
            + "        \"rep:glob\":\"glob1\"\n"
            + "      }";

    private Ace acePage = null;

    private RestrictionDefinition repGlobDef;
    private RestrictionDefinition repNtNamesDef;

    private Session jcrSession;

    private Privilege jcrRead;

    private Privilege jcrWrite;

    @Override
    protected AccessFormPage createPageModel() {
        acePage = new Ace();
        return acePage;
    }

    @Override
    @BeforeEach
    void beforeEach() throws RepositoryException {
        super.beforeEach();

        Mockito.when(acePage.request.getParameter("pid")).thenReturn("testUser1");

        acePage.getAcl = Mockito.mock(GetAcl.class);
        acePage.getAce = Mockito.mock(GetAce.class);

        jcrSession = Mockito.mock(JackrabbitSession.class);
        Mockito.when(acePage.request.getResourceResolver().adaptTo(Session.class)).thenReturn(jcrSession);

        mockPrivilegeLookup();
        mockAuthorizableLookup();
        mockRestrictionProvider(false);
        mockPrincipalManager();
    }

    protected void mockPrincipalManager() throws RepositoryException {
        // mock principalMgr
        PrincipalManager principalMgr = Mockito.mock(PrincipalManager.class);
        Mockito.when(((JackrabbitSession)jcrSession).getPrincipalManager()).thenReturn(principalMgr);
        Mockito.when(principalMgr.getPrincipal("testUser1")).thenReturn(() -> "testUser1");
        Mockito.when(principalMgr.getPrincipal("testGroup1")).thenReturn(new TestGroupPrincipal("testGroup1"));
    }

    protected void mockRestrictionProvider(boolean isManditory) {
        // mock the restriction provider calls
        RestrictionProvider restrictionProvider = Mockito.mock(RestrictionProvider.class);
        acePage.restrictionProviders = Collections.singletonList(restrictionProvider);
        repGlobDef = new RestrictionDefinitionImpl(AccessControlConstants.REP_GLOB, Type.STRING, isManditory);
        repNtNamesDef = new RestrictionDefinitionImpl(AccessControlConstants.REP_NT_NAMES, Type.STRINGS, isManditory);
        Mockito.when(restrictionProvider.getSupportedRestrictions(acePage.resource.getPath()))
            .thenReturn(new HashSet<>(Arrays.asList(repGlobDef, repNtNamesDef)));
    }

    protected void mockAuthorizableLookup() throws RepositoryException {
        // mock the authorizable lookup path
        UserManager userMgr = Mockito.mock(UserManager.class);
        Mockito.when(acePage.request.getResourceResolver().adaptTo(Session.class)).thenReturn(jcrSession);
        Mockito.when(((JackrabbitSession)jcrSession).getUserManager()).thenReturn(userMgr);
        User userAuthorizable = Mockito.mock(User.class);
        Mockito.when(userMgr.getAuthorizable("testUser1")).thenReturn(userAuthorizable);
    }

    protected void mockPrivilegeLookup() throws RepositoryException {
        // mock the privilege lookup
        AccessControlManager acm = Mockito.mock(AccessControlManager.class);
        Mockito.when(jcrSession.getAccessControlManager()).thenReturn(acm);

        Privilege repReadNodes = createMockPrivilege(PrivilegeConstants.REP_READ_NODES, new Privilege[0]);
        Privilege repReadProperties = createMockPrivilege(PrivilegeConstants.REP_READ_PROPERTIES, new Privilege[0]);
        jcrRead = createMockPrivilege(PrivilegeConstants.JCR_READ, new Privilege[] {
                repReadNodes,
                repReadProperties
        });
        jcrWrite = createMockPrivilege(PrivilegeConstants.JCR_WRITE, new Privilege[0]);

        Privilege all = createMockPrivilege(PrivilegeConstants.JCR_ALL, new Privilege[] {
                jcrRead,
                jcrWrite
        });
        Mockito.when(acm.privilegeFromName(PrivilegeConstants.JCR_ALL)).thenReturn(all);
        Mockito.when(acm.privilegeFromName(PrivilegeConstants.REP_READ_NODES)).thenReturn(repReadNodes);
        Mockito.when(acm.privilegeFromName(PrivilegeConstants.REP_READ_PROPERTIES)).thenReturn(repReadProperties);
        Mockito.when(acm.privilegeFromName(PrivilegeConstants.JCR_READ)).thenReturn(jcrRead);
        Mockito.when(acm.privilegeFromName(PrivilegeConstants.JCR_WRITE)).thenReturn(jcrWrite);

        Mockito.when(acm.getSupportedPrivileges(acePage.getAcePath())).thenReturn(new Privilege[] {
                all,
                jcrRead,
                repReadNodes,
                repReadNodes,
                jcrWrite
        });
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.Ace#init()}.
     */
    @Test
    void testInit() {
        super.testInit();
        assertEquals("testUser1", acePage.getPrincipalId());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.Ace#createPrivilegesPriorityMap()}.
     */
    @Test
    void testCreatePrivilegesPriorityMap() {
        assertNotNull(Ace.createPrivilegesPriorityMap());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.Ace#getPrincipalId()}.
     */
    @Test
    void testGetPrincipalId() {
        assertNull(acePage.getPrincipalId());
        super.testInit();
        assertEquals("testUser1", acePage.getPrincipalId());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.Ace#getIsInvalidPrincipal()}.
     * @throws RepositoryException 
     */
    @Test
    void testGetIsInvalidPrincipal() throws RepositoryException {
        assertTrue(acePage.getIsInvalidPrincipal());
        super.testInit();
        assertFalse(acePage.getIsInvalidPrincipal());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.Ace#getAcePath()}.
     */
    @Test
    void testGetAcePath() {
        assertEquals("/content/test1", acePage.getAcePath());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.Ace#isExists()}.
     * @throws RepositoryException 
     */
    @Test
    void testIsExists() throws RepositoryException {
        assertFalse(acePage.isExists());

        // exists calculation happens in here
        testGetPersistedPrivilegesMap();

        assertTrue(acePage.isExists());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.Ace#getPrivileges()}.
     * @throws RepositoryException 
     */
    @Test
    void testGetPrivileges() throws RepositoryException {
        mockPersistedPrivileges(ACE_JSON);

        Collection<PrivilegeItem> privileges = acePage.getPrivileges();
        assertNotNull(privileges);
        assertEquals(2, privileges.size());
        assertTrue(privileges.stream().anyMatch(p -> jcrRead.getName().equals(p.getName())));
        assertTrue(privileges.stream().anyMatch(p -> jcrWrite.getName().equals(p.getName())));
    }

    protected void mockPersistedPrivileges(String ace) throws RepositoryException {
        Assertions.assertDoesNotThrow(() -> page.init());

        JsonObject aceJson;
        try (StringReader strReader = new StringReader(ace);
                JsonReader jsonReader = Json.createReader(strReader)) {
            aceJson = jsonReader.readObject();
        }
        Mockito.when(acePage.getAce.getAce(jcrSession, acePage.resource.getPath(), "testUser1"))
            .thenReturn(aceJson);

        Map<String, String[]> rawReqParams = new HashMap<>();
        RequestParameterMap requestParameterMap = new RequestParameterMapImpl(rawReqParams);
        Mockito.when(acePage.request.getRequestParameterMap()).thenReturn(requestParameterMap);

        // clear this out from any previous call so it is calculated again
        acePage.persistedPrivilegesMap = null;
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.Ace#populateEntriesFromPreviousFailedPost(java.util.Map, java.util.Map, java.util.Map)}.
     */
    @Test
    void testPopulateEntriesFromPreviousFailedPost() {
        // mock the request parameters map
        Map<String, String[]> rawReqParams = new HashMap<>();
        rawReqParams.put("restriction@jcr:read@rep:glob@Allow", new String[] {"glob1"});
        rawReqParams.put("restriction@jcr:write@rep:ntNames@Deny", new String[] {"ntNames1", "ntNames2"});
        rawReqParams.put("key1", new String[] {"value2"});
        RequestParameterMap requestParameterMap = new RequestParameterMapImpl(rawReqParams);
        Mockito.when(acePage.request.getRequestParameterMap()).thenReturn(requestParameterMap);

        Map<String, List<RestrictionItem>> postedAllowRestrictionsMap = new HashMap<>(); 
        Map<String, List<RestrictionItem>> postedDenyRestrictionsMap = new HashMap<>(); 
        Map<String, String[]> entriesMap = acePage.populateEntriesFromPreviousFailedPost(postedAllowRestrictionsMap,
                postedDenyRestrictionsMap, acePage.toSrMap(acePage.getSupportedRestrictions()));
        assertNotNull(entriesMap);
        assertEquals(2, entriesMap.size());
        assertArrayEquals(new String[] {"glob1"}, entriesMap.get("restriction@jcr:read@rep:glob@Allow"));
        assertArrayEquals(new String[] {"ntNames1", "ntNames2"}, entriesMap.get("restriction@jcr:write@rep:ntNames@Deny"));
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.Ace#populateEntriesForMissingMandatoryRestrictions(java.util.List, java.util.Set)}.
     */
    @Test
    void testPopulateEntriesForMissingMandatoryRestrictions() {
        // test when no restrictions are mandatory
        List<RestrictionItem> newRestrictionsList = new ArrayList<>();
        acePage.populateEntriesForMissingMandatoryRestrictions(newRestrictionsList, acePage.getSupportedRestrictions());
        assertEquals(0, newRestrictionsList.size());

        // mock a restriction being mandatory
        mockRestrictionProvider(true);
        acePage.populateEntriesForMissingMandatoryRestrictions(newRestrictionsList, acePage.getSupportedRestrictions());
        assertEquals(2, newRestrictionsList.size());
        assertTrue(newRestrictionsList.stream().anyMatch(p -> p.getName().equals(AccessControlConstants.REP_GLOB)));
        assertTrue(newRestrictionsList.stream().anyMatch(p -> p.getName().equals(AccessControlConstants.REP_NT_NAMES)));
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.Ace#toSrMap(java.util.Set)}.
     */
    @Test
    void testToSrMap() {
        Map<String, RestrictionDefinition> srMap = acePage.toSrMap(acePage.getSupportedRestrictions());
        assertNotNull(srMap);
        assertEquals(2, srMap.size());
        assertTrue(srMap.containsKey(repGlobDef.getName()));
        assertTrue(srMap.containsKey(repNtNamesDef.getName()));
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.Ace#getSupportedOrRegisteredPrivileges(javax.jcr.Session, java.lang.String)}.
     * @throws RepositoryException 
     */
    @Test
    void testGetSupportedOrRegisteredPrivileges() throws RepositoryException {
        // mock that the resource does exist
        Mockito.when(jcrSession.nodeExists(acePage.getAcePath())).thenReturn(true);
        @NotNull
        Privilege[] supportedOrRegisteredPrivileges = acePage.getSupportedOrRegisteredPrivileges(jcrSession, acePage.getAcePath());
        assertNotNull(supportedOrRegisteredPrivileges);
        assertEquals(5, supportedOrRegisteredPrivileges.length);
        assertTrue(Stream.of(supportedOrRegisteredPrivileges).anyMatch(p -> jcrRead.equals(p)));
        assertTrue(Stream.of(supportedOrRegisteredPrivileges).anyMatch(p -> jcrWrite.equals(p)));

        // mock that the resource does not exist
        Mockito.when(jcrSession.nodeExists(Ace.PATH_REPOSITORY)).thenReturn(false);
        Workspace workspace = Mockito.mock(JackrabbitWorkspace.class);
        Mockito.when(jcrSession.getWorkspace()).thenReturn(workspace);
        PrivilegeManager privilegeManager = Mockito.mock(PrivilegeManager.class);
        Mockito.when(((JackrabbitWorkspace)workspace).getPrivilegeManager()).thenReturn(privilegeManager);
        Mockito.when(privilegeManager.getRegisteredPrivileges()).thenReturn(new Privilege[] {
                jcrRead,
                jcrWrite
        });
        supportedOrRegisteredPrivileges = acePage.getSupportedOrRegisteredPrivileges(jcrSession, Ace.PATH_REPOSITORY);
        assertNotNull(supportedOrRegisteredPrivileges);
        assertEquals(2, supportedOrRegisteredPrivileges.length);
        assertTrue(Stream.of(supportedOrRegisteredPrivileges).anyMatch(p -> jcrRead.equals(p)));
        assertTrue(Stream.of(supportedOrRegisteredPrivileges).anyMatch(p -> jcrWrite.equals(p)));
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.Ace#initialPrivilegesMap(java.util.Map, java.lang.String)}.
     * @throws RepositoryException 
     */
    @Test
    void testInitialPrivilegesMap() throws RepositoryException {
        Map<Privilege, String> privilegeLongestPathMap = AceUtils.getPrivilegeLongestPathMap(jcrSession);
        // mock that the resource does exist
        Mockito.when(jcrSession.nodeExists(acePage.getAcePath())).thenReturn(true);
        Map<Privilege, PrivilegeItem> initialPrivilegesMap = acePage.initialPrivilegesMap(privilegeLongestPathMap, acePage.getAcePath());
        assertNotNull(initialPrivilegesMap);
        assertEquals(4, initialPrivilegesMap.size());
        assertTrue(initialPrivilegesMap.containsKey(jcrRead));
        assertTrue(initialPrivilegesMap.containsKey(jcrWrite));
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.Ace#getPersistedPrivilegesMap()}.
     * @throws RepositoryException 
     */
    @Test
    void testGetPersistedPrivilegesMap() throws RepositoryException {
        Assertions.assertDoesNotThrow(() -> page.init());

        JsonObject aceJson;
        try (StringReader strReader = new StringReader(ACE_JSON);
                JsonReader jsonReader = Json.createReader(strReader)) {
            aceJson = jsonReader.readObject();
        }
        Mockito.when(acePage.getAce.getAce(jcrSession, acePage.resource.getPath(), "testUser1"))
            .thenReturn(aceJson);

        Map<Privilege, PrivilegeItem> persistedPrivilegesMap = acePage.getPersistedPrivilegesMap();
        assertNotNull(persistedPrivilegesMap);
        assertEquals(2, persistedPrivilegesMap.size());
        assertTrue(persistedPrivilegesMap.containsKey(jcrRead));
        assertTrue(persistedPrivilegesMap.containsKey(jcrWrite));
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.Ace#jsonToRestrictionItems(java.util.Map, javax.json.JsonObject)}.
     */
    @Test
    void testJsonToRestrictionItems() {
        JsonObject restrictionsJson;
        try (StringReader strReader = new StringReader(RESTRICTIONS_JSON);
                JsonReader jsonReader = Json.createReader(strReader)) {
            restrictionsJson = jsonReader.readObject();
        }
        List<RestrictionItem> jsonToRestrictionItems = acePage.jsonToRestrictionItems(acePage.toSrMap(acePage.getSupportedRestrictions()), restrictionsJson);
        assertNotNull(jsonToRestrictionItems);
        assertEquals(2, jsonToRestrictionItems.size());
        assertTrue(jsonToRestrictionItems.stream().anyMatch(p -> repGlobDef.getName().equals(p.getName())));
        assertTrue(jsonToRestrictionItems.stream().anyMatch(p -> repNtNamesDef.getName().equals(p.getName())));
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.Ace#getSupportedRestrictions()}.
     */
    @Test
    void testGetSupportedRestrictions() {
        Set<RestrictionDefinition> supportedRestrictions = acePage.getSupportedRestrictions();
        assertNotNull(supportedRestrictions);
        assertEquals(2, supportedRestrictions.size());
        assertTrue(supportedRestrictions.contains(repGlobDef));
        assertTrue(supportedRestrictions.contains(repNtNamesDef));
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.Ace#getSupportedRestrictionsInfo()}.
     */
    @Test
    void testGetSupportedRestrictionsInfo() {
        List<RestrictionDefinitionInfo> supportedRestrictionsInfo = acePage.getSupportedRestrictionsInfo();
        assertNotNull(supportedRestrictionsInfo);
        assertEquals(2, supportedRestrictionsInfo.size());
        assertEquals(repGlobDef.getName(), supportedRestrictionsInfo.get(0).getName());
        assertEquals(repNtNamesDef.getName(), supportedRestrictionsInfo.get(1).getName());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.Ace#getPrivilegeAggregationsAsJSON()}.
     * @throws RepositoryException 
     */
    @Test
    void testGetPrivilegeAggregationsAsJSON() throws RepositoryException {
        // mock that the resource does exist
        Mockito.when(jcrSession.nodeExists(acePage.getAcePath())).thenReturn(true);
        String privilegeAggregationsAsJSON = acePage.getPrivilegeAggregationsAsJSON();
        assertNotNull(privilegeAggregationsAsJSON);
        JsonObject json;
        try (StringReader strReader = new StringReader(privilegeAggregationsAsJSON);
                JsonReader jsonReader = Json.createReader(strReader)) {
            json = jsonReader.readObject();
        }
        assertEquals(2, json.size());
        assertTrue(json.containsKey(PrivilegeConstants.JCR_ALL));
        assertTrue(json.containsKey(PrivilegeConstants.JCR_READ));
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.Ace#getExistingRestrictionNamesAsJSON()}.
     * @throws RepositoryException 
     */
    @Test
    void testGetExistingRestrictionNamesAsJSON() throws RepositoryException {
        // try again with no existing restrictions
        mockPersistedPrivileges(ACE_JSON);
        String existingRestrictionNamesAsJSON = acePage.getExistingRestrictionNamesAsJSON();
        assertNotNull(existingRestrictionNamesAsJSON);
        JsonObject restrictionNamesJson;
        try (StringReader strReader = new StringReader(existingRestrictionNamesAsJSON);
                JsonReader jsonReader = Json.createReader(strReader)) {
            restrictionNamesJson = jsonReader.readObject();
        }
        assertNotNull(restrictionNamesJson);
        JsonValue allow = restrictionNamesJson.get("allow");
        assertTrue(allow instanceof JsonArray);
        assertEquals(0, ((JsonArray)allow).size());
        JsonValue deny = restrictionNamesJson.get("deny");
        assertTrue(deny instanceof JsonArray);
        assertEquals(0, ((JsonArray)deny).size());

        // try again with existing restrictions
        mockPersistedPrivileges(ACE_JSON_WITH_RESTRICTIONS);
        existingRestrictionNamesAsJSON = acePage.getExistingRestrictionNamesAsJSON();
        assertNotNull(existingRestrictionNamesAsJSON);
        try (StringReader strReader = new StringReader(existingRestrictionNamesAsJSON);
                JsonReader jsonReader = Json.createReader(strReader)) {
            restrictionNamesJson = jsonReader.readObject();
        }
        assertNotNull(restrictionNamesJson);
        allow = restrictionNamesJson.get("allow");
        assertTrue(allow instanceof JsonArray);
        assertEquals(1, ((JsonArray)allow).size());
        assertEquals("jcr:read@rep:ntNames", ((JsonArray)allow).getString(0));
        deny = restrictionNamesJson.get("deny");
        assertTrue(deny instanceof JsonArray);
        assertEquals(1, ((JsonArray)deny).size());
        assertEquals("jcr:write@rep:glob", ((JsonArray)deny).getString(0));
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.Ace#getOrderValue()}.
     */
    @Test
    void testGetOrderValue() {
        assertEquals("", acePage.getOrderValue());
        Mockito.when(acePage.request.getParameter("order")).thenReturn("first");
        assertEquals("first", acePage.getOrderValue());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.Ace#getOrderList()}.
     * @throws RepositoryException 
     */
    @Test
    void testGetOrderList() throws RepositoryException {
        JsonObject aclJson;
        try (StringReader strReader = new StringReader(AclTest.ACL_JSON);
                JsonReader jsonReader = Json.createReader(strReader)) {
            aclJson = jsonReader.readObject();
        }
        Mockito.when(acePage.getAcl.getAcl(jcrSession, acePage.resource.getPath()))
            .thenReturn(aclJson);

        Collection<PrincipalPrivilege> orderList = acePage.getOrderList();
        assertNotNull(orderList);
        assertEquals(2, orderList.size());
        PrincipalPrivilege[] array = orderList.toArray(new PrincipalPrivilege[orderList.size()]);
        assertEquals("testUser1", array[0].getName());
        assertEquals("testGroup1", array[1].getName());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.Ace#fieldValuesFromReqParams(org.apache.sling.api.request.RequestParameter[])}.
     */
    @Test
    void testFieldValuesFromReqParams() {
        RequestParameter [] paramValues = new RequestParameter[] {
                new RequestParameterImpl("key1", "value1"),
                new RequestParameterImpl("key1", "value2"),
        };
        assertArrayEquals(new String[] {"value1", "value2"}, 
                acePage.fieldValuesFromReqParams(paramValues));
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.Ace#getFieldValuesForPattern(java.util.regex.Pattern)}.
     */
    @Test
    void testGetFieldValuesForPattern() {
        Map<String, String[]> rawReqParams = new HashMap<>();
        rawReqParams.put("restriction@jcr:read@rep:glob@Allow", new String[] {"glob1"});
        rawReqParams.put("restriction@jcr:write@rep:ntNames@Deny", new String[] {"ntNames1", "ntNames2"});
        rawReqParams.put("key1", new String[] {"value2"});
        RequestParameterMap requestParameterMap = new RequestParameterMapImpl(rawReqParams);
        Mockito.when(acePage.request.getRequestParameterMap()).thenReturn(requestParameterMap);

        Map<String, String[]> fieldValuesForPattern = acePage.getFieldValuesForPattern(Ace.RESTRICTION_PATTERN);
        assertNotNull(fieldValuesForPattern);
        assertEquals(2, fieldValuesForPattern.size());
        assertArrayEquals(new String[] {"glob1"}, fieldValuesForPattern.get("restriction@jcr:read@rep:glob@Allow"));
        assertArrayEquals(new String[] {"ntNames1", "ntNames2"}, fieldValuesForPattern.get("restriction@jcr:write@rep:ntNames@Deny"));
    }

}
