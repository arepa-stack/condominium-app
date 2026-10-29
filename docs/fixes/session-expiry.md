# Fix: sesión vencida (401) dejaba navegar con pantallas en error

Documento de traspaso para otro agente o persona. Estado al 2026-10-28.

## Estado
- **Código:** worktree `.claude/worktrees/agent-a08b033d8c380f580`, rama `worktree-agent-a08b033d8c380f580`, base
  `origin/dev` (`4b6057e`). **Sin commit.**
- **Tests:** 204 en verde (`./gradlew :shared:testDebugUnitTest -PapiEnv=DEV`), 17 en `SessionExpiryTest`.
- **Build:** compilan `:androidApp:assembleDebug` y `:shared:wasmJsBrowserDevelopmentExecutableDistribution`
  (`-PapiEnv=DEV`).
- **Prueba manual:** lista para re-probar (iteración 2: chequeo local del token y sin «blip»).

## Issue
- **Síntoma (web, reportado por el usuario):**
  1. Con la sesión vencida (o token inválido) se podía seguir navegando; las pantallas mostraban error y algunas un 401.
  2. Durante la prueba manual, si el usuario **borra** la clave `jwt_token` del `localStorage` (en vez de editarla a `"x"`)
     mientras está dentro de la app y navega a otra pantalla, no lo sacaba al login y las pantallas mostraban error de
     conexión (`CloudOff` + «Revisa tu conexión e inténtalo de nuevo»).
- **Esperado:** cualquier request autenticada que responda 401 —tanto si llevó un token vencido/inválido como si el token
  ya había sido eliminado del almacenamiento— cierra la sesión en memoria y lleva al login **una sola vez**, desde
  cualquier pantalla, limpiando el back stack. Un fallo de red o un 500 no cierran la sesión.
  3. (Iteración 2) Con el token borrado o vencido, al tocar otra pestaña la app **primero entraba a la pantalla destino**
     (carga o error) y ~1,5 s después, con el 401 del servidor, saltaba al login con animación («blip»). Cartelera sin
     usuario cargado no hacía request («No hay comunicados publicados») y nunca redirigía.
- **Android:** mismo código en `commonMain`, así que tenía el mismo comportamiento.

## Causa raíz (confirmada en código)
1. **Falta de manejo central de 401:** `ApiServiceImpl.safeRequest` convertía el 401 en `Response.error(401)` y los
   repositorios en `ApiFailureException(401)`; cada pantalla mostraba su propio `ErrorState` (con el ícono `CloudOff` y
   texto de conexión por defecto). Solo `RemoteAuthRepository.loadCurrentUser` borraba el token ante un 401 en el arranque.
2. **Borrado de `jwt_token` en `localStorage` (`sentToken == null`):** `LocalStorageTokenManager.getToken()` lee en vivo de
   `localStorage.getItem("jwt_token")`. Si se borra la clave antes de la siguiente request autenticada (`authenticated = true`),
   `sessionExpiry.currentToken()` devuelve `null`, la request sale sin header `Authorization` y el backend responde `401`.
   Como `SessionExpiry.onUnauthorized` tenía `if (sentToken == null) return`, ignoraba ese `401` asumiendo que una request
   sin token era pública, cuando en realidad la distinción entre request pública y autenticada ya la tiene
   `safeRequest(authenticated)`.
3. **Blip (iteración 2):** el fix solo reaccionaba al 401 del servidor, que llega después de navegar y de dibujar la
   pantalla destino; y si la pantalla no hace request (Cartelera sin usuario), no reaccionaba nunca.

## Token del backend (verificado en `condominium-api`, solo lectura)
`SupabaseAuthRepository.signIn` (`src/modules/auth/infrastructure/repositories/SupabaseAuthRepository.ts:46-62`) devuelve
el `access_token` de `supabase.auth.signInWithPassword` (más `expires_in`); las rutas lo validan con
`supabase.auth.getUser(token)`. Es un JWT de Supabase Auth, que trae `exp` (segundos). La app no usa el `refresh_token`:
la sesión dura lo que dure el JWT (1 h por defecto en Supabase; no verificado el valor configurado en el proyecto).

