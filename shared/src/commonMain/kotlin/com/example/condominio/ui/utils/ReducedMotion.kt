package com.example.condominio.ui.utils

import androidx.compose.runtime.Composable

/**
 * True when the user asked the OS to reduce motion (Android: animations off; iOS: Reduce Motion;
 * web: `prefers-reduced-motion: reduce`). Decorative/looping animations should stay static then.
 */
@Composable
expect fun rememberReducedMotion(): Boolean
