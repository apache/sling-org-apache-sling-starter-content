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
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.api.security.authorization.PrivilegeManager;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.RestrictionDefinition;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.RestrictionProvider;
import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.jcr.jackrabbit.accessmanager.GetAce;
import org.apache.sling.jcr.jackrabbit.accessmanager.GetAcl;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Source;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The ace page options.
 */
@Model(adaptables=SlingHttpServletRequest.class)
public class Ace extends AccessFormPage {
    // for principal ace
    protected static final String PATH_REPOSITORY = "/:repository";

    // for matching restriction request parameters
    protected static final Pattern RESTRICTION_PATTERN = Pattern.compile("^restriction@([^@]+)@([^@]+)@(Allow|Deny)$");
    protected static final Pattern RESTRICTION_PATTERN_DELETE = Pattern.compile(String.format("^restriction@([^@]+)@([^@]+)%s$",
            SlingPostConstants.SUFFIX_DELETE));

    // use these hints to influence the order that the privileges are displayed
    protected static final Map<String, Integer> privilegesPriority = createPrivilegesPriorityMap();
    protected static Map<String, Integer> createPrivilegesPriorityMap() {
        Map<String, Integer> myMap = new HashMap<>();
        myMap.put(PrivilegeConstants.JCR_READ, 1);
        myMap.put(PrivilegeConstants.REP_WRITE, 2);
        myMap.put(PrivilegeConstants.JCR_READ_ACCESS_CONTROL, 3);
        myMap.put(PrivilegeConstants.JCR_MODIFY_ACCESS_CONTROL, 4);

        myMap.put(PrivilegeConstants.JCR_WRITE, 1);
        myMap.put(PrivilegeConstants.JCR_NODE_TYPE_MANAGEMENT, 2);
        return myMap;
    }

    protected String principalId;
    protected Map<Privilege, PrivilegeItem> persistedPrivilegesMap = null;
    private boolean aceExists;

    @Inject @Source("osgi-services")
    protected List<RestrictionProvider> restrictionProviders = null;

    @Inject @Source("osgi-services") 
    public GetAce getAce = null;

    @Inject @Source("osgi-services") 
    public GetAcl getAcl = null;

    /**
     * Instantiates the model.
     */
    @Override
    @PostConstruct
    protected void init() throws IOException {
        super.init();
        principalId = request.getParameter("pid");
    }

    public String getPrincipalId() {
        return principalId;
    }

    public boolean getIsInvalidPrincipal() throws RepositoryException {
        boolean isInValidPrincipal = true;
        if (principalId != null && !principalId.isEmpty()) {
            Session session = request.getResourceResolver().adaptTo(Session.class);
            if (session instanceof JackrabbitSession) {
                UserManager userManager = ((JackrabbitSession)session).getUserManager();
                if (userManager != null) {
                    Authorizable authorizable = userManager.getAuthorizable(principalId);
                    if (authorizable != null) {
                        isInValidPrincipal = false;
                    } else {
                        //no user/group matches the supplied principal id
                    }
                }
            }
        }

        return isInValidPrincipal;
    }

    protected String getAcePath() {
        return resource.getPath();
    }

    public boolean isExists() {
        return aceExists;
    }

