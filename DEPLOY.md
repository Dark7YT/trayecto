# Deploy de Trayecto a producción

Guía paso a paso para pasar de `localhost` a una URL pública usando solo tiers
gratuitos. Tiempo estimado: **60–90 min la primera vez** (después solo
`git push` y todo se redeploya solo).

> Si algo falla en el camino, salta directo a [Troubleshooting](#troubleshooting)
> al final — los errores más comunes están resueltos ahí.

---

## Arquitectura de deploy

Tenemos **dos repos** en GitHub y **dos hosts** distintos (la división es a
propósito: frontend y backend escalan diferente y Vercel/Render son los
mejores tiers gratuitos para cada uno):

```
GitHub                                 Producción
──────────────────────                 ───────────────────────────────
Dark7YT/trayecto       ── push ──►    Render        (Spring Boot API)
  └ trayecto-api/                       trayecto-api.onrender.com
  └ render.yaml
  └ .github/workflows/api.yml

Dark7YT/trayecto-web   ── push ──►    Vercel        (Next.js frontend)
  └ src/, public/                       trayecto.vercel.app
  └ vercel.json
  └ .github/workflows/web.yml

                                       Neon          (PostgreSQL 17)
                                       ImgBB         (fotos del odómetro)
                                       Gmail SMTP    (emails)
                                       Google Cloud  (OAuth + reCAPTCHA)
                                       UptimeRobot   (keep-alive del backend)
```

**Stack final**:

| Componente | Proveedor | Plan | Costo |
|---|---|---|---|
| Frontend (Next.js 16) | Vercel | Hobby | Gratis |
| Backend (Spring Boot 4) | Render | Free | Gratis (750h/mes) |
| PostgreSQL | Neon | Free | Gratis (0.5GB, auto-pause) |
| Email | Gmail SMTP + App Password | — | Gratis (500/día) |
| Fotos del odómetro | ImgBB | Free | Gratis ilimitado |
| reCAPTCHA v3 | Google | Free | Gratis |
| Google OAuth | Google | Free | Gratis |
| Keep-alive backend | UptimeRobot | Free | Gratis (50 monitors) |

**Sin tarjeta de crédito en ningún punto del proceso.**

---

## 0. Pre-flight: subir el código a GitHub

Antes de tocar Render/Vercel, asegurate de que ambos repos están actualizados.

### 0.1 Verificar estado local

Desde la raíz del proyecto (`km-cost-tracker/`):

```bash
git status
git log --oneline -3
```

Tiene que decir `On branch master` y los últimos 3 commits deben incluir los
fixes de deploy. Si ves `Your branch is ahead of 'origin/master' by N commit(s)`,
falta pushear.

### 0.2 Pushear el backend

```bash
git add .github/workflows/api.yml trayecto-api/
git commit -m "chore(deploy): backend ready for Render"  # solo si hay cambios
git push origin master
```

Verifica en GitHub que `https://github.com/Dark7YT/trayecto` tiene:
- `trayecto-api/Dockerfile`
- `trayecto-api/render.yaml`
- `trayecto-api/.dockerignore`
- `.github/workflows/api.yml`

### 0.3 Pushear el frontend

```bash
cd trayecto-web
git status
git add .github/workflows/web.yml .env.local.example vercel.json
git commit -m "chore(deploy): frontend ready for Vercel"  # solo si hay cambios
git push origin master
cd ..
```

Verifica en GitHub que `https://github.com/Dark7YT/trayecto-web` tiene:
- `vercel.json`
- `.env.local.example`
- `.github/workflows/web.yml`
- `src/`, `public/`, `package.json`, etc.

> **Nota sobre el gitlink huérfano**: el outer repo muestra `trayecto-web`
> como "modified content" en `git status`. Es un gitlink residual del setup
> inicial — no afecta el deploy (Render solo lee `trayecto-api/`). Si querés
> limpiarlo después:
> ```bash
> git rm --cached trayecto-web
> git commit -m "chore: remove stale gitlink to frontend submodule"
> git push origin master
> ```

---

## 1. Crear todas las cuentas externas (15 min)

Hacelo todo de una para tener las pestañas abiertas y copy-pasteable.
Todas son gratis y NO piden tarjeta.

| # | Servicio | URL | Login con |
|---|---|---|---|
| 1 | **Neon** (Postgres) | https://neon.tech | GitHub |
| 2 | **Render** (backend) | https://render.com | GitHub |
| 3 | **Vercel** (frontend) | https://vercel.com | GitHub |
| 4 | **ImgBB** (fotos) | https://imgbb.com | email |
| 5 | **Google Cloud Console** (OAuth) | https://console.cloud.google.com | Google |
| 6 | **reCAPTCHA Admin** | https://www.google.com/recaptcha/admin/create | Google |
| 7 | **UptimeRobot** (keep-alive) | https://uptimerobot.com | email |

---

## 2. Crear la base de datos en Neon

1. Login en Neon → botón **"Create project"** arriba a la derecha.
2. Configura:
   - **Project name**: `trayecto`
   - **Postgres version**: 17 (la default sirve)
   - **Region**: la más cercana a Render. Si Render va a usar `oregon`
     (us-west), elegí `us-west-2`. Si vas a usar `sa-east-1` (São Paulo, más
     cercano a Perú), elegí `sa-east-1` también — y cambiá `region: oregon`
     por `region: ohio` o lo que esté disponible en `trayecto-api/render.yaml`.
3. Tras crear, te abre el **Dashboard → "Connection string"**. Vas a ver algo así:
   ```
   postgresql://neondb_owner:npg_AbCdEfGh1234@ep-ancient-queen-ac7g4c32-pooler.sa-east-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require
   ```
4. **Anota estos 3 valores por separado** (los vas a necesitar al configurar Render):

   | Variable | Cómo se construye |
   |---|---|
   | `DATABASE_URL` | `jdbc:postgresql://` + el host + el `/neondb?...`. **Borra las credenciales del medio**. Ejemplo: `jdbc:postgresql://ep-ancient-queen-ac7g4c32-pooler.sa-east-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require` |
   | `DATABASE_USER` | Lo que está entre `postgresql://` y `:`. Ejemplo: `neondb_owner` |
   | `DATABASE_PASSWORD` | Lo que está entre `:` y `@`. Ejemplo: `npg_AbCdEfGh1234` |

> **CRÍTICO**: la URL JDBC NO debe incluir credenciales. Spring Boot las toma
> de las dos vars separadas. Si las dejás en la URL, Hikari puede confundirse
> y el backend no arranca.

5. Neon auto-pausa la DB tras 5 min sin queries. El primer query post-pausa
   tarda ~1 segundo en despertar — invisible para el usuario.

---

## 3. Configurar Google OAuth (5 min)

Para "Login con Google" en producción.

1. Andá a https://console.cloud.google.com → **"Select a project"** arriba →
   **"NEW PROJECT"**.
   - **Project name**: `trayecto`
   - Click **Create**.

2. Menu hamburguesa → **"APIs & Services"** → **"OAuth consent screen"**:
   - **User type**: External → Create.
   - **App name**: `Trayecto`
   - **User support email**: tu email
   - **Developer contact**: tu email
   - **Save and continue** → Save todos los siguientes pasos sin agregar nada.
   - Volvé al dashboard.

3. **"Credentials"** → **"+ CREATE CREDENTIALS"** → **"OAuth client ID"**:
   - **Application type**: Web application
   - **Name**: `trayecto-prod`
   - **Authorized JavaScript origins**: dejá vacío por ahora (lo agregás
     después de tener la URL de Vercel).
   - **Authorized redirect URIs**: dejá vacío por ahora (lo agregás después
     de tener la URL de Render).
   - Click **Create**.

4. **Anota** los dos valores que te muestra el popup:
   - **Client ID** → será `GOOGLE_OAUTH_CLIENT_ID` en Render.
   - **Client secret** → será `GOOGLE_OAUTH_CLIENT_SECRET` en Render.

(Volveremos en el [Paso 7](#7-completar-google-oauth) a agregar la redirect URI
una vez que Render nos dé la URL del backend.)

---

## 4. Configurar reCAPTCHA v3 (2 min)

Para anti-bot en register/login.

1. Andá a https://www.google.com/recaptcha/admin/create
2. Configura:
   - **Label**: `Trayecto`
   - **reCAPTCHA type**: **reCAPTCHA v3** (no v2)
   - **Domains** (uno por línea):
     ```
     localhost
     trayecto.vercel.app
     ```
     (Si después usás custom domain, lo agregás acá.)
   - **Owners**: tu email
   - **Accept the Terms of Service** ✓
   - Click **Submit**.

3. **Anota los dos valores** que te muestra:
   - **Site Key** (pública) → será `NEXT_PUBLIC_RECAPTCHA_SITE_KEY` en Vercel.
   - **Secret Key** (privada) → será `RECAPTCHA_SECRET_KEY` en Render.

---

## 5. Configurar Resend para envío de emails (3 min)

> **¿Por qué Resend y no Gmail SMTP?** Render free tier **bloquea los puertos
> SMTP outbound** (25, 465, 587) para evitar abuso. Cualquier provider SMTP
> (Gmail, Brevo, SendGrid) falla con timeout. Resend ofrece API HTTP
> (`POST /emails`) que pasa el bloqueo. Quota free: **100 emails/día = 3.000/mes**.

### 5.1 Crear cuenta

1. Abrí **https://resend.com** → **Sign up** (con GitHub o email).
2. Confirma tu email (link de bienvenida).

### 5.2 Crear API Key

1. En el dashboard, sidebar izquierdo → **API Keys**.
2. Click **"+ Create API Key"**.
3. Configurá:
   - **Name**: `Trayecto Production`
   - **Permission**: Full access (default).
   - **Domain**: `All domains`.
4. Click **Add**. Te muestra la key UNA vez, con formato:
   ```
   re_AbCdEfGh_1234567890XYZ
   ```
5. **Copiala a tu txt YA**:
   ```
   RESEND_API_KEY=re_AbCdEfGh_1234567890XYZ
   ```

### 5.3 Remitente sin dominio propio

Resend te permite enviar **desde `onboarding@resend.dev`** sin verificar nada,
pero **solo al email verificado en tu cuenta** (el que usaste al registrarte).
Para portfolio donde solo vos vas a probar, es perfecto.

Anotá en tu txt:
```
MAIL_FROM=Trayecto <onboarding@resend.dev>
```

> **Si en el futuro compras un dominio**: en Resend → Domains → Add Domain →
> agregás un registro DNS TXT + uno DKIM. Después podés cambiar `MAIL_FROM` a
> `noreply@tudominio.com` y enviar a cualquier dirección sin restricciones.

---

## 6. Configurar ImgBB (1 min)

Para subir las fotos del odómetro. El frontend sube directo desde el browser,
sin pasar por el backend (ahorra ancho de banda en Render).

1. https://imgbb.com → **Sign up** con email (no necesitás verificar nada para
   obtener API key).
2. Una vez dentro, andá a https://api.imgbb.com → click **"Get API key"**.
3. **Anota** la API key → será `NEXT_PUBLIC_IMGBB_API_KEY` en Vercel.

> ImgBB tier free es ilimitado (no hay quota mensual documentada). Si en algún
> momento empiezan a 429, podés migrar a Cloudinary free tier (25GB).

---

## 7. Deploy del backend a Render (15 min)

### 7.1 Crear el servicio via Blueprint

1. Login en Render → **"+ New"** arriba → **"Blueprint"**.
2. **Connect your GitHub account** si no lo hiciste antes → autorizá el repo
   `Dark7YT/trayecto`.
3. Render escanea el repo y encuentra `trayecto-api/render.yaml` →
   **"Apply"**.
4. Render arranca el primer build automáticamente (~5–8 min: compila Java,
   descarga deps Maven, construye imagen Docker).

### 7.2 Configurar las env vars

**MIENTRAS** Render buildea (no esperes a que termine), andá a:

`tu-servicio` → **"Environment"** tab.

Vas a ver vars ya creadas con valores autogenerados (`JWT_SECRET`,
`SPRING_PROFILES_ACTIVE=prod`, `PORT=8080`, etc.) y otras con un placeholder
amarillo "Add a value". **Click "Add Environment Variable" o edita las
existentes** y completá esta tabla EXACTA:

| Key | Value | De dónde sale |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://ep-XXX.aws.neon.tech/neondb?sslmode=require&channel_binding=require` | Paso 2 (sin user:pass embebido) |
| `DATABASE_USER` | `neondb_owner` (o el tuyo) | Paso 2 |
| `DATABASE_PASSWORD` | `npg_AbCdEfGh1234` (el tuyo) | Paso 2 |
| `CORS_ALLOWED_ORIGINS` | `https://trayecto.vercel.app` | Lo vas a saber tras Paso 8. Por ahora poné un placeholder como `https://placeholder.local` — lo actualizás al final. |
| `APP_URL` | `https://trayecto.vercel.app` | Mismo placeholder por ahora |
| `COOKIE_DOMAIN` | (dejar **vacío**) | Solo se usa con custom domain |
| `GOOGLE_OAUTH_CLIENT_ID` | `135170...apps.googleusercontent.com` | Paso 3 |
| `GOOGLE_OAUTH_CLIENT_SECRET` | `GOCSPX-...` | Paso 3 |
| `RECAPTCHA_SECRET_KEY` | `6Ldzbv...` | Paso 4 (Secret Key, no Site Key) |
| `RESEND_API_KEY` | `re_AbCdEfGh_...` | Paso 5 |
| `MAIL_FROM` | `Trayecto <onboarding@resend.dev>` | Paso 5 (placeholder de Resend sin dominio propio) |

**No toques**: `JWT_SECRET` (auto-generada), `JWT_ACCESS_TTL`, `JWT_REFRESH_TTL`,
`PORT`, `SPRING_PROFILES_ACTIVE`, `APP_MAIL_PROVIDER` (=resend),
`BREVO_SMTP_HOST`, `BREVO_SMTP_PORT`, `RECAPTCHA_MIN_SCORE`. Render las setea
según `render.yaml`.

**Dejá vacías** (inertes en prod con Resend): `BREVO_SMTP_USER`,
`BREVO_SMTP_KEY`, `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`,
`CLOUDINARY_API_SECRET`.

> Después de agregar/cambiar vars, Render dispara un **redeploy automático**.
> Si el primer build aún no terminó, el nuevo build cancela el anterior — eso es OK.

### 7.3 Verificar deploy

1. Esperá a que el servicio pase a estado **"Live"** (verde, arriba a la derecha).
2. **Anotá la URL** que te dio Render: algo como
   `https://trayecto-api.onrender.com` (puede tener un sufijo random tipo
   `trayecto-api-abc123.onrender.com` — usá el que aparezca).
3. Test de health rápido — abrí en el navegador:
   ```
   https://<tu-url-de-render>/actuator/health/liveness
   ```
   Debe responder:
   ```json
   {"status":"UP"}
   ```
4. Test de DB — abrí:
   ```
   https://<tu-url-de-render>/actuator/health
   ```
   Debe mostrar `"db":{"status":"UP"}` dentro. Si dice `DOWN`, revisá las vars
   de `DATABASE_*` (es lo más común — credenciales mal copiadas o URL con
   prefix `jdbc:` faltante).

---

## 8. Completar Google OAuth con la URL de Render

Ahora que tenés la URL del backend, volvé a https://console.cloud.google.com
→ tu proyecto `trayecto` → **APIs & Services** → **Credentials** → click en
el OAuth client `trayecto-prod`.

**Authorized redirect URIs** → **+ ADD URI**:
```
https://trayecto-api.onrender.com/login/oauth2/code/google
```
(Usá tu URL real de Render.)

Click **SAVE**. Tarda ~30s en propagarse.

---

## 9. Deploy del frontend a Vercel (8 min)

### 9.1 Importar el repo

1. Login en Vercel → **"+ Add New..."** → **"Project"**.
2. **Import Git Repository** → selecciona `Dark7YT/trayecto-web`.
   Si no aparece, click **"Adjust GitHub App Permissions"** y autoriza el repo.
3. Vercel detecta Next.js automáticamente. Configurá:
   - **Framework Preset**: Next.js (auto)
   - **Root Directory**: `./` (raíz del repo — `trayecto-web` ya ES la raíz)
   - **Build Command**: `npm run build` (default, sirve)
   - **Output Directory**: `.next` (default, sirve)
   - **Install Command**: `npm ci` (default, sirve)

### 9.2 Environment Variables

Expandí la sección **"Environment Variables"** y agregá:

| Key | Value | De dónde sale |
|---|---|---|
| `NEXT_PUBLIC_API_URL` | `https://trayecto-api.onrender.com` | URL de Render del Paso 7.3 |
| `NEXT_PUBLIC_WS_URL` | `wss://trayecto-api.onrender.com/ws` | **WSS**, no WS (HTTPS = WSS) |
| `NEXT_PUBLIC_RECAPTCHA_SITE_KEY` | `6Lc...` | Paso 4 (Site Key, no Secret) |
| `NEXT_PUBLIC_IMGBB_API_KEY` | `febfc6...` | Paso 6 |

Click **Deploy** abajo a la derecha.

### 9.3 Verificar deploy

1. Vercel buildea (~2–4 min para Next 16 + Tailwind 4).
2. Cuando termine, click **"Visit"** → te da una URL tipo
   `https://trayecto-web.vercel.app` o `https://trayecto.vercel.app` (depende
   del nombre del proyecto).
3. **Anotá la URL exacta** — la vas a meter en Render en el siguiente paso.

---

## 10. Cerrar el círculo: actualizar CORS en Render

El backend ahora necesita saber qué frontend tiene permiso para hablarle.

1. Volvé a Render → tu servicio → **Environment**.
2. Edita estas dos vars con la URL EXACTA de Vercel (sin slash final):
   ```
   CORS_ALLOWED_ORIGINS=https://trayecto-web.vercel.app
   APP_URL=https://trayecto-web.vercel.app
   ```
3. Click **"Save Changes"** → Render redeploya automáticamente (~2 min).
4. (Opcional) Si Vercel te da dos URLs (`trayecto-web-xyz.vercel.app` para
   preview y `trayecto-web.vercel.app` canónica), poné las dos separadas por coma:
   ```
   CORS_ALLOWED_ORIGINS=https://trayecto-web.vercel.app,https://trayecto-web-xyz.vercel.app
   ```

---

## 11. Keep-alive con UptimeRobot (3 min)

Render free duerme tras 15 min sin requests. El primer request post-sueño
tarda ~30s en responder (cold start de JVM + Hibernate). UptimeRobot lo
mantiene caliente pingando el endpoint de health cada 5 min.

1. Login en UptimeRobot → **"+ Add New Monitor"**.
2. Configurá:
   - **Monitor Type**: **HTTP(s)**
   - **Friendly Name**: `Trayecto API`
   - **URL (or IP)**: `https://trayecto-api.onrender.com/actuator/health/liveness`
   - **Monitoring Interval**: **5 minutes** (el mínimo del free tier)
3. Click **Create Monitor**.

Costo de horas en Render:
- 5 min × (60/5 pings/h) × 24h × 30 días = ~720h/mes
- Render free permite 750h/mes → entra por los pelos.

> Si Render llega al límite a fin de mes y duerme: no es un drama, el siguiente
> ping de UptimeRobot lo despierta y resetea la cuenta al primer día del mes
> siguiente. Para portfolio demo es aceptable.

---

## 12. Smoke test end-to-end (5 min)

1. Abrí `https://trayecto-web.vercel.app/register` (tu URL real).
2. **Registrate** con un email real:
   - Nombre: el tuyo
   - Email: uno tuyo que puedas revisar
   - Password: 8+ chars
   - Acepta reCAPTCHA (debería ser invisible v3).
3. **Verificá el email**: chequea inbox (también spam) — debe llegar en <1 min.
   Click en el link → te redirige a la app verificado.
4. **Login** con la cuenta nueva.
5. **Login con Google** (logout primero) — debería abrir el popup de Google y
   loguearte con tu cuenta.
6. **Crea un viaje**:
   - Botón "+ Nuevo viaje"
   - Llena datos, opcional subí una foto del odómetro (debería subirse a ImgBB).
   - Cerralo (marca el odómetro final).
7. **Verifica analytics**: andá a `/analytics` → los KPIs deben mostrar tu viaje.
8. **Verifica notificaciones**: la campana arriba debería tener al menos una
   (la de "Email verificado" o "Bienvenida").

Si TODO pasa: **🎉 deploy completo y funcionando.**

A partir de ahora:
- **`git push origin master` en `Dark7YT/trayecto`** → Render rebuilda + redeploya el backend.
- **`git push origin master` en `Dark7YT/trayecto-web`** → Vercel rebuilda + redeploya el frontend.
- Los GitHub Actions corren tests/typecheck antes — si fallan, los servicios siguen sirviendo la versión anterior, prod no se rompe.

---

## Troubleshooting

### Render: "Exited with status 1" justo al arrancar

Casi siempre es un placeholder `${VAR}` sin resolver. Mirá los logs entre
`The following 1 profile is active: "prod"` y `Exited with status 1`. Buscá
líneas con `Could not resolve placeholder` — ahí está el nombre exacto de la
var faltante.

Vars más comúnmente olvidadas:
- `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`
- `BREVO_SMTP_USER`, `BREVO_SMTP_KEY`, `MAIL_FROM`
- `RECAPTCHA_SECRET_KEY`

### Render: "Connection refused" o "DB unreachable" en `/actuator/health`

1. Verificá que `DATABASE_URL` empiece con **`jdbc:postgresql://`** (con el `jdbc:`).
2. Verificá que **NO** tenga credenciales embebidas (`user:pass@`).
3. Verificá la región: si Neon está en `sa-east-1` y Render en `oregon`, las
   queries van a tardar ~150ms. No es bloqueante, pero ideal mismas regiones.
4. Probá la URL Neon con `psql` localmente para confirmar que las credenciales
   funcionan.

### "CORS error" en el browser al hacer login

1. Abrí DevTools → Network → mirá la request que falla.
2. Confirmá que el `Origin` header coincide EXACTO con `CORS_ALLOWED_ORIGINS`
   en Render (sin slash final, con `https://`).
3. Tras cambiar la var en Render, esperá el redeploy completo (no se aplica caliente).

### Email de verificación no llega

1. Esperá 60s — Gmail puede tardar en propagar.
2. Revisá spam / "Todos los mensajes".
3. Si nunca llega: probablemente `BREVO_SMTP_KEY` está mal:
   - ¿Es la App Password de 16 chars, NO tu password normal de Gmail?
   - ¿Sin espacios? La App Password se muestra con espacios, tenés que quitarlos al pegar.
4. Los logs de Render muestran `MailSendException` si el SMTP rechaza.

### "Refresh token cookie not found" tras login

Esto es 99% un problema de cookies cross-domain.

1. Verificá que el frontend está en HTTPS (Vercel sí lo está por default).
2. Verificá que estás usando Chrome/Edge (no Safari/Brave con "block third-party cookies").
3. Verificá en DevTools → Application → Cookies que aparece una cookie
   `refresh_token` con `SameSite=None` y `Secure=true`.
4. Si no aparece: el backend no la está seteando — revisá los logs del
   endpoint `/api/v1/auth/login`.

### Cold start de 30s la primera vez del día

Esperado en Render free. Mitigaciones:
- UptimeRobot pingando cada 5 min mantiene el JVM caliente.
- Para demo en vivo: pingá manualmente `/actuator/health/liveness` 1 min
  antes de la demo para warmup completo.

### Vercel: "Module not found" durante build

Probablemente una dep de dev en `dependencies` faltante, o lockfile
desactualizado. En local:
```bash
cd trayecto-web
rm -rf node_modules package-lock.json
npm install
git add package-lock.json
git commit -m "chore: refresh lockfile"
git push origin master
```

### Frontend abre pero `/api/v1/...` devuelve 401 inmediato

El access token no se está mandando o expiró. Casos:
1. `NEXT_PUBLIC_API_URL` apunta a un dominio que NO tiene los CORS configurados.
2. La cookie de refresh no llegó (ver problema de cookies arriba).
3. El backend rechaza el JWT — probablemente cambió `JWT_SECRET` entre deploys
   y los tokens viejos quedaron inválidos. Solución: logout + login.

### Notificaciones por WebSocket no llegan en producción

`NEXT_PUBLIC_WS_URL` debe ser **`wss://`** (con doble S), no `ws://`. HTTPS en
el frontend requiere WSS en el WebSocket.

---

## Apéndice A: Custom domain (opcional)

Si compras un dominio (ej. `trayecto.app` en Namecheap, ~$10/año):

1. **Vercel** → Settings → Domains:
   - Agregar `trayecto.app`
   - Agregar `www.trayecto.app`
2. **Render** → Settings → Custom Domain:
   - Agregar `api.trayecto.app`
3. **En tu DNS provider** (Namecheap/Cloudflare/etc):
   - `trayecto.app` → CNAME a `cname.vercel-dns.com`
   - `api.trayecto.app` → CNAME a `<tu-render-host>` (te lo da Render)
4. **En Render** → Environment:
   - `COOKIE_DOMAIN=.trayecto.app` (con el punto inicial)
   - `CORS_ALLOWED_ORIGINS=https://trayecto.app,https://www.trayecto.app`
   - `APP_URL=https://trayecto.app`
   - Save → redeploy.
5. **En Vercel** → Environment:
   - `NEXT_PUBLIC_API_URL=https://api.trayecto.app`
   - `NEXT_PUBLIC_WS_URL=wss://api.trayecto.app/ws`
   - Redeploy.
6. **En Google Cloud Console** → OAuth client:
   - Agregar `https://api.trayecto.app/login/oauth2/code/google` a redirect URIs.
   - Agregar `https://trayecto.app` a JavaScript origins.
7. **En reCAPTCHA Admin**:
   - Agregar `trayecto.app` a domains.

Con custom domain compartido, la cookie del refresh (en `.trayecto.app`) es
visible para el middleware del frontend → la app se siente como una unidad.

---

## Apéndice B: Checklist final pre-deploy

Antes de declarar "deploy listo", confirmá:

- [ ] Ambos repos en GitHub con últimos commits pusheados
- [ ] Render: `/actuator/health/liveness` → `{"status":"UP"}`
- [ ] Render: `/actuator/health` → `db: UP`
- [ ] Vercel: la página principal carga sin errores en consola
- [ ] Vercel: registro de nuevo usuario funciona end-to-end
- [ ] Email de verificación llega y el link funciona
- [ ] Login con Google funciona
- [ ] Crear un viaje funciona (con foto opcional)
- [ ] `/analytics` muestra KPIs después de cerrar el viaje
- [ ] La campana de notificaciones tiene al menos una notificación
- [ ] UptimeRobot está pingando cada 5 min y muestra "Up"
- [ ] (Opcional) GitHub Actions corre verde en ambos repos tras un push de prueba

Si todos los checkboxes están en ✓: **deploy 100% listo.** 🚀
