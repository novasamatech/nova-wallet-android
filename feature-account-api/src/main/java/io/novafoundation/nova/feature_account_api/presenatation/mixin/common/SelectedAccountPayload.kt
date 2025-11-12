package io.novafoundation.nova.feature_account_api.presenatation.mixin.common

sealed interface SelectedAccountPayload {
    data class MetaAccount(val metaId: Long) : SelectedAccountPayload
    data class Address(val address: String) : SelectedAccountPayload
}
