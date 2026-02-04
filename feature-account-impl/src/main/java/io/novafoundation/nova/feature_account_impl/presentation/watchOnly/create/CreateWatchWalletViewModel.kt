package io.novafoundation.nova.feature_account_impl.presentation.watchOnly.create

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AddressInputMixinFactory
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.isInputValid
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.mixinWithInputFlow
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.setAddress
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.watchOnly.create.CreateWatchWalletInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class WatchOnlyAccountType {
    CUSTOM, DEMO
}

class CreateWatchWalletViewModel(
    private val router: AccountRouter,
    private val addressInputMixinFactory: AddressInputMixinFactory,
    private val interactor: CreateWatchWalletInteractor,
    private val accountInteractor: AccountInteractor,
    private val resourceManager: ResourceManager,
    private val appLinksProvider: AppLinksProvider
) : BaseViewModel() {

    private val customPage = createCustomPage()
    private val demoPage = createDemoPage()
    val pages = listOf(customPage, demoPage)

    private val selectedMode = MutableStateFlow(WatchOnlyAccountType.CUSTOM)

    private val selectedPage = selectedMode.map {
        when (it) {
            WatchOnlyAccountType.CUSTOM -> customPage
            WatchOnlyAccountType.DEMO -> demoPage
        }
    }.shareInBackground()

    val nameFlow = selectedPage.flatMapLatest { it.config.nameInput }
        .shareInBackground()
    val substrateAddressFlow = selectedPage.flatMapLatest { it.config.substrateAddressInput.mixinWithInputFlow() }
        .shareInBackground()
    val evmAddressFlow = selectedPage.flatMapLatest { it.config.evmAddressInput.mixinWithInputFlow() }
        .shareInBackground()

    private val termsAreChecked = MutableStateFlow(false)

    val buttonState = combine(
        nameFlow,
        substrateAddressFlow,
        evmAddressFlow,
        termsAreChecked
    ) { name, substrateAddress, evmAddress, termsChecked ->
        when {
            name.isEmpty() -> disabledStateFrom(R.string.account_enter_wallet_nickname)
            !substrateAddress.isInputValid() && !evmAddress.isInputValid() -> disabledStateFrom(R.string.watch_only_add_any_address)
            !termsChecked -> disabledStateFrom(R.string.watch_only_accept_terms)
            else -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_continue))
        }
    }.shareInBackground()

    private val _scamWarningEvent = MutableLiveData<Event<ScamRiskWarningBottomSheet.Payload>>()
    val scamWarningEvent: LiveData<Event<ScamRiskWarningBottomSheet.Payload>> = _scamWarningEvent

    private val _openEmailEvent = MutableLiveData<Event<String>>()
    val openEmailEvent: LiveData<Event<String>> = _openEmailEvent

    fun homeButtonClicked() {
        router.back()
    }

    fun customAccountSelected() {
        selectedMode.value = WatchOnlyAccountType.CUSTOM
    }

    fun demoAccountSelected() {
        selectedMode.value = WatchOnlyAccountType.DEMO
    }

    fun nextClicked() {
        _scamWarningEvent.value = Event(ScamRiskWarningBottomSheet.Payload(appLinksProvider.email))
    }

    fun onMailClicked() {
        _openEmailEvent.value = appLinksProvider.email.event()
    }

    fun onWarningConfirmed() = launch {
        val result = withContext(Dispatchers.Default) {
            interactor.createWallet(
                name = nameFlow.first(),
                substrateAddress = substrateAddressFlow.first().input,
                evmAddress = evmAddressFlow.first().input,
            )
        }

        result
            .onSuccess { continueBasedOnCodeStatus() }
            .onFailure { it.printStackTrace(); showError(it) }
    }

    fun onTermsChecked(isChecked: Boolean) {
        termsAreChecked.value = isChecked
    }

    private fun disabledStateFrom(@StringRes reason: Int): DescriptiveButtonState {
        return DescriptiveButtonState.Disabled(resourceManager.getString(reason))
    }

    private suspend fun continueBasedOnCodeStatus() {
        if (accountInteractor.isCodeSet()) {
            router.openMain()
        } else {
            router.openCreatePincode()
        }
    }

    private fun createCustomPage() = WatchOnlyModePageModel(
        modeName = resourceManager.getString(R.string.watch_only_custom_title),
        config = createPage(isEditable = true)
    )

    private fun createDemoPage(): WatchOnlyModePageModel {
        val demoAccount = interactor.demoAccount()

        return WatchOnlyModePageModel(
            modeName = resourceManager.getString(R.string.watch_only_demo_title),
            config = createPage(
                isEditable = false,
                defaultName = demoAccount.name,
                defaultSubstrateAddress = demoAccount.substrateAddress,
                defaultEvmAddress = demoAccount.evmAddress
            )
        )
    }

    private fun createPage(
        isEditable: Boolean,
        defaultName: String = "",
        defaultSubstrateAddress: String = "",
        defaultEvmAddress: String = ""
    ): WatchOnlyModePageModel.Config {
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

        nameInput.value = defaultName
        substrateAddressInput.setAddress(defaultSubstrateAddress)
        evmAddressInput.setAddress(defaultEvmAddress)

        return WatchOnlyModePageModel.Config(
            isEditable = isEditable,
            nameInput = nameInput,
            substrateAddressInput = substrateAddressInput,
            evmAddressInput = evmAddressInput
        )
    }
}
