# Trayecto

App web para registro de viajes en carro y cálculo automático de costos.

## Estructura

```
km-cost-tracker/
├── trayecto-api/      Backend Spring Boot 4 + Modulith 2 (DDD + CQRS, monolito modular)
└── trayecto-web/      Frontend Next.js 16 (App Router) + TanStack Query + shadcn/ui
```

## Stack

- **Backend:** Java 21, Spring Boot 4.0.6, Spring Modulith 2.0.6, PostgreSQL 16, Flyway, JWT + Google OAuth.
- **Frontend:** Next.js 16, React 19, TypeScript, TailwindCSS 4, shadcn/ui, TanStack Query, STOMP.
- **Infra (free tier):** Vercel (web) + Render (api) + Neon (db) + Brevo (email) + Cloudinary (fotos) + Google Cloud Vision (OCR).

## Arranque local

### Backend
```powershell
cd trayecto-api
copy .env.example .env   # rellenar con tus secrets locales
./mvnw spring-boot:run
```

### Frontend
```powershell
cd trayecto-web
copy .env.local.example .env.local   # rellenar
npm install
npm run dev
```

## Plan

Ver [plan consolidado](../../.claude/plans/quiero-desarrollar-un-app-web-misty-stroustrup.md).
