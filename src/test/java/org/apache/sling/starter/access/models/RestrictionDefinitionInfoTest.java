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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.RestrictionDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

class RestrictionDefinitionInfoTest {
    private RestrictionDefinitionInfo rdInfo;

    @BeforeEach
    void beforeEach() {
        RestrictionDefinition rd = createMockRestrictionDefintion();
        rdInfo = new RestrictionDefinitionInfo(AccessControlConstants.REP_ITEM_NAMES + " DisplayName", rd);
    }

    protected RestrictionDefinition createMockRestrictionDefintion() {
        RestrictionDefinition rd = Mockito.mock(RestrictionDefinition.class);
        Mockito.when(rd.getName()).thenReturn(AccessControlConstants.REP_ITEM_NAMES);
        Mockito.when(rd.getRequiredType()).thenAnswer(new Answer<Type<?>>() {
            @Override
            public Type<?> answer(InvocationOnMock invocation) throws Throwable {
                return Type.STRINGS;
            }
        });
        return rd;
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.RestrictionDefinitionInfo#getDisplayName()}.
     */
    @Test
    void testGetDisplayName() {
        assertEquals(AccessControlConstants.REP_ITEM_NAMES + " DisplayName", rdInfo.getDisplayName());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.RestrictionDefinitionInfo#getName()}.
     */
    @Test
    void testGetName() {
        assertEquals(AccessControlConstants.REP_ITEM_NAMES, rdInfo.getName());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.RestrictionDefinitionInfo#getRequiredType()}.
     */
    @Test
    void testGetRequiredType() {
        assertEquals(Type.STRINGS, rdInfo.getRequiredType());
    }

    /**
     * Test method for {@link org.apache.sling.starter.access.models.RestrictionDefinitionInfo#isMandatory()}.
     */
    @Test
    void testIsMandatory() {
        assertFalse(rdInfo.isMandatory());

        // also try a manditory one
        RestrictionDefinition manditoryRd = createMockRestrictionDefintion();
        Mockito.when(manditoryRd.isMandatory()).thenReturn(true);
        RestrictionDefinitionInfo manditoryRdInfo = new RestrictionDefinitionInfo(AccessControlConstants.REP_ITEM_NAMES + " DisplayName", manditoryRd);
        assertTrue(manditoryRdInfo.isMandatory());
    }

}
