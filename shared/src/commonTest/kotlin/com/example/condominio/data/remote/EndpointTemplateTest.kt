package com.example.condominio.data.remote

import kotlin.test.Test
import kotlin.test.assertEquals

class EndpointTemplateTest {
    @Test fun `replaces UUID segment and strips api v1 app prefix`() {
        assertEquals(
            "payments/{id}",
            endpointTemplate("/api/v1/app/payments/550e8400-e29b-41d4-a716-446655440000")
        )
    }

    @Test fun `replaces nested resource ids`() {
        assertEquals(
            "decisions/decisions/{id}/quotes/{id}",
            endpointTemplate("/api/v1/app/decisions/decisions/dec-123/quotes/98765")
        )
    }

    @Test fun `replaces building code after by-code even when lowercase hyphenated`() {
        assertEquals(
            "buildings/by-code/{id}",
            endpointTemplate("/api/v1/app/buildings/by-code/COND-AB12")
        )
        assertEquals(
            "buildings/by-code/{id}/units",
            endpointTemplate("/api/v1/app/buildings/by-code/torre-norte/units")
        )
    }

    @Test fun `preserves static auth and app routes and version segments`() {
        assertEquals("auth/login", endpointTemplate("/auth/login"))
        assertEquals("auth/change-password-first-login", endpointTemplate("auth/change-password-first-login"))
        assertEquals("registration-requests", endpointTemplate("/registration-requests"))
        assertEquals("users/me/password", endpointTemplate("/api/v1/app/users/me/password"))
        assertEquals("api/v2/status", endpointTemplate("/api/v2/status"))
    }
}
