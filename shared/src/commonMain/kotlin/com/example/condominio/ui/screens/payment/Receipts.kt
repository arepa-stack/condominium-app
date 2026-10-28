package com.example.condominio.ui.screens.payment

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.condominio.data.model.Invoice
import com.example.condominio.data.model.Payment
import com.example.condominio.data.model.PaymentStatus
import com.example.condominio.data.repository.AuthRepository
import com.example.condominio.ui.screens.billing.invoiceStatusLabel
import com.example.condominio.ui.utils.formatBs
import com.example.condominio.ui.utils.formatCurrency
import com.example.condominio.ui.utils.invoiceTitle
import com.example.condominio.ui.utils.receiptHtml
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.time.Clock

/** "Building · Unit" of the signed-in resident, the receipt's first line. */
@Composable
private fun receiptHeader(): String {
    val user by koinInject<AuthRepository>().currentUser.collectAsState(null)
    val unit = user?.currentUnit ?: return "Apto"
    return listOf(unit.buildingName, unit.unitName).filter { it.isNotBlank() }.joinToString(" · ")
}

@Composable
private fun receiptFooter(): String =
    stringResource(Res.string.receipt_generated_on, formatPaymentDate(Clock.System.now().toEpochMilliseconds()))

@Composable
private fun usd(amount: Double): String = stringResource(Res.string.currency_amount, formatCurrency(amount))

/** Printable invoice: amounts, status and the approved payments applied to it. */
@Composable
internal fun invoiceReceiptHtml(invoice: Invoice, payments: List<Payment>): String {
    val rows = buildList {
        add(stringResource(Res.string.web_invoice_detail_period) to invoiceMonthLabel(invoice.period))
        invoice.dueDate?.let { add(stringResource(Res.string.web_invoice_detail_due_date) to formatPaymentDate(it)) }
        add(stringResource(Res.string.web_invoice_detail_status) to invoiceStatusLabel(invoice.status))
        add(stringResource(Res.string.total_label) to usd(invoice.amount))
        add(stringResource(Res.string.paid_label) to usd(invoice.paid))
        add(stringResource(Res.string.remaining_label) to usd(invoice.remaining))
    }
    val table = listOf(
        listOf(
            stringResource(Res.string.date_label),
            stringResource(Res.string.method_label),
            stringResource(Res.string.reference_label),
            stringResource(Res.string.web_invoice_detail_applied)
        )
    ) + payments.filter { it.status == PaymentStatus.APPROVED }.map { p ->
        val applied = p.allocations.find { it.invoiceId == invoice.id }?.amount ?: p.amount
        listOf(formatPaymentDate(p.date), paymentMethodLabel(p.method), p.reference ?: "—", usd(applied))
    }
    return receiptHtml(
        header = receiptHeader(),
        title = invoiceTitle(invoice.description) ?: stringResource(Res.string.invoice_period_label, invoiceMonthLabel(invoice.period)),
        rows = rows,
        table = table,
        footer = receiptFooter()
    )
}

/** Printable payment receipt: same rows as the detail screen, plus amount and status. */
@Composable
internal fun paymentReceiptHtml(payment: Payment): String {
    val status = when (payment.status) {
        PaymentStatus.APPROVED -> stringResource(Res.string.status_approved)
        PaymentStatus.REJECTED -> stringResource(Res.string.status_rejected)
        PaymentStatus.PENDING -> stringResource(Res.string.status_pending)
    }
    val rows = buildList {
        add(stringResource(Res.string.transaction_id) to "#TXN-${payment.id.take(8)}")
        add(stringResource(Res.string.receipt_amount) to usd(payment.amount))
        payment.amountBs?.let { add(stringResource(Res.string.amount_in_bs) to stringResource(Res.string.bs_amount, formatBs(it))) }
        payment.exchangeRate?.let {
            add(stringResource(Res.string.exchange_rate_label) to stringResource(Res.string.exchange_rate_value, formatBs(it), bcvDateLabel(payment.rateDate)))
        }
        add(stringResource(Res.string.date_label) to formatPaymentDate(payment.date))
        add(stringResource(Res.string.method_label) to paymentMethodLabel(payment.method))
        add(stringResource(Res.string.bank_label) to (payment.bank ?: "—"))
        add(stringResource(Res.string.reference_label) to (payment.reference ?: "—"))
        payment.userName?.let { add(stringResource(Res.string.paid_by_label) to it) }
        add(stringResource(Res.string.web_invoice_detail_status) to status)
        payment.processorName?.let { add(stringResource(Res.string.processed_by_label) to it) }
    }
    return receiptHtml(
        header = receiptHeader(),
        title = stringResource(Res.string.payment_receipt_title),
        rows = rows,
        footer = receiptFooter()
    )
}
