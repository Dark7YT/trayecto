# Deploy de Trayecto a producción

Guía paso a paso para pasar de localhost a una URL pública usando solo tiers
gratuitos. Tiempo estimado: **45-60 min la primera vez** (después solo `git push`).

## Stack final en prod

| Componente | Proveedor | Plan | Costo |
|---|---|---|---|
| Frontend (Next.js) | Vercel | Hobby | Gratis |
| Backend (Spring Boot) | Render | Free | Gratis (750h/mes) |
| PostgreSQL | Neon | Free | Gratis (0.5GB, auto-pause) |
| Email | Gmail SMTP + App Password | — | Gratis (500/día) |
| Fotos del odómetro | ImgBB | Free | Gratis ilimitado |
| reCAPTCHA v3 | Google | Free | Gratis |
| Google OAuth | Google | Free | Gratis |
| Keep-alive backend | UptimeRobot | Free | Gratis (50 monitors) |

Sin tarjeta de crédito en ningún punto.

---

## 0. Antes de empezar

Necesitas un repositorio en GitHub con el código. Si aún no lo tienes:

```bash
cd C:\Users\sebas\OneDrive\Documents\PersonalProjects\km-cost-tracker
gh repo create trayecto --public --source=. --remote=origin --push
```

O por la UI de GitHub → New repository → push desde local.

---

## 1. Crear cuentas externas (10 min, sin tarjeta)

Crea cuenta en cada uno de estos servicios. **Todos gratis, sin verificación
de pago**:

1. **Neon** — https://neon.tech (PostgreSQL) → con GitHub
2. **Render** — https://render.com (backend host) → con GitHub
3. **Vercel** — https://vercel.com (frontend host) → con GitHub
4. **ImgBB** — https://imgbb.com (storage fotos) → con email
5. **Google Cloud Console** — https://console.cloud.google.com (OAuth) — necesitas Google account
6. **Google reCAPTCHA Admin** — https://www.google.com/recaptcha/admin/create
7. **UptimeRobot** — https://uptimerobot.com (keep-alive) → con email

---

## 2. Configurar Neon (Postgres)

1. Login → Create project → nombre `trayecto` → región más cercana al backend.
2. Tras crear, en el dashboard verás un connection string tipo:
   ```
   postgresql://user:pass@ep-xxx.us-east-2.aws.neon.tech/trayecto?sslmode=require
   ```
3. **Anota estos tres valores** (los necesitas en Render):
   - `DATABASE_URL`: convierte la URL prepending `jdbc:` →
     `jdbc:postgresql://ep-xxx.us-east-2.aws.neon.tech/trayecto?sslmode=require`
   - `DATABASE_USER`: parte antes del `:`
   - `DATABASE_PASSWORD`: parte entre `:` y `@`

> Neon auto-pausa la DB tras 5 min de inactividad. El primer query tras pausa
> toma ~1 segundo en despertar — invisible para el usuario.

---

## 3. Configurar Google OAuth (5 min)

Para "Login con Google" en producción.

1. https://console.cloud.google.com → New Project → nombre `trayecto`.
2. Menu → "APIs & Services" → "OAuth consent screen":
   - User type: **External** → Create
   - App name: `Trayecto`
   - User support email: el tuyo
   - Developer email: el tuyo
   - Save
3. Credentials → "Create Credentials" → "OAuth client ID":
   - Type: **Web application**
   - Name: `trayecto-prod`
   - **Authorized redirect URI**: déjalo en blanco por ahora — lo seteás tras tener el dominio de Render.
4. Anota: `Client ID` y `Client Secret`.

---

## 4. Configurar reCAPTCHA v3 (2 min)

1. https://www.google.com/recaptcha/admin/create
2. Label: `Trayecto`
3. Type: **reCAPTCHA v3**
4. Domains: agrega `localhost`, `trayecto.vercel.app` (lo que sea tu dominio de Vercel) — puedes editarlos después.
5. Anota: `Site Key` (pública) y `Secret Key` (privada).