    public Collection<PrivilegeItem> getPrivileges() throws RepositoryException {
        Map<Privilege, PrivilegeItem> privilegesMap = getPersistedPrivilegesMap(); 
        if (privilegesMap == null || privilegesMap.isEmpty()) {
            return Collections.emptyList();
        }

        //make a temp map for quick lookup below
        Set<RestrictionDefinition> supportedRestrictions = getSupportedRestrictions();
        Map<String, RestrictionDefinition> srMap = toSrMap(supportedRestrictions);

        Map<String, List<RestrictionItem>> postedAllowRestrictionsMap = new HashMap<>(); 
        Map<String, List<RestrictionItem>> postedDenyRestrictionsMap = new HashMap<>(); 
        Map<String, String[]> fieldValues = populateEntriesFromPreviousFailedPost(postedAllowRestrictionsMap, postedDenyRestrictionsMap, srMap);
        Map<String, String[]> toDeleteFieldValues = getFieldValuesForPattern(RESTRICTION_PATTERN_DELETE);
        //entries from the previous failed POST.
        for (PrivilegeItem entry : privilegesMap.values()) {
            String privilegeName = entry.getName();
            // check for any submitted form fields in case of error and redisplay of the page
            String paramValue = request.getParameter(String.format("privilege@%s", privilegeName));
            if (paramValue != null) {
                //req param was here from a failed post?
                if ("granted".equals(paramValue)) {
                    entry.setGranted(true);
                } else if ("denied".equals(paramValue)) {
                    entry.setDenied(true);
                }
            } else {
                // check for delete existing param
                String paramDeleteValue = request.getParameter(String.format("privilege@%s@Delete", privilegeName));
                if (paramDeleteValue != null) {
                    //req param was here from a failed post?
                    if ("granted".equals(paramDeleteValue)) {
                        entry.setGranted(false);
                    } else if ("denied".equals(paramDeleteValue)) {
                        entry.setDenied(false);
                    }
                }
            }

            for (boolean forAllow : new boolean [] {true, false}) {
                // first add items for any posted fields
                Map<String, List<RestrictionItem>> postedRestrictionsMap = forAllow ? postedAllowRestrictionsMap : postedDenyRestrictionsMap;
                List<RestrictionItem> newRestrictionsList = postedRestrictionsMap.computeIfAbsent(privilegeName, pn -> new ArrayList<>());
                //now merge in any declared restrictions that were not posted
                List<RestrictionItem> declaredRestrictions = forAllow ? entry.getAllowRestrictions() : entry.getDenyRestrictions();
                if (declaredRestrictions != null && !declaredRestrictions.isEmpty()) {
                    for (RestrictionItem ri : declaredRestrictions) {
                        String restrictionName = ri.getName();

                        boolean addIt = true;
                        String fieldKeyPrefix = String.format("restriction@%s@%s", privilegeName, restrictionName);
                        String fieldKey = String.format("%s@%s", fieldKeyPrefix, (forAllow ? "Allow" : "Deny"));
                        // skip it if it was requested to be deleted in the previous POST attempt
                        // or already handled above
                        if (toDeleteFieldValues.containsKey(String.format("%s%s", fieldKeyPrefix, SlingPostConstants.SUFFIX_DELETE))) {
                            addIt = false;
                        } else if (fieldValues.containsKey(fieldKey)) {
                            // mark the form posted item as exists since it also
                            //  had a persisted value
                            newRestrictionsList.stream()
                                .filter(list -> list.getName().equals(restrictionName))
                                .forEach(item -> item.setExists(true));
                            addIt = false;
                        }

                        if (addIt) {
                            newRestrictionsList.add(ri);
                        }
                    }
                }

                // check if we are missing an item for any mandatory restrictions
                populateEntriesForMissingMandatoryRestrictions(newRestrictionsList, supportedRestrictions);

                // and apply it
                if (forAllow) {
                    entry.setAllowRestrictions(newRestrictionsList);
                } else {
                    entry.setDenyRestrictions(newRestrictionsList);
                }

                // populate restrictions to delete here.
                toDeleteFieldValues.keySet().stream()
                    .filter(key -> RESTRICTION_PATTERN_DELETE.matcher(key).matches())
                    .forEach(key -> {
                        Matcher matcher = RESTRICTION_PATTERN_DELETE.matcher(key);
                        if (matcher.matches()) {
                            String restrictionName = matcher.group(2);
                            if (forAllow) {
                                entry.addAllowRestrictionToDelete(restrictionName);
                            } else {
                                entry.addDenyRestrictionToDelete(restrictionName);
                            }
                        }
                    });
            }
        }

        List<PrivilegeItem> list = new ArrayList<>(privilegesMap.values());
        list.sort((p1, p2) -> {
            String longestPath1 = p1.getLongestPath();
            String[] segments1 = longestPath1.split("/");
            String longestPath2 = p2.getLongestPath();
            String[] segments2 = longestPath2.split("/");

            for (int i = 0; i < segments1.length; i++) {
                Integer priority1 = privilegesPriority.getOrDefault(segments1[i], 1000);
                if (segments2.length <= i) {
                    return longestPath1.compareTo(longestPath2);
                }
                Integer priority2 = privilegesPriority.getOrDefault(segments2[i], 1000);
                int cmp = priority1.compareTo(priority2);
                if (cmp != 0) {
                    return cmp;
                }
            }
            if (segments1.length == segments2.length) {
                // natural sort of the namespace-free last segment of each path
                String lastSegment1 = segments1[segments1.length - 1];
                String lastSegment2 = segments2[segments2.length - 1];
                lastSegment1 = lastSegment1.substring(lastSegment1.indexOf(':'));
                lastSegment2 = lastSegment2.substring(lastSegment2.indexOf(':'));
                return lastSegment1.compareTo(lastSegment2);
            }
            return longestPath1.compareTo(longestPath2);
        });

        // loop through one more time to annotate the items with
        //   extra css markers to help render a tree-ish view
        boolean lastBranch = true;
        for (int i = list.size() - 1; i > 0; i--) {
            PrivilegeItem item = list.get(i);
            if (lastBranch) {
                item.addExtraCssClass("lastBranch");
            }
            if (lastBranch && item.getDepth() == 1) {
                lastBranch = false;
            }
            if (i == list.size() - 1) {
                item.addExtraCssClass("lastSibling");
            } else {
                boolean lastSibling = true;
                // if the there remains another item that has the same depth as this
                //  then this is not the last sibling
                for (int j = i + 1; j < list.size(); j++) {
                    PrivilegeItem nextItem = list.get(j);
                    if (nextItem.getDepth() == item.getDepth()) {
                        String nextItemParentPath = ResourceUtil.getParent(nextItem.getLongestPath());
                        String itemParentPath = ResourceUtil.getParent(item.getLongestPath());
                        if (nextItemParentPath != null && nextItemParentPath.equals(itemParentPath)) {
                            lastSibling = false;
                            break;
                        }
                    }
                }
                if (lastSibling) {
                    item.addExtraCssClass("lastSibling");
                }
            }
        }
        return list;
    }

