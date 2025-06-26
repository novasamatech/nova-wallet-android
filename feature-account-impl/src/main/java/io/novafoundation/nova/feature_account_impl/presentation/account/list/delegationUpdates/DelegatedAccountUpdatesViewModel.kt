package io.novafoundation.nova.feature_account_impl.presentation.account.list.delegationUpdates

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.delegated.DelegatedMetaAccountUpdatesListingMixin.FilterType
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.delegated.DelegatedMetaAccountUpdatesListingMixinFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DelegatedAccountUpdatesViewModel(
    private val delegatedMetaAccountUpdatesListingMixinFactory: DelegatedMetaAccountUpdatesListingMixinFactory,
    private val accountRouter: AccountRouter,
    private val appLinksProvider: AppLinksProvider,
    private val accountInteractor: AccountInteractor
) : BaseViewModel(), Browserable {

    private val listingMixin = delegatedMetaAccountUpdatesListingMixinFactory.create(viewModelScope)

    val filtersAvailableFlow = listingMixin.accountTypeFilter.map { it !is FilterType.UserIgnored }

    val accounts: Flow<List<Any>> = listingMixin.metaAccountsFlow

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    fun clickAbout() {
        openBrowserEvent.value = appLinksProvider.wikiProxy.event()
    }

    fun showProxieds() {
        listingMixin.filterBy(FilterType.Proxied)
    }

    fun showMultisig() {
        listingMixin.filterBy(FilterType.Multisig)
    }

    fun clickDone() {
        launch {
            accountInteractor.switchToNotDeactivatedAccountIfNeeded()

            accountRouter.back()
        }
    }
}
