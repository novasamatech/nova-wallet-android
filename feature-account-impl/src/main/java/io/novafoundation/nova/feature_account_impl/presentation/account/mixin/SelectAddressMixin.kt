package io.novafoundation.nova.feature_account_impl.presentation.account.mixin

import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.SelectAddressMixin
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.SelectAddressRequester
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.toRequestFilter
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
        val metaAccountFilter = payload.filter.toRequestFilter()
        val request = SelectAddressRequester.Request(payload.chain.id, selectedAddress, metaAccountFilter)
        selectAddressRequester.openRequest(request)
    }
}