    /**
     * Populate the restriction item list from data from a previously failed POST request
     * @param list the list of restriction items
     * @param srMap map where the key is the restriction name and the value is the restriction definition
     * @return map of field values that were found in the form context
     */
    protected Map<String, String[]> populateEntriesFromPreviousFailedPost(Map<String, List<RestrictionItem>> allowMap,
            Map<String, List<RestrictionItem>> denyMap,
            Map<String, RestrictionDefinition> srMap) {
        Map<String, String[]> toDeleteFieldValues = getFieldValuesForPattern(RESTRICTION_PATTERN_DELETE);
        Map<String, String[]> fieldValues = getFieldValuesForPattern(RESTRICTION_PATTERN);
        Set<Entry<String, String[]>> fieldValuesEntrySet = fieldValues.entrySet();
        for (Entry<String, String[]> entry : fieldValuesEntrySet) {
            String deleteKey = String.format("%s%s", entry.getKey(), SlingPostConstants.SUFFIX_DELETE);
            if (toDeleteFieldValues.containsKey(deleteKey)) {
                //it was requested to be deleted in the previous POST attempt
                continue;
            }

            Matcher matcher = RESTRICTION_PATTERN.matcher(entry.getKey());
            if (matcher.matches()) {
                String privilegeName = matcher.group(1);
                String restrictionName = matcher.group(2);
                boolean isAllow = "Allow".equals(matcher.group(3));

                RestrictionDefinition rd = srMap.get(restrictionName);
                if (rd != null) {
                    Object value = entry.getValue();
                    String[] strings = entry.getValue();
                    if (rd.getRequiredType().isArray()) {
                        value = strings;
                    } else if (strings.length > 0) {
                        //use the first one?
                        value = strings[0];
                    }

                    RestrictionItem ri = new RestrictionItem(rd, value, false);
                    Map<String, List<RestrictionItem>> map = isAllow ? allowMap : denyMap;
                    List<RestrictionItem> list = map.computeIfAbsent(privilegeName, n -> new ArrayList<>());
                    list.add(ri);
                }
            }
        }
        return fieldValues;
    }

