# misCalorias

Full containerized app: Spring Boot API + Angular frontend + MySQL, with
passwordless authentication over Telegram OTP.

| Component | Stack | Container | URL |
| --- | --- | --- | --- |
| Backend | Java 21, Spring Boot 3.5, JPA/Hibernate, Flyway, Security + JWT, springdoc | `miscalorias-backend` | <http://localhost:8081> |
| Frontend | Angular 22 (standalone, signals), served by nginx | `miscalorias-frontend` | <http://localhost:8080> |
| Database | MySQL 8.4 | `miscalorias-mysql` | `localhost:3306` |

The frontend nginx proxies `/api` to the backend container, so the whole app lives on
**one origin** (`http://localhost:8080`) and CORS is not involved in normal use.

```
backend/            Spring Boot API (Dockerfile, multi-stage Maven build)
frontend/           Angular app + nginx.conf (SPA fallback + /api proxy)
docker-compose.yml  mysql + backend + frontend
```

## Run everything

```bash
cp backend/.env.example backend/.env   # fill in the secrets (Telegram, JWT)
docker compose up -d --build
docker compose ps                      # all three should be (healthy)
```

Open <http://localhost:8080>.

```bash
docker compose logs -f backend         # API logs (OTP fallback, errors)
docker compose down                    # stop (data survives in the volume)
docker compose down -v                 # stop and wipe the database
```

## Telegram OTP delivery

1. Create a bot with **@BotFather** and copy the token.
2. Send any message to your bot from the account that must receive the codes.
3. Ask **@userinfobot** for your numeric chat id.
4. Put both in `backend/.env`:

```bash
TELEGRAM_BOT_TOKEN="123456:ABC..."
TELEGRAM_CHAT_ID="123456789"
TELEGRAM_FALLBACK_TO_LOG=false   # fail loudly instead of logging the OTP
```

The chat id can also be linked to the user row, so it no longer depends on the env var:

```sql
UPDATE app_users SET telegram_chat_id = 123456789
WHERE email = 'geovanni.vera23@gmail.com';
```

Without a token/chat id, `TELEGRAM_FALLBACK_TO_LOG=true` (default) prints the OTP in
`docker compose logs backend` — enough to develop offline.

## Authentication flow

```
POST /api/auth/otp/request  { "email": "..." }              -> 202, code sent over Telegram
POST /api/auth/otp/verify   { "email": "...", "code": "..." } -> 200, JWT (12 h)
GET  /api/auth/me           Authorization: Bearer <token>     -> 200, current user
```

Rules: 6-digit code, 5-minute TTL, 5 attempts, 30-second resend cooldown, one active
code per user (a new request invalidates the previous one). Unknown emails return the
same response as known ones to avoid account enumeration. Codes are stored hashed
(BCrypt) and the bot token is never logged.

Base user seeded by Flyway: `geovanni.vera23@gmail.com`.

Configuration: `backend/src/main/resources/application.yml` (`app.jwt`, `app.otp`,
`app.telegram`, `app.cors`). Environment overrides live in `backend/.env`
(gitignored, loaded by compose through `env_file`).

## Nutrition domain (phase 1)

```
GET /api/profile   -> profile + calculated goals (JWT required)
PUT /api/profile   -> partial update { age, heightCm, currentWeightKg, gymDaysPerWeek, deficitPct }
```

Goals are derived on every read (never stored) using Mifflin-St Jeor:

```
TMB        = 10*kg + 6.25*cm - 5*age + 5     (female: -161)
TDEE       = TMB * activityFactor
targetKcal = TDEE * (1 - deficitPct)
green band = targetKcal +/- 5%
protein    = proteinPerKg * kg        (green floor = 90%)
fat        = fatPerKg * kg
carbs      = (targetKcal - protein*4 - fat*9) / 4
water      = kg * 35 ml               (gym day: +500 ml)
```

Reference profile (male, 70 kg, 170 cm, 25 y, 1.48, 0.12, 2.0/0.8):
**TMB 1643 · TDEE 2431 · target 2139 kcal · 140 P · 56 F · 269 C · 2450/2950 ml**.

Schema: `V3__nutrition_domain.sql` (`user_profiles`, `food_items`, `quick_plates`,
`daily_logs`, `meal_entries`, `biweekly_checkins`) and `V4__seed_3b_catalog_and_profile.sql`
(10 global 3B foods, 3 quick plates, initial profile).

## Catalog (slice 2)

```
GET  /api/foods?q=...        global + own items, filter by name or state tag
POST /api/foods              create a custom item (base values per 100 g)
GET  /api/quick-plates       global + own plates
POST /api/quick-plates       save a new combo
```

`userId: null` means a shared 3B item. `stateTag` accepts `Cocido | Crudo | Pieza | Drenado | Directo`.
`quickGrams` is exposed as a `List<Integer>` parsed from `quick_grams_json`.

## Daily log (slice 2)

```
GET    /api/daily-logs/{date}                    summary (creates the day on first access)
POST   /api/daily-logs/{date}/entries/scale      { foodItemId, grams, loggedTime? }
POST   /api/daily-logs/{date}/entries/quick-plate{ quickPlateId, loggedTime? }
POST   /api/daily-logs/{date}/entries/off-plan   { label, kcal, loggedTime? }
DELETE /api/daily-logs/{date}/entries/{entryId}
PATCH  /api/daily-logs/{date}                    { waterMl? , addWaterMl?, isGymDay? }
```

Every call returns the same `DailySummaryResponse`: `targets`, `consumed`,
`remaining`, `missingToTmb` (`max(0, tmb - consumedKcal)`), `inGreenMargin`
(consumed inside the ±5% band **and** protein ≥ 90% of the target),
`cleanEatingPct` (share of calories not marked off-plan, 100 when nothing is logged),
`waterTargetMl` (2.45 L rest / 2.95 L gym for the reference profile) and `entries`.

