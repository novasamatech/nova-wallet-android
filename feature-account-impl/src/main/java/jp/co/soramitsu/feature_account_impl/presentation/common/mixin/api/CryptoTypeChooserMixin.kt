package jp.co.soramitsu.feature_account_impl.presentation.common.mixin.api

import androidx.lifecycle.LiveData
import jp.co.soramitsu.common.utils.Event
import jp.co.soramitsu.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet.Payload
import jp.co.soramitsu.feature_account_impl.presentation.view.advanced.encryption.model.CryptoTypeModel
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