    /**
     * Populate restriction item entries for any restriction definitions that are
     * mandatory and not already declared.
     * @param list the list of restriction items
     * @param supportedRestrictions supported restrictions set
     */
    protected void populateEntriesForMissingMandatoryRestrictions(List<RestrictionItem> list,
            Set<RestrictionDefinition> supportedRestrictions) {
        if (supportedRestrictions != null) {
            for (RestrictionDefinition rd : supportedRestrictions) {
                if (rd.isMandatory() && list.stream().noneMatch(item -> item.getName().equals(rd.getName()))) {
                    //missing it, so add an item to the list
                    RestrictionItem ri = new RestrictionItem(rd, null, false);
                    list.add(ri);
                }
            }
        }
    }

    /**
     * Convert the supported restrictions to a map for quick lookup later
     * @param supportedRestrictions the supported restrictions
     * @return a map where the key is the restriction name and the value is the restriction definition
     */
    protected Map<String, RestrictionDefinition> toSrMap(Set<RestrictionDefinition> supportedRestrictions) {
        Map<String, RestrictionDefinition> srMap = new HashMap<>();
        for (RestrictionDefinition restrictionDefinition : supportedRestrictions) {
            srMap.put(restrictionDefinition.getName(), restrictionDefinition);
        }
        return srMap;
    }

    /**
     * Calculates the supported privileges in the resource path exists, or the registered
     * privileges if the resource path does not exist
     * 
     * @param jcrSession the current session
     * @param resourcePath the resource path to consider
     * @return
     * @throws RepositoryException
     */
    protected @NotNull Privilege[] getSupportedOrRegisteredPrivileges(@NotNull Session jcrSession, @Nullable String resourcePath) 
            throws RepositoryException {
        Privilege[] supportedPrivileges = null;
        if (resourcePath != null && jcrSession.nodeExists(resourcePath)) {
            supportedPrivileges = jcrSession.getAccessControlManager().getSupportedPrivileges(resourcePath);
        } else {
            // non-existing path. We can't determine what is supported there, so consider all registered privileges
            Workspace workspace = jcrSession.getWorkspace();
            if (workspace instanceof JackrabbitWorkspace) {
                PrivilegeManager privilegeManager = ((JackrabbitWorkspace)workspace).getPrivilegeManager();
                supportedPrivileges = privilegeManager.getRegisteredPrivileges();
            }
        }
        return supportedPrivileges == null ? new Privilege[0] : supportedPrivileges;
    }

    protected Map<Privilege, PrivilegeItem> initialPrivilegesMap(
            Map<Privilege, String> privilegeToLongestPath, String acePath) {
        Map<Privilege, PrivilegeItem> newMap = new HashMap<>();
        Privilege[] supportedPrivileges = null;
        try {
            Session jcrSession = request.getResourceResolver().adaptTo(Session.class);
            supportedPrivileges = getSupportedOrRegisteredPrivileges(jcrSession,
                    PATH_REPOSITORY.equals(acePath) ? null : acePath );
        } catch (RepositoryException e) {
            //ignore
            supportedPrivileges = null;
        }
        if (supportedPrivileges != null) {
            for (Privilege privilege : supportedPrivileges) {
                PrivilegeItem p1 = new PrivilegeItem(privilege.getName(), false, false, privilegeToLongestPath.get(privilege));
                newMap.put(privilege, p1);
            }
        }
        return newMap;
    }

