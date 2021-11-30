package io.novafoundation.nova.feature_account_impl.presentation.account.advancedEncryption

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.input.Input
import io.novafoundation.nova.common.utils.input.ifModifiable
import io.novafoundation.nova.common.utils.input.map
import io.novafoundation.nova.common.utils.input.modifyIfNotNull
import io.novafoundation.nova.common.utils.input.modifyInput
import io.novafoundation.nova.common.utils.input.valueOrNull
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.account.add.chainIdOrNull
import io.novafoundation.nova.feature_account_impl.data.mappers.mapCryptoTypeToCryptoTypeModel
import io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.AdvancedEncryptionInteractor
import io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.valiadtion.AdvancedEncryptionValidationPayload
import io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.valiadtion.AdvancedEncryptionValidationSystem
import io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.valiadtion.mapAdvancedEncryptionValidationFailureToUi
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.AdvancedEncryptionCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.AdvancedEncryptionResponder
import io.novafoundation.nova.feature_account_impl.presentation.view.advanced.encryption.model.CryptoTypeModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AdvancedEncryptionViewModel(
    private val router: AccountRouter,
    private val payload: AddAccountPayload,
    private val interactor: AdvancedEncryptionInteractor,
    private val resourceManager: ResourceManager,
    private val validationSystem: AdvancedEncryptionValidationSystem,
    private val validationExecutor: ValidationExecutor,
    private val advancedEncryptionResponder: AdvancedEncryptionResponder,
) : BaseViewModel(),
    Validatable by validationExecutor {

    private val encryptionTypes = getCryptoTypeModels()

    private val _substrateCryptoTypeInput = singleReplaySharedFlow<Input<CryptoTypeModel>>()
    val substrateCryptoTypeInput: Flow<Input<CryptoTypeModel>> = _substrateCryptoTypeInput

    private val _substrateDerivationPathInput = singleReplaySharedFlow<Input<String>>()
    val substrateDerivationPathInput: Flow<Input<String>> = _substrateDerivationPathInput

    private val _ethereumCryptoTypeInput = singleReplaySharedFlow<Input<CryptoTypeModel>>()
    val ethereumCryptoTypeInput: Flow<Input<CryptoTypeModel>> = _ethereumCryptoTypeInput

    private val _ethereumDerivationPathInput = singleReplaySharedFlow<Input<String>>()
    val ethereumDerivationPathInput: Flow<Input<String>> = _ethereumDerivationPathInput

    val showSubstrateEncryptionTypeChooserEvent = MutableLiveData<Event<DynamicListBottomSheet.Payload<CryptoTypeModel>>>()

    init {
        loadInitialState()
    }

    private fun loadInitialState() = launch {
        val initialState = interactor.getInitialInputState(payload.chainIdOrNull)

        val latestState = advancedEncryptionResponder.lastState

        val initialSubstrateType = initialState.substrateCryptoType.modifyIfNotNull(latestState?.substrateCryptoType)
        val initialSubstrateDerivationPath = initialState.substrateDerivationPath.modifyIfNotNull(latestState?.substrateDerivationPath)
        val initialEthereumType = initialState.ethereumCryptoType.modifyIfNotNull(latestState?.ethereumCryptoType)
        val initialEthereumDerivationPath = initialState.ethereumDerivationPath.modifyIfNotNull(latestState?.ethereumDerivationPath)

        _substrateCryptoTypeInput.emit(initialSubstrateType.map(::encryptionTypeToUi))
        _substrateDerivationPathInput.emit(initialSubstrateDerivationPath)
        _ethereumCryptoTypeInput.emit(initialEthereumType.map(::encryptionTypeToUi))
        _ethereumDerivationPathInput.emit(initialEthereumDerivationPath)
    }

    fun substrateDerivationPathChanged(new: String) = _substrateDerivationPathInput.modifyInputAsync(new)

    fun ethereumDerivationPathChanged(new: String) = _ethereumDerivationPathInput.modifyInputAsync(new)

    fun substrateEncryptionClicked() = substrateCryptoTypeInput.ifModifiable { current ->
        showSubstrateEncryptionTypeChooserEvent.value = Event(DynamicListBottomSheet.Payload(encryptionTypes, current))
    }

    fun substrateEncryptionChanged(newCryptoType: CryptoTypeModel) {
        launch {
            _substrateCryptoTypeInput.modifyInput(newCryptoType)
        }
    }

    fun homeButtonClicked() {
        router.back()
    }

    fun applyClicked() = launch {
        val payload = AdvancedEncryptionValidationPayload(
            substrateDerivationPathInput = substrateDerivationPathInput.first(),
            ethereumDerivationPathInput = ethereumDerivationPathInput.first()
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformer = { mapAdvancedEncryptionValidationFailureToUi(resourceManager, it) }
        ) {
            respondWithCurrentState()
        }
    }

    private fun respondWithCurrentState() = launch {
        val response = AdvancedEncryptionCommunicator.Response(
            substrateCryptoType = substrateCryptoTypeInput.first().valueOrNull?.cryptoType,
            substrateDerivationPath = substrateDerivationPathInput.first().valueOrNull,
            ethereumCryptoType = ethereumCryptoTypeInput.first().valueOrNull?.cryptoType,
            ethereumDerivationPath = ethereumDerivationPathInput.first().valueOrNull,
        )

        advancedEncryptionResponder.respond(response)

        router.back()
    }

    private fun getCryptoTypeModels(): List<CryptoTypeModel> {
        val types = interactor.getCryptoTypes()

        return types.map { mapCryptoTypeToCryptoTypeModel(resourceManager, it) }
    }

    private fun <I> Flow<Input<I>>.ifModifiable(action: suspend (I) -> Unit) {
        launch {
            first().ifModifiable { action(it) }
        }
    }

    private fun <I> MutableSharedFlow<Input<I>>.modifyInputAsync(newValue: I) {
        launch {
            modifyInput(newValue)
        }
    }

    private fun encryptionTypeToUi(encryptionType: CryptoType): CryptoTypeModel = mapCryptoTypeToCryptoTypeModel(resourceManager, encryptionType)
}
