# Test Case 001 – Full Feature-Toggle Lifecycle

- **Feature:** Feature Toggle Lifecycle – Create, Assign, Evaluate, Modify and Delete
- **Priority:** High
- **Status:** Draft
- **Author:** abstratium
- **Date:** 2026-05-17

---

## Preconditions

See [PRECONDITIONS.md](./PRECONDITIONS.md).

Additional preconditions specific to this test:

- The user is authenticated and signed in.
- The application is accessible at the base URL.
- No pre-existing data for the entities created in this test is assumed (the test starts by deleting any such data if it exists).

---

## Test Objective

Verify the end-to-end lifecycle of feature toggles, including:

1. Cleaning up any pre-existing test data.
2. Creating stages, rules, toggles, and assignments.
3. Evaluating toggle values via the Toggle Tester for various stages, contexts, and client criteria.
4. Modifying an existing assignment and re-evaluating.
5. Cleaning up all created data and asserting it is fully removed.

---

## Test Data

| Entity | Name | Details |
|--------|------|---------|
| Stage | `temp` | No parent |
| Stage | `temp-child` | Inherits from `temp` |
| Rule | `catch-all` | No criteria |
| Rule | `new-feature-users` | `userId` matches regex `(alice\|bob)` |
| Toggle | `maintenance-window` | Context: `public` |
| Toggle | `feature-123` | Context: `public` |
| Assignment | `maintenance-window` / `temp` / `catch-all` | Value: `off` |
| Assignment | `feature-123` / `temp` / `new-feature-users` | Value: `off` |
| Assignment | `feature-123` / `temp-child` / `new-feature-users` | Value: `on` |

---

## Reusable Teardown Function

The following teardown steps are encapsulated in a reusable function `deleteAllTestData(page)` that can be called both at the start (pre-condition cleanup) and at the end (post-condition cleanup) of the test.

> **Constraint:** Navigation between pages MUST be performed only by clicking links in the application header or on-page navigation elements. Direct URL entry (except the initial base URL) is **not permitted**.

### `deleteAllTestData(page)` – Steps

```gherkin
Function deleteAllTestData:

  # --- Toggles page: remove assignments and toggle for "feature-123" ---
  Given the user navigates to the Toggles page by clicking the "Toggles" link in the header
  When the user searches for toggle "feature-123" in the toggles table
  And the toggle "feature-123" exists in the table
  Then the user clicks the "⚙️ Manage" button for toggle "feature-123"
  And the assignments panel opens showing the list of assignments
  And for each assignment row in the assignments table, the user clicks the "🗑️ Delete" icon
  And confirms each deletion in the confirmation dialog by clicking "Delete"
  And asserts that the assignments table is empty (shows "No stage & rule assignments yet")
  Then the user clicks the "Close" button to close the assignments panel
  And the user clicks the "🗑️ Delete" icon on the "feature-123" toggle row
  And confirms deletion in the confirmation dialog by clicking "Delete"
  And asserts that "feature-123" is no longer present in the toggles table

  # --- Toggles page: remove assignments and toggle for "maintenance-window" ---
  When the user searches for toggle "maintenance-window" in the toggles table
  And the toggle "maintenance-window" exists in the table
  Then the user clicks the "⚙️ Manage" button for toggle "maintenance-window"
  And the assignments panel opens
  And for each assignment row in the assignments table, the user clicks the "🗑️ Delete" icon
  And confirms each deletion in the confirmation dialog by clicking "Delete"
  And asserts that the assignments table is empty
  Then the user clicks the "Close" button to close the assignments panel
  And the user clicks the "🗑️ Delete" icon on the "maintenance-window" toggle row
  And confirms deletion in the confirmation dialog by clicking "Delete"
  And asserts that "maintenance-window" is no longer present in the toggles table

  # --- Rules page: remove rule "new-feature-users" ---
  Given the user navigates to the Rules page by clicking the "Rules" link in the header
  When the user locates the rule "new-feature-users" in the rules table
  And it exists
  Then the user clicks the "🗑️ Delete" icon for rule "new-feature-users"
  And confirms deletion in the confirmation dialog by clicking "Delete"
  And asserts that "new-feature-users" is no longer present in the rules table

  # --- Stages page: remove stage "temp-child" (child before parent) ---
  Given the user navigates to the Stages page by clicking the "Stages" link in the header
  When the user locates the stage "temp-child" in the stages table
  And it exists
  Then the user clicks the "🗑️ Delete" icon for stage "temp-child"
  And confirms deletion in the confirmation dialog by clicking "Delete"
  And asserts that "temp-child" is no longer present in the stages table

  # --- Stages page: remove stage "temp" ---
  When the user locates the stage "temp" in the stages table
  And it exists
  Then the user clicks the "🗑️ Delete" icon for stage "temp"
  And confirms deletion in the confirmation dialog by clicking "Delete"
  And asserts that "temp" is no longer present in the stages table
```

