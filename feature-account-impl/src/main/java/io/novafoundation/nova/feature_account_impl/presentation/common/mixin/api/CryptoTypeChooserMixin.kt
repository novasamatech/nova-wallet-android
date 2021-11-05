package io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api

import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet.Payload
import io.novafoundation.nova.feature_account_impl.presentation.view.advanced.encryption.model.CryptoTypeModel
import kotlinx.coroutines.flow.Flow

interface WithCryptoTypeChooserMixin {

    val cryptoTypeChooserMixin: CryptoTypeChooserMixin
}

interface CryptoTypeChooserMixin {

    val selectedEncryptionTypeFlow: Flow<CryptoTypeModel>
    val selectionFrozen: Flow<Boolean>

    val encryptionTypeChooserEvent: LiveData<Event<Payload<CryptoTypeModel>>>

    fun chooseEncryptionClicked()
    fun selectedEncryptionChanged(newCryptoType: CryptoTypeModel)
}
