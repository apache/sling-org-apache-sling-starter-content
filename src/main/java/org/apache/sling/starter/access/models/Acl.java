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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.jcr.jackrabbit.accessmanager.GetAcl;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;

@Model(adaptables=SlingHttpServletRequest.class)
public class Acl extends AccessFormPage {
    private List<PrincipalPrivilege> principalPrivilegeList;

    @OSGiService
    public GetAcl getAcl = null;

    public Collection<PrincipalPrivilege> getPrincipals() throws RepositoryException {
        if (principalPrivilegeList == null) {
            principalPrivilegeList = new ArrayList<>();

            Session jcrSession = request.getResourceResolver().adaptTo(Session.class);
            PrincipalManager principalManager = ((JackrabbitSession)jcrSession).getPrincipalManager();
            JsonObject acl = getAcl.getAcl(jcrSession, resource.getPath());
            for (Entry<String, JsonValue> entry : acl.entrySet()) {
                String uid = entry.getKey();
                Principal principal = principalManager.getPrincipal(uid);
                if (principal != null) {
                    PrincipalPrivilege pi = new PrincipalPrivilege(principal);
                    AtomicBoolean allow = new AtomicBoolean(false);
                    AtomicBoolean deny = new AtomicBoolean(false);
                    JsonObject privilegesObj = ((JsonObject)entry.getValue()).getJsonObject("privileges");
                    privilegesObj.values().stream()
                        .forEach(item -> {
                            allow.set(allow.get() || ((JsonObject)item).containsKey("allow"));
                            deny.set(deny.get() || ((JsonObject)item).containsKey("deny"));
                        });
                    if (allow.get()) {
                        pi.setAllow(true);
                    }
                    if (deny.get()) {
                        pi.setDeny(true);
                    }
                    principalPrivilegeList.add(pi);
                }
            }
        }

        return principalPrivilegeList;
    }

}
