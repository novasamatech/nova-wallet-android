package io.novafoundation.nova.feature_dapp_impl.presentation.main

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.createAddressIcon
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.defaultSubstrateAddress
import io.novafoundation.nova.feature_dapp_api.data.model.DappMetadata
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.data.mappers.mapDappMetadataToDappModel
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainDAppViewModel(
    private val router: DAppRouter,
    private val addressIconGenerator: AddressIconGenerator,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val dappInteractor: DappInteractor,
) : BaseViewModel(), Browserable {

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    val currentAddressIconFlow = selectedAccountUseCase.selectedMetaAccountFlow()
        .map { addressIconGenerator.createAddressIcon(it.defaultSubstrateAddress, AddressIconGenerator.SIZE_BIG) }
        .inBackground()
        .share()

    private val groupedDAppsFlow = flowOf {
        dappInteractor.getDAppMetadatasByCategory().mapValues { (_, dapps) ->
            dapps.map(::mapDappMetadataToDappModel)
        }
    }
        .inBackground()
        .share()

    private val categoriesFlow = groupedDAppsFlow.map { it.keys.toList() }
        .distinctUntilChanged()
        .inBackground()
        .share()

    val categoriesStateFlow = categoriesFlow
        .withLoading()
        .share()

    private val _selectedCategoryPositionFlow = MutableStateFlow(0)
    val selectedCategoryPositionFlow: Flow<Int> = _selectedCategoryPositionFlow

    val shownDappsFlow = combine(groupedDAppsFlow, _selectedCategoryPositionFlow) { grouping, category ->
        grouping.getValue(categoriesFlow.first()[category])
    }
        .withLoading()
        .share()

    init {
        syncDApps()
    }

    fun categorySelected(position: Int) = launch {
        _selectedCategoryPositionFlow.emit(position)
    }

    fun accountIconClicked() {
        router.openChangeAccount()
    }

    fun dappClicked(dapp: DappModel) {
        router.openDAppBrowser(dapp.url)
    }

    private fun syncDApps() = launch {
        dappInteractor.syncDAppMetadatas()
    }
}
