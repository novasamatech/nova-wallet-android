package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface AddressInputMixin {

    val inputFlow: MutableStateFlow<String>

    val state: Flow<AddressInputState>

    fun pasteClicked()

    fun clearClicked()

    fun scanClicked()

    fun myselfClicked()
}
