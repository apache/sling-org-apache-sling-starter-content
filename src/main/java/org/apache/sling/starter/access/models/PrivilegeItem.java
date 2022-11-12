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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PrivilegeItem {
    private String name;
    private boolean granted;
    private boolean denied;
    private String longestPath;
    private long depth;
    private String extraCssClasses;
    private List<RestrictionItem> allowRestrictions;
    private List<RestrictionItem> denyRestrictions;
    private Set<String> allowRestrictionsToDelete;
    private Set<String> denyRestrictionsToDelete;
    private boolean allowExists = false;
    private boolean denyExists = false;

    public PrivilegeItem(String name, boolean granted, boolean denied, String longestPath) {
        this(name, granted, denied, longestPath, Collections.emptyList(), Collections.emptyList());
    }
    public PrivilegeItem(String name, boolean granted, boolean denied, String longestPath, 
            List<RestrictionItem> allowRestrictions, List<RestrictionItem> denyRestrictions) {
        super();
        this.name = name;
        this.granted = granted;
        this.denied = denied;
        this.longestPath = longestPath;
        this.depth = longestPath.chars().filter(ch -> ch == '/').count();
        this.allowRestrictions = allowRestrictions;
        this.allowRestrictions.forEach(r -> r.setDepth(depth + 1));
        this.denyRestrictions = denyRestrictions;
        this.denyRestrictions.forEach(r -> r.setDepth(depth + 1));
    }

    public String getName() {
        return name;
    }

    public boolean getNone() {
        return !granted && !denied;
    }

    public boolean getGranted() {
        return granted;
    }

    public boolean getDenied() {
        return denied;
    }

    public void setGranted(boolean granted) {
        this.granted = granted;
    }

    public void setDenied(boolean denied) {
        this.denied = denied;
    }

    public boolean isAllowExists() {
        return allowExists;
    }

    public void setAllowExists(boolean allowExists) {
        this.allowExists = allowExists;
    }

    public boolean isDenyExists() {
        return denyExists;
    }

    public void setDenyExists(boolean denyExists) {
        this.denyExists = denyExists;
    }

    public String getLongestPath() {
        return longestPath;
    }

    public void addExtraCssClass(String addClass) {
        if (this.extraCssClasses == null) {
            this.extraCssClasses  = addClass;
        } else {
            this.extraCssClasses = String.format("%s %s", this.extraCssClasses, addClass);
        }
    }

    public String getDepthCss() {
        String css;
        if (extraCssClasses != null) {
            css = String.format("depth%d %s", this.depth, extraCssClasses);
        } else {
            css = String.format("depth%d", this.depth);
        }
        return css;
    }

    public long getDepth() {
        return depth;
    }

    public List<RestrictionItem> getAllowRestrictions() {
        return allowRestrictions;
    }

    public List<RestrictionItem> getDenyRestrictions() {
        return denyRestrictions;
    }

    public void setAllowRestrictions(List<RestrictionItem> restrictions) {
        this.allowRestrictions = restrictions;
    }
    public void setDenyRestrictions(List<RestrictionItem> restrictions) {
        this.denyRestrictions = restrictions;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PrivilegeItem [name=");
        builder.append(name);
        builder.append(", granted=");
        builder.append(granted);
        builder.append(", denied=");
        builder.append(denied);
        builder.append(", longestPath=");
        builder.append(longestPath);
        builder.append(", depth=");
        builder.append(depth);
        builder.append(", extraCssClasses=");
        builder.append(extraCssClasses);
        builder.append(", allowRestrictions=");
        builder.append(allowRestrictions);
        builder.append(", denyRestrictions=");
        builder.append(denyRestrictions);
        builder.append("]");
        return builder.toString();
    }

    public void addAllowRestrictionToDelete(String restrictionName) {
        if (allowRestrictionsToDelete == null) {
            allowRestrictionsToDelete = new HashSet<>();
        }
        allowRestrictionsToDelete.add(restrictionName);
    }

    public Collection<String> getAllowRestrictionsToDelete() {
        if (allowRestrictionsToDelete != null) {
            return allowRestrictionsToDelete;
        }
        return Collections.emptySet();
    }

    public void addDenyRestrictionToDelete(String restrictionName) {
        if (denyRestrictionsToDelete == null) {
            denyRestrictionsToDelete = new HashSet<>();
        }
        denyRestrictionsToDelete.add(restrictionName);
    }

    public Collection<String> getDenyRestrictionsToDelete() {
        if (denyRestrictionsToDelete != null) {
            return denyRestrictionsToDelete;
        }
        return Collections.emptySet();
    }

}
