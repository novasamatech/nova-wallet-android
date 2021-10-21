package io.novafoundation.nova.feature_account_impl.presentation.mnemonic.confirm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.map
import io.novafoundation.nova.common.utils.sendEvent
import io.novafoundation.nova.common.vibration.DeviceVibrator
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.data.mappers.mapAddAccountPayloadToAddAccountType
import io.novafoundation.nova.feature_account_impl.domain.account.add.AddAccountInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import jp.co.soramitsu.fearless_utils.encrypt.junction.BIP32JunctionDecoder
import jp.co.soramitsu.fearless_utils.encrypt.junction.JunctionDecoder
import kotlinx.coroutines.launch

class ConfirmMnemonicViewModel(
    private val interactor: AccountInteractor,
    private val addAccountInteractor: AddAccountInteractor,
    private val router: AccountRouter,
    private val deviceVibrator: DeviceVibrator,
    private val resourceManager: ResourceManager,
    private val payload: ConfirmMnemonicPayload
) : BaseViewModel() {

    private val originMnemonic = payload.mnemonic

    val shuffledMnemonic = originMnemonic.shuffled()

    private val confirmationMnemonicWords = MutableLiveData<List<String>>(emptyList())

    private val _resetConfirmationEvent = MutableLiveData<Event<Unit>>()
    val resetConfirmationEvent: LiveData<Event<Unit>> = _resetConfirmationEvent

    private val _removeLastWordFromConfirmationEvent = MutableLiveData<Event<Unit>>()
    val removeLastWordFromConfirmationEvent: LiveData<Event<Unit>> = _removeLastWordFromConfirmationEvent

    val nextButtonEnableLiveData: LiveData<Boolean> = confirmationMnemonicWords.map {
        originMnemonic.size == it.size
    }

    val skipVisible = payload.createExtras != null

    private val _matchingMnemonicErrorAnimationEvent = MutableLiveData<Event<Unit>>()
    val matchingMnemonicErrorAnimationEvent: LiveData<Event<Unit>> = _matchingMnemonicErrorAnimationEvent

    fun homeButtonClicked() {
        router.back()
    }

    fun resetConfirmationClicked() {
        reset()
    }

    private fun reset() {
        confirmationMnemonicWords.value = mutableListOf()
        _resetConfirmationEvent.sendEvent()
    }

    fun addWordToConfirmMnemonic(word: String) {
        confirmationMnemonicWords.value?.let {
            val wordList = mutableListOf<String>().apply {
                addAll(it)
                add(word)
            }
            confirmationMnemonicWords.value = wordList
        }
    }

    fun removeLastWordFromConfirmation() {
        confirmationMnemonicWords.value?.let {
            if (it.isEmpty()) {
                return
            }
            val wordList = mutableListOf<String>().apply {
                addAll(it.subList(0, it.size - 1))
            }
            confirmationMnemonicWords.value = wordList
        }

        _removeLastWordFromConfirmationEvent.sendEvent()
    }

    fun nextButtonClicked() {
        confirmationMnemonicWords.value?.let { enteredWords ->
            if (originMnemonic == enteredWords) {
                proceed()
            } else {
                deviceVibrator.makeShortVibration()
                _matchingMnemonicErrorAnimationEvent.sendEvent()
            }
        }
    }

    private fun proceed() {
        val createExtras = payload.createExtras

        if (createExtras != null) {
            createAccount(createExtras)
        } else {
            finishConfirmGame()
        }
    }

    private fun finishConfirmGame() {
        router.back()
    }

    private fun createAccount(extras: ConfirmMnemonicPayload.CreateExtras) {
        viewModelScope.launch {
            val mnemonicString = originMnemonic.joinToString(" ")

            with(extras) {
                val addAccountType = mapAddAccountPayloadToAddAccountType(addAccountPayload)

                addAccountInteractor.createAccount(accountName, mnemonicString, cryptoType, derivationPath, addAccountType)
                    .onSuccess { continueBasedOnCodeStatus() }
                    .onFailure(::showAccountCreationError)
            }
        }
    }

    private fun showAccountCreationError(throwable: Throwable) {
        val (title, message) = when (throwable) {
            is JunctionDecoder.DecodingError, is BIP32JunctionDecoder.DecodingError -> {
                resourceManager.getString(R.string.account_invalid_derivation_path_title) to
                    resourceManager.getString(R.string.account_invalid_derivation_path_message)
            }
            else -> {
                resourceManager.getString(R.string.common_error_general_title) to
                    resourceManager.getString(R.string.common_undefined_error_message)
            }
        }

        showError(title, message)
    }

    fun matchingErrorAnimationCompleted() {
        reset()
    }

    private suspend fun continueBasedOnCodeStatus() {
        if (interactor.isCodeSet()) {
            router.openMain()
        } else {
            router.openCreatePincode()
        }
    }

    fun skipClicked() {
        proceed()
    }
}
