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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.jackrabbit.accessmanager.PrivilegesInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

abstract class AccessFormPageTest {
    protected AccessFormPage page;
    protected Node currentNode;
    protected ResourceResolver rr;
    protected abstract AccessFormPage createPageModel();

    @BeforeEach
    void beforeEach() throws RepositoryException {
        page = createPageModel();
        page.privilegesInfo = Mockito.mock(PrivilegesInfo.class);
        page.resource = Mockito.mock(Resource.class);
        page.request = Mockito.mock(SlingHttpServletRequest.class);
        page.response = Mockito.mock(SlingHttpServletResponse.class);

        rr = Mockito.mock(ResourceResolver.class);
        currentNode = Mockito.mock(Node.class);
        Mockito.when(page.resource.adaptTo(Node.class)).thenReturn(currentNode);
        Mockito.when(page.resource.getPath()).thenReturn("/content/test1");
        Mockito.when(page.resource.getResourceResolver()).thenReturn(rr);
        Mockito.when(page.request.getResourceResolver()).thenReturn(rr);
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.AccessFormPage#init()}.
     */
    @Test
    void testInit() {
        Assertions.assertDoesNotThrow(() -> page.init());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.AccessFormPage#getPrivilegesInfo()}.
     */
    @Test
    void testGetPrivilegesInfo() {
        assertNotNull(page.getPrivilegesInfo());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.AccessFormPage#getCanReadAccessControl()}.
     */
    @Test
    void testGetCanReadAccessControl() {
        Mockito.when(page.privilegesInfo.canReadAccessControl(currentNode)).thenReturn(true);
        assertTrue(page.getCanReadAccessControl());
        Mockito.when(page.privilegesInfo.canReadAccessControl(currentNode)).thenReturn(false);
        assertFalse(page.getCanReadAccessControl());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.AccessFormPage#getCanModifyAccessControl()}.
     */
    @Test
    void testGetCanModifyAccessControl() {
        Mockito.when(page.privilegesInfo.canModifyAccessControl(currentNode)).thenReturn(true);
        assertTrue(page.getCanModifyAccessControl());
        Mockito.when(page.privilegesInfo.canModifyAccessControl(currentNode)).thenReturn(false);
        assertFalse(page.getCanModifyAccessControl());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.AccessFormPage#getLocation()}.
     */
    @Test
    void testGetLocation() {
        Mockito.when(rr.map(page.resource.getPath())).thenReturn("/test1");
        assertEquals("/test1", page.getLocation());
    }

}
