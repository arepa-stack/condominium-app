package com.example.condominio.data.remote

import com.example.condominio.telemetry.Telemetry
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.encodedPath
import kotlinx.coroutines.CancellationException
import kotlin.time.TimeSource

private val STATIC_SEGMENT = Regex("[a-z]+(-[a-z]+)*|v\\d+")

/**
 * Normalizes a request path into a low-cardinality template without IDs or building codes.
 * Strips leading/trailing `/` and the `api/v1/app/` prefix; keeps a segment only when it matches
 * `[a-z]+(-[a-z]+)*` or `v\d+` and the preceding segment is not `by-code`, replacing everything else with `{id}`.
 */
fun endpointTemplate(path: String): String {
    val segments = path.trim('/').removePrefix("api/v1/app/").trim('/').split('/').filter { it.isNotEmpty() }
    return segments.mapIndexed { i, seg ->
        if (segments.getOrNull(i - 1) != "by-code" && STATIC_SEGMENT.matches(seg)) seg else "{id}"
    }.joinToString("/")
}

internal val businessEvents: Map<String, String> = mapOf(
    "POST auth/login" to "login",
    "POST registration-requests" to "registration_requested",
    "POST auth/reset-password" to "password_reset_requested",
    "POST auth/change-password-first-login" to "first_password_set",
    "PATCH users/me/password" to "password_changed",
    "PATCH users/me" to "profile_updated",
    "DELETE users/me" to "account_deleted",
    "POST payments" to "payment_reported",
    "POST decisions/decisions/{id}/votes" to "vote_cast",
    "POST decisions/decisions/{id}/quotes" to "quote_uploaded",
    "POST information-center/announcements/{id}/reaction" to "announcement_reaction",
)

fun apiTelemetry(telemetry: Telemetry, time: TimeSource = TimeSource.Monotonic) = createClientPlugin("ApiTelemetry") {
    on(Send) { request ->
        val endpoint = "${request.method.value} ${endpointTemplate(request.url.encodedPath)}"
        val start = time.markNow()
        fun apiError(status: Int) = report {
            telemetry.event(
                "api_error",
                mapOf("endpoint" to endpoint, "status" to status, "duration_ms" to start.elapsedNow().inWholeMilliseconds)
            )
        }
        val call = try {
            proceed(request)
        } catch (e: Throwable) {
            if (e !is CancellationException) apiError(0)
            throw e
        }
        val status = call.response.status.value
        if (status >= 400) apiError(status) else businessEvents[endpoint]?.let { name -> report { telemetry.event(name) } }
        call
    }
}

private inline fun report(block: () -> Unit) {
    try { block() } catch (_: Throwable) {}
}
