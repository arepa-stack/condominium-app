package com.example.condominio.data.remote

class Response<T>(
    val bodyValue: T?,
    val isSuccessful: Boolean,
    val code: Int,
    val errorBodyString: String? = null
) {
    fun body(): T? = bodyValue
    fun errorBody() = this
    fun string() = errorBodyString ?: ""

    companion object {
        fun <T> success(body: T?): Response<T> = Response(body, true, 200)
        fun <T> error(code: Int, errorBody: String): Response<T> = Response(null, false, code, errorBody)
    }
}
