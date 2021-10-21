package io.novafoundation.nova.feature_account_impl.presentation.exporting.seed

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.account.export.seed.ExportSeedInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportPayload
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportSource
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ExportSeedViewModel(
    private val router: AccountRouter,
    private val interactor: ExportSeedInteractor,
    resourceManager: ResourceManager,
    accountInteractor: AccountInteractor,
    chainRegistry: ChainRegistry,
    payload: ExportPayload,
) : ExportViewModel(
    accountInteractor,
    payload,
    resourceManager,
    chainRegistry,
    ExportSource.Seed
) {

    val exportingSecretFlow = flowOf {
        interactor.getSeedForExport(payload.metaId, payload.chainId)
    }
        .inBackground()
        .share()

    fun back() {
        router.back()
    }

    fun exportClicked() {
        showSecurityWarning()
    }

    override fun securityWarningConfirmed() {
        launch {
            val exportingSecret = exportingSecretFlow.first()
            val chainName = chain().name

            val shareText = if (exportingSecret.derivationPath.isNullOrBlank()) {
                resourceManager.getString(R.string.export_seed_without_derivation, chainName, exportingSecret.secret)
            } else {
                resourceManager.getString(R.string.export_seed_with_derivation, chainName, exportingSecret.secret, exportingSecret.derivationPath)
            }

            exportText(shareText)
        }
    }
}
