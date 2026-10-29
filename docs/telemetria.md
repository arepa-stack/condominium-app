# Telemetría (analítica y errores)

Guía centralizada para configurar, probar y mantener la telemetría de la app en Android y Web.

---

## Índice

1. [Qué es y glosario](#1-qué-es-y-glosario)
2. [DEV vs PROD](#2-dev-vs-prod)
3. [Qué medimos y qué nunca](#3-qué-medimos-y-qué-nunca)
   - 3.1 [Eventos y errores de API](#31-eventos-y-errores-de-api)
   - 3.2 [Privacidad y PII](#32-privacidad-y-pii)
   - 3.3 [Web vs Android en los informes](#33-web-vs-android-en-los-informes)
4. [Web vs Android](#4-web-vs-android)
   - 4.1 [Herramientas y diferencias](#41-herramientas-y-diferencias)
   - 4.2 [Cómo funciona por dentro](#42-cómo-funciona-por-dentro)
5. [Configurar](#5-configurar)
   - 5.1 [Local (local.properties y Google Services)](#51-local-localproperties-y-google-services)
   - 5.2 [GitHub Actions (GA_MEASUREMENT_ID y SENTRY_DSN)](#52-github-actions-ga_measurement_id-y-sentry_dsn)
   - 5.3 [Consolas (Firebase, GA4, Crashlytics y Sentry)](#53-consolas-firebase-ga4-crashlytics-y-sentry)
6. [Probar](#6-probar)
   - 6.1 [Web (Chrome y Sentry)](#61-web-chrome-y-sentry)
   - 6.2 [Android (DebugView, Logcat y Crashlytics)](#62-android-debugview-logcat-y-crashlytics)
   - 6.3 [Cuándo aparece cada dato](#63-cuándo-aparece-cada-dato)
7. [Problemas frecuentes (Adblock, DebugView vacío, builds)](#7-problemas-frecuentes-adblock-debugview-vacío-builds)
8. [Pendientes](#8-pendientes)

---

### Busco… → ve a

| Busco… | Ve a |
|---|---|
| Probar telemetría en Android paso a paso | [§6.2 Android (DebugView, Logcat y Crashlytics)](#62-android-debugview-logcat-y-crashlytics) |
| Probar telemetría en Web (Chrome) | [§6.1 Web (Chrome y Sentry)](#61-web-chrome-y-sentry) |
| DebugView de Firebase está vacío o no llegan eventos | [§7 Problemas frecuentes](#7-problemas-frecuentes-adblock-debugview-vacío-builds) |
| Adblock o extensiones bloquean eventos en web | [§7 Problemas frecuentes](#7-problemas-frecuentes-adblock-debugview-vacío-builds) |
| Configurar variables en GitHub Actions para CI/CD | [§5.2 GitHub Actions](#52-github-actions-ga_measurement_id-y-sentry_dsn) |
| Configurar credenciales locales y `local.properties` | [§5.1 Local](#51-local-localproperties-y-google-services) |
| Configurar Firebase, GA4 o Sentry en sus consolas | [§5.3 Consolas](#53-consolas-firebase-ga4-crashlytics-y-sentry) |
| Lista de eventos de negocio y errores que se envían | [§3.1 Eventos y errores de API](#31-eventos-y-errores-de-api) |
| Reglas de privacidad, PII y por qué quitamos AdID | [§3.2 Privacidad y PII](#32-privacidad-y-pii) |
| Separar usuarios web y app en GA | [§3.3 Web vs Android en los informes](#33-web-vs-android-en-los-informes) |
| Cuánto tardan en verse los eventos o errores | [§6.3 Cuándo aparece cada dato](#63-cuándo-aparece-cada-dato) |
| Tareas pendientes antes de publicar en producción | [§8 Pendientes](#8-pendientes) |

---

## 1. Qué es y glosario

La telemetría mide el uso real de la app (pantallas visitadas y acciones clave) y reporta caídas o fallos técnicos en tiempo de ejecución.
Permite saber qué funciones se utilizan y corregir errores antes de que afecten a más residentes, sin almacenar nunca información personal identificable.

| Término | Qué es |
|---|---|
| Firebase Analytics | Servicio de Google que registra eventos de negocio y pantallas en Android. |
| Crashlytics | Herramienta de Firebase para capturar excepciones no fatales y crashes en Android. |
| GA4 (Google Analytics 4) | Analítica web estándar vía `gtag.js` conectada al mismo proyecto Firebase. |
| Measurement ID | Identificador del flujo de datos web en GA4 con formato `G-XXXXXXXXXX`. |
| Sentry | Plataforma externa que captura errores de JavaScript en navegadores web. |
| DSN | Dirección pública de Sentry (`https://...@...`) que indica dónde enviar los errores. |
| Loader Script | Script optimizado de Sentry que carga el SDK completo solo cuando ocurre un error. |
| DebugView | Vista en tiempo real de Firebase para depurar eventos en Android al instante. |
| `app_env` | Propiedad de usuario (`DEV` o `PROD`) para segmentar analítica y reportes de errores. |
| Medición optimizada | Función automática de GA4 que mide clics y scroll; debe estar **desactivada**. |

---

## 2. DEV vs PROD

DEV y PROD están en proyectos y propiedades **totalmente separados**. Los datos de prueba jamás se mezclan con los residentes reales.
Regla obligatoria: todo cambio se valida primero en DEV. **Nunca** se colocan claves o archivos de PROD en entornos de desarrollo local.

| Elemento | DEV (rama `dev`, API beta) | PROD (rama `main`, API prod) |
|---|---|---|
| Proyecto Firebase | `condominium-app-dev` | `condominium-app-prod` |
| Propiedad GA4 | Asociada a `condominium-app-dev` | Asociada a `condominium-app-prod` |
| Proyecto Sentry | `condominium-app-web-dev` | `condominium-app-web-prod` |
| GitHub Environment | `dev` | `prod` |
| Configuración Android | `androidApp/firebase/dev/google-services.json` | `androidApp/firebase/prod/google-services.json` |
| Claves web en GitHub | `vars.GA_MEASUREMENT_ID` y `vars.SENTRY_DSN` en `dev` | `vars.GA_MEASUREMENT_ID` y `vars.SENTRY_DSN` en `prod` |

---

## 3. Qué medimos y qué nunca

### 3.1 Eventos y errores de API

- **Pantallas visitadas**: `screen_view` (Android) y `page_view` (Web) usan `screenName(route)`, recortando argumentos (`payment_detail/{paymentId}` → `payment_detail`).
- **Eventos de negocio**: se envían únicamente cuando el endpoint responde con éxito (`2xx`):

| Evento | Endpoint asociado |
|---|---|
| `login` | `POST auth/login` |
| `registration_requested` | `POST registration-requests` |
| `password_reset_requested` | `POST auth/reset-password` |
| `first_password_set` | `POST auth/change-password-first-login` |
| `password_changed` | `PATCH users/me/password` |
| `profile_updated` | `PATCH users/me` |
| `account_deleted` | `DELETE users/me` |
| `payment_reported` | `POST payments` |
| `vote_cast` | `POST decisions/decisions/{id}/votes` |
| `quote_uploaded` | `POST decisions/decisions/{id}/quotes` |
| `announcement_reaction` | `POST information-center/announcements/{id}/reaction` |

- **Errores de API**: ante respuestas HTTP `>= 400` o desconexión de red se emite `api_error` con `{endpoint, status, duration_ms}` (`status = 0` indica fallo de red/offline).
- **Fallos de decodificación**: si la API responde con un JSON inválido o inesperado, `safeRequest` invoca `recordError` con el nombre del endpoint y mensaje saneado.

### 3.2 Privacidad y PII

- **Sin identificadores de usuario**: no se ejecuta `setUserId`; los usuarios son anónimos mediante identificadores de instancia rotables de Firebase y cookies de primer origen (`_ga`).
- **Cero información personal (PII)**: jamás se envían nombres, correos, teléfonos, montos en dinero, bancos, referencias, UUIDs ni códigos de edificio.
- **Rutas anonimizadas**: `endpointTemplate` reemplaza cualquier ID numérico, UUID o código tras `by-code` por `{id}`.
- **Web limpia**: `page_location` reporta solo `origin + pathname`. El hash (`#dashboard`) y tokens de consulta (`?code=...`) nunca se envían a Sentry ni a GA4.
- **Sin identificadores de anuncios**: AdMob fue eliminado por completo, la recolección de AdID está desactivada y el permiso `com.google.android.gms.permission.AD_ID` fue removido del manifest.

### 3.3 Web vs Android en los informes

Existe una sola propiedad de GA4 por ambiente que reúne dos flujos de datos (Android y Web). El panel de Analytics en la consola de Firebase es simplemente una vista filtrada de esa misma propiedad GA4.

- **Cómo separar plataformas en GA4**:
  - En informes estándar: pulsar **Añadir comparación** arriba y filtrar por la dimensión **Plataforma** (`Android` / `web`) o **Nombre del flujo**.
  - En **Explorar**: usar la dimensión **Plataforma** como fila, columna o filtro de desglose.
- **Cómo separar en Firebase**: usar el selector desplegable de aplicación en la parte superior del panel.
- ⚠️ **Métricas de usuarios**: un residente que usa tanto la web como la app Android cuenta como 2 usuarios activos distintos, ya que por privacidad no se utiliza `setUserId` ni cruce de identidades.
- **Recomendación de uso**: consultar [analytics.google.com](https://analytics.google.com) para análisis métrico y segmentación cotidiana; usar la consola de Firebase para consultas rápidas y depuración en vivo con DebugView.

---

## 4. Web vs Android

### 4.1 Herramientas y diferencias

| Aspecto | Web (`wasmJs`) | Android |
|---|---|---|
| Herramienta analítica | GA4 (`gtag.js`) | Firebase Analytics |
| Herramienta de errores | Sentry Loader Script | Firebase Crashlytics |
| Dónde se configura | `local.properties` (local) o GitHub vars (`dev`/`prod`) | `androidApp/firebase/<env>/google-services.json` |
| Frecuencia de envío | Casi inmediata (stream HTTP de GA4 y Sentry) | Por lotes (~1 hora); inmediato en DebugView |
| Cuándo se activa | Variables presentes con formato válido (`G-...`, `https://...`) | JSON presente; debug solo si `apiEnv == DEV`, release siempre |
| Dónde ver errores | Panel de issues de Sentry | Panel de incidencias de Crashlytics |
| Impacto de Adblock | Extensiones de escritorio lo bloquean por defecto | No se afecta (salvo bloqueadores de DNS locales) |
| Sin configuración | No-op seguro (no inserta scripts externos ni falla) | No-op seguro (`NoTelemetry`, omite plugins) |

### 4.2 Cómo funciona por dentro

- **Contrato común**: `shared/src/commonMain/kotlin/com/example/condominio/telemetry/Telemetry.kt` define `screen()`, `event()`, `recordError()` y `NoTelemetry`.
- **Plugin de red Ktor**: `shared/src/commonMain/kotlin/com/example/condominio/data/remote/ApiTelemetry.kt` intercepta llamadas con `on(Send)` en `SharedModule`.
- **Plantilla de rutas**: `endpointTemplate(path)` normaliza rutas quitando `/api/v1/app/` y convirtiendo IDs a `{id}` sin incluir query strings.
- **Navegación**: `shared/src/commonMain/kotlin/com/example/condominio/ui/navigation/CondominioNavGraph.kt` registra `telemetry.screen` al cambiar de ruta.
- **Android**: `androidApp/src/main/java/com/example/condominio/FirebaseTelemetry.kt` implementa `Telemetry` sobre Firebase; `CondominioApp.kt` inyecta `NoTelemetry` si no hay apps de Firebase inicializadas.
- **Web**: `shared/src/wasmJsMain/kotlin/com/example/condominio/telemetry/WebTelemetry.kt` envía eventos a `window.gtag` y `window.Sentry` mediante interoperabilidad JS.
- **Inyección web**: `shared/build.gradle.kts` reemplaza `%GA_MEASUREMENT_ID%`, `%SENTRY_DSN%` y `%APP_ENV%` en `index.html` durante `wasmJsProcessResources`.
- **Copia Android**: `androidApp/build.gradle.kts` copia el JSON de `firebase/<env>/` a `google-services.json` y fija `manifestPlaceholders["telemetryEnabled"]`.

---

## 5. Configurar

### 5.1 Local (local.properties y Google Services)

1. En `local.properties` (no versionado), definir el ambiente y las claves de prueba (en local solo claves DEV; si se dejan vacías no habrá telemetría web):
   ```properties
   API_ENV=DEV
   GA_MEASUREMENT_ID=G-XXXXXXXXXX
   SENTRY_DSN=https://xxxxxxxx@o000000.ingest.sentry.io/0000000
   ```
2. Descargar el archivo `google-services.json` del proyecto DEV y colocarlo en `androidApp/firebase/dev/google-services.json` (git-ignorado). Si trabajas en un worktree, copia ahí también la carpeta `androidApp/firebase/` (al estar git-ignorada no viaja automáticamente con git).
3. **Orden de lectura de variables**: `local.properties` → propiedades Gradle (`-P`) → variables de entorno.
4. **Orden de resolución del ambiente (`apiEnv`)**: `-PapiEnv` → `API_ENV` en `local.properties` → `DEV` por defecto.
5. ⚠️ **Aviso de claves duplicadas**: si una propiedad aparece más de una vez en `local.properties`, prevalece la **última** (asegúrate de no dejar líneas de PROD al final).
6. Si modificas `local.properties`, detén y reinicia el servidor web de desarrollo para que tome los cambios.

### 5.2 GitHub Actions (GA_MEASUREMENT_ID y SENTRY_DSN)

1. Ve a **Settings → Environments** en el repositorio de GitHub. No crear variables con el mismo nombre a nivel repositorio para evitar conflictos de precedencia.
2. En cada ambiente (`dev` y `prod`), crea las siguientes **Environment variables** (no secrets):
   - `GA_MEASUREMENT_ID`: identificador GA4 correspondiente (`G-...`).
   - `SENTRY_DSN`: DSN público del proyecto correspondiente (`https://...`).
3. El workflow `.github/workflows/deploy-web.yml` inyecta estas variables según la rama: `main` usa `prod`, y cualquier otra rama que no sea `main` usa `dev`.
4. Para verificar la inyección en el deploy, abre **Ver código fuente de la página** (view-source) en el navegador y comprueba que el `G-...` y el DSN correspondan al ambiente; si ves un valor viejo o desactualizado, ve a DevTools → Application → Service workers → Unregister ([§7](#7-problemas-frecuentes-adblock-debugview-vacío-builds)).

### 5.3 Consolas (Firebase, GA4, Crashlytics y Sentry)

- [ ] **Firebase**:
  1. Crear proyectos separados `condominium-app-dev` y `condominium-app-prod` con Google Analytics habilitado (usar la MISMA cuenta de Google Analytics en ambos; Firebase crea una propiedad separada por proyecto).
  2. En **Configuración del proyecto → Tus apps**, registrar app Android con paquete `com.nibs.aptocondominios` (sin SHA-1) y descargar `google-services.json`.
  3. Registrar app Web (sin Firebase Hosting) y copiar solo el `measurementId`.
- [ ] **Google Analytics 4** (en [analytics.google.com](https://analytics.google.com), no en Firebase):
  1. **Administrar → Propiedad → Visualización de datos → Definiciones personalizadas** (no es retroactivo: crearlas antes de subir a PROD; para PROD repetir en la propiedad PROD):
     - Dimensión de evento: `endpoint`
     - Dimensión de usuario: `app_env`
     - Dimensión de evento: `status` (opcional)
     - Métrica de evento: `duration_ms` (Tipo: Tiempo → Milisegundos)
  2. **Administrar → Recopilación y modificación de datos → Flujos de datos → Web → Medición optimizada**: **APAGAR** (evita duplicar vistas de página y lectura de URLs completas).
- [ ] **Crashlytics**:
  - No requiere configuración manual; se activa automáticamente al recibir el primer crash ([§6.2](#62-android-debugview-logcat-y-crashlytics)).
- [ ] **Sentry**:
  1. Crear proyecto Browser JavaScript `condominium-app-web-dev` (y `condominium-app-web-prod`). Nota: la región de datos (US/EU) se elige al crear la organización y no se cambia.
  2. Copiar DSN desde **Settings → Projects → <proyecto> → Client Keys (DSN)**.
  3. En **SDK Setup → Loader Script**: desactivar Performance, Session Replay y Debug Bundles.
  4. En **Security & Privacy**: activar Data Scrubber y Default Scrubbers; Prevent Storing of IP Addresses es recomendado.
  5. **Plan y límites**: inicia con trial Business de 14 días → luego pasa a plan gratuito Developer (cupo mensual de errores limitado y 1 usuario; ver [sentry.io/pricing](https://sentry.io/pricing)). Los eventos que superen el cupo se descartan.
- [ ] **Release Android PROD**:
  - Comando: `.\gradlew.bat :androidApp:bundleRelease -PapiEnv=PROD` con `androidApp/firebase/prod/google-services.json` presente.
  
  | Build | apiEnv | ¿Envía? |
  |---|---|---|
  | release | cualquiera | sí |
  | debug | DEV | sí |
  | debug | PROD | no (protege PROD) |

- [ ] **Al publicar en Play por primera vez**:
  1. Contenido de la app → **Anuncios**: No.
  2. Contenido de la app → **ID de publicidad**: no se usa (el permiso `AD_ID` está removido; si se declara "sí", Play puede bloquear la publicación).
  3. Contenido de la app → **Seguridad de los datos**: además de los datos propios de la app (cuenta, nombre, correo, pagos, etc.), declarar:
     - Actividad en la app → Interacciones.
     - Info y rendimiento → Registros de fallas y Diagnóstico.
     - ID de dispositivo u otros.
     - Ubicación aproximada (opcional, GA la deriva de la IP).
     - Para todos: recopilado sí, compartido no (Firebase es proveedor de servicios), obligatorio, propósito Estadísticas; cifrado en tránsito sí.
  4. **Política de privacidad** (obligatoria, URL en Contenido de la app): apartado con servicios (Firebase Analytics, Crashlytics, GA4 con cookie `_ga`, Sentry), qué se recoge, qué NO (nombre, correo, teléfono, montos, referencias bancarias, IDs internos; sin publicidad) y para qué (estadísticas y estabilidad).

---

## 6. Probar

### 6.1 Web (Chrome y Sentry)

Usa siempre una ventana de incógnito o un perfil de Chrome sin extensiones bloqueadoras ([§7](#7-problemas-frecuentes-adblock-debugview-vacío-builds)).

Desde la raíz del repo (o del worktree en el que trabajes):

```powershell
.\gradlew.bat :shared:wasmJsBrowserDevelopmentRun --continuous -PapiEnv=DEV
```

| Prueba | Qué hacer | Qué esperar |
|---|---|---|
| SDKs cargados | En Consola (`F12`): `typeof gtag` y `typeof Sentry` | Responden `"function"` y `"object"`. |
| Visitas de página | Pestaña Network, filtrar por `collect` | Requests `g/collect` con `tid=G-...`, `en=page_view`, `up.app_env=DEV` y ruta limpia sin UUIDs. |
| Error de API | Clic derecho en un request de API en Network → **Block request URL** → reintentar | Emite `en=api_error` con `status=0`. (No usar modo Offline de Chrome porque bloquea GA). |
| Error en Sentry | En Consola (`F12`): `Sentry.captureException(new Error("prueba"))` | Petición `200 OK` al endpoint `/envelope/`. Aparece en Sentry DEV en 1–2 min. |
| Vista en vivo | En Google Analytics → **Informes → Tiempo real** | Aparecen los usuarios y eventos activos en tiempo real. |

*(Nota: el Loader de Sentry descarga el bundle completo únicamente cuando ocurre el primer error).*

### 6.2 Android (DebugView, Logcat y Crashlytics)

Detén la ejecución continua de la web antes de compilar Android para evitar bloqueos de Gradle ([§7](#7-problemas-frecuentes-adblock-debugview-vacío-builds)).

```powershell
adb devices
.\gradlew.bat :androidApp:installDebug -PapiEnv=DEV
# Si falla con INSTALL_FAILED_UPDATE_INCOMPATIBLE (ya hay instalada otra build firmada con otra clave, p. ej. un release):
# adb uninstall com.nibs.aptocondominios
adb shell setprop log.tag.FA VERBOSE
adb shell setprop debug.firebase.analytics.app com.nibs.aptocondominios
adb shell am force-stop com.nibs.aptocondominios
adb shell monkey -p com.nibs.aptocondominios 1
adb logcat -s FA FA-SVC
```

| Mensaje en Logcat | Significado |
|---|---|
| `App measurement enabled … google app id: 1:<num>:android:…` | Firebase arrancó. Verifica el número con tu `google-services.json`. |
| `Faster debug mode event logging enabled` | Debug mode activo correctamente. |
| `Logging screen view with name, class: login, login` | Pantalla registrada exitosamente. |
| `Logging event: … name=login` o `api_error` | Evento de negocio o fallo de red registrado. |
| `Connected to remote service` | Lote de eventos despachado hacia Firebase. |

*(Líneas de ruido ignorables: `Connection attempt already in progress`, `TCF`, `Tag Manager is not found`, `Install Referrer`).*

- **DebugView**: Firebase Console DEV → **Analytics → DebugView** → seleccionar dispositivo en la esquina superior izquierda.
- **Probar `api_error`**: activar Modo Avión en el teléfono → intentar recargar datos → quitar Modo Avión (Firebase encola y envía).
- **Probar Crashlytics**: provocar un crash, **reabrir la app** (Crashlytics envía al inicio siguiente) y esperar ~5 min:
  ```powershell
  adb shell am crash com.nibs.aptocondominios
  ```
  Para diagnosticar en Logcat: `adb shell setprop log.tag.FirebaseCrashlytics DEBUG` y buscar `Crashlytics report upload complete`.
- **Al terminar**: restaurar modo normal con `adb shell setprop debug.firebase.analytics.app .none.`.

### 6.3 Cuándo aparece cada dato

| Destino | Tiempo estimado de visualización |
|---|---|
| DebugView de Firebase | Segundos tras la acción |
| GA4 Tiempo Real (Web) | Segundos |
| GA4 Tiempo Real (Android) | Minutos (en lotes, salvo con DebugView activo) |
| Consola de Sentry | 1 a 2 minutos |
| Consola de Crashlytics | ~5 minutos tras **reabrir** la aplicación |
| Informes estándar GA4 | 24 a 48 horas (los datos de DebugView se excluyen de aquí) |
| Dimensiones personalizadas | Aplican únicamente a datos recibidos **después** de su creación |

---

## 7. Problemas frecuentes (Adblock, DebugView vacío, builds)

| Síntoma | Causa probable | Solución |
|---|---|---|
| `ERR_BLOCKED_BY_CLIENT` en `sentry-cdn.com` o sin peticiones `collect` | Extensión de bloqueo (uBlock, Adblock, Surfshark CleanWeb, Brave Shields) o Edge (Prevención de seguimiento estricta) | Usar ventana de incógnito o perfil limpio. Surfshark: lo bloquea CleanWeb de la extensión, no la VPN; Bypasser no sirve para esto. Identificar: desactivar extensiones una a una; uBlock tiene un Logger con la regla exacta. |
| DebugView en Firebase no muestra eventos | Falta activar la propiedad de debug o dispositivo incorrecto en el selector | Ejecutar `adb shell setprop debug.firebase.analytics.app com.nibs.aptocondominios`, reiniciar app y elegir dispositivo en DebugView. |
| Logcat no muestra líneas de `FA` ni `FA-SVC` | Falta `google-services.json` o se compila debug apuntando a `PROD` | Colocar el JSON en `androidApp/firebase/dev/`. Debug contra `PROD` deshabilita la telemetría por seguridad. |
| Datos locales aparecen en el proyecto de PROD | Clave duplicada en `local.properties` (gana la última) | Revisar `local.properties` y asegurar que no existan líneas duplicadas de `GA_MEASUREMENT_ID` o `SENTRY_DSN`. |
| `%GA_MEASUREMENT_ID%` se ve literal en el HTML | El build no leyó las variables de `local.properties` | Verificar que las claves no estén vacías y recompilar la distribución web. |
| Build corrupto o fallos extraños de compilación | Dos builds de Gradle ejecutándose simultáneamente en el worktree | Detener el servidor web (`Ctrl+C`) antes de compilar Android. Alternativa estática: `.\gradlew.bat :shared:wasmJsBrowserDevelopmentExecutableDistribution -PapiEnv=DEV` y luego `npx serve shared\build\dist\wasmJs\developmentExecutable -l 8080` (sin recarga automática). |
| Deploy web publicado muestra versión vieja | El Service Worker tiene cacheados los recursos anteriores | En DevTools (`F12`) → **Application → Service Workers → Unregister** y borrar almacenamiento. |

---

## 8. Pendientes

- [ ] Desactivar Medición optimizada en los flujos web de GA4 (DEV y PROD).
- [ ] Configurar variables de entorno `GA_MEASUREMENT_ID` y `SENTRY_DSN` en los environments `dev` y `prod` de GitHub.
- [ ] Crear proyecto Firebase PROD (`condominium-app-prod`), definiciones personalizadas en GA4 PROD y proyecto Sentry PROD (`condominium-app-web-prod`).
- [ ] Sentry: tras el trial, revisar que el plan gratuito alcance.
- [ ] Al publicar en Play: declaraciones de Contenido de la app y política de privacidad ([§5.3](#53-consolas-firebase-ga4-crashlytics-y-sentry)).
- [ ] Ajustar `.github/workflows/deploy-web.yml`: validar que el paso "Verify API target" falle si las claves web están vacías en `prod`.
- [ ] Evaluar túnel propio para Sentry si se requiere capturar errores en clientes con adblock estricto en escritorio.
- [ ] Evaluar banner de cookies únicamente si cambia la jurisdicción o se incorporan servicios publicitarios/seguimiento cruzado.
- [ ] iOS: sin telemetría hasta que exista la aplicación nativa en el repositorio.
