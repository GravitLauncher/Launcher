# Repository guidance

## Code changes

- Keep code clean, readable, and consistent with the surrounding codebase.
- Follow existing project abstractions and module boundaries. Prefer extending the established design over duplicating behavior or bypassing abstractions.
- Consider performance in hot paths and avoid unnecessary work, allocations, and I/O.
- Apply security checks at trust boundaries: validate inputs, enforce authorization, and handle secrets and errors safely.
- If a change requires breaking compatibility, state that explicitly in its description and explain the impact. A breaking change can be preferable to a messy hack or abstraction bypass; use common sense and balance the tradeoffs.

## Build and verification

- Prefer the installed `gradle` command when available and its version is 9.x or newer.
- If Gradle is unavailable, or the installed version is lower than 9.x, use the repository's `./gradlew` wrapper.
- Use `gradle installDist` (or `./gradlew installDist` when using the wrapper) to create executables.
- Use `gradle test` (or `./gradlew test` when using the wrapper) to run tests.
- Run the relevant build, checks, and tests for the change when practical, and report what was run and any failures.

## Commits and pull requests

- Prefix commit subjects with one of: `[FEATURE]`, `[FIX]`, `[REFACTOR]`, or `[DOCS]`, according to the change. Use `[ANY]` for merge commits and dependency updates.
- For commits created with AI assistance, include a `Co-authored-by` trailer identifying the AI co-author.
- PR text must be reviewed by a human before the pull request is submitted or published.
