package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import kotlinx.coroutines.flow.Flow

class SimpleMaxActionProvider(
    override val maxAvailableForDisplay: Flow<Balance>,
    override val maxAvailableForAction: Flow<MaxActionProvider.MaxAvailableForAction?>
) : MaxActionProvider