Scale entries compute `grams / 100` against the food's per-100 g values, rounding
kcal to a whole number and macros to one decimal. Entries are denormalized: editing a
catalog item never rewrites history.

## Analytics and check-ins (slice 3)

```
GET  /api/analytics/14-days?endDate=YYYY-MM-DD
GET  /api/analytics/compare-weeks?baseStart=YYYY-MM-DD&currentStart=YYYY-MM-DD
GET  /api/checkins
POST /api/checkins   { checkinDate?, weightKg, waistCm?, note? }
POST /api/notifications/test-tmb-check
```

`14-days` returns 14 points (`D1`..`D14`), the fortnight summary, and previous week
(days 1-7) vs current week (days 8-14) with `kcalDelta`, `proteinDelta`,
`greenPctDelta`, `cleanPctDelta`.

Averaging rule: `avgKcal`, `avgProtein` and `avgCleanEatingPct` are averaged over days
that actually have entries (`loggedDays`); `greenDaysPct` is measured against
`totalDays`, because adherence counts every day you were supposed to eat well.

Check-ins compute `greenDaysPct` and `cleanEatingPct` from the 14 days ending at
`checkinDate`, update the profile weight, and apply the guided adjustment rule:
stalled weight **and** waist with >= 80% green days suggests raising the deficit to 14%.

## Notifications

`@EnableScheduling` drives two sweeps in `America/Mexico_City` (`app.time.zone`):

| Job | Cron | Rule |
| --- | --- | --- |
| TMB floor alert | `0 30 20 * * *` | notifies when the day is below the TMB floor |
| Check-in reminder | `0 0 8 * * *` | 14+ days since the last check-in (or profile creation) |

Both only target active users with a `telegram_chat_id`. The TMB check alerts even when
nothing was logged (0 kcal is below the floor), which is the intended nudge.
Disable with `NOTIFICATIONS_ENABLED=false`; cron expressions live in `application.yml`.

## Other endpoints

- Health: <http://localhost:8081/actuator/health>
- Swagger UI: <http://localhost:8081/swagger-ui.html>
- API docs: <http://localhost:8081/v3/api-docs>
- Schema migrations: `backend/src/main/resources/db/migration` (`ddl-auto: none`)

## Frontend

Angular 22 standalone + signals, no UI framework: hand-written CSS with a
mobile-first shell (`max-width: 28rem`, full-bleed on phones) and three fixed tabs.

```
/login      OTP over Telegram (email → code → JWT)
/today      calories vs TMB and green band, macros, water, scale entries, off-plan, quick plates
/analytics  14-day KPIs and chart, week-over-week comparison, fortnight check-in + history
/plan       editable age/height/weight/gym days, live goals, educational accordion
```

Palette: graphite `#141615`, military green `#4B5D44` (hover `#5C7253`, accent text
`#7FA072`), cream `#F2EDE4`, amber `#D99B4E` for alerts and off-plan food.

### Design tokens & themes

Colors live **only** in `src/styles.css` (Angular's global stylesheet). Components use
`var(--token)`; no component declares a hex value.

```
[data-theme="graphite"]  #141615 base · #1E211F card · #4B5D44 green · #F2EDE4 cream · #D99B4E alert
[data-theme="spruce"]    #EEE2CB base · #F7EFE0 card · #1B4532 spruce · tomato #EE4129 alert
```

Requested core tokens: `--bg-base`, `--bg-card`, `--border-subtle`, `--text-primary`,
`--text-secondary`, `--accent-primary`, `--accent-primary-hover`, `--accent-text`,
`--accent-alert`. Derived for contrast-safe usage: `--bg-card-alt` (inputs),
`--bg-nav`, `--on-accent`, `--on-alert`, `--accent-alert-ink` (readable alert text),
`--accent-alert-soft`, `--accent-alert-border`, `--accent-soft`, `--band-fill`,
`--band-border`, `--track-bg`, `--overlay`, `--shadow`.

Theme selection lives in **Mi Plan → Tema**, persists in `localStorage`
(`recomp3b.theme`) and is applied to `<html data-theme="...">`. A small inline script in
`index.html` sets the attribute before the first paint, so the stored theme never
flashes the default. Switching also updates `<meta name="theme-color">`.

Contrast (WCAG, computed): every text-on-surface pair is >= 4.5:1 in both themes
(e.g. secondary text 6.4:1 dark / 5.9:1 light; button text 6.1:1 / 9.5:1). White on
tomato is 3.87:1, so it only applies to large text or icons — alert body text uses
`--accent-alert-ink` over the soft tint (5.2:1).

**Dates are always resolved in `America/Mexico_City`** (`core/date.util.ts`), never from
`toISOString()`, because after 18:00 local the UTC day is already tomorrow.

PWA: `public/manifest.webmanifest` + `public/icon.svg`, theme color `#141615`.

Tests: `npm test -- --watch=false` (Vitest). The suite covers the timezone rule and the
"Today" page rendering (calories, TMB floor warning, entries).

## Local development (without containers)

```bash
docker compose up -d mysql

cd backend && set -a && source .env && set +a && mvn spring-boot:run
cd frontend && npm start          # proxy.conf.json points /api to localhost:8081
```

Tests: `cd backend && mvn test` · `cd frontend && npm test -- --watch=false`

## Notes

- JWT secret must be 32+ bytes or the context refuses to start. Generate with
  `openssl rand -base64 48`.
- `backend/.env` is excluded from the Docker image via `.dockerignore`; secrets are
  injected at runtime by compose.
- Backend runs as a non-root user inside the image.
