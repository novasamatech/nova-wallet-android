package io.novafoundation.nova.feature_account_impl.presentation.exporting.json.confirm

import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.advancedEncryption.AdvancedEncryptionModePayload
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportViewModel

class ExportJsonConfirmViewModel(
    private val router: AccountRouter,
    private val payload: ExportJsonConfirmPayload
) : ExportViewModel() {

    val json = payload.json

    fun back() {
        router.back()
    }

    fun confirmClicked() {
        exportText(json)
    }

    fun optionsClicked() {
        val payload = AdvancedEncryptionModePayload.View(payload.exportPayload.metaId, payload.exportPayload.chainId, hideDerivationPaths = true)

        router.openAdvancedSettings(payload)
    }
}
