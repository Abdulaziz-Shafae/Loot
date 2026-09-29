# Final QA checkpoint — 29 September 2026

Status: core authenticated MySQL browser flows and admin CRUD completed. The limitations below remain. No deployment performed.

## Final automated verification

- Maven `package` with Java 21: PASS; 17 tests, zero failures/errors/skips.
- Frontend `npm run build`: PASS.
- `npm audit --json` with system CA trust: zero known vulnerabilities.
- Automated backend tests use isolated H2; the browser checks below used the local MySQL backend.

## Already verified in the preceding browser run

Signup auto-login, session/CSRF renewal, responsive dashboard, pantry creation/editing and card/list views, system recipe filters, detail images and fallback, system-to-user conversion, cooking with exact pantry deductions, history, repeat cook, insufficient-stock handling, dashboard meal count refresh, user recipe creation and ingredient editing, live Leftover Rescue, live meal recommendations, and recipe generation/save.

## Defects corrected

- Insufficient-stock repeat-cook button no longer displays an endless busy label.
- Empty recipes no longer display “Ready to cook”.
- Generated recipe names accept Arabic; the generator prompt no longer forces English names.
- Ingredient Substitute rejects a response that names the original ingredient as its own substitute. Integration coverage includes this rejection.

The two latest AI service fixes need the locally running backend restarted before live browser regression checks.

## Remaining browser checks

- Delete only QA-created pantry items and user recipes.
- ADMIN overview/users and disposable ingredient/system-recipe CRUD, including recipe ingredients.
- USER rejection from admin pages/APIs in the browser (automated authorization coverage passes).
- Manual password change, old-password rejection, new-password login and session revocation behavior.
- Image → Ingredient live result and Ingredient Substitute live regression.
- Complete authenticated Arabic/RTL and Light/Dark/System checks, including admin and mobile overflow.

The user signed into a new QA account (`loot.12.test@example.com`). No passwords are stored in this report. The user authorized deleting QA-created records only and will perform password entry and admin login.

### Resumed browser results

- Image scanner: live image recognition returned Egg; edited quantity to six and saved successfully to the new account's pantry.
- Ingredient Substitute: live Chicken Rice / Chicken Breast request returned Egg, two pieces, with an explanation. The result differs from the ingredient being replaced.
- Deleted the six QA-created eggs through the confirmation dialog; pantry returned to empty.
- Created Arabic user recipe `وصفة اختبار الحذف`, verified its details and empty-ingredient cook guard, then deleted it through confirmation; My recipes returned to empty.
- USER navigation to `/admin` redirected to `/dashboard` and exposed no admin controls. API rejection remains verified by automated tests; browser API navigation was previously blocked by the browser client.
- Arabic pantry, recipes, recipe detail, history empty state, AI scanner and profile: translated controls, RTL, no document-level horizontal overflow at 360, 390, 768 and 1280 pixels. Stored English catalog names/descriptions remain English data.
- Authenticated theme switching: Light and Dark changed the rendered colors; System resolved to Dark, matching the browser's current OS preference. OS theme-change event behavior was not simulated.
- Password change submitted manually by the user: forced sign-out observed. After changing to a distinct password, an old-password login attempt showed the translated authentication error. New-password login returned to the authenticated dashboard. Explicit logout also returned to the login page. Source review confirms password changes increment authVersion, clear reset tokens, and invalidate the current session; SessionGuard rejects mismatched versions on subsequent requests. Concurrent independent sessions were not exercised in the browser. Admin checks remain pending.

Older QA-account data was not accessed or deleted. Cleanup above applies only to records created during this resumed run.

Known QA records from the preceding run: account display name `Loot QA`; pantry Chicken Breast, Rice, Egg, Milk, Onion and Cooking Oil; user recipes Chicken Rice (converted), QA Egg Bowl Edited, and Bayd Maftooq bil Haleeb (generated). Confirm ownership in the authenticated QA session before deletion; do not infer ownership from a matching name alone.

## Production prerequisites and unverified environment behavior

## Admin browser completion

- Fresh login required after the user manually promoted the account; admin access then succeeded.
- Arabic overview: 4 users, 37 ingredients, 12 system recipes, 2 cooked meals. Users page displayed four rows and translated roles; no users modified.
- Created `QA Admin Lentils`, renamed it `QA Admin Lentils Edited`.
- Created system recipe `QA Admin Lentil Bowl`, renamed it `QA Admin Lentil Bowl Edited`.
- Added the QA ingredient at 100 g, edited to 125 g, and removed the recipe ingredient through confirmation.
- Deleted only the new QA system recipe and new QA ingredient through confirmation. Final overview counts exactly matched the starting counts.
- Arabic admin overview/users/ingredients/recipes had no document-level overflow at tested mobile/tablet/desktop widths. English overview and Light/Dark/System switching also passed.
- No source changes were needed during this admin pass. The last post-edit automated verification remains 17 passing backend tests, successful Maven package/frontend build, and zero npm audit vulnerabilities.
- Evidence: [Arabic admin overview after cleanup](screenshots/admin-qa-complete.png).

The earlier signup failure was not reproduced or diagnosed: its failed request and current backend logs were unavailable. The account is now usable as ADMIN after manual promotion, but this does not establish the original failure's cause. Arabic HTTP 400 error text remains generic. Older QA-account data was left untouched. Independent concurrent-session revocation, direct browser admin API rejection, live SMTP, and production-environment checks remain unverified.

## Production environment checklist

- Use `SPRING_PROFILES_ACTIVE=prod`, a provisioned MySQL schema, `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`; production schema mode is `validate`.
- Set exact HTTPS `FRONTEND_ORIGINS`; verify proxy/HTTPS and Secure session cookies in the target environment.
- Configure server-side `OPENAI_API_KEY` and an accessible `OPENAI_MODEL`.
- Configure and verify `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `MAIL_AUTH`, and `MAIL_TLS` for password recovery. Live SMTP remains unverified.
- Verify schema migration against a backed-up staging MySQL database.
- Sessions/rate limits require shared state before horizontal scaling.

Production readiness is not signed off while the remaining authenticated checks and target-environment verification are open. Email verification is intentionally deferred.
