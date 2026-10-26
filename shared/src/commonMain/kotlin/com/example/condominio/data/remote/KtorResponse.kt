package com.example.condominio.data.remote

class Response<T>(
    val bodyValue: T?,
    val isSuccessful: Boolean,
    val code: Int,
    val errorBodyString: String? = null,
    /** The request never got an HTTP answer (offline, DNS, `Fail to fetch` on wasm…). */
    val isNetworkError: Boolean = false
) {
    fun body(): T? = bodyValue
    fun errorBody() = this
    fun string() = errorBodyString ?: ""

    companion object {
        fun <T> success(body: T?): Response<T> = Response(body, true, 200)
        fun <T> error(code: Int, errorBody: String): Response<T> = Response(null, false, code, errorBody)
        fun <T> networkError(message: String): Response<T> = Response(null, false, 0, message, isNetworkError = true)
    }
}

/** The server answered with an HTTP error. Screens map [code] to their own text; this message is never shown. */
class ApiFailureException(val code: Int) : Exception("HTTP $code")

/** The request did not reach the server. */
class NetworkFailureException(detail: String? = null) : Exception(detail ?: "Network error")

/** The failure to put in a `Result` for an unsuccessful response: typed, so screens never show server text. */
fun <T> Response<T>.toFailure(): Throwable =
    if (isNetworkError) NetworkFailureException(errorBodyString) else ApiFailureException(code)
