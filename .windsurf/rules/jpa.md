---
trigger: glob
globs: **/service/*.java
---
## Envers Constraints

All JPA entities in this project are audited with `@Audited`. This imposes the following constraints:

**JQL Restrictions:**
- No bulk `DELETE WHERE` or `UPDATE WHERE` - use iterative operations
- Audit queries cannot traverse relations (only ID constraints allowed)

**Audit Table Schema:**
- Primary key: `(entity_id, REV)` composite
- Foreign key: `REV -> REVINFO(REV)` mandatory
- Columns: `REV BIGINT NOT NULL`, `REVTYPE TINYINT`, all entity columns nullable

**Unsupported Mappings:**
- Bags (non-unique `List`) - use `@IndexColumn` or `@CollectionId`
- `@OneToMany` + `@JoinColumn` - requires `@AuditJoinTable` for audit join table
- Collections of components