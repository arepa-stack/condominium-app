package com.example.condominio.telemetry

/** Analytics + error reports. Implementations never throw and never block; no PII ever (no ids, names, emails, amounts). */
interface Telemetry {
    fun screen(name: String)
    fun event(name: String, params: Map<String, Any> = emptyMap())   // values: String | Long | Int | Double
    fun recordError(throwable: Throwable, where: String)             // non-fatal (Crashlytics / Sentry)
}

object NoTelemetry : Telemetry {
    override fun screen(name: String) {}
    override fun event(name: String, params: Map<String, Any>) {}
    override fun recordError(throwable: Throwable, where: String) {}
}

/** "payment_detail/{paymentId}" → "payment_detail"; "create_payment?invoiceId={invoiceId}" → "create_payment". */
fun screenName(route: String?): String? =
    route?.substringBefore('/')?.substringBefore('?')?.takeIf { it.isNotBlank() }
