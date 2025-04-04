package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.config.variants

import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.spannable.highlightedText
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfig
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.config.BuildPolkadotVaultVariantConfig

internal fun ParitySignerConfig(resourceManager: ResourceManager, appLinksProvider: AppLinksProvider): PolkadotVaultVariantConfig {
    return BuildPolkadotVaultVariantConfig(resourceManager) {
        sign {
            troubleShootingLink = appLinksProvider.paritySignerTroubleShooting
            supportsProofSigning = false
        }

        connect {
            instructions {
                step(
                    resourceManager.highlightedText(
                        R.string.account_parity_signer_import_start_step_1,
                        R.string.account_parity_signer_import_start_step_1_highlighted
                    )
                )

                step(
                    resourceManager.highlightedText(
                        R.string.account_parity_signer_import_start_step_2,
                        R.string.account_parity_signer_import_start_step_2_highlighted
                    )
                )
                image(R.string.account_parity_signer_import_start_select_top, R.drawable.my_parity_signer)

                step(
                    resourceManager.highlightedText(
                        R.string.account_parity_signer_import_start_step_3,
                        R.string.account_parity_signer_import_start_step_3_highlighted
                    )
                )
            }
        }

        common {
            iconRes = R.drawable.ic_parity_signer
            nameRes = R.string.account_parity_signer
        }
    }
}
