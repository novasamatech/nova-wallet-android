package io.novafoundation.nova.feature_account_impl.presentation.exporting.seed

import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_account_impl.domain.account.export.seed.ExportSeedInteractor
import io.novafoundation.nova.feature_account_api.presenatation.AccountRouter
import io.novafoundation.nova.feature_account_api.presenatation.account.advancedEncryption.AdvancedEncryptionModePayload
import io.novafoundation.nova.feature_account_api.presenatation.exporting.ExportPayload
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportViewModel

class ExportSeedViewModel(
    private val router: AccountRouter,
    private val interactor: ExportSeedInteractor,
    private val exportPayload: ExportPayload,
) : ExportViewModel() {

    val seedFlow = flowOf {
        interactor.getAccountSeed(exportPayload.metaId, exportPayload.chainId)
    }
        .inBackground()
        .share()

    fun optionsClicked() {
        val viewRequest = AdvancedEncryptionModePayload.View(exportPayload.metaId, exportPayload.chainId)

        router.openAdvancedSettings(viewRequest)
    }

    fun back() {
        router.back()
    }
}
