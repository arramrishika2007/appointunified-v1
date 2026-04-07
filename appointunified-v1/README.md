# AppointUnified — V1 Complete Codebase

> **Unified Smart Appointment Engine** · Healthcare · Government · Services  
> Stack: Spring Boot 3.2 · Java 17 · Next.js 14 · Neon PostgreSQL · Vercel · Railway

---

## 🚀 What's in V1

V1 is the **Core Booking Engine** — a production-grade, fully deployable appointment system with:

- ✅ User registration & JWT authentication (phone + email)
- ✅ Multi-sector professional profiles (Healthcare / Government / Services)
- ✅ 4-step booking wizard with real conflict detection
- ✅ Available-slot engine (weekday schedule → open time grid)
- ✅ Appointment management: cancel, reschedule, complete, no-show
- ✅ Email notifications (confirmation, cancellation, reschedule)
- ✅ Verified badge system (admin approval workflow ready)
- ✅ Full REST API with Swagger UI

### ✨ 5 New Features Added to V1 (not in original roadmap)

| # | Feature | Where |
|---|---------|--------|
| 1 | **Smart Slot Recommendation Engine** | Backend: `SlotRecommendationService` · Frontend: Dashboard "Recommended for You" · API: `GET /api/recommendations/slots` |
| 2 | **Provider Availability Mood Status** | Backend: `availability_mood` column on `professionals` · Frontend: Mood badge on every card + dropdown on pro dashboard · API: `PATCH /api/professionals/me/mood` |
| 3 | **Booking Intent Drafts** | Backend: `booking_drafts` table + CRUD API · Frontend: Auto-save during wizard, "Saved Drafts" tab in My Bookings |
| 4 | **Shareable Appointment Links + iCal Export** | Backend: `share_token` on `appointments` + `/ical` endpoint · Frontend: Copy share link, Download to Calendar, public `/appointments/share/[token]` page |
| 5 | **Proactive Appointment Reminder Scheduler** | Backend: `ReminderSchedulerService` with 24h / 2h / 30min notification windows using Spring `@Scheduled` |

---

## 📁 Project Structure

```
appointunified-v1/
├── backend/                          Spring Boot 3.2 (Java 17)
│   ├── src/main/java/com/appointunified/
│   │   ├── AppointUnifiedApplication.java
│   │   ├── config/
│   │   │   ├── AsyncConfig.java
│   │   │   ├── OpenApiConfig.java
│   │   │   └── SecurityConfig.java
│   │   ├── controller/
│   │   │   ├── AuthController.java
│   │   │   ├── AppointmentController.java
│   │   │   ├── ProfessionalController.java
│   │   │   ├── RecommendationController.java
│   │   │   └── ServiceController.java
│   │   ├── dto/
│   │   │   ├── request/  (AuthRequest, AppointmentRequest, ProfessionalRequest)
│   │   │   └── response/ (ApiResponse, AuthResponse, AppointmentResponse, ...)
│   │   ├── entity/
│   │   │   ├── User.java
│   │   │   ├── Professional.java   ← includes availabilityMood (Feature 2)
│   │   │   ├── Appointment.java    ← includes shareToken (Feature 4)
│   │   │   ├── Service.java
│   │   │   ├── Availability.java
│   │   │   ├── BookingDraft.java   ← Feature 3
│   │   │   └── RefreshToken.java
│   │   ├── enums/
│   │   │   └── AvailabilityMood, AppointmentStatus, Sector, UserRole, ...
│   │   ├── exception/
│   │   │   ├── AppException.java
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── repository/  (all Spring Data JPA repos with custom queries)
│   │   ├── security/
│   │   │   ├── JwtTokenProvider.java
│   │   │   └── JwtAuthenticationFilter.java
│   │   └── service/
│   │       ├── AuthService.java
│   │       ├── AppointmentService.java  ← iCal, share, drafts (Features 3 & 4)
│   │       ├── ProfessionalService.java ← mood update (Feature 2)
│   │       ├── NotificationService.java
│   │       ├── ReminderSchedulerService.java  ← Feature 5
│   │       └── SlotRecommendationService.java ← Feature 1
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── db/migration/V1__initial_schema.sql  ← all tables including new features
│   ├── .env.example
│   ├── Dockerfile
│   └── pom.xml
│
├── frontend/                          Next.js 14 (App Router)
│   ├── app/
│   │   ├── layout.tsx
│   │   ├── page.tsx                   Landing page
│   │   ├── globals.css                Design system
│   │   ├── auth/
│   │   │   ├── login/page.tsx
│   │   │   └── signup/page.tsx
│   │   ├── explore/[sector]/page.tsx  Search + filter providers
│   │   ├── provider/[id]/page.tsx     Full provider profile
│   │   ├── booking/[id]/page.tsx      4-step wizard (auto-draft, Feature 3)
│   │   ├── dashboard/
│   │   │   ├── page.tsx              User dashboard (Recommendations, Feature 1)
│   │   │   └── bookings/page.tsx     My Bookings (Share, iCal, Drafts, Features 3 & 4)
│   │   ├── professional/
│   │   │   ├── onboarding/page.tsx   Registration wizard
│   │   │   └── dashboard/page.tsx    Pro dashboard (Mood panel, Feature 2)
│   │   └── appointments/share/[token]/page.tsx  Public share view (Feature 4)
│   ├── components/
│   │   ├── layout/Navbar.tsx
│   │   └── provider/ProfessionalCard.tsx  (mood badge, Feature 2)
│   ├── lib/
│   │   ├── api.ts                     Axios client + auto-refresh
│   │   ├── store.ts                   Zustand auth store
│   │   └── utils.ts                   MOOD_CONFIG, STATUS_CONFIG, formatters
│   ├── types/index.ts                 All TypeScript types
│   ├── .env.example
│   ├── Dockerfile
│   └── package.json
│
├── .github/workflows/
│   ├── ci.yml                        CI: lint + test + build (both apps)
│   └── deploy.yml                    Deploy: Vercel + Railway
│
├── docker-compose.yml                Local dev stack (Postgres + MailHog)
├── .gitignore
└── README.md
```

