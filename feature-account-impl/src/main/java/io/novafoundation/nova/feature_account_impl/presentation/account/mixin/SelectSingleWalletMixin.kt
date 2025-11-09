package io.novafoundation.nova.feature_account_impl.presentation.account.mixin

import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.presenatation.mixin.common.toRequestFilter
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectSingleWallet.SelectSingleWalletMixin
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectSingleWallet.SelectSingleWalletRequester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class SelectSingleWalletMixinFactory(
    private val selectSingleWalletRequester: SelectSingleWalletRequester,
    private val metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
) : SelectSingleWalletMixin.Factory {

    override fun create(
        coroutineScope: CoroutineScope,
        payloadFlow: Flow<SelectSingleWalletMixin.Payload>,
        onWalletSelect: (Long) -> Unit
    ): SelectSingleWalletMixin {
        return RealSelectSingleWalletMixin(
            coroutineScope,
            selectSingleWalletRequester,
            payloadFlow,
            metaAccountGroupingInteractor,
            onWalletSelect
        )
    }
}

class RealSelectSingleWalletMixin(
    private val coroutineScope: CoroutineScope,
    private val selectSingleWalletRequester: SelectSingleWalletRequester,
    private val payloadFlow: Flow<SelectSingleWalletMixin.Payload>,
    private val metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
    private val onWalletSelect: (Long) -> Unit
) : SelectSingleWalletMixin {

    init {
        selectSingleWalletRequester.responseFlow
            .onEach { onWalletSelect(it.metaId) }
            .launchIn(coroutineScope)
    }

    override val isSelectWalletAvailableFlow: Flow<Boolean> = payloadFlow.map { payload ->
        metaAccountGroupingInteractor.hasAvailableMetaAccountsForChain(payload.chain.id, payload.filter)
    }

    override suspend fun openSelectWallet(selectedWallet: Long?) {
        val payload = payloadFlow.first()
        val metaAccountFilter = payload.filter.toRequestFilter()
        val request = SelectSingleWalletRequester.Request(payload.chain.id, selectedWallet, metaAccountFilter)
        selectSingleWalletRequester.openRequest(request)
    }
}
