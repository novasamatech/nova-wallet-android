package io.novafoundation.nova.feature_account_api.domain.interfaces

import android.graphics.drawable.Drawable
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.view.TintedIcon
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

class SelectedWalletModel(
    val typeIcon: TintedIcon?,
    val walletIcon: Drawable,
    val name: String,
    val hasUpdates: Boolean,
)

interface SelectedAccountUseCase {

    fun selectedMetaAccountFlow(): Flow<MetaAccount>

    fun selectedAddressModelFlow(chain: suspend () -> Chain): Flow<AddressModel>

    fun selectedWalletModelFlow(): Flow<SelectedWalletModel>

    suspend fun getSelectedMetaAccount(): MetaAccount
}
