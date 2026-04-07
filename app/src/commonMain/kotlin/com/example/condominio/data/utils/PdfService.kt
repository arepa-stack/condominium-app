package com.example.condominio.data.utils

import com.example.condominio.data.model.Payment

interface PdfService {
    /**
     * Generates a receipt PDF for the given payment and returns the platform-specific file path.
     */
    fun generateReceipt(payment: Payment): String
}
