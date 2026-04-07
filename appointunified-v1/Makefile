# ─────────────────────────────────────────────────────────────
#  AppointUnified V1 — Developer Makefile
#  Usage: make <target>
# ─────────────────────────────────────────────────────────────

.PHONY: help dev-up dev-down backend frontend seed test-backend test-frontend clean build-all

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

# ─── Local dev ───────────────────────────────────────────────

dev-up: ## Start local Postgres + MailHog (Docker)
	docker compose up -d postgres mailhog
	@echo "✅ Postgres: localhost:5432"
	@echo "✅ MailHog:  http://localhost:8025"

dev-down: ## Stop all local Docker services
	docker compose down

dev-all: ## Start ALL services including backend + frontend
	docker compose up -d

# ─── Backend ─────────────────────────────────────────────────

backend: ## Run Spring Boot backend (requires dev-up)
	cd backend && mvn spring-boot:run

backend-build: ## Build backend JAR
	cd backend && mvn clean package -DskipTests

backend-docker: ## Build backend Docker image
	cd backend && mvn spring-boot:build-image -DskipTests

# ─── Frontend ────────────────────────────────────────────────

frontend: ## Run Next.js frontend dev server
	cd frontend && npm run dev

frontend-build: ## Build Next.js for production
	cd frontend && npm run build

frontend-install: ## Install frontend dependencies
	cd frontend && npm install

# ─── Database ────────────────────────────────────────────────

seed: ## Seed the local database with test data
	@if [ -z "$$NEON_DATABASE_URL" ]; then \
		psql postgresql://au_user:au_password@localhost:5432/appointunified -f backend/src/main/resources/db/seed.sql; \
	else \
		psql $$NEON_DATABASE_URL -f backend/src/main/resources/db/seed.sql; \
	fi
	@echo "✅ Seed data loaded. Test credentials in seed.sql"

migrate: ## Run Flyway migrations manually
	cd backend && mvn flyway:migrate

# ─── Tests ───────────────────────────────────────────────────

test-backend: ## Run backend tests
	cd backend && mvn test

test-frontend: ## Run frontend lint + type-check + tests
	cd frontend && npm run lint && npm run type-check && npm run test

test-all: test-backend test-frontend ## Run all tests

# ─── Utilities ────────────────────────────────────────────────

clean: ## Clean build artifacts
	cd backend && mvn clean
	rm -rf frontend/.next frontend/out

swagger: ## Open Swagger UI in browser
	open http://localhost:8080/api/swagger-ui

mailhog: ## Open MailHog web UI in browser
	open http://localhost:8025

env-check: ## Check required env vars are set
	@echo "Checking backend env vars..."
	@[ -n "$$NEON_DATABASE_URL" ]   && echo "✅ NEON_DATABASE_URL"  || echo "❌ NEON_DATABASE_URL  (required)"
	@[ -n "$$JWT_SECRET" ]          && echo "✅ JWT_SECRET"         || echo "❌ JWT_SECRET          (required)"
	@[ -n "$$APP_FRONTEND_URL" ]     && echo "✅ APP_FRONTEND_URL"    || echo "❌ APP_FRONTEND_URL    (used in emails and share links)"
	@[ -n "$$SENDGRID_API_KEY" ]    && echo "✅ SENDGRID_API_KEY"   || echo "❌ SENDGRID_API_KEY    (required for email)"
	@[ -n "$$CLOUDINARY_URL" ]      && echo "✅ CLOUDINARY_URL"     || echo "❌ CLOUDINARY_URL      (required for uploads)"
