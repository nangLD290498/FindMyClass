# MVP Implementation — First Increment

Date: 20 November 2026. The BRD remains a draft. This document records the implementation baseline used to make the confirmed feature groups testable; it does not mark unanswered BRD questions as approved.

## Included

- Existing Google sign-in, role selection, and teacher onboarding retained.
- Vietnamese UI for all new screens, using the existing visual style.
- Teacher course drafts, editing, publishing, hiding, and permanent archiving.
- Course subjects, levels, areas, online/offline delivery, public venue/platform name, tuition per course in VND, capacity, date ranges, and multiple weekly meeting slots.
- Public teacher pages with biography, subjects, published courses, source-tagged share links, downloadable QR codes, and profile editing.
- Public marketplace with paginated results and combined filters for keyword (title/subject/teacher name), subject, area, mode, tuition range, and weekday. Newest published courses appear first. Filters are represented in the URL.
- Course detail and authenticated student applications, with explicit consent to share contact details with the course owner.
- Student application list, current status, schedule, and cancellation before attending.
- Teacher CRM with course/status filters, contact information, private notes, and append-only status/note history. Students cannot read teacher notes or history actor identities.
- Teacher analytics with date filters, total recorded page loads, applications, currently confirmed/attending applications, conversion percentage, and source breakdown.
- Current Free plan usage display and configurable server-enforced course limit. Pro is explicitly unavailable until pricing and entitlements are settled; there is no payment or upgrade action.
- Flyway V2 migration with InnoDB foreign keys, uniqueness constraints, and status/capacity checks. Course descriptions and CRM notes use utf8mb4 columns sized to match input validation.

## Working rules requiring product confirmation

These are pilot defaults, not answers entered into the BRD:

| BRD references | Implemented baseline |
|---|---|
| Q01–Q03 | Public browsing; applications require an existing Student account. Existing Google-only, one-role-per-account policy remains. Parent/child accounts are deferred. |
| Q04 | Applications begin as `NEW`. Teachers can move New → Contacted/Confirmed/Rejected/Cancelled; Contacted → Confirmed/Rejected/Cancelled; Confirmed → Attending/Cancelled; Attending → Cancelled. No backward transitions. Teachers may append notes without changing status. |
| Q05 | Only Confirmed/Attending consume seats. Confirmation and creation serialize through a database course-row lock. Duplicate applications for the same student/course are rejected, including after cancellation/rejection. Cancellation releases a confirmed seat. No waitlist. Applications close when full, hidden, archived, or past the course end date. |
| Q06 | Multiple non-overlapping weekly slots, Monday=1 through Sunday=7, Vietnam time. Dates are inclusive. No overnight slots, holiday exceptions, makeup sessions, or conflict detection across different courses. Once seats are confirmed, schedule/date/location/mode/tuition changes are rejected pending a notification/change policy. Capacity cannot fall below confirmed enrollment. |
| Q07 | Tuition is per whole course, denominated in VND; zero is permitted. Subject, level, and area are bounded free-text fields. |
| Q08 | Stable public teacher URLs use account IDs. No custom slugs, image uploads, qualification checks, or theme editor. QR generation is local; links are not sent to third-party QR services. Location/platform names are public; the form warns against entering private meeting URLs/passwords. |
| Q09 | Subject/area use case-insensitive partial text matching, without geographic radius or address geocoding. Search is not accent-insensitive. Schedule filtering is by weekday, not time range. |
| Q10–Q11 | Complete-profile teachers can publish without moderation. Draft/Hidden/Archived details are not publicly accessible, including via direct links. Existing applicants retain their own application/course details. Archive is irreversible and retains records; no hard delete. Ended published courses remain browsable but cannot accept new applications. |
| Q12 | Student tracking and cancellation are included. Teachers cannot manually enroll students, edit submitted contact details, or export personal data. Notifications are on-screen only, plus status/history after refresh; no email/Zalo/SMS delivery. |
| Q13 | `utm_source` is carried from teacher pages to course links and application submission. The current application URL determines attribution; absent/invalid sources become `direct`. Source names allow letters, digits, `_`, `.`, `-` up to 80 characters. Sources are lowercased. No cross-session first-touch tracking or referrer inference. |
| Q13 | Views count browser-generated tracking events, not unique people; signed-in owner views are ignored. StrictMode duplicate effects are guarded in the UI. No robust bot filtering or fraud-resistant analytics. Application source is recorded at submission. |
| Q13/Q17 | Default analytics period is the last 30 calendar days in Vietnam, inclusive. Maximum range is 366 days. Applications are grouped by submission date; confirmed totals use their current status. Cancelled/rejected applications remain in the application denominator. Source rows sum to displayed totals. Teacher-page and course-page views are combined. |
| Q14 | Every teacher currently uses Free. `FREE_COURSE_LIMIT=-1` means no limit; a nonnegative value limits non-archived courses, including drafts/hidden courses. A teacher-row lock prevents concurrent creation from bypassing the limit. No Pro sale, billing, grace period, renewal, or downgrade implementation. |
| Q15 | New screens are Vietnamese, as selected by the product owner. The previously requested English login introduction remains English. |
| Q16 | Public DTOs omit teacher email and student information. Student contact details are available only to that student and the course-owning teacher. Teacher-private CRM notes are omitted from student responses. Retention/deletion and parent/minor-data policies remain open. |

