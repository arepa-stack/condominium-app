# Auditoría de UI — Web Interface Guidelines (oct 2026)

Revisión de `shared/src/commonMain/kotlin/com/example/condominio/ui/` contra las
[Web Interface Guidelines de Vercel](https://github.com/vercel-labs/web-interface-guidelines),
adaptadas a Compose Multiplatform (en web todo se dibuja en un `<canvas>`, así que las reglas de DOM/CSS
se traducen a `contentDescription`, `semantics`, `KeyboardOptions`, `maxLines`, etc.).

- **Método:** 9 revisiones por pantalla, leyendo el código completo. **Sin ejecución**: los hallazgos
  de producto (bloque A) están confirmados en código (2026-10-21), pero conviene reproducirlos en la app
  al corregirlos.
- **Alcance:** Android, iOS y web comparten este código; salvo que se indique, cada hallazgo aplica a todas
  las plataformas.
- **Total aproximado:** ~480 hallazgos. Este documento lista los relevantes con ID; el resto queda como
  barrido por pantalla (bloque F).

## Estados

| Estado | Significado |
|---|---|
| 🔍 Verificar | Detectado en código; falta reproducirlo en la app |
| ⬜ Pendiente | Confirmado (o evidente en código), sin empezar |
| 🔄 En curso | Alguien lo está corrigiendo (anotar rama/PR) |
| ✅ Hecho | Corregido y verificado (anotar commit) |
| ⏸️ Descartado | No se corrige (anotar motivo) |

Al cambiar un estado, actualizar también la columna **Ref** (rama, PR o commit).

## Resumen

| Bloque | Ítems | 🔍 | ⬜ | 🔄 | ✅ |
|---|---|---|---|---|---|
| A. Errores de producto | 15 | 0 | 14 | 0 | 1 |
| B. Componentes compartidos | 10 | 0 | 0 | 0 | 10 |
| C. Textos e i18n | 9 | 0 | 4 | 1 | 4 |
| D. Contraste | 6 | 0 | 2 | 0 | 4 |
| E. Por pantalla (prioritarios) | 37 | 0 | 37 | 0 | 0 |

---

## A. Errores de producto (lógica / datos) — prioridad alta

| ID | Pantalla | Problema | Dónde | Estado | Ref |
|---|---|---|---|---|---|
| A-01 | Pagos | El monto con coma decimal ("150,50") da "Monto inválido": se parsea con `toDoubleOrNull()` | `CreatePaymentScreen.kt:774`, `CreatePaymentViewModel.kt:117` | ⬜ Pendiente | Confirmado en código (2026-10-21) |
| A-02 | Pagos (iOS) | `formatCurrency` en iOS devuelve separador de miles; al volver a parsearlo, montos ≥ 1000 dan "Monto inválido" | `CreatePaymentViewModel.kt:110`, `KmpFormatters.ios.kt:15` | ⬜ Pendiente | Confirmado en código (2026-10-21) |
| A-03 | Pagos | Marcar/desmarcar facturas sobrescribe sin aviso el monto que escribió el usuario | `CreatePaymentViewModel.kt:110` | ⬜ Pendiente | Confirmado en código (2026-10-21) |
| A-04 | Votaciones | Presupuesto "1.500" (miles en es) se envía como 1.5 sin avisar; "1500,50" se rechaza | `DecisionSheetsStubs.kt:121` | ⬜ Pendiente | Confirmado en código (2026-10-21) |
| A-05 | Dashboard | Muestra "¡Estás al día!" cuando el saldo aún carga o falló (`totalDebt` por defecto 0.0) | `DashboardScreen.kt:219, 801`, `DashboardViewModel.kt:69-80` | ⬜ Pendiente | Confirmado en código (2026-10-21) |
| A-06 | Dashboard | Doble carga al abrir (`init` + `ON_RESUME`) y `refresh()` no cancela el job anterior → datos viejos / de otra unidad | `DashboardViewModel.kt:29, 93-96`, `DashboardScreen.kt:97` | ⬜ Pendiente | Confirmado en código (2026-10-21) |
| A-07 | Varias | Errores de red mostrados como vacío o pantalla en blanco, sin reintentar: historial de pagos, crear pago ("No tienes recibos pendientes"), detalle de pago, detalle de factura ("No se encontraron facturas"), dashboard, selector de propiedad | `PaymentHistoryViewModel.kt:53`, `CreatePaymentViewModel.kt:56`, `PaymentDetailScreen.kt:241`, `InvoiceDetailScreen.kt:163-184`, `DashboardScreen.kt:108`, `UnitSelectionViewModel.kt:49` | ⬜ Pendiente | Confirmado en código (2026-10-21) |
| A-08 | Facturas | El error de la lista nunca se limpia: tras un refresh exitoso sigue mostrándose | `InvoiceListViewModel.kt:29, 37` | ⬜ Pendiente | Confirmado en código (2026-10-21) |
| A-09 | Perfil | Preferencias de notificación solo en memoria: no se guardan ni se envían a la API | `NotificationSettingsScreen.kt:31-33` | ⬜ Pendiente | Confirmado en código (2026-10-21) |
| A-10 | Perfil | Editar perfil: la unidad es editable pero no se envía (solo `name`); además bloquea guardar si no hay unidad | `EditProfileScreen.kt:85-90`, `EditProfileViewModel.kt:53-75`, `RemoteAuthRepository.kt:204-210` | ⬜ Pendiente | Confirmado en código (2026-10-21) |
| A-11 | Perfil | Cambiar contraseña pide la actual pero el backend no la verifica (solo envía `newPassword`) | `ChangePasswordScreen.kt:232`, `RemoteAuthRepository.kt:226-228` | ⬜ Pendiente | Confirmado en código (2026-10-21) |
| A-12 | Pagos / Facturas | Botones que no hacen nada: "Descargar recibo" (solo `println`), "Descargar factura" (TODO), "Contactar soporte" (onClick vacío) | `PaymentDetailScreen.kt:71-75, 117, 354`, `CondominioNavGraph.kt:351` | ⬜ Pendiente | Confirmado en código (2026-10-21) |
| A-13 | Votaciones | Porcentajes sin formato ("33.333333333333336%"); empates reciben #1/#2 y "Liderando" pese al aviso "Empate técnico" | `DecisionDetailScreen.kt:1150, 170-175, 1082-1091` | ⬜ Pendiente | Confirmado en código (2026-10-21) |
| A-14 | Primer login | Sin salida: no se puede cerrar sesión ni volver si el usuario no cambia la contraseña | `FirstLoginChangePasswordScreen.kt:48-86` | ⬜ Pendiente | Confirmado en código (2026-10-21) |
| A-15 | Votaciones | Detalle de votación muestra error de JSON si un presupuesto no tiene archivo (`file_url` null) | `DecisionModels.kt:61` | ✅ Hecho | `worktree-agent-a8dd877d108cce00f` — `fileUrl: String?`; sin archivo se oculta "Ver archivo" |

## B. Componentes compartidos — un arreglo corrige todas las pantallas

| ID | Componente | Problema | Dónde | Estado | Ref |
|---|---|---|---|---|---|
| B-01 | `TopBarWithBack` | Botón atrás sin nombre accesible (`Res.string.back` ya existe); título con `maxLines = 1` pero sin `overflow = Ellipsis` ni `heading()` | `TopBarWithBack.kt:49, 79, 55-72` | ✅ Hecho | `worktree-agent-a8dd877d108cce00f` |
| B-02 | `LabeledOutlinedField` | La etiqueta es un `Text` aparte, no asociado al campo; no expone `contentType` (autofill). `isError` y `supportingText` ya existen, pero las pantallas no los usan (ver E-L4, E-P1) | `LabeledOutlinedField.kt:47-71` | ✅ Hecho | `worktree-agent-a8dd877d108cce00f` — `contentType` disponible; aplicarlo en las pantallas es E-L1 |
| B-03 | `AppSidebar` | Ítems sin `selectable(role = Role.Tab)` ni `selectableGroup()`: no se anuncia la sección activa | `AppShell.kt:61, 89` | ✅ Hecho | `worktree-agent-a8dd877d108cce00f` |
| B-04 | `AppBottomBar` / `AppNavRail` | Sin `Role.Tab`/selected; `contentDescription = label` duplica el texto; área táctil ~38dp; `fontSize` 10/11sp fijo | `AppBottomBar.kt:135-154, 188-189` | ✅ Hecho | `worktree-agent-a8dd877d108cce00f` |
| B-05 | `StatusBadge` / `AdminWebStyle` | Texto del color de estado sobre el mismo color al 10–20%: contraste 2–3.8:1. Falta un token `on*Container` por estado | `StatusBadge.kt:35`, `AdminWebStyle.kt:30-31` | ✅ Hecho | `worktree-agent-a8dd877d108cce00f` — `onStatusContainer()` + `AptoOnWarningContainer` (`Color.kt`) |
| B-06 | `LoadingState` / `CondoButtons` | Spinners sin semántica: no se anuncia la carga y el botón queda sin nombre | `LoadingState.kt:29`, `CondoButtons.kt:41-42` | ✅ Hecho | `worktree-agent-a8dd877d108cce00f` |
| B-07 | `Type.kt` | Ningún estilo con `fontFeatureSettings = "tnum"`: montos y tablas desalineados | `Type.kt:21-136` | ✅ Hecho | `worktree-agent-a8dd877d108cce00f` |
| B-08 | `FilterPillChip` | Sin `Role.Tab`/selected, ~36dp de alto, selección solo por color | `AnnouncementComponents.kt:266-278` | ✅ Hecho | `worktree-agent-a8dd877d108cce00f` |
| B-09 | `NavGraph` | Transición fade de 700 ms (la de por defecto de `NavHost`: no hay `enterTransition`/`exitTransition` propios), no respeta "reducir movimiento" | `CondominioNavGraph.kt:192-203` | ✅ Hecho | `worktree-agent-a8dd877d108cce00f` — fade 200 ms; sin transición con reducir movimiento (`ui/utils/ReducedMotion.kt`) |
| B-10 | `ShimmerEffect` / animaciones infinitas | No respetan reduced motion (shimmer, escáner QR, barra del splash) | `ShimmerEffect.kt:19-27`, `QrScannerScreen.kt:151`, `SplashScreen.kt:117` | ✅ Hecho | `worktree-agent-a8dd877d108cce00f` |

## C. Textos e i18n

| ID | Problema | Dónde | Estado | Ref |
|---|---|---|---|---|
| C-01 | ~60 textos visibles hardcodeados en Kotlin (muchos ya existen en `strings.xml`): sobre todo Pagos (~30), Cartelera, splash/marca ("Tu condominio, en orden.", "VIVIENDA ELEVADA", "Apto by Nibs") | `PaymentHistoryScreen.kt`, `PaymentDetailScreen.kt`, `CreatePaymentScreen.kt`, `BillboardDetailScreen.kt`, `AnnouncementComponents.kt:41-47, 176`, `AppShell.kt:74-133`, `SplashScreen.kt` | ⬜ Pendiente | |
| C-02 | Claves faltantes en `values-en`: `download_invoice`, `no_invoices_for_filter`, `billboard_detail_title`, `decisions_see_detail`, `decisions_urgent`, `decisions_finalized_on` (y `settings`, `share`, sin uso). A la inversa, `values-en` tiene 36 claves que no existen en `values` (caja chica: `cat_*`, `category_*`, `register_expense`…), todas sin uso: borrar | `values-en/strings.xml` | ✅ Hecho | `worktree-agent-a8dd877d108cce00f` — 36 claves en-only borradas (sin uso verificado); 8 traducciones añadidas |
| C-03 | Title Case en español ("Deuda Total", "Iniciar Sesión", "Cerrar Sesión", "Historial de Pagos"…): pasar a sentence case | `values/strings.xml` | ✅ Hecho | `worktree-agent-a8dd877d108cce00f` |
| C-04 | Mayúsculas dentro del texto ("GANADOR", "CORREO ELECTRÓNICO", estados): aplicar con estilo, no en el recurso | `values/strings.xml`, `RegisterScreen.kt` | ✅ Hecho | `worktree-agent-a8dd877d108cce00f` |
| C-05 | Tono mezclado tú / usted / vos ("Seleccione", "acérquese", "Debés") | `values/strings.xml:144`, `CreatePaymentScreen.kt:500` | ⬜ Pendiente | |
| C-06 | "..." en vez de "…" (`loading`, `qr_scan_scanning`) y falta de ortografía "Registrate" | `values*/strings.xml:24, 79` | ✅ Hecho | `worktree-agent-a8dd877d108cce00f` |
| C-07 | Plurales armados a mano ("1 presupuestos", "entendido(s)", "recibo(s) seleccionado(s)"): usar `<plurals>` | `values*/strings.xml`, `CreatePaymentScreen.kt:357, 986` | 🔄 En curso | `worktree-agent-a8dd877d108cce00f` — `<plurals>` de `strings.xml` hechos; falta el plural armado en `CreatePaymentScreen.kt` (paso posterior) |
| C-08 | Fechas: formatos distintos entre pantallas, ISO crudo en cartelera, meses en español también en inglés | `InvoiceListScreen.kt:482`, `InvoiceDetailScreen.kt:749`, `BillboardDetailScreen.kt:313`, `PaymentHistoryScreen.kt:258`, `DecisionComponents.kt:87` | ⬜ Pendiente | |
| C-09 | `$` hardcodeado en lugar de `currency_amount` (puede contradecir la moneda mostrada) | `DashboardScreen.kt:597`, `InvoiceDetailScreen.kt:353`, `DecisionDetailScreen.kt:848, 1345`, `DecisionSheetsStubs.kt:373`, `CreatePaymentScreen.kt:306, 351, 418` | ⬜ Pendiente | |

## D. Contraste (WCAG AA: 4.5:1 texto normal, 3:1 texto grande / íconos)

| ID | Par | Contraste | Dónde | Estado | Ref |
|---|---|---|---|---|---|
| D-01 | Badges de estado (web: todos; móvil: aviso y pendiente) | 1.99–3.82:1 | `StatusBadge.kt`, `AdminWebStyle.kt` (ver B-05) | ✅ Hecho | `worktree-agent-a8dd877d108cce00f` — ver B-05; móvil ≥ 4.9:1, web ≥ 4.8:1 |
| D-02 | Pestaña inactiva barra inferior/rail (#9CA3AF) | 2.39–2.54:1 | `AppBottomBar.kt:113, 174` | ✅ Hecho | `worktree-agent-a8dd877d108cce00f` — `AptoNavInactive` #6B7280: 4.83:1 (blanco), 4.55:1 (rail web) |
| D-03 | Pestaña activa móvil (#FF6B00 sobre blanco) | 2.86:1 | `AppBottomBar.kt:112` | ✅ Hecho | `worktree-agent-a8dd877d108cce00f` — activa = `colorScheme.primary`: #C45006 4.67:1 (móvil), #B45309 4.73:1 (rail web) |
| D-04 | Texto blanco del panel de marca del login sobre el degradado | 1.79–3.19:1 | `AppShell.kt:124-135` | ⬜ Pendiente | |
| D-05 | Blanco sobre el primary móvil (#DB5A07) | 3.83:1 | `Color.kt:28` | ✅ Hecho | `worktree-agent-a8dd877d108cce00f` — #DB5A07 → #C45006: 4.67:1 |
| D-06 | Textos con `onSurface` al 50–60% de alfa (perfil, notificaciones, primer login) | α 0.5: 3.3:1; α 0.6: ~4.5:1 (en el límite) | `EditProfileScreen.kt:72`, `NotificationSettingsScreen.kt:80`, `FirstLoginChangePasswordScreen.kt:160` | ⬜ Pendiente | |

Pares que pasan: `onSurfaceVariant` sobre blanco/fondo (5.05–9.38:1), blanco sobre primary web (5.02:1).

## E. Por pantalla — prioritarios

### Login y autenticación
| ID | Problema | Dónde | Estado | Ref |
|---|---|---|---|---|
| E-L1 | Ningún campo con autofill (`contentType` Email/Password/NewPassword/nombres) ni `autoCorrectEnabled = false` en email/códigos | `LoginScreen.kt:242-287`, `RegisterScreen.kt:263-305`, `ChangePasswordScreen.kt:298` | ⬜ Pendiente | |
| E-L2 | Enter repetido dispara el login dos veces (`onDone` no mira `isLoading`); email sin `trim()` | `LoginScreen.kt:283-286`, `LoginViewModel.kt:67` | ⬜ Pendiente | |
| E-L3 | FirstLogin y Register no se pueden enviar desde el teclado en móvil (en web Enter ya envía; en móvil no hay ImeAction) | `FirstLoginChangePasswordScreen.kt:138-143`, `RegisterScreen.kt:300-305` | ⬜ Pendiente | |
| E-L4 | Errores sin `liveRegion` ni `isError` por campo; fallos silenciosos si `exception.message` es null | `ForgotPasswordViewModel.kt:38`, `FirstLoginChangePasswordViewModel.kt:57`, `RegisterViewModel.kt:95`, `LoginViewModel.kt:90` | ⬜ Pendiente | |
| E-L5 | Escáner QR: cerrar es `Text("✕")` sin descripción y de 40dp; código pasado sin codificar a la ruta | `QrScannerScreen.kt:97-105, 60-69` | ⬜ Pendiente | |
| E-L6 | Términos aceptados al registrarse sin enlace a los documentos | `RegisterScreen.kt:335` | ⬜ Pendiente | |
| E-L7 | Splash con `delay(2400)` fijo en cada arranque (también web) | `SplashScreen.kt:32` | ⬜ Pendiente | |

### Dashboard
| ID | Problema | Dónde | Estado | Ref |
|---|---|---|---|---|
| E-D1 | "Cartelera" hardcodeado en móvil | `DashboardScreen.kt:423` | ⬜ Pendiente | |
| E-D2 | Avatar se anuncia solo con la inicial; acciones rápidas se leen dos veces | `DashboardScreen.kt:362, 774, 522` | ⬜ Pendiente | |
| E-D3 | Dos "Ver todo" con destinos distintos, ~16dp de alto, sin rol | `DashboardScreen.kt:171-176, 243-251, 980` | ⬜ Pendiente | |
| E-D4 | Sin estado vacío de transacciones en móvil; badges de 9sp ilegibles | `DashboardScreen.kt:181, 587, 635` | ⬜ Pendiente | |
| E-D5 | Nombre/unidad/descripciones sin `maxLines` ni `weight` | `DashboardScreen.kt:280-293, 333-345, 755` | ⬜ Pendiente | |

### Seleccionar propiedad
| ID | Problema | Dónde | Estado | Ref |
|---|---|---|---|---|
| E-U1 | Sin estado vacío (usuario sin edificios) | `UnitSelectionScreen.kt:98-121, 357-396` | ⬜ Pendiente | |
| E-U2 | El ícono "colapsar" no colapsa (`expandedBuildingId` nunca vuelve a null) | `UnitSelectionScreen.kt:240`, `UnitSelectionViewModel.kt:60` | ⬜ Pendiente | |
| E-U3 | Expandido/seleccionado no expuesto a accesibilidad | `UnitSelectionScreen.kt:166-169, 238-247, 477-498` | ⬜ Pendiente | |

### Facturación
| ID | Problema | Dónde | Estado | Ref |
|---|---|---|---|---|
| E-F1 | Botón "Pagar" anidado dentro de una card clicable | `InvoiceListScreen.kt:207, 295` | ⬜ Pendiente | |
| E-F2 | Tabla web no lazy (`Column + forEach`) | `InvoiceListScreen.kt:390-407` | ⬜ Pendiente | |
| E-F3 | Montos: columna "Pagado" centrada, sin `maxLines` (se parten) | `InvoiceListScreen.kt:248, 487-505` | ⬜ Pendiente | |
| E-F4 | Filtro con `remember` (se pierde al volver del detalle) | `InvoiceListScreen.kt:64` | ⬜ Pendiente | |

### Pagos
| ID | Problema | Dónde | Estado | Ref |
|---|---|---|---|---|
| E-P1 | Validación con un único error al pie, sin inline ni foco; el error no se limpia al editar | `CreatePaymentScreen.kt:523-529`, `CreatePaymentViewModel.kt:63-93` | ⬜ Pendiente | |
| E-P2 | Checkbox de facturas sin semántica (toggleable / Role.Checkbox) | `CreatePaymentScreen.kt:259-284` | ⬜ Pendiente | |
| E-P3 | Android no acepta PDF (`PickVisualMedia.ImageOnly`) aunque el texto lo promete; tamaño no validado | `CreatePaymentScreen.kt:672-677`, `ImagePicker.android.kt:15` | ⬜ Pendiente | |
| E-P4 | DatePicker permite fechas futuras | `CreatePaymentScreen.kt:842-859` | ⬜ Pendiente | |
| E-P5 | "CANCELAR OPERACIÓN" solo vuelve al paso 1 | `CreatePaymentScreen.kt:565` | ⬜ Pendiente | |
| E-P6 | "Ver completo" y el recuadro del comprobante clicables sin comprobante | `PaymentDetailScreen.kt:217-224, 294` | ⬜ Pendiente | |

### Cartelera
| ID | Problema | Dónde | Estado | Ref |
|---|---|---|---|---|
| E-C1 | URL sin extensión se trata como imagen; sin estado de error de imagen; nombre de archivo crudo (`%20`) | `BillboardDetailScreen.kt:78, 522, 645-658` | ⬜ Pendiente | |
| E-C2 | En móvil no se muestra el conteo de "entendido" (`reactionsCount` sin usar) | `BillboardDetailScreen.kt:127-132, 669` | ⬜ Pendiente | |
| E-C3 | Detalle sin "Reintentar" en error; doble espacio final por `Spacer(80.dp)` | `BillboardDetailScreen.kt:207, 222-239` | ⬜ Pendiente | |
| E-C4 | Botón de reacción toggle sin estado accesible; métricas que son solo números | `BillboardDetailScreen.kt:707-771`, `AnnouncementComponents.kt:104-128` | ⬜ Pendiente | |

### Votaciones
| ID | Problema | Dónde | Estado | Ref |
|---|---|---|---|---|
| E-V1 | Voto irreversible confirmado con "Confirmar" genérico; opciones no seleccionables al tocar la fila | `DecisionSheetsStubs.kt:335-365, 412` | ⬜ Pendiente | |
| E-V2 | Sheet se puede cerrar mientras envía; sin errores inline | `DecisionSheetsStubs.kt:122-140, 171-191` | ⬜ Pendiente | |
| E-V3 | "Urgente" en una votación ya vencida; "Borradores" vs "Recepción" para el mismo estado | `DecisionsListScreen.kt:345-360`, `values/strings.xml:290, 296` | ⬜ Pendiente | |
| E-V4 | Lectura síncrona del archivo completo (5 MB) en el callback de UI | `DecisionSheetsStubs.kt:102` | ⬜ Pendiente | |

### Perfil
| ID | Problema | Dónde | Estado | Ref |
|---|---|---|---|---|
| E-R1 | Cerrar sesión sin confirmación ni loading (doble envío); más peso visual que "Eliminar cuenta" | `ProfileScreen.kt:286-298, 411-420` | ⬜ Pendiente | |
| E-R2 | Switches sin etiqueta (no toggleable); en web doble foco por ítem | `NotificationSettingsScreen.kt:155-224` | ⬜ Pendiente | |
| E-R3 | Selector de idioma sin `selectable`/RadioButton; ripple fuera de las esquinas | `ProfileScreen.kt:766-777` | ⬜ Pendiente | |
| E-R4 | Sin aviso de cambios sin guardar ni confirmación al guardar | `EditProfileScreen.kt:37-47`, `ChangePasswordScreen.kt:52-62` | ⬜ Pendiente | |

### Web (index.html / PWA)
| ID | Problema | Dónde | Estado | Ref |
|---|---|---|---|---|
| E-W1 | Falta `<meta name="color-scheme" content="light">`, `translate="no"` y `<noscript>`; `lang` fijo en `es` | `wasmJsMain/resources/index.html` | ⬜ Pendiente | |
| E-W2 | Ícono `"any maskable"` combinado; manifest sin `lang`, `id`, `description` | `manifest.webmanifest` | ⬜ Pendiente | |

## F. Barrido general (sin ID, aplicar al tocar cada pantalla)

- `semantics { heading() }` en títulos de página y de sección (falta en todas las pantallas).
- `Role.Button` / `onClickLabel` en filas y tarjetas clicables con `Modifier.clickable`.
- `maxLines` + `TextOverflow.Ellipsis` en nombres, descripciones y celdas de tabla.
- `key = { it.id }` en `items(...)` de listas.
- `Color(0x…)` / `Color.White` fuera de `theme/` → tokens.
- `fontSize` fijo en sp → `MaterialTheme.typography`.
- Área táctil mínima de 48dp en íconos y textos clicables.
- `pointerHoverIcon(Hand)` y estado de foco visible en web (no solo hover).

## Historial

| Fecha | Cambio |
|---|---|
| 2026-10-16 | Auditoría inicial (9 revisiones por pantalla, rama `poc/web-admin-theme`) |
| 2026-10-21 | Revisión contra el código actual (sin cambios desde la auditoría): bloque A confirmado en código → ⬜ Pendiente; corregidas las descripciones de A-10, B-01, B-02, B-09, C-02, C-07, C-09, D-06 y E-L3 |
| 2026-10-21 | Paso 0 (`worktree-agent-a8dd877d108cce00f`): B-01…B-10, C-02, C-03, C-04, C-06, D-01, D-02, D-03 y D-05 → ✅; C-07 → 🔄 (falta `CreatePaymentScreen.kt`). A-15 (nuevo, hallado probando web contra DEV) → ✅. Primary móvil #DB5A07 → #C45006; nuevos `AptoNavInactive`, `AptoOnWarningContainer`, `onStatusContainer()` y `rememberReducedMotion()` |
