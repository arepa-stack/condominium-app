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

| Bloque | Ítems | 🔍 | ⬜ | 🔄 | ✅ | ⏸️ |
|---|---|---|---|---|---|---|
| A. Errores de producto | 21 | 0 | 5 | 1 | 14 | 1 |
| B. Componentes compartidos | 10 | 0 | 0 | 0 | 10 | 0 |
| C. Textos e i18n | 9 | 0 | 0 | 4 | 5 | 0 |
| D. Contraste | 6 | 0 | 2 | 0 | 4 | 0 |
| E. Por pantalla (prioritarios) | 37 | 0 | 18 | 0 | 19 | 0 |

---

## A. Errores de producto (lógica / datos) — prioridad alta

| ID | Pantalla | Problema | Dónde | Estado | Ref |
|---|---|---|---|---|---|
| A-01 | Pagos | El monto con coma decimal ("150,50") da "Monto inválido": se parsea con `toDoubleOrNull()` | `CreatePaymentScreen.kt:774`, `CreatePaymentViewModel.kt:117` | ✅ Hecho | `6292019` — `parseAmount()` común (`KmpFormatters.kt`): coma o punto, con o sin miles |
| A-02 | Pagos (iOS) | `formatCurrency` en iOS devuelve separador de miles; al volver a parsearlo, montos ≥ 1000 dan "Monto inválido" | `CreatePaymentViewModel.kt:110`, `KmpFormatters.ios.kt:15` | ✅ Hecho | `6292019` — el campo se rellena con `formatAmountInput()` (nunca con el formato de pantalla); iOS `formatCurrency` = "1500.25" como Android/web |
| A-03 | Pagos | Marcar/desmarcar facturas sobrescribe sin aviso el monto que escribió el usuario | `CreatePaymentViewModel.kt:110` | ✅ Hecho | `6292019` — autollenado solo mientras el usuario no editó el monto (`amountEdited`); vaciarlo lo reactiva |
| A-04 | Votaciones | Presupuesto "1.500" (miles en es) se envía como 1.5 sin avisar; "1500,50" se rechaza | `DecisionSheetsStubs.kt:121` | ✅ Hecho | `worktree-agent-a692a3abb4259849f` — el monto del presupuesto usa `parseAmount` (vía `parseQuoteAmount`): "1.500" = 1500 y "1500,50" se acepta; error inline "Monto inválido" y proveedor < 2 caracteres |
| A-05 | Dashboard | Muestra "¡Estás al día!" cuando el saldo aún carga o falló (`totalDebt` por defecto 0.0) | `DashboardScreen.kt:219, 801`, `DashboardViewModel.kt:69-80` | ✅ Hecho | `72230f5` — `totalDebt: Double?` (null = desconocido); mientras carga, spinner; si falla, "No pudimos cargar tu saldo" + Reintentar (móvil y KPI web, que muestran "—"). "Al día" solo con saldo cargado ≤ 0 |
| A-06 | Dashboard | Doble carga al abrir (`init` + `ON_RESUME`) y `refresh()` no cancela el job anterior → datos viejos / de otra unidad | `DashboardViewModel.kt:29, 93-96`, `DashboardScreen.kt:97` | ✅ Hecho | `72230f5` — `onResume()` ignora el primer ON_RESUME (ya cargó `init`); cada carga cancela la anterior (`loadJob`, con sus peticiones hijas); al cambiar de unidad se descartan saldo/pagos/caja chica de la anterior |
| A-07 | Varias | Errores de red mostrados como vacío o pantalla en blanco, sin reintentar: historial de pagos, crear pago ("No tienes recibos pendientes"), detalle de pago, detalle de factura ("No se encontraron facturas"), dashboard, selector de propiedad | `PaymentHistoryViewModel.kt:53`, `CreatePaymentViewModel.kt:56`, `PaymentDetailScreen.kt:241`, `InvoiceDetailScreen.kt:163-184`, `DashboardScreen.kt:108`, `UnitSelectionViewModel.kt:49` | 🔄 En curso | `6292019` — pagos hecho (historial, crear pago, detalle) con el nuevo `ErrorState` (`EmptyState.kt`) y `fetchPayments`/`fetchPayment` (`Result`); `72230f5` — dashboard hecho (usuario/unidad: `ErrorState` a pantalla completa; transacciones: `ErrorState` en lugar de la lista; saldo: ver A-05); `3ee3188` — detalle de factura hecho (factura: `ErrorState` a pantalla completa / `WebEmptyFrame`; pagos de la factura: `ErrorState` en la sección, con reintento propio `loadPayments`; carga separada de factura y pagos, que antes podía mostrar "No se encontraron facturas" mientras la factura aún cargaba) y lista de facturas (`ErrorState` + Reintentar en lugar del mensaje crudo de la excepción). Falta selector de propiedad |
| A-08 | Facturas | El error de la lista nunca se limpia: tras un refresh exitoso sigue mostrándose | `InvoiceListViewModel.kt:29, 37` | ✅ Hecho | `3ee3188` — `loadInvoices()` limpia `error` al empezar; el fallo muestra `ErrorState` + Reintentar (A-07) |
| A-09 | Perfil | Preferencias de notificación solo en memoria: no se guardan ni se envían a la API | `NotificationSettingsScreen.kt:31-33` | ⬜ Pendiente | Confirmado en código (2026-10-21) |
| A-10 | Perfil | Editar perfil: la unidad es editable pero no se envía (solo `name`); además bloquea guardar si no hay unidad | `EditProfileScreen.kt:85-90`, `EditProfileViewModel.kt:53-75`, `RemoteAuthRepository.kt:204-210` | ⬜ Pendiente | Confirmado en código (2026-10-21) |
| A-11 | Perfil | Cambiar contraseña pide la actual pero el backend no la verifica (solo envía `newPassword`) | `ChangePasswordScreen.kt:232`, `RemoteAuthRepository.kt:226-228` | ⬜ Pendiente | Confirmado en código (2026-10-21) |
| A-12 | Pagos / Facturas | Botones que no hacen nada: "Descargar recibo" (solo `println`), "Descargar factura" (TODO), "Contactar soporte" (onClick vacío) | `PaymentDetailScreen.kt:71-75, 117, 354`, `CondominioNavGraph.kt:351` | ⏸️ Descartado | Falta backend: no hay endpoint de descarga de recibo/factura ni de soporte (verificado en `condominium-api`, 2026-10-21). Factura re-verificada en el paso 3 (2026-10-24): `billing/presentation/routes.ts` solo tiene `POST /invoices/preview-pdf` (importación admin), no descarga; UI sin tocar |
| A-13 | Votaciones | Porcentajes sin formato ("33.333333333333336%"); empates reciben #1/#2 y "Liderando" pese al aviso "Empate técnico" | `DecisionDetailScreen.kt:1150, 170-175, 1082-1091` | ✅ Hecho | `worktree-agent-a692a3abb4259849f` — porcentajes enteros (`formatPercent`); empates comparten posición (`rankTallies`) y "Liderando" solo con líder único (`leaderQuoteId`); `DecisionTally.kt` |
| A-14 | Primer login | Sin salida: no se puede cerrar sesión ni volver si el usuario no cambia la contraseña | `FirstLoginChangePasswordScreen.kt:48-86` | ⬜ Pendiente | Confirmado en código (2026-10-21) |
| A-15 | Votaciones | Detalle de votación muestra error de JSON si un presupuesto no tiene archivo (`file_url` null) | `DecisionModels.kt:61` | ✅ Hecho | `1893483` — `fileUrl: String?`; sin archivo se oculta "Ver archivo" |
| A-16 | Pagos (web) | Calendario: encabezado y mes muestran un día/mes de menos (formato UTC en zona local) | `CreatePaymentScreen.kt:1021, 1210` | ✅ Hecho | `6292019` — Material3 1.9 wasm formatea con `Date` JS en zona local; en web se pasa un `DatePickerFormatter` en UTC (kotlinx-datetime + `month_*`); encabezado `dd/MM/yyyy` (con el mes largo no cabía el año), forma larga solo para accesibilidad |
| A-17 | Login (arranque) | Sin red al abrir la app se muestra el login aunque el token sigue guardado: `fetchCurrentUser` falla y no se distingue "sin conexión" de "sesión inválida" (el token solo se borra con 401). Parece un deslogueo y no ofrece reintentar. Además, al arrancar se llama 3 veces a `users/me` | `LoginViewModel.kt:25-48`, `RemoteAuthRepository.kt:157` | ⬜ Pendiente | Hallado en Android (2026-10-22); corregir en el paso 6 |
| A-18 | Varias (web) | Sin red, en wasm el fallo de `fetch` llega como `kotlin.Error: Fail to fetch` (no es `Exception`): los `catch (e: Exception)` de `safeRequest` y de los repositorios no lo atrapan, la corrutina muere (error no capturado en consola) y la pantalla queda en spinner infinito; ningún `ErrorState` aparece en web | `ApiServiceImpl.kt:35`, `RemotePaymentRepository.kt:225` y demás `catch` de `data/repository/` | ✅ Hecho | `3ee3188` — hallado por el tester en web (2026-10-24). `catch (e: Throwable)` + `e.rethrowIfCancellation()` (nuevo `data/remote/NetworkErrors.kt`; relanza `CancellationException`) en `safeRequest` y en los 36 `catch` de red de los repositorios. Los `catch` que solo parsean JSON/enums quedan como estaban |
| A-19 | Pagos (Android) | El historial de pagos carga dos veces al abrirlo (2× `users/me` + 2× `payments?unit_id=…` a la vez): `init` del ViewModel y el primer `ON_RESUME` de la pantalla; además las cargas no se cancelan entre sí | `PaymentHistoryViewModel.kt:24`, `PaymentHistoryScreen.kt:75` | ✅ Hecho | `3ee3188` — hallado por el tester (2026-10-25). Un solo `loadJob` cancelable (como A-06). Sin ventanas de tiempo: `OnScreenReturn` (`ui/utils/ScreenReturn.kt`) avisa (a) al (re)entrar en composición —volver del detalle, fiable en web; pero también la rotación en Android recrea la composición, así que el ViewModel solo recarga si antes la pantalla avisó `onOpenChild()` al navegar hacia adelante (rotación/apertura ⇒ 0 recargas)— y (b) en ON_START solo si el mismo observer vio antes un ON_STOP (`BackgroundReturnDetector`; `onBackgroundReturn()`, recarga siempre; la rotación crea un observer nuevo, no cuenta; un ON_RESUME/ON_START tardío de wasm sin ON_STOP se ignora). Si ya hay una carga en curso, la señal se ignora (esa carga es igual de reciente); `loadPayments()`/`refresh()` explícitos siempre cargan. Se eliminó `ReloadGate`. Mismo mecanismo en el Dashboard. Tests: `ui/ScreenReturnTest.kt`. Manual: ciclo de vida real de NavHost/wasm |
| A-20 | Varias (web) | `safeRequest` hacía `printStackTrace()` en cada fallo: sin red la consola web se llena de trazas `kotlin.Error: Fail to fetch` | `ApiServiceImpl.kt:37` | ✅ Hecho | `3ee3188` — hallado por el tester (2026-10-25). Quitado (no hay logger de debug en el proyecto); el error sigue llegando a la pantalla vía `Response.error` |
| A-21 | Facturas (Android) | Detalle de factura: los botones inferiores "Ver todos los pagos" / "Descargar factura" se parten en dos líneas en Compact | `InvoiceDetailScreen.kt:87-139` | ✅ Hecho | `3ee3188` — hallado por el tester (2026-10-25). Lado a lado si ambas etiquetas caben en una línea (medidas con `rememberTextMeasurer`); si no, apilados a ancho completo. Etiquetas `maxLines = 1` + elipsis como red de seguridad. Botones Material (touch target ≥ 48dp) |

