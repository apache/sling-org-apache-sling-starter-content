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

import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants;

/**
 * Utility for helping to sort and order privileges by their longest aggregate depth
 */
public class AceUtils {

    private AceUtils() {
        // private constructor to hide the implicit public one
    }

    /**
     * If the privilege is contained in multiple aggregate privileges, then
     * calculate the instance with the greatest depth.
     */
    private static void toLongestPath(String prefix, Privilege parentPrivilege, Map<Privilege, String> privilegeToLongestPath) {
        Privilege[] declaredAggregatePrivileges = parentPrivilege.getDeclaredAggregatePrivileges();
        for (Privilege privilege : declaredAggregatePrivileges) {
            String candidatePath = String.format("%s/%s", prefix, privilege.getName());
            String oldValue = privilegeToLongestPath.get(privilege);
            if (oldValue == null || oldValue.length() < candidatePath.length()) {
                privilegeToLongestPath.put(privilege, candidatePath);

                // continue drilling down to the leaf privileges
                toLongestPath(candidatePath, privilege, privilegeToLongestPath);
            }
        }
    }

    /**
     * Calculate the longest path for each of the possible privileges
     * @param jcrSession the current users JCR session
     * @return map where the key is the privilege and the value is the longest path
     */
    public static Map<Privilege, String> getPrivilegeLongestPathMap(Session jcrSession) throws RepositoryException {
        AccessControlManager accessControlManager = jcrSession.getAccessControlManager();
        Privilege jcrAll = accessControlManager.privilegeFromName(PrivilegeConstants.JCR_ALL);

        Map<Privilege, String> privilegeToLongestPath = new HashMap<>();
        privilegeToLongestPath.put(jcrAll, jcrAll.getName());
        toLongestPath(jcrAll.getName(), jcrAll, privilegeToLongestPath);
        return privilegeToLongestPath;
    }

}
