package io.novafoundation.nova.feature_account_impl.presentation.exporting.json.confirm

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportSource
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

class ExportJsonConfirmViewModel(
    private val router: AccountRouter,
    resourceManager: ResourceManager,
    accountInteractor: AccountInteractor,
    chainRegistry: ChainRegistry,
    payload: ExportJsonConfirmPayload
) : ExportViewModel(
    accountInteractor,
    payload.exportPayload,
    resourceManager,
    chainRegistry,
    ExportSource.Json
) {

    val json = payload.json

    fun changePasswordClicked() {
        back()
    }

    fun back() {
        router.back()
    }

    fun confirmClicked() {
        exportText(json)
    }
}
