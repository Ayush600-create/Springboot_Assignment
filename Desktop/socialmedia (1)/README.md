# Virality Engine — Backend Engineering Assignment

A Spring Boot microservice acting as a central API gateway and guardrail system with Redis-backed concurrency protection, virality scoring, and a smart notification engine.

---

## Tech Stack

- **Java 17** + **Spring Boot 3.x**
- **PostgreSQL** — source of truth for all content
- **Redis** — gatekeeper for all counters, cooldowns, and notifications
- **Docker** — local infrastructure via docker-compose

---

## Quick Start

### 1. Start infrastructure
```bash
docker-compose up -d
```

### 2. Run the application
```bash
./mvnw spring-boot:run
```

### 3. Import Postman collection
Import `postman_collection.json` into Postman and start hitting endpoints.

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/posts` | Create a new post |
| POST | `/api/posts/{postId}/comments` | Add a comment (with guardrails for bots) |
| POST | `/api/posts/{postId}/like` | Like a post (human only) |

### Example: Create Post
```json
POST /api/posts
{
  "authorId": 1,
  "authorType": "USER",
  "content": "Hello world!"
}
```

### Example: Bot Comment
```json
POST /api/posts/1/comments
{
  "authorId": 1,
  "authorType": "BOT",
  "content": "I am a bot!",
  "depthLevel": 0,
  "postOwnerId": 1
}
```

### Example: Like a Post
```json
POST /api/posts/1/like
{
  "userId": 1
}
```

---

## Architecture Overview

```
Client Request
     │
     ▼
PostController
     │
     ▼
PostService ──────────────────────────────────────────┐
     │                                                 │
     ▼                                                 ▼
RedisGuardrailService                          PostgreSQL (JPA)
 ├── Virality Score (INCR)                     Source of truth
 ├── Horizontal Cap (Lua EVAL)                 for all content
 ├── Vertical Cap (depth check)
 ├── Cooldown Cap (SET NX)
 └── Notification Engine (LIST + TTL)
```

**PostgreSQL** stores all actual data (posts, comments, likes, users, bots).  
**Redis** acts as the gatekeeper — no DB write happens unless Redis guardrails pass.

---

## Thread Safety — How Atomic Locks Are Guaranteed

This is the core of the assignment. Three distinct Redis patterns are used:

### 1. Horizontal Cap — Lua Script (EVAL)

The naive approach of `GET → check → INCR` is a **TOCTOU race condition**. Under 200 concurrent requests, two threads can both read `count = 99`, both pass the check, and both increment — resulting in 101 bot replies.

**Solution: Lua script executed via Redis `EVAL`.**

```lua
local current = tonumber(redis.call('GET', KEYS[1]) or 0)
if current >= tonumber(ARGV[1]) then
  return -1
end
return redis.call('INCR', KEYS[1])
```

Redis executes Lua scripts **atomically and single-threadedly** — no other command can interleave between the `GET` and the `INCR`. This guarantees that under 200 concurrent requests, **exactly 100 will succeed and 100 will be rejected** with a `429 Too Many Requests`. It is impossible for the counter to reach 101.

### 2. Cooldown Cap — SET NX (SET if Not eXists)

```
SET cooldown:bot_{id}:human_{id} "1" NX EX 600
```

`SET NX` is **atomic by design** in Redis. Only one caller can "win" the set. If the key already exists, all subsequent calls return `null` (blocked). The `EX 600` (10 minutes TTL) ensures the lock auto-expires. No manual cleanup needed.

### 3. Vertical Cap — Pure Logic

Depth level is passed in the request and checked before any Redis mutation — no atomicity needed since it's a pure integer comparison with no shared state.

### Rollback Strategy

If the Redis bot count is incremented (Lua script succeeds) but the subsequent **PostgreSQL write fails**, the service calls `decrementBotCount()` to roll back the Redis counter. This keeps Redis and the database in sync.

---

## Redis Key Schema

| Key Pattern | Type | Description |
|---|---|---|
| `post:{id}:virality_score` | String (int) | Running virality score |
| `post:{id}:bot_count` | String (int) | Total bot replies on a post |
| `cooldown:bot_{id}:human_{id}` | String (TTL 10m) | Per-bot per-human cooldown lock |
| `notif_cooldown:user_{id}` | String (TTL 15m) | Notification send cooldown |
| `user:{id}:pending_notifs` | List | Queued notification strings |

All state is **exclusively in Redis** — no Java `HashMap`, `static` variables, or in-memory counters anywhere in the codebase.

---

## Phase 3: Notification Engine

When a bot interacts with a user's post:
- **No active cooldown** → log `"Push Notification Sent to User"` immediately, set 15-min cooldown
- **Cooldown active** → push notification string to `user:{id}:pending_notifs` Redis List

A `@Scheduled` CRON task runs every **5 minutes**, scans all pending notification keys, drains each list atomically, and logs a summarized message:

```
Summarized Push Notification: Bot Alpha and [3] others interacted with your posts.
```

---

## Guardrail Response Codes

| Guardrail | Condition | HTTP Status |
|---|---|---|
| Horizontal Cap | > 100 bot replies on a post | `429 Too Many Requests` |
| Vertical Cap | depth_level > 20 | `429 Too Many Requests` |
| Cooldown Cap | Same bot+human within 10 min | `429 Too Many Requests` |
| Duplicate Like | User already liked post | `409 Conflict` |