## Diseño del fix
`SessionExpiry` es el único lugar que decide que la sesión venció. Dos disparadores, el mismo evento:
1. **Local (antes de enviar o de navegar):** `SessionExpiry.usableToken()` lee el token y comprueba con `isUsable` (una
   sola función) que sea un JWT cuyo payload (base64url de la 2.ª parte) se pueda leer y cuyo `exp` no haya pasado según el
   reloj inyectado (`nowSeconds`, por defecto `kotlin.time.Clock.System`). Sin `exp` → usable (decide el servidor). Si no
   hay token, o está mal formado o vencido, llama a `onUnauthorized(token)` (borra el token si había, emite una vez) y
   devuelve `null`. Lo usan:
   - `ApiServiceImpl.safeRequest` (requests autenticadas): con `null` no envía nada y devuelve el mismo
     `Response.error(401, …)` que daría el servidor.
   - `onTabSelected` en `CondominioNavGraph` (bottom bar, sidebar, rail y accesos del dashboard a pestañas): con `null`
     no navega; el collector lleva al login y la pestaña nunca se dibuja.
2. **Servidor:** un 401 en una request autenticada que sí se envió → `onUnauthorized(sentToken)` (como antes).

La redirección por expiración al login va **sin animación** (`expiredRedirect` en el NavHost: enter/exit `None` solo
cuando el destino es `login` tras una expiración; se rearma al salir del login). El logout normal conserva el fade.

**Cobertura de navegación:** el chequeo previo está solo en `onTabSelected`. Las demás navegaciones (de una pantalla a su
detalle; 32 `navigate(...)` en el NavGraph) no se chequean antes: envolverlas todas, o bloquear el contenido de cada
destino hasta verificar el token (un frame vacío en toda navegación), no es simple ni libre de riesgo. En esas rutas la
primera request de la pantalla falla localmente al instante (sin red) y el login aparece sin animación; puede verse la
pantalla destino uno o dos frames. splash, login, registro y QR no se chequean (sus requests son públicas).

```
request autenticada ──> ApiServiceImpl.safeRequest(authenticated = true)
                          │ captura el token enviado (SessionExpiry.currentToken())
                          │   └─ si hay token no nulo, rearma el flag notifiedWhileTokenMissing = false
                          │ respuesta 401 (solo si authenticated == true)
                          v
                        SessionExpiry.onUnauthorized(sentToken)
                          │ Mutex: varios 401 simultáneos = un solo evento
                          │ Caso A (sentToken == null, token borrado de localStorage):
                          │   ignora si ya hay un token nuevo guardado o si ya notificó en este episodio
                          │   marca notifiedWhileTokenMissing = true + emite en `events`
                          │ Caso B (sentToken != null, token expirado/inválido):
                          │   ignora si el token guardado ya no es el enviado (401 tardío o nuevo login)
                          │   clearToken() + notifiedWhileTokenMissing = true + emite en `events`
                          v
                        CondominioNavGraph (LaunchedEffect sobre events)
                          authRepository.logout()  // limpia usuario y caché de 10 s de users/me
                          navigate("login") { popUpTo(0) { inclusive = true } }  // salvo en splash/login
```