## B. Componentes compartidos — un arreglo corrige todas las pantallas

| ID | Componente | Problema | Dónde | Estado | Ref |
|---|---|---|---|---|---|
| B-01 | `TopBarWithBack` | Botón atrás sin nombre accesible (`Res.string.back` ya existe); título con `maxLines = 1` pero sin `overflow = Ellipsis` ni `heading()` | `TopBarWithBack.kt:49, 79, 55-72` | ✅ Hecho | `1893483` |
| B-02 | `LabeledOutlinedField` | La etiqueta es un `Text` aparte, no asociado al campo; no expone `contentType` (autofill). `isError` y `supportingText` ya existen, pero las pantallas no los usan (ver E-L4, E-P1) | `LabeledOutlinedField.kt:47-71` | ✅ Hecho | `1893483` — `contentType` disponible; aplicarlo en las pantallas es E-L1 |
| B-03 | `AppSidebar` | Ítems sin `selectable(role = Role.Tab)` ni `selectableGroup()`: no se anuncia la sección activa | `AppShell.kt:61, 89` | ✅ Hecho | `1893483` |
| B-04 | `AppBottomBar` / `AppNavRail` | Sin `Role.Tab`/selected; `contentDescription = label` duplica el texto; área táctil ~38dp; `fontSize` 10/11sp fijo | `AppBottomBar.kt:135-154, 188-189` | ✅ Hecho | `1893483` |
| B-05 | `StatusBadge` / `AdminWebStyle` | Texto del color de estado sobre el mismo color al 10–20%: contraste 2–3.8:1. Falta un token `on*Container` por estado | `StatusBadge.kt:35`, `AdminWebStyle.kt:30-31` | ✅ Hecho | `1893483` — `onStatusContainer()` + `AptoOnWarningContainer` (`Color.kt`) |
| B-06 | `LoadingState` / `CondoButtons` | Spinners sin semántica: no se anuncia la carga y el botón queda sin nombre | `LoadingState.kt:29`, `CondoButtons.kt:41-42` | ✅ Hecho | `1893483` |
| B-07 | `Type.kt` | Ningún estilo con `fontFeatureSettings = "tnum"`: montos y tablas desalineados | `Type.kt:21-136` | ✅ Hecho | `1893483` |
| B-08 | `FilterPillChip` | Sin `Role.Tab`/selected, ~36dp de alto, selección solo por color | `AnnouncementComponents.kt:266-278` | ✅ Hecho | `1893483` |
| B-09 | `NavGraph` | Transición fade de 700 ms (la de por defecto de `NavHost`: no hay `enterTransition`/`exitTransition` propios), no respeta "reducir movimiento" | `CondominioNavGraph.kt:192-203` | ✅ Hecho | `1893483` — fade 200 ms; sin transición con reducir movimiento (`ui/utils/ReducedMotion.kt`) |
| B-10 | `ShimmerEffect` / animaciones infinitas | No respetan reduced motion (shimmer, escáner QR, barra del splash) | `ShimmerEffect.kt:19-27`, `QrScannerScreen.kt:151`, `SplashScreen.kt:117` | ✅ Hecho | `1893483` |

