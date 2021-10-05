package jp.co.soramitsu.feature_account_impl.presentation.importing

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import jp.co.soramitsu.common.base.BaseViewModel
import jp.co.soramitsu.common.mixin.MixinFactory
import jp.co.soramitsu.common.resources.ClipboardManager
import jp.co.soramitsu.common.resources.ResourceManager
import jp.co.soramitsu.common.utils.Event
import jp.co.soramitsu.common.utils.combine
import jp.co.soramitsu.common.utils.map
import jp.co.soramitsu.common.utils.switchMap
import jp.co.soramitsu.common.view.ButtonState
import jp.co.soramitsu.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet.Payload
import jp.co.soramitsu.core.model.CryptoType
import jp.co.soramitsu.feature_account_api.domain.interfaces.AccountAlreadyExistsException
import jp.co.soramitsu.feature_account_api.domain.interfaces.AccountInteractor
import jp.co.soramitsu.feature_account_api.presenatation.account.add.AddAccountPayload
import jp.co.soramitsu.feature_account_impl.R
import jp.co.soramitsu.feature_account_impl.data.mappers.mapAddAccountPayloadToAddAccountType
import jp.co.soramitsu.feature_account_impl.domain.account.add.AddAccountInteractor
import jp.co.soramitsu.feature_account_impl.presentation.AccountRouter
import jp.co.soramitsu.feature_account_impl.presentation.common.mixin.api.CryptoTypeChooserMixin
import jp.co.soramitsu.feature_account_impl.presentation.common.mixin.api.ForcedChainMixin
import jp.co.soramitsu.feature_account_impl.presentation.common.mixin.api.WithCryptoTypeChooserMixin
import jp.co.soramitsu.feature_account_impl.presentation.common.mixin.api.WithForcedChainMixin
import jp.co.soramitsu.feature_account_impl.presentation.common.mixin.impl.CryptoTypeChooserFactory
import jp.co.soramitsu.feature_account_impl.presentation.importing.source.model.FileRequester
import jp.co.soramitsu.feature_account_impl.presentation.importing.source.model.ImportError
import jp.co.soramitsu.feature_account_impl.presentation.importing.source.model.ImportSource
import jp.co.soramitsu.feature_account_impl.presentation.importing.source.model.JsonImportSource
import jp.co.soramitsu.feature_account_impl.presentation.importing.source.model.MnemonicImportSource
import jp.co.soramitsu.feature_account_impl.presentation.importing.source.model.RawSeedImportSource
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