---

## Test Steps

```gherkin
Feature: Feature Toggle Full Lifecycle

  Background:
    Given the application is running and accessible at the base URL
    And the user is authenticated and signed in
    And the user is on the home page (Toggles page)

  Scenario: Full lifecycle – create, evaluate, modify, and clean up feature toggles

    # =========================================================
    # STEP 1 – Pre-condition cleanup
    # =========================================================
    When the reusable function "deleteAllTestData" is called
    Then all pre-existing test data is removed (or was already absent)
    And the toggles table does not contain "feature-123"
    And the toggles table does not contain "maintenance-window"
    And the rules table does not contain "new-feature-users"
    And the stages table does not contain "temp-child"
    And the stages table does not contain "temp"

    # =========================================================
    # STEP 2 – Create stages
    # =========================================================
    Given the user navigates to the Stages page by clicking the "Stages" link in the header
    When the user clicks "+ Add Stage"
    And fills in Stage Name with "temp"
    And leaves Parent Stage as "-- No Parent --"
    And clicks "Create Stage"
    Then a success toast appears
    And the stages table contains a row where Name is "temp" and Parent Stage is "-"

    When the user clicks "+ Add Stage"
    And fills in Stage Name with "temp-child"
    And selects "temp" as the Parent Stage
    And clicks "Create Stage"
    Then a success toast appears
    And the stages table contains a row where Name is "temp-child" and Parent Stage is "temp"

    # =========================================================
    # STEP 3 – Create rules
    # =========================================================
    Given the user navigates to the Rules page by clicking the "Rules" link in the header
    When the user clicks "+ Add Rule"
    And fills in Rule Name with "catch-all"
    And leaves the Criteria section empty
    And clicks "Create Rule"
    Then a success toast appears
    And the rules table contains a row where Name is "catch-all" and Criteria is "None (catch-all)"

    When the user clicks "+ Add Rule"
    And fills in Rule Name with "new-feature-users"
    And fills in Criteria Key with "userId"
    And fills in Criteria Value with "(alice|bob)"
    And clicks the "Add" button next to the criteria fields
    And the criteria tag "userId = (alice|bob)" appears in the criteria list
    And clicks "Create Rule"
    Then a success toast appears
    And the rules table contains a row where Name is "new-feature-users" and Criteria is "userId: (alice|bob)"

    # =========================================================
    # STEP 4 – Create toggle "maintenance-window"
    # =========================================================
    Given the user navigates to the Toggles page by clicking the "Toggles" link in the header
    When the user clicks "+ Add Toggle"
    And fills in Toggle Name with "maintenance-window"
    And fills in Context with "public"
    And clicks "Create Toggle"
    Then a success toast appears
    And the toggles table contains a row where Name is "maintenance-window" and Context is "public"

    When the user clicks the "⚙️ Manage" button for toggle "maintenance-window"
    Then the assignments panel opens for "maintenance-window"
    And the user clicks "+ Add Assignment"
    And selects Stage "temp" using the stage autocomplete
    And selects Rule "catch-all" using the rule autocomplete
    And fills in Value with "off"
    And clicks "Add Assignment"
    Then a success toast appears
    And the assignments table contains a row:
      | Stage | Rule      | Value |
      | temp  | catch-all | off   |

    # =========================================================
    # STEP 5 – Create toggle "feature-123"
    # =========================================================
    When the user clicks "Close" to close the assignments panel
    And the user clicks "+ Add Toggle"
    And fills in Toggle Name with "feature-123"
    And fills in Context with "public"
    And clicks "Create Toggle"
    Then a success toast appears
    And the toggles table contains a row where Name is "feature-123" and Context is "public"

    When the user clicks the "⚙️ Manage" button for toggle "feature-123"
    Then the assignments panel opens for "feature-123"

    And the user clicks "+ Add Assignment"
    And selects Stage "temp" using the stage autocomplete
    And selects Rule "new-feature-users" using the rule autocomplete
    And fills in Value with "off"
    And clicks "Add Assignment"
    Then a success toast appears
    And the assignments table contains a row:
      | Stage | Rule              | Value |
      | temp  | new-feature-users | off   |

    And the user clicks "+ Add Assignment"
    And selects Stage "temp-child" using the stage autocomplete
    And selects Rule "new-feature-users" using the rule autocomplete
    And fills in Value with "on"
    And clicks "Add Assignment"
    Then a success toast appears
    And the assignments table contains 2 rows:
      | Stage      | Rule              | Value |
      | temp       | new-feature-users | off   |
      | temp-child | new-feature-users | on    |

    # =========================================================
    # STEP 6 – Navigate to Toggle Tester via "Tester" header link
    # =========================================================
    Given the user navigates to the Toggle Tester page by clicking the "Tester" link in the header

    # --- Query set 1: stage "temp", context "public", userId = "bob" ---
    When the user selects stage "temp" from the Stage dropdown
    And fills in Context with "public"
    And leaves Toggle Name Filter empty
    And sets existing criterion "userId" to "bob"
    And clicks "Run Query"
    Then the results table is displayed with heading 'Results for stage "temp"'
    And the results table contains the following toggle values:

    | Toggle             | Resolved Value |
    |--------------------|----------------|
    | maintenance-window | off            |
    | feature-123        | off            |

    # --- Query set 2: stage "temp", context "public", userId = "alice" ---
    When the user sets existing criterion "userId" to "alice"
    And clicks "Run Query"
    Then the results table contains the following toggle values:

    | Toggle             | Resolved Value |
    |--------------------|----------------|
    | maintenance-window | off            |
    | feature-123        | off            |

    # --- Query set 3: stage "temp", context "public", userId = "charlie" ---
    When the user sets existing criterion "userId" to "charlie"
    And clicks "Run Query"
    Then the results table contains the following toggle values:

    | Toggle             | Resolved Value |
    |--------------------|----------------|
    | maintenance-window | off            |
    | feature-123        | off            |

    # --- Query set 4: stage "temp-child", context "public", userId = "bob" ---
    When the user selects stage "temp-child" from the Stage dropdown
    And sets existing criterion "userId" to "bob"
    And clicks "Run Query"
    Then the results table is displayed with heading 'Results for stage "temp-child"'
    And the results table contains the following toggle values:

    | Toggle             | Resolved Value |
    |--------------------|----------------|
    | maintenance-window | off            |
    | feature-123        | on             |

    # --- Query set 5: stage "temp-child", context "public", userId = "alice" ---
    When the user sets existing criterion "userId" to "alice"
    And clicks "Run Query"
    Then the results table contains the following toggle values:

    | Toggle             | Resolved Value |
    |--------------------|----------------|
    | maintenance-window | off            |
    | feature-123        | on             |

    # --- Query set 6: stage "temp-child", context "public", userId = "charlie" ---
    When the user sets existing criterion "userId" to "charlie"
    And clicks "Run Query"
    Then the results table contains the following toggle values:

    | Toggle             | Resolved Value |
    |--------------------|----------------|
    | maintenance-window | off            |
    | feature-123        | off            |

    # =========================================================
    # STEP 7 – Modify "maintenance-window" assignment value to "on"
    # =========================================================
    Given the user navigates to the Toggles page by clicking the "Toggles" link in the header
    When the user clicks the "⚙️ Manage" button for toggle "maintenance-window"
    Then the assignments panel opens for "maintenance-window"
    And the user clicks the "✏️ Edit" icon for the assignment row (Stage: "temp", Rule: "catch-all")
    And changes the Value field from "off" to "on"
    And clicks "Update"
    Then a success toast appears
    And the assignments table contains a row:
      | Stage | Rule      | Value |
      | temp  | catch-all | on    |

    # =========================================================
    # STEP 8 – Re-evaluate after modification
    # =========================================================
    Given the user navigates to the Toggle Tester page by clicking the "Tester" link in the header

    # --- Re-query: stage "temp", context "public" ---
    When the user selects stage "temp" from the Stage dropdown
    And fills in Context with "public"
    And leaves Toggle Name Filter empty
    And clicks "Run Query"
    Then the results table contains the following toggle values:

    | Toggle             | Resolved Value |
    |--------------------|----------------|
    | maintenance-window | on             |

    # --- Re-query: stage "temp-child", context "public" ---
    When the user selects stage "temp-child" from the Stage dropdown
    And clicks "Run Query"
    Then the results table contains the following toggle values:

    | Toggle             | Resolved Value |
    |--------------------|----------------|
    | maintenance-window | on             |

    # =========================================================
    # STEP 9 – Post-condition cleanup
    # =========================================================
    When the reusable function "deleteAllTestData" is called
    Then all test data is removed
    And the toggles table does not contain "feature-123"
    And the toggles table does not contain "maintenance-window"
    And the rules table does not contain "new-feature-users"
    And the stages table does not contain "temp-child"
    And the stages table does not contain "temp"
```

