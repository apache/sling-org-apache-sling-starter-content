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

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.jcr.Node;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.jackrabbit.accessmanager.PrivilegesInfo;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;

/**
 * Base class for common ACL/ACE functionality
 */
public abstract class AccessFormPage {
    protected PrivilegesInfo privilegesInfo = null;

    @ScriptVariable
    protected SlingHttpServletRequest request;

    @ScriptVariable
    protected SlingHttpServletResponse response;

    @ScriptVariable
    protected Resource resource;

    /**
     * Instantiates the model.
     */
    @PostConstruct
    protected void init() throws IOException {
        if (!getCanReadAccessControl()) {
            if (request.getRemoteUser() == null) {
                // 404 for anonymous
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                // 403 for logged in user
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
        }
    }

    /**
     * Lazy create the PrivilegesInfo field the first time it is used.
     */
    protected PrivilegesInfo getPrivilegesInfo() {
        if (privilegesInfo == null) {
            privilegesInfo = new PrivilegesInfo();
        }
        return privilegesInfo;
    }

    /**
     * Checks whether the current user has been granted privileges
     * to modify the access control of the current node.
     *  
     * @return true if the current user has the privileges, false otherwise
     */
    public boolean getCanReadAccessControl() {
        return getPrivilegesInfo().canReadAccessControl(resource.adaptTo(Node.class));
    }

    /**
     * Checks whether the current user has been granted privileges
     * to modify the access control of the current node.
     *  
     * @return true if the current user has the privileges, false otherwise
     */
    public boolean getCanModifyAccessControl() {
        return getPrivilegesInfo().canModifyAccessControl(resource.adaptTo(Node.class));
    }

    public String getLocation() {
        String location = null;
        if (resource != null) {
            location = resource.getResourceResolver().map(resource.getPath());
        }
        return location;
    }

}
