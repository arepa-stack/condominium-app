package com.example.condominio.ui.utils

import com.example.condominio.data.model.UserPendingException
import com.example.condominio.data.remote.ApiFailureException
import com.example.condominio.data.remote.NetworkFailureException
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource

/** Which auth form failed; the same HTTP code means different things in each (A-24). */
enum class AuthAction { Login, ResetPassword, ChangePassword, Register, UpdateProfile, DeleteAccount }

/**
 * Text for a failed auth request. Never the repository's or the server's own message: those are
 * English/technical. Network failure → connection text; known HTTP codes → a specific text; the rest → generic.
 */
fun authErrorText(error: Throwable, action: AuthAction): UiText = UiText.StringResource(authErrorRes(error, action))

private fun authErrorRes(error: Throwable, action: AuthAction): StringResource = when (error) {
    is UserPendingException -> Res.string.error_account_pending
    is NetworkFailureException -> Res.string.error_check_connection
    is ApiFailureException -> when (action) {
        AuthAction.Login -> when (error.code) {
            400, 401, 403 -> Res.string.error_invalid_credentials
            else -> Res.string.error_unknown
        }
        AuthAction.ResetPassword -> when (error.code) {
            400, 422 -> Res.string.error_invalid_email
            else -> Res.string.error_unknown
        }
        AuthAction.ChangePassword -> when (error.code) {
            400, 422 -> Res.string.first_login_password_policy
            401 -> Res.string.error_session_expired
            else -> Res.string.error_unknown
        }
        AuthAction.UpdateProfile, AuthAction.DeleteAccount -> when (error.code) {
            401 -> Res.string.error_session_expired
            else -> Res.string.error_unknown
        }
        AuthAction.Register -> when (error.code) {
            409 -> Res.string.error_registration_conflict
            404 -> Res.string.qr_invalid_code
            400, 422 -> Res.string.error_registration_invalid
            else -> Res.string.error_unknown
        }
    }
    else -> Res.string.error_unknown
}
