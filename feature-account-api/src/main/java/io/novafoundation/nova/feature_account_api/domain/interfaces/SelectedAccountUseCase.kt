package io.novafoundation.nova.feature_account_api.domain.interfaces

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SelectedWalletModel(
    @DrawableRes val typeIcon: Int?,
    val walletIcon: Drawable,
    val name: String,
)

class SelectedAccountUseCase(
    private val accountRepository: AccountRepository,
    private val addressIconGenerator: AddressIconGenerator,
) {

    fun selectedMetaAccountFlow(): Flow<MetaAccount> = accountRepository.selectedMetaAccountFlow()

    fun selectedAddressModelFlow(chain: suspend () -> Chain) = selectedMetaAccountFlow().map {
        addressIconGenerator.createAccountAddressModel(
            chain = chain(),
            account = it,
            name = null
        )
    }

    fun selectedWalletModelFlow(): Flow<SelectedWalletModel> = selectedMetaAccountFlow().map {
        val icon = addressIconGenerator.createAddressIcon(
            accountId = it.substrateAccountId,
            sizeInDp = AddressIconGenerator.SIZE_BIG,
        )

        val typeIcon = when(it.type) {
            LightMetaAccount.Type.SECRETS -> null // no icon for secrets acount
            LightMetaAccount.Type.WATCH_ONLY -> R.drawable.ic_watch
        }

        SelectedWalletModel(
            typeIcon = typeIcon,
            walletIcon = icon,
            name = it.name
        )
    }

    suspend fun getSelectedMetaAccount(): MetaAccount = accountRepository.getSelectedMetaAccount()
}