---

## 5. Configurar Gmail SMTP (3 min)

Para que el backend envíe emails de verificación, reset password, etc.

1. https://myaccount.google.com/security → activa **2-Step Verification** si no la tienes.
2. https://myaccount.google.com/apppasswords → "App passwords"
3. App: "Mail" · Device: "Other" → "Trayecto" → Generate
4. Anota la **App Password de 16 caracteres** (cópiala sin espacios).

> Quota: 500 emails/día. Suficiente para portfolio.

---

## 6. Configurar ImgBB (1 min)

Para subir las fotos del odómetro (opcional). El frontend ya está
implementado para usar este servicio.

1. https://imgbb.com → Sign up con email
2. https://api.imgbb.com → "Get API key"
3. Anota la API key.

---

## 7. Deploy del backend a Render (10 min)

### Crear el servicio

1. Login en Render → **New + → Blueprint**.
2. Connect repository → selecciona tu repo `trayecto`.
3. Render detecta `trayecto-api/render.yaml` y propone crear el servicio.
4. Click "Apply" → Render arranca el primer build.

### Configurar env vars

Mientras buildea, ve al servicio creado → **Environment** y completa los
valores marcados como `sync: false` en `render.yaml`:

| Variable | Valor |
|---|---|
| `DATABASE_URL` | URL Neon con `jdbc:` prefix |
| `DATABASE_USER` | usuario de Neon |
| `DATABASE_PASSWORD` | password de Neon |
| `CORS_ALLOWED_ORIGINS` | `https://trayecto.vercel.app` (lo configuras tras el deploy de Vercel) |
| `APP_URL` | `https://trayecto.vercel.app` |
| `COOKIE_DOMAIN` | déjala **vacía** si usas `*.vercel.app` + `*.onrender.com` |
| `GOOGLE_OAUTH_CLIENT_ID` | de paso 3 |
| `GOOGLE_OAUTH_CLIENT_SECRET` | de paso 3 |
| `RECAPTCHA_SECRET_KEY` | de paso 4 (secret) |
| `BREVO_SMTP_USER` | tu-email@gmail.com |
| `BREVO_SMTP_KEY` | App Password de paso 5 (sin espacios) |
| `MAIL_FROM` | tu-email@gmail.com |

(`CLOUDINARY_*` se quedan vacíos — el frontend ya no usa Cloudinary.)

### Verificar deploy

1. Render hace primer build ~5-7 min (compila Java, baja deps, empaqueta Docker).
2. Cuando pase a "Live" estado verde, **anota la URL** que te dio Render:
   `https://trayecto-api.onrender.com` (o similar).
3. Test rápido: abre `https://<tu-url>/actuator/health/liveness` → debería responder `{"status":"UP"}`.

### Completar Google OAuth con la URL de Render

Ahora que tienes la URL, vuelve a Google Cloud Console (paso 3) y agrega como
**Authorized redirect URI**:
```
https://trayecto-api.onrender.com/login/oauth2/code/google
```

---

## 8. Deploy del frontend a Vercel (5 min)

1. Login en Vercel → **Add New → Project**.
2. Import tu repo `trayecto`.
3. **Framework Preset**: Next.js (auto-detect).
4. **Root Directory**: `trayecto-web` ← importante.
5. **Environment Variables** → agrega:

   | Variable | Valor |
   |---|---|
   | `NEXT_PUBLIC_API_URL` | `https://trayecto-api.onrender.com` (de paso 7) |
   | `NEXT_PUBLIC_WS_URL` | `wss://trayecto-api.onrender.com/ws` (nota el `wss://`) |
   | `NEXT_PUBLIC_RECAPTCHA_SITE_KEY` | site key de paso 4 |
   | `NEXT_PUBLIC_IMGBB_API_KEY` | API key de paso 6 |

6. Click **Deploy** → tarda ~3-4 min.
7. Vercel te da una URL tipo `https://trayecto.vercel.app`.

### Actualizar CORS en Render

