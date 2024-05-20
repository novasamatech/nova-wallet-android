package io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfigProvider
import io.novafoundation.nova.feature_account_impl.domain.account.details.WalletDetailsInteractor
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.ProxyFormatter
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.AccountFormatterFactory

class WalletDetailsMixinFactory(
    private val polkadotVaultVariantConfigProvider: PolkadotVaultVariantConfigProvider,
    private val resourceManager: ResourceManager,
    private val accountFormatterFactory: AccountFormatterFactory,
    private val proxyFormatter: ProxyFormatter,
    private val interactor: WalletDetailsInteractor
) {

    suspend fun create(metaId: Long): WalletDetailsMixin {
        val metaAccount = interactor.getMetaAccount(metaId)
        return when (metaAccount.type) {
            Type.SECRETS -> SecretsWalletDetailsMixin(resourceManager, accountFormatterFactory, interactor, metaAccount)

            Type.WATCH_ONLY -> WatchOnlyWalletDetailsMixin(resourceManager, accountFormatterFactory, interactor, metaAccount)

            Type.LEDGER_LEGACY -> LedgerWalletDetailsMixin(resourceManager, accountFormatterFactory, interactor, metaAccount)

            Type.PARITY_SIGNER,
            Type.POLKADOT_VAULT -> PolkadotVaultWalletDetailsMixin(
                polkadotVaultVariantConfigProvider,
                resourceManager,
                accountFormatterFactory,
                interactor,
                metaAccount
            )

            Type.PROXIED -> ProxiedWalletDetailsMixin(resourceManager, accountFormatterFactory, interactor, proxyFormatter, metaAccount)

            // TODO generic ledger wallet details
            Type.LEDGER -> LedgerWalletDetailsMixin(resourceManager, accountFormatterFactory, interactor, metaAccount)
        }
    }
}
