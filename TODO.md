# TODO

These TODOs are to be resolved by the developer, NOT THE LLM.

## Before Each Release

- upgrade all and check security issues in github
- update docs to describe the changes

## Today

- criteria is now a list not a map
- move the value from the rule to the assignment
  - fix the UIs use of those endpoints
  - check remaining classes do not start with Toggle if not necessary
- compile the backend and check for errors that need fixing. remember: rules have no value, toggle-stage-rules have a value. if you find something where you need to implement something else, stop and ask for help.
- add tests!
- is @Transactional implemented properly? there seem to be a lot and is it right on the level of service or better on boundary? or does that start a default tx anyway?

- **`e2e-tests/pages/TODO.page.ts`** — This file should be renamed and populated with actual page objects for abstoggle's UI once the feature toggle pages exist.
- **`e2e-tests/tests/happy.spec.ts`** — The test body is empty. Write actual e2e tests once feature toggle UI is implemented.




## Tomorrow

- add an evaluator endpoint for people who want that. add a quarkus test for it. give it a configurable cache.

- performance: rather than loading all toggles with all their content, load only the names and descriptions and let the user load details when needed

## Later (not yet necessary for initial release)





