package io.novafoundation.nova.feature_account_api.presenatation.mixin.selectSingleWallet

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.feature_account_api.domain.filter.selectAddress.SelectAccountFilter
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface SelectSingleWalletMixin {

    class Payload(val chain: Chain, val filter: SelectAccountFilter)

    interface Factory {

        fun create(
            coroutineScope: CoroutineScope,
            payloadFlow: Flow<Payload>,
            onWalletSelect: (Long) -> Unit
        ): SelectSingleWalletMixin
    }

    val isSelectWalletAvailableFlow: Flow<Boolean>

    suspend fun openSelectWallet(selectedWallet: Long?)
}

fun BaseFragment<*, *>.bindSelectWallet(selectAddressMixin: SelectSingleWalletMixin, isAvailable: (Boolean) -> Unit) {
    selectAddressMixin.isSelectWalletAvailableFlow.observe {
        isAvailable(it)
    }
}
