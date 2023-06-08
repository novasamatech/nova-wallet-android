package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.config.variants

import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfig
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.config.BuildPolkadotVaultVariantConfig

internal fun ParitySignerConfig(resourceManager: ResourceManager, appLinksProvider: AppLinksProvider) : PolkadotVaultVariantConfig {
    return BuildPolkadotVaultVariantConfig(resourceManager) {
        sign {
            troubleShootingLink = appLinksProvider.paritySignerTroubleShooting
        }

        connect {
            instructions {
                step(R.string.account_parity_signer_import_start_step_1)

                step(R.string.account_parity_signer_import_start_step_2)
                image(R.string.account_parity_signer_import_start_select_top, R.drawable.my_parity_signer)

                step(R.string.account_parity_signer_import_start_step_3)
            }
        }

        common {
            iconRes = R.drawable.ic_parity_signer
            nameRes = R.string.common_parity_signer
        }
    }
}
