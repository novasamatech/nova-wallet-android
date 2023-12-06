package io.novafoundation.nova.feature_account_api.domain.interfaces

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.data.proxy.MetaAccountsUpdatesRegistry
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.asPolkadotVaultVariantOrThrow
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfigProvider
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class SelectedWalletModel(
    @DrawableRes val typeIcon: Int?,
    val walletIcon: Drawable,
    val name: String,
    val hasUpdates: Boolean,
)

class SelectedAccountUseCase(
    private val accountRepository: AccountRepository,
    private val walletUiUseCase: WalletUiUseCase,
    private val addressIconGenerator: AddressIconGenerator,
    private val polkadotVaultVariantConfigProvider: PolkadotVaultVariantConfigProvider,
    private val metaAccountsUpdatesRegistry: MetaAccountsUpdatesRegistry
) {

    fun selectedMetaAccountFlow(): Flow<MetaAccount> = accountRepository.selectedMetaAccountFlow()

    fun selectedAddressModelFlow(chain: suspend () -> Chain) = selectedMetaAccountFlow().map {
        addressIconGenerator.createAccountAddressModel(
            chain = chain(),
            account = it,
            name = null
        )
    }

    fun selectedWalletModelFlow(): Flow<SelectedWalletModel> = combine(
        selectedMetaAccountFlow(),
        metaAccountsUpdatesRegistry.observeUpdatesExist()
    ) { metaAccount, hasMetaAccountsUpdates ->
        val icon = walletUiUseCase.walletIcon(metaAccount, transparentBackground = false)

        val typeIcon = when (val type = metaAccount.type) {
            LightMetaAccount.Type.SECRETS -> null // no icon for secrets account
            LightMetaAccount.Type.WATCH_ONLY -> R.drawable.ic_watch_only_filled
            LightMetaAccount.Type.PARITY_SIGNER, LightMetaAccount.Type.POLKADOT_VAULT -> {
                val config = polkadotVaultVariantConfigProvider.variantConfigFor(type.asPolkadotVaultVariantOrThrow())
                config.common.iconRes
            }

            LightMetaAccount.Type.LEDGER -> R.drawable.ic_ledger
            LightMetaAccount.Type.PROXIED -> R.drawable.ic_proxy
        }

        SelectedWalletModel(
            typeIcon = typeIcon,
            walletIcon = icon,
            name = metaAccount.name,
            hasUpdates = hasMetaAccountsUpdates
        )
    }

    suspend fun getSelectedMetaAccount(): MetaAccount = accountRepository.getSelectedMetaAccount()
}
