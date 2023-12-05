package io.novafoundation.nova.feature_account_impl.presentation.account.common.listing

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.ChipLabelModel
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.asPolkadotVaultVariantOrThrow
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfigProvider
import io.novafoundation.nova.feature_account_impl.R

class MetaAccountTypePresentationMapper(
    private val resourceManager: ResourceManager,
    private val polkadotVaultVariantConfigProvider: PolkadotVaultVariantConfigProvider,
) {

    fun mapMetaAccountTypeToUi(type: LightMetaAccount.Type): ChipLabelModel? {
        return when (type) {
            LightMetaAccount.Type.SECRETS -> null
            LightMetaAccount.Type.WATCH_ONLY -> ChipLabelModel(
                iconRes = R.drawable.ic_watch_only_filled,
                title = resourceManager.getString(R.string.account_watch_only)
            )
            LightMetaAccount.Type.PARITY_SIGNER, LightMetaAccount.Type.POLKADOT_VAULT -> {
                val config = polkadotVaultVariantConfigProvider.variantConfigFor(type.asPolkadotVaultVariantOrThrow())

                ChipLabelModel(
                    iconRes = config.common.iconRes,
                    title = resourceManager.getString(config.common.nameRes)
                )
            }
            LightMetaAccount.Type.LEDGER -> ChipLabelModel(
                iconRes = R.drawable.ic_ledger,
                title = resourceManager.getString(R.string.common_ledger)
            )

            LightMetaAccount.Type.PROXIED -> ChipLabelModel(
                iconRes = R.drawable.ic_proxy,
                title = resourceManager.getString(R.string.account_proxieds)
            )
        }
    }
}
