package com.example.condominio.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RoutesTest {
    @Test fun `a plain building code goes into the route as is`() {
        assertEquals("register_form/COND-1234", registerFormRoute("COND-1234"))
    }

    @Test fun `slashes and spaces in a code cannot break the route`() {
        val route = registerFormRoute("a/b c")
        assertEquals("register_form/", route.substring(0, "register_form/".length))
        assertFalse(route.removePrefix("register_form/").contains('/'))
        assertFalse(route.contains(' '))
    }

    @Test fun `query and brace characters are encoded`() {
        val arg = registerFormRoute("x?y={z}#").removePrefix("register_form/")
        assertFalse(arg.any { it in "?{}#" })
    }
}
