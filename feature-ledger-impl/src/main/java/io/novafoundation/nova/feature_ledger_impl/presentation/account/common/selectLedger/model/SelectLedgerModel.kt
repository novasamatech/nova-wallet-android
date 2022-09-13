package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.model

data class SelectLedgerModel(
    val name: String,
    val id: String,
    val isConnecting: Boolean,
)
