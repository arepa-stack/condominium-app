package com.example.condominio.telemetry

import com.example.condominio.data.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Analytics + error reports. Implementations never throw and never block; no PII ever (the building id is the only tenant id allowed; no user/unit ids, names, emails, amounts). */
interface Telemetry {
    fun screen(name: String)
    fun event(name: String, params: Map<String, Any> = emptyMap())   // values: String | Long | Int | Double | Boolean
    fun recordError(throwable: Throwable, where: String)             // non-fatal (Crashlytics / Sentry)
    /** Active tenant; null clears it (logout). Only non-personal tenant ids (see docs/telemetria.md §3.2). */
    fun setTenant(buildingId: String?)
}

object NoTelemetry : Telemetry {
    override fun screen(name: String) {}
    override fun event(name: String, params: Map<String, Any>) {}
    override fun recordError(throwable: Throwable, where: String) {}
    override fun setTenant(buildingId: String?) {}
}

/** "payment_detail/{paymentId}" → "payment_detail"; "create_payment?invoiceId={invoiceId}" → "create_payment". */
fun screenName(route: String?): String? =
    route?.substringBefore('/')?.substringBefore('?')?.takeIf { it.isNotBlank() }

/** Building id of the active unit (null when signed out), without repeats; feeds [Telemetry.setTenant]. */
fun AuthRepository.activeBuildingId(): Flow<String?> =
    currentUser.map { it?.currentUnit?.buildingId }.distinctUntilChanged()
