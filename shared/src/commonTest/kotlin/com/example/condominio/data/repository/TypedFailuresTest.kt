package com.example.condominio.data.repository

import com.example.condominio.data.remote.ApiFailureException
import com.example.condominio.data.remote.NetworkFailureException
import com.example.condominio.data.remote.Response
import com.example.condominio.testutil.FakeApiService
import com.example.condominio.testutil.FakeTokenManager
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/** Repositories report failures as typed exceptions: the screens, not the server text, decide what to show (A-24). */
class TypedFailuresTest {
    private val api = FakeApiService()
    private val auth = RemoteAuthRepository(api, FakeTokenManager())

    @Test fun `login 401 is an api failure with its code`() = runTest {
        api.loginResponse = Response.error(401, "Error from server: 401 Unauthorized")
        val e = assertIs<ApiFailureException>(auth.login("a@b.c", "x").exceptionOrNull())
        assertEquals(401, e.code)
    }

    @Test fun `login without network is a network failure`() = runTest {
        api.loginResponse = Response.networkError("Fail to fetch")
        assertIs<NetworkFailureException>(auth.login("a@b.c", "x").exceptionOrNull())
    }

    @Test fun `reset password failure is typed`() = runTest {
        api.resetResponse = Response.error(400, "bad")
        assertEquals(400, assertIs<ApiFailureException>(auth.resetPassword("a@b.c").exceptionOrNull()).code)
        api.resetResponse = Response.networkError("down")
        assertIs<NetworkFailureException>(auth.resetPassword("a@b.c").exceptionOrNull())
    }

    @Test fun `first login password change failure is typed`() = runTest {
        api.firstLoginResponse = Response.error(400, "bad")
        assertEquals(400, assertIs<ApiFailureException>(auth.changePasswordFirstLogin("Abcdefg1").exceptionOrNull()).code)
    }

    @Test fun `registration request failure is typed`() = runTest {
        val onboarding = RemoteOnboardingRepository(api)
        api.registrationResponse = Response.error(409, "dup")
        assertEquals(409, assertIs<ApiFailureException>(onboarding.submitRegistrationRequest("C", "u", "e", "n", "l", "d").exceptionOrNull()).code)
        api.registrationResponse = Response.networkError("down")
        assertIs<NetworkFailureException>(onboarding.submitRegistrationRequest("C", "u", "e", "n", "l", "d").exceptionOrNull())
    }

    @Test fun `building lookup by code distinguishes not found from no network`() = runTest {
        val buildings = RemoteBuildingRepository(api)
        api.buildingByCodeResponse = Response.error(404, "nf")
        assertEquals(404, assertIs<ApiFailureException>(buildings.getBuildingByCode("X").exceptionOrNull()).code)
        api.buildingByCodeResponse = Response.networkError("down")
        assertIs<NetworkFailureException>(buildings.getBuildingByCode("X").exceptionOrNull())
    }

    @Test fun `building units lookup by code distinguishes error from no network`() = runTest {
        val buildings = RemoteBuildingRepository(api)
        api.buildingUnitsByCodeResponse = Response.error(500, "err")
        assertEquals(500, assertIs<ApiFailureException>(buildings.getBuildingUnitsByCode("X").exceptionOrNull()).code)
        api.buildingUnitsByCodeResponse = Response.networkError("down")
        assertIs<NetworkFailureException>(buildings.getBuildingUnitsByCode("X").exceptionOrNull())
    }
}
