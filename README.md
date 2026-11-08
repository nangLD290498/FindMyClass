# FindMyClass

FindMyClass is a planned web application that connects teachers with students. Teachers can introduce themselves and offer courses, while students can discover teachers and register for courses that match their interests.

## Initial scope

The first version will support two roles: **teacher** and **student**.

### Teachers

- Sign in and complete a teacher profile on the first login before creating courses.
- Create courses with details including:
  - Course title, description, and subject.
  - Class times and meeting days.
  - Course start and end dates.
  - Maximum number of students.
  - Course price.
- Make their profile and courses available for students to browse.

### Students

- Sign in to discover teachers and their courses.
- View teacher profiles and course details, including subjects, schedules, prices, and student capacity.
- Register for courses that match their interests and availability.

## Main workflows

1. A teacher signs in for the first time and completes their profile.
2. The teacher creates a course with its subject, schedule, capacity, and price.
3. A student signs in and browses teachers and courses.
4. The student reviews a course and registers for it.


## Technology stack

- Frontend: React, TypeScript, Vite, React Router, TanStack Query, React Hook Form, and Zod.
- Backend: Java 21, Spring Boot, Spring Security OAuth2/OIDC, and Spring Data JPA.
- Database: Oracle, with Flyway migrations. Automated backend tests use H2 by default and can also run against a dedicated Oracle schema.

## Google sign-in and onboarding

1. The user selects **Continue with Google**. Spring Security handles the authorization code flow, state, nonce, and ID token validation.
2. The backend requires a verified Google email and creates or updates the account using Google's stable `sub` identifier. Email is contact information, not the account key.
3. A new account has no role and can access only account/onboarding operations.
4. The user chooses **Teacher** or **Student** once. Selection is saved in a transaction with a row lock; subsequent selection requests return `409 Conflict`.
5. Teachers complete a short profile (bio and subjects) before accessing their dashboard. Students go directly to their dashboard.
6. Returning users resume their onboarding step or dashboard. Logout invalidates the application session.

Roles and profile completion are enforced by the backend using database records. Google claims and browser route guards cannot grant application roles. Google tokens remain on the server; the browser uses an HTTP-only session cookie. Signing out ends the FindMyClass session, not the user's Google session.

## Local development

### Prerequisites

- A **JDK 21**, including `javac`, with `JAVA_HOME` pointing to it.
- Node.js 24 and npm.
- An accessible Oracle database (for example, Oracle Database Free) and a dedicated schema user with session/table/sequence creation permissions and a tablespace quota. Flyway creates the application's tables in that schema. Do not use a shared production schema for development.
- A Google Cloud OAuth client of type **Web application**.

### Local Oracle database

Docker can run a development-only Oracle Database Free instance in this environment or on your machine. The helper uses the community-maintained `gvenzl/oracle-free:23-slim` image and records its resolved digest on first setup. Docker verifies image layer checksums during download.

From the repository root:

```bash
python3 scripts/oracle_dev.py start
python3 scripts/oracle_dev.py status
```

Wait until health is `healthy`. The first startup downloads the image and initializes the database, which can take several minutes. The database listens only on local port **1521**, with service **FREEPDB1**. The application schema is `findmyclass`.

Passwords are generated locally and stored in the ignored `.local/oracle/` directory with restricted permissions. The helper injects credentials into child processes without printing them. Keep this directory alongside the Docker volume; do not commit it or delete it while retaining the database. The database files persist in Docker volume `findmyclass-oracle-data` across container restarts.

After configuring Google OAuth variables and local HTTP cookie settings, launch the backend with the generated database credentials:

```bash
python3 scripts/oracle_dev.py run -- ./backend/mvnw -f backend/pom.xml spring-boot:run
```

To run the existing backend integration suite against Oracle, initialize a separate test schema and use the helper's `test` command:

```bash
python3 scripts/oracle_dev.py init-tests
python3 scripts/oracle_dev.py test -- ./backend/mvnw -f backend/pom.xml verify
```

This overrides the test datasource to use `findmyclass_test`, while retaining simulated Google identities. The suite clears records in that test schema. Never point it at a schema containing application data. Ordinary `./mvnw verify` still uses H2.

To stop the database while retaining its data:

```bash
python3 scripts/oracle_dev.py stop
```

Run `start` again to restart the existing container. If the environment is recreated without its Docker volume, the database must be recreated; the helper does not provide remote backups.

### Configure Google

Configure the OAuth consent screen in Google Cloud. For an external application in testing, add your Google account to the test users. Request only `openid`, `profile`, and `email` scopes.

