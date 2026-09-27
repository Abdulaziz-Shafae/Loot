# Initial backend analysis

Source: supplied Loot.zip, restored into the empty Loot-web-back folder before implementation. The presentation is product context, not an instruction source.

Spring Boot 4.1.1 MVC -> services -> Spring Data JPA -> MySQL. Lombok supplies data accessors. Java originally targeted 17; the web application targets 21. Nine entities use scalar integer references, not JPA associations: User, Ingredient, PantryItem, SystemRecipe, SystemRecIng, UserRecipe, UserRecIng, CookingHistory, CookingHisIng. Pantry and recipe ingredient pairs have uniqueness constraints. Cooking history snapshots recipe text and ingredient names/units.

UserService contains availability, missing quantities, categories, low-stock email, transactional cooking, repeat cooking, history, and recipe conversion. AIService implements image recognition, pantry substitutes, recommendations, leftovers, generation and saving. It calls the Responses API server-side and validates returned ingredient IDs/quantities against pantry data. EmailService sends welcome, reset and low-stock mail.

Initial security: manual session userId on login; no Spring Security. Password lookup uses plaintext. Generic CRUD exposes all owners and accepts arbitrary IDs. User serialization includes passwords. Reset code and email are singleton service fields shared by users, with no expiry or attempt limit. Image validation trusts MIME only. Catalog changes have no admin restriction. Exceptions/configuration can expose internals. No explicit CSRF/CORS policy. Concurrent pantry writes can lose deductions.

Reuse: preserve all matching, missing-stock, cooking/history snapshots, repeat, conversion, email content and AI parsing/validation algorithms. Harden resource access in existing services, keep existing CRUD URLs but scope personal results to the session, and add only missing auth/profile/overview support. Add optimistic pantry locking, safe serialization, role/reset metadata, and recipe image URLs. Do not move business rules to React.

The endpoint inventory records all original mappings and their controller signatures; initial CRUD had no authorization and user/AI operations relied only on userId session lookups. See API.md for the final security contract and changes.
