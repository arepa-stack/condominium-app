package com.example.condominio.data.remote

import com.example.condominio.data.model.CreatePaymentRequest
import com.example.condominio.data.model.RegistrationRequestBody
import com.example.condominio.data.model.UpdateUserRequest
import com.example.condominio.testutil.FakeTelemetry
import com.example.condominio.testutil.FakeTokenManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.callContext
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

private const val NOW = 1_700_000_000L
private fun b64(s: String) = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(s.encodeToByteArray())
private fun jwt(payload: String) = "${b64("""{"alg":"HS256","typ":"JWT"}""")}.${b64(payload)}.signature"
private val SAVED = jwt("""{"sub":"u1","exp":${NOW + 3600}}""")

@OptIn(InternalAPI::class)
private class TelemetryTestEngine(
    private val time: TestTimeSource,
    var status: HttpStatusCode = HttpStatusCode.OK,
    var offline: Boolean = false,
    var body: String = "{}",
) : HttpClientEngineBase("telemetry-test-engine") {
    override val config = HttpClientEngineConfig()
    override val dispatcher: CoroutineDispatcher = Dispatchers.Unconfined

    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        time += 120.milliseconds
        if (offline) throw Exception("Fail to fetch")
        return HttpResponseData(
            status, GMTDate(), headersOf("Content-Type", "application/json"),
            HttpProtocolVersion.HTTP_1_1, ByteReadChannel(body), callContext()
        )
    }
}

class ApiTelemetryTest {
    private val time = TestTimeSource()
    private val fake = FakeTelemetry()
    private val engine = TelemetryTestEngine(time)
    private val api = ApiServiceImpl(
        HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }) }
            install(apiTelemetry(fake, time))
            defaultRequest { url("https://api.test/") }
        },
        SessionExpiry(FakeTokenManager(SAVED), nowSeconds = { NOW }),
        fake
    )

    @Test fun `a 500 response emits api_error with template status and duration`() = runTest {
        engine.status = HttpStatusCode.InternalServerError
        assertEquals(500, api.getPayment("pay-uuid-99").code)
        assertEquals(
            listOf("api_error" to mapOf<String, Any>("endpoint" to "GET payments/{id}", "status" to 500, "duration_ms" to 120L)),
            fake.events
        )
    }

    @Test fun `an offline failure emits api_error with status 0 and keeps network error`() = runTest {
        engine.offline = true
        assertTrue(api.getBuildings().isNetworkError)
        assertEquals(
            listOf("api_error" to mapOf<String, Any>("endpoint" to "GET buildings", "status" to 0, "duration_ms" to 120L)),
            fake.events
        )
    }

    @Test fun `a successful read emits no telemetry`() = runTest {
        engine.body = "[]"
        assertTrue(api.getBuildings().isSuccessful)
        assertTrue(fake.events.isEmpty())
        assertTrue(fake.errors.isEmpty())
    }

    @Test fun `query parameters never appear in api_error telemetry`() = runTest {
        engine.status = HttpStatusCode.InternalServerError
        api.getPayments(unitId = "unit-secret-1", year = 2025)
        val params = fake.events.single().second
        assertEquals("GET payments", params["endpoint"])
        assertFalse(params.values.any { "unit-secret-1" in it.toString() || "2025" in it.toString() })
    }

    @Test fun `each successful business endpoint emits its business event`() = runTest {
        val cases: List<Pair<String, suspend () -> Any>> = listOf(
            "login" to { api.login(mapOf("email" to "a@b.c", "password" to "p")) },
            "registration_requested" to {
                api.submitRegistrationRequest(
                    RegistrationRequestBody("COND-1", "u1", "a@b.c", "Ana", "P", "V1")
                )
            },
            "password_reset_requested" to { api.resetPassword("a@b.c") },
            "first_password_set" to { api.changePasswordFirstLogin("newPass123") },
            "password_changed" to { api.changePassword("newPass123") },
            "profile_updated" to { api.updateUser(UpdateUserRequest(name = "Ana", phone = null, unit = null)) },
            "account_deleted" to { api.deleteAccount("reason") },
            "payment_reported" to {
                api.createPayment(
                    CreatePaymentRequest(amount = 10.0, unitId = "u1", method = "PAGO_MOVIL", date = "2026-01-01")
                )
            },
            "vote_cast" to { api.castVote("dec-1", "u1", "q-1") },
            "quote_uploaded" to {
                api.uploadDecisionQuote("dec-1", "u1", "Prov", "100", null, byteArrayOf(1), "q.pdf", "application/pdf")
            },
            "announcement_reaction" to { api.toggleAnnouncementReaction("ann-1") },
        )
        for ((expectedEvent, call) in cases) {
            fake.events.clear()
            call()
            assertEquals(listOf(expectedEvent to emptyMap()), fake.events, "Expected event $expectedEvent")
        }
    }

    @Test fun `a failed business endpoint emits only api_error and not the business event`() = runTest {
        engine.status = HttpStatusCode.InternalServerError
        api.createPayment(CreatePaymentRequest(amount = 10.0, unitId = "u1", method = "PAGO_MOVIL", date = "2026-01-01"))
        assertEquals(listOf("api_error"), fake.events.map { it.first })
    }

    @Test fun `telemetry throwing never breaks a request`() = runTest {
        fake.throwOnCall = true
        engine.status = HttpStatusCode.InternalServerError
        assertEquals(500, api.getBuildings().code)
    }

    @Test fun `a decoding failure records a non-fatal error with endpoint template and returns 500`() = runTest {
        engine.status = HttpStatusCode.OK
        engine.body = "{}" // getBuildings expects a JSON array []
        val res = api.getBuildings()
        assertEquals(500, res.code)
        assertEquals("GET buildings", fake.errors.single().second)
    }

    @Test fun `a decoding failure never sends the response body to telemetry`() = runTest {
        engine.status = HttpStatusCode.OK
        engine.body = """{"full_name":"Juan Secreto"}"""
        api.getBuildings()
        val recorded = fake.errors.single().first
        assertFalse("Juan Secreto" in (recorded.message ?: ""))
        assertFalse("Juan Secreto" in recorded.toString())
    }
}
