# AGENTS.md

Guía rápida para agentes (y personas) que trabajan en este repositorio.

## Proyecto

App de condominio (residentes) en **Kotlin Multiplatform + Compose Multiplatform**.

- `shared/` — UI, ViewModels, red (Ktor), DI (Koin) compartidos. Targets: Android, iOS y **web (`wasmJs`)**.
- `androidApp/` — app Android (entrada, Firebase Analytics + Crashlytics).
- Configuración local: copiar `local.properties.example` a `local.properties` (no versionado).
- API: URL por ambiente desde `local.properties` (`API_ENV`, `<ENV>_API_URL`); ver `docs/web.md` §2.
- Telemetría: Firebase Analytics + Crashlytics en Android (`androidApp/firebase/<env>/google-services.json`) y GA4 + Sentry Loader en web (`GA_MEASUREMENT_ID`, `SENTRY_DSN`), con proyectos separados para DEV y PROD; ver `docs/telemetria.md`.

## Ramas

- `feature/*` → PR a `dev` (DEV, API beta) → PR a `main` (PROD). Push a `dev`/`main` despliega la web en Vercel
  (`.github/workflows/deploy-web.yml`). Detalle en `docs/web.md` §4.

## Versión web

Toda la información para correr, probar, desplegar y modificar la versión web (servidor de desarrollo, ambientes,
PWA/service worker, reglas de layout adaptable, versiones acopladas y limitaciones) está en:

➡️ **[docs/web.md](docs/web.md)**

Reglas clave al tocar UI:
- El layout móvil (`Compact`, < 600dp) no cambia; lo ancho va detrás de `isWide()`/`isExpanded()` (`ui/components/WebLayout.kt`).
- Nada de APIs JVM/Android en `commonMain`: el target web no compila.
- Un solo build de Gradle a la vez (builds concurrentes corrompen `build/`); errores frecuentes en `docs/web.md` §9.
- Compilar ambos antes de entregar: `./gradlew :androidApp:assembleDebug :shared:wasmJsBrowserDistribution`.

## Forma de trabajo: agentes del proyecto

Todo cambio de código pasa por un agente de `.claude/agents/` (Claude Code). No implementes directo en el checkout
principal: cada agente trabaja en su propio worktree, con TDD, y no hace commit.

| Agente | Rol | Cuándo usarlo |
|---|---|---|
| [`implementer`](.claude/agents/implementer.md) | Implementa una funcionalidad o mejora acotada con TDD | Features, cambios de UI, mejoras |
| [`fixer`](.claude/agents/fixer.md) | Resuelve un bug o issue: reproduce con un test, causa raíz, fix mínimo, barrido del patrón | Bugs reportados, issues de GitHub, ítems de `docs/pendientes.md` |
| [`security-reviewer`](.claude/agents/security-reviewer.md) | Revisión de seguridad de solo lectura (app y, si se pide, backend) | Antes de un PR a `main`, o al tocar sesión, red, archivos o permisos |

Cada agente existe en dos formatos, uno por herramienta:
- `.claude/agents/<nombre>.md` — Claude Code. **Fuente única de las instrucciones.**
- `.agents/agents/<nombre>.md` — Antigravity (`agy`). Wrapper: frontmatter propio y solo lo que funciona distinto ahí
  (sin aislamiento de worktree automático, iteración en la misma conversación); remite a la fuente.

**Al crear, renombrar, borrar o cambiar el nombre/descripción de un agente, actualiza ambos archivos en el mismo cambio.**
Las instrucciones se editan solo en `.claude/agents/`; el wrapper de `.agents/agents/` se toca únicamente si cambia algo
propio de Antigravity. El wrapper debe declarar `tools:` (sin esa lista agy solo da herramientas de lectura).
Comprueba que agy los carga con `agy agents`.

Cómo usarlos:
- En Antigravity: `agy --agent implementer -i "<tarea, rama base y worktree>"`, o pídele «usa el fixer para…».
- Pídelo por nombre en Claude Code: «usa el implementer para…», «usa el fixer para el issue #N», «corre el
  security-reviewer sobre esta rama». Indica la rama base (por defecto, la rama actual).
- Ciclo: el agente entrega tests en verde, build de Android y web, y una **guía de prueba manual** por plataforma
  (web en Chrome y Android). Pruebas a mano siguiendo la guía; el feedback vuelve al **mismo** agente, que itera en su
  worktree. Recién cuando lo apruebas: commit, merge a tu rama `feature/*` y PR a `dev`.
- TDD: un test existente que falla significa que cambió un comportamiento; solo se actualiza si el cambio se pidió
  explícitamente. Tests en `shared/src/commonTest`: `./gradlew :shared:testDebugUnitTest -PapiEnv=DEV`.
- Antes de un PR, los tests en verde son obligatorios. CI (`.github/workflows/ci.yml`) corre tests, build Android y
  build web en cada PR a `dev`/`main`; sus checks (`tests`, `android`, `web`) son obligatorios para mergear a `main`
  (en `dev` solo informan: GitHub Actions no puede saltarse un ruleset y `sync-main-to-dev.yml` hace push directo).

## Otros documentos

- `docs/condominium_app_documentation.md` — documentación general de la app.
- `docs/telemetria.md` — telemetría (analítica y errores) web y Android.
- `docs/web-pwa-handoff.md` — historial de decisiones de la migración a web/PWA.
- `docs/ui-audit.md` — auditoría de UI con estado por hallazgo (actualizar el estado al corregir).
- `docs/pendientes.md` — pendientes abiertos (bugs, falta de backend, seguridad).
- `api_reference.md`, `swagger.json` — referencia de la API.
