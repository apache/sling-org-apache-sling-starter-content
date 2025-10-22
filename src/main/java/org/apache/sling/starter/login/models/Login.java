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

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.auth.core.spi.AuthenticationHandler.FAILURE_REASON_CODES;
import org.apache.sling.auth.core.spi.JakartaAuthenticationHandler;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Model to assist rendering the custom login page
 */
@Model(adaptables = SlingJakartaHttpServletRequest.class)
public class Login {

    @SlingObject
    protected SlingJakartaHttpServletRequest request;

    /**
     * Return the resource path to go to after login is successful
     * @return the resource path or null
     */
    public String getResource() {
        return request.getParameter("resource");
    }

    /**
     * Returns an informational message according to the value provided in the
     * <code>j_reason_code</code> request parameter. Supported reasons are invalid
     * credentials and session timeout.
     *
     * @return The "translated" reason to render the login form or an empty string
     *         if there is no specific reason
     */
    public String getReason() {
        String reason = null;
        String jReasonCode = request.getParameter(JakartaAuthenticationHandler.FAILURE_REASON_CODE);
        if (jReasonCode != null && !jReasonCode.isEmpty()) {
            FAILURE_REASON_CODES reasonCode;
            try {
                reasonCode = FAILURE_REASON_CODES.valueOf(jReasonCode);
            } catch (IllegalArgumentException iae) {
                reasonCode = FAILURE_REASON_CODES.UNKNOWN;
            }

            switch (reasonCode) {
                case ACCOUNT_LOCKED:
                    reason = "Account is locked";
                    break;
                case ACCOUNT_NOT_FOUND:
                    reason = "Account was not found";
                    break;
                case PASSWORD_EXPIRED:
                    reason = "Password expired";
                    break;
                case PASSWORD_EXPIRED_AND_NEW_PASSWORD_IN_HISTORY:
                    reason = "Password expired and new password found in password history";
                    break;
                case UNKNOWN, INVALID_LOGIN:
                default:
                    reason = "User name and password do not match";
                    break;
            }
        }

        return reason;
    }
}
