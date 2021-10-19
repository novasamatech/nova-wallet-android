package jp.co.soramitsu.feature_account_impl.presentation.exporting.mnemonic

import jp.co.soramitsu.common.resources.ResourceManager
import jp.co.soramitsu.common.utils.flowOf
import jp.co.soramitsu.common.utils.inBackground
import jp.co.soramitsu.common.utils.invoke
import jp.co.soramitsu.feature_account_api.domain.interfaces.AccountInteractor
import jp.co.soramitsu.feature_account_impl.R
import jp.co.soramitsu.feature_account_impl.domain.account.export.mnemonic.ExportMnemonicInteractor
import jp.co.soramitsu.feature_account_impl.presentation.AccountRouter
import jp.co.soramitsu.feature_account_impl.presentation.exporting.ExportPayload
import jp.co.soramitsu.feature_account_impl.presentation.exporting.ExportSource
import jp.co.soramitsu.feature_account_impl.presentation.exporting.ExportViewModel
import jp.co.soramitsu.feature_account_impl.presentation.view.mnemonic.mapMnemonicToMnemonicWords
import jp.co.soramitsu.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ExportMnemonicViewModel(
    private val router: AccountRouter,
    private val interactor: ExportMnemonicInteractor,
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
}
