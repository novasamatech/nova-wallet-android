package io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault

import androidx.annotation.StringRes
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.domain.model.PolkadotVaultVariant

fun ResourceManager.polkadotVaultLabelFor(polkadotVaultVariant: PolkadotVaultVariant): String {
    return when (polkadotVaultVariant) {
        PolkadotVaultVariant.POLKADOT_VAULT -> getString(R.string.account_polkadot_vault)
        PolkadotVaultVariant.PARITY_SIGNER -> getString(R.string.account_parity_signer)
    }
}

fun ResourceManager.formatWithPolkadotVaultLabel(@StringRes stringRes: Int, polkadotVaultVariant: PolkadotVaultVariant): String {
    return getString(stringRes, polkadotVaultLabelFor(polkadotVaultVariant))
}
