# Condominio App — Documentación Completa del Proyecto

> **Propósito de este documento**: Fuente de conocimiento unificada para importar en NotebookLM. Contiene toda la información técnica, arquitectural, de negocio y de API del proyecto Condominio.

---

## 1. Visión General del Proyecto

**Condominio** es una aplicación móvil multiplataforma (Android e iOS) diseñada para la gestión de pagos y administración de condominios residenciales. Permite a residentes reportar pagos, consultar su solvencia y ver facturas, mientras que los administradores y miembros de junta directiva pueden gestionar edificios, aprobar pagos y manejar caja chica.

### Problema que Resuelve

La gestión de condominios tradicionalmente se maneja con hojas de cálculo, grupos de WhatsApp y recibos físicos. Esto genera:
- Falta de transparencia en el estado de cuenta
- Dificultad para verificar pagos
- Pérdida de comprobantes
- Conflictos entre residentes y administración

Condominio digitaliza todo este flujo: el residente reporta su pago con comprobante, la administración lo verifica, y ambas partes tienen visibilidad en tiempo real del estado de cuenta.

---

## 2. Stack Tecnológico

### Plataforma
- **Kotlin Multiplatform (KMP)**: Código compartido entre Android e iOS
- **Jetpack Compose Multiplatform**: UI declarativa compartida
- **Material 3**: Sistema de diseño de Google

### Dependencias Principales

| Categoría | Tecnología | Propósito |
|-----------|-----------|-----------|
| UI | Jetpack Compose + Material 3 | Framework de UI declarativo |
| Navegación | Jetpack Navigation Compose | Navegación entre pantallas |
| Red | Ktor Client | Cliente HTTP multiplataforma |
| Serialización | kotlinx.serialization | Serialización/deserialización JSON |
| Inyección de Dependencias | Koin | DI ligero y multiplataforma |
| Base de Datos Local | Room (Android) | Cache y persistencia offline |
| Almacenamiento | DataStore Preferences | Tokens y preferencias |
| Imágenes | Coil Compose + Coil Network Ktor | Carga y cache de imágenes |
| Fechas | kotlinx-datetime | Manejo de fechas multiplataforma |

### Configuración de Build

- **Namespace**: `com.example.condominio`
- **Compile SDK**: 36
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36
- **JVM Target**: Java 17
- **Kotlin**: Con KSP (Kotlin Symbol Processing) para Room

### Targets

- **Android**: Compilado con Android Gradle Plugin
- **iOS**: Framework estático (`ComposeApp`) para iosX64, iosArm64, iosSimulatorArm64

---

## 3. Arquitectura

### Patrón Arquitectural: MVVM + Clean Architecture

```
┌─────────────────────────────────────────────────────┐
│                    UI Layer                          │
│  ┌──────────┐  ┌────────────┐  ┌──────────────────┐ │
│  │  Screen   │──│  ViewModel │──│  UI State (Flow) │ │
│  │ (Compose) │  │  (Koin DI) │  │   (StateFlow)    │ │
│  └──────────┘  └────────────┘  └──────────────────┘ │
├─────────────────────────────────────────────────────┤
│                   Data Layer                         │
│  ┌──────────────┐  ┌──────────────┐                 │
│  │  Repository   │──│  ApiService   │ ←── Ktor HTTP  │
│  │  (Interface)  │  │ (Interface)   │                │
│  └──────┬───────┘  └──────────────┘                 │
│         │                                            │
│  ┌──────┴───────┐  ┌──────────────┐                 │
│  │  Repository   │──│ Room Database │ ←── SQLite     │
│  │  (Impl)       │  │ + DAOs        │                │
│  └──────────────┘  └──────────────┘                 │
├─────────────────────────────────────────────────────┤
│                  Platform Layer                      │
│  ┌──────────┐  ┌──────────┐  ┌───────────────────┐  │
│  │ Android  │  │   iOS    │  │  Token Manager    │  │
│  │ (OkHttp) │  │ (Darwin) │  │  (DataStore)      │  │
│  └──────────┘  └──────────┘  └───────────────────┘  │
└─────────────────────────────────────────────────────┘
```

### Estructura de Directorios del Código Fuente

