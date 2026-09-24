# Handoff: versión Web + PWA (Kotlin/Wasm) — cambios y cómo probarlos

> Documento para el agente/dev que continúa este trabajo. Léelo completo antes de tocar código.

## 0. Estado y reglas

- **Rama:** `feat/web-pwa` (sale de `main` @ `e4b6398`).
- **Nada está commiteado.** Todos los cambios están en el working tree.
- **Regla del usuario:** NO hacer `git commit` salvo que el usuario lo pida explícitamente en su mensaje actual.
- Al commitear, incluir `kotlin-js-store/wasm/package-lock.json` (lockfile npm de Kotlin/Wasm). `.claude/launch.json` es config local de preview; decidir con el usuario si se versiona.
- Entorno donde se desarrolló: Windows 10, JDK 21, Android SDK en `C:\Users\User\AppData\Local\Android\Sdk`, Python 3 disponible.

## 1. Objetivo

Correr la app de residentes (Compose Multiplatform, módulo `shared`) en el navegador e instalarla como PWA, reutilizando la UI común, sin cambiar el comportamiento de la app Android. Caso principal: usuarios de iPhone sin app nativa iOS.

## 2. Resumen de cambios

### 2.1 Versiones (forzadas por compatibilidad — no bajar sin leer el motivo)

| Pieza | Antes | Ahora | Motivo |
|---|---|---|---|
| Compose Multiplatform | 1.7.0 | **1.10.3** | Web Beta; 1.11+ exige Kotlin 2.3.20 para web |
| Kotlin | 2.3.10 | **2.3.21** | Koin 4.2 trae stdlib 2.3.20 (error "stdlib version differs from compiler") |
| Koin | 4.0.0 | **4.2.0** | Koin 4.0/4.1.1 en wasm → `IrLinkageError ... LocalViewModelStoreOwner$stable` → **pantalla en blanco** |
| AGP | 8.6.1 | **8.9.1** | Koin 4.2 trae `androidx.activity 1.12.x`, que exige AGP ≥ 8.9.1 (`checkDebugAarMetadata`) |
| Gradle wrapper | 8.7 | **8.11.1** | Requerido por AGP 8.9 |
| Ktor | 2.3.12 | **3.3.3** | Ktor 2 no tiene target wasm |
| Coil network | `coil-network-ktor2` | **`coil-network-ktor3`** | Ktor 3 |
| lifecycle | 2.8.7 | **2.9.6** + `lifecycle-runtime-compose` (org.jetbrains) en commonMain | `androidx.lifecycle.compose.LocalLifecycleOwner` no resolvía en wasm |
| KSP, Room, sqlite | presentes | **eliminados** | Ver 2.2 |

Archivos: `gradle/libs.versions.toml`, `build.gradle.kts` (root), `gradle/wrapper/gradle-wrapper.properties`, `shared/build.gradle.kts`.

### 2.2 Código eliminado

| Archivo | Motivo |
|---|---|
| `shared/src/commonMain/.../data/local/AppDatabase.kt`, `Converters.kt`, `dao/UserDao.kt`, `dao/PaymentDao.kt`, `entity/UserEntity.kt`, `entity/PaymentEntity.kt` | Room sin consumidores (solo el DI los registraba). Room no tiene target web → bloqueaba compilar `wasmJs` |
| `shared/src/commonMain/.../data/repository/RoomAuthRepository.kt` | No estaba registrado en Koin; `AuthRepository` = `RemoteAuthRepository` |
| `shared/src/androidMain/.../data/local/Converters.kt` | Archivo vacío (solo comentario) |
| `shared/src/androidMain/.../data/remote/AuthInterceptor.kt` | Reemplazado por plugin Ktor común (ver 2.3) |
| `shared/src/androidMain/.../di/NetworkModule.kt` | `HttpClient` movido a `SharedModule` común; engine OkHttp a `AppModule` |

`TokenManager.tokenFlow` también se eliminó (sin usos). `LoginViewModel.onClearDatabaseClick()` ya tenía la llamada a DB comentada antes de este trabajo. En dispositivos existentes queda huérfano el archivo `condominio_db` (inofensivo).

Recuperable con `git checkout main -- <ruta>` si se decide usar Room para cache offline (en ese caso, en `androidMain`, no `commonMain`).

### 2.3 Refactors en código común/Android

