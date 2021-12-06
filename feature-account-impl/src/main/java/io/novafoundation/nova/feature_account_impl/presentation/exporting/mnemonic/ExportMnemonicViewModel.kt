package io.novafoundation.nova.feature_account_impl.presentation.exporting.mnemonic

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.account.export.mnemonic.ExportMnemonicInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.AdvancedEncryptionRequester
import io.novafoundation.nova.feature_account_impl.presentation.account.advancedEncryption.AdvancedEncryptionPayload
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportPayload
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportSource
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportViewModel
import io.novafoundation.nova.feature_account_impl.presentation.view.mnemonic.mapMnemonicToMnemonicWords
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ExportMnemonicViewModel(
    private val router: AccountRouter,
    private val interactor: ExportMnemonicInteractor,
    private val advancedEncryptionRequester: AdvancedEncryptionRequester,
    resourceManager: ResourceManager,
    chainRegistry: ChainRegistry,
    accountInteractor: AccountInteractor,
    payload: ExportPayload,
) : ExportViewModel(
    accountInteractor,
    payload,
    resourceManager,
    chainRegistry,
    ExportSource.Mnemonic
) {

    val exportingSecretFlow = flowOf {
        interactor.getMnemonic(payload.metaId, payload.chainId)
    }
        .inBackground()
        .share()

    val mnemonicWordsFlow = exportingSecretFlow.map {
        mapMnemonicToMnemonicWords(it.secret.wordList)
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

            val mnemonic = exportingSecret.secret.words
            val chainName = chain().name

            val derivationPath = exportingSecret.derivationPath

            val shareText = if (derivationPath.isNullOrBlank()) {
                resourceManager.getString(R.string.export_mnemonic_without_derivation, chainName, mnemonic)
            } else {
                resourceManager.getString(R.string.export_mnemonic_with_derivation, chainName, mnemonic, derivationPath)
            }

            exportText(shareText)
        }
    }

    fun openConfirmMnemonic() = launch {
        val mnemonic = exportingSecretFlow.first().secret

        router.openConfirmMnemonicOnExport(mnemonic.wordList)
    }

    fun optionsClicked() {
        val viewRequest = AdvancedEncryptionPayload.View(
            metaAccountId = exportPayload.metaId,
            chainId = exportPayload.chainId
        )
        advancedEncryptionRequester.openRequest(viewRequest)
    }
}