```
app/src/
├── commonMain/kotlin/com/example/condominio/
│   ├── App.kt                           # Entry point compartido
│   ├── data/
│   │   ├── model/
│   │   │   ├── Models.kt               # Modelos de dominio (User, Payment, Invoice, Balance, etc.)
│   │   │   ├── ApiResponses.kt          # DTOs de API + funciones de mapeo toDomain()
│   │   │   ├── Building.kt             # Modelo de edificio
│   │   │   ├── UnitDto.kt              # DTO de unidad/apartamento
│   │   │   └── PettyCashModels.kt      # Modelos de caja chica
│   │   ├── remote/
│   │   │   ├── ApiService.kt           # Interfaz del servicio API
│   │   │   ├── ApiServiceImpl.kt       # Implementación con Ktor
│   │   │   └── KtorResponse.kt         # Wrapper de respuestas HTTP
│   │   ├── local/
│   │   │   ├── AppDatabase.kt          # Base de datos Room
│   │   │   ├── TokenManager.kt         # Gestión de JWT con DataStore
│   │   │   ├── Converters.kt           # Convertidores de tipo Room
│   │   │   ├── dao/
│   │   │   │   ├── UserDao.kt          # DAO de usuarios
│   │   │   │   └── PaymentDao.kt       # DAO de pagos
│   │   │   └── entity/
│   │   │       ├── UserEntity.kt       # Entidad Room de usuario
│   │   │       └── PaymentEntity.kt    # Entidad Room de pago
│   │   ├── repository/
│   │   │   ├── AuthRepository.kt       # Interfaz de autenticación
│   │   │   ├── RemoteAuthRepository.kt # Implementación remota de auth
│   │   │   ├── RoomAuthRepository.kt   # Implementación con Room (cache)
│   │   │   ├── PaymentRepository.kt    # Interfaz de pagos
│   │   │   ├── RemotePaymentRepository.kt # Implementación remota de pagos
│   │   │   ├── BuildingRepository.kt   # Interfaz + implementación de edificios
│   │   │   ├── PettyCashRepository.kt  # Interfaz de caja chica
│   │   │   └── PettyCashRepositoryImpl.kt # Implementación de caja chica
│   │   └── utils/
│   │       ├── PlatformFileReader.kt   # Lectura de archivos multiplataforma
│   │       └── PdfService.kt           # Servicio de generación de PDF
│   └── ui/
│       ├── navigation/
│       │   └── CondominioNavGraph.kt    # Grafo de navegación completo
│       ├── screens/
│       │   ├── auth/
│       │   │   └── PendingApprovalScreen.kt    # Pantalla de aprobación pendiente
│       │   ├── login/
│       │   │   ├── LoginScreen.kt              # Pantalla de login
│       │   │   └── LoginViewModel.kt           # ViewModel de login
│       │   ├── register/
│       │   │   ├── RegisterScreen.kt           # Pantalla de registro
│       │   │   └── RegisterViewModel.kt        # ViewModel de registro
│       │   ├── dashboard/
│       │   │   ├── DashboardScreen.kt          # Pantalla principal / dashboard
│       │   │   └── DashboardViewModel.kt       # ViewModel del dashboard
│       │   ├── payment/
│       │   │   ├── CreatePaymentScreen.kt      # Crear nuevo pago
│       │   │   ├── CreatePaymentViewModel.kt   # ViewModel crear pago
│       │   │   ├── PaymentHistoryScreen.kt     # Historial de pagos
│       │   │   ├── PaymentHistoryViewModel.kt  # ViewModel historial
│       │   │   ├── PaymentDetailScreen.kt      # Detalle de un pago
│       │   │   └── PaymentDetailViewModel.kt   # ViewModel detalle
│       │   ├── billing/
│       │   │   ├── InvoiceListScreen.kt        # Lista de facturas
│       │   │   ├── InvoiceListViewModel.kt     # ViewModel lista facturas
│       │   │   ├── InvoiceDetailScreen.kt      # Detalle de factura
│       │   │   └── InvoiceDetailViewModel.kt   # ViewModel detalle factura
│       │   ├── pettycash/
│       │   │   ├── PettyCashScreen.kt          # Pantalla de caja chica
│       │   │   └── PettyCashViewModel.kt       # ViewModel caja chica
│       │   ├── profile/
│       │   │   ├── ProfileScreen.kt            # Pantalla de perfil
│       │   │   ├── ProfileViewModel.kt         # ViewModel perfil
│       │   │   ├── EditProfileScreen.kt        # Editar perfil
│       │   │   ├── EditProfileViewModel.kt     # ViewModel editar perfil
│       │   │   ├── ChangePasswordScreen.kt     # Cambiar contraseña
│       │   │   ├── ChangePasswordViewModel.kt  # ViewModel cambiar contraseña
│       │   │   └── NotificationSettingsScreen.kt # Config. de notificaciones
│       │   └── UnitSelectionScreen.kt          # Selección de unidad
│       │   └── UnitSelectionViewModel.kt       # ViewModel selección de unidad
│       └── theme/
│           ├── Theme.kt                        # Tema Material 3
│           ├── Color.kt                        # Paleta de colores
│           └── Type.kt                         # Tipografía
├── androidMain/kotlin/com/example/condominio/
│   ├── CondominioApp.kt                 # Application class Android (Koin)
│   └── di/
│       ├── AppModule.kt                 # Módulo DI general
│       └── NetworkModule.kt             # Módulo DI de red (OkHttp)
└── main/
    ├── AndroidManifest.xml
    └── res/                              # Recursos Android
```

---

## 4. Flujo de Navegación

### Rutas de la Aplicación

```
login ──────────────────────┬──► unit_selection ──► dashboard
   │                        │        ▲
   ├──► register ──► pending_approval │
   │                        │        │
   └──► pending_approval    └────────┘
                                     │
dashboard ──────────────────────────┘
   ├──► create_payment (con invoiceId opcional)
   ├──► payment_history ──► payment_detail/{paymentId}
   ├──► invoice_list ──┬──► invoice_detail/{invoiceId}
   │                   └──► create_payment?invoiceId={id}
   ├──► decisions_list ──► decision_detail/{decisionId}
   ├──► profile ──┬──► edit_profile
   │              ├──► change_password
   │              └──► notification_settings
   └──► unit_selection (cambiar unidad activa)
```

> Nota: caja chica en APK es un **widget dentro del Dashboard** (solo lectura del balance del edificio), no una pantalla dedicada. La gestión completa (ingresos/gastos/historial) vive en el Web Admin.

### Detalle de Rutas

| Ruta | Pantalla | Descripción |
|------|----------|-------------|
| `login` | LoginScreen | Inicio de sesión con email y contraseña |
| `register` | RegisterScreen | Registro de nuevo residente |
| `pending_approval` | PendingApprovalScreen | Pantalla de espera tras registro |
| `unit_selection` | UnitSelectionScreen | Selección de unidad (multi-unidad) |
| `dashboard` | DashboardScreen | Panel principal con resumen de solvencia |
| `create_payment` | CreatePaymentScreen | Reportar un nuevo pago |
| `create_payment?invoiceId={id}` | CreatePaymentScreen | Pago vinculado a factura específica |
| `payment_history` | PaymentHistoryScreen | Historial de todos los pagos |
| `payment_detail/{paymentId}` | PaymentDetailScreen | Detalle completo de un pago |
| `invoice_list` | InvoiceListScreen | Lista de facturas del usuario |
| `invoice_detail/{invoiceId}` | InvoiceDetailScreen | Detalle de factura con pagos aplicados |
| `decisions_list` | DecisionsListScreen | Lista de decisiones colectivas del edificio (cotizaciones/votación) |
| `decision_detail/{decisionId}` | DecisionDetailScreen | Detalle de una decisión: quotes, votación, tally, resolución |
| `profile` | ProfileScreen | Perfil del usuario |
| `edit_profile` | EditProfileScreen | Edición de datos personales |
| `change_password` | ChangePasswordScreen | Cambio de contraseña |
| `notification_settings` | NotificationSettingsScreen | Configuración de notificaciones |

### Flujo de Login

1. El usuario ingresa email y contraseña
2. Si el login es exitoso:
   - Si tiene **múltiples unidades** → navega a `unit_selection`
   - Si tiene **una sola unidad** → navega directamente a `dashboard`
3. Si el usuario está en estado **pending** → navega a `pending_approval`
4. Desde `pending_approval` puede hacer logout y volver a `login`

---

## 5. Sistema de Roles y Permisos

### Roles Globales del Sistema