---

## Expected Results

- All entities (stages, rules, toggles, assignments) can be created via the UI.
- Navigating between pages is done exclusively via header links; no direct URL entry is used.
- The Toggle Tester correctly resolves toggle values based on stage inheritance and rule criteria.
- Modifying an assignment immediately affects the resolved value in the Toggle Tester.
- All created entities can be deleted via the UI, and their absence is confirmed in the respective tables.

---

## Acceptance Criteria

- [ ] Stage "temp" is created with no parent.
- [ ] Stage "temp-child" is created with "temp" as parent.
- [ ] Rule "catch-all" is created with no criteria.
- [ ] Rule "new-feature-users" is created with criterion `userId = (alice|bob)`.
- [ ] Toggle "maintenance-window" is created with context "public".
- [ ] Toggle "feature-123" is created with context "public".
- [ ] Assignment `maintenance-window` / `temp` / `catch-all` → value `off` is created.
- [ ] Assignment `feature-123` / `temp` / `new-feature-users` → value `off` is created.
- [ ] Assignment `feature-123` / `temp-child` / `new-feature-users` → value `on` is created.
- [ ] Toggle Tester returns correct values for all 6 initial query scenarios (see table above).
- [ ] Assignment for `maintenance-window` is updated to value `on`.
- [ ] Toggle Tester returns `on` for `maintenance-window` on both `temp` and `temp-child` after modification.
- [ ] All test entities are deleted and confirmed absent from their respective tables.

