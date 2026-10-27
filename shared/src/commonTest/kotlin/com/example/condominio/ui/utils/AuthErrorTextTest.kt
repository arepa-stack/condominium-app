package com.example.condominio.ui.utils

import com.example.condominio.data.model.UserPendingException
import com.example.condominio.data.remote.ApiFailureException
import com.example.condominio.data.remote.NetworkFailureException
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AuthErrorTextTest {
    private fun res(e: Throwable, a: AuthAction): StringResource =
        assertIs<UiText.StringResource>(authErrorText(e, a)).res

    @Test fun `login 401 400 and 403 are invalid credentials`() {
        listOf(400, 401, 403).forEach {
            assertEquals(Res.string.error_invalid_credentials, res(ApiFailureException(it), AuthAction.Login))
        }
    }

    @Test fun `login server error is generic not invalid credentials`() {
        assertEquals(Res.string.error_unknown, res(ApiFailureException(500), AuthAction.Login))
    }

    @Test fun `network failure shows the connection text in every form`() {
        AuthAction.entries.forEach {
            assertEquals(Res.string.error_check_connection, res(NetworkFailureException("Fail to fetch"), it))
        }
    }

    @Test fun `profile update and account deletion have their own codes`() {
        assertEquals(Res.string.error_session_expired, res(ApiFailureException(401), AuthAction.UpdateProfile))
        assertEquals(Res.string.error_unknown, res(ApiFailureException(500), AuthAction.UpdateProfile))
        assertEquals(Res.string.error_session_expired, res(ApiFailureException(401), AuthAction.DeleteAccount))
        assertEquals(Res.string.error_unknown, res(ApiFailureException(400), AuthAction.DeleteAccount))
    }

    @Test fun `a pending account has its own text`() {
        assertEquals(Res.string.error_account_pending, res(UserPendingException("Your account is pending approval"), AuthAction.Login))
    }

    @Test fun `register conflict not found and invalid map to their own texts`() {
        assertEquals(Res.string.error_registration_conflict, res(ApiFailureException(409), AuthAction.Register))
        assertEquals(Res.string.qr_invalid_code, res(ApiFailureException(404), AuthAction.Register))
        assertEquals(Res.string.error_registration_invalid, res(ApiFailureException(400), AuthAction.Register))
    }

    @Test fun `reset password 400 is an invalid email`() {
        assertEquals(Res.string.error_invalid_email, res(ApiFailureException(400), AuthAction.ResetPassword))
    }

    @Test fun `change password 400 is the policy and 401 an expired session`() {
        assertEquals(Res.string.first_login_password_policy, res(ApiFailureException(400), AuthAction.ChangePassword))
        assertEquals(Res.string.error_session_expired, res(ApiFailureException(401), AuthAction.ChangePassword))
    }

    @Test fun `anything else is generic and never the raw message`() {
        assertEquals(Res.string.error_unknown, res(Exception("Login failed (401): Error from server"), AuthAction.Login))
        assertEquals(Res.string.error_unknown, res(Exception(), AuthAction.Register))
    }
}
