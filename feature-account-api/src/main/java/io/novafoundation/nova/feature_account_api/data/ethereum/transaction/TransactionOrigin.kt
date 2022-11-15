package io.novafoundation.nova.feature_account_api.data.ethereum.transaction

sealed class TransactionOrigin {

    object SelectedWallet: TransactionOrigin()
}
