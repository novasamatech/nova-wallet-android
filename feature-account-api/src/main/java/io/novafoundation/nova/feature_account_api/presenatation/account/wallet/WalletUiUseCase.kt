package io.novafoundation.nova.feature_account_api.presenatation.account.wallet

import android.graphics.drawable.Drawable
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import kotlinx.coroutines.flow.Flow

class WalletModel(val metaId: Long, val name: String, val icon: Drawable?) {

    override fun equals(other: Any?): Boolean {
        return other is WalletModel && metaId == other.metaId && name == other.name
    }
}

interface WalletUiUseCase {

    fun selectedWalletUiFlow(showAddressIcon: Boolean = false): Flow<WalletModel>

    fun walletUiFlow(metaId: Long, showAddressIcon: Boolean = false): Flow<WalletModel>

    suspend fun selectedWalletUi(): WalletModel

    suspend fun walletIcon(metaAccount: MetaAccount, transparentBackground: Boolean = true): Drawable

    suspend fun walletUiFor(metaAccount: MetaAccount): WalletModel
}
