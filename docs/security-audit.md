# Auditoría de seguridad — condominium-app

> **Confidencial.** Hallazgos sin corregir. **No publicar** esta rama mientras el repo sea público y haya hallazgos
> abiertos que faciliten tomar cuentas.

- **Fecha:** 2026-10-24
- **Alcance:** rama `feature/ui-audit-fixes` (base `dev`), `shared/`, `androidApp/`, recursos web, build e historial
  de git. Análisis estático, solo lectura.
- **Hallazgos del backend:** en `condominium-api`, `docs/security-audit.md` (rama `docs/security-audit`). Los graves
  (Crítica/Alta) están allí.

Estados: ⬜ Pendiente · 🔄 En curso · ✅ Hecho · ⏸️ Descartado (con motivo).

## Acción operativa inmediata

| # | Acción | Estado |
|---|---|---|
| OP-1 | Rotar el token de Vercel que está en `local.properties` (ver APP-M2) y dejarlo solo en GitHub Secrets. | ⬜ |

## Media

### APP-M1 — Logs de red con BODY independientes del tipo de build — ⬜
- **Dónde:** `shared/src/androidMain/.../di/AppModule.kt:31`; `shared/build.gradle.kts:139`; flag
  `ENABLE_NETWORK_LOGS` en `local.properties`.
- **Problema:** `HttpLoggingInterceptor.Level.BODY` registra en logcat `Authorization: Bearer …` (sin `redactHeader`),
  contraseñas de login y cambio de contraseña, y respuestas con datos personales. El flag no depende de debug/release:
  un release compilado con el flag activo lo trae.
- **Escenario:** bug report, adb o recolector de logs del fabricante obtiene el token; con BE-M1 del backend es toma de
  cuenta.
- **Arreglo:** activar solo si `ENABLE_NETWORK_LOGS && debug` (constante por variante o `FLAG_DEBUGGABLE`),
  `redactHeader("Authorization")`, nunca BODY en release.
- **Confianza:** confirmado en código.

### APP-M2 — Token de Vercel en texto plano en `local.properties` — ⬜
- **Dónde:** `local.properties` (no versionado; nunca entró al historial de git).
- **Problema:** token de despliegue en un archivo que leen herramientas y agentes; durante esta auditoría apareció en
  la salida de una herramienta.
- **Arreglo:** OP-1.

### APP-M3 — Web: JWT en `localStorage`, sin CSP, enlaces abiertos sin filtrar el esquema — ⬜
- **Dónde:** `shared/src/wasmJsMain/.../LocalStorageTokenManager.kt:9-11`; `shared/src/wasmJsMain/resources/vercel.json`
  (sin CSP ni cabeceras); `BillboardDetailScreen.kt:153,197,461` (`uriHandler.openUri`, Markdown);
  `Pickers.wasmJs.kt:22` (`window.open(url)`).
- **Escenario:** un anuncio con enlace Markdown `javascript:` (o `attachment_url`/`proof_url` controlables) ejecuta
  código en el origen de la app y lee `jwt_token`.
- **Arreglo:** lista blanca `https`/`http` antes de `openUri`/`window.open`; `onLinkClick` propio en el Markdown; CSP
  estricta, `X-Content-Type-Options` y `frame-ancestors 'none'` en `vercel.json`.
- **Confianza:** verificar si mikepenz-markdown 0.41 pasa `javascript:` al uriHandler.

## Baja

- **APP-B1 — Tráfico en claro permitido en release** (`androidApp/src/main/AndroidManifest.xml:19`,
  `usesCleartextTraffic="true"`). Ponerlo a `false` o `network_security_config` con HTTP solo en debug. ⬜
- **APP-B2 — `println` en producción** (`RemoteAuthRepository.kt:37,42,67`, `RemotePaymentRepository.kt:127`,
  `ApiServiceImpl.kt:36`): email del login, inicio del token, cuerpos de error. Quitar o logger solo debug. ⬜
- **APP-B3 — Logout solo local** (`RemoteAuthRepository.kt:195-198`): el access token sigue válido en servidor;
  `refresh_token` sin uso. Llamar a signOut cuando el backend lo ofrezca. ⬜
- **APP-B4 — Segmentos de ruta y nombres de archivo sin codificar** (`ApiServiceImpl.kt:103,107` `by-code/$code` desde
  QR; `:170,259` `filename` en Content-Disposition). Un QR con `../` redirige a otro endpoint. `encodeURLPathPart` y
  sanear nombre. Probable. ⬜

## Info (revisado, sin problema o a considerar)

- Sesión Android en DataStore sin cifrar en `filesDir`; las reglas de backup solo incluyen `sharedpref` y `database`,
  así que el token no se respalda.
- FileProvider no exportado (solo `cache-path`); solo la actividad LAUNCHER exportada; sin deep links.
- `sw.js:87` solo cachea GET del mismo origen; nunca respuestas de la API.
- 401 en `/users/me` borra el token (`RemoteAuthRepository.kt:157`).
- Release con `isMinifyEnabled=false` (`androidApp/build.gradle.kts:207`).
- IDs de AdMob de prueba de Google (`AndroidManifest.xml:27`, `AppOpenAdManager.kt:18`): cambiar antes de publicar.
- Keystore y `keystore.properties` en `.gitignore`; el `local.properties` versionado en 41050672 solo tenía `sdk.dir`.
- Dependencias (ktor 3.3.3, okhttp-logging 4.12.0, coil 3.4.0, mikepenz-markdown 0.41.0): sin CVE identificado con
  certeza; verificar con OSV o Dependabot.

## No revisado

- Target iOS (no hay proyecto Xcode en el repo).
- Comportamiento real del renderer Markdown con `javascript:` (APP-M3).
