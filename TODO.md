# TODO

These TODOs are to be resolved by the developer, NOT THE LLM.

## Before Each Release

- upgrade all and check security issues in github
- update docs to describe the changes

## Today


- TEST THIS: add e2e that ensure you cannot delete stages, toggles or rules if they are in assignments.

- rename "rule_value" in TSR to just "value" or "toggle_value"? "rule" is not right, "value" is a mare in terms of refactoring later because its a very generic term.

- add an evaluator endpoint for people who want that - i.e. it simply takes the context and outputs the toggle value. add a quarkus test for it. give it a configurable cache.



## Tomorrow

- performance: rather than loading all toggles with all their content, load only the names and descriptions and let the user load details when needed

## Later (not yet necessary for initial release)





