# AGENTS.md

Guía rápida para agentes (y personas) que trabajan en este repositorio.

## Proyecto

App de condominio (residentes) en **Kotlin Multiplatform + Compose Multiplatform**.

- `shared/` — UI, ViewModels, red (Ktor), DI (Koin) compartidos. Targets: Android, iOS y **web (`wasmJs`)**.
- `androidApp/` — app Android (entrada, AdMob).
- Configuración local: copiar `local.properties.example` a `local.properties` (no versionado).
- API: URL por ambiente desde `local.properties` (`API_ENV`, `<ENV>_API_URL`); ver `docs/web.md` §2.

## Versión web

Toda la información para correr, probar, desplegar y modificar la versión web (servidor de desarrollo, ambientes,
PWA/service worker, reglas de layout adaptable, versiones acopladas y limitaciones) está en:

➡️ **[docs/web.md](docs/web.md)**

Reglas clave al tocar UI:
- El layout móvil (`Compact`, < 600dp) no cambia; lo ancho va detrás de `isWide()`/`isExpanded()` (`ui/components/WebLayout.kt`).
- Nada de APIs JVM/Android en `commonMain`: el target web no compila.
- Compilar ambos antes de entregar: `./gradlew :androidApp:assembleDebug :shared:wasmJsBrowserDistribution`.

## Otros documentos

- `docs/condominium_app_documentation.md` — documentación general de la app.
- `docs/web-pwa-handoff.md` — historial de decisiones de la migración a web/PWA.
- `api_reference.md`, `swagger.json` — referencia de la API.
