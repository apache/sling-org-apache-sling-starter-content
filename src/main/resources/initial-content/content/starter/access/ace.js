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

// NOTE: this code expects 'aggregates' and 'existingRestrictionNames' variables
//    to be defined before this is script is invoked
if (typeof aggregates == 'undefined') {
    console.error("Expected 'aggregates' variable to be defined");
}
if (typeof existingRestrictionNames == 'undefined') {
    console.error("Expected 'existingRestrictionNames' variable to be defined");
}

/**
 * attach click handler for delete ACE button
 */
const deleteButton = document.getElementById("deleteButton");
if (deleteButton) {
    deleteButton.addEventListener('click', (e) => {
        e.preventDefault();
        document.getElementById("deleteAceForm").submit();
    });
}

/** reference to the dialog */
const restrictionsDialog = document.getElementById('restrictionsDialog');

/**
 * populate the specified restriction state in the restrictions dialog
 * @param rname the restriction name
 * @param rvalues the restriction values array
 */
const addRestrictionForPrivilege = (rname, rvalues) => {
    restrictionsDialog.querySelectorAll('p[data-restriction="' + rname + '"] input.declare-restriction').forEach((input) => {
        input.checked = true;
        // manually trigger the change handler
        input.dispatchEvent(new Event("change"));

        const p = input.closest("p");
        // remove the old rows
        p.querySelectorAll("span.restriction-value, span.restriction-values").forEach((span) => {
            span.remove();
        });
        // iterate rvalues to add new rows
        rvalues.forEach((rvalue) => {
            addRestrictionValueRowFn({ target: p }, rvalue);
        });
    });
};

/**
 * handler for links that open the restrictions modal dialog
 */
const showRestrictionsDialog = (e) => {
    e.preventDefault();
    if (typeof restrictionsDialog.showModal === "function") {
        // populate the dialog with the current state
        const c = e.target;
        const privilegeName = c.closest("tr").getAttribute('data-privilege');
        document.getElementById("for-privilege").textContent = privilegeName;

        const td = c.closest("td");
        const isAllow = "allow" == td.querySelector("input.granted_or_denied").value;
        const form = document.getElementById("modifyAceRestrictionsForm");
        form.setAttribute("data-privilege", privilegeName);
        form.setAttribute("data-for-privilege-allow", isAllow);

        // clear out the old dialog state
        restrictionsDialog.querySelectorAll('p.restriction-row').forEach((row) => {
            // uncheck it
            const checkbox = row.querySelector("input.declare-restriction");
            if (checkbox.checked) {
                checkbox.checked = false;
                // manually trigger the change handler
                checkbox.dispatchEvent(new Event("change"));
            }

            // reset the value rows back to a neutral state
            row.querySelectorAll("span.restriction-value, span.restriction-values").forEach((span) => {
                span.remove();
            });
            addRestrictionValueRowFn({ target: row }, "");
        });

        // fill in the current restriction details to a temp obj
        const rvaluesMap = {};
        // loop through once to collect all the current values
        td.querySelectorAll("span.restriction-state input").forEach((input) => {
            const restrictionRegex = /^restriction@([^@]+)@([^@]+)(@(Allow|Deny))?$/;
            const matches = restrictionRegex.exec(input.getAttribute("name"));
            if (matches) {
                const restrictionName = matches[2];
                if (restrictionName) {
                    const value = input.value;
                    if (value) {
                        if (!rvaluesMap[restrictionName]) {
                            rvaluesMap[restrictionName] = {
                                values: []
                            };
                        }
                        if (value) {
                            rvaluesMap[restrictionName]["values"].push(value);
                        }
                    }
                }
            }
        });

        // now loop through the collected items to do the work
        Object.keys(rvaluesMap).forEach((rname) => {
            const robj = rvaluesMap[rname];
            addRestrictionForPrivilege(rname, robj["values"]);
        });

        restrictionsDialog.showModal();
    } else {
        alert("Sorry, the <dialog> API is not supported by this browser.");
    }
};

/**
 * Attach the click handler to all the edit restrictions links
 */
document.querySelectorAll('span.editRestrictions a').forEach((link) => {
    link.addEventListener('click', showRestrictionsDialog);
});


/**
 * Attach click handler to the restrictions dialog ok button
 */
