## Specification-Driven Development File Placement

When creating or updating files related to specification-driven development, do not place them inside the `PreventForgettingMedicationAndroidApp` directory.

All specification-driven development files must be created and maintained at the repository root level or under root-level documentation/specification directories.

### Disallowed location

- `PreventForgettingMedicationAndroidApp/**`

### Allowed locations

- `/docs/**`
- `/specs/**`
- `/AGENTS.md`
- `/README.md`
- Other root-level directories explicitly intended for documentation or specifications

### Rules

- Do not create `requirements.md`, `product-spec.md`, `architecture.md`, `glossary.md`, `spec.md`, `acceptance.md`, `tasks.md`, `design.md`, `constraints.md`, or change-history files under `PreventForgettingMedicationAndroidApp`.
- If a specification-driven development file is needed, create it at the repository root or in a root-level directory such as `/docs` or `/specs`.
- If an existing instruction, template, or workflow would place specification files under `PreventForgettingMedicationAndroidApp`, override that behavior and use the repository root structure instead.
- Treat `PreventForgettingMedicationAndroidApp` as an application/source directory, not as the location for specification-driven development documents.

### Preferred structure

- `/docs/requirements.md`
- `/docs/product-spec.md`
- `/docs/architecture.md`
- `/docs/glossary.md`
- `/specs/<feature>/spec.md`
- `/specs/<feature>/acceptance.md`
- `/specs/<feature>/tasks.md`
- `/specs/<feature>/design.md`
- `/specs/<feature>/constraints.md`

Always keep specification-driven development artifacts separated from application source directories.