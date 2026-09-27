# Web API contract

All paths below begin `/api/v1`. The original route-by-route inventory is in ENDPOINT-INVENTORY.md. Existing matching, cooking, history, conversion, and AI routes are retained. Private reads and mutations now require a Spring Security session; no browser-supplied owner ID is trusted. IDs on create payloads are ignored. Resource ownership is checked on the backend, including nested recipe/history ingredient records.

## Authentication and session

| Method | Path | Input | Response/access |
|---|---|---|---|
| GET | `/user/csrf` | None | Public `{token, headerName}`; materializes CSRF token |
| POST | `/user/add` | `{name,email,password,phoneNumber}` | Public, CSRF required; always creates USER; returns message |
| POST | `/user/login` | `{email,password}` | Public, CSRF required; rotates session ID, saves SecurityContext; safe User profile |
| POST | `/user/logout` | None | Authenticated + CSRF; invalidates session |
| GET | `/user/me` | None | Current safe User profile |
| PUT | `/user/me` | `{name,phoneNumber}` | Updates current profile; email remains unchanged |
| PUT | `/user/me/password` | `{currentPassword,password}` | Changes password; revokes all sessions and outstanding reset code |
| POST | `/user/forgot-password` | `{email}` | Generic public response; email code, 10-minute expiry |
| POST | `/user/reset-password` | `{email,code,password}` | Public + CSRF; six-digit code, five attempts, one-time use |
| GET | `/user/get` | None | ADMIN only; safe User list |
| DELETE | `/user/delete/{id}` | None | ADMIN only; deletes user and their children atomically; cannot delete active admin account |

The old `/user/update/{id}` is replaced by `/user/me` and `/user/me/password`. Reset email/code path-variable endpoints are replaced with the two JSON-body endpoints, so reset codes no longer appear in URLs. Do not call the old routes.

Passwords/reset metadata/authVersion are excluded from JSON. Profile contains id/name/email/phoneNumber/role; IDs are used for resource actions, not displayed unnecessarily. Registration ignores role/id/password-reset fields supplied by clients. New passwords require 8–72 characters, uppercase/lowercase/number/symbol, and at most 72 UTF-8 bytes.

React first requests `/user/csrf`, holds the masked token in memory, and sends its named header on all mutations, including login. It refreshes after login/logout/password change. HttpSession stores the CSRF secret; default XOR/BREACH handling is retained. Cookies are HttpOnly, SameSite=Lax, Secure in the production profile, with a 30-minute idle timeout. Session credentials never enter localStorage. Every authenticated session is checked against account existence, role and authVersion, so password resets, account deletion and role changes invalidate old sessions.

## Existing CRUD paths retained

Each group has GET `/get`, POST `/add`, PUT `/update/{id}`, DELETE `/delete/{id}`. Inputs retain the existing entity fields; id and userId are read-only at the API boundary.

| Group | GET output | Write payload | Authorization |
|---|---|---|---|
| `/pantry` | Current user's PantryItem[] | ingredientId, quantity, lowStockThreshold | Owner; session sets userId |
| `/recipe` | Current user's UserRecipe[] | name, description, category, instructions, imageUrl | Owner; session sets userId |
| `/recipe/ingredient` | Ingredients of current user's recipes | userRecipeId, ingredientId, requiredQuantity | Owning recipe required |
| `/history` | Current user's CookingHistory[] including cookedAt/type | Existing history fields; userId from session | Owner |
| `/history/ingredient` | Current user's history ingredient snapshots | cookingHistoryId, ingredientId, usedQuantity, existing snapshot fields | Owning history required |
| `/ingredient` | Ingredient[] | name, unit | Authenticated read; ADMIN writes |
| `/system` | SystemRecipe[] | name, description, category, instructions, imageUrl | Authenticated read; ADMIN writes |
| `/system/ingredient` | SystemRecIng[] | systemRecipeId, ingredientId, requiredQuantity | Authenticated read; ADMIN writes |