restrictionsDialog.querySelector("button#dlgOk").addEventListener("click", (e) => {
    e.preventDefault();

    const form = document.getElementById("modifyAceRestrictionsForm");
    if (form.reportValidity()) {
        // copy the dialog state to the restriction-state elements
        const privilegeName = form.getAttribute('data-privilege');
        // determine which column is being updated
        const tdIdx = form.getAttribute('data-for-privilege-allow') == "true" ? 2 : 3;
        const restrictionState = document.querySelector("table#privileges tr[data-privilege='" + privilegeName + "'] td:nth-of-type(" + tdIdx + ") span.restriction-state");
        // clear the old state
        restrictionState.replaceChildren();

        // fill in the current restriction details to a temp obj
        const rnames = new Set();
        // inject the new state
        restrictionsDialog.querySelectorAll("input").forEach((input) => {
            // if unchecked restriction is one that was existing before, then add
            //  a hidden field to remove the restriction
            if (input.classList.contains("declare-restriction") && !input.checked) {
                const restrictionName = input.parentElement.innerText.trim();
                const prname = privilegeName + "@" + restrictionName;
                const key = tdIdx == 2 ? "allow" : "deny";
                if (existingRestrictionNames[key].includes(prname)) {
                    //add a hidden input to remove the existing restriction
                    restrictionState.insertAdjacentHTML("beforeend", "<input type='hidden' name='restriction@" + prname + "@Delete' value='" + ("allow" == key ? 'allow' : 'deny') + "'/>");
                }
            }
            if (!input.disabled && input.name) {
                const clone = input.cloneNode();
                clone.type = "hidden";
                restrictionState.appendChild(clone);

                // keep track of the names for use later
                const restrictionRegex = /^restriction@([^@]+)@([^@]+)(@(Delete|Allow|Deny))?$/;
                const matches = restrictionRegex.exec(input.getAttribute("name"));
                const restrictionName = matches[2];
                if (restrictionName) {
                    rnames.add(restrictionName);
                }
            }
        });

        // now update the label of the corresponding 'edit restrictions' link'
        const restrictionDetails = document.querySelector("table#privileges tr[data-privilege='" + privilegeName + "'] td:nth-of-type(" + tdIdx + ") span.editRestrictions span.restriction-details");
        // clear out the old value
        restrictionDetails.replaceChildren();
        // insert the new value
        if (rnames.size == 0) {
            restrictionDetails.insertAdjacentText("beforeend", "No Restrictions");
        } else {
            restrictionDetails.insertAdjacentText("beforeend", Array.from(rnames).join(", "));
        }

        // all done, so close the dialog
        restrictionsDialog.close();
    }
});

/**
 * Attach click handler to the restrictions dialog cancel button
 */
restrictionsDialog.querySelector("button#dlgCancel").addEventListener("click", (e) => {
    e.preventDefault();
    restrictionsDialog.close();
});

/**
 * function that toggle the display of the restriction value input rows
 */
const applyRestrictionFn = (e) => {
    e.preventDefault();
    const c = e.target;
    const p = c.closest("p");
    p.querySelectorAll("input[type='text']").forEach((input) => {
        input.disabled = !c.checked;
    });
    p.querySelectorAll("span").forEach((span) => {
        span.style.display = c.checked ? '' : 'none';
    });
};

/**
 * Attach change handler to the restriction checkboxes
 */
document.querySelectorAll('input.declare-restriction').forEach((checkbox) => {
    checkbox.addEventListener('change', applyRestrictionFn);
});

/**
 * Helper to disable the delete restriction value row button if there is only one left
 */
const maybeDisableDeleteMultivalue = (p) => {
    // disable the delete button if there is only one row left
    const buttons = p.querySelectorAll("button.delete-multivalue");
    const form = document.getElementById("modifyAceRestrictionsForm");
    const canModify = form.getAttribute("data-canModify") == "true";
    buttons.forEach((button) => {
        button.disabled = !canModify || buttons.length == 1;
    });
};

/**
 * function that removes the restriction value row
 */
const removeRestrictionValueRowFn = (e) => {
    if (typeof e.preventDefault === "function") {
        e.preventDefault();
    }
    const c = e.target;
    const p = c.closest("p");
    if (p.querySelectorAll("span").length > 1) {
        c.closest("span").remove();
        maybeDisableDeleteMultivalue(p);
    } else {
        alert("Sorry, the last row can not be removed.");
    }
}

