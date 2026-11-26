# :feature:today:data

DTO mappers, Room entities and DAOs, repository implementations, Hilt module.

Today and the days behind My Day, cache first: `TodayRepositoryImpl` stores `GET /today` and today's and yesterday's `GET /day` per account (stripped of blood pressure and sleep stages), keeps other days in memory, and writes only while the account that asked is signed in. Spec: `docs/feature-specs/today.md`.