Vuelve a Render → tu servicio → Environment → setea `CORS_ALLOWED_ORIGINS`
con la URL exacta de Vercel:
```
CORS_ALLOWED_ORIGINS=https://trayecto.vercel.app
APP_URL=https://trayecto.vercel.app
```
Y dispara un re-deploy manual (Manual Deploy → Deploy latest commit).

---

## 9. Keep-alive con UptimeRobot (2 min)

Render free duerme tras 15 min sin requests. UptimeRobot lo mantiene caliente
pingando el endpoint de health.

1. Login UptimeRobot → **New Monitor**.
2. Monitor Type: **HTTP(s)**
3. Friendly Name: `Trayecto API`
4. URL: `https://trayecto-api.onrender.com/actuator/health/liveness`
5. Monitoring Interval: **5 min** (free permite mínimo 5).
6. Create monitor.

> Render permite 750h/mes. Con UptimeRobot pingando cada 5 min el servicio
> queda activo 24/7, eso son ~720h/mes — entra en el free tier por los pelos.

---

## 10. Smoke test en producción (5 min)

1. Abre `https://trayecto.vercel.app/register`
2. Registra cuenta nueva con un email real.
3. Verifica que llega el email (revisa spam si no aparece en 1 min).
4. Click en el link → verificación OK → login con la nueva cuenta.
5. Crea un viaje, ciérralo, ve a `/analytics` — KPIs deberían actualizarse.

Si todo OK: **deploy completo**. ¡Listo!

---

## CI/CD automático

A partir de ahora:
- **Push a `main`** → GitHub Actions corre tests → Vercel y Render rebuild + redeploy.
- Si los tests fallan, ambos servicios siguen sirviendo la versión anterior — no se rompe prod.
- Los PRs corren CI antes del merge → bloqueas merge si CI rojo.

Los workflows están en `.github/workflows/api.yml` (backend) y `web.yml` (frontend).

---

## Troubleshooting común

### "CORS error" al hacer login
- Verifica que `CORS_ALLOWED_ORIGINS` en Render tiene **exactamente** la URL de Vercel, sin `/` al final.
- Re-deploy Render tras cambiar la var (no se aplica caliente).

### "Refresh token cookie not found"
- Verifica que el frontend está sirviendo en **HTTPS** (Vercel sí lo hace por defecto).
- Si usas Brave/Safari con "third-party cookies blocked": el navegador puede estar bloqueando la cookie cross-site. Recomienda Chrome para demo.

### Email no llega
- Gmail SMTP envía pero a veces tarda 30-60s en propagar.
- Si nunca llega: verifica que `BREVO_SMTP_KEY` es la App Password (sin espacios), no tu password normal.

### Cold start lento (~30s primer request)
- Es esperado en Render free.
- UptimeRobot lo mitiga pingando cada 5 min.
- Para portfolio demo: pinga manualmente el `/actuator/health/liveness` 1 min antes.

### Backend devuelve 500 en `/api/v1/auth/login`
- Revisa los logs en Render dashboard → Logs.
- Probable: `DATABASE_URL` mal formateada (falta `jdbc:` prefix o `?sslmode=require`).

---

## Custom domain (opcional, no obligatorio)

Si compras un dominio (ej. `trayecto.app` en Namecheap, ~$10/año):

1. Vercel: Settings → Domains → Add `trayecto.app` y `www.trayecto.app`.
2. Render: Settings → Custom Domain → Add `api.trayecto.app`.
3. En tu DNS provider:
   - `trayecto.app` → CNAME a `cname.vercel-dns.com`
   - `api.trayecto.app` → CNAME a la URL de Render
4. En Render: setea `COOKIE_DOMAIN=.trayecto.app` (con el punto inicial) y redeploy.
5. En Vercel: actualiza `NEXT_PUBLIC_API_URL=https://api.trayecto.app`.

Con esto, el route guard del middleware (proxy.ts) verá la cookie del refresh
también en el frontend → la app se siente como una sola unidad.