Recipe creation responses additionally include `{message,id}` for ingredient editing. Other mutations preserve message responses. Ingredient units are `g`, `ml`, `piece`; categories are `Breakfast`, `Lunch`, `Dinner`, `Snack`. Recipe image URLs must be HTTPS or empty. Names accept Unicode letters, marks, digits, spaces, apostrophes and hyphens. Ingredient deletion/unit changes are rejected while referenced, avoiding broken scalar relationships. Recipe deletion removes ingredient links while preserving history snapshots.

## Matching, cooking and history

All original `/user/can/{listType}/{recipeId}`, `/missing/...`, `/system`, `/system/{category}`, `/user`, `/user/{category}`, `/possible/{system|user|history}/{category}`, `/almost/{system|user|history}/{category}`, `/low`, `/low/email`, `/cook/{listType}/{recipeId}`, `/cook/{listType}/{recipeId}/done`, `/history`, `/history/{category}`, `/history/{historyId}/repeat`, `/history/{historyId}/repeat/done`, `/system/{recipeId}/convert` routes remain. See original inventory for HTTP methods and DTO signatures. Empty legacy matching results can still be a message object rather than an array.

New GET `/user/availability/{type}` (`system` or `user`) returns `[{id,canCook,almost,missing}]`, composing the existing Java matching services. Almost means 1–3 missing ingredients, as in the original service. This avoids one browser request per recipe and supplies dashboard statistics. It is not a new implementation of matching. Existing catalog/pantry/history APIs supply all remaining dashboard values. Large catalogs still involve per-recipe queries; pagination/bulk query optimization is a future scalability improvement.

Cooking and repeat remain transactional Java operations. A pantry `@Version` field prevents silent lost updates on concurrent deductions: one conflicting write returns 409 and its transaction rolls back. React confirms the action, calls the backend and refreshes data; it never deducts stock.

## AI

All seven original POST routes remain: `/ai/image/to/ingredient` (multipart field `image`), `/ai/image/to/ingredient/add` (ImageToIngredientDTO), `/ai/ingredient/substitute/{recipeId}/{listType}/{ingredientId}`, `/ai/recipe/recommendation` (request string DTO), `/ai/leftover/rescue` (LeftoverDTO[]), `/ai/recipe/generator` (request string DTO), `/ai/recipe/generator/add` (GeneratedRecipeDTO). The five tools plus two save actions use the existing AIService. Substitution is recipe-aware, not a standalone free-text lookup. Recommendation/generated responses are rendered as text, never HTML. AI calls stay on the server.

Image analysis accepts matching JPEG/PNG/WEBP MIME and file signatures, maximum 5 MB; request maximum 6 MB. Leftovers are bounded to 30 validated entries; prompts to 2,000 characters. Numeric AI output is checked for finite values. Existing pantry/output validation is retained. HTTP connect/read timeouts are 10/60 seconds. Upstream errors return safe 503 responses.

## Admin, errors and limits

GET `/admin/overview` returns actual counts `{users,ingredients,recipes,cooks}` and requires ROLE_ADMIN. Catalog management reuses existing APIs. Admin does not gain access to another user's pantry/history through personal APIs.

Safe errors use `{message}` with 400/401/403/404/409/413/429/503/500 as appropriate; existing controller field validation strings are also safe and handled by the client. No exception text/SQL/credentials are returned. Error logs record exception class only.

In-memory bounded rate limits: login 10 per email /15 min, reset email 3 per email /15 min, reset verification 10 per email /15 min plus five attempts per code; sensitive POSTs 40 per IP /15 min; AI 30 per IP and 20 per user /15 min. Limits reset on restart and apply per instance. Deploy one instance initially; add a shared limiter/session store before horizontal scaling. Forwarded client IPs are not trusted automatically. A trusted reverse proxy should apply its own per-client limit.

Security configuration follows [Spring's session persistence guidance](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html) and [CSRF token endpoint guidance](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html). CORS accepts only the explicitly configured origins and credentials; no wildcard origin is allowed.
