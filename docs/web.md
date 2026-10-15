# Versión web (Kotlin/Wasm + PWA)

Guía para trabajar con la versión web de la app. La web **no es otro proyecto**: es un target más (`wasmJs`) del módulo
`shared`, que comparte UI (Compose Multiplatform), ViewModels, repositorios y red con Android.

- Estado de la tecnología: Compose Multiplatform para web está en **Beta** (Android/iOS en Stable).
- Navegadores soportados: Chrome 119+, Firefox 120+, **Safari/iOS 18.2+** (requiere WasmGC).
- Público: residentes, detrás de login (sin requisitos de SEO).

---

## 1. Dónde está cada cosa

| Qué | Dónde |
|---|---|
| UI, ViewModels, red, DI común | `shared/src/commonMain/` |
| Código solo web (entrada, `actual`s) | `shared/src/wasmJsMain/kotlin/` |
| Punto de entrada web | `shared/src/wasmJsMain/kotlin/main.kt` (Koin + `ComposeViewport` + navegación del navegador + títulos de pestaña) |
| HTML, PWA, service worker, íconos | `shared/src/wasmJsMain/resources/` (`index.html`, `manifest.webmanifest`, `sw.js`, `icon-*.png`, `_headers`) |
| Implementaciones web | `LocalStorageTokenManager` (JWT en `localStorage`), `WebFiles.kt` (selector de archivos), `Pickers.wasmJs.kt`, `KmpFormatters.wasmJs.kt`, `PlatformLocaleEffect.wasmJs.kt`, `QrScannerView.wasmJs.kt` |
| Layout adaptable (web/tablet) | `shared/src/commonMain/kotlin/com/example/condominio/ui/components/WebLayout.kt`, `AppShell.kt`, `TopBarWithBack.kt` |
| Armazón y anchos por ruta | `ui/navigation/CondominioNavGraph.kt` |
| Configuración de API por ambiente | generada en build: `shared/build/generated/apiConfig/.../ApiConfig.kt` (ver §2) |

---

## 2. Ambientes (DEV / PROD)

La URL de la API se **fija al compilar** a partir de `local.properties` (no versionado):

```properties
API_ENV=DEV                     # DEV | PROD (o cualquier nombre con su <ENV>_API_URL). Por defecto DEV
DEV_API_URL=https://condominium-api-beta.nibs-tech.com/
PROD_API_URL=https://condominium-api.nibs-tech.com/
ENABLE_NETWORK_LOGS=true        # logs HTTP en Android
```

- Sobrescribir para una compilación: `-PapiEnv=PROD` (tiene prioridad sobre `local.properties`).
- Nuevo ambiente: agregar `STAGING_API_URL=...` y usar `API_ENV=STAGING`. Sin cambios de código.
- Verificar a qué apunta el build: `cat shared/build/generated/apiConfig/com/example/condominio/ApiConfig.kt`.
- Cambiar de ambiente exige **recompilar**. El token de un ambiente no sirve en el otro (la app vuelve al login con 401).
- ⚠️ Builds de release (Android o deploy web) deben compilarse con `PROD`.

Lo genera la tarea `:shared:generateApiConfig` (en `shared/build.gradle.kts`), consumida por `SharedModule` (`defaultRequest`).

---

## 3. Correr la web en local

### Desarrollo con recarga en vivo

```bash
./gradlew :shared:wasmJsBrowserDevelopmentRun --continuous
# o con ambiente explícito:
./gradlew :shared:wasmJsBrowserDevelopmentRun --continuous -PapiEnv=DEV
```

- Servidor webpack en `http://localhost:8080`. Al guardar, Gradle recompila (~1 min) y el navegador **recarga la página
  completa** (se pierde el estado de pantalla; la sesión se mantiene).
- No es *hot reload* con estado: Compose Hot Reload solo existe para el target de escritorio (JVM), que este proyecto no tiene.
- Build de desarrollo sin optimizar (~37 MB de wasm): solo para trabajar, nunca para publicar.
- Si al cortar con Ctrl+C el puerto 8080 queda ocupado, matar el proceso `node` que lo usa.