## C. Textos e i18n

| ID | Problema | Dónde | Estado | Ref |
|---|---|---|---|---|
| C-01 | ~60 textos visibles hardcodeados en Kotlin (muchos ya existen en `strings.xml`): sobre todo Pagos (~30), Cartelera, splash/marca ("Tu condominio, en orden.", "VIVIENDA ELEVADA", "Apto by Nibs") | `PaymentHistoryScreen.kt`, `PaymentDetailScreen.kt`, `CreatePaymentScreen.kt`, `BillboardDetailScreen.kt`, `AnnouncementComponents.kt:41-47, 176`, `AppShell.kt:74-133`, `SplashScreen.kt` | 🔄 En curso | `6292019` — Pagos hecho (crear, historial, detalle; también el nombre del método, antes `PaymentMethod.label` solo en español); `72230f5` — dashboard hecho ("Cartelera", E-D1); falta el resto |
| C-02 | Claves faltantes en `values-en`: `download_invoice`, `no_invoices_for_filter`, `billboard_detail_title`, `decisions_see_detail`, `decisions_urgent`, `decisions_finalized_on` (y `settings`, `share`, sin uso). A la inversa, `values-en` tiene 36 claves que no existen en `values` (caja chica: `cat_*`, `category_*`, `register_expense`…), todas sin uso: borrar | `values-en/strings.xml` | ✅ Hecho | `1893483` — 36 claves en-only borradas (sin uso verificado); 8 traducciones añadidas |
| C-03 | Title Case en español ("Deuda Total", "Iniciar Sesión", "Cerrar Sesión", "Historial de Pagos"…): pasar a sentence case | `values/strings.xml` | ✅ Hecho | `1893483` |
| C-04 | Mayúsculas dentro del texto ("GANADOR", "CORREO ELECTRÓNICO", estados): aplicar con estilo, no en el recurso | `values/strings.xml`, `RegisterScreen.kt` | ✅ Hecho | `1893483` |
| C-05 | Tono mezclado tú / usted / vos ("Seleccione", "acérquese", "Debés") | `values/strings.xml:144`, `CreatePaymentScreen.kt:500` | 🔄 En curso | `6292019` — Pagos hecho ("Adjunta el comprobante", "acércate", "Nombre del banco"); falta revisar el resto de `strings.xml` (p. ej. `select_unit_placeholder` "Seleccione unidad") |
| C-06 | "..." en vez de "…" (`loading`, `qr_scan_scanning`) y falta de ortografía "Registrate" | `values*/strings.xml:24, 79` | ✅ Hecho | `1893483` |
| C-07 | Plurales armados a mano ("1 presupuestos", "entendido(s)", "recibo(s) seleccionado(s)"): usar `<plurals>` | `values*/strings.xml`, `CreatePaymentScreen.kt:357, 986` | 🔄 En curso | `1893483` — `<plurals>` de `strings.xml` hechos; `6292019` — `CreatePaymentScreen.kt` usa `invoices_selected_count` (`<plurals>`). Falta `units_count_label` ("%1$d unidades", en uso: "1 unidades") |
| C-08 | Fechas: formatos distintos entre pantallas, ISO crudo en cartelera, meses en español también en inglés | `InvoiceListScreen.kt:482`, `InvoiceDetailScreen.kt:749`, `BillboardDetailScreen.kt:313`, `PaymentHistoryScreen.kt:258`, `DecisionComponents.kt:87` | 🔄 En curso | `6292019` — Pagos hecho: `dd/MM/yyyy` en historial, detalle y crear pago (como el detalle de factura); meses de facturas por recurso `month_*`; `72230f5` — dashboard `dd/MM/yyyy` (`formatPaymentDate`) en móvil y tabla web; `3ee3188` — Facturación hecha: vencimiento de la tabla web y fechas del detalle con `formatPaymentDate`; periodo "2025-03" → "Marzo 2025"/"March 2025" con `invoiceMonthLabel` (ahora `internal`, de crear pago; ya no antepone "Enero" a periodos que no son `YYYY-MM`) en lista y detalle; falta cartelera y votaciones |; `worktree-agent-a692a3abb4259849f` — votaciones hecho: plazos `dd/MM/yyyy HH:mm` (`formatDecisionDeadline` ahora con año); falta cartelera |
| C-09 | `$` hardcodeado en lugar de `currency_amount` (puede contradecir la moneda mostrada) | `DashboardScreen.kt:597`, `InvoiceDetailScreen.kt:353`, `DecisionDetailScreen.kt:848, 1345`, `DecisionSheetsStubs.kt:373`, `CreatePaymentScreen.kt:306, 351, 418` | ✅ Hecho | `6292019` — Pagos hecho (crear, historial, detalle); `72230f5` — dashboard hecho; `3ee3188` — factura hecha (montos del resumen y de la tabla web de pagos; "Aplicado"/"Monto total" de los pagos, que salían sin símbolo, ahora con `currency_amount`); falta votaciones |; `worktree-agent-a692a3abb4259849f` — votaciones hecho (presupuestos, ganador y hoja de voto con `currency_amount`); ya no quedan `$` hardcodeados |

