# Verification — 27 September 2026

## Builds and automated checks

- Backend: JDK 21, Maven wrapper `package` succeeds, including 12 tests with zero failures/errors. Production JAR: `Loot-web-back/target/Loot-0.0.1-SNAPSHOT.jar`.
- Frontend: actual `npm install` succeeds (audit reports zero vulnerabilities); `npm run build` succeeds with Vite 7.3.6. Output is in `Loot-web-front/dist`.
- Backend tests run on H2 in MySQL compatibility mode. They cover session login/logout/rotation, CSRF, CORS, ADMIN restrictions, registration hashing and protected fields, cross-user pantry/recipe access, nested ingredient ownership, pantry/recipe CRUD, cooking/deduction/history/conversion/repeat, reset expiry/attempts/one-use/session revocation, upload validation and rate limiting.
- AI integration tests exercise all five existing service flows and save actions with simulated upstream HTTP responses. These verify application integration, not live model quality or account access.

## Browser integration and visual checks

Used the in-app browser against Vite and the explicit H2 test launcher; API calls use the actual Spring controllers/services, with only email delivery mocked.

- Session login to the disposable ADMIN account and authenticated dashboard succeed.
- Added 500 g rice with a 50 g threshold through the pantry modal.
- Recipe availability changed to ready to cook.
- Cooking Simple Rice Bowl through the confirmation dialog reduced rice to 400 g, saved a 100 g history snapshot, and updated dashboard counts to one pantry item, one cookable recipe and one cooked meal.
- Admin overview returned the actual fixture counts: one user, two ingredients, one system recipe and one cooked meal.
- Public desktop light/dark and Arabic mobile layouts were visually inspected. Authenticated desktop light and Arabic RTL mobile (390 × 844) were also inspected; navigation, language switch and responsive layout worked.
- Browser console error query returned no errors during the authenticated flow.
- Screenshots: [desktop dashboard](screenshots/dashboard-desktop.jpg), [Arabic mobile dashboard](screenshots/dashboard-arabic-mobile.jpg).

## Environment-dependent checks still required

- Run the supplied migration against a backup/staging copy of the actual MySQL schema. H2 tests do not establish MySQL migration compatibility.
- Verify SMTP delivery and live OpenAI model access with your server credentials. Neither service was called live during QA.
- Verify HTTPS, reverse proxy, Secure cookies and exact CORS origins in the deployment environment. No deployment was performed.
- This was targeted browser verification, not an exhaustive browser/accessibility audit. Additional CRUD/security behaviors were checked by backend integration tests.
- Sessions and rate limits are per instance; use a shared store before horizontal scaling. Large catalogs need pagination and query optimization.

## Test data

The optional `BrowserTestApplication` is test-classpath-only, binds to 127.0.0.1, and is excluded from the production JAR. It seeds `admin@example.test` / `Kitchen12!`, a small ingredient catalog and one recipe. Its H2 data disappears when stopped. Normal startup never creates these accounts or fixtures. See the root README for exact launch commands.
