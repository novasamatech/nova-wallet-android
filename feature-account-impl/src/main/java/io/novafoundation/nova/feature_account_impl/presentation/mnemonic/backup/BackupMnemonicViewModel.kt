package io.novafoundation.nova.feature_account_impl.presentation.mnemonic.backup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.sendEvent
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.AdvancedEncryptionInteractor
import io.novafoundation.nova.feature_account_impl.domain.account.export.mnemonic.ExportMnemonicInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.AdvancedEncryptionRequester
import io.novafoundation.nova.feature_account_impl.presentation.account.advancedEncryption.AdvancedEncryptionPayload
import io.novafoundation.nova.feature_account_impl.presentation.common.mnemonic.spacedWords
import io.novafoundation.nova.feature_account_impl.presentation.lastResponseOrDefault
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.confirm.ConfirmMnemonicPayload
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.confirm.ConfirmMnemonicPayload.CreateExtras
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BackupMnemonicViewModel(
    private val interactor: AccountInteractor,
    private val exportMnemonicInteractor: ExportMnemonicInteractor,
    private val router: AccountRouter,
    private val payload: BackupMnemonicPayload,
    private val advancedEncryptionInteractor: AdvancedEncryptionInteractor,
    private val resourceManager: ResourceManager,
    private val advancedEncryptionRequester: AdvancedEncryptionRequester
) : BaseViewModel() {

    private val mnemonicFlow = flowOf {
        when (payload) {
            is BackupMnemonicPayload.Confirm -> exportMnemonicInteractor.getMnemonic(payload.metaAccountId, payload.chainId)
            is BackupMnemonicPayload.Create -> interactor.generateMnemonic()
        }
    }
        .inBackground()
        .share()

    private val _showMnemonicWarningDialog = MutableLiveData<Event<Unit>>()
    val showMnemonicWarningDialog: LiveData<Event<Unit>> = _showMnemonicWarningDialog

    private val warningAcceptedFlow = MutableStateFlow(false)

    val mnemonicDisplay = combine(
        mnemonicFlow,
        warningAcceptedFlow
    ) { mnemonic, warningAccepted ->
        mnemonic.spacedWords().takeIf { warningAccepted }
    }

    val continueText = flowOf {
        val stringRes = when (payload) {
            is BackupMnemonicPayload.Confirm -> R.string.account_confirm_mnemonic
            is BackupMnemonicPayload.Create -> R.string.common_continue
        }

        resourceManager.getString(stringRes)
    }
        .inBackground()
        .share()

    init {
        _showMnemonicWarningDialog.sendEvent()
    }

    fun homeButtonClicked() {
        router.back()
    }

    fun optionsClicked() {
        val advancedEncryptionPayload = when (payload) {
            is BackupMnemonicPayload.Confirm -> AdvancedEncryptionPayload.View(payload.metaAccountId, payload.chainId)
            is BackupMnemonicPayload.Create -> AdvancedEncryptionPayload.Change(payload.addAccountPayload)
        }

        advancedEncryptionRequester.openRequest(advancedEncryptionPayload)
    }

    fun warningAccepted() {
        warningAcceptedFlow.value = true
    }

    fun warningDeclined() {
        router.back()
    }

    fun nextClicked() = launch {
        val createExtras = (payload as? BackupMnemonicPayload.Create)?.let {
            val advancedEncryptionResponse = advancedEncryptionRequester.lastResponseOrDefault(it.addAccountPayload, advancedEncryptionInteractor)

            CreateExtras(
                accountName = it.newWalletName,
                addAccountPayload = it.addAccountPayload,
                advancedEncryptionPayload = advancedEncryptionResponse
            )
        }

        val payload = ConfirmMnemonicPayload(
            mnemonic = mnemonicFlow.first().wordList,
            createExtras = createExtras
        )

        router.openConfirmMnemonicOnCreate(payload)
    }
}
