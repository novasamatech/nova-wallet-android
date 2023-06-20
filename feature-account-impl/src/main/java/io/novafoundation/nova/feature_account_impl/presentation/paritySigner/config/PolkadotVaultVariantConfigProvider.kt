package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.config

import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.model.PolkadotVaultVariant
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfig
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfigProvider
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.config.variants.ParitySignerConfig
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.config.variants.PolkadotVaultConfig

class RealPolkadotVaultVariantConfigProvider(
    private val resourceManager: ResourceManager,
    private val appLinksProvider: AppLinksProvider,
) : PolkadotVaultVariantConfigProvider {

    private val paritySignerConfig by lazy { ParitySignerConfig(resourceManager, appLinksProvider) }
    private val polkadotVaultConfig by lazy { PolkadotVaultConfig(resourceManager, appLinksProvider) }

    override fun variantConfigFor(variant: PolkadotVaultVariant): PolkadotVaultVariantConfig {
        return when (variant) {
            PolkadotVaultVariant.POLKADOT_VAULT -> polkadotVaultConfig
            PolkadotVaultVariant.PARITY_SIGNER -> paritySignerConfig
        }
    }
}