## D. Contraste (WCAG AA: 4.5:1 texto normal, 3:1 texto grande / íconos)

| ID | Par | Contraste | Dónde | Estado | Ref |
|---|---|---|---|---|---|
| D-01 | Badges de estado (web: todos; móvil: aviso y pendiente) | 1.99–3.82:1 | `StatusBadge.kt`, `AdminWebStyle.kt` (ver B-05) | ✅ Hecho | `1893483` — ver B-05; móvil ≥ 4.9:1, web ≥ 4.8:1 |
| D-02 | Pestaña inactiva barra inferior/rail (#9CA3AF) | 2.39–2.54:1 | `AppBottomBar.kt:113, 174` | ✅ Hecho | `1893483` — `AptoNavInactive` #6B7280: 4.83:1 (blanco), 4.55:1 (rail web) |
| D-03 | Pestaña activa móvil (#FF6B00 sobre blanco) | 2.86:1 | `AppBottomBar.kt:112` | ✅ Hecho | `1893483` — activa = `colorScheme.primary`: #C45006 4.67:1 (móvil), #B45309 4.73:1 (rail web) |
| D-04 | Texto blanco del panel de marca del login sobre el degradado | 1.79–3.19:1 | `AppShell.kt:124-135` | ⬜ Pendiente | |
| D-05 | Blanco sobre el primary móvil (#DB5A07) | 3.83:1 | `Color.kt:28` | ✅ Hecho | `1893483` — #DB5A07 → #C45006: 4.67:1 |
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
| E-D1 | "Cartelera" hardcodeado en móvil | `DashboardScreen.kt:423` | ✅ Hecho | `72230f5` — `billboard_title` |
| E-D2 | Avatar se anuncia solo con la inicial; acciones rápidas se leen dos veces | `DashboardScreen.kt:362, 774, 522` | ✅ Hecho | `72230f5` — avatar `Role.Button` + "Perfil" (`clearAndSetSemantics`, sin la inicial); ícono de acción rápida sin `contentDescription` y la acción con `Role.Button` |
| E-D3 | Dos "Ver todo" con destinos distintos, ~16dp de alto, sin rol | `DashboardScreen.kt:171-176, 243-251, 980` | ✅ Hecho | `72230f5` — el de facturas ahora dice "Ver facturas" (`see_invoices`); el de transacciones sigue "Ver todo" con `onClickLabel` "Historial"; ambos `Role.Button` y `minimumInteractiveComponentSize()` (48dp), también en web (`WebLink`) |
| E-D4 | Sin estado vacío de transacciones en móvil; badges de 9sp ilegibles | `DashboardScreen.kt:181, 587, 635` | ✅ Hecho | `72230f5` — vacío móvil con `web_dashboard_no_transactions` (o `ErrorState` si falló); badge y "Procesado por" con `labelSmall` (sin 9sp fijo); `key = { it.id }` |
| E-D5 | Nombre/unidad/descripciones sin `maxLines` ni `weight` | `DashboardScreen.kt:280-293, 333-345, 755` | ✅ Hecho | `72230f5` — `maxLines = 1` + `Ellipsis` y `weight` en nombre, unidad y descripción de factura; píldora de unidad web con ancho máx. 360dp. Web: tabla de transacciones con fecha/monto/estado al ancho de su celda más ancha (medido, sin saltos) y la descripción absorbe el resto; KPI apiladas si no caben (< 640dp) y etiqueta en máx. 2 líneas |

### Seleccionar propiedad
| ID | Problema | Dónde | Estado | Ref |
|---|---|---|---|---|
| E-U1 | Sin estado vacío (usuario sin edificios) | `UnitSelectionScreen.kt:98-121, 357-396` | ⬜ Pendiente | |
| E-U2 | El ícono "colapsar" no colapsa (`expandedBuildingId` nunca vuelve a null) | `UnitSelectionScreen.kt:240`, `UnitSelectionViewModel.kt:60` | ⬜ Pendiente | |
| E-U3 | Expandido/seleccionado no expuesto a accesibilidad | `UnitSelectionScreen.kt:166-169, 238-247, 477-498` | ⬜ Pendiente | |

### Facturación
| ID | Problema | Dónde | Estado | Ref |
|---|---|---|---|---|
| E-F1 | Botón "Pagar" anidado dentro de una card clicable | `InvoiceListScreen.kt:207, 295` | ✅ Hecho | `3ee3188` — tarjeta móvil: solo el área de datos es clicable (`Role.Button`, `onClickLabel` "Detalle de factura") y "Pagar" es hermano, no hijo (mismo aspecto que `ListItemCard`); fila de la tabla web igual: celdas clicables + botón aparte |
| E-F2 | Tabla web no lazy (`Column + forEach`) | `InvoiceListScreen.kt:390-407` | ✅ Hecho | `3ee3188` — `LazyColumn` con `key = id` dentro de la `WebCard` (cabecera fija, la card crece hasta el fondo y las filas hacen scroll dentro); grid móvil con `key = id` |
| E-F3 | Montos: columna "Pagado" centrada, sin `maxLines` (se parten) | `InvoiceListScreen.kt:248, 487-505` | ✅ Hecho | `3ee3188` — móvil: columnas al ancho de su contenido (`SpaceBetween`), "Pagado" alineado a la izquierda, montos y etiquetas `maxLines = 1`/`softWrap = false`; web: columnas fijas medidas con `rememberTextMeasurer` como el Dashboard (vencimiento, total, restante, estado, acción), celdas sin salto de línea, concepto/periodo con elipsis |
| E-F4 | Filtro con `remember` (se pierde al volver del detalle) | `InvoiceListScreen.kt:64` | ✅ Hecho | `3ee3188` — `selectedTab` en `InvoiceListUiState` (`onTabSelected`); el ViewModel sigue vivo en el back stack |

### Pagos
| ID | Problema | Dónde | Estado | Ref |
|---|---|---|---|---|
| E-P1 | Validación con un único error al pie, sin inline ni foco; el error no se limpia al editar | `CreatePaymentScreen.kt:523-529`, `CreatePaymentViewModel.kt:63-93` | ✅ Hecho | `6292019` — error por campo (`isError` + `supportingText`), foco al primer campo inválido, se limpia al editar; errores de formulario con `liveRegion` |
| E-P2 | Checkbox de facturas sin semántica (toggleable / Role.Checkbox) | `CreatePaymentScreen.kt:259-284` | ✅ Hecho | `6292019` — `Surface(checked, onCheckedChange)` + `Role.Checkbox` |
| E-P3 | Android no acepta PDF (`PickVisualMedia.ImageOnly`) aunque el texto lo promete; tamaño no validado | `CreatePaymentScreen.kt:672-677`, `ImagePicker.android.kt:15` | ✅ Hecho | `6292019` — `rememberDocumentPickerLauncher` (imagen o PDF, también en web) con el mismo límite de 5 MB que votaciones; el multipart envía el MIME real; el detalle abre los PDF con el visor externo |
| E-P4 | DatePicker permite fechas futuras | `CreatePaymentScreen.kt:842-859` | ✅ Hecho | `6292019` — `SelectableDates` hasta hoy |
| E-P5 | "CANCELAR OPERACIÓN" solo vuelve al paso 1 | `CreatePaymentScreen.kt:565` | ✅ Hecho | `6292019` — sale del flujo (volver al paso 1 sigue en la flecha atrás) |
| E-P6 | "Ver completo" y el recuadro del comprobante clicables sin comprobante | `PaymentDetailScreen.kt:217-224, 294` | ✅ Hecho | `6292019` — solo clicables (y "Ver completo" visible) con comprobante |

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
| E-V1 | Voto irreversible confirmado con "Confirmar" genérico; opciones no seleccionables al tocar la fila | `DecisionSheetsStubs.kt:335-365, 412` | ✅ Hecho | `worktree-agent-a692a3abb4259849f` — fila completa seleccionable (`selectable`, rol RadioButton); botón "Confirmar voto por <proveedor>" |
| E-V2 | Sheet se puede cerrar mientras envía; sin errores inline | `DecisionSheetsStubs.kt:122-140, 171-191` | ✅ Hecho | `worktree-agent-a692a3abb4259849f` — no se cierra (X, Cancelar, atrás, fuera, gesto) mientras envía; el VM ignora cierre y doble envío; errores inline por campo; "No hay unidad activa" como recurso |
| E-V3 | "Urgente" en una votación ya vencida; "Borradores" vs "Recepción" para el mismo estado | `DecisionsListScreen.kt:345-360`, `values/strings.xml:290, 296` | ✅ Hecho | `worktree-agent-a692a3abb4259849f` — votación vencida muestra "Plazo vencido" en vez de "Urgente"; filtro es "Borradores" → "Recepción" (igual que el estado) |
| E-V4 | Lectura síncrona del archivo completo (5 MB) en el callback de UI | `DecisionSheetsStubs.kt:102` | ✅ Hecho | `worktree-agent-a692a3abb4259849f` — el archivo se lee en `Dispatchers.Default` (no en el callback de UI) y el envío espera a que termine la lectura |

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
| 2026-10-21 | Paso 0 (`1893483`): B-01…B-10, C-02, C-03, C-04, C-06, D-01, D-02, D-03 y D-05 → ✅; C-07 → 🔄 (falta `CreatePaymentScreen.kt`). A-15 (nuevo, hallado probando web contra DEV) → ✅. Primary móvil #DB5A07 → #C45006; nuevos `AptoNavInactive`, `AptoOnWarningContainer`, `onStatusContainer()` y `rememberReducedMotion()` |
| 2026-10-21 | Paso 1, Pagos (`6292019`): A-01, A-02, A-03 y E-P1…E-P6 → ✅; A-07 → 🔄 (pagos hecho); C-01, C-05, C-08 y C-09 → 🔄 (pagos hecho); C-07 sigue 🔄 (falta `units_count_label`); A-12 → ⏸️ (falta backend). Nuevos `parseAmount()`/`formatAmountInput()` (`KmpFormatters.kt`), `ErrorState` (`EmptyState.kt`) y `PaymentRepository.fetchPayments`/`fetchPayment`. Resumen con columna ⏸️. A-16 (nuevo, hallado probando web contra DEV en UTC−4) → ✅ |
| 2026-10-21 | Paso 2, Dashboard (`72230f5`): A-05, A-06 y E-D1…E-D5 → ✅; A-07 sigue 🔄 (dashboard hecho; faltan detalle de factura y selector de propiedad); C-01, C-08 y C-09 siguen 🔄 (dashboard hecho). Nuevas claves `error_loading_dashboard`, `error_loading_balance`, `see_invoices` |
| 2026-10-22 | A-17 (nuevo, hallado probando en Android): sin red el arranque muestra el login aunque el token sigue guardado → ⬜, para el paso 6 |
| 2026-10-24 | Paso 3, Facturación (`3ee3188`): A-08 y E-F1…E-F4 → ✅; A-07 sigue 🔄 (detalle y lista de factura hechos; falta selector de propiedad); C-08 y C-09 siguen 🔄 (Facturación hecha); A-12 sigue ⏸️ (re-verificado: no hay endpoint de PDF de factura). `invoiceMonthLabel` pasa a `internal`. Nuevas claves `error_loading_invoice_list`, `error_loading_invoice`, `error_loading_invoice_payments` |
| 2026-10-24 | A-18 (nuevo, hallado por el tester en web sin red: `kotlin.Error: Fail to fetch` no era atrapado) → ✅ (`3ee3188`). Nuevo `Throwable.rethrowIfCancellation()` (`data/remote/NetworkErrors.kt`) |
| 2026-10-25 | A-19 (doble carga del historial de pagos en Android), A-20 (`printStackTrace` en `safeRequest`) y A-21 (botones del detalle de factura partidos en Android), hallados por el tester → ✅ (`3ee3188`) |
| 2026-10-25 | A-19 corregido de nuevo (regresión en web: no recargaba al volver del detalle): `OnScreenReturn` + `BackgroundReturnDetector` (`ui/utils/ScreenReturn.kt`, sin ventanas de tiempo), también en el Dashboard (reemplaza «saltar el primer resume» de A-06) |
| 2026-10-25 | A-19 ajuste: girar el teléfono ya no recarga Historial ni Inicio; la pantalla avisa `onOpenChild()` al navegar hacia adelante y solo entonces la re-entrada recarga (`OnScreenReturn(onReenter, onBackgroundReturn)`; volver de segundo plano igual) |
| 2026-10-25 | Paso 4, Votaciones (`worktree-agent-a692a3abb4259849f`): A-04, A-13, C-09 y E-V1…E-V4 → ✅; C-08 sigue 🔄 (votaciones hecho; falta cartelera). Nuevas claves `decisions_vote_confirm_for`, `decisions_upload_provider_invalid`, `decisions_upload_amount_invalid`; `decisions_filter_reception` = "Recepción" |