### Build de producción + servidor estático

```bash
./gradlew :shared:wasmJsBrowserDistribution            # salida: shared/build/dist/wasmJs/productionExecutable/
python -m http.server 8080 -d shared/build/dist/wasmJs/productionExecutable
```

- El servidor simple de Python permite cache del navegador: tras recompilar, recargar con **Ctrl + Shift + R** o
  DevTools → Application → *Clear site data*.
- Alternativa sin cache: la configuración `web-dist` de `.claude/launch.json` (Python con `Cache-Control: no-store`).
- Peso de producción: ~15 MB de wasm sin comprimir (~5 MB gzip); el hosting debe comprimir (brotli/gzip).

### Si la pestaña queda "congelada"
El navegador pausa el dibujado de Compose cuando la pestaña o la ventana están ocultas: la app queda detenida en el splash o
login hasta volver a mostrarla. No es un bug de la app.

---

## 4. Deploy

1. `./gradlew :shared:wasmJsBrowserDistribution -PapiEnv=PROD`
2. Subir `shared/build/dist/wasmJs/productionExecutable/` a un hosting estático con HTTPS (p. ej. Cloudflare Pages).
   - Las rutas usan hash (`/#dashboard`): no se necesitan rewrites.
   - `_headers` ya marca `sw.js` e `index.html` como `no-cache`.
   - El servidor debe entregar `.wasm` como `application/wasm`.
3. Subir `CACHE` en `sw.js` solo si cambia la **estrategia** de cache (no hace falta en cada deploy).
4. CORS de la API: debe permitir el origen del sitio (hoy `*` en DEV y PROD).

---

## 5. PWA y caches (importante)

- `sw.js`: `.wasm` (nombres con hash) → *cache-first*; todo lo demás → *network-first* con `fetch(url, { cache: 'no-cache' })`.
  Así un deploy nuevo se ve en la **primera** recarga. Usa la URL (no el `Request`) porque las navegaciones rechazan `init`.
- `index.html` borra `compose_web_resources_cache` antes de arrancar: Compose cachea sus recursos (textos/imágenes) sin
  versión, y mezclar un `strings.cvr` viejo con un build nuevo produce textos en blanco y el error
  `The padding option is set to PRESENT, but the input is not properly padded`.
- `index.html` también:
  - comprueba WasmGC y muestra "actualiza tu navegador" si no hay soporte;
  - convierte el `<input>` oculto que Compose usa para contraseñas en `type="password"` (si no, el teclado móvil sugiere y
    aprende contraseñas). Depende del markup interno de Compose: revisarlo al actualizar Compose.

---

## 6. Reglas de UI para web

Adaptar por **ancho de ventana**, no por plataforma (`ui/components/WebLayout.kt`):

| Clase | Ancho | Navegación |
|---|---|---|
| `Compact` | < 600dp (teléfono) | barra inferior — **UI móvil original, no se toca** |
| `Medium` | 600–1024dp (tablet) | rail de íconos |
| `Expanded` | ≥ 1024dp (escritorio) | barra lateral con marca |

- Todo cambio de layout ancho va detrás de `isWide()` / `isExpanded()`; el camino `Compact` debe quedar idéntico.
- Contenedor único: todas las páginas de la app usan `WebContainerWidth` (1440dp) y empiezan en `pagePadding()` (32dp en
  ancho), así el título nunca "salta" entre pantallas.
- Formularios tipo ajustes: `Modifier.formWidth()` (máx. 720dp, alineado a la izquierda, nunca centrado).
- Piezas disponibles: `WebCard`, `AdaptiveColumns` (2 columnas en Expanded), `WebEmptyFrame` (estados vacío/error),
  `screenBackground(...)` (canvas gris en ancho), `TopBarWithBack` (cabecera de página en ancho, acciones a la derecha).
