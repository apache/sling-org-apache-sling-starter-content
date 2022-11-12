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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.jackrabbit.oak.spi.security.authorization.restriction.RestrictionDefinition;

public class RestrictionItem {
    private RestrictionDefinition rd;
    private Object value;
    private boolean exists;
    private long depth;
    private String extraCssClasses;

    public RestrictionItem(RestrictionDefinition rd, Object value, boolean exists) {
        super();
        this.rd = rd;
        this.value = value;
        this.exists = exists;
    }

    public String getName() {
        return rd.getName();
    }

    public boolean isMultiValue() {
        return rd.getRequiredType().isArray();
    }

    public String getValue() throws RepositoryException {
        String v = null;
        if (value instanceof Value) {
            v = ((Value)value).getString();
        } else if (value instanceof Value[]) {
            Value[] va = (Value[])value;
            if (va.length > 0) {
                v = va[0].getString();
            }
        } else if (value instanceof String) {
            v = (String)value;
        } else if (value instanceof String[]) {
            String[] values = (String[])value;
            if (values.length > 0) {
                v = values[0];
            }
        }

        if (v == null) {
            //empty string if no values yet
            v = "";
        }
        return v;
    }

    public List<String> getValues() throws RepositoryException {
        List<String> values = new ArrayList<>();
        if (value instanceof Value) {
            values.add(((Value)value).getString());
        } else if (value instanceof Value[]) {
            Value[] va = (Value[])value;
            for (Value v : va) {
                values.add(v.getString());
            }
        } else if (value instanceof String) {
            values.add((String)value);
        } else if (value instanceof String[]) {
            values.addAll(Arrays.asList((String[])value));
        }
        if (values.isEmpty()) {
            //empty string if no values yet
            values.add("");
        }
        return values;
    }

    public boolean isExists() {
        return exists;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
    }

    public boolean isMandatory() {
        return rd.isMandatory();
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

    public void setDepth(long depth) {
        this.depth = depth;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RestrictionItem [name=");
        builder.append(rd.getName());
        builder.append(", value=");
        try {
            builder.append(getValues());
        } catch (RepositoryException e) {
            // ignore
        }
        builder.append(", exists=");
        builder.append(exists);
        builder.append(", depth=");
        builder.append(depth);
        builder.append(", extraCssClasses=");
        builder.append(extraCssClasses);
        builder.append("]");
        return builder.toString();
    }

}