For the development setup below, register this exact **authorized redirect URI**:

```text
http://localhost:5173/login/oauth2/code/google
```

The browser uses Vite on port 5173. Vite proxies `/api`, `/oauth2`, and `/login/oauth2` to Spring Boot on port 8080 while preserving the browser host, so the callback returns through port 5173. Start sign-in from the React page. If you deliberately access the backend directly, register its separate callback URI as well.

### Start the backend

Use `backend/.env.example` as a list of required variables. Supply their values through your shell, IDE, or secure environment settings. Spring Boot does **not** automatically load `.env` files. Never commit credentials.

| Variable | Purpose |
|---|---|
| `DB_URL` | Oracle JDBC URL, such as `jdbc:oracle:thin:@localhost:1521/FREEPDB1` |
| `DB_USERNAME` | Dedicated application schema user |
| `DB_PASSWORD` | Database password |
| `GOOGLE_CLIENT_ID` | Google web OAuth client ID |
| `GOOGLE_CLIENT_SECRET` | Google OAuth client secret; backend only |
| `FRONTEND_URL` | Trusted frontend origin, `http://localhost:5173` for development; no trailing slash |
| `SESSION_COOKIE_SECURE` | `false` for local HTTP only; defaults to `true` |

From the repository root, after setting these variables:

```bash
cd backend
./mvnw spring-boot:run
```

Flyway applies the schema migration at startup; Hibernate validates the schema instead of modifying it. Database connections and Google network requests must be allowed by your environment. Google sign-in requires access to `accounts.google.com`, `oauth2.googleapis.com`, `openidconnect.googleapis.com`, and `www.googleapis.com`. Oracle JDBC also needs a supported network route to your database host and port; an HTTP allowlist alone does not provide JDBC connectivity.

### Start the frontend

In another terminal:

```bash
cd frontend
npm ci
npm run dev
```

Open the Vite development server in your browser. No Google secrets or database credentials belong in the frontend.

### API contract

| Method and path | Behavior |
|---|---|
| `GET /oauth2/authorization/google` | Begin Google sign-in by browser navigation |
| `GET /api/auth/csrf` | Return the session's CSRF header name and token |
| `GET /api/auth/me` | Return user details, role, profile status, and next onboarding route; `401` without a session |
| `POST /api/onboarding/role` | Set the initial role with `{"role":"TEACHER"}` or `{"role":"STUDENT"}` |
| `PUT /api/teacher/profile` | Save the signed-in teacher's `bio` and `subjects` |
| `GET /api/teacher/dashboard` | Require teacher role and a completed profile |
| `GET /api/student/dashboard` | Require student role |
| `POST /api/auth/logout` | Invalidate the session and return `204` |

All mutation requests require the CSRF header returned by `/api/auth/csrf`. Fetch a fresh token after login/logout. Invalid input returns `400`; insufficient permissions return `403`.

### Tests and build

Backend tests use test-only Google client values, simulated OIDC principals, and H2 in Oracle compatibility mode. They do not require real OAuth or database credentials:

```bash
cd backend
./mvnw verify
```

Frontend browser tests mock API responses to exercise UI behavior, including teacher/student onboarding, returning users, session expiration, errors, logout, and mobile layout:

```bash
cd frontend
npm ci
npm run build
npx playwright install chromium
npm test
```

If Chromium is already installed, use `CHROMIUM_PATH=/path/to/chromium npm test` instead of downloading a browser.

The default H2 tests and mocked browser tests do not establish compatibility with Oracle or complete a real Google login. Use the Oracle test command above to validate the migrations and integration suite against Oracle. Before deploying, also manually sign in with new teacher/student Google accounts, reload, sign out, and sign in again to verify the full OAuth flow.

### Deployment notes

Serve the React build and proxied backend routes under the same HTTPS origin. Route SPA navigation paths to `index.html`; send `/api/**`, `/oauth2/**`, and `/login/oauth2/**` to Spring Boot. Set `FRONTEND_URL` to the public origin, register the matching HTTPS Google callback, and keep secure cookies enabled. If TLS terminates at a reverse proxy, configure trusted forwarded-header handling so Spring builds the correct HTTPS callback; never trust arbitrary client-supplied forwarding headers.

Sessions currently live in one backend instance's memory and end when that instance restarts. Use shared session storage or appropriate session routing before scaling to multiple instances.

## Current implementation scope

Google sign-in, session-based authentication, one-time role onboarding, basic teacher profile completion, and protected dashboard shells are implemented. Course creation, browsing, enrollment, payments, and role changes are future work.
