package com.example.condominio

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HelloTest {
    @Test
    fun runTest_works() = runTest {
        assertEquals(2, 1 + 1)
    }
}
