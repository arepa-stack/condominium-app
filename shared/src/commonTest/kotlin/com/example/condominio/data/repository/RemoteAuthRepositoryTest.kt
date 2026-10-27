package com.example.condominio.data.repository

import com.example.condominio.data.model.SessionExpiredException
import com.example.condominio.data.model.User
import com.example.condominio.data.model.UserUnit
import com.example.condominio.data.remote.ApiFailureException
import com.example.condominio.data.remote.NetworkFailureException
import com.example.condominio.data.remote.Response
import kotlinx.coroutines.flow.first
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

    // --- Perfil (paso 8) ---

    @Test fun `updating the profile sends only the name`() = runTest {
        repo().updateUser(User(id = "1", name = "Ana Maria", email = "a@b.c", units = listOf(UserUnit("u1", "b1", "1A", "B"))))
        val sent = api.updateRequests.single()
        assertEquals("Ana Maria", sent.name)
        assertNull(sent.unit)
        assertNull(sent.phone)
    }

    @Test fun `an updated name shows in the current user without asking the server`() = runTest {
        val repo = repo()
        repo.fetchCurrentUser()
        repo.updateUser(User(id = "1", name = "Ana Maria", email = "a@b.c"))
        assertEquals("Ana Maria", repo.currentUser.first()?.name)
    }

    @Test fun `a failed update leaves the current user as it was`() = runTest {
        val repo = repo()
        repo.fetchCurrentUser()
        api.updateUserResponse = Response.error(500, "x")
        val result = repo.updateUser(User(id = "1", name = "Ana Maria", email = "a@b.c"))
        assertIs<ApiFailureException>(result.exceptionOrNull())
        assertEquals("Ana", repo.currentUser.first()?.name)
    }

    @Test fun `an update without network is a network failure`() = runTest {
        api.updateUserResponse = Response.networkError("Fail to fetch")
        val result = repo().updateUser(User(id = "1", name = "Ana", email = "a@b.c"))
        assertIs<NetworkFailureException>(result.exceptionOrNull())
    }

    @Test fun `a rejected password change is typed with its code`() = runTest {
        api.changePasswordResponse = Response.error(400, "x")
        val result = repo().changePassword("a", "b")
        assertEquals(400, assertIs<ApiFailureException>(result.exceptionOrNull()).code)
    }

    @Test fun `a failed account deletion is typed and keeps the session`() = runTest {
        api.deleteAccountResponse = Response.error(500, "x")
        val result = repo().deleteAccount(null)
        assertIs<ApiFailureException>(result.exceptionOrNull())
        assertEquals("saved-token", tokens.token)
    }
}
