package io.novafoundation.nova.feature_account_api.data.ethereum.transaction

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import jp.co.soramitsu.fearless_utils.runtime.AccountId

sealed class TransactionOrigin {

    object SelectedWallet : TransactionOrigin()

    class WalletWithAccount(val accountId: AccountId) : TransactionOrigin()

    class Wallet(val metaAccount: MetaAccount): TransactionOrigin()
}

fun AccountId.intoOrigin(): TransactionOrigin = TransactionOrigin.WalletWithAccount(this)

fun MetaAccount.intoOrigin(): TransactionOrigin = TransactionOrigin.Wallet(this)
