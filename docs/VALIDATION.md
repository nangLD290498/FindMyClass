# Validation Record — First MVP Increment

Date: 20 November 2026.

| Check | Outcome | What it establishes |
|---|---|---|
| Frontend production build | Passed | TypeScript compilation and Vite production bundling |
| Frontend formatting | Passed | Repository Prettier checks |
| Backend `verify` on H2 | 16 passed, 0 failed/errors/skipped | Authentication, authorization, course lifecycle/search, input validation, consent, duplicate applications, private CRM notes, cancellation, source analytics, concurrent seat confirmation, concurrent configured-limit enforcement |
| Backend `verify` on dedicated Oracle schema | 16 passed, 0 failed/errors/skipped | V1/V2 migration, schema validation, and the same backend workflows on Oracle |
| Existing mocked Playwright regression suite | 10 passed | Original login/onboarding/session/logout/error/mobile UI behavior |
| Browser-to-real-backend suite | 2 passed | Teacher creates/publishes a course; student visits a source-tagged teacher page, applies; teacher confirms with a private note; student sees confirmation after reload without seeing the note; analytics, QR, profile editing, plan display, marketplace filtering, and mobile layout; anonymous/private routing and hidden-course access |
| Production package contents | Passed | Test login fixture and test classes are absent from the production jar |
| Live Oracle-backed app | Passed | Startup/migration; public courses and CSRF return 200; anonymous teacher API returns 401; test-login URL is unavailable to anonymous clients (401) |
| Real Google account sign-in | Not performed | Browser tests use explicitly simulated OIDC identities; actual Google authentication remains a separate manual check |
| Production deployment/load test | Not performed | No deployment or performance target was approved |

The Oracle tests use `findmyclass_test`, not the application's `findmyclass` schema. The full-stack browser tests use a separate H2 process, actual HTTP APIs and CSRF/session handling; course/application responses are not mocked. Its identity fixture lives only in test sources and requires explicit opt-in. Full-stack screenshots contain synthetic test data.

Reproduction commands and remaining product decisions are in [IMPLEMENTATION.md](IMPLEMENTATION.md) and [BRD.md](BRD.md). This record does not claim unresolved product rules, Pro billing, notifications, or moderation have been implemented.