/**
 * function that adds a new restriction value row
 * @param e the triggering event
 * @param value (optional) the initial value of the new text input
 */
const addRestrictionValueRowFn = (e, value) => {
    if (typeof e.preventDefault === "function") {
        e.preventDefault();
    }
    const c = e.target;
    const p = c.closest("p");
    const form = c.closest("form");
    const canModify = form.getAttribute("data-canModify") == "true";
    const privilegeName = form.getAttribute('data-privilege');
    const suffix = form.getAttribute('data-for-privilege-allow') == "true" ? "Allow" : "Deny";
    const restrictionName = p.getAttribute('data-restriction');
    const multival = p.getAttribute("data-multival") == "true";
    const checked = p.querySelector("input.declare-restriction").checked;
    let row = '<span class="restriction-value' + (multival ? 's' : '') + '"' + (checked ? '' : ' style="display: none;"') + '>' +
        '<input type="text" name="restriction@' + privilegeName + '@' + restrictionName + '@' + suffix + '" value="' + (value || '').replaceAll('"', '&quot;') + '" required="required"' + (canModify && checked ? '' : ' disabled="disabled"') + ' /> ';
    if (multival) {
        row += '<button type="button" class="add-multivalue"' + (canModify ? '' : ' disabled="disabled"') + '>+</button> ' +
            '<button type="button" class="delete-multivalue">-</button>';
    }
    row += '</span>';
    p.insertAdjacentHTML("beforeend", row);
    if (multival) {
        const newRow = p.lastElementChild;
        newRow.querySelector("button.add-multivalue")
            .addEventListener('click', addRestrictionValueRowFn);
        newRow.querySelector("button.delete-multivalue")
            .addEventListener('click', removeRestrictionValueRowFn);
        maybeDisableDeleteMultivalue(p);
    }
}

/**
 * toggle the restrictions links when permission checkbox changes
 */
const setAggregatePrivilege = (privilegeName, val, checked) => {
    const btn = document.querySelector("input[name='privilege@" + privilegeName + "'][type=checkbox][value='" + val + "']");
    if ((checked && !btn.checked) ||
        (!checked && btn.disabled)) {
        let ancestorChecked = false;
        if (!checked) {
            // check if any ancestor checkboxes are still checked
            Object.keys(aggregates).every((pname) => {
                const pobj = aggregates[pname];
                if (pobj.includes(privilegeName)) {
                    ancestorChecked = document.querySelector("input[name='privilege@" + pname + "'][type=checkbox][value='" + val + "']").checked;
                }
                return !ancestorChecked;
            });
        }
        if (!ancestorChecked) {
            btn.checked = checked;
            btn.disabled = checked;

            const td = btn.closest("td");
            td.querySelectorAll("input.delete").forEach((input) => {
                input.disabled = checked;
            });
            td.querySelectorAll(".restriction-state input").forEach((input) => {
                input.disabled = !checked;
            });
            td.querySelector(".editRestrictions").style.display = checked ? '' : 'none';
        }
    }
};

/**
 * handler for changes to allow/deny privilege checkboxes
 */
const applyAggregatePrivilegeFn = (e) => {
    if (typeof e.preventDefault === "function") {
        e.preventDefault();
    }
    const c = e.target;
    const div = c.parentElement.parentElement;
    // toggle the disabled state of the hidden "delete" input (if any)
    div.querySelectorAll("input.delete").forEach((input) => {
        input.disabled = c.checked;
    });

    // toggle the disabled state of any restriction state inputs
    div.querySelectorAll(".restriction-state input").forEach((input) => {
        input.disabled = !c.checked;
    });

    // toggle the visibility of the edit restrictions link
    div.querySelector("span.editRestrictions").style.display = c.checked ? '' : 'none';

    // set/unset any affected aggregate privileges
    const privilegeName = c.closest("tr").getAttribute('data-privilege');
    const a = aggregates[privilegeName];
    if (a) {
        a.forEach((name) => {
            setAggregatePrivilege(name, c.value, c.checked);
        });
    }
};

/**
 * Attach the change handler to all the allow/deny privilege checkboxes
 */
document.querySelectorAll('input.granted_or_denied').forEach((checkbox) => {
    checkbox.addEventListener('change', applyAggregatePrivilegeFn);
    if (checkbox.checked) {
        // pre-process any checkboxes that are already checked
        applyAggregatePrivilegeFn({ target: checkbox });
    }
});
