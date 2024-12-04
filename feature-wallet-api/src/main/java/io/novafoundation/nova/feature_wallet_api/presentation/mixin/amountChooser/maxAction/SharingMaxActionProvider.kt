package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction

import io.novafoundation.nova.common.utils.shareInBackground
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class SharingMaxActionProvider(
    inner: MaxActionProvider,
    coroutineScope: CoroutineScope
) : MaxActionProvider, CoroutineScope by coroutineScope {

    override val maxAvailableBalance: Flow<MaxAvailableBalance> = inner.maxAvailableBalance.shareInBackground()
}
