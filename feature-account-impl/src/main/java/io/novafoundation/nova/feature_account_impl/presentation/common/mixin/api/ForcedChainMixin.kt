package io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api

import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import kotlinx.coroutines.flow.Flow

interface ForcedChainMixin {

    val forcedChainLiveData: Flow<ChainUi?>
}

interface WithForcedChainMixin {

    val forcedChainMixin: ForcedChainMixin
}
