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

import java.security.Principal;

import org.apache.jackrabbit.api.security.principal.GroupPrincipal;
import org.jetbrains.annotations.NotNull;

public class PrincipalPrivilege {
    private String principalName;
    private boolean isAllow;
    private boolean isDeny;
    private boolean isGroup;

    public PrincipalPrivilege(@NotNull Principal principal) {
        super();
        this.principalName = principal.getName();
        this.isGroup = principal instanceof GroupPrincipal;
    }

    public String getName() {
        return principalName;
    }

    public String getPrivilegesDisplayName() {
        StringBuilder buffer = new StringBuilder();
        if (isAllow) {
            buffer.append("allow");
        }
        if (isDeny) {
            if (buffer.length() > 0) {
                buffer.append(" / ");
            }
            buffer.append("deny");
        }
        return buffer.toString();
    }

    public boolean isAllow() {
        return isAllow;
    }

    public void setAllow(boolean isAllow) {
        this.isAllow = isAllow;
    }

    public boolean isDeny() {
        return isDeny;
    }

    public void setDeny(boolean isDeny) {
        this.isDeny = isDeny;
    }

    public boolean getIsGroup() {
        return isGroup;
    }

}
