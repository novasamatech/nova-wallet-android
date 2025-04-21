package io.novafoundation.nova.feature_account_api.data.ethereum.transaction

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novasama.substrate_sdk_android.runtime.AccountId

sealed class TransactionOrigin {

    data object SelectedWallet : TransactionOrigin()

    class WalletWithAccount(val accountId: AccountId) : TransactionOrigin()

    class Wallet(val metaAccount: MetaAccount) : TransactionOrigin()

    class WalletWithId(val metaId: Long) : TransactionOrigin()
}

fun AccountId.intoOrigin(): TransactionOrigin = TransactionOrigin.WalletWithAccount(this)

fun MetaAccount.intoOrigin(): TransactionOrigin = TransactionOrigin.Wallet(this)
