# Pendientes tras la auditoría de UI

Estado al 2026-10-26. Rama `feature/ui-audit-fixes` (`ae19aff`, sin push): los 9 pasos de `docs/ui-audit.md` están
integrados con 183 tests en verde. Aquí queda lo que falta, ordenado por prioridad.

## 1. Urgente (manual, fuera del código)
Hallazgos de la revisión de seguridad (2026-10-25).
- **Rotar el token de Vercel** que está en `local.properties` (empieza con `vcp_`): quedó expuesto en la salida de un
  subagente.
- **Admin semilla**: una migración de `condominium-api` crea un admin con contraseña fija (BE-A5). Revisar si existe en
  DEV/PROD y rotarla.
- **Bucket `payment-proofs` público** (BE-A1): pasarlo a privado y servir los comprobantes con URLs firmadas.

## 2. Antes del PR a `dev`
- [ ] **A-28**: el selector de propiedad muestra «No pudimos cargar tus propiedades» con red (web y Android).
  - Hipótesis: `UserUnitDto.buildingId` es obligatorio (`ApiResponses.kt:90-96`), pero el backend lo omite si el join
    `units` viene nulo. La decodificación falla y `safeRequest` lo convierte en un 500.
  - Duda: si fuera eso, también fallaría el arranque, y el dashboard carga.
  - Para confirmarlo: con la pestaña Network abierta, esperar más de 10 s en el dashboard y abrir el selector. Copiar la
    respuesta de `users/me`.
  - Arreglo probable: `buildingId` nullable y separar el error de decodificación de un 500 real.
  - Si no se resuelve, pasarlo a ⏸️ con motivo: el PR debe salir solo con ✅ o ⏸️.
- [ ] Compilar producción en el checkout principal: `./gradlew :androidApp:assembleDebug :shared:wasmJsBrowserDistribution -PapiEnv=DEV`.
- [ ] Repaso rápido de todas las pantallas en web (DEV) y Android.
- [ ] Decidir si se versiona `.claude/agents/` (`ui-audit-fixer.md`, `security-reviewer.md`).
- [ ] Push y PR a `dev`, con `docs/ui-audit-pruebas.md` como «Cómo probar». Después, borrar esa guía.
- [ ] Opcional: `/security-review` sobre la rama.

## 3. Falta backend (⏸️ en `ui-audit.md`)
| ID | Qué falta |
|---|---|
| A-09 | Endpoint y esquema para las preferencias de notificación. Hoy solo viven en memoria. |
| A-11 | `PATCH users/me/password` no verifica la contraseña actual; solo recibe `new_password`. |
| A-12 | Descargar recibo, descargar factura y contactar soporte: no hay endpoints. Los botones no hacen nada. |
| E-L6 | URLs de Términos de servicio y Política de privacidad para enlazar en el registro. |

## 4. Seguridad: backend (`condominium-api`, necesita su propia sesión)
- **Crítica:**
  - BE-C1: un miembro de la junta puede crear admins o asignar roles; no se valida quién hace el pedido
    (`users/presentation/routes.ts:343,362,419`).
  - BE-C2: una membresía pendiente ya da acceso a los datos de otra unidad.
- **Alta:**
  - BE-A1: el bucket es público (ver §1).
  - BE-A2: el rol BOARD se trata como global.
  - BE-A3: el registro de pagos confía en los ids que manda el cliente.
  - BE-A4: las votaciones no validan el edificio.
  - BE-A5: admin semilla (ver §1).
- **Media y baja:** BE-M1…M5 y BE-B1…B3. Ver el reporte del `security-reviewer`; conviene volver a correrlo para el
  detalle.

## 5. Seguridad: app
- APP-M1: los logs de red dependen de `local.properties` y no del tipo de build. Un release podría loguear cuerpos con
  datos personales.
- APP-M2: el token de Vercel está en texto plano en `local.properties` (ver §1).
- APP-M3: el JWT se guarda en `localStorage` y no hay CSP. Los enlaces ya se filtran (A-22).
  - La CSP no se agregó en el paso 9 porque requiere permitir el script inline (hash), `'wasm-unsafe-eval'`,
    `blob:`/`data:` y la API, y hay que probarla en todos los flujos.
- APP-B1…B4: tráfico en claro permitido, `println`, logout solo local y segmento de ruta sin codificar.

## 6. Bugs y mejoras conocidas (fuera de los IDs)
- Cartelera: el error de la lista muestra la excepción cruda (resuelto en `feature/offline-errorstate`).
- Votaciones: la lista y el detalle no usan `ErrorState` (resuelto en `feature/offline-errorstate`).
- `QrScannerViewModel.kt:58`: ante cualquier fallo no de red muestra `qr_invalid_code`, incluyendo errores 5xx del servidor (`ApiFailureException`). Debe alinearse con `RegisterViewModel` distinguiendo 404/400 (código inválido) de 5xx (`error_unknown` o error de servidor) en vez de asumir siempre código inválido.
- Un pago rechazado se ve como «Aplicado». Puede venir así de la API: verificar.
- Los montos no tienen separador de miles.
- El título de la pestaña del navegador no se traduce.
- Android: tocar la pestaña Pagos estando en Historial no vuelve a la raíz.
- Web: F5 en una URL de detalle vuelve al dashboard.
- Web: el botón atrás del navegador no muestra el diálogo «¿Descartar los cambios?», porque `bindToBrowserNavigation`
  no pasa por `BackHandler`. La flecha de la app y Esc sí lo muestran.
- Sidebar: el texto del ítem activo queda en ~4.1:1 sobre su tinte, por debajo de AA.
- Caja chica y el contador de anuncios no leídos se ocultan sin aviso si fallan.
- Icono maskable: `icon-512.png` tiene fondo gris y poco margen, así que se recorta con un halo. Necesita arte nuevo:
  naranja hasta el borde y el glifo dentro del 80 % central.

## 7. Sin probar
- La PWA instalada en Android (necesita HTTPS: después del deploy a DEV).
- Paso 3: modo avión y TalkBack en Android.
- Paso 8: las filas de la guía no se marcaron una por una.
- A-22 (enlaces seguros en Cartelera) y E-U1 (usuario sin unidades): DEV no tiene datos; los cubren tests.
- DEV no tiene facturas pendientes ni parciales: falta probar «Pagar» y los montos de una parcial.
- iOS no se compiló (solo hay Windows).

## 8. Deuda de tests
- `RemoteAuthRepositoryTest` › `creating the repository with a saved token does not call users me` espera 150 ms
  reales. Es el primer sospechoso si algún día falla al azar.
- En el paso 8, el rojo de TDD fue por compilación (símbolos inexistentes), no por aserción.
- CI (`.github/workflows/ci.yml`) corre los tests y los builds de Android y web en cada PR a `dev`/`main`; sus checks
  `tests`, `android` y `web` son obligatorios en los rulesets de `dev` y `main`.