| Rol | Descripción | Alcance |
|-----|-------------|---------|
| `admin` | Administrador del sistema | Acceso total a todos los edificios y funciones |
| `board` | Miembro de junta directiva | Acceso a recursos de sus edificios asignados |
| `resident` | Residente | Acceso solo a sus propios recursos y unidades |

### Roles por Edificio (Building Role)

Un usuario puede tener un rol diferente en cada edificio donde tiene unidades:

| Building Role | Descripción |
|---------------|-------------|
| `board` | Miembro de junta de ese edificio |
| `resident` | Residente simple del edificio |
| `owner` | Propietario de unidad en el edificio |

### Diferencia Clave: Role Global vs Building Role

- **`user.role`**: Rol global del sistema. Define los permisos base del usuario.
- **`user.units[].building_role`**: Rol específico en cada edificio. Un `resident` global podría ser `board` en un edificio específico.

### Matriz de Permisos por Endpoint

| Acción | Admin | Board | Resident |
|--------|-------|-------|----------|
| Crear usuario | ✅ | ❌ | ❌ |
| Eliminar usuario | ✅ | ❌ | ❌ |
| Aprobar registro | ✅ | ✅ (mismo edificio) | ❌ |
| Listar todos los usuarios | ✅ | ✅ | ❌ |
| Ver perfil propio | ✅ | ✅ | ✅ |
| Editar perfil propio | ✅ | ✅ | ✅ |
| Cambiar rol de usuario | ✅ | ❌ | ❌ |
| Crear edificio | ✅ | ❌ | ❌ |
| Editar edificio | ✅ | ❌ | ❌ |
| Crear apartamentos | ✅ | ❌ | ❌ |
| Ver edificios | ✅ | ✅ | ✅ |
| Reportar pago | ✅ | ✅ | ✅ |
| Ver pagos propios | ✅ | ✅ | ✅ |
| Ver todos los pagos | ✅ | ✅ (mismo edificio) | ❌ |
| Aprobar/rechazar pagos | ✅ | ✅ (mismo edificio) | ❌ |
| Cargar deuda (crear factura) | ✅ | ✅ | ❌ |
| Ver facturas | ✅ | ✅ | ✅ (propias) |
| Ver balance de unidad | ✅ | ✅ | ✅ (propia) |
| Gestionar caja chica | ✅ | ✅ (mismo edificio) | ❌ |
| Asignar unidades a usuarios | ✅ | ✅ (mismo edificio) | ❌ |

---

## 6. Modelos de Datos

### 6.1 Modelos de Dominio (Capa Interna)

#### User
```kotlin
data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: String = "resident",          // admin | board | resident
    val status: String = "active",          // active | pending | rejected
    val units: List<UserUnit> = emptyList(),
    val buildingRoles: List<BuildingRole> = emptyList(),
    val currentUnit: UserUnit? = null,      // Unidad seleccionada actualmente
    val profileBuildingId: String? = null
)
// Propiedades computadas:
// - apartmentUnit: nombre de la unidad actual
// - building: nombre del edificio actual
// - buildingId: ID del edificio actual
```

#### UserUnit
```kotlin
data class UserUnit(
    val unitId: String,
    val buildingId: String,
    val unitName: String,
    val buildingName: String,
    val isPrimary: Boolean = false
)
```

#### BuildingRole
```kotlin
data class BuildingRole(
    val buildingId: String,
    val role: String
)
```

#### Payment
```kotlin
data class Payment(
    val id: String,
    val amount: Double,
    val date: Long,                         // Epoch milliseconds (KMP compatible)
    val status: PaymentStatus,              // PENDING | APPROVED | REJECTED
    val description: String,
    val method: PaymentMethod,              // PAGO_MOVIL | TRANSFER | CASH
    val reference: String? = null,
    val bank: String? = null,
    val phone: String? = null,
    val proofUrl: String? = null,           // URL del comprobante de pago
    val allocations: List<PaymentAllocation> = emptyList(),
    val createdAt: Long? = null,
    val processedAt: Long? = null,
    val processorName: String? = null,
    val userName: String? = null
)
```

#### PaymentAllocation
```kotlin
data class PaymentAllocation(
    val invoiceId: String,
    val amount: Double,
    val invoicePeriod: String? = null
)
```

#### Invoice
```kotlin
data class Invoice(
    val id: String,
    val period: String,                     // Formato: "2026-01"
    val amount: Double,
    val paid: Double,
    val remaining: Double,
    val status: InvoiceStatus,              // PENDING | PAID | CANCELLED | OVERDUE
    val description: String? = null,
    val dueDate: Long? = null,
    val type: InvoiceType = InvoiceType.COMMON  // COMMON | PETTY_CASH_REPLENISHMENT
)
```

#### Balance
```kotlin
data class Balance(
    val unitId: String,
    val totalDebt: Double,
    val currency: String = "USD",
    val pendingInvoices: List<Invoice>
)
```

#### Building
```kotlin
data class Building(
    val id: String,
    val name: String,
    val address: String? = null,
    val rif: String? = null,                // RIF (Registro de Información Fiscal - Venezuela)
    val createdAt: String? = null,
    val updatedAt: String? = null
)
```

#### UnitDto (Apartamento)
```kotlin
data class UnitDto(
    val id: String,
    val buildingId: String,
    val name: String,                       // Ej: "1-A", "2-B"
    val floor: String?,                     // Piso
    val aliquot: Double?                    // Porcentaje de alícuota
)
```

#### PaymentSummary
```kotlin
data class PaymentSummary(
    val solvencyStatus: SolvencyStatus,     // SOLVENT ("Al día") | PENDING ("Pagos Pendientes")
    val lastPaymentDate: Long?,
    val recentTransactions: List<Payment>,
    val unitName: String = "",
    val totalDebt: Double = 0.0
)
```

### 6.2 Modelos de Caja Chica (Petty Cash)

#### PettyCashTransactionType
```kotlin
enum class PettyCashTransactionType {
    INCOME, EXPENSE
}
```

#### PettyCashCategory
```kotlin
enum class PettyCashCategory(val displayName: String) {
    REPAIR("Reparación"),
    CLEANING("Limpieza"),
    EMERGENCY("Emergencia"),
    OFFICE("Oficina"),
    UTILITIES("Servicios"),
    OTHER("Otro")
}
```

#### PettyCashBalanceDto
```kotlin
data class PettyCashBalanceDto(
    val currentBalance: Double,
    val currency: String,
    val updatedAt: String
)
```

