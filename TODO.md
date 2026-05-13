# TODO

These TODOs are to be resolved by the developer, NOT THE LLM.

## Before Each Release

- upgrade all and check security issues in github
- update docs to describe the changes

## Today

- Stage Inheritance
- add tests!
- is @Transactional implemented properly? there seem to be a lot and is it right on the level of service or better on boundary? or does that start a default tx anyway?
- change package name from demo


- [ ] - Update SECURITY_DESIGN.md with project-specific information
- [ ] - Replace `src/main/webui/src/app/demo` with project-specific components
- [ ] - Update database migration files
- [ ] - remove all references to `demo` in the entire project
- [ ] - remove all files with `demo` in their name

- **Database migration files** — The existing migration files use demo/baseline table structures. Update them to reflect the abstoggle feature toggle schema once that is designed.
- **`e2e-tests/pages/TODO.page.ts`** — This file should be renamed and populated with actual page objects for abstoggle's UI once the feature toggle pages exist.
- **`docs/ephemeral-and-volatile-and-temporary-but-interesting/`** — The files in this directory still reference `abstracore` as the service name in example Loki queries. Update once the observability stack is configured for abstoggle.
- **`e2e-tests/tests/happy.spec.ts`** — The test body is empty. Write actual e2e tests once feature toggle UI is implemented.




## Tomorrow

- performance: rather than loading all toggles with all their content, load only the names and descriptions and let the user load details when needed

## Later (not yet necessary for initial release)





