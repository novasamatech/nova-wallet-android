package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput

import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.inputSpec.AddressInputSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface AddressInputMixin {

    suspend fun getInputSpec(): AddressInputSpec

    val inputFlow: MutableStateFlow<String>

    val state: Flow<AddressInputState>

    val externalIdentifierEventLiveData: LiveData<AccountIdentifierProvider.Event>

    val selectedExternalAccountFlow: Flow<ExtendedLoadingState<ExternalAccount?>>

    fun pasteClicked()

    fun clearClicked()

    fun scanClicked()

    fun myselfClicked()

    fun selectedExternalAddressClicked()

    fun loadExternalIdentifiers()

    fun selectExternalAccount(it: ExternalAccount)

    suspend fun getAddress(): String

    fun clearExtendedAccount()
}

suspend fun AddressInputMixin.isAddressValid(input: String) = getInputSpec().isValidAddress(input)
