package io.novafoundation.nova.feature_account_impl.presentation.importing

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.MixinFactory
import io.novafoundation.nova.common.resources.ClipboardManager
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.combine
import io.novafoundation.nova.common.utils.map
import io.novafoundation.nova.common.utils.switchMap
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet.Payload
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountAlreadyExistsException
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.data.mappers.mapAddAccountPayloadToAddAccountType
import io.novafoundation.nova.feature_account_impl.domain.account.add.AddAccountInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.CryptoTypeChooserMixin
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.ForcedChainMixin
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.WithCryptoTypeChooserMixin
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.WithForcedChainMixin
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.model.FileRequester
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.model.ImportError
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.model.ImportSource
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.model.JsonImportSource
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.model.MnemonicImportSource
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.model.RawSeedImportSource
import jp.co.soramitsu.fearless_utils.encrypt.junction.BIP32JunctionDecoder
import jp.co.soramitsu.fearless_utils.encrypt.junction.JunctionDecoder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ImportAccountViewModel(
    private val addAccountInteractor: AddAccountInteractor,
    private val interactor: AccountInteractor,
    private val router: AccountRouter,
    private val resourceManager: ResourceManager,
    forcedChainMixinFactory: MixinFactory<ForcedChainMixin>,
    cryptoTypeChooserFactory: MixinFactory<CryptoTypeChooserMixin>,
    private val clipboardManager: ClipboardManager,
    private val fileReader: FileReader,
    private val payload: AddAccountPayload,
) : BaseViewModel(),
    WithCryptoTypeChooserMixin,
    WithForcedChainMixin {

    override val forcedChainMixin: ForcedChainMixin = forcedChainMixinFactory.create(scope = this)
    override val cryptoTypeChooserMixin: CryptoTypeChooserMixin = cryptoTypeChooserFactory.create(scope = this)

    val nameLiveData = MutableLiveData<String>()

    val sourceTypes = provideSourceType()

    private val _selectedSourceTypeLiveData = MutableLiveData<ImportSource>()

    val selectedSourceTypeLiveData: LiveData<ImportSource> = _selectedSourceTypeLiveData

    private val _showSourceChooserLiveData = MutableLiveData<Event<Payload<ImportSource>>>()
    val showSourceSelectorChooserLiveData: LiveData<Event<Payload<ImportSource>>> = _showSourceChooserLiveData

    val derivationPathLiveData = MutableLiveData<String>()

    private val sourceTypeValid = _selectedSourceTypeLiveData.switchMap(ImportSource::validationLiveData)

    private val importInProgressLiveData = MutableLiveData(false)

    private val nextButtonEnabledLiveData = sourceTypeValid.combine(nameLiveData) { sourceTypeValid, name ->
        sourceTypeValid && name.isNotEmpty()
    }

    val nextButtonState = nextButtonEnabledLiveData.combine(importInProgressLiveData) { enabled, inProgress ->
        when {
            inProgress -> ButtonState.PROGRESS
            enabled -> ButtonState.NORMAL
            else -> ButtonState.DISABLED
        }
    }

    val changeableAdvancedFields = _selectedSourceTypeLiveData.map { it !is JsonImportSource }
    val cryptoTypeChooserEnabled = changeableAdvancedFields.combine(cryptoTypeChooserMixin.selectionFrozen.asLiveData()) { changeable, selectionFrozen ->
        changeable && !selectionFrozen
    }

    init {
        _selectedSourceTypeLiveData.value = sourceTypes.first()
    }

    fun homeButtonClicked() {
        router.back()
    }

    fun openSourceChooserClicked() {
        selectedSourceTypeLiveData.value?.let {
            _showSourceChooserLiveData.value = Event(Payload(sourceTypes, it))
        }
    }

    fun sourceTypeChanged(it: ImportSource) {
        _selectedSourceTypeLiveData.value = it
    }

    fun nextClicked() = launch {
        importInProgressLiveData.value = true

        val sourceType = selectedSourceTypeLiveData.value!!

        val cryptoType = cryptoTypeChooserMixin.selectedEncryptionTypeFlow.first().cryptoType
        val derivationPath = derivationPathLiveData.value.orEmpty()
        val name = nameLiveData.value!!

        viewModelScope.launch {
            import(sourceType, name, derivationPath, cryptoType)
                .onSuccess { continueBasedOnCodeStatus() }
                .onFailure(::handleCreateAccountError)

            importInProgressLiveData.value = false
        }
    }

    fun systemCallResultReceived(requestCode: Int, intent: Intent) {
        val selectedSource = selectedSourceTypeLiveData.value!!

        if (selectedSource is FileRequester) {
            val currentRequestCode = selectedSource.chooseJsonFileEvent.value!!.peekContent()

            if (requestCode == currentRequestCode) {
                selectedSource.fileChosen(intent.data!!)
            }
        }
    }

    private suspend fun continueBasedOnCodeStatus() {
        if (interactor.isCodeSet()) {
            router.openMain()
        } else {
            router.openCreatePincode()
        }
    }

    private fun handleCreateAccountError(throwable: Throwable) {
        var errorMessage = selectedSourceTypeLiveData.value?.handleError(throwable)

        if (errorMessage == null) {
            errorMessage = when (throwable) {
                is AccountAlreadyExistsException -> ImportError(
                    titleRes = R.string.account_add_already_exists_message,
                    messageRes = R.string.account_error_try_another_one
                )
                is JunctionDecoder.DecodingError, is BIP32JunctionDecoder.DecodingError -> ImportError(
                    titleRes = R.string.account_invalid_derivation_path_title,
                    messageRes = R.string.account_invalid_derivation_path_message
                )
                else -> ImportError()
            }
        }

        val title = resourceManager.getString(errorMessage.titleRes)
        val message = resourceManager.getString(errorMessage.messageRes)

        showError(title, message)
    }

    private fun provideSourceType(): List<ImportSource> {
        return listOf(
            MnemonicImportSource(),
            JsonImportSource(
                nameLiveData,
                cryptoTypeChooserMixin,
                addAccountInteractor,
                resourceManager,
                clipboardManager,
                fileReader,
                viewModelScope,
                payload
            ),
            RawSeedImportSource()
        )
    }

    private suspend fun import(
        sourceType: ImportSource,
        name: String,
        derivationPath: String,
        cryptoType: CryptoType
    ): Result<Unit> {
        val addAccountType = mapAddAccountPayloadToAddAccountType(payload)

        return when (sourceType) {
            is MnemonicImportSource -> addAccountInteractor.importFromMnemonic(
                accountName = name,
                mnemonic = sourceType.mnemonicContentLiveData.value!!,
                encryptionType = cryptoType,
                derivationPath = derivationPath,
                addAccountType = addAccountType
            )
            is RawSeedImportSource -> addAccountInteractor.importFromSeed(
                accountName = name,
                seed = sourceType.rawSeedLiveData.value!!,
                encryptionType = cryptoType,
                derivationPath = derivationPath,
                addAccountType = addAccountType
            )
            is JsonImportSource -> addAccountInteractor.importFromJson(
                json = sourceType.jsonContentLiveData.value!!,
                password = sourceType.passwordLiveData.value!!,
                name = name,
                addAccountType = addAccountType
            )
        }
    }
}
