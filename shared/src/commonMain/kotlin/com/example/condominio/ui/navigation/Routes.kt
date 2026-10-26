package com.example.condominio.ui.navigation

import io.ktor.http.encodeURLParameter

/**
 * Route to the registration form for a building code. The code comes from a QR or is typed by hand,
 * so it is encoded: a `/`, `?` or `{` in it must not change which route matches (E-L5).
 */
fun registerFormRoute(code: String): String = "register_form/${code.encodeURLParameter()}"
