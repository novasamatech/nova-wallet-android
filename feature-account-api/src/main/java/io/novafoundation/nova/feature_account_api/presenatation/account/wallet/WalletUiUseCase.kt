package io.novafoundation.nova.feature_account_api.presenatation.account.wallet

import android.graphics.drawable.Drawable
import kotlinx.coroutines.flow.Flow

class WalletModel(val name: String, val icon: Drawable?)

interface WalletUiUseCase {

    fun selectedWalletUiFlow(showAddressIcon: Boolean = false): Flow<WalletModel>
}
