package io.novafoundation.nova.feature_account_impl.presentation.account.mixin

import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.utils.Filter
import io.novafoundation.nova.feature_account_api.domain.filter.MetaAccountFilter
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.SelectAddressMixin
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.SelectAddressRequester
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class SelectAddressMixinFactory(
    private val selectAddressRequester: SelectAddressRequester,
    private val metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
) : SelectAddressMixin.Factory {

    override fun create(
        coroutineScope: CoroutineScope,
        payloadFlow: Flow<SelectAddressMixin.Payload>,
        onAddressSelect: (String) -> Unit
    ): SelectAddressMixin {
        return RealSelectAddressMixin(
            coroutineScope,
            selectAddressRequester,
            payloadFlow,
            metaAccountGroupingInteractor,
            onAddressSelect
        )
    }
}

class RealSelectAddressMixin(
    private val coroutineScope: CoroutineScope,
    private val selectAddressRequester: SelectAddressRequester,
    private val payloadFlow: Flow<SelectAddressMixin.Payload>,
    private val metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
    private val onAddressSelect: (String) -> Unit
) : SelectAddressMixin {

    init {
        selectAddressRequester.responseFlow
            .onEach { onAddressSelect(it.selectedAddress) }
            .launchIn(coroutineScope)
    }

    override val isSelectAddressAvailableFlow: Flow<Boolean> = payloadFlow.map { payload ->
        metaAccountGroupingInteractor.hasAvailableMetaAccountsForChain(payload.chain.id, payload.filter)
    }

    override suspend fun openSelectAddress(selectedAddress: String?) {
        val payload = payloadFlow.first()
        val metaAccountFilter = getMetaAccountsFilterPayload(payload.filter)
        val request = SelectAddressRequester.Request(payload.chain.id, selectedAddress, metaAccountFilter)
        selectAddressRequester.openRequest(request)
    }

    private suspend fun getMetaAccountsFilterPayload(filter: Filter<MetaAccount>): SelectAddressRequester.Request.Filter {
        return if (filter is MetaAccountFilter && filter.mode == MetaAccountFilter.Mode.EXCLUDE) {
            SelectAddressRequester.Request.Filter.ExcludeMetaIds(filter.metaIds)
        } else {
            SelectAddressRequester.Request.Filter.Everything
        }
    }
}
