# TODO

These TODOs are to be resolved by the developer, NOT THE LLM.

## Before Each Release

- upgrade all and check security issues in github
- update docs to describe the changes

## Today


- test what happens with change notes if not configured.
  - make notes inline, regardless. get rid of the shitty dialog.

- provide endpoints to query history of toggles, rules and stages and TSRs => finish compiling and testing.

- not all fields are shown, e.g. toggle#description is missing: 
    name=asdf, enabled=true, context=

- performance of history: are there indices on revinfo for searching, and on aud tables for searching by rev number and on aud tables for searching by id?


## Tomorrow


## Later (not yet necessary for initial release)

- performance: rather than loading all toggles with all their content, load only the names and descriptions and let the user load details when needed



