package io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin

import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfigProvider
import io.novafoundation.nova.feature_account_impl.domain.account.details.WalletDetailsInteractor
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.ProxyFormatter
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.AccountFormatterFactory
import io.novafoundation.nova.feature_ledger_core.domain.LedgerMigrationTracker

class WalletDetailsMixinFactory(
    private val polkadotVaultVariantConfigProvider: PolkadotVaultVariantConfigProvider,
    private val resourceManager: ResourceManager,
    private val accountFormatterFactory: AccountFormatterFactory,
    private val proxyFormatter: ProxyFormatter,
    private val interactor: WalletDetailsInteractor,
    private val appLinksProvider: AppLinksProvider,
    private val ledgerMigrationTracker: LedgerMigrationTracker,
) {

    suspend fun create(metaId: Long, host: WalletDetailsMixinHost): WalletDetailsMixin {
        val metaAccount = interactor.getMetaAccount(metaId)

        return when (metaAccount.type) {
            Type.SECRETS -> SecretsWalletDetailsMixin(
                resourceManager = resourceManager,
                accountFormatterFactory = accountFormatterFactory,
                interactor = interactor,
                metaAccount = metaAccount
            )

            Type.WATCH_ONLY -> WatchOnlyWalletDetailsMixin(
                resourceManager = resourceManager,
                accountFormatterFactory = accountFormatterFactory,
                interactor = interactor,
                metaAccount = metaAccount
            )

            Type.LEDGER_LEGACY -> LegacyLedgerWalletDetailsMixin(
                resourceManager = resourceManager,
                accountFormatterFactory = accountFormatterFactory,
                interactor = interactor,
                host = host,
                appLinksProvider = appLinksProvider,
                metaAccount = metaAccount
            )

            Type.LEDGER -> GenericLedgerWalletDetailsMixin(
                resourceManager = resourceManager,
                accountFormatterFactory = accountFormatterFactory,
                interactor = interactor,
                ledgerMigrationTracker = ledgerMigrationTracker,
                metaAccount = metaAccount
            )

            Type.PARITY_SIGNER,
            Type.POLKADOT_VAULT -> PolkadotVaultWalletDetailsMixin(
                polkadotVaultVariantConfigProvider = polkadotVaultVariantConfigProvider,
                resourceManager = resourceManager,
                accountFormatterFactory = accountFormatterFactory,
                interactor = interactor,
                metaAccount = metaAccount
            )

            Type.PROXIED -> ProxiedWalletDetailsMixin(
                resourceManager = resourceManager,
                accountFormatterFactory = accountFormatterFactory,
                interactor = interactor,
                proxyFormatter = proxyFormatter,
                metaAccount = metaAccount
            )
        }
    }
}
