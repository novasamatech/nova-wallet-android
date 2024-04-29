package io.novafoundation.nova.feature_account_api.presenatation.account.wallet

import android.graphics.drawable.Drawable
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novasama.substrate_sdk_android.runtime.AccountId
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

    suspend fun walletIcon(
        substrateAccountId: AccountId?,
        ethereumAccountId: AccountId?,
        chainAccountIds: List<AccountId>,
        iconSize: Int = AddressIconGenerator.SIZE_MEDIUM,
        transparentBackground: Boolean = true
    ): Drawable

    suspend fun walletIcon(metaAccount: MetaAccount, iconSize: Int = AddressIconGenerator.SIZE_MEDIUM, transparentBackground: Boolean = true): Drawable

    suspend fun walletUiFor(metaAccount: MetaAccount): WalletModel
}
