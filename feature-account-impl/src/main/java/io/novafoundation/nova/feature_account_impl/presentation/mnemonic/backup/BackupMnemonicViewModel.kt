package io.novafoundation.nova.feature_account_impl.presentation.mnemonic.backup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.sendEvent
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.AdvancedEncryptionInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.AdvancedEncryptionRequester
import io.novafoundation.nova.feature_account_impl.presentation.lastResponseOrDefault
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.confirm.ConfirmMnemonicPayload
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.confirm.ConfirmMnemonicPayload.CreateExtras
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BackupMnemonicViewModel(
    private val interactor: AccountInteractor,
    private val router: AccountRouter,
    private val accountName: String?,
    private val addAccountPayload: AddAccountPayload,
    private val advancedEncryptionInteractor: AdvancedEncryptionInteractor,
    private val advancedEncryptionRequester: AdvancedEncryptionRequester
) : BaseViewModel() {

    private val mnemonicFlow = flowOf { interactor.generateMnemonic() }
        .inBackground()
        .share()

    private val _showMnemonicWarningDialog = MutableLiveData<Event<Unit>>()
    val showMnemonicWarningDialog: LiveData<Event<Unit>> = _showMnemonicWarningDialog

    private val warningAccepted = MutableStateFlow(false)

    val mnemonicDisplay = combine(
        mnemonicFlow,
        warningAccepted
    ) { mnemonc, warningAccepted ->
        mnemonc.words.takeIf { warningAccepted }
    }

    init {
        _showMnemonicWarningDialog.sendEvent()
    }

    fun homeButtonClicked() {
        router.back()
    }

    fun optionsClicked() {
        advancedEncryptionRequester.openRequest(addAccountPayload)
    }

    fun warningAccepted() {
        warningAccepted.value = true
    }

    fun warningDeclined() {
        router.back()
    }

    fun nextClicked() = launch {
        val advancedEncryptionResponse = advancedEncryptionRequester.lastResponseOrDefault(addAccountPayload, advancedEncryptionInteractor)
        val mnemonic = mnemonicFlow.first()

        val payload = ConfirmMnemonicPayload(
            mnemonic = mnemonic.wordList,
            CreateExtras(
                accountName = accountName,
                addAccountPayload = addAccountPayload,
                advancedEncryptionPayload = advancedEncryptionResponse
            )
        )

        router.openConfirmMnemonicOnCreate(payload)
    }
}
