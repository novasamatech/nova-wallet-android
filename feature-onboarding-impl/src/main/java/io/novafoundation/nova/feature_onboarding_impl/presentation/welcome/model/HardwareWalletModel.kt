package io.novafoundation.nova.feature_onboarding_impl.presentation.welcome.model

import io.novafoundation.nova.feature_account_api.domain.model.PolkadotVaultVariant

sealed class HardwareWalletModel {

    class PolkadotVault(val variant: PolkadotVaultVariant) : HardwareWalletModel()

    object LedgerGeneric : HardwareWalletModel()

    object LedgerLegacy : HardwareWalletModel()
}