#### PettyCashTransactionDto
```kotlin
data class PettyCashTransactionDto(
    val id: String,
    val type: PettyCashTransactionType,     // INCOME | EXPENSE
    val amount: Double,
    val description: String,
    val category: PettyCashCategory,
    val evidenceUrl: String? = null,        // URL de evidencia del gasto
    val createdAt: String
)
```

### 6.3 Enums del Sistema

| Enum | Valores | Descripción |
|------|---------|-------------|
| PaymentStatus | `PENDING`, `APPROVED`, `REJECTED` | Estado de un pago reportado |
| PaymentMethod | `PAGO_MOVIL` ("Pago Móvil"), `TRANSFER` ("Transferencia"), `CASH` ("Efectivo") | Método de pago utilizado |
| InvoiceStatus (API v6) | `PENDING`, `PARTIAL`, `PAID`, `CANCELLED` | Estado de factura. `PARTIAL` es nativo v6 (sin alias `PARTIALLY_PAID`). |
| InvoiceType (API v6) | `EXPENSE`, `DEBT`, `EXTRAORDINARY`, `PETTY_CASH_REPLENISHMENT` | Tipo de factura |
| InvoiceTag | `NORMAL`, `PETTY_CASH` | Tag de visibilidad/origen |
| SolvencyStatus | `SOLVENT` ("Al día"), `PENDING` ("Pagos Pendientes") | Estado de solvencia del residente |
| DecisionStatus | `RECEPTION`, `VOTING`, `TIEBREAK_PENDING`, `RESOLVED`, `CANCELLED` | Estado de una decisión colectiva |
| ResultingType | `INVOICE`, `ASSESSMENT` | Cómo se materializa una decisión resuelta |
| PettyCashTransactionType | `INCOME`, `EXPENSE` | Tipo de transacción de caja chica (solo usado en Web Admin) |
| PettyCashCategory | `REPAIR`, `CLEANING`, `EMERGENCY`, `OFFICE`, `UTILITIES`, `OTHER` | Categoría de gasto de caja chica (solo usado en Web Admin) |

---

## 7. Capa de Datos — Repositorios

### AuthRepository (Autenticación)
```kotlin
interface AuthRepository {
    val currentUser: Flow<User?>
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(name: String, email: String, unitId: String, building: String, password: String): Result<User>
    suspend fun getUnits(buildingId: String): Result<List<UnitDto>>
    suspend fun getUnit(unitId: String): Result<UnitDto>
    suspend fun logout()
    suspend fun fetchCurrentUser(): Result<User>
    suspend fun updateUser(user: User): Result<Unit>
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>
    fun setCurrentUnit(unit: UserUnit)
}
```

**Implementaciones**:
- `RemoteAuthRepository`: Comunicación con el backend via Ktor
- `RoomAuthRepository`: Cache local con Room Database

### PaymentRepository (Pagos y Facturación)
```kotlin
interface PaymentRepository {
    suspend fun getPayments(unitId: String? = null): List<Payment>
    suspend fun getPayment(id: String): Payment?
    suspend fun getPaymentSummary(): Result<PaymentSummary>
    suspend fun createPayment(
        amount: Double, date: Date, description: String,
        method: PaymentMethod, unitId: String,
        allocations: List<PaymentAllocation> = emptyList(),
        reference: String?, bank: String?, phone: String?,
        proofUrl: String?, buildingId: String?
    ): Result<Payment>
    suspend fun getBalance(unitId: String): Result<Balance>
    suspend fun getInvoices(unitId: String, status: String? = null): Result<List<Invoice>>
    suspend fun getInvoice(id: String): Result<Invoice>
    suspend fun getInvoicePayments(invoiceId: String): Result<List<Payment>>
}
```

### BuildingRepository (Edificios)
```kotlin
interface BuildingRepository {
    suspend fun getBuildings(): Result<List<Building>>
    suspend fun getBuilding(id: String): Result<Building>
}
```

### PettyCashRepository (Caja Chica)
```kotlin
interface PettyCashRepository {
    suspend fun getBalance(buildingId: String): Result<PettyCashBalanceDto>
    suspend fun getHistory(
        buildingId: String,
        type: PettyCashTransactionType? = null,
        category: PettyCashCategory? = null,
        page: Int = 1, limit: Int = 10
    ): Result<List<PettyCashTransactionDto>>
    suspend fun registerIncome(buildingId: String, amount: Double, description: String): Result<PettyCashTransactionDto>
    suspend fun registerExpense(
        buildingId: String, amount: Double, description: String,
        category: PettyCashCategory, evidencePath: String? = null
    ): Result<PettyCashTransactionDto>
}
```

---

## 8. Capa de Red — ApiService

### Interfaz del Servicio API

```kotlin
interface ApiService {
    // === Autenticación ===
    suspend fun register(request: Map<String, String>): Response<RegisterResponse>
    suspend fun login(credentials: Map<String, String>): Response<LoginResponse>
    
    // === Usuario ===
    suspend fun getCurrentUser(): Response<UserProfile>
    suspend fun updateUser(updates: UpdateUserRequest): Response<UserProfile>
    suspend fun getUserUnits(id: String): Response<List<UserUnitDto>>
    
    // === Edificios ===
    suspend fun getBuildings(): Response<List<Building>>
    suspend fun getBuilding(id: String): Response<Building>
    suspend fun getBuildingUnits(id: String): Response<List<UnitDto>>
    suspend fun getUnitDetails(id: String): Response<UnitDto>
    
    // === Pagos ===
    suspend fun getPaymentSummary(): Response<PaymentSummaryDto>
    suspend fun getPayments(unitId: String? = null, year: Int? = null): Response<List<PaymentDto>>
    suspend fun getPayment(id: String): Response<PaymentDto>
    suspend fun createPayment(request: CreatePaymentRequest): Response<PaymentDto>
    suspend fun createPaymentMultipart(
        amount: String, unitId: String, buildingId: String?,
        method: String, date: String, notes: String?,
        reference: String?, bank: String?, allocations: String?,
        proofImage: ByteArray? = null, fileName: String? = null
    ): Response<PaymentDto>
    
    // === Facturación ===
    suspend fun getBalance(unitId: String): Response<BalanceDto>
    suspend fun getInvoices(unitId: String, status: String? = null): Response<List<InvoiceDto>>
    suspend fun getInvoice(id: String): Response<InvoiceDto>
    suspend fun getInvoicePayments(id: String): Response<List<PaymentDto>>
    
    // === Caja Chica ===
    suspend fun getPettyCashBalance(buildingId: String): Response<PettyCashBalanceDto>
    suspend fun getPettyCashHistory(
        buildingId: String, type: String? = null, category: String? = null,
        page: Int = 1, limit: Int = 10
    ): Response<List<PettyCashTransactionDto>>
    suspend fun registerPettyCashIncome(request: RegisterIncomeRequest): Response<PettyCashTransactionDto>
    suspend fun registerPettyCashExpense(
        buildingId: String, amount: String, description: String,
        category: String, evidenceImage: ByteArray? = null, fileName: String? = null
    ): Response<PettyCashTransactionDto>
}
```

