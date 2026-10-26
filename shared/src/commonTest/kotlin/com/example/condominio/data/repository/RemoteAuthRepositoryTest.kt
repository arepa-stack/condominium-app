package com.example.condominio.data.repository

import com.example.condominio.data.model.SessionExpiredException
import com.example.condominio.data.remote.Response
import com.example.condominio.testutil.FakeApiService
import com.example.condominio.testutil.FakeTokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.TestTimeSource

@OptIn(ExperimentalTime::class)
class RemoteAuthRepositoryTest {
    private val api = FakeApiService()
    private val tokens = FakeTokenManager("saved-token")
    private val time = TestTimeSource()

    private fun repo() = RemoteAuthRepository(api, tokens, time)

    @Test fun `creating the repository with a saved token does not call users me`() = runTest {
        repo()
        // The init used to launch a background fetch on Dispatchers.Default; give it real time to show up.
        withContext(Dispatchers.Default) { delay(150) }
        assertEquals(0, api.currentUserCount)
    }

    @Test fun `a 401 on users me clears the token and reports an expired session`() = runTest {
        api.currentUserResponse = Response.error(401, "Unauthorized")
        val result = repo().fetchCurrentUser()
        assertIs<SessionExpiredException>(result.exceptionOrNull())
        assertNull(tokens.token)
    }

    @Test fun `a server or network failure keeps the token and is not an expired session`() = runTest {
        api.currentUserResponse = Response.error(500, "Network down")
        val result = repo().fetchCurrentUser()
        assertTrue(result.isFailure)
        assertFalse(result.exceptionOrNull() is SessionExpiredException)
        assertEquals("saved-token", tokens.token)
    }

    @Test fun `a second fetch right after the first reuses the user`() = runTest {
        val repo = repo()
        assertTrue(repo.fetchCurrentUser().isSuccess)
        time += 1.seconds
        assertTrue(repo.fetchCurrentUser().isSuccess)
        assertEquals(1, api.currentUserCount)
    }

    @Test fun `a fetch after the window asks the server again`() = runTest {
        val repo = repo()
        repo.fetchCurrentUser()
        time += 30.seconds
        repo.fetchCurrentUser()
        assertEquals(2, api.currentUserCount)
    }

    @Test fun `a failed fetch is never cached`() = runTest {
        val repo = repo()
        api.currentUserResponse = Response.error(500, "down")
        repo.fetchCurrentUser()
        repo.fetchCurrentUser()
        assertEquals(2, api.currentUserCount)
    }

    @Test fun `logout drops the cached user`() = runTest {
        val repo = repo()
        repo.fetchCurrentUser()
        repo.logout()
        repo.fetchCurrentUser()
        assertEquals(2, api.currentUserCount)
    }
}
