package com.example.condominio.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Decides when lifecycle events mean "the app came back from background": only an ON_START preceded by an
 * ON_STOP seen by this same instance. A late ON_START/ON_RESUME without a previous ON_STOP (web) is ignored.
 */
class BackgroundReturnDetector {
    private var stopped = false

    /** Feed every lifecycle event; true when this one is a return from background. */
    fun onEvent(event: Lifecycle.Event): Boolean = when (event) {
        Lifecycle.Event.ON_STOP -> { stopped = true; false }
        Lifecycle.Event.ON_START -> stopped.also { stopped = false }
        else -> false
    }
}

/**
 * Reports the two ways a screen may be showing again, without relying on timing (A-19):
 * - [onReenter] each time it enters composition: opening, coming back from another destination (NavHost
 *   disposes the screen while covered) and also recreation (rotation). The ViewModel decides if it is a real
 *   return (it only reloads after its own `onOpenChild()`).
 * - [onBackgroundReturn] on ON_START only after an ON_STOP seen by this same observer: the app back from
 *   background. A recreated Activity (rotation) registers a new observer, so it never counts.
 */
@Composable
fun OnScreenReturn(onReenter: () -> Unit, onBackgroundReturn: () -> Unit) {
    val currentOnReturn by rememberUpdatedState(onReenter)
    val currentOnBackground by rememberUpdatedState(onBackgroundReturn)
    LaunchedEffect(Unit) { currentOnReturn() }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val detector = BackgroundReturnDetector()
        val observer = LifecycleEventObserver { _, event ->
            if (detector.onEvent(event)) currentOnBackground()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