## Archivos
| Archivo | Cambio |
|---|---|
| `shared/src/commonMain/kotlin/com/example/condominio/data/remote/SessionExpiry.kt` | Nuevo. `usableToken()` + `isUsable` (chequeo local del JWT, reloj inyectable), `onUnauthorized(sentToken)` (soporta token expirado y token borrado de storage sin duplicar eventos), `events`. |
| `shared/src/commonMain/kotlin/com/example/condominio/data/remote/ApiServiceImpl.kt` | Recibe `SessionExpiry`; `safeRequest(authenticated)` toma el token de `usableToken()` (sin token usable: no envía y devuelve 401) y llama a `onUnauthorized` si `authenticated && status == 401`. `login`, `register`, `resetPassword`, `submitRegistrationRequest`, `getBuildingByCode` y `getBuildingUnitsByCode` van con `authenticated = false`. |
| `shared/src/commonMain/kotlin/com/example/condominio/di/SharedModule.kt` | `single { SessionExpiry(get()) }`, antes de `ApiServiceImpl`. |
| `shared/src/commonMain/kotlin/com/example/condominio/ui/navigation/CondominioNavGraph.kt` | `LaunchedEffect` que observa `sessionExpiry.events`, ejecuta `authRepository.logout()` y lleva al login sin animación (`expiredRedirect`); `onTabSelected` comprueba `usableToken()` antes de navegar. |
| `shared/src/commonTest/kotlin/com/example/condominio/data/remote/SessionExpiryTest.kt` | Nuevo. 17 tests contra el `ApiServiceImpl` real con un `HttpClientEngine` falso hecho a mano (cuenta las requests que llegan) y JWT armados en el test. |

## Reglas que el fix fija (no romper)
Cada una tiene su test en `SessionExpiryTest`:
- Un 401 en una request autenticada con token borra el token y emite **un** evento.
- Un 401 en una request autenticada cuando el token ya fue borrado de storage (`sentToken == null`) emite **un** evento.
- Varias requests autenticadas que fallan con 401 seguidas (con token, sin token, o una con token seguida de otra ya sin token) emiten **un solo** evento por sesión/episodio. Los tests las corren en secuencia: el `Mutex` de `onUnauthorized` (para 401 realmente simultáneos) no tiene test propio.
- Al volver a iniciar sesión (nuevo token visto en `currentToken()`), el estado se rearma para detectar una futura expiración o borrado de token en la nueva sesión.
- Un 401 tardío de un token viejo (o con `sentToken == null` cuando ya se guardó un token nuevo) no cierra la sesión nueva.
- Los endpoints públicos (`authenticated = false`: `login`, `register`, `resetPassword`, `submitRegistrationRequest`, `getBuildingByCode`, `getBuildingUnitsByCode`) nunca disparan expiración de sesión ante un 401.
- Un 500 y un fallo de red conservan la sesión.
- Request autenticada sin token: no llega al engine y emite un evento.
- JWT con `exp` pasado: no se envía, se borra el token y emite. JWT mal formado (`x`): igual.
- JWT con `exp` futuro: se envía. JWT sin `exp`: se envía (decide el servidor).
- Request pública sin token: se envía y no emite.
- Sin test (manual): el chequeo de `onTabSelected` y la redirección sin animación.

Al agregar un endpoint en `ApiServiceImpl`: pasa por `safeRequest`; si es público (sin sesión) o un 401 significa otra
cosa, márcalo `authenticated = false`.

## Barrido
- `ApiServiceImpl` tiene 36 requests, todas por `safeRequest` (30 autenticadas, 6 públicas: `login`, `register`,
  `resetPassword`, `submitRegistrationRequest`, `getBuildingByCode` y `getBuildingUnitsByCode`). Es el único consumidor de
  `HttpClient` de la app.
- El bearer lo agrega el plugin `BearerAuth` de `SharedModule.kt`, que lee `tokenManager.getToken()` en cada request
  (en web, `localStorage` vía `LocalStorageTokenManager`); por eso editar o borrar `jwt_token` en DevTools afecta la
  siguiente request sin recargar.
- `RemoteAuthRepository.loadCurrentUser` mantiene su propio `clearToken()` con 401: redundante e inocuo; se dejó porque
  tiene tests y `LoginViewModel` depende de `SessionExpiredException`.

## Pendiente y riesgos
- **Coil** (`coil-network-ktor3`) carga imágenes con su propio cliente: si alguna imagen privada lleva bearer, su 401 no
  pasa por `SessionExpiry`. No revisado.