- Auth (login/registro): panel de marca a la izquierda en Expanded (`AuthBrandPanel`); `unit_selection` no lo usa porque
  también se abre con sesión iniciada.
- Navegación por pestañas: `onTabSelected` en `CondominioNavGraph`. "Inicio" hace `popBackStack("dashboard")`; los accesos
  del dashboard a otras secciones usan `onTabSelected` (no `navigate` directo) para no romper la pila.
- **Emojis**: el renderer web no trae fuente de emojis → usar `String.emojiSafe()` (o un `Icon`) en textos con emoji.
  Desde Compose Multiplatform 1.12 hay descarga automática de fuentes (al actualizar, se puede quitar).
- **Fechas**: la API manda fechas de calendario como `yyyy-MM-dd` o `yyyy-MM-ddT00:00:00.000Z`. Tratarlas como fecha local
  (tomar `yyyy-MM-dd`, medianoche local), nunca como instante UTC (en UTC−4 se ve el día anterior). El `DatePicker` de
  Material3 usa milisegundos UTC: convertir al leer/escribir (ver `CreatePaymentScreen`).
- Nada de APIs JVM/Android en `commonMain` (`String.format`, `java.*`, `Bundle`…): el target web no compila. Hay helpers
  comunes (`formatArgs`, `formatCurrency`, `formatDate`).

---

## 7. Versiones acopladas (no bajar sin leer)

| Pieza | Versión | Por qué |
|---|---|---|
| Compose Multiplatform | 1.10.3 | web Beta; 1.11+ exige Kotlin 2.3.20+ para web |
| Kotlin | 2.3.21 | lo exige Koin 4.2 |
| Koin | 4.2.0 | 4.0/4.1 rompen la web (`IrLinkageError` en ViewModels → pantalla en blanco) |
| AGP / Gradle | 8.9.1 / 8.11.1 | Koin 4.2 trae `androidx.activity` 1.12 |
| Ktor | 3.3.3 | Ktor 2 no tiene target wasm |
| kotlinx-datetime | 0.7.1 | la web lo resuelve a 0.7.x; usar `kotlin.time.Clock`/`Instant` |
| multiplatform-markdown-renderer | 0.41.0 | 0.31 rompe el detalle de comunicado en web; 0.41 ya no publica `iosX64` |

Gradle/build para wasm:
- `settings.gradle.kts` usa `PREFER_SETTINGS` + repos ivy de Node/Yarn/Binaryen (el plugin wasm agrega repos propios).
- `gradle.properties`: `kotlin.js.yarn=false` (npm) y `kotlin.daemon.jvmargs=-Xmx4096m` (el link de producción necesita memoria).
- Si cambian dependencias JS: `./gradlew kotlinWasmUpgradePackageLock` (lockfile en `kotlin-js-store/wasm/`).
- Errores `IrLinkageError` en consola del navegador = librería compilada contra otra versión de Compose/Kotlin: subirla.

---

## 8. Limitaciones conocidas

- Sin escáner QR en web: se ingresa el código manualmente, o el QR codifica `https://<web>/?code=COND-XXXX` y la cámara
  nativa abre el registro directo.
- "Descargar recibo/factura" y "Contactar soporte" no hacen nada en web.
- Sin cache offline de datos (solo el *shell*); sin web push (la API no tiene endpoints).
- Primera carga pesada (~5 MB comprimidos); accesibilidad limitada (render en canvas).
- La pestaña oculta pausa la app (ver §3).

---

## 9. Checklist antes de publicar un cambio web

1. `./gradlew :androidApp:assembleDebug :shared:wasmJsBrowserDistribution` sin errores.
2. Probar en escritorio (≥ 1024px), tablet (~900px) y móvil (375px) que el layout cambie y que **móvil quede igual**.
3. Login, navegación por la barra lateral, botón atrás del navegador, recarga con sesión.
4. Consola del navegador sin `IrLinkageError` ni `Exception`.
5. Build de release con `-PapiEnv=PROD`.

Historial de decisiones de la migración: `docs/web-pwa-handoff.md`.
