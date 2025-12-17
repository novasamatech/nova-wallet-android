package io.novafoundation.nova.feature_account_api.presenatation.mixin.common

import io.novafoundation.nova.feature_account_api.domain.filter.selectAddress.SelectAccountFilter

fun SelectWalletFilterPayload.toMetaAccountsFilter(): SelectAccountFilter {
    return when (this) {
        is SelectWalletFilterPayload.Everything -> SelectAccountFilter.Everything()

        is SelectWalletFilterPayload.ControllableWallets -> SelectAccountFilter.ControllableWallets()

        is SelectWalletFilterPayload.ExcludeMetaIds -> SelectAccountFilter.ExcludeMetaAccounts(this.metaIds)
    }
}

fun SelectAccountFilter.toRequestFilter(): SelectWalletFilterPayload {
    return when (this) {
        is SelectAccountFilter.Everything -> SelectWalletFilterPayload.Everything

        is SelectAccountFilter.ControllableWallets -> SelectWalletFilterPayload.ControllableWallets

        is SelectAccountFilter.ExcludeMetaAccounts -> SelectWalletFilterPayload.ExcludeMetaIds(this.metaIds)
    }
}