    protected Map<Privilege, PrivilegeItem> getPersistedPrivilegesMap() throws RepositoryException {
        if (persistedPrivilegesMap == null) {
            Session jcrSession = request.getResourceResolver().adaptTo(Session.class);
            Map<Privilege, String> privilegeToLongestPath = AceUtils.getPrivilegeLongestPathMap(jcrSession);
            String acePath = getAcePath();
            persistedPrivilegesMap = initialPrivilegesMap(privilegeToLongestPath, acePath);

            JsonObject ace;
            try {
                ace = getAce.getAce(jcrSession, acePath, getPrincipalId());
            } catch (ResourceNotFoundException rnfe) {
                // no ACE exists yet?
                ace = null;
            }
            if (ace != null) {
                aceExists = true;
                AccessControlManager acm = jcrSession.getAccessControlManager();

                //make a temp map for quick lookup below
                Set<RestrictionDefinition> supportedRestrictions = getSupportedRestrictions();
                Map<String, RestrictionDefinition> srMap = toSrMap(supportedRestrictions);

                JsonObject privileges = ace.getJsonObject("privileges");
                for (String pn : privileges.keySet()) {
                    Privilege p = acm.privilegeFromName(pn);
                    PrivilegeItem privilegeItem = persistedPrivilegesMap.computeIfAbsent(p, key -> new PrivilegeItem(key.getName(), false, false, privilegeToLongestPath.get(key)));

                    JsonObject privilegeObj = privileges.getJsonObject(pn);
                    JsonValue allowJsonValue = privilegeObj.get("allow");
                    if (allowJsonValue != null) {
                        privilegeItem.setAllowExists(true);
                        privilegeItem.setGranted(true);
                        if (allowJsonValue instanceof JsonObject) {
                            List<RestrictionItem> restrictionItems = jsonToRestrictionItems(srMap, (JsonObject)allowJsonValue);
                            privilegeItem.setAllowRestrictions(restrictionItems);
                        }
                    }
                    JsonValue denyJsonValue = privilegeObj.get("deny");
                    if (denyJsonValue != null) {
                        privilegeItem.setDenyExists(true);
                        privilegeItem.setDenied(true);
                        if (denyJsonValue instanceof JsonObject) {
                            List<RestrictionItem> restrictionItems = jsonToRestrictionItems(srMap, (JsonObject)denyJsonValue);
                            privilegeItem.setDenyRestrictions(restrictionItems);
                        }
                    }
                }
            }
        }
        return persistedPrivilegesMap;
    }

    protected List<RestrictionItem> jsonToRestrictionItems(Map<String, RestrictionDefinition> srMap,
            JsonObject restrictionsObj) {
        List<RestrictionItem> restrictionItems = new ArrayList<>();
        for (Entry<String, JsonValue> entry : restrictionsObj.entrySet()) {
            String rn = entry.getKey();
            RestrictionDefinition rd = srMap.get(rn);
            if (rd != null) {
                Object value = null;
                JsonValue jsonValue = entry.getValue();
                if (jsonValue instanceof JsonArray) {
                    JsonArray jsonArray = (JsonArray)jsonValue;
                    String [] values = new String[jsonArray.size()];
                    for (int i = 0; i < values.length; i++) {
                        values[i] = jsonArray.getString(i);
                    }
                    value = values;
                } else if (jsonValue instanceof JsonString) {
                    value = ((JsonString)jsonValue).getString();
                }
                restrictionItems.add(new RestrictionItem(rd, value, true));
            }
        }
        return restrictionItems;
    }

    public Set<RestrictionDefinition> getSupportedRestrictions() {
        Set<RestrictionDefinition> supportedRestrictions = new HashSet<>();
        for (RestrictionProvider rp : restrictionProviders) {
            supportedRestrictions.addAll(rp.getSupportedRestrictions(resource.getPath()));
        }
        return supportedRestrictions;
    }

    public List<RestrictionDefinitionInfo> getSupportedRestrictionsInfo() {
        return getSupportedRestrictions().stream()
            .map(rd -> new RestrictionDefinitionInfo(rd.getName(), rd))
            .sorted(Comparator.comparing(RestrictionDefinitionInfo::getDisplayName))
            .collect(Collectors.toList());
    }

    /**
     * Returns a structure with the aggregate privilege relationships that can
     * be used on the client side UI.
     * @return JSON representation of the privilege aggregations
     * @throws RepositoryException
     */
    public String getPrivilegeAggregationsAsJSON() throws RepositoryException {
        JsonBuilderFactory factory = Json.createBuilderFactory(Collections.emptyMap());
        JsonObjectBuilder builder = factory.createObjectBuilder();

        Session jcrSession = request.getResourceResolver().adaptTo(Session.class);
        Map<Privilege, String> privilegeToLongestPath = AceUtils.getPrivilegeLongestPathMap(jcrSession);
        Privilege[] supported = getSupportedOrRegisteredPrivileges(jcrSession, resource.getPath());
        for (Privilege privilege : supported) {
            Privilege[] aggregatePrivileges = privilege.getAggregatePrivileges();
            if (aggregatePrivileges != null && aggregatePrivileges.length > 0) {
                // order these so the client side iteration will process from the top down
                List<Privilege> list = new ArrayList<>(Arrays.asList(aggregatePrivileges));
                list.sort((Privilege o1, Privilege o2) -> privilegeToLongestPath.get(o1).compareTo(privilegeToLongestPath.get(o2)));

                JsonArrayBuilder aggregateArray = factory.createArrayBuilder();

                for (Privilege privilege2 : list) {
                    aggregateArray.add(privilege2.getName());
                }

                builder.add(privilege.getName(), aggregateArray);
            }
        }

        JsonObject jsonObj = builder.build();
        return jsonObj.toString();
    }

