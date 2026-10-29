package com.example.condominio.telemetry

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

private fun gtagEvent(name: String, json: String) {
    js("try { if (window.gtag) window.gtag('event', name, JSON.parse(json)); } catch (e) {}")
}

private fun gtagScreen(name: String) {
    js("try { if (window.gtag) window.gtag('event', 'page_view', { page_title: name, page_location: location.origin + location.pathname + '#' + name }); } catch (e) {}")
}

private fun sentryError(message: String, where: String) {
    js("try { if (window.Sentry) window.Sentry.captureException(new Error(message), { tags: { where: where }, fingerprint: [where, message] }); } catch (e) {}")
}

object WebTelemetry : Telemetry {
    override fun screen(name: String) {
        try { gtagScreen(name) } catch (_: Throwable) {}
    }

    override fun event(name: String, params: Map<String, Any>) {
        try {
            val json = buildJsonObject {
                for ((k, v) in params) {
                    when (v) {
                        is String -> put(k, JsonPrimitive(v))
                        is Number -> put(k, JsonPrimitive(v))
                        is Boolean -> put(k, JsonPrimitive(v))
                    }
                }
            }.toString()
            gtagEvent(name, json)
        } catch (_: Throwable) {}
    }

    override fun recordError(throwable: Throwable, where: String) {
        try {
            val msg = "${throwable::class.simpleName ?: "Throwable"}: ${throwable.message ?: ""}"
            sentryError(msg, where)
        } catch (_: Throwable) {}
    }
}