| Archivo | Cambio |
|---|---|
| `shared/src/commonMain/.../di/SharedModule.kt` (**nuevo**) | `HttpClient(get<HttpClientEngine>())` con: plugin `BearerAuth` (agrega `Authorization: Bearer <token>` desde `TokenManager` en cada request), `ContentNegotiation` JSON (mismas opciones), `defaultRequest` URL `https://condominium-api.nibs-tech.com/`. Además todos los repositorios, use case y `viewModelOf(...)` (antes en `AppModule`) |
| `shared/src/androidMain/.../di/AppModule.kt` | Solo bindings Android: `DataStore<Preferences>` (mismo archivo `auth_prefs.preferences_pb`), `TokenManager` → `DataStoreTokenManager`, `PlatformFileReader`, `PdfService`, `HttpClientEngine` = `OkHttp.create { logging BODY + timeouts 30s }` |
| `androidApp/.../CondominioApp.kt` | `modules(sharedModule, appModule)` |
| `shared/src/commonMain/.../data/local/TokenManager.kt` | Ahora **interfaz** (`saveToken/getToken/clearToken`) |
| `shared/src/androidMain/.../data/local/DataStoreTokenManager.kt` (**nuevo**) | Implementación DataStore; misma clave `jwt_token` → sesiones existentes se conservan |
| `shared/src/commonMain/.../data/remote/ApiServiceImpl.kt` | Multipart Ktor 3: `provider = { ByteReadChannel(bytes) }` (antes `ByteReadPacket`) en `createPaymentMultipart` y `uploadDecisionQuote` |
| `shared/src/commonMain/.../App.kt` | Parámetros `registerCode: String?` y `onNavHostReady: suspend (NavHostController) -> Unit`; crea el `NavController`; import coil ktor3; contenedor centrado `widthIn(max = 600.dp)` (**afecta también Android en tablets/landscape**) |
| `shared/src/commonMain/.../ui/navigation/CondominioNavGraph.kt` | Param `registerCode`; tras splash → `login` y, si hay código, `register_form/$registerCode`. `arguments?.getString(k)` → `arguments?.read { getStringOrNull(k) }` (API común de SavedState) |
| `shared/src/commonMain/.../ui/utils/KmpFormatters.kt` | Nuevo `String.formatArgs(vararg)`: reemplaza `%N$s`, `%N$d` y `%%` (sustituto común de `String.format` JVM) |
| `DecisionDetailScreen.kt`, `DecisionsListScreen.kt` | `.format(...)` → `.formatArgs(...)` sobre strings de recursos |
| `DecisionSheetsStubs.kt` | `"%.2f MB".format(x)` / `"$%.2f".format(x)` → `formatCurrency(x)` |

Nota: commonMain tenía código solo-JVM (`String.format`, `Bundle.getString`) que nunca falló porque el target iOS no se compila en Windows. Ahora el target wasm lo detecta.

### 2.4 Target web (todo nuevo, `shared/src/wasmJsMain/`)

| Archivo | Qué hace |
|---|---|
| `kotlin/main.kt` | `startKoin(sharedModule, webModule)`; `webModule`: engine `Js`, `LocalStorageTokenManager`, `WebFileReader`, `PdfService` no-op. Lee `?code=` de la URL (via `extractBuildingCode`) → `App(registerCode)`. `bindToBrowserNavigation()` (rutas con hash: `#login`, `#register_form/X`). `ComposeViewport(document.body)`; quita `#loading` |
| `.../data/local/LocalStorageTokenManager.kt` | JWT en `localStorage` (clave `jwt_token`). Riesgo XSS documentado en el archivo |
| `.../data/utils/WebFiles.kt` | `pickFile(accept)`: `<input type=file>` oculto → lee bytes con `FileReader` → guarda en mapa por `blob:` URL → devuelve `(blobUrl, mime)`. `WebFileReader` lee de ese mapa (bytes + nombre) |
| `.../data/utils/Pickers.wasmJs.kt` | `rememberImagePickerLauncher` (`image/*`), `rememberDocumentPickerLauncher` (`image/*,application/pdf`), `rememberExternalViewerLauncher` (`window.open(url, "_blank")`) |
| `.../ui/utils/KmpFormatters.wasmJs.kt` | `formatCurrency` = `toFixed(2)`; `formatDate` soporta tokens `yyyy MMMM MMM MM dd` (meses vía `Date.toLocaleString` con idioma de la app); `formatMonthYear` = mes largo es-ES (igual que Android) |
| `.../ui/locale/PlatformLocaleEffect.wasmJs.kt` | Setea `<html lang>` y sobrescribe `navigator.language(s)` para que `stringResource` siga el idioma elegido en la app |
| `.../ui/screens/register/QrScannerView.wasmJs.kt` | Sin escáner: texto "ingresa el código manualmente" (la pantalla ya tiene botón de ingreso manual) |
| `resources/index.html` | Loader, registro de SW, chequeo WasmGC (si falla: mensaje "actualiza tu navegador"), carga `shared.js` |
| `resources/manifest.webmanifest` | Nombre "Apto Condominios", `display: standalone`, íconos 192/512 |
| `resources/sw.js` | Cache `condominio-v1`: stale-while-revalidate para GET same-origin; API (otro origen) siempre red. Solo borra caches `condominio-*` viejos (no el `compose_web_resources_cache`). **Subir `CACHE` en cada deploy** |
| `resources/_headers` | `no-cache` para `sw.js` e `index.html` (formato Cloudflare Pages/Netlify) |
| `resources/icon-192.png`, `icon-512.png` | Generados desde `design/logo_playstore.png` (que en realidad es JPEG). Logo tiene borde gris → pedir versión sin borde para "maskable" |

