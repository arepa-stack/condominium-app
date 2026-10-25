package com.example.condominio.data.remote

import kotlinx.coroutines.CancellationException

/**
 * Network calls catch [Throwable], not just [Exception]: on wasm a real network failure is thrown as
 * `kotlin.Error: Fail to fetch`, which `catch (e: Exception)` lets through, killing the coroutine and
 * leaving the screen on an endless spinner (A-18). Coroutine cancellation must still propagate.
 */
fun Throwable.rethrowIfCancellation() {
    if (this is CancellationException) throw this
}
