package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.start

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.list.instruction.InstructionItem
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfig.ConnectPage
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfigProvider
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.formatWithPolkadotVaultLabel
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.ParitySignerStartPayload

class StartImportParitySignerViewModel(
    private val router: AccountRouter,
    private val payload: ParitySignerStartPayload,
    private val polkadotVaultVariantConfigProvider: PolkadotVaultVariantConfigProvider,
    private val resourceManager: ResourceManager
) : BaseViewModel() {

    private val variantConfig = polkadotVaultVariantConfigProvider.variantConfigFor(payload.variant)

    val pages = createPages()

    val polkadotVaultVariantIcon = variantConfig.common.iconRes

    val title = resourceManager.formatWithPolkadotVaultLabel(R.string.account_parity_signer_import_start_title, payload.variant)

    fun backClicked() {
        router.back()
    }

    fun scanQrCodeClicked() {
        router.openScanImportParitySigner(payload)
    }

    private fun createPages(): List<ParitySignerPageModel> {
        return variantConfig.pages.map {
            it.instructions
            ParitySignerPageModel(
                modeName = it.pageName,
                guideItems = it.instructions.map { instruction ->
                    when (instruction) {
                        is ConnectPage.Instruction.Image -> InstructionItem.Image(instruction.imageRes, instruction.label)
                        is ConnectPage.Instruction.Step -> InstructionItem.Step(instruction.index, instruction.content)
                    }
                }
            )
        }
    }
}
