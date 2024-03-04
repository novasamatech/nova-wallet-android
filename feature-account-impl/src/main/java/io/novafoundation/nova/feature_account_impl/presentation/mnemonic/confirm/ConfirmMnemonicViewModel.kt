package io.novafoundation.nova.feature_account_impl.presentation.mnemonic.confirm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.added
import io.novafoundation.nova.common.utils.modified
import io.novafoundation.nova.common.utils.removed
import io.novafoundation.nova.common.utils.sendEvent
import io.novafoundation.nova.common.vibration.DeviceVibrator
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.common.model.toAdvancedEncryption
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.data.mappers.mapAddAccountPayloadToAddAccountType
import io.novafoundation.nova.feature_account_impl.data.mappers.mapOptionalNameToNameChooserState
import io.novafoundation.nova.feature_account_impl.domain.account.add.AddAccountInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novasama.substrate_sdk_android.encrypt.junction.BIP32JunctionDecoder
import io.novasama.substrate_sdk_android.encrypt.junction.JunctionDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ConfirmMnemonicViewModel(
    private val interactor: AccountInteractor,
    private val addAccountInteractor: AddAccountInteractor,
    private val router: AccountRouter,
    private val deviceVibrator: DeviceVibrator,
    private val resourceManager: ResourceManager,
    private val config: ConfirmMnemonicConfig,
    private val payload: ConfirmMnemonicPayload
) : BaseViewModel() {

    private val originMnemonic = payload.mnemonic

    private val shuffledMnemonic = originMnemonic.shuffled()

    private val _sourceWords = MutableStateFlow(initialSourceWords())
    val sourceWords: Flow<List<MnemonicWord>> = _sourceWords

    private val _destinationWords = MutableStateFlow<List<MnemonicWord>>(emptyList())
    val destinationWords: Flow<List<MnemonicWord>> = _destinationWords

    val nextButtonEnabled = destinationWords.map {
        originMnemonic.size == it.size
    }

    val skipVisible = payload.createExtras != null && config.allowShowingSkip

    private val _matchingMnemonicErrorAnimationEvent = MutableLiveData<Event<Unit>>()
    val matchingMnemonicErrorAnimationEvent: LiveData<Event<Unit>> = _matchingMnemonicErrorAnimationEvent

    fun homeButtonClicked() {
        router.back()
    }

    fun sourceWordClicked(sourceWord: MnemonicWord) {
        val markedAsRemoved = sourceWord.copy(removed = true)

        val destinationWordsSnapshot = _destinationWords.value
        val destinationWord = sourceWord.copy(
            indexDisplay = (destinationWordsSnapshot.size + 1).toString()
        )

        _sourceWords.value = _sourceWords.value.modified(markedAsRemoved, markedAsRemoved.byMyId())
        _destinationWords.value = destinationWordsSnapshot.added(destinationWord)
    }

    fun destinationWordClicked(destinationWord: MnemonicWord) = launch(Dispatchers.Default) {
        val sourceWord = _sourceWords.value.first { it.content == destinationWord.content }
        val modifiedSourceWord = sourceWord.copy(removed = false)

        _sourceWords.value = _sourceWords.value.modified(modifiedSourceWord, modifiedSourceWord.byMyId())
        _destinationWords.value = _destinationWords.value.removed(destinationWord.byMyId()).fixIndices()
    }

    fun reset() {
        _destinationWords.value = emptyList()
        _sourceWords.value = initialSourceWords()
    }

    fun skipClicked() {
        proceed()
    }

    fun continueClicked() {
        val mnemonicFromDestination = _destinationWords.value.map(MnemonicWord::content)

        if (mnemonicFromDestination == originMnemonic) {
            proceed()
        } else {
            deviceVibrator.makeShortVibration()
            _matchingMnemonicErrorAnimationEvent.sendEvent()
        }
    }

    private fun List<MnemonicWord>.fixIndices(): List<MnemonicWord> {
        return mapIndexed { index, word ->
            word.copy(indexDisplay = (index + 1).toString())
        }
    }

    private fun initialSourceWords(): List<MnemonicWord> {
        return shuffledMnemonic.mapIndexed { index, word ->
            MnemonicWord(
                id = index,
                content = word,
                indexDisplay = null, // source does not have indexing
                removed = false
            )
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

    private fun MnemonicWord.byMyId(): (MnemonicWord) -> Boolean = { it.id == id }

    private fun createAccount(extras: ConfirmMnemonicPayload.CreateExtras) {
        viewModelScope.launch {
            val mnemonicString = originMnemonic.joinToString(" ")

            with(extras) {
                val accountNameState = mapOptionalNameToNameChooserState(accountName)
                val addAccountType = mapAddAccountPayloadToAddAccountType(addAccountPayload, accountNameState)
                val advancedEncryption = advancedEncryptionModel.toAdvancedEncryption()

                addAccountInteractor.createAccount(mnemonicString, advancedEncryption, addAccountType)
                    .onSuccess { continueBasedOnCodeStatus() }
                    .onFailure(::showAccountCreationError)
            }
        }
    }

    private fun showAccountCreationError(throwable: Throwable) {
        val (title, message) = when (throwable) {
            is JunctionDecoder.DecodingError, is BIP32JunctionDecoder.DecodingError -> {
                resourceManager.getString(R.string.account_invalid_derivation_path_title) to
                    resourceManager.getString(R.string.account_invalid_derivation_path_message_v2_2_0)
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
}
