package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.inputSpec.AddressInputSpec
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface AddressInputMixin {

    suspend fun getInputSpec(): AddressInputSpec

    val inputFlow: MutableStateFlow<String>

    val state: Flow<AddressInputState>

    val showExternalAccountsFlow: Flow<ExternalAccountsWithSelected>

    val selectedExternalIdentifierFlow: Flow<ExtendedLoadingState<ExternalAccount?>>

    fun pasteClicked()

    fun clearClicked()

    fun scanClicked()

    fun myselfClicked()

    fun selectedExternalAddressClicked()

    fun onInputFocusChanged()

    fun onKeyboardGone()

    fun selectExternalAccount(it: ExternalAccount)

    fun getExternalAccountIdentifier(): String?

    suspend fun getAddress(): String

    fun isValidExternalAccount(externalAccount: ExternalAccount): Boolean

    fun clearExtendedAccount()
}

class ExternalAccountsWithSelected(
    val accounts: List<ExternalAccount>,
    val selected: ExternalAccount?
)

suspend fun AddressInputMixin.isAddressValid(input: String) = getInputSpec().isValidAddress(input)
