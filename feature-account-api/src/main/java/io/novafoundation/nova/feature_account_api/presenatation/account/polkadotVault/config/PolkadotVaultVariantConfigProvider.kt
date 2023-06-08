package io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config

import io.novafoundation.nova.feature_account_api.domain.model.PolkadotVaultVariant

interface PolkadotVaultVariantConfigProvider {

    fun variantConfigFor(variant: PolkadotVaultVariant): PolkadotVaultVariantConfig
}
