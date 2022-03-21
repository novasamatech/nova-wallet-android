package io.novafoundation.nova.feature_account_api.presenatation.account.wallet

import kotlinx.coroutines.flow.Flow


class WalletModel(val name: String)

interface WalletUiUseCase {

    fun selectedWalletUiFlow(): Flow<WalletModel>
}