---

## 9. API del Backend — Referencia Completa

**Base URL**: `http://localhost:3000` (Development)
**Autenticación**: Bearer Token (JWT)
**Versión API**: v2.1.0 (Swagger: Condominio API v1.0.0)

### 9.1 Autenticación

#### POST `/auth/register`
Registrar nuevo residente.

**Permisos**: Público

**Request Body**:
```json
{
  "name": "Juan Pérez",
  "email": "juan@example.com",
  "password": "SecurePass123",
  "unit_id": "uuid-del-apartamento",
  "building_id": "uuid-del-edificio"
}
```

**Response**: Regresa `access_token`, `refresh_token`, `expires_in` y el perfil del usuario. El usuario queda en estado `pending` hasta que un Admin o Board lo apruebe.

#### POST `/auth/login`
Iniciar sesión.

**Permisos**: Público

**Request Body**:
```json
{
  "email": "juan@example.com",
  "password": "SecurePass123"
}
```

**Response**: Regresa `access_token`, `refresh_token`, `expires_in` y el perfil del usuario incluyendo sus `units` (unidades asignadas) y `buildingRoles`.

### 9.2 Usuarios

#### GET `/users/me`
Obtener perfil del usuario actual. **Permisos**: Usuario autenticado.

#### PATCH `/users/me`
Actualizar perfil propio (nombre, teléfono). **Permisos**: Usuario autenticado.

#### GET `/users`
Listar todos los usuarios con filtros opcionales (`building_id`, `unit_id`, `role`, `status`). **Permisos**: Admin o Board.

#### GET `/users/:id`
Obtener usuario por ID. **Permisos**: Admin, Board (mismo edificio), o el propio usuario.

#### POST `/users`
Crear nuevo usuario. **Permisos**: Solo Admin. Permite asignar rol, edificio y unidad.

#### PATCH `/users/:id`
Actualizar usuario. **Permisos**: Admin (todos los campos), Board (sin rol), Usuario (solo nombre/teléfono).

#### DELETE `/users/:id`
Eliminar usuario. **Permisos**: Solo Admin.

#### POST `/users/:id/approve`
Aprobar registro de usuario pendiente. **Permisos**: Admin o Board (mismo edificio).

#### GET `/users/:id/units`
Obtener unidades asignadas a un usuario. **Permisos**: Admin, Board, Usuario mismo.

#### POST `/users/:id/units`
Asignar o actualizar unidad de usuario. Si la unidad ya existe, actualiza el `building_role`. **Permisos**: Admin, Board (solo sus edificios).

**Casos de uso**:
- Asignar nueva unidad a usuario
- Promover residente a board
- Degradar board a resident
- Cambiar unidad primaria

### 9.3 Edificios y Apartamentos

#### GET `/buildings`
Listar todos los edificios. **Permisos**: Público.

#### GET `/buildings/:id`
Obtener edificio por ID. **Permisos**: Público.

#### POST `/buildings`
Crear nuevo edificio. **Permisos**: Solo Admin.

#### PATCH `/buildings/:id`
Actualizar edificio. **Permisos**: Solo Admin.

#### GET `/buildings/:id/units`
Listar apartamentos de un edificio. **Permisos**: Público.

#### GET `/buildings/units/:id`
Obtener apartamento por ID. **Permisos**: Público.

#### POST `/buildings/:id/units`
Crear apartamento individual. **Permisos**: Solo Admin.

#### POST `/buildings/:id/units/batch`
Crear apartamentos en lote especificando pisos y unidades por piso. **Permisos**: Solo Admin.

**Request Body ejemplo**:
```json
{
  "floors": ["1", "2", "3"],
  "unitsPerFloor": ["A", "B", "C"]
}
```
Esto crea 9 unidades: 1-A, 1-B, 1-C, 2-A, 2-B, 2-C, 3-A, 3-B, 3-C.

### 9.4 Pagos

#### GET `/payments`
Historial de pagos del usuario. Filtros: `year`, `unit_id`, `building_id`. **Permisos**: Usuario autenticado.

#### GET `/payments/summary`
Resumen con estado de solvencia (`SOLVENT` / `PENDING`), último pago, períodos pagados/pendientes. **Permisos**: Usuario autenticado.

#### GET `/payments/:id`
Detalles de un pago incluyendo allocations. **Permisos**: Admin, Board (mismo edificio), Residente (mismo apartamento).

#### POST `/payments`
Reportar nuevo pago. Soporta `multipart/form-data` para subir comprobante de pago. **Permisos**: Usuario autenticado.

**Campos del request**:
- `amount` (requerido): Monto del pago
- `date` (requerido): Fecha del pago
- `method` (requerido): `PAGO_MOVIL` | `TRANSFER` | `CASH`
- `unit_id` (requerido): Unidad que paga
- `reference` (opcional): Número de referencia
- `bank` (opcional): Banco utilizado
- `proof_image` (opcional): Imagen del comprobante
- `periods` (opcional): Períodos que cubre el pago
- `allocations` (opcional): Distribución del pago entre facturas
- `notes` (opcional): Notas adicionales

#### GET `/payments/admin/payments`
Listar todos los pagos (vista administrativa). Filtros: `building_id`, `status`, `period`, `year`, `unit_id`. **Permisos**: Admin (todos), Board (su edificio).

#### PATCH `/payments/admin/payments/:id`
Aprobar o rechazar un pago. **Permisos**: Admin, Board (mismo edificio).

**Request Body**:
```json
{
  "status": "APPROVED",
  "notes": "Confirmado",
  "approved_periods": ["2026-01"]
}
```