### 2.5 Configuración de build

| Archivo | Cambio | Motivo |
|---|---|---|
| `settings.gradle.kts` | `RepositoriesMode.PREFER_SETTINGS` + repos ivy de Node.js, Yarn, Binaryen | `FAIL_ON_PROJECT_REPOS` rompía `kotlinWasmNodeJsSetup` (el plugin agrega repos por proyecto) |
| `gradle.properties` | `kotlin.js.yarn=false` | `~/.yarnrc` global del usuario apunta a Yarn Berry → `Unsupported option name ("--ignore-scripts")`. Se usa npm |
| `gradle.properties` | `kotlin.daemon.jvmargs=-Xmx4096m` | Link de wasm producción daba `OutOfMemoryError` con 2 GB |
| `shared/build.gradle.kts` | `wasmJs { browser(); binaries.executable() }`, `wasmJsMain` depende de `ktor-client-js`; DataStore movido a `androidMain`; fuera Room/KSP | — |
| `kotlin-js-store/wasm/package-lock.json` | Nuevo lockfile. Si cambian deps: `./gradlew kotlinWasmUpgradePackageLock` | — |
| `README.md` | Sección "Web / PWA" | — |

## 3. Cómo compilar

```bash
./gradlew :androidApp:assembleDebug
./gradlew :shared:wasmJsBrowserDistribution
```

Salida web: `shared/build/dist/wasmJs/productionExecutable/` (≈15 MB de `.wasm` sin comprimir; bundle JS = `shared.js`).

Errores conocidos y solución:
- `Lock file was changed` → `./gradlew kotlinWasmUpgradePackageLock`
- `OutOfMemoryError` en wasm → confirmar `-Xmx4096m` y `./gradlew --stop`
- Pantalla en blanco + `IrLinkageError` en consola → versión de Koin incorrecta (debe ser 4.2.x)

## 4. Cómo probar

### 4.1 Web en PC

```bash
python -m http.server 8080 -d shared/build/dist/wasmJs/productionExecutable
```

Abrir `http://localhost:8080`. **Entre builds limpiar cache**: DevTools → Application → Storage → *Clear site data* (el SW sirve la versión anterior).

| Prueba | Resultado esperado | Estado |
|---|---|---|
| Carga inicial | Loader → splash → login | ✅ verificado |
| Login credenciales falsas | Mensaje de error; consola: `Login failed (401)` | ✅ verificado |
| Login credenciales reales | Dashboard / selección de unidad | ⏳ requiere cuenta |
| Recargar (F5) con sesión | Sigue logueado (token en localStorage) | ⏳ |
| Logout | Vuelve a login; `localStorage.jwt_token` vacío | ⏳ |
| "¿No tienes cuenta?" | Pantalla QR con texto "Escáner no disponible…"; URL `#register_scanner` | ✅ verificado |
| Botón atrás del navegador | Vuelve a login; URL `#login` | ✅ verificado |
| `http://localhost:8080/?code=COND-XXXX` | Tras splash abre registro; URL `#register_form/COND-XXXX`; el backend valida el código | ✅ verificado (código inválido → "Código inválido o edificio no encontrado") |
| Crear pago con comprobante | Preview de imagen (blob URL), envío multipart OK | ⏳ requiere cuenta |
| Subir presupuesto (imagen y PDF) | Muestra nombre y tamaño "X.XX MB"; >5 MB se rechaza; envío OK | ⏳ |
| Perfil → cambiar idioma a EN | Textos en inglés | ⏳ (sobrescritura de `navigator.language` no probada) |
| Banderas 🇪🇸🇺🇸 en Perfil | Pueden verse como cuadros (wasm no trae fuente emoji) | ⏳ probable fallo visual; fix: texto "ES"/"EN" |
| Fechas / montos | Formato igual a Android | ⏳ |
| Viewport móvil (DevTools 375×812) | Layout correcto | ✅ verificado (splash/login) |
| Ventana ancha | Contenido centrado, máx. 600dp | ✅ verificado |
| DevTools → Application | Manifest sin errores, SW `activated`, instalable | ✅ verificado |

