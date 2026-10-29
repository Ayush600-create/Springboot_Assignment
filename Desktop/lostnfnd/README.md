# CampusGuard — Smart Lost and Found

Centralized, secure lost-and-found portal for college campuses. Students report lost and found items, receive weighted matches, submit ownership claims, and complete admin-controlled OTP handover.

## Architecture

- **Backend:** Java 17, Spring Boot 3.3, Spring Security (JWT), Spring Data JPA, MySQL 8
- **Frontend:** React 18, Vite, TypeScript, Tailwind CSS, Framer Motion, Lucide
- **Integrations:** JavaMail (optional SMTP), AES-256-GCM for private identifying details

The backend does not drop the existing MySQL database. Hibernate `ddl-auto` is `update`.

## Local setup

### Prerequisites

- Java 17+ (the project targets 17; newer JDKs usually work)
- Maven 3.9+
- Node.js 20+
- MySQL 8 with a `campusguard` schema (created automatically if the JDBC URL allows it)

### 1. Local secrets (never commit)

Copy `backend/src/main/resources/application-local.example.properties` to
`backend/src/main/resources/application-local.properties` (gitignored) and set:

```
spring.datasource.password=<your existing MySQL password>
campusguard.crypto.key=<32-byte key, Base64-encoded>
campusguard.jwt.secret=<long random JWT secret>
campusguard.admin.email=<admin login email>
campusguard.admin.password=<admin login password>
```

Do not replace a working password with a guessed one. Environment variables also work: `MYSQL_PASSWORD`, `CAMPUSGUARD_ENCRYPTION_KEY`, `CAMPUSGUARD_JWT_SECRET`, `CAMPUSGUARD_ADMIN_EMAIL`, `CAMPUSGUARD_ADMIN_PASSWORD`.

### 2. Backend

```bash
cd backend
mvn test
# Surefire runs *Test and *IT classes (matching, claims, OTP, full flow).
mvn spring-boot:run
```

Default URL: `http://localhost:8080`

Active profiles default to `local,demo`. Demo seed data is labeled **Demo data** in the UI.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Vite is on `http://localhost:5173` and proxies `/api` to the backend.

Production frontend build:

```bash
cd frontend
npm run build
```

## Demo data (profile `demo` only)

Seeded only when the `demo` profile is active, and only if those users are not already present.

| Account | Email | Password | Role |
| --- | --- | --- | --- |
| Aisha Rahman | aisha@srmist.edu.in | `CampusGuard!demo` | Student |
| Rahul Mehta | rahul@srmist.edu.in | `CampusGuard!demo` | Student |
| Admin | from env / local properties | from env / local properties | Admin |

Headphones pair: Text 85.5, other factors 100, final **94.2 HIGH_CONFIDENCE**.  
One claim: verification score **50.0**, threshold **60.0**, status **REJECTED**, review **FLAGGED_FOR_ADMIN_REVIEW**.

Admin credentials are never committed. Seed student passwords are stored hashed.

Typing a student ID does **not** verify an account. New students stay `PENDING_APPROVAL` unless `campusguard.auto-approve-college-email` is true.

## Configuration keys

