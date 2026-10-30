package com.example.condominio

import android.content.Context
import android.os.Bundle
import com.example.condominio.telemetry.Telemetry
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

class FirebaseTelemetry(context: Context) : Telemetry {
    private val analytics = FirebaseAnalytics.getInstance(context)
    private val crashlytics = FirebaseCrashlytics.getInstance()

    init {
        try {
            analytics.setUserProperty("app_env", ApiConfig.ENV)
            crashlytics.setCustomKey("app_env", ApiConfig.ENV)
        } catch (_: Throwable) {}
    }

    override fun screen(name: String) {
        try {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, name)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, name)
            }
            analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
        } catch (_: Throwable) {}
    }

    override fun event(name: String, params: Map<String, Any>) {
        try {
            val bundle = Bundle().apply {
                for ((k, v) in params) {
                    when (v) {
                        is String -> putString(k, v)
                        is Int -> putLong(k, v.toLong())
                        is Long -> putLong(k, v)
                        is Double -> putDouble(k, v)
                        is Boolean -> putString(k, v.toString())
                    }
                }
            }
            analytics.logEvent(name, bundle)
        } catch (_: Throwable) {}
    }

    override fun recordError(throwable: Throwable, where: String) {
        try {
            crashlytics.setCustomKey("where", where)
            crashlytics.recordException(throwable)
        } catch (_: Throwable) {}
    }

    override fun setTenant(buildingId: String?) {
        try {
            analytics.setUserProperty("building_id", buildingId)
            crashlytics.setCustomKey("building_id", buildingId ?: "")
        } catch (_: Throwable) {}
    }
}