- Sin aviso de «tu sesión venció» en el login: solo redirige. Posible mejora.
- Una acción en vuelo que dé 401 (p. ej. votar) se pierde; el usuario la repite tras iniciar sesión.
- Un token con firma inválida se asume 401 del servidor; no verificado si `getUser` da 401 o 403 en todas las rutas.
- **Reloj del dispositivo:** el chequeo de `exp` usa la hora local. Un reloj adelantado cierra la sesión antes de tiempo;
  uno atrasado deja pasar un token vencido (y entonces decide el 401 del servidor, como antes).
- Navegaciones que no son de pestaña: sin chequeo previo (ver «Cobertura de navegación»).
- Atrás del navegador tras la redirección: la navegación web está ligada al historial (`bindToBrowserNavigation`);
  verificar que no vuelva a pantallas autenticadas (W3).

## Verificación manual
Preparación (PowerShell, en el worktree):
```
python -m http.server 8082 -d shared/build/dist/wasmJs/developmentExecutable
```
En Chrome, `localhost:8082` → F12 → Console:
`for (const r of await navigator.serviceWorker.getRegistrations()) await r.unregister(); for (const k of await caches.keys()) await caches.delete(k); location.reload();`
Cada prueba de expiración cierra la sesión: iniciar sesión a mano antes de la siguiente. Network filtrado en «Fetch/XHR».

| # | Qué hacer | Qué esperar |
|---|---|---|
| W1 | Dashboard → Application → Local Storage → **borrar** `jwt_token`; sin recargar, tocar Facturas o Decisiones | Login inmediato, sin fade; no se ve la pantalla destino; **ninguna** request nueva en Network |
| W2 | Igual con `jwt_token` = `x` | Igual que W1; `jwt_token` desaparece |
| W3 | JWT con `exp` pasado (snippet abajo); tocar una pestaña | Igual que W1; `jwt_token` desaparece |
| W4 | `jwt_token` borrado → Cartelera (pestaña o acceso del dashboard) | Login inmediato (antes: «No hay comunicados publicados») |
| W5 | `jwt_token` borrado → abrir un detalle desde una lista (p. ej. una factura) | Login sin fade al instante; puede verse el detalle un frame; ninguna request en Network |
| W6 | Tras W1–W5, iniciar sesión y navegar por pestañas; luego cerrar sesión desde Perfil | Navegación normal con fade; el logout normal conserva el fade |
| W7 | Tras la redirección, botón atrás del navegador | No vuelve a pantallas autenticadas |
| W8 | Login con contraseña incorrecta | «Credenciales inválidas», sin redirección extra |
| W9 | Esperar > 10 s, Network → Offline, tocar otra pestaña | Error de conexión; `jwt_token` sigue; con Online, reintentar funciona |
| A1 | Android: login con contraseña incorrecta | Error de credenciales |
| A2 | Android: modo avión (panel rápido) y tocar una pestaña | Error de conexión; la sesión sigue |
| A3 | Android: dejar la app abierta más de lo que dura el JWT (~1 h) y tocar una pestaña | Login inmediato, sin ver la pestaña ni esperar la red |

JWT con `exp` pasado (W3), en Console con la sesión iniciada. La firma queda inválida, pero el cliente lo detecta antes de
enviarlo:
```js
const [h, p, s] = localStorage.getItem('jwt_token').split('.');
const b = JSON.parse(atob(p.replace(/-/g, '+').replace(/_/g, '/')));
b.exp = Math.floor(Date.now() / 1000) - 60;
localStorage.setItem('jwt_token', [h, btoa(JSON.stringify(b)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, ''), s].join('.'));
```

En Android el token vive en DataStore y no se puede editar sin `adb run-as`; A y C quedan cubiertos por los tests y por
W1–W3 (mismo código).

## Para cerrar
1. Pasar la verificación manual.
2. Commit en la rama del worktree, merge a una rama `feature/*` y PR a `dev`.
