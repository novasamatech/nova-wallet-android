package io.novafoundation.nova.feature_dapp_impl.presentation.main

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.createAddressIcon
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.defaultSubstrateAddress
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainDAppViewModel(
    private val router: DAppRouter,
    private val addressIconGenerator: AddressIconGenerator,
    private val selectedAccountUseCase: SelectedAccountUseCase,
) : BaseViewModel(), Browserable {

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    val currentAddressIcon = selectedAccountUseCase.selectedMetaAccountFlow()
        .map { addressIconGenerator.createAddressIcon(it.defaultSubstrateAddress, AddressIconGenerator.SIZE_BIG) }
        .inBackground()
        .share()

    fun accountIconClicked() {
        router.openChangeAccount()
    }

    // TODO urls are hardcoded since this is placeholder for future work as the part of dapp tasks
    fun subIdClicked() = launch {
//        val defaultAddress = selectedAccountUseCase.getSelectedMetaAccount().defaultSubstrateAddress
//        val subIdUrl = "https://sub.id/#/$defaultAddress"
//
//        openBrowserEvent.value = subIdUrl.event()
//
        router.openDAppBrowser()
    }
}
