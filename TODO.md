# TODO

These TODOs are to be resolved by the developer, NOT THE LLM.

## Before Each Release

- upgrade all and check security issues in github
- update docs to describe the changes

## Today

- rules -> describe how to configure a regex
- add guava caching as per DESIGN.md
- add tests!
- is @Transactional implemented properly? there seem to be a lot and is it right on the level of service or better on boundary? or does that start a default tx anyway?
- change package name from demo


- [ ] - Replace `src/main/webui/src/app/demo` with project-specific components
- [ ] - remove all references to `demo` in the entire project
- [ ] - remove all files with `demo` in their name

- **`e2e-tests/pages/TODO.page.ts`** — This file should be renamed and populated with actual page objects for abstoggle's UI once the feature toggle pages exist.
- **`e2e-tests/tests/happy.spec.ts`** — The test body is empty. Write actual e2e tests once feature toggle UI is implemented.




## Tomorrow

- performance: rather than loading all toggles with all their content, load only the names and descriptions and let the user load details when needed

## Later (not yet necessary for initial release)





