package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.config.variants

import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.spannable.highlightedText
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfig
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.config.BuildPolkadotVaultVariantConfig

internal fun PolkadotVaultConfig(resourceManager: ResourceManager, appLinksProvider: AppLinksProvider): PolkadotVaultVariantConfig {
    return BuildPolkadotVaultVariantConfig(resourceManager) {
        sign {
            troubleShootingLink = appLinksProvider.polkadotVaultTroubleShooting
            supportsProofSigning = true
        }

        connect {
            instructions {
                step(
                    resourceManager.highlightedText(
                        R.string.account_polkadot_vault_import_start_step_1,
                        R.string.account_polkadot_vault_import_start_step_1_highlighted
                    )
                )

                step(
                    resourceManager.highlightedText(
                        R.string.account_polkadot_vault_import_start_step_2,
                        R.string.account_polkadot_vault_import_start_step_2_highlighted
                    )
                )
                image(R.string.account_polkadot_vault_import_start_empty_derivation_hint, R.drawable.polkadot_vault_account)

                step(
                    resourceManager.highlightedText(
                        R.string.account_polkadot_vault_import_start_step_3,
                        R.string.account_polkadot_vault_import_start_step_3_highlighted
                    )
                )
            }
        }

        common {
            iconRes = R.drawable.ic_polkadot_vault
            nameRes = R.string.account_polkadot_vault
        }
    }
}
