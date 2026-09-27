# Loot | لوت

A Spring Boot + React smart pantry application, built from the supplied backend and logo assets. One repository, two applications:

- `Loot-web-back`: Java 21, Spring Boot 4.1.1, JPA, MySQL, Spring Security sessions, Mail and existing OpenAI integration.
- `Loot-web-front`: React, Vite, React Router, Axios, Lucide and plain CSS. English/Arabic with mirrored RTL; System/Light/Dark themes.

## Implemented

Public landing/about/features, login, registration, email-code reset, dashboard with real counts, pantry card/list CRUD, system/my recipe filters, recipe detail and ingredient editor, cooking/conversion, repeatable cooking history, profile/password settings, all five Loot AI tools, and role-protected admin overview/users/ingredients/system recipes. Supplied branding is used in the header, hero and favicon. Category illustrations and broken-image fallbacks keep cards stable.

The existing Java matching, pantry deduction, history snapshots, conversion, email and AI logic remain server-side. Security adds BCrypt, server-side ownership/role checks, session fixation protection, CSRF, configurable CORS, safe errors, reset expiry/attempts, rate limits and upload checks. Only language and theme preferences are stored locally.

Analysis and complete original route inventory: [architecture](docs/ARCHITECTURE.md), [inventory](docs/ENDPOINT-INVENTORY.md). Final contracts and changed routes: [API.md](docs/API.md).

## Database and migration

Create a MySQL database named `loot` (utf8mb4) and an application database user. Supply its credentials through environment variables. No real secrets are committed.

For an existing database, stop the old backend, back it up, and run [001-web-security.sql](docs/migrations/001-web-security.sql) once. It adds user role/reset/auth-version metadata, recipe image URLs, pantry optimistic locking, and widens history instructions to TEXT. The original nine-table scalar-ID model is retained. Review any pre-existing orphan ingredient references before production use.

For a fresh development database, use `DDL_AUTO=update` for the first start. Switch to `validate` after schema creation. Production always uses `validate`; the migration is explicit, not automatically destructive.

Legacy plaintext passwords are deliberately unsupported. The SQL migration removes them and marks those accounts for password reset. Configure SMTP so users can use Forgot Password, or recreate disposable development accounts. There is no fallback plaintext comparison. Promote an existing admin manually using the commented SQL example; registration can never choose ADMIN.

## Configuration

Use [.env.example](.env.example) as a checklist and set values in the terminal/IDE environment. Spring Boot does **not** automatically read `.env` files.

| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL connection |
| `DDL_AUTO` | `update` for initial fresh development schema, otherwise `validate` |
| `FRONTEND_ORIGINS` | Comma-separated exact origins; development `http://localhost:5173` |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | SMTP configuration |
| `MAIL_AUTH`, `MAIL_TLS` | SMTP authentication and STARTTLS; defaults true |
| `OPENAI_API_KEY` | Server-only API key |
| `OPENAI_MODEL` | Defaults to model from the supplied backend; choose a model supported by your API account |
| `SESSION_COOKIE_SECURE` | False only for local HTTP; production profile forces true |
| `SPRING_PROFILES_ACTIVE` | Set `prod` in production |
| `VITE_API_BASE_URL` | Optional public frontend API base; defaults `/api/v1` |

Keep frontend and API on the same origin in production using a reverse proxy: `/api` to Spring Boot, all other application routes to Vite's built `index.html`. This fits SameSite=Lax sessions. Serve HTTPS. Configure the actual frontend origin; never `*`. Frontend environment variables must never contain database, mail or OpenAI secrets.

## Run the backend (PowerShell)

Requires JDK 21 and MySQL. From this repository:

```powershell
cd Loot-web-back
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:DB_URL = 'jdbc:mysql://localhost:3306/loot'
$env:DB_USERNAME = 'loot'
$env:DB_PASSWORD = '<your database password>'
# Fresh development database only:
$env:DDL_AUTO = 'update'
# Set SMTP and OPENAI_API_KEY using the configuration table above.
.\mvnw.cmd spring-boot:run
```

Backend: `http://localhost:8080`. macOS/Linux equivalent: `./mvnw spring-boot:run` with exported variables. The supplied Windows wrapper's null Target-array check was fixed for regular `.m2` directories.

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
java -jar target/Loot-0.0.1-SNAPSHOT.jar
```

Tests use an isolated H2 database in MySQL compatibility mode. They never require or modify your MySQL database. On networks whose CA is installed in Windows but not Java, use `MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NUL`; do not disable TLS verification.

## Run the frontend

Requires Node 22.12+ or a current Node 24 release and npm. In a second terminal:

```powershell
cd Loot-web-front
npm install
npm run dev
```

Open `http://localhost:5173` (the configured development CORS origin). Vite proxies `/api` to localhost:8080. Production build:

```powershell
npm run build
npm run preview
```

`preview` serves the built frontend; use the production reverse proxy or configure a backend origin for API calls when previewing. Prefer `npm run dev` for connected local development. Browser sessions are HttpOnly cookies; Axios supplies credentials and the in-memory CSRF header.

## Optional disposable browser test backend

```powershell
cd Loot-web-back
.\mvnw.cmd '-Dspring-boot.run.main-class=com.example.loot.BrowserTestApplication' spring-boot:test-run
```

This explicit **test-classpath-only** launcher uses H2, mocked email delivery, and a tiny fixture catalog. It creates `admin@example.test` / `Kitchen12!` for local QA only. It is never packaged into the production JAR, and normal application startup has no seeded accounts or mock data. Stop it before starting the real MySQL backend on port 8080. The test fixture database is discarded on shutdown. AI calls still require real configuration except in automated tests, where the upstream HTTP responses are simulated.

## Verification and deployment limits

See [VERIFICATION.md](docs/VERIFICATION.md) for results and boundaries. Live OpenAI billing/model access, SMTP delivery and a production MySQL migration need verification with your environment. Rate limiting and HTTP sessions are currently per instance; configure a shared store before running multiple backend instances. Large catalogs would benefit from pagination and bulk availability queries. No deployment has been performed.
