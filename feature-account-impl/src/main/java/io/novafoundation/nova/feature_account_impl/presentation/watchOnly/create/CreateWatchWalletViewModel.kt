package io.novafoundation.nova.feature_account_impl.presentation.watchOnly.create

import androidx.annotation.StringRes
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.common.view.ChipActionsModel
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AddressInputMixinFactory
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.isAddressValid
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.data.repository.WatchWalletSuggestion
import io.novafoundation.nova.feature_account_impl.domain.watchOnly.create.CreateWatchWalletInteractor
import io.novafoundation.nova.feature_account_api.presenatation.AccountRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateWatchWalletViewModel(
    private val router: AccountRouter,
    private val addressInputMixinFactory: AddressInputMixinFactory,
    private val interactor: CreateWatchWalletInteractor,
    private val accountInteractor: AccountInteractor,
    private val resourceManager: ResourceManager
) : BaseViewModel() {

    val nameInput = MutableStateFlow("")

    val substrateAddressInput = with(addressInputMixinFactory) {
        create(
            inputSpecProvider = substrateInputSpec(),
            errorDisplayer = this@CreateWatchWalletViewModel::showError,
            showAccountEvent = null,
            coroutineScope = this@CreateWatchWalletViewModel,
        )
    }

    val evmAddressInput = with(addressInputMixinFactory) {
        create(
            inputSpecProvider = evmInputSpec(),
            errorDisplayer = this@CreateWatchWalletViewModel::showError,
            showAccountEvent = null,
            coroutineScope = this@CreateWatchWalletViewModel,
        )
    }

    val buttonState = combine(
        nameInput,
        substrateAddressInput.inputFlow,
        evmAddressInput.inputFlow
    ) { name, substrateAddress, evmAddress ->
        when {
            name.isEmpty() -> disabledStateFrom(R.string.account_enter_wallet_nickname)
            !substrateAddressInput.isAddressValid(substrateAddress) -> disabledStateFrom(R.string.accoount_enter_substrate_address)
            evmAddress.isNotEmpty() && !evmAddressInput.isAddressValid(evmAddress) -> disabledStateFrom(R.string.accoount_enter_evm_address)
            else -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_continue))
        }
    }

    private val walletSuggestions = flowOf { interactor.suggestions() }
        .shareInBackground()

    val suggestionChipActionModels = walletSuggestions.mapList(::mapWalletSuggestionToChipAction)
        .shareInBackground()

    fun homeButtonClicked() {
        router.back()
    }

    fun nextClicked() = launch {
        val result = withContext(Dispatchers.Default) {
            interactor.createWallet(
                name = nameInput.value,
                substrateAddress = substrateAddressInput.inputFlow.value,
                evmAddress = evmAddressInput.inputFlow.value
            )
        }

        result
            .onSuccess { continueBasedOnCodeStatus() }
            .onFailure { it.printStackTrace(); showError(it) }
    }

    fun walletSuggestionClicked(index: Int) {
        launch {
            val suggestion = walletSuggestions.first()[index]

            nameInput.value = suggestion.name
            substrateAddressInput.inputFlow.value = suggestion.substrateAddress
            evmAddressInput.inputFlow.value = suggestion.evmAddress.orEmpty()
        }
    }

    private fun disabledStateFrom(@StringRes reason: Int): DescriptiveButtonState {
        return DescriptiveButtonState.Disabled(resourceManager.getString(reason))
    }

    private fun mapWalletSuggestionToChipAction(walletSuggestion: WatchWalletSuggestion): ChipActionsModel {
        return ChipActionsModel(action = walletSuggestion.name)
    }

    private suspend fun continueBasedOnCodeStatus() {
        if (accountInteractor.isCodeSet()) {
            router.openMain()
        } else {
            router.openCreatePincode()
        }
    }
}