---

## ⚡ Quick Start (Local Dev)

### Prerequisites
- Java 17 (Temurin recommended)
- Node.js 20+
- Docker & Docker Compose (for local Postgres + mail)
- Maven 3.9+

### 1. Clone & setup

```bash
git clone https://github.com/your-org/appointunified.git
cd appointunified-v1
cp .env.example .env
```

### 2. Start local services (Postgres + MailHog)

```bash
docker compose up -d postgres mailhog
# Postgres → localhost:5432
# MailHog UI → http://localhost:8025
```

### 3. Backend

```bash
cd backend
# Backend env is already covered by the root .env file for Docker Compose.
# If you run Spring Boot directly, export the same variables first.

mvn spring-boot:run
# API → http://localhost:8080/api
# Swagger UI → http://localhost:8080/api/swagger-ui
```

### 4. Frontend

```bash
cd frontend
cp .env.example .env.local
# Edit NEXT_PUBLIC_API_URL if needed

npm install
npm run dev
# App → http://localhost:3000
```

---

## 🔑 Required Environment Variables

### Backend (`.env` or Railway env vars)

| Variable | Description | Where to get |
|----------|-------------|-------------|
| `NEON_DATABASE_URL` | PostgreSQL connection string | [console.neon.tech](https://console.neon.tech) |
| `JWT_SECRET` | 64+ char base64 string | `openssl rand -base64 64` |
| `APP_FRONTEND_URL` | Public frontend URL used in emails and iCal links | Your frontend URL |
| `FIREBASE_CREDENTIALS` | Base64-encoded Firebase Admin SDK JSON | Firebase Console → Service Accounts |
| `FIREBASE_PROJECT_ID` | Firebase project ID | Firebase Console |
| `SENDGRID_API_KEY` | SendGrid API key | [app.sendgrid.com](https://app.sendgrid.com) |
| `MAIL_FROM` | Sender email used for notifications | Your verified sender address |
| `MAIL_FROM_NAME` | Sender display name | Your app or brand name |
| `CLOUDINARY_URL` | Full Cloudinary URL | [console.cloudinary.com](https://console.cloudinary.com) |
| `CLOUDINARY_UPLOAD_PRESET` | Cloudinary upload preset | Cloudinary Console |
| `NEXT_PUBLIC_UPLOADCARE_PUBLIC_KEY` | Uploadcare public key for browser uploads | Uploadcare dashboard |
| `NEXT_PUBLIC_GEOAPIFY_API_KEY` | Geoapify geocoding key | Geoapify dashboard |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | Your frontend URL |

### Frontend (`.env.local`)

| Variable | Description |
|----------|-------------|
| `NEXT_PUBLIC_API_URL` | Spring Boot API URL e.g. `https://api.railway.app/api` |
| `NEXT_PUBLIC_APP_URL` | Your Vercel app URL |
| `NEXT_PUBLIC_FIREBASE_*` | Firebase Web SDK config |
| `NEXT_PUBLIC_CLOUDINARY_CLOUD_NAME` | For client-side uploads |
| `NEXT_PUBLIC_UPLOADCARE_PUBLIC_KEY` | Uploadcare public key |
| `NEXT_PUBLIC_GEOAPIFY_API_KEY` | Geoapify geocoding key |

---

## 🚢 Deployment

### Frontend → Vercel (free)

1. Push to GitHub
2. Import project at [vercel.com/new](https://vercel.com/new)
3. Set root directory to `frontend/`
4. Add all `NEXT_PUBLIC_*` env vars
5. Deploy — auto-deploys on every `git push main`

### Backend → Railway (free tier)

```bash
# Install Railway CLI
npm install -g @railway/cli

# Login and link
railway login
railway init
railway up --service backend

# Set env vars via Railway dashboard or CLI
railway variables set NEON_DATABASE_URL=...
```

### Database → Neon (free tier)

1. Create project at [console.neon.tech](https://console.neon.tech)
2. Copy the connection string (with `?sslmode=require`)
3. Set as `NEON_DATABASE_URL`
4. Flyway auto-runs `V1__initial_schema.sql` on first startup

---

## 📡 API Reference

Base URL: `http://localhost:8080/api`  
Full docs: `http://localhost:8080/api/swagger-ui`

### Auth
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/auth/signup` | Public | Register new user |
| POST | `/auth/login` | Public | Login → JWT pair |
| POST | `/auth/refresh` | Public | Refresh access token |
| DELETE | `/auth/logout` | JWT | Revoke refresh tokens |

### Professionals
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/professionals` | Public | Search verified professionals |
| GET | `/professionals/{id}` | Public | Get profile |
| GET | `/professionals/{id}/slots` | Public | Available slots for date |
| POST | `/professionals/register` | PROFESSIONAL | Submit profile |
| PATCH | `/professionals/me` | PROFESSIONAL | Update profile |
| PATCH | `/professionals/me/mood` | PROFESSIONAL | **[NEW]** Update live mood status |

### Appointments
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/appointments` | JWT | Book appointment |
| GET | `/appointments/me` | JWT | My appointments |
| PUT | `/appointments/{id}/cancel` | JWT | Cancel |
| PUT | `/appointments/{id}/reschedule` | JWT | Reschedule |
| POST | `/appointments/{id}/complete` | PROFESSIONAL | Mark complete |
| POST | `/appointments/{id}/no-show` | PROFESSIONAL | Mark no-show |
| GET | `/appointments/{id}/share` | JWT | **[NEW]** Get share URL |
| GET | `/appointments/{id}/ical` | Public | **[NEW]** Download iCal file |
| GET | `/appointments/share/{token}` | Public | **[NEW]** View shared appointment |
| POST | `/appointments/drafts` | JWT | **[NEW]** Save booking draft |
| GET | `/appointments/drafts` | JWT | **[NEW]** Get my drafts |
| DELETE | `/appointments/drafts/{id}` | JWT | **[NEW]** Delete draft |

### Recommendations (NEW)
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/recommendations/slots` | JWT | **[NEW]** Personalised slot recommendations |

---

## 🗄️ Database Schema Highlights

```sql
-- Core tables
users               -- auth, role, risk_score
professionals       -- + availability_mood, mood_note (Feature 2)
services            -- per-professional catalogue
appointments        -- + share_token, share_expires_at (Feature 4)
availability        -- weekly schedule (weekday + time range)
refresh_tokens      -- hashed, rotated on use

-- New V1 feature tables
booking_drafts      -- auto-saved wizard state, 48h TTL (Feature 3)
availability_exceptions  -- holiday/partial-day blocking (Feature 5 DB layer)
user_booking_preferences -- preference learning seed (Feature 1)
slot_recommendations     -- recommendation tracking for model training (Feature 1)

-- Supporting tables
verifications       -- document upload + hash dedup
reviews             -- with helpful_count, is_flagged
audit_logs          -- full tamper-evident trail
notification_log    -- sent notification history
notification_preferences  -- per-user channel settings
```

---

## 🧪 Running Tests

```bash
# Backend
cd backend
mvn test

# Frontend
cd frontend
npm test
npm run type-check
npm run lint
```

---

## 🗺️ What's Next (V2 Preview)

V2 adds the **Multi-Sector Trust System**:
- Document upload via Cloudinary with SHA-256 dedup
- Admin verification queue UI
- Verified badge lifecycle
- OTP phone verification via Firebase
- Sector-specific credential fields (NMC for healthcare, Employee ID for govt)

---

## 🛠️ Tech Stack

| Layer | Technology | Hosting |
|-------|-----------|---------|
| Frontend | Next.js 14, TypeScript, Tailwind CSS, Zustand | Vercel (free) |
| Backend | Spring Boot 3.2, Java 17, Spring Security | Railway (free) |
| Database | Neon PostgreSQL (serverless) | Neon (free) |
| Auth | Firebase Auth + Spring JWT | Firebase (free) |
| Email | SendGrid SMTP | SendGrid (100/day free) |
| Storage | Cloudinary | Cloudinary (free) |
| CI/CD | GitHub Actions | GitHub (free) |
| Local dev | Docker Compose (Postgres + MailHog) | Local |

---

*AppointUnified V1 — Built to survive real-world unpredictability.*