## Run locally

Follow the README instructions for JDK 21, Node.js 24, MySQL, Google credentials, the backend, and the frontend. Run `npm ci` in `frontend` after pulling the updated lockfile. V2 runs automatically at backend startup. Back up a database containing useful data before applying migrations.

Development frontend: port 5173. Default backend: port 8080. `VITE_API_TARGET` can override the Vite development proxy target; it is a server-side development configuration, not a browser credential.

Use Google sign-in to create accounts and add courses through the application.

## Validation commands

```bash
# Backend integration suite (H2, simulated OIDC)
cd backend
./mvnw verify

# Same suite against the dedicated MySQL test database from the README
./mvnw verify

# Frontend production build and existing mocked UI regression tests
cd ../frontend
npm ci
npm run build
npx playwright install chromium
npm test

# Full browser → actual HTTP API → H2 flow; JDK 21 required
npm run test:fullstack
```

If Chromium is already installed, set `CHROMIUM_PATH=/path/to/chromium` instead of downloading it. On Windows, the full-stack helper is intended to run inside WSL/Linux. Maven honors its normal settings and `MAVEN_ARGS` if a local network proxy is needed; no environment-specific proxy address is committed.

The full-stack runner starts an isolated backend on loopback port 8081 and Vite on 5174, then stops them. Its test-only login fixture is under `backend/src/test` and requires an explicit JVM flag. It uses simulated OIDC sessions and runs course, enrollment, CRM, and analytics requests through the application stack and H2. Verify MySQL with the dedicated test database and verify Google consent and token exchange by signing in manually.

Full-stack screenshots are written to `frontend/test-results/screenshots/`. They contain synthetic test identities/data and are regenerated by the test run.

## Deferred / limitations

- Pro entitlements/pricing/billing; tuition payments; admin moderation and teacher verification.
- Parent/child identities, deletion/retention policy, privacy/terms content, and production compliance decisions.
- Email/Zalo/SMS, live notifications/polling, cancellation/reapplication policy changes, holiday/makeup scheduling.
- Custom page themes/slugs/images; reviews, advertising, AI, native apps.
- Pilot CRM lists and analytics aggregation currently load a teacher's records before filtering/aggregating; not optimized for large datasets. Production use needs pagination/aggregate queries, rate limiting, spam/bot controls, retention rules, hosting/backups, and a privacy review.
- Google OAuth still requires valid credentials, allowed destinations, correct callback URLs, and browser trust. No certificate verification bypass is introduced.

Use the open questions in `BRD.md` to replace these defaults with agreed product rules before a public launch.
