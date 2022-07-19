package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput

import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.inputSpec.AddressInputSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow


interface AddressInputMixin {

    suspend fun getInputSpec(): AddressInputSpec

    val inputFlow: MutableStateFlow<String>

    val state: Flow<AddressInputState>

    fun pasteClicked()

    fun clearClicked()

    fun scanClicked()

    fun myselfClicked()
}
