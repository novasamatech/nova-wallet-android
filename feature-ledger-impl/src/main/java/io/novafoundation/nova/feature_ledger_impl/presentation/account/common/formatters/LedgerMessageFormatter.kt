package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters

interface LedgerMessageFormatter {

    suspend fun appName(): String
}
