package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.start

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfigProvider
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.formatWithPolkadotVaultLabel
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_api.presenatation.AccountRouter
import io.novafoundation.nova.feature_account_api.presenatation.paritySigner.connect.ParitySignerStartPayload

class StartImportParitySignerViewModel(
    private val router: AccountRouter,
    private val payload: ParitySignerStartPayload,
    private val polkadotVaultVariantConfigProvider: PolkadotVaultVariantConfigProvider,
    private val resourceManager: ResourceManager
) : BaseViewModel() {

    private val variantConfig = polkadotVaultVariantConfigProvider.variantConfigFor(payload.variant)

    val instructions = variantConfig
        .connect.instructions

    val polkadotVaultVariantIcon = variantConfig.common.iconRes

    val title = resourceManager.formatWithPolkadotVaultLabel(R.string.account_parity_signer_import_start_title, payload.variant)

    fun backClicked() {
        router.back()
    }

    fun scanQrCodeClicked() {
        router.openScanImportParitySigner(payload)
    }
}
