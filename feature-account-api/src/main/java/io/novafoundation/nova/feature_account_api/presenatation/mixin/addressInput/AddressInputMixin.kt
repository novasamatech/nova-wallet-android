package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput

import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.externalAccount.ExternalAccountResolver
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.inputSpec.AddressInputSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface AddressInputMixin : ExternalAccountResolver {

    suspend fun getInputSpec(): AddressInputSpec

    val inputFlow: MutableStateFlow<String>

    val state: Flow<AddressInputState>

    fun pasteClicked()

    fun clearClicked()

    fun scanClicked()

    fun myselfClicked()

    suspend fun getAddress(): String

    fun clearExtendedAccount()
}

suspend fun AddressInputMixin.isAddressValid(input: String) = getInputSpec().isValidAddress(input)

fun AddressInputMixin.setAddress(input: String) {
    inputFlow.value = input
}
