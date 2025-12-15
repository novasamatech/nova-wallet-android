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

        connectPage {
            name(resourceManager.getString(R.string.account_pair_public_key))

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
                image(labelRes = null, R.drawable.polkadot_vault_account)

                step(
                    resourceManager.highlightedText(
                        R.string.account_polkadot_vault_import_start_step_3,
                        R.string.account_polkadot_vault_import_start_step_3_highlighted
                    )
                )
            }
        }

        connectPage {
            name(resourceManager.getString(R.string.account_import_private_key))

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
                image(labelRes = null, R.drawable.polkadot_vault_account)

                step(
                    resourceManager.highlightedText(
                        R.string.account_polkadot_vault_import_start_step_3_private,
                        R.string.account_polkadot_vault_import_start_step_3_private_highlighted
                    )
                )

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