| Key | Default | Meaning |
| --- | --- | --- |
| `campusguard.matching.weights.text` | 0.40 | Text similarity weight |
| `campusguard.matching.weights.location` | 0.20 | Location weight |
| `campusguard.matching.weights.date` | 0.15 | Date weight |
| `campusguard.matching.weights.category` | 0.10 | Category weight |
| `campusguard.matching.weights.brand` | 0.10 | Brand weight |
| `campusguard.matching.weights.color` | 0.05 | Color weight |
| `campusguard.matching.confidence.high` | 80.0 | HIGH_CONFIDENCE ≥ this score |
| `campusguard.matching.confidence.possible` | 60.0 | POSSIBLE_MATCH from this score up to high |
| `campusguard.matching.date-max-days` | 14 | Date factor falls to 0 beyond this gap |
| `campusguard.claim.verification.threshold` | 60.0 | Ownership check threshold (0–100) |
| `campusguard.claim.verification.max-attempts` | 3 | Max verification attempts per claimant per item |
| `campusguard.otp.expiry-minutes` | 10 | Handover OTP lifetime |
| `campusguard.otp.max-attempts` | 3 | OTP guess limit |
| `campusguard.mail.enabled` / `SMTP_ENABLED` | false | Real email send |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM` | empty | SMTP |

Startup fails if matching weights do not sum to **1.00**.

## Missing-data policy

If an optional factor (brand, color, and similarly incomplete location/date/text) is missing on **either** report, that factor scores **0**. Weights are **never** re-normalized. Missing data is never treated as a perfect match.

Private identifying details are encrypted with AES-256-GCM (key from environment). Hashing is not used because verification needs a similarity score against the original text. Private details are never returned by any API and are not written to logs. Ownership verification uses token Jaccard against the full private payload and against each line, taking the higher score so a serial-number line does not dilute a matching ownership statement.

## SMTP and OTP limitation

If SMTP is not enabled or send fails, handover `otpStatus` becomes `DELIVERY_UNAVAILABLE`. The UI shows **OTP email is not configured**. Completion is blocked. There is no fallback that displays or logs the OTP. Unit/integration tests use a mock mail sender. Real delivery has **not** been proven against a live SMTP server.

## Security features status

| Feature | Implemented | Tested | Limitations |
| --- | --- | --- | --- |
| BCrypt password hashing | Yes | Yes (register/login IT) | — |
| JWT session + role-based admin routes | Yes | Yes (student forbidden on `/api/admin/**`) | JWT secret must be set in local config for production |
| College email format + admin approval | Yes | Yes (pending login forbidden until approve) | Format check is not proof of mailbox ownership unless you enable SMTP verification later |
| Private details AES-256-GCM | Yes | Yes (API must not contain serial / secret text) | Key must be 32 bytes; losing the key makes old private data unreadable |
| Public/private field split | Yes | Yes | Photograph files require a logged-in user; UUID filenames reduce guessing |
| Matching formula 94.2 / weight sum / missing=0 | Yes | Yes (unit tests) | Text uses token Jaccard, not embeddings |
| Duplicate match prevention | Yes | Yes | Recalc updates the existing pair |
| Claim threshold 50 vs 60 / 45 | Yes | Yes | Threshold only via configuration |
| Invalid claim transitions → HTTP 409 | Yes | Yes | — |
| Admin reopen once with reason | Yes | Yes | — |
| OTP hash, expiry, reuse, attempt limit, SMTP-off block | Yes | Yes (mock mail) | Real SMTP untested |
| Login and verification rate limits | Yes | Partial (IT uses raised test limits) | In-memory limiter; not clustered |
| Secure uploads (type/size/safe name) | Yes | Partial (IT uploads a JPEG) | Content-type from client; not full magic-byte sniffing |
| IDOR on claims | Yes | Yes (outsider forbidden) | File UUIDs are reachable by any authenticated user who knows the name |
| CORS limited to Vite origin | Yes | Untested in browser automation | — |
| Secrets not committed | Yes | N/A | Keep `application-local.properties` gitignored |

A row is **operational** only when Implemented and Tested are both Yes.

## Claim state machine

`PENDING` → `VERIFIED` (score ≥ threshold) or `REJECTED` (below threshold, flagged for admin).  
`VERIFIED` → `UNDER_REVIEW` (admin opens).  
`REJECTED` → `UNDER_REVIEW` (admin reopen, reason required, once).  
`UNDER_REVIEW` → `APPROVED_FOR_HANDOVER` or `REJECTED`.  
`APPROVED_FOR_HANDOVER` → `COMPLETED` after correct OTP. Item then `RETURNED`.

Match score (same item?) and verification score (ownership answers vs private details) are stored separately and must not be swapped.

## Verify locally

1. Log in as a demo student, open the dashboard, submit a report with a photo.
2. Search/browse and confirm results come from MySQL.
3. Open the headphones match: 94.2% HIGH_CONFIDENCE, six factor bars, disclaimer.
4. Submit a claim. Below-threshold claims stay rejected and flagged.
5. Log in as admin, review, approve or reject with a reason, issue OTP (expect “not configured” without SMTP).
6. Confirm private serial numbers never appear on item cards or JSON.