### 9.5 Facturación

#### GET `/billing/invoices`
Listar facturas con filtros: `unit_id`, `building_id`, `status`, `period`, `year`, `month`, `user_id`. **Permisos**: Admin o Board.

#### POST `/billing/debt`
Cargar deuda a un apartamento (crear factura). **Permisos**: Admin o Board.

**Request Body**:
```json
{
  "unit_id": "uuid",
  "amount": 100.00,
  "period": "2026-01",
  "description": "Condominio enero 2026",
  "due_date": "2026-01-31"
}
```

#### POST `/billing/invoices/preview`
Previsualizar facturación antes de confirmarla. **Permisos**: Admin o Board.

#### POST `/billing/invoices/confirm`
Confirmar facturación previamente previsualizada. **Permisos**: Admin o Board.

#### GET `/billing/units/:id/balance`
Obtener balance de un apartamento (deuda total, facturas pendientes con detalle). **Permisos**: Admin, Board, Residente (mismo apartamento).

#### GET `/billing/units/:id/invoices`
Listar facturas de un apartamento. **Permisos**: Admin, Residente (mismo apartamento).

#### GET `/billing/invoices/:id`
Detalles de una factura. **Permisos**: Admin, Board, Residente (mismo apartamento).

#### GET `/billing/invoices/:id/payments`
Listar pagos aplicados a una factura específica. **Permisos**: Admin o Board.

### 9.6 Caja Chica (Petty Cash) — APK scope

El APK consume **un único endpoint** de caja chica (solo lectura del balance para el widget del Dashboard). La gestión completa (ingresos/gastos/historial) vive en el Web Admin.

#### GET `/api/v1/app/petty-cash/funds/:buildingId`
Balance actual del fondo de caja chica del edificio. **Permisos**: Board o Admin del edificio.

### 9.7 Decisiones Colectivas — APK scope

