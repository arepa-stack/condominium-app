package com.example.condominio.testutil

import com.example.condominio.telemetry.Telemetry

class FakeTelemetry(var throwOnCall: Boolean = false) : Telemetry {
    val screens = mutableListOf<String>()
    val events = mutableListOf<Pair<String, Map<String, Any>>>()
    val errors = mutableListOf<Pair<Throwable, String>>()

    override fun screen(name: String) {
        if (throwOnCall) error("telemetry failed")
        screens += name
    }

    override fun event(name: String, params: Map<String, Any>) {
        if (throwOnCall) error("telemetry failed")
        events += name to params
    }

    override fun recordError(throwable: Throwable, where: String) {
        if (throwOnCall) error("telemetry failed")
        errors += throwable to where
    }
}