    public String getExistingRestrictionNamesAsJSON() throws RepositoryException {
        JsonObjectBuilder jsonObjBuilder = Json.createObjectBuilder();
        for (boolean forAllow : new boolean[] {true, false}) {
            JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
            Set<String> alreadyProcessed = new HashSet<>();
            for (PrivilegeItem pi : getPrivileges()) {
                if ((forAllow && pi.getGranted()) ||
                        (!forAllow && pi.getDenied())) {
                    List<RestrictionItem> restrictions = forAllow ? pi.getAllowRestrictions() : pi.getDenyRestrictions();
                    for (RestrictionItem ri : restrictions) {
                        if (ri.isExists()) {
                            String name = String.format("%s@%s", pi.getName(), ri.getName());
                            if (!alreadyProcessed.contains(name)) {
                                jsonArrayBuilder.add(name);
                                alreadyProcessed.add(name);
                            }
                        }
                    }
                }
            }
            jsonObjBuilder.add(forAllow ? "allow" : "deny", jsonArrayBuilder);
        }
        return jsonObjBuilder.build().toString();
    }

    public String getOrderValue() {
        String parameter = request.getParameter("order");
        return parameter == null ? "" : parameter;
    }

    public Collection<PrincipalPrivilege> getOrderList() throws RepositoryException {
        List<PrincipalPrivilege> list = new ArrayList<>();
        Session jcrSession = request.getResourceResolver().adaptTo(Session.class);
        PrincipalManager principalManager = ((JackrabbitSession)jcrSession).getPrincipalManager();
        String pid = getPrincipalId();
        JsonObject acl = getAcl.getAcl(jcrSession, getAcePath());
        for (String uid : acl.keySet()) {
            if (pid != null && pid.equals(uid)) {
                //skip it
                continue;
            }
            Principal principal = principalManager.getPrincipal(uid);
            if (principal != null) {
                PrincipalPrivilege pi = new PrincipalPrivilege(principal);
                list.add(pi);
            }
        }
        return list;
    }

    protected String [] fieldValuesFromReqParams(RequestParameter[] paramValues) {
        String[] fieldValues = null;
        fieldValues = new String[paramValues.length];
        for (int i=0; i < paramValues.length; i++) {
            fieldValues[i] = paramValues[i].getString();
        }
        return fieldValues;
    }

    /**
     * Gets the values of the specified field from the request.  If not
     * supplied the values from the supplied resource are returned.
     * 
     * @param keyPattern the regular expression to used to match against the field keys
     * @return the values of the field or empty map if not available
     */
    protected Map<String, String[]> getFieldValuesForPattern(Pattern keyPattern) {
        Map<String, String[]> resultMap = new HashMap<>();

        @NotNull
        RequestParameterMap requestParameterMap = request.getRequestParameterMap();
        Set<Entry<String, RequestParameter[]>> entrySet2 = requestParameterMap.entrySet();
        for (Entry<String, RequestParameter[]> entry : entrySet2) {
            String key2 = entry.getKey();
            if (!resultMap.containsKey(key2)) {
                Matcher matcher = keyPattern.matcher(key2);
                if (matcher.matches()) {
                    //just use the original request value
                    RequestParameter[] paramValues = entry.getValue();
                    if (paramValues != null) {
                        String[] fieldValues = fieldValuesFromReqParams(paramValues);
                        resultMap.put(key2, fieldValues);
                    }
                }
            }
        }

        return resultMap;
    }

}
