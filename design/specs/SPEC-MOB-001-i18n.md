# 📝 Especificación Técnica: Sistema de Internacionalización (i18n) - KMP Optimized

**ID:** SPEC-MOB-001  
**Estado:** Aprobado para Implementación  
**Versión:** 1.1.0 (Corregida para KMP)  
**Fecha:** 13/04/2026  
**Responsable:** Líder Técnico de Condominio

---

## 1. Definición del Problema
Actualmente, la aplicación móvil utiliza textos estáticos ("hardcoded") en español dentro de los componentes de Jetpack Compose en el módulo `shared`. Esto impide la escalabilidad, dificulta la mantenibilidad y rompe la arquitectura limpia (Clean Architecture) al mezclar lógica de presentación con literales de texto.

## 2. Requisitos Funcionales
* **Soporte Multi-idioma:** Soporte inicial para Español (ES) e Inglés (EN).
* **Detección Automática:** Carga automática del idioma basada en el Locale del sistema operativo.
* **Consistencia de Dominio:** Uso de un glosario técnico estandarizado para términos financieros (Caja Chica, Solvencia, etc.).

## 3. Especificaciones Técnicas

### 3.1 Arquitectura de Recursos (Multiplatform)
Para permitir que las pantallas compartidas en `commonMain` accedan a los recursos de forma eficiente en todas las plataformas, se utilizará **Compose Multiplatform Resources**.

| Recurso | Localización | Propósito |
| :--- | :--- | :--- |
| `strings.xml` | `shared/src/commonMain/composeResources/values/` | Recursos por defecto (Español). |
| `strings.xml` (EN) | `shared/src/commonMain/composeResources/values-en/` | Traducciones al Inglés. |
| `UiText.kt` | `shared/src/commonMain/kotlin/.../ui/utils/` | Clase sellada para emitir strings desde ViewModels. |

### 3.2 Glosario Estandarizado (SDD Contract)
Claves obligatorias para paridad con la API:

* **`dashboard_balance`**: "Saldo Total" / "Total Balance".
* **`status_solvent`**: "Solvente" / "Solvent".
* **`status_overdue`**: "Moroso" / "Overdue".
* **`petty_cash`**: "Caja Chica" / "Petty Cash".

### 3.3 Protocolo de Implementación
Toda cadena de texto debe consumirse a través de los recursos generados por el plugin de Compose o mediante la clase `UiText`.

```kotlin
// Uso directo en UI
Text(
    text = stringResource(Res.string.dashboard_balance), 
    style = MaterialTheme.typography.bodyMedium
)

// Uso en ViewModel vía UiText
_uiState.update { it.copy(error = UiText.StringResource(Res.string.error_missing_fields)) }
```

## 4. Plano de Verificación
* **Cambio de Locale Dinámico:** Validar que al cambiar el idioma del sistema, la app actualice los textos sin reinicio (usando la reactividad de `stringResource`).
* **Formato de Moneda y Fechas:** Verificar que `KmpFormatters` utilice el `Locale` del sistema para separadores decimales y nombres de meses.
* **Fallback:** Asegurar que si una clave falta en EN, se muestre por defecto el valor en ES.

## 5. Criterios de Aceptación (DoD)
1. [ ] Eliminación del 100% de literales en **todas** las pantallas dentro de `shared/src/commonMain/kotlin/com/example/condominio/ui/screens/`.
2. [ ] Todos los ViewModels refactorizados para usar `UiText` en estados de error y mensajes dinámicos.
3. [ ] Implementación de `KmpFormatters` sensible al Locale en Android e iOS (Completado).
4. [ ] Mapeo de errores de la API a códigos de traducción locales.

## 6. Inventario de Pantallas Pendientes
| Módulo | Pantallas | ViewModels |
| :--- | :--- | :--- |
| **Auth/Register** | `RegisterScreen`, `PendingApprovalScreen` | `RegisterViewModel` |
| **Billing** | `InvoiceListScreen`, `InvoiceDetailScreen` | `InvoiceListViewModel`, `InvoiceDetailViewModel` |
| **Payment** | `CreatePaymentScreen`, `PaymentDetailScreen` | `CreatePaymentViewModel`, `PaymentDetailViewModel` |
| **Petty Cash** | `PettyCashScreen` | `PettyCashViewModel` |
| **Profile** | `ProfileScreen`, `EditProfileScreen`, `ChangePasswordScreen`, `NotificationSettingsScreen` | `ProfileViewModel`, `EditProfileViewModel`, `ChangePasswordViewModel` |
| **Common** | `UnitSelectionScreen` | `UnitSelectionViewModel` |
