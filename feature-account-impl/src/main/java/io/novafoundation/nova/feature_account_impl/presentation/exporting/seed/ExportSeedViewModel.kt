package io.novafoundation.nova.feature_account_impl.presentation.exporting.seed

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.domain.account.export.seed.ExportSeedInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.AdvancedEncryptionRequester
import io.novafoundation.nova.feature_account_impl.presentation.account.advancedEncryption.AdvancedEncryptionPayload
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportPayload
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

class ExportSeedViewModel(
    private val router: AccountRouter,
    private val interactor: ExportSeedInteractor,
    private val advancedEncryptionRequester: AdvancedEncryptionRequester,
    resourceManager: ResourceManager,
    accountInteractor: AccountInteractor,
    chainRegistry: ChainRegistry,
    payload: ExportPayload,
) : ExportViewModel(
    accountInteractor,
    payload,
    resourceManager,
    chainRegistry
) {

    val seedFlow = flowOf {
        interactor.getAccountSeed(payload.metaId, payload.chainId)
    }
        .inBackground()
        .share()

    fun optionsClicked() {
        val viewRequest = AdvancedEncryptionPayload.View(exportPayload.metaId, exportPayload.chainId)

        advancedEncryptionRequester.openRequest(viewRequest)
    }

    fun back() {
        router.back()
    }
}