Prefijo de todos los endpoints APK: `/api/v1/app/decisions/decisions`. Permisos: residentes del edificio. Orden por `created_at DESC`. Paginación `{ data, metadata: { total, page, limit, total_pages, has_next_page, has_prev_page } }` (alineado a PR #44).

#### GET `/decisions`
Lista de decisiones. Query: `building_id`, `status?`, `search?`, `page?`, `limit?`. Retorna `{ data: DecisionDto[], metadata }`.

#### GET `/decisions/:id`
Detalle bundle: `{ decision, quotes, tally, my_vote }`. `my_vote` puede ser `null` o incluir `apartment_label`.

#### POST `/decisions/:id/quotes` (multipart)
Subir presupuesto con archivo adjunto. Campos: `unit_id`, `provider_name`, `amount`, `notes?`, `file`.

#### DELETE `/decisions/:id/quotes/:quoteId`
Eliminar (soft) un presupuesto subido por el residente.

#### POST `/decisions/:id/votes`
Emitir voto. Body: `{ apartment_id, quote_id }`.

#### GET `/decisions/:id/results?round=N`
Tally de una ronda (por defecto la ronda activa).

### 9.8 Endpoint de Salud

#### GET `/health`
Verificar que el servidor está funcionando.

---

## 10. Autenticación y Seguridad

### Flujo de Autenticación (JWT)

1. El usuario hace login con email y contraseña
2. El backend retorna un `access_token` (JWT) y un `refresh_token`
3. El `access_token` se almacena localmente con `TokenManager` (DataStore)
4. Todas las solicitudes HTTP incluyen el header `Authorization: Bearer {token}`
5. El token expira según `expires_in` (segundos)

### TokenManager
```kotlin
class TokenManager(private val dataStore: DataStore<Preferences>) {
    suspend fun saveToken(token: String)
    suspend fun getToken(): String?
    val tokenFlow: Flow<String?>
    suspend fun clearToken()
}
```

Almacena el JWT en DataStore con la key `jwt_token`.

### Códigos de Error HTTP

| Código | Descripción |
|--------|-------------|
| 200 | OK - Éxito |
| 201 | Created - Recurso creado |
| 400 | Bad Request - Datos inválidos |
| 401 | Unauthorized - No autenticado |
| 403 | Forbidden - Sin permisos |
| 404 | Not Found - Recurso no encontrado |
| 500 | Internal Server Error - Error del servidor |

---

## 11. Persistencia Local

### Room Database

La app usa Room para cache local:

**Entidades**:
- `UserEntity`: Cache de datos del usuario logueado
- `PaymentEntity`: Cache de pagos para acceso offline

**DAOs**:
- `UserDao`: Operaciones CRUD de usuario
- `PaymentDao`: Operaciones CRUD de pagos

### DataStore Preferences

Se usa para almacenar:
- JWT token (`jwt_token`)
- Preferencias del usuario

---

## 12. Inyección de Dependencias (Koin)

### Módulos

- **AppModule** (Android): Provee instancias de Repository, ViewModel, Database, TokenManager
- **NetworkModule** (Android): Configura Ktor HttpClient con OkHttp engine, logging interceptor, y autenticación automática via TokenManager

### Patrón de Inyección

Los ViewModels se inyectan vía `koinViewModel()` en las pantallas Compose:
```kotlin
val viewModel: LoginViewModel = koinViewModel()
```

---

## 13. Flujos de Negocio Principales

### 13.1 Flujo de Registro de Nuevo Residente

1. Residente abre la app → Pantalla de Login
2. Click en "Registrarse" → Pantalla de Registro
3. Completa: nombre, email, contraseña, selecciona edificio y apartamento
4. El backend crea el usuario con `status: "pending"`
5. El residente ve la pantalla de "Aprobación Pendiente"
6. Un Admin o Board aprueba el registro via `POST /users/:id/approve`
7. El residente puede hacer login normalmente

### 13.2 Flujo de Reporte de Pago

1. Residente accede al Dashboard
2. Click en "Pagar" → Pantalla de Crear Pago
3. Alternativamente: desde lista de facturas, click en factura pendiente → Crear Pago con `invoiceId` pre-seleccionado
4. Completa: monto, método de pago, referencia, banco, fecha
5. Opcionalmente adjunta comprobante (foto)
6. Opcionalmente distribuye el pago entre múltiples facturas (allocations)
7. El pago se crea con `status: PENDING`
8. Un Admin o Board revisa y aprueba/rechaza el pago

### 13.3 Flujo de Caja Chica (APK — solo lectura)

1. Board/Admin ve el **widget de balance** de caja chica en el Dashboard del edificio
2. El widget consulta `GET /api/v1/app/petty-cash/funds/:buildingId` y muestra el saldo
3. La gestión completa (ingresos, gastos con categoría + evidencia, historial) se hace desde el **Web Admin**, no desde el APK

### 13.4 Flujo de Facturación

1. Admin/Board carga deuda a apartamentos (`POST /billing/debt`)
2. Se genera factura con `status: PENDING`
3. El residente ve la factura en su lista
4. El residente reporta un pago y lo asigna a la factura
5. Al aprobar el pago, el `paid_amount` de la factura se actualiza
6. Si `paid_amount >= amount`, la factura pasa a `PAID`
7. Pagos parciales son soportados

### 13.5 Flujo de Multi-Unidad

Un usuario puede estar asignado a múltiples unidades/apartamentos. Por ejemplo, un propietario con 3 apartamentos en el mismo edificio:

1. Al hacer login, si tiene múltiples unidades → pantalla de selección
2. Selecciona la unidad activa
3. El dashboard y todas las consultas se hacen sobre la unidad seleccionada
4. Puede cambiar de unidad desde el dashboard

### 13.6 Flujo de Decisiones Colectivas

Proceso formal para tomar decisiones de edificio (reparaciones grandes, servicios, etc.):

1. Admin/Board crea una decisión desde el Web Admin (el APK no la crea)
2. **Estado RECEPTION**: residentes suben presupuestos (`POST /quotes`) hasta `reception_deadline`
3. **Estado VOTING**: cada apartamento vota por un presupuesto (`POST /votes`) hasta `voting_deadline`
4. **Estado TIEBREAK_PENDING**: si hay empate, ronda de desempate entre los presupuestos empatados
5. **Estado RESOLVED**: se determina ganador (`winner_quote_id`), opcionalmente se materializa en factura o derrama (`resulting_type`)
6. **Estado CANCELLED**: decisión cancelada con motivo (`cancel_reason`)

El APK muestra la lista filtrable por estado, el detalle con tally animado (barras + donut + highlight del ganador), y permite subir quotes y votar según el estado activo.

---

## 14. Pantallas de la Aplicación

### 14.1 LoginScreen
- Campos: Email, Contraseña
- Validación de formulario
- Botón de login
- Link a registro
- Opción de limpiar base de datos local (para desarrollo)

### 14.2 RegisterScreen
- Campos: Nombre, Email, Contraseña
- Selector de edificio (cargado del API)
- Selector de apartamento (filtrado por edificio)
- Validación de formulario

### 14.3 PendingApprovalScreen
- Mensaje informativo de que el registro está pendiente de aprobación
- Botón de logout

### 14.4 UnitSelectionScreen
- Lista de unidades asignadas al usuario
- Muestra nombre de unidad y edificio
- Selección para establecer unidad activa

### 14.5 DashboardScreen
- Estado de solvencia (SOLVENT/PENDING) con indicador visual
- Balance actual / deuda total
- Transacciones recientes
- Accesos rápidos a:
  - Crear pago
  - Ver historial de pagos
  - Ver facturas
  - Caja chica
  - Perfil
  - Cambiar unidad

### 14.6 CreatePaymentScreen
- Formulario de pago con:
  - Monto
  - Método: Pago Móvil, Transferencia, Efectivo
  - Referencia bancaria
  - Banco
  - Fecha
  - Notas
  - Upload de comprobante (imagen)
  - Selección de facturas para asignar el pago (allocations)
- Si viene con `invoiceId`, pre-selecciona esa factura

### 14.7 PaymentHistoryScreen
- Lista scrollable de pagos
- Muestra: monto, fecha, estado (badge de color), método

### 14.8 PaymentDetailScreen
- Información completa del pago
- Estado con verificación visual (Approved/Pending/Rejected)
- Comprobante de pago (imagen)
- Detalles del procesamiento (quién aprobó, cuándo)
- Allocations (a qué facturas se asignó)

### 14.9 InvoiceListScreen
- Lista de facturas con estado, período, monto, pagado
- Click en factura pagada → detalle
- Click en factura pendiente → crear pago

### 14.10 InvoiceDetailScreen
- Detalle completo de la factura
- Pagos aplicados a esta factura
- Botón para pagar el remanente
- Links a historial de pagos y lista de facturas

### 14.11 Petty Cash Widget (en DashboardScreen)
- Card inline dentro del Dashboard (no hay pantalla dedicada)
- Muestra solo el balance del edificio (`GET /app/petty-cash/funds/:buildingId`)
- Sin flujos de alta de ingresos/gastos (esa gestión es exclusiva del Web Admin)

### 14.12 ProfileScreen
- Datos del usuario (nombre, email, rol, unidad, edificio)
- Accesos a:
  - Editar perfil
  - Cambiar contraseña
  - Configurar notificaciones
  - Cerrar sesión

### 14.13 EditProfileScreen
- Edición de nombre y teléfono

### 14.14 ChangePasswordScreen
- Contraseña actual y nueva contraseña

### 14.15 NotificationSettingsScreen
- Configuración de preferencias de notificaciones

### 14.16 DecisionsListScreen
- Lista de decisiones del edificio con chips de estado (RECEPTION / VOTING / TIEBREAK / RESOLVED / CANCELLED)
- Búsqueda + filtro por estado
- Paginación contra `{ data, metadata }` (API v6 / PR #44)

### 14.17 DecisionDetailScreen
- Bundle: decision + quotes + tally + my_vote
- Secciones: datos de la decisión, lista de quotes (con archivo adjunto), visualización animada del tally (barras, donut, highlight del ganador), CTA para votar o subir presupuesto según el estado
- Respeta soft-delete de quotes y reglas de tiebreak

---

## 15. Mapeo DTO → Dominio

La app implementa un patrón de mapeo explícito entre DTOs (Data Transfer Objects) del API y modelos de dominio internos usando funciones de extensión `toDomain()`:

### Reglas de Mapeo

- **PaymentDto → Payment**: Maneja múltiples formatos de fecha (LocalDate, Instant), fallback de status a PENDING, normalización de métodos de pago (ej: "TRANSFERENCIA" → TRANSFER)
- **InvoiceDto → Invoice**: Mapeo directo sobre el enum v6 (`PENDING | PARTIAL | PAID | CANCELLED`). El alias legacy `PARTIALLY_PAID` fue eliminado del APK al migrar al contrato v6.
- **PaymentSummaryDto → PaymentSummary**: Convierte solvency_status string a enum SolvencyStatus
- **AllocationDto → PaymentAllocation**: Mapeo directo
- **DecisionDto / QuoteDto / VoteDto / TallyDto**: Serialización directa via `kotlinx.serialization` (no hay capa de mapeo extra); `VoteDto.apartment_label` es opcional (default `null`) y llega poblado cuando viene embebido en `DecisionDetailDto.my_vote`.

### Gotchas del Mapeo

1. Las fechas pueden venir como `LocalDate` ("2026-01-15") o como `Instant` ISO 8601 ("2026-01-15T10:00:00Z")
2. El campo `paymentId` de PaymentDto tiene prioridad sobre `id` (para respuestas de join tables)
3. Si `status` es null en PaymentDto, se asume APPROVED (pagos de join tables)
4. `notes` tiene prioridad sobre `description` como texto descriptivo del pago
5. `Json { ignoreUnknownKeys = true }` en Ktor da gracia hacia adelante: campos añadidos por el backend (ej. `has_next_page`, `apartment_label`) no rompen la deserialización si el DTO APK aún no los declara
6. Renombres de envelope sí son breaking (ej. PR #44: `items` → `data`). Requieren actualización del DTO + cualquier sitio que lea la colección
5. `remaining` se calcula como `amount - paid` si no viene del backend

---

## 16. Diseño Visual

### Pantallas de Diseño Disponibles

La carpeta `design/` contiene mockups de las siguientes pantallas:
- `home_dashboard/` — Dashboard principal
- `owner_login/` — Login
- `payment_details/` — Detalle de pago
- `payment_history/` — Historial de pagos
- `register_new_payment/` — Registro de pago
- `user_registration/` — Registro de usuario

### Tema Material 3

La app utiliza Material 3 con una paleta cálida y premium definida en:
- `Color.kt` — Paleta de colores del tema
- `Type.kt` — Tipografía personalizada
- `Theme.kt` — Configuración del tema Material 3

---

## 17. Contexto de Negocio

### País de Operación: Venezuela (y LATAM en general)

El sistema está diseñado para condominios en Venezuela, lo cual explica:
- **Métodos de pago**: Pago Móvil (sistema venezolano de transferencias instantáneas), Transferencias bancarias, Efectivo
- **Moneda**: USD (dolarización de facto en Venezuela)
- **RIF**: Campo para Registro de Información Fiscal de los edificios
- **Campos contextuales**: Bancos venezolanos, Pago Móvil con referencia y teléfono

### Terminología del Dominio

| Término | Significado |
|---------|-------------|
| Condominio | Cuota mensual de mantenimiento del edificio |
| Edificio (Building) | Inmueble residencial gestionado por la plataforma |
| Apartamento/Unidad (Unit) | Unidad habitacional dentro del edificio |
| Alícuota | Porcentaje de participación de cada unidad en los gastos comunes |
| Junta Directiva (Board) | Comité electivo de propietarios que administra el edificio |
| Solvencia | Estado de estar al día con los pagos de condominio |
| Caja Chica (Petty Cash) | Fondo para gastos menores del edificio |
| Pago Móvil | Sistema de pagos instantáneos inter-bancarios de Venezuela |
| Período | Mes de facturación (formato: "YYYY-MM") |

---

## 18. Endpoints Completos del Swagger

Lista completa de los 30+ endpoints disponibles en el backend:

| Método | Endpoint | Categoría |
|--------|----------|-----------|
| POST | `/auth/login` | Autenticación |
| POST | `/auth/register` | Autenticación |
| GET | `/users/` | Usuarios |
| POST | `/users/` | Usuarios |
| GET | `/users/me` | Usuarios |
| PATCH | `/users/me` | Usuarios |
| GET | `/users/{id}` | Usuarios |
| PATCH | `/users/{id}` | Usuarios |
| DELETE | `/users/{id}` | Usuarios |
| POST | `/users/{id}/approve` | Usuarios |
| GET | `/users/{id}/units` | Usuarios |
| POST | `/users/{id}/units` | Usuarios |
| GET | `/buildings/` | Edificios |
| POST | `/buildings/` | Edificios |
| GET | `/buildings/{id}` | Edificios |
| PATCH | `/buildings/{id}` | Edificios |
| GET | `/buildings/{id}/units` | Edificios |
| POST | `/buildings/{id}/units` | Edificios |
| POST | `/buildings/{id}/units/batch` | Edificios |
| GET | `/buildings/units/{id}` | Edificios |
| GET | `/payments/` | Pagos |
| POST | `/payments/` | Pagos |
| GET | `/payments/summary` | Pagos |
| GET | `/payments/{id}` | Pagos |
| GET | `/payments/admin/payments` | Pagos (Admin) |
| PATCH | `/payments/admin/payments/{id}` | Pagos (Admin) |
| GET | `/billing/invoices` | Facturación |
| POST | `/billing/debt` | Facturación |
| POST | `/billing/invoices/preview` | Facturación |
| POST | `/billing/invoices/confirm` | Facturación |
| GET | `/billing/invoices/{id}` | Facturación |
| GET | `/billing/invoices/{id}/payments` | Facturación |
| GET | `/billing/units/{id}/balance` | Facturación |
| GET | `/billing/units/{id}/invoices` | Facturación |
| GET | `/app/petty-cash/funds/{buildingId}` | Caja Chica (APK widget) |
| GET | `/app/decisions/decisions` | Decisiones |
| GET | `/app/decisions/decisions/{id}` | Decisiones |
| POST | `/app/decisions/decisions/{id}/quotes` | Decisiones |
| DELETE | `/app/decisions/decisions/{id}/quotes/{quoteId}` | Decisiones |
| POST | `/app/decisions/decisions/{id}/votes` | Decisiones |
| GET | `/app/decisions/decisions/{id}/results` | Decisiones |
| GET | `/health` | Sistema |

> Los endpoints de gestión completa de caja chica (`income`, `expense`, `history`) existen en el backend pero son consumidos únicamente por el Web Admin. El APK solo lee el balance.

---

*Generado automáticamente desde el código fuente del proyecto Condominio.*
*Última actualización: 2026-04-24 — sincronizado con API v6 + PR #44 (decisions pagination).*
