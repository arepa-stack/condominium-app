package com.example.condominio.data.remote

import com.example.condominio.data.local.TokenManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * The one place that reacts to an authenticated request answered with 401 (expired or invalid token):
 * drops the token and tells the app (once) so it can go back to login from whatever screen is open.
 */
@OptIn(ExperimentalTime::class)
class SessionExpiry(
    private val tokenManager: TokenManager,
    private val nowSeconds: () -> Long = { Clock.System.now().epochSeconds },
) {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events.asSharedFlow()
    private val lock = Mutex()
    // True once a missing-token 401 has been emitted for this session; reset when a token is seen again.
    private var notifiedWhileTokenMissing = false

    /** The saved token; seeing one again re-arms the missing-token notice for the new session. */
    private suspend fun currentToken(): String? = tokenManager.getToken().also {
        if (it != null) lock.withLock { notifiedWhileTokenMissing = false }
    }

    /**
     * An authenticated request sent with [sentToken] got a 401.
     * - If the token was already gone from storage ([sentToken] == null), emits once per session so deleting
     *   `jwt_token` in DevTools or another tab still returns the user to login.
     * - If a token was sent, ignored when it is no longer the saved one (already expired by a sibling request
     *   or replaced by a new login).
     */
    suspend fun onUnauthorized(sentToken: String?) {
        lock.withLock {
            val saved = tokenManager.getToken()
            if (sentToken == null) {
                if (saved != null || notifiedWhileTokenMissing) return
                notifiedWhileTokenMissing = true
                _events.tryEmit(Unit)
                return
            }
            if (saved != sentToken) return
            tokenManager.clearToken()
            notifiedWhileTokenMissing = true
            _events.tryEmit(Unit)
        }
    }

    /**
     * The token to send, or null when there is none or it is malformed or past its `exp` (the server would answer 401):
     * then the session is expired right away, as with a 401, without sending anything. Also checked before opening an
     * authenticated screen so it never gets drawn.
     */
    suspend fun usableToken(): String? {
        val token = currentToken()
        if (token != null && isUsable(token)) return token
        onUnauthorized(token)
        return null
    }

    /** Usable unless the JWT payload can't be read or its `exp` (seconds) has passed; no `exp` → the server decides. */
    private fun isUsable(token: String): Boolean = runCatching {
        val payload = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL).decode(token.split('.')[1])
        val exp = Json.parseToJsonElement(payload.decodeToString()).jsonObject["exp"] ?: return true
        exp.jsonPrimitive.double > nowSeconds()
    }.getOrDefault(false)
}