---

## Toggle Tester Assertion Summary

### Initial State (before modification)

| Stage      | Context | userId  | Toggle             | Expected Value |
|------------|---------|---------|--------------------|----------------|
| temp       | public  | bob     | maintenance-window | off            |
| temp       | public  | bob     | feature-123        | off            |
| temp       | public  | alice   | maintenance-window | off            |
| temp       | public  | alice   | feature-123        | off            |
| temp       | public  | charlie | maintenance-window | off            |
| temp       | public  | charlie | feature-123        | off            |
| temp-child | public  | bob     | maintenance-window | off            |
| temp-child | public  | bob     | feature-123        | on             |
| temp-child | public  | alice   | maintenance-window | off            |
| temp-child | public  | alice   | feature-123        | on             |
| temp-child | public  | charlie | maintenance-window | off            |
| temp-child | public  | charlie | feature-123        | off            |

> **Note on `feature-123` / `temp` / `alice` and `bob`:** The assignment is on stage `temp` with rule `new-feature-users`, value `off`. Both alice and bob match the rule, so the value is `off`. They are not `on` at this stage.

> **Note on `maintenance-window` / `temp-child`:** `temp-child` inherits from `temp`. The `catch-all` assignment on `temp` is inherited, so `maintenance-window` resolves to `off`.

### After modifying `maintenance-window` assignment value to `on`

| Stage      | Context | Toggle             | Expected Value |
|------------|---------|--------------------|----------------|
| temp       | public  | maintenance-window | on             |
| temp-child | public  | maintenance-window | on             |

---

## Notes

- The test relies on stage inheritance: `temp-child` inherits toggle assignments from parent stage `temp`.
- The `catch-all` rule has no criteria, so it matches every client context.
- The `new-feature-users` rule uses the regex `(alice|bob)` against the `userId` attribute.
- Deletion order matters: child stages must be deleted before parent stages. Toggle assignments must be deleted before the toggle itself. (The system may enforce referential integrity.)
- Navigation is strictly via header links: `Stages`, `Rules`, `Toggles`, `Tester`.

---

## Related Test Cases

- None yet.

---

## Tags

`lifecycle`, `stages`, `rules`, `toggles`, `toggle-tester`, `assignments`, `inheritance`, `regex`, `e2e`