### 4.2 PWA en celular real (requiere HTTPS)

**Android por USB:**

```bash
adb reverse tcp:8080 tcp:8080
```

Chrome del teléfono → `http://localhost:8080` → ⋮ → *Instalar app*. Consola remota: `chrome://inspect` en el PC.

**iPhone (iOS ≥ 18.2):** túnel temporal (no cerrar la terminal; el `ERR Failed to initialize DNS local resolver` es inofensivo):

```bash
cloudflared tunnel --url http://localhost:8080
```

Usar la URL `https://….trycloudflare.com` que aparece en el recuadro al inicio. Si no responde: `cloudflared tunnel --protocol http2 --url http://localhost:8080`. Safari → Compartir → *Añadir a pantalla de inicio*. La URL es pública mientras el túnel esté abierto.

Checklist (decide go/no-go de la web): teclado no tapa campos, autocompletar contraseña, pegar, teclado de email, scroll fluido, rotación, tiempo de primera carga y de segunda carga (cache), subir foto con cámara desde el picker, abre sin barra del navegador al estar instalada, modo avión → abre shell con error de red.

### 4.3 Regresión Android (obligatoria — solo se compiló, no se corrió)

```bash
./gradlew :androidApp:installDebug
```

| Prueba | Qué cubre |
|---|---|
| Login / logout / cerrar y reabrir app sin perder sesión | `DataStoreTokenManager`, plugin `BearerAuth` |
| Usuario ya logueado antes de actualizar → sigue logueado | Misma clave/archivo DataStore |
| Primer login con cambio de contraseña | Navegación |
| Crear pago **con comprobante** | Multipart Ktor 3 (`ByteReadChannel`) — **riesgo más alto**: verificar que el backend acepte el request |
| Subir presupuesto en decisión | Multipart + `formatCurrency` en tamaño/monto |
| Ver votación de decisión (porcentajes) y lista (fechas límite) | `formatArgs` |
| Detalle de pago/factura/decisión/anuncio | `arguments?.read { getStringOrNull }` |
| Registro escaneando QR real | ML Kit (no tocado) + ruta `register_form/{buildingId}` |
| Cambio de idioma | Sin cambios esperados |
| Tablet u horizontal | Contenido limitado a 600dp (cambio visual nuevo) |
| AdMob al abrir | AGP/activity nuevos |
| Revisión visual general | Salto Compose 1.7 → 1.10 puede alterar Material3/insets |
| `./gradlew :androidApp:assembleRelease` | Build firmado no probado |

## 5. Riesgos conocidos

1. Salto grande de versiones (CMP, Ktor, Koin, AGP): posibles cambios visuales o de comportamiento en runtime.
2. Multipart Ktor 3 puede enviar sin `Content-Length` (chunked); confirmar con el backend.
3. `widthIn(max = 600.dp)` aplica a Android. Si no se quiere: mover el wrapper a `main.kt` web.
4. iOS nunca compiló en este proyecto y no se verificó; `iosMain` no tiene binding de `TokenManager` ni DI.
5. Bundle web ≈15 MB sin comprimir: servir con brotli/gzip (Cloudflare lo hace).
6. JWT en `localStorage` expuesto a XSS; alternativa: cookie HttpOnly si el backend la soporta (el CORS actual `*` + `allow-credentials: true` no sirve para cookies).

## 6. Fuera de alcance / pendientes

- Deploy: Cloudflare Pages (subir `productionExecutable/`). Rutas usan hash → no requiere rewrites.
- Fallback target `js` para iOS < 18.2.
- Web push: la API no tiene endpoints (requiere VAPID + suscripciones en backend).
- Escáner QR in-browser (zxing-js). Hoy: ingreso manual o QR con URL `https://<web>/?code=COND-XXXX` escaneado por la cámara nativa.
- Recibo PDF: en web es no-op. En Android se genera pero nunca se muestra (`PaymentDetailScreen.kt` solo hace `println`) — bug previo, tarea separada.
- Decisión pendiente del usuario: ¿restaurar Room en `androidMain` para cache offline? (hoy eliminado).
