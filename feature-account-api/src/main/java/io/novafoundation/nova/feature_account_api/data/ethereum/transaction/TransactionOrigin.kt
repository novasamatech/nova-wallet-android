package io.novafoundation.nova.feature_account_api.data.ethereum.transaction

import jp.co.soramitsu.fearless_utils.runtime.AccountId

sealed class TransactionOrigin {

    object SelectedWallet : TransactionOrigin()

    class WalletWithAccount(val accountId: AccountId) : TransactionOrigin()
}
