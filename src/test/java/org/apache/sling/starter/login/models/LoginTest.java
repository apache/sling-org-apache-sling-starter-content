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
package org.apache.sling.starter.login.models;

import java.util.stream.Stream;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.auth.core.spi.AuthenticationHandler.FAILURE_REASON_CODES;
import org.apache.sling.auth.core.spi.JakartaAuthenticationHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 */
class LoginTest {

    private Login login;

    @BeforeEach
    protected void before() {
        login = new Login();
        login.request = Mockito.mock(SlingJakartaHttpServletRequest.class);
    }

    /**
     * Test method for {@link org.apache.sling.starter.login.models.Login#getResource()}.
     */
    @Test
    void testGetResource() {
        Mockito.when(login.request.getParameter("resource")).thenReturn("/myresource1");
        assertEquals("/myresource1", login.getResource());
    }

    protected static Stream<Arguments> testGetReasonArgs() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of("", null),
                Arguments.of(FAILURE_REASON_CODES.ACCOUNT_LOCKED.name(), "Account is locked"),
                Arguments.of(FAILURE_REASON_CODES.ACCOUNT_NOT_FOUND.name(), "Account was not found"),
                Arguments.of(FAILURE_REASON_CODES.PASSWORD_EXPIRED.name(), "Password expired"),
                Arguments.of(
                        FAILURE_REASON_CODES.PASSWORD_EXPIRED_AND_NEW_PASSWORD_IN_HISTORY.name(),
                        "Password expired and new password found in password history"),
                Arguments.of(FAILURE_REASON_CODES.UNKNOWN.name(), "User name and password do not match"),
                Arguments.of(FAILURE_REASON_CODES.INVALID_LOGIN.name(), "User name and password do not match"),
                Arguments.of("other", "User name and password do not match"));
    }
    /**
     * Test method for {@link org.apache.sling.starter.login.models.Login#getReason()}.
     */
    @ParameterizedTest
    @MethodSource("testGetReasonArgs")
    void testGetReason(String reasonCode, String expectedMsg) {
        Mockito.when(login.request.getParameter(JakartaAuthenticationHandler.FAILURE_REASON_CODE))
                .thenReturn(reasonCode);
        assertEquals(expectedMsg, login.getReason());
    }
}
