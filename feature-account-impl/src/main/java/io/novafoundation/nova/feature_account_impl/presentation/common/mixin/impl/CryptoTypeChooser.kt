package io.novafoundation.nova.feature_account_impl.presentation.common.mixin.impl

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.mixin.MixinFactory
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet.Payload
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_impl.data.mappers.mapCryptoTypeToCryptoTypeModel
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.CryptoTypeChooserMixin
import io.novafoundation.nova.feature_account_impl.presentation.view.advanced.encryption.model.CryptoTypeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CryptoTypeChooserFactory(
    private val interactor: AccountInteractor,
    private val addAccountPayload: AddAccountPayload,
    private val resourceManager: ResourceManager
) : MixinFactory<CryptoTypeChooserMixin> {

    override fun create(scope: CoroutineScope): CryptoTypeChooser {
        return CryptoTypeChooser(interactor, addAccountPayload, scope, resourceManager)
    }
}

class CryptoTypeChooser(
    private val interactor: AccountInteractor,
    private val addAccountPayload: AddAccountPayload,
    private val scope: CoroutineScope,
    private val resourceManager: ResourceManager
) : CryptoTypeChooserMixin {

    private val encryptionTypes = getCryptoTypeModels()

    override val selectedEncryptionTypeFlow = MutableSharedFlow<CryptoTypeModel>(replay = 1)
    override val selectionFrozen = MutableSharedFlow<Boolean>(replay = 1)

    init {
        scope.launch(Dispatchers.Default) {
            val chainId = addAccountPayload.castOrNull<AddAccountPayload.ChainAccount>()?.chainId

            val (cryptoType, frozen) = interactor.getPreferredCryptoType(chainId)

            selectedEncryptionTypeFlow.emit(mapCryptoTypeToCryptoTypeModel(resourceManager, cryptoType))
            selectionFrozen.emit(frozen)
        }
    }

    override val encryptionTypeChooserEvent = MutableLiveData<Event<Payload<CryptoTypeModel>>>()

    override fun chooseEncryptionClicked() = ensureNotFrozen {
        val selectedType = selectedEncryptionTypeFlow.first()

        encryptionTypeChooserEvent.value = Event(Payload(encryptionTypes, selectedType))
    }

    override fun selectedEncryptionChanged(newCryptoType: CryptoTypeModel) = ensureNotFrozen {
        selectedEncryptionTypeFlow.emit(newCryptoType)
    }

    private inline fun ensureNotFrozen(crossinline action: suspend () -> Unit) {
        scope.launch {
            if (selectionFrozen.first().not()) {
                action()
            }
        }
    }

    private fun getCryptoTypeModels(): List<CryptoTypeModel> {
        val types = interactor.getCryptoTypes()

        return types.map { mapCryptoTypeToCryptoTypeModel(resourceManager, it) }
    }
}
