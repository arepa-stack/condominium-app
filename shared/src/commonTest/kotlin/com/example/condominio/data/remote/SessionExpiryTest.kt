package com.example.condominio.data.remote

import com.example.condominio.testutil.FakeTokenManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.callContext
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.test.TestScope
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.test.*

private const val NOW = 1_700_000_000L
private fun b64(s: String) = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(s.encodeToByteArray())
/** header.payload.signature like the backend's (Supabase) tokens; the signature is never checked by the app. */
private fun jwt(payload: String) = "${b64("""{"alg":"HS256","typ":"JWT"}""")}.${b64(payload)}.signature"
private val SAVED = jwt("""{"sub":"u1","exp":${NOW + 3600}}""")
private val NEW = jwt("""{"sub":"u1","exp":${NOW + 7200}}""")

/** Answers every request with [status], or fails like a dropped connection when [offline]. */
@OptIn(InternalAPI::class)
private class StatusEngine(var status: HttpStatusCode = HttpStatusCode.OK, var offline: Boolean = false) :
    HttpClientEngineBase("status-engine") {
    override val config = HttpClientEngineConfig()
    override val dispatcher: CoroutineDispatcher = Dispatchers.Unconfined

    var calls = 0

    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        calls++
        if (offline) throw Exception("Fail to fetch")
        return HttpResponseData(
            status, GMTDate(), headersOf("Content-Type", "application/json"),
            HttpProtocolVersion.HTTP_1_1, ByteReadChannel("{}"), callContext()
        )
    }
}

class SessionExpiryTest {
    private val tokens = FakeTokenManager(SAVED)
    private val expiry = SessionExpiry(tokens, nowSeconds = { NOW })
    private val engine = StatusEngine()
    private val api = ApiServiceImpl(
        HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }) }
            defaultRequest { url("https://api.test/") }
        },
        expiry
    )

    private fun TestScope.collectEvents(into: MutableList<Unit>): Job =
        launch(UnconfinedTestDispatcher(testScheduler)) { expiry.events.collect { into += it } }

    @Test fun `a 401 on an authenticated request clears the token and emits one expiry event`() = runTest {
        engine.status = HttpStatusCode.Unauthorized
        val events = mutableListOf<Unit>()
        val job = collectEvents(events)
        assertEquals(401, api.getBuildings().code)
        assertNull(tokens.token)
        assertEquals(1, events.size)
        job.cancel()
    }

    @Test fun `several requests failing with 401 together emit a single expiry event`() = runTest {
        engine.status = HttpStatusCode.Unauthorized
        val events = mutableListOf<Unit>()
        val job = collectEvents(events)
        api.getBuildings(); api.getPayments(null, null); api.getExchangeRates()
        assertEquals(1, events.size)
        job.cancel()
    }

    @Test fun `a 401 from login is wrong credentials and keeps the token`() = runTest {
        engine.status = HttpStatusCode.Unauthorized
        val events = mutableListOf<Unit>()
        val job = collectEvents(events)
        assertEquals(401, api.login(mapOf("email" to "a@b.c", "password" to "x")).code)
        assertEquals(SAVED, tokens.token)
        assertTrue(events.isEmpty())
        job.cancel()
    }

    @Test fun `a 401 when the token was already deleted from storage still emits one expiry event`() = runTest {
        tokens.token = null
        engine.status = HttpStatusCode.Unauthorized
        val events = mutableListOf<Unit>()
        val job = collectEvents(events)
        api.getBuildings(); api.getPayments(null, null); api.getExchangeRates()
        assertEquals(1, events.size)
        job.cancel()
    }

    @Test fun `a new login resets expiry so a later deleted token expires the new session too`() = runTest {
        tokens.token = null
        engine.status = HttpStatusCode.Unauthorized
        val events = mutableListOf<Unit>()
        val job = collectEvents(events)
        api.getBuildings()
        assertEquals(1, events.size)

        // User logs in again (saves a token and makes an authenticated request, or token is present then deleted)
        tokens.token = NEW
        engine.status = HttpStatusCode.OK
        api.getBuildings()

        // Token is deleted again in the new session
        tokens.token = null
        engine.status = HttpStatusCode.Unauthorized
        api.getBuildings(); api.getPayments(null, null)
        assertEquals(2, events.size)
        job.cancel()
    }

    @Test fun `a 401 with a token followed by a 401 after token is cleared emits a single expiry event`() = runTest {
        engine.status = HttpStatusCode.Unauthorized
        val events = mutableListOf<Unit>()
        val job = collectEvents(events)
        api.getBuildings() // sentToken = SAVED, clears token and emits 1st event
        api.getPayments(null, null) // sentToken = null (already cleared), must not emit a 2nd event
        assertEquals(1, events.size)
        job.cancel()
    }

    @Test fun `public registration endpoints do not emit expiry on 401`() = runTest {
        engine.status = HttpStatusCode.Unauthorized
        val events = mutableListOf<Unit>()
        val job = collectEvents(events)
        api.getBuildingByCode("CODE1")
        api.getBuildingUnitsByCode("CODE1")
        assertEquals(SAVED, tokens.token)
        assertTrue(events.isEmpty())
        job.cancel()
    }

    @Test fun `a 500 keeps the session`() = runTest {
        engine.status = HttpStatusCode.InternalServerError
        val events = mutableListOf<Unit>()
        val job = collectEvents(events)
        assertEquals(500, api.getBuildings().code)
        assertEquals(SAVED, tokens.token)
        assertTrue(events.isEmpty())
        job.cancel()
    }

    @Test fun `a network failure keeps the session`() = runTest {
        engine.offline = true
        val events = mutableListOf<Unit>()
        val job = collectEvents(events)
        assertTrue(api.getBuildings().isNetworkError)
        assertEquals(SAVED, tokens.token)
        assertTrue(events.isEmpty())
        job.cancel()
    }

    @Test fun `a late 401 for an old token does not log out the new session`() = runTest {
        val events = mutableListOf<Unit>()
        val job = collectEvents(events)
        tokens.token = "new-token"
        expiry.onUnauthorized("old-token")
        assertEquals("new-token", tokens.token)
        assertTrue(events.isEmpty())
        job.cancel()
    }

    @Test fun `a late 401 sent without token does not log out a session saved meanwhile`() = runTest {
        val events = mutableListOf<Unit>()
        val job = collectEvents(events)
        tokens.token = "new-token"
        expiry.onUnauthorized(null)
        assertEquals("new-token", tokens.token)
        assertTrue(events.isEmpty())
        job.cancel()
    }

    // A + C: what the server would reject anyway is caught before sending, so no request and no 1.5 s wait.

    @Test fun `an authenticated request without a token is not sent and emits one expiry event`() = runTest {
        tokens.token = null
        val events = mutableListOf<Unit>()
        val job = collectEvents(events)
        assertEquals(401, api.getBuildings().code)
        assertEquals(0, engine.calls)
        assertEquals(1, events.size)
        job.cancel()
    }

    @Test fun `a token whose exp has passed is not sent, is cleared and emits one expiry event`() = runTest {
        tokens.token = jwt("""{"sub":"u1","exp":${NOW - 1}}""")
        val events = mutableListOf<Unit>()
        val job = collectEvents(events)
        assertEquals(401, api.getBuildings().code)
        assertEquals(0, engine.calls)
        assertNull(tokens.token)
        assertEquals(1, events.size)
        job.cancel()
    }

    @Test fun `a token whose exp is in the future is sent`() = runTest {
        val events = mutableListOf<Unit>()
        val job = collectEvents(events)
        api.getBuildings()
        assertEquals(1, engine.calls)
        assertEquals(SAVED, tokens.token)
        assertTrue(events.isEmpty())
        job.cancel()
    }

    @Test fun `a malformed token is treated as expired`() = runTest {
        tokens.token = "x"
        val events = mutableListOf<Unit>()
        val job = collectEvents(events)
        assertEquals(401, api.getBuildings().code)
        assertEquals(0, engine.calls)
        assertNull(tokens.token)
        assertEquals(1, events.size)
        job.cancel()
    }

    @Test fun `a token without exp is sent and left to the server`() = runTest {
        tokens.token = jwt("""{"sub":"u1"}""")
        val events = mutableListOf<Unit>()
        val job = collectEvents(events)
        api.getBuildings()
        assertEquals(1, engine.calls)
        assertTrue(events.isEmpty())
        job.cancel()
    }

    @Test fun `a public request without a token is sent and does not emit`() = runTest {
        tokens.token = null
        val events = mutableListOf<Unit>()
        val job = collectEvents(events)
        api.getBuildingByCode("CODE1")
        assertEquals(1, engine.calls)
        assertTrue(events.isEmpty())
        job.cancel()
    }
}
